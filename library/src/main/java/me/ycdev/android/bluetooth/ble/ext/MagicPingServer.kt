package me.ycdev.android.bluetooth.ble.ext

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import me.ycdev.android.bluetooth.ble.BleCharacteristicInfo
import me.ycdev.android.bluetooth.ble.server.BleGattServerBase
import me.ycdev.android.lib.common.packets.PacketsWorker
import me.ycdev.android.lib.common.packets.TinyPacketsWorker
import timber.log.Timber

class MagicPingServer(context: Context) : BleGattServerBase(TAG, context) {
    private val packetsWorkerMapping = hashMapOf<BluetoothDevice, PacketsWorker>()

    init {
        setOperationTimeout(MagicPingProfile.BLE_OPERATION_TIMEOUT)
    }

    override fun buildAdvertiseSettings(): AdvertiseSettings {
        return AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setConnectable(true)
            .setTimeout(0)
            .build()
    }

    override fun buildAdvertiseData(): AdvertiseData {
        return AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .setIncludeTxPowerLevel(false)
            .addServiceUuid(ParcelUuid(MagicPingProfile.PING_SERVICE))
            .build()
    }

    override fun createBleServices(): List<BluetoothGattService> {
        return arrayListOf(MagicPingProfile.createService())
    }

    private fun getPacketsWorker(device: BluetoothDevice): PacketsWorker {
        var packetsWorker = packetsWorkerMapping[device]
        if (packetsWorker == null) {
            packetsWorker = TinyPacketsWorker(MyParserCallback(device))
            packetsWorkerMapping[device] = packetsWorker
        }
        return packetsWorker
    }

    override fun onIncomingData(
        device: BluetoothDevice,
        characteristic: BleCharacteristicInfo,
        value: ByteArray
    ) {
        super.onIncomingData(device, characteristic, value)
        getPacketsWorker(device).parsePackets(value)
    }

    override fun packetDataForSend(device: BluetoothDevice, mtu: Int, data: ByteArray): List<ByteArray> {
        val packetsWorker = getPacketsWorker(device)
        packetsWorker.maxPacketSize = mtu
        return packetsWorker.packetData(data)
    }

    inner class MyParserCallback(val device: BluetoothDevice) : PacketsWorker.ParserCallback {
        override fun onDataParsed(data: ByteArray) {
            val pingMessage = String(data)
            Timber.tag(TAG).d("Received data[%s] from [%s]", pingMessage, device)
            val ackMessage = "ACK{$pingMessage}"
            sendData(
                device,
                MagicPingProfile.PING_SERVICE,
                MagicPingProfile.PING_CHARACTERISTIC,
                ackMessage.toByteArray()
            )
        }
    }

    companion object {
        private const val TAG = "MagicPingServer"
    }
}
