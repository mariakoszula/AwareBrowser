package com.monitoringtool.awarebrowser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
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

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(ESM.ACTION_AWARE_ESM_EXPIRED)) {
            if(ToolbarActivity.MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_ESM, "ESM expired send BrowserClosed message again.");

            Toast.makeText(context, "ESM expired. Wait for re-run.", Toast.LENGTH_LONG).show();
            Intent browserClosed = new Intent();
            browserClosed.setAction(ToolbarActivity.ACTION_CLOSE_BROWSER);
            context.sendBroadcast(browserClosed);

        } else if (intent.getAction().equals(ESM.ACTION_AWARE_ESM_DISMISSED)) {
            if(ToolbarActivity.MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_ESM, "ESM dismissed. Call BrowserClosed intent agian.");
            Toast.makeText(context, "ESM dismissed. Wait for re-run.", Toast.LENGTH_LONG).show();

            Intent browserClosed = new Intent();
            browserClosed.setAction(ToolbarActivity.ACTION_CLOSE_BROWSER);
            context.sendBroadcast(browserClosed);

        } else if (intent.getAction().equals(ESM.ACTION_AWARE_ESM_ANSWERED)) {
            if(ToolbarActivity.MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_ESM, "Yupi! ESM answered.");

            //AysncTask sendBroadcast to synchronize with server and end Browser_service
           // sendBroadcast(new Intent(Aware.ACTION_AWARE_SYNC_DATA)); and kill service after this
            if(ToolbarActivity.MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_ESM, "Send data to the server");

            //stop BrowserService;
            context.stopService(new Intent(context, Browser_Service.class));
            if(ToolbarActivity.MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_ESM, "Stop Browser_Service");
        }
    }
}