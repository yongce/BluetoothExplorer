package me.ycdev.android.ble.common.sig

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.ParcelUuid
import me.ycdev.android.ble.common.server.BleGattServerBase

open class TimeServiceServer(context: Context) : BleGattServerBase(TAG, context) {
    private var receiverRegisterred = false

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

    override fun getClientConfigDescriptorUuid() =
        TimeServiceProfile.CLIENT_CONFIG

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

    override fun createBleServices(): List<BluetoothGattService> {
        return arrayListOf(TimeServiceProfile.createTimeService())
    }

    override fun onCharacteristicReadRequest(characteristic: BluetoothGattCharacteristic): ByteArray? {
        return TimeServiceProfile.getExactTime(
            System.currentTimeMillis(),
            TimeServiceProfile.ADJUST_NONE
        )
    }

    override fun start(resultCallback: ((Boolean) -> Unit)?) {
        super.start { success ->
            if (success) {
                // Register for system clock events
                val filter = IntentFilter().apply {
                    addAction(Intent.ACTION_TIME_TICK)
                    addAction(Intent.ACTION_TIME_CHANGED)
                    addAction(Intent.ACTION_TIMEZONE_CHANGED)
                }
                context.registerReceiver(timeReceiver, filter)
                receiverRegisterred = true
            }
            if (resultCallback != null) {
                resultCallback(success)
            }
        }
    }

    override fun stop(resultCallback: (() -> Unit)?) {
        super.stop {
            if (receiverRegisterred) {
                context.unregisterReceiver(timeReceiver)
                receiverRegisterred = false
            }
            if (resultCallback != null) {
                resultCallback()
            }
        }
    }

    private fun notifyTimeChange(time: Long, adjustReason: Byte) {
        val exactTime = TimeServiceProfile.getExactTime(time, adjustReason)
        notifyRegisteredDevices {
            sendData(
                it,
                TimeServiceProfile.TIME_SERVICE,
                TimeServiceProfile.CURRENT_TIME,
                exactTime
            )
        }
    }

    companion object {
        private const val TAG = "TimeServiceServer"
    }
}