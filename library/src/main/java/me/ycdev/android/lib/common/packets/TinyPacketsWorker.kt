package me.ycdev.android.lib.common.packets

class TinyPacketsWorker(private val callback: PacketsWorker.ParserCallback) : PacketsWorker {
    override var maxPacketSize: Int = DEFAULT_MAX_PACKET_SIZE

    private var expectedSize: Int = -1
    private val buffer = ArrayList<Byte>()

    override fun packetData(data: ByteArray): List<ByteArray> {
        val payload = ByteArray(data.size + HEADER_SIZE)
        payload[0] = ((data.size ushr 8) and 0xff).toByte()
        payload[1] = (data.size and 0xff).toByte()
        data.copyInto(payload, HEADER_SIZE)

        val packetSize = maxOf(HEADER_SIZE + 1, maxPacketSize)
        return payload.asIterable().chunked(packetSize).map { it.toByteArray() }
    }

    override fun parsePackets(data: ByteArray) {
        data.forEach { buffer.add(it) }
        while (true) {
            if (expectedSize < 0) {
                if (buffer.size < HEADER_SIZE) {
                    return
                }
                expectedSize = ((buffer.removeAt(0).toInt() and 0xff) shl 8) or
                        (buffer.removeAt(0).toInt() and 0xff)
            }
            if (buffer.size < expectedSize) {
                return
            }
            val payload = ByteArray(expectedSize)
            for (i in 0 until expectedSize) {
                payload[i] = buffer.removeAt(0)
            }
            expectedSize = -1
            callback.onDataParsed(payload)
        }
    }

    companion object {
        private const val HEADER_SIZE = 2
        private const val DEFAULT_MAX_PACKET_SIZE = 20
    }
}
