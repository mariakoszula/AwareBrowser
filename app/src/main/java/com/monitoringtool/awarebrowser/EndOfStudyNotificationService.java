package com.monitoringtool.awarebrowser;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

public class EndOfStudyNotificationService extends Service {
    private static int NOTIFICATION_ID = 1;
    private NotificationManager notificationManager;
    private PendingIntent pendingIntent;
    private static final int browserRequestCode = 9090;
    private static final String EXTRA_KEY_NOTIFICATION="notificationID";
    public EndOfStudyNotificationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Context appContext = getApplicationContext();
        notificationManager  = (NotificationManager) appContext.getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder buildNotification = new NotificationCompat.Builder(this);
        buildNotification.setSmallIcon(R.drawable.ic_browser);
        buildNotification.setContentTitle(this.getResources().getString(R.string.app_name));
        buildNotification.setContentText(this.getResources().getString(R.string.end_of_study_notification));
        notificationManager.notify(NOTIFICATION_ID, buildNotification.build());
        return super.onStartCommand(intent, flags, startId);
    }
}
