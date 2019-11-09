package me.ycdev.android.bluetooth.explorer.ble.device

import android.bluetooth.le.ScanFilter
import android.os.ParcelUuid
import me.ycdev.android.bluetooth.explorer.ble.BleConstants

class MfpService : BleDevice {
    override fun id(): String = ID

    override fun buildBleScanFilter(): List<ScanFilter> {
        val filters = arrayListOf<ScanFilter>()
        val dataMask = byteArrayOf(0x00)
        filters.add(
            ScanFilter.Builder()
                .setServiceData(ParcelUuid(BleConstants.SERVICE_MFP), dataMask, dataMask)
                .build()
        )
        return filters
    }

    companion object {
        const val ID = "MfpService"
    }
}