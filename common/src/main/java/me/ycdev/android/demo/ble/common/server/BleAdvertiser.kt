package me.ycdev.android.demo.ble.common.server

interface BleAdvertiser {
    fun isAdvertising(): Boolean
    fun start(): Boolean
    fun stop()
}