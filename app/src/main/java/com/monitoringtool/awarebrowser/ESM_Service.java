package com.monitoringtool.awarebrowser;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ESM;
import com.aware.providers.ESM_Provider;

/**
 * Created by Maria on 2015-07-02.
 */
public class ESM_Service extends Service{
    private static final int WAIT_TIME = 1 * 60 * 1000;

    private ESM_Status_Receiver esm_statuses;
    @Override
    public void onCreate() {
        super.onCreate();

        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, true);
        sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));

        Intent browserClosed = new Intent();
        browserClosed.setAction(ToolbarActivity.ACTION_CLOSE_BROWSER);
        sendBroadcast(browserClosed);
        Toast.makeText(getApplicationContext(), getResources().getString(R.string.esm_answer), Toast.LENGTH_LONG).show();


        if (ToolbarActivity.MONITORING_DEBUG_FLAG)
            Log.d(ToolbarActivity.LOG_TAG, "Send broadcast about stopping");

        Intent browserService = new Intent(getApplicationContext(), Browser_Service.class);
        getApplicationContext().startService(browserService);

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ToolbarActivity.MONITORING_DEBUG_FLAG) Log.d(ToolbarActivity.LOG_TAG, "Start Service");
        final Handler stopService = new Handler();
        stopService.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (ToolbarActivity.MONITORING_DEBUG_FLAG)
                    Log.d(ToolbarActivity.LOG_TAG, "Stop service");
                    sendBroadcast(new Intent(Aware.ACTION_AWARE_SYNC_DATA));
                    stopSelf();
            }
        }, WAIT_TIME);

        return super.onStartCommand(intent, flags, startId);

    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, false);
        if(esm_statuses != null)   unregisterReceiver(esm_statuses);
        sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));

    }

    public class ESM_Status_Receiver extends BroadcastReceiver {
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

                //stopSelf();
            }
        }
    }
}
