package me.ycdev.android.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import java.util.Locale
import me.ycdev.android.lib.common.perms.PermissionUtils
import me.ycdev.android.lib.common.utils.EncodingUtils.fromHexString
import timber.log.Timber

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

    @SuppressLint("MissingPermission")
    fun isBluetoothEnabled(context: Context): Boolean {
        if (!hasBluetoothConnectPermission(context)) {
            return false
        }
        val btAdapter = getBluetoothAdapter(context)
        return try {
            btAdapter != null && btAdapter.isEnabled
        } catch (e: SecurityException) {
            Timber.tag(TAG).w(e, "No permission to check Bluetooth state")
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun startBluetoothSysUI(context: Activity, requestCode: Int) {
        if (!hasBluetoothConnectPermission(context)) {
            Timber.tag(TAG).w("No Bluetooth connect permission")
            return
        }
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        try {
            context.startActivityForResult(enableBtIntent, requestCode)
        } catch (e: SecurityException) {
            Timber.tag(TAG).w(e, "Failed to start Bluetooth system UI")
        }
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
        if (!hasBluetoothConnectPermission(context)) {
            Timber.tag(TAG).w("No Bluetooth connect permission")
            return false
        }
        if (!isBluetoothEnabled(context)) {
            Timber.tag(TAG).w("Bluetooth not enabled")
            return false
        }
        return true
    }

    @SuppressLint("MissingPermission")
    fun isDeviceBonded(context: Context, btAddress: String): Boolean {
        if (!hasBluetoothConnectPermission(context)) {
            return false
        }
        return try {
            getBluetoothAdapter(context)?.bondedDevices?.find { it.address == btAddress } != null
        } catch (e: SecurityException) {
            Timber.tag(TAG).w(e, "No permission to query bonded devices")
            false
        }
    }

    fun bleScanPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    fun bleAdvertisePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_ADVERTISE)
        } else {
            emptyArray()
        }
    }

    fun bleConnectPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            emptyArray()
        }
    }

    fun hasBluetoothScanPermission(context: Context): Boolean {
        return PermissionUtils.hasPermissions(context, *bleScanPermissions())
    }

    fun hasBluetoothAdvertisePermission(context: Context): Boolean {
        return PermissionUtils.hasPermissions(context, *bleAdvertisePermissions())
    }

    fun hasBluetoothConnectPermission(context: Context): Boolean {
        return PermissionUtils.hasPermissions(context, *bleConnectPermissions())
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

    fun adapterStateStr(state: Int): String {
        return when (state) {
            BluetoothAdapter.STATE_TURNING_OFF -> "STATE_TURNING_OFF"
            BluetoothAdapter.STATE_OFF -> "STATE_OFF"
            BluetoothAdapter.STATE_TURNING_ON -> "STATE_TURNING_ON"
            BluetoothAdapter.STATE_ON -> "STATE_ON"
            BluetoothAdapter.STATE_DISCONNECTING -> "STATE_DISCONNECTING"
            BluetoothAdapter.STATE_DISCONNECTED -> "STATE_DISCONNECTED"
            BluetoothAdapter.STATE_CONNECTING -> "STATE_CONNECTING"
            BluetoothAdapter.STATE_CONNECTED -> "STATE_CONNECTED"
            else -> "UNKNOWN-$state"
        }
    }

    fun bleScanErrorStr(errCode: Int): String {
        return when (errCode) {
            ScanCallback.SCAN_FAILED_ALREADY_STARTED -> "ALREADY_STARTED"
            ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "APPLICATION_REGISTRATION_FAILED"
            ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> "INTERNAL_ERROR"
            ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> "FEATURE_UNSUPPORTED"
            else -> "UNKNOWN-$errCode"
        }
    }

    fun profileStr(profile: Int): String {
        return when (profile) {
            BluetoothProfile.A2DP -> "A2DP"
            BluetoothProfile.GATT -> "GATT"
            BluetoothProfile.GATT_SERVER -> "GATT_SERVER"
            BluetoothProfile.HEADSET -> "HEADSET"
            PROFILE_HEALTH -> "HEALTH"
            else -> "Profile-$profile"
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

    private const val PROFILE_HEALTH = 3
}
