package com.munazzar.plotline;

import org.json.JSONObject;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.view.Window;
import android.view.WindowInsetsController;
import android.webkit.JavascriptInterface;
import android.widget.Toast;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class Bridge {
    final MainActivity a;
    Bridge(MainActivity a) { this.a = a; }

    @JavascriptInterface
    public void schedule(String json) { Reminders.schedule(a.getApplicationContext(), json); }

    /* which downloaded model reads notification replies when the app is closed ("" = quick reader only) */
    @JavascriptInterface
    public void setReplyModel(String file, String label, int ctx, int threads) {
        Notify.prefs(a.getApplicationContext()).edit().putString("aiModel", file == null ? "" : file).putString("aiLabel", label == null ? "" : label).putInt("aiCtx", ctx).putInt("aiThreads", threads).apply();
    }

    /* this item's notification history, newest first (key: h:<habit> · s:<goal>/<step> · d:<day goal> · g:<goal> incl. its steps) */
    @JavascriptInterface
    public String notifLog(String key) { return Notify.history(a.getApplicationContext(), key == null ? "" : key); }

    @JavascriptInterface
    public void notifCancelSnooze(int nid) { Notify.cancelSnooze(a.getApplicationContext(), nid); }

    /* Android 12+ asks the user to allow on-time alarms ("Alarms & reminders"); without it reminders can drift by minutes */
    @JavascriptInterface
    public String importText() { return a.importTxt; }

    @JavascriptInterface
    public boolean exactAlarms() { return Build.VERSION.SDK_INT < 31 || Reminders.canExact(a.getSystemService(android.app.AlarmManager.class)); }

    @JavascriptInterface
    public void openExactAlarms() {
        a.runOnUiThread(new Runnable() { public void run() {
            try { a.startActivity(new Intent("android.settings.REQUEST_SCHEDULE_EXACT_ALARM", Uri.parse("package:" + a.getPackageName()))); }
            catch (Exception e) { try { a.startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + a.getPackageName()))); } catch (Exception ignored) { } }
        } });
    }

    /* ---- app lock: fingerprint / face, or the phone's own screen lock as the fallback ---- */
    static final int BIO_STRONG = 15, BIO_WEAK = 255, DEVICE_CRED = 32768;

    @JavascriptInterface
    public String bioState() {
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                android.hardware.biometrics.BiometricManager bm = a.getSystemService(android.hardware.biometrics.BiometricManager.class);
                int bio = bm.canAuthenticate(BIO_WEAK), any = bm.canAuthenticate(BIO_WEAK | DEVICE_CRED);
                return "{\"bio\":" + (bio == 0) + ",\"any\":" + (any == 0) + ",\"enrolled\":" + (bio != 11) + "}";
            }
            android.app.KeyguardManager km = a.getSystemService(android.app.KeyguardManager.class);
            return "{\"bio\":false,\"any\":" + (km != null && km.isDeviceSecure()) + ",\"enrolled\":false}";
        } catch (Exception e) { return "{\"bio\":false,\"any\":false}"; }
    }

    @JavascriptInterface
    public void bioAuth(final String title, final String sub) {
        a.runOnUiThread(new Runnable() { public void run() {
            try {
                android.hardware.biometrics.BiometricPrompt.Builder b = new android.hardware.biometrics.BiometricPrompt.Builder(a).setTitle(title).setSubtitle(sub);
                if (Build.VERSION.SDK_INT >= 30) b.setAllowedAuthenticators(BIO_WEAK | DEVICE_CRED);
                else b.setDeviceCredentialAllowed(true);
                b.build().authenticate(new android.os.CancellationSignal(), a.getMainExecutor(), new android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                    @Override public void onAuthenticationSucceeded(android.hardware.biometrics.BiometricPrompt.AuthenticationResult r) { a.js("window.__bio&&window.__bio('ok','')"); }
                    @Override public void onAuthenticationError(int code, CharSequence msg) { a.js("window.__bio&&window.__bio('err'," + org.json.JSONObject.quote(String.valueOf(msg)) + ")"); }
                });
            } catch (Exception e) { a.js("window.__bio&&window.__bio('err'," + org.json.JSONObject.quote(String.valueOf(e.getMessage())) + ")"); }
        } });
    }

    /* hide Plotline's screen in the recent-apps view and block screenshots */
    @JavascriptInterface
    public void setSecure(final boolean on) {
        a.runOnUiThread(new Runnable() { public void run() {
            if (on) a.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
            else a.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
        } });
    }

    /* ---- permissions and voice ---- */
    @JavascriptInterface
    public void askPerm(final String key, final String csv) { a.runOnUiThread(new Runnable() { public void run() { a.askPerms(key, csv.split(",")); } }); }

    @JavascriptInterface
    public boolean hasPerm(String p) { return a.checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED; }

    @JavascriptInterface
    public String voiceState() { return Voice.state(a); }

    @JavascriptInterface
    public void voiceStart(String id, String lang, boolean allowCloud) { Voice.start(a, id, lang, allowCloud); }

    @JavascriptInterface
    public void voiceStop() { Voice.stop(a); }

    /* ---- automations ---- */
    @JavascriptInterface
    public void autoSet(String json) { Auto.setRules(a.getApplicationContext(), json); }

    @JavascriptInterface
    public String autoStatus() { Auto.sampleSteps(a.getApplicationContext()); return Auto.status(a.getApplicationContext()); }

    @JavascriptInterface
    public void autoCheck() { new Thread(new Runnable() { public void run() { Auto.check(a.getApplicationContext(), null); } }).start(); }

    @JavascriptInterface
    public String autoApps() { return Auto.apps(a); }

    @JavascriptInterface
    public long screenNow(String csv) { return Auto.screenNow(a, csv == null ? "" : csv); }

    @JavascriptInterface
    public void placeHere() { Auto.hereAsync(a); }

    @JavascriptInterface
    public void liveShare(boolean on) { LiveShare.set(a.getApplicationContext(), on); }

    @JavascriptInterface
    public boolean liveShareOn() { return LiveShare.on(a.getApplicationContext()); }

    @JavascriptInterface
    public void activityLoad(int days) { Auto.activityAsync(a, days); }

    @JavascriptInterface
    public void openUsageAccess() { a.runOnUiThread(new Runnable() { public void run() { try { a.startActivity(new Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)); } catch (Exception ignored) { } } }); }

    @JavascriptInterface
    public void openAppSettings() { a.runOnUiThread(new Runnable() { public void run() { try { a.startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + a.getPackageName()))); } catch (Exception ignored) { } } }); }

    @JavascriptInterface
    public void openHealthConnect() {
        a.runOnUiThread(new Runnable() { public void run() {
            try { a.startActivity(new Intent("android.health.connect.action.MANAGE_HEALTH_PERMISSIONS").putExtra("android.intent.extra.PACKAGE_NAME", a.getPackageName())); }
            catch (Exception e) { try { a.startActivity(new Intent("android.health.connect.action.HEALTH_HOME_SETTINGS")); } catch (Exception ignored) { } }
        } });
    }

    /* the Google scopes Plotline asks for (sync, plus your email address for sharing) */
    @JavascriptInterface
    public void setScopes(String s) { if (s != null && s.matches("[a-zA-Z0-9:/._ -]{10,300}")) a.prefs().edit().putString("scopes", s).apply(); }

    /* sharing: lets the hourly invite check run with the app closed (see ShareCheck) */
    @JavascriptInterface
    public void shareCfg(String json) { if (json != null && json.length() < 20000) ShareCheck.config(a.getApplicationContext(), json); }

    @JavascriptInterface
    public String notifState() { return Notify.state(a.getApplicationContext()); }

    /* minutes > 0 mutes reminders for that long; 0 turns them back on */
    @JavascriptInterface
    public String notifMute(int minutes) { return minutes > 0 ? Notify.mute(a.getApplicationContext(), minutes) : Notify.muteTo(a.getApplicationContext(), 0); }

    @JavascriptInterface
    public boolean notificationsGranted() {
        return Build.VERSION.SDK_INT < 33 || a.checkSelfPermission("android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED;
    }

    @JavascriptInterface
    public void requestNotifications() { a.runOnUiThread(new Runnable() { public void run() { a.askNotifications(); } }); }

    @JavascriptInterface
    public void copy(final String text) {
        a.runOnUiThread(new Runnable() { public void run() {
            ClipboardManager cm = (ClipboardManager) a.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("Plotline", text));
        } });
    }

    @JavascriptInterface
    public boolean saveFile(String name, final String mime, String content, final boolean open) {
        try {
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            cv.put(MediaStore.MediaColumns.MIME_TYPE, mime);
            cv.put("relative_path", "Download/Plotline");
            Uri uri = a.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
            if (uri == null) return false;
            try (OutputStream os = a.getContentResolver().openOutputStream(uri)) {
                os.write(content.getBytes(StandardCharsets.UTF_8));
            }
            final Uri fu = uri;
            a.runOnUiThread(new Runnable() { public void run() {
                if (open) {
                    Intent v = new Intent(Intent.ACTION_VIEW);
                    v.setDataAndType(fu, mime);
                    v.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try { a.startActivity(v); return; } catch (Exception ignored) { }
                }
                Toast.makeText(a, "Saved to Downloads/Plotline", Toast.LENGTH_SHORT).show();
            } });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @JavascriptInterface
    public void setBars(final String color, final boolean light) {
        a.runOnUiThread(new Runnable() { public void run() {
            try {
                int c = Color.parseColor(color.trim());
                Window w = a.getWindow();
                w.setStatusBarColor(0);
                w.setNavigationBarColor(0);
                a.web.setBackgroundColor(c);
                ((android.view.View) a.web.getParent()).setBackgroundColor(c);
                if (Build.VERSION.SDK_INT >= 30) {
                    WindowInsetsController ic = w.getInsetsController();
                    if (ic != null) {
                        int m = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                        ic.setSystemBarsAppearance(light ? m : 0, m);
                    }
                }
            } catch (Exception ignored) { }
        } });
    }

    @JavascriptInterface
    public void googleToken(final boolean interactive) {
        a.runOnUiThread(new Runnable() { public void run() { a.fetchToken(interactive); } });
    }

    @JavascriptInterface
    public void invalidateToken(String token) {
        try { android.accounts.AccountManager.get(a).invalidateAuthToken("com.google", token); } catch (Exception ignored) { }
    }

    @JavascriptInterface
    public void googleSignOut() { a.prefs().edit().remove("acct").apply(); }

    @JavascriptInterface
    public void openUrl(final String url) {
        a.runOnUiThread(new Runnable() { public void run() {
            try { a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception ignored) { }
        } });
    }

    @JavascriptInterface
    public void share(final String subject, final String text) {
        a.runOnUiThread(new Runnable() { public void run() {
            Intent s = new Intent(Intent.ACTION_SEND);
            s.setType("text/plain");
            s.putExtra(Intent.EXTRA_SUBJECT, subject);
            s.putExtra(Intent.EXTRA_TEXT, text);
            a.startActivity(Intent.createChooser(s, subject));
        } });
    }

    /* share an image Plotline drew (year in review): saved to Pictures/Plotline, then the share sheet */
    @JavascriptInterface
    public boolean shareImage(final String name, final String b64, final String text) {
        try {
            byte[] png = android.util.Base64.decode(b64.substring(b64.indexOf(',') + 1), android.util.Base64.DEFAULT);
            ContentValues v = new ContentValues();
            v.put(MediaStore.Images.Media.DISPLAY_NAME, name); v.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            v.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Plotline");
            final Uri u = a.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
            if (u == null) return false;
            try (OutputStream o = a.getContentResolver().openOutputStream(u)) { o.write(png); }
            a.runOnUiThread(new Runnable() { public void run() {
                Intent s = new Intent(Intent.ACTION_SEND); s.setType("image/png"); s.putExtra(Intent.EXTRA_STREAM, u);
                if (text != null && !text.isEmpty()) s.putExtra(Intent.EXTRA_TEXT, text);
                s.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                a.startActivity(Intent.createChooser(s, "Share your year"));
            } });
            return true;
        } catch (Exception e) { return false; }
    }

    /* focus mode: hide the status and navigation bars; a swipe from the edge shows them briefly */
    @JavascriptInterface
    public void immersive(final boolean on) {
        a.runOnUiThread(new Runnable() { public void run() {
            try {
                Window w = a.getWindow();
                if (Build.VERSION.SDK_INT >= 30) {
                    WindowInsetsController c = w.getInsetsController();
                    if (c == null) return;
                    if (on) {
                        c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                        c.hide(android.view.WindowInsets.Type.systemBars());
                    } else c.show(android.view.WindowInsets.Type.systemBars());
                } else {
                    int base = android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
                    int f = base | (on ? (android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | android.view.View.SYSTEM_UI_FLAG_FULLSCREEN | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) : 0);
                    w.getDecorView().setSystemUiVisibility(f);
                }
            } catch (Exception ignored) { }
        } });
    }

    @JavascriptInterface
    public String widgetQueue() { return W.takeQueue(a.getApplicationContext()); }

    @JavascriptInterface
    public void widget(String json) {
        a.getSharedPreferences("plotline_widget", 0).edit().putString("data", json).apply();
        W.updateAll(a.getApplicationContext());
    }

    /* on-device model (Insights) — see LocalLlm */
    @JavascriptInterface public String llmStatus() { return LocalLlm.status(a); }
    @JavascriptInterface public void llmDownload(String url, String name) { LocalLlm.download(a, url, name); }
    @JavascriptInterface public void llmDelete(String name) { LocalLlm.delete(a, name); }
    @JavascriptInterface public void llmStart(String id, String name, int ctx, int threads) { LocalLlm.start(a, id, name, ctx, threads); }
    @JavascriptInterface public void llmChat(String id, String body) { LocalLlm.chat(a, id, body); }
    @JavascriptInterface public void llmCancel(String id) { LocalLlm.cancel(id); }
    @JavascriptInterface public void llmStop() { new Thread(new Runnable() { public void run() { LocalLlm.stop(); } }).start(); }

    /* ---- native app (1.14): the web engine reads and writes the one shared copy of the data ---- */
    @JavascriptInterface
    public boolean shellMode() { return a.shell != null; }

    @JavascriptInterface
    public String storeGet() {
        if (a.shell == null) return "";
        final NStore st = NStore.get(a);
        if (!st.exists()) return "";
        final String[] out = {""};
        final java.util.concurrent.CountDownLatch l = new java.util.concurrent.CountDownLatch(1);
        a.runOnUiThread(new Runnable() { public void run() { try { out[0] = st.json(); } finally { l.countDown(); } } });
        try { l.await(3, java.util.concurrent.TimeUnit.SECONDS); } catch (InterruptedException ignored) { }
        return out[0];
    }

    @JavascriptInterface
    public void storeSet(final String json) {
        if (a.shell == null || json == null || json.length() < 10) return;
        a.runOnUiThread(new Runnable() { public void run() {
            NStore st = NStore.get(a);
            boolean same = st.fromWeb(json);
            if (!same && st.onLocalChange != null) st.onLocalChange.run();
        } });
    }

    @JavascriptInterface
    public void unlocked() { a.runOnUiThread(new Runnable() { public void run() { a.unlocked(); } }); }

    @JavascriptInterface
    public void nativeOff() { NCrash.setNative(a, false); a.runOnUiThread(new Runnable() { public void run() { a.recreate(); } }); }

    @JavascriptInterface
    public void nativeOn() { NCrash.setNative(a, true); a.runOnUiThread(new Runnable() { public void run() { a.recreate(); } }); }

    @JavascriptInterface
    public boolean nativeEnabled() { return NCrash.nativeOn(a); }

    @JavascriptInterface
    public void closeClassic() { a.runOnUiThread(new Runnable() { public void run() { if (a.shell != null) a.shell.hideClassic(); } }); }
    /* 2.0.8: engine actions run with the web layer hidden */
    @JavascriptInterface
    public boolean classicOn() { return a.shell == null || a.shell.classic; }

    @JavascriptInterface
    public void ntoast(final String msg, final boolean undo) {
        a.runOnUiThread(new Runnable() { public void run() {
            if (a.shell == null || a.shell.classic) return;
            String m = android.text.Html.fromHtml(msg == null ? "" : msg, android.text.Html.FROM_HTML_MODE_LEGACY).toString().replace('\uFFFC', ' ').trim();
            if (m.isEmpty()) return;
            if (undo) a.shell.showToast(m, "Undo", new Runnable() { public void run() { a.js("ACT.undo&&ACT.undo()"); } });
            else NShell.toast(m);
        } });
    }

    @JavascriptInterface
    public void nroute(final String r) {
        a.runOnUiThread(new Runnable() { public void run() {
            if (a.shell == null) return;
            final NShell sh = a.shell; final int[] n = {0};
            sh.root.postDelayed(new Runnable() { public void run() {
                if (sh.route(r)) return;
                if (++n[0] < 6) { sh.root.postDelayed(this, 300); return; }   /* the engine's save reaches the store ~200ms later */
                sh.openClassic(r);
            } }, 380);
        } });
    }

    @JavascriptInterface
    public void nneedUi() { a.runOnUiThread(new Runnable() { public void run() { if (a.shell != null && !a.shell.classic) { a.shell.closeSheet(); a.shell.classicTab = -1; a.shell.showClassicLayer(false); } } }); }
    @JavascriptInterface
    public void nconfirm(final String t, final String m, final String ok, final boolean danger) {
        a.runOnUiThread(new Runnable() { public void run() {
            if (a.shell == null) return;
            NSheets.confirm(a.shell, t, m, ok, danger, new Runnable() { public void run() { a.js("window.__nconfYes&&window.__nconfYes()"); } });
        } });
    }
    /* a sheet the engine wants while the native screens are showing */
    @JavascriptInterface
    public void nsheet(final String kind, final String json) {
        a.runOnUiThread(new Runnable() { public void run() {
            if (a.shell == null) return;
            try { NEng.engSheet(a.shell, kind, new JSONObject(json == null || json.isEmpty() ? "{}" : json)); } catch (Exception e) { NCrash.log(a, "nsheet " + kind, e); }
        } });
    }
}
