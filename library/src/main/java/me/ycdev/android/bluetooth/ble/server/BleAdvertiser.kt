package me.ycdev.android.bluetooth.ble.server

import androidx.annotation.AnyThread

interface BleAdvertiser {
    @AnyThread
    fun isAdvertising(): Boolean
    @AnyThread
    fun start(resultCallback: ((Boolean) -> Unit)?)
    @AnyThread
    fun stop(resultCallback: (() -> Unit)?)
}