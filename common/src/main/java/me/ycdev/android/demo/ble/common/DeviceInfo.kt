package me.ycdev.android.demo.ble.common

import android.bluetooth.le.ScanResult

import java.util.Comparator

class DeviceInfo(var number: Int, var btAddress: String, var scanResult: ScanResult) {
    var foundCount: Int = 0

    init {
        this.foundCount = 1
    }

    class TimestampComparator : Comparator<DeviceInfo> {
        override fun compare(lhs: DeviceInfo, rhs: DeviceInfo): Int {
            if (lhs.scanResult.timestampNanos < rhs.scanResult.timestampNanos) {
                return 1
            } else if (lhs.scanResult.timestampNanos > rhs.scanResult.timestampNanos) {
                return -1
            }
            return 0
        }
    }
}
