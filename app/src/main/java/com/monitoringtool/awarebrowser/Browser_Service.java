package com.monitoringtool.awarebrowser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ESM;
import com.aware.providers.Aware_Provider;
import com.aware.providers.ESM_Provider;
import com.aware.providers.Network_Provider;
import com.aware.providers.Processor_Provider;
import com.aware.providers.Telephony_Provider;
import com.aware.utils.Aware_Plugin;

import static com.aware.Aware_Preferences.DEBUG_FLAG;
import static com.aware.Aware_Preferences.STATUS_NETWORK_EVENTS;
import static com.aware.Aware_Preferences.STATUS_NETWORK_TRAFFIC;
import static com.aware.Aware_Preferences.STATUS_PROCESSOR;
import static com.aware.Aware_Preferences.STATUS_TELEPHONY;
import static com.aware.Aware_Preferences.STATUS_WEBSERVICE;
import static com.aware.Aware_Preferences.WEBSERVICE_SERVER;

public class Browser_Service extends Aware_Plugin {

    @Override
    public void onCreate() {
        super.onCreate();


        DATABASE_TABLES = Browser_Provider.DATABASE_TABLES;
        TABLES_FIELDS = Browser_Provider.TABLES_FIELDS;
        CONTEXT_URIS = new Uri[]{ Browser_Provider.Browser_Data.CONTENT_URI };



    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ToolbarActivity.MONITORING_DEBUG_FLAG) Log.d(ToolbarActivity.LOG_TAG, "Start Service");


       return super.onStartCommand(intent, flags, startId);

    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }



}

