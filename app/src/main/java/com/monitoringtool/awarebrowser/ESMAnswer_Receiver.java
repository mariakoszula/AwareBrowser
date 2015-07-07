package com.monitoringtool.awarebrowser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
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


public class ESMAnswer_Receiver extends BroadcastReceiver {

    private static final String LOG_TAG_ESM = "WPL:ESM";
    private static final int esmToAnswer = 4;
    private static final int maxEsmToRepeatTimes = 1;
    private int esmAnsweredCount = 0;
    private static int repeatedESMs = 0;


    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(ESM.ACTION_AWARE_ESM_EXPIRED)) {
            if (ToolbarActivity.MONITORING_DEBUG_FLAG)
                Log.d(LOG_TAG_ESM, "ESM expired send BrowserClosed message again.");
            Toast.makeText(context, context.getResources().getString(R.string.esm_rerun), Toast.LENGTH_LONG).show();
            sendActionCloseBrowser(context);
        } else if (intent.getAction().equals(ESM.ACTION_AWARE_ESM_DISMISSED)) {
            if (ToolbarActivity.MONITORING_DEBUG_FLAG)
                Log.d(LOG_TAG_ESM, "ESM dismissed. Call BrowserClosed intent again.");
            Toast.makeText(context, context.getResources().getString(R.string.esm_rerun), Toast.LENGTH_LONG).show();

            sendActionCloseBrowser(context);
        } else if (intent.getAction().equals(ESM.ACTION_AWARE_ESM_ANSWERED)) {
            if (ToolbarActivity.MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_ESM, "Yupi! ESM answered.");

            esmAnsweredCount++;
            if (esmAnsweredCount == esmToAnswer) {
                if (ToolbarActivity.MONITORING_DEBUG_FLAG)
                    Log.d(LOG_TAG_ESM, "All ESM Answered. Catch Queue complete");
                Toast.makeText(context, context.getResources().getString(R.string.esm_thanks), Toast.LENGTH_LONG).show();
            }
        } else if (intent.getAction().equals(ESM.ACTION_AWARE_ESM_QUEUE_COMPLETE)) {
            if (ToolbarActivity.MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_ESM, "All ESM answered");
            new SendDataToTheServerTask().execute(context);
        }
    }

    private class SendDataToTheServerTask extends AsyncTask<Context, Void, Context> {

        @Override
        protected Context doInBackground(Context... contexts) {
            //Stop collecting data about connection, when ESM are answered
            contexts[0].stopService(new Intent(contexts[0], Browser_Service.class));
            if (ToolbarActivity.MONITORING_DEBUG_FLAG)
                Log.d(LOG_TAG_ESM, "Stop Browser_Service");

            return contexts[0];
        }

        @Override
        protected void onPostExecute(final Context context) {
            if (ToolbarActivity.MONITORING_DEBUG_FLAG)
                Log.d(ToolbarActivity.LOG_TAG, "Webservices event: Data was send to the server");
            context.sendBroadcast(new Intent(Aware.ACTION_AWARE_SYNC_DATA));

        }


    }


    private void sendActionCloseBrowser(Context context) {
        context.sendBroadcast(new Intent(ESM.ACTION_AWARE_ESM_CLEAN_QUEUE));
        if(ToolbarActivity.MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_ESM, "Cleaning the queue");
        if (repeatedESMs < maxEsmToRepeatTimes) {
            Intent browserClosed = new Intent();
            browserClosed.setAction(ToolbarActivity.ACTION_CLOSE_BROWSER);
            context.sendBroadcast(browserClosed);
            repeatedESMs++;
        } else {
            if (ToolbarActivity.MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_ESM, "ESM was not answered");
            Toast.makeText(context, context.getResources().getString(R.string.esm_never_answered), Toast.LENGTH_LONG).show();
            context.stopService(new Intent(context, Browser_Service.class));
        }
    }
}