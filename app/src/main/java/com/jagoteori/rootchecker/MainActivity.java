package com.jagoteori.rootchecker;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.app.ActivityCompat;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;

public class MainActivity extends AppCompatActivity implements LocationAssistant.Listener {
    LinearLayoutCompat linearLayout;
    LocationManager locationManager;
    Location locationByGps;
    Location locationByNetwork;

    Location currentLocation;
    double latitude;
    double longitude;

    private LocationAssistant assistant;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        assistant = new LocationAssistant(this, this, LocationAssistant.Accuracy.HIGH, 5000, false);
        assistant.setVerbose(true);

//        permissions.add(ACCESS_FINE_LOCATION);
//        permissions.add(ACCESS_COARSE_LOCATION);
//        permissionsToRequest = findUnAskedPermissions(permissions);
//        Log.i("Permission", "onCreate: unasked permission" + permissions);
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//
//
//            if (permissionsToRequest.size() > 0)
//                requestPermissions(permissionsToRequest.toArray(new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);
//        }

        linearLayout = findViewById(R.id.linear_layout);
        TextView device = findViewById(R.id.creator);
        device.setText(getDeviceName());

//        RootBeer rootBeer = new RootBeer(this);

        // TODO:: Initialization Security Checker
        SecurityChecker.init(this);
        SecurityChecker securityChecker = SecurityChecker.getInstance();
        SecurityResult securityResult = securityChecker.performSecurityCheck(this);

        if (securityResult == SecurityResult.ON_ROOTED_DEVICE) {
            resultCheckContent("is Rooted Device", true);
        } else if (securityResult == SecurityResult.ON_DEVELOPER_MODE) {
            resultCheckContent("Developer Mode On", true);
        }

//        resultCheckContent("Root Management Apps", rootBeer.detectRootManagementApps());
//        resultCheckContent("Potentially Dangerous Apps", rootBeer.detectPotentiallyDangerousApps());
//        resultCheckContent("Dangerous Props", rootBeer.checkForDangerousProps());
//        resultCheckContent("Root Cloaking Apps", rootBeer.detectRootCloakingApps());
//        resultCheckContent("TestKeys", rootBeer.detectTestKeys());
//        resultCheckContent("For RW Path", rootBeer.checkForRWPaths());
//        resultCheckContent("Root via Native Check", rootBeer.checkForRootNative());
//        resultCheckContent("Magisk specific checks", rootBeer.checkForMagiskBinary());
//        // ---
//        resultCheckContent("Root Binary BusyBox Check", rootBeer.isRootedWithBusyBoxCheck());
//        resultCheckContent("SU Binary Check", rootBeer.checkForSuBinary());
//        resultCheckContent("2nd SU Binary Check", rootBeer.checkSuExists());

//        boolean isDeveloperModeOn = Settings.Secure.getInt(this.getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0;
//        resultCheckContent("Developer Mode 2 Off", isDeveloperModeOn);
//        boolean isADBEnabled = Settings.Secure.getInt(this.getContentResolver(), Settings.Global.ADB_ENABLED, 0) != 0;
//        resultCheckContent("ADB Enabled", isADBEnabled);
//        boolean isDebuggable = (0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE));
//        resultCheckContent("Debuggable", isDebuggable);

        if (isLocationPermissionGranted()) {
            getLocation();
            Log.i("Granted", "onCreate: granted");
        } else {
            Log.i("Granted", "onCreate: false");
        }
    }

    void resultCheckContent(String title, boolean result) {
        TextView titleText = new TextView(this);
        titleText.setText(title);
        titleText.setTextSize(16);
        titleText.setTypeface(null, Typeface.BOLD);
        titleText.setPadding(0, 0, 0, 6);

        linearLayout.addView(titleText);

        // (2) ------------------------------------

        LinearLayout rowLayout = new LinearLayout(this);
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
        rowLayout.setGravity(Gravity.CENTER_VERTICAL);

        // ------------------------------------
        boolean isPassCheck = !result;

        TextView contentText = new TextView(this);
        contentText.setText(isPassCheck ? "Pass" : "Fail");
        contentText.setTextSize(14);
        contentText.setTextColor(getResources().getColor(isPassCheck ? R.color.green : R.color.red));

        rowLayout.addView(contentText);

        ImageView imageView = new ImageView(this);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(54, 54);
        imageParams.setMargins(12, 0, 0, 0);
        imageView.setLayoutParams(imageParams);

        if (isPassCheck) {
            imageView.setImageResource(R.drawable.ic_check);
        } else {
            imageView.setImageResource(R.drawable.ic_close);
        }

        rowLayout.setPadding(0, 0, 0, 16);
        rowLayout.addView(imageView);

        linearLayout.addView(rowLayout);
    }

    boolean isLocationPermissionGranted() {
        if (ActivityCompat.checkSelfPermission(
                this,
                ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            ACCESS_FINE_LOCATION,
                            ACCESS_COARSE_LOCATION
                    },
                    123
            );
            return false;
        } else {
            return true;
        }
    }

    private void getLocation() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        LocationListener gpsLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                locationByGps = location;
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };

        LocationListener networkLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                locationByNetwork = location;
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };

        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        isLocationPermissionGranted();

        if (hasGps || hasNetwork) {
            if (hasGps) {

                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        5000,
                        0F,
                        gpsLocationListener
                );
            }
            if (hasNetwork) {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        5000,
                        0F,
                        networkLocationListener
                );
            }

            if (locationByGps != null && locationByNetwork != null) {
                if (locationByGps.getAccuracy() > locationByNetwork.getAccuracy()) {
                    currentLocation = locationByGps;
                    latitude = currentLocation.getLatitude();
                    longitude = currentLocation.getLongitude();
                    // use latitude and longitude as per your need
                } else {
                    currentLocation = locationByNetwork;
                    latitude = currentLocation.getLatitude();
                    longitude = currentLocation.getLongitude();
                    // use latitude and longitude as per your need
                }
            }

        } else {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        }
    }


    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        }
        return capitalize(manufacturer) + " " + model;
    }

    private static String capitalize(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        char[] arr = str.toCharArray();
        boolean capitalizeNext = true;
        StringBuilder phrase = new StringBuilder();
        for (char c : arr) {
            if (capitalizeNext && Character.isLetter(c)) {
                phrase.append(Character.toUpperCase(c));
                capitalizeNext = false;
                continue;
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true;
            }
            phrase.append(c);
        }
        return phrase.toString();
    }

    private ArrayList<String> findUnAskedPermissions(ArrayList<String> wanted) {
        ArrayList<String> result = new ArrayList<String>();

        for (String perm : wanted) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }

        return result;
    }

    private boolean hasPermission(String permission) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
            }
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        assistant.start();
    }

    @Override
    protected void onPause() {
        assistant.stop();
        super.onPause();
    }

    @Override
    public void onNeedLocationPermission() {
        assistant.requestAndPossiblyExplainLocationPermission();
    }

    @Override
    public void onExplainLocationPermission() {
        new AlertDialog.Builder(this)
                .setMessage("Permission")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        assistant.requestLocationPermission();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        assistant.requestLocationPermission();
                    }
                })
                .show();
    }

    @Override
    public void onLocationPermissionPermanentlyDeclined(View.OnClickListener fromView,
                                                        DialogInterface.OnClickListener fromDialog) {
        new AlertDialog.Builder(this)
                .setMessage("Ditolak permanen")
                .setPositiveButton("OK", fromDialog)
                .show();
    }

    @Override
    public void onNeedLocationSettingsChange() {
        new AlertDialog.Builder(this)
                .setMessage("Switch Location")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        assistant.changeLocationSettings();
                    }
                })
                .show();
    }

    @Override
    public void onFallBackToSystemSettings(View.OnClickListener fromView, DialogInterface.OnClickListener fromDialog) {
        new AlertDialog.Builder(this)
                .setMessage("Ganti lOcation Long")
                .setPositiveButton("OK", fromDialog)
                .show();
    }

    @Override
    public void onNewLocationAvailable(Location location) {
        Log.i("location", "onNewLocationAvailable: " + location.getLatitude() + " || " + location.getLongitude());
    }

    @Override
    public void onMockLocationsDetected(View.OnClickListener fromView, DialogInterface.OnClickListener fromDialog) {
        Log.i("location", "onMockLocationsDetected: ");
    }

    @Override
    public void onError(LocationAssistant.ErrorType type, String message) {
        Log.i("location", "onError: " + message);
    }
}