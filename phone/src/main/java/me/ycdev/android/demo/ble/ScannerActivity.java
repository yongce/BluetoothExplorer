package me.ycdev.android.demo.ble;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.ycdev.android.demo.ble.common.BleScanner;
import me.ycdev.android.demo.ble.common.BluetoothHelper;
import me.ycdev.android.demo.ble.common.DeviceInfo;
import me.ycdev.android.lib.common.perms.PermissionCallback;
import me.ycdev.android.lib.common.perms.PermissionUtils;
import me.ycdev.android.lib.common.utils.DateTimeUtils;
import timber.log.Timber;

public class ScannerActivity extends AppCompatActivity implements View.OnClickListener,
        PermissionCallback, BleScanner.ScanListener {
    private static final String TAG = "bledemo.scanner";

    private static final int RC_BLE_SCAN_PERMS = 100;
    private static final int RC_BT_ENABLE = 101;

    @BindView(R2.id.scan_btn)
    Button mBleScanBtn;
    @BindView(R2.id.status)
    TextView mStatusView;
    @BindView(R2.id.devices)
    RecyclerView mListView;

    private MyAdapter mAdapter;
    private DeviceInfo.TimestampComparator mDeviceComparator = new DeviceInfo.TimestampComparator();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        initContentViews();
    }

    private void initContentViews() {
        ButterKnife.bind(this);

        mAdapter = new MyAdapter(this);
        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                updateContentViews();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                updateContentViews();
            }
        });

        mListView.setAdapter(mAdapter);
        mListView.setLayoutManager(new LinearLayoutManager(this));

        DividerItemDecoration itemDecoration = new DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL);
        mListView.addItemDecoration(itemDecoration);

        updateContentViews();
    }

    private void updateContentViews() {
        if (BleScanner.getInstance().isScanning()) {
            mBleScanBtn.setText(R.string.ble_scan_stop);
            mStatusView.setText(getString(R.string.ble_status_scanning, mAdapter.getItemCount()));
        } else {
            mBleScanBtn.setText(R.string.ble_scan_start);
            mStatusView.setText(getString(R.string.ble_status_scanning_done, mAdapter.getItemCount()));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BT_ENABLE) {
            if (BluetoothHelper.isBluetoothEnabled(this)) {
                scanBleDevices();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (requestCode == RC_BLE_SCAN_PERMS) {
            if (PermissionUtils.verifyPermissions(grantResults)) {
                scanBleDevices();
            }
        }
    }

    @Override
    public void onRationaleDenied(int requestCode) {
        Timber.tag(TAG).i("Perms request was denied: %d", requestCode);
    }

    @Override
    public void onClick(View v) {
        if (v == mBleScanBtn) {
            scanBleDevices();
        }
    }

    @Override
    public void onDeviceFound(@NonNull DeviceInfo device, boolean newDevice) {
        List<DeviceInfo> devices = BleScanner.getInstance().getDevices();
        Collections.sort(devices, mDeviceComparator);
        mAdapter.submitList(devices);
    }

    @OnClick(R2.id.scan_btn)
    void scanBleDevices() {
        if (BleScanner.hasNoPermissions(this)) {
            BleScanner.requestPermsForBleScan(this, RC_BLE_SCAN_PERMS, this);
            return;
        }

        if (!BluetoothHelper.isBluetoothEnabled(this)) {
            BluetoothHelper.startBluetoothSysUI(this, RC_BT_ENABLE);
            return;
        }

        boolean scanning = BleScanner.getInstance().isScanning();
        Timber.tag(TAG).d("BLE is scanning: %s", scanning);
        if (!scanning) {
            BleScanner.getInstance().startScanning(this);
        } else {
            BleScanner.getInstance().stopScanning();
        }
        updateContentViews();
    }

    private static class MyDiffItemCallback extends DiffUtil.ItemCallback<DeviceInfo> {
        @Override
        public boolean areItemsTheSame(@NonNull DeviceInfo oldItem, @NonNull DeviceInfo newItem) {
            return oldItem.btAddress.equals(newItem.btAddress);
        }

        @Override
        public boolean areContentsTheSame(@NonNull DeviceInfo oldItem, @NonNull DeviceInfo newItem) {
            return oldItem.btAddress.equals(newItem.btAddress)
                    && oldItem.scanResult.getTimestampNanos() == newItem.scanResult.getTimestampNanos();
        }
    }

    private static class MyAdapter extends ListAdapter<DeviceInfo, MyViewHolder> {
        private LayoutInflater mInflater;
        private String mUnknownDeviceName;

        MyAdapter(Context context) {
            super(new MyDiffItemCallback());
            mInflater = LayoutInflater.from(context);
            mUnknownDeviceName = context.getString(R.string.ble_unknown_device);
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = mInflater.inflate(R.layout.devices_list_item, parent, false);
            return new MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            DeviceInfo item = getItem(position);
            BluetoothDevice btDevice = item.scanResult.getDevice();

            String name = btDevice.getName();
            if (TextUtils.isEmpty(name)) {
                name = mUnknownDeviceName;
            }
            holder.nameView.setText(String.format(Locale.US, "#%1$d %2$s (%3$d)",
                    item.number, name, item.foundCount));
            holder.addressView.setText(btDevice.getAddress());

            long timestamp = System.currentTimeMillis() -
                    (SystemClock.elapsedRealtimeNanos() - item.scanResult.getTimestampNanos()) / 1000000;
            holder.timestampView.setText(DateTimeUtils.getReadableTimeStamp(timestamp));
        }
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        @BindView(R2.id.name)
        TextView nameView;
        @BindView(R2.id.address)
        TextView addressView;
        @BindView(R2.id.timestamp)
        TextView timestampView;

        MyViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
