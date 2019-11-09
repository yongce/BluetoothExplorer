package me.ycdev.android.demo.ble.common.device

import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import me.ycdev.android.ble.common.BluetoothHelper
import me.ycdev.android.lib.common.utils.EncodingUtils.encodeWithHex
import timber.log.Timber

class Tp2 : BleDevice {
    override fun id(): String = ID

    override fun buildBleScanFilter(): List<ScanFilter> {
        val filters = ArrayList<ScanFilter>()
        filters.add(
            ScanFilter.Builder()
                .setManufacturerData(0xFDA7, ByteArray(0x00), ByteArray(0x00))
                .build()
        )
        return filters
    }

    override fun checkScanResult(scanResult: ScanResult, filter: String?): Boolean {
        if (filter.isNullOrEmpty()) {
            return true
        }

        val data = scanResult.scanRecord?.getManufacturerSpecificData(0xFDA7)
        if (data == null || data.size < 24) {
            Timber.tag(TAG).w("Unknown record found")
            return false
        }

        val left = encodeWithHex(data.copyOfRange(0, 6))
        val right = encodeWithHex(data.copyOfRange(6, 12))
        return left.endsWith(filter, true) || right.endsWith(filter, true)
    }

    override fun getReadableManufacturerData(id: Int, data: ByteArray): String {
        var formattedData = encodeWithHex(data)
        if (id == 0xFDA7) {
            val left = BluetoothHelper.addressStr(data.copyOfRange(0, 6))
            val right = BluetoothHelper.addressStr(data.copyOfRange(6, 12))
            formattedData += " (left=$left, right=$right)"
        }
        return formattedData
    }

    companion object {
        private const val TAG = "Tp2"

        const val ID = "Tp2"
    }
}