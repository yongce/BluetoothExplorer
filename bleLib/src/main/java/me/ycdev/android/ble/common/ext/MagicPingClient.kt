package me.ycdev.android.ble.common.ext

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattService
import android.content.Context
import me.ycdev.android.ble.common.BleCharacteristicInfo
import me.ycdev.android.ble.common.client.BleGattClientBase
import me.ycdev.android.ble.common.client.ClientState
import me.ycdev.android.ble.common.client.ClientState.DISCONNECTED
import me.ycdev.android.lib.common.async.HandlerExecutor
import me.ycdev.android.lib.common.async.TaskScheduler
import me.ycdev.android.lib.common.packets.PacketsWorker
import me.ycdev.android.lib.common.packets.TinyPacketsWorker
import me.ycdev.android.lib.common.type.IntegerHolder
import timber.log.Timber

class MagicPingClient(context: Context) : BleGattClientBase(TAG, context), PacketsWorker.ParserCallback {
    private val packetsWorker = TinyPacketsWorker(this)
    private val messageId = IntegerHolder(0)
    private val taskScheduler = TaskScheduler(HandlerExecutor.withMainLooper(), TAG)

    init {
        setOperationTimeout(MagicPingProfile.BLE_OPERATION_TIMEOUT)
    }

    override fun onStateChanged(device: BluetoothDevice, newState: ClientState) {
        super.onStateChanged(device, newState)
        if (newState == DISCONNECTED) {
            taskScheduler.clear()
        }
    }

    override fun onServicesDiscovered(
        device: BluetoothDevice,
        services: List<BluetoothGattService>
    ) {
        super.onServicesDiscovered(device, services)
        Timber.tag(TAG).d("onServicesDiscovered")
        val pingService = services.find { it.uuid == MagicPingProfile.PING_SERVICE }
        if (pingService != null) {
            val pingCharacteristic = pingService.getCharacteristic(MagicPingProfile.PING_CHARACTERISTIC)
            if (pingCharacteristic != null) {
                requestMtu(MTU_REQUEST)
                listen(pingCharacteristic)
                schedulePingTask()
            } else {
                Timber.tag(TAG).w("No characteristic found")
            }
        } else {
            Timber.tag(TAG).w("No service found")
        }
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BleCharacteristicInfo,
        data: ByteArray
    ) {
        super.onCharacteristicChanged(gatt, characteristic, data)
        packetsWorker.parsePackets(data)
    }

    override fun onDataParsed(data: ByteArray) {
        val message = String(data)
        Timber.tag(TAG).d("Received: %s", message)
    }

    override fun packetDataForSend(mtu: Int, data: ByteArray): List<ByteArray> {
        packetsWorker.maxPacketSize = mtu
        return packetsWorker.packetData(data)
    }

    override fun close() {
        super.close()
        taskScheduler.clear()
    }

    fun sendPingMessage() {
        sendData(
            MagicPingProfile.PING_SERVICE,
            MagicPingProfile.PING_CHARACTERISTIC,
            getPingData()
        )
    }

    private fun schedulePingTask() {
        Timber.tag(TAG).d("schedule ping task")
        taskScheduler.clear()
        taskScheduler.schedulePeriod({
            sendPingMessage()
        }, 0, PING_INTERVAL)
    }

    private fun getPingData(): ByteArray {
        messageId.value++
        return "This is a Ping message#${messageId.value} from MagicPingClient".toByteArray()
    }

    companion object {
        private const val TAG = "MagicPingClient"

        private const val PING_INTERVAL = 1000 * 60L // 60 seconds
        private const val MTU_REQUEST = 512
    }
}
