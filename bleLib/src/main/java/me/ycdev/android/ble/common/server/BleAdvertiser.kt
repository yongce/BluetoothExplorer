package me.ycdev.android.ble.common.server

interface BleAdvertiser {
    fun isAdvertising(): Boolean
    fun start(): Boolean
    fun stop()
}