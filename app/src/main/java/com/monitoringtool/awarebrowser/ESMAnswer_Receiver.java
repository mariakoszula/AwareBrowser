package com.monitoringtool.awarebrowser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.ESM;
import com.aware.providers.ESM_Provider;

/**
 * Created by Maria on 2015-07-05.
 */
/*After ESM is answered run WebService */


public class ESMAnswer_Receiver extends BroadcastReceiver {

    private static final String LOG_TAG_ESM = "WPL:ESM";
    private static final int esmToAnswer = 4;
    private int esmAnsweredCount = 0;
    private boolean wasRepeated = false;
    private static final int WAIT_TIME_FOR_SEND_DATA = 5 * 1000;


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
            contexts[0].sendBroadcast(new Intent(Aware.ACTION_AWARE_SYNC_DATA));
            try {
                Thread.sleep(WAIT_TIME_FOR_SEND_DATA);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return contexts[0];
        }

        @Override
        protected void onPostExecute(Context context) {
            if (ToolbarActivity.MONITORING_DEBUG_FLAG)
                Log.d(ToolbarActivity.LOG_TAG, "Webservices event: Data was send to the server");

            //stop BrowserService;
            context.stopService(new Intent(context, Browser_Service.class));
            if (ToolbarActivity.MONITORING_DEBUG_FLAG)
                Log.d(LOG_TAG_ESM, "Stop Browser_Service");
            Toast.makeText(context, context.getResources().getString(R.string.can_turn_off_data), Toast.LENGTH_LONG).show();
        }
    }


    private void sendActionCloseBrowser(Context context) {
        if (!wasRepeated) {
            Intent browserClosed = new Intent();
            browserClosed.setAction(ToolbarActivity.ACTION_CLOSE_BROWSER);
            context.sendBroadcast(browserClosed);
            wasRepeated = true;
        }else{
            if(ToolbarActivity.MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_ESM, "ESM was not answered");
            context.stopService(new Intent(context, Browser_Service.class));
        }
    }
}