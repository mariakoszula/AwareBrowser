package com.monitoringtool.awarebrowser;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
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
    public static final String DASHBOARD_STUDY_URL = "https://api.awareframework.com/index.php/webservice/index/407/ADKuMzjP3L3C";


    @Override
    public void onCreate() {
        super.onCreate();

        if(ToolbarActivity.MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_SERVICE, "Browser_service runnig");

        new SetUpAware().execute();



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


        //Prepare SESSION_ID - unique for each session
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
        mySharedPref = getSharedPreferences(ToolbarActivity.SHARED_PREF_FILE, Context.MODE_PRIVATE);
        editor = mySharedPref.edit();



        if(mySharedPref.getBoolean(ToolbarActivity.KEY_FIRST_INSTALL, true)) {
            //Start WebService - sync data with Server every  x minutes - default 30 -- run from second install when ESM is running -- move t his two ESM answered?? let me think about it probably better to avoid to much work @TODO move to the ESMAnswer??
        Aware.setSetting(getApplicationContext(), STATUS_WEBSERVICE, true);
        Aware.setSetting(getApplicationContext(), WEBSERVICE_SERVER, DASHBOARD_STUDY_URL);
        Aware.setSetting(getApplicationContext(), FREQUENCY_WEBSERVICE, 0);
        }
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
        /*Aware.setSetting(getApplicationContext(), STATUS_WEBSERVICE, false);*/

        sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));
    }

    private class SetUpAware extends AsyncTask<Void, Integer, Void>{

        @Override
        protected Void doInBackground(Void... voids) {
         /*   final Handler handler = new Handler();

            final Runnable doWait = new Runnable() {
                @Override
                public void run() {
                    if(ToolbarActivity.MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_SERVICE, "Prepare some delay after sensors are started: " + WAIT_TIME_FOR_AWARE);
                }
            };
            handler.postDelayed(doWait, WAIT_TIME_FOR_AWARE);
*/
            startSensors();
           /* try {
                Thread.sleep(WAIT_TIME_FOR_AWARE);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if(ToolbarActivity.MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_SERVICE, "Progress: " + values[0]);

        }

        @Override
        protected void onPostExecute(Void aLong) {
            if(ToolbarActivity.MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_SERVICE, "On Post Exectue Aware started");
            Intent sendAwareReady = new Intent();
            sendAwareReady.setAction(ToolbarActivity.ACTION_AWARE_READY);
            sendBroadcast(sendAwareReady);
        }
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
        stopSensors();


        SharedPreferences mySharedPref = getSharedPreferences(ToolbarActivity.SHARED_PREF_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mySharedPref.edit();
        editor.putBoolean(ToolbarActivity.KEY_IS_BROWSER_SERVICE_RUNNING, false);
        editor.commit();
    }

}

