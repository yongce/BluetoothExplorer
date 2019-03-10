package me.ycdev.android.ble.common.ext

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import me.ycdev.android.ble.common.server.BleGattServerBase
import me.ycdev.android.lib.common.packets.PacketsWorker
import me.ycdev.android.lib.common.packets.TinyPacketsWorker
import timber.log.Timber
import java.util.UUID

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

    override fun addBleServices(gattServer: BluetoothGattServer): Boolean {
        return gattServer.addService(MagicPingProfile.createService())
    }

    override fun getPacketsWorker(
        device: BluetoothDevice,
        characteristicUuid: UUID
    ): PacketsWorker {
        var packetsWorker = packetsWorkerMapping[device]
        if (packetsWorker == null) {
            packetsWorker = TinyPacketsWorker(MyParserCallback(device))
            packetsWorkerMapping[device] = packetsWorker
        }
        return packetsWorker
    }

    override fun onCharacteristicReadRequest(characteristic: BluetoothGattCharacteristic): ByteArray? {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun onIncomingData(
        device: BluetoothDevice,
        characteristicUuid: UUID,
        value: ByteArray
    ) {
        super.onIncomingData(device, characteristicUuid, value)
        getPacketsWorker(device, characteristicUuid).parsePackets(value)
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