package com.mobvoi.bt.profile

import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothProfile
import android.content.Context
import me.ycdev.android.lib.common.utils.WeakListenerManager

class A2dpProfileMonitor private constructor(val context: Context) :
    WeakListenerManager<BluetoothProfileListener>() {

    private var profileHelper: BluetoothProfileHelper<BluetoothA2dp>

    init {
        profileHelper = BluetoothProfileHelper(context.applicationContext, BluetoothProfile.A2DP, TAG) {
            notifyListeners { it.onProxyChanged(BluetoothProfile.A2DP) }
        }
        profileHelper.initProfile()
    }

    fun getProfileProxy(): BluetoothA2dp? {
        return profileHelper.profileProxy
    }

    companion object {
        private const val TAG = "A2dpProfileMonitor"

        @SuppressLint("StaticFieldLeak")
        @Volatile private var INSTANCE: A2dpProfileMonitor? = null

        fun getInstance(context: Context): A2dpProfileMonitor =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: A2dpProfileMonitor(context.applicationContext).also { INSTANCE = it }
            }
    }
}