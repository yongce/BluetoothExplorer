package me.ycdev.android.bluetooth

import java.util.Locale

class BluetoothException : Exception {
    constructor(message: String) : super(message)

    constructor(format: String, vararg args: Any) : super(
        String.format(
            Locale.US,
            format,
            *args
        )
    )
}
