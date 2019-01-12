package me.ycdev.android.demo.ble

import android.bluetooth.le.ScanFilter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.ParcelUuid
import android.os.SystemClock
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.RadioButton
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
import me.ycdev.android.demo.ble.common.BleConstants
import me.ycdev.android.demo.ble.common.BluetoothHelper
import me.ycdev.android.demo.ble.common.client.BleScanner
import me.ycdev.android.demo.ble.common.client.DeviceInfo
import me.ycdev.android.demo.ble.common.server.MagicRadioProfile
import me.ycdev.android.demo.ble.common.server.TimeServiceProfile
import me.ycdev.android.lib.common.perms.PermissionCallback
import me.ycdev.android.lib.common.perms.PermissionUtils
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.Objects

class ScannerActivity : AppCompatActivity(), View.OnClickListener, PermissionCallback,
    BleScanner.ScanListener {
    @BindView(R.id.only_named_devices)
    internal lateinit var onlyNamedDevicesCheckbox: CheckBox
    @BindView(R.id.scan_btn)
    internal lateinit var leScanBtn: Button
    @BindView(R.id.status)
    internal lateinit var statusView: TextView
    @BindView(R.id.devices)
    internal lateinit var listView: RecyclerView

    private lateinit var selectedFilterCheckBox: RadioButton

    private lateinit var adapter: MyAdapter
    private val deviceComparator = DeviceInfo.TimestampComparator()

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)
        Timber.tag(TAG).d("onCreate")

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        Objects.requireNonNull<ActionBar>(supportActionBar).setDisplayHomeAsUpEnabled(true)

        initContentViews()
    }

    private fun initContentViews() {
        ButterKnife.bind(this)

        selectedFilterCheckBox = findViewById(R.id.filter_all)
        selectedFilterCheckBox.isChecked = true

        adapter = MyAdapter(this)
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                updateContentViews()
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                updateContentViews()
            }
        })

        listView.adapter = adapter
        listView.layoutManager = LinearLayoutManager(this)

        val itemDecoration = DividerItemDecoration(
            this,
            DividerItemDecoration.VERTICAL
        )
        listView.addItemDecoration(itemDecoration)

        updateContentViews()
    }

    private fun updateContentViews() {
        if (BleScanner.instance.isScanning) {
            leScanBtn.setText(R.string.ble_scan_stop)
            statusView.text = getString(
                R.string.ble_status_scanning,
                selectedFilterCheckBox.text,
                adapter.itemCount
            )
        } else {
            leScanBtn.setText(R.string.ble_scan_start)
            statusView.text = getString(R.string.ble_status_scanning_done, adapter.itemCount)
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
        requestCode: Int,
        @NonNull permissions: Array<String>,
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
        if (v === leScanBtn) {
            scanBleDevices()
        }
    }

    @OnClick(
        R.id.filter_all,
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

    override fun onDestroy() {
        super.onDestroy()
        Timber.tag(TAG).d("onDestroy")
        BleScanner.instance.stopScanning()
    }

    override fun onDeviceFound(@NonNull device: DeviceInfo, newDevice: Boolean) {
        val devices = BleScanner.instance.devices
        Collections.sort(devices, deviceComparator)
        adapter.submitList(devices)
    }

    private fun buildScanFiltersForMagicWw(): List<ScanFilter> {
        val filters = ArrayList<ScanFilter>()
        filters.add(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(BleConstants.SERVICE_MAGIC_WW))
                .build()
        )
        return filters
    }

    private fun buildScanFiltersForMagicFuture(): List<ScanFilter> {
        val filters = ArrayList<ScanFilter>()
        filters.add(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(BleConstants.SERVICE_MAGIC_FUTURE))
                .build()
        )
        return filters
    }

    private fun buildScanFiltersForMagicRadio(): List<ScanFilter> {
        val filters = ArrayList<ScanFilter>()
        filters.add(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(MagicRadioProfile.RADIO_SERVICE))
                .build()
        )
        return filters
    }

    private fun buildScanFiltersForTimeService(): List<ScanFilter> {
        val filters = ArrayList<ScanFilter>()
        filters.add(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(TimeServiceProfile.TIME_SERVICE))
                .build()
        )
        return filters
    }

    private fun setupScanFilter() {
        when (selectedFilterCheckBox.id) {
            R.id.filter_all -> BleScanner.instance.setScanFilters(null)
            R.id.filter_magic_ww -> BleScanner.instance.setScanFilters(buildScanFiltersForMagicWw())
            R.id.filter_magic_future -> BleScanner.instance.setScanFilters(buildScanFiltersForMagicFuture())
            R.id.filter_magic_radio -> BleScanner.instance.setScanFilters(buildScanFiltersForMagicRadio())
            R.id.filter_time_service -> BleScanner.instance.setScanFilters(buildScanFiltersForTimeService())
            else -> throw RuntimeException("Unknown filter")
        }

        BleScanner.instance.setOnlyNamedDevices(onlyNamedDevicesCheckbox.isChecked)
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
            setupScanFilter()
            adapter.submitList(null)
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

    private class MyAdapter internal constructor(val context: Context) :
        ListAdapter<DeviceInfo, MyViewHolder>(MyDiffItemCallback()) {
        private val timestampFormatter = SimpleDateFormat("dd-HH:mm:ss", Locale.US)

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

            val timestamp = System.currentTimeMillis() -
                    (SystemClock.elapsedRealtimeNanos() - item.scanResult.timestampNanos) / 1000000
            holder.timestampView.text = timestampFormatter.format(Date(timestamp))

            holder.itemView.setOnClickListener {
                // stop BLE scan first
                BleScanner.instance.stopScanning()

                val intent: Intent = Intent(context, BleClientActivity::class.java)
                intent.putExtra(BleClientActivity.EXTRA_DEVICE, btDevice)
                context.startActivity(intent)
            }
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
        private const val TAG = "ScannerActivity"

        private const val RC_BLE_SCAN_PERMS = 100
        private const val RC_BT_ENABLE = 101
    }
}
