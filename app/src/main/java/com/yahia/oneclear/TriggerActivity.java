package com.yahia.oneclear;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Invisible activity launched by the home-screen widget. It just opens
 * MainActivity in autostart mode, because the wipe needs an activity to host
 * the permission prompts and the default-SMS role picker.
 */
public class TriggerActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra(MainActivity.EXTRA_AUTOSTART, true);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(i);
        finish();
    }
}
