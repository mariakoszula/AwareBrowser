package com.monitoringtool.awarebrowser;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by Maria on 2015-06-20.
 */
public class BrowserProvider extends ContentProvider {
    public static final String AUTHORITY = "com.monitoringtool.awarebrowser.provider.loadingpt";

    public static final class Browser_Data implements BaseColumns {
        private Browser_Data() {
        };

        public static final Uri CONTENT_URI = Uri.parse("content://"
                + BrowserProvider.AUTHORITY + "/");
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.aware.screen";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.aware.screen";

        public static final String _ID = "_id";
        public static final String TIMESTAMP = "timestamp";
        public static final String DEVICE_ID = "device_id";
        public static final String SCREEN_STATUS = "screen_status";
    }


    @Override
    public boolean onCreate() {
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] strings, String s, String[] strings1, String s1) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        return null;
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        return 0;
    }
}
