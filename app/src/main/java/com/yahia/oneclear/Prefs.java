package com.yahia.oneclear;

import android.content.Context;
import android.content.SharedPreferences;

/** Stores the single "ask before wiping" flag. */
public final class Prefs {
    private static final String FILE = "onetapclear";
    private static final String KEY_CONFIRM = "confirm_before_wipe";

    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static boolean isConfirmEnabled(Context c) {
        return sp(c).getBoolean(KEY_CONFIRM, true);
    }

    public static void setConfirmEnabled(Context c, boolean v) {
        sp(c).edit().putBoolean(KEY_CONFIRM, v).apply();
    }
}
