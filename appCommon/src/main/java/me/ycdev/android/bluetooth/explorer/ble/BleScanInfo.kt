package me.ycdev.android.bluetooth.explorer.ble

import android.bluetooth.le.ScanResult
import me.ycdev.android.lib.common.utils.EncodingUtils

data class BleScanInfo(val scanResult: ScanResult) {
    var deviceNumber: Int = 0
    val rawData: String = EncodingUtils.encodeWithHex(scanResult.scanRecord?.bytes)
}
