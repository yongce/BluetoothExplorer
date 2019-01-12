package me.ycdev.android.demo.ble.common.client

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.content.Context
import androidx.annotation.MainThread
import me.ycdev.android.demo.ble.common.BluetoothHelper
import me.ycdev.android.demo.ble.common.client.ClientState.CONNECTED
import me.ycdev.android.demo.ble.common.client.ClientState.CONNECTING
import me.ycdev.android.demo.ble.common.client.ClientState.DISCONNECTED
import me.ycdev.android.demo.ble.common.client.ClientState.DISCONNECTING
import me.ycdev.android.lib.common.utils.MainHandler
import timber.log.Timber

class BleGattClient(val context: Context) {
    private val appContext = context.applicationContext
    private val gattCallback = MyGattCallback()
    private var device: BluetoothDevice? = null
    private var gatt: BluetoothGatt? = null
    private var callerCallback: Callback? = null
    private var state = ClientState.DISCONNECTED

    fun isStarted() = state == CONNECTED || state == CONNECTING

    fun connect(address: String, callback: Callback): Boolean {
        if (!BluetoothHelper.canDoBleOperations(appContext)) {
            return false
        }
        val device = BluetoothHelper.getBluetoothAdapter(appContext)
            ?.getRemoteDevice(address)
        if (device == null) {
            Timber.tag(TAG).w("Failed to get remote device [%s]", address)
            return false
        }
        return connect(device, callback)
    }

    fun connect(device: BluetoothDevice, callback: Callback): Boolean {
        Timber.tag(TAG).d("Connect to %s", device)

        // try to close the previous GATT client first
        gatt?.close()

        callerCallback = callback
        if (state == DISCONNECTED || state == DISCONNECTING) {
            updateState(CONNECTING)
        }

        this.device = device
        gatt = device.connectGatt(appContext, false, gattCallback)
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

    private fun updateState(newState: ClientState) {
        Timber.tag(TAG).d("updateState: %s", newState)
        state = newState
        MainHandler.post { callerCallback?.onStateChanged(newState) }
    }

    private inner class MyGattCallback : BluetoothGattCallback() {
        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            Timber.tag(TAG).d(
                "onReadRemoteRssi, rssi[%d] status[%s]",
                rssi, BluetoothHelper.gattStatusCodeStr(status)
            )
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            Timber.tag(TAG).d(
                "onCharacteristicRead, uuid[%s] status[%s]",
                characteristic.uuid,
                BluetoothHelper.gattStatusCodeStr(status)
            )
            // on data available
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            Timber.tag(TAG).d("onCharacteristicChanged, uuid[%s]", characteristic.uuid)
            // on data available
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            Timber.tag(TAG).d(
                "onCharacteristicWrite, uuid[%s] status[%s]",
                characteristic.uuid,
                BluetoothHelper.gattStatusCodeStr(status)
            )
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Timber.tag(TAG).d(
                "onServicesDiscovered, status[%s]",
                BluetoothHelper.gattStatusCodeStr(status)
            )
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val services: List<BluetoothGattService>? = gatt.services
                services?.forEach { Timber.tag(TAG).d("Service found: %s", it.uuid) }
            }
        }

        override fun onPhyUpdate(gatt: BluetoothGatt, txPhy: Int, rxPhy: Int, status: Int) {
            Timber.tag(TAG).d(
                "onPhyUpdate, txPhy[%d] rxPhy[%d] status[%s]",
                txPhy, rxPhy,
                BluetoothHelper.gattStatusCodeStr(status)
            )
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Timber.tag(TAG).d(
                "onMtuChanged, mtu[%d] status[%s]",
                mtu, BluetoothHelper.gattStatusCodeStr(status)
            )
        }

        override fun onReliableWriteCompleted(gatt: BluetoothGatt, status: Int) {
            Timber.tag(TAG).d(
                "onReliableWriteCompleted, status[%s]",
                BluetoothHelper.gattStatusCodeStr(status)
            )
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            Timber.tag(TAG).d(
                "onDescriptorWrite, uuid[%s] status[%s]",
                descriptor.uuid,
                BluetoothHelper.gattStatusCodeStr(status)
            )
        }

        override fun onDescriptorRead(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            Timber.tag(TAG).d(
                "onDescriptorRead, uuid[%s] status[%s]",
                descriptor.uuid,
                BluetoothHelper.gattStatusCodeStr(status)
            )
        }

        override fun onPhyRead(gatt: BluetoothGatt, txPhy: Int, rxPhy: Int, status: Int) {
            Timber.tag(TAG).d(
                "onPhyRead, txPhy[%d] rxPhy[%d] status[%s]",
                txPhy, rxPhy,
                BluetoothHelper.gattStatusCodeStr(status)
            )
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Timber.tag(TAG).d(
                "onConnectionStateChange, status[%s] newState[%s] device[%s]",
                BluetoothHelper.gattStatusCodeStr(status),
                BluetoothHelper.gattConnectionStateStr(newState),
                gatt.device
            )
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    updateState(CONNECTED)
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_CONNECTING -> updateState(CONNECTING)
                BluetoothProfile.STATE_DISCONNECTING -> updateState(DISCONNECTING)
                BluetoothProfile.STATE_DISCONNECTED -> updateState(DISCONNECTED)
            }
        }
    }

    interface Callback {
        @MainThread
        fun onStateChanged(newState: ClientState)
        @MainThread
        fun onServicesDiscovered(services: List<BluetoothGattService>)
    }

    companion object {
        private const val TAG = "BleGattClient"
    }
}
