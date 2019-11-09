package me.ycdev.android.demo.ble.common

import android.bluetooth.le.ScanResult
import me.ycdev.android.lib.common.utils.EncodingUtils

data class BleScanInfo(val scanResult: ScanResult) {
    val rawData: String = EncodingUtils.encodeWithHex(scanResult.scanRecord?.bytes)
}