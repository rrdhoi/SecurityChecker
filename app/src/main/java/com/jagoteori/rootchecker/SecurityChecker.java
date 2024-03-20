package com.jagoteori.rootchecker;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.provider.Settings;

import com.scottyab.rootbeer.RootBeer;

public class SecurityChecker {
    private static volatile SecurityChecker single_instance = null;
    private RootBeer rootBeer = null;

    private SecurityChecker() {}
    public static synchronized void init(Context context) {
        if (single_instance == null) {
            single_instance = new SecurityChecker();
            single_instance.rootBeer = new RootBeer(context);
        }
    }

    public static SecurityChecker getInstance() {
        if (single_instance == null) {
            single_instance = new SecurityChecker();
        }
        return single_instance;
    }

    public RootBeer getRootBeer() {
        if (rootBeer == null) {
            throw new IllegalStateException("RootBeer instance not initialized. Call init() first.");
        }
        return rootBeer;
    }

    SecurityResult performSecurityCheck(Context context) {
        try {
            boolean isRooted = getRootBeer().isRooted();
            boolean isDeveloperModeOn = Settings.Secure.getInt(context.getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0;
            boolean isADBEnabled = Settings.Secure.getInt(context.getContentResolver(), Settings.Global.ADB_ENABLED, 0) != 0;
            boolean isDebuggable = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;

            if (isRooted) {
                return SecurityResult.ON_ROOTED_DEVICE;
            } else if (isDeveloperModeOn || isADBEnabled || isDebuggable) {
                return SecurityResult.ON_DEVELOPER_MODE;
            } else {
                return SecurityResult.PASS;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return SecurityResult.ERROR;
        }
    }
}
