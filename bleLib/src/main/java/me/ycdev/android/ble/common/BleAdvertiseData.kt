package me.ycdev.android.ble.common

import android.util.SparseArray
import me.ycdev.android.lib.common.utils.EncodingUtils.encodeWithHex

class BleAdvertiseData(
    val payloadSize: Int,
    val flags: Int,
    val serviceUuids: List<String>,
    var localNameShort: String?,
    val deviceName: String?,
    val txPowerLevel: Int,
    val serviceData: Map<String, String>,
    val manufacturerData: SparseArray<String>
) {

    class Parser {
        var payloadSize: Int = 0
        var flags: Int = FLAGS_NONE
        var serviceUuids: ArrayList<String> = arrayListOf()
        var localNameShort: String? = null
        var deviceName: String? = null
        var txPowerLevel: Int = TX_POWER_LEVEL_NONE
        var serviceData: HashMap<String, String> = hashMapOf()
        var manufacturerData: SparseArray<String> = SparseArray()

        fun parse(data: ByteArray): BleAdvertiseData {
            var index = 0
            while (index < data.size) {
                val len: Int = data[index].toInt() and 0xFF
                if (len == 0) {
                    break // no more data
                }
                val type = data[index + 1]
                val fieldData = data.copyOfRange(index + 2, index + 2 + len - 1)
                processData(type, fieldData)

                index += len + 1
                payloadSize = index
            }

            return BleAdvertiseData(
                payloadSize,
                flags,
                serviceUuids,
                localNameShort,
                deviceName,
                txPowerLevel,
                serviceData,
                manufacturerData
            )
        }

        private fun processData(type: Byte, data: ByteArray) {
            when (type) {
                TYPE_FLAGS -> {
                    check(data.size == 1) {
                        "The data is too big for TYPE_FLAGS (size = ${data.size})."
                    }
                    flags = data[0].toInt()
                }

                TYPE_SERVICE_UUID_16_BIT -> {
                    data.reverse()
                    serviceUuids.add(encodeWithHex(data, false))
                }
                TYPE_LOCAL_NAME_SHORT -> localNameShort = String(data)
                TYPE_LOCAL_NAME_COMPLETE -> deviceName = String(data)

                TYPE_TX_POWER_LEVEL -> {
                    check(data.size == 1) {
                        "The data is too big for TYPE_TX_POWER_LEVEL (size = ${data.size})."
                    }
                    txPowerLevel = data[0].toInt()
                }

                TYPE_SERVICE_DATA_16_BIT -> {
                    check(data.size > 2) {
                        "The data is too small for TYPE_SERVICE_DATA_16_BIT (size = ${data.size})."
                    }
                    val oneUuid = encodeWithHex(data.copyOfRange(0, 2).apply { reverse() }, false)
                    val oneData = encodeWithHex(data.copyOfRange(2, data.size), false)
                    serviceData.put(oneUuid, oneData)
                }

                TYPE_MANUFACTURER_DATA -> {
                    check(data.size > 2) {
                        "The data is too small for TYPE_MANUFACTURER_DATA (size = ${data.size})."
                    }
                    val vendorId = data[0].toInt().and(0xFF) or
                            data[1].toInt().and(0xFF).shl(8)
                    val vendorData = data.copyOfRange(2, data.size)
                    manufacturerData.put(vendorId, encodeWithHex(vendorData, false))
                }
            }
        }

        companion object {
            const val TYPE_FLAGS: Byte = 0x01
            const val TYPE_SERVICE_UUID_16_BIT: Byte = 0x03
            const val TYPE_SERVICE_UUID_32_BIT: Byte = 0x05
            const val TYPE_SERVICE_UUID_128_BIT: Byte = 0x07
            const val TYPE_LOCAL_NAME_SHORT: Byte = 0x08
            const val TYPE_LOCAL_NAME_COMPLETE: Byte = 0x09
            const val TYPE_TX_POWER_LEVEL: Byte = 0x0A
            const val TYPE_SERVICE_DATA_16_BIT: Byte = 0x16
            const val TYPE_SERVICE_DATA_32_BIT: Byte = 0x20
            const val TYPE_SERVICE_DATA_128_BIT: Byte = 0x21
            const val TYPE_MANUFACTURER_DATA: Byte = 0xFF.toByte()
        }
    }

    companion object {
        const val FLAGS_NONE = -1

        /**
         * The max value defined by Android is 1.
         * Please refer "frameworks/base/core/java/android/bluetooth/le/AdvertisingSetParameters.java"
         */
        const val TX_POWER_LEVEL_NONE = 127 // the max value defined by Android is 1
    }
}
