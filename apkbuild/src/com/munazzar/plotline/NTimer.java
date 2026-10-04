package com.munazzar.plotline;

import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

/* The focus timer on a step: pick minutes, a pill shows the countdown, a sheet asks if the step is done when it ends.
   The timer lives in settings.timer (the engine schedules the end-of-session alarm from it). */
final class NTimer {
    final NShell sh; LinearLayout pill; TextView tt, tn; long end = 0; String gid = "", sid = "", title = ""; int mins = 0; boolean fired;
    final Handler h = new Handler(Looper.getMainLooper());
    final Runnable tick = new Runnable() { public void run() { paint(); } };

    NTimer(NShell sh) { this.sh = sh; }

    static NTimer of(NShell sh) { if (sh.timer == null) sh.timer = new NTimer(sh); return sh.timer; }

    JSONObject timer() { JSONObject t = sh.st.settings().optJSONObject("timer"); return t; }

    /* called when the data changes */
    void sync() {
        JSONObject t = timer();
        if (t == null || t.optString("s").isEmpty()) { end = 0; hide(); return; }
        JSONObject g = sh.st.find("goals", t.optString("g")); JSONObject s = null;
        JSONArray a = g == null ? null : g.optJSONArray("steps");
        for (int i = 0; a != null && i < a.length(); i++) { JSONObject x = a.optJSONObject(i); if (x != null && t.optString("s").equals(x.optString("id"))) s = x; }
        if (s == null) { try { sh.st.settings().put("timer", JSONObject.NULL); } catch (Exception ignored) { } sh.saveQuiet(); hide(); return; }
        end = t.optLong("end"); gid = t.optString("g"); sid = t.optString("s"); mins = t.optInt("m"); title = s.optString("title");
        if (end != t.optLong("end") || !fired) { }
        fired = false; show();
    }

    void show() {
        if (pill == null || pill.getParent() != sh.nroot) {
            if (pill != null && pill.getParent() instanceof android.view.ViewGroup) ((android.view.ViewGroup) pill.getParent()).removeView(pill);
            android.content.Context c = sh.a;
            pill = NUi.row(c); pill.setGravity(Gravity.CENTER_VERTICAL); pill.setBackground(NUi.round(NTheme.bg2, 99, NTheme.line2)); pill.setPadding(NUi.dp(18), NUi.dp(8), NUi.dp(8), NUi.dp(8)); pill.setElevation(NUi.dp(10));
            tt = NUi.body(c, "--:--", 24, NTheme.accent, 800); tt.setTypeface(NFont.display(800)); pill.addView(tt, NUi.lp(NUi.dp(66), -2));
            tn = NUi.text(c, "", 13, NTheme.muted); tn.setSingleLine(true); tn.setEllipsize(android.text.TextUtils.TruncateAt.END); tn.setMaxWidth(NUi.dp(120)); pill.addView(tn);
            pill.addView(NUi.ibtn(c, "plus", new View.OnClickListener() { public void onClick(View v) { addTime(); } }));
            pill.addView(NUi.ibtn(c, "stop", new View.OnClickListener() { public void onClick(View v) { stop(); } }));
            NUi.tap(tn, new View.OnClickListener() { public void onClick(View v) { sh.push(new NGoalScreen(sh, gid)); } });
            FrameLayout.LayoutParams l = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL); l.bottomMargin = sh.bot + NUi.dp(92);
            sh.nroot.addView(pill, l);
        }
        tn.setText(title); pill.setVisibility(View.VISIBLE); h.removeCallbacks(tick); paint();
    }

    void hide() { h.removeCallbacks(tick); if (pill != null) pill.setVisibility(View.GONE); }

    void paint() {
        if (end <= 0 || pill == null) return;
        long left = end - System.currentTimeMillis();
        if (left <= 0) { finish(); return; }
        tt.setText((left / 60000) + ":" + String.format(java.util.Locale.US, "%02d", (left % 60000) / 1000));
        h.removeCallbacks(tick); h.postDelayed(tick, 500);
    }

    void start(String g, String s, int m) {
        try { JSONObject t = new JSONObject(); t.put("g", g); t.put("s", s); t.put("end", System.currentTimeMillis() + m * 60000L); t.put("m", m); sh.st.settings().put("timer", t); } catch (Exception ignored) { }
        sh.save(); sh.a.askNotifications(); sync(); NShell.toast("Focus timer: " + m + " min");
    }

    void stop() { try { sh.st.settings().put("timer", JSONObject.NULL); } catch (Exception ignored) { } sh.save(); end = 0; hide(); NShell.toast("Timer stopped"); }

    void addTime() { JSONObject t = timer(); if (t == null) return; try { t.put("end", t.optLong("end") + 5 * 60000L); } catch (Exception ignored) { } sh.save(); sync(); }

    /* ---- time's up ---- */
    void finish() {
        if (fired) return; fired = true; hide();
        final String g = gid, s = sid, ttl = title; final int m = mins; end = 0;
        sh.a.jsRet("(window.__ntimerfin?window.__ntimerfin():'')", new android.webkit.ValueCallback<String>() { public void onReceiveValue(String v) {
            try { sh.st.settings().put("timer", JSONObject.NULL); } catch (Exception ignored) { }
            sh.saveQuiet();
            JSONObject st = sh.st.find("goals", g); boolean done = false; JSONArray a = st == null ? null : st.optJSONArray("steps");
            for (int i = 0; a != null && i < a.length(); i++) { JSONObject x = a.optJSONObject(i); if (x != null && s.equals(x.optString("id"))) done = x.optBoolean("done"); }
            NShell.toast("Focus session complete · " + m + " min on “" + ttl + "”");
            NFx.vib(sh.a); NFx.beep(); NFx.burst(sh);
            if (!done) timesUp(g, s, ttl, m);
        } });
    }

    void timesUp(final String g, final String s, String ttl, int m) {
        android.content.Context c = sh.a; NForms F = new NForms(sh);
        LinearLayout b = F.sheetBody("Time’s up · " + m + " min logged", ttl, "Is this step done?");
        LinearLayout r = NUi.row(c); r.setGravity(Gravity.END); r.setPadding(0, NUi.dp(20), 0, 0);
        r.addView(NUi.btn(c, "5 more min", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); start(g, s, 5); } }));
        LinearLayout.LayoutParams l = NUi.lp(-2, -2); l.leftMargin = NUi.dp(8); r.addView(NUi.btn(c, "Not yet", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } }), l);
        r.addView(NUi.btn(c, "Mark done", true, new View.OnClickListener() { public void onClick(View v) {
            sh.closeSheet(); JSONObject gg = sh.st.find("goals", g); JSONArray a = gg == null ? null : gg.optJSONArray("steps");
            for (int i = 0; a != null && i < a.length(); i++) { JSONObject x = a.optJSONObject(i); if (x != null && s.equals(x.optString("id")) && !x.optBoolean("done")) { String msg = NActs.toggleStep(sh.st, gg, x); sh.save(); if (msg != null) NShell.toast(msg); } }
        } }), l);
        b.addView(r); sh.sheet(b);
    }

    /* ---- pick minutes ---- */
    static void pick(final NShell sh, final String g, final String s) {
        JSONObject gg = sh.st.find("goals", g); JSONArray a = gg == null ? null : gg.optJSONArray("steps"); String ttl = "";
        for (int i = 0; a != null && i < a.length(); i++) { JSONObject x = a.optJSONObject(i); if (x != null && s.equals(x.optString("id"))) ttl = x.optString("title"); }
        if (ttl.isEmpty()) return;
        android.content.Context c = sh.a; NForms F = new NForms(sh);
        LinearLayout b = F.sheetBody("Focus timer", ttl, "You’ll get a chime and a notification when it ends. The minutes are logged on this step.");
        NFlow fl = new NFlow(c, 8, 8);
        for (final int m : new int[]{5, 10, 15, 25, 45, 60, 90}) fl.addView(NUi.chip(c, m + " min", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); of(sh).start(g, s, m); } }), new android.view.ViewGroup.MarginLayoutParams(-2, NUi.dp(42)));
        b.addView(fl, NUi.mt(14));
        b.addView(NForms.actions(c, NUi.btn(c, "Cancel", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } })));
        sh.sheet(b);
    }
}
