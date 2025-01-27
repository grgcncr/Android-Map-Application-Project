package com.example.androiddevmapproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ResultsMapActivity extends AppCompatActivity implements MapView.OnFirstLayoutListener {
    private List<Polygon> circles = new ArrayList<>();
    private static List<GeoPoint> circlesloc = new ArrayList<>();
    private LocationListener locationListener;
    private LocationManager locationManager;
    private IMapController mapController;
    private MapView map = null;
    private static final int LOCATION_PERMISSIONS_REQUEST_CODE = 1;
    private static GeoPoint curloc;
    private Marker curlocmarker;
    private int secsession_id = 0;
    Uri circlesUri = Uri.withAppendedPath(MapContentProvider.CONTENT_URI, "circles");
    Uri markersUri = Uri.withAppendedPath(MapContentProvider.CONTENT_URI, "markers");
    Intent serviceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results_map);
        serviceIntent = new Intent(this, MapService.class);
        map = findViewById(R.id.mapView);
        map.addOnFirstLayoutListener(this);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = location -> {
            if (map != null) {
                deleteMarker(curlocmarker);
                curloc = new GeoPoint(location.getLatitude(), location.getLongitude());
                mapController.setCenter(curloc);
                mapController.animateTo(curloc);
                placeMarker(curloc);
                mapController.setZoom(16.5);

                Polygon pol = new Polygon();
                Iterator<Polygon> circlesiterator = circles.iterator();
                Iterator<GeoPoint> circleslociterator = circlesloc.iterator();
                int l = 0;
                int dis = 0;

                while (circlesiterator.hasNext() && circleslociterator.hasNext()) {
                    if(l == 0 ){
                        dis = MapService.calculateDistance(curloc ,circleslociterator.next());
                    } else {
                        int dis2 = MapService.calculateDistance(curloc,circleslociterator.next());
                        if(dis2 < dis ){
                            pol = circlesiterator.next();
                        }
                    }

                    l++;
                }

                circlesiterator = circles.iterator();
                circleslociterator = circlesloc.iterator();

                android.graphics.Paint outlinePaint = new android.graphics.Paint();
                outlinePaint.setColor(android.graphics.Color.RED);
                pol.getOutlinePaint().set(outlinePaint);
                map.getOverlays().add(pol);
                //paint.setColor(Color.RED);
                map.invalidate();
            };
        };
        getLocation();

        //handling go back button
        findViewById(R.id.returnButton).setOnClickListener(view ->{
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(),MainActivity.class);
            startActivity(intent);
            finish();
            locationManager.removeUpdates(locationListener);
            //onDestroy();
        });

        //handling service pause/restart button
        findViewById(R.id.pauseButton).setOnClickListener(view -> {
            serviceIntent = new Intent(this,MapService.class);
            stopService(serviceIntent);
            startService(serviceIntent);
        });

    }

    public void getLocation(){
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 50, locationListener);
        } else {
            ActivityCompat.requestPermissions(this,new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if (requestCode == LOCATION_PERMISSIONS_REQUEST_CODE) {
            int count = 0;
            for (String permission: permissions){
                if (permission == android.Manifest.permission.ACCESS_FINE_LOCATION || permission == Manifest.permission.ACCESS_COARSE_LOCATION){
                    if (grantResults[count] == PackageManager.PERMISSION_GRANTED){
                        getLocation();
                    }
                }
                count++;
            }
        }


    }

    public void placeMarker(GeoPoint geo){
        if (map != null){
            curlocmarker = new Marker(map);
            curlocmarker.setPosition(geo);
            curlocmarker.setAnchor(Marker.ANCHOR_CENTER,Marker.ANCHOR_BOTTOM);
            map.getOverlays().add(curlocmarker);
            map.setExpectedCenter(geo);
        }
    }

    public void deleteMarker (Marker marker){
        if (map != null) {
            map.getOverlays().remove(marker);
            map.invalidate();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume();
        getLocation();
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Configuration.getInstance().save(this, prefs);
        map.onPause();
        locationManager.removeUpdates(locationListener);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //stopService(serviceIntent);
    }

    @SuppressLint("Range")
    @Override
    public void onFirstLayout(View v, int left, int top, int right, int bottom) {
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        map.setTileSource(TileSourceFactory.MAPNIK);
        mapController = map.getController();
        //Setting start point to Athens (then it changes to current location with marker placement)
        mapController.setCenter(new GeoPoint(37.9838 , 23.7275));
        mapController.animateTo(new GeoPoint(37.9838 , 23.7275));
        map.setMultiTouchControls(true);
        mapController.setZoom(11.0);

        //Loading existing circles on the map
        @SuppressLint("Recycle") Cursor circleCursor = getContentResolver().query(circlesUri, null, null, null, null);
        @SuppressLint("Recycle") Cursor markerCursor = getContentResolver().query(markersUri, null, null, null, null);

        while (true){
            assert circleCursor != null;
            if (!circleCursor.moveToNext()) break;
            secsession_id = circleCursor.getInt(circleCursor.getColumnIndex("session_id"));
        }
        circleCursor = getContentResolver().query(circlesUri, null, null, null, null);
        while (true){
            assert circleCursor != null;
            if (!circleCursor.moveToNext()) break;
            if(secsession_id == circleCursor.getInt(circleCursor.getColumnIndex("session_id"))){
                //Log.i("Test","lalala" + String.valueOf(MainActivity.session_id));
                GeoPoint center = new GeoPoint(circleCursor.getDouble(circleCursor.getColumnIndex("lat")),circleCursor.getDouble(circleCursor.getColumnIndex("long")));
                // Create a list of GeoPoints to represent the vertices of the polygon
                List<GeoPoint> circlePoints = new ArrayList<>();
                int numberOfPoints = 120;
                for (int i = 0; i < numberOfPoints; i++) {
                    double angle = Math.toRadians((360.0 / numberOfPoints) * i);
                    double lat = center.getLatitude() + ( ((double) 100 /1000) / 111.32) * Math.cos(angle);
                    double lon = center.getLongitude() + ( ((double) 100 /1000) / (111.32 * Math.cos(Math.toRadians(center.getLatitude())))) * Math.sin(angle);
                    circlePoints.add(new GeoPoint(lat,lon));
                }

                // Create a polygon overlay
                Polygon circle = new Polygon();
                circle.setPoints(circlePoints);
                circles.add(circle);
                circlesloc.add(center);
                // Add the polygon to the map overlays
                map.getOverlays().add(circle);
            }
        }

        while (true){
            assert markerCursor != null;
            if (!markerCursor.moveToNext()) break;
            if(secsession_id == markerCursor.getInt(markerCursor.getColumnIndex("session_id"))){
                Log.i("Test","marker");
                GeoPoint center = new GeoPoint(markerCursor.getDouble(markerCursor.getColumnIndex("lat")),markerCursor.getDouble(markerCursor.getColumnIndex("long")));
                Marker marker = new Marker(map);
                marker.setPosition(center);
                marker.setAnchor(Marker.ANCHOR_CENTER,Marker.ANCHOR_BOTTOM);
                marker.setTitle(markerCursor.getString(markerCursor.getColumnIndex("status")));
                map.getOverlays().add(marker);

            }
        }

        markerCursor.close();
        circleCursor.close();




    }

}