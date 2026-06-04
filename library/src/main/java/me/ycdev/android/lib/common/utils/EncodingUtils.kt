package me.ycdev.android.lib.common.utils

object EncodingUtils {
    fun encodeWithHex(data: ByteArray?): String {
        return encodeWithHex(data, false)
    }

    fun encodeWithHex(data: ByteArray?, withPrefix: Boolean): String {
        if (data == null) {
            return ""
        }
        val text = data.joinToString(separator = "") { "%02x".format(it) }
        return if (withPrefix) "0x$text" else text
    }

    fun fromHexString(hex: String): ByteArray {
        val cleanHex = hex.replace("\\s".toRegex(), "")
        require(cleanHex.length % 2 == 0) { "Bad hex string length: ${cleanHex.length}" }
        return ByteArray(cleanHex.length / 2) { index ->
            cleanHex.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }
}
