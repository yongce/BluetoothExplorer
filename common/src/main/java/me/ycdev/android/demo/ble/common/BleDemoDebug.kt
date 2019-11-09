package me.ycdev.android.demo.ble.common

import android.util.Log

object BleDemoDebug {
    var bleScan = false

    fun refreshDebugSwitches() {
        bleScan = Log.isLoggable("bd.bleScan", Log.DEBUG)
    }
}