package com.yahia.oneclear;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

/**
 * Invisible (translucent) activity launched by the home-screen widget. It does
 * the wipe in the background and finishes without ever showing the app's UI —
 * this is the "true one tap" entry point.
 *
 * The app must already be set up (permissions granted + default SMS app), which
 * the user does once from MainActivity. This screen never prompts for those.
 */
public class TriggerActivity extends Activity {

    private final Handler main = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Prefs.isConfirmEnabled(this)) {
            new AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_Alert)
                    .setTitle("تأكيد المسح النهائي")
                    .setMessage("حذف كل سجل المكالمات وكل الرسائل نهائيًا بدون رجعة. متأكد؟")
                    .setPositiveButton("نعم، امسح", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface d, int w) { doWipe(); }
                    })
                    .setNegativeButton("إلغاء", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface d, int w) { finish(); }
                    })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        public void onCancel(DialogInterface d) { finish(); }
                    })
                    .show();
        } else {
            doWipe();
        }
    }

    private void doWipe() {
        Toast.makeText(this, "جارٍ المسح…", Toast.LENGTH_SHORT).show();
        final boolean openAccounts = Prefs.isOpenAccountsEnabled(this);
        new Thread(new Runnable() {
            @Override public void run() {
                final String summary = WipeFlow.runSilentWipe(TriggerActivity.this);
                main.post(new Runnable() {
                    @Override public void run() {
                        Toast.makeText(TriggerActivity.this, summary, Toast.LENGTH_LONG).show();
                        if (openAccounts) {
                            WipeFlow.openAccountsSettings(TriggerActivity.this);
                        }
                        finish();
                    }
                });
            }
        }).start();
    }
}
