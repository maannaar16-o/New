package com.yahia.oneclear;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Setup + manual-wipe screen. One-time setup here (permissions + default SMS
 * app) enables the truly-silent one-tap widget. Wipes the call log and SMS,
 * then points the user at the Accounts screen for manual account removal.
 */
public class MainActivity extends Activity {

    private static final int REQ_PERMS = 101;
    private static final int REQ_DEFAULT_SMS = 102;

    private static final int RED = 0xFFD32F2F;
    private static final int TEXT = 0xFF1A1A1A;
    private static final int SUBTLE = 0xFF5A5A66;

    private TextView statusView;
    private boolean wipeInProgress = false;

    private static final String[] NEEDED_PERMS = {
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.READ_SMS,
            Manifest.permission.GET_ACCOUNTS
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFFF4F5F7);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        // ---- Header ----
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackground(getDrawable(R.drawable.header_bg));
        int hp = dp(18);
        header.setPadding(hp, hp, hp, hp);
        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tcp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleCol.setLayoutParams(tcp);
        TextView h1 = new TextView(this);
        h1.setText("مسح الرسائل والمكالمات");
        h1.setTextColor(Color.WHITE);
        h1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
        h1.setTypeface(Typeface.DEFAULT_BOLD);
        TextView h2 = new TextView(this);
        h2.setText("امسح مكالماتك ورسائلك بضغطة واحدة");
        h2.setTextColor(0xFFFFE0E0);
        h2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        h2.setPadding(0, dp(4), 0, 0);
        titleCol.addView(h1);
        titleCol.addView(h2);
        ImageView broom = new ImageView(this);
        broom.setImageDrawable(getDrawable(R.drawable.ic_broom));
        broom.setColorFilter(Color.WHITE);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(46), dp(46));
        broom.setLayoutParams(ip);
        header.addView(titleCol);
        header.addView(broom);
        root.addView(header);
        root.addView(gap(dp(16)));

        // ---- Hero wipe button ----
        Button wipeBtn = new Button(this);
        wipeBtn.setText("امسح الآن");
        wipeBtn.setAllCaps(false);
        wipeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        wipeBtn.setTypeface(Typeface.DEFAULT_BOLD);
        wipeBtn.setTextColor(Color.WHITE);
        wipeBtn.setBackground(getDrawable(R.drawable.btn_primary));
        LinearLayout.LayoutParams hbp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(62));
        wipeBtn.setLayoutParams(hbp);
        wipeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { startWipeWithConfirm(); }
        });
        root.addView(wipeBtn);
        root.addView(gap(dp(10)));

        // ---- Warning ----
        TextView warn = new TextView(this);
        warn.setText("⚠️ المسح نهائي ولا يمكن التراجع عنه.");
        warn.setTextColor(RED);
        warn.setTypeface(Typeface.DEFAULT_BOLD);
        warn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        warn.setBackground(getDrawable(R.drawable.warn_bg));
        int wp = dp(12);
        warn.setPadding(wp, dp(10), wp, dp(10));
        root.addView(warn);
        root.addView(gap(dp(16)));

        // ---- What it does ----
        LinearLayout doCard = card();
        doCard.addView(cardTitle("بيعمل إيه؟"));
        doCard.addView(bullet("📞  يمسح كل سجل المكالمات"));
        doCard.addView(bullet("💬  يمسح كل الرسائل النصية (SMS)"));
        doCard.addView(bullet("👤  يفتح شاشة الحسابات لإزالتها بنفسك"));
        root.addView(doCard);
        root.addView(gap(dp(12)));

        // ---- Status ----
        LinearLayout statusCard = card();
        statusCard.addView(cardTitle("الحالة"));
        statusView = new TextView(this);
        statusView.setTextColor(SUBTLE);
        statusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        statusView.setLineSpacing(dp(4), 1f);
        statusCard.addView(statusView);
        root.addView(statusCard);
        root.addView(gap(dp(12)));

        // ---- Options ----
        LinearLayout optCard = card();
        optCard.addView(cardTitle("الإعدادات"));
        final CheckBox confirmBox = new CheckBox(this);
        confirmBox.setText("اطلب تأكيد قبل المسح (للأمان)");
        confirmBox.setTextColor(TEXT);
        confirmBox.setChecked(Prefs.isConfirmEnabled(this));
        confirmBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton bv, boolean checked) {
                Prefs.setConfirmEnabled(MainActivity.this, checked);
            }
        });
        optCard.addView(confirmBox);
        final CheckBox openAccountsBox = new CheckBox(this);
        openAccountsBox.setText("افتح شاشة الحسابات بعد المسح");
        openAccountsBox.setTextColor(TEXT);
        openAccountsBox.setChecked(Prefs.isOpenAccountsEnabled(this));
        openAccountsBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton bv, boolean checked) {
                Prefs.setOpenAccountsEnabled(MainActivity.this, checked);
            }
        });
        optCard.addView(openAccountsBox);
        root.addView(optCard);
        root.addView(gap(dp(12)));

        // ---- Secondary actions ----
        Button accountsBtn = outlineButton("فتح شاشة الحسابات");
        accountsBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { WipeFlow.openAccountsSettings(MainActivity.this); }
        });
        root.addView(accountsBtn);
        root.addView(gap(dp(8)));
        Button restoreSmsBtn = outlineButton("استعادة تطبيق الرسائل العادي");
        restoreSmsBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { WipeFlow.openDefaultSmsSettings(MainActivity.this); }
        });
        root.addView(restoreSmsBtn);
        root.addView(gap(dp(16)));

        // ---- Facebook Lite auto-clear ----
        LinearLayout fbCard = card();
        fbCard.addView(cardTitle("مسح بيانات فيسبوك وإنستجرام (تلقائي)"));
        fbCard.addView(bodyText(
                "يفتح إعدادات التطبيق ويضغط بدلًا عنك: محو الذاكرة المؤقتة ← إدارة المساحة ← "
                + "تحديد كل الخيارات (بما فيها الحسابات والإعدادات) ← تأكيد ← مسح.\n"
                + "يتطلب تفعيل «خدمة الوصول» مرة واحدة. سيب الجهاز أثناء العمل."));
        Button enableAccBtn = outlineButton("① تفعيل خدمة الوصول");
        enableAccBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { openAccessibilitySettings(); }
        });
        fbCard.addView(enableAccBtn);
        fbCard.addView(gap(dp(8)));
        Button startLiteBtn = outlineButton("② ابدأ مسح فيسبوك لايت");
        startLiteBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { startAutoClear(FbClearService.FB_PKG, "فيسبوك لايت"); }
        });
        fbCard.addView(startLiteBtn);
        fbCard.addView(gap(dp(8)));
        Button startAppBtn = outlineButton("③ ابدأ مسح فيسبوك (العادي)");
        startAppBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { startAutoClear(FbClearService.FB_APP, "فيسبوك"); }
        });
        fbCard.addView(startAppBtn);
        fbCard.addView(gap(dp(8)));
        Button startIgLiteBtn = outlineButton("④ ابدأ مسح إنستجرام لايت");
        startIgLiteBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { startAutoClear(FbClearService.IG_LITE, "إنستجرام لايت"); }
        });
        fbCard.addView(startIgLiteBtn);
        fbCard.addView(gap(dp(8)));
        Button startMessengerBtn = outlineButton("⑤ ابدأ مسح ماسنجر");
        startMessengerBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { startAutoClear(FbClearService.MESSENGER, "ماسنجر"); }
        });
        fbCard.addView(startMessengerBtn);
        root.addView(fbCard);
        root.addView(gap(dp(16)));

        // ---- Help ----
        LinearLayout helpCard = card();
        helpCard.addView(cardTitle("الضغطة الواحدة من الشاشة الرئيسية"));
        helpCard.addView(bodyText(
                "1) اعمل الإعداد مرة واحدة هنا: اضغط «امسح الآن» واقبل الصلاحيات + اجعله «تطبيق الرسائل الافتراضي».\n"
                + "2) اضغط مطوّلًا على الشاشة الرئيسية ← الودجتس (Widgets) ← اختر «مسح الرسائل والمكالمات» وضعه على الشاشة.\n"
                + "بعدها أي ضغطة على الودجت تمسح في الخلفية فورًا، دون فتح التطبيق.\n"
                + "لمسح فوري بلا أي نافذة: أزل علامة «اطلب تأكيد قبل المسح».\n"
                + "لا تنسَ استعادة تطبيق الرسائل العادي بعد الانتهاء."));
        root.addView(helpCard);

        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        StringBuilder s = new StringBuilder();
        s.append(WipeFlow.isDefaultSms(this)
                ? "✔ تطبيق الرسائل الافتراضي: هذا التطبيق (مؤقتًا للمسح)\n"
                : "• تطبيق الرسائل الافتراضي: تطبيقك العادي\n");
        List<String> accts = WipeFlow.listAccounts(this);
        if (accts.isEmpty()) {
            s.append("• الحسابات على الجهاز: (اضغط «فتح شاشة الحسابات» لعرضها)");
        } else {
            s.append("• الحسابات على الجهاز (").append(accts.size()).append("): ");
            s.append(android.text.TextUtils.join("، ", accts));
        }
        statusView.setText(s.toString());
    }

    // ---- wipe orchestration (unchanged logic) ----

    private void startWipeWithConfirm() {
        if (wipeInProgress) return;
        if (Prefs.isConfirmEnabled(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("تأكيد المسح النهائي")
                    .setMessage("سيتم حذف كل سجل المكالمات وكل الرسائل النصية نهائيًا، بدون إمكانية استرجاع.\n\nمتأكد؟")
                    .setPositiveButton("نعم، امسح", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface d, int w) { beginWipe(); }
                    })
                    .setNegativeButton("إلغاء", null)
                    .show();
        } else {
            beginWipe();
        }
    }

    private void beginWipe() {
        wipeInProgress = true;
        if (!hasAllPerms()) {
            requestPermissions(missingPerms(), REQ_PERMS);
            return;
        }
        ensureDefaultSmsThenWipe();
    }

    private void ensureDefaultSmsThenWipe() {
        if (WipeFlow.isDefaultSms(this)) {
            performWipe();
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RoleManager rm = (RoleManager) getSystemService(ROLE_SERVICE);
                Intent i = rm.createRequestRoleIntent(RoleManager.ROLE_SMS);
                startActivityForResult(i, REQ_DEFAULT_SMS);
            } else {
                Intent i = new Intent(android.provider.Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                i.putExtra(android.provider.Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
                startActivityForResult(i, REQ_DEFAULT_SMS);
            }
        } catch (Exception e) {
            performWipe();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] perms, int[] results) {
        super.onRequestPermissionsResult(requestCode, perms, results);
        if (requestCode == REQ_PERMS) {
            ensureDefaultSmsThenWipe();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_DEFAULT_SMS) {
            performWipe();
        }
    }

    private void performWipe() {
        int calls = 0;
        boolean callsOk;
        if (hasPerm(Manifest.permission.WRITE_CALL_LOG)) {
            calls = WipeFlow.wipeCallLog(this);
            callsOk = calls >= 0;
        } else {
            callsOk = false;
        }

        String smsLine;
        if (WipeFlow.isDefaultSms(this)) {
            int sms = WipeFlow.wipeSms(this);
            smsLine = (sms >= 0) ? ("الرسائل: تم حذف " + sms) : "الرسائل: تعذّر الحذف";
        } else {
            smsLine = "الرسائل: تم التخطّي (لم تُجعل التطبيق الافتراضي)";
        }

        String callsLine = callsOk ? ("المكالمات: تم حذف " + calls) : "المكالمات: تعذّر (صلاحية مرفوضة)";

        wipeInProgress = false;

        new AlertDialog.Builder(this)
                .setTitle("انتهى المسح")
                .setMessage(callsLine + "\n" + smsLine + "\n\n"
                        + "الحسابات وكلمات المرور لا يمكن حذفها تلقائيًا (يمنعها نظام أندرويد). "
                        + "اضغط «فتح الحسابات» لإزالتها بنفسك.\n\n"
                        + "ولا تنسَ استعادة تطبيق الرسائل العادي حتى تستقبل الرسائل مجددًا.")
                .setPositiveButton("فتح الحسابات", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        WipeFlow.openAccountsSettings(MainActivity.this);
                    }
                })
                .setNeutralButton("استعادة الرسائل", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        WipeFlow.openDefaultSmsSettings(MainActivity.this);
                    }
                })
                .setNegativeButton("تمام", null)
                .show();

        Toast.makeText(this, "تم", Toast.LENGTH_SHORT).show();
    }

    // ---- Facebook auto-clear (works for Facebook Lite and the regular app) ----

    private void startAutoClear(final String pkg, final String label) {
        if (!isInstalled(pkg)) {
            Toast.makeText(this, label + " غير مثبّت على الجهاز.", Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("مسح بيانات " + label)
                .setMessage("سيفتح التطبيق الإعدادات ويضغط تلقائيًا: محو الذاكرة المؤقتة ← إدارة المساحة ← "
                        + "تحديد كل الخيارات ← مسح. هذا يحذف بيانات " + label + " ويسجّل خروجك نهائيًا.\n\nمتابعة؟")
                .setPositiveButton("نعم، ابدأ", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        if (!isAccessibilityOn() || FbClearService.instance == null) {
                            Toast.makeText(MainActivity.this,
                                    "فعّل «خدمة الوصول» لهذا التطبيق أولًا ثم أعد المحاولة.",
                                    Toast.LENGTH_LONG).show();
                            openAccessibilitySettings();
                            return;
                        }
                        Toast.makeText(MainActivity.this, "جارٍ المسح… سيب الجهاز لحد ما يخلّص",
                                Toast.LENGTH_SHORT).show();
                        FbClearService.instance.startFor(pkg);
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private boolean isInstalled(String pkg) {
        try {
            getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isAccessibilityOn() {
        if (FbClearService.instance != null) return true;
        try {
            String flat = android.provider.Settings.Secure.getString(getContentResolver(),
                    android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (flat == null || flat.isEmpty()) return false;
            String comp = getPackageName() + "/" + FbClearService.class.getName();
            return flat.contains(comp);
        } catch (Exception e) {
            return false;
        }
    }

    private void openAccessibilitySettings() {
        try {
            startActivity(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (Exception ignored) {}
    }

    // ---- permission helpers ----

    private boolean hasPerm(String p) {
        return checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasAllPerms() {
        for (String p : NEEDED_PERMS) {
            if (!hasPerm(p)) return false;
        }
        return true;
    }

    private String[] missingPerms() {
        List<String> miss = new ArrayList<>();
        for (String p : NEEDED_PERMS) {
            if (!hasPerm(p)) miss.add(p);
        }
        return miss.toArray(new String[0]);
    }

    // ---- UI helpers ----

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics());
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(getDrawable(R.drawable.card_bg));
        int p = dp(16);
        c.setPadding(p, p, p, p);
        return c;
    }

    private TextView cardTitle(String t) {
        TextView tv = new TextView(this);
        tv.setText(t);
        tv.setTextColor(TEXT);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(0, 0, 0, dp(8));
        return tv;
    }

    private TextView bullet(String t) {
        TextView tv = new TextView(this);
        tv.setText(t);
        tv.setTextColor(0xFF333333);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tv.setPadding(0, dp(5), 0, dp(5));
        return tv;
    }

    private TextView bodyText(String t) {
        TextView tv = new TextView(this);
        tv.setText(t);
        tv.setTextColor(SUBTLE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setLineSpacing(dp(4), 1f);
        return tv;
    }

    private Button outlineButton(String t) {
        Button btn = new Button(this);
        btn.setText(t);
        btn.setAllCaps(false);
        btn.setTextColor(RED);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        btn.setBackground(getDrawable(R.drawable.btn_outline));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        btn.setLayoutParams(lp);
        return btn;
    }

    private View gap(int h) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h));
        return v;
    }
}
