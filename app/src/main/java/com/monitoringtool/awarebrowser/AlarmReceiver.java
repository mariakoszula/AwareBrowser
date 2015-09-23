package com.monitoringtool.awarebrowser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class AlarmReceiver extends BroadcastReceiver {
    public AlarmReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent startNotificationService = new Intent(context, NotificationService.class);
        context.startService(startNotificationService);

    }
}
