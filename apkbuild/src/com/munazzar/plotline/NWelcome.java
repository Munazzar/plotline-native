package com.munazzar.plotline;

import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

/* First run: the welcome (name, start fresh / explore with examples), the one-time setup list, then the tour offer.
   The choices are applied by the engine (examples, settings); this draws them. */
final class NWelcome {
    final NShell sh; final android.content.Context c;
    FrameLayout root; EditText name; JSONArray items = new JSONArray(); boolean setupOpen;

    NWelcome(NShell sh) { this.sh = sh; this.c = sh.a; }

    static void run(NShell sh) { new NWelcome(sh).show(); }

    void show() {
        root = new FrameLayout(c); root.setBackgroundColor(NTheme.bg); root.setClickable(true);
        ScrollView sv = new ScrollView(c); sv.setFillViewport(true); sv.setVerticalScrollBarEnabled(false);
        LinearLayout col = NUi.col(c); col.setGravity(Gravity.CENTER_VERTICAL); col.setPadding(NUi.dp(26), sh.top + NUi.dp(30), NUi.dp(26), sh.bot + NUi.dp(30));
        col.addView(NUi.label(c, "Welcome to Plotline", NTheme.accent));
        TextView h = NUi.title(c, "Small steps.\nA clear path.\nYour whole life in view.", 38); h.setPadding(0, NUi.dp(12), 0, 0); col.addView(h);
        TextView p = NUi.text(c, "Set short and long-term goals, break them into steps, connect them, and watch the bigger picture take shape. Everything stays on your device.", 15, NTheme.muted); p.setLineSpacing(0, 1.25f); col.addView(p, NUi.mt(14));
        col.addView(NForms.fieldLabel(c, "What should we call you?"));
        name = NForms.input(c, "Your first name", "", false); name.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS | android.text.InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        col.addView(name);
        LinearLayout b = NUi.row(c);
        b.addView(NUi.btn(c, "Start fresh", false, new View.OnClickListener() { public void onClick(View v) { choose("empty"); } }), NUi.lpw(0, NUi.dp(50), 1));
        LinearLayout.LayoutParams l = NUi.lpw(0, NUi.dp(50), 1.4f); l.leftMargin = NUi.dp(10);
        b.addView(NUi.btn(c, "Explore with examples", true, new View.OnClickListener() { public void onClick(View v) { choose("demo"); } }), l);
        col.addView(b, NUi.mt(22));
        TextView sy = NUi.link(c, "I already use Plotline on another device", new View.OnClickListener() { public void onClick(View v) { choose("sync"); } });
        sy.setGravity(Gravity.CENTER); col.addView(sy, NUi.mt(18));
        sv.addView(col, new FrameLayout.LayoutParams(-1, -2));
        root.addView(sv, new FrameLayout.LayoutParams(-1, -1));
        sh.nroot.addView(root, new FrameLayout.LayoutParams(-1, -1));
    }

    void close() { sh.hideKeyboard(); if (root != null && root.getParent() != null) ((android.view.ViewGroup) root.getParent()).removeView(root); }

    void choose(String kind) {
        String nm = name.getText().toString().trim();
        if (kind.equals("sync")) {
            try { sh.st.settings().put("onboarded", true); } catch (Exception ignored) { }
            sh.save(); sh.a.js("window.__nonboard&&window.__nonboard('','sync')");
            close(); sh.waitOnboard = false; sh.openSettings(); return;
        }
        sh.a.js("window.__nonboard&&window.__nonboard(" + JSONObject.quote(nm) + "," + JSONObject.quote(kind) + ")");
        try { sh.st.settings().put("onboarded", true); sh.st.settings().put("name", nm); } catch (Exception ignored) { }
        close(); sh.waitOnboard = false;
        sh.hideClassic();
        root.postDelayed(new Runnable() { public void run() { setup(); } }, 500);
    }

    /* ---- one-time setup ---- */
    void setup() {
        sh.a.jsRet("(window.__nsetup?window.__nsetup():'[]')", new android.webkit.ValueCallback<String>() { public void onReceiveValue(String v) {
            try { String j = v; if (j != null && j.startsWith("\"")) j = new JSONObject("{\"x\":" + j + "}").getString("x"); items = new JSONArray(j == null ? "[]" : j); } catch (Exception e) { items = new JSONArray(); }
            paintSetup();
        } });
    }

    void paintSetup() {
        NForms F = new NForms(sh);
        LinearLayout b = F.sheetBody("Set up Plotline", "A few choices, once", "Each one is optional and you can change it any time in Settings.");
        for (int k = 0; k < items.length(); k++) {
            final JSONObject o = items.optJSONObject(k); if (o == null) continue;
            LinearLayout r = NUi.row(c); r.setPadding(0, NUi.dp(12), 0, NUi.dp(4)); r.setGravity(Gravity.CENTER_VERTICAL);
            String ic = o.optString("k").equals("notif") ? "bell" : o.optString("k").equals("exact") ? "timer" : o.optString("k").equals("lock") ? "lock" : "link";
            r.addView(NUi.icon(c, o.optBoolean("done") ? "check" : ic, 20, o.optBoolean("done") ? NTheme.accent : NTheme.muted), NUi.lp(NUi.dp(34), NUi.dp(20)));
            LinearLayout tx = NUi.col(c); tx.addView(NUi.body(c, o.optString("t") + (o.optBoolean("opt") && !o.optBoolean("done") ? "  · optional" : ""), 15.5f, NTheme.text, 700));
            TextView s = NUi.text(c, o.optString("x"), 12.5f, NTheme.muted); s.setLineSpacing(0, 1.15f); tx.addView(s, NUi.mt(2));
            r.addView(tx, NUi.lpw(0, -2, 1));
            if (o.optBoolean("done")) r.addView(NUi.body(c, "On", 13, NTheme.accent, 700));
            else r.addView(NUi.btn(c, o.optString("btn"), !o.optBoolean("opt"), new View.OnClickListener() { public void onClick(View v) { doItem(o.optString("k")); } }));
            b.addView(r, NUi.mt(k == 0 ? 10 : 0));
        }
        b.addView(NUi.btn(c, "Done", true, new View.OnClickListener() { public void onClick(View v) { finish(); } }), NUi.mt(18));
        sh.sheet(b);
    }

    void doItem(String k) {
        if (k.equals("sync")) { sh.a.js("window.__nact('', 'setupDone', {}, false)"); sh.closeSheet(); sh.openSettings(); return; }
        sh.a.js("window.__nact('', 'setupDo', {k:" + JSONObject.quote(k) + "}, false)");
        sh.closeSheet();
        root.postDelayed(new Runnable() { public void run() { setup(); } }, 1800);
    }

    void finish() {
        sh.a.js("window.__nact('', 'setupDone', {}, false)");
        try { sh.st.settings().put("setupDone", true); } catch (Exception ignored) { }
        sh.save(); sh.closeSheet();
        root.postDelayed(new Runnable() { public void run() { tourAsk(); } }, 500);
    }

    void tourAsk() {
        NForms F = new NForms(sh);
        LinearLayout b = F.sheetBody("Welcome", "Take a quick tour?", "Two minutes, with examples, through goals, habits, the calendar, timeline and more.");
        LinearLayout r = NUi.row(c);
        r.addView(NUi.btn(c, "Not now", false, new View.OnClickListener() { public void onClick(View v) {
            try { sh.st.settings().put("tourOffered", true); } catch (Exception ignored) { } sh.save(); sh.closeSheet(); NShell.toast("You can take the tour any time from Settings"); } }), NUi.lpw(0, NUi.dp(50), 1));
        LinearLayout.LayoutParams l = NUi.lpw(0, NUi.dp(50), 1); l.leftMargin = NUi.dp(10);
        r.addView(NUi.btn(c, "Start tour", true, new View.OnClickListener() { public void onClick(View v) {
            try { sh.st.settings().put("tourOffered", true); } catch (Exception ignored) { } sh.save(); sh.closeSheet(); NTour.start(sh); } }), l);
        b.addView(r, NUi.mt(20));
        sh.sheet(b);
    }
}
