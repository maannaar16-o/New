package com.yahia.oneclear;

import android.content.Context;
import android.content.SharedPreferences;

/** Small on-device flags for the wipe behaviour. */
public final class Prefs {
    private static final String FILE = "onetapclear";
    private static final String KEY_CONFIRM = "confirm_before_wipe";
    private static final String KEY_OPEN_ACCOUNTS = "open_accounts_after_wipe";

    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    /** Whether to ask for confirmation before wiping. Applies to the button and the widget. */
    public static boolean isConfirmEnabled(Context c) {
        return sp(c).getBoolean(KEY_CONFIRM, true);
    }

    public static void setConfirmEnabled(Context c, boolean v) {
        sp(c).edit().putBoolean(KEY_CONFIRM, v).apply();
    }

    /** Whether to open the system Accounts screen after wiping (for manual account removal). */
    public static boolean isOpenAccountsEnabled(Context c) {
        return sp(c).getBoolean(KEY_OPEN_ACCOUNTS, true);
    }

    public static void setOpenAccountsEnabled(Context c, boolean v) {
        sp(c).edit().putBoolean(KEY_OPEN_ACCOUNTS, v).apply();
    }
}
