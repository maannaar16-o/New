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
 * Automates, on the user's own device and with their own taps, the steps to
 * wipe Facebook Lite's data:
 *   App info → (Storage) → Clear cache → Clear storage / Manage space →
 *   in Facebook Lite's Manage-Space screen:
 *     1) make sure "Clear all" (مسح الكل) is ticked (this ticks the caches),
 *     2) tick "Accounts and settings" (الحسابات والإعدادات) on its own — this
 *        pops a confirmation dialog,
 *     3) confirm that dialog,
 *     4) press the final Clear (مسح) and confirm.
 *
 * Runs only while a user-started flow is active; stops on completion/timeout.
 * Button wording differs by device/language — tune the label arrays below.
 */
public class FbClearService extends AccessibilityService {

    public static FbClearService instance;

    public static final String FB_PKG = "com.facebook.lite";

    // Steps
    private static final int IDLE = 0;
    private static final int GO_STORAGE = 1;
    private static final int CLEAR_CACHE = 2;
    private static final int MANAGE = 3;
    private static final int FB_SELECT_ALL = 4;   // ensure "مسح الكل" ticked
    private static final int FB_ACCOUNTS = 5;     // tick "الحسابات والإعدادات"
    private static final int FB_ACCOUNTS_CONFIRM = 6; // confirm its dialog
    private static final int FB_FINAL_CLEAR = 7;  // press "مسح"
    private static final int FB_FINAL_CONFIRM = 8; // confirm final dialog

    private static final long OVERALL_TIMEOUT_MS = 40000;
    private static final long STEP_TIMEOUT_MS = 9000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean running = false;
    private int step = IDLE;
    private long startedAt = 0;
    private long stepStartedAt = 0;
    private int tickToken = 0;
    private boolean cacheClicked = false;
    private boolean manageClicked = false;
    private boolean accountsClicked = false;
    private boolean accountsConfirmed = false;
    private boolean finalClearClicked = false;
    private int finalConfirmClicks = 0;

    // ===== Labels (lower-case). Arabic + English + variants. =====
    private static final String[] STORAGE_NAV = {
            "storage & cache", "storage and cache", "storage usage", "storage",
            "التخزين والذاكرة المؤقتة", "التخزين والذاكرة", "وحدة التخزين",
            "مساحة التخزين", "استخدام وحدة التخزين", "التخزين"
    };
    private static final String[] CACHE_LABELS = {
            "clear cache", "empty cache",
            "محو ذاكرة التخزين المؤقت", "مسح ذاكرة التخزين المؤقت",
            "مسح التخزين المؤقت", "إفراغ ذاكرة التخزين المؤقت", "مسح الذاكرة المؤقتة"
    };
    private static final String[] MANAGE_SPACE = {
            "manage space", "manage storage", "clear storage", "clear data", "clear all data",
            "إدارة المساحة", "إدارة مساحة التخزين", "إدارة التخزين",
            "مسح مساحة التخزين", "مسح البيانات", "مسح وحدة التخزين", "مسح كل البيانات"
    };
    // The "Clear all" master checkbox inside FB's Manage-Space screen:
    private static final String[] SELECT_ALL = {
            "مسح الكل", "select all", "clear all", "تحديد الكل", "اختيار الكل"
    };
    // The "Accounts and settings" row (must be ticked on its own):
    private static final String[] ACCOUNTS_SETTINGS = {
            "الحسابات والإعدادات", "الحسابات و الإعدادات",
            "accounts and settings", "accounts & settings"
    };
    // Affirmative button in the dialog that appears after ticking accounts.
    // (Deliberately excludes "مسح"/"clear" so it never grabs the Clear button early.)
    private static final String[] DIALOG_CONFIRM = {
            "متابعة", "استمرار", "موافق", "نعم", "تأكيد", "تفعيل", "تحديد",
            "continue", "ok", "okay", "yes", "enable", "confirm", "select"
    };
    // The final Clear button on the Manage-Space screen:
    private static final String[] FINAL_CLEAR = {
            "مسح البيانات", "clear data", "حذف البيانات", "مسح", "clear", "erase", "حذف"
    };
    // The final "are you sure" dialog after pressing Clear:
    private static final String[] FINAL_CONFIRM = {
            "مسح", "حذف", "موافق", "نعم", "تأكيد", "متابعة",
            "delete", "ok", "okay", "yes", "erase", "clear", "confirm"
    };
    private static final String[] CANCEL_EXCLUDE = {
            "cancel", "إلغاء", "الغاء", "later", "لا،", "رجوع", "back"
    };
    // Keep the final-Clear search off the master checkbox, caches, accounts and header.
    private static final String[] NOT_CLEAR_EXCLUDE = {
            "الكل", "all", "cache", "الذاكرة", "المؤقت", "المؤقتة",
            "الحسابات", "accounts", "وحدة التخزين", "على هاتفك",
            "الصور", "الفيديو", "مقاطع", "أخرى", "settings", "الإعدادات"
    };
    private static final String[] CACHE_ONLY_EXCLUDE = {
            "cache", "الذاكرة المؤقتة", "المؤقتة", "المؤقت", "الكاش"
    };
    // ============================================================

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

    public void startFacebookLite() {
        running = true;
        step = GO_STORAGE;
        cacheClicked = manageClicked = accountsClicked = accountsConfirmed = finalClearClicked = false;
        finalConfirmClicks = 0;
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
            switch (step) {
                case GO_STORAGE: setStep(CLEAR_CACHE); break;
                case CLEAR_CACHE: setStep(MANAGE); break;
                case MANAGE: finish("تعذّر فتح «إدارة المساحة»"); return;
                case FB_SELECT_ALL: setStep(FB_ACCOUNTS); break;
                case FB_ACCOUNTS: finish("تعذّر تحديد «الحسابات والإعدادات» — ابعت صورة الشاشة"); return;
                case FB_ACCOUNTS_CONFIRM: setStep(FB_FINAL_CLEAR); break;
                case FB_FINAL_CLEAR: finish("تعذّر إيجاد زر «مسح» النهائي — ابعت صورة الشاشة"); return;
                case FB_FINAL_CONFIRM: finish("تم مسح بيانات فيسبوك لايت ✔"); return;
                default: finish("خلص"); return;
            }
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) { scheduleTick(500); return; }

        CharSequence pkg = root.getPackageName();
        boolean onFb = pkg != null && FB_PKG.contentEquals(pkg);

        switch (step) {
            case GO_STORAGE: {
                if (find(root, CACHE_LABELS, null) != null
                        || find(root, MANAGE_SPACE, CACHE_ONLY_EXCLUDE) != null) {
                    setStep(CLEAR_CACHE); scheduleTick(300); return;
                }
                AccessibilityNodeInfo nav = find(root, STORAGE_NAV, null);
                if (nav != null) click(nav);
                scheduleTick(900);
                return;
            }
            case CLEAR_CACHE: {
                if (!cacheClicked) {
                    AccessibilityNodeInfo c = find(root, CACHE_LABELS, null);
                    if (c != null) { click(c); cacheClicked = true; scheduleTick(900); return; }
                }
                setStep(MANAGE); scheduleTick(400); return;
            }
            case MANAGE: {
                if (!manageClicked) {
                    AccessibilityNodeInfo m = find(root, MANAGE_SPACE, CACHE_ONLY_EXCLUDE);
                    if (m != null) { click(m); manageClicked = true; setStep(FB_SELECT_ALL); scheduleTick(1400); return; }
                }
                scheduleTick(500); return;
            }
            case FB_SELECT_ALL: {
                if (!onFb) { scheduleTick(500); return; }
                // Tick "Clear all" only if it exists and is not already checked.
                AccessibilityNodeInfo sa = findCheckableByLabel(root, SELECT_ALL);
                if (sa != null && !sa.isChecked()) {
                    clickNode(sa);
                    scheduleTick(700);
                    return;
                }
                setStep(FB_ACCOUNTS); scheduleTick(500); return;
            }
            case FB_ACCOUNTS: {
                if (!onFb) { scheduleTick(500); return; }
                AccessibilityNodeInfo acc = findCheckableByLabel(root, ACCOUNTS_SETTINGS);
                if (acc != null && acc.isChecked()) { setStep(FB_FINAL_CLEAR); scheduleTick(500); return; }
                if (acc != null && !accountsClicked) {
                    clickNode(acc);
                    accountsClicked = true;
                    setStep(FB_ACCOUNTS_CONFIRM);
                    scheduleTick(900);
                    return;
                }
                scheduleTick(500); return;
            }
            case FB_ACCOUNTS_CONFIRM: {
                // A dialog appears after ticking accounts+settings; confirm it.
                AccessibilityNodeInfo cf = find(root, DIALOG_CONFIRM, CANCEL_EXCLUDE);
                if (cf != null && !accountsConfirmed) {
                    click(cf); accountsConfirmed = true; setStep(FB_FINAL_CLEAR); scheduleTick(1000); return;
                }
                scheduleTick(500); return;
            }
            case FB_FINAL_CLEAR: {
                AccessibilityNodeInfo clr = find(root, FINAL_CLEAR, NOT_CLEAR_EXCLUDE);
                if (clr != null && !finalClearClicked) {
                    click(clr); finalClearClicked = true; setStep(FB_FINAL_CONFIRM); scheduleTick(1000); return;
                }
                scheduleTick(500); return;
            }
            case FB_FINAL_CONFIRM: {
                AccessibilityNodeInfo cf = find(root, FINAL_CONFIRM, NOT_CLEAR_EXCLUDE);
                if (cf != null) { click(cf); finalConfirmClicks++; scheduleTick(900); return; }
                if (finalConfirmClicks >= 1) { finish("تم مسح بيانات فيسبوك لايت ✔"); return; }
                scheduleTick(500); return;
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

    /** First clickable node whose text/desc matches one of labels (respecting excludes). */
    private AccessibilityNodeInfo find(AccessibilityNodeInfo node, String[] labels, String[] excludes) {
        AccessibilityNodeInfo t = findTextNode(node, labels, excludes);
        return t == null ? null : clickableSelfOrAncestor(t);
    }

    /** First raw node whose text/desc matches (no clickable requirement). */
    private AccessibilityNodeInfo findTextNode(AccessibilityNodeInfo node, String[] labels, String[] excludes) {
        if (node == null) return null;
        CharSequence tx = node.getText();
        CharSequence d = node.getContentDescription();
        String hay = ((tx == null ? "" : tx) + " " + (d == null ? "" : d))
                .toLowerCase(Locale.ROOT).trim();
        if (!hay.isEmpty() && matches(hay, labels, excludes)) return node;
        int n = node.getChildCount();
        for (int i = 0; i < n; i++) {
            AccessibilityNodeInfo r = findTextNode(node.getChild(i), labels, excludes);
            if (r != null) return r;
        }
        return null;
    }

    /** The checkbox associated with a labelled row. */
    private AccessibilityNodeInfo findCheckableByLabel(AccessibilityNodeInfo root, String[] labels) {
        AccessibilityNodeInfo textNode = findTextNode(root, labels, null);
        if (textNode == null) return null;
        if (textNode.isCheckable()) return textNode;
        AccessibilityNodeInfo p = textNode;
        for (int up = 0; up < 5 && p != null; up++) {
            AccessibilityNodeInfo cb = findCheckable(p);
            if (cb != null) return cb;
            p = p.getParent();
        }
        return null;
    }

    private AccessibilityNodeInfo findCheckable(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isCheckable()) return node;
        int n = node.getChildCount();
        for (int i = 0; i < n; i++) {
            AccessibilityNodeInfo r = findCheckable(node.getChild(i));
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

    /** Click a (possibly non-clickable) checkable node via its clickable ancestor. */
    private boolean clickNode(AccessibilityNodeInfo n) {
        if (n == null) return false;
        AccessibilityNodeInfo c = clickableSelfOrAncestor(n);
        if (c == null) c = n;
        return c.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    private boolean click(AccessibilityNodeInfo n) {
        return n != null && n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }
}
