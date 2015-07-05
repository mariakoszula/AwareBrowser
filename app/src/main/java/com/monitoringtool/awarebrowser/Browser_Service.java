package com.monitoringtool.awarebrowser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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

public class Browser_Service extends Aware_Plugin {

    private static final String LOG_TAG_SERVICE = "WLT:Browser_Service";
    public static final String DASHBOARD_STUDY_URL = "https://api.awareframework.com/index.php/webservice/index/403/yqA2zgDrJOPl";
    //private ESMStatusListener esm_statuses;

    public static final String KEY_SESSION_ID = "KEY_SESSION_ID";

    private static final int WAIT_TIME = 1 * 60 * 1000;

    @Override
    public void onCreate() {
        super.onCreate();

        if(ToolbarActivity.MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_SERVICE, "Browser_service runnig");



        startSensors();

        /*Listen to ESM answers */
       /* IntentFilter esm_filter = new IntentFilter();
        esm_filter.addAction(ESM.ACTION_AWARE_ESM_DISMISSED);
        esm_filter.addAction(ESM.ACTION_AWARE_ESM_EXPIRED);
        esm_filter.addAction(ESM.ACTION_AWARE_ESM_ANSWERED);
        esm_filter.addAction(ESM.ACTION_AWARE_ESM_QUEUE_COMPLETE);
        registerReceiver(esm_statuses, esm_filter);*/


        /*To save datatabase in remote server*/
        DATABASE_TABLES = Browser_Provider.DATABASE_TABLES;
        TABLES_FIELDS = Browser_Provider.TABLES_FIELDS;
        CONTEXT_URIS = new Uri[]{ Browser_Provider.Browser_Data.CONTENT_URI };



    }

    private void startSensors() {
        if(ToolbarActivity.MONITORING_DEBUG_FLAG) Aware.setSetting(this, DEBUG_FLAG, true);

        if(ToolbarActivity.MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_SERVICE, "START Sensors...");

        SharedPreferences mySharedPref = getSharedPreferences(ToolbarActivity.SHARED_PREF_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mySharedPref.edit();

        //Prepare DEVICE_ID - the same for the device -- probably aware will take it
       // UUID uuid_ID = UUID.randomUUID();

      //  Aware.setSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID, uuid_ID.toString());


        //Prepare SESSION_ID - unique for each session -- add to Aware
        UUID uuid_session = UUID.randomUUID();
        Aware.setSetting(getApplicationContext(), Aware_Preferences.SESSION_ID, uuid_session);
        if(ToolbarActivity.MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_SERVICE, "Session ID, unique for each session: "+ Aware.getSetting(getApplicationContext(), Aware_Preferences.SESSION_ID));


        //Activate Aware Sensors

        //Start Network Sensor
        Aware.setSetting(this, STATUS_NETWORK_EVENTS, true);
        Aware.setSetting(this, STATUS_NETWORK_TRAFFIC, true);

        //Start Telephony Sensor
        Aware.setSetting(this, STATUS_TELEPHONY, true);

        //Start ESM Sensor
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, true);

        //Start Processor load - only for debugging purpose
        if(ToolbarActivity.MONITORING_DEBUG_FLAG){
            Aware.setSetting(getApplicationContext(), STATUS_PROCESSOR, true);
            Aware.setSetting(getApplicationContext(), FREQUENCY_PROCESSOR, 180);
        }

        //@TODO Google Activity Recognition plugin
        //Install if aware first install

        //Start WebService - sync data with Server every  x minutes - default 30 -- run from second install when ESM is running
   //     Aware.setSetting(getApplicationContext(), STATUS_WEBSERVICE, true);
    //    Aware.setSetting(getApplicationContext(), WEBSERVICE_SERVER, DASHBOARD_STUDY_URL);
     //   Aware.setSetting(getApplicationContext(), FREQUENCY_WEBSERVICE, 30);

        sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));
    }

    private void stopSensors() {
        if(ToolbarActivity.MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_SERVICE, "STOP Sensors...");

        //Stop Network Sensor
        Aware.setSetting(this, STATUS_NETWORK_EVENTS, false);
        Aware.setSetting(this, STATUS_NETWORK_TRAFFIC, false);

        //Stop Telephony Sensor
        Aware.setSetting(this, STATUS_TELEPHONY, false);


        //Stop ESM Sensor
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, false);

        //Stop Processor Sensor
        if(ToolbarActivity.MONITORING_DEBUG_FLAG){
            Aware.setSetting(this, STATUS_PROCESSOR, false);
        }

        //Stop WebServices
     //   Aware.setSetting(getApplicationContext(), STATUS_WEBSERVICE, false);
//


        sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ToolbarActivity.MONITORING_DEBUG_FLAG) Log.d(ToolbarActivity.LOG_TAG, "Start Service");
        SharedPreferences mySharedPref = getSharedPreferences(ToolbarActivity.SHARED_PREF_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mySharedPref.edit();
        editor.putBoolean(ToolbarActivity.KEY_IS_BROWSER_SERVICE_RUNNING, true);
        editor.commit();

        if(ToolbarActivity.MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_SERVICE, "Aware device id: " + Aware.getSetting(getApplicationContext(), DEVICE_ID));
       return super.onStartCommand(intent, flags, startId);

    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(ToolbarActivity.MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_SERVICE, "Browser_service terminated");
       // getApplicationContext().unregisterReceiver(esm_statuses);
        stopSensors();


        SharedPreferences mySharedPref = getSharedPreferences(ToolbarActivity.SHARED_PREF_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mySharedPref.edit();
        editor.putBoolean(ToolbarActivity.KEY_IS_BROWSER_SERVICE_RUNNING, false);
        editor.commit();
    }

/*    public class ESMStatusListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("ESM", "Am I here at all?.");

            String trigger = null;
            String ans = null;

            Cursor esm_data = context.getContentResolver().query(ESM_Provider.ESM_Data.CONTENT_URI, null, null, null, null);

            if (esm_data != null && esm_data.moveToLast()) {
                ans = esm_data.getString(esm_data.getColumnIndex(ESM_Provider.ESM_Data.ANSWER));
                trigger = esm_data.getString(esm_data.getColumnIndex(ESM_Provider.ESM_Data.TRIGGER));
            }
            if (esm_data != null) {
                esm_data.close();
            }
            if (trigger != null && !trigger.contains("com.monitoringtool.awarebrowser")) {
                Log.d("ESM", "Somebody else initiated the ESM, no need to react, returning.");
                return;
            }


            if (intent.getAction().equals(ESM.ACTION_AWARE_ESM_EXPIRED)) {
                Log.d(ToolbarActivity.LOG_TAG, "ESM expired could end service");

                Toast.makeText(context, "ESM expired.", Toast.LENGTH_LONG).show();
            } else if (intent.getAction().equals(ESM.ACTION_AWARE_ESM_DISMISSED)) {
                Log.d(ToolbarActivity.LOG_TAG, "ESM dismissed could end service");

                Toast.makeText(context, "ESM dismissed.", Toast.LENGTH_LONG).show();
            } else if (intent.getAction().equals(ESM.ACTION_AWARE_ESM_ANSWERED)) {
                Log.d(ToolbarActivity.LOG_TAG, "ESM answered could end service");
                if(ToolbarActivity.MONITORING_DEBUG_FLAG) Log.d(ToolbarActivity.LOG_TAG, "Yeey, esm asnwered");
                //AysncTask sendBroadcast
                sendBroadcast(new Intent(Aware.ACTION_AWARE_SYNC_DATA));
                //stopSelf();
            } else if(intent.getAction().equals(ESM.ACTION_AWARE_ESM_QUEUE_COMPLETE)){
                Log.d(ToolbarActivity.LOG_TAG, "ESM queue completa");

                Toast.makeText(context, "ESM queueu complete.", Toast.LENGTH_LONG).show();
                sendBroadcast(new Intent(Aware.ACTION_AWARE_SYNC_DATA));
                //stopSelf();
            }
        }
    }*/
}

