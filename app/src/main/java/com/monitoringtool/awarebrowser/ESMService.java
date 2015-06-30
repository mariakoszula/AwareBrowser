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
import android.widget.Toolbar;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ESM;
import com.aware.providers.ESM_Provider;

import java.util.logging.LogRecord;

public class ESMService extends Service {

    private static final int WAIT_TIME = 2 * 60 * 1000;


    @Override
    public void onCreate() {
        super.onCreate();
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, true);
        sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));

        Intent browserClosed = new Intent();
        browserClosed.setAction(ToolbarActivity.ACTION_CLOSE_BROWSER);
        sendBroadcast(browserClosed);
        Toast.makeText(getApplicationContext(), getResources().getString(R.string.esm_answer), Toast.LENGTH_SHORT).show();

        if (ToolbarActivity.MONITORING_DEBUG_FLAG)
            Log.d(ToolbarActivity.LOG_TAG, "Send broadcast about stopping");

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

        sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));

    }
}

