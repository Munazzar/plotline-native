package com.munazzar.plotline;

import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

/* Small native sheets that used to open in the web layer: weekly review, PIN. */
final class NSheets {
    static String hash(String s) {
        try {
            byte[] d = java.security.MessageDigest.getInstance("SHA-256").digest(("waypoints:" + s).getBytes("UTF-8"));
            StringBuilder b = new StringBuilder(); for (byte x : d) b.append(String.format(java.util.Locale.US, "%02x", x)); return b.toString();
        } catch (Exception e) { return ""; }
    }

    static LinearLayout kv(android.content.Context c, String k, String v, boolean red) {
        LinearLayout r = NUi.row(c); r.setPadding(0, NUi.dp(12), 0, NUi.dp(12));

        r.addView(NUi.text(c, k, 14.5f, NTheme.muted), NUi.lpw(0, -2, 1));
        r.addView(NUi.body(c, v, 14.5f, red ? 0xFFFF7A7A : NTheme.text, 700));
        return r;
    }

    /* ---- weekly review ---- */
    static void weekly(final NShell sh) {
        sh.a.jsRet("(window.__nweek?window.__nweek():'')", new android.webkit.ValueCallback<String>() { public void onReceiveValue(String v) {
            try {
                String j = v; if (j != null && j.startsWith("\"")) j = new JSONObject("{\"x\":" + j + "}").getString("x");
                weeklySheet(sh, new JSONObject(j));
            } catch (Exception e) { NCrash.log(sh.a, "weekly", e); NShell.toast("Couldn’t open the weekly review"); }
        } });
    }

    static void weeklySheet(final NShell sh, JSONObject d) {
        final android.content.Context c = sh.a; NForms F = new NForms(sh);
        LinearLayout b = F.sheetBody(d.optString("a") + " – " + d.optString("b"), "Weekly review", null);
        b.addView(kv(c, "Steps finished", String.valueOf(d.optInt("done")), false));
        b.addView(kv(c, "Late", String.valueOf(d.optInt("late")), d.optInt("late") > 0));
        b.addView(kv(c, "Due in the next 7 days", String.valueOf(d.optInt("ahead")), false));
        if (!d.optString("habits").isEmpty()) b.addView(kv(c, "Habits kept", d.optString("habits"), false));
        JSONArray top = d.optJSONArray("top");
        if (top != null && top.length() > 0) { StringBuilder sb = new StringBuilder(); for (int i = 0; i < top.length(); i++) sb.append(i > 0 ? "\n" : "").append("✓ ").append(top.optString(i)); TextView t = NUi.text(c, sb.toString(), 13, NTheme.muted); t.setLineSpacing(0, 1.2f); b.addView(t, NUi.mt(10)); }
        final int late = d.optInt("late");
        if (late > 0) b.addView(NUi.btn(c, "Move " + late + " late step" + (late == 1 ? "" : "s") + " into this week", false, new View.OnClickListener() { public void onClick(View v) {
            sh.a.js("window.__nact('', 'pushLate', {}, false)"); sh.closeSheet(); NShell.toast(late + " late step" + (late == 1 ? "" : "s") + " moved into this week"); } }), NUi.mt(10));
        b.addView(NForms.fieldLabel(c, "What went well?")); final EditText a = NForms.input(c, "", "", true); a.setMinLines(3); b.addView(a);
        b.addView(NForms.fieldLabel(c, "What got in the way?")); final EditText bb = NForms.input(c, "", "", true); bb.setMinLines(3); b.addView(bb);
        b.addView(NForms.fieldLabel(c, "Focus for next week")); final EditText cc = NForms.input(c, "", "", true); cc.setMinLines(3); b.addView(cc);
        b.addView(NForms.actions(c,
            NUi.btn(c, "Later", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } }),
            NUi.btn(c, "Save to journal", true, new View.OnClickListener() { public void onClick(View v) {
                sh.a.js("window.__nweeksave(" + JSONObject.quote(a.getText().toString()) + "," + JSONObject.quote(bb.getText().toString()) + "," + JSONObject.quote(cc.getText().toString()) + ")");
                sh.closeSheet(); NShell.toast("Weekly review saved to your journal"); } })));
        sh.sheet(b);
    }

    /* ---- PIN ---- */
    static void setPin(final NShell sh) {
        final android.content.Context c = sh.a; NForms F = new NForms(sh);
        boolean has = !sh.st.settings().optString("pinHash").isEmpty();
        LinearLayout b = F.sheetBody("Privacy", has ? "Change PIN" : "Set a PIN", "4 to 8 digits. Until sync arrives, a forgotten PIN can only be cleared by erasing this device’s data, so keep a backup.");
        b.addView(NForms.fieldLabel(c, "New PIN")); final EditText a = NForms.input(c, "", "", false);
        a.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD); a.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(8)}); b.addView(a);
        b.addView(NForms.fieldLabel(c, "Repeat PIN")); final EditText r = NForms.input(c, "", "", false);
        r.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD); r.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(8)}); b.addView(r);
        b.addView(NForms.actions(c,
            NUi.btn(c, "Cancel", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } }),
            NUi.btn(c, "Save PIN", true, new View.OnClickListener() { public void onClick(View v) {
                String x = a.getText().toString();
                if (!x.matches("[0-9]{4,8}")) { NShell.toast("Use 4 to 8 digits"); return; }
                if (!x.equals(r.getText().toString())) { NShell.toast("The PINs don’t match"); return; }
                try { sh.st.settings().put("pinHash", hash(x)); } catch (Exception ignored) { }
                sh.save(); sh.closeSheet(); NShell.toast("PIN set"); } })));
        sh.sheet(b);
    }

    static void removePin(final NShell sh) {
        final android.content.Context c = sh.a; NForms F = new NForms(sh);
        LinearLayout b = F.sheetBody("Privacy", "Remove PIN?", "Anyone with this device will be able to open the app.");
        b.addView(NForms.actions(c,
            NUi.btn(c, "Cancel", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } }),
            NUi.btn(c, "Remove PIN", true, new View.OnClickListener() { public void onClick(View v) {
                try { sh.st.settings().put("pinHash", ""); } catch (Exception ignored) { }
                sh.save(); sh.closeSheet(); NShell.toast("PIN removed"); } })));
        sh.sheet(b);
    }

    /* ---- calendar export (2.0.7): the engine builds the .ics, native saves and opens it ---- */
    static void ics(final NShell sh, String kind, String gid, String sid) {
        final String js = "window.__nics(" + JSONObject.quote(kind) + "," + JSONObject.quote(gid == null ? "" : gid) + "," + JSONObject.quote(sid == null ? "" : sid) + ")";
        sh.a.jsRet(js, new android.webkit.ValueCallback<String>() { public void onReceiveValue(String v) {
            try {
                JSONObject o = new JSONObject(unq(v));
                if (o.has("err") || o.optString("text").isEmpty()) { NShell.toast(o.optString("msg").isEmpty() ? "Nothing to export" : o.optString("msg")); return; }
                new Bridge(sh.a).saveFile(o.optString("name"), "text/calendar", o.optString("text"), true);
                NShell.toast(o.optString("msg").isEmpty() ? "Open the file to add it to your calendar" : o.optString("msg"));
            } catch (Exception e) { NCrash.log(sh.a, "ics", e); NShell.toast("Couldn’t export"); }
        } });
    }

    /* ---- assistant settings ---- */
    static String unq(String v) { try { return v != null && v.startsWith("\"") ? new JSONObject("{\"x\":" + v + "}").getString("x") : v; } catch (Exception e) { return ""; } }

    static void assistant(final NShell sh) {
        sh.a.jsRet("(window.__nins?window.__nins():'')", new android.webkit.ValueCallback<String>() { public void onReceiveValue(String v) {
            try { assistantSheet(sh, new JSONObject(unq(v))); } catch (Exception e) { NCrash.log(sh.a, "assistant", e); NShell.toast("Couldn’t open assistant settings"); }
        } });
    }

    static void ins(NShell sh, String k, String v) { sh.a.js("window.__ninsset(" + JSONObject.quote(k) + "," + JSONObject.quote(v) + ")"); }

    static void sw(final NShell sh, LinearLayout b, String title, String sub, boolean on, final String key) {
        final android.content.Context c = sh.a;
        LinearLayout r = NUi.row(c); r.setPadding(0, NUi.dp(12), 0, NUi.dp(4));
        LinearLayout tx = NUi.col(c); tx.addView(NUi.body(c, title, 15.5f, NTheme.text, 600));
        TextView t = NUi.text(c, sub, 12.5f, NTheme.muted); t.setLineSpacing(0, 1.15f); tx.addView(t, NUi.mt(2));
        r.addView(tx, NUi.lpw(0, -2, 1));
        final NSettings.Sw w = new NSettings.Sw(c, on); r.addView(w, NUi.lp(NUi.dp(48), NUi.dp(30)));
        NUi.tap(r, new View.OnClickListener() { public void onClick(View v) { NUi.haptic(v); w.on = !w.on; w.invalidate(); ins(sh, key, w.on ? "1" : "0"); } });
        b.addView(r);
    }

    static void assistantSheet(final NShell sh, final JSONObject d) {
        final android.content.Context c = sh.a; NForms F = new NForms(sh);
        final JSONObject s = d.optJSONObject("s"); final String eng = s.optString("engine");
        LinearLayout b = F.sheetBody("Ask", "Assistant settings", "Plotline builds a short overview of your plan and finds the notes that match your question on this device. Only that goes to the engine you pick.");
        b.addView(NForms.fieldLabel(c, "Answer engine"));
        JSONObject en = d.optJSONObject("eng"); java.util.List<String[]> eo = new java.util.ArrayList<>();
        for (String k : new String[]{"local", "api", "copy"}) eo.add(new String[]{k, en.optString(k)});
        final String[] ec = {eng};
        b.addView(NForms.choice(c, eo.toArray(new String[0][]), ec, new Runnable() { public void run() { ins(sh, "engine", ec[0]); sh.closeSheet(); sh.pager.postDelayed(new Runnable() { public void run() { assistant(sh); } }, 350); } }));
        if (eng.equals("local")) {
            b.addView(NUi.text(c, "An open-source model runs " + (d.optBoolean("nat") ? "natively on this phone with llama.cpp" : "inside the app with llama.cpp (wllama)") + ". It downloads once, then works offline. Nothing leaves this device. Smaller models answer faster; bigger ones understand more.", 13, NTheme.muted), NUi.mt(12));
            b.addView(NForms.fieldLabel(c, "Model"));
            JSONArray ls = d.optJSONArray("llms"); java.util.List<String[]> lo = new java.util.ArrayList<>();
            for (int i = 0; i < ls.length(); i++) { JSONObject l = ls.optJSONObject(i); lo.add(new String[]{l.optString("k"), l.optString("n") + (l.optBoolean("have") ? " · on this phone" : "")}); }
            final String[] lc = {s.optString("llm")};
            b.addView(NForms.choice(c, lo.toArray(new String[0][]), lc, new Runnable() { public void run() { ins(sh, "llm", lc[0]); sh.closeSheet(); sh.pager.postDelayed(new Runnable() { public void run() { assistant(sh); } }, 350); } }));
            if (d.optBoolean("curHave")) b.addView(NUi.btn(c, "Delete this model from the phone", false, new View.OnClickListener() { public void onClick(View v) {
                sh.a.js("window.__nmdel&&window.__nmdel()"); sh.closeSheet(); NShell.toast("Model deleted"); } }), NUi.mt(12));
        }
        if (eng.equals("copy")) b.addView(NUi.text(c, "You copy a ready-made prompt into Claude, ChatGPT, Gemini or any assistant. Plotline itself sends nothing.", 13, NTheme.muted), NUi.mt(12));
        if (eng.equals("api")) {
            b.addView(NUi.text(c, "Any OpenAI-compatible server. Open-source models on your own computer with Ollama or LM Studio keep everything at home. The key stays on this device and is never synced or backed up.", 13, NTheme.muted), NUi.mt(12));
            JSONArray ap = d.optJSONArray("api"); NFlow fl = new NFlow(c, 8, 8);
            final EditText url = NForms.input(c, "http://localhost:11434/v1", s.optString("url"), false), model = NForms.input(c, "llama3.2", s.optString("model"), false), key = NForms.input(c, "", d.optString("key"), false);
            for (int i = 0; i < ap.length(); i++) { final JSONObject a = ap.optJSONObject(i);
                fl.addView(NUi.chip(c, a.optString("n"), a.optString("u").equals(s.optString("url")), new View.OnClickListener() { public void onClick(View v) { url.setText(a.optString("u")); model.setText(a.optString("m")); } }), new android.view.ViewGroup.MarginLayoutParams(-2, NUi.dp(40))); }
            b.addView(fl, NUi.mt(12));
            b.addView(NForms.fieldLabel(c, "Server address")); b.addView(url);
            b.addView(NForms.fieldLabel(c, "Model")); b.addView(model);
            b.addView(NForms.fieldLabel(c, "API key (not needed for Ollama)")); key.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD); b.addView(key);
            b.addView(NUi.text(c, "For Ollama on your computer, start it with OLLAMA_ORIGINS=" + d.optString("origin") + ". The phone app needs an https address (for example through Tailscale).", 12.5f, NTheme.muted), NUi.mt(10));
            final Runnable saveAll = new Runnable() { public void run() { ins(sh, "url", url.getText().toString()); ins(sh, "model", model.getText().toString()); ins(sh, "key", key.getText().toString()); } };
            b.addView(NUi.btn(c, "Test connection", false, new View.OnClickListener() { public void onClick(View v) {
                saveAll.run(); sh.a.js("window.__ninstest&&window.__ninstest()"); NShell.toast("Testing…"); poll(sh, 0); } }), NUi.mt(12));
            sh.settingsSave = saveAll;
        } else sh.settingsSave = null;
        if (eng.equals("local") && !d.optBoolean("nat")) sw(sh, b, "Use the GPU", "Faster on laptops. Off on phones by default, where it can freeze the device or garble answers.", d.optBoolean("gpu"), "gpu");
        sw(sh, b, "Smart search", "Finds notes by meaning, not just words. Downloads a 23 MB open-source model (all-MiniLM-L6-v2) once.", s.optBoolean("smart"), "smart");
        sw(sh, b, "Include private entries", "Off by default. Private journal entries stay out of every search and prompt.", s.optBoolean("priv"), "priv");
        b.addView(NUi.btn(c, "See what the AI knows about you", false, new View.OnClickListener() { public void onClick(View v) { if (sh.settingsSave != null) sh.settingsSave.run(); about(sh); } }), NUi.mt(14));
        b.addView(NForms.actions(c, NUi.btn(c, "Done", true, new View.OnClickListener() { public void onClick(View v) { if (sh.settingsSave != null) sh.settingsSave.run(); sh.settingsSave = null; sh.closeSheet(); } })));
        sh.sheet(b);
    }

    static void poll(final NShell sh, final int n) {
        sh.pager.postDelayed(new Runnable() { public void run() {
            sh.a.jsRet("window.__nitGet&&window.__nitGet()", new android.webkit.ValueCallback<String>() { public void onReceiveValue(String v) {
                String t = unq(v);
                if (t != null && !t.isEmpty() && !t.equals("null")) NShell.toast(t); else if (n < 8) poll(sh, n + 1);
            } });
        } }, 1200);
    }

    static void about(final NShell sh) {
        sh.a.jsRet("(window.__nabout?window.__nabout():'')", new android.webkit.ValueCallback<String>() { public void onReceiveValue(String v) {
            android.content.Context c = sh.a; NForms F = new NForms(sh);
            LinearLayout b = F.sheetBody("Ask", "What the AI knows about you", "This overview goes with every question, together with the notes that match it. It’s built on this device from your plan.");
            TextView t = NUi.text(c, unq(v), 12, NTheme.muted); t.setTypeface(android.graphics.Typeface.MONOSPACE); t.setTextIsSelectable(true); b.addView(t, NUi.mt(12));
            b.addView(NForms.actions(c, NUi.btn(c, "Done", true, new View.OnClickListener() { public void onClick(View x) { sh.closeSheet(); } })));
            sh.sheet(b);
        } });
    }

    /* ---- save the place I'm at ---- */
    static void placeNew(final NShell sh, final Runnable done) {
        NShell.toast("Finding where you are…");
        sh.a.js("window.__nplace&&window.__nplace()");
        pollPlace(sh, 0, done);
    }

    static void pollPlace(final NShell sh, final int n, final Runnable done) {
        sh.pager.postDelayed(new Runnable() { public void run() {
            sh.a.jsRet("window.__nphGet&&window.__nphGet()", new android.webkit.ValueCallback<String>() { public void onReceiveValue(String v) {
                String t = unq(v);
                if (t == null || t.isEmpty() || t.equals("null")) { if (n < 25) pollPlace(sh, n + 1, done); else NShell.toast("Couldn’t find your location. Try again outside or with Wi-Fi on"); return; }
                try {
                    JSONObject r = new JSONObject(t);
                    if (r.has("err")) { NShell.toast("perm".equals(r.optString("err")) ? "Allow location to save a place" : "Couldn’t find your location. Try again outside or with Wi-Fi on"); return; }
                    placeSheet(sh, r, done);
                } catch (Exception e) { NShell.toast("Couldn’t find your location"); }
            } });
        } }, 800);
    }

    static void placeSheet(final NShell sh, final JSONObject r, final Runnable done) {
        final android.content.Context c = sh.a; NForms F = new NForms(sh);
        LinearLayout b = F.sheetBody("Places", "Save this place", "Accurate to about " + Math.round(r.optDouble("acc", 50)) + " m.");
        b.addView(NForms.fieldLabel(c, "Name")); final EditText nm = NForms.input(c, "Gym, Work, Home, Mosque…", "", false); nm.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(30)}); b.addView(nm);
        b.addView(NForms.fieldLabel(c, "Emoji")); final EditText em = NForms.input(c, "📍", "", false); b.addView(em);
        b.addView(NForms.fieldLabel(c, "Size")); final String[] sz = {"150"};
        b.addView(NForms.choice(c, new String[][]{{"100", "Small · 100 m"}, {"150", "Normal · 150 m"}, {"300", "Large · 300 m"}}, sz, null));
        b.addView(NForms.actions(c,
            NUi.btn(c, "Cancel", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } }),
            NUi.btn(c, "Save place", true, new View.OnClickListener() { public void onClick(View v) {
                String n = nm.getText().toString().trim(); if (n.isEmpty()) { NShell.toast("Give it a name"); return; }
                sh.a.js("window.__nplacesave(" + JSONObject.quote(n) + "," + JSONObject.quote(em.getText().toString()) + "," + sz[0] + "," + r.optDouble("lat") + "," + r.optDouble("lng") + ")");
                sh.closeSheet(); NShell.toast("Place saved"); if (done != null) done.run(); } })));
        sh.sheet(b);
    }

    static void placeDel(final NShell sh, final String id, final Runnable done) {
        final android.content.Context c = sh.a; NForms F = new NForms(sh);
        LinearLayout b = F.sheetBody("Places", "Delete this place?", "Automations that use it stop working until you pick another place.");
        b.addView(NForms.actions(c,
            NUi.btn(c, "Cancel", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } }),
            NUi.btn(c, "Delete", true, new View.OnClickListener() { public void onClick(View v) { sh.a.js("window.__nplacedel(" + JSONObject.quote(id) + ")"); sh.closeSheet(); if (done != null) done.run(); } })));
        sh.sheet(b);
    }

    /* ---- share a goal, habit or step ---- */
    static void share(final NShell sh, final String kind, final String id, final String goalId) {
        NShell.toast("Getting ready to share…");
        sh.a.js("window.__nshprep(" + JSONObject.quote(kind) + "," + JSONObject.quote(id) + "," + JSONObject.quote(goalId == null ? "" : goalId) + ")");
        pollShare(sh, 0, new ShCb() { public void got(JSONObject r) {
            if (r.has("err")) { NShell.toast(r.optString("err")); return; }
            if (r.optInt("sync") == 1) { confirmSheet(sh, "Connect Google first", "Sharing uses your Google account, so turn on sync first.", "Set up sync", new Runnable() { public void run() { sh.push(new NSettings(sh, "account")); } }); return; }
            if (r.optInt("allow") == 1) { confirmSheet(sh, "One quick permission", "Google needs to confirm your email address to Plotline, so the people you invite know it’s you. Plotline sees nothing else.", "Allow", new Runnable() { public void run() { sh.run("shAllow", null); } }); return; }
            if (!r.optBoolean("told")) told(sh, r); else form(sh, r);
        } });
    }

    interface ShCb { void got(JSONObject r); }

    static void pollShare(final NShell sh, final int n, final ShCb cb) {
        sh.pager.postDelayed(new Runnable() { public void run() {
            sh.a.jsRet("window.__nshGet&&window.__nshGet()", new android.webkit.ValueCallback<String>() { public void onReceiveValue(String v) {
                String t = unq(v);
                if (t == null || t.isEmpty() || t.equals("null")) { if (n < 40) pollShare(sh, n + 1, cb); else NShell.toast("That took too long. Try again"); return; }
                try { cb.got(new JSONObject(t)); } catch (Exception e) { NShell.toast("Couldn’t share"); }
            } });
        } }, 700);
    }

    /* the web askConfirm, drawn natively: title, message, Cancel + the action (red when it's destructive) */
    static void confirm(final NShell sh, String title, String msg, String ok, boolean danger, final Runnable yes) {
        final android.content.Context c = sh.a;
        LinearLayout b = NUi.col(c);
        TextView t = NUi.title(c, android.text.Html.fromHtml(title, android.text.Html.FROM_HTML_MODE_LEGACY).toString(), 28); b.addView(t);
        TextView s = NUi.text(c, android.text.Html.fromHtml(msg, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim(), 14.5f, NTheme.muted); s.setLineSpacing(0, 1.22f); s.setPadding(0, NUi.dp(8), 0, 0); b.addView(s);
        TextView go = NUi.btn(c, android.text.Html.fromHtml(ok, android.text.Html.FROM_HTML_MODE_LEGACY).toString(), true, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); yes.run(); } });
        if (danger) { go.setBackground(NUi.ripple(NUi.round(NTheme.LATE, 16, 0), 16)); go.setTextColor(0xFFFFFFFF); }
        b.addView(NForms.actions(c, NUi.btn(c, "Cancel", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } }), go));
        sh.sheet(b);
    }

    static void confirmSheet(final NShell sh, String title, String text, String go, final Runnable ok) {
        final android.content.Context c = sh.a; NForms F = new NForms(sh);
        LinearLayout b = F.sheetBody("Share", title, text);
        b.addView(NForms.actions(c, NUi.btn(c, "Cancel", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } }),
            NUi.btn(c, go, true, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); ok.run(); } })));
        sh.sheet(b);
    }

    static void told(final NShell sh, final JSONObject r) {
        final android.content.Context c = sh.a; NForms F = new NForms(sh);
        LinearLayout b = F.sheetBody("Before you share", "What happens when you share", null);
        TextView t = NUi.text(c, r.optString("facts"), 13.5f, NTheme.text); t.setLineSpacing(0, 1.25f); b.addView(t, NUi.mt(12));
        b.addView(NForms.actions(c, NUi.btn(c, "Cancel", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } }),
            NUi.btn(c, "I understand, continue", true, new View.OnClickListener() { public void onClick(View v) { sh.a.js("window.__nshtold()"); sh.closeSheet(); sh.pager.postDelayed(new Runnable() { public void run() { form(sh, r); } }, 300); } })));
        sh.sheet(b);
    }

    static void form(final NShell sh, final JSONObject r) {
        final android.content.Context c = sh.a; NForms F = new NForms(sh);
        final boolean goal = "goal".equals(r.optString("kind"));
        LinearLayout b = F.sheetBody("Share", r.optString("title"), null);
        b.addView(NForms.fieldLabel(c, "How"));
        final String[] mode = {"together"};
        JSONArray ms = r.optJSONArray("modes"); final LinearLayout mb = NUi.col(c);
        final Runnable[] paint = new Runnable[1];
        final JSONArray msf = ms;
        paint[0] = new Runnable() { public void run() {
            mb.removeAllViews();
            for (int i = 0; i < msf.length(); i++) { final JSONObject m = msf.optJSONObject(i); boolean on = m.optString("k").equals(mode[0]);
                LinearLayout row = NUi.row(c); row.setPadding(NUi.dp(12), NUi.dp(10), NUi.dp(12), NUi.dp(10)); row.setBackground(NUi.round(on ? NTheme.alpha(NTheme.accent, .12f) : NTheme.surface, 16, on ? NTheme.accent : NTheme.line));
                row.addView(NUi.text(c, m.optString("e"), 20, NTheme.text), NUi.lp(NUi.dp(36), -2));
                LinearLayout tx = NUi.col(c); tx.addView(NUi.body(c, m.optString("n"), 15, NTheme.text, 700)); tx.addView(NUi.text(c, m.optString("x"), 12.5f, NTheme.muted), NUi.mt(2));
                row.addView(tx, NUi.lpw(0, -2, 1));
                NUi.tap(row, new View.OnClickListener() { public void onClick(View v) { mode[0] = m.optString("k"); paint[0].run(); } });
                mb.addView(row, NUi.mt(i == 0 ? 0 : 8)); }
        } };
        paint[0].run(); b.addView(mb);
        b.addView(NForms.fieldLabel(c, "With · their Google email"));
        final EditText em = NForms.input(c, "sara@gmail.com", "", false); em.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS); b.addView(em);
        b.addView(NUi.text(c, "The email they use in Plotline. Add more than one with commas.", 12.5f, NTheme.muted), NUi.mt(6));
        final java.util.LinkedHashMap<String, EditText> asg = new java.util.LinkedHashMap<>();
        JSONArray ss = r.optJSONArray("steps");
        if (goal && ss != null && ss.length() > 0) {
            b.addView(NForms.fieldLabel(c, "Assign steps · optional"));
            b.addView(NUi.text(c, "Assigned steps show up for that person to do. Leave a step unassigned to do it yourself.", 12.5f, NTheme.muted));
            for (int i = 0; i < ss.length(); i++) { JSONObject s = ss.optJSONObject(i);
                b.addView(NUi.text(c, s.optString("t"), 13.5f, NTheme.text), NUi.mt(10));
                EditText e = NForms.input(c, "me", "", false); e.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS); asg.put(s.optString("id"), e); b.addView(e, NUi.mt(4)); }
        }
        LinearLayout lk = NUi.col(c); lk.setBackground(NUi.round(NTheme.surface, 16, NTheme.line)); lk.setPadding(NUi.dp(12), NUi.dp(10), NUi.dp(12), NUi.dp(10));
        lk.addView(NUi.body(c, "🔒  End-to-end encrypted", 14, NTheme.text, 700));
        lk.addView(NUi.text(c, "They’ll see the title" + (goal ? ", the steps" : "") + ", the mode, and your name, progress, streak and check-in days for this one item, live. Nothing else from your plan. Only the people in it can read it.", 12.5f, NTheme.muted), NUi.mt(4));
        b.addView(lk, NUi.mt(16));
        b.addView(NForms.actions(c, NUi.btn(c, "Cancel", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } }),
            NUi.btn(c, "Share", true, new View.OnClickListener() { public void onClick(View v) {
                JSONObject as = new JSONObject(); try { for (java.util.Map.Entry<String, EditText> en : asg.entrySet()) as.put(en.getKey(), en.getValue().getText().toString().trim()); } catch (Exception ignored) { }
                sh.hideKeyboard(); NShell.toast("Sharing…");
                sh.a.js("window.__nshgo(" + JSONObject.quote(mode[0]) + "," + JSONObject.quote(em.getText().toString()) + "," + as + ")");
                sh.closeSheet();
                pollShare(sh, 0, new ShCb() { public void got(JSONObject x) {
                    if (x.has("err")) { NShell.toast(x.optString("err")); return; }
                    sent(sh, x); } });
            } })));
        sh.sheet(b);
    }

    static void sent(final NShell sh, final JSONObject x) {
        final android.content.Context c = sh.a; NForms F = new NForms(sh);
        LinearLayout b = F.sheetBody("Shared", "Invite sent to " + x.optString("who"), "“" + x.optString("title") + "” shows up in their Plotline, with a notification on their Android phone. Nothing else to do.");
        if (x.optInt("later") > 0) {
            LinearLayout lk = NUi.col(c); lk.setBackground(NUi.round(NTheme.surface, 16, NTheme.line)); lk.setPadding(NUi.dp(12), NUi.dp(10), NUi.dp(12), NUi.dp(10));
            lk.addView(NUi.body(c, x.optString("laterWho") + (x.optInt("later") == 1 ? " isn’t" : " aren’t") + " on Plotline yet", 14, NTheme.text, 700));
            lk.addView(NUi.text(c, "Send them the app. Once they sign in, your Plotline unlocks the invite for them automatically (usually within the hour).", 12.5f, NTheme.muted), NUi.mt(4));
            b.addView(lk, NUi.mt(14));
        }
        View done = NUi.btn(c, "Done", true, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } });
        if (x.optInt("later") > 0) b.addView(NForms.actions(c, NUi.btn(c, "Send them Plotline", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); try { new Bridge(sh.a).share("Plotline", x.optString("msg")); } catch (Exception ignored) { } } }), done));
        else b.addView(NForms.actions(c, done));
        sh.sheet(b);
    }
}
