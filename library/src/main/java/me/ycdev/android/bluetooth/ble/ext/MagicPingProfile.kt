package me.ycdev.android.bluetooth.ble.ext

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import java.util.UUID

object MagicPingProfile {
    const val BLE_OPERATION_TIMEOUT: Long = 15_000 // 15 seconds

    val PING_SERVICE: UUID = UUID.fromString("0000a001-0000-4080-973d-fe81d6397f29")
    val PING_CHARACTERISTIC: UUID = UUID.fromString("0000a001-1000-4080-973d-fe81d6397f29")

    fun createService(): BluetoothGattService {
        val service = BluetoothGattService(PING_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        val ping = BluetoothGattCharacteristic(
            PING_CHARACTERISTIC,
            // Read-only characteristic, supports notifications
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        service.addCharacteristic(ping)

        return service
    }
}
