package me.ycdev.android.bluetooth.ble.client

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import me.ycdev.android.bluetooth.ble.BleCharacteristicInfo
import me.ycdev.android.bluetooth.ble.BleConfigs
import me.ycdev.android.bluetooth.ble.client.BleCentralHelper.Contract
import me.ycdev.android.lib.common.utils.EncodingUtils.encodeWithHex
import timber.log.Timber
import java.util.UUID

abstract class BleGattClientBase(private val ownerTag: String, val context: Context) :
    Contract {

    private val centralHelper =
        BleCentralHelper(context, this)
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

    fun requestMtu(mtu: Int) = centralHelper.requestMtu(mtu)

    fun readData(serviceUuid: UUID, characteristicUUid: UUID) =
        centralHelper.readData(serviceUuid, characteristicUUid)

    fun sendData(serviceUuid: UUID, characteristicUUid: UUID, data: ByteArray) =
        centralHelper.sendData(serviceUuid, characteristicUUid, data)

    fun listen(characteristic: BluetoothGattCharacteristic) =
        centralHelper.listen(characteristic)

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

    /**
     * It will be invoked when data is received from the remote device.
     *
     * The default implementation just print logs if needed.
     * You can override it if you want to handle the incoming data.
     */
    @CallSuper
    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BleCharacteristicInfo,
        data: ByteArray
    ) {

        if (BleConfigs.bleDataLog) {
            Timber.tag(ownerTag).v(
                "Received data [%s] from %s",
                encodeWithHex(data), characteristic
            )
        }
    }

    /**
     * Split the data into segments to suit the MTU limit for sending the data.
     *
     * The default implementation is just to send the entire data directly.
     * You have to override it if the data may exceed the MTU limit.
     */
    override fun packetDataForSend(mtu: Int, data: ByteArray): List<ByteArray> {
        return arrayListOf(data)
    }

    interface Callback {
        @MainThread
        fun onStateChanged(device: BluetoothDevice, newState: ClientState)
        @MainThread
        fun onServicesDiscovered(device: BluetoothDevice, services: List<BluetoothGattService>)
    }
}
