package me.ycdev.android.bluetooth.explorer.ble

import java.util.UUID

object BleConstants {
    /**
     * Please refer [Google Fast Pair Service](https://developers.google.com/nearby/fast-pair/spec).
     */
    val SERVICE_GFP: UUID = UUID.fromString("0000fe2c-0000-1000-8000-00805f9b34fb")

    val SERVICE_MAGIC_WW: UUID = UUID.fromString("735dc4fa-348e-11e7-a919-92ebcb67fe33")

    val SERVICE_MFP: UUID = UUID.fromString("0000fda6-0000-1000-8000-00805f9b34fb")
}