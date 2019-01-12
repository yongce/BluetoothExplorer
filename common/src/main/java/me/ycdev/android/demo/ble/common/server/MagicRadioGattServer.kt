package me.ycdev.android.demo.ble.common.server

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import me.ycdev.android.lib.common.async.HandlerExecutor
import me.ycdev.android.lib.common.async.TaskScheduler
import me.ycdev.android.lib.common.type.IntegerHolder
import timber.log.Timber

class MagicRadioGattServer(context: Context) : BleGattServerBase(TAG, context) {
    private val messageId = IntegerHolder(0)
    private val taskScheduler = TaskScheduler(HandlerExecutor.withMainLooper(), TAG)

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
            .addServiceUuid(ParcelUuid(MagicRadioProfile.RADIO_SERVICE))
            .build()
    }

    override fun addBleServices(gattServer: BluetoothGattServer): Boolean {
        return gattServer.addService(MagicRadioProfile.createService())
    }

    override fun notifyAllConnectedDevices(): Boolean {
        return true
    }

    private fun getFmOneData(): ByteArray {
        messageId.value++
        return "FM one: message#${messageId.value}".toByteArray()
    }

    private fun getFmTwoData(): ByteArray {
        messageId.value++
        return "FM two: message#${messageId.value}".toByteArray()
    }

    override fun onCharacteristicReadRequest(characteristic: BluetoothGattCharacteristic): ByteArray? {
        return when (characteristic.uuid) {
            MagicRadioProfile.FM_ONE_CHARACTERISTIC -> getFmOneData()
            MagicRadioProfile.FM_TWO_CHARACTERISTIC -> getFmTwoData()
            else -> null
        }
    }

    override fun onStart() {
        taskScheduler.schedulePeriod({
            val fmOne = gattServer?.getService(MagicRadioProfile.RADIO_SERVICE)
                ?.getCharacteristic(MagicRadioProfile.FM_ONE_CHARACTERISTIC)
            fmOne?.value = getFmOneData()

            val fmTwo = gattServer?.getService(MagicRadioProfile.RADIO_SERVICE)
                ?.getCharacteristic(MagicRadioProfile.FM_TWO_CHARACTERISTIC)
            fmTwo?.value = getFmTwoData()

            notifyRegisteredDevices {
                if (fmOne != null) {
                    Timber.tag(TAG).d("Publish: %s", String(fmOne.value))
                    gattServer?.notifyCharacteristicChanged(it, fmOne, false)
                }
                if (fmTwo != null) {
                    Timber.tag(TAG).d("Publish: %s", String(fmTwo.value))
                    gattServer?.notifyCharacteristicChanged(it, fmTwo, false)
                }
            }
        }, 0, MESSAGES_INTERVAL)
    }

    override fun onStop() {
        taskScheduler.clear()
    }

    companion object {
        private const val TAG = "MagicRadioGattServer"

        private const val MESSAGES_INTERVAL = 1000 * 30L // 30 seconds
    }
}
