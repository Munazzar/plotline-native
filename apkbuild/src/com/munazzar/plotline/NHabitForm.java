package com.munazzar.plotline;

import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.Gravity;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/* The web's full habit form: build / break / routine, with every field, plus the template picker. */
final class NHabitForm {
    static final String[][] HP = {{"morning", "Morning"}, {"afternoon", "Afternoon"}, {"evening", "Evening"}, {"any", "Anytime"}};

    static JSONArray tpls() { try { return new JSONArray(NHabitTpl.JSON); } catch (Exception e) { return new JSONArray(); } }

    static JSONArray parseSteps(String txt, JSONArray old) {
        JSONArray out = new JSONArray(); Pattern rx = Pattern.compile("^(.*?)[\\s·:,\\-–—]+(\\d{1,3})\\s*(m|min|mins|minutes)?\\.?$", Pattern.CASE_INSENSITIVE);
        int n = 0;
        for (String line : txt.split("\n")) {
            String l = line.trim(); if (l.isEmpty() || n >= 30) continue; n++;
            Matcher m = rx.matcher(l); String title = l; int min = 0;
            if (m.find()) { String t = m.group(1).trim(); if (!t.isEmpty()) title = t; min = Math.min(240, Integer.parseInt(m.group(2))); }
            String id = null; for (int i = 0; old != null && i < old.length(); i++) { JSONObject o = old.optJSONObject(i); if (o != null && o.optString("title").equals(title)) { id = o.optString("id"); break; } }
            try { JSONObject s = new JSONObject(); s.put("id", id != null ? id : NStore.uid()); s.put("title", title); s.put("min", min); out.put(s); } catch (Exception ignored) { }
        }
        return out;
    }
    static String stepsText(JSONArray st) { StringBuilder b = new StringBuilder(); for (int i = 0; st != null && i < st.length(); i++) { JSONObject s = st.optJSONObject(i); if (s == null) continue; if (b.length() > 0) b.append("\n"); b.append(s.optString("title")); if (s.optInt("min") > 0) b.append(" ").append(s.optInt("min")); } return b.toString(); }

    static void open(final NShell sh, final JSONObject h, String presetKind, JSONObject tpl) {
        final android.content.Context c = sh.a; final NForms F = new NForms(sh); final boolean edit = h != null;
        final JSONObject src = edit ? h : tpl != null ? tpl : new JSONObject();
        final String[] kind = {edit ? NHabits.kind(h) : tpl != null ? tpl.optString("kind", "build") : presetKind == null ? "build" : presetKind};
        LinearLayout b = F.sheetBody("Habit", edit ? "Edit habit" : tpl != null ? "New habit from template" : "New habit", edit ? null : "Something you do (or stop doing) on a rhythm.");
        final LinearLayout buildG = NUi.col(c), quitG = NUi.col(c), routG = NUi.col(c), freqG = NUi.col(c), limG = NUi.col(c), stopG = NUi.col(c);
        final Runnable sync = new Runnable() { public void run() {
            String k = kind[0];
            buildG.setVisibility(k.equals("build") ? View.VISIBLE : View.GONE); quitG.setVisibility(k.equals("quit") ? View.VISIBLE : View.GONE);
            routG.setVisibility(k.equals("routine") ? View.VISIBLE : View.GONE); freqG.setVisibility(k.equals("quit") ? View.GONE : View.VISIBLE);
        } };
        if (!edit) { b.addView(NForms.fieldLabel(c, "Type")); b.addView(NForms.choice(c, new String[][]{{"build", "Build"}, {"quit", "Break"}, {"routine", "Routine"}}, kind, sync)); }
        b.addView(NForms.fieldLabel(c, "Name"));
        final EditText title = NForms.input(c, kind[0].equals("quit") ? "Quit smoking" : kind[0].equals("routine") ? "Morning routine" : "Read 20 pages", edit || tpl != null ? src.optString("title") : "", false); b.addView(title);
        NVoice.attach(c, title);
        /* icon */
        b.addView(NForms.fieldLabel(c, "Icon"));
        final String[] icon = {edit ? NHabits.icon(h) : src.optString("icon")};
        final List<String> icons = new ArrayList<>(); for (String e : NForms.ICONS) icons.add(e); if (!icon[0].isEmpty() && !icons.contains(icon[0])) icons.add(0, icon[0]);
        HorizontalScrollView ihs = new HorizontalScrollView(c); ihs.setHorizontalScrollBarEnabled(false); final LinearLayout ir = NUi.row(c); ihs.addView(ir);
        final Runnable[] paintI = new Runnable[1];
        paintI[0] = new Runnable() { public void run() {
            ir.removeAllViews();
            for (final String e : icons) {
                TextView t = NUi.text(c, e, 22, NTheme.text); t.setGravity(Gravity.CENTER);
                t.setBackground(NUi.ripple(e.equals(icon[0]) ? NUi.round(NTheme.alpha(NTheme.accent, .25f), 14, NTheme.accent) : NUi.round(NTheme.surface, 14, NTheme.line), 14));
                NUi.tap(t, new View.OnClickListener() { public void onClick(View v) { icon[0] = e; paintI[0].run(); } });
                LinearLayout.LayoutParams l = NUi.lp(NUi.dp(48), NUi.dp(48)); l.rightMargin = NUi.dp(8); ir.addView(t, l);
            }
        } };
        paintI[0].run(); b.addView(ihs);
        /* ---- break ---- */
        quitG.addView(NForms.fieldLabel(c, "Approach"));
        final String[] mode = {src.optString("mode", "quit").equals("limit") ? "limit" : "quit"};
        final Runnable msync = new Runnable() { public void run() { stopG.setVisibility(mode[0].equals("quit") ? View.VISIBLE : View.GONE); limG.setVisibility(mode[0].equals("limit") ? View.VISIBLE : View.GONE); } };
        quitG.addView(NForms.choice(c, new String[][]{{"quit", "Stop completely"}, {"limit", "Cut down"}}, mode, msync));
        stopG.addView(NForms.fieldLabel(c, edit ? "Current run started" : "When did you stop?"));
        long startMs = edit && h.optLong("start") > 0 ? h.optLong("start") : System.currentTimeMillis();
        final String[] sd = {NDates.ymd(startMs)}; final String[] stt = {new java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(new java.util.Date(startMs))};
        stopG.addView(NForms.datePick(c, sd, "Pick a date"));
        stopG.addView(NForms.timePick(c, stt, "Time"), NUi.mt(8));
        TextView hint = NUi.text(c, "Already stopped a while ago? Set the real date and the clock starts there.", 12.5f, NTheme.muted); hint.setPadding(0, NUi.dp(6), 0, 0); stopG.addView(hint);
        quitG.addView(stopG);
        limG.addView(NForms.fieldLabel(c, "Daily limit · unit"));
        LinearLayout lr = NUi.row(c);
        final EditText limit = NForms.input(c, "2", String.valueOf(Math.max(0, src.optInt("limit", 1))), false); limit.setInputType(InputType.TYPE_CLASS_NUMBER);
        final EditText lunit = NForms.input(c, "cups, sessions", src.optString("unit"), false);
        lr.addView(limit, NUi.lpw(0, -2, 1)); lr.addView(new View(c), NUi.lp(NUi.dp(10), 1)); lr.addView(lunit, NUi.lpw(0, -2, 2)); limG.addView(lr);
        quitG.addView(limG);
        quitG.addView(NForms.fieldLabel(c, "When the urge hits, I will… · optional"));
        final EditText plan = NForms.input(c, "Drink water and walk for five minutes", src.optString("plan"), false); quitG.addView(plan);
        quitG.addView(NForms.fieldLabel(c, "Money per day it costs · optional"));
        LinearLayout cr = NUi.row(c);
        final EditText cur = NForms.input(c, "$", src.optString("cur", "$").isEmpty() ? "$" : src.optString("cur", "$"), false); final EditText cost = NForms.input(c, "0", src.optDouble("cost", 0) > 0 ? trimNum(src.optDouble("cost")) : "", false); cost.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        cr.addView(cur, NUi.lp(NUi.dp(72), -2)); cr.addView(new View(c), NUi.lp(NUi.dp(10), 1)); cr.addView(cost, NUi.lpw(0, -2, 1)); quitG.addView(cr);
        quitG.addView(NForms.fieldLabel(c, "Minutes per day it takes · optional"));
        final EditText mins = NForms.input(c, "0", src.optInt("mins") > 0 ? String.valueOf(src.optInt("mins")) : "", false); mins.setInputType(InputType.TYPE_CLASS_NUMBER); quitG.addView(mins);
        b.addView(quitG);
        msync.run();
        /* ---- build ---- */
        buildG.addView(NForms.fieldLabel(c, "Tie it to something you already do · optional"));
        final EditText cue = NForms.input(c, "After I pour my morning coffee", src.optString("cue"), false); buildG.addView(cue);
        buildG.addView(NForms.fieldLabel(c, "Amount per day · unit"));
        LinearLayout tr = NUi.row(c);
        final EditText target = NForms.input(c, "1", String.valueOf(Math.max(1, src.optInt("target", 1))), false); target.setInputType(InputType.TYPE_CLASS_NUMBER);
        final EditText unit = NForms.input(c, "glasses, pages, minutes", kind[0].equals("quit") ? "" : src.optString("unit"), false);
        tr.addView(target, NUi.lpw(0, -2, 1)); tr.addView(new View(c), NUi.lp(NUi.dp(10), 1)); tr.addView(unit, NUi.lpw(0, -2, 2)); buildG.addView(tr);
        buildG.addView(NForms.fieldLabel(c, "Small version for hard days · optional"));
        final EditText mini = NForms.input(c, "Just put on running shoes", src.optString("mini"), false); buildG.addView(mini);
        b.addView(buildG);
        /* ---- routine ---- */
        routG.addView(NForms.fieldLabel(c, "Steps · one per line, minutes at the end"));
        JSONArray srcSteps = edit ? NHabits.steps(h) : null;
        String stxt = edit ? stepsText(srcSteps) : tpl != null && tpl.has("steps") ? stepsText(parseSteps(tpl.optString("steps"), null)) : "";
        final EditText steps = NForms.input(c, "Drink a glass of water 1\nStretch 5\nPlan the day 5", stxt, true); steps.setMinLines(4); routG.addView(steps);
        b.addView(routG);
        /* ---- how often (build + routine) ---- */
        freqG.addView(NForms.fieldLabel(c, "How often"));
        final String[] freq = {edit ? NHabits.freq(h) : src.optString("freq", "daily")};
        final boolean[] days = new boolean[7];
        if (edit) { for (int d : NHabits.days(h)) days[d] = true; } else if (src.optJSONArray("days") != null) { JSONArray da = src.optJSONArray("days"); for (int i = 0; i < da.length(); i++) days[da.optInt(i)] = true; } else for (int i = 0; i < 7; i++) days[i] = true;
        if (edit && !freq[0].equals("days")) for (int i = 0; i < 7; i++) days[i] = true;
        final LinearLayout daysF = NUi.col(c), timesF = NUi.col(c);
        final Runnable fsync = new Runnable() { public void run() { daysF.setVisibility(freq[0].equals("days") ? View.VISIBLE : View.GONE); timesF.setVisibility(freq[0].equals("times") ? View.VISIBLE : View.GONE); } };
        freqG.addView(NForms.choice(c, new String[][]{{"daily", "Every day"}, {"days", "Some days"}, {"times", "X a week"}}, freq, fsync));
        daysF.addView(NForms.fieldLabel(c, "On"));
        final LinearLayout dr = NUi.row(c); final Runnable[] paintD = new Runnable[1];
        paintD[0] = new Runnable() { public void run() {
            dr.removeAllViews();
            for (int i = 0; i < 7; i++) { final int k = i;
                TextView t = NUi.body(c, NDates.DAYS[i].substring(0, 2), 13, days[i] ? NTheme.onAccent : NTheme.text, 700); t.setGravity(Gravity.CENTER);
                t.setBackground(NUi.ripple(days[i] ? NUi.oval(NTheme.accent, 0, 0) : NUi.oval(NTheme.surface, NTheme.line2, 1), 99));
                NUi.tap(t, new View.OnClickListener() { public void onClick(View v) { days[k] = !days[k]; paintD[0].run(); } });
                LinearLayout.LayoutParams l = NUi.lpw(0, NUi.dp(42), 1); l.rightMargin = NUi.dp(5); dr.addView(t, l); }
        } };
        paintD[0].run(); daysF.addView(dr); freqG.addView(daysF);
        timesF.addView(NForms.fieldLabel(c, "Times a week"));
        final String[] times = {String.valueOf(edit ? NHabits.times(h) : Math.min(6, Math.max(1, src.optInt("times", 3))))};
        timesF.addView(NForms.choice(c, new String[][]{{"1", "1×"}, {"2", "2×"}, {"3", "3×"}, {"4", "4×"}, {"5", "5×"}, {"6", "6×"}}, times, null)); freqG.addView(timesF);
        fsync.run();
        freqG.addView(NForms.fieldLabel(c, "Time of day"));
        final String[] part = {edit ? NHabits.part(h) : src.optString("part", "any").isEmpty() ? "any" : src.optString("part", "any")};
        freqG.addView(NForms.choice(c, HP, part, null));
        freqG.addView(NForms.fieldLabel(c, "Reminder time · optional"));
        final String[] tm = {src.optString("time")};
        freqG.addView(NForms.timePick(c, tm, "No reminder (long-press to clear)"));
        final NSettings.Sw remind = new NSettings.Sw(c, src.optBoolean("remind"));
        LinearLayout rr = NUi.row(c); rr.setPadding(0, NUi.dp(14), 0, 0); rr.addView(NUi.body(c, "Remind me", 15, NTheme.text, 600), NUi.lpw(0, -2, 1)); rr.addView(remind);
        NUi.tap(rr, new View.OnClickListener() { public void onClick(View v) { remind.on = !remind.on; remind.invalidate(); } });
        freqG.addView(rr);
        b.addView(freqG);
        sync.run();
        /* why / area / goal */
        b.addView(NForms.fieldLabel(c, "Why it matters · optional"));
        final EditText why = NForms.input(c, "Read this on the days you don’t feel like it", src.optString("why"), true); why.setMinLines(2); b.addView(why);
        NVoice.attach(c, why);
        b.addView(NForms.fieldLabel(c, "Life area"));
        final String[] area = {NTheme.areaKey(src.optString("area", "personal")).isEmpty() ? "personal" : NTheme.areaKey(src.optString("area", "personal"))};
        b.addView(NForms.choice(c, NForms.areaOpts(), area, null));
        final String[] goal = {edit ? h.optString("goalId") : ""};
        final List<String[]> gl = new ArrayList<>();
        for (String[] o : NGoalForm.parentList(sh.st, null)) { JSONObject g = sh.st.find("goals", o[0]); if (g != null && "active".equals(g.optString("status"))) gl.add(o); }
        if (!gl.isEmpty()) {
            b.addView(NForms.fieldLabel(c, "Supports a goal · optional"));
            final TextView gv = NUi.body(c, "", 15, NTheme.text, 600); gv.setBackground(NUi.ripple(NUi.round(NTheme.surface, 14, NTheme.line2), 14)); gv.setPadding(NUi.dp(16), NUi.dp(13), NUi.dp(16), NUi.dp(13)); gv.setSingleLine(true); gv.setEllipsize(android.text.TextUtils.TruncateAt.END);
            final Runnable gp = new Runnable() { public void run() { String t = "None"; for (String[] o : gl) if (o[0].equals(goal[0])) t = o[1].trim(); gv.setText(t + "   ▾"); } };
            gp.run();
            NUi.tap(gv, new View.OnClickListener() { public void onClick(View v) {
                final String[] items = new String[gl.size() + 1]; items[0] = "None"; for (int i = 0; i < gl.size(); i++) items[i + 1] = gl.get(i)[1];
                new android.app.AlertDialog.Builder(c).setItems(items, new android.content.DialogInterface.OnClickListener() { public void onClick(android.content.DialogInterface d, int i) { goal[0] = i == 0 ? "" : gl.get(i - 1)[0]; gp.run(); } }).show();
            } });
            b.addView(gv);
        }
        List<View> acts = new ArrayList<>();
        acts.add(NUi.btn(c, "Cancel", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } }));
        acts.add(NUi.btn(c, edit ? "Save" : "Create", true, new View.OnClickListener() { public void onClick(View v) {
            String t = title.getText().toString().trim();
            if (t.isEmpty()) { NShell.toast("Give it a name"); return; }
            String k = kind[0];
            try {
                JSONObject o = edit ? h : NForms.newHabit(k);
                if (tpl != null && !edit) { for (java.util.Iterator<String> it = tpl.keys(); it.hasNext(); ) { String key = it.next(); if (!key.equals("steps") && !key.equals("kind")) o.put(key, tpl.get(key)); } }
                o.put("title", t); o.put("icon", icon[0]); o.put("why", why.getText().toString().trim()); o.put("area", area[0]); o.put("goalId", goal[0]);
                if (!k.equals("quit")) {
                    String fq = freq[0]; JSONArray da = new JSONArray(); int cnt = 0; for (int i = 0; i < 7; i++) if (days[i]) { da.put(i); cnt++; }
                    if (fq.equals("days") && (cnt == 0 || cnt == 7)) { fq = "daily"; da = new JSONArray("[0,1,2,3,4,5,6]"); }
                    if (da.length() == 0) da = new JSONArray("[0,1,2,3,4,5,6]");
                    o.put("freq", fq); o.put("days", da); o.put("times", Integer.parseInt(times[0])); o.put("part", part[0]);
                    String tt = tm[0] == null ? "" : tm[0]; o.put("time", tt); o.put("remind", remind.on && !tt.isEmpty());
                    if (remind.on && !tt.isEmpty()) sh.a.askNotifications();
                }
                if (k.equals("build")) {
                    o.put("cue", cue.getText().toString().trim()); o.put("mini", mini.getText().toString().trim());
                    int tg = 1; try { tg = Math.max(1, Math.min(999, Integer.parseInt(target.getText().toString().trim()))); } catch (Exception ignored) { }
                    o.put("target", tg); o.put("unit", unit.getText().toString().trim());
                }
                if (k.equals("routine")) {
                    JSONArray st = parseSteps(steps.getText().toString(), edit ? NHabits.steps(h) : null);
                    if (st.length() == 0) { NShell.toast("Add at least one step"); return; }
                    o.put("steps", st);
                }
                if (k.equals("quit")) {
                    o.put("mode", mode[0]); int lm = 0; try { lm = Math.max(0, Integer.parseInt(limit.getText().toString().trim())); } catch (Exception ignored) { }
                    o.put("limit", lm); o.put("unit", lunit.getText().toString().trim()); o.put("plan", plan.getText().toString().trim());
                    double cs = 0; try { cs = Math.max(0, Double.parseDouble(cost.getText().toString().trim())); } catch (Exception ignored) { }
                    o.put("cost", cs); String cu = cur.getText().toString().trim(); o.put("cur", cu.isEmpty() ? "$" : cu);
                    int mn = 0; try { mn = Math.max(0, Math.min(1440, Integer.parseInt(mins.getText().toString().trim()))); } catch (Exception ignored) { }
                    o.put("mins", mn);
                    if (mode[0].equals("quit") && NDates.valid(sd[0])) {
                        long ms = NDates.cal(sd[0]).getTimeInMillis(); String[] hm = (stt[0] == null || stt[0].isEmpty() ? "00:00" : stt[0]).split(":");
                        java.util.Calendar cc = java.util.Calendar.getInstance(); cc.setTimeInMillis(ms); cc.set(java.util.Calendar.HOUR_OF_DAY, Integer.parseInt(hm[0])); cc.set(java.util.Calendar.MINUTE, Integer.parseInt(hm[1])); cc.set(java.util.Calendar.SECOND, 0); cc.set(java.util.Calendar.MILLISECOND, 0);
                        long ns = Math.min(System.currentTimeMillis(), cc.getTimeInMillis());
                        if (!edit || Math.abs(ns - h.optLong("start")) > 60000) { if (edit && ns != h.optLong("start")) { o.put("mile", 0); if (NDates.ymd(ns).compareTo(o.optString("startDate")) < 0) o.put("startDate", NDates.ymd(ns)); } o.put("start", ns); }
                    }
                }
                if (!edit) {
                    o.put("kind", k); o.put("createdAt", System.currentTimeMillis());
                    o.put("startDate", k.equals("quit") && o.optLong("start") > 0 ? NDates.ymd(o.optLong("start")) : NDates.ymd());
                    if (k.equals("quit")) o.put("mile", NActs.floorMile(NActs.MILES, (int) ((System.currentTimeMillis() - o.optLong("start")) / 86400000L)));
                    sh.st.arr("habits").put(o);
                }
                sh.closeSheet(); sh.save();
                if (edit) NShell.toast("Saved");
                else { sh.push(new NHabitScreen(sh, o.optString("id"))); NShell.toast(k.equals("quit") ? "Clock started. You’ve got this." : k.equals("routine") ? "Routine created" : "Habit created"); }
            } catch (Exception e) { NCrash.log(c, "habit save", e); }
        } }));
        b.addView(NForms.actions(c, acts.toArray(new View[0])));
        sh.sheet(b);
        if (!edit && tpl == null) NForms.focus(title);
    }

    static String trimNum(double d) { return d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d); }

    /* web tplGrid(only): a section per kind ("<Kind>" + "templates"), one .tpl row per template, onto a page */
    static void tplGrid(final NShell sh, LinearLayout into, String only) {
        final android.content.Context c = sh.a; JSONArray all = tpls();
        String[][] groups = {{"build", "Build a habit"}, {"quit", "Break a habit"}, {"routine", "Routines"}};
        for (String[] g : groups) {
            if (only != null && !only.equals(g[0])) continue;
            LinearLayout hd = NUi.sectionHead(c, g[1], null, null); hd.addView(NUi.label(c, "templates", NTheme.muted)); into.addView(hd);
            boolean first = true;
            for (int i = 0; i < all.length(); i++) {
                final JSONObject t = all.optJSONObject(i); if (t == null || !t.optString("kind").equals(g[0])) continue;
                String sub = t.optString("kind").equals("quit") ? (t.optString("mode").equals("limit") ? "Max " + t.optInt("limit") + " " + t.optString("unit") + " a day" : "Live clean-time clock")
                    : t.optString("kind").equals("routine") ? t.optString("steps").split("\n").length + " steps" : t.optString("freq").equals("times") ? t.optInt("times") + "× a week" : t.optInt("target", 1) > 1 ? t.optInt("target") + " " + t.optString("unit") + " a day" : "Every day";
                int col = NTheme.areaCol(t.optString("area"));
                LinearLayout r = NUi.row(c); r.setGravity(Gravity.CENTER_VERTICAL); r.setBackground(NUi.ripple(NUi.round(NTheme.surface, 18, NTheme.line), 18)); r.setPadding(NUi.dp(14), NUi.dp(12), NUi.dp(14), NUi.dp(12));
                TextView ic = NUi.text(c, t.optString("icon"), 18, NTheme.text); ic.setGravity(Gravity.CENTER); ic.setIncludeFontPadding(false);
                ic.setBackground(NUi.round(NTheme.alpha(col, .16f), 12, NTheme.alpha(col, .3f))); r.addView(ic, NUi.lp(NUi.dp(36), NUi.dp(36)));
                LinearLayout tx = NUi.col(c); tx.setPadding(NUi.dp(12), 0, 0, 0);
                tx.addView(NUi.body(c, t.optString("title"), 14.5f, NTheme.text, 600)); tx.addView(NUi.text(c, sub, 12.5f, NTheme.muted));
                r.addView(tx, NUi.lpw(0, -2, 1));
                NUi.tap(r, new View.OnClickListener() { public void onClick(View v) { open(sh, null, null, t); } });
                into.addView(r, NUi.mt(first ? 0 : 10)); first = false;
            }
        }
    }

    /* the template grid, grouped: build / break / routine */
    static void templates(final NShell sh) {
        final android.content.Context c = sh.a; NForms F = new NForms(sh);
        LinearLayout b = F.sheetBody("Templates", "Start from a template", "Pick one, adjust anything, then save.");
        JSONArray all = tpls();
        String[][] groups = {{"build", "Build a habit"}, {"quit", "Break a habit"}, {"routine", "Routines"}};
        for (String[] g : groups) {
            b.addView(NUi.sectionHead(c, g[1], null, null));
            for (int i = 0; i < all.length(); i++) {
                final JSONObject t = all.optJSONObject(i); if (t == null || !t.optString("kind").equals(g[0])) continue;
                String sub = t.optString("kind").equals("quit") ? (t.optString("mode").equals("limit") ? "Max " + t.optInt("limit") + " " + t.optString("unit") + " a day" : "Live clean-time clock")
                    : t.optString("kind").equals("routine") ? t.optString("steps").split("\n").length + " steps" : t.optString("freq").equals("times") ? t.optInt("times") + "× a week" : t.optInt("target", 1) > 1 ? t.optInt("target") + " " + t.optString("unit") + " a day" : "Every day";
                int col = NTheme.areaCol(t.optString("area"));
                LinearLayout r = NUi.row(c); r.setBackground(NUi.ripple(NUi.round(NTheme.surface, 16, NTheme.line), 16)); r.setPadding(NUi.dp(12), NUi.dp(11), NUi.dp(12), NUi.dp(11));
                TextView ic = NUi.text(c, t.optString("icon"), 17, NTheme.text); ic.setGravity(Gravity.CENTER); ic.setBackground(NUi.round(NTheme.alpha(col, .22f), 10, 0)); r.addView(ic, NUi.lp(NUi.dp(34), NUi.dp(34)));
                LinearLayout tx = NUi.col(c); tx.setPadding(NUi.dp(12), 0, 0, 0);
                tx.addView(NUi.body(c, t.optString("title"), 15, NTheme.text, 650)); tx.addView(NUi.text(c, sub, 12.5f, NTheme.muted));
                r.addView(tx, NUi.lpw(0, -2, 1));
                NUi.tap(r, new View.OnClickListener() { public void onClick(View v) { open(sh, null, null, t); } });
                b.addView(r, NUi.mt(8));
            }
        }
        sh.sheet(b);
    }
}
