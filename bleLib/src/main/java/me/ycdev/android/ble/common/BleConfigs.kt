package me.ycdev.android.ble.common

import android.os.Handler
import android.os.HandlerThread

object BleConfigs {
    var bleOperationLog = false
    var bleDataLog = false
    var bleScanLog = false

    internal val bleHandler: Handler by lazy {
        val thread = HandlerThread("BleOp")
        thread.start()
        Handler(thread.looper)
    }

    fun enableAllLogs(enable: Boolean) {
        bleOperationLog = enable
        bleDataLog = enable
        bleScanLog = enable
    }
}
