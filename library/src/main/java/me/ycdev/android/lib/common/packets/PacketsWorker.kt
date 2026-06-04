package me.ycdev.android.lib.common.packets

interface PacketsWorker {
    var maxPacketSize: Int

    fun packetData(data: ByteArray): List<ByteArray>

    fun parsePackets(data: ByteArray)

    interface ParserCallback {
        fun onDataParsed(data: ByteArray)
    }
}
