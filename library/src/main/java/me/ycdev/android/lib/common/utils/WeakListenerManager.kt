package me.ycdev.android.lib.common.utils

import java.lang.ref.WeakReference

open class WeakListenerManager<T : Any> {
    private val listeners = mutableListOf<WeakReference<T>>()

    fun addListener(listener: T) {
        removeListener(listener)
        listeners.add(WeakReference(listener))
    }

    fun removeListener(listener: T) {
        listeners.removeAll { it.get() == null || it.get() === listener }
    }

    protected fun notifyListeners(action: (T) -> Unit) {
        val iterator = listeners.iterator()
        while (iterator.hasNext()) {
            val listener = iterator.next().get()
            if (listener == null) {
                iterator.remove()
            } else {
                action(listener)
            }
        }
    }
}
