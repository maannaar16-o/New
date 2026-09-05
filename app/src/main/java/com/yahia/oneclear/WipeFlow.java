package com.yahia.oneclear;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.CallLog;
import android.provider.Settings;
import android.provider.Telephony;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure helpers that perform (or point the user at) the three requested wipes.
 * All async UI (permission prompts, default-SMS role picker) is driven by
 * {@link MainActivity}; this class only does work that can run synchronously.
 */
public final class WipeFlow {
    private WipeFlow() {}

    /** Deletes every row in the call log. Returns rows deleted, or -1 on failure. */
    public static int wipeCallLog(Context c) {
        try {
            return c.getContentResolver().delete(CallLog.Calls.CONTENT_URI, null, null);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Deletes every SMS. Only works when this app is the default SMS app; the
     * platform silently ignores the delete otherwise. Returns rows deleted, or
     * -1 on failure.
     */
    public static int wipeSms(Context c) {
        try {
            return c.getContentResolver().delete(Telephony.Sms.CONTENT_URI, null, null);
        } catch (Exception e) {
            return -1;
        }
    }

    public static boolean hasWriteCallLog(Context c) {
        return c.checkSelfPermission(Manifest.permission.WRITE_CALL_LOG)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Performs the whole wipe with no dialogs and returns a short Arabic summary.
     * Assumes setup is already done (permissions granted, default SMS app set);
     * anything not set up is reported in the summary rather than prompted for.
     * Safe to call off the main thread.
     */
    public static String runSilentWipe(Context c) {
        String callsLine;
        if (hasWriteCallLog(c)) {
            int calls = wipeCallLog(c);
            callsLine = (calls >= 0) ? ("المكالمات: حُذف " + calls) : "المكالمات: تعذّر";
        } else {
            callsLine = "المكالمات: تخطّي (افتح التطبيق واقبل الصلاحية)";
        }

        String smsLine;
        if (isDefaultSms(c)) {
            int sms = wipeSms(c);
            smsLine = (sms >= 0) ? ("الرسائل: حُذف " + sms) : "الرسائل: تعذّر";
        } else {
            smsLine = "الرسائل: تخطّي (اجعل التطبيق الافتراضي من داخله)";
        }

        return callsLine + " • " + smsLine;
    }

    /** True if this app currently holds the default-SMS role. */
    public static boolean isDefaultSms(Context c) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RoleManager rm = (RoleManager) c.getSystemService(Context.ROLE_SERVICE);
                return rm != null && rm.isRoleHeld(RoleManager.ROLE_SMS);
            }
            String def = Telephony.Sms.getDefaultSmsPackage(c);
            return def != null && def.equals(c.getPackageName());
        } catch (Exception e) {
            return false;
        }
    }

    /** Human-readable list of accounts currently on the device (best effort). */
    public static List<String> listAccounts(Context c) {
        List<String> out = new ArrayList<>();
        try {
            AccountManager am = AccountManager.get(c);
            for (Account a : am.getAccounts()) {
                out.add(a.name + "  (" + a.type + ")");
            }
        } catch (Exception ignored) {}
        return out;
    }

    /** Opens the system Accounts screen so the user can remove accounts by hand. */
    public static void openAccountsSettings(Context c) {
        Intent[] candidates = new Intent[] {
                new Intent(Settings.ACTION_SYNC_SETTINGS),
                new Intent("android.settings.ACCOUNT_SYNC_SETTINGS"),
                new Intent(Settings.ACTION_SETTINGS)
        };
        for (Intent i : candidates) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                c.startActivity(i);
                return;
            } catch (Exception ignored) {}
        }
    }

    /** Opens the "default SMS app" chooser so the user can restore their messenger. */
    public static void openDefaultSmsSettings(Context c) {
        Intent[] candidates = new Intent[] {
                new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
                new Intent("android.settings.MANAGE_DEFAULT_APPS_SETTINGS"),
                new Intent(Settings.ACTION_SETTINGS)
        };
        for (Intent i : candidates) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                c.startActivity(i);
                return;
            } catch (Exception ignored) {}
        }
    }

    /** Intent that opens per-app settings, used only as a fallback helper. */
    public static Intent appDetailsIntent(Context c) {
        return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + c.getPackageName()));
    }
}
