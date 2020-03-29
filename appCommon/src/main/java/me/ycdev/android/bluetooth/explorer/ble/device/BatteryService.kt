package me.ycdev.android.bluetooth.explorer.ble.device

import android.bluetooth.le.ScanFilter
import android.os.ParcelUuid
import me.ycdev.android.bluetooth.ble.sig.BatteryServiceProfile

class BatteryService : BleDevice {
    override fun id(): String = ID

    override fun buildBleScanFilter(): List<ScanFilter> {
        val filters = ArrayList<ScanFilter>()
        filters.add(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(BatteryServiceProfile.BATTERY_SERVICE))
                .build()
        )
        return filters
    }

    companion object {
        const val ID = "BatteryService"
    }
}
