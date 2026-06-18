package me.ycdev.android.bluetooth.explorer.utils

import android.bluetooth.BluetoothDevice
import android.content.Context

object BluetoothInfoDumper {
    fun dumpDevice(context: Context, device: BluetoothDevice, dump: (String) -> Unit) {
        val deviceName = BluetoothDeviceUtils.getDeviceName(context, device)
        dump("\nDump of device ${device.address} ($deviceName)")
        dumpUuids(context, device, dump)
    }

    fun dumpUuids(context: Context, device: BluetoothDevice, dump: (String) -> Unit) {
        val uuids = BluetoothDeviceUtils.getDeviceUuids(context, device)
        if (uuids.isNullOrEmpty()) {
            dump("No UUIDs found\n")
        } else {
            for ((index, uuid) in uuids.withIndex()) {
                dump("UUID#${index + 1}: $uuid\n")
            }
        }
    }
}
