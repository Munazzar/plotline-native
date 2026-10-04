package com.munazzar.plotline;

import android.content.Context;
import android.text.Html;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/* 2.0.8: sheets that used to open on the web layer, drawn natively from data the engine hands over. */
final class NEng {
    private NEng() { }

    interface Got { void on(JSONObject o); }

    /* call a hook that returns JSON; ignore failures quietly */
    static void hook(final NShell sh, String js, final Got g) {
        sh.a.jsRet(js, new android.webkit.ValueCallback<String>() { public void onReceiveValue(String v) {
            try { String u = NSheets.unq(v); if (u == null || u.isEmpty()) return; JSONObject o = new JSONObject(u); if (o.has("err")) { NCrash.log(sh.a, "hook", new Exception(o.optString("err"))); return; } g.on(o); }
            catch (Exception e) { NCrash.log(sh.a, "hook", e); }
        } });
    }

    static String q(String s) { return JSONObject.quote(s == null ? "" : s); }

    static View bar(Context c, int pct, int col) {
        LinearLayout tr = NUi.row(c); tr.setBackground(NUi.round(NTheme.line2, 4, 0));
        View f = new View(c); f.setBackground(NUi.round(col, 4, 0));
        tr.addView(f, new LinearLayout.LayoutParams(0, NUi.dp(6), Math.max(0, Math.min(100, pct))));
        tr.addView(new View(c), new LinearLayout.LayoutParams(0, NUi.dp(6), 100 - Math.max(0, Math.min(100, pct))));
        return tr;
    }

    static View imgOf(Context c, String data, int h) {
        try {
            int i = data.indexOf("base64,"); byte[] by = android.util.Base64.decode(data.substring(i + 7), android.util.Base64.DEFAULT);
            android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeByteArray(by, 0, by.length);
            ImageView iv = new ImageView(c); iv.setImageBitmap(bm); iv.setScaleType(ImageView.ScaleType.CENTER_CROP); iv.setClipToOutline(true);
            iv.setOutlineProvider(new android.view.ViewOutlineProvider() { public void getOutline(View v, android.graphics.Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), NUi.dp(18)); } });
            iv.setLayoutParams(NUi.lp(-1, NUi.dp(h)));
            return iv;
        } catch (Exception e) { return null; }
    }

    static CharSequence rich(String html) {
        String h = html.replaceAll("(?i)<mark>", "<span style=\"background-color:#FFD54F\">").replaceAll("(?i)</mark>", "</span>");
        CharSequence s = Html.fromHtml(h, Html.FROM_HTML_MODE_LEGACY);
        int n = s.length(); while (n > 0 && s.charAt(n - 1) == '\n') n--;
        return s.subSequence(0, n);
    }

    /* ---------- journal entry viewer (web viewEntry) ---------- */
    static void viewEntry(final NShell sh, String id) {
        hook(sh, "window.__nentry(" + q(id) + ")", new Got() { public void on(JSONObject o) { entrySheet(sh, o); } });
    }

    static void entrySheet(final NShell sh, final JSONObject o) {
        final Context c = sh.a;
        LinearLayout b = NUi.col(c);
        if (!o.optString("img").isEmpty()) { View iv = imgOf(c, o.optString("img"), 200); if (iv != null) { b.addView(iv); ((LinearLayout.LayoutParams) iv.getLayoutParams()).bottomMargin = NUi.dp(14); } }
        b.addView(NUi.label(c, o.optString("head"), NTheme.accent));
        TextView t = NUi.title(c, o.optString("title"), 28); t.setPadding(0, NUi.dp(8), 0, 0); b.addView(t);
        if (!o.optString("mood").isEmpty()) b.addView(NUi.body(c, o.optString("mood"), 14, NTheme.text, 600), NUi.mt(10));
        if (!o.optString("html").isEmpty()) { TextView x = NUi.text(c, "", 15.5f, NTheme.text); x.setText(rich(o.optString("html"))); x.setLineSpacing(0, 1.3f); x.setTextIsSelectable(true); b.addView(x, NUi.mt(12)); }
        final JSONObject g = o.optJSONObject("g"), h = o.optJSONObject("h");
        if (g != null) {
            int col = NTheme.areaCol(g.optString("area"));
            LinearLayout k = NUi.col(c); k.setBackground(NUi.round(NTheme.alpha(col, .12f), 18, NTheme.alpha(col, .35f))); k.setPadding(NUi.dp(14), NUi.dp(12), NUi.dp(14), NUi.dp(12));
            k.addView(NUi.label(c, g.optString("lbl"), NTheme.muted));
            k.addView(NUi.body(c, g.optString("title"), 16, NTheme.text, 700), NUi.mt(4));
            k.addView(bar(c, g.optInt("pct"), col), NUi.mt(8));
            k.addView(NUi.label(c, g.optString("sub"), NTheme.muted), NUi.mt(8));
            if (!g.optString("step").isEmpty()) k.addView(NUi.label(c, g.optString("step"), NTheme.muted), NUi.mt(6));
            b.addView(k, NUi.mt(14));
        }
        if (h != null) {
            int col = NTheme.areaCol(h.optString("area"));
            LinearLayout k = NUi.col(c); k.setBackground(NUi.round(NTheme.alpha(col, .12f), 18, NTheme.alpha(col, .35f))); k.setPadding(NUi.dp(14), NUi.dp(12), NUi.dp(14), NUi.dp(12));
            k.addView(NUi.label(c, "Habit", NTheme.muted));
            k.addView(NUi.body(c, h.optString("title"), 16, NTheme.text, 700), NUi.mt(4));
            k.addView(NUi.label(c, h.optString("sub"), NTheme.muted), NUi.mt(6));
            b.addView(k, NUi.mt(14));
        }
        final String eid = o.optString("id");
        NFlow f = new NFlow(c, 8, 8);
        f.addView(NUi.btn(c, "🧵 Thread", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); v.postDelayed(new Runnable() { public void run() { new NForms(sh).threadsFor("entry", eid); } }, 300); } }), new android.view.ViewGroup.MarginLayoutParams(-2, NUi.dp(44)));
        if (o.optBoolean("note")) f.addView(NUi.btn(c, "✎ Edit", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); final JSONObject e = sh.st.find("entries", eid); if (e != null) v.postDelayed(new Runnable() { public void run() { new NForms(sh).entry(e); } }, 300); } }), new android.view.ViewGroup.MarginLayoutParams(-2, NUi.dp(44)));
        f.addView(NUi.btn(c, "Close", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } }), new android.view.ViewGroup.MarginLayoutParams(-2, NUi.dp(44)));
        if (g != null) f.addView(NUi.btn(c, "Open goal", true, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); sh.route("goal/" + g.optString("id")); } }), new android.view.ViewGroup.MarginLayoutParams(-2, NUi.dp(44)));
        if (h != null) f.addView(NUi.btn(c, "Open habit", true, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); sh.route("habit/" + h.optString("id")); } }), new android.view.ViewGroup.MarginLayoutParams(-2, NUi.dp(44)));
        b.addView(f, NUi.mt(18));
        sh.sheet(b);
    }
    /* ---------- Ask: what to search (web askScope) ---------- */
    static void askScope(final NShell sh, final Runnable after) {
        hook(sh, "window.__nscope()", new Got() { public void on(JSONObject o) {
            final Context c = sh.a; NForms F = new NForms(sh);
            LinearLayout b = NUi.col(c);
            b.addView(NUi.title(c, "What to search", 28));
            TextView s = NUi.text(c, "Answers use notes from this time range. Goals and habits are always included.", 13.5f, NTheme.muted); s.setPadding(0, NUi.dp(6), 0, 0); b.addView(s);
            final String[] win = {String.valueOf(o.optInt("win"))}, goal = {o.optString("goal")};
            final Runnable apply = new Runnable() { public void run() { sh.a.js("window.__nscopeset(" + win[0] + "," + q(goal[0]) + ")"); if (after != null) sh.a.web.postDelayed(after, 120); } };
            b.addView(NForms.fieldLabel(c, "Time range"));
            b.addView(NForms.choice(c, new String[][]{{"7", "7 days"}, {"30", "30 days"}, {"90", "90 days"}, {"0", "All time"}}, win, apply));
            b.addView(NForms.fieldLabel(c, "Focus"));
            JSONArray gs = o.optJSONArray("goals"); List<String[]> op = new ArrayList<>(); op.add(new String[]{"", "Whole plan"});
            for (int i = 0; gs != null && i < gs.length(); i++) { JSONObject x = gs.optJSONObject(i); op.add(new String[]{x.optString("id"), x.optString("t")}); }
            b.addView(NForms.select(c, op.toArray(new String[0][]), goal, apply));
            b.addView(NForms.actions(c, NUi.btn(c, "Done", true, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } })));
            sh.sheet(b);
        } });
    }
    /* sheets the engine asks for (Bridge.nsheet) */
    static void engSheet(NShell sh, String kind, JSONObject o) {
        switch (kind) {
            case "acct": acct(sh, o); break;
            case "rxb": NShare.banner(sh, o); break;
            case "rxburst": NShare.burst(sh, o.optString("e", "💬")); break;
            default: NCrash.log(sh.a, "engSheet", new Exception("unknown " + kind));
        }
    }

    /* an option card, like the web .acct-o buttons */
    static View option(final Context c, String title, String sub, boolean on, View.OnClickListener l) {
        LinearLayout r = NUi.col(c);
        r.setBackground(NUi.ripple(NUi.round(on ? NTheme.alpha(NTheme.accent, .1f) : NTheme.surface, 16, on ? NTheme.alpha(NTheme.accent, .55f) : NTheme.line2), 16));
        r.setPadding(NUi.dp(16), NUi.dp(14), NUi.dp(16), NUi.dp(14));
        r.addView(NUi.body(c, title, 15.5f, NTheme.text, 700));
        TextView s = NUi.text(c, sub, 13, NTheme.muted); s.setLineSpacing(0, 1.18f); r.addView(s, NUi.mt(4));
        NUi.tap(r, l);
        return r;
    }

    /* ---------- Settings → Turn off sync (web syncOff) ---------- */
    static void syncOff(final NShell sh) {
        final Context c = sh.a; JSONObject sy = sh.st.settings().optJSONObject("sync"); String em = sy == null ? "" : sy.optString("email");
        LinearLayout b = NUi.col(c);
        b.addView(NUi.title(c, "Turn off sync?", 28));
        TextView s = NUi.text(c, "This device stops syncing with " + (em.isEmpty() ? "Google Drive" : em) + ". Your Drive copy is not touched.", 14.5f, NTheme.muted); s.setPadding(0, NUi.dp(8), 0, 0); s.setLineSpacing(0, 1.2f); b.addView(s);
        b.addView(option(c, "Keep my data on this device", "It stays tied to " + (em.isEmpty() ? "this account" : em) + ". If you later connect a different account, Plotline asks first.", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); sh.run("syncOffKeep", null); } }), NUi.mt(16));
        b.addView(option(c, "Remove it from this device", "Syncs one last time, then clears goals, habits and journal here. Sign in again to get them back.", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); sh.run("syncOffRemove", null); } }), NUi.mt(10));
        b.addView(NForms.actions(c, NUi.btn(c, "Cancel", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } })));
        sh.sheet(b);
    }

    /* ---------- a different Google account signed in (web acctChoice) ---------- */
    static void acct(final NShell sh, JSONObject o) {
        final Context c = sh.a; String old = o.optString("old"), em = o.optString("em"); int n = o.optInt("n"), h = o.optInt("h"), j = o.optInt("j");
        LinearLayout b = NUi.col(c);
        b.addView(NUi.label(c, "Different Google account", NTheme.accent));
        TextView t = NUi.title(c, "This device holds " + old + "’s plan", 28); t.setPadding(0, NUi.dp(10), 0, 0); b.addView(t);
        TextView s = NUi.text(c, "", 13.5f, NTheme.muted); s.setLineSpacing(0, 1.2f); s.setPadding(0, NUi.dp(8), 0, 0);
        s.setText(Html.fromHtml("You signed in as <b>" + android.text.TextUtils.htmlEncode(em) + "</b>. The " + n + " goal" + (n == 1 ? "" : "s") + ", " + h + " habit" + (h == 1 ? "" : "s") + " and " + j + " journal entr" + (j == 1 ? "y" : "ies") + " here belong to " + android.text.TextUtils.htmlEncode(old) + ". Nothing has synced yet.", Html.FROM_HTML_MODE_LEGACY));
        b.addView(s);
        b.addView(option(c, "Switch to " + em, "Recommended. " + old + "’s data is set aside safely on this device and comes back when you switch back. You get " + em + "’s own plan.", true, new View.OnClickListener() { public void onClick(View v) { sh.sheetLock = false; sh.closeSheet(); sh.run("acctSwitch", null); } }), NUi.mt(16));
        b.addView(option(c, "Copy this data into " + em, "Everything here is added to " + em + "’s Drive and merged with what’s already there. Only if both accounts are yours.", false, new View.OnClickListener() { public void onClick(View v) { sh.sheetLock = false; sh.closeSheet(); sh.run("acctMerge", null); } }), NUi.mt(10));
        b.addView(NForms.actions(c, NUi.btn(c, "Cancel · stay with " + old, false, new View.OnClickListener() { public void onClick(View v) { sh.sheetLock = false; sh.closeSheet(); sh.run("acctCancel", null); } })));
        sh.sheet(b); sh.sheetLock = true;
        sh.sheetBack = new Runnable() { public void run() { sh.sheetLock = false; sh.closeSheet(); sh.run("acctCancel", null); } };
    }

    /* ---------- Settings → Erase everything (web wipe), then the native welcome ---------- */
    static void wipe(final NShell sh) {
        NSheets.confirm(sh, "Erase everything?", "All goals, steps, moments and settings on this device will be deleted. Your Google Drive copy is not touched, so you can sync it back.", "Erase everything", true, new Runnable() { public void run() {
            sh.a.jsRet("window.__nwipe()", new android.webkit.ValueCallback<String>() { public void onReceiveValue(String v) {
                sh.root.postDelayed(new Runnable() { public void run() { sh.popAll(); sh.tabTo(0); NWelcome.run(sh); } }, 600);
            } });
        } });
    }
}
