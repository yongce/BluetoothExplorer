package me.ycdev.android.demo.ble

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
import me.ycdev.android.demo.ble.common.BleConstants
import me.ycdev.android.demo.ble.common.BluetoothHelper
import me.ycdev.android.demo.ble.common.server.BleAdvertiser
import me.ycdev.android.demo.ble.common.server.BleAdvertiserSimple
import me.ycdev.android.demo.ble.common.server.MagicRadioGattServer
import me.ycdev.android.demo.ble.common.server.TimeServiceGattServer
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Objects

class AdvertiserActivity : AppCompatActivity() {
    private var bleAdvertiser: BleAdvertiser? = null
    private val advertiserCallback = MyAdvertiseCallback()

    private lateinit var selectedFilterCheckBox: RadioButton

    @BindView(R.id.advertise_btn)
    internal lateinit var advertiseBtn: Button
    @BindView(R.id.status)
    internal lateinit var statusView: TextView

    private val timestampFormatter = SimpleDateFormat("dd-HH:mm:ss", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_advertiser)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        Objects.requireNonNull<ActionBar>(supportActionBar).setDisplayHomeAsUpEnabled(true)

        initContentViews()
    }

    private fun initContentViews() {
        ButterKnife.bind(this)

        // Hide the "all" option
        findViewById<View>(R.id.filter_all).visibility = View.GONE
        selectedFilterCheckBox = findViewById(R.id.filter_magic_future)
        selectedFilterCheckBox.isChecked = true

        addStatusLog(getString(R.string.ble_status_init))
    }

    private fun updateContentViews() {
        if (bleAdvertiser?.isAdvertising() == true) {
            advertiseBtn.setText(R.string.ble_advertiser_stop)
        } else {
            advertiseBtn.setText(R.string.ble_advertiser_start)
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
        bleAdvertiser!!.start()
    }

    private fun advertiseForMagicFuture() {
        val settings = AdvertiseSettings.Builder()
            .setTimeout(0)
            .setConnectable(true)
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .build()
        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(BleConstants.SERVICE_MAGIC_FUTURE))
            .setIncludeDeviceName(true)
            .build()

        bleAdvertiser = BleAdvertiserSimple(this)
            .setSettings(settings)
            .setData(data)
            .setCallback(advertiserCallback)
        bleAdvertiser!!.start()
    }

    private fun advertiseForMagicRadio() {
        bleAdvertiser = MagicRadioGattServer(this)
        bleAdvertiser!!.start()
    }

    private fun advertiseForTimeService() {
        bleAdvertiser = TimeServiceGattServer(this)
        bleAdvertiser!!.start()
    }

    private fun startAdvertising() {
        addStatusLog(R.string.ble_status_advertise_initiated, selectedFilterCheckBox.text)
        when (selectedFilterCheckBox.id) {
            R.id.filter_magic_ww -> advertiseForMagicWw()
            R.id.filter_magic_future -> advertiseForMagicFuture()
            R.id.filter_magic_radio -> advertiseForMagicRadio()
            R.id.filter_time_service -> advertiseForTimeService()
        }
    }

    private fun stopAdvertising() {
        addStatusLog(R.string.ble_status_advertise_finished)
        bleAdvertiser?.stop()
    }

    @OnClick(R.id.advertise_btn)
    internal fun onAdvertiseButtonClick() {
        if (!BluetoothHelper.isBluetoothEnabled(this)) {
            BluetoothHelper.startBluetoothSysUI(this, RC_BT_ENABLE)
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
        R.id.filter_magic_ww,
        R.id.filter_magic_future,
        R.id.filter_magic_radio,
        R.id.filter_time_service
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
        bleAdvertiser?.stop()
    }

    private inner class MyAdvertiseCallback : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            val errorStr = BleAdvertiserSimple.errorCodeStr(errorCode)
            addStatusLog(R.string.ble_status_advertise_error, errorStr)
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            addStatusLog(R.string.ble_status_advertise_broadcasting)
        }
    }

    companion object {
        private const val TAG = "AdvertiserActivity"

        private const val RC_BT_ENABLE = 101
    }
}
