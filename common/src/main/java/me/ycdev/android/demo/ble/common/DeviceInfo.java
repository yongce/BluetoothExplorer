package me.ycdev.android.demo.ble.common;

import android.bluetooth.le.ScanResult;

import java.util.Comparator;

public class DeviceInfo {
    public int number;
    public int foundCount;
    public String btAddress;
    public ScanResult scanResult;

    public DeviceInfo(int number, String btAddress, ScanResult scanResult) {
        this.number = number;
        this.foundCount = 1;
        this.btAddress = btAddress;
        this.scanResult = scanResult;
    }

    public static class TimestampComparator implements Comparator<DeviceInfo> {
        @Override
        public int compare(DeviceInfo lhs, DeviceInfo rhs) {
            if (lhs.scanResult.getTimestampNanos() < rhs.scanResult.getTimestampNanos()) {
                return 1;
            } else if (lhs.scanResult.getTimestampNanos() > rhs.scanResult.getTimestampNanos()) {
                return -1;
            }
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj);
        }
    }
}
