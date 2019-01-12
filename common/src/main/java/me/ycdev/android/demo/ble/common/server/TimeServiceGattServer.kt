package me.ycdev.android.demo.ble.common.server

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.ParcelUuid

class TimeServiceGattServer(context: Context) : BleGattServerBase(TAG, context.applicationContext) {
    /**
     * Listens for system clock events and triggers a notification to subscribers.
     */
    private val timeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val adjustReason = when (intent.action) {
                Intent.ACTION_TIME_CHANGED -> TimeServiceProfile.ADJUST_MANUAL
                Intent.ACTION_TIMEZONE_CHANGED -> TimeServiceProfile.ADJUST_TIMEZONE
                Intent.ACTION_TIME_TICK -> TimeServiceProfile.ADJUST_NONE
                else -> TimeServiceProfile.ADJUST_NONE
            }
            val now = System.currentTimeMillis()
            notifyTimeChange(now, adjustReason)
        }
    }

    override fun getClientConfigDescriptorUuid() = TimeServiceProfile.CLIENT_CONFIG

    override fun addBleServices(gattServer: BluetoothGattServer): Boolean {
        return gattServer.addService(TimeServiceProfile.createTimeService())
    }

    override fun buildAdvertiseSettings(): AdvertiseSettings {
        return AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .build()
    }

    override fun buildAdvertiseData(): AdvertiseData {
        return AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .setIncludeTxPowerLevel(false)
            .addServiceUuid(ParcelUuid(TimeServiceProfile.TIME_SERVICE))
            .build()
    }

    override fun onStart() {
        // Register for system clock events
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        appContext.registerReceiver(timeReceiver, filter)
    }

    override fun onStop() {
        appContext.unregisterReceiver(timeReceiver)
    }

    override fun onCharacteristicReadRequest(characteristic: BluetoothGattCharacteristic): ByteArray? {
        return TimeServiceProfile.getExactTime(System.currentTimeMillis(), TimeServiceProfile.ADJUST_NONE)
    }

    private fun notifyTimeChange(time: Long, adjustReason: Byte) {
        val exactTime = TimeServiceProfile.getExactTime(time, adjustReason)
        val timeCharacteristic = gattServer
            ?.getService(TimeServiceProfile.TIME_SERVICE)
            ?.getCharacteristic(TimeServiceProfile.CURRENT_TIME)
        timeCharacteristic?.value = exactTime
        notifyRegisteredDevices { gattServer?.notifyCharacteristicChanged(it, timeCharacteristic, false) }
    }

    companion object {
        private const val TAG = "TimeServiceGattServer"
    }
}