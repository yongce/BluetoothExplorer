package me.ycdev.android.demo.ble.common.device

import android.bluetooth.le.ScanFilter
import android.os.ParcelUuid
import me.ycdev.android.demo.ble.common.BleDemoConstants

class MfpService : BleDevice {
    override fun id(): String = ID

    override fun buildBleScanFilter(): List<ScanFilter> {
        val filters = arrayListOf<ScanFilter>()
        val dataMask = byteArrayOf(0x00)
        filters.add(
            ScanFilter.Builder()
                .setServiceData(ParcelUuid(BleDemoConstants.SERVICE_MFP), dataMask, dataMask)
                .build()
        )
        return filters
    }

    companion object {
        const val ID = "MfpService";
    }
}