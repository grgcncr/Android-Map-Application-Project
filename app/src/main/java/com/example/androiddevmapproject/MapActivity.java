package com.example.androiddevmapproject;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
//import android.graphics.Paint;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Paint;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import android.widget.Button;
import android.widget.Toast;

import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class MapActivity extends AppCompatActivity implements MapView.OnFirstLayoutListener {

    private LocationListener locationListener;
    private LocationManager locationManager;
    private IMapController mapController;
    private List<Polygon> circles = new ArrayList<>();
    private static List<GeoPoint> circlesloc = new ArrayList<>();
    private List<Marker> markers = new ArrayList<>();
    private MapListener mapListener;
    private Polygon.OnClickListener polygonClickListener;

    public static GeoPoint getCurloc() {
        return curloc;
    }
    private static final int LOCATION_PERMISSIONS_REQUEST_CODE = 1;
    private static GeoPoint curloc;
    private Marker curlocmarker;
    private static MapView map = null;
    public static MapView getMap() {
        return map;
    }
    Uri circlesUri = Uri.withAppendedPath(MapContentProvider.CONTENT_URI, "circles");
    Uri markersUri = Uri.withAppendedPath(MapContentProvider.CONTENT_URI, "markers");
    Intent serviceIntent ;
    private Button startButton;

    @SuppressLint("Range")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        SharedPreferences preferences = getSharedPreferences("Prefs",MODE_PRIVATE);
//        @SuppressLint("CommitPrefEdits") SharedPreferences.Editor editor = preferences.edit();
//        if (preferences.getBoolean("started",true));
        startButton = findViewById(R.id.startButton);
        setContentView(R.layout.activity_map);

        serviceIntent = new Intent(this, MapService.class);
        super.onCreate(savedInstanceState);
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
                mapController.setZoom(18.0);



            };
        };
        getLocation();
        map.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) { //pressing zoom buttons trigger longPressListener. Bug at: https://github.com/osmdroid/osmdroid/issues/1822
                //remove any circles close by, else add one.
                if(!removeCircle(p)){
                    addCircle(p);
                }
                return true;
            }
        }));

        //handling start button
        findViewById(R.id.startButton).setOnClickListener(view ->{
            MainActivity.session_id++;

            Log.i("Test",String.valueOf(MainActivity.session_id));
            startButton = findViewById(R.id.startButton);
            startButton.setEnabled(false);
            MainActivity.strbtnpressed = true;
            // Start the service
            startService(serviceIntent);
            Toast.makeText(getApplicationContext(), "Started Session", Toast.LENGTH_SHORT).show();
            for (GeoPoint c : circlesloc){
                ContentValues circleValues = new ContentValues();
                circleValues.put("session_id", MainActivity.session_id);
                circleValues.put("lat", c.getLatitude());
                circleValues.put("long", c.getLongitude());
                getContentResolver().insert(circlesUri, circleValues);
            }
        });

        //handling cancel button
        findViewById(R.id.cancelButton).setOnClickListener(view ->{
            startButton = findViewById(R.id.startButton);
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(),MainActivity.class);
            startActivity(intent);
            @SuppressLint("Recycle") Cursor circlecursor = getContentResolver().query(circlesUri, null, null, null, null);
            @SuppressLint("Recycle") Cursor markercursor = getContentResolver().query(markersUri, null, null, null, null);
            while (true) {
                assert circlecursor != null;
                if (!circlecursor.moveToNext()) break;
                if(MainActivity.session_id == circlecursor.getInt(circlecursor.getColumnIndex("session_id"))){
                    getContentResolver().delete(circlesUri, "session_id = ?", new String[]{String.valueOf(MainActivity.session_id)});
                }
            }
            circlecursor.close();
            while (true) {
                assert markercursor != null;
                if (!circlecursor.moveToNext()) break;
                if(MainActivity.session_id == markercursor.getInt(markercursor.getColumnIndex("session_id"))){
                    getContentResolver().delete(markersUri, "session_id = ?", new String[]{String.valueOf(MainActivity.session_id)});
                }
            }
            markercursor.close();
            MainActivity.strbtnpressed = false;
            circles.clear();
            circlesloc.clear();
            Log.i("Test",String.valueOf(MainActivity.session_id));
            locationManager.removeUpdates(locationListener);
        });
        if(MainActivity.strbtnpressed){
            startButton = findViewById(R.id.startButton);
            startButton.setEnabled(false);
        }
    }

    @SuppressLint({"Range", "Recycle"})
    private boolean removeCircle(GeoPoint p) {
        Cursor circlecursor = getContentResolver().query(circlesUri, null, null, null, null);
        Cursor markercursor = getContentResolver().query(markersUri, null, null, null, null);

        boolean flag = false;

        Iterator<Polygon> circlesiterator = circles.iterator();
        Iterator<GeoPoint> circleslociterator = circlesloc.iterator();
        while (circlesiterator.hasNext() && circleslociterator.hasNext()) {
            Polygon polygon = circlesiterator.next();
            GeoPoint circle = circleslociterator.next();
            if (MapService.calculateDistance(p,circle) < 100) { // pixel tolerance is indicative
                while (true) {
                    assert circlecursor != null;
                    if (!circlecursor.moveToNext()) break;
                    //GeoPoint circlepoint = new GeoPoint(circlecursor.getDouble(circlecursor.getColumnIndex("lat")),circlecursor.getDouble(circlecursor.getColumnIndex("long")));
                    while (true) {
                        assert markercursor != null;
                        if (!markercursor.moveToNext()) break;
                        GeoPoint markerpoint = new GeoPoint(markercursor.getDouble(markercursor.getColumnIndex("lat")),markercursor.getDouble(markercursor.getColumnIndex("long")));
                        int d = MapService.calculateDistance(markerpoint,circle);
                        if ( d == 100 || (d < 100.5 && d > 99.5)){
                            String selection = "lat = ? AND long = ?";
                            @SuppressLint("Range") String[] selectionArgs = {String.valueOf(markercursor.getDouble(markercursor.getColumnIndex("lat"))), String.valueOf(markercursor.getDouble(markercursor.getColumnIndex("long")))};
                            getContentResolver().delete(markersUri, selection, selectionArgs);
                        }
                    }
                    markercursor = getContentResolver().query(markersUri, null, null, null, null);
                    String selection = "lat = ? AND long = ?";
                    @SuppressLint("Range") String[] selectionArgs = {String.valueOf(circlecursor.getDouble(circlecursor.getColumnIndex("lat"))), String.valueOf(circlecursor.getDouble(circlecursor.getColumnIndex("long")))};
                    getContentResolver().delete(circlesUri, selection, selectionArgs);

                }
                circlesiterator.remove();
                circleslociterator.remove();
                map.getOverlays().remove(polygon);
                flag = true;
                Toast.makeText(getApplicationContext(), "Removed Circle", Toast.LENGTH_SHORT).show();
            }
        }

        assert circlecursor != null;
        circlecursor.close();
        return flag;

    }


    private void addCircle(GeoPoint center) {
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
        // Add the polygon to the map overlays
        map.getOverlays().add(circle);
        circles.add(circle);
        circlesloc.add(center);
        if(MainActivity.strbtnpressed){
            ContentValues circleValues = new ContentValues();
            circleValues.put("session_id", MainActivity.session_id);
            circleValues.put("lat", center.getLatitude());
            circleValues.put("long", center.getLongitude());
            getContentResolver().insert(circlesUri, circleValues);
        }
        Toast.makeText(getApplicationContext(), "Added Circle", Toast.LENGTH_SHORT).show();
    }



    public void getLocation(){
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 50, locationListener);
        } else {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if (requestCode == LOCATION_PERMISSIONS_REQUEST_CODE) {
            int count = 0;
            for (String permission: permissions){
                if (permission == Manifest.permission.ACCESS_FINE_LOCATION || permission == Manifest.permission.ACCESS_COARSE_LOCATION){
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
            curlocmarker.setTitle("Current\nLocation");
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

        if(MainActivity.strbtnpressed){

            //Loading existing circles and markers on the map if start button is pressed
            @SuppressLint("Recycle") Cursor circleCursor = getContentResolver().query(circlesUri, null, null, null, null);
            @SuppressLint("Recycle") Cursor markerCursor = getContentResolver().query(markersUri, null, null, null, null);

            while (true){
                assert circleCursor != null;
                if (!circleCursor.moveToNext()) break;
                if(MainActivity.session_id == circleCursor.getInt(circleCursor.getColumnIndex("session_id"))){
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
                    Polygon circle = new Polygon();
                    circle.setPoints(circlePoints);
                    circles.add(circle);
                    circlesloc.add(center);
                    map.getOverlays().add(circle);
                }
            }

            while (true){
                assert markerCursor != null;
                if (!markerCursor.moveToNext()) break;
                if(MainActivity.session_id == markerCursor.getInt(markerCursor.getColumnIndex("session_id"))){
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
}