package me.ycdev.android.lib.common.async

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor

class HandlerTaskExecutor private constructor(private val handler: Handler) : Executor {
    override fun execute(command: Runnable) {
        handler.post(command)
    }

    companion object {
        fun withMainLooper(): HandlerTaskExecutor =
            HandlerTaskExecutor(Handler(Looper.getMainLooper()))
    }
}
