package me.ycdev.android.ble.common.sig

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import me.ycdev.android.ble.common.server.BleGattServerBase
import timber.log.Timber
import kotlin.random.Random

open class BatteryServiceServer(context: Context) : BleGattServerBase(TAG, context) {
    private lateinit var defaultBatteryService: BluetoothGattService
    private lateinit var boxBatteryService: BluetoothGattService

    private val random = Random(System.currentTimeMillis())

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
            .addServiceUuid(ParcelUuid(BatteryServiceProfile.BATTERY_SERVICE))
            .build()
    }

    override fun createBleServices(): List<BluetoothGattService> {
        defaultBatteryService = BatteryServiceProfile.createBatteryService()
        boxBatteryService = BatteryServiceProfile.createBatteryService()
        return arrayListOf(defaultBatteryService, boxBatteryService)
    }

    override fun onCharacteristicReadRequest(characteristic: BluetoothGattCharacteristic): ByteArray? {
        val instanceId = characteristic.service.instanceId
        val level = when (instanceId) {
            defaultBatteryService.instanceId -> (1 + random.nextInt(49)) * 2 // 2,4,..,100
            boxBatteryService.instanceId -> (1 + random.nextInt(49)) * 2 - 1 // 1,3,..,99
            else -> {
                Timber.tag(TAG).e("Unknown instance ID")
                0
            }
        }
        Timber.tag(TAG).d(
            "On read request, battery level [%d] serviceInstanceId[%d] characteristicInstanceId",
            level,
            instanceId, characteristic.instanceId
        )
        return byteArrayOf(level.toByte())
    }

    companion object {
        private const val TAG = "BatteryServiceServer"
    }
}