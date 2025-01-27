package com.example.androiddevmapproject;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    public static int session_id = 0;
    public static boolean strbtnpressed = false;
    private boolean empty = true;
    Uri circlesUri = Uri.withAppendedPath(MapContentProvider.CONTENT_URI, "circles");
    private BroadcastReceiver mapBroadcastReceiver;


    @SuppressLint({"Range", "Recycle"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


//        MapDBHelper dbHelper = new MapDBHelper(this);
//        SQLiteDatabase db = dbHelper.getWritableDatabase();
//        int currentVersion = db.getVersion();
//        dbHelper.onUpgrade(db, currentVersion, MapDBHelper.DB_VERSION);

        setContentView(R.layout.activity_main);

        mapBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("MapBroadcastReceiver", "Received broadcast: " + intent.getAction());
                if (intent.getAction() != null && Objects.equals(intent.getAction(), LocationManager.MODE_CHANGED_ACTION)) {
                    Intent serviceIntent = new Intent(context, MapService.class);

                    if ((isLocationEnabled(context))) {
                        startService(serviceIntent);
                    } else {
                        stopService(serviceIntent);
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(LocationManager.MODE_CHANGED_ACTION);
        registerReceiver(mapBroadcastReceiver, filter);

        @SuppressLint("Recycle") Cursor circleCursor = getContentResolver().query(circlesUri, null, null, null, null);
        while (true){
            assert circleCursor != null;
            if (!circleCursor.moveToNext()) break;
            if(session_id < circleCursor.getInt(circleCursor.getColumnIndex("session_id"))) {
                session_id = circleCursor.getInt(circleCursor.getColumnIndex("session_id"));
                empty = false;
            }
        }
        //if(session_id!=0)session_id++;
        Log.i("Test",String.valueOf(MainActivity.session_id));

        findViewById(R.id.beginbutton).setOnClickListener(view ->{
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(),MapActivity.class);
            startActivity(intent);
            //finish();
        });

        findViewById(R.id.Endbutton).setOnClickListener(view ->{
            if (session_id != 0){
                Log.i("Test",String.valueOf(MainActivity.session_id));
                strbtnpressed = false;

                Toast.makeText(getApplicationContext(), "Ended Session", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "You haven't started a session", Toast.LENGTH_SHORT).show();
            }

        });

        findViewById(R.id.resultsButton).setOnClickListener(view ->{
            Cursor circlecursor = getContentResolver().query(circlesUri, null, null, null, null);
            while (true){
                assert circlecursor != null;
                if (!circlecursor.moveToNext()) break;
                empty = false;
            }
            if (session_id != 0 && !empty){
                Log.i("Test",String.valueOf(MainActivity.session_id));
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(),ResultsMapActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(getApplicationContext(), "No results to show", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the receiver when the activity is destroyed
        if (mapBroadcastReceiver != null) {
            unregisterReceiver(mapBroadcastReceiver);
        }
    }
    private boolean isLocationEnabled(Context context) {
        // Check if location services are enabled
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

}