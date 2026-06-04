package me.ycdev.android.lib.common.async

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor

class TaskScheduler(looper: Looper, private val tag: String) {
    private val handler = Handler(looper)
    private val tasks = mutableListOf<Runnable>()

    fun schedulePeriod(executor: Executor, delayMillis: Long, periodMillis: Long, action: () -> Unit) {
        val task = object : Runnable {
            override fun run() {
                executor.execute { action() }
                handler.postDelayed(this, periodMillis)
            }
        }
        tasks.add(task)
        handler.postDelayed(task, delayMillis)
    }

    fun clear() {
        tasks.forEach { handler.removeCallbacks(it) }
        tasks.clear()
    }
}
