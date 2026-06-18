package me.ycdev.android.bluetooth.explorer

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.util.Objects
import me.ycdev.android.bluetooth.BluetoothHelper
import me.ycdev.android.bluetooth.explorer.databinding.ActivityPairedDevicesBinding
import me.ycdev.android.bluetooth.explorer.databinding.DevicesListItemBinding
import me.ycdev.android.bluetooth.explorer.utils.BluetoothDeviceUtils
import me.ycdev.android.lib.common.utils.MainHandler
import timber.log.Timber

class PairedDevicesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPairedDevicesBinding

    private lateinit var adapter: MyAdapter
    private var devices: MutableList<BluetoothDevice>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPairedDevicesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Timber.tag(TAG).d("onCreate")

        setSupportActionBar(binding.toolbar)
        Objects.requireNonNull<ActionBar>(supportActionBar).setDisplayHomeAsUpEnabled(true)

        initContentViews()
    }

    private fun initContentViews() {
        binding.content.refreshBtn.setOnClickListener { refreshPairedDevices() }

        adapter = MyAdapter(this)
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                updateContentViews()
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                updateContentViews()
            }
        })

        binding.content.devices.adapter = adapter
        binding.content.devices.layoutManager = LinearLayoutManager(this)

        val itemDecoration = DividerItemDecoration(
            this,
            DividerItemDecoration.VERTICAL
        )
        binding.content.devices.addItemDecoration(itemDecoration)

        binding.content.status.text = getString(R.string.ble_status_init)

        MainHandler.postDelayed({
            refreshPairedDevices()
        }, 500)
    }

    private fun updateContentViews() {
        val deviceCount = devices?.size ?: 0
        binding.content.status.text = resources.getQuantityString(
            R.plurals.ble_paired_viewer_status,
            deviceCount,
            Utils.getTimestamp(),
            deviceCount
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
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

    private fun refreshPairedDevices() {
        if (!BluetoothHelper.isBluetoothEnabled(this)) {
            BluetoothHelper.startBluetoothSysUI(this,
                RC_BT_ENABLE
            )
            return
        }

        devices = BluetoothDeviceUtils.getBondedDevices(this)
        adapter.submitList(devices)

        updateContentViews()
    }

    private class MyDiffItemCallback : DiffUtil.ItemCallback<BluetoothDevice>() {
        override fun areItemsTheSame(oldItem: BluetoothDevice, newItem: BluetoothDevice): Boolean {
            return oldItem.address == newItem.address
        }

        override fun areContentsTheSame(oldItem: BluetoothDevice, newItem: BluetoothDevice): Boolean {
            return oldItem.address == newItem.address
        }
    }

    private class MyAdapter internal constructor(val context: Context) :
        ListAdapter<BluetoothDevice, MyViewHolder>(
            MyDiffItemCallback()
        ) {

        private val inflater: LayoutInflater = LayoutInflater.from(context)
        private val unknownDeviceName: String = context.getString(R.string.ble_unknown_device)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val itemView = inflater.inflate(R.layout.devices_list_item, parent, false)
            return MyViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val item = getItem(position)
            var name = BluetoothDeviceUtils.getDeviceName(context, item)
            if (TextUtils.isEmpty(name)) {
                name = unknownDeviceName
            }
            holder.binding.name.text = name
            holder.binding.address.text = item.address

            holder.itemView.setOnClickListener {
                val intent = Intent(context, BleClientActivity::class.java)
                intent.putExtra(BleClientActivity.EXTRA_DEVICE, item)
                context.startActivity(intent)
            }
        }
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: DevicesListItemBinding = DevicesListItemBinding.bind(itemView)

        init {
            binding.timestamp.visibility = View.GONE
            binding.advertiseData.visibility = View.GONE
        }
    }

    companion object {
        private const val TAG = "PairedDevicesActivity"

        private const val RC_BT_ENABLE = 101
    }
}
