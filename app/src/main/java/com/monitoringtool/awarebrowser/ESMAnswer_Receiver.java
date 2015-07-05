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
public class ESMAnswer_Receiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(ESM.ACTION_AWARE_ESM_EXPIRED)) {
            Log.d(ToolbarActivity.LOG_TAG, "ESM expired could end service");

            Toast.makeText(context, "ESM expired.", Toast.LENGTH_LONG).show();
        } else if (intent.getAction().equals(ESM.ACTION_AWARE_ESM_DISMISSED)) {
            Log.d(ToolbarActivity.LOG_TAG, "ESM dismissed could end service");

            Toast.makeText(context, "ESM dismissed.", Toast.LENGTH_LONG).show();
        } else if (intent.getAction().equals(ESM.ACTION_AWARE_ESM_ANSWERED)) {
            Log.d(ToolbarActivity.LOG_TAG, "ESM answered could end service");
            if(ToolbarActivity.MONITORING_DEBUG_FLAG) Log.d(ToolbarActivity.LOG_TAG, "Yeey, esm asnwered");
            //AysncTask sendBroadcast to synchronize with server and end Browser_service
           // sendBroadcast(new Intent(Aware.ACTION_AWARE_SYNC_DATA));
            //stopSelf();
        } else if(intent.getAction().equals(ESM.ACTION_AWARE_ESM_QUEUE_COMPLETE)){
            Log.d(ToolbarActivity.LOG_TAG, "ESM queue completa");

            Toast.makeText(context, "ESM queueu complete.", Toast.LENGTH_LONG).show();
            //sendBroadcast(new Intent(Aware.ACTION_AWARE_SYNC_DATA));
            //stopSelf();
        }
    }
}