package me.ycdev.android.bluetooth.explorer

import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
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
import me.ycdev.android.bluetooth.BluetoothHelper
import me.ycdev.android.bluetooth.ble.BleAdvertiseData.Parser
import me.ycdev.android.bluetooth.ble.client.BleScanner
import me.ycdev.android.bluetooth.explorer.BleClientActivity.ClientType
import me.ycdev.android.bluetooth.explorer.BleClientActivity.ClientType.DEFAULT
import me.ycdev.android.bluetooth.explorer.ble.BleHelper
import me.ycdev.android.bluetooth.explorer.ble.BleScanInfo
import me.ycdev.android.bluetooth.explorer.ble.device.BatteryService
import me.ycdev.android.bluetooth.explorer.ble.device.BleDevice
import me.ycdev.android.bluetooth.explorer.ble.device.DefaultDevice
import me.ycdev.android.bluetooth.explorer.ble.device.GfpService
import me.ycdev.android.bluetooth.explorer.ble.device.MagicPing
import me.ycdev.android.bluetooth.explorer.ble.device.MagicRadio
import me.ycdev.android.bluetooth.explorer.ble.device.MagicWw
import me.ycdev.android.bluetooth.explorer.ble.device.MfpService
import me.ycdev.android.bluetooth.explorer.ble.device.TimeService
import me.ycdev.android.bluetooth.explorer.ble.device.Tp2
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

    @BindView(R2.id.only_named_devices)
    internal lateinit var onlyNamedDevicesCheckbox: CheckBox
    @BindView(R2.id.scan_record_filter)
    internal lateinit var scanRecordFilterEditor: EditText
    @BindView(R2.id.scan_btn)
    internal lateinit var leScanBtn: Button
    @BindView(R2.id.status)
    internal lateinit var statusView: TextView
    @BindView(R2.id.devices)
    internal lateinit var listView: RecyclerView

    private lateinit var selectedFilterCheckBox: RadioButton

    private lateinit var adapter: MyAdapter
    private val deviceComparator =
        TimestampComparator()

    private val bleDevices = hashMapOf<String, BleDevice>()
    private var curDevice: BleDevice? = null

    init {
        bleDevices[DefaultDevice.ID] = DefaultDevice()
        bleDevices[BatteryService.ID] = BatteryService()
        bleDevices[GfpService.ID] = GfpService()
        bleDevices[MagicPing.ID] = MagicPing()
        bleDevices[MagicRadio.ID] = MagicRadio()
        bleDevices[MagicWw.ID] = MagicWw()
        bleDevices[MfpService.ID] = MfpService()
        bleDevices[TimeService.ID] = TimeService()
        bleDevices[Tp2.ID] = Tp2()
    }

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
        R2.id.filter_all,
        R2.id.filter_magic_ww,
        R2.id.filter_magic_ping,
        R2.id.filter_magic_radio,
        R2.id.filter_gfp_service,
        R2.id.filter_mfp_service,
        R2.id.filter_time_service,
        R2.id.filter_battery_service,
        R2.id.filter_tp2
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
        val scanInfo = BleScanInfo(result)
        val filter = scanRecordFilterEditor.text.toString()
        if (!curDevice!!.checkScanResult(result, filter)) {
            if (AppDebug.bleScan) {
                Timber.tag(TAG).d("ignored the scan record: %s", scanInfo.rawData)
            }
            return
        }

        Timber.tag(TAG).d("ScanRecordRawData: %s", scanInfo.rawData)
        adapter.addItem(scanInfo)
    }

    private fun getSelectedDevice(): BleDevice {
        val deviceId = when (selectedFilterCheckBox.id) {
            R.id.filter_all -> DefaultDevice.ID
            R.id.filter_magic_ww -> MagicWw.ID
            R.id.filter_magic_ping -> MagicPing.ID
            R.id.filter_magic_radio -> MagicRadio.ID
            R.id.filter_gfp_service -> GfpService.ID
            R.id.filter_mfp_service -> MfpService.ID
            R.id.filter_time_service -> TimeService.ID
            R.id.filter_battery_service -> BatteryService.ID
            R.id.filter_tp2 -> Tp2.ID
            else -> throw RuntimeException("Unknown device filter")
        }
        return bleDevices[deviceId] ?: throw RuntimeException("Device[$deviceId] not found")
    }

    private fun setupScanFilter() {
        val device = getSelectedDevice()
        bleScanner.setScanFilters(device.buildBleScanFilter())
        bleScanner.setOnlyNamedDevices(onlyNamedDevicesCheckbox.isChecked)
        curDevice = device
    }

    private fun getClientType(): Int {
        return when (selectedFilterCheckBox.id) {
            R.id.filter_magic_ping -> ClientType.MAGIC_PING
            R.id.filter_magic_radio -> ClientType.MAGIC_RADIO
            R.id.filter_battery_service -> ClientType.GSS_BATTERY_SERVICE
            R.id.filter_tp2 -> ClientType.GSS_BATTERY_SERVICE
            else -> DEFAULT
        }
    }

    @OnClick(R2.id.scan_btn)
    internal fun scanBleDevices() {
        if (BleScanner.hasNoPermissions(this)) {
            BleHelper.requestPermsForBleScan(this,
                RC_BLE_SCAN_PERMS, this)
            return
        }

        if (!BluetoothHelper.isBluetoothEnabled(this)) {
            BluetoothHelper.startBluetoothSysUI(this,
                RC_BT_ENABLE
            )
            return
        }

        val scanning = bleScanner.isScanning
        Timber.tag(TAG).d("BLE is scanning: %s", scanning)
        if (!scanning) {
            setupScanFilter()
            adapter.clearData()
            bleScanner.startScanning(this)
        } else {
            bleScanner.stopScanning()
        }
        updateContentViews()
    }

    private class MyDiffItemCallback : DiffUtil.ItemCallback<BleScanInfo>() {
        override fun areItemsTheSame(@NonNull oldItem: BleScanInfo, @NonNull newItem: BleScanInfo): Boolean {
            return oldItem.scanResult.device.address == newItem.scanResult.device.address
        }

        override fun areContentsTheSame(@NonNull oldItem: BleScanInfo, @NonNull newItem: BleScanInfo): Boolean {
            return oldItem.scanResult.device.address == newItem.scanResult.device.address &&
                    oldItem.scanResult.timestampNanos == newItem.scanResult.timestampNanos
        }
    }

    private inner class MyAdapter internal constructor(val context: Context) :
        ListAdapter<BleScanInfo, MyViewHolder>(
            MyDiffItemCallback()
        ) {
        private val timestampFormatter = SimpleDateFormat("yyyyMMdd-HH:mm:ss", Locale.US)

        private val inflater: LayoutInflater = LayoutInflater.from(context)
        private val unknownDeviceName: String = context.getString(R.string.ble_unknown_device)
        private val noScanRecordTips: String = context.getString(R.string.ble_no_scan_record)

        private val scanResults = HashMap<String, BleScanInfo>()

        fun clearData() {
            scanResults.clear()
            submitList(null)
            notifyDataSetChanged()
        }

        fun addItem(item: BleScanInfo) {
            scanResults[item.scanResult.device.address] = item
            val data = ArrayList<BleScanInfo>(scanResults.values)
            Collections.sort(data, deviceComparator)
            submitList(data)
            notifyDataSetChanged()
        }

        @NonNull
        override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): MyViewHolder {
            val itemView = inflater.inflate(R.layout.devices_list_item, parent, false)
            return MyViewHolder(itemView)
        }

        override fun onBindViewHolder(@NonNull holder: MyViewHolder, position: Int) {
            val item = getItem(position)
            val btDevice = item.scanResult.device

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
                    (SystemClock.elapsedRealtimeNanos() - item.scanResult.timestampNanos) / 1000000
            holder.timestampView.text = timestampFormatter.format(Date(timestamp))

            val scanRecord = item.scanResult.scanRecord
            if (scanRecord != null) {
                val advertiseData = Parser().parse(scanRecord.bytes)
                val sb = StringBuilder()
                sb.append("ScanRecordRawData: ").append(item.rawData)
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
                        .append(", data: ")
                        .append(curDevice!!.getReadableManufacturerData(id, data))
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
        @BindView(R2.id.name)
        internal lateinit var nameView: TextView
        @BindView(R2.id.address)
        internal lateinit var addressView: TextView
        @BindView(R2.id.timestamp)
        internal lateinit var timestampView: TextView
        @BindView(R2.id.advertise_data)
        internal lateinit var advertiseDataView: TextView

        init {
            ButterKnife.bind(this, itemView)
        }
    }

    class TimestampComparator : Comparator<BleScanInfo> {
        override fun compare(lhs: BleScanInfo, rhs: BleScanInfo): Int {
            if (lhs.scanResult.timestampNanos < rhs.scanResult.timestampNanos) {
                return 1
            } else if (lhs.scanResult.timestampNanos > rhs.scanResult.timestampNanos) {
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
