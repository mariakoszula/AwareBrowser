package com.monitoringtool.awarebrowser;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.utils.Aware_Plugin;


import java.util.UUID;

import static com.aware.Aware_Preferences.DEBUG_FLAG;
import static com.aware.Aware_Preferences.DEVICE_ID;
import static com.aware.Aware_Preferences.FREQUENCY_PROCESSOR;
import static com.aware.Aware_Preferences.FREQUENCY_WEBSERVICE;
import static com.aware.Aware_Preferences.STATUS_NETWORK_EVENTS;
import static com.aware.Aware_Preferences.STATUS_NETWORK_TRAFFIC;
import static com.aware.Aware_Preferences.STATUS_PROCESSOR;
import static com.aware.Aware_Preferences.STATUS_TELEPHONY;
import static com.aware.Aware_Preferences.STATUS_WEBSERVICE;
import static com.aware.Aware_Preferences.WEBSERVICE_SERVER;

public class BrowserPlugin extends Aware_Plugin {

    private static final String LOG_TAG_SERVICE = "AB:BrowserPlugin";


    private static final boolean MONITORING_DEBUG_FLAG = BrowserActivity.MONITORING_DEBUG_FLAG;


    private static final String SHARED_PREF_FILE = BrowserActivity.SHARED_PREF_FILE;
    private static final String ACTION_AWARE_READY = BrowserActivity.ACTION_AWARE_READY;

    private static final String KEY_IS_BROWSER_SERVICE_RUNNING = BrowserActivity.KEY_IS_BROWSER_SERVICE_RUNNING;
    private SharedPreferences mySharedPref;
    private SharedPreferences.Editor editor;

    @Override
    public void onCreate() {
        super.onCreate();


        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_SERVICE, "Browser_service runnig");

        /*To save datatabase in remote server*/
        DATABASE_TABLES = Browser_Provider.DATABASE_TABLES;
        TABLES_FIELDS = Browser_Provider.TABLES_FIELDS;
        CONTEXT_URIS = new Uri[]{ Browser_Provider.Browser_Data.CONTENT_URI };

        new SetUpAware().execute();
    }

    private class SetUpAware extends AsyncTask<Void, Integer, Void>{

        @Override
        protected Void doInBackground(Void... voids) {
            startSensors();
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_SERVICE, "Progress: " + values[0]);

        }

        @Override
        protected void onPostExecute(Void aLong) {
            if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_SERVICE, "On Post Exectue Aware started");
            Intent sendAwareReady = new Intent();
            sendAwareReady.setAction(ACTION_AWARE_READY);
            sendBroadcast(sendAwareReady);
        }
    }

    private void startSensors() {
        if(MONITORING_DEBUG_FLAG) Aware.setSetting(this, DEBUG_FLAG, true);

        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_SERVICE, "START Sensors...");

        //Prepare SESSION_ID - unique for each session
        UUID uuid_session = UUID.randomUUID();
        Aware.setSetting(getApplicationContext(), Aware_Preferences.SESSION_ID, uuid_session);
        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_SERVICE, "Session ID, unique for each session: "+ Aware.getSetting(getApplicationContext(), Aware_Preferences.SESSION_ID));

        //Activate Aware Sensors
        //Start Network Sensor
        Aware.setSetting(this, STATUS_NETWORK_EVENTS, true);
        Aware.setSetting(this, STATUS_NETWORK_TRAFFIC, true);

        //Start Telephony Sensor
        Aware.setSetting(this, STATUS_TELEPHONY, true);

        //Start ESM Sensor
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, true);

        sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));
    }

    private void stopSensors() {
        if (MONITORING_DEBUG_FLAG)
            Log.d(LOG_TAG_SERVICE, "STOP Sensors...");

        //Stop Network Sensor
        Aware.setSetting(this, STATUS_NETWORK_EVENTS, false);
        Aware.setSetting(this, STATUS_NETWORK_TRAFFIC, false);

        //Stop Telephony Sensor
        Aware.setSetting(this, STATUS_TELEPHONY, false);


        //Stop ESM Sensor
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, false);

        sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_SERVICE, "Start Service");
        mySharedPref = getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE);
        editor = mySharedPref.edit();
        editor.putBoolean(KEY_IS_BROWSER_SERVICE_RUNNING, true);
        editor.commit();

        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_SERVICE, "Aware device id: " + Aware.getSetting(getApplicationContext(), DEVICE_ID));
       return super.onStartCommand(intent, flags, startId);

    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_SERVICE, "Browser_service terminated");
        stopSensors();
        mySharedPref = getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE);
        editor = mySharedPref.edit();
        editor.putBoolean(KEY_IS_BROWSER_SERVICE_RUNNING, false);
        editor.commit();
    }


}

