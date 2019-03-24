package me.ycdev.android.ble.common.sig

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import java.util.UUID

/**
 * https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.battery_service.xml
 */
object BatteryServiceProfile {
    /**
     * Battery Service UUID
     */
    val BATTERY_SERVICE: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
    val BATTERY_LEVEL_CHARACTERISTIC: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

    fun createBatteryService(): BluetoothGattService {
        val service = BluetoothGattService(BATTERY_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val batteryLevel = BluetoothGattCharacteristic(
            BATTERY_LEVEL_CHARACTERISTIC,
            // Read-only characteristic, supports notifications
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        service.addCharacteristic(batteryLevel)
        return service
    }
}