package me.ycdev.android.ble.common.client

import android.content.Context

class BleGattClientSimple(context: Context) : BleGattClientBase(TAG, context) {

    companion object {
        private const val TAG = "BleGattClientSimple"
    }
}
