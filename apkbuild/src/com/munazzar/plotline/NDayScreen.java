package com.munazzar.plotline;

import android.app.DatePickerDialog;
import android.view.Gravity;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/* Everything on one date, all editable: habits (counts, routine steps, rest day, slips), activity, day goals, steps,
   journal entries, thread updates, and the reminders, replies and cheers of that day (web vDay). */
final class NDayScreen extends NPage {
    String ds;
    static final java.util.Set<String> DAYX = new java.util.HashSet<>();
    JSONArray reacts; String reactsDay = "";
    NDayScreen(NShell sh, String ds) { super(sh); this.ds = NDates.valid(ds) ? ds : NDates.ymd(); back = true; }

    static final int[] EX_K = {2, 5, 8, 9, 10, 11, 14, 16, 25, 26, 32, 34, 36, 37, 44, 48, 51, 53, 54, 56, 57, 64, 66, 70, 71, 73, 74, 75, 76, 78, 79, 81, 83};
    static final String[] EX_N = {"Badminton", "Basketball", "Cycling", "Cycling", "Boot camp", "Boxing", "Cricket", "Dancing", "Elliptical", "Class", "Golf", "Gymnastics", "HIIT", "Hiking", "Martial arts", "Pilates", "Climbing", "Rowing", "Rowing", "Running", "Treadmill run", "Soccer", "Squash", "Strength", "Stretching", "Swimming", "Swimming", "Table tennis", "Tennis", "Volleyball", "Walking", "Weightlifting", "Yoga"};
    static final String[] EX_E = {"🏸", "🏀", "🚴", "🚴", "💪", "🥊", "🏏", "💃", "💪", "💪", "⛳", "🤸", "🔥", "🥾", "🥋", "🧘", "🧗", "🚣", "🚣", "🏃", "🏃", "⚽", "💪", "🏋️", "🧘", "🏊", "🏊", "🏓", "🎾", "🏐", "🚶", "🏋️", "🧘"};

    static String exName(int k) { for (int i = 0; i < EX_K.length; i++) if (EX_K[i] == k) return EX_N[i]; return "Workout"; }
    static String exIcon(int k) { for (int i = 0; i < EX_K.length; i++) if (EX_K[i] == k) return EX_E[i]; return "💪"; }

    void go(int d) {
        String n = NDates.fromN(NDates.dnum(ds) + d);
        if (n.compareTo(NDates.ymd()) > 0) return;
        ds = n; refresh(); sv.scrollTo(0, 0);
    }

    @Override void build() {
        final String td = NDates.ymd();
        final boolean fut = ds.compareTo(td) > 0;
        int rel = NDates.dnum(ds) - NDates.today();
        List<View> acts = new ArrayList<>();
        acts.add(NUi.ibtn(c, "back", new View.OnClickListener() { public void onClick(View v) { go(-1); } }));
        acts.add(NUi.ibtn(c, "cal", new View.OnClickListener() { public void onClick(View v) { pick(); } }));
        if (ds.compareTo(td) < 0) acts.add(NUi.ibtn(c, "next", new View.OnClickListener() { public void onClick(View v) { go(1); } }));
        acts.add(gear());
        header(rel == 0 ? "Today" : rel == -1 ? "Yesterday" : NDates.weekday(ds), acts.toArray(new View[0]));
        add(NUi.label(c, NDates.longDateYear(ds) + (rel < -1 ? " · " + (-rel) + " days ago" : ""), NTheme.muted), 8);

        final JSONArray habits = st.arr("habits");
        int[] r = NHabits.dayRatio(habits, ds);
        JSONObject log = st.settings().optJSONObject("actLog");
        JSONObject A = log == null ? null : log.optJSONObject(ds);
        List<JSONObject> ents = new ArrayList<>();
        for (JSONObject e : NStore.list(st.arr("entries"))) if (NDates.ymd(e.optLong("t")).equals(ds)) ents.add(e);
        java.util.Collections.sort(ents, new java.util.Comparator<JSONObject>() { public int compare(JSONObject a, JSONObject b) { return Long.compare(a.optLong("t"), b.optLong("t")); } });
        List<JSONObject> moods = new ArrayList<>(); for (JSONObject e : ents) if (!NStore.s(e, "mood").isEmpty()) moods.add(e);
        List<JSONObject> dg = NActs.daysOn(st, ds);
        final List<JSONObject> notes = notes(ds, null);
        if (!ds.equals(reactsDay)) { reacts = null; loadReacts(); }
        final List<JSONObject> rxDay = reactsFor(null);

        /* summary chips */
        LinearLayout sum = NUi.row(c);
        if (r[0] > 0) chip(sum, "✅", r[1] + "/" + r[0], "habits");
        if (A != null && A.optInt("s") > 0) chip(sum, "👟", String.format(java.util.Locale.US, "%,d", A.optInt("s")), "steps");
        if (A != null && A.optInt("z") > 0) chip(sum, "😴", NDates.fmtMin(A.optInt("z")), "sleep");
        if (A != null && A.optInt("x") > 0) chip(sum, "🏋️", NDates.fmtMin(A.optInt("x")), "workout");
        if (A != null && A.optInt("m") > 0) chip(sum, "📱", NDates.fmtMin(A.optInt("m")), "screen");
        if (!moods.isEmpty()) chip(sum, NStore.s(moods.get(moods.size() - 1), "mood"), String.valueOf(moods.size()), "mood");
        if (!ents.isEmpty()) chip(sum, "📓", String.valueOf(ents.size()), "entries");
        if (!notes.isEmpty()) chip(sum, "🔔", String.valueOf(notes.size()), "reminders");
        if (!rxDay.isEmpty()) chip(sum, "👏", String.valueOf(rxDay.size()), "cheers");
        if (!dg.isEmpty()) { int d = 0; for (JSONObject x : dg) if (x.optBoolean("done")) d++; chip(sum, "🎯", d + "/" + dg.size(), "day goals"); }
        if (sum.getChildCount() > 0) add(NBits.hscroll(c, sum), 16);

        /* habits */
        add(NUi.sectionHead(c, "Habits", null, null));
        if (r[0] > 0) { TextView k = NUi.label(c, r[1] + " of " + r[0] + " kept", NTheme.muted); k.setPadding(0, 0, 0, NUi.dp(8)); add(k, -6); }
        List<JSONObject> hs = new ArrayList<>();
        int k0 = NDates.dnum(ds);
        for (JSONObject h : NStore.list(habits)) {
            if (NHabits.status(h).equals("archived")) continue;
            boolean ok = NHabits.kind(h).equals("quit") ? NDates.dnum(NDates.ymd(h.optLong("start", System.currentTimeMillis()))) <= k0 : NHabits.due(h, ds) || NHabits.val(h, ds) > 0 || NHabits.skip(h, ds);
            if (ok) hs.add(h);
        }
        if (hs.isEmpty()) add(muted("No habits were due on this day."));
        else {
            LinearLayout box = NBits.listBox(c);
            for (int i = 0; i < hs.size(); i++) { if (i > 0) box.addView(NBits.divider(c)); box.addView(habitRow(hs.get(i), fut)); }
            add(box);
        }
        if (fut) add(muted("This day hasn’t happened yet."), 8);

        /* activity */
        if (A != null) activity(A);

        /* goals for the day */
        if (!dg.isEmpty() || !fut) {
            add(NUi.sectionHead(c, "Goals for the day", "Add", new View.OnClickListener() { public void onClick(View v) { new NForms(sh).dayGoal(ds); } }));
            if (dg.isEmpty()) add(muted("No goals were set for this day."));
            else {
                LinearLayout box = NBits.listBox(c);
                for (int i = 0; i < dg.size(); i++) {
                    final JSONObject x = dg.get(i);
                    if (i > 0) box.addView(NBits.divider(c));
                    View chk = NBits.check(c, x.optBoolean("done"), NTheme.accent, new View.OnClickListener() { public void onClick(View v) { NActs.toggleDay(x); sh.save(); } });
                    JSONObject g = st.find("goals", NStore.s(x, "goalId"));
                    String sub = (x.optString("time").isEmpty() ? "" : NDates.fmtTime(x.optString("time"))) + (g != null ? (x.optString("time").isEmpty() ? "" : " · ") + g.optString("title") : "");
                    LinearLayout row = NBits.row(c, chk, x.optString("title"), sub, NTheme.muted, null, x.optBoolean("done"));
                    NUi.tap(row, new View.OnClickListener() { public void onClick(View v) { new NForms(sh).dayGoal(ds, x); } });
                    box.addView(row);
                }
                add(box);
            }
        }

        /* steps */
        List<JSONObject> sd = new ArrayList<>(); for (JSONObject e : ents) if ("step".equals(e.optString("type"))) sd.add(e);
        List<JSONObject[]> due = new ArrayList<>();
        for (JSONObject[] gs : NActs.stepsOn(st, ds)) if (!gs[1].optBoolean("done")) due.add(gs);
        if (!sd.isEmpty() || !due.isEmpty()) {
            add(NUi.sectionHead(c, "Steps", null, null));
            LinearLayout box = NBits.listBox(c);
            for (JSONObject e : sd) {
                if (box.getChildCount() > 0) box.addView(NBits.divider(c));
                final JSONObject g = st.find("goals", NStore.s(e, "goalId"));
                View chk = NBits.check(c, true, g == null ? NTheme.accent : NTheme.areaCol(g.optString("area")), new View.OnClickListener() { public void onClick(View v) { } });
                LinearLayout row = NBits.row(c, chk, e.optString("text", "Step"), (g == null ? "" : g.optString("title") + " · ") + NDates.fmtClock(e.optLong("t")), NTheme.muted, null, false);
                if (g != null) NUi.tap(row, new View.OnClickListener() { public void onClick(View v) { sh.push(new NGoalScreen(sh, g.optString("id"))); } });
                box.addView(row);
            }
            for (final JSONObject[] gs : due) {
                if (box.getChildCount() > 0) box.addView(NBits.divider(c));
                View chk = NBits.check(c, false, NTheme.areaCol(gs[0].optString("area")), new View.OnClickListener() { public void onClick(View v) { String m = NActs.toggleStep(st, gs[0], gs[1]); sh.save(); if (m != null) NShell.toast(m); } });
                LinearLayout row = NBits.row(c, chk, gs[1].optString("title"), gs[0].optString("title") + " · was due this day", NTheme.muted, null, false);
                NUi.tap(row, new View.OnClickListener() { public void onClick(View v) { sh.push(new NGoalScreen(sh, gs[0].optString("id"))); } });
                box.addView(row);
            }
            add(box);
        }

        /* journal */
        List<JSONObject> jl = new ArrayList<>(); for (JSONObject e : ents) { String t = e.optString("type"); if (!t.equals("step") && !t.equals("focus")) jl.add(e); }
        add(NUi.sectionHead(c, "Journal", fut ? null : "Write", new View.OnClickListener() { public void onClick(View v) { new NForms(sh).entry(null, null, ds); } }));
        if (jl.isEmpty()) add(muted("No journal entries on this day."));
        else {
            LinearLayout box = NBits.listBox(c);
            for (int i = 0; i < jl.size(); i++) { if (i > 0) box.addView(NBits.divider(c)); box.addView(NJournalPage.entryRow(sh, jl.get(i))); }
            add(box);
        }

        /* thread updates */
        List<JSONObject[]> tu = new ArrayList<>();
        for (JSONObject t : NStore.list(st.arr("threads"))) {
            JSONArray ups = t.optJSONArray("ups"); if (ups == null) continue;
            for (int i = 0; i < ups.length(); i++) { JSONObject u = ups.optJSONObject(i); if (u != null && !NStore.isDel(u) && NDates.ymd(u.optLong("t")).equals(ds)) tu.add(new JSONObject[]{t, u}); }
        }
        if (!tu.isEmpty()) {
            add(NUi.sectionHead(c, "Threads", null, null));
            LinearLayout box = NBits.listBox(c);
            for (int i = 0; i < tu.size(); i++) {
                final JSONObject t = tu.get(i)[0], u = tu.get(i)[1];
                if (i > 0) box.addView(NBits.divider(c));
                TextView em = NUi.text(c, NForms.tagEmoji(u.optString("k")), 20, NTheme.text); em.setLayoutParams(NUi.lp(NUi.dp(30), -2));
                LinearLayout row = NBits.row(c, em, u.optString("x").replace('\n', ' '), t.optString("title") + " · " + NDates.fmtClock(u.optLong("t")), NTheme.muted, null, false);
                NUi.tap(row, new View.OnClickListener() { public void onClick(View v) { sh.push(new NThreadScreen(sh, t.optString("id"))); } });
                box.addView(row);
            }
            add(box);
        }

        /* reminders, replies and cheers */
        if (!notes.isEmpty() || !rxDay.isEmpty()) {
            add(NUi.sectionHead(c, "Reminders, replies and cheers", null, null));
            if (!notes.isEmpty()) add(chat(notes), 4);
            if (!rxDay.isEmpty()) add(cheers(rxDay), 10);
        } else {
            add(NUi.sectionHead(c, "Reminders and replies", null, null));
            add(muted("No reminders on this day. The phone keeps the latest 400."));
        }
    }

    void chip(LinearLayout row, String e, String v, String l) {
        LinearLayout.LayoutParams p = NUi.lp(-2, -2); p.rightMargin = NUi.dp(8);
        row.addView(NBits.chipStat(c, e, v, l), p);
    }

    void pick() {
        DatePickerDialog d = new DatePickerDialog(c, new DatePickerDialog.OnDateSetListener() { public void onDateSet(DatePicker p, int y, int m, int dd) {
            String n = String.format(java.util.Locale.US, "%04d-%02d-%02d", y, m + 1, dd);
            if (n.compareTo(NDates.ymd()) > 0) n = NDates.ymd();
            ds = n; refresh(); sv.scrollTo(0, 0);
        } }, NDates.year(ds), NDates.month(ds), NDates.day(ds));
        d.getDatePicker().setMaxDate(System.currentTimeMillis());
        d.show();
    }

    /* ---- one habit on this day ---- */
    View habitRow(final JSONObject h, final boolean fut) {
        LinearLayout wrap = NUi.col(c);
        LinearLayout r = NUi.row(c);
        r.setPadding(NUi.dp(14), NUi.dp(10), NUi.dp(12), NUi.dp(10));
        TextView ic = NUi.text(c, NHabits.icon(h), 20, NTheme.text); ic.setGravity(Gravity.CENTER);
        ic.setBackground(NUi.round(NTheme.alpha(NTheme.areaCol(h.optString("area")), .18f), 12, 0));
        r.addView(ic, NUi.lp(NUi.dp(40), NUi.dp(40)));
        LinearLayout mid = NUi.col(c); mid.setPadding(NUi.dp(12), 0, NUi.dp(8), 0);
        mid.addView(NUi.ell(NUi.body(c, h.optString("title"), 16, NTheme.text, 700), 2));
        final int v = NHabits.val(h, ds), n = NHabits.target(h);
        final boolean dn = NHabits.done(h, ds), sk = NHabits.skip(h, ds), pz = NHabits.paused(h, ds), quit = NHabits.kind(h).equals("quit");
        String st1; int stCol = NTheme.muted;
        LinearLayout ctl = NUi.row(c);
        if (quit && NHabits.limitMode(h)) {
            JSONObject lg = h.optJSONObject("log"); final int cnt = lg == null ? 0 : lg.optInt(ds, 0);
            st1 = cnt + " of " + NHabits.limit(h) + (cnt > NHabits.limit(h) ? " · over" : "");
            if (!fut) { ctl.addView(NBits.sbtn(c, "minus", new View.OnClickListener() { public void onClick(View x) { lim(h, -1); } })); LinearLayout.LayoutParams l = NUi.lp(NUi.dp(36), NUi.dp(36)); l.leftMargin = NUi.dp(6); ctl.addView(NBits.sbtn(c, "plus", new View.OnClickListener() { public void onClick(View x) { lim(h, 1); } }), l); }
        } else if (quit) {
            int sl = slipsOn(h).size();
            st1 = sl > 0 ? sl + " slip" + (sl > 1 ? "s" : "") : "Clean";
            TextView pill = NUi.body(c, sl > 0 ? "Slip" : "Clean", 13, sl > 0 ? NTheme.LATE : NTheme.text, 700);
            pill.setPadding(NUi.dp(12), NUi.dp(6), NUi.dp(12), NUi.dp(6)); pill.setBackground(NUi.round(NTheme.alpha(sl > 0 ? NTheme.LATE : NTheme.accent, .16f), 99, 0));
            ctl.addView(pill);
        } else {
            st1 = pz ? "Paused" : sk ? "Rest day" : n > 1 ? v + " of " + n + (h.optString("unit").isEmpty() ? "" : " " + h.optString("unit")) : dn ? "Done" : "Not done";
            if (!fut && !pz) {
                if (n > 1 && !NHabits.kind(h).equals("routine")) {
                    ctl.addView(NBits.sbtn(c, "minus", new View.OnClickListener() { public void onClick(View x) { adj(h, -1); } }));
                    LinearLayout.LayoutParams l = NUi.lp(NUi.dp(36), NUi.dp(36)); l.leftMargin = NUi.dp(6);
                    ctl.addView(NBits.sbtn(c, "plus", new View.OnClickListener() { public void onClick(View x) { adj(h, 1); } }), l);
                }
                LinearLayout.LayoutParams l = NUi.lp(NUi.dp(40), NUi.dp(40)); l.leftMargin = NUi.dp(8);
                ctl.addView(NBits.check(c, dn, NTheme.areaCol(h.optString("area")), new View.OnClickListener() { public void onClick(View x) {
                    NHabits.setVal(h, ds, v >= n ? 0 : n);
                    if (v < n) NActs.milestone(st, h);
                    sh.save();
                } }), l);
            }
        }
        /* what else happened with this habit on the day (web dyHabitRow) */
        final String hid = h.optString("id");
        final List<JSONObject> hn = notes(ds, hid), hr = reactsFor(hid);
        final List<JSONObject> he = new ArrayList<>();
        for (JSONObject e : NStore.list(st.arr("entries"))) if (NDates.ymd(e.optLong("t")).equals(ds) && hid.equals(entryHabit(e))) he.add(e);
        final List<JSONObject[]> htu = new ArrayList<>();
        for (JSONObject t : NStore.list(st.arr("threads"))) {
            JSONObject lk = t.optJSONObject("link"); if (lk == null || !"habit".equals(lk.optString("k")) || !hid.equals(lk.optString("id"))) continue;
            JSONArray ups = t.optJSONArray("ups"); for (int k = 0; ups != null && k < ups.length(); k++) { JSONObject u = ups.optJSONObject(k); if (u != null && !NStore.isDel(u) && NDates.ymd(u.optLong("t")).equals(ds)) htu.add(new JSONObject[]{t, u}); }
        }
        String extra = (hn.isEmpty() ? "" : " · 🔔 " + hn.size()) + (hr.isEmpty() ? "" : " · 👏 " + hr.size()) + (he.isEmpty() ? "" : " · 📓 " + he.size()) + (htu.isEmpty() ? "" : " · 🧵 " + htu.size());
        mid.addView(NUi.ell(NUi.text(c, st1 + extra, 13, stCol), 1), NUi.mt(2));
        r.addView(mid, NUi.lpw(0, -2, 1));
        final View chev = NUi.icon(c, "down", 16, NTheme.muted); chev.setRotation(DAYX.contains(hid) ? 180 : 0);
        LinearLayout.LayoutParams cvl = NUi.lp(NUi.dp(16), NUi.dp(16)); cvl.rightMargin = NUi.dp(8); r.addView(chev, cvl);
        r.addView(ctl);
        wrap.addView(r);
        final LinearLayout hb = NUi.col(c); hb.setPadding(NUi.dp(66), 0, NUi.dp(14), NUi.dp(10));
        hb.setVisibility(DAYX.contains(hid) ? View.VISIBLE : View.GONE);
        NUi.tap(r, new View.OnClickListener() { public void onClick(View x) {
            boolean o = !DAYX.contains(hid); if (o) DAYX.add(hid); else DAYX.remove(hid);
            hb.setVisibility(o ? View.VISIBLE : View.GONE); chev.animate().rotation(o ? 180 : 0).setDuration(200).start();
        } });
        wrap.addView(hb);

        /* routine steps */
        if (NHabits.kind(h).equals("routine") && NHabits.steps(h).length() > 0) {
            JSONArray stp = NHabits.steps(h); JSONObject rsAll = NHabits.obj(h, "rs"); JSONArray rd = rsAll.optJSONArray(ds);
            for (int i = 0; i < stp.length(); i++) {
                final JSONObject sp = stp.optJSONObject(i); if (sp == null) continue;
                boolean on = false; if (rd != null) for (int j = 0; j < rd.length(); j++) if (sp.optString("id").equals(rd.optString(j))) on = true;
                LinearLayout row = NUi.row(c); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(0, NUi.dp(4), 0, NUi.dp(4));
                View chk = NBits.check(c, on, NTheme.areaCol(h.optString("area")), new View.OnClickListener() { public void onClick(View x) { toggleRoutineStep(h, sp.optString("id")); } });
                if (fut) chk.setEnabled(false);
                LinearLayout.LayoutParams cl = NUi.lp(NUi.dp(28), NUi.dp(28)); cl.rightMargin = NUi.dp(12);
                row.addView(chk, cl);
                row.addView(NUi.text(c, sp.optString("title"), 14, on ? NTheme.muted : NTheme.text), NUi.lpw(0, -2, 1));
                if (sp.optInt("min") > 0) row.addView(NUi.label(c, sp.optInt("min") + " min", NTheme.muted));
                hb.addView(row);
            }
        }
        /* slips and urges */
        if (quit && !NHabits.limitMode(h)) {
            hb.addView(sub("Slips"));
            List<JSONObject> sl = slipsOn(h);
            if (sl.isEmpty()) hb.addView(NUi.text(c, "No slips. A clean day.", 13, NTheme.muted));
            for (final JSONObject s : sl) {
                LinearLayout row = NUi.row(c); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(0, NUi.dp(2), 0, NUi.dp(2));
                TextView tx = NUi.text(c, "", 14, NTheme.text);
                tx.setText(android.text.Html.fromHtml("<b>" + NDates.fmtClock(s.optLong("t")) + "</b>" + (s.optString("trig").isEmpty() ? "" : " · " + android.text.TextUtils.htmlEncode(s.optString("trig"))) + (s.optString("note").isEmpty() ? "" : " · " + android.text.TextUtils.htmlEncode(s.optString("note"))), android.text.Html.FROM_HTML_MODE_LEGACY));
                row.addView(tx, NUi.lpw(0, -2, 1));
                row.addView(NBits.sbtn(c, "trash", new View.OnClickListener() { public void onClick(View x) { delSlip(h, s); } }));
                hb.addView(row);
            }
            if (!fut) { TextView add = NUi.link(c, "Log a slip", new View.OnClickListener() { public void onClick(View x) { addSlip(h); } }); add.setPadding(0, NUi.dp(6), 0, 0); hb.addView(add); }
            JSONArray ur = h.optJSONArray("urges"); List<JSONObject> ul = new ArrayList<>();
            for (int k = 0; ur != null && k < ur.length(); k++) { JSONObject u = ur.optJSONObject(k); if (u != null && u.optLong("t") > 0 && NDates.ymd(u.optLong("t")).equals(ds)) ul.add(u); }
            if (!ul.isEmpty()) { hb.addView(sub("Urges")); for (JSONObject u : ul) { TextView t = NUi.text(c, "", 14, NTheme.text); t.setText(android.text.Html.fromHtml("<b>" + NDates.fmtClock(u.optLong("t")) + "</b> · " + (NUrge.ok(u) ? "rode it out" : "gave in"), android.text.Html.FROM_HTML_MODE_LEGACY)); hb.addView(t); } }
        }
        /* rest day + open habit */
        NFlow acts = new NFlow(c, 8, 8);
        if (!quit && !fut && !pz) acts.addView(NUi.btn(c, sk ? "Remove rest day" : "Make it a rest day", false, new View.OnClickListener() { public void onClick(View x) {
            try { if (sk) NHabits.obj(h, "skip").remove(ds); else { NHabits.setVal(h, ds, 0); NHabits.obj(h, "skip").put(ds, 1); } } catch (Exception ignored) { }
            sh.save();
        } }), new android.view.ViewGroup.MarginLayoutParams(-2, NUi.dp(38)));
        acts.addView(NUi.btn(c, "Open habit", false, new View.OnClickListener() { public void onClick(View x) { sh.push(new NHabitScreen(sh, hid)); } }), new android.view.ViewGroup.MarginLayoutParams(-2, NUi.dp(38)));
        hb.addView(acts, NUi.mt(10));
        if (!hn.isEmpty()) { hb.addView(sub("Reminders and replies")); hb.addView(chat(hn)); }
        if (!hr.isEmpty()) { hb.addView(sub("Cheers")); hb.addView(cheers(hr)); }
        if (!he.isEmpty()) { hb.addView(sub("Journal")); for (final JSONObject e : he) hb.addView(entBtn((NStore.s(e, "mood").isEmpty() ? "" : NStore.s(e, "mood") + " ") + NForms.trunc(!e.optString("title").isEmpty() ? e.optString("title") : e.optString("text", "Entry"), 80), NDates.fmtClock(e.optLong("t")), new View.OnClickListener() { public void onClick(View v) { NEng.viewEntry(sh, e.optString("id")); } })); }
        if (!htu.isEmpty()) { hb.addView(sub("Thread updates")); for (final JSONObject[] tu : htu) hb.addView(entBtn(NForms.tagEmoji(tu[1].optString("k")) + " " + NForms.trunc(tu[1].optString("x"), 80), NForms.trunc(tu[0].optString("title"), 30) + " · " + NDates.fmtClock(tu[1].optLong("t")), new View.OnClickListener() { public void onClick(View v) { sh.push(new NThreadScreen(sh, tu[0].optString("id"))); } })); }
        return wrap;
    }

    /* web entryHabit: the habit an entry belongs to */
    String entryHabit(JSONObject e) {
        String hid = NStore.s(e, "hid"); if (!hid.isEmpty() && st.find("habits", hid) != null) return hid;
        if ("habit".equals(e.optString("type")) && !e.optString("text").isEmpty()) for (JSONObject h : NStore.list(st.arr("habits"))) if (e.optString("text").endsWith("· " + h.optString("title"))) return h.optString("id");
        return "";
    }

    TextView sub(String t) { TextView v = NUi.label(c, t.toUpperCase(), NTheme.muted); v.setTextSize(11); v.setLetterSpacing(.06f); v.setPadding(0, NUi.dp(16), 0, NUi.dp(6)); return v; }

    View entBtn(String t, String small, View.OnClickListener l) {
        LinearLayout b = NUi.row(c); b.setGravity(Gravity.CENTER_VERTICAL); b.setPadding(NUi.dp(12), NUi.dp(10), NUi.dp(12), NUi.dp(10));
        b.setBackground(NUi.ripple(NUi.round(NTheme.surface2, 14, NTheme.line), 14));
        b.addView(NUi.ell(NUi.text(c, t, 14, NTheme.text), 2), NUi.lpw(0, -2, 1));
        TextView s = NUi.text(c, small, 11.5f, NTheme.muted); s.setPadding(NUi.dp(10), 0, 0, 0); b.addView(s);
        NUi.tap(b, l);
        LinearLayout.LayoutParams lp = NUi.mt(6); b.setLayoutParams(lp);
        return b;
    }

    /* ---- reminders, replies (the phone's notification log) and cheers ---- */
    static final String[][] NH_TXT = {{"sent", "Reminder sent"}, {"again", "Reminded again, as you asked"}, {"held", "Held while muted"}, {"done", "Marked done from the notification"}, {"mute", "Muted with the button"}, {"reply", "You replied"}, {"reply-ai", "You replied"}, {"undo", "Undone from the notification"}};
    static String nhTxt(String k) { for (String[] x : NH_TXT) if (x[0].equals(k)) return x[1]; return k; }

    List<JSONObject> notes(String day, String hid) {
        List<JSONObject> out = new ArrayList<>();
        try {
            JSONArray L = new JSONArray(Notify.history(c, "*"));
            for (int i = 0; i < L.length(); i++) {
                JSONObject e = L.optJSONObject(i); if (e == null || e.optLong("t") <= 0 || !NDates.ymd(e.optLong("t")).equals(day)) continue;
                if (hid != null) { boolean hit = ("h:" + hid).equals(e.optString("key")); JSONArray al = e.optJSONArray("also"); for (int k = 0; !hit && al != null && k < al.length(); k++) if (("h:" + hid).equals(al.optString(k))) hit = true; if (!hit) continue; }
                out.add(e);
            }
        } catch (Exception ignored) { }
        java.util.Collections.sort(out, new java.util.Comparator<JSONObject>() { public int compare(JSONObject a, JSONObject b) { return Long.compare(a.optLong("t"), b.optLong("t")); } });
        return out;
    }

    void loadReacts() {
        final String d = ds;
        sh.a.jsRet("(window.__ndyreacts?window.__ndyreacts(" + JSONObject.quote(d) + "):'[]')", new android.webkit.ValueCallback<String>() { public void onReceiveValue(String v) {
            try { JSONArray a = new JSONArray(NSheets.unq(v)); boolean ch = reacts == null ? a.length() > 0 : !a.toString().equals(reacts.toString()); reacts = a; reactsDay = d; if (ch && d.equals(ds)) refresh(); } catch (Exception ignored) { }
        } });
    }

    List<JSONObject> reactsFor(String hid) {
        List<JSONObject> out = new ArrayList<>(); if (reacts == null) return out;
        for (int i = 0; i < reacts.length(); i++) { JSONObject r = reacts.optJSONObject(i); if (r != null && (hid == null ? r.optString("hid").isEmpty() : hid.equals(r.optString("hid")))) out.add(r); }
        return out;
    }

    View chat(List<JSONObject> L) {
        LinearLayout col = NUi.col(c);
        for (JSONObject e : L) {
            String kind = e.optString("kind"), txt = nhTxt(kind), ttl = e.optString("title");
            LinearLayout it = NUi.col(c);
            LinearLayout.LayoutParams il = NUi.lp(-2, -2); il.topMargin = NUi.dp(8);
            if (kind.startsWith("reply")) {
                TextView b = NUi.text(c, e.optString("text"), 14, NTheme.bg); b.setPadding(NUi.dp(13), NUi.dp(9), NUi.dp(13), NUi.dp(9)); b.setBackground(bubble(true)); b.setMaxWidth(c.getResources().getDisplayMetrics().widthPixels * 7 / 10);
                it.addView(b); it.setGravity(Gravity.END);
                TextView s = NUi.text(c, NDates.fmtClock(e.optLong("t")) + (e.optString("res").isEmpty() ? "" : " · " + e.optString("res")), 11.5f, NTheme.muted); s.setPadding(NUi.dp(4), NUi.dp(3), NUi.dp(4), 0); it.addView(s);
                il.gravity = Gravity.END;
            } else if (kind.equals("sent") || kind.equals("again") || kind.equals("held")) {
                TextView b = NUi.text(c, "", 14, NTheme.text); b.setText(android.text.Html.fromHtml("<b>" + android.text.TextUtils.htmlEncode(ttl.isEmpty() ? txt : ttl) + "</b>" + (e.optString("text").isEmpty() ? "" : "<br>" + android.text.TextUtils.htmlEncode(e.optString("text"))), android.text.Html.FROM_HTML_MODE_LEGACY));
                b.setPadding(NUi.dp(13), NUi.dp(9), NUi.dp(13), NUi.dp(9)); b.setBackground(bubble(false)); b.setMaxWidth(c.getResources().getDisplayMetrics().widthPixels * 7 / 10); it.addView(b);
                TextView s = NUi.text(c, txt + " · " + NDates.fmtClock(e.optLong("t")), 11.5f, NTheme.muted); s.setPadding(NUi.dp(4), NUi.dp(3), NUi.dp(4), 0); it.addView(s);
            } else {
                TextView s = NUi.text(c, txt + (ttl.isEmpty() ? "" : " · " + ttl) + (e.optString("text").isEmpty() ? "" : " · " + e.optString("text")) + (e.optString("res").isEmpty() ? "" : " · " + e.optString("res")) + " · " + NDates.fmtClock(e.optLong("t")), 11.5f, NTheme.muted);
                s.setGravity(Gravity.CENTER); it.addView(s); il = NUi.lp(-1, -2); il.topMargin = NUi.dp(8);
            }
            col.addView(it, il);
        }
        return col;
    }

    android.graphics.drawable.GradientDrawable bubble(boolean me) {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        float r = NUi.dp(16), s = NUi.dp(5);
        g.setCornerRadii(me ? new float[]{r, r, r, r, s, s, r, r} : new float[]{r, r, r, r, r, r, s, s});
        g.setColor(me ? NTheme.accent : NTheme.surface2); if (!me) g.setStroke(NUi.dp(1), NTheme.line);
        return g;
    }

    View cheers(List<JSONObject> L) {
        LinearLayout col = NUi.col(c);
        for (JSONObject r : L) {
            LinearLayout row = NUi.row(c); row.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(NUi.text(c, r.optString("e"), 22, NTheme.text), NUi.lp(NUi.dp(34), -2));
            LinearLayout tx = NUi.col(c);
            TextView t = NUi.text(c, "", 14, NTheme.text);
            t.setText(android.text.Html.fromHtml("<b>" + android.text.TextUtils.htmlEncode((r.optInt("me") == 1 ? "You → " : "") + r.optString("who")) + "</b>" + (r.optString("m").isEmpty() ? "" : " “" + android.text.TextUtils.htmlEncode(r.optString("m")) + "”"), android.text.Html.FROM_HTML_MODE_LEGACY));
            tx.addView(t);
            tx.addView(NUi.text(c, NForms.trunc(r.optString("item"), 30) + " · " + NDates.fmtClock(r.optLong("t")), 11.5f, NTheme.muted));
            row.addView(tx, NUi.lpw(0, -2, 1));
            col.addView(row, NUi.mt(8));
        }
        return col;
    }

    List<JSONObject> slipsOn(JSONObject h) {
        List<JSONObject> l = new ArrayList<>(); JSONArray sl = h.optJSONArray("slips");
        if (sl != null) for (int i = 0; i < sl.length(); i++) { JSONObject s = sl.optJSONObject(i); if (s != null && NDates.ymd(s.optLong("t")).equals(ds)) l.add(s); }
        return l;
    }

    void adj(JSONObject h, int d) { NHabits.setVal(h, ds, NHabits.val(h, ds) + d); NActs.milestone(st, h); sh.save(); }

    void lim(JSONObject h, int d) {
        try { JSONObject lg = NHabits.obj(h, "log"); int v = Math.max(0, lg.optInt(ds, 0) + d); if (v > 0) lg.put(ds, v); else lg.remove(ds); } catch (Exception ignored) { }
        sh.save();
    }

    void toggleRoutineStep(JSONObject h, String sid) {
        try {
            JSONObject rs = NHabits.obj(h, "rs"); JSONArray a = rs.optJSONArray(ds); JSONArray n = new JSONArray(); boolean had = false;
            if (a != null) for (int i = 0; i < a.length(); i++) { if (sid.equals(a.optString(i))) had = true; else n.put(a.optString(i)); }
            if (!had) n.put(sid);
            if (n.length() == 0) rs.remove(ds); else rs.put(ds, n);
            if (n.length() > 0) NHabits.obj(h, "skip").remove(ds);
            if (NHabits.done(h, ds)) NActs.milestone(st, h);
        } catch (Exception ignored) { }
        sh.save();
    }

    void delSlip(final JSONObject h, final JSONObject s) {
        LinearLayout b = new NForms(sh).sheetBody(h.optString("title"), "Remove this slip?", "Your clean-day count is worked out again without it.");
        b.addView(NForms.actions(c, NUi.btn(c, "Cancel", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } }),
            NUi.btn(c, "Remove", true, new View.OnClickListener() { public void onClick(View v) {
                JSONArray sl = h.optJSONArray("slips");
                try { if (sl != null) for (int i = sl.length() - 1; i >= 0; i--) if (sl.optJSONObject(i) != null && sl.optJSONObject(i).optLong("t") == s.optLong("t")) sl.remove(i); h.put("mile", 0); } catch (Exception ignored) { }
                sh.closeSheet(); sh.save(); NShell.toast("Slip removed");
            } })));
        sh.sheet(b);
    }

    void addSlip(final JSONObject h) {
        LinearLayout b = new NForms(sh).sheetBody(h.optString("title"), "Add a slip", NDates.longDate(ds));
        b.addView(NForms.fieldLabel(c, "Time"));
        final String[] tm = {"12:00"};
        b.addView(NForms.timePick(c, tm, "12:00"));
        b.addView(NForms.fieldLabel(c, "Note"));
        final EditText note = NForms.input(c, "Optional", "", false); b.addView(note);
        b.addView(NForms.actions(c, NUi.btn(c, "Cancel", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } }),
            NUi.btn(c, "Add", true, new View.OnClickListener() { public void onClick(View v) {
                try {
                    String[] p = (tm[0] == null || tm[0].isEmpty() ? "12:00" : tm[0]).split(":");
                    java.util.Calendar k = NDates.cal(ds); k.set(java.util.Calendar.HOUR_OF_DAY, Integer.parseInt(p[0])); k.set(java.util.Calendar.MINUTE, Integer.parseInt(p[1]));
                    if (k.getTimeInMillis() > System.currentTimeMillis()) { NShell.toast("That time hasn’t happened yet"); return; }
                    JSONObject s = new JSONObject(); s.put("t", k.getTimeInMillis()); s.put("trig", ""); s.put("note", note.getText().toString().trim());
                    NHabits.arr(h, "slips").put(s); h.put("mile", 0);
                } catch (Exception ignored) { }
                sh.closeSheet(); sh.save(); NShell.toast("Slip logged");
            } })));
        sh.sheet(b);
    }

    /* ---- activity recorded by the phone ---- */
    void activity(JSONObject A) {
        add(NUi.sectionHead(c, "Activity", "All activity", new View.OnClickListener() { public void onClick(View v) { sh.push(new NActivityScreen(sh)); } }));
        JSONObject set = st.settings().optJSONObject("act");
        int goal = set == null ? 8000 : set.optInt("goal", 8000); double sleepG = set == null ? 7.5 : set.optDouble("sleep", 7.5);
        GridLayout g = new GridLayout(c); g.setColumnCount(2);
        int s = A.optInt("s", -1), z = A.optInt("z", 0), x = A.optInt("x", 0), m = A.optInt("m", -1);
        tile(g, "👟", s >= 0 ? String.format(java.util.Locale.US, "%,d", s) : "—", "steps", s >= 0 ? s / (float) goal : -1);
        tile(g, "😴", z > 0 ? NDates.fmtMin(z) : "—", "sleep", z > 0 ? z / (float) (sleepG * 60) : -1);
        tile(g, "🏋️", x > 0 ? NDates.fmtMin(x) : "—", "workouts", -1);
        tile(g, "📱", m >= 0 ? NDates.fmtMin(m) : "—", "screen time", -1);
        LinearLayout.LayoutParams gl = NUi.mt(0); gl.leftMargin = gl.rightMargin = -NUi.dp(4);
        body.addView(g, gl);
        LinearLayout box = null;
        JSONArray ss = A.optJSONArray("ss");
        if (ss != null) for (int i = 0; i < ss.length(); i++) { JSONArray p = ss.optJSONArray(i); if (p == null) continue; box = det(box, "😴", "Asleep " + NDates.fmtClock(p.optLong(0)) + " → " + NDates.fmtClock(p.optLong(1)), NDates.fmtMin((int) ((p.optLong(1) - p.optLong(0)) / 60000))); }
        JSONArray w = A.optJSONArray("w");
        if (w != null) for (int i = 0; i < w.length(); i++) { JSONObject o = w.optJSONObject(i); if (o == null) continue; box = det(box, exIcon(o.optInt("k")), exName(o.optInt("k")), NDates.fmtMin(o.optInt("m")) + " · " + NDates.fmtClock(o.optLong("t"))); }
        JSONArray ap = A.optJSONArray("a");
        if (ap != null) for (int i = 0; i < ap.length(); i++) { JSONArray p = ap.optJSONArray(i); if (p == null) continue; box = det(box, "📱", p.optString(0), NDates.fmtMin(p.optInt(1))); }
        JSONObject pl = A.optJSONObject("p");
        if (pl != null) {
            JSONArray places = st.settings().optJSONArray("places");
            for (String id : NHabits.keys(pl)) {
                if (pl.optInt(id) <= 0 || places == null) continue;
                for (int i = 0; i < places.length(); i++) { JSONObject p = places.optJSONObject(i); if (p != null && id.equals(p.optString("id"))) box = det(box, p.optString("emoji", "📍"), p.optString("name"), NDates.fmtMin(pl.optInt(id)) + " there"); }
            }
        }
        if (box != null) add(box, 10);
    }

    LinearLayout det(LinearLayout box, String e, String t, String s) {
        if (box == null) box = NBits.listBox(c); else box.addView(NBits.divider(c));
        TextView em = NUi.text(c, e, 20, NTheme.text); em.setLayoutParams(NUi.lp(NUi.dp(30), -2));
        box.addView(NBits.row(c, em, t, null, NTheme.muted, NBits.meta(c, s, NTheme.muted), false));
        return box;
    }

    void tile(GridLayout g, String emoji, String big, String lab, float prog) {
        LinearLayout t = NUi.col(c);
        t.setBackground(NUi.card(22)); t.setPadding(NUi.dp(16), NUi.dp(14), NUi.dp(16), NUi.dp(14));
        t.addView(NUi.text(c, emoji, 22, NTheme.text));
        TextView b = NUi.text(c, big, 26, NTheme.text); b.setTypeface(NFont.display(800)); t.addView(b, NUi.mt(6));
        t.addView(NUi.ell(NUi.text(c, lab, 13, NTheme.muted), 1));
        if (prog >= 0) t.addView(NBits.bar(c, Math.min(1, prog), NTheme.accent), NUi.mt(8));
        GridLayout.LayoutParams l = new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f));
        l.width = 0; l.setMargins(NUi.dp(4), NUi.dp(4), NUi.dp(4), NUi.dp(4));
        g.addView(t, l);
    }
}
