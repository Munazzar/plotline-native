package com.munazzar.plotline;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import java.io.InputStream;
import org.json.JSONObject;

/* 2.0.8: the Studio room on Home (settings.home = 'studio'). The animated scene (cork board, bench, window with
   the real sky, the cat) is the 1.13 SVG/CSS scene itself, drawn by its own small scene view inside the native page:
   no web screens or sheets, and every tap is handled natively. The engine builds the page (window.__nstudiopage)
   and later hands over only the board/bench parts that changed (window.__nstudioparts), so the cat keeps walking. */
final class NStudio {
    final NShell sh;
    final Scene web;
    boolean loaded;
    float nTop, nBot, nL, nR;   /* the scrollable notes board, in CSS px from the room's top */
    int roomTop;

    final class Scene extends WebView {
        Scene(android.content.Context c) { super(c); }
        @Override public boolean onTouchEvent(MotionEvent e) {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                float d = getResources().getDisplayMetrics().density, x = e.getX() / d, y = e.getY() / d;
                if (x >= nL && x <= nR && y >= nTop && y <= nBot && getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
            }
            return super.onTouchEvent(e);
        }
        @Override protected void onWindowVisibilityChanged(int v) { super.onWindowVisibilityChanged(v); if (loaded) evaluateJavascript("window.__vis&&window.__vis(" + (v == View.VISIBLE) + ")", null); }
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    NStudio(final NShell sh) {
        this.sh = sh;
        web = new Scene(sh.a);
        web.setBackgroundColor(NTheme.bg);
        web.setVerticalScrollBarEnabled(false); web.setHorizontalScrollBarEnabled(false);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true); s.setTextZoom(100); s.setAllowFileAccess(false); s.setSupportZoom(false);
        s.setMediaPlaybackRequiresUserGesture(true);
        web.addJavascriptInterface(new Api(), "PlotStudio");
        web.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                String sc = u.getScheme() == null ? "" : u.getScheme();
                if (!sc.equals("http") && !sc.equals("https")) return null;   /* the page itself (data:) and about: */
                String hs = u.getHost() == null ? "" : u.getHost();
                if (hs.equals("fonts.googleapis.com") || hs.equals("fonts.gstatic.com")) return null;   /* same web fonts as the app */
                if (!MainActivity.HOST.equals(hs)) return new WebResourceResponse("text/plain", "utf-8", 403, "Blocked", null, new java.io.ByteArrayInputStream(new byte[0]));
                String p = u.getPath(); if (p == null || p.equals("/")) return null;
                try { InputStream in = sh.a.getAssets().open("www" + p); return new WebResourceResponse(MainActivity.mime(p), MainActivity.mime(p).startsWith("text") ? "utf-8" : null, in); }
                catch (Exception e) { return new WebResourceResponse("text/plain", "utf-8", 404, "Not found", null, new java.io.ByteArrayInputStream(new byte[0])); }
            }
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) { return true; }
            @Override public void onPageFinished(WebView v, String url) { loaded = true; v.evaluateJavascript("window.__sat&&window.__sat(" + Math.round(sh.top / v.getResources().getDisplayMetrics().density) + ")", null); }
        });
        web.setLayoutParams(new LinearLayout.LayoutParams(-1, NUi.dp(560)));
        load();
    }

    void load() {
        sh.a.jsRet("(window.__nstudiopage?window.__nstudiopage():'')", new android.webkit.ValueCallback<String>() { public void onReceiveValue(String v) {
            String h = NSheets.unq(v);
            if (h == null || h.isEmpty()) return;
            web.loadDataWithBaseURL("https://" + MainActivity.HOST + "/", h, "text/html", "utf-8", null);
        } });
    }

    /* the data changed: only the board, tray and journal are rebuilt */
    void update() {
        if (!loaded) return;
        sh.a.jsRet("(window.__nstudioparts?window.__nstudioparts():'')", new android.webkit.ValueCallback<String>() { public void onReceiveValue(String v) {
            String p = NSheets.unq(v); if (p == null || p.isEmpty() || !loaded) return;
            web.evaluateJavascript("window.__parts&&window.__parts(" + p + ")", null);
        } });
    }

    View view() {
        if (web.getParent() instanceof android.view.ViewGroup) ((android.view.ViewGroup) web.getParent()).removeView(web);
        return web;
    }

    final class Api {
        @JavascriptInterface public void size(final float top, final float h) {
            web.post(new Runnable() { public void run() {
                float d = web.getResources().getDisplayMetrics().density;
                android.view.ViewGroup.LayoutParams l = web.getLayoutParams();
                int nh = Math.max(NUi.dp(200), Math.round(h * d));
                if (l != null && l.height != nh) { l.height = nh; web.setLayoutParams(l); }
                roomTop = Math.round(top * d); web.scrollTo(0, roomTop);
            } });
        }
        @JavascriptInterface public void notes(float l, float t, float r, float b) { nL = l; nTop = t; nR = r; nBot = b; }
        @JavascriptInterface public void play(final boolean v) {
            web.post(new Runnable() { public void run() {
                try { JSONObject s = sh.st.settings().optJSONObject("studio"); if (s == null) { s = new JSONObject(); sh.st.settings().put("studio", s); } s.put("play", v); } catch (Exception ignored) { }
                sh.saveQuiet();
            } });
        }
        @JavascriptInterface public void act(final String a, final String json) {
            web.post(new Runnable() { public void run() {
                JSONObject d; try { d = new JSONObject(json == null ? "{}" : json); } catch (Exception e) { d = new JSONObject(); }
                d.remove("act");
                String id = d.optString("id");
                switch (a) {
                    case "go": if (!sh.route(d.optString("to"))) sh.openClassic(d.optString("to")); break;
                    case "open": sh.route("goal/" + id); break;
                    case "openHabit": sh.route("habit/" + id); break;
                    case "stJournal": sh.tabTo(5); break;
                    case "addEntry": new NForms(sh).entry(null); break;
                    default: sh.run(a, d);
                }
            } });
        }
    }

    /* Home → the room toggle (web homeStyle) */
    static void setHome(NShell sh, String k) {
        try { sh.st.settings().put("home", k); } catch (Exception ignored) { }
        sh.save();
        NShell.toast("studio".equals(k) ? "Studio room" : "Classic cards view");
    }

    static boolean on(NShell sh) { return "studio".equals(sh.st.settings().optString("home")); }
}
