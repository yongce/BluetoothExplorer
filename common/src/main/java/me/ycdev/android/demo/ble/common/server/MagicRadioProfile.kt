package me.ycdev.android.demo.ble.common.server

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import java.util.UUID

object MagicRadioProfile {
    val RADIO_SERVICE: UUID = UUID.fromString("0000a001-0000-43cd-bf2a-a96c0e493e29")
    val FM_ONE_CHARACTERISTIC: UUID = UUID.fromString("0000a001-1000-43cd-bf2a-a96c0e493e29")
    val FM_TWO_CHARACTERISTIC: UUID = UUID.fromString("0000a001-2000-43cd-bf2a-a96c0e493e29")

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