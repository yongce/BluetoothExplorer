package me.ycdev.android.bluetooth.ble.server

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattService
import android.content.Context
import androidx.annotation.AnyThread
import androidx.annotation.CallSuper
import java.util.UUID
import me.ycdev.android.bluetooth.ble.BleCharacteristicInfo
import me.ycdev.android.bluetooth.ble.BleConfigs
import me.ycdev.android.bluetooth.ble.server.BlePeripheralHelper.Contract
import me.ycdev.android.lib.common.utils.EncodingUtils.encodeWithHex
import me.ycdev.android.lib.common.utils.MainHandler
import timber.log.Timber

abstract class BleGattServerBase(val ownerTag: String, val context: Context) : BleAdvertiser,
    Contract {

    private val peripheralHelper =
        BlePeripheralHelper(context, this)

    fun setOperationTimeout(timeoutMs: Long) {
        peripheralHelper.operationTimeout = timeoutMs
    }

    override fun isAdvertising(): Boolean = peripheralHelper.isAdvertising()

    /**
     * @param resultCallback Will be invoked in main thread.
     */
    @AnyThread
    override fun start(resultCallback: ((Boolean) -> Unit)?) {
        BleConfigs.bleHandler.post {
            val success = peripheralHelper.start()
            if (resultCallback != null) {
                MainHandler.post {
                    resultCallback(success)
                }
            }
        }
    }

    /**
     * @param resultCallback Will be invoked in main thread.
     */
    @AnyThread
    override fun stop(resultCallback: (() -> Unit)?) {
        BleConfigs.bleHandler.post {
            peripheralHelper.stop()
            if (resultCallback != null) {
                MainHandler.post {
                    resultCallback()
                }
            }
        }
    }

    /**
     * It will be invoked when the read operation is requested by the remote device.
     *
     * The default implementation just ignore the request and print a waring logs.
     * You can override it if you want to handle the read operation.
     */
    override fun onCharacteristicReadRequest(characteristic: BluetoothGattCharacteristic): ByteArray? {
        return null
    }

    /**
     * It will be invoked when data is received from the remote device.
     *
     * The default implementation just print logs if needed.
     * You can override it if you want to handle the incoming data.
     */
    @CallSuper
    override fun onIncomingData(
        device: BluetoothDevice,
        characteristic: BleCharacteristicInfo,
        value: ByteArray
    ) {
        if (BleConfigs.bleDataLog) {
            Timber.tag(ownerTag).v(
                "Received data [%s] at %s from %s",
                encodeWithHex(value),
                characteristic, device
            )
        }
    }

    /**
     * Split the data into segments to suit the MTU limit for sending the data.
     *
     * The default implementation is just to send the entire data directly.
     * You have to override it if the data may exceed the MTU limit.
     */
    override fun packetDataForSend(
        device: BluetoothDevice,
        mtu: Int,
        data: ByteArray
    ): List<ByteArray> {
        return arrayListOf(data)
    }

    fun addService(gattServer: BluetoothGattServer, service: BluetoothGattService) =
        peripheralHelper.addService(gattServer, service)

    fun sendData(
        device: BluetoothDevice,
        serviceUuid: UUID,
        characteristicUUid: UUID,
        data: ByteArray
    ) = peripheralHelper.sendData(device, serviceUuid, characteristicUUid, data)

    fun notifyRegisteredDevices(func: (BluetoothDevice) -> Unit) =
        peripheralHelper.notifyRegisteredDevices(func)
}
