package com.monitoringtool.awarebrowser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.ESM;
import com.aware.providers.ESM_Provider;

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
    private static final String KEY_WEBSERVICE_IS_NOT_RUNNING = "KEY_WEBSERVICE_IS_NOT_RUNNING";
    private static final String SHARED_PREF_FILE = BrowserActivity.SHARED_PREF_FILE;
    private static final int webServiceSynchroTimeInMinutes = 60;
    //private static final String DASHBOARD_STUDY_URL = "https://api.awareframework.com/index.php/webservice/index/407/ADKuMzjP3L3C";
    private static final String DASHBOARD_STUDY_URL = "https://api.awareframework.com/index.php/webservice/index/464/oYLfIx1ecrWR";

    private static final String LOG_TAG_ESM = "AB:ESM";

    private static final int NUMBER_OF_ESM_TO_ANSWER = 4;
    private static final int ESM_REPEAT_TIME = 1;
    private static int esmAnsweredCount = 0;
    private static final String KEY_REPEATED_ESM = "REPEATED_ESM";
    private SharedPreferences mySharedPref;
    private SharedPreferences.Editor editor;
    public static final String KEY_ESM_EVALUTATION_RUNNING = BrowserClosedReceiver.KEY_ESM_EVALUTATION_RUNNING;


    @Override
    public void onReceive(Context context, Intent intent) {

        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_ESM, String.format("ESM counter answer  %d", esmAnsweredCount));

        mySharedPref = context.getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE);
        editor = mySharedPref.edit();
        SharedPreferences.Editor editor = mySharedPref.edit();
        editor.putBoolean(KEY_ESM_EVALUTATION_RUNNING, true);
        editor.commit();
        if(intent.getAction().equals(ESM.ACTION_AWARE_ESM_DISMISSED) || intent.getAction().equals(ESM.ACTION_AWARE_ESM_EXPIRED)) {
                esmAnsweredCount = 0;
                rerunESM(context);
        } else if (intent.getAction().equals(ESM.ACTION_AWARE_ESM_ANSWERED)) {
            if (MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_ESM, "Yupi! ESM answered. counter");
            esmAnsweredCount++;
        } else if (intent.getAction().equals(ESM.ACTION_AWARE_ESM_QUEUE_COMPLETE) && esmAnsweredCount == NUMBER_OF_ESM_TO_ANSWER)  {
            if (MONITORING_DEBUG_FLAG)
                Log.d(LOG_TAG_ESM, "All ESM Answered and Queue completed");
                removeEmptyAnswers(context);
                Toast.makeText(context, context.getResources().getString(R.string.esm_thanks), Toast.LENGTH_SHORT).show();
                editor.putInt(KEY_REPEATED_ESM, 0);
                editor.commit();
                new SendDataToTheServerTask().execute(context);
        }else if (mySharedPref.getInt(KEY_REPEATED_ESM, 0) > ESM_REPEAT_TIME){
            editor.putInt(KEY_REPEATED_ESM, 0);
            editor.commit();
            editor.putBoolean(KEY_ESM_EVALUTATION_RUNNING, false);
            editor.commit();
        }
    }

    private class SendDataToTheServerTask extends AsyncTask<Context, Void, Context> {

        @Override
        protected Context doInBackground(Context... contexts) {
            Log.d(LOG_TAG_ESM, "Start send data to the remote server every " + String.valueOf(mySharedPref.getBoolean(KEY_WEBSERVICE_IS_NOT_RUNNING, true)) );
            if (mySharedPref.getBoolean(KEY_WEBSERVICE_IS_NOT_RUNNING, true)) {
                if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_ESM, "Start send data to the remote server every " + String.valueOf(webServiceSynchroTimeInMinutes) + "minutes");
                Aware.setSetting(contexts[0], STATUS_WEBSERVICE, true);
                Aware.setSetting(contexts[0], WEBSERVICE_SERVER, DASHBOARD_STUDY_URL);
                Aware.setSetting(contexts[0], FREQUENCY_WEBSERVICE, webServiceSynchroTimeInMinutes);
                contexts[0].sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));

                editor.putBoolean(KEY_WEBSERVICE_IS_NOT_RUNNING, false);
                editor.commit();

            }
            contexts[0].sendBroadcast(new Intent(Aware.ACTION_AWARE_SYNC_DATA));
            return contexts[0];
        }

        @Override
        protected void onPostExecute(final Context context) {
            editor.putBoolean(KEY_ESM_EVALUTATION_RUNNING, false);
            editor.commit();
            if (MONITORING_DEBUG_FLAG)
                Log.d(LOG_TAG_ESM, "Stop Sensors - Queue completed");

            //Stop collecting data about connection
            context.stopService(new Intent(context, BrowserPlugin.class));



        }
    }

    private void rerunESM(Context context) {
        int repeatedESM = mySharedPref.getInt(KEY_REPEATED_ESM, 0);

        if (repeatedESM < ESM_REPEAT_TIME) {
            Intent browserClosed = new Intent();
            browserClosed.setAction(ACTION_AWARE_CLOSE_BROWSER);
            context.sendBroadcast(browserClosed);
            repeatedESM++;
            editor.putInt(KEY_REPEATED_ESM, repeatedESM);
            editor.commit();
            Toast.makeText(context, context.getResources().getString(R.string.esm_rerun), Toast.LENGTH_SHORT).show();
            if (MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_ESM, "Repeated ESMS " + String.valueOf(mySharedPref.getInt(KEY_REPEATED_ESM, 0)));
        }else{
            if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_ESM, "No ESM action");
            context.stopService(new Intent(context, BrowserPlugin.class));
            editor.putInt(KEY_REPEATED_ESM, 0);
            editor.commit();
        }
    }

    private void removeEmptyAnswers(Context context){
        context.getContentResolver().delete(ESM_Provider.ESM_Data.CONTENT_URI, ESM_Provider.ESM_Data.STATUS + "=" + ESM.STATUS_DISMISSED, null);
        context.getContentResolver().delete(ESM_Provider.ESM_Data.CONTENT_URI, ESM_Provider.ESM_Data.STATUS + "=" + ESM.STATUS_EXPIRED, null);
        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_ESM, "ESM remove empty answers");
    }
}