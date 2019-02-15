package me.ycdev.android.ble.common.client

import android.content.Context
import me.ycdev.android.lib.common.packets.PacketsWorker
import me.ycdev.android.lib.common.packets.RawPacketsWorker

class BleGattClientSimple(context: Context) : BleGattClientBase(TAG, context), PacketsWorker.ParserCallback {
    private val packetsWorker = RawPacketsWorker(this)

    override fun getPacketsWorker(): PacketsWorker = packetsWorker

    override fun onDataParsed(data: ByteArray) {
        TODO("not implemented")
    }

    companion object {
        private const val TAG = "BleGattClientSimple"
    }
}
