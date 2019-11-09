package me.ycdev.android.bluetooth.ble.client

import android.content.Context

class BleGattClientSimple(context: Context) : BleGattClientBase(
    TAG, context) {

    companion object {
        private const val TAG = "BleGattClientSimple"
    }
}
