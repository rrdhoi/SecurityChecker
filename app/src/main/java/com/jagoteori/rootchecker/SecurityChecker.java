package com.jagoteori.rootchecker;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.util.Log;

import com.scottyab.rootbeer.RootBeer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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
            int fakeGpsAppsCount = 0;

            for (int i = 0; i<getListOfFakeLocationApps(context).size(); i++){
                fakeGpsAppsCount += i;
                Log.i("LISTFAKELOCATION", "onCreate: "+ getListOfFakeLocationApps(context).get(i));
            }

            if (isRooted) {
                return SecurityResult.ON_ROOTED_DEVICE;
            } else if (isDeveloperModeOn || isADBEnabled || isDebuggable) {
                return SecurityResult.ON_DEVELOPER_MODE;
            } else if (fakeGpsAppsCount != 0) {
                return SecurityResult.FAKE_GPS_APP_DETECTED;
            } else {
                return SecurityResult.PASS;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return SecurityResult.ERROR;
        }
    }

    public static List<String> getListOfFakeLocationApps(Context context) {
        List<String> runningApps = getRunningApps(context, false);
        for (int i = runningApps.size() - 1; i >= 0; i--) {
            String app = runningApps.get(i);
            if(!hasAppPermission(context, app, "android.permission.ACCESS_MOCK_LOCATION")){
                runningApps.remove(i);
            }
        }
        return runningApps;
    }

    public static List<String> getRunningApps(Context context, boolean includeSystem) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        List<String> runningApps = new ArrayList<>();

        List<ActivityManager.RunningAppProcessInfo> runAppsList = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo processInfo : runAppsList) {
            for (String pkg : processInfo.pkgList) {
                runningApps.add(pkg);
            }
        }

        try {
            //can throw securityException at api<18 (maybe need "android.permission.GET_TASKS")
            List<ActivityManager.RunningTaskInfo> runningTasks = activityManager.getRunningTasks(1000);
            for (ActivityManager.RunningTaskInfo taskInfo : runningTasks) {
                runningApps.add(taskInfo.topActivity.getPackageName());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        List<ActivityManager.RunningServiceInfo> runningServices = activityManager.getRunningServices(1000);
        for (ActivityManager.RunningServiceInfo serviceInfo : runningServices) {
            runningApps.add(serviceInfo.service.getPackageName());
        }

        runningApps = new ArrayList<>(new HashSet<>(runningApps));

        if(!includeSystem){
            for (int i = runningApps.size() - 1; i >= 0; i--) {
                String app = runningApps.get(i);
                if(isSystemPackage(context, app)){
                    runningApps.remove(i);
                }
            }
        }
        return runningApps;
    }

    public static boolean isSystemPackage(Context context, String app){
        PackageManager packageManager = context.getPackageManager();
        try {
            PackageInfo pkgInfo = packageManager.getPackageInfo(app, 0);
            return (pkgInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean hasAppPermission(Context context, String app, String permission){
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo;
        try {
            packageInfo = packageManager.getPackageInfo(app, PackageManager.GET_PERMISSIONS);
            if(packageInfo.requestedPermissions!= null){
                for (String requestedPermission : packageInfo.requestedPermissions) {
                    if (requestedPermission.equals(permission)) {
                        return true;
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }
}
