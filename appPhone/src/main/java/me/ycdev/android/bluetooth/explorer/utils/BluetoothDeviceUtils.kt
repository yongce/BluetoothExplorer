package me.ycdev.android.bluetooth.explorer.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.ParcelUuid
import me.ycdev.android.bluetooth.BluetoothHelper
import me.ycdev.android.bluetooth.explorer.R
import timber.log.Timber

object BluetoothDeviceUtils {
    private const val TAG = "BluetoothDeviceUtils"

    fun getDeviceName(context: Context, device: BluetoothDevice?): String {
        if (device == null || !BluetoothHelper.hasBluetoothConnectPermission(context)) {
            return context.getString(R.string.ble_unknown_device)
        }
        return getDeviceNameWithPermission(context, device)
    }

    fun isDeviceBonded(context: Context, device: BluetoothDevice?): Boolean {
        if (device == null || !BluetoothHelper.hasBluetoothConnectPermission(context)) {
            return false
        }
        return getBondStateWithPermission(device) == BluetoothDevice.BOND_BONDED
    }

    fun getBondedDevices(context: Context): MutableList<BluetoothDevice>? {
        if (!BluetoothHelper.hasBluetoothConnectPermission(context)) {
            return null
        }
        return getBondedDevicesWithPermission(context)
    }

    fun getDeviceUuids(context: Context, device: BluetoothDevice): Array<ParcelUuid>? {
        if (!BluetoothHelper.hasBluetoothConnectPermission(context)) {
            return null
        }
        return getDeviceUuidsWithPermission(device)
    }

    fun getConnectedDevices(context: Context, profile: BluetoothProfile): List<BluetoothDevice>? {
        if (!BluetoothHelper.hasBluetoothConnectPermission(context)) {
            return null
        }
        return getConnectedDevicesWithPermission(profile)
    }

    fun createBond(context: Context, device: BluetoothDevice): Boolean {
        if (!BluetoothHelper.hasBluetoothConnectPermission(context)) {
            return false
        }
        return createBondWithPermission(device)
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceNameWithPermission(context: Context, device: BluetoothDevice): String {
        return try {
            device.name ?: context.getString(R.string.ble_unknown_device)
        } catch (e: SecurityException) {
            Timber.tag(TAG).w(e, "Missing permission to read Bluetooth device name")
            context.getString(R.string.ble_unknown_device)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getBondStateWithPermission(device: BluetoothDevice): Int? {
        return try {
            device.bondState
        } catch (e: SecurityException) {
            Timber.tag(TAG).w(e, "Missing permission to read Bluetooth bond state")
            null
        }
    }

    @SuppressLint("MissingPermission")
    private fun getBondedDevicesWithPermission(context: Context): MutableList<BluetoothDevice>? {
        return try {
            BluetoothHelper.getBluetoothAdapter(context)?.bondedDevices?.toMutableList()
        } catch (e: SecurityException) {
            Timber.tag(TAG).w(e, "Missing permission to read bonded Bluetooth devices")
            null
        }
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceUuidsWithPermission(device: BluetoothDevice): Array<ParcelUuid>? {
        return try {
            device.uuids
        } catch (e: SecurityException) {
            Timber.tag(TAG).w(e, "Missing permission to read Bluetooth device UUIDs")
            null
        }
    }

    @SuppressLint("MissingPermission")
    private fun getConnectedDevicesWithPermission(profile: BluetoothProfile): List<BluetoothDevice>? {
        return try {
            profile.connectedDevices
        } catch (e: SecurityException) {
            Timber.tag(TAG).w(e, "Missing permission to read connected Bluetooth devices")
            null
        }
    }

    @SuppressLint("MissingPermission")
    private fun createBondWithPermission(device: BluetoothDevice): Boolean {
        return try {
            device.createBond()
        } catch (e: SecurityException) {
            Timber.tag(TAG).w(e, "Missing permission to create Bluetooth bond")
            false
        }
    }
}
