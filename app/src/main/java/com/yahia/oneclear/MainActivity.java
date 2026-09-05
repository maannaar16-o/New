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
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * One screen, one button. Tapping "امسح الآن" wipes the call log and every SMS,
 * then sends the user to the Accounts screen to remove accounts by hand
 * (accounts cannot be removed programmatically — see README_AR.md).
 */
public class MainActivity extends Activity {

    public static final String EXTRA_AUTOSTART = "autostart";

    private static final int REQ_PERMS = 101;
    private static final int REQ_DEFAULT_SMS = 102;

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
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        root.addView(title("مسح بضغطة واحدة"));
        root.addView(body("بضغطة واحدة يمسح هذا التطبيق:\n"
                + "• كل سجل المكالمات\n"
                + "• كل الرسائل النصية (SMS)\n"
                + "ثم يفتح لك شاشة الحسابات لإزالتها بنفسك."));
        root.addView(warn("⚠️ المسح نهائي ولا يمكن التراجع عنه. لا يمكن استرجاع المكالمات أو الرسائل بعد حذفها."));

        statusView = body("");
        root.addView(statusView);

        Button wipeBtn = button("امسح الآن");
        wipeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        wipeBtn.setTextColor(Color.WHITE);
        wipeBtn.setBackgroundColor(0xFFC62828);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(64));
        clp.topMargin = dp(14);
        clp.bottomMargin = dp(10);
        wipeBtn.setLayoutParams(clp);
        wipeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { startWipeWithConfirm(); }
        });
        root.addView(wipeBtn);

        final CheckBox confirmBox = new CheckBox(this);
        confirmBox.setText("اطلب تأكيد قبل المسح (للأمان)");
        confirmBox.setChecked(Prefs.isConfirmEnabled(this));
        confirmBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton bv, boolean checked) {
                Prefs.setConfirmEnabled(MainActivity.this, checked);
            }
        });
        root.addView(confirmBox);

        Button accountsBtn = button("فتح شاشة الحسابات (لإزالتها يدويًا)");
        accountsBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { WipeFlow.openAccountsSettings(MainActivity.this); }
        });
        root.addView(space(dp(6)));
        root.addView(accountsBtn);

        Button restoreSmsBtn = button("استعادة تطبيق الرسائل العادي");
        restoreSmsBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { WipeFlow.openDefaultSmsSettings(MainActivity.this); }
        });
        root.addView(restoreSmsBtn);

        root.addView(space(dp(12)));
        root.addView(body("للضغطة الواحدة من الشاشة الرئيسية: اضغط مطوّلًا على الشاشة ← الودجتس (Widgets) "
                + "← اختر «مسح بضغطة واحدة» وضعه على الشاشة."));

        setContentView(scroll);

        if (getIntent() != null && getIntent().getBooleanExtra(EXTRA_AUTOSTART, false)) {
            startWipeWithConfirm();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        StringBuilder s = new StringBuilder();
        s.append(WipeFlow.isDefaultSms(this)
                ? "• تطبيق الرسائل الافتراضي: هذا التطبيق (مؤقتًا للمسح)\n"
                : "• تطبيق الرسائل الافتراضي: تطبيقك العادي\n");
        List<String> accts = WipeFlow.listAccounts(this);
        if (accts.isEmpty()) {
            s.append("• الحسابات على الجهاز: (اضغط الزر لعرضها/إزالتها)");
        } else {
            s.append("• الحسابات على الجهاز (").append(accts.size()).append("): ");
            s.append(android.text.TextUtils.join("، ", accts));
        }
        statusView.setText(s.toString());
    }

    // ---- wipe orchestration ----

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
        // Ask to become the default SMS app (required to delete SMS).
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
            // Could not open the picker; wipe what we can.
            performWipe();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] perms, int[] results) {
        super.onRequestPermissionsResult(requestCode, perms, results);
        if (requestCode == REQ_PERMS) {
            // Proceed regardless: whatever was granted, wipe what we can.
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
        boolean callsOk = true;
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

    // ---- small UI helpers ----
    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics());
    }

    private TextView title(String t) {
        TextView tv = new TextView(this);
        tv.setText(t);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(0, 0, 0, dp(8));
        return tv;
    }

    private TextView body(String t) {
        TextView tv = new TextView(this);
        tv.setText(t);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tv.setPadding(0, dp(4), 0, dp(4));
        return tv;
    }

    private TextView warn(String t) {
        TextView tv = body(t);
        tv.setTextColor(0xFFC62828);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        return tv;
    }

    private Button button(String t) {
        Button btn = new Button(this);
        btn.setText(t);
        btn.setAllCaps(false);
        return btn;
    }

    private View space(int h) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h));
        return v;
    }
}
