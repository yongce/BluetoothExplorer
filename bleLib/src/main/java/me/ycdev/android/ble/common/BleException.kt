package me.ycdev.android.ble.common

import java.util.Locale

class BleException : Exception {
    constructor(message: String) : super(message)

    constructor(format: String, vararg args: Any) : super(
        String.format(
            Locale.US,
            format,
            *args
        )
    )
}
