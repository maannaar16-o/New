package com.yahia.oneclear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Required for default-SMS-app eligibility (MMS/WAP push). No-op by design.
 */
public class MmsDeliverReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // No-op.
    }
}
