package me.ycdev.android.demo.ble

import android.app.Application

import me.ycdev.android.lib.common.utils.ApplicationUtils
import timber.log.Timber

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        ApplicationUtils.initApplication(this)
        Timber.tag(TAG).d("app start...")
    }

    companion object {
        private const val TAG = "bledemo.app"
    }
}
