package com.yahia.oneclear;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

/**
 * Required for default-SMS-app eligibility (the "compose message" activity).
 * This app cannot send messages, so it just tells the user and closes.
 */
public class ComposeSmsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toast.makeText(this,
                "هذا التطبيق للمسح فقط ولا يرسل رسائل. رجّع تطبيق الرسائل العادي.",
                Toast.LENGTH_LONG).show();
        finish();
    }
}
