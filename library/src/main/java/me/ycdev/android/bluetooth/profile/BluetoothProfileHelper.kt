package com.mobvoi.bt.profile

import android.bluetooth.BluetoothProfile
import android.content.Context
import me.ycdev.android.bluetooth.BluetoothHelper
import timber.log.Timber

class BluetoothProfileHelper<T : BluetoothProfile>(
    val context: Context,
    val btProfile: Int,
    val tag: String,
    val onChangeListener: () -> Unit
) {
    private val btProfileListener = MyProfileListener()
    var profileProxy: T? = null

    fun initProfile() {
        val btAdapter = BluetoothHelper.getBluetoothAdapter(context)
        val res = btAdapter?.getProfileProxy(context, btProfileListener, btProfile)
        Timber.tag(tag).d("init profile result: $res")
        // Wait for BluetoothProfile.ServiceListener.onServiceConnected to be called
    }

    fun closeProfile() {
        if (profileProxy != null) {
            val btAdapter = BluetoothHelper.getBluetoothAdapter(context)
            btAdapter?.closeProfileProxy(btProfile, profileProxy)
            // Wait for BluetoothProfile.ServiceListener.onServiceDisconnected to be called
        }
    }

    private inner class MyProfileListener : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            Timber.tag(tag).d("BluetoothProfile, connected: %s",
                BluetoothHelper.profileStr(profile)
            )
            if (profile == btProfile) {
                @Suppress("UNCHECKED_CAST")
                profileProxy = proxy as T
                onChangeListener()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            Timber.tag(tag).d("BluetoothProfile, disconnected: %s",
                BluetoothHelper.profileStr(profile)
            )
            if (profile == btProfile) {
                profileProxy = null
                onChangeListener()
            }
        }
    }
}
