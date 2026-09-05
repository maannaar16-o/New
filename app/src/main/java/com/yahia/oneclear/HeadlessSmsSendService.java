package com.yahia.oneclear;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Required for default-SMS-app eligibility (respond-via-message). No-op:
 * this app does not send messages.
 */
public class HeadlessSmsSendService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        stopSelf(startId);
        return START_NOT_STICKY;
    }
}
