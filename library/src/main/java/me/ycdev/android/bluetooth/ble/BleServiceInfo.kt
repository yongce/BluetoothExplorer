package me.ycdev.android.bluetooth.ble

import android.bluetooth.BluetoothGattService
import java.util.UUID

data class BleServiceInfo(
    val uuid: UUID,
    val instanceId: Int
) {
    companion object {
        fun from(service: BluetoothGattService): BleServiceInfo {
            return BleServiceInfo(
                service.uuid,
                service.instanceId
            )
        }
    }
}
