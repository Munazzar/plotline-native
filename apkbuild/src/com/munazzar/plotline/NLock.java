package com.munazzar.plotline;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONObject;

/* 2.0.8: the app lock screen, natively (web showLock / unlock / lockBio / forgotPin). */
final class NLock {
    private NLock() { }

    static View view;

    /* the Plotline mark (web LOGO svg, 40×40) */
    static final class Logo extends View {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Logo(Context c) { super(c); }
        @Override protected void onDraw(Canvas cv) {
            float s = Math.min(getWidth(), getHeight()) / 40f;
            cv.save(); cv.scale(s, s);
            p.setStyle(Paint.Style.FILL); p.setColor(NTheme.surface2); cv.drawRoundRect(new RectF(0, 0, 40, 40), 10, 10, p);
            cv.translate(2.2f, 2f); cv.scale(.9f, .9f);
            int ac = NTheme.accent;
            p.setShader(new RadialGradient(31, 15.5f, 7.5f, NTheme.alpha(ac, .6f), NTheme.alpha(ac, 0f), Shader.TileMode.CLAMP)); cv.drawCircle(31, 15.5f, 7.5f, p); p.setShader(null);
            p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND);
            p.setColor(NTheme.alpha(ac, .55f)); p.setStrokeWidth(.9f); p.setPathEffect(new android.graphics.DashPathEffect(new float[]{1.6f, 1.6f}, 0)); cv.drawLine(31, 3.5f, 31, 34.5f, p); p.setPathEffect(null);
            lane(cv, 0xFF46C99B, 13, 8.5f, 18, 26, new float[]{13}, 31);
            lane(cv, 0xFF6B9BFF, 6, 15.5f, 6, 12.5f, new float[]{18, 23}, -1);
            lane(cv, 0xFFB48CFF, 10, 22.5f, 15, 28.5f, new float[]{10}, 31);
            lane(cv, 0xFFFF8C6B, 4.5f, 29.5f, 11, 19.5f, new float[]{4.5f, 25}, 31);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(.85f); p.setColor(NTheme.alpha(ac, .55f)); cv.drawCircle(31, 15.5f, 4.7f, p);
            p.setStyle(Paint.Style.FILL); p.setColor(ac); cv.drawCircle(31, 15.5f, 2.9f, p);
            cv.restore();
        }
        void lane(Canvas cv, int col, float x0, float y, float a, float b, float[] dots, float end) {
            p.setStyle(Paint.Style.STROKE); p.setColor(NTheme.alpha(col, .38f)); p.setStrokeWidth(1.2f); cv.drawLine(x0, y, 31, y, p);
            p.setColor(col); p.setStrokeWidth(2.4f); cv.drawLine(a, y, b, y, p);
            p.setStyle(Paint.Style.FILL);
            for (float d : dots) cv.drawCircle(d, y, 1.4f, p);
            if (end > 0) cv.drawCircle(end, y, 1.75f, p);
        }
    }

    static boolean bioOn(NShell sh) { JSONObject l = sh.st.settings().optJSONObject("lock"); return l != null && l.optBoolean("on"); }
    static boolean pinOn(NShell sh) { return !sh.st.settings().optString("pinHash", "").isEmpty(); }

    static void show(final NShell sh) {
        if (view != null) return;
        final Context c = sh.a;
        final boolean bio = bioOn(sh), pin = pinOn(sh);
        FrameLayout ov = new FrameLayout(c); ov.setBackgroundColor(NTheme.bg); ov.setClickable(true);
        ScrollView sv = new ScrollView(c); sv.setFillViewport(true);
        FrameLayout cen = new FrameLayout(c);
        LinearLayout box = NUi.col(c); box.setGravity(Gravity.CENTER_HORIZONTAL);
        int w = Math.min(NUi.dp(340), c.getResources().getDisplayMetrics().widthPixels - NUi.dp(48));
        box.addView(new Logo(c), NUi.lp(NUi.dp(64), NUi.dp(64)));
        TextView h = NUi.text(c, "WELCOME BACK", 44, NTheme.text); h.setTypeface(NFont.display(800)); h.setGravity(Gravity.CENTER); NUi.cssLh(h, .9f);
        LinearLayout.LayoutParams hl = NUi.lp(-1, -2); hl.topMargin = NUi.dp(22); hl.bottomMargin = NUi.dp(8); box.addView(h, hl);
        if (bio) {
            TextView s = NUi.text(c, "Unlock with your fingerprint, face or screen lock", 13.5f, NTheme.muted); s.setGravity(Gravity.CENTER); box.addView(s, NUi.lp(-1, -2));
            TextView b = NUi.btn(c, "🔒  Unlock", true, new View.OnClickListener() { public void onClick(View v) { bioPrompt(sh); } });
            LinearLayout.LayoutParams bl = NUi.lp(-1, NUi.dp(50)); bl.topMargin = NUi.dp(6); box.addView(b, bl);
        }
        if (pin) {
            TextView s = NUi.text(c, bio ? "or enter your PIN" : "Enter your PIN to open Plotline", bio ? 12.5f : 13.5f, NTheme.muted); s.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams sl = NUi.lp(-1, -2); sl.topMargin = NUi.dp(bio ? 16 : 0); box.addView(s, sl);
            final EditText in = NForms.input(c, "", "", false);
            in.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            in.setGravity(Gravity.CENTER); in.setTextSize(32); in.setTypeface(NFont.display(800)); in.setLetterSpacing(.45f);
            LinearLayout.LayoutParams il = NUi.lp(-1, -2); il.topMargin = NUi.dp(20); il.bottomMargin = NUi.dp(8); box.addView(in, il);
            final TextView err = NUi.text(c, "", 13, NTheme.LATE); err.setGravity(Gravity.CENTER); box.addView(err, NUi.lp(-1, -2));
            final Runnable check = new Runnable() { public void run() {
                String v = in.getText().toString();
                if (NSheets.hash(v).equals(sh.st.settings().optString("pinHash"))) { sh.a.unlocked(); }
                else { err.setText("That PIN isn’t right"); in.setText(""); in.requestFocus(); }
            } };
            in.setImeOptions(EditorInfo.IME_ACTION_DONE);
            in.setOnEditorActionListener(new TextView.OnEditorActionListener() { public boolean onEditorAction(TextView v, int a, android.view.KeyEvent e) { check.run(); return true; } });
            TextView ub = NUi.btn(c, "Unlock with PIN", !bio, new View.OnClickListener() { public void onClick(View v) { check.run(); } });
            LinearLayout.LayoutParams ul = NUi.lp(-1, NUi.dp(50)); ul.topMargin = NUi.dp(10); box.addView(ub, ul);
            TextView fg = NUi.link(c, "Forgot PIN?", new View.OnClickListener() { public void onClick(View v) { forgot(sh); } });
            LinearLayout.LayoutParams fl = NUi.lp(-2, -2); fl.topMargin = NUi.dp(16); fl.gravity = Gravity.CENTER_HORIZONTAL; box.addView(fg, fl);
            if (!bio) in.postDelayed(new Runnable() { public void run() { NForms.focus(in); } }, 100);
        }
        cen.addView(box, new FrameLayout.LayoutParams(w, -2, Gravity.CENTER));
        cen.setPadding(NUi.dp(24), sh.top + NUi.dp(24), NUi.dp(24), sh.bot + NUi.dp(24));
        sv.addView(cen, new FrameLayout.LayoutParams(-1, -1));
        ov.addView(sv, new FrameLayout.LayoutParams(-1, -1));
        sh.root.addView(ov, new FrameLayout.LayoutParams(-1, -1));
        if (sh.sheetLayer != null) sh.sheetLayer.bringToFront();
        view = ov;
        if (bio) ov.postDelayed(new Runnable() { public void run() { if (view != null) bioPrompt(sh); } }, 250);
        /* a PIN reset or lock change elsewhere (Google reset, sync) ends the lock */
        ov.postDelayed(new Runnable() { public void run() { if (view == null) return; if (!pinOn(sh) && !bioOn(sh)) { sh.a.unlocked(); return; } view.postDelayed(this, 1000); } }, 1000);
    }

    static void hide(NShell sh) {
        if (view == null) return;
        final View v = view; view = null;
        sh.hideKeyboardNow();
        v.animate().alpha(0f).setDuration(450).setInterpolator(NUi.EASE).withEndAction(new Runnable() { public void run() { if (v.getParent() != null) ((FrameLayout) v.getParent()).removeView(v); } }).start();
    }

    static void bioPrompt(final NShell sh) {
        final MainActivity a = sh.a;
        try {
            android.hardware.biometrics.BiometricPrompt.Builder b = new android.hardware.biometrics.BiometricPrompt.Builder(a).setTitle("Unlock Plotline").setSubtitle("");
            if (Build.VERSION.SDK_INT >= 30) b.setAllowedAuthenticators(android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK | android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL);
            else b.setDeviceCredentialAllowed(true);
            b.build().authenticate(new android.os.CancellationSignal(), a.getMainExecutor(), new android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                @Override public void onAuthenticationSucceeded(android.hardware.biometrics.BiometricPrompt.AuthenticationResult r) { a.unlocked(); }
                @Override public void onAuthenticationError(int code, CharSequence msg) { String m = String.valueOf(msg); if (!m.toLowerCase().contains("cancel")) NShell.toast(m); }
            });
        } catch (Exception e) { NShell.toast(String.valueOf(e.getMessage())); }
    }

    /* web forgotPin */
    static void forgot(final NShell sh) {
        JSONObject sy = sh.st.settings().optJSONObject("sync");
        if (sy != null && sy.optBoolean("on") && !sy.optString("email").isEmpty()) {
            final Context c = sh.a; LinearLayout b = NUi.col(c);
            b.addView(NUi.title(c, "Forgot your PIN?", 28));
            TextView s = NUi.text(c, "Sign in to " + sy.optString("email") + " to prove it’s you. Your PIN will be removed and nothing else changes.", 14.5f, NTheme.muted); s.setLineSpacing(0, 1.2f); s.setPadding(0, NUi.dp(8), 0, 0); b.addView(s);
            b.addView(NForms.actions(c, NUi.btn(c, "Cancel", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } }),
                NUi.btn(c, "Reset with Google", true, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); sh.a.js("resetPinWithGoogle()"); } })));
            sh.sheet(b); return;
        }
        NSheets.confirm(sh, "Forgot your PIN?", "Sync isn’t on, so there’s no account to verify you with. Erase this device’s data, then restore a backup file.", "Erase and start over", true, new Runnable() { public void run() {
            sh.a.jsRet("window.__nwipe()", new android.webkit.ValueCallback<String>() { public void onReceiveValue(String v) {
                sh.root.postDelayed(new Runnable() { public void run() { sh.a.unlocked(); sh.popAll(); sh.tabTo(0); NWelcome.run(sh); } }, 600);
            } });
        } });
    }
}
