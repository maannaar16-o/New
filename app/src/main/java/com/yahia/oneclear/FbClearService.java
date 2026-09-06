package com.yahia.oneclear;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.Locale;

/**
 * Automates, on the user's own device and with their own taps, wiping Facebook
 * Lite's data:
 *   App info → (Storage) → Clear cache → Clear storage / Manage space →
 *   in Facebook Lite's Manage-Space screen:
 *     1) ensure "Clear all" (مسح الكل) is ticked (ticks the caches),
 *     2) tick "Accounts and settings" (الحسابات والإعدادات) — it is greyed as
 *        "not recommended", so ACTION_CLICK is unreliable; we tap its real
 *        screen coordinates instead. This raises a confirmation dialog,
 *     3) press "موافق" in that dialog,
 *     4) press the final Clear (مسح).
 *
 * On the Facebook screen we use real gesture taps (dispatchGesture) because the
 * custom list items don't reliably respond to ACTION_CLICK. Runs only while a
 * user-started flow is active; stops on completion/timeout.
 */
public class FbClearService extends AccessibilityService {

    public static FbClearService instance;

    public static final String FB_PKG = "com.facebook.lite";
    public static final String FB_APP = "com.facebook.katana";

    /** The app whose data we are currently wiping (set when the flow starts). */
    private volatile String targetPkg = FB_PKG;

    private static final int IDLE = 0;
    private static final int GO_STORAGE = 1;
    private static final int CLEAR_CACHE = 2;
    private static final int MANAGE = 3;
    private static final int FB_SELECT_ALL = 4;
    private static final int FB_ACCOUNTS = 5;
    private static final int FB_ACCOUNTS_CONFIRM = 6;
    private static final int FB_FINAL_CLEAR = 7;
    private static final int FB_FINAL_CONFIRM = 8;

    private static final long OVERALL_TIMEOUT_MS = 60000;
    private static final long STEP_TIMEOUT_MS = 15000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean running = false;
    private int step = IDLE;
    private long startedAt = 0;
    private long stepStartedAt = 0;
    private int tickToken = 0;
    private boolean cacheClicked = false;
    private boolean manageClicked = false;
    private boolean accountsConfirmed = false;
    private boolean finalClearClicked = false;
    private int accountsTaps = 0;
    private int finalClearScrolls = 0;
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
    private static final String[] SELECT_ALL = {
            "مسح الكل", "select all", "clear all", "تحديد الكل", "اختيار الكل"
    };
    private static final String[] ACCOUNTS_SETTINGS = {
            "الحسابات والإعدادات", "الحسابات و الإعدادات",
            "accounts and settings", "accounts & settings"
    };
    // Affirmative in the "clear personal files and settings?" dialog (موافق).
    private static final String[] DIALOG_CONFIRM = {
            "موافق", "متابعة", "استمرار", "نعم", "تأكيد", "تفعيل",
            "ok", "okay", "continue", "yes", "enable", "confirm"
    };
    // The final blue Clear button (مسح) on the Manage-Space screen.
    private static final String[] FINAL_CLEAR = {
            "مسح البيانات", "clear data", "حذف البيانات", "مسح", "clear", "clean", "erase", "حذف"
    };
    // A possible final "are you sure" dialog (kept off the Clear button itself).
    private static final String[] FINAL_CONFIRM = {
            "موافق", "نعم", "تأكيد", "متابعة", "حذف", "delete", "ok", "yes", "confirm"
    };
    private static final String[] CANCEL_EXCLUDE = {
            "cancel", "إلغاء", "الغاء", "later", "لا،", "رجوع", "back"
    };
    // Keep the final-Clear search off the master checkbox, caches, accounts, header.
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

    /** Start the automated clear-data flow for the given app package. */
    public void startFor(String pkg) {
        targetPkg = (pkg == null || pkg.isEmpty()) ? FB_PKG : pkg;
        running = true;
        step = GO_STORAGE;
        cacheClicked = manageClicked = accountsConfirmed = finalClearClicked = false;
        accountsTaps = 0;
        finalClearScrolls = 0;
        finalConfirmClicks = 0;
        startedAt = stepStartedAt = System.currentTimeMillis();
        openAppInfo(targetPkg);
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
            finish("انتهى الوقت — بعض الخطوات قد تحتاج ضبط");
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
                case FB_FINAL_CONFIRM: finish("تم مسح بيانات التطبيق ✔"); return;
                default: finish("خلص"); return;
            }
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) { scheduleTick(500); return; }

        CharSequence pkg = root.getPackageName();
        boolean onFb = pkg != null && targetPkg != null && targetPkg.contentEquals(pkg);

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
                    if (m != null) { click(m); manageClicked = true; setStep(FB_SELECT_ALL); scheduleTick(1500); return; }
                }
                scheduleTick(500); return;
            }
            case FB_SELECT_ALL: {
                if (!onFb) { scheduleTick(500); return; }
                AccessibilityNodeInfo sa = findCheckableByLabel(root, SELECT_ALL);
                if (sa != null && !sa.isChecked()) { gestureTap(sa); scheduleTick(800); return; }
                setStep(FB_ACCOUNTS); scheduleTick(500); return;
            }
            case FB_ACCOUNTS: {
                if (!onFb) { scheduleTick(500); return; }
                AccessibilityNodeInfo accBox = findCheckableByLabel(root, ACCOUNTS_SETTINGS);
                if (accBox != null && accBox.isChecked()) { setStep(FB_FINAL_CLEAR); scheduleTick(500); return; }
                // Tap the accounts row with a real gesture. Vary the target across
                // retries: the checkbox, then the whole row, then the label.
                AccessibilityNodeInfo target = null;
                AccessibilityNodeInfo txt = findTextNode(root, ACCOUNTS_SETTINGS, null);
                if (accountsTaps == 0 && accBox != null) target = accBox;
                else if (txt != null) target = (accountsTaps == 1) ? rowOf(txt) : txt;
                else target = accBox;
                if (target != null) {
                    gestureTap(target);
                    accountsTaps++;
                    setStep(FB_ACCOUNTS_CONFIRM);
                    scheduleTick(1100);
                    return;
                }
                scheduleTick(500); return;
            }
            case FB_ACCOUNTS_CONFIRM: {
                // Dialog "هل تريد مسح الملفات الشخصية والإعدادات" → tap موافق.
                AccessibilityNodeInfo ok = findTextNode(root, DIALOG_CONFIRM, CANCEL_EXCLUDE);
                if (ok != null) {
                    gestureTap(ok); accountsConfirmed = true;
                    setStep(FB_FINAL_CLEAR); scheduleTick(1300); return;
                }
                // No dialog appeared → the tap missed; retry the accounts tap.
                if (accountsTaps < 5) { setStep(FB_ACCOUNTS); scheduleTick(700); return; }
                setStep(FB_FINAL_CLEAR); scheduleTick(700); return;
            }
            case FB_FINAL_CLEAR: {
                if (finalClearClicked) { setStep(FB_FINAL_CONFIRM); scheduleTick(600); return; }
                DisplayMetrics dm = getResources().getDisplayMetrics();
                // Phase 1: scroll to the bottom. Facebook Lite draws its own UI, so in
                // Arabic the "مسح" button sits below the fold and there is no scrollable
                // node — a few strong swipes bring it into view.
                if (finalClearScrolls < 3) {
                    finalClearScrolls++;
                    if (!scrollForward(root)) scrollDown();
                    scheduleTick(850);
                    return;
                }
                // Phase 2: tap Clear. Prefer a real on-screen button node; otherwise tap
                // the fixed bottom-centre where the full-width Clear bar always sits
                // (its text isn't exposed to accessibility on FB Lite).
                AccessibilityNodeInfo btn = findActionButton(root, dm.widthPixels, dm.heightPixels);
                boolean tapped = false;
                if (btn != null) {
                    Rect r = new Rect();
                    btn.getBoundsInScreen(r);
                    if (r.centerY() > 0 && r.centerY() < dm.heightPixels) {
                        gestureTap(btn);
                        AccessibilityNodeInfo c = clickableSelfOrAncestor(btn);
                        if (c != null) c.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        tapped = true;
                    }
                }
                if (!tapped) {
                    tapXY(dm.widthPixels * 0.5f, dm.heightPixels * 0.88f);
                }
                finalClearClicked = true;
                setStep(FB_FINAL_CONFIRM);
                scheduleTick(1500);
                return;
            }
            case FB_FINAL_CONFIRM: {
                AccessibilityNodeInfo cf = findTextNode(root, FINAL_CONFIRM, CANCEL_EXCLUDE);
                if (cf != null) { gestureTap(cf); finalConfirmClicks++; scheduleTick(900); return; }
                // Most devices clear directly with no extra dialog; don't linger.
                if (finalConfirmClicks >= 1 || (now - stepStartedAt) > 2500) {
                    finish("تم مسح بيانات التطبيق ✔");
                    return;
                }
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
                returnToApp();
            }
        });
    }

    /** Bring our own app back to the front after the flow ends. */
    private void returnToApp() {
        try {
            Intent i = new Intent(FbClearService.this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(i);
        } catch (Exception ignored) {}
    }

    // ---- gesture (real coordinate tap) ----

    private void gestureTap(AccessibilityNodeInfo n) {
        if (n == null) return;
        Rect r = new Rect();
        n.getBoundsInScreen(r);
        if (r.width() <= 0 || r.height() <= 0) return;
        Path path = new Path();
        path.moveTo(r.exactCenterX(), r.exactCenterY());
        try {
            GestureDescription.StrokeDescription stroke =
                    new GestureDescription.StrokeDescription(path, 0L, 60L);
            dispatchGesture(new GestureDescription.Builder().addStroke(stroke).build(), null, null);
        } catch (Exception ignored) {}
    }

    private AccessibilityNodeInfo rowOf(AccessibilityNodeInfo n) {
        AccessibilityNodeInfo p = (n == null) ? null : n.getParent();
        return p != null ? p : n;
    }

    /** Scroll the list down using the native accessibility scroll action. */
    private boolean scrollForward(AccessibilityNodeInfo root) {
        AccessibilityNodeInfo s = findScrollable(root);
        if (s == null) return false;
        try {
            return s.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
        } catch (Exception e) {
            return false;
        }
    }

    private AccessibilityNodeInfo findScrollable(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isScrollable()) return node;
        int n = node.getChildCount();
        for (int i = 0; i < n; i++) {
            AccessibilityNodeInfo r = findScrollable(node.getChild(i));
            if (r != null) return r;
        }
        return null;
    }

    private void scrollDown() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int x = dm.widthPixels / 2;
        int y1 = (int) (dm.heightPixels * 0.82f);
        int y2 = (int) (dm.heightPixels * 0.18f);
        Path p = new Path();
        p.moveTo(x, y1);
        p.lineTo(x, y2);
        try {
            GestureDescription.StrokeDescription stroke =
                    new GestureDescription.StrokeDescription(p, 0L, 320L);
            dispatchGesture(new GestureDescription.Builder().addStroke(stroke).build(), null, null);
        } catch (Exception ignored) {}
    }

    /** A real coordinate tap at (x, y) — used when the target has no tappable node. */
    private void tapXY(float x, float y) {
        Path p = new Path();
        p.moveTo(x, y);
        try {
            GestureDescription.StrokeDescription stroke =
                    new GestureDescription.StrokeDescription(p, 0L, 60L);
            dispatchGesture(new GestureDescription.Builder().addStroke(stroke).build(), null, null);
        } catch (Exception ignored) {}
    }

    /**
     * The action button (the final "مسح"): a clickable, non-checkable node with
     * no checkbox inside it — so it is never confused with a list row — that
     * either reads as clear/delete text, is a Button, or is a wide bottom bar.
     * Picks the lowest such node on screen.
     */
    private AccessibilityNodeInfo findActionButton(AccessibilityNodeInfo root, int w, int h) {
        AccessibilityNodeInfo[] best = new AccessibilityNodeInfo[1];
        int[] bestY = { Integer.MIN_VALUE };
        scanActionButton(root, w, h, best, bestY);
        return best[0];
    }

    private void scanActionButton(AccessibilityNodeInfo node, int w, int h,
                                  AccessibilityNodeInfo[] best, int[] bestY) {
        if (node == null) return;
        if (node.isClickable() && node.isEnabled()
                && !node.isCheckable() && !hasCheckableDescendant(node)) {
            Rect r = new Rect();
            node.getBoundsInScreen(r);
            CharSequence tx = node.getText();
            CharSequence d = node.getContentDescription();
            String hay = ((tx == null ? "" : tx) + " " + (d == null ? "" : d))
                    .toLowerCase(Locale.ROOT).trim();
            boolean textMatch = !hay.isEmpty() && matches(hay, FINAL_CLEAR, NOT_CLEAR_EXCLUDE);
            CharSequence cn = node.getClassName();
            boolean isButton = cn != null && cn.toString().toLowerCase(Locale.ROOT).contains("button");
            boolean bigBar = r.width() > w * 0.5f && r.height() > h * 0.03f && r.height() < h * 0.18f;
            if ((textMatch || isButton || bigBar) && r.height() > 0 && r.centerY() > bestY[0]) {
                bestY[0] = r.centerY();
                best[0] = node;
            }
        }
        int n = node.getChildCount();
        for (int i = 0; i < n; i++) scanActionButton(node.getChild(i), w, h, best, bestY);
    }

    private boolean hasCheckableDescendant(AccessibilityNodeInfo node) {
        if (node == null) return false;
        int n = node.getChildCount();
        for (int i = 0; i < n; i++) {
            AccessibilityNodeInfo ch = node.getChild(i);
            if (ch == null) continue;
            if (ch.isCheckable()) return true;
            if (hasCheckableDescendant(ch)) return true;
        }
        return false;
    }

    // ---- node search ----

    private AccessibilityNodeInfo find(AccessibilityNodeInfo node, String[] labels, String[] excludes) {
        AccessibilityNodeInfo t = findTextNode(node, labels, excludes);
        return t == null ? null : clickableSelfOrAncestor(t);
    }

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

    private boolean click(AccessibilityNodeInfo n) {
        return n != null && n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }
}
