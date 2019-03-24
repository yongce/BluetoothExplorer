package me.ycdev.android.ble.common.sig

import android.bluetooth.BluetoothGatt
import android.content.Context
import androidx.annotation.MainThread
import me.ycdev.android.ble.common.BleCharacteristicInfo
import me.ycdev.android.ble.common.client.BleGattClientBase
import me.ycdev.android.lib.common.utils.MainHandler
import timber.log.Timber

class BatteryServiceClient(context: Context) : BleGattClientBase(TAG, context) {
    private var readCallback: Callback? = null

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BleCharacteristicInfo,
        data: ByteArray
    ) {
        super.onCharacteristicChanged(gatt, characteristic, data)
        val instanceId = characteristic.serviceInfo.instanceId
        val batteryLevel = data[0].toInt() and 0xFF
        Timber.tag(TAG).d("Battery level [$batteryLevel] for serviceInstance[$instanceId]")
        val callback = readCallback
        if (callback != null) {
            MainHandler.post { callback.onBatteryLevelChanged(instanceId, batteryLevel) }
        }
    }

    fun readBatteryLevel(callback: Callback?) {
        readCallback = callback
        readData(
            BatteryServiceProfile.BATTERY_SERVICE,
            BatteryServiceProfile.BATTERY_LEVEL_CHARACTERISTIC
        )
    }

    interface Callback {
        @MainThread
        fun onBatteryLevelChanged(instanceId: Int, level: Int)
    }

    companion object {
        private const val TAG = "BatteryServiceClient"
    }
}