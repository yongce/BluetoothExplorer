package me.ycdev.android.bluetooth.ble.client

import android.annotation.SuppressLint
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import androidx.annotation.MainThread
import me.ycdev.android.bluetooth.BluetoothHelper
import me.ycdev.android.bluetooth.ble.BleConfigs
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

    private var nextNumber = 1

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
    @SuppressLint("MissingPermission")
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
            Timber.tag(TAG).w("No BLE scan permissions")
            return false
        }

        Timber.tag(TAG).d("Start scanning")
        return if (!isScanning) {
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
    @SuppressLint("MissingPermission")
    fun stopScanning() {
        Timber.tag(TAG).d("Stop scanning")
        if (isScanning) {
            if (BluetoothHelper.hasBluetoothScanPermission(appContext)) {
                try {
                    leScanner!!.stopScan(scanCallback)
                } catch (e: SecurityException) {
                    Timber.tag(TAG).w(e, "No permission to stop BLE scanning")
                }
            }
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
        if (onlyNamedDevices && getDeviceName(result) == null) {
            return // skip "UNKNOWN" devices
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
                device.address, getDeviceName(result), getBondState(result), getDeviceType(result)
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceName(result: ScanResult): String? {
        result.scanRecord?.deviceName?.let { return it }
        if (!BluetoothHelper.hasBluetoothConnectPermission(appContext)) {
            return null
        }
        return try {
            result.device.name
        } catch (e: SecurityException) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    private fun getBondState(result: ScanResult): Int {
        if (!BluetoothHelper.hasBluetoothConnectPermission(appContext)) {
            return 0
        }
        return try {
            result.device.bondState
        } catch (e: SecurityException) {
            0
        }
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceType(result: ScanResult): Int {
        if (!BluetoothHelper.hasBluetoothConnectPermission(appContext)) {
            return 0
        }
        return try {
            result.device.type
        } catch (e: SecurityException) {
            0
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
            Timber.tag(TAG).w("onScanFailed: %s", BluetoothHelper.bleScanErrorStr(errorCode))
        }
    }

    interface ScanListener {
        fun onDeviceFound(result: ScanResult)
    }

    companion object {
        private const val TAG = "BleScanner"

        val BLE_SCAN_PERMS: Array<String>
            get() = BluetoothHelper.bleScanPermissions()

        fun hasNoPermissions(context: Context): Boolean {
            return !BluetoothHelper.hasBluetoothScanPermission(context)
        }
    }
}
