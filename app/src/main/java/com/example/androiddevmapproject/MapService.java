package com.example.androiddevmapproject;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;
import android.app.Service;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import org.osmdroid.api.IMapController;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;


public class MapService extends Service {

    private IMapController mapController;
    private LocationListener locationListener;
    private LocationManager locationManager;
    private GeoPoint curloc;
    private GeoPoint prevloc;
    private boolean incidecircle = false;
    private String markerdiraction;
    private final int LOCATION_PERMISSIONS_REQUEST_CODE = 3;
    Uri circlesUri = Uri.withAppendedPath(MapContentProvider.CONTENT_URI, "circles");
    Uri markersUri = Uri.withAppendedPath(MapContentProvider.CONTENT_URI, "markers");

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("MapService", "Service Created");
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = location -> {
            @SuppressLint("Recycle") Cursor circleCursor = getContentResolver().query(circlesUri, null, null, null, null);

            while (true){
                assert circleCursor != null;
                if (!circleCursor.moveToNext()) break;
                @SuppressLint("Range") GeoPoint loadedcircle = new GeoPoint(circleCursor.getDouble(circleCursor.getColumnIndex("lat")),circleCursor.getDouble(circleCursor.getColumnIndex("long")));
                curloc = new GeoPoint(location.getLatitude(), location.getLongitude());
                int d = calculateDistance(curloc,loadedcircle);
                if(d<100){
                    incidecircle = true;
                    if (prevloc != null && calculateDistance(loadedcircle,prevloc) > 100 ){
                        for (GeoPoint g : connect(curloc,prevloc)){
                            if ((calculateDistance(loadedcircle,g) == 100)||(calculateDistance(loadedcircle,g) < 101 && calculateDistance(loadedcircle,g) > 99)){
                                markerdiraction = "Entrance\nPoint";
                                placeMarker(g);
                                Log.d("MapService", "Marker Placed");
                                break;
                            }
                        }
                    }
                } else if (d > 100) {
                    incidecircle = false;
                    if (prevloc != null && calculateDistance(loadedcircle,prevloc) < 100 ){
                        for (GeoPoint g : connect(curloc,prevloc)){
                            if ((calculateDistance(loadedcircle,g) == 100)||(calculateDistance(loadedcircle,g) < 100.5 && calculateDistance(loadedcircle,g) > 99.5)){
                                markerdiraction = "Exit\nPoint";
                                placeMarker(g);
                                Log.d("MapService", "Marker Placed");
                                break;
                            }
                        }
                    }
                } else {
                    if (prevloc != null && calculateDistance(loadedcircle,prevloc) > 100 ){
                        markerdiraction = "Entrance\nPoint";
                        placeMarker(curloc);
                        Log.d("MapService", "Marker Placed");
                    } else if (prevloc != null && calculateDistance(loadedcircle,prevloc) < 100 ){
                        markerdiraction = "Exit\nPoint";
                        placeMarker(curloc);
                        Log.d("MapService", "Marker Placed");
                    }
                }
            };
            prevloc = curloc;

        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("MapService", "Service Started");
        getLocation();
        return START_NOT_STICKY; // You can adjust the return value based on your needs
    }

    @Override
    public IBinder onBind(Intent intent) {
        // This service is not designed to be bound
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("MapService", "Service destroyed");
        locationManager.removeUpdates(locationListener);
    }

    public static ArrayList<GeoPoint> connect(GeoPoint startPoint, GeoPoint endPoint) {
        ArrayList<GeoPoint> linePoints = new ArrayList<>();

        double startLat = startPoint.getLatitude();
        double startLon = startPoint.getLongitude();
        double endLat = endPoint.getLatitude();
        double endLon = endPoint.getLongitude();

        double deltaLat = (endLat - startLat) / 100;
        double deltaLon = (endLon - startLon) / 100;

        for (int i = 0; i <= 500; i++) {
            double currentLat = startLat + i * deltaLat;
            double currentLon = startLon + i * deltaLon;
            linePoints.add(new GeoPoint(currentLat, currentLon));
        }

        return linePoints;
    }

    public void placeMarker(GeoPoint geo) {
        if (MapActivity.getMap() != null) {
            MapActivity.getMap().post(() -> {
                Marker marker = new Marker(MapActivity.getMap());
                marker.setPosition(geo);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                marker.setTitle(markerdiraction);
                MapActivity.getMap().getOverlays().add(marker);
                MapActivity.getMap().setExpectedCenter(geo);
                ContentValues circleValues = new ContentValues();
                circleValues.put("session_id", MainActivity.session_id);
                circleValues.put("lat", geo.getLatitude());
                circleValues.put("long", geo.getLongitude());
                if(markerdiraction.equals("Entrance\nPoint")){
                    circleValues.put("status","Entrance\nPoint");
                } else {
                    circleValues.put("status","Exit\nPoint");
                }
                Uri insertedCircleUri = getContentResolver().insert(markersUri, circleValues);
            });
        } else {
            Log.e("MapService", "Map not initialized");
        }
    }


    public static int calculateDistance(GeoPoint startPoint, GeoPoint endPoint) {
        double earthRadius = 6371000; // Radius of the Earth in meters

        double lat1 = Math.toRadians(startPoint.getLatitude());
        double lon1 = Math.toRadians(startPoint.getLongitude());
        double lat2 = Math.toRadians(endPoint.getLatitude());
        double lon2 = Math.toRadians(endPoint.getLongitude());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return (int) Math.round(earthRadius * c);
    }

    public void getLocation(){
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //Log.d("MapService","test log from getLocation()");
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 50, locationListener);

        }else {
            Log.i("Map Service","didn't get location");
        }
    }

}