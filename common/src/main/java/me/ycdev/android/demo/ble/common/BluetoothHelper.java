package me.ycdev.android.demo.ble.common;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class BluetoothHelper {
    public static boolean supportBle(@NonNull Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    @Nullable
    public static BluetoothAdapter getBluetoothAdapter(@NonNull Context context) {
        BluetoothManager btMgr = context.getSystemService(BluetoothManager.class);
        if (btMgr != null) {
            return btMgr.getAdapter();
        }
        return null;
    }

    public static boolean isBluetoothEnabled(@NonNull Context context) {
        BluetoothAdapter btAdapter = getBluetoothAdapter(context);
        return btAdapter != null && btAdapter.isEnabled();
    }

    public static void startBluetoothSysUI(@NonNull Activity context, int requestCode) {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        context.startActivityForResult(enableBtIntent, requestCode);
    }
}
