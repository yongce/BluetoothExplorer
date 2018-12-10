package me.ycdev.android.demo.ble

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import me.ycdev.android.demo.ble.common.BleScanner
import me.ycdev.android.demo.ble.common.BluetoothHelper
import me.ycdev.android.demo.ble.common.DeviceInfo
import me.ycdev.android.lib.common.perms.PermissionCallback
import me.ycdev.android.lib.common.perms.PermissionUtils
import me.ycdev.android.lib.common.utils.DateTimeUtils
import timber.log.Timber
import java.util.*

class ScannerActivity : AppCompatActivity(), View.OnClickListener, PermissionCallback,
    BleScanner.ScanListener {

    @BindView(R.id.scan_btn)
    internal lateinit var mBleScanBtn: Button
    @BindView(R.id.status)
    internal lateinit var mStatusView: TextView
    @BindView(R.id.devices)
    internal lateinit var mListView: RecyclerView

    private lateinit var mAdapter: MyAdapter
    private val mDeviceComparator = DeviceInfo.TimestampComparator()

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        Objects.requireNonNull<ActionBar>(supportActionBar).setDisplayHomeAsUpEnabled(true)

        initContentViews()
    }

    private fun initContentViews() {
        ButterKnife.bind(this)

        mAdapter = MyAdapter(this)
        mAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                updateContentViews()
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                updateContentViews()
            }
        })

        mListView.adapter = mAdapter
        mListView.layoutManager = LinearLayoutManager(this)

        val itemDecoration = DividerItemDecoration(
            this,
            DividerItemDecoration.VERTICAL
        )
        mListView.addItemDecoration(itemDecoration)

        updateContentViews()
    }

    private fun updateContentViews() {
        if (BleScanner.instance.isScanning) {
            mBleScanBtn.setText(R.string.ble_scan_stop)
            mStatusView.text = getString(R.string.ble_status_scanning, mAdapter.itemCount)
        } else {
            mBleScanBtn.setText(R.string.ble_scan_start)
            mStatusView.text = getString(R.string.ble_status_scanning_done, mAdapter.itemCount)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_BT_ENABLE) {
            if (BluetoothHelper.isBluetoothEnabled(this)) {
                scanBleDevices()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, @NonNull permissions: Array<String>,
        @NonNull grantResults: IntArray
    ) {
        if (requestCode == RC_BLE_SCAN_PERMS) {
            if (PermissionUtils.verifyPermissions(grantResults)) {
                scanBleDevices()
            }
        }
    }

    override fun onRationaleDenied(requestCode: Int) {
        Timber.tag(TAG).i("Perms request was denied: %d", requestCode)
    }

    override fun onClick(v: View) {
        if (v === mBleScanBtn) {
            scanBleDevices()
        }
    }

    override fun onDeviceFound(@NonNull device: DeviceInfo, newDevice: Boolean) {
        val devices = BleScanner.instance.devices
        Collections.sort(devices, mDeviceComparator)
        mAdapter.submitList(devices)
    }

    @OnClick(R.id.scan_btn)
    internal fun scanBleDevices() {
        if (BleScanner.hasNoPermissions(this)) {
            BleScanner.requestPermsForBleScan(this, RC_BLE_SCAN_PERMS, this)
            return
        }

        if (!BluetoothHelper.isBluetoothEnabled(this)) {
            BluetoothHelper.startBluetoothSysUI(this, RC_BT_ENABLE)
            return
        }

        val scanning = BleScanner.instance.isScanning
        Timber.tag(TAG).d("BLE is scanning: %s", scanning)
        if (!scanning) {
            BleScanner.instance.startScanning(this)
        } else {
            BleScanner.instance.stopScanning()
        }
        updateContentViews()
    }

    private class MyDiffItemCallback : DiffUtil.ItemCallback<DeviceInfo>() {
        override fun areItemsTheSame(@NonNull oldItem: DeviceInfo, @NonNull newItem: DeviceInfo): Boolean {
            return oldItem.btAddress == newItem.btAddress
        }

        override fun areContentsTheSame(@NonNull oldItem: DeviceInfo, @NonNull newItem: DeviceInfo): Boolean {
            return oldItem.btAddress == newItem.btAddress && oldItem.scanResult.timestampNanos == newItem.scanResult.timestampNanos
        }
    }

    private class MyAdapter internal constructor(context: Context) :
        ListAdapter<DeviceInfo, MyViewHolder>(MyDiffItemCallback()) {
        private val mInflater: LayoutInflater = LayoutInflater.from(context)
        private val mUnknownDeviceName: String = context.getString(R.string.ble_unknown_device)

        @NonNull
        override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): MyViewHolder {
            val itemView = mInflater.inflate(R.layout.devices_list_item, parent, false)
            return MyViewHolder(itemView)
        }

        override fun onBindViewHolder(@NonNull holder: MyViewHolder, position: Int) {
            val item = getItem(position)
            val btDevice = item.scanResult.device

            var name = btDevice.name
            if (TextUtils.isEmpty(name)) {
                name = mUnknownDeviceName
            }
            holder.nameView.text = String.format(
                Locale.US, "#%1\$d %2\$s (%3\$d)",
                item.number, name, item.foundCount
            )
            holder.addressView.text = btDevice.address

            val timestamp =
                System.currentTimeMillis() - (SystemClock.elapsedRealtimeNanos() - item.scanResult.timestampNanos) / 1000000
            holder.timestampView.text = DateTimeUtils.getReadableTimeStamp(timestamp)
        }
    }

    internal class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        @BindView(R.id.name)
        internal lateinit var nameView: TextView
        @BindView(R.id.address)
        internal lateinit var addressView: TextView
        @BindView(R.id.timestamp)
        internal lateinit var timestampView: TextView

        init {
            ButterKnife.bind(this, itemView)
        }
    }

    companion object {
        private const val TAG = "bledemo.scanner"

        private const val RC_BLE_SCAN_PERMS = 100
        private const val RC_BT_ENABLE = 101
    }
}
