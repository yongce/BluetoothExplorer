package me.ycdev.android.ble.common.internal

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.os.SystemClock
import me.ycdev.android.ble.common.BleConfigs
import me.ycdev.android.ble.common.BleException
import me.ycdev.android.ble.common.BluetoothHelper
import me.ycdev.android.ble.common.internal.BleGattHelperBase.Operation.NO_OP
import timber.log.Timber
import java.util.UUID

internal open class BleGattHelperBase {
    var operationTimeout: Long = OPERATION_TIMEOUT_DEFAULT

    private val defaultWorkspace = DeviceWorkspace()
    private val workspacesMapping = hashMapOf<BluetoothDevice, DeviceWorkspace>()
    protected var operationLock = Object()

    protected fun getWorkspace(device: BluetoothDevice?): DeviceWorkspace {
        synchronized(workspacesMapping) {
            if (device == null) {
                return defaultWorkspace
            }

            var ws = workspacesMapping[device]
            if (ws == null) {
                ws = DeviceWorkspace()
                workspacesMapping[device] = ws
            }
            return ws
        }
    }

    @Throws(BleException::class)
    protected fun waitForOperationLocked(device: BluetoothDevice?, uuid: UUID?, op: Operation) {
        if (BleConfigs.bleOperationLog) {
            Timber.tag(TAG).d(
                "waitForOperation device[%s] operation[%s] uuid[%s]",
                device, op, uuid
            )
        }

        val ws = getWorkspace(device)
        ws.setOperation(op, uuid)

        try {
            val timeStart = SystemClock.elapsedRealtime()
            operationLock.wait(operationTimeout)
            if (ws.curOperation != Operation.NO_OP) {
                throw BleException(
                    "Operation[%s] for %s/%s timeout after %dms",
                    op, device ?: "-", uuid ?: "-", operationTimeout
                )
            }

            val timeUsed = SystemClock.elapsedRealtime() - timeStart
            if (timeUsed >= operationTimeout) {
                Timber.tag(TAG).w("Operation[%s] timeout, timeUsed: %dms", op, timeUsed)
            } else {
                if (BleConfigs.bleOperationLog) {
                    Timber.tag(TAG).d("Operation[%s] done, timeUsed: %dms", op, timeUsed)
                }
            }

            checkExceptionLocked(device)
        } finally {
            ws.resetOperation()
        }
    }

    protected fun checkExceptionLocked(device: BluetoothDevice?) {
        val ws = getWorkspace(device)
        val anyException = ws.curException
        ws.curException = null
        if (anyException != null) {
            throw anyException
        }
    }

    protected fun checkAndNotify(status: Int, op: Operation) {
        synchronized(operationLock) {
            try {
                checkGattStatusCodeLocked(null, status, op)
                checkOperationLocked(null, op)
                notifyCompletionLocked(null)
            } catch (e: BleException) {
                notifyExceptionLocked(null, e)
            }
        }
    }

    protected fun checkAndNotify(device: BluetoothDevice, status: Int, op: Operation) {
        checkAndNotify(device, status, op, null)
    }

    protected fun checkAndNotify(device: BluetoothDevice, status: Int, op: Operation, uuid: UUID?) {
        synchronized(operationLock) {
            try {
                checkGattStatusCodeLocked(device, status, op)
                if (uuid != null) {
                    checkCharacteristicUuidLocked(device, uuid)
                }
                checkOperationLocked(device, op)
                notifyCompletionLocked(device)
            } catch (e: BleException) {
                notifyExceptionLocked(device, e)
            }
        }
    }

    private fun notifyCompletionLocked(device: BluetoothDevice?) {
        val ws = getWorkspace(device)
        if (BleConfigs.bleOperationLog) {
            Timber.tag(TAG).d("notifyCompletion: %s", ws.curOperation)
        }
        operationLock.notify()
        ws.curOperation = NO_OP
    }

    protected fun notifyExceptionLocked(device: BluetoothDevice?, e: BleException) {
        Timber.tag(TAG).w("notifyException: %s", e.toString())
        val ws = getWorkspace(device)
        ws.curException = e
        notifyCompletionLocked(device)
    }

    @Throws(BleException::class)
    private fun checkGattStatusCodeLocked(
        device: BluetoothDevice?,
        status: Int,
        operation: Operation
    ) {
        when (status) {
            BluetoothGatt.GATT_SUCCESS -> {
            }
            BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION -> throw BleException("Not paired yet")
            else -> {
                if (device != null) {
                    throw BleException(
                        "Operation [%s] on device [%s] failed: %s",
                        operation, device, BluetoothHelper.gattStatusCodeStr(status)
                    )
                } else {
                    throw BleException(
                        "Operation [%s] failed: %s",
                        operation, BluetoothHelper.gattStatusCodeStr(status)
                    )
                }
            }
        }
    }

    private fun checkCharacteristicUuidLocked(device: BluetoothDevice, uuid: UUID) {
        val ws = getWorkspace(device)
        if (ws.curUuid == null) {
            Timber.tag(TAG).w(
                "Unexpected event from characteristic [%s] on device [%s] while doing operation [%s]",
                uuid, device, ws.curOperation
            )
        }
        if (uuid != ws.curUuid) {
            Timber.tag(TAG).w(
                "Unexpected characteristic [%s] ([%s] expected) on device [%s] while doing operation [%s]",
                uuid, ws.curUuid, device, ws.curOperation
            )
        }
    }

    private fun checkOperationLocked(device: BluetoothDevice?, operation: Operation) {
        val ws = getWorkspace(device)
        if (ws.curOperation != operation) {
            Timber.tag(TAG).w(
                "Unexpected result for operation [%s] while doing operation [%s] on device [%s]",
                operation, ws.curOperation, device
            )
        }
    }

    enum class Operation {
        NO_OP,
        ADD_SERVICE,
        CONNECT,
        DISCONNECT,
        DISCOVERY_SERVICES,
        CONFIG_MTU,
        READ_CHARACTERISTIC,
        WRITE_CHARACTERISTIC,
        READ_DESCRIPTOR,
        WRITE_DESCRIPTOR
    }

    class DeviceWorkspace {
        var mtu = 23 - 3 // TODO 3 bytes will be gone if use 23 directly

        var curOperation: Operation = NO_OP
        var curUuid: UUID? = null
        var curException: Exception? = null

        fun setOperation(op: Operation, uuid: UUID?) {
            curOperation = op
            curUuid = uuid
            curException = null
        }

        fun resetOperation() {
            curOperation = NO_OP
            curUuid = null
            curException = null
        }
    }

    companion object {
        private const val TAG = "BleGattHelperBase"

        const val OPERATION_TIMEOUT_DEFAULT = 10_000L // 10 seconds
    }
}
