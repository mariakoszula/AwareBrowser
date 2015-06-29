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

import java.util.logging.LogRecord;

public class ESMService extends Service {

    private static final int WAIT_TIME = 3 * 60 * 1000;
    private ESMStatusListener esm_statuses;

    @Override
    public void onCreate() {
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, true);
        sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));

        IntentFilter esm_filter = new IntentFilter();
        esm_filter.addAction(ESM.ACTION_AWARE_ESM_DISMISSED);
        esm_filter.addAction(ESM.ACTION_AWARE_ESM_EXPIRED);
        esm_filter.addAction(ESM.ACTION_AWARE_ESM_ANSWERED);
        registerReceiver(esm_statuses, esm_filter);

        if(ToolbarActivity.MONITORING_DEBUG_FLAG) Log.d(ToolbarActivity.LOG_TAG, "Send broadcast about stopping");
        Intent browserClosed = new Intent();
        browserClosed.setAction(ToolbarActivity.ACTION_CLOSE_BROWSER);
        sendBroadcast(browserClosed);

        super.onCreate();
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(ToolbarActivity.MONITORING_DEBUG_FLAG) Log.d(ToolbarActivity.LOG_TAG, "Start Service");
        final Handler stopService = new Handler();
        stopService.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(ToolbarActivity.MONITORING_DEBUG_FLAG) Log.d(ToolbarActivity.LOG_TAG, "Stop service");
                stopSelf();
            }
        }, WAIT_TIME);

        return super.onStartCommand(intent, flags, startId);

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, false);
        sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));
        unregisterReceiver(esm_statuses);
        super.onDestroy();
    }

    public class ESMStatusListener extends BroadcastReceiver {
        /*Handling incoming ESM broadcasts, checking if it is not dismissed etc.*/
        @Override
        public void onReceive(Context context, Intent intent) {
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
                Toast.makeText(getApplicationContext(), "ESM expired.", Toast.LENGTH_LONG).show();
            } else if (intent.getAction().equals(ESM.ACTION_AWARE_ESM_DISMISSED)) {
                Toast.makeText(getApplicationContext(), "ESM dismissed.", Toast.LENGTH_LONG).show();
            } else if (intent.getAction().equals(ESM.ACTION_AWARE_ESM_ANSWERED)) {
                if(ToolbarActivity.MONITORING_DEBUG_FLAG) Log.d(ToolbarActivity.LOG_TAG, "Yeey, esm asnwered");
            }
        }
    }
}
