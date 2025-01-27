package com.example.androiddevmapproject;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.Objects;

public class MapContentProvider extends ContentProvider {

    // Authority is a unique string for the content provider
    private static final String AUTHORITY = "com.example.androiddevmapproject";

    // Define the content URI for the top-level authority
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    // UriMatcher used to match the content URI
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Constants for the different URI patterns
    private static final int CIRCLES = 1;
    private static final int MARKERS = 2;

    // Initialize the UriMatcher
    static {
        uriMatcher.addURI(AUTHORITY, "circles", CIRCLES);
        uriMatcher.addURI(AUTHORITY, "markers", MARKERS);
    }

    private MapDBHelper dbHelper;

    @Override
    public boolean onCreate() {
        dbHelper = new MapDBHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor;

        switch (uriMatcher.match(uri)) {
            case CIRCLES:
                cursor = db.query("CIRCLES", projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case MARKERS:
                cursor = db.query("MARKERS", projection, selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }


        return cursor;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id;

        switch (uriMatcher.match(uri)) {
            case CIRCLES:
                id = db.insert("CIRCLES", null, values);
                break;
            case MARKERS:
                id = db.insert("MARKERS", null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        return Uri.withAppendedPath(uri, String.valueOf(id));
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsUpdated;

        switch (uriMatcher.match(uri)) {
            case CIRCLES:
                rowsUpdated = db.update("CIRCLES", values, selection, selectionArgs);
                break;
            case MARKERS:
                rowsUpdated = db.update("MARKERS", values, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        // Notify any observers
        Objects.requireNonNull(getContext()).getContentResolver().notifyChange(uri, null);

        return rowsUpdated;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted;

        switch (uriMatcher.match(uri)) {
            case CIRCLES:
                rowsDeleted = db.delete("CIRCLES", selection, selectionArgs);
                break;
            case MARKERS:
                rowsDeleted = db.delete("MARKERS", selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        // Notify any observers
        Objects.requireNonNull(getContext()).getContentResolver().notifyChange(uri, null);

        return rowsDeleted;
    }

    @Override
    public String getType(@NonNull Uri uri) {

        return null;
    }
}
