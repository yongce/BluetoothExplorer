package me.ycdev.android.ble.common.ext

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService
import android.content.Context
import me.ycdev.android.ble.common.client.BleGattClientBase
import me.ycdev.android.ble.common.client.ClientState
import me.ycdev.android.lib.common.packets.PacketsWorker
import me.ycdev.android.lib.common.packets.RawPacketsWorker
import timber.log.Timber

class MagicRadioClient(context: Context) : BleGattClientBase(TAG, context), PacketsWorker.ParserCallback {
    private val packetsWorker = RawPacketsWorker(this)

    override fun getPacketsWorker(): PacketsWorker = packetsWorker

    override fun onDataParsed(data: ByteArray) {
        val message = String(data)
        Timber.tag(TAG).d("Received: %s", message)
    }

    override fun onStateChanged(device: BluetoothDevice, newState: ClientState) {
        super.onStateChanged(device, newState)
        Timber.tag(TAG).d("onStateChanged: %s", newState)
    }

    override fun onServicesDiscovered(
        device: BluetoothDevice,
        services: List<BluetoothGattService>
    ) {
        super.onServicesDiscovered(device, services)
        Timber.tag(TAG).d("onServicesDiscovered")
        services.find { it.uuid == MagicRadioProfile.RADIO_SERVICE }?.let {
            val fmOneCharacteristic = it.getCharacteristic(MagicRadioProfile.FM_ONE_CHARACTERISTIC)
            if (fmOneCharacteristic != null) {
                centralHelper.listen(fmOneCharacteristic)
            }

            val fmTwoCharacteristic = it.getCharacteristic(MagicRadioProfile.FM_TWO_CHARACTERISTIC)
            if (fmTwoCharacteristic != null) {
                centralHelper.listen(fmTwoCharacteristic)
            }
        }
    }

    companion object {
        private const val TAG = "MagicRadioClient"
    }
}
