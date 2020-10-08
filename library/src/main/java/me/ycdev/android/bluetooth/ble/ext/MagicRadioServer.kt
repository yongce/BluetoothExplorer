package me.ycdev.android.bluetooth.ble.ext

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.Looper
import android.os.ParcelUuid
import me.ycdev.android.bluetooth.ble.server.BleGattServerBase
import me.ycdev.android.lib.common.async.HandlerTaskExecutor
import me.ycdev.android.lib.common.async.TaskScheduler
import me.ycdev.android.lib.common.type.IntegerHolder
import timber.log.Timber

class MagicRadioServer(context: Context) : BleGattServerBase(
    TAG, context) {
    private val messageId = IntegerHolder(0)
    private val taskScheduler = TaskScheduler(Looper.getMainLooper(), TAG)
    private val taskExecutor = HandlerTaskExecutor.withMainLooper()

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

    override fun createBleServices(): List<BluetoothGattService> {
        return arrayListOf(MagicRadioProfile.createService())
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

    override fun start(resultCallback: ((Boolean) -> Unit)?) {
        super.start { success ->
            if (success) {
                taskScheduler.schedulePeriod(taskExecutor, 0, MESSAGES_INTERVAL) {
                    val fmOneData = getFmOneData()
                    val fmTwoData = getFmTwoData()

                    notifyRegisteredDevices {
                        Timber.tag(TAG).d("Publish: %s", String(fmOneData))
                        sendData(
                            it,
                            MagicPingProfile.PING_SERVICE,
                            MagicRadioProfile.FM_ONE_CHARACTERISTIC,
                            fmOneData
                        )
                        Timber.tag(TAG).d("Publish: %s", String(fmTwoData))
                        sendData(
                            it,
                            MagicRadioProfile.RADIO_SERVICE,
                            MagicRadioProfile.FM_TWO_CHARACTERISTIC,
                            fmTwoData
                        )
                    }
                }
            }
            if (resultCallback != null) {
                resultCallback(success)
            }
        }
    }

    override fun stop(resultCallback: (() -> Unit)?) {
        super.stop {
            taskScheduler.clear()
            if (resultCallback != null) {
                resultCallback()
            }
        }
    }

    companion object {
        private const val TAG = "MagicRadioServer"

        private const val MESSAGES_INTERVAL = 1000 * 30L // 30 seconds
    }
}
