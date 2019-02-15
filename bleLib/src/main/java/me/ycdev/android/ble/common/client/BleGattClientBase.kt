package me.ycdev.android.ble.common.client

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattService
import android.content.Context
import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import me.ycdev.android.ble.common.BleDebugConfigs
import me.ycdev.android.lib.common.utils.EncodingUtils
import timber.log.Timber
import java.util.UUID

abstract class BleGattClientBase(val ownerTag: String, val context: Context) :
    BleCentralHelper.Contract {

    protected val centralHelper = BleCentralHelper(context, this)
    private var callback: Callback? = null

    fun setOperationTimeout(timeoutMs: Long) {
        centralHelper.operationTimeout = timeoutMs
    }

    fun connect(address: String, callback: Callback): Boolean {
        this.callback = callback
        return centralHelper.connect(address)
    }

    fun connect(device: BluetoothDevice, callback: Callback): Boolean {
        this.callback = callback
        return centralHelper.connect(device)
    }

    fun discoveryServices() = centralHelper.discoveryServices()

    fun isStarted() = centralHelper.isStarted()

    fun isConnected() = centralHelper.isConnected()

    fun sendData(serviceUuid: UUID, characteristicUUid: UUID, data: ByteArray) =
        centralHelper.sendData(serviceUuid, characteristicUUid, data)

    @CallSuper
    open fun close() = centralHelper.close()

    @MainThread @CallSuper
    override fun onStateChanged(device: BluetoothDevice, newState: ClientState) {
        callback?.onStateChanged(device, newState)
    }

    @MainThread @CallSuper
    override fun onServicesDiscovered(device: BluetoothDevice, services: List<BluetoothGattService>) {
        callback?.onServicesDiscovered(device, services)
    }

    final override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        uuid: UUID,
        data: ByteArray
    ) {

        if (BleDebugConfigs.bleDataLog) {
            Timber.tag(ownerTag)
                .v("Received data [%s]", EncodingUtils.encodeWithHex(data))
        }
        getPacketsWorker().parsePackets(data)
    }

    interface Callback {
        @MainThread
        fun onStateChanged(device: BluetoothDevice, newState: ClientState)
        @MainThread
        fun onServicesDiscovered(device: BluetoothDevice, services: List<BluetoothGattService>)
    }
}
