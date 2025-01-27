package com.example.androiddevmapproject;


import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.LocationManager;
import android.os.IBinder;
import android.util.Log;

public class MapBroadcastReceiver extends BroadcastReceiver {

    private boolean isBound;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("MapBroadcastReceiver", "Received broadcast: " + intent.getAction());
        String action = intent.getAction();
        if (action != null && action.equals("android.location.MODE_CHANGED")) {

            Intent serviceIntent = new Intent(context, MapService.class);

            if (isLocationEnabled(context)) {
                context.startService(serviceIntent);
            } else {
                context.stopService(serviceIntent);
            }
        }
    }
    private boolean gpschecker(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }


//    private ServiceConnection serviceConnection = new ServiceConnection() {
//        @Override
//        public void onServiceConnected(ComponentName name, IBinder service) {
//            Context.BindServiceFlags binder = (MapService.MapServiceBinder) service;
//            mapService = binder.getService();
//            isBound = true;
//            Log.d(TAG, "Service connected");
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName name) {
//            isBound = false;
//            Log.d(TAG, "Service disconnected");
//        }
//    };
private boolean isLocationEnabled(Context context) {
    // Check if location services are enabled
    LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
}

}