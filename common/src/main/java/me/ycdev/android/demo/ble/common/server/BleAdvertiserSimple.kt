package me.ycdev.android.demo.ble.common.server

import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import androidx.annotation.MainThread
import me.ycdev.android.demo.ble.common.BluetoothHelper
import timber.log.Timber

class BleAdvertiserSimple(context: Context) : BleAdvertiser {
    private val appContext: Context = context.applicationContext
    private var advertiser: BluetoothLeAdvertiser? = null
    private val realCallback = MyAdvertiseCallback()
    private var advertsing: Boolean = false

    private var callerSettings: AdvertiseSettings? = null
    private var callerData: AdvertiseData? = null
    private var callerCallback: AdvertiseCallback? = null

    override fun isAdvertising(): Boolean {
        return advertsing
    }

    fun setSettings(settings: AdvertiseSettings): BleAdvertiserSimple {
        callerSettings = settings
        return this
    }

    fun setData(data: AdvertiseData): BleAdvertiserSimple {
        callerData = data
        return this
    }

    fun setCallback(callback: AdvertiseCallback?): BleAdvertiserSimple {
        callerCallback = callback
        return this
    }

    @MainThread
    override fun start(): Boolean {
        if (advertiser == null) {
            if (!BluetoothHelper.canDoBleOperations(appContext)) {
                return false
            }

            advertiser = BluetoothHelper.getBluetoothAdapter(appContext)?.bluetoothLeAdvertiser
            if (advertiser == null) {
                Timber.tag(TAG).w("Failed to get BLE advertiser")
                return false
            }
        }

        Timber.tag(TAG).d("Start advertising: %s", callerData)
        return if (!advertsing) {
            advertiser!!.startAdvertising(callerSettings, callerData, realCallback)
            advertsing = true
            true
        } else {
            Timber.tag(TAG).w("Advertising was already started")
            false
        }
    }

    @MainThread
    override fun stop() {
        Timber.tag(TAG).d("Stop advertising")
        if (advertsing) {
            advertiser!!.stopAdvertising(realCallback)
            callerCallback = null
            advertsing = false
        } else {
            Timber.tag(TAG).w("No advertising started")
        }
    }

    private inner class MyAdvertiseCallback : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            Timber.tag(TAG).w("Advertising failed: %s", errorCodeStr(errorCode))
            advertsing = false
            callerCallback?.onStartFailure(errorCode)
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Timber.tag(TAG).d("Advertising is started successfully")
            callerCallback?.onStartSuccess(settingsInEffect)
        }
    }

    companion object {
        private const val TAG = "BleAdvertiserSimple"

        fun errorCodeStr(errorCode: Int): String {
            return when (errorCode) {
                AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "ALREADY_STARTED"
                AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "DATA_TOO_LARGE"
                AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "FEATURE_UNSUPPORTED"
                AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "INTERNAL_ERROR"
                AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "TOO_MANY_ADVERTISERS"
                else -> "UNKNOWN_ERROR"
            }
        }
    }
}
