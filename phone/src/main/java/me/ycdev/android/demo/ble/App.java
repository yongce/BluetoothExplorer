package me.ycdev.android.demo.ble;

import android.app.Application;

import me.ycdev.android.lib.common.utils.ApplicationUtils;
import timber.log.Timber;

public class App extends Application {
    private static final String TAG = "bledemo.app";

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());
        ApplicationUtils.initApplication(this);
        Timber.tag(TAG).d("app start...");
    }
}
