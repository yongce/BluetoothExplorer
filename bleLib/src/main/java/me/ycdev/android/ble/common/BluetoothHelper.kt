package me.ycdev.android.ble.common

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import me.ycdev.android.lib.common.utils.EncodingUtils.fromHexString
import timber.log.Timber
import java.util.Locale

object BluetoothHelper {
    private const val TAG = "BluetoothHelper"

    fun supportBle(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    }

    fun getBluetoothManager(context: Context): BluetoothManager? {
        return context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    fun getBluetoothAdapter(context: Context): BluetoothAdapter? {
        return getBluetoothManager(context)?.adapter
    }

    fun isBluetoothEnabled(context: Context): Boolean {
        val btAdapter = getBluetoothAdapter(context)
        return btAdapter != null && btAdapter.isEnabled
    }

    fun startBluetoothSysUI(context: Activity, requestCode: Int) {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        context.startActivityForResult(enableBtIntent, requestCode)
    }

    fun canDoBleOperations(context: Context): Boolean {
        if (!supportBle(context)) {
            Timber.tag(TAG).w("This device doesn't support BLE")
            return false
        }
        val btAdapter = getBluetoothAdapter(context)
        if (btAdapter == null) {
            Timber.tag(TAG).w("Failed to get BluetoothAdapter")
            return false
        }
        if (!btAdapter.isEnabled) {
            Timber.tag(TAG).w("Bluetooth not enabled")
            return false
        }
        return true
    }

    fun isDeviceBonded(context: Context, btAddress: String): Boolean {
        return getBluetoothAdapter(context)?.bondedDevices?.find { it.address == btAddress } != null
    }

    fun gattStatusCodeStr(status: Int): String {
        // For unknown status code, please refer "system/bt/stack/include/gatt_api.h" in AOSP
        return when (status) {
            BluetoothGatt.GATT_SUCCESS -> "GATT_SUCCESS"
            BluetoothGatt.GATT_FAILURE -> "GATT_FAILURE"
            BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION -> "GATT_INSUFFICIENT_AUTHENTICATION"
            BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION -> "GATT_INSUFFICIENT_ENCRYPTION"
            BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> "GATT_INVALID_ATTRIBUTE_LENGTH"
            BluetoothGatt.GATT_INVALID_OFFSET -> return "GATT_INVALID_OFFSET"
            BluetoothGatt.GATT_READ_NOT_PERMITTED -> return "GATT_READ_NOT_PERMITTED"
            BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED -> return "GATT_REQUEST_NOT_SUPPORTED"
            BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> return "GATT_WRITE_NOT_PERMITTED"
            0x01 -> "GATT_INVALID_HANDLE"
            0x04 -> "GATT_INVALID_PDU"
            0x08 -> "GATT_INSUF_AUTHORIZATION"
            0x80 -> "GATT_NO_RESOURCES"
            0x84 -> "GATT_BUSY"
            0x85 -> "GATT_ERROR"
            else -> return String.format(Locale.US, "GATT_STATUS_UNKNOWN-[0x%x]", status)
        }
    }

    fun connectionStateStr(state: Int): String {
        return when (state) {
            BluetoothProfile.STATE_DISCONNECTED -> "DISCONNECTED"
            BluetoothProfile.STATE_CONNECTING -> "CONNECTING"
            BluetoothProfile.STATE_CONNECTED -> "CONNECTED"
            BluetoothProfile.STATE_DISCONNECTING -> "DISCONNECTING"
            else -> "CONNECTION_STATE_UNKNOWN-[$state]"
        }
    }

    fun bondStateStr(state: Int): String {
        return when (state) {
            BluetoothDevice.BOND_NONE -> "BOND_NONE"
            BluetoothDevice.BOND_BONDING -> "BOND_BONDING"
            BluetoothDevice.BOND_BONDED -> "BOND_BONDED"
            else -> "UNKNOWN-$state"
        }
    }

    fun addressStr(address: ByteArray): String {
        if (address.size != 6) {
            throw IllegalArgumentException("Bad address length: ${address.size}")
        }

        return address.joinToString(separator = ":", transform = { data -> String.format("%02X", data) })
    }

    fun parseAddressStr(address: String): ByteArray {
        if (address.length != 17) {
            throw IllegalArgumentException("Bad address: $address")
        }

        return fromHexString(address.replace(":", ""))
    }
}
