package me.ycdev.android.bluetooth.ble.ext

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import java.util.UUID

object MagicRadioProfile {
    val RADIO_SERVICE: UUID = UUID.fromString("0000a002-0000-4b84-a28a-899f5a8b5f71")
    val FM_ONE_CHARACTERISTIC: UUID = UUID.fromString("0000a002-1000-4b84-a28a-899f5a8b5f71")
    val FM_TWO_CHARACTERISTIC: UUID = UUID.fromString("0000a002-2000-4b84-a28a-899f5a8b5f71")

    fun createService(): BluetoothGattService {
        val service = BluetoothGattService(RADIO_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        val fmOne = BluetoothGattCharacteristic(
            FM_ONE_CHARACTERISTIC,
            // Read-only characteristic, supports notifications
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        val fmTwo = BluetoothGattCharacteristic(
            FM_TWO_CHARACTERISTIC,
            // Read-only characteristic, supports notifications
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        service.addCharacteristic(fmOne)
        service.addCharacteristic(fmTwo)

        return service
    }
}