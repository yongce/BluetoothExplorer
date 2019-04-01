package me.ycdev.android.ble.common.client

import android.Manifest
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import androidx.annotation.MainThread
import me.ycdev.android.ble.common.BleConfigs
import me.ycdev.android.ble.common.BluetoothHelper
import me.ycdev.android.lib.common.perms.PermissionUtils
import me.ycdev.android.lib.common.utils.MainHandler
import timber.log.Timber

class BleScanner(context: Context) {
    private val appContext: Context = context.applicationContext
    private var leScanner: BluetoothLeScanner? = null
    private val scanCallback = MyScanCallback()
    var isScanning: Boolean = false
        private set
    private var scanSetting: ScanSettings? = null
    private var scanFilters: List<ScanFilter>? = null
    private var listener: ScanListener? = null
    private var onlyNamedDevices = false

    private val allScanResults = HashMap<String, ScanResult>()
    private var nextNumber = 1

    val devices: List<ScanResult>
        get() = ArrayList(allScanResults.values)

    fun setScanSettings(scanSettings: ScanSettings) {
        scanSetting = scanSettings
    }

    fun setScanFilters(scanFilters: List<ScanFilter>?) {
        this.scanFilters = scanFilters
    }

    fun setOnlyNamedDevices(onlyNamedDevices: Boolean) {
        this.onlyNamedDevices = onlyNamedDevices
    }

    @MainThread
    fun startScanning(listener: ScanListener): Boolean {
        if (leScanner == null) {
            if (!BluetoothHelper.canDoBleOperations(appContext)) {
                return false
            }

            leScanner = BluetoothHelper.getBluetoothAdapter(
                appContext
            )?.bluetoothLeScanner
            if (leScanner == null) {
                Timber.tag(TAG).w("Failed to get BLE scanner")
                return false
            }
        }

        if (hasNoPermissions(appContext)) {
            Timber.tag(TAG).w("No location permissions")
            return false
        }

        Timber.tag(TAG).d("Start scanning")
        return if (!isScanning) {
            allScanResults.clear()
            Timber.tag(TAG).d("Scan filters: %s", scanFilters)
            Timber.tag(TAG).d("Scan settings: %s", scanSetting)
            leScanner!!.startScan(scanFilters, buildScanSettings(), scanCallback)
            isScanning = true
            this.listener = listener
            nextNumber = 1
            true
        } else {
            Timber.tag(TAG).w("Scanning was already started")
            false
        }
    }

    @MainThread
    fun stopScanning() {
        Timber.tag(TAG).d("Stop scanning")
        if (isScanning) {
            leScanner!!.stopScan(scanCallback)
            listener = null
            isScanning = false
        }
    }

    private fun buildScanSettings(): ScanSettings {
        if (scanSetting != null) {
            return scanSetting as ScanSettings
        }
        return ScanSettings.Builder().build()
    }

    private fun onDeviceFound(result: ScanResult, tag: String) {
        logScanResult(result, tag)
        if (onlyNamedDevices && result.device.name == null) {
            return // skip "UNKNOWN" devices
        }

        val btAddress = result.device.address
        allScanResults[btAddress] = result
        if (BleConfigs.bleScanLog) {
            Timber.tag(TAG).d("Total scan results: %d", allScanResults.size)
        }
        MainHandler.post {
            if (listener != null) {
                listener!!.onDeviceFound(result)
            }
        }
    }

    private fun logScanResult(result: ScanResult, tag: String) {
        val device = result.device
        if (BleConfigs.bleScanLog) {
            Timber.tag(TAG).d(
                "[%s] rssi: %s, address: %s, name: [%s], bondState: %d, type: %d",
                tag, result.rssi,
                device.address, device.name, device.bondState, device.type
            )
        }
        val uuids = device.uuids
        if (uuids != null && uuids.isNotEmpty()) {
            for (uuid in uuids) {
                Timber.tag(TAG).d("UUID: %s", uuid.uuid)
            }
        }
    }

    private inner class MyScanCallback : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            when (callbackType) {
                ScanSettings.CALLBACK_TYPE_FIRST_MATCH -> onDeviceFound(result, "first match")
                ScanSettings.CALLBACK_TYPE_MATCH_LOST -> logScanResult(result, "match lost")
                ScanSettings.CALLBACK_TYPE_ALL_MATCHES -> onDeviceFound(result, "all match")
                else -> Timber.tag(TAG).w("unknown callbackType: %d", callbackType)
            }
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            Timber.tag(TAG).d("onBatchScanResults: %d", results.size)
        }

        override fun onScanFailed(errorCode: Int) {
            Timber.tag(TAG).w("onScanFailed: %d", errorCode)
        }
    }

    interface ScanListener {
        fun onDeviceFound(result: ScanResult)
    }

    companion object {
        private const val TAG = "BleScanner"

        val BLE_SCAN_PERMS = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)

        fun hasNoPermissions(context: Context): Boolean {
            return !PermissionUtils.hasPermissions(context, *BLE_SCAN_PERMS)
        }
    }
}
