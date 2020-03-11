package me.ycdev.android.bluetooth.explorer

import android.annotation.SuppressLint
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Intent
import android.os.Bundle
import android.os.ParcelUuid
import android.view.View
import android.widget.RadioButton
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import me.ycdev.android.bluetooth.BluetoothHelper
import me.ycdev.android.bluetooth.ble.ext.MagicPingServer
import me.ycdev.android.bluetooth.ble.ext.MagicRadioServer
import me.ycdev.android.bluetooth.ble.server.BleAdvertiser
import me.ycdev.android.bluetooth.ble.server.BleAdvertiserSimple
import me.ycdev.android.bluetooth.ble.sig.BatteryServiceServer
import me.ycdev.android.bluetooth.ble.sig.TimeServiceServer
import me.ycdev.android.bluetooth.explorer.ble.BleConstants
import me.ycdev.android.bluetooth.explorer.databinding.ActivityAdvertiserBinding
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Objects

class AdvertiserActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdvertiserBinding

    private var bleAdvertiser: BleAdvertiser? = null
    private val advertiserCallback = MyAdvertiseCallback()

    private lateinit var selectedFilterCheckBox: RadioButton

    private val timestampFormatter = SimpleDateFormat("dd-HH:mm:ss", Locale.US)

    private val startCallback: (Boolean) -> Unit = {
        updateContentViews()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdvertiserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        Objects.requireNonNull<ActionBar>(supportActionBar).setDisplayHomeAsUpEnabled(true)

        initContentViews()
    }

    private fun initContentViews() {
        // Hide the "all" option
        binding.content.filters.all.visibility = View.GONE
        selectedFilterCheckBox = binding.content.filters.magicPing
        selectedFilterCheckBox.isChecked = true

        binding.content.filters.magicWw.setOnClickListener { onCheckboxClick(it) }
        binding.content.filters.magicPing.setOnClickListener { onCheckboxClick(it) }
        binding.content.filters.magicRadio.setOnClickListener { onCheckboxClick(it) }
        binding.content.filters.gfpService.setOnClickListener { onCheckboxClick(it) }
        binding.content.filters.mfpService.setOnClickListener { onCheckboxClick(it) }
        binding.content.filters.timeService.setOnClickListener { onCheckboxClick(it) }
        binding.content.filters.batteryService.setOnClickListener { onCheckboxClick(it) }

        addStatusLog(getString(R.string.ble_status_init))

        binding.content.advertiseBtn.setOnClickListener { onAdvertiseButtonClick() }
    }

    private fun updateContentViews() {
        if (bleAdvertiser?.isAdvertising() == true) {
            binding.content.advertiseBtn.setText(R.string.ble_advertiser_stop)
        } else {
            binding.content.advertiseBtn.setText(R.string.ble_advertiser_start)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun addStatusLog(status: CharSequence) {
        Timber.tag(TAG).d("Status changed: %s", status)
        val timestamp = timestampFormatter.format(Date(System.currentTimeMillis()))
        val oldStatus = binding.content.status.text
        binding.content.status.text = "$timestamp $status\n\n$oldStatus"
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
        addStatusLog(R.string.ble_status_advertise_initiated, selectedFilterCheckBox.text)
        when (selectedFilterCheckBox.id) {
            R.id.magic_ww -> advertiseForMagicWw()
            R.id.magic_ping -> advertiseForMagicPing()
            R.id.magic_radio -> advertiseForMagicRadio()
            R.id.time_service -> advertiseForTimeService()
            R.id.battery_service -> advertiseForBatteryService()
        }
    }

    private fun stopAdvertising() {
        addStatusLog(R.string.ble_status_advertise_finished)
        bleAdvertiser?.stop {
            updateContentViews()
        }
    }

    private fun onAdvertiseButtonClick() {
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

    private fun onCheckboxClick(v: View) {
        if (v != selectedFilterCheckBox) {
            selectedFilterCheckBox.isChecked = false
            selectedFilterCheckBox = v as RadioButton
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
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
