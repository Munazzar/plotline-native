package com.munazzar.plotline;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.FrameLayout;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/* Habits: the week at a glance, today's habits by part of the day, habits being broken, and the rest. */
final class NHabitsPage extends NPage {
    String filter = "today";
    int calOff = 0;
    boolean showArch = false;
    NHabitsPage(NShell sh) { super(sh); }

    boolean calOn() { JSONObject l = st.settings().optJSONObject("layout"); return l != null && l.optBoolean("hcal"); }

    void newHabit() {
        final NForms F = new NForms(sh);
        LinearLayout b = F.sheetBody("New", "What do you want to work on?", null);
        /* web: three big .tpl rows (emoji in a 46 tile), then every template section */
        String[][] K = {{"build", "🌱", "Build a habit", "Something to do daily or a few times a week"}, {"quit", "🛡️", "Break a habit", "Stop completely, or cut down to a daily limit"}, {"routine", "🔁", "Create a routine", "A short sequence with a guided timer"}};
        for (int i = 0; i < K.length; i++) {
            final String k = K[i][0];
            LinearLayout r = NUi.row(c); r.setGravity(Gravity.CENTER_VERTICAL); r.setPadding(NUi.dp(16), NUi.dp(16), NUi.dp(16), NUi.dp(16));
            r.setBackground(NUi.ripple(NUi.round(NTheme.surface, 18, NTheme.line), 18));
            TextView ic = NUi.text(c, K[i][1], 23, NTheme.text); ic.setGravity(Gravity.CENTER); ic.setIncludeFontPadding(false);
            ic.setBackground(NUi.round(NTheme.alpha(NTheme.accent, .16f), 15, NTheme.alpha(NTheme.accent, .3f))); r.addView(ic, NUi.lp(NUi.dp(46), NUi.dp(46)));
            LinearLayout tx = NUi.col(c); tx.setPadding(NUi.dp(12), 0, 0, 0);
            tx.addView(NUi.body(c, K[i][2], 14.5f, NTheme.text, 600)); tx.addView(NUi.text(c, K[i][3], 12.5f, NTheme.muted));
            r.addView(tx, NUi.lpw(0, -2, 1));
            NUi.tap(r, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); v.postDelayed(new Runnable() { public void run() { NHabitForm.open(sh, null, k, null); } }, 300); } });
            b.addView(r, NUi.mt(i == 0 ? 16 : 10));
        }
        NHabitForm.tplGrid(sh, b, null);
        sh.sheet(b);
    }

    @Override void build() {
        final NForms F = new NForms(sh);
        header("Habits",
            NUi.ibtn(c, "cal", calOn(), new View.OnClickListener() { public void onClick(View v) {
                try { JSONObject l = st.settings().optJSONObject("layout"); if (l == null) { l = new JSONObject(); st.settings().put("layout", l); } l.put("hcal", !calOn()); } catch (Exception ignored) { }
                filter = "today"; sh.save();
            } }),
            NUi.ibtn(c, "plus", new View.OnClickListener() { public void onClick(View v) { newHabit(); } }), gear());
        final String td = NDates.ymd();
        final JSONArray habits = st.arr("habits");
        List<JSONObject> all = new ArrayList<>(), live = new ArrayList<>(), arch = new ArrayList<>();
        for (JSONObject h : NStore.list(habits)) { all.add(h); if (NHabits.status(h).equals("archived")) arch.add(h); else live.add(h); }
        int[] ratio = NHabits.dayRatio(habits, td);
        int quits = 0; for (JSONObject h : live) if (NHabits.kind(h).equals("quit")) quits++;
        add(NUi.label(c, all.isEmpty() ? "Build good ones. Break the rest." : (ratio[0] > 0 ? ratio[1] + " of " + ratio[0] + " done today" : "Nothing due today") + " · " + quits + " being broken", NTheme.muted), 28);   /* web .ph .data: row-gap 14 + margin 14 */

        if (all.isEmpty()) {
            LinearLayout e = NUi.col(c); e.setGravity(Gravity.CENTER_HORIZONTAL);
            e.setBackground(NUi.round(0, 22, NTheme.line2)); e.setPadding(NUi.dp(20), NUi.dp(28), NUi.dp(20), NUi.dp(24));
            TextView em = NUi.text(c, "🌱", 34, NTheme.text); e.addView(em);
            TextView t = NUi.body(c, "Small things, done daily", 18, NTheme.text, 700); t.setPadding(0, NUi.dp(10), 0, 0); e.addView(t);
            TextView s = NUi.text(c, "Build a habit, run a routine, or break one that holds you back. Start from a template or from scratch.", 14, NTheme.muted); s.setGravity(Gravity.CENTER); s.setPadding(0, NUi.dp(6), 0, NUi.dp(14)); e.addView(s);
            e.addView(NUi.btn(c, "New habit", true, new View.OnClickListener() { public void onClick(View v) { newHabit(); } }));
            add(e, 18); return;
        }

        String[][] F2 = {{"today", "Today"}, {"vista", "Vista"}, {"build", "Build"}, {"quit", "Break"}, {"routine", "Routines"}, {"templates", "Templates"}};
        add(NBits.seg(c, F2, filter, new NBits.Pick() { public void on(String k) {
            filter = k; refresh();
        } }), 30);   /* web .ph margin-bottom 30 */

        if (filter.equals("vista")) { add(new NHabitVista(sh, this).card, 4); return; }
        if (filter.equals("templates")) {
            TextView tn = NUi.text(c, "Tap a template, adjust anything, and save. Use them as often as you like.", 13, NTheme.muted); tn.setLineSpacing(0, 1.3f); add(tn, 0);
            NHabitForm.tplGrid(sh, body, null); return;
        }

        if (filter.equals("today")) {
            List<JSONObject> today = NHabits.today(habits);
            weekStrip(habits);
            if (!today.isEmpty()) { summary(habits); nudge(live); }
            String[][] parts = {{"morning", "Morning"}, {"afternoon", "Afternoon"}, {"evening", "Evening"}, {"any", "Anytime"}};
            for (String[] p : parts) {
                List<JSONObject> l = new ArrayList<>();
                for (JSONObject h : today) if (NHabits.part(h).equals(p[0])) l.add(h);
                if (l.isEmpty()) continue;
                int d = 0; for (JSONObject h : l) if (NHabits.done(h, td)) d++;
                section(p[1], d + " of " + l.size(), l);
            }
            List<JSONObject> q = new ArrayList<>();
            for (JSONObject h : live) if (NHabits.kind(h).equals("quit") && "active".equals(NHabits.status(h))) q.add(h);
            if (!q.isEmpty()) quitSection(q, "Breaking");
            List<JSONObject> rest = new ArrayList<>();
            for (JSONObject h : live) if (!NHabits.kind(h).equals("quit") && "active".equals(NHabits.status(h)) && !NHabits.due(h, td)) rest.add(h);
            if (!rest.isEmpty()) notToday(rest);
            if (today.isEmpty() && q.isEmpty()) add(NBits.empty(c, "Nothing scheduled for today", "Enjoy it, or add something new."), 18);
        } else {
            List<JSONObject> l = new ArrayList<>();
            for (JSONObject h : live) if (NHabits.kind(h).equals(filter)) l.add(h);
            if (l.isEmpty()) {
                final String fk = filter;
                add(NBits.empty(c, null, filter.equals("quit") ? "Nothing to break yet. Quit something completely, or cut it down to a daily limit." : filter.equals("routine") ? "No routines yet. A routine is a short sequence you run the same way each time, like a morning start." : "No habits to build yet.",
                    NBits.ibtnText(c, "plus", filter.equals("quit") ? "Break a habit" : filter.equals("routine") ? "New routine" : "New habit", true, new View.OnClickListener() { public void onClick(View v) { NHabitForm.open(sh, null, fk, null); } })), 0);
            } else if (filter.equals("quit")) quitSection(l, null);
            else if (filter.equals("routine")) for (JSONObject r : l) routineCard(r);
            else section(null, null, l);
            NHabitForm.tplGrid(sh, body, filter);   /* web: the matching templates under the list */
        }
        if (!arch.isEmpty()) archived(arch);
    }

    /* ---- the last 7 days (tap one to open it) and the month ---- */
    static final int PINK = 0xFFD9534F;

    void weekStrip(JSONArray habits) {
        LinearLayout r = NUi.row(c); r.setGravity(Gravity.TOP);
        int tn = NDates.today();
        for (int k = tn - 6; k <= tn; k++) {
            final String ds = NDates.fromN(k);
            int[] q = NHabits.dayRatio(habits, ds); float p = q[0] == 0 ? 0 : q[1] / (float) q[0];
            boolean miss = p == 0 && q[0] > 0 && k < tn;
            LinearLayout cell = NUi.col(c); cell.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.setBackground(NUi.ripple(NUi.round(NTheme.surface, 16, k == tn ? NTheme.accent : NTheme.line), 16));
            cell.setPadding(0, NUi.dp(9), 0, NUi.dp(8));
            TextView dl = NBits.meta(c, NDates.DAYS[NDates.dow(k)].substring(0, 1), NTheme.muted); dl.setTextSize(10.5f); dl.setGravity(Gravity.CENTER);
            cell.addView(dl);
            NRing ring = new NRing(c); ring.strokeDp = 4;
            if (miss) { ring.color = NTheme.alpha(PINK, .55f); ring.track = NTheme.alpha(PINK, .55f); ring.set(1, false); }
            else ring.set(p, false);
            if (p >= 1) ring.centerFill = NUi.mix(NTheme.accent, .22f, NTheme.surface);
            ring.bodyNum = true; ring.text(String.valueOf(NDates.day(ds)), "");
            LinearLayout.LayoutParams rl = NUi.lp(NUi.dp(32), NUi.dp(32)); rl.topMargin = NUi.dp(5); cell.addView(ring, rl);
            TextView sm = NBits.meta(c, q[0] == 0 ? "–" : q[1] + "/" + q[0], NTheme.muted); sm.setTextSize(10); sm.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams sl = NUi.lp(-1, -2); sl.topMargin = NUi.dp(5); cell.addView(sm, sl);
            NUi.tap(cell, new View.OnClickListener() { public void onClick(View v) { sh.push(new NDayScreen(sh, ds)); } });
            LinearLayout.LayoutParams l = NUi.lpw(0, -2, 1); if (k < tn) l.rightMargin = NUi.dp(6);
            r.addView(cell, l);
        }
        add(r, 4);
        LinearLayout tg = NUi.row(c); tg.setGravity(Gravity.CENTER);
        tg.setBackground(NUi.ripple(NUi.dashed(0, 14, NTheme.line2, 1), 14)); tg.setPadding(NUi.dp(9), NUi.dp(9), NUi.dp(9), NUi.dp(9));
        tg.addView(NUi.icon(c, "cal", 16, NTheme.text));
        TextView tl = NUi.body(c, calOn() ? "Hide calendar" : "Show the month", 13.5f, NTheme.text, 600); tl.setPadding(NUi.dp(8), 0, NUi.dp(8), 0); tg.addView(tl);
        tg.addView(NUi.icon(c, calOn() ? "chevup" : "chev", 16, NTheme.text));
        NUi.tap(tg, new View.OnClickListener() { public void onClick(View v) {
            try { JSONObject l = st.settings().optJSONObject("layout"); if (l == null) { l = new JSONObject(); st.settings().put("layout", l); } l.put("hcal", !calOn()); } catch (Exception ignored) { }
            sh.save();
        } });
        add(tg, 8);
        if (calOn()) monthCal(habits);
    }

    void monthCal(JSONArray habits) {
        java.util.Calendar k = java.util.Calendar.getInstance(); k.set(java.util.Calendar.DAY_OF_MONTH, 1); k.add(java.util.Calendar.MONTH, calOff);
        int first = NDates.dnum(NDates.ymd(k)), days = k.getActualMaximum(java.util.Calendar.DAY_OF_MONTH), tn = NDates.today();
        int due = 0, done = 0;
        for (int d = 0; d < days; d++) { if (first + d > tn) break; int[] q = NHabits.dayRatio(habits, NDates.fromN(first + d)); due += q[0]; done += q[1]; }
        LinearLayout head = NUi.row(c); head.setPadding(0, NUi.dp(14), 0, NUi.dp(12));
        head.addView(NBits.sbtn(c, "back", new View.OnClickListener() { public void onClick(View v) { calOff--; refresh(); } }));
        LinearLayout hl = NUi.col(c); hl.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView mt = NUi.text(c, NDates.MONTHS[k.get(java.util.Calendar.MONTH)] + " " + k.get(java.util.Calendar.YEAR), 19, NTheme.text); mt.setTypeface(NFont.display(700)); mt.setGravity(Gravity.CENTER);
        hl.addView(mt);
        TextView cap = NBits.meta(c, due > 0 ? Math.round(done * 100f / due) + "% kept · " + done + " of " + due : "Nothing due yet", NTheme.muted); cap.setGravity(Gravity.CENTER); hl.addView(cap, NUi.mt(2));
        head.addView(hl, NUi.lpw(0, -2, 1));
        View nx = NBits.sbtn(c, "next", new View.OnClickListener() { public void onClick(View v) { if (calOff < 0) { calOff++; refresh(); } } });
        nx.setAlpha(calOff < 0 ? 1f : .35f);
        head.addView(nx);
        add(head);
        LinearLayout dows = NUi.row(c);
        String[] wd = {"M", "T", "W", "T", "F", "S", "S"};
        for (int i = 0; i < 7; i++) {
            TextView d = NBits.meta(c, wd[i], NTheme.muted); d.setTextSize(11); d.setGravity(Gravity.CENTER); d.setPadding(0, 0, 0, NUi.dp(4));
            LinearLayout.LayoutParams dl = NUi.lpw(0, -2, 1); if (i < 6) dl.rightMargin = NUi.dp(6); dows.addView(d, dl);
        }
        add(dows);
        int lead = (NDates.dow(first) + 6) % 7; LinearLayout row = null, grid = NUi.col(c);
        for (int i = 0; i < lead + days; i++) {
            if (i % 7 == 0) { row = NUi.row(c); LinearLayout.LayoutParams rp = NUi.lp(-1, -2); if (i > 0) rp.topMargin = NUi.dp(6); grid.addView(row, rp); }
            LinearLayout.LayoutParams cp = NUi.lpw(0, -2, 1); if (i % 7 < 6) cp.rightMargin = NUi.dp(6);
            if (i < lead) { row.addView(new View(c), cp); continue; }
            final int n = first + i - lead; final String ds = NDates.fromN(n); boolean fut = n > tn;
            int[] q = fut ? new int[]{0, 0} : NHabits.dayRatio(habits, ds); float p = q[0] == 0 ? 0 : q[1] / (float) q[0];
            boolean full = q[0] > 0 && p >= 1, part = !full && p > 0, miss = !fut && q[0] > 0 && p == 0 && n < tn;
            NUi.Sq cell = new NUi.Sq(c, 72);
            int bg = full ? NUi.mix(NTheme.accent, .34f, NTheme.surface) : part ? NUi.mix(NTheme.accent, .14f, NTheme.surface) : NTheme.surface;
            int bd = full ? NTheme.accent : miss ? NTheme.alpha(PINK, .55f) : NTheme.line;
            cell.setBackground(NUi.round(bg, 14, bd));
            if (n == tn) cell.setForeground(NUi.round(0, 14, NTheme.accent));
            TextView d = NUi.body(c, String.valueOf(i - lead + 1), 15, NTheme.text, 700); d.setGravity(Gravity.CENTER); cell.addView(d);
            if (!fut && q[0] > 0) { TextView sm = NBits.meta(c, q[1] + "/" + q[0], NTheme.muted); sm.setTextSize(10); sm.setGravity(Gravity.CENTER); cell.addView(sm); }
            if (fut) cell.setAlpha(.35f); else NUi.tap(cell, new View.OnClickListener() { public void onClick(View v) { sh.push(new NDayScreen(sh, ds)); } });
            row.addView(cell, cp);
        }
        if (row != null) for (int i = (lead + days) % 7; i != 0 && i < 7; i++) { LinearLayout.LayoutParams cp = NUi.lpw(0, 1, 1); if (i < 6) cp.rightMargin = NUi.dp(6); row.addView(new View(c), cp); }
        add(grid);
        LinearLayout leg = NUi.row(c); leg.setPadding(0, NUi.dp(12), 0, 0);
        String[] ln = {"All done", "Some", "Missed"}; int[] lf = {NUi.mix(NTheme.accent, .34f, NTheme.surface), NUi.mix(NTheme.accent, .14f, NTheme.surface), 0}; int[] lb = {NTheme.accent, NTheme.line2, PINK};
        for (int i = 0; i < 3; i++) {
            View sw = new View(c); sw.setBackground(NUi.round(lf[i], 4, lb[i]));
            leg.addView(sw, NUi.lp(NUi.dp(12), NUi.dp(12)));
            TextView t = NUi.text(c, ln[i], 12, NTheme.text); t.setPadding(NUi.dp(6), 0, NUi.dp(14), 0); leg.addView(t);
        }
        add(leg);
        add(NUi.text(c, "Tap a day to see and change everything", 12, NTheme.muted), 6);
    }

    /* ---- today: ring, last 7 days as bars, perfect run ---- */
    void summary(JSONArray habits) {
        int[] t = NHabits.dayRatio(habits, NDates.ymd()); float f = t[0] == 0 ? 0 : t[1] / (float) t[0];
        LinearLayout card = NUi.col(c); card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setBackground(NUi.round(NTheme.surface, 28, NTheme.line)); card.setPadding(NUi.dp(24), NUi.dp(22), NUi.dp(24), NUi.dp(22));
        NRing ring = new NRing(c); ring.strokeDp = 9; ring.inline = true; ring.set(f, true); ring.text(Math.round(f * 100) + "", "%");
        card.addView(ring, NUi.lp(NUi.dp(108), NUi.dp(108)));
        TextView cap = NBits.meta(c, "Today · " + t[1] + " of " + t[0] + " done", NTheme.muted); cap.setGravity(Gravity.CENTER); cap.setAllCaps(true); cap.setLetterSpacing(.12f);
        LinearLayout.LayoutParams cl = NUi.lp(-1, -2); cl.topMargin = NUi.dp(18); card.addView(cap, cl);
        LinearLayout bars = NUi.row(c); bars.setGravity(Gravity.BOTTOM);
        int tn = NDates.today(); int wd = 0, wn = 0;
        for (int k = tn - 6; k <= tn; k++) {
            final String ds = NDates.fromN(k);
            int[] q = NHabits.dayRatio(habits, ds); float p = q[0] == 0 ? 0 : q[1] / (float) q[0]; wd += q[0]; wn += q[1];
            LinearLayout col = NUi.col(c); col.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
            View bar = new View(c); bar.setBackground(NUi.round(p > 0 ? NTheme.accent : NTheme.line2, 7, 0));
            col.addView(bar, NUi.lp(NUi.dp(24), Math.min(NUi.dp(59), Math.max(NUi.dp(6), Math.round(NUi.dp(78) * p)))));   /* web .hbars: p*100% of 78px, shrunk to fit the label */
            TextView lb = NBits.meta(c, NDates.DAYS[NDates.dow(k)].substring(0, 1), k == tn ? NTheme.text : NTheme.muted); lb.setTextSize(10); lb.setGravity(Gravity.CENTER); if (k == tn) lb.setTypeface(NFont.mono(700));
            LinearLayout.LayoutParams ll = NUi.lp(-1, -2); ll.topMargin = NUi.dp(6); col.addView(lb, ll);
            NUi.tap(col, new View.OnClickListener() { public void onClick(View v) { sh.push(new NDayScreen(sh, ds)); } });
            LinearLayout.LayoutParams bl = NUi.lpw(0, NUi.dp(78), 1); if (k < tn) bl.rightMargin = NUi.dp(8);
            bars.addView(col, bl);
        }
        LinearLayout.LayoutParams bp = NUi.lp(-1, -2); bp.topMargin = NUi.dp(10); card.addView(bars, bp);
        int pr = NHabits.perfectRun(habits);
        TextView cap2 = NBits.meta(c, (wd > 0 ? Math.round(wn * 100f / wd) + "% kept this week" : "This week") + (pr > 1 ? " · " + pr + " perfect days in a row" : ""), NTheme.muted); cap2.setGravity(Gravity.CENTER); cap2.setAllCaps(true); cap2.setLetterSpacing(.12f);
        LinearLayout.LayoutParams c2 = NUi.lp(-1, -2); c2.topMargin = NUi.dp(10); card.addView(cap2, c2);
        add(card, 14);
    }

    void nudge(List<JSONObject> live) {
        String yd = NDates.fromN(NDates.today() - 1), td = NDates.ymd();
        List<JSONObject> miss = new ArrayList<>();
        for (JSONObject h : live) if ("active".equals(NHabits.status(h)) && !NHabits.kind(h).equals("quit") && !NHabits.freq(h).equals("times") && NHabits.due(h, yd) && !NHabits.skip(h, yd) && !NHabits.done(h, yd) && !NHabits.done(h, td) && NHabits.due(h, td) && yd.compareTo(NHabits.startDate(h)) >= 0) miss.add(h);
        if (miss.isEmpty()) return;
        String mini = ""; for (JSONObject h : miss) if (!h.optString("mini").isEmpty()) { mini = h.optString("mini"); break; }
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < Math.min(3, miss.size()); i++) names.append(i > 0 ? ", " : "").append(miss.get(i).optString("title"));
        LinearLayout r = NUi.row(c); r.setGravity(Gravity.TOP);
        r.setBackground(NUi.dashed(NUi.mix(NTheme.accent, .06f, NTheme.bg), 20, NTheme.alpha(NTheme.accent, .45f), 1.5f)); r.setPadding(NUi.dp(18), NUi.dp(16), NUi.dp(18), NUi.dp(16));
        android.widget.ImageView fi = NUi.icon(c, "flag", 20, NTheme.accent);
        LinearLayout.LayoutParams fl = NUi.lp(NUi.dp(20), NUi.dp(20)); fl.topMargin = NUi.dp(2); fl.rightMargin = NUi.dp(14); r.addView(fi, fl);
        LinearLayout tx = NUi.col(c);
        tx.addView(NUi.body(c, "Missed yesterday: " + names + (miss.size() > 3 ? " +" + (miss.size() - 3) : ""), 15, NTheme.text, 700));
        tx.addView(NUi.text(c, "Missing once is normal. Try not to miss twice." + (mini.isEmpty() ? "" : " The small version counts: “" + mini + "”."), 13.5f, NTheme.muted), NUi.mt(2));
        r.addView(tx, NUi.lpw(0, -2, 1));
        add(r, 16);
    }

    void notToday(List<JSONObject> l) {
        add(NUi.sectionHead(c, "Not today", null, null));
        LinearLayout row = NUi.row(c);
        LinearLayout cur = null; int n = 0;
        android.widget.GridLayout.LayoutParams unused = null;
        LinearLayout wrap = NUi.col(c);
        LinearLayout line = null; int w = 0;
        for (final JSONObject h : l) {
            TextView t = NUi.chip(c, NHabits.icon(h) + " " + h.optString("title") + "  " + NHabits.freqText(h), false, new View.OnClickListener() { public void onClick(View v) { sh.push(new NHabitScreen(sh, h.optString("id"))); } });
            LinearLayout.LayoutParams p = NUi.lp(-2, -2); p.rightMargin = NUi.dp(8); p.bottomMargin = NUi.dp(8);
            row.addView(t, p);
        }
        add(NBits.hscroll(c, row));
    }

    void archived(List<JSONObject> arch) {
        LinearLayout h = NUi.row(c); h.setPadding(0, NUi.dp(24), 0, NUi.dp(8));
        h.addView(NUi.label(c, "Archived · " + arch.size() + (showArch ? "  ▴" : "  ▾"), NTheme.muted), NUi.lpw(0, -2, 1));
        NUi.tap(h, new View.OnClickListener() { public void onClick(View v) { showArch = !showArch; refresh(); } });
        add(h);
        if (!showArch) return;
        LinearLayout box = NBits.listBox(c);
        for (int i = 0; i < arch.size(); i++) { if (i > 0) box.addView(NBits.dividerFull(c)); box.addView(habitRow(arch.get(i))); }
        add(box);
    }

    void section(String title, String meta, List<JSONObject> l) {
        if (title != null) {
            LinearLayout h = NUi.row(c); h.setPadding(0, NUi.dp(28), 0, NUi.dp(10));
            h.addView(NUi.title(c, title, 24), NUi.lpw(0, -2, 1));
            if (meta != null) h.addView(NUi.label(c, meta, NTheme.muted));
            add(h);
        }
        LinearLayout box = NBits.listBox(c);
        for (int i = 0; i < l.size(); i++) { if (i > 0) box.addView(NBits.dividerFull(c)); box.addView(habitRow(l.get(i))); }
        add(box, title == null ? 4 : 0);
    }

    View sepDot() { View d = new View(c); d.setBackground(NUi.oval(NTheme.dim, 0, 0)); LinearLayout.LayoutParams l = NUi.lp(NUi.dp(3), NUi.dp(3)); l.leftMargin = l.rightMargin = NUi.dp(3.5f); d.setLayoutParams(l); return d; }

    /* the small line under a habit's name: streak, this week, steps, cue or time (web's hSub) */
    View subLine(JSONObject h) {
        NFlow r = new NFlow(c, 7, 2);
        int col = NTheme.areaCol(h.optString("area")); boolean first = true;
        if ("paused".equals(NHabits.status(h))) { r.addView(NUi.text(c, "Paused", 12.5f, NTheme.muted)); first = false; }
        int st = NHabits.streak(h);
        if (st > 0) {
            if (!first) r.addView(sepDot()); first = false;
            LinearLayout fg = NUi.row(c); fg.addView(NUi.icon(c, "flamef", 13, col));
            TextView t = NUi.body(c, st + (NHabits.freq(h).equals("times") ? " wk" : ""), 12.5f, col, 700); t.setPadding(NUi.dp(3), 0, 0, 0); fg.addView(t); r.addView(fg);
        }
        String mid = null;
        if (NHabits.freq(h).equals("times")) mid = NHabits.weekCount(h, NDates.wkStart(NDates.today())) + " of " + NHabits.times(h) + " this week";
        else if (NHabits.freq(h).equals("days") && NHabits.days(h).size() < 7) mid = NHabits.freqText(h);
        if (mid != null) { if (!first) r.addView(sepDot()); first = false; r.addView(NUi.text(c, mid, 12.5f, NTheme.muted)); }
        if (NHabits.kind(h).equals("routine")) {
            JSONArray ss = NHabits.steps(h); int k = ss.length(), mn = 0; for (int i = 0; i < k; i++) { JSONObject x = ss.optJSONObject(i); if (x != null) mn += x.optInt("min", 0); }
            if (!first) r.addView(sepDot()); first = false; r.addView(NUi.text(c, k + (k == 1 ? " step" : " steps") + (mn > 0 ? " · " + mn + " min" : ""), 12.5f, NTheme.muted));
        }
        String last = !h.optString("cue").isEmpty() ? h.optString("cue") : NDates.fmtTime(h.optString("time"));
        if (!last.isEmpty()) { if (!first) r.addView(sepDot()); first = false; TextView t = NUi.text(c, last, 12.5f, NTheme.muted); r.addView(t); }
        return r;
    }

    android.widget.ImageView bell(String icon, boolean on, final View.OnClickListener l) {
        android.widget.ImageView i = NUi.icon(c, icon, 17, on ? NTheme.accent : NTheme.muted);
        i.setAlpha(on ? 1f : .55f); i.setPadding(NUi.dp(6), NUi.dp(6), NUi.dp(6), NUi.dp(6));
        i.setLayoutParams(NUi.lp(NUi.dp(29), NUi.dp(29)));
        NUi.tap(i, l); return i;
    }

    View habitRow(final JSONObject h) {
        LinearLayout r = NUi.row(c);
        r.setPadding(NUi.dp(16), NUi.dp(14), NUi.dp(16), NUi.dp(14));
        boolean active = "active".equals(NHabits.status(h));
        int col = NTheme.areaCol(h.optString("area"));
        TextView ic = NUi.text(c, NHabits.icon(h), 23, NTheme.text); ic.setGravity(Gravity.CENTER);
        ic.setBackground(NUi.round(NUi.mix(col, .16f, NTheme.surface), 15, NTheme.alpha(col, .32f)));
        LinearLayout.LayoutParams il = NUi.lp(NUi.dp(46), NUi.dp(46)); il.rightMargin = NUi.dp(14); r.addView(ic, il);
        LinearLayout mid = NUi.col(c);
        boolean done = NHabits.done(h, NDates.ymd());
        LinearLayout tl = NUi.row(c);
        /* web .t.ell: the title with the unit as an inline span, one line, ellipsised together */
        android.text.SpannableStringBuilder tsb = new android.text.SpannableStringBuilder(h.optString("title"));
        int tg = NHabits.target(h);
        if (NHabits.kind(h).equals("build") && tg > 1) {
            tsb.append("  "); int a0 = tsb.length(); tsb.append((tg + " " + h.optString("unit")).toUpperCase());
            tsb.setSpan(new android.text.style.AbsoluteSizeSpan(10, true), a0, tsb.length(), 0); tsb.setSpan(new android.text.style.TypefaceSpan(NFont.mono(500)), a0, tsb.length(), 0);
            tsb.setSpan(new android.text.style.ForegroundColorSpan(NTheme.muted), a0, tsb.length(), 0); tsb.setSpan(new android.text.style.ScaleXSpan(1.05f), a0, tsb.length(), 0);
        }
        tl.addView(NUi.ell(NUi.body(c, tsb, 16, active && !done ? NTheme.text : NTheme.muted, 600), 1), NUi.lpw(0, -2, 1));
        mid.addView(tl);
        mid.addView(subLine(h), NUi.mt(2));
        mid.addView(NBits.week(c, h, new Runnable() { public void run() { sh.save(); } }), NUi.mt(9));
        r.addView(mid, NUi.lpw(0, -2, 1));
        String td = NDates.ymd();
        final String hid = h.optString("id");
        if (active) {
            if (h.optJSONObject("auto") != null && !h.optJSONObject("auto").optString("type").isEmpty())
                r.addView(bell("flame", true, new View.OnClickListener() { public void onClick(View v) { NAutoEdit.open(sh, "habit", hid, null); } }));
            if (!NHabits.kind(h).equals("quit"))
                r.addView(bell("bell", h.optBoolean("remind"), new View.OnClickListener() { public void onClick(View v) { NReminders.open(sh, "habit", hid); } }));
        }
        if (active && !NHabits.skip(h, td)) {
            boolean rt = NHabits.routine(h) && !NHabits.done(h, td);
            if (rt) {
                FrameLayout play = new FrameLayout(c);
                play.setBackground(NUi.ripple(NUi.oval(0, NTheme.line2, 4), 99));
                android.widget.ImageView pi = new android.widget.ImageView(c); pi.setImageDrawable(new NIcon("play", NTheme.text));
                play.addView(pi, new FrameLayout.LayoutParams(NUi.dp(18), NUi.dp(18), Gravity.CENTER));
                NUi.tap(play, new View.OnClickListener() { public void onClick(View v) { NUrge.routine(sh, hid); } });
                LinearLayout.LayoutParams pl = NUi.lp(NUi.dp(50), NUi.dp(50)); pl.leftMargin = NUi.dp(8); r.addView(play, pl);
            } else {
                final NRing ring = NBits.habitRing(c, h, 50); ring.strokeDp = 4;
                NUi.tap(ring, new View.OnClickListener() { public void onClick(View v) {
                    NUi.haptic(v);
                    boolean full = NActs.habitTap(h);
                    String m = full ? NActs.milestone(st, h) : null;
                    sh.save();
                    if (m != null) NShell.toast(m);
                } });
                LinearLayout.LayoutParams rl = NUi.lp(NUi.dp(50), NUi.dp(50)); rl.leftMargin = NUi.dp(8); r.addView(ring, rl);
            }
        } else if (NHabits.skip(h, td)) r.addView(NBits.meta(c, "Rest day", NTheme.muted));
        NUi.tap(r, new View.OnClickListener() { public void onClick(View v) { sh.push(new NHabitScreen(sh, hid)); } });
        r.setAlpha(active ? 1f : .6f);
        return r;
    }

    /* web rcard: tinted card, part · minutes + emoji tile, the name, up to 6 steps (ring/check, struck when done,
       minutes in mono), then the ink Start/Continue/Run again button and today's count · streak */
    void routineCard(final JSONObject h) {
        int col = NTheme.areaCol(h.optString("area")); String td = NDates.ymd();
        LinearLayout card = NUi.col(c);
        card.setBackground(NUi.ripple(NCard.bg(col, 26), 26));
        card.setPadding(NUi.dp(20), NUi.dp(20), NUi.dp(20), NUi.dp(18));
        int mins = 0; JSONArray stp = NHabits.steps(h); for (int i = 0; i < stp.length(); i++) mins += stp.optJSONObject(i).optInt("min");
        String pn = NHabits.part(h); pn = pn.equals("morning") ? "Morning" : pn.equals("afternoon") ? "Afternoon" : pn.equals("evening") ? "Evening" : "Anytime";
        LinearLayout top = NUi.row(c); top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(NUi.label(c, pn + " · " + (mins > 0 ? mins + " min" : stp.length() + " steps"), NTheme.INK_MUTED), NUi.lpw(0, -2, 1));
        TextView hic = NUi.text(c, NHabits.icon(h), 18, NTheme.text); hic.setGravity(Gravity.CENTER); hic.setIncludeFontPadding(false);
        hic.setBackground(NUi.round(NTheme.alpha(NTheme.INK, .12f), 12, 0));   /* .tint .hic: ink 12%, no ring */ top.addView(hic, NUi.lp(NUi.dp(36), NUi.dp(36)));
        card.addView(top);
        TextView t = NUi.title(c, h.optString("title"), 30); t.setTextColor(NTheme.INK); NUi.cssLh(t, .95f);
        card.addView(t, NUi.mt(10));
        JSONArray done = NHabits.obj(h, "rs").optJSONArray(td);
        LinearLayout list = NUi.col(c);
        for (int i = 0; i < Math.min(6, stp.length()); i++) {
            JSONObject sp = stp.optJSONObject(i); boolean on = false;
            if (done != null) for (int j = 0; j < done.length(); j++) if (sp.optString("id").equals(done.optString(j))) on = true;
            LinearLayout li = NUi.row(c); li.setGravity(Gravity.CENTER_VERTICAL);
            android.widget.FrameLayout dot = new android.widget.FrameLayout(c);
            dot.setBackground(on ? NUi.oval(NTheme.INK, 0, 0) : NUi.oval(0, NTheme.alpha(NTheme.INK, .35f), 1.5f));
            if (on) dot.addView(NUi.icon(c, "check", 11, 0xFFF4F2EC), new android.widget.FrameLayout.LayoutParams(NUi.dp(11), NUi.dp(11), Gravity.CENTER));
            li.addView(dot, NUi.lp(NUi.dp(18), NUi.dp(18)));
            TextView nm = NUi.ell(NUi.text(c, sp.optString("title"), 14, NTheme.INK), 1); nm.setPadding(NUi.dp(10), 0, NUi.dp(10), 0);
            if (on) { nm.setAlpha(.6f); nm.setPaintFlags(nm.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG); }
            li.addView(nm, NUi.lpw(0, -2, 1));
            if (sp.optInt("min") > 0) { TextView m = NUi.text(c, sp.optInt("min") + "m", 10.5f, NTheme.INK); m.setTypeface(NFont.mono(500)); m.setAlpha(.7f); li.addView(m); }
            list.addView(li, NUi.mt(i == 0 ? 0 : 6));
        }
        if (stp.length() > 6) { TextView more = NUi.text(c, "+" + (stp.length() - 6) + " more", 13, NTheme.INK); more.setAlpha(.7f); more.setPadding(NUi.dp(28), 0, 0, 0); list.addView(more, NUi.mt(6)); }
        card.addView(list, NUi.mt(10));
        int v = NHabits.val(h, td), n = NHabits.target(h), s = NHabits.streak(h);
        LinearLayout acts = NUi.row(c); acts.setGravity(Gravity.CENTER_VERTICAL);
        acts.addView(NBits.inkSm(c, "play", v >= n ? "Run again" : v > 0 ? "Continue" : "Start", true, new View.OnClickListener() { public void onClick(View x) { NUrge.routine(sh, h.optString("id")); } }));
        TextView st1 = NUi.label(c, (v >= n ? "Done today" : v > 0 ? v + " of " + n + " today" : "") + (s > 0 ? (v > 0 || v >= n ? " · " : "") + s + (NHabits.freq(h).equals("times") ? " wk" : "-day") + " streak" : ""), NTheme.INK_MUTED);
        st1.setPadding(NUi.dp(10), 0, 0, 0); acts.addView(st1, NUi.lpw(0, -2, 1));
        card.addView(acts, NUi.mt(14));
        NUi.tap(card, new View.OnClickListener() { public void onClick(View v2) { sh.push(new NHabitScreen(sh, h.optString("id"))); } });
        add(card, 10);
    }

    void quitSection(List<JSONObject> l, String title) {
        if (title != null) {
            LinearLayout h = NUi.row(c); h.setPadding(0, NUi.dp(28), 0, NUi.dp(10));
            h.addView(NUi.title(c, title, 24), NUi.lpw(0, -2, 1));
            h.addView(NUi.link(c, "All", new View.OnClickListener() { public void onClick(View v) { filter = "quit"; refresh(); } }));
            add(h);
        }
        for (final JSONObject q : l) add(qcard(sh, q), 10);
    }

    /* web qcard: tinted card, "Since your last slip" + the emoji tile, the name, a live clock (or today's count when
       cutting down), progress to the next milestone, and ink buttons */
    static View qcard(final NShell sh, final JSONObject q) {
        final android.content.Context c = sh.a;
        int col = NTheme.areaCol(q.optString("area"));
        LinearLayout card = NUi.col(c);
        card.setBackground(NUi.ripple(NCard.bg(col, 26), 26));
        card.setPadding(NUi.dp(20), NUi.dp(20), NUi.dp(20), NUi.dp(18));
        boolean lim = NHabits.limitMode(q);
        final int today = q.optJSONObject("log") == null ? 0 : q.optJSONObject("log").optInt(NDates.ymd(), 0);
        boolean slipped = false; JSONArray sl0 = q.optJSONArray("slips");
        for (int i = 0; sl0 != null && i < sl0.length(); i++) { JSONObject s0 = sl0.optJSONObject(i); if (s0 != null && s0.optLong("t") >= q.optLong("start")) slipped = true; }
        LinearLayout top = NUi.row(c); top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(NUi.ell(NUi.label(c, lim ? "Cutting down · max " + NHabits.limit(q) + (q.optString("unit").isEmpty() ? "" : " " + q.optString("unit")) + " a day" : slipped ? "Since your last slip" : "Free since " + NDates.fmtDate(NDates.ymd(NHabits.since(q))), NTheme.INK_MUTED), 1), NUi.lpw(0, -2, 1));
        TextView ic = NUi.text(c, NHabits.icon(q), 18, NTheme.text); ic.setGravity(Gravity.CENTER); ic.setIncludeFontPadding(false);
        ic.setBackground(NUi.round(NTheme.alpha(NTheme.INK, .12f), 12, 0));   /* .tint .hic: ink 12%, no ring */
        LinearLayout.LayoutParams icl = NUi.lp(NUi.dp(36), NUi.dp(36)); icl.leftMargin = NUi.dp(10); top.addView(ic, icl);
        card.addView(top);
        TextView t = NUi.title(c, q.optString("title"), 30); t.setTextColor(NTheme.INK); NUi.cssLh(t, .95f);
        card.addView(t, NUi.mt(10));
        if (lim) {
            LinearLayout big = NUi.row(c); big.setGravity(Gravity.BOTTOM);
            TextView num = NUi.text(c, String.valueOf(today), 66, today > NHabits.limit(q) ? 0xFFFF7A7A : NTheme.INK); num.setTypeface(NFont.display(800)); NUi.cssLh(num, .82f);
            big.addView(num);
            TextView u = NUi.text(c, "of " + NHabits.limit(q) + " today", 15, NTheme.INK); u.setPadding(NUi.dp(10), 0, 0, NUi.dp(6)); big.addView(u);
            card.addView(big, NUi.mt(10));
            card.addView(NBits.bar(c, NHabits.limit(q) == 0 ? (today > 0 ? 1 : 0) : Math.min(1f, today / (float) NHabits.limit(q)), NTheme.INK, 5), NUi.mt(10));
            int st1 = NHabits.limitStreak(q);
            card.addView(NUi.label(c, (today > NHabits.limit(q) ? "Over the limit today · tomorrow is a fresh start" : Math.max(0, NHabits.limit(q) - today) + " left today") + (st1 > 0 ? " · " + st1 + " day" + (st1 > 1 ? "s" : "") + " on track" : ""), NTheme.INK_MUTED), NUi.mt(10));
        } else {
            final long since = NHabits.since(q);
            /* web clockHTML: <b>9</b><small>days</small><span class="hms">14:24:11</span>, ticking every second */
            final TextView clk = NUi.text(c, "", 15, NTheme.INK); clk.setIncludeFontPadding(false);
            final Runnable[] tick = new Runnable[1];
            tick[0] = new Runnable() { public void run() {
                long ms = Math.max(0, System.currentTimeMillis() - since); long d = ms / 86400000L, h = ms % 86400000L / 3600000L, m = ms % 3600000L / 60000L, sc = ms % 60000L / 1000L;
                android.text.SpannableStringBuilder sb = new android.text.SpannableStringBuilder();
                int a0 = sb.length(); sb.append(String.valueOf(d));
                sb.setSpan(new android.text.style.AbsoluteSizeSpan(66, true), a0, sb.length(), 0); sb.setSpan(new android.text.style.TypefaceSpan(NFont.display(800)), a0, sb.length(), 0);
                sb.append("  "); int a1 = sb.length(); sb.append(d == 1 ? "day" : "days"); sb.setSpan(new android.text.style.TypefaceSpan(NFont.body(700)), a1, sb.length(), 0);
                sb.append("    "); int a2 = sb.length(); sb.append(String.format(java.util.Locale.US, "%02d:%02d:%02d", h, m, sc));
                sb.setSpan(new android.text.style.TypefaceSpan(NFont.mono(500)), a2, sb.length(), 0); sb.setSpan(new android.text.style.ForegroundColorSpan(NTheme.alpha(NTheme.INK, .72f)), a2, sb.length(), 0);
                clk.setText(sb);
                if (clk.isAttachedToWindow()) clk.postDelayed(this, 1000);
            } };
            tick[0].run();
            clk.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                public void onViewAttachedToWindow(View v) { v.removeCallbacks(tick[0]); v.postDelayed(tick[0], 1000); }
                public void onViewDetachedFromWindow(View v) { v.removeCallbacks(tick[0]); }
            });
            card.addView(clk, NUi.mt(10));
            int n = NHabits.cleanDays(q);
            int nx = 0; for (int m : NActs.MILES) if (m > n) { nx = m; break; } if (nx == 0) nx = n + 365;
            int pv = NActs.floorMile(NActs.MILES, n);
            float f = (System.currentTimeMillis() - since - pv * 86400000L) / (float) ((nx - pv) * 86400000L);
            card.addView(NBits.bar(c, Math.max(0, Math.min(1, f)), NTheme.INK, 5), NUi.mt(10));
            String saved = "";
            if (q.optDouble("cost", 0) > 0 || q.optDouble("mins", 0) > 0) {
                JSONArray sl = q.optJSONArray("slips"); java.util.Set<String> sd = new java.util.HashSet<>();
                if (sl != null) for (int i = 0; i < sl.length(); i++) { JSONObject s = sl.optJSONObject(i); if (s != null && s.optLong("t") >= q.optLong("start")) sd.add(NDates.ymd(s.optLong("t"))); }
                double days = Math.max(0, (System.currentTimeMillis() - q.optLong("start")) / 86400000.0 - sd.size());
                double money = days * q.optDouble("cost", 0), mins = days * q.optDouble("mins", 0);
                if (money >= .5) saved = " · " + q.optString("cur", "$") + (money >= 100 ? String.format(java.util.Locale.US, "%,d", Math.round(money)) : String.format(java.util.Locale.US, money < 10 ? "%.2f" : "%.0f", money)) + " saved";
                else if (mins >= 30) saved = " · " + (mins >= 60 ? Math.round(mins / 60) + " h" : Math.round(mins) + " min") + " back";
            }
            card.addView(NUi.label(c, "Next milestone: " + mileName(nx) + saved, NTheme.INK_MUTED), NUi.mt(10));
        }
        NFlow acts = new NFlow(c, 8, 8);
        if (lim) {
            acts.addView(NBits.inkSm(c, "plus", "Log one", true, new View.OnClickListener() { public void onClick(View v) {
                NUi.haptic(v); int cur = q.optJSONObject("log") == null ? 0 : q.optJSONObject("log").optInt(NDates.ymd(), 0);
                try { NHabits.obj(q, "log").put(NDates.ymd(), cur + 1); } catch (Exception ignored) { }
                sh.save();
            } }));
            if (today > 0) acts.addView(NBits.inkSm(c, null, "Undo", false, new View.OnClickListener() { public void onClick(View v) {
                try { JSONObject lg = NHabits.obj(q, "log"); int cur = lg.optInt(NDates.ymd(), 0) - 1; if (cur > 0) lg.put(NDates.ymd(), cur); else lg.remove(NDates.ymd()); } catch (Exception ignored) { }
                sh.save();
            } }));
            acts.addView(NBits.inkSm(c, "wave", "Urge", false, new View.OnClickListener() { public void onClick(View v) { NUrge.urge(sh, q); } }));
        } else {
            acts.addView(NBits.inkSm(c, "wave", "I have an urge", true, new View.OnClickListener() { public void onClick(View v) { NUrge.urge(sh, q); } }));
            acts.addView(NBits.inkSm(c, null, "Log a slip", false, new View.OnClickListener() { public void onClick(View v) { NUrge.slip(sh, q); } }));
        }
        card.addView(acts, NUi.mt(14));
        NUi.tap(card, new View.OnClickListener() { public void onClick(View v) { sh.push(new NHabitScreen(sh, q.optString("id"))); } });
        return card;
    }

    static String mileName(int m) { return m >= 365 ? (m / 365) + " year" + (m > 365 ? "s" : "") : m >= 30 ? Math.round(m / 30f) + " month" + (m >= 60 ? "s" : "") : m % 7 == 0 && m >= 7 ? (m / 7) + " week" + (m > 7 ? "s" : "") : m + " day" + (m > 1 ? "s" : ""); }

    TextView inkBtn(String s, boolean solid, View.OnClickListener l) {
        TextView t = NUi.body(c, s, 15, solid ? 0xFFFFFFFF : NTheme.INK, 700); t.setGravity(Gravity.CENTER); t.setPadding(NUi.dp(16), 0, NUi.dp(16), 0); t.setMinHeight(NUi.dp(44));
        t.setBackground(NUi.ripple(solid ? NUi.round(NTheme.INK, 14, 0) : NUi.round(0, 14, NTheme.alpha(NTheme.INK, .3f)), 14));
        NUi.tap(t, l); return t;
    }

}
