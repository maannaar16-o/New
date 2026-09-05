package com.yahia.oneclear;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.Locale;

/**
 * Automates, on the user's own device and with their own screen taps, the
 * sequence to wipe Facebook Lite's data:
 *   App info → (Storage) → Clear cache → Clear storage / Manage space →
 *   (in Facebook Lite's Manage-Space screen) tick every option → Clear → confirm.
 *
 * It only runs while {@link #running} is set (the user starts it explicitly),
 * and stops on completion or timeout. Button labels differ by phone brand and
 * language, so the label arrays below are the place to add your device's exact
 * wording if a step is missed.
 */
public class FbClearService extends AccessibilityService {

    public static FbClearService instance;

    public static final String FB_PKG = "com.facebook.lite";

    // Steps
    private static final int IDLE = 0;
    private static final int GO_STORAGE = 1;   // reach the Storage screen if needed
    private static final int CLEAR_CACHE = 2;  // tap "Clear cache"
    private static final int MANAGE = 3;       // tap "Clear storage" / "Manage space"
    private static final int FB_SELECT = 4;    // in Facebook Lite: tick all + tap Clear
    private static final int CONFIRM = 5;      // confirm dialog

    private static final long OVERALL_TIMEOUT_MS = 30000;
    private static final long STEP_TIMEOUT_MS = 9000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean running = false;
    private int step = IDLE;
    private long startedAt = 0;
    private long stepStartedAt = 0;
    private int tickToken = 0;
    private boolean cacheClicked = false;
    private boolean manageClicked = false;
    private boolean clearClicked = false;
    private int confirmClicks = 0;

    // ===== Labels (lower-case). Arabic + English + common variants. =====
    private static final String[] STORAGE_NAV = {
            "storage & cache", "storage and cache", "storage usage", "storage",
            "التخزين والذاكرة المؤقتة", "التخزين والذاكرة", "وحدة التخزين",
            "مساحة التخزين", "استخدام وحدة التخزين", "التخزين"
    };
    private static final String[] CLEAR_CACHE = {
            "clear cache", "empty cache",
            "محو ذاكرة التخزين المؤقت", "مسح ذاكرة التخزين المؤقت",
            "مسح التخزين المؤقت", "إفراغ ذاكرة التخزين المؤقت", "مسح الذاكرة المؤقتة"
    };
    // Opens Facebook Lite's Manage-Space screen (FB declares a manageSpaceActivity,
    // so "Clear storage/data" launches it instead of the system dialog):
    private static final String[] MANAGE_SPACE = {
            "manage space", "manage storage", "clear storage", "clear data", "clear all data",
            "إدارة المساحة", "إدارة مساحة التخزين", "إدارة التخزين",
            "مسح مساحة التخزين", "مسح البيانات", "مسح وحدة التخزين", "مسح كل البيانات"
    };
    private static final String[] SELECT_ALL = {
            "select all", "check all",
            "تحديد الكل", "اختيار الكل", "تحديد جميع", "تحديد الكلّ"
    };
    // The button that actually clears inside FB's Manage-Space screen:
    private static final String[] CLEAR_BTN = {
            "clear data", "clear", "delete", "erase",
            "مسح البيانات", "مسح", "حذف", "إزالة", "امسح"
    };
    private static final String[] CONFIRM_BTN = {
            "delete", "ok", "okay", "yes", "erase", "clear", "clear all data", "continue",
            "حذف", "موافق", "نعم", "تم", "مسح", "متابعة", "استمرار", "تأكيد"
    };
    private static final String[] CANCEL_EXCLUDE = {
            "cancel", "إلغاء", "الغاء", "later", "لا،"
    };
    private static final String[] CACHE_ONLY_EXCLUDE = {
            "cache", "الذاكرة المؤقتة", "المؤقتة", "المؤقت", "الكاش"
    };
    // ====================================================================

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public void onDestroy() {
        if (instance == this) instance = null;
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) { /* driven by ticks */ }

    @Override
    public void onInterrupt() { }

    /** Called from the app to start the Facebook Lite wipe flow. */
    public void startFacebookLite() {
        running = true;
        step = GO_STORAGE;
        cacheClicked = manageClicked = clearClicked = false;
        confirmClicks = 0;
        startedAt = stepStartedAt = System.currentTimeMillis();
        openAppInfo(FB_PKG);
        scheduleTick(1000);
    }

    private void openAppInfo(String pkg) {
        try {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + pkg));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK
                    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivity(i);
        } catch (Exception ignored) {}
    }

    private void scheduleTick(long delayMs) {
        final int token = ++tickToken;
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                if (token != tickToken) return;
                tick();
            }
        }, delayMs);
    }

    private void setStep(int s) {
        step = s;
        stepStartedAt = System.currentTimeMillis();
    }

    private void tick() {
        if (!running) { step = IDLE; return; }

        long now = System.currentTimeMillis();
        if (now - startedAt > OVERALL_TIMEOUT_MS) {
            finish("انتهى الوقت — بعض الخطوات قد تحتاج ضبط أسماء الأزرار");
            return;
        }
        if (now - stepStartedAt > STEP_TIMEOUT_MS) {
            // Give up this step; try to advance sensibly.
            if (step == GO_STORAGE) { setStep(CLEAR_CACHE); }
            else if (step == CLEAR_CACHE) { setStep(MANAGE); }
            else if (step == MANAGE) { finish("تعذّر فتح «إدارة المساحة» — عدّل الأسماء في MANAGE_SPACE"); return; }
            else if (step == FB_SELECT) { setStep(CONFIRM); }
            else { finish("خلص"); return; }
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) { scheduleTick(500); return; }

        CharSequence pkg = root.getPackageName();
        boolean onFb = pkg != null && FB_PKG.contentEquals(pkg);

        switch (step) {
            case GO_STORAGE: {
                // If clear-cache or manage is already visible, skip navigation.
                if (find(root, CLEAR_CACHE, null) != null || find(root, MANAGE_SPACE, CACHE_ONLY_EXCLUDE) != null) {
                    setStep(CLEAR_CACHE);
                    scheduleTick(300);
                    return;
                }
                AccessibilityNodeInfo nav = find(root, STORAGE_NAV, null);
                if (nav != null) { click(nav); }
                scheduleTick(900);
                return;
            }
            case CLEAR_CACHE: {
                if (!cacheClicked) {
                    AccessibilityNodeInfo c = find(root, CLEAR_CACHE, null);
                    if (c != null) { click(c); cacheClicked = true; scheduleTick(900); return; }
                }
                setStep(MANAGE);
                scheduleTick(400);
                return;
            }
            case MANAGE: {
                AccessibilityNodeInfo m = find(root, MANAGE_SPACE, CACHE_ONLY_EXCLUDE);
                if (m != null && !manageClicked) {
                    click(m); manageClicked = true;
                    setStep(FB_SELECT);
                    scheduleTick(1400);
                    return;
                }
                scheduleTick(500);
                return;
            }
            case FB_SELECT: {
                if (!onFb) { scheduleTick(500); return; } // wait for FB's screen
                // Tick every option (accounts, settings, etc.).
                AccessibilityNodeInfo sa = find(root, SELECT_ALL, null);
                if (sa != null) { click(sa); }
                checkAllCheckables(root);
                if (!clearClicked) {
                    AccessibilityNodeInfo clr = find(root, CLEAR_BTN, CANCEL_EXCLUDE);
                    if (clr != null) { click(clr); clearClicked = true; setStep(CONFIRM); scheduleTick(1000); return; }
                }
                scheduleTick(700);
                return;
            }
            case CONFIRM: {
                AccessibilityNodeInfo cf = find(root, CONFIRM_BTN, CANCEL_EXCLUDE);
                if (cf != null) { click(cf); confirmClicks++; scheduleTick(900); return; }
                if (confirmClicks >= 1) { finish("تم مسح بيانات فيسبوك لايت ✔"); return; }
                scheduleTick(500);
                return;
            }
            default:
                scheduleTick(500);
        }
    }

    private void finish(final String msg) {
        running = false;
        step = IDLE;
        handler.post(new Runnable() {
            @Override public void run() {
                Toast.makeText(FbClearService.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ---- node helpers ----

    private void checkAllCheckables(AccessibilityNodeInfo node) {
        if (node == null) return;
        try {
            if (node.isCheckable() && !node.isChecked() && node.isEnabled()) {
                AccessibilityNodeInfo t = clickableSelfOrAncestor(node);
                if (t != null) t.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        } catch (Exception ignored) {}
        int n = node.getChildCount();
        for (int i = 0; i < n; i++) {
            checkAllCheckables(node.getChild(i));
        }
    }

    private AccessibilityNodeInfo find(AccessibilityNodeInfo node, String[] labels, String[] excludes) {
        if (node == null) return null;
        CharSequence t = node.getText();
        CharSequence d = node.getContentDescription();
        String hay = ((t == null ? "" : t) + " " + (d == null ? "" : d))
                .toLowerCase(Locale.ROOT).trim();
        if (!hay.isEmpty() && matches(hay, labels, excludes)) {
            AccessibilityNodeInfo click = clickableSelfOrAncestor(node);
            if (click != null) return click;
        }
        int n = node.getChildCount();
        for (int i = 0; i < n; i++) {
            AccessibilityNodeInfo r = find(node.getChild(i), labels, excludes);
            if (r != null) return r;
        }
        return null;
    }

    private boolean matches(String hay, String[] labels, String[] excludes) {
        if (excludes != null) {
            for (String ex : excludes) {
                if (hay.contains(ex.toLowerCase(Locale.ROOT))) return false;
            }
        }
        for (String lb : labels) {
            if (hay.contains(lb.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private AccessibilityNodeInfo clickableSelfOrAncestor(AccessibilityNodeInfo n) {
        AccessibilityNodeInfo cur = n;
        int guard = 0;
        while (cur != null && guard++ < 12) {
            if (cur.isClickable() && cur.isEnabled()) return cur;
            cur = cur.getParent();
        }
        return null;
    }

    private boolean click(AccessibilityNodeInfo n) {
        return n != null && n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }
}
