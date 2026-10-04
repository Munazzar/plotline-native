package com.munazzar.plotline;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Calendar;
import org.json.JSONArray;
import org.json.JSONObject;

/* The bell sheet: one small native sheet to turn a habit / step / day goal / goal's reminders on and choose when.
   Changes save as you tap, like the web. The "next reminders" preview comes from the engine (it knows the schedule rules). */
final class NReminders {
    final NShell sh; final android.content.Context c; final String k, id, gid, sid; final Runnable back;
    LinearLayout box; String nextTxt = "", warn = "", ft = ""; boolean loaded;
    static final String[] PT = {"morning", "afternoon", "evening", "any"}, PTV = {"08:00", "13:00", "19:00", "09:00"};

    NReminders(NShell sh, String k, String id, String gid, String sid, Runnable back) { this.sh = sh; this.c = sh.a; this.k = k; this.id = id; this.gid = gid; this.sid = sid; this.back = back; }

    static void open(NShell sh, String k, String id) { new NReminders(sh, k, id, null, null, null).show(); }
    static void openStep(NShell sh, String gid, String sid, Runnable back) { new NReminders(sh, "step", null, gid, sid, back).show(); }

    JSONObject item() {
        if (k.equals("habit")) return sh.st.find("habits", id);
        if (k.equals("day")) return sh.st.find("days", id);
        if (k.equals("goal")) return sh.st.find("goals", id);
        JSONObject g = sh.st.find("goals", gid); if (g == null) return null;
        JSONArray a = g.optJSONArray("steps"); for (int i = 0; a != null && i < a.length(); i++) { JSONObject s = a.optJSONObject(i); if (s != null && sid.equals(s.optString("id"))) return s; }
        return null;
    }

    String partTime(JSONObject h) { String p = h.optString("part", "any"); for (int i = 0; i < PT.length; i++) if (PT[i].equals(p)) return PTV[i]; return "09:00"; }

    void show() {
        JSONObject o = item(); if (o == null) { NShell.toast("This item is gone"); return; }
        NForms F = new NForms(sh);
        LinearLayout b = F.sheetBody("Reminders", o.optString("title"), null);
        box = NUi.col(c); b.addView(box);
        LinearLayout acts = NUi.row(c); acts.setGravity(android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL); acts.setPadding(0, NUi.dp(22), 0, 0);
        if (back != null) { acts.addView(NUi.btn(c, "Back", false, new View.OnClickListener() { public void onClick(View v) { back.run(); } })); }
        LinearLayout.LayoutParams l = NUi.lp(-2, -2); l.leftMargin = NUi.dp(10);
        acts.addView(NUi.btn(c, "Done", true, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } }), l);
        b.addView(acts);
        sh.sheet(b);
        paint(); fetch();
    }

    void fetch() {
        sh.a.jsRet("(window.__nrem?window.__nrem(" + JSONObject.quote(k) + "," + JSONObject.quote(id == null ? "" : id) + "," + JSONObject.quote(gid == null ? "" : gid) + "," + JSONObject.quote(sid == null ? "" : sid) + "):'')", new android.webkit.ValueCallback<String>() { public void onReceiveValue(String v) {
            try { JSONObject r = new JSONObject(NSheets.unq(v)); JSONArray n = r.optJSONArray("next"); StringBuilder sb = new StringBuilder();
                nextTxt = n != null && n.length() > 0 ? "Next: " + n.optString(0) : "No reminder coming up";
                String then = ""; for (int i = 1; n != null && i < n.length(); i++) then += (i > 1 ? " · " : "") + n.optString(i);
                if (!then.isEmpty()) nextTxt += "\nThen " + then;
                warn = r.optBoolean("warn") ? "This type is turned off in Settings → Notifications." : ""; ft = r.optString("ft"); loaded = true; paint();
            } catch (Exception ignored) { }
        } });
    }

    /* edit + save + repaint */
    void touch(JSONObject o) {
        long now = System.currentTimeMillis();
        try { o.put("u", now); if (k.equals("step")) { JSONObject g = sh.st.find("goals", gid); if (g != null) g.put("u", now); o.put("reminded", false); } } catch (Exception ignored) { }
        sh.save(); paint(); fetch();
    }
    void set(String f, Object v) { JSONObject o = item(); if (o == null) return; try { o.put(f, v); } catch (Exception ignored) { } touch(o); }
    void notif(boolean on) { if (on) sh.a.js("typeof askNotif==='function'&&askNotif()"); }

    /* ---- pieces ---- */
    interface Pk { void on(String v); }
    void sw(String title, String sub, boolean on, final Pk pk) {
        LinearLayout r = NUi.row(c); r.setPadding(0, NUi.dp(12), 0, NUi.dp(4));
        LinearLayout tx = NUi.col(c); tx.addView(NUi.body(c, title, 15.5f, NTheme.text, 600)); tx.addView(NUi.text(c, sub, 12.5f, NTheme.muted), NUi.mt(2));
        r.addView(tx, NUi.lpw(0, -2, 1));
        final NSettings.Sw w = new NSettings.Sw(c, on); r.addView(w, NUi.lp(NUi.dp(48), NUi.dp(30)));
        NUi.tap(r, new View.OnClickListener() { public void onClick(View v) { NUi.haptic(v); w.on = !w.on; w.invalidate(); pk.on(w.on ? "1" : "0"); } });
        box.addView(r);
    }
    void label(String t) { box.addView(NForms.fieldLabel(c, t)); }
    void chips(String[][] opts, String cur, final Pk pk) {
        NFlow f = new NFlow(c, 8, 8);
        for (final String[] o : opts) f.addView(NUi.chip(c, o[1], o[0].equals(cur), new View.OnClickListener() { public void onClick(View v) { pk.on(o[0]); } }), new ViewGroup.MarginLayoutParams(-2, NUi.dp(40)));
        box.addView(f);
    }
    void timeRow(String label, final String cur, final boolean clear, final Pk pk) {
        LinearLayout r = NUi.row(c); r.setPadding(0, NUi.dp(10), 0, NUi.dp(2));
        r.addView(NUi.text(c, label, 13, NTheme.muted), NUi.lpw(0, -2, 1));
        TextView t = NUi.body(c, cur.isEmpty() ? "None" : NDates.fmtTime(cur), 15, NTheme.accent, 700); r.addView(t);
        NUi.tap(r, new View.OnClickListener() { public void onClick(View v) {
            int h = 9, m = 0; try { h = Integer.parseInt(cur.substring(0, 2)); m = Integer.parseInt(cur.substring(3, 5)); } catch (Exception ignored) { }
            new TimePickerDialog(sh.a, new TimePickerDialog.OnTimeSetListener() { public void onTimeSet(android.widget.TimePicker tp, int hh, int mm) { pk.on(NSettings.hhmm(hh, mm)); } }, h, m, false).show();
        } });
        box.addView(r);
    }
    void dateRow(String label, final String cur, final Pk pk) {
        LinearLayout r = NUi.row(c); r.setPadding(0, NUi.dp(10), 0, NUi.dp(2));
        r.addView(NUi.text(c, label, 13, NTheme.muted), NUi.lpw(0, -2, 1));
        r.addView(NUi.body(c, NDates.valid(cur) ? NDates.fmtDate(cur) : "Pick a date", 15, NTheme.accent, 700));
        NUi.tap(r, new View.OnClickListener() { public void onClick(View v) {
            Calendar x = NDates.valid(cur) ? NDates.cal(cur) : Calendar.getInstance();
            new DatePickerDialog(sh.a, new DatePickerDialog.OnDateSetListener() { public void onDateSet(android.widget.DatePicker p, int y, int m, int d) { Calendar z = Calendar.getInstance(); z.clear(); z.set(y, m, d); pk.on(NDates.ymd(z)); } }, x.get(Calendar.YEAR), x.get(Calendar.MONTH), x.get(Calendar.DAY_OF_MONTH)).show();
        } });
        box.addView(r);
    }

    void paint() {
        if (box == null) return;
        box.removeAllViews();
        final JSONObject o = item(); if (o == null) { box.addView(NUi.text(c, "This item is gone.", 14, NTheme.muted)); return; }
        if (k.equals("habit")) habit(o); else if (k.equals("goal")) goal(o); else stepDay(o);
        LinearLayout nx = NUi.row(c); nx.setBackground(NUi.round(NTheme.surface, 16, NTheme.line)); nx.setPadding(NUi.dp(12), NUi.dp(12), NUi.dp(12), NUi.dp(12)); nx.setGravity(android.view.Gravity.CENTER_VERTICAL);
        nx.addView(NUi.icon(c, "bell", 18, NTheme.accent), NUi.lp(NUi.dp(30), NUi.dp(18)));
        LinearLayout tx = NUi.col(c); tx.addView(NUi.body(c, loaded ? nextTxt.split("\n")[0] : "…", 14.5f, NTheme.text, 700));
        if (loaded && nextTxt.contains("\n")) tx.addView(NUi.text(c, nextTxt.substring(nextTxt.indexOf('\n') + 1), 12.5f, NTheme.muted), NUi.mt(2));
        if (!warn.isEmpty()) tx.addView(NUi.text(c, warn, 12.5f, 0xFFFFB347), NUi.mt(2));
        nx.addView(tx, NUi.lpw(0, -2, 1)); box.addView(nx, NUi.mt(18));
    }

    void habit(final JSONObject h) {
        final boolean on = h.optBoolean("remind");
        sw(on ? "Reminder on" : "Reminder off", on ? "You can change anything below" : "Turn it on to get a nudge", on, new Pk() { public void on(String v) {
            boolean b = v.equals("1"); try { h.put("remind", b); if (b && h.optString("time").isEmpty()) h.put("time", partTime(h)); } catch (Exception ignored) { } notif(b); touch(h); } });
        if (!on) return;
        final String t = h.optString("time").isEmpty() ? partTime(h) : h.optString("time");
        label("Remind me at");
        java.util.TreeSet<String> sug = new java.util.TreeSet<>(); if (!h.optString("time").isEmpty()) sug.add(h.optString("time")); sug.add(partTime(h)); for (String x : new String[]{"07:00", "08:00", "12:30", "18:00", "21:00"}) sug.add(x);
        String[][] so = new String[sug.size()][2]; int i = 0; for (String x : sug) { so[i][0] = x; so[i][1] = NDates.fmtTime(x); i++; }
        chips(so, t, new Pk() { public void on(String v) { set("time", v); } });
        timeRow("Or pick a time", t, false, new Pk() { public void on(String v) { set("time", v); } });
        label("If it’s still not done");
        chips(new String[][]{{"0", "No nudge"}, {"30", "30 min later"}, {"60", "1 h later"}, {"120", "2 h later"}, {"180", "3 h later"}}, String.valueOf(h.optInt("again", 0)), new Pk() { public void on(String v) { set("again", Integer.parseInt(v)); } });
        label("Second reminder");
        chips(new String[][]{{"", "None"}, {"12:30", NDates.fmtTime("12:30")}, {"18:00", NDates.fmtTime("18:00")}, {"21:00", NDates.fmtTime("21:00")}}, h.optString("time2"), new Pk() { public void on(String v) { set("time2", v); } });
        timeRow("Or pick a time", h.optString("time2"), true, new Pk() { public void on(String v) { set("time2", v); } });
        if (!ft.isEmpty()) box.addView(NUi.text(c, "On the days this habit is due · " + ft + ". Change the schedule in Edit.", 12.5f, NTheme.muted), NUi.mt(10));
    }

    String dayRem(JSONObject x) { return x.has("remind") && !x.isNull("remind") ? x.optString("remind") : x.optString("time").isEmpty() ? "" : "0"; }

    void stepDay(final JSONObject o) {
        final boolean day = k.equals("day");
        final String r = day ? dayRem(o) : (o.isNull("remind") ? "" : o.optString("remind", ""));
        final String date = day ? o.optString("date") : o.optString("due"), time = o.optString("time");
        sw(r.isEmpty() ? "Reminder off" : "Reminder on", r.isEmpty() ? "Turn it on to get a nudge" : "You can change anything below", !r.isEmpty(), new Pk() { public void on(String v) {
            boolean b = v.equals("1");
            try { String nr = b ? (time.isEmpty() ? "m" : "0") : ""; o.put("remind", nr); if (nr.equals("c")) { o.put("remDate", date.isEmpty() ? NDates.ymd() : date); o.put("remTime", time.isEmpty() ? "09:00" : time); } } catch (Exception ignored) { } notif(b); touch(o); } });
        dateRow(day ? "Day" : "Due", date, new Pk() { public void on(String v) { set(day ? "date" : "due", v); } });
        timeRow("Time", time, true, new Pk() { public void on(String v) { set("time", v); } });
        if (!time.isEmpty()) { box.addView(NUi.text(c, "Clear time", 12.5f, NTheme.accent), NUi.mt(2)); View cl = box.getChildAt(box.getChildCount() - 1); NUi.tap(cl, new View.OnClickListener() { public void onClick(View v) { set("time", ""); } }); }
        if (r.isEmpty()) return;
        label("When");
        java.util.List<String[]> w = new java.util.ArrayList<>();
        w.add(new String[]{"0", time.isEmpty() ? "At 9:00 AM" : "At " + NDates.fmtTime(time)}); w.add(new String[]{"15", "15 min before"}); w.add(new String[]{"30", "30 min before"}); w.add(new String[]{"60", "1 h before"}); w.add(new String[]{"1440", "1 day before"});
        if (!day) w.add(new String[]{"10080", "1 week before"}); w.add(new String[]{"m", "Morning of"}); w.add(new String[]{"c", "Custom"});
        chips(w.toArray(new String[0][]), r, new Pk() { public void on(String v) {
            try { o.put("remind", v); if (v.equals("c")) { o.put("remDate", o.optString("remDate").isEmpty() ? (date.isEmpty() ? NDates.ymd() : date) : o.optString("remDate")); o.put("remTime", o.optString("remTime").isEmpty() ? (time.isEmpty() ? "09:00" : time) : o.optString("remTime")); } } catch (Exception ignored) { } touch(o); } });
        if (r.equals("c")) {
            dateRow("Remind on", o.optString("remDate").isEmpty() ? (date.isEmpty() ? NDates.ymd() : date) : o.optString("remDate"), new Pk() { public void on(String v) { set("remDate", v); } });
            timeRow("At", o.optString("remTime").isEmpty() ? (time.isEmpty() ? "09:00" : time) : o.optString("remTime"), false, new Pk() { public void on(String v) { set("remTime", v); } });
        }
        if (date.isEmpty() && !r.equals("c")) box.addView(NUi.text(c, "Add a " + (day ? "day" : "due date") + " so the reminder knows when.", 12.5f, 0xFFFFB347), NUi.mt(8));
    }

    void goal(final JSONObject g) {
        label("Check-in");
        chips(new String[][]{{"", "Off"}, {"daily", "Every day"}, {"weekly", "Every week"}}, g.optString("checkin"), new Pk() { public void on(String v) {
            try { g.put("checkin", v); if (!v.isEmpty() && g.optString("checkinTime").isEmpty()) g.put("checkinTime", "19:00"); } catch (Exception ignored) { } if (!v.isEmpty()) notif(true); touch(g); } });
        if (!g.optString("checkin").isEmpty()) {
            timeRow("At", g.optString("checkinTime", "19:00"), false, new Pk() { public void on(String v) { set("checkinTime", v); } });
            if (g.optString("checkin").equals("weekly")) {
                label("On");
                String[][] d = new String[7][2]; for (int i = 0; i < 7; i++) { d[i][0] = String.valueOf(i); d[i][1] = NDates.DAYS[i]; }
                chips(d, String.valueOf(g.optInt("checkinDay", 0)), new Pk() { public void on(String v) { set("checkinDay", Integer.parseInt(v)); } });
            }
        }
        String td = g.optString("targetDate");
        label("Target date" + (td.isEmpty() ? "" : " · " + NDates.fmtDate(td)));
        if (!td.isEmpty()) chips(new String[][]{{"", "Off"}, {"0", "On the day"}, {"1", "1 day before"}, {"7", "1 week before"}, {"30", "1 month before"}}, g.isNull("remTarget") ? "" : g.optString("remTarget", ""), new Pk() { public void on(String v) { set("remTarget", v); if (!v.isEmpty()) notif(true); } });
        else box.addView(NUi.text(c, "Set a target date in Edit to get reminded before it.", 12.5f, NTheme.muted));
        JSONArray st = g.optJSONArray("steps"); java.util.List<JSONObject> open = new java.util.ArrayList<>();
        for (int i = 0; st != null && i < st.length(); i++) { JSONObject s = st.optJSONObject(i); if (s != null && !s.optBoolean("done") && !NStore.isDel(s)) open.add(s); }
        if (!open.isEmpty()) {
            label("Steps");
            boolean undated = false;
            for (int i = 0; i < Math.min(12, open.size()); i++) {
                final JSONObject s = open.get(i);
                LinearLayout r = NUi.row(c); r.setPadding(0, NUi.dp(8), 0, NUi.dp(8)); r.setGravity(android.view.Gravity.CENTER_VERTICAL);
                LinearLayout tx = NUi.col(c); tx.addView(NUi.body(c, s.optString("title"), 14.5f, NTheme.text, 600)); tx.addView(NUi.text(c, s.optString("due").isEmpty() ? "No date" : NDates.fmtDate(s.optString("due")) + (s.optString("time").isEmpty() ? "" : " · " + NDates.fmtTime(s.optString("time"))), 12, NTheme.muted), NUi.mt(2));
                r.addView(tx, NUi.lpw(0, -2, 1));
                boolean on = !(s.isNull("remind") || s.optString("remind", "").isEmpty()) && !s.optString("due").isEmpty();
                r.addView(NUi.icon(c, "bell", 20, on ? NTheme.accent : NTheme.muted), NUi.lp(NUi.dp(24), NUi.dp(20)));
                NUi.tap(r, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); sh.pager.postDelayed(new Runnable() { public void run() {
                    NReminders.openStep(sh, id, s.optString("id"), new Runnable() { public void run() { sh.closeSheet(); sh.pager.postDelayed(new Runnable() { public void run() { NReminders.open(sh, "goal", id); } }, 300); } }); } }, 300); } });
                box.addView(r);
                if (!s.optString("due").isEmpty() && (s.isNull("remind") || s.optString("remind", "").isEmpty())) undated = true;
            }
            if (undated) box.addView(NUi.btn(c, "🔔  Remind me for every dated step", false, new View.OnClickListener() { public void onClick(View v) {
                int n = 0; JSONArray a = g.optJSONArray("steps");
                for (int i = 0; a != null && i < a.length(); i++) { JSONObject s = a.optJSONObject(i); if (s != null && !s.optBoolean("done") && !s.optString("due").isEmpty() && (s.isNull("remind") || s.optString("remind", "").isEmpty())) { try { s.put("remind", "0"); s.put("reminded", false); s.put("u", System.currentTimeMillis()); } catch (Exception ignored) { } n++; } }
                notif(true); try { g.put("u", System.currentTimeMillis()); } catch (Exception ignored) { } sh.save(); paint(); fetch(); NShell.toast("Reminders on for " + n + " step" + (n == 1 ? "" : "s")); } }), NUi.mt(8));
        }
    }
}
