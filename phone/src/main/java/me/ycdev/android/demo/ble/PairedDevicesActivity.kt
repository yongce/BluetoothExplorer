package me.ycdev.android.demo.ble

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import me.ycdev.android.demo.ble.common.BluetoothHelper
import me.ycdev.android.lib.common.utils.MainHandler
import timber.log.Timber
import java.util.Objects

class PairedDevicesActivity : AppCompatActivity() {
    @BindView(R.id.refresh_btn)
    internal lateinit var refreshBtn: Button
    @BindView(R.id.status)
    internal lateinit var statusView: TextView
    @BindView(R.id.devices)
    internal lateinit var listView: RecyclerView

    private lateinit var adapter: MyAdapter
    private var devices: MutableList<BluetoothDevice>? = null

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paired_devices)
        Timber.tag(TAG).d("onCreate")

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        Objects.requireNonNull<ActionBar>(supportActionBar).setDisplayHomeAsUpEnabled(true)

        initContentViews()
    }

    private fun initContentViews() {
        ButterKnife.bind(this)

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

        statusView.text = getString(R.string.ble_status_init)

        MainHandler.postDelayed({
            refreshPairedDevices()
        }, 500)
    }

    private fun updateContentViews() {
        statusView.text = getString(
            R.string.ble_paired_viewer_status,
            Utils.getTimestamp(), (devices?.size ?: 0)
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_BT_ENABLE) {
            if (BluetoothHelper.isBluetoothEnabled(this)) {
                refreshPairedDevices()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.tag(TAG).d("onDestroy")
    }

    @OnClick(R.id.refresh_btn)
    internal fun refreshPairedDevices() {
        if (!BluetoothHelper.isBluetoothEnabled(this)) {
            BluetoothHelper.startBluetoothSysUI(this, RC_BT_ENABLE)
            return
        }

        devices = BluetoothHelper.getBluetoothAdapter(this)?.bondedDevices?.toMutableList()
        adapter.submitList(devices)

        updateContentViews()
    }

    private class MyDiffItemCallback : DiffUtil.ItemCallback<BluetoothDevice>() {
        override fun areItemsTheSame(@NonNull oldItem: BluetoothDevice, @NonNull newItem: BluetoothDevice): Boolean {
            return oldItem.address == newItem.address
        }

        override fun areContentsTheSame(@NonNull oldItem: BluetoothDevice, @NonNull newItem: BluetoothDevice): Boolean {
            return oldItem.address == newItem.address
        }
    }

    private class MyAdapter internal constructor(val context: Context) :
        ListAdapter<BluetoothDevice, MyViewHolder>(MyDiffItemCallback()) {

        private val mInflater: LayoutInflater = LayoutInflater.from(context)
        private val mUnknownDeviceName: String = context.getString(R.string.ble_unknown_device)

        @NonNull
        override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): MyViewHolder {
            val itemView = mInflater.inflate(R.layout.devices_list_item, parent, false)
            return MyViewHolder(itemView)
        }

        override fun onBindViewHolder(@NonNull holder: MyViewHolder, position: Int) {
            val item = getItem(position)
            var name = item.name
            if (TextUtils.isEmpty(name)) {
                name = mUnknownDeviceName
            }
            holder.nameView.text = name
            holder.addressView.text = item.address

            holder.itemView.setOnClickListener {
                val intent = Intent(context, BleClientActivity::class.java)
                intent.putExtra(BleClientActivity.EXTRA_DEVICE, item)
                context.startActivity(intent)
            }
        }
    }

    internal class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        @BindView(R.id.name)
        internal lateinit var nameView: TextView
        @BindView(R.id.address)
        internal lateinit var addressView: TextView

        init {
            ButterKnife.bind(this, itemView)
        }
    }

    companion object {
        private const val TAG = "PairedDevicesActivity"

        private const val RC_BT_ENABLE = 101
    }
}
