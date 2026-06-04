package timber.log

import android.util.Log

object Timber {
    open class DebugTree

    fun plant(tree: DebugTree) {
        // Logging is always available through android.util.Log in this compatibility shim.
    }

    fun tag(tag: String): Logger = Logger(tag)

    class Logger(private val tag: String) {
        fun v(message: String, vararg args: Any?) = log(Log.VERBOSE, null, message, *args)

        fun d(message: String, vararg args: Any?) = log(Log.DEBUG, null, message, *args)

        fun i(message: String, vararg args: Any?) = log(Log.INFO, null, message, *args)

        fun w(message: String, vararg args: Any?) = log(Log.WARN, null, message, *args)

        fun w(throwable: Throwable, message: String, vararg args: Any?) =
            log(Log.WARN, throwable, message, *args)

        fun e(message: String, vararg args: Any?) = log(Log.ERROR, null, message, *args)

        fun e(throwable: Throwable, message: String, vararg args: Any?) =
            log(Log.ERROR, throwable, message, *args)

        private fun log(priority: Int, throwable: Throwable?, message: String, vararg args: Any?) {
            val text = if (args.isEmpty()) message else message.format(*args)
            if (throwable == null) {
                Log.println(priority, tag, text)
            } else {
                Log.println(priority, tag, "$text\n${Log.getStackTraceString(throwable)}")
            }
        }
    }
}
