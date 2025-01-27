package com.example.androiddevmapproject;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
public class MapDBHelper extends SQLiteOpenHelper {
    public static final int DB_VERSION = 1;
    private Context context;
    private static final String DB_NAME = "AndroidDevMapProject.db";


    public MapDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //Creating DB
        String TableQueryNO1 = "CREATE TABLE IF NOT EXISTS CIRCLES (_id INTEGER PRIMARY KEY AUTOINCREMENT, session_id INTEGER, lat DOUBLE, long DOUBLE);";
        String TableQueryNO2 = "CREATE TABLE IF NOT EXISTS MARKERS (_id INTEGER PRIMARY KEY AUTOINCREMENT, session_id INTEGER, lat DOUBLE, long DOUBLE , status TEXT);";
        db.execSQL(TableQueryNO1);
        db.execSQL(TableQueryNO2);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //Dropping tables and remaking them
        db.execSQL("DROP TABLE IF EXISTS CIRCLES;");
        db.execSQL("DROP TABLE IF EXISTS MARKERS;");
        onCreate(db);
    }
}