package me.ycdev.android.ble.common.server

import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.annotation.CallSuper
import me.ycdev.android.ble.common.BleDebugConfigs
import me.ycdev.android.lib.common.utils.EncodingUtils
import timber.log.Timber
import java.util.UUID

abstract class BleGattServerBase(val ownerTag: String, val context: Context) : BleAdvertiser,
    BlePeripheralHelper.Contract {

    val peripheralHelper = BlePeripheralHelper(context, this)

    fun setOperationTimeout(timeoutMs: Long) {
        peripheralHelper.operationTimeout = timeoutMs
    }

    override fun isAdvertising(): Boolean = peripheralHelper.isAdvertising()

    override fun start(): Boolean = peripheralHelper.start()

    override fun stop() = peripheralHelper.stop()

    @CallSuper
    override fun onIncomingData(
        device: BluetoothDevice,
        characteristicUuid: UUID,
        value: ByteArray
    ) {
        if (BleDebugConfigs.bleDataLog) {
            Timber.tag(ownerTag)
                .v("Received data [%s]", EncodingUtils.encodeWithHex(value))
        }
    }

    fun sendData(
        device: BluetoothDevice,
        serviceUuid: UUID,
        characteristicUUid: UUID,
        data: ByteArray
    ) = peripheralHelper.sendData(device, serviceUuid, characteristicUUid, data)
}