package me.ycdev.android.ble.common.client

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import me.ycdev.android.ble.common.BleDebugConfigs
import me.ycdev.android.ble.common.BleException
import me.ycdev.android.ble.common.BleGattHelperBase
import me.ycdev.android.ble.common.BleGattHelperBase.Operation.CONFIG_MTU
import me.ycdev.android.ble.common.BleGattHelperBase.Operation.DISCOVERY_SERVICES
import me.ycdev.android.ble.common.BleGattHelperBase.Operation.READ_CHARACTERISTIC
import me.ycdev.android.ble.common.BleGattHelperBase.Operation.WRITE_CHARACTERISTIC
import me.ycdev.android.ble.common.BluetoothHelper
import me.ycdev.android.ble.common.client.ClientState.CONNECTED
import me.ycdev.android.ble.common.client.ClientState.CONNECTING
import me.ycdev.android.ble.common.client.ClientState.DISCONNECTED
import me.ycdev.android.ble.common.client.ClientState.DISCONNECTING
import me.ycdev.android.lib.common.packets.PacketsWorker
import me.ycdev.android.lib.common.utils.EncodingUtils
import me.ycdev.android.lib.common.utils.MainHandler
import timber.log.Timber
import java.util.UUID

class BleCentralHelper(val context: Context, val contract: Contract) : BleGattHelperBase() {
    private val gattCallback = MyGattCallback()
    private var device: BluetoothDevice? = null
    private var gatt: BluetoothGatt? = null
    private var state = ClientState.DISCONNECTED

    fun isStarted() = state == CONNECTED || state == CONNECTING

    fun isConnected() = state == CONNECTED

    fun connect(address: String): Boolean {
        if (!BluetoothHelper.canDoBleOperations(context)) {
            return false
        }
        val device = BluetoothHelper.getBluetoothAdapter(context)
            ?.getRemoteDevice(address)
        if (device == null) {
            Timber.tag(TAG).w("Failed to get remote device [%s]", address)
            return false
        }
        return connect(device)
    }

    fun connect(device: BluetoothDevice): Boolean {
        Timber.tag(TAG).d("Connect to %s", device)

        // try to close the previous GATT client first
        gatt?.close()

        if (state == DISCONNECTED || state == DISCONNECTING) {
            updateState(CONNECTING)
        }

        this.device = device
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // The connect requesting will fail with the error "onClientConnectionState() - status=133"
            // when use BluetoothDevice#TRANSPORT_AUTO and the remote device is Android N or higher version.
            gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            gatt = device.connectGatt(context, false, gattCallback)
        }
        return gatt != null
    }

    fun close() {
        if (gatt != null) {
            Timber.tag(TAG).d("Close the connection")
            // always use #close() instead of #disconnect() to avoid the following failure:
            //     "BluetoothGatt/D onClientRegistered() - status=133 clientIf=0"
            // The system has limit on the number of GATT clients:
            //     "BluetoothGatt/D onClientRegistered() - status=0 clientIf=32"
            gatt?.close()
            gatt = null
            state = DISCONNECTED
        }
    }

    fun discoveryServices() {
        bleHandler.post { doDiscoveryServices() }
    }

    @WorkerThread
    private fun doDiscoveryServices() {
        Timber.tag(TAG).d("discovery services...")
        if (state != CONNECTED) {
            Timber.tag(TAG).w("not connected, failed to discovery services")
            return
        }

        synchronized(operationLock) {
            val anyGatt = getConnectedDeviceLocked()
            anyGatt.discoverServices()
            waitForOperationLocked(anyGatt.device, null, DISCOVERY_SERVICES)
        }
    }

    fun listen(characteristic: BluetoothGattCharacteristic) {
        bleHandler.post { doListen(characteristic) }
    }

    @WorkerThread
    fun doListen(characteristic: BluetoothGattCharacteristic) {
        Timber.tag(TAG).d("listener on characteristic: %s", characteristic.uuid)
        synchronized(operationLock) {
            val anyGatt = getConnectedDeviceLocked()
            if (!anyGatt.setCharacteristicNotification(characteristic, true)) {
                throw BleException(
                    "Failed to enable characteristic notification [%s] on device [%s].",
                    characteristic.uuid, anyGatt.device
                )
            }
        }
    }

    fun requestMtu(mtu: Int) {
        bleHandler.post { doRequestMtu(mtu) }
    }

    @WorkerThread
    fun doRequestMtu(mtu: Int) {
        Timber.tag(TAG).d("request mtu: %d", mtu)
        synchronized(operationLock) {
            val anyGatt = getConnectedDeviceLocked()
            anyGatt.requestMtu(mtu)
            waitForOperationLocked(anyGatt.device, null, CONFIG_MTU)
        }
    }

    fun sendData(serviceUuid: UUID, characteristicUUid: UUID, data: ByteArray) {
        bleHandler.post { doSendData(serviceUuid, characteristicUUid, data) }
    }

    @WorkerThread
    private fun doSendData(serviceUuid: UUID, characteristicUUid: UUID, data: ByteArray) {
        Timber.tag(TAG).d("sendData: %d", data.size)
        if (data.isEmpty()) {
            Timber.tag(TAG).w("Zero length data, ignore")
            return
        }
        if (state != CONNECTED) {
            Timber.tag(TAG).w("not connected, failed to send data")
            return
        }

        val curGatt = gatt
        if (curGatt == null) {
            Timber.tag(TAG).w("no BluetoothGatt, failed to send data")
            return
        }

        val service = curGatt.getService(serviceUuid)
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
        val dataParser = contract.getPacketsWorker()
        dataParser.maxPacketSize = getWorkspace(curGatt.device).mtu
        val segments = dataParser.packetData(data)

        try {
            synchronized(operationLock) {
                for (s in segments) {
                    if (BleDebugConfigs.bleDataLog) {
                        Timber.tag(TAG).v("Sending data [%s]", EncodingUtils.encodeWithHex(s))
                    }
                    if (!characteristic.setValue(s)) {
                        Timber.tag(TAG).w("Failed to set characteristic value")
                        return
                    }
                    if (!curGatt.writeCharacteristic(characteristic)) {
                        Timber.tag(TAG).w("Failed to write characteristic")
                        return
                    }

                    waitForOperationLocked(curGatt.device, characteristicUUid, WRITE_CHARACTERISTIC)
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w("Failed to send data: %s", e.toString())
        }
    }

    private fun updateState(newState: ClientState) {
        Timber.tag(TAG).d("updateState: %s", newState)
        state = newState
        MainHandler.post { contract.onStateChanged(device!!, newState) }
    }

    private fun getConnectedDeviceLocked(): BluetoothGatt {
        val anyGatt = gatt ?: throw BleException("Bluetooth GATT not connected")
        checkExceptionLocked(anyGatt.device)
        return anyGatt
    }

    private fun checkAndNotify(gatt: BluetoothGatt, status: Int, op: Operation) {
        checkAndNotify(gatt, status, op, null)
    }

    private fun checkAndNotify(gatt: BluetoothGatt, status: Int, op: Operation, uuid: UUID?) {
        synchronized(operationLock) {
            try {
                val localGatt = checkDeviceLocked(gatt)
                checkAndNotify(localGatt.device, status, op, uuid)
            } catch (e: BleException) {
                notifyExceptionLocked(gatt.device, e)
            }
        }
    }

    @Throws(BleException::class)
    private fun checkDeviceLocked(gatt: BluetoothGatt): BluetoothGatt {
        val curGatt = this.gatt ?: throw BleException(
            "Received an event from device [%s] when not connected.",
            gatt.device
        )
        if (gatt.device != curGatt.device) {
            throw BleException(
                "Received an event from an unexpected device [%s]. Expected [%s]",
                gatt.device, curGatt.device
            )
        }
        return curGatt
    }

    private inner class MyGattCallback : BluetoothGattCallback() {
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            // We must read the data right now. Otherwise it may be updated by next incoming data.
            val uuid = characteristic.uuid
            val data = characteristic.value
            if (BleDebugConfigs.bleOperationLog) {
                Timber.tag(TAG).d("onCharacteristicChanged, uuid[%s]", uuid)
            }
            contract.onCharacteristicChanged(gatt, uuid, data)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (BleDebugConfigs.bleOperationLog) {
                Timber.tag(TAG).d(
                    "onCharacteristicRead, uuid[%s] status[%s]",
                    characteristic.uuid,
                    BluetoothHelper.gattStatusCodeStr(status)
                )
            }
            checkAndNotify(gatt, status, READ_CHARACTERISTIC, characteristic.uuid)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (BleDebugConfigs.bleOperationLog) {
                Timber.tag(TAG).d(
                    "onCharacteristicWrite, uuid[%s] status[%s]",
                    characteristic.uuid,
                    BluetoothHelper.gattStatusCodeStr(status)
                )
            }
            checkAndNotify(gatt, status, WRITE_CHARACTERISTIC, characteristic.uuid)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Timber.tag(TAG).d(
                "onServicesDiscovered, status[%s]",
                BluetoothHelper.gattStatusCodeStr(status)
            )

            checkAndNotify(gatt, status, DISCOVERY_SERVICES)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val services: List<BluetoothGattService>? = gatt.services
                services?.forEach { Timber.tag(TAG).d("Service found: %s", it.uuid) }
                if (services != null) {
                    MainHandler.post { contract.onServicesDiscovered(gatt.device, services) }
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Timber.tag(TAG).d(
                "onMtuChanged, mtu[%d] status[%s]",
                mtu, BluetoothHelper.gattStatusCodeStr(status)
            )
            if (status == BluetoothGatt.GATT_SUCCESS) {
                this@BleCentralHelper.getWorkspace(gatt.device).mtu = mtu
                checkAndNotify(gatt, status, CONFIG_MTU)
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (BleDebugConfigs.bleOperationLog) {
                Timber.tag(TAG).d(
                    "onDescriptorWrite, uuid[%s] status[%s]",
                    descriptor.uuid,
                    BluetoothHelper.gattStatusCodeStr(status)
                )
            }
        }

        override fun onDescriptorRead(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (BleDebugConfigs.bleOperationLog) {
                Timber.tag(TAG).d(
                    "onDescriptorRead, uuid[%s] status[%s]",
                    descriptor.uuid,
                    BluetoothHelper.gattStatusCodeStr(status)
                )
            }
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (BleDebugConfigs.bleOperationLog) {
                Timber.tag(TAG).d(
                    "onConnectionStateChange, status[%s] newState[%s] device[%s]",
                    BluetoothHelper.gattStatusCodeStr(status),
                    BluetoothHelper.connectionStateStr(newState),
                    gatt.device
                )
            }
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    updateState(CONNECTED)
                    discoveryServices()
                }
                BluetoothProfile.STATE_CONNECTING -> updateState(CONNECTING)
                BluetoothProfile.STATE_DISCONNECTING -> updateState(DISCONNECTING)
                BluetoothProfile.STATE_DISCONNECTED -> updateState(DISCONNECTED)
            }
        }
    }

    interface Contract {
        fun getPacketsWorker(): PacketsWorker

        @MainThread
        fun onStateChanged(device: BluetoothDevice, newState: ClientState)
        @MainThread
        fun onServicesDiscovered(device: BluetoothDevice, services: List<BluetoothGattService>)
        fun onCharacteristicChanged(gatt: BluetoothGatt, uuid: UUID, data: ByteArray)
    }

    companion object {
        private const val TAG = "BleCentralHelper"
    }
}
