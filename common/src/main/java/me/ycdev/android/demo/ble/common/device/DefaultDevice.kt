package me.ycdev.android.demo.ble.common.device

import android.bluetooth.le.ScanFilter

class DefaultDevice : BleDevice {
    override fun id(): String = ID

    override fun buildBleScanFilter(): List<ScanFilter>? = null

    companion object {
        const val ID = "DefaultDevice"
    }
}