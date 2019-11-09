package me.ycdev.android.bluetooth.ble.ext

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattService
import android.content.Context
import me.ycdev.android.bluetooth.ble.BleCharacteristicInfo
import me.ycdev.android.bluetooth.ble.client.BleGattClientBase
import me.ycdev.android.bluetooth.ble.client.ClientState
import timber.log.Timber

class MagicRadioClient(context: Context) : BleGattClientBase(
    TAG, context) {
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
                listen(fmOneCharacteristic)
            }

            val fmTwoCharacteristic = it.getCharacteristic(MagicRadioProfile.FM_TWO_CHARACTERISTIC)
            if (fmTwoCharacteristic != null) {
                listen(fmTwoCharacteristic)
            }
        }
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BleCharacteristicInfo,
        data: ByteArray
    ) {
        super.onCharacteristicChanged(gatt, characteristic, data)
        val message = String(data)
        Timber.tag(TAG).d("Received: %s", message)
    }

    companion object {
        private const val TAG = "MagicRadioClient"
    }
}
