package me.ycdev.android.bluetooth.explorer

import android.util.Log

object AppDebug {
    var bleScan = false

    fun refreshDebugSwitches() {
        bleScan = Log.isLoggable("bd.bleScan", Log.DEBUG)
    }
}