package me.ycdev.android.bluetooth.explorer

import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import android.widget.TextView
import me.ycdev.android.bluetooth.ble.R.id
import me.ycdev.android.bluetooth.ble.R.layout

class MainActivity : WearableActivity() {

    private var mTextView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_main)

        mTextView = findViewById(id.text)

        // Enables Always-on
        setAmbientEnabled()
    }
}
