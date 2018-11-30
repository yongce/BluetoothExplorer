package me.ycdev.android.demo.ble.common;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.ParcelUuid;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.ycdev.android.lib.common.perms.PermissionCallback;
import me.ycdev.android.lib.common.perms.PermissionRequestParams;
import me.ycdev.android.lib.common.perms.PermissionUtils;
import me.ycdev.android.lib.common.utils.ApplicationUtils;
import timber.log.Timber;

public class BleScanner {
    private static final String TAG = "BleScanner";

    public interface ScanListener {
        void onDeviceFound(@NonNull DeviceInfo device, boolean newDevice);
    }

    private static final String[] BLE_SCAN_PERMS = new String[] {
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    @SuppressLint("StaticFieldLeak")
    private static BleScanner sInstance = new BleScanner();

    private Context mAppContext;
    private BluetoothLeScanner mLeScanner;
    private ScanCallback mScanCallback = new MyScanCallback();
    private boolean mScanning;
    private ScanListener mListener;

    private Map<String, DeviceInfo> mAllScanResults = new HashMap<>();
    private int mNextNumber = 1;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    public static BleScanner getInstance() {
        return sInstance;
    }

    private BleScanner() {
        mAppContext = ApplicationUtils.getApplicationContext();
    }

    public boolean isScanning() {
        return mScanning;
    }

    public List<DeviceInfo> getDevices() {
        return new ArrayList<>(mAllScanResults.values());
    }

    public static boolean hasNoPermissions(@NonNull Context context) {
        return !PermissionUtils.hasPermissions(context, BLE_SCAN_PERMS);
    }

    public static void requestPermsForBleScan(@NonNull Activity activity, int requestCode,
            @NonNull PermissionCallback callback) {
        Timber.tag(TAG).d("Request perms for BLE scan");
        PermissionRequestParams params = new PermissionRequestParams();
        params.requestCode = requestCode;
        params.permissions = BLE_SCAN_PERMS;
        params.rationaleContent = activity.getString(R.string.ble_scan_perm_rationale);
        params.callback = callback;
        PermissionUtils.requestPermissions(activity, params);
    }

    @MainThread
    public boolean startScanning(ScanListener listener) {
        if (mLeScanner == null) {
            if (!BluetoothHelper.supportBle(mAppContext)) {
                Timber.tag(TAG).d("This device doesn't support BLE");
                return false;
            }
            BluetoothAdapter btAdapter = BluetoothHelper.getBluetoothAdapter(mAppContext);
            if (btAdapter == null) {
                Timber.tag(TAG).w("Failed to get BluetoothAdapter");
                return false;
            }
            if (!btAdapter.isEnabled()) {
                Timber.tag(TAG).w("Bluetooth not enabled");
                return false;
            }

            mLeScanner = btAdapter.getBluetoothLeScanner();
            if (mLeScanner == null) {
                Timber.tag(TAG).w("Failed to get BLE scanner");
                return false;
            }
        }

        if (hasNoPermissions(mAppContext)) {
            Timber.tag(TAG).w("No location permissions");
            return false;
        }

        Timber.tag(TAG).d("Start scanning");
        if (!mScanning) {
            mAllScanResults.clear();
            mLeScanner.startScan(buildScanFilters(), buildScanSettings(), mScanCallback);
            mScanning = true;
            mListener = listener;
            mNextNumber = 1;
            return true;
        } else {
            Timber.tag(TAG).w("Scanning was already started");
            return false;
        }
    }

    public void stopScanning() {
        Timber.tag(TAG).d("Stop scanning");
        if (mScanning) {
            mLeScanner.stopScan(mScanCallback);
            mListener = null;
            mScanning = false;
        }
    }

    private ScanSettings buildScanSettings() {
        return new ScanSettings.Builder().build();
    }

    private List<ScanFilter> buildScanFilters() {
        List<ScanFilter> filters = new ArrayList<>();
//        filters.add(new ScanFilter.Builder()
//                .setDeviceAddress("41:B0:87:14:EB:B1")
//                .build());
//        filters.add(new ScanFilter.Builder()
//                .setDeviceName("iPhone 5s")
//                .build());
//        filters.add(new ScanFilter.Builder()
//                .setServiceUuid(ParcelUuid.fromString("735dc4fa-348e-11e7-a919-92ebcb67fe33"))
//                .build());
        return filters;
    }

    private void onDeviceFound(@NonNull ScanResult result, String tag) {
        String btAddress = result.getDevice().getAddress();
        DeviceInfo item = mAllScanResults.get(btAddress);
        boolean newDevice = (item == null);
        if (!newDevice) {
            item.scanResult = result;
            item.foundCount++;
        } else {
            item = new DeviceInfo(mNextNumber++, btAddress, result);
            mAllScanResults.put(btAddress, item);
        }
        logScanResult(result, tag);
        Timber.tag(TAG).d("Total scan results: %d", mAllScanResults.size());
        if (mListener != null) {
            mListener.onDeviceFound(item, newDevice);
        }
    }

    private void logScanResult(@NonNull ScanResult result, String tag) {
        BluetoothDevice device = result.getDevice();
        Timber.tag(TAG).d("[%s] rssi: %s, address: %s, name: [%s], bondState: %d, type: %d",
                tag, result.getRssi(),
                device.getAddress(), device.getName(), device.getBondState(), device.getType());
        ParcelUuid[] uuids = device.getUuids();
        if (uuids != null && uuids.length > 0) {
            for (ParcelUuid uuid : uuids) {
                Timber.tag(TAG).d("UUID: %s", uuid.getUuid());
            }
        }
    }

    private class MyScanCallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (callbackType == ScanSettings.CALLBACK_TYPE_FIRST_MATCH) {
                onDeviceFound(result, "first match");
            } else if (callbackType == ScanSettings.CALLBACK_TYPE_MATCH_LOST) {
                logScanResult(result, "match lost");
            } else if (callbackType == ScanSettings.CALLBACK_TYPE_ALL_MATCHES) {
                onDeviceFound(result, "all match");
            } else {
                Timber.tag(TAG).w("unknown callbackType: %d", callbackType);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Timber.tag(TAG).d("onBatchScanResults: %d", results.size());
        }

        @Override
        public void onScanFailed(int errorCode) {
            Timber.tag(TAG).w("onScanFailed: %d", errorCode);
        }
    }
}
