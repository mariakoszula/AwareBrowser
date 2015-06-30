package com.monitoringtool.awarebrowser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.widget.Toast;

import com.aware.ESM;
import com.aware.providers.ESM_Provider;

/**
 * Created by Maria on 2015-06-30.
 */

  /*@TODO catch ACTION_ESM_ANSWER why this is not working?*/
public class ESMStatusReceiver extends BroadcastReceiver{
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
        }
    }
}

