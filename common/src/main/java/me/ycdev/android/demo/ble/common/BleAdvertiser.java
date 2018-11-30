package me.ycdev.android.demo.ble.common;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.ParcelUuid;

import me.ycdev.android.lib.common.utils.ApplicationUtils;
import timber.log.Timber;

public class BleAdvertiser {
    private static final String TAG = "BleAdvertiser";

    @SuppressLint("StaticFieldLeak")
    private static BleAdvertiser sInstance;

    private Context mAppContext;
    private BluetoothLeAdvertiser mAdvertiser;
    private AdvertiseCallback mAdvertiseCallback = new MyAdvertiseCallback();
    private boolean mAdvertising;

    public static BleAdvertiser getInstance() {
        return sInstance;
    }

    public boolean isAdvertising() {
        return mAdvertising;
    }

    public boolean startAdvertising(String serviceUuid) {
        if (mAdvertiser == null) {
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

            mAdvertiser = btAdapter.getBluetoothLeAdvertiser();
            if (mAdvertiser == null) {
                Timber.tag(TAG).w("Failed to get BLE advertiser");
                return false;
            }
        }

        Timber.tag(TAG).d("Start advertising: %s", serviceUuid);
        if (!mAdvertising) {
            mAdvertiser.startAdvertising(buildAdvertiseSettings(),
                    buildAdvertiseData(serviceUuid), mAdvertiseCallback);
            mAdvertising = true;
            return true;
        } else {
            Timber.tag(TAG).w("Advertising was already started");
            return false;
        }
    }

    public void stopAdvertising() {
        Timber.tag(TAG).d("Stop advertising");
        if (mAdvertising) {
            mAdvertiser.stopAdvertising(mAdvertiseCallback);
            mAdvertising = false;
        }
    }

    private BleAdvertiser() {
        mAppContext = ApplicationUtils.getApplicationContext();
    }

    private AdvertiseSettings buildAdvertiseSettings() {
        return new AdvertiseSettings.Builder()
                .setTimeout(0)
                .build();
    }

    private AdvertiseData buildAdvertiseData(String serviceUuid) {
        return new AdvertiseData.Builder()
                .addServiceUuid(ParcelUuid.fromString(serviceUuid))
                .setIncludeDeviceName(true)
                .build();
    }

    private class MyAdvertiseCallback extends AdvertiseCallback {
        @Override
        public void onStartFailure(int errorCode) {
            Timber.tag(TAG).w("Advertising failed: %d", errorCode);
        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Timber.tag(TAG).d("Advertising is started successfully");
        }
    }
}
