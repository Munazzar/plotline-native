package com.munazzar.plotline;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/* Add/edit sheets for goals, steps, habits, moments and threads. Objects are created in the web app's exact
   shape so the web version, sync and reminders read them without any conversion. */
final class NForms {
    final NShell sh; final Context c;
    NForms(NShell sh) { this.sh = sh; this.c = sh.a; }

    /* ---------- small form widgets ---------- */
    static EditText input(Context c, String hint, String value, boolean multi) {
        EditText e = new EditText(c);
        e.setHint(hint); e.setText(value == null ? "" : value);
        e.setTextColor(NTheme.text); e.setHintTextColor(NTheme.alpha(NTheme.muted, .9f));
        e.setTypeface(NFont.body(500)); e.setTextSize(16);
        e.setBackground(NUi.round(NTheme.surface, 14, NTheme.line2));
        e.setPadding(NUi.dp(16), NUi.dp(13), NUi.dp(16), NUi.dp(13));
        if (multi) { e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES); e.setMinLines(3); e.setGravity(Gravity.TOP); }
        else { e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES); e.setSingleLine(true); }
        return e;
    }

    static TextView fieldLabel(Context c, String s) { TextView t = NUi.label(c, s, NTheme.muted); t.setPadding(0, NUi.dp(18), 0, NUi.dp(8)); return t; }

    static void focus(final EditText e) {
        e.postDelayed(new Runnable() { public void run() {
            e.requestFocus();
            InputMethodManager im = (InputMethodManager) e.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            im.showSoftInput(e, InputMethodManager.SHOW_IMPLICIT);
        } }, 380);
    }

    /* a row of choice chips; returns the holder; selection in sel[0] */
    static HorizontalScrollView choice(final Context c, final String[][] opts, final String[] sel, final Runnable onChange) {
        HorizontalScrollView hs = new HorizontalScrollView(c); hs.setHorizontalScrollBarEnabled(false); hs.setClipToPadding(false);
        final LinearLayout r = NUi.row(c);
        hs.addView(r);
        final Runnable[] paint = new Runnable[1];
        paint[0] = new Runnable() { public void run() {
            r.removeAllViews();
            for (final String[] o : opts) {
                TextView t = NUi.chip(c, o[1], o[0].equals(sel[0]), new View.OnClickListener() { public void onClick(View v) { sel[0] = o[0]; paint[0].run(); if (onChange != null) onChange.run(); } });
                LinearLayout.LayoutParams l = NUi.lp(-2, -2); l.rightMargin = NUi.dp(8); r.addView(t, l);
            }
        } };
        paint[0].run();
        return hs;
    }

    /* tappable value that opens the native date picker; d[0] holds yyyy-MM-dd or "" */
    static TextView datePick(final Context c, final String[] d, final String empty) {
        final TextView t = NUi.body(c, "", 15, NTheme.text, 600);
        t.setBackground(NUi.ripple(NUi.round(NTheme.surface, 14, NTheme.line2), 14));
        t.setPadding(NUi.dp(16), NUi.dp(13), NUi.dp(16), NUi.dp(13));
        final Runnable paint = new Runnable() { public void run() { t.setText(NDates.valid(d[0]) ? NDates.longDate(d[0]) + "   ✕" : empty); t.setTextColor(NDates.valid(d[0]) ? NTheme.text : NTheme.muted); } };
        paint.run();
        NUi.tap(t, new View.OnClickListener() { public void onClick(View v) {
            if (NDates.valid(d[0]) && t.getTag() == null) { /* first tap on a set date offers to clear via long press; open picker */ }
            Calendar k = NDates.valid(d[0]) ? NDates.cal(d[0]) : Calendar.getInstance();
            new DatePickerDialog(c, new DatePickerDialog.OnDateSetListener() { public void onDateSet(DatePicker p, int y, int m, int dd) {
                Calendar x = Calendar.getInstance(); x.clear(); x.set(y, m, dd); d[0] = NDates.ymd(x); paint.run();
            } }, k.get(Calendar.YEAR), k.get(Calendar.MONTH), k.get(Calendar.DAY_OF_MONTH)).show();
        } });
        t.setOnLongClickListener(new View.OnLongClickListener() { public boolean onLongClick(View v) { d[0] = ""; paint.run(); return true; } });
        return t;
    }

    static TextView timePick(final Context c, final String[] tm, final String empty) {
        final TextView t = NUi.body(c, "", 15, NTheme.text, 600);
        t.setBackground(NUi.ripple(NUi.round(NTheme.surface, 14, NTheme.line2), 14));
        t.setPadding(NUi.dp(16), NUi.dp(13), NUi.dp(16), NUi.dp(13));
        final Runnable paint = new Runnable() { public void run() { String f = NDates.fmtTime(tm[0]); t.setText(f.isEmpty() ? empty : f); t.setTextColor(f.isEmpty() ? NTheme.muted : NTheme.text); } };
        paint.run();
        NUi.tap(t, new View.OnClickListener() { public void onClick(View v) {
            int h = 9, m = 0;
            if (tm[0] != null && tm[0].matches("\\d{1,2}:\\d{2}")) { h = Integer.parseInt(tm[0].split(":")[0]); m = Integer.parseInt(tm[0].split(":")[1]); }
            new TimePickerDialog(c, new TimePickerDialog.OnTimeSetListener() { public void onTimeSet(TimePicker p, int hh, int mm) {
                tm[0] = String.format(java.util.Locale.US, "%02d:%02d", hh, mm); paint.run();
            } }, h, m, android.text.format.DateFormat.is24HourFormat(c)).show();
        } });
        t.setOnLongClickListener(new View.OnLongClickListener() { public boolean onLongClick(View v) { tm[0] = ""; paint.run(); return true; } });
        return t;
    }

    static LinearLayout actions(Context c, View... bs) {
        LinearLayout r = NUi.row(c); r.setGravity(Gravity.END | Gravity.CENTER_VERTICAL); r.setPadding(0, NUi.dp(22), 0, 0);
        for (View b : bs) { LinearLayout.LayoutParams l = NUi.lp(-2, -2); l.leftMargin = NUi.dp(10); r.addView(b, l); }
        return r;
    }

    LinearLayout sheetBody(String kicker, String title, String sub) {
        LinearLayout b = NUi.col(c);
        b.addView(NUi.label(c, kicker, NTheme.accent));
        TextView t = NUi.title(c, title, 30); t.setPadding(0, NUi.dp(6), 0, 0); b.addView(t);
        if (sub != null) { TextView s = NUi.text(c, sub, 14, NTheme.muted); s.setPadding(0, NUi.dp(6), 0, 0); s.setLineSpacing(0, 1.2f); b.addView(s); }
        return b;
    }

    static String[][] areaOpts() {
        String[][] o = new String[NGen.AREA_ID.length][2];
        for (int i = 0; i < o.length; i++) { o[i][0] = NGen.AREA_ID[i]; o[i][1] = NGen.AREA_NAME[i]; }
        return o;
    }

    /* ---------- goals ---------- */
    void goal(JSONObject g) { goal(g, ""); }

    void goal(final JSONObject g, final String parent) { NGoalForm.open(sh, g, parent); }

    static JSONObject newGoal(String title) throws Exception {
        JSONObject o = new JSONObject();
        o.put("img", ""); o.put("parent", ""); o.put("id", NStore.uid()); o.put("title", title); o.put("why", ""); o.put("area", "personal");
        o.put("horizon", "month"); o.put("priority", 2); o.put("pinned", false); o.put("startDate", NDates.ymd()); o.put("targetDate", "");
        o.put("checkin", ""); o.put("checkinTime", "19:00"); o.put("checkinDay", 1); o.put("lastCheckin", NDates.ymd()); o.put("status", "active");
        o.put("steps", new JSONArray()); o.put("links", new JSONArray()); o.put("createdAt", System.currentTimeMillis()); o.put("completedAt", JSONObject.NULL);
        return o;
    }

    static JSONObject newStep(String title, String due, String time) throws Exception {
        JSONObject s = new JSONObject();
        s.put("id", NStore.uid()); s.put("title", title); s.put("due", NDates.valid(due) ? due : ""); s.put("time", time == null ? "" : time);
        s.put("remind", ""); s.put("reminded", false); s.put("note", ""); s.put("done", false); s.put("doneAt", JSONObject.NULL);
        return s;
    }

    void step(final JSONObject g, final JSONObject s) {
        final boolean edit = s != null;
        LinearLayout b = sheetBody(g.optString("title"), edit ? "Edit step" : "Add a step", null);
        b.addView(fieldLabel(c, "Step"));
        final EditText title = input(c, "Buy running shoes", edit ? s.optString("title") : "", false); b.addView(title);
        NVoice.attach(c, title);
        b.addView(fieldLabel(c, "Due"));
        final String[] due = {edit ? s.optString("due") : ""};
        final TextView dueV = datePick(c, due, "No date (long-press to clear)");
        String[][] quick = {{"0", "Today"}, {"1", "Tomorrow"}, {"3", "3 days"}, {"7", "1 week"}, {"14", "2 weeks"}, {"30", "1 month"}, {"", "No date"}};
        HorizontalScrollView qh = new HorizontalScrollView(c); qh.setHorizontalScrollBarEnabled(false);
        LinearLayout qr = NUi.row(c); qh.addView(qr);
        for (final String[] q : quick) { LinearLayout.LayoutParams ql = NUi.lp(-2, -2); ql.rightMargin = NUi.dp(8);
            qr.addView(NUi.chip(c, q[1], false, new View.OnClickListener() { public void onClick(View v) {
                due[0] = q[0].isEmpty() ? "" : NDates.fromN(NDates.today() + Integer.parseInt(q[0]));
                dueV.setText(NDates.valid(due[0]) ? NDates.longDate(due[0]) + "   ✕" : "No date (long-press to clear)"); dueV.setTextColor(NDates.valid(due[0]) ? NTheme.text : NTheme.muted);
            } }), ql); }
        b.addView(qh, NUi.mt(0));
        b.addView(dueV, NUi.mt(10));
        b.addView(fieldLabel(c, "Time"));
        final String[] tm = {edit ? s.optString("time") : ""};
        b.addView(timePick(c, tm, "Any time (long-press to clear)"));
        b.addView(fieldLabel(c, "Repeat"));
        final String[] rep = {edit ? s.optString("repeat") : ""};
        b.addView(choice(c, new String[][]{{"", "Never"}, {"daily", "Daily"}, {"weekly", "Weekly"}, {"monthly", "Monthly"}}, rep, null));
        b.addView(fieldLabel(c, "Reminder"));
        final String[] rem = {edit && s.opt("remind") != null && !s.isNull("remind") ? String.valueOf(s.opt("remind")) : ""};
        List<String[]> ro = new ArrayList<>();
        ro.add(new String[]{"", "No reminder"}); ro.add(new String[]{"0", "At due time"}); ro.add(new String[]{"15", "15 minutes before"}); ro.add(new String[]{"30", "30 minutes before"}); ro.add(new String[]{"60", "1 hour before"}); ro.add(new String[]{"1440", "1 day before"}); ro.add(new String[]{"10080", "1 week before"}); ro.add(new String[]{"m", "Morning of the day"});
        if (rem[0].equals("c") && edit) ro.add(new String[]{"c", "Custom · " + NGoalSteps.remLabel(s)});
        else if (!rem[0].isEmpty() && !rem[0].equals("c")) { boolean has = false; for (String[] r0 : ro) if (r0[0].equals(rem[0])) has = true; if (!has) ro.add(new String[]{rem[0], NGoalSteps.remLabel(s)}); }
        b.addView(choice(c, ro.toArray(new String[0][]), rem, null));
        b.addView(fieldLabel(c, "Note"));
        final EditText note = input(c, "Anything to remember", edit ? s.optString("note") : "", true); note.setMinLines(2); b.addView(note);
        NVoice.attach(c, note);
        List<View> acts = new ArrayList<>();
        if (edit) acts.add(NUi.btn(c, "Delete", false, new View.OnClickListener() { public void onClick(View v) {
            JSONArray st = g.optJSONArray("steps");
            for (int i = st.length() - 1; i >= 0; i--) if (st.optJSONObject(i).optString("id").equals(s.optString("id"))) st.remove(i);
            sh.closeSheet(); sh.save(); NShell.toast("Step deleted");
        } }));
        acts.add(NUi.btn(c, edit ? "Save" : "Add step", true, new View.OnClickListener() { public void onClick(View v) {
            String t = title.getText().toString().trim();
            if (t.isEmpty()) { title.setError("Name the step"); return; }
            try {
                JSONObject o = edit ? s : newStep(t, due[0], tm[0]);
                String nd = NDates.valid(due[0]) ? due[0] : "", nt = tm[0] == null ? "" : tm[0];
                String oldRem = edit && s.opt("remind") != null && !s.isNull("remind") ? String.valueOf(s.opt("remind")) : "";
                if (edit && (!nd.equals(s.optString("due")) || !nt.equals(s.optString("time")) || !rem[0].equals(oldRem))) o.put("reminded", false);
                o.put("title", t); o.put("due", nd); o.put("time", nt); o.put("note", note.getText().toString().trim()); o.put("remind", rem[0]); o.put("repeat", rep[0]);
                if (!rem[0].isEmpty() && !nd.isEmpty()) sh.a.askNotifications();
                if (!edit) { g.optJSONArray("steps").put(o); if ("done".equals(g.optString("status"))) { g.put("status", "active"); g.put("completedAt", JSONObject.NULL); } }
                sh.closeSheet(); sh.save(); NShell.toast(edit ? "Saved" : "Step added");
            } catch (Exception e) { NCrash.log(c, "step save", e); }
        } }));
        if (edit) {
            b.addView(fieldLabel(c, "Focus timer" + (s.optInt("focus") > 0 ? " · " + s.optInt("focus") + " min so far" : "")));
            NFlow tf = new NFlow(c, 8, 8);
            for (final int m : new int[]{5, 15, 25, 45, 60}) tf.addView(NUi.chip(c, m + " min", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); NTimer.of(sh).start(g.optString("id"), s.optString("id"), m); } }), new android.view.ViewGroup.MarginLayoutParams(-2, NUi.dp(40)));
            b.addView(tf);
            NFlow mf = new NFlow(c, 8, 8);
            mf.addView(NUi.chip(c, "↑ Earlier", false, new View.OnClickListener() { public void onClick(View v) { moveStep(g, s, -1); } }), new android.view.ViewGroup.MarginLayoutParams(-2, NUi.dp(40)));
            mf.addView(NUi.chip(c, "↓ Later", false, new View.OnClickListener() { public void onClick(View v) { moveStep(g, s, 1); } }), new android.view.ViewGroup.MarginLayoutParams(-2, NUi.dp(40)));
            mf.addView(NUi.chip(c, "📅 Add to calendar", false, new View.OnClickListener() { public void onClick(View v) {
                if (!NDates.valid(due[0])) { NShell.toast("Set a date first"); return; }
                try { s.put("due", due[0]); s.put("time", tm[0] == null ? "" : tm[0]); s.put("remind", rem[0]); } catch (Exception ignored) { }
                sh.save(); sh.closeSheet(); NSheets.ics(sh, "icsStep", g.optString("id"), s.optString("id"));
            } }), new android.view.ViewGroup.MarginLayoutParams(-2, NUi.dp(40)));
            LinearLayout.LayoutParams mlp = NUi.lp(-1, -2); mlp.topMargin = NUi.dp(12);
            b.addView(mf, mlp);
        }
        b.addView(actions(c, acts.toArray(new View[0])));
        sh.sheet(b);
        if (!edit) focus(title);
    }

    void moveStep(JSONObject g, JSONObject s, int d) {
        JSONArray st = g.optJSONArray("steps"); int i = -1;
        for (int k = 0; st != null && k < st.length(); k++) if (st.optJSONObject(k).optString("id").equals(s.optString("id"))) i = k;
        int j = i + d; if (i < 0 || j < 0 || j >= st.length()) return;
        try { JSONObject o = st.getJSONObject(i); st.put(i, st.getJSONObject(j)); st.put(j, o); } catch (Exception ignored) { return; }
        sh.closeSheet(); sh.save(); NShell.toast("Now step " + (j + 1));
    }

    /* connect a goal to the goals it supports or depends on (two-way links, like the web linkGoals) */
    void connect(final JSONObject g) {
        LinearLayout b = sheetBody("Connect goals", "Connect goals", "Which goals does “" + g.optString("title") + "” support or depend on?");
        final List<JSONObject> others = new ArrayList<>(); final List<Boolean> on = new ArrayList<>();
        JSONArray lk = g.optJSONArray("links");
        for (JSONObject x : NActs.goals(sh.st, null)) { if (x.optString("id").equals(g.optString("id"))) continue; others.add(x); boolean has = false; for (int i = 0; lk != null && i < lk.length(); i++) if (x.optString("id").equals(lk.optString(i))) has = true; on.add(has); }
        if (others.isEmpty()) b.addView(NUi.body(c, "Create another goal first.", 14, NTheme.muted, 500), NUi.mt(12));
        for (int i = 0; i < others.size(); i++) {
            final int k = i; JSONObject x = others.get(i);
            LinearLayout r = NUi.row(c); r.setGravity(Gravity.CENTER_VERTICAL); r.setPadding(0, NUi.dp(10), 0, NUi.dp(10));
            final NSettings.Sw sw = new NSettings.Sw(c, on.get(i));
            LinearLayout t = NUi.col(c); t.addView(NUi.body(c, x.optString("title"), 15, NTheme.text, 600)); t.addView(NUi.body(c, NTheme.areaName(x.optString("area")), 12, NTheme.muted, 500));
            r.addView(t, new LinearLayout.LayoutParams(0, -2, 1)); r.addView(sw, NUi.lp(NUi.dp(48), NUi.dp(30)));
            View.OnClickListener tg = new View.OnClickListener() { public void onClick(View v) { boolean n = !on.get(k); on.set(k, n); sw.on = n; sw.invalidate(); } };
            r.setOnClickListener(tg); sw.setOnClickListener(tg);
            b.addView(r);
        }
        b.addView(actions(c,
            NUi.btn(c, "Cancel", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } }),
            NUi.btn(c, "Save", true, new View.OnClickListener() { public void onClick(View v) {
                try {
                    for (int i = 0; i < others.size(); i++) { JSONObject x = others.get(i); setLink(g, x, on.get(i)); }
                } catch (Exception ex) { NCrash.log(c, "connect", ex); }
                sh.closeSheet(); sh.save(); NShell.toast("Connections saved");
            } })));
        sh.sheet(b);
    }

    static void setLink(JSONObject a, JSONObject b, boolean link) throws Exception {
        String ai = a.optString("id"), bi = b.optString("id");
        JSONArray la = a.optJSONArray("links"), lb = b.optJSONArray("links");
        if (la == null) { la = new JSONArray(); a.put("links", la); } if (lb == null) { lb = new JSONArray(); b.put("links", lb); }
        a.put("links", withLink(la, bi, link)); b.put("links", withLink(lb, ai, link));
    }

    static JSONArray withLink(JSONArray src, String id, boolean link) throws Exception {
        JSONArray o = new JSONArray(); boolean has = false;
        for (int i = 0; i < src.length(); i++) { String v = src.optString(i); if (v.equals(id)) { has = true; if (!link) continue; } o.put(v); }
        if (link && !has) o.put(id);
        return o;
    }

    /* ---------- habits ---------- */
    static final String[] ICONS = {"💧", "📖", "🏃", "🚶", "💪", "🧘", "🙏", "🛏️", "🌙", "☀️", "🥗", "🍎", "🦷", "✍️", "🧠", "🎧", "🎯", "💼", "💸", "🧹", "👨‍👩‍👧", "🌱", "⏰", "🚭", "🍬", "☕", "📵", "📱", "🎮", "🍔", "🍺", "🛒"};

    void habit(JSONObject h) { habit(h, "build"); }

    void habit(final JSONObject h, String presetKind) { NHabitForm.open(sh, h, presetKind, null); }

    static JSONObject newHabit(String kind) throws Exception {
        JSONObject o = new JSONObject();
        long now = System.currentTimeMillis();
        o.put("id", NStore.uid()); o.put("kind", kind); o.put("title", "Habit"); o.put("icon", ""); o.put("why", ""); o.put("cue", ""); o.put("mini", ""); o.put("plan", "");
        o.put("area", "personal"); o.put("goalId", ""); o.put("freq", "daily"); o.put("days", new JSONArray("[0,1,2,3,4,5,6]")); o.put("times", 3); o.put("target", 1); o.put("unit", "");
        o.put("part", "any"); o.put("time", ""); o.put("remind", false); o.put("steps", new JSONArray()); o.put("log", new JSONObject()); o.put("rs", new JSONObject()); o.put("skip", new JSONObject());
        o.put("pz", new JSONArray()); o.put("mode", "quit"); o.put("limit", 1); o.put("start", kind.equals("quit") ? now : 0); o.put("slips", new JSONArray()); o.put("urges", new JSONArray());
        o.put("cost", 0); o.put("cur", "$"); o.put("mins", 0); o.put("mile", 0); o.put("smile", 0); o.put("status", "active"); o.put("startDate", NDates.ymd()); o.put("createdAt", now);
        return o;
    }

    /* ---------- moments (journal entries) ---------- */
    void entry(final JSONObject e) { NEntryForm.open(sh, e, null, null); }

    void entry(JSONObject e, String gid, String date) { NEntryForm.open(sh, e, gid, date); }

    static String moodEmoji(String k) { switch (k) { case "5": return "😄"; case "4": return "🙂"; case "3": return "😐"; case "2": return "😕"; case "1": return "😞"; default: return k; } }

    /* ---------- threads ---------- */
    static final String[][] TAGS = {{"note", "💭 Thought"}, {"idea", "💡 Idea"}, {"prog", "🚧 Progress"}, {"block", "⛔ Blocked"}, {"done", "✅ Done"}};
    static String tagEmoji(String k) { for (String[] t : TAGS) if (t[0].equals(k)) return t[1].substring(0, t[1].indexOf(' ')); return "💭"; }

    /* a web <select>: a field that opens a menu; rows with a null value are group headings */
    static TextView select(final Context c, final String[][] opts, final String[] sel, final Runnable onChange) {
        final TextView t = NUi.body(c, "", 15, NTheme.text, 600);
        t.setBackground(NUi.ripple(NUi.round(NTheme.surface, 14, NTheme.line2), 14));
        t.setPadding(NUi.dp(16), NUi.dp(13), NUi.dp(16), NUi.dp(13));
        final Runnable paint = new Runnable() { public void run() { String l = opts.length > 0 ? opts[0][1] : ""; for (String[] o : opts) if (o[0] != null && o[0].equals(sel[0])) l = o[1]; t.setText(l + "  ▾"); } };
        paint.run();
        NUi.tap(t, new View.OnClickListener() { public void onClick(View v) {
            android.widget.PopupMenu pm = new android.widget.PopupMenu(c, t);
            for (int i = 0; i < opts.length; i++) { android.view.MenuItem m = pm.getMenu().add(0, i, i, opts[i][0] == null ? opts[i][1].toUpperCase() : opts[i][1]); if (opts[i][0] == null) m.setEnabled(false); }
            pm.setOnMenuItemClickListener(new android.widget.PopupMenu.OnMenuItemClickListener() { public boolean onMenuItemClick(android.view.MenuItem m) {
                String v = opts[m.getItemId()][0]; if (v == null) return true; sel[0] = v; paint.run(); if (onChange != null) onChange.run(); return true; } });
            pm.show();
        } });
        return t;
    }

    static String trunc(String s, int n) { return s == null ? "" : s.length() > n ? s.substring(0, n - 1) + "…" : s; }

    /* thrLinkSel: not linked, active goals, habits not archived, and "this journal entry" when started from one */
    String[][] linkOpts(String curK, String curId) {
        List<String[]> o = new ArrayList<>(); o.add(new String[]{"", "Not linked · stands on its own"});
        List<JSONObject> gs = NActs.goals(sh.st, "active");
        if (!gs.isEmpty()) { o.add(new String[]{null, "Goals"}); for (JSONObject g : gs) o.add(new String[]{"goal:" + g.optString("id"), "🎯 " + trunc(g.optString("title"), 48)}); }
        List<JSONObject> hs = new ArrayList<>(); for (JSONObject h : NStore.list(sh.st.arr("habits"))) if (!"archived".equals(NHabits.status(h))) hs.add(h);
        if (!hs.isEmpty()) { o.add(new String[]{null, "Habits"}); for (JSONObject h : hs) o.add(new String[]{"habit:" + h.optString("id"), NHabits.icon(h) + " " + trunc(h.optString("title"), 48)}); }
        if ("entry".equals(curK)) o.add(new String[]{"entry:" + curId, "📓 This journal entry"});
        return o.toArray(new String[0][]);
    }

    static Object pickLink(String v) throws Exception {
        if (v == null || v.isEmpty()) return JSONObject.NULL;
        int i = v.indexOf(':'); JSONObject o = new JSONObject(); o.put("k", v.substring(0, i)); o.put("id", v.substring(i + 1)); return o;
    }

    void thread(final JSONObject t) { thread(t, null, null, null); }

    /* the web thrNewSheet / thrEdit */
    void thread(final JSONObject t, final String lk, final String lid, String ttl) {
        final boolean edit = t != null;
        LinearLayout b = sheetBody("Thread", edit ? "Edit thread" : "Start a thread", edit ? null : "A running log. Add a first thought now and keep adding as it grows.");
        b.addView(fieldLabel(c, edit ? "Name" : "Name it"));
        final EditText title = input(c, "App idea, Moving plans, Learning guitar…", edit ? t.optString("title") : (ttl == null ? "" : ttl), false); b.addView(title);
        final String[] k = {"note"};
        final EditText first = input(c, "What’s on your mind?", "", true);
        if (!edit) { b.addView(fieldLabel(c, "First update (optional)")); b.addView(choice(c, TAGS, k, null)); first.setMinLines(3); b.addView(first, NUi.mt(10)); NVoice.attach(c, first); }
        JSONObject cl = edit ? t.optJSONObject("link") : null;
        String ck = edit ? (cl == null ? "" : cl.optString("k")) : (lk == null ? "" : lk), ci = edit ? (cl == null ? "" : cl.optString("id")) : (lid == null ? "" : lid);
        final String[] link = {ck.isEmpty() ? "" : ck + ":" + ci};
        b.addView(fieldLabel(c, edit ? "Linked to" : "Link it to"));
        b.addView(select(c, linkOpts(ck, ci), link, null));
        if (!edit) { TextView hint = NUi.text(c, "Optional. A linked thread also shows on that goal or habit.", 12.5f, NTheme.muted); b.addView(hint, NUi.mt(6)); }
        List<View> acts = new ArrayList<>();
        if (edit) {
            TextView del = NUi.btn(c, "Delete", false, new View.OnClickListener() { public void onClick(View v) {
                sh.closeSheet();
                NSheets.confirm(sh, "Delete this thread?", "Its updates go with it. Your goals, habits and journal stay as they are.", "Delete", true, new Runnable() { public void run() {
                    sh.st.remove("threads", t.optString("id")); sh.save(); sh.pop(); sh.route("threads"); NShell.toast("Thread deleted"); } });
            } });
            del.setTextColor(NTheme.LATE); acts.add(del);
        } else acts.add(NUi.btn(c, "Cancel", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } }));
        acts.add(NUi.btn(c, edit ? "Save" : "Start thread", true, new View.OnClickListener() { public void onClick(View v) {
            String n = title.getText().toString().trim();
            if (n.isEmpty()) { title.setError("Name it"); return; }
            try {
                long now = System.currentTimeMillis();
                if (edit) { t.put("title", n); t.put("link", pickLink(link[0])); t.put("u", now); sh.closeSheet(); sh.save(); NShell.toast("Saved"); return; }
                JSONObject o = new JSONObject();
                o.put("id", NStore.uid()); o.put("title", n); o.put("link", pickLink(link[0])); o.put("status", "open"); o.put("created", now); o.put("u", now);
                JSONArray ups = new JSONArray();
                String x = first.getText().toString().trim();
                if (!x.isEmpty()) { JSONObject u = new JSONObject(); u.put("id", NStore.uid()); u.put("t", now); u.put("k", k[0]); u.put("x", x); u.put("u", now); ups.put(u); }
                o.put("ups", ups);
                sh.st.arr("threads").put(o);
                sh.closeSheet(); sh.save();
                sh.route("thread/" + o.optString("id"));
                NShell.toast("Thread started");
            } catch (Exception e) { NCrash.log(c, "thread save", e); }
        } }));
        b.addView(actions(c, acts.toArray(new View[0])));
        sh.sheet(b);
        if (!edit && title.getText().length() == 0) focus(title);
    }

    /* thrFrom: the threads linked to a goal, habit or journal entry; none yet → start one */
    void threadsFor(final String k, final String id) {
        JSONObject it = sh.st.find(k.equals("goal") ? "goals" : k.equals("habit") ? "habits" : "entries", id);
        if (it == null) return;
        String ttl = k.equals("entry") ? trunc((it.optString("title").isEmpty() ? it.optString("text").split("\n")[0] : it.optString("title")).trim(), 60) : it.optString("title");
        final List<JSONObject> L = new ArrayList<>();
        for (JSONObject t : NStore.list(sh.st.arr("threads"))) { JSONObject l = t.optJSONObject("link"); if (l != null && k.equals(l.optString("k")) && id.equals(l.optString("id"))) L.add(t); }
        if (L.isEmpty()) { thread(null, k, id, ttl); return; }
        java.util.Collections.sort(L, new java.util.Comparator<JSONObject>() { public int compare(JSONObject a, JSONObject b2) { return Long.compare(NJournalPage.last(b2), NJournalPage.last(a)); } });
        LinearLayout b = sheetBody("Threads", trunc(ttl.isEmpty() ? "This item" : ttl, 40), null);
        LinearLayout list = NUi.col(c);
        for (final JSONObject t : L) {
            JSONObject u = NJournalPage.lastUp(t);
            LinearLayout r = NUi.row(c); r.setGravity(Gravity.CENTER_VERTICAL); r.setPadding(NUi.dp(4), NUi.dp(12), NUi.dp(4), NUi.dp(12));
            r.addView(NUi.text(c, u != null ? tagEmoji(u.optString("k")) : "🧵", 20, NTheme.text), NUi.lp(NUi.dp(36), -2));
            LinearLayout tx = NUi.col(c);
            tx.addView(NUi.ell(NUi.body(c, t.optString("title"), 15, NTheme.text, 700), 1));
            tx.addView(NUi.ell(NUi.text(c, u != null ? u.optString("x") : "Nothing logged yet", 13, NTheme.muted), 1));
            r.addView(tx, NUi.lpw(0, -2, 1));
            r.addView(NUi.label(c, NDates.ago(NJournalPage.last(t)), NTheme.muted));
            NUi.tap(r, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); sh.route("thread/" + t.optString("id")); } });
            list.addView(r);
        }
        b.addView(list, NUi.mt(14));
        b.addView(actions(c, NUi.btn(c, "Close", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } }),
            NUi.btn(c, "+ New thread", true, new View.OnClickListener() { public void onClick(View v) {
                sh.closeSheet(); JSONObject it2 = sh.st.find(k.equals("goal") ? "goals" : k.equals("habit") ? "habits" : "entries", id);
                final String t2 = it2 == null ? "" : !it2.optString("title").isEmpty() ? it2.optString("title") : trunc(it2.optString("text").split("\n")[0], 60);
                v.postDelayed(new Runnable() { public void run() { thread(null, k, id, t2); } }, 380);
            } })));
        sh.sheet(b);
    }

    /* web thrAddSheet: add an update to a thread from its card */
    void threadAdd(final JSONObject t) {
        LinearLayout b = sheetBody(t.optString("title"), "Add an update", null);
        final String[] k = {"note"};
        b.addView(choice(c, TAGS, k, null), NUi.mt(12));
        final EditText in = input(c, "What’s new?", "", true); in.setMinLines(4);
        b.addView(in, NUi.mt(12));
        b.addView(actions(c,
            NUi.btn(c, "Cancel", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } }),
            NUi.btn(c, "Add update", true, new View.OnClickListener() { public void onClick(View v) {
                String x = in.getText().toString().trim(); if (x.isEmpty()) return;
                try { addUpdate(t, k[0], x); t.put("u", System.currentTimeMillis()); } catch (Exception ignored) { }
                sh.closeSheet(); sh.save(); NShell.toast("Added");
            } })));
        sh.sheet(b);
        in.postDelayed(new Runnable() { public void run() { in.requestFocus(); } }, 350);
    }

    static void addUpdate(JSONObject t, String k, String x) throws Exception {
        long now = System.currentTimeMillis();
        JSONObject u = new JSONObject(); u.put("id", NStore.uid()); u.put("t", now); u.put("k", k); u.put("x", x); u.put("u", now);
        JSONArray ups = t.optJSONArray("ups"); if (ups == null) { ups = new JSONArray(); t.put("ups", ups); }
        ups.put(u);
        if ("done".equals(t.optString("status"))) t.put("status", "open");
    }

    /* ---------- the + menu ---------- */
    void addMenu() {
        LinearLayout b = sheetBody("Add", "What's new?", null);
        String[][] items = {{"🎯", "Goal", "Something you want to achieve"}, {"✅", "Habit", "Build or break a habit"}, {"📅", "Goal for today", "A one-day to-do"}, {"✍️", "Moment", "Write in your journal"}, {"🧵", "Thread", "A running log of an idea"}, {"✨", "Plan with AI", "Describe it, get steps and habits"}};
        for (int i = 0; i < items.length; i++) {
            final int k = i;
            LinearLayout r = NUi.row(c);
            r.setBackground(NUi.ripple(NUi.round(NTheme.surface, 18, NTheme.line), 18));
            r.setPadding(NUi.dp(16), NUi.dp(14), NUi.dp(16), NUi.dp(14));
            TextView e = NUi.text(c, items[i][0], 24, NTheme.text); r.addView(e, NUi.lp(NUi.dp(40), -2));
            LinearLayout tx = NUi.col(c);
            tx.addView(NUi.body(c, items[i][1], 16, NTheme.text, 700));
            tx.addView(NUi.text(c, items[i][2], 13, NTheme.muted));
            r.addView(tx, NUi.lpw(0, -2, 1));
            NUi.tap(r, new View.OnClickListener() { public void onClick(View v) {
                sh.closeSheet();
                v.postDelayed(new Runnable() { public void run() {
                    switch (k) {
                        case 0: goal(null); break;
                        case 1: habit(null); break;
                        case 2: dayGoal(NDates.ymd()); break;
                        case 3: entry(null); break;
                        case 4: thread(null); break;
                        default: sh.openClassic("ai");
                    }
                } }, 260);
            } });
            b.addView(r, NUi.mt(i == 0 ? 16 : 8));
        }
        sh.sheet(b);
    }

    /* a goal for a given day (the web app's "days") */
    void dayGoal(final String date) { dayGoal(date, null); }

    void dayGoal(final String date, final JSONObject x) { dayGoal(date, x, ""); }

    void dayGoal(final String date, final JSONObject x, final String preTime) {
        final boolean edit = x != null;
        LinearLayout b = sheetBody(NDates.dayName(date), edit ? "Edit goal for the day" : "Goal for the day", null);
        b.addView(fieldLabel(c, "What do you want to get done?"));
        final EditText title = input(c, "Call the bank", edit ? x.optString("title") : "", false); b.addView(title);
        NVoice.attach(c, title);
        b.addView(fieldLabel(c, "Date"));
        final String[] dt = {edit ? x.optString("date") : date};
        b.addView(datePick(c, dt, "Pick a date"));
        b.addView(fieldLabel(c, "Time · optional"));
        final String[] tm = {edit ? x.optString("time") : (preTime == null ? "" : preTime)};
        b.addView(timePick(c, tm, "Any time"));
        b.addView(fieldLabel(c, "How long"));
        final String[] dur = {String.valueOf(edit && x.optInt("dur") > 0 ? x.optInt("dur") : 30)};
        b.addView(choice(c, new String[][]{{"15", "15m"}, {"30", "30m"}, {"45", "45m"}, {"60", "1h"}, {"90", "1.5h"}, {"120", "2h"}, {"180", "3h"}}, dur, null));
        b.addView(fieldLabel(c, "Part of a goal?"));
        List<String[]> go = new ArrayList<>(); go.add(new String[]{"", "None"});
        for (JSONObject g : NActs.goals(sh.st, "active")) go.add(new String[]{g.optString("id"), g.optString("title")});
        final String[] gid = {edit ? NStore.s(x, "goalId") : ""};
        b.addView(choice(c, go.toArray(new String[0][]), gid, null));
        final View save = NUi.btn(c, edit ? "Save" : "Add", true, new View.OnClickListener() { public void onClick(View v) {
            String t = title.getText().toString().trim();
            if (t.isEmpty()) { title.setError("Write it down"); return; }
            try {
                if (edit) { x.put("title", t); x.put("date", NDates.valid(dt[0]) ? dt[0] : date); x.put("dur", Integer.parseInt(dur[0])); x.put("time", tm[0] == null ? "" : tm[0]); x.put("goalId", gid[0].isEmpty() ? JSONObject.NULL : gid[0]); }
                else { JSONObject o = addDay(sh.st, t, NDates.valid(dt[0]) ? dt[0] : date, tm[0]); if (o != null) { o.put("dur", Integer.parseInt(dur[0])); if (!gid[0].isEmpty()) o.put("goalId", gid[0]); } }
            } catch (Exception ignored) { }
            sh.closeSheet(); sh.save(); NShell.toast(edit ? "Saved" : "Added to " + NDates.dayName(date).toLowerCase());
        } });
        title.setImeOptions(EditorInfo.IME_ACTION_DONE);
        title.setOnEditorActionListener(new TextView.OnEditorActionListener() { public boolean onEditorAction(TextView v, int a, android.view.KeyEvent e) { if (e == null || e.getAction() == android.view.KeyEvent.ACTION_UP) save.performClick(); return true; } });
        List<View> acts = new ArrayList<>();
        if (edit) acts.add(NUi.btn(c, "Delete", false, new View.OnClickListener() { public void onClick(View v) { sh.st.remove("days", x.optString("id")); sh.closeSheet(); sh.save(); NShell.toast("Deleted"); } }));
        else acts.add(NUi.btn(c, "Cancel", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } }));
        acts.add(save);
        b.addView(actions(c, acts.toArray(new View[0])));
        sh.sheet(b);
        if (!edit) focus(title);
    }

    static JSONObject addDay(NStore st, String title, String date, String time) {
        try {
            JSONObject o = new JSONObject();
            o.put("id", NStore.uid()); o.put("title", title); o.put("date", date); o.put("time", time == null ? "" : time); o.put("goalId", JSONObject.NULL);
            o.put("done", false); o.put("doneAt", JSONObject.NULL); o.put("createdAt", System.currentTimeMillis());
            st.arr("days").put(o);
            return o;
        } catch (Exception e) { return null; }
    }
}
