package me.ycdev.android.bluetooth.explorer.utils

import android.bluetooth.BluetoothDevice

object BluetoothInfoDumper {
    fun dumpDevice(device: BluetoothDevice, dump: (String) -> Unit) {
        dump("\nDump of device ${device.address} (${device.name})")
        dumpUuids(device, dump)
    }

    fun dumpUuids(device: BluetoothDevice, dump: (String) -> Unit) {
        val uuids = device.uuids
        if (uuids.isNullOrEmpty()) {
            dump("No UUIDs found\n")
        } else {
            for ((index, uuid) in uuids.withIndex()) {
                dump("UUID#${index + 1}: $uuid\n")
            }
        }
    }
}
