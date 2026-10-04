package com.munazzar.plotline;

import android.content.Intent;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

/* Plan with AI: describe what you want (or what changed), write the plan here or paste it from any AI, check it,
   then create. The planner, checks and import are the web's own (plangen.js); this screen draws them. */
final class NAiPlan extends NPage {
    JSONObject d; String sig = ""; boolean polling, specOpen;
    String text = "", change = "", reply = "";
    boolean loaded;

    String tab0;
    NAiPlan(NShell sh) { this(sh, "new"); }
    NAiPlan(NShell sh, String tab) { super(sh); back = true; tab0 = tab; }

    @Override void onShow() { super.onShow(); startPoll(); }

    void startPoll() { if (polling) return; polling = true; if (tab0 != null) { sh.a.js("window.__naitab&&window.__naitab(" + JSONObject.quote(tab0) + ")"); tab0 = null; } tick(); }

    boolean active() { return !sh.stack.isEmpty() && sh.stack.get(sh.stack.size() - 1) == this; }

    void tick() {
        if (!active()) { polling = false; return; }
        sh.a.jsRet("window.__nai&&window.__nai()", new android.webkit.ValueCallback<String>() { @Override public void onReceiveValue(String v) {
            long next = 1500;
            try {
                if (v != null) {
                    JSONObject o = new JSONObject(v);
                    if (!o.has("err")) {
                        if (!loaded) { loaded = true; text = o.optString("text"); change = o.optString("change"); reply = o.optString("reply"); }
                        JSONObject k = new JSONObject(o.toString()); k.remove("text"); k.remove("change"); k.remove("reply");
                        String ns = k.toString();
                        if (!ns.equals(sig)) { sig = ns; d = o; refresh(); }
                        if (o.optBoolean("sheet") && !sh.classic) sh.showClassicLayer(false);
                        next = o.optBoolean("busy") ? 400 : 1500;
                    }
                }
            } catch (Exception ignored) { }
            if (polling) frame.postDelayed(new Runnable() { public void run() { tick(); } }, next);
        } });
    }

    void push() { sh.a.js("window.__naiset(" + JSONObject.quote(text) + "," + JSONObject.quote(change) + "," + JSONObject.quote(reply) + ")"); }
    void run(String js) { push(); sh.a.js(js); sig = ""; }

    void copy(String kind, final String ok) {
        push();
        sh.a.jsRet("window.__naiprompt(" + JSONObject.quote(kind) + ")", new android.webkit.ValueCallback<String>() { @Override public void onReceiveValue(String v) {
            try { String t = v == null ? "" : new JSONObject("{\"x\":" + v + "}").getString("x");
                if (t.isEmpty()) { NShell.toast(need()); return; }
                new Bridge(sh.a).copy(t); NShell.toast(ok); } catch (Exception ignored) { }
        } });
    }
    String need() { return d != null && "change".equals(d.optString("tab")) ? "Describe what changed first" : "Write a few lines about what you want first"; }

    LinearLayout stepCard(String num, boolean on) {
        LinearLayout r = NUi.row(c); r.setAlpha(on ? 1f : .92f); r.setGravity(Gravity.TOP);
        TextView nm = NUi.body(c, num, 14, on ? NTheme.onAccent : NTheme.muted, 800); nm.setGravity(Gravity.CENTER);
        nm.setBackground(NUi.oval(on ? NTheme.accent : NTheme.surface2, on ? NTheme.accent : NTheme.line2, 1));
        r.addView(nm, NUi.lp(NUi.dp(30), NUi.dp(30)));
        LinearLayout col = NUi.col(c); col.setPadding(NUi.dp(14), 0, 0, 0);
        r.addView(col, NUi.lpw(0, -2, 1));
        body.addView(r, NUi.mt(22));
        return col;
    }

    EditText area(LinearLayout p, String hint, String val, final int which, int lines) {
        final EditText e = NForms.input(c, hint, val, true); e.setMinLines(lines);
        e.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int cc) { }
            public void onTextChanged(CharSequence s, int a, int b, int cc) { }
            public void afterTextChanged(Editable s) { if (which == 0) text = s.toString(); else if (which == 1) change = s.toString(); else reply = s.toString(); }
        });
        p.addView(e, NUi.mt(10));
        return e;
    }

    View link(final String label, final String url) {
        return NUi.btn(c, label + " ↗", false, new View.OnClickListener() { public void onClick(View v) { try { sh.a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception ignored) { } } });
    }

    @Override void build() {
        if (d == null) { header("Plan with AI"); add(muted("Loading…"), 16); return; }
        final boolean ch = "change".equals(d.optString("tab")), has = d.optBoolean("has"), busy = d.optBoolean("busy");
        final String eng = d.optString("eng");
        header("Plan with AI");
        TextView sub = NUi.label(c, eng.isEmpty() ? "Works with ChatGPT, Claude, Gemini or any assistant" : "On this device, or with any AI assistant", NTheme.muted); sub.setPadding(0, NUi.dp(6), 0, 0); body.addView(sub);
        add(NBits.seg(c, new String[][]{{"new", "New plan"}, {"change", "Change of plans"}}, d.optString("tab"), new NBits.Pick() { public void on(String k) { push(); reply = ""; sh.a.js("window.__naitab(" + JSONObject.quote(k) + ")"); sig = ""; } }), 16);

        /* 1 */
        LinearLayout s1 = stepCard("1", !has);
        s1.addView(NUi.body(c, ch ? "What changed?" : "Describe what you want", 18, NTheme.text, 700));
        s1.addView(muted(ch ? "Your whole plan goes along, with every goal and step id. Completed steps are locked. The AI can only rework, reschedule or add what’s still open." : "Messy is fine: hopes, worries, things you keep putting off. You don’t need to know your goals yet."), NUi.mt(4));
        EditText t1 = area(s1, ch ? "e.g. I started a job with long weekdays, so training moves to weekends. We’re moving in March, so the family days pause for a month." : "e.g. I want to be healthier, maybe run a race. Save for a house in 3 years. Spend more real time with my kids. Learn to cook properly.", ch ? change : text, ch ? 1 : 0, 5);
        NVoice.attach(c, t1);
        if (eng.isEmpty()) {
            TextView t = NUi.text(c, "Want it written right here? Pick an on-device model in assistant settings.", 13.5f, NTheme.muted); t.setPadding(0, NUi.dp(12), 0, 0);
            NUi.tap(t, new View.OnClickListener() { public void onClick(View v) { NSheets.assistant(sh); } }); s1.addView(t);
        } else {
            LinearLayout g = NUi.col(c); g.setBackground(NUi.round(NTheme.surface, 20, NTheme.line)); g.setPadding(NUi.dp(14), NUi.dp(12), NUi.dp(14), NUi.dp(12));
            g.addView(NUi.body(c, busy ? "Writing your plan…" : ch ? "Rework my plan here" : "Write the plan here", 15.5f, NTheme.text, 700));
            g.addView(NUi.text(c, d.optString("engName") + (eng.equals("api") ? "" : " · nothing leaves this device"), 12.5f, NTheme.muted), NUi.mt(2));
            if (busy) {
                g.addView(NUi.text(c, d.optString("prog"), 13, NTheme.muted), NUi.mt(8));
                g.addView(btns(NUi.btn(c, "Stop", false, new View.OnClickListener() { public void onClick(View v) { sh.a.js("ACT.aiGenStop()"); } })), NUi.mt(8));
            } else {
                if (!d.optString("aerr").isEmpty()) g.addView(NUi.text(c, d.optString("aerr"), 13, 0xFFFF7A7A), NUi.mt(8));
                g.addView(btns(NUi.btn(c, ch ? "✦  Rework my plan" : "✦  Create my plan", true, new View.OnClickListener() { public void onClick(View v) {
                    String t = ch ? change : text; if (t.trim().isEmpty()) { NShell.toast(need()); return; }
                    if (ch && !d.optBoolean("hasGoals")) { NShell.toast("No goals yet. Start with New plan"); return; }
                    sh.hideKeyboard(); run("ACT.aiGen()"); } })), NUi.mt(10));
            }
            s1.addView(g, NUi.mt(14));
        }
        TextView or = NUi.text(c, eng.isEmpty() ? "" : "Or copy it into any AI:", 12.5f, NTheme.muted); if (!eng.isEmpty()) { or.setPadding(0, NUi.dp(14), 0, 0); s1.addView(or); }
        s1.addView(btns(NUi.btn(c, "Copy prompt", eng.isEmpty(), new View.OnClickListener() { public void onClick(View v) { copy("prompt", "Prompt copied. Paste it into any AI"); } }),
            link("Claude", "https://claude.ai/new"), link("ChatGPT", "https://chatgpt.com/"), link("Gemini", "https://gemini.google.com/app")), NUi.mt(eng.isEmpty() ? 14 : 8));

        /* 2 */
        LinearLayout s2 = stepCard("2", true);
        s2.addView(NUi.body(c, "Paste the whole reply", 18, NTheme.text, 700));
        s2.addView(muted("The app finds the plan and checks every field against the format."), NUi.mt(4));
        area(s2, "Paste the AI’s full answer here", reply, 2, 4);
        s2.addView(btns(NUi.btn(c, "Check plan", !has, new View.OnClickListener() { public void onClick(View v) { sh.hideKeyboard(); run("ACT.parseAI()"); } })), NUi.mt(12));
        JSONArray errs = d.optJSONArray("errs");
        if (errs != null && errs.length() > 0) {
            LinearLayout eb = NUi.col(c); eb.setBackground(NUi.round(NTheme.alpha(0xFFFF7A7A, .10f), 18, NTheme.alpha(0xFFFF7A7A, .45f))); eb.setPadding(NUi.dp(14), NUi.dp(12), NUi.dp(14), NUi.dp(12));
            eb.addView(NUi.body(c, "The plan doesn’t match the format yet", 15, NTheme.text, 700));
            for (int i = 0; i < errs.length(); i++) { TextView e = NUi.text(c, "•  " + errs.optString(i), 13, NTheme.text); e.setPadding(0, NUi.dp(4), 0, 0); eb.addView(e); }
            eb.addView(btns(NUi.btn(c, "Copy fix-it message", true, new View.OnClickListener() { public void onClick(View v) { copy("fix", "Fix-it message copied. Paste it into the same AI chat"); } })), NUi.mt(10));
            eb.addView(NUi.text(c, "Paste it into the same AI chat, then paste the new reply here.", 12.5f, NTheme.muted), NUi.mt(6));
            s2.addView(eb, NUi.mt(14));
        }

        /* 3 */
        LinearLayout s3 = stepCard("3", has);
        s3.addView(NUi.body(c, ch ? "Review the changes" : "Review and create", 18, NTheme.text, 700));
        if (!has) s3.addView(muted(ch ? "You’ll see exactly what gets added, rescheduled or removed before anything changes. Completed steps are never touched." : "Your goals show up here to check before anything is created."), NUi.mt(4));
        else review(s3);

        /* format */
        LinearLayout f = NUi.col(c); f.setBackground(NUi.card(20)); f.setPadding(NUi.dp(16), NUi.dp(14), NUi.dp(16), NUi.dp(14));
        LinearLayout fh = NUi.row(c); fh.addView(NUi.body(c, "The exact format the AI must follow", 15, NTheme.text, 700), NUi.lpw(0, -2, 1)); fh.addView(NUi.icon(c, specOpen ? "up" : "down", 18, NTheme.muted));
        NUi.tap(fh, new View.OnClickListener() { public void onClick(View v) { specOpen = !specOpen; refresh(); } }); f.addView(fh);
        if (specOpen) {
            TextView sp = NUi.text(c, d.optString("spec"), 11.5f, NTheme.muted); sp.setTypeface(android.graphics.Typeface.MONOSPACE); sp.setTextIsSelectable(true); sp.setPadding(0, NUi.dp(10), 0, 0); f.addView(sp);
            f.addView(btns(NUi.btn(c, "Copy format", false, new View.OnClickListener() { public void onClick(View v) { copy("spec", "Format copied"); } })), NUi.mt(10));
        }
        add(f, 26);
    }

    LinearLayout btns(View... bs) {
        NFlow fl = new NFlow(c, 8, 8); for (View b : bs) fl.addView(b, new android.view.ViewGroup.MarginLayoutParams(-2, NUi.dp(46)));
        LinearLayout w = NUi.col(c); w.addView(fl); return w;
    }

    void review(LinearLayout p) {
        final boolean up = d.optBoolean("up");
        JSONObject m = d.optJSONObject("main");
        if (m != null) mainCard(p, m);
        JSONArray rows = d.optJSONArray("rows");
        for (int i = 0; rows != null && i < rows.length(); i++) {
            final JSONObject x = rows.optJSONObject(i); if (x == null) continue;
            final int idx = x.optInt("i"); final boolean keep = x.optBoolean("keep"), sel = x.optBoolean("sel");
            int col = NTheme.areaCol(x.optString("area"));
            LinearLayout r = NUi.row(c); r.setGravity(Gravity.TOP); r.setBackground(NUi.round(sel ? NTheme.alpha(col, .12f) : NTheme.surface, 18, sel ? NTheme.alpha(col, .55f) : NTheme.line));
            r.setPadding(NUi.dp(12), NUi.dp(12), NUi.dp(12), NUi.dp(12)); r.setAlpha(keep ? .6f : 1f);
            LinearLayout.LayoutParams rl = NUi.mt(10); rl.leftMargin = NUi.dp(Math.min(x.optInt("dp"), 3) * 14); 
            TextView ck = NUi.body(c, sel ? "✓" : "", 14, NTheme.onAccent, 800); ck.setGravity(Gravity.CENTER);
            ck.setBackground(NUi.round(sel ? col : 0, 7, sel ? col : NTheme.line2));
            r.addView(ck, NUi.lp(NUi.dp(24), NUi.dp(24)));
            LinearLayout tx = NUi.col(c); tx.setPadding(NUi.dp(12), 0, 0, 0); r.addView(tx, NUi.lpw(0, -2, 1));
            final boolean mainOn = m != null;
            if (mainOn && !x.optBoolean("rm") && !keep) {
                final EditText ti = NForms.input(c, "Goal title", x.optString("t"), false); ti.setPadding(0, NUi.dp(2), 0, NUi.dp(2)); ti.setBackground(null);
                ti.addTextChangedListener(new TextWatcher() { public void beforeTextChanged(CharSequence s, int a, int b, int cc) { } public void onTextChanged(CharSequence s, int a, int b, int cc) { } public void afterTextChanged(Editable s) { sh.a.js("window.__naititle(" + idx + "," + JSONObject.quote(s.toString()) + ")"); } });
                tx.addView(ti);
            } else tx.addView(NUi.body(c, x.optString("t"), 15.5f, NTheme.text, 700));
            TextView bd = NUi.body(c, x.optString("badge"), 11, col, 800); bd.setPadding(0, NUi.dp(4), 0, 0); tx.addView(bd);
            if (!x.optString("par").isEmpty()) tx.addView(NUi.text(c, "↳ under " + x.optString("par"), 12, NTheme.muted), NUi.mt(3));
            String act = x.optString("act");
            if ("add".equals(act) || "update".equals(act)) {
                String meta = x.optString("az") + "  ·  " + x.optString("hz") + (x.optString("dt").isEmpty() ? "" : "  ·  " + x.optString("dt"));
                tx.addView(NUi.text(c, meta, 12, NTheme.muted), NUi.mt(4));
            }
            if (!x.optString("det").isEmpty()) tx.addView(NUi.text(c, x.optString("det"), 12, NTheme.muted), NUi.mt(4));
            if (x.optBoolean("rm")) tx.addView(NUi.text(c, "This goal will be deleted. Its history stays in your journal.", 12.5f, NTheme.muted), NUi.mt(6));
            JSONArray ss = x.optJSONArray("steps");
            for (int j = 0; ss != null && j < ss.length(); j++) {
                JSONObject s = ss.optJSONObject(j); if (s == null) continue;
                String line = (j + 1) + ". " + s.optString("t") + (s.optString("d").isEmpty() ? "" : " · " + s.optString("d")) + (s.optBoolean("b") ? " 🔔" : "") + (s.optBoolean("nw") ? "  NEW" : "");
                TextView sl = NUi.text(c, line, 13, NTheme.text); sl.setPadding(0, NUi.dp(4), 0, 0); tx.addView(sl);
            }
            if (!keep) NUi.tap(r, new View.OnClickListener() { public void onClick(View v) { sh.a.js("window.__naitoggle(" + idx + ")"); sig = ""; } });
            p.addView(r, rl);
        }
        int n = d.optInt("nsel");
        String lbl = up ? "Apply " + n + " change" + (n == 1 ? "" : "s") : m != null ? (n > 0 ? "Create main goal + " + n + " sub-goal" + (n == 1 ? "" : "s") : "Create main goal") : n > 0 ? "Create goal" + (n == 1 ? "" : "s") : "Create goal";
        if (!up && m == null && n > 1) lbl = "Create " + n + " goals";
        final boolean can = n > 0 || m != null;
        View go = NUi.btn(c, lbl, true, new View.OnClickListener() { public void onClick(View v) {
            if (!can) return; sh.hideKeyboard(); if (mainTarget != null) sh.a.js("window.__naimain('target'," + JSONObject.quote(mainTarget[0]) + ")"); sh.a.js("ACT.doImport()");
            frame.postDelayed(new Runnable() { public void run() { if (!sh.stack.isEmpty() && sh.stack.get(sh.stack.size() - 1) == NAiPlan.this) sh.pop(); } }, 350);
        } });
        go.setAlpha(can ? 1f : .4f);
        p.addView(btns(go, NUi.btn(c, "Discard", false, new View.OnClickListener() { public void onClick(View v) { reply = ""; sh.a.js("ACT.clearAI()"); sig = ""; } })), NUi.mt(14));
    }

    void mainCard(LinearLayout p, JSONObject m) {
        int col = NTheme.areaCol(NTheme.areaKey(m.optString("area")));
        LinearLayout k = NUi.col(c); k.setBackground(NUi.round(NTheme.alpha(col, .12f), 22, NTheme.alpha(col, .5f))); k.setPadding(NUi.dp(14), NUi.dp(14), NUi.dp(14), NUi.dp(14));
        k.addView(NUi.label(c, "Main goal · " + (m.optBoolean("wrap") ? "the AI sent separate goals, so they’ll go under this one. Rename it if you like" : "everything below sits under it"), NTheme.muted));
        k.addView(fieldEdit("title", "Name your main goal", m.optString("title"), false, 80), NUi.mt(8));
        k.addView(fieldEdit("why", "Why it matters · optional", m.optString("why"), true, 400), NUi.mt(6));
        final String[] ar = {m.optString("area")};
        java.util.List<String[]> ao = new java.util.ArrayList<>(); JSONArray aa = d.optJSONArray("areas");
        for (int i = 0; aa != null && i < aa.length(); i++) ao.add(new String[]{aa.optString(i), aa.optString(i)});
        k.addView(NForms.choice(c, ao.toArray(new String[0][]), ar, new Runnable() { public void run() { sh.a.js("window.__naimain('area'," + JSONObject.quote(ar[0]) + ")"); sig = ""; } }), NUi.mt(10));
        final String[] hz = {m.optString("horizon")};
        k.addView(NForms.choice(c, NGoalForm.HZ, hz, new Runnable() { public void run() { sh.a.js("window.__naimain('horizon'," + JSONObject.quote(hz[0]) + ")"); sig = ""; } }), NUi.mt(8));
        final String[] tg = {m.optString("target")};
        k.addView(NForms.datePick(c, tg, "Target date · optional"), NUi.mt(8));
        LinearLayout row = NUi.row(c);
        row.addView(NUi.btn(c, m.optString("img").isEmpty() ? "Add a cover image" : "Change cover", false, new View.OnClickListener() { public void onClick(View v) {
            sh.a.imgCb = new MainActivity.ImgCb() { public void got(String url) { sh.a.js("window.__naimain('img'," + JSONObject.quote(url) + ")"); sig = ""; } };
            sh.a.pickImage("goalcover"); } }));
        if (!m.optString("img").isEmpty()) { LinearLayout.LayoutParams l = NUi.lp(-2, -2); l.leftMargin = NUi.dp(8); row.addView(NUi.btn(c, "Remove", false, new View.OnClickListener() { public void onClick(View v) { sh.a.js("window.__naimain('img','')"); sig = ""; } }), l); }
        k.addView(row, NUi.mt(10));
        JSONArray ss = m.optJSONArray("steps");
        for (int j = 0; ss != null && j < ss.length(); j++) { JSONObject s = ss.optJSONObject(j); TextView sl = NUi.text(c, (j + 1) + ". " + s.optString("t") + (s.optString("d").isEmpty() ? "" : " · " + s.optString("d")), 13, NTheme.text); sl.setPadding(0, NUi.dp(4), 0, 0); k.addView(sl); }
        p.addView(k, NUi.mt(12));
        mainTarget = tg;
    }
    String[] mainTarget;

    EditText fieldEdit(final String f, String hint, String val, boolean multi, int max) {
        EditText e = NForms.input(c, hint, val, multi);
        e.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(max)});
        e.addTextChangedListener(new TextWatcher() { public void beforeTextChanged(CharSequence s, int a, int b, int cc) { } public void onTextChanged(CharSequence s, int a, int b, int cc) { } public void afterTextChanged(Editable s) { sh.a.js("window.__naimain(" + JSONObject.quote(f) + "," + JSONObject.quote(s.toString()) + ")"); } });
        return e;
    }
}
