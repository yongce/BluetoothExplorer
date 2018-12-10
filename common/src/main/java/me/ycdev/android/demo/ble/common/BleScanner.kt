package me.ycdev.android.demo.ble.common

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import androidx.annotation.MainThread
import androidx.annotation.NonNull
import me.ycdev.android.lib.common.perms.PermissionCallback
import me.ycdev.android.lib.common.perms.PermissionRequestParams
import me.ycdev.android.lib.common.perms.PermissionUtils
import me.ycdev.android.lib.common.utils.ApplicationUtils
import timber.log.Timber
import java.util.*

class BleScanner private constructor() {

    private val mAppContext: Context = ApplicationUtils.getApplicationContext()
    private var mLeScanner: BluetoothLeScanner? = null
    private val mScanCallback = MyScanCallback()
    var isScanning: Boolean = false
        private set
    private var mListener: ScanListener? = null

    private val mAllScanResults = HashMap<String, DeviceInfo>()
    private var mNextNumber = 1

    val devices: List<DeviceInfo>
        get() = ArrayList(mAllScanResults.values)

    interface ScanListener {
        fun onDeviceFound(@NonNull device: DeviceInfo, newDevice: Boolean)
    }

    @MainThread
    fun startScanning(listener: ScanListener): Boolean {
        if (mLeScanner == null) {
            if (!BluetoothHelper.supportBle(mAppContext)) {
                Timber.tag(TAG).d("This device doesn't support BLE")
                return false
            }
            val btAdapter = BluetoothHelper.getBluetoothAdapter(mAppContext)
            if (btAdapter == null) {
                Timber.tag(TAG).w("Failed to get BluetoothAdapter")
                return false
            }
            if (!btAdapter.isEnabled) {
                Timber.tag(TAG).w("Bluetooth not enabled")
                return false
            }

            mLeScanner = btAdapter.bluetoothLeScanner
            if (mLeScanner == null) {
                Timber.tag(TAG).w("Failed to get BLE scanner")
                return false
            }
        }

        if (hasNoPermissions(mAppContext)) {
            Timber.tag(TAG).w("No location permissions")
            return false
        }

        Timber.tag(TAG).d("Start scanning")
        return if (!isScanning) {
            mAllScanResults.clear()
            mLeScanner!!.startScan(buildScanFilters(), buildScanSettings(), mScanCallback)
            isScanning = true
            mListener = listener
            mNextNumber = 1
            true
        } else {
            Timber.tag(TAG).w("Scanning was already started")
            false
        }
    }

    fun stopScanning() {
        Timber.tag(TAG).d("Stop scanning")
        if (isScanning) {
            mLeScanner!!.stopScan(mScanCallback)
            mListener = null
            isScanning = false
        }
    }

    private fun buildScanSettings(): ScanSettings {
        return ScanSettings.Builder().build()
    }

    private fun buildScanFilters(): List<ScanFilter> {
//        filters.add(new ScanFilter.Builder()
        //                .setDeviceAddress("41:B0:87:14:EB:B1")
        //                .build());
        //        filters.add(new ScanFilter.Builder()
        //                .setDeviceName("iPhone 5s")
        //                .build());
        //        filters.add(new ScanFilter.Builder()
        //                .setServiceUuid(ParcelUuid.fromString("735dc4fa-348e-11e7-a919-92ebcb67fe33"))
        //                .build());
        return ArrayList()
    }

    private fun onDeviceFound(@NonNull result: ScanResult, tag: String) {
        val btAddress = result.device.address
        var item: DeviceInfo? = mAllScanResults[btAddress]
        val newDevice = item == null
        if (!newDevice) {
            item!!.scanResult = result
            item.foundCount++
        } else {
            item = DeviceInfo(mNextNumber++, btAddress, result)
            mAllScanResults[btAddress] = item
        }
        logScanResult(result, tag)
        Timber.tag(TAG).d("Total scan results: %d", mAllScanResults.size)
        if (mListener != null) {
            mListener!!.onDeviceFound(item, newDevice)
        }
    }

    private fun logScanResult(@NonNull result: ScanResult, tag: String) {
        val device = result.device
        Timber.tag(TAG).d(
            "[%s] rssi: %s, address: %s, name: [%s], bondState: %d, type: %d",
            tag, result.rssi,
            device.address, device.name, device.bondState, device.type
        )
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

    companion object {
        private const val TAG = "BleScanner"

        private val BLE_SCAN_PERMS = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)

        @SuppressLint("StaticFieldLeak")
        val instance = BleScanner()

        fun hasNoPermissions(@NonNull context: Context): Boolean {
            return !PermissionUtils.hasPermissions(context, *BLE_SCAN_PERMS)
        }

        fun requestPermsForBleScan(
            @NonNull activity: Activity, requestCode: Int,
            @NonNull callback: PermissionCallback
        ) {
            Timber.tag(TAG).d("Request perms for BLE scan")
            val params = PermissionRequestParams()
            params.requestCode = requestCode
            params.permissions = BLE_SCAN_PERMS
            params.rationaleContent = activity.getString(R.string.ble_scan_perm_rationale)
            params.callback = callback
            PermissionUtils.requestPermissions(activity, params)
        }
    }
}
