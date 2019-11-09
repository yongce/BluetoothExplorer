package me.ycdev.android.bluetooth.explorer

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Utils {
    private val timestampFormatter = SimpleDateFormat("dd-HH:mm:ss", Locale.US)

    fun getTimestamp() = timestampFormatter.format(Date(System.currentTimeMillis()))
}