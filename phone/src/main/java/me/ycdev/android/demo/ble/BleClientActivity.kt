package me.ycdev.android.demo.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import me.ycdev.android.demo.ble.common.BluetoothHelper
import me.ycdev.android.demo.ble.common.client.BleGattClient
import me.ycdev.android.demo.ble.common.client.ClientState
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Objects

class BleClientActivity : AppCompatActivity() {
    @BindView(R.id.connect_btn)
    internal lateinit var connectBtn: Button
    @BindView(R.id.pair_btn)
    internal lateinit var pairBtn: Button
    @BindView(R.id.status)
    internal lateinit var statusView: TextView

    private lateinit var gattClient: BleGattClient
    private val gattCallback = MyCallback()
    private var receiver: BroadcastReceiver? = null
    private var device: BluetoothDevice? = null

    private val timestampFormatter = SimpleDateFormat("dd-HH:mm:ss", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble_client)
        Timber.tag(TAG).d("onCreate")

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        Objects.requireNonNull<ActionBar>(supportActionBar).setDisplayHomeAsUpEnabled(true)

        device = intent.getParcelableExtra(EXTRA_DEVICE)
        if (device == null) {
            Timber.tag(TAG).e("No device specified to connect")
            finish()
            return
        }

        gattClient = BleGattClient(this)

        initContentViews()
    }

    private fun initContentViews() {
        ButterKnife.bind(this)
        updateContentViews()
        addStatusLog(getString(R.string.ble_status_init))
    }

    private fun updateContentViews() {
        if (gattClient.isStarted()) {
            connectBtn.setText(R.string.ble_client_disconnect)
        } else {
            connectBtn.setText(R.string.ble_client_connect)
        }

        if (device?.bondState == BluetoothDevice.BOND_BONDED) {
            pairBtn.isEnabled = false
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

    @OnClick(R.id.connect_btn)
    internal fun connect() {
        if (!BluetoothHelper.isBluetoothEnabled(this)) {
            BluetoothHelper.startBluetoothSysUI(this, RC_BT_ENABLE)
            return
        }

        val started = gattClient.isStarted()
        Timber.tag(TAG).d("BLE client is started: %s", started)
        if (!started) {
            addStatusLog(R.string.ble_status_client_connecting, device!!.address, device!!.name)
            gattClient.connect(device!!, gattCallback)
        } else {
            gattClient.close()
        }

        updateContentViews()
    }

    @OnClick(R.id.pair_btn)
    internal fun pair() {
        if (!BluetoothHelper.isBluetoothEnabled(this)) {
            BluetoothHelper.startBluetoothSysUI(this, RC_BT_ENABLE)
            return
        }

        if (receiver == null) {
            receiver = MyReceiver()
        }
        val intentFilter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        registerReceiver(receiver, intentFilter)

        addStatusLog(R.string.ble_status_client_pairing, device!!.address, device!!.name)
        if (device?.createBond() != true) {
            addStatusLog(R.string.ble_status_client_pair_failed)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_BT_ENABLE) {
            if (BluetoothHelper.isBluetoothEnabled(this)) {
//                connect()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.tag(TAG).d("onDestroy")
        gattClient.close()

        if (receiver != null) {
            unregisterReceiver(receiver)
        }
    }

    private inner class MyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED == action) {
                val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                val state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
                addStatusLog(
                    R.string.ble_status_client_pair_state_changed,
                    BluetoothHelper.bondStateStr(state),
                    device.address, device.name
                )
                updateContentViews()

                // The device address will change when bonding or bonded
                this@BleClientActivity.device = device
            }
        }
    }

    private inner class MyCallback : BleGattClient.Callback {
        @MainThread
        override fun onStateChanged(newState: ClientState) {
            addStatusLog(R.string.ble_status_client_state_changed, newState)
            updateContentViews()
        }

        override fun onServicesDiscovered(services: List<BluetoothGattService>) {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }
    }

    companion object {
        private const val TAG = "BleClientActivity"

        private const val RC_BT_ENABLE = 101

        const val EXTRA_DEVICE = "extra.device"
    }
}
