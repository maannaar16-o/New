package com.yahia.oneclear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Required so the app is eligible to be the default SMS app (only the default
 * SMS app may delete the SMS provider). This app is a wiper, not a messenger,
 * so it intentionally does nothing with delivered messages.
 */
public class SmsDeliverReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // No-op: see class comment.
    }
}
