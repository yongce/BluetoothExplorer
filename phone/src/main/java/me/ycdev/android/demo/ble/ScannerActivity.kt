package me.ycdev.android.demo.ble

import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
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
import androidx.core.util.forEach
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import me.ycdev.android.ble.common.BleAdvertiseData
import me.ycdev.android.ble.common.BluetoothHelper
import me.ycdev.android.ble.common.client.BleScanner
import me.ycdev.android.ble.common.ext.MagicPingProfile
import me.ycdev.android.ble.common.ext.MagicRadioProfile
import me.ycdev.android.ble.common.sig.BatteryServiceProfile
import me.ycdev.android.ble.common.sig.TimeServiceProfile
import me.ycdev.android.demo.ble.BleClientActivity.ClientType
import me.ycdev.android.demo.ble.BleClientActivity.ClientType.DEFAULT
import me.ycdev.android.demo.ble.common.BleDemoConstants
import me.ycdev.android.demo.ble.common.BleDemoHelper
import me.ycdev.android.lib.common.perms.PermissionCallback
import me.ycdev.android.lib.common.perms.PermissionUtils
import me.ycdev.android.lib.common.utils.EncodingUtils.encodeWithHex
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Comparator
import java.util.Date
import java.util.Locale
import java.util.Objects

class ScannerActivity : AppCompatActivity(), View.OnClickListener, PermissionCallback,
    BleScanner.ScanListener {
    private lateinit var bleScanner: BleScanner

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
    private val deviceComparator = TimestampComparator()

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)
        Timber.tag(TAG).d("onCreate")

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        Objects.requireNonNull<ActionBar>(supportActionBar).setDisplayHomeAsUpEnabled(true)

        bleScanner = BleScanner(this)

        initContentViews()
    }

    private fun initContentViews() {
        ButterKnife.bind(this)

        selectedFilterCheckBox = findViewById(R.id.filter_magic_ping)
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
        if (bleScanner.isScanning) {
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
        R.id.filter_magic_ping,
        R.id.filter_magic_radio,
        R.id.filter_gfp_service,
        R.id.filter_mfp_service,
        R.id.filter_time_service,
        R.id.filter_battery_service
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
        bleScanner.stopScanning()
    }

    override fun onDeviceFound(@NonNull result: ScanResult) {
        Timber.tag(TAG).d("ScanRecordRawData: %s", encodeWithHex(result.scanRecord?.bytes))
        val devices = bleScanner.devices
        Collections.sort(devices, deviceComparator)
        adapter.submitList(devices)
    }

    private fun buildScanFiltersForMagicWw(): List<ScanFilter> {
        val filters = ArrayList<ScanFilter>()
        filters.add(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(BleDemoConstants.SERVICE_MAGIC_WW))
                .build()
        )
        return filters
    }

    private fun buildScanFiltersForMagicPing(): List<ScanFilter> {
        val filters = ArrayList<ScanFilter>()
        filters.add(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(MagicPingProfile.PING_SERVICE))
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

    private fun buildScanFiltersForGfpService(): List<ScanFilter> {
        val filters = arrayListOf<ScanFilter>()
        filters.add(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(BleDemoConstants.SERVICE_GFP))
                .build()
        )
        return filters
    }

    private fun buildScanFiltersForMfpService(): List<ScanFilter> {
        val filters = arrayListOf<ScanFilter>()
        val dataMask = byteArrayOf(0x00)
        filters.add(
            ScanFilter.Builder()
                .setServiceData(ParcelUuid(BleDemoConstants.SERVICE_MFP), dataMask, dataMask)
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

    private fun buildScanFiltersForBatteryService(): List<ScanFilter> {
        val filters = ArrayList<ScanFilter>()
        filters.add(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(BatteryServiceProfile.BATTERY_SERVICE))
                .build()
        )
        return filters
    }

    private fun setupScanFilter() {
        when (selectedFilterCheckBox.id) {
            R.id.filter_all -> bleScanner.setScanFilters(null)
            R.id.filter_magic_ww -> bleScanner.setScanFilters(buildScanFiltersForMagicWw())
            R.id.filter_magic_ping -> bleScanner.setScanFilters(buildScanFiltersForMagicPing())
            R.id.filter_magic_radio -> bleScanner.setScanFilters(buildScanFiltersForMagicRadio())
            R.id.filter_gfp_service -> bleScanner.setScanFilters(buildScanFiltersForGfpService())
            R.id.filter_mfp_service -> bleScanner.setScanFilters(buildScanFiltersForMfpService())
            R.id.filter_time_service -> bleScanner.setScanFilters(buildScanFiltersForTimeService())
            R.id.filter_battery_service -> bleScanner.setScanFilters(buildScanFiltersForBatteryService())
            else -> throw RuntimeException("Unknown filter")
        }

        bleScanner.setOnlyNamedDevices(onlyNamedDevicesCheckbox.isChecked)
    }

    private fun getClientType(): Int {
        return when (selectedFilterCheckBox.id) {
            R.id.filter_magic_ping -> ClientType.MAGIC_PING
            R.id.filter_magic_radio -> ClientType.MAGIC_RADIO
            R.id.filter_battery_service -> ClientType.GSS_BATTERY_SERVICE
            else -> DEFAULT
        }
    }

    @OnClick(R.id.scan_btn)
    internal fun scanBleDevices() {
        if (BleScanner.hasNoPermissions(this)) {
            BleDemoHelper.requestPermsForBleScan(this, RC_BLE_SCAN_PERMS, this)
            return
        }

        if (!BluetoothHelper.isBluetoothEnabled(this)) {
            BluetoothHelper.startBluetoothSysUI(this, RC_BT_ENABLE)
            return
        }

        val scanning = bleScanner.isScanning
        Timber.tag(TAG).d("BLE is scanning: %s", scanning)
        if (!scanning) {
            setupScanFilter()
            adapter.submitList(null)
            bleScanner.startScanning(this)
        } else {
            bleScanner.stopScanning()
        }
        updateContentViews()
    }

    private class MyDiffItemCallback : DiffUtil.ItemCallback<ScanResult>() {
        override fun areItemsTheSame(@NonNull oldItem: ScanResult, @NonNull newItem: ScanResult): Boolean {
            return oldItem.device.address == newItem.device.address
        }

        override fun areContentsTheSame(@NonNull oldItem: ScanResult, @NonNull newItem: ScanResult): Boolean {
            return oldItem.device.address == newItem.device.address &&
                    oldItem.timestampNanos == newItem.timestampNanos
        }
    }

    private inner class MyAdapter internal constructor(val context: Context) :
        ListAdapter<ScanResult, MyViewHolder>(MyDiffItemCallback()) {
        private val timestampFormatter = SimpleDateFormat("dd-HH:mm:ss", Locale.US)

        private val inflater: LayoutInflater = LayoutInflater.from(context)
        private val unknownDeviceName: String = context.getString(R.string.ble_unknown_device)
        private val noScanRecordTips: String = context.getString(R.string.ble_no_scan_record)

        @NonNull
        override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): MyViewHolder {
            val itemView = inflater.inflate(R.layout.devices_list_item, parent, false)
            return MyViewHolder(itemView)
        }

        override fun onBindViewHolder(@NonNull holder: MyViewHolder, position: Int) {
            val item = getItem(position)
            val btDevice = item.device

            var name = btDevice.name
            if (TextUtils.isEmpty(name)) {
                name = unknownDeviceName
            }
            holder.nameView.text = String.format(
                Locale.US, "%s",
                name
            )
            holder.addressView.text = btDevice.address

            val timestamp = System.currentTimeMillis() -
                    (SystemClock.elapsedRealtimeNanos() - item.timestampNanos) / 1000000
            holder.timestampView.text = timestampFormatter.format(Date(timestamp))

            val scanRecord = item.scanRecord
            if (scanRecord != null) {
                val advertiseData = BleAdvertiseData.Parser().parse(scanRecord.bytes)
                val sb = StringBuilder()
                sb.append("ScanRecordRawData: ").append(encodeWithHex(scanRecord.bytes))
                sb.append("\nPayload size: ").append(advertiseData.payloadSize)
                if (scanRecord.advertiseFlags != -1) {
                    sb.append("\nAdvertiseFlags: 0x").append(scanRecord.advertiseFlags.toString(16))
                }
                if (scanRecord.txPowerLevel != Int.MIN_VALUE) {
                    sb.append("\nTxPowerLevel: ").append(scanRecord.txPowerLevel)
                }
                if (scanRecord.deviceName != null) {
                    sb.append("\nDeviceName: ").append(scanRecord.deviceName)
                }
                if (scanRecord.serviceUuids != null) {
                    sb.append(scanRecord.serviceUuids.joinToString("") { "\nService UUID: $it" })
                }
                for ((uuid, data) in scanRecord.serviceData) {
                    sb.append("\nService with data, UUID: ").append(uuid)
                        .append(", data: ").append(encodeWithHex(data))
                }
                scanRecord.manufacturerSpecificData.forEach { id, data ->
                    sb.append("\nManufacturer ID: 0x").append(String.format("%04x", id))
                        .append(", data: ").append(encodeWithHex(data))
                }

                holder.advertiseDataView.text = sb.toString()
            } else {
                holder.advertiseDataView.text = noScanRecordTips
            }

            holder.itemView.setOnClickListener {
                // stop BLE scan first
                bleScanner.stopScanning()
                updateContentViews()

                val intent = Intent(context, BleClientActivity::class.java)
                intent.putExtra(BleClientActivity.EXTRA_DEVICE, btDevice)
                intent.putExtra(BleClientActivity.EXTRA_CLIENT_TYPE, getClientType())
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
        @BindView(R.id.advertise_data)
        internal lateinit var advertiseDataView: TextView

        init {
            ButterKnife.bind(this, itemView)
        }
    }

    class TimestampComparator : Comparator<ScanResult> {
        override fun compare(lhs: ScanResult, rhs: ScanResult): Int {
            if (lhs.timestampNanos < rhs.timestampNanos) {
                return 1
            } else if (lhs.timestampNanos > rhs.timestampNanos) {
                return -1
            }
            return 0
        }
    }

    companion object {
        private const val TAG = "ScannerActivity"

        private const val RC_BLE_SCAN_PERMS = 100
        private const val RC_BT_ENABLE = 101
    }
}
