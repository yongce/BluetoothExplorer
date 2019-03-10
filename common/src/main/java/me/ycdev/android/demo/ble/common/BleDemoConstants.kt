package me.ycdev.android.demo.ble.common

import java.util.UUID

object BleDemoConstants {
    /**
     * Please refer [Google Fast Pair Service](https://developers.google.com/nearby/fast-pair/spec).
     */
    val SERVICE_GFP = UUID.fromString("0000FE2C-0000-1000-8000-00805f9b34fb")

    val SERVICE_MAGIC_WW: UUID = UUID.fromString("735dc4fa-348e-11e7-a919-92ebcb67fe33")

    val SERVICE_MFP = UUID.fromString("0000a1b2-0000-1000-8000-00805f9b34fb")
}