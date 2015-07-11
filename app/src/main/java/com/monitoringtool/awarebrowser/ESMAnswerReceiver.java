package com.monitoringtool.awarebrowser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.provider.Browser;
import android.util.Log;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.ESM;

import static com.aware.Aware_Preferences.FREQUENCY_WEBSERVICE;
import static com.aware.Aware_Preferences.STATUS_WEBSERVICE;
import static com.aware.Aware_Preferences.WEBSERVICE_SERVER;

/**
 * Created by Maria on 2015-07-05.
 */
/*After ESM is answered run WebService */


public class ESMAnswerReceiver extends BroadcastReceiver {
    private static final boolean MONITORING_DEBUG_FLAG = BrowserActivity.MONITORING_DEBUG_FLAG;
    private static final String ACTION_AWARE_CLOSE_BROWSER = BrowserActivity.ACTION_AWARE_CLOSE_BROWSER;
    private static final String SHARED_PREF_FILE = BrowserActivity.SHARED_PREF_FILE;
    private static final int webServiceSynchroTimeInMinutes = 30;
    private static final String DASHBOARD_STUDY_URL = "https://api.awareframework.com/index.php/webservice/index/407/ADKuMzjP3L3C";

    private static final String LOG_TAG_ESM = "AB:ESM";

    private static final String KEY_FIRST_INSTALL = BrowserActivity.KEY_FIRST_INSTALL;
    private static final int esmToAnswer = 4;
    private static final int maxEsmToRepeatTimes = 1;
    private static int esmAnsweredCount = 0;
    private static int repeatedESMs = 0;
    private SharedPreferences mySharedPref;
    private SharedPreferences.Editor editor;



    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(ESM.ACTION_AWARE_ESM_EXPIRED)) {
            if (MONITORING_DEBUG_FLAG)
                Log.d(LOG_TAG_ESM, "ESM expired send BrowserClosed message again.");
            sendActionCloseBrowser(context);
        } else if (intent.getAction().equals(ESM.ACTION_AWARE_ESM_DISMISSED)) {
            if (MONITORING_DEBUG_FLAG)
                Log.d(LOG_TAG_ESM, "ESM dismissed. Call BrowserClosed intent again.");
            sendActionCloseBrowser(context);
        } else if (intent.getAction().equals(ESM.ACTION_AWARE_ESM_ANSWERED)) {
            if (MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_ESM, "Yupi! ESM answered.");
            esmAnsweredCount++;
            if (esmAnsweredCount == esmToAnswer) {
                if (MONITORING_DEBUG_FLAG)
                    Log.d(LOG_TAG_ESM, "All ESM Answered. Catch Queue complete");
                Toast.makeText(context, context.getResources().getString(R.string.esm_thanks), Toast.LENGTH_SHORT).show();
            }
        } else if (intent.getAction().equals(ESM.ACTION_AWARE_ESM_QUEUE_COMPLETE)) {
            if (MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_ESM, "Queue Complete. All ESM answered");
            new SendDataToTheServerTask().execute(context);
        }else{
            context.stopService(new Intent(context, BrowserPlugin.class));
        }
    }

    private class SendDataToTheServerTask extends AsyncTask<Context, Void, Context> {

        @Override
        protected Context doInBackground(Context... contexts) {
            //Setup application to synchronize with remote Server every webServiceSynchroTimeInMinutes
            mySharedPref = contexts[0].getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE);
            editor = mySharedPref.edit();
            if (mySharedPref.getBoolean(KEY_FIRST_INSTALL, true)) {
                if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_ESM, "Start send data to the remote server every " + String.valueOf(webServiceSynchroTimeInMinutes) + "minutes");
                Aware.setSetting(contexts[0], STATUS_WEBSERVICE, true);
                Aware.setSetting(contexts[0], WEBSERVICE_SERVER, DASHBOARD_STUDY_URL);
                Aware.setSetting(contexts[0], FREQUENCY_WEBSERVICE, webServiceSynchroTimeInMinutes);
                contexts[0].sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));
            }
            contexts[0].sendBroadcast(new Intent(Aware.ACTION_AWARE_SYNC_DATA));


            return contexts[0];
        }

        @Override
        protected void onPostExecute(final Context context) {
            if (MONITORING_DEBUG_FLAG)
                Log.d(LOG_TAG_ESM, "Stop Sensors - Queue completed");
            Toast.makeText(context, context.getResources().getString(R.string.esm_queue_clean), Toast.LENGTH_SHORT).show();
            //Stop collecting data about connection
            context.stopService(new Intent(context, BrowserPlugin.class));



        }
    }


    private void sendActionCloseBrowser(Context context) {
        context.sendBroadcast(new Intent(ESM.ACTION_AWARE_ESM_CLEAN_QUEUE));
        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_ESM, "Cleaning the queue");
        if (repeatedESMs < maxEsmToRepeatTimes) {
            Toast.makeText(context, context.getResources().getString(R.string.esm_rerun), Toast.LENGTH_SHORT).show();
            Intent browserClosed = new Intent();
            browserClosed.setAction(ACTION_AWARE_CLOSE_BROWSER);
            context.sendBroadcast(browserClosed);
            repeatedESMs++;
        } else {
            if (MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_ESM, "ESM was not answered");
            Toast.makeText(context, context.getResources().getString(R.string.esm_never_answered), Toast.LENGTH_SHORT).show();
            context.stopService(new Intent(context, BrowserPlugin.class));
        }
    }
}