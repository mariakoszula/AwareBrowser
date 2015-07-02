package com.monitoringtool.awarebrowser;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Environment;
import android.provider.BaseColumns;
import android.util.Log;

import com.aware.utils.DatabaseHelper;

import com.aware.Aware;

import java.sql.SQLException;
import java.util.HashMap;

/**
 * Created by Maria on 2015-06-20.
 */
public class Browser_Provider extends ContentProvider {
    public static String AUTHORITY = "com.monitoringtool.awarebrowser.provider.browserplt";
    public static final int DATABASE_VERSION = 1;

    private static final int BROWSERPLT = 1;
    private static final int BROWSERPLT_ID = 2;

    public static final class Browser_Data implements BaseColumns {
        private Browser_Data() {
        };

        public static final Uri CONTENT_URI = Uri.parse("content://"
                + Browser_Provider.AUTHORITY + "/browserplt");
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.aware.browserplt";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.aware.browserplt";

        public static final String _ID = "_id";
        public static final String TIMESTAMP = "timestamp";
        public static final String DEVICE_ID = "device_id";
        public static final String UNIQUE_DEVICE_ID = "unique_device_id";
        public static final String WEB_PAGE = "web_page";
        public static final String PAGE_LOAD_TIME = "page_load_time";

    }

    public static final String DATABASE_NAME = Environment.getExternalStorageDirectory() + "/AWARE/browserplt.db";
    public static final String[] DATABASE_TABLES = {"browserplt"};

    public static final String[] TABLES_FIELDS = {
            Browser_Data._ID + " integer primary key autoincrement,"
            + Browser_Data.TIMESTAMP + " real default 0,"
            + Browser_Data.DEVICE_ID + " text default '',"
            + Browser_Data.UNIQUE_DEVICE_ID + " text default '',"
            + Browser_Data.WEB_PAGE + " text default '',"
            + Browser_Data.PAGE_LOAD_TIME + " real default 0,"
            + "UNIQUE (" + Browser_Data.TIMESTAMP + "," + Browser_Data.DEVICE_ID + ")"
    };

    private static UriMatcher sUriMatcher = null;
    private static HashMap<String, String> screenProjectionMap = null;
    private static DatabaseHelper databaseHelper = null;
    private static SQLiteDatabase database = null;

    private boolean initializeDB() {
        if (databaseHelper == null) {
            databaseHelper = new DatabaseHelper( getContext(), DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS );
        }
        if( databaseHelper != null && ( database == null || ! database.isOpen() )) {
            database = databaseHelper.getWritableDatabase();
        }
        return( database != null && databaseHelper != null);
    }

    @Override
    public boolean onCreate() {
        AUTHORITY = getContext().getPackageName() + ".provider.browserplt";

        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(Browser_Provider.AUTHORITY, DATABASE_TABLES[0],
                BROWSERPLT);
        sUriMatcher.addURI(Browser_Provider.AUTHORITY,
                DATABASE_TABLES[0] + "/#", BROWSERPLT_ID);

        screenProjectionMap = new HashMap<String, String>();
        screenProjectionMap.put(Browser_Data._ID, Browser_Data._ID);
        screenProjectionMap.put(Browser_Data.TIMESTAMP, Browser_Data.TIMESTAMP);
        screenProjectionMap.put(Browser_Data.DEVICE_ID, Browser_Data.DEVICE_ID);
        screenProjectionMap.put(Browser_Data.UNIQUE_DEVICE_ID, Browser_Data.UNIQUE_DEVICE_ID);
        screenProjectionMap.put(Browser_Data.WEB_PAGE, Browser_Data.WEB_PAGE);
        screenProjectionMap.put(Browser_Data.PAGE_LOAD_TIME, Browser_Data.PAGE_LOAD_TIME);

        return true;
    }

    /**
     * Query entries from the database
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return null;
        }

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri)) {
            case BROWSERPLT:
                qb.setTables(DATABASE_TABLES[0]);
                qb.setProjectionMap(screenProjectionMap);
                break;
            default:

                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Cursor c = qb.query(database, projection, selection, selectionArgs,
                    null, null, sortOrder);
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        } catch (IllegalStateException e) {
            if (Aware.DEBUG)
                Log.e(Aware.TAG, e.getMessage());

            return null;
        }
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case BROWSERPLT:
                return Browser_Data.CONTENT_TYPE;
            case BROWSERPLT_ID:
                return Browser_Data.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    /**
     * Delete entry from the database
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY, "Database unavailable...");
            return 0;
        }

        int count = 0;
        switch (sUriMatcher.match(uri)) {
            case BROWSERPLT:
                database.beginTransaction();
                count = database.delete(DATABASE_TABLES[0], selection,
                        selectionArgs);
                database.setTransactionSuccessful();
                database.endTransaction();
                break;
            default:

                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }


    /**
     * Update application on the database
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return 0;
        }
        int count = 0;
        switch (sUriMatcher.match(uri)) {
            case BROWSERPLT:
                database.beginTransaction();
                count = database.update(DATABASE_TABLES[0], values, selection,
                        selectionArgs);
                database.setTransactionSuccessful();
                database.endTransaction();
                break;
            default:

                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
    /**
     * Insert entry to the database
     */
    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return null;
        }
        ContentValues values = (initialValues != null) ? new ContentValues(
                initialValues) : new ContentValues();

        switch (sUriMatcher.match(uri)) {
            case BROWSERPLT:
                database.beginTransaction();
                long screen_id = database.insertWithOnConflict(DATABASE_TABLES[0],
                        Browser_Data.DEVICE_ID, values,SQLiteDatabase.CONFLICT_IGNORE);
                database.setTransactionSuccessful();
                database.endTransaction();
                if (screen_id > 0) {
                    Uri screenUri = ContentUris.withAppendedId(
                            Browser_Data.CONTENT_URI, screen_id);
                    getContext().getContentResolver().notifyChange(screenUri, null);
                    return screenUri;
                }
                try {
                    throw new SQLException("Failed to insert row into " + uri);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            default:

                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }
}