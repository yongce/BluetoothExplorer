package me.ycdev.android.demo.ble.common.device

import android.bluetooth.le.ScanFilter
import android.os.ParcelUuid
import me.ycdev.android.demo.ble.common.BleDemoConstants

class MagicWw : BleDevice {
    override fun id(): String = ID

    override fun buildBleScanFilter(): List<ScanFilter> {
        val filters = ArrayList<ScanFilter>()
        filters.add(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(BleDemoConstants.SERVICE_MAGIC_WW))
                .build()
        )
        return filters
    }

    companion object {
        const val ID = "MagicWw";
    }
}