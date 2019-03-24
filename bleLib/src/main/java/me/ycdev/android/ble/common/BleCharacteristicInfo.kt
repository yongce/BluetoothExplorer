package me.ycdev.android.ble.common

import android.bluetooth.BluetoothGattCharacteristic
import java.util.UUID

data class BleCharacteristicInfo(
    val uuid: UUID,
    val instanceId: Int,
    val serviceInfo: BleServiceInfo
) {
    companion object {
        fun from(characteristic: BluetoothGattCharacteristic): BleCharacteristicInfo {
            return BleCharacteristicInfo(
                characteristic.uuid,
                characteristic.instanceId,
                BleServiceInfo(
                    characteristic.service.uuid,
                    characteristic.service.instanceId
                )
            )
        }
    }
}