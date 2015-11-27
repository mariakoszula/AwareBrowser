package com.monitoringtool.awarebrowser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class EndOfStudyAlarmReceiver extends BroadcastReceiver {
    public EndOfStudyAlarmReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent startEndOfStudyNotificationService = new Intent(context, EndOfStudyNotificationService.class);
        context.startService(startEndOfStudyNotificationService);

    }
}
