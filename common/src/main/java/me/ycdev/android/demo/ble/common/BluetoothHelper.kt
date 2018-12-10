package me.ycdev.android.demo.ble.common

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.annotation.NonNull
import androidx.annotation.Nullable

object BluetoothHelper {
    fun supportBle(@NonNull context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    }

    @Nullable
    fun getBluetoothAdapter(@NonNull context: Context): BluetoothAdapter? {
        val btMgr = context.getSystemService<BluetoothManager>(BluetoothManager::class.java)
        return btMgr?.adapter
    }

    fun isBluetoothEnabled(@NonNull context: Context): Boolean {
        val btAdapter = getBluetoothAdapter(context)
        return btAdapter != null && btAdapter.isEnabled
    }

    fun startBluetoothSysUI(@NonNull context: Activity, requestCode: Int) {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        context.startActivityForResult(enableBtIntent, requestCode)
    }
}
