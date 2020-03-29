package me.ycdev.android.bluetooth.explorer.ble.device

import android.bluetooth.le.ScanFilter
import android.os.ParcelUuid
import me.ycdev.android.bluetooth.explorer.ble.BleConstants

class GfpService : BleDevice {
    override fun id(): String = ID

    override fun buildBleScanFilter(): List<ScanFilter> {
        val filters = arrayListOf<ScanFilter>()
        filters.add(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(BleConstants.SERVICE_GFP))
                .build()
        )
        return filters
    }

    companion object {
        const val ID = "GfpService"
    }
}
