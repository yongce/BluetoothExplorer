package me.ycdev.android.ble.common.server

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import me.ycdev.android.ble.common.BleDebugConfigs
import me.ycdev.android.ble.common.BleException
import me.ycdev.android.ble.common.BleGattHelperBase
import me.ycdev.android.ble.common.BleGattHelperBase.Operation.WRITE_CHARACTERISTIC
import me.ycdev.android.ble.common.BluetoothHelper
import me.ycdev.android.lib.common.packets.PacketsWorker
import me.ycdev.android.lib.common.utils.EncodingUtils
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.Arrays
import java.util.UUID
import kotlin.math.max

class BlePeripheralHelper(val context: Context, val contract: Contract) : BleGattHelperBase() {
    private val bleAdvertiser = BleAdvertiserSimple(context)
    protected var gattServer: BluetoothGattServer? = null
    private val gattServerCallback = MyGattServerCallback()

    // device address -> (characteristic uuid -> pending write buffer)
    private val writerBuffer = hashMapOf<String, HashMap<UUID, ByteBuffer>>()

    private val registeredDevices = mutableSetOf<BluetoothDevice>()

    fun isAdvertising(): Boolean = bleAdvertiser.isAdvertising()

    @MainThread
    fun start(): Boolean {
        if (!BluetoothHelper.canDoBleOperations(context)) {
            return false
        }

        // TODO what will happen if switch the order?

        // Step 1: Start BLE advertiser
        bleAdvertiser.setSettings(contract.buildAdvertiseSettings())
            .setData(contract.buildAdvertiseData())
            .setCallback(contract.getAdvertiseCallback())
        if (!bleAdvertiser.start()) {
            return false
        }

        // Step 2: Start BLE GATT server
        gattServer = BluetoothHelper.getBluetoothManager(context)
            ?.openGattServer(context, gattServerCallback)
        if (gattServer == null) {
            Timber.tag(TAG).w("Failed to get BluetoothGattServer")
            bleAdvertiser.stop()
            return false
        }

        if (!contract.addBleServices(gattServer!!)) {
            bleAdvertiser.stop()
            return false
        }

        contract.onStart()
        return true
    }

    @MainThread
    fun stop() {
        contract.onStop()

        // Step 1: Stop BLE GATT server
        gattServer?.close()

        // Step 2: Stop BLE advertiser
        bleAdvertiser.stop()
    }

    /**
     * Send update to any devices that are subscribed to the characteristic.
     */
    fun notifyRegisteredDevices(func: (BluetoothDevice) -> Unit) {
        if (registeredDevices.isEmpty()) {
            Timber.tag(TAG).d("No devices registered. Discard notification.")
            return
        }

        Timber.tag(TAG).d("Sending update to %d devices", registeredDevices.size)
        registeredDevices.forEach {
            func(it)
        }
    }

    fun sendData(
        device: BluetoothDevice,
        serviceUuid: UUID,
        characteristicUUid: UUID,
        data: ByteArray,
        confirm: Boolean = false
    ) {
        bleHandler.post { doSendData(device, serviceUuid, characteristicUUid, data, confirm) }
    }

    @WorkerThread
    private fun doSendData(
        device: BluetoothDevice,
        serviceUuid: UUID,
        characteristicUUid: UUID,
        data: ByteArray,
        confirm: Boolean
    ) {
        Timber.tag(TAG).d("sendData[%d] to [%s]", data.size, device)
        if (data.isEmpty()) {
            Timber.tag(TAG).w("Zero length data, ignore")
            return
        }

        val gatt = gattServer
        if (gatt == null) {
            Timber.tag(TAG).w("GattServer not started, failed to send data")
            return
        }

        val service = gatt.getService(serviceUuid)
        if (service == null) {
            Timber.tag(TAG).w("no GATT service found, failed to send data")
            return
        }

        val characteristic = service.getCharacteristic(characteristicUUid)
        if (characteristic == null) {
            Timber.tag(TAG).w("no characteristic found, failed to send data")
            return
        }

        // always set the latest MTU before packaging data
        val dataParser = contract.getPacketsWorker(device, characteristicUUid)
            ?: throw BleException("No PacketsWorker configured")
        dataParser.maxPacketSize = getWorkspace(device).mtu
        val segments = dataParser.packetData(data)

        try {
            synchronized(operationLock) {
                for (s in segments) {
                    if (BleDebugConfigs.bleDataLog) {
                        Timber.tag(TAG).v("Sending data [%s]", EncodingUtils.encodeWithHex(s))
                    }
                    if (!characteristic.setValue(s) || !gatt.notifyCharacteristicChanged(device, characteristic, confirm)) {
                        Timber.tag(TAG).w("Failed to write characteristic")
                        return
                    }

                    waitForOperationLocked(device, characteristicUUid, WRITE_CHARACTERISTIC)
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w("Failed to send data: %s", e.toString())
        }
    }

    private inner class MyGattServerCallback : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            if (BleDebugConfigs.bleOperationLog) {
                Timber.tag(TAG).d(
                    "onConnectionStateChange device[%s] status[%s] newState[%s]",
                    device, BluetoothHelper.gattStatusCodeStr(status),
                    BluetoothHelper.connectionStateStr(newState)
                )
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (contract.notifyAllConnectedDevices()) {
                    registeredDevices.add(device)
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // Remove device from any active subscriptions
                registeredDevices.remove(device)
            }
        }

        override fun onDescriptorReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            descriptor: BluetoothGattDescriptor
        ) {
            if (BleDebugConfigs.bleOperationLog) {
                Timber.tag(TAG).d(
                    "onDescriptorReadRequest, device[%s] reqId[%d]",
                    device, requestId
                )
            }
            if (contract.getClientConfigDescriptorUuid() == descriptor.uuid) {
                val result = if (registeredDevices.contains(device)) {
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                } else {
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                }
                gattServer?.sendResponse(
                    device, requestId, BluetoothGatt.GATT_SUCCESS, 0, result
                )
            } else {
                Timber.tag(TAG).w("Unknown descriptor read request uuid[%s]", descriptor.uuid)
                gattServer?.sendResponse(
                    device, requestId, BluetoothGatt.GATT_FAILURE, 0, null
                )
            }
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            if (BleDebugConfigs.bleOperationLog) {
                Timber.tag(TAG).d(
                    "onDescriptorWriteRequest, device[%s] reqId[%d]",
                    device, requestId
                )
            }
            if (contract.getClientConfigDescriptorUuid() == descriptor.uuid) {
                var status = BluetoothGatt.GATT_SUCCESS
                if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
                    Timber.tag(TAG).d("Device[%s] subscribe to notifications", device)
                    registeredDevices.add(device)
                } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
                    Timber.tag(TAG).d("Device[%s] unsubscribe from notifications", device)
                    registeredDevices.remove(device)
                } else {
                    Timber.tag(TAG).w("Unknown value: %s", Arrays.toString(value))
                    status = BluetoothGatt.GATT_FAILURE
                }

                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, status, 0, null)
                }
            } else {
                Timber.tag(TAG).w("Unknown descriptor write request uuid[%s]", descriptor.uuid)
                if (responseNeeded) {
                    gattServer?.sendResponse(
                        device, requestId, BluetoothGatt.GATT_FAILURE, 0, null
                    )
                }
            }
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (BleDebugConfigs.bleOperationLog) {
                Timber.tag(TAG).d(
                    "onCharacteristicReadRequest device[%s] reqId[%d]",
                    device, requestId
                )
            }
            val value = contract.onCharacteristicReadRequest(characteristic)
            gattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            val uuid = characteristic.uuid
            if (BleDebugConfigs.bleOperationLog) {
                Timber.tag(TAG).d(
                    "onCharacteristicWriteRequest device[%s] reqId[%d] uuid=[%s] preparedWrite=[%s] responseNeeded=[%s]",
                    device, requestId, characteristic.uuid, preparedWrite, responseNeeded
                )
            }
            if (preparedWrite) {
                var deviceBuffer = writerBuffer[device.address]
                if (deviceBuffer == null) {
                    deviceBuffer = hashMapOf()
                    writerBuffer[device.address] = deviceBuffer
                }
                var buffer = deviceBuffer[uuid]
                if (buffer == null) {
                    buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE)!!
                    deviceBuffer[uuid] = buffer
                }

                if (buffer.remaining() >= value.size) {
                    buffer.put(value)
                } else {
                    val newBufSize = max(buffer.capacity() + value.size, buffer.capacity() * 2)
                    val newBuffer = ByteBuffer.allocate(newBufSize)
                    buffer.flip()
                    newBuffer.put(buffer)
                    newBuffer.put(value)
                    deviceBuffer[uuid] = newBuffer
                }
            } else {
                contract.onIncomingData(device, uuid, value)
            }

            if (responseNeeded) {
                gattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value)
            }
        }

        override fun onExecuteWrite(device: BluetoothDevice, requestId: Int, execute: Boolean) {
            if (BleDebugConfigs.bleOperationLog) {
                Timber.tag(TAG).d(
                    "onExecuteWrite device[%s] reqId[%d] execute[%s]",
                    device, requestId, execute
                )
            }
            gattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
            val deviceBuffer = writerBuffer[device.address]
            if (deviceBuffer != null) {
                for ((uuid, buf) in deviceBuffer) {
                    if (!execute) {
                        // write request canceled
                        buf.clear()
                        continue
                    }

                    buf.flip()
                    if (buf.remaining() == 0) {
                        // no data
                        buf.clear()
                        continue
                    }

                    val data = ByteArray(buf.remaining())
                    buf.get(data)
                    buf.clear()
                    contract.onIncomingData(device, uuid, data)
                }
            }
        }

        override fun onNotificationSent(device: BluetoothDevice, status: Int) {
            if (BleDebugConfigs.bleOperationLog) {
                Timber.tag(TAG).d(
                    "onNotificationSent device[%s] status[%s]",
                    device, BluetoothHelper.gattStatusCodeStr(status)
                )
            }
            checkAndNotify(device, status, WRITE_CHARACTERISTIC)
        }

        override fun onMtuChanged(device: BluetoothDevice, mtu: Int) {
            Timber.tag(TAG).d("onMtuChanged device[%s] mtu[%d]", device, mtu)
            this@BlePeripheralHelper.getWorkspace(device).mtu = mtu
        }

        override fun onServiceAdded(status: Int, service: BluetoothGattService) {
            Timber.tag(TAG).d(
                "onServiceAdded service[%s] status[%s]",
                service.uuid, BluetoothHelper.gattStatusCodeStr(status)
            )
        }
    }

    interface Contract {
        // config
        fun buildAdvertiseSettings(): AdvertiseSettings
        fun buildAdvertiseData(): AdvertiseData
        fun addBleServices(gattServer: BluetoothGattServer): Boolean

        fun getAdvertiseCallback(): AdvertiseCallback? = null
        fun getClientConfigDescriptorUuid(): UUID? = null
        fun notifyAllConnectedDevices(): Boolean = false
        fun getPacketsWorker(
            device: BluetoothDevice,
            characteristicUuid: UUID
        ): PacketsWorker? = null

        // callbacks
        fun onStart() {
        }

        fun onStop() {
        }

        fun onCharacteristicReadRequest(characteristic: BluetoothGattCharacteristic): ByteArray?

        fun onIncomingData(
            device: BluetoothDevice,
            characteristicUuid: UUID,
            value: ByteArray
        )
    }

    companion object {
        private const val TAG = "BlePeripheralHelper"

        const val DEFAULT_BUFFER_SIZE = 1024
    }
}
