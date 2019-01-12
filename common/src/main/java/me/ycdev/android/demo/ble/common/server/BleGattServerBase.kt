package me.ycdev.android.demo.ble.common.server

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import androidx.annotation.MainThread
import me.ycdev.android.demo.ble.common.BluetoothHelper
import timber.log.Timber
import java.util.Arrays
import java.util.UUID

abstract class BleGattServerBase(private val ownerTag: String, context: Context) : BleAdvertiser {
    protected var appContext = context.applicationContext
    protected var gattServer: BluetoothGattServer? = null
    private val gattServerCallback = MyGattServerCallback()
    private val bleAdvertiser = BleAdvertiserSimple(context)

    private val registeredDevices = mutableSetOf<BluetoothDevice>()

    protected abstract fun buildAdvertiseSettings(): AdvertiseSettings

    protected abstract fun buildAdvertiseData(): AdvertiseData

    protected open fun getAdvertiseCallback(): AdvertiseCallback? {
        return null
    }

    protected abstract fun addBleServices(gattServer: BluetoothGattServer): Boolean

    protected open fun notifyAllConnectedDevices(): Boolean {
        return false
    }

    protected open fun getClientConfigDescriptorUuid(): UUID? {
        return null
    }

    protected abstract fun onCharacteristicReadRequest(characteristic: BluetoothGattCharacteristic): ByteArray?

    protected open fun onStart() {
    }

    protected open fun onStop() {
    }

    override fun isAdvertising(): Boolean = bleAdvertiser.isAdvertising()

    @MainThread
    override fun start(): Boolean {
        if (!BluetoothHelper.canDoBleOperations(appContext)) {
            return false
        }

        // TODO what will happen if switch the order?

        // Step 1: Start BLE advertiser
        bleAdvertiser.setSettings(buildAdvertiseSettings())
            .setData(buildAdvertiseData())
            .setCallback(getAdvertiseCallback())
        if (!bleAdvertiser.start()) {
            return false
        }

        // Step 2: Start BLE GATT server
        gattServer = BluetoothHelper.getBluetoothManager(appContext)
            ?.openGattServer(appContext, gattServerCallback)
        if (gattServer == null) {
            Timber.tag(ownerTag).w("Failed to get BluetoothGattServer")
            bleAdvertiser.stop()
            return false
        }

        if (!addBleServices(gattServer!!)) {
            bleAdvertiser.stop()
            return false
        }

        onStart()
        return true
    }

    @MainThread
    override fun stop() {
        onStop()

        // Step 1: Stop BLE GATT server
        gattServer?.close()

        // Step 2: Stop BLE advertiser
        bleAdvertiser.stop()
    }

    /**
     * Send update to any devices that are subscribed to the characteristic.
     */
    protected fun notifyRegisteredDevices(func: (BluetoothDevice) -> Unit) {
        if (registeredDevices.isEmpty()) {
            Timber.tag(ownerTag).d("No devices registered. Discard notification.")
            return
        }

        Timber.tag(ownerTag).d("Sending update to %d devices", registeredDevices.size)
        registeredDevices.forEach {
            func(it)
        }
    }

    private inner class MyGattServerCallback : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            Timber.tag(ownerTag).d(
                "onConnectionStateChange device[%s] status[%s] newState[%s]",
                device, BluetoothHelper.gattStatusCodeStr(status),
                BluetoothHelper.gattConnectionStateStr(newState)
            )

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (notifyAllConnectedDevices()) {
                    registeredDevices.add(device)
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // Remove device from any active subscriptions
                registeredDevices.remove(device)
            }
        }

        override fun onDescriptorReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            descriptor: BluetoothGattDescriptor
        ) {
            Timber.tag(ownerTag).d(
                "onDescriptorReadRequest, device[%s] reqId[%d]",
                device, requestId
            )
            if (getClientConfigDescriptorUuid() == descriptor.uuid) {
                val result = if (registeredDevices.contains(device)) {
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                } else {
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                }
                gattServer?.sendResponse(
                    device, requestId, BluetoothGatt.GATT_SUCCESS, 0, result
                )
            } else {
                Timber.tag(ownerTag).w("Unknown descriptor read request uuid[%s]", descriptor.uuid)
                gattServer?.sendResponse(
                    device, requestId, BluetoothGatt.GATT_FAILURE, 0, null
                )
            }
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            Timber.tag(ownerTag).d(
                "onDescriptorWriteRequest, device[%s] reqId[%d]",
                device, requestId
            )
            if (getClientConfigDescriptorUuid() == descriptor.uuid) {
                var status = BluetoothGatt.GATT_SUCCESS
                if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
                    Timber.tag(ownerTag).d("Device[%s] subscribe to notifications", device)
                    registeredDevices.add(device)
                } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
                    Timber.tag(ownerTag).d("Device[%s] unsubscribe from notifications", device)
                    registeredDevices.remove(device)
                } else {
                    Timber.tag(ownerTag).w("Unknown value: %s", Arrays.toString(value))
                    status = BluetoothGatt.GATT_FAILURE
                }

                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, status, 0, null)
                }
            } else {
                Timber.tag(ownerTag).w("Unknown descriptor write request uuid[%s]", descriptor.uuid)
                if (responseNeeded) {
                    gattServer?.sendResponse(
                        device, requestId, BluetoothGatt.GATT_FAILURE, 0, null
                    )
                }
            }
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            Timber.tag(ownerTag).d(
                "onCharacteristicReadRequest device[%s] reqId[%d]",
                device, requestId
            )
            val value = onCharacteristicReadRequest(characteristic)
            gattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
        }

        override fun onNotificationSent(device: BluetoothDevice, status: Int) {
            Timber.tag(ownerTag).d(
                "onNotificationSent device[%s] status[%s]",
                device, BluetoothHelper.gattStatusCodeStr(status)
            )
        }

        override fun onMtuChanged(device: BluetoothDevice, mtu: Int) {
            Timber.tag(ownerTag).d("onMtuChanged device[%s] mtu[%d]", device, mtu)
        }

        override fun onPhyUpdate(device: BluetoothDevice, txPhy: Int, rxPhy: Int, status: Int) {
            Timber.tag(ownerTag).d(
                "onPhyUpdate device[%s] txPhy[%d] rxPhy[%d] status[%s]",
                device, txPhy, rxPhy,
                BluetoothHelper.gattStatusCodeStr(status)
            )
        }

        override fun onExecuteWrite(device: BluetoothDevice, requestId: Int, execute: Boolean) {
            Timber.tag(ownerTag).d(
                "onExecuteWrite device[%s] reqId[%d] execute[%s]",
                device, requestId, execute
            )
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            Timber.tag(ownerTag).d(
                "onCharacteristicWriteRequest device[%s] reqId[%d]",
                device, requestId
            )
        }

        override fun onPhyRead(device: BluetoothDevice, txPhy: Int, rxPhy: Int, status: Int) {
            Timber.tag(ownerTag).d(
                "onPhyRead device[%s] txPhy[%d] rxPhy[%d] status[%s]",
                device, txPhy, rxPhy,
                BluetoothHelper.gattStatusCodeStr(status)
            )
        }

        override fun onServiceAdded(status: Int, service: BluetoothGattService) {
            Timber.tag(ownerTag).d(
                "onServiceAdded service[%s] status[%s]",
                service.uuid, BluetoothHelper.gattStatusCodeStr(status)
            )
        }
    }
}
