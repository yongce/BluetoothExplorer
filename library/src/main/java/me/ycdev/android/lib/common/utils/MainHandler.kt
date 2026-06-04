package me.ycdev.android.lib.common.utils

import android.os.Handler
import android.os.Looper

object MainHandler {
    private val handler = Handler(Looper.getMainLooper())

    fun post(action: () -> Unit): Boolean = handler.post(action)

    fun postDelayed(action: () -> Unit, delayMillis: Long): Boolean =
        handler.postDelayed(action, delayMillis)
}
