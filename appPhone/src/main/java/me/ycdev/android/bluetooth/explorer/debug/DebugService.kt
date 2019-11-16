package me.ycdev.android.bluetooth.explorer.debug

import android.app.Service
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.mobvoi.bt.profile.A2dpProfileMonitor
import com.mobvoi.bt.profile.BluetoothProfileListener
import me.ycdev.android.bluetooth.explorer.BuildConfig
import me.ycdev.android.bluetooth.explorer.utils.BluetoothInfoDumper
import timber.log.Timber

class DebugService : Service(), BluetoothProfileListener {
    override fun onCreate() {
        super.onCreate()
        Timber.tag(TAG).d("onCreate")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.tag(TAG).d("onStartCommand: $intent")
        if (intent != null) {
            when (intent.action) {
                ACTION_DUMP_DEVICES -> dumpDevices()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun dumpDevices() {
        if (!DEBUG_FEATURE) {
            return
        }

        val dump: (String) -> Unit = {
            Timber.tag(TAG).d(it)
        }

        dump("Dump A2DP devices...\n")
        val a2dpProfile = A2dpProfileMonitor.getInstance(this).getProfileProxy()
        if (a2dpProfile == null) {
            dump("A2DP proxy not connected yet\n")
        } else {
            val a2dpDevices = a2dpProfile.connectedDevices
            if (a2dpDevices.isNullOrEmpty()) {
                dump("No connected A2DP devices\n")
            } else {
                a2dpDevices.forEach { BluetoothInfoDumper.dumpDevice(it, dump) }
            }
        }
    }

    override fun onProxyChanged(btProfile: Int) {
        if (btProfile == BluetoothProfile.A2DP) {
            Timber.tag(TAG).d(
                "A2DP proxy changed: %s",
                A2dpProfileMonitor.getInstance(this).getProfileProxy()
            )
        }
    }

    companion object {
        private const val TAG = "DebugService"

        private val DEBUG_FEATURE = BuildConfig.DEBUG || Build.TYPE != "user"

        private const val ACTION_DUMP_DEVICES = "debug.ACTION_DUMP_DEVICES"
    }
}