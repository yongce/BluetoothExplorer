package me.ycdev.android.bluetooth.explorer

import android.annotation.SuppressLint
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Intent
import android.os.Bundle
import android.os.ParcelUuid
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import me.ycdev.android.bluetooth.BluetoothHelper
import me.ycdev.android.bluetooth.ble.R.id
import me.ycdev.android.bluetooth.ble.R.layout
import me.ycdev.android.bluetooth.ble.R.string
import me.ycdev.android.bluetooth.ble.ext.MagicPingServer
import me.ycdev.android.bluetooth.ble.ext.MagicRadioServer
import me.ycdev.android.bluetooth.ble.server.BleAdvertiser
import me.ycdev.android.bluetooth.ble.server.BleAdvertiserSimple
import me.ycdev.android.bluetooth.ble.sig.BatteryServiceServer
import me.ycdev.android.bluetooth.ble.sig.TimeServiceServer
import me.ycdev.android.bluetooth.explorer.ble.BleConstants
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Objects

class AdvertiserActivity : AppCompatActivity() {
    private var bleAdvertiser: BleAdvertiser? = null
    private val advertiserCallback = MyAdvertiseCallback()

    private lateinit var selectedFilterCheckBox: RadioButton

    @BindView(id.advertise_btn)
    internal lateinit var advertiseBtn: Button
    @BindView(id.status)
    internal lateinit var statusView: TextView

    private val timestampFormatter = SimpleDateFormat("dd-HH:mm:ss", Locale.US)

    private val startCallback: (Boolean) -> Unit = {
        updateContentViews()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_advertiser)

        val toolbar = findViewById<Toolbar>(id.toolbar)
        setSupportActionBar(toolbar)
        Objects.requireNonNull<ActionBar>(supportActionBar).setDisplayHomeAsUpEnabled(true)

        initContentViews()
    }

    private fun initContentViews() {
        ButterKnife.bind(this)

        // Hide the "all" option
        findViewById<View>(id.filter_all).visibility = View.GONE
        selectedFilterCheckBox = findViewById(id.filter_magic_ping)
        selectedFilterCheckBox.isChecked = true

        addStatusLog(getString(string.ble_status_init))
    }

    private fun updateContentViews() {
        if (bleAdvertiser?.isAdvertising() == true) {
            advertiseBtn.setText(string.ble_advertiser_stop)
        } else {
            advertiseBtn.setText(string.ble_advertiser_start)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun addStatusLog(status: CharSequence) {
        Timber.tag(TAG).d("Status changed: %s", status)
        val timestamp = timestampFormatter.format(Date(System.currentTimeMillis()))
        val oldStatus = statusView.text
        statusView.text = "$timestamp $status\n\n$oldStatus"
    }

    private fun addStatusLog(@StringRes resId: Int, vararg formatArgs: Any) {
        val status = getString(resId, *formatArgs)
        addStatusLog(status)
    }

    private fun advertiseForMagicWw() {
        val settings = AdvertiseSettings.Builder()
            .setTimeout(0)
            .setConnectable(false)
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .build()
        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(BleConstants.SERVICE_MAGIC_WW))
            .setIncludeDeviceName(true)
            .build()

        bleAdvertiser = BleAdvertiserSimple(this)
            .setSettings(settings)
            .setData(data)
            .setCallback(advertiserCallback)
        bleAdvertiser!!.start(startCallback)
    }

    private fun advertiseForMagicPing() {
        bleAdvertiser = MagicPingServer(this)
        bleAdvertiser!!.start(startCallback)
    }

    private fun advertiseForMagicRadio() {
        bleAdvertiser = MagicRadioServer(this)
        bleAdvertiser!!.start(startCallback)
    }

    private fun advertiseForTimeService() {
        bleAdvertiser = TimeServiceServer(this)
        bleAdvertiser!!.start(startCallback)
    }

    private fun advertiseForBatteryService() {
        bleAdvertiser = BatteryServiceServer(this)
        bleAdvertiser!!.start(startCallback)
    }

    private fun startAdvertising() {
        addStatusLog(string.ble_status_advertise_initiated, selectedFilterCheckBox.text)
        when (selectedFilterCheckBox.id) {
            id.filter_magic_ww -> advertiseForMagicWw()
            id.filter_magic_ping -> advertiseForMagicPing()
            id.filter_magic_radio -> advertiseForMagicRadio()
            id.filter_time_service -> advertiseForTimeService()
            id.filter_battery_service -> advertiseForBatteryService()
        }
    }

    private fun stopAdvertising() {
        addStatusLog(string.ble_status_advertise_finished)
        bleAdvertiser?.stop {
            updateContentViews()
        }
    }

    @OnClick(id.advertise_btn)
    internal fun onAdvertiseButtonClick() {
        if (!BluetoothHelper.isBluetoothEnabled(this)) {
            BluetoothHelper.startBluetoothSysUI(this,
                RC_BT_ENABLE
            )
            return
        }

        val advertising = bleAdvertiser?.isAdvertising() ?: false
        Timber.tag(TAG).d("BLE is advertising: %s", advertising)
        if (!advertising) {
            startAdvertising()
        } else {
            stopAdvertising()
        }

        updateContentViews()
    }

    @OnClick(
        id.filter_magic_ww,
        id.filter_magic_ping,
        id.filter_magic_radio,
        id.filter_gfp_service,
        id.filter_mfp_service,
        id.filter_time_service,
        id.filter_battery_service
    )
    fun onCheckboxClick(v: View) {
        if (v != selectedFilterCheckBox) {
            selectedFilterCheckBox.isChecked = false
            selectedFilterCheckBox = v as RadioButton
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_BT_ENABLE) {
            if (BluetoothHelper.isBluetoothEnabled(this)) {
                onAdvertiseButtonClick()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bleAdvertiser?.stop(null)
    }

    private inner class MyAdvertiseCallback : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            val errorStr = BleAdvertiserSimple.errorCodeStr(errorCode)
            addStatusLog(string.ble_status_advertise_error, errorStr)
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            addStatusLog(string.ble_status_advertise_broadcasting)
        }
    }

    companion object {
        private const val TAG = "AdvertiserActivity"

        private const val RC_BT_ENABLE = 101
    }
}
