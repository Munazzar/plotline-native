package com.munazzar.plotline;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.SharedPreferences;
import org.json.JSONObject;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Window;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {
    static final String HOST = "appassets.androidplatform.net";
    static final int REQ_FILE = 11, REQ_NOTIF = 12, REQ_ACCT = 13, REQ_CONSENT = 14, REQ_NIMP = 15, REQ_NIMG = 16;
    String imgKey = "cardImg";
    interface ImgCb { void got(String url); }
    ImgCb imgCb;

    void pickImage(String key) {
        imgKey = key;
        Intent it = new Intent(Intent.ACTION_OPEN_DOCUMENT); it.addCategory(Intent.CATEGORY_OPENABLE); it.setType("image/*");
        try { startActivityForResult(it, REQ_NIMG); } catch (Exception e) { NShell.toast("No file picker on this phone"); }
    }
    String importTxt = "", importKind = "backup";

    void pickImport(String kind) {
        importKind = kind;
        Intent it = new Intent(Intent.ACTION_OPEN_DOCUMENT); it.addCategory(Intent.CATEGORY_OPENABLE); it.setType("*/*");
        try { startActivityForResult(it, REQ_NIMP); } catch (Exception e) { NShell.toast("No file picker on this phone"); }
    }
    static final String SCOPE = "oauth2:https://www.googleapis.com/auth/drive.appdata";
    String pendingJs;
    boolean pageReady;
    WebView web;
    int barTop, barBot;
    ValueCallback<Uri[]> fileCb;
    Uri camUri;
    NShell shell;
    boolean lockShown;
    long stoppedAt;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Window w = getWindow();
        w.setStatusBarColor(0xFF0A0F1F);
        w.setNavigationBarColor(0xFF0A0F1F);
        Reminders.createChannel(this);
        NCrash.install(this);

        web = new WebView(this);
        web.setBackgroundColor(0xFF0A0F1F);
        web.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        web.setVerticalScrollBarEnabled(false); web.setHorizontalScrollBarEnabled(false);
        /* edge to edge: the page draws behind transparent status and navigation bars; their heights reach
           CSS as --sat/--sab. The keyboard is handled by padding the root so inputs stay visible. */
        final android.widget.FrameLayout root = new android.widget.FrameLayout(this);
        root.setBackgroundColor(0xFF0A0F1F);
        root.addView(web, new android.widget.FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        w.setStatusBarColor(0); w.setNavigationBarColor(0);
        w.setNavigationBarContrastEnforced(false); w.setStatusBarContrastEnforced(false);
        if (Build.VERSION.SDK_INT >= 30) w.setDecorFitsSystemWindows(false);
        /* full-screen (focus) mode: draw into the camera cut-out too, so no black band is left at the top */
        android.view.WindowManager.LayoutParams lp = w.getAttributes(); lp.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES; w.setAttributes(lp);
        w.getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        root.setOnApplyWindowInsetsListener(new android.view.View.OnApplyWindowInsetsListener() {
            @Override public android.view.WindowInsets onApplyWindowInsets(android.view.View v, android.view.WindowInsets in) {
                float d = getResources().getDisplayMetrics().density;
                int top = in.getSystemWindowInsetTop(), bot = in.getSystemWindowInsetBottom(), nav = in.getStableInsetBottom();
                boolean kb = bot > nav + (int) (80 * d);
                root.setPadding(0, 0, 0, kb ? bot : 0);
                barTop = Math.round(top / d); barBot = kb ? 0 : Math.round(bot / d);
                if (shell != null) shell.insets(top, kb ? 0 : bot, kb ? bot : 0);
                js("document.documentElement.style.setProperty('--sat','" + barTop + "px');document.documentElement.style.setProperty('--sab','" + barBot + "px')");
                return in;
            }
        });

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setTextZoom(100);
        s.setSupportZoom(false); s.setBuiltInZoomControls(false); s.setDisplayZoomControls(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportMultipleWindows(false);

        web.addJavascriptInterface(new Bridge(this), "PlotlineNative");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if (!HOST.equals(u.getHost())) return null;
                String p = u.getPath();
                if (p == null || p.equals("/")) p = "/index.html";
                try {
                    InputStream in = getAssets().open("www" + p);
                    WebResourceResponse res = new WebResourceResponse(mime(p), mime(p).startsWith("text") ? "utf-8" : null, in);
                    // Cross-origin isolation lets the on-device model (wllama) use several CPU threads.
                    if (!p.endsWith("picker.html")) {   /* Google's picker runs in its own window without isolation */
                        Map<String, String> h = new HashMap<>();
                        h.put("Cross-Origin-Opener-Policy", "same-origin");
                        h.put("Cross-Origin-Embedder-Policy", "credentialless");
                        h.put("Cross-Origin-Resource-Policy", "same-origin");
                        res.setResponseHeaders(h);
                    }
                    return res;
                } catch (IOException e) {
                    return new WebResourceResponse("text/plain", "utf-8", 404, "Not found", null, new ByteArrayInputStream(new byte[0]));
                }
            }

            @Override
            public void onPageFinished(WebView v, String url) {
                pageReady = true;
                if (pendingJs != null) { v.evaluateJavascript(pendingJs, null); pendingJs = null; }
                v.evaluateJavascript("document.documentElement.style.setProperty('--sat','" + barTop + "px');document.documentElement.style.setProperty('--sab','" + barBot + "px')", null);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if (HOST.equals(u.getHost())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) { }
                return true;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCb != null) fileCb.onReceiveValue(null);
                fileCb = cb;
                String[] types = p.getAcceptTypes();
                String t = (types != null && types.length > 0 && types[0] != null && types[0].length() > 0) ? types[0] : "*/*";
                if (t.contains(",")) t = t.contains("json") ? "application/json" : "*/*";
                Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                pick.addCategory(Intent.CATEGORY_OPENABLE);
                pick.setType(t);
                Intent chooser = Intent.createChooser(pick, t.startsWith("image") ? "Add a photo" : "Choose a file");
                camUri = null;
                if (t.startsWith("image")) {
                    try {
                        ContentValues cv = new ContentValues();
                        cv.put(MediaStore.Images.Media.DISPLAY_NAME, "plotline_" + System.currentTimeMillis() + ".jpg");
                        cv.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                        cv.put("relative_path", "Pictures/Plotline");
                        camUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
                        Intent cam = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        cam.putExtra(MediaStore.EXTRA_OUTPUT, camUri);
                        cam.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cam});
                    } catch (Exception e) { camUri = null; }
                }
                try { startActivityForResult(chooser, REQ_FILE); }
                catch (Exception e) { fileCb = null; return false; }
                return true;
            }
        });

        /* native app on top; the web page underneath stays hidden and runs sync, reminders and widgets */
        if (NCrash.nativeOn(this)) {
            try {
                web.setVisibility(android.view.View.INVISIBLE);
                shell = new NShell(this, root);
                JSONObject st0 = NStore.get(this).settings();
                /* brand-new: the web welcome and setup run first; first launch after the update: the engine moves the data over */
                if (!st0.optBoolean("onboarded")) { final NShell fs = shell; root.post(new Runnable() { public void run() { NWelcome.run(fs); } }); }
                /* web landHere: open on the chosen start page (settings.landing) unless something else asked for a page */
                if (state == null && st0.optBoolean("onboarded") && getIntent().getStringExtra("route") == null) {
                    String L = st0.optString("landing", ""); if (L.equals("day/__today")) L = "day/" + NDates.ymd();
                    String[] p = L.split("/"); NStore ns = NStore.get(this);
                    boolean ok = !L.isEmpty() && !(p.length > 1 && ((p[0].equals("thread") && ns.find("threads", p[1]) == null) || (p[0].equals("habit") && ns.find("habits", p[1]) == null) || (p[0].equals("goal") && ns.find("goals", p[1]) == null)));
                    if (ok) shell.route(L);
                }
                if (lockOn()) showLock();
                /* a clean start clears the crash counter (two crashes in a row fall back to the classic app) */
                root.postDelayed(new Runnable() { public void run() { NCrash.p(MainActivity.this).edit().putInt("crashes", 0).apply(); } }, 15000);
            } catch (Throwable e) {
                NCrash.log(this, "shell start", e);
                NShell bad = NShell.I;
                if (bad != null) { NStore.get(this).listeners.remove(bad); try { if (bad.nroot != null) root.removeView(bad.nroot); } catch (Exception ignored) { } NShell.I = null; }
                shell = null; web.setVisibility(android.view.View.VISIBLE);
            }
        }
        if (state != null) web.restoreState(state);
        else web.loadUrl("https://" + HOST + "/index.html");
        handleLink(getIntent());
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        handleLink(i);
    }

    /* plotline://auth#access_token=...&expires_in=... comes back from browser sign-in */
    void handleLink(Intent i) {
        if (i == null) return;
        String route = i.getStringExtra("route");
        if (route != null && route.matches("[A-Za-z0-9/-]{1,60}")) {
            if (shell != null && !lockShown && shell.route(route)) { i.removeExtra("route"); return; }
            if (shell != null) shell.showClassicLayer(false);
            js("window.__route&&window.__route('" + route + "')");
            i.removeExtra("route");
        }
        if (i.getData() == null) return;
        Uri d = i.getData();
        if (!"plotline".equals(d.getScheme()) || !"auth".equals(d.getHost())) return;
        String frag = d.getEncodedFragment();
        if (frag == null) return;
        Uri q = Uri.parse("x://x?" + frag);
        String tok = q.getQueryParameter("access_token");
        String exp = q.getQueryParameter("expires_in");
        try {
            JSONObject o = new JSONObject();
            if (tok != null) o.put("token", tok); else o.put("err", "no_token");
            o.put("exp", exp != null ? Integer.parseInt(exp) : 3600);
            js("window.__gauth&&window.__gauth(" + JSONObject.quote(o.toString()) + ")");
        } catch (Exception ignored) { }
        i.setData(null);
    }

    void js(final String code) {
        runOnUiThread(new Runnable() { public void run() {
            if (pageReady) web.evaluateJavascript(code, null); else pendingJs = pendingJs == null ? code : pendingJs + ";" + code;
        } });
    }

    /* ask the web engine for a value (a JSON string) and get it on the UI thread */
    void jsRet(final String code, final ValueCallback<String> cb) {
        runOnUiThread(new Runnable() { public void run() {
            if (!pageReady) { cb.onReceiveValue(null); return; }
            web.evaluateJavascript(code, new ValueCallback<String>() { @Override public void onReceiveValue(String v) {
                String out = null;
                try { if (v != null && !v.equals("null")) { Object o = new org.json.JSONTokener(v).nextValue(); out = o == null ? null : String.valueOf(o); } } catch (Exception ignored) { }
                cb.onReceiveValue(out);
            } });
        } });
    }

    SharedPreferences prefs() { return getSharedPreferences("plotline_auth", 0); }

    void sendAuth(String token, String email, String err) {
        try {
            JSONObject o = new JSONObject();
            if (token != null) { o.put("token", token); o.put("native", true); o.put("exp", 3000); }
            if (email != null) o.put("email", email);
            if (err != null) o.put("err", err);
            js("window.__gauth&&window.__gauth(" + JSONObject.quote(o.toString()) + ")");
        } catch (Exception ignored) { }
    }

    void chooseAccount() {
        try {
            Intent it = AccountManager.newChooseAccountIntent(null, null, new String[]{"com.google"}, null, null, null, null);
            startActivityForResult(it, REQ_ACCT);
        } catch (Exception e) { sendAuth(null, null, "no_account_picker"); }
    }

    void fetchToken(final boolean interactive) {
        final String name = prefs().getString("acct", null);
        if (name == null) { if (interactive) chooseAccount(); else sendAuth(null, null, "no_account"); return; }
        Account acct = new Account(name, "com.google");
        AccountManager am = AccountManager.get(this);
        AccountManagerCallback<Bundle> cb = new AccountManagerCallback<Bundle>() {
            public void run(AccountManagerFuture<Bundle> f) {
                try {
                    Bundle b = f.getResult();
                    String t = b.getString(AccountManager.KEY_AUTHTOKEN);
                    if (t != null) { sendAuth(t, name, null); return; }
                    Intent need = b.getParcelable(AccountManager.KEY_INTENT);
                    if (need != null && interactive) { startActivityForResult(need, REQ_CONSENT); return; }
                    sendAuth(null, null, need != null ? "needs_consent" : "no_token");
                } catch (Exception e) {
                    sendAuth(null, null, e.getClass().getSimpleName() + (e.getMessage() != null ? ": " + e.getMessage() : ""));
                }
            }
        };
        String sc = "oauth2:" + prefs().getString("scopes", SCOPE.substring(7));
        if (interactive) am.getAuthToken(acct, sc, null, this, cb, null);
        else am.getAuthToken(acct, sc, null, false, cb, null);
    }

    static String mime(String p) {
        p = p.toLowerCase();
        if (p.endsWith(".html")) return "text/html";
        if (p.endsWith(".js")) return "text/javascript";
        if (p.endsWith(".css")) return "text/css";
        if (p.endsWith(".json")) return "application/json";
        if (p.endsWith(".woff2")) return "font/woff2";
        if (p.endsWith(".png")) return "image/png";
        if (p.endsWith(".wasm")) return "application/wasm";
        if (p.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }

    @Override
    protected void onActivityResult(int req, int rc, Intent data) {
        if (req == REQ_NIMG) {
            if (rc == RESULT_OK && data != null && data.getData() != null && shell != null) {
                try {
                    java.io.InputStream in = getContentResolver().openInputStream(data.getData());
                    android.graphics.BitmapFactory.Options o = new android.graphics.BitmapFactory.Options(); o.inJustDecodeBounds = true;
                    java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream(); byte[] buf = new byte[16384]; int n;
                    while ((n = in.read(buf)) > 0) bo.write(buf, 0, n); in.close();
                    byte[] raw = bo.toByteArray();
                    android.graphics.BitmapFactory.decodeByteArray(raw, 0, raw.length, o);
                    int ss = 1; while (Math.max(o.outWidth, o.outHeight) / ss > 1400) ss *= 2;
                    o = new android.graphics.BitmapFactory.Options(); o.inSampleSize = ss;
                    android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeByteArray(raw, 0, raw.length, o);
                    float sc = Math.min(1f, 1000f / Math.max(bm.getWidth(), bm.getHeight()));
                    if (sc < 1f) bm = android.graphics.Bitmap.createScaledBitmap(bm, Math.round(bm.getWidth() * sc), Math.round(bm.getHeight() * sc), true);
                    java.io.ByteArrayOutputStream jo = new java.io.ByteArrayOutputStream(); bm.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, jo);
                    String url = "data:image/jpeg;base64," + android.util.Base64.encodeToString(jo.toByteArray(), android.util.Base64.NO_WRAP);
                    if (imgCb != null) { ImgCb cb = imgCb; imgCb = null; cb.got(url); NShell.toast("Image added"); return; }
                    shell.st.settings().put(imgKey, url); if (imgKey.equals("cardImg")) shell.st.settings().put("cardBg", "image");
                    shell.save(); NShell.toast("Image set");
                } catch (Exception e) { NShell.toast("Couldn’t use that image"); }
            }
            return;
        }
        if (req == REQ_NIMP) {
            if (rc == RESULT_OK && data != null && data.getData() != null) {
                try {
                    java.io.InputStream in = getContentResolver().openInputStream(data.getData());
                    java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream(); byte[] buf = new byte[16384]; int n;
                    while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
                    in.close(); importTxt = bo.toString("UTF-8");
                    if (shell != null) shell.run("nImport", NMore.d("k", importKind));
                } catch (Exception e) { NShell.toast("Couldn’t read that file"); }
            }
            return;
        }
        if (req == REQ_ACCT) {
            if (rc == RESULT_OK && data != null && data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME) != null) {
                prefs().edit().putString("acct", data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)).apply();
                fetchToken(true);
            } else sendAuth(null, null, "Sign-in cancelled");
            return;
        }
        if (req == REQ_CONSENT) {
            if (rc == RESULT_OK) fetchToken(false); else sendAuth(null, null, "Permission not granted");
            return;
        }
        if (req == REQ_FILE && fileCb != null) {
            Uri[] res = null;
            if (rc == RESULT_OK) {
                if (data != null && data.getData() != null) {
                    res = new Uri[]{data.getData()};
                    dropCam();
                } else if (camUri != null) {
                    res = new Uri[]{camUri};
                }
            } else dropCam();
            fileCb.onReceiveValue(res);
            fileCb = null;
            camUri = null;
            return;
        }
        super.onActivityResult(req, rc, data);
    }

    void dropCam() {
        if (camUri != null) {
            try { getContentResolver().delete(camUri, null, null); } catch (Exception ignored) { }
            camUri = null;
        }
    }

    /* runtime permissions asked from the page; the answer goes to window.__perm(key, granted) */
    String permKey = ""; static NVoice nativeMic;
    void askPerms(String key, String[] perms) {
        permKey = key;
        boolean all = true; for (String p : perms) if (checkSelfPermission(p) != android.content.pm.PackageManager.PERMISSION_GRANTED) all = false;
        if (all && key.equals("nmic")) { if (nativeMic != null) nativeMic.permResult(true); return; }
        if (all) { js("window.__perm&&window.__perm(" + org.json.JSONObject.quote(key) + ",true)"); return; }
        requestPermissions(perms, 20);
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] res) {
        super.onRequestPermissionsResult(req, perms, res);
        if (req != 20) return;
        boolean ok = res.length > 0; for (int r : res) if (r != android.content.pm.PackageManager.PERMISSION_GRANTED) ok = false;
        if (permKey.equals("nmic")) { if (nativeMic != null) nativeMic.permResult(ok); return; }
        js("window.__perm&&window.__perm(" + org.json.JSONObject.quote(permKey) + "," + ok + ")");
    }

    void askNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, REQ_NOTIF);
        }
    }

    @Override
    public void onBackPressed() {
        if (shell != null) {
            if (lockShown) { moveTaskToBack(true); return; }
            if (shell.back()) return;
            if (!shell.classic) { moveTaskToBack(true); return; }
            web.evaluateJavascript("(window.__nback&&window.__nback())?'1':'0'", new ValueCallback<String>() {
                @Override public void onReceiveValue(String v) { if (v == null || !v.contains("1")) shell.hideClassic(); }
            });
            return;
        }
        web.evaluateJavascript("(window.__back&&window.__back())?'1':'0'", new ValueCallback<String>() {
            @Override public void onReceiveValue(String v) {
                if (v == null || !v.contains("1")) moveTaskToBack(true);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    protected void onDestroy() {
        if (shell != null) { NStore.get(this).listeners.remove(shell); if (NShell.I == shell) { NShell.I = null; NStore.get(this).onLocalChange = null; } }
        LocalLlm.stop(); super.onDestroy();
    }

    /* ---- app lock in the native app: the web lock screen is shown over everything until it reports unlocked ---- */
    boolean lockOn() {
        JSONObject st = NStore.get(this).settings();
        JSONObject l = st.optJSONObject("lock");
        return !st.optString("pinHash", "").isEmpty() || (l != null && l.optBoolean("on"));
    }

    void showLock() {
        if (shell == null) return;
        if (shell.classic) shell.hideClassic();
        lockShown = true;
        shell.closeSheet();
        NLock.show(shell);   /* 2.0.8: native lock screen */
    }

    void unlocked() {
        if (shell == null || !lockShown) return;
        lockShown = false;
        NLock.hide(shell);
    }

    @Override
    protected void onStop() { super.onStop(); stoppedAt = System.currentTimeMillis(); }

    @Override
    protected void onStart() {
        super.onStart();
        if (shell != null && stoppedAt > 0 && !lockShown && lockOn()) {
            JSONObject l = NStore.get(this).settings().optJSONObject("lock");
            long after = l == null ? 0 : l.optLong("after", 0) * 60000L;
            if (System.currentTimeMillis() - stoppedAt >= after) showLock();
        }
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration c) {
        super.onConfigurationChanged(c);
        if (shell != null) try { if (!shell.lookKey().equals(shell.look)) shell.build(); } catch (Exception e) { NCrash.log(this, "config", e); }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shell != null) try { shell.resume(); } catch (Exception e) { NCrash.log(this, "resume", e); }
        if (web != null) web.evaluateJavascript("window.dispatchEvent(new Event('app-resume'))", null);
    }
}
