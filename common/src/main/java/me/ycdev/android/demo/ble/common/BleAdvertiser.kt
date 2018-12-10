package me.ycdev.android.demo.ble.common

import android.annotation.SuppressLint
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import me.ycdev.android.lib.common.utils.ApplicationUtils
import timber.log.Timber

class BleAdvertiser private constructor() {

    private val mAppContext: Context = ApplicationUtils.getApplicationContext()
    private var mAdvertiser: BluetoothLeAdvertiser? = null
    private val mAdvertiseCallback = MyAdvertiseCallback()
    var isAdvertising: Boolean = false
        private set

    fun startAdvertising(serviceUuid: String): Boolean {
        if (mAdvertiser == null) {
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

            mAdvertiser = btAdapter.bluetoothLeAdvertiser
            if (mAdvertiser == null) {
                Timber.tag(TAG).w("Failed to get BLE advertiser")
                return false
            }
        }

        Timber.tag(TAG).d("Start advertising: %s", serviceUuid)
        return if (!isAdvertising) {
            mAdvertiser!!.startAdvertising(
                buildAdvertiseSettings(),
                buildAdvertiseData(serviceUuid), mAdvertiseCallback
            )
            isAdvertising = true
            true
        } else {
            Timber.tag(TAG).w("Advertising was already started")
            false
        }
    }

    fun stopAdvertising() {
        Timber.tag(TAG).d("Stop advertising")
        if (isAdvertising) {
            mAdvertiser!!.stopAdvertising(mAdvertiseCallback)
            isAdvertising = false
        }
    }

    private fun buildAdvertiseSettings(): AdvertiseSettings {
        return AdvertiseSettings.Builder()
            .setTimeout(0)
            .build()
    }

    private fun buildAdvertiseData(serviceUuid: String): AdvertiseData {
        return AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid.fromString(serviceUuid))
            .setIncludeDeviceName(true)
            .build()
    }

    private inner class MyAdvertiseCallback : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            Timber.tag(TAG).w("Advertising failed: %d", errorCode)
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Timber.tag(TAG).d("Advertising is started successfully")
        }
    }

    companion object {
        private const val TAG = "BleAdvertiser"

        @SuppressLint("StaticFieldLeak")
        val instance: BleAdvertiser? = null
    }
}
