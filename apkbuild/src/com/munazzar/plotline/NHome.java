package com.munazzar.plotline;

import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/* Home: greeting, quick actions, your day, up next, today's goals, habits, threads and what's coming. */
final class NHome extends NPage {
    NStudio studio;

    NHome(NShell sh) { super(sh); }

    @Override void build() {
        final NForms F = new NForms(sh);
        JSONObject set = st.settings();
        String name = set.optString("name", "").trim();
        if (!st.exists()) {
            header("Plotline", gear());
            add(NBits.empty(c, "Getting your plan ready…", "Moving your goals and habits into the new app. This takes a moment the first time."), 24);
            return;
        }
        /* Studio room on top (settings.home = 'studio'), full-bleed under the status bar like the web */
        final boolean stu = NStudio.on(sh);
        if (stu) {
            if (studio == null) studio = new NStudio(sh); else studio.update();
            View room = studio.view();
            int side = NUi.dp(sh.wide() ? 32 : 16);
            LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(-1, room.getLayoutParams() != null ? room.getLayoutParams().height : NUi.dp(560));
            rl.leftMargin = -side; rl.rightMargin = -side; rl.topMargin = -(sh.top + NUi.dp(18)); rl.bottomMargin = NUi.dp(8);
            body.addView(room, rl);
        } else studio = null;
        JSONObject eng = engine();
        sharingCard(eng);
        demoBar(eng);
        header(NDates.greeting() + (name.isEmpty() ? "" : ",\n" + name),
            NUi.ibtn(c, stu ? "cards" : "room", stu, new View.OnClickListener() { public void onClick(View v) { NStudio.setHome(sh, stu ? "classic" : "studio"); } }),
            NUi.ibtn(c, "plus", new View.OnClickListener() { public void onClick(View v) { F.goal(null); } }), gear());
        int act = NActs.goals(st, "active").size(), late = 0;
        for (JSONObject g : NActs.goals(st, "active")) { org.json.JSONArray ss = g.optJSONArray("steps"); for (int i = 0; ss != null && i < ss.length(); i++) { JSONObject x = ss.optJSONObject(i); if (x != null && !x.optBoolean("done") && !x.optString("due").isEmpty() && NDates.dnum(x.optString("due")) < NDates.today()) late++; } }
        int wk = NActs.stepsDoneThisWeek(st);
        if (act > 0) add(NJCards.data(c, wk + " step" + (wk == 1 ? "" : "s") + " done this week" + (late > 0 ? " · " + late + " late" : "") + " · " + act + " active goal" + (act == 1 ? "" : "s"), NTheme.muted), 14);

        /* quick actions */
        LinearLayout qa = NUi.row(c);
        String[][] Q = {{"✨", "Plan with AI"}, {"🎯", "Goal"}, {"✅", "Habit"}, {"✍️", "Write"}, {"🧵", "Thread"}, {"💬", "Ask"}, {"📅", "Today in full"}};
        for (int i = 0; i < Q.length; i++) {
            final int k = i;
            android.text.SpannableString sp = new android.text.SpannableString(Q[i][0] + "  " + Q[i][1]);
            sp.setSpan(new android.text.style.AbsoluteSizeSpan(16, true), 0, Q[i][0].length(), 0);
            TextView t = NUi.body(c, sp, 13.5f, NTheme.text, 600);
            t.setGravity(Gravity.CENTER); t.setPadding(NUi.dp(15), 0, NUi.dp(15), 0);
            t.setBackground(NUi.ripple(NUi.round(i == 0 ? NUi.mix(NTheme.accent, .16f, NTheme.surface) : NTheme.surface, 99, i == 0 ? NTheme.alpha(NTheme.accent, .5f) : NTheme.line2), 99));
            NUi.tap(t, new View.OnClickListener() { public void onClick(View v) {
                switch (k) { case 0: sh.openClassic("ai"); break; case 1: F.goal(null); break; case 2: F.habit(null); break; case 3: F.entry(null); break; case 4: F.thread(null); break; case 5: sh.openClassic("ask"); break; case 6: sh.push(new NDayScreen(sh, NDates.ymd())); break; default: NStudio.setHome(sh, "studio"); sv.smoothScrollTo(0, 0); }
            } });
            LinearLayout.LayoutParams l = NUi.lp(-2, NUi.dp(42)); l.rightMargin = NUi.dp(8); qa.addView(t, l);
        }
        add(NBits.hscroll(c, qa), 16);

        yourDay(F);
        insights(eng);
        weeklyCard();
        todayGoals(F);
        habits();
        planCard();
        activity();
        threads(F);
        upNext();
        comingUp();
        pinned();
        askEngine();
    }

    /* ---- what only the web engine knows (insight rules, sample data, sharing) ---- */
    String homeJson; long homeAt; boolean homeAsk;

    JSONObject engine() { try { return homeJson == null ? null : new JSONObject(homeJson); } catch (Exception e) { return null; } }

    void askEngine() {
        if (homeAsk || System.currentTimeMillis() - homeAt < 4000) return;
        homeAsk = true;
        sh.a.jsRet("window.__nhome&&window.__nhome()", new android.webkit.ValueCallback<String>() { @Override public void onReceiveValue(String v) {
            homeAsk = false; homeAt = System.currentTimeMillis();
            if (v == null || v.equals(homeJson)) return;
            homeJson = v; stale = true;
            if (sh.stack.isEmpty() && !sh.classic && sh.pages[sh.pager.page] == NHome.this) refresh();
        } });
    }

    void demoBar(JSONObject eng) {
        if (eng == null || eng.optInt("demo", 0) <= 0 || st.settings().optBoolean("demoHide")) return;
        LinearLayout r = NUi.row(c); r.setGravity(Gravity.CENTER_VERTICAL);
        r.setBackground(NUi.dashed(NUi.mix(NTheme.accent, .06f, NTheme.surface), 18, NUi.mix(NTheme.accent, .55f, NTheme.bg), 1));
        r.setPadding(NUi.dp(16), NUi.dp(10), NUi.dp(10), NUi.dp(10));
        r.addView(NUi.icon(c, "flag", 17, NTheme.accent), NUi.lp(NUi.dp(17), NUi.dp(17)));
        TextView t = NUi.text(c, "You’re exploring with sample data.", 14, NTheme.text);
        LinearLayout.LayoutParams tl = NUi.lpw(0, -2, 1); tl.leftMargin = NUi.dp(8); tl.rightMargin = NUi.dp(10);
        r.addView(t, tl);
        r.addView(NUi.btnSm(c, "Remove it", false, new View.OnClickListener() { public void onClick(View v) { sh.run("demoRemove", null); } }));
        LinearLayout.LayoutParams hl = NUi.lp(-2, -2); hl.leftMargin = NUi.dp(10);
        r.addView(NBits.sbtn(c, "minus", new View.OnClickListener() { public void onClick(View v) { try { st.settings().put("demoHide", true); } catch (Exception ignored) { } sh.save(); } }), hl);
        add(r, 0); ((LinearLayout.LayoutParams) r.getLayoutParams()).bottomMargin = NUi.dp(16);
    }

    /* web shTodayCard: invites, unread reactions and the allow card, right on Today */
    void sharingCard(JSONObject eng) {
        LinearLayout box = NUi.col(c); box.setVisibility(View.GONE);
        add(box, 14);
        NShare.today(sh, box);
    }

    /* ---- insights: the web's own rules (streak at risk, best weekday, idle goals, steps, sleep, quiet threads, perfect days) ---- */
    void insights(JSONObject eng) {
        if (eng == null) return;
        final JSONArray ins = eng.optJSONArray("ins");
        if (ins == null || ins.length() == 0) return;
        add(NUi.sectionHead(c, "Insights", "Ask AI", new View.OnClickListener() { public void onClick(View v) { sh.tab(6); } }));
        LinearLayout box = NUi.col(c);
        for (int i = 0; i < ins.length(); i++) {
            final JSONObject x = ins.optJSONObject(i); if (x == null) continue;
            LinearLayout card = NUi.row(c); card.setGravity(Gravity.TOP);
            card.setBackground(NUi.card(20));
            card.setPadding(NUi.dp(16), NUi.dp(14), NUi.dp(16), NUi.dp(14));
            card.addView(NUi.text(c, x.optString("e"), 22, NTheme.text));
            LinearLayout hb = NUi.col(c);
            TextView t = NUi.text(c, x.optString("t"), 14.5f, NTheme.text); t.setLineSpacing(0, 1.3f);
            hb.addView(t);
            NFlow acts = new NFlow(c, 8, 8);
            final String a = x.optString("a");
            if (!a.isEmpty()) acts.addView(NUi.btnSm(c, x.optString("l", "Open"), false, new View.OnClickListener() { public void onClick(View v) {
                String id = x.optString("i");
                if (a.equals("openHabit")) sh.route("habit/" + id);
                else if (a.equals("open")) sh.route("goal/" + id);
                else if (a.startsWith("go:")) { if (!sh.route(a.substring(3))) sh.openClassic(a.substring(3)); }
                else if (a.equals("thrAddSheet")) sh.route("thread/" + id);
                else sh.run(a, NMore.d("id", id));
            } }));
            TextView ask = NUi.btnSm(c, "Ask AI", false, new View.OnClickListener() { public void onClick(View v) { sh.ask(x.optString("q")); } });
            ask.setBackground(NUi.ripple(NUi.round(0, 12, 0), 12));
            ask.setCompoundDrawablesRelative(NUi.iconD("ai", 16, NTheme.text), null, null, null); ask.setCompoundDrawablePadding(NUi.dp(8));
            acts.addView(ask);
            hb.addView(acts, NUi.mt(10));
            LinearLayout.LayoutParams hl = NUi.lpw(0, -2, 1); hl.leftMargin = NUi.dp(12);
            card.addView(hb, hl);
            box.addView(card, NUi.mt(i == 0 ? 0 : 10));
        }
        add(box);
    }

    /* ---- weekly review nudge (Fri–Sun, when not reviewed in the last 5 days) ---- */
    void weeklyCard() {
        if (NActs.goals(st, "active").isEmpty()) return;
        int dow = NDates.dow(NDates.today());
        String lr = st.settings().optString("lastReview", "");
        boolean reviewed = NDates.valid(lr) && NDates.daysUntil(lr) > -5;
        if (!(dow == 0 || dow >= 5) || reviewed) return;
        /* web: .li with a 30px accent ring holding the flag, title, line, chevron; radius 18, line-2 border */
        LinearLayout r = NUi.row(c); r.setGravity(Gravity.CENTER_VERTICAL);
        r.setBackground(NUi.ripple(NUi.round(0, 18, NTheme.line2), 18));
        r.setPadding(NUi.dp(18), NUi.dp(15), NUi.dp(18), NUi.dp(15));
        android.widget.FrameLayout ic = new android.widget.FrameLayout(c); ic.setBackground(NUi.oval(0, NTheme.accent, 2));
        ic.addView(NUi.icon(c, "flag", 15, NTheme.accent), new android.widget.FrameLayout.LayoutParams(NUi.dp(15), NUi.dp(15), Gravity.CENTER));
        r.addView(ic, NUi.lp(NUi.dp(30), NUi.dp(30)));
        LinearLayout tx = NUi.col(c);
        tx.addView(NUi.body(c, "Time for your weekly review", 16, NTheme.text, 600));
        tx.addView(NUi.text(c, "Two minutes to look back and set next week’s focus", 13, NTheme.muted));
        LinearLayout.LayoutParams tl = NUi.lpw(0, -2, 1); tl.leftMargin = NUi.dp(14); tl.rightMargin = NUi.dp(14);
        r.addView(tx, tl);
        r.addView(NUi.icon(c, "next", 16, NTheme.muted), NUi.lp(NUi.dp(16), NUi.dp(16)));
        NUi.tap(r, new View.OnClickListener() { public void onClick(View v) { NSheets.weekly(sh); } });
        add(r, 0); ((LinearLayout.LayoutParams) r.getLayoutParams()).bottomMargin = NUi.dp(14);
    }

    /* ---- Plan with AI (explained, rework, minimize) ---- */
    void planCard() {
        View v = aip(sh, "home");
        add(v, 8); ((LinearLayout.LayoutParams) v.getLayoutParams()).bottomMargin = NUi.dp(18);
    }

    /* web aiPlanCard(where): the Plan with AI card (home can minimise it to one row) */
    static View aip(final NShell sh, String where) {
        final android.content.Context c = sh.a; final NStore st = sh.st;
        final boolean home = where.equals("home"), min = home && st.settings().optBoolean("aipHide");
        LinearLayout card = NUi.row(c);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR, new int[]{NUi.mix(NTheme.accent, .16f, NTheme.surface), NTheme.surface});
        bg.setCornerRadius(NUi.dp(24)); bg.setStroke(Math.max(1, NUi.dp(1)), NUi.mix(NTheme.accent, .4f, NTheme.line));
        card.setBackground(bg);
        FrameLayout ic = new FrameLayout(c); ic.setBackground(NUi.round(NTheme.accent, min ? 12 : 14, 0));
        ic.addView(NUi.icon(c, "ai", 22, NTheme.bg), new FrameLayout.LayoutParams(NUi.dp(22), NUi.dp(22), Gravity.CENTER));
        if (min) {
            card.setPadding(NUi.dp(10), NUi.dp(8), NUi.dp(8), NUi.dp(8)); card.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout mb = NUi.row(c); mb.setGravity(Gravity.CENTER_VERTICAL);
            mb.addView(ic, NUi.lp(NUi.dp(38), NUi.dp(38)));
            LinearLayout mt = NUi.col(c);
            mt.addView(NUi.body(c, "Plan with AI", 15, NTheme.text, 700));
            mt.addView(NUi.ell(NUi.text(c, "Describe a goal, get steps and habits", 12.5f, NTheme.muted), 1));
            LinearLayout.LayoutParams ml = NUi.lpw(0, -2, 1); ml.leftMargin = NUi.dp(12); mb.addView(mt, ml);
            NUi.tap(mb, new View.OnClickListener() { public void onClick(View v) { sh.push(new NAiPlan(sh, "new")); } });
            card.addView(mb, NUi.lpw(0, -2, 1));
            LinearLayout.LayoutParams xl = NUi.lp(-2, -2); xl.leftMargin = NUi.dp(6);
            card.addView(NUi.ibtn(c, "down", new View.OnClickListener() { public void onClick(View v) { try { st.settings().put("aipHide", false); } catch (Exception ignored) { } sh.save(); } }), xl);
            return card;
        }
        card.setPadding(NUi.dp(18), NUi.dp(18), NUi.dp(18), NUi.dp(18)); card.setGravity(Gravity.TOP);
        card.addView(ic, NUi.lp(NUi.dp(44), NUi.dp(44)));
        LinearLayout b = NUi.col(c);
        b.addView(NUi.body(c, "Plan with AI", 17, NTheme.text, 700));
        TextView p = NUi.text(c, "Say what you want in your own words, like “get fit by summer”, “save for a house” or “a calmer morning”. The AI drafts a goal with steps, dates and the habits to get there. You check and edit everything before anything is saved.", 14, NTheme.muted);
        p.setLineSpacing(0, 1.3f); b.addView(p, NUi.mt(6));
        NFlow acts = new NFlow(c, 8, 8);
        TextView start = NUi.btnSm(c, "Start a plan", true, new View.OnClickListener() { public void onClick(View v) { sh.push(new NAiPlan(sh, "new")); } });
        start.setCompoundDrawablesRelative(NUi.iconD("ai", 16, NTheme.onAccent), null, null, null); start.setCompoundDrawablePadding(NUi.dp(8));
        acts.addView(start);
        acts.addView(NUi.btnSm(c, "Rework my plan", false, new View.OnClickListener() { public void onClick(View v) { sh.push(new NAiPlan(sh, st.arr("goals").length() > 0 ? "change" : "new")); } }));
        if (home) { TextView mn = NUi.btnSm(c, "Minimize", false, new View.OnClickListener() { public void onClick(View v) { try { st.settings().put("aipHide", true); } catch (Exception ignored) { } sh.save(); } }); mn.setBackground(NUi.ripple(NUi.round(0, 12, 0), 12)); acts.addView(mn); }
        b.addView(acts, NUi.mt(12));
        LinearLayout.LayoutParams bl = NUi.lpw(0, -2, 1); bl.leftMargin = NUi.dp(14);
        card.addView(b, bl);
        return card;
    }

    /* ---- pinned goals ---- */
    void pinned() {
        List<JSONObject> pins = new ArrayList<>();
        for (JSONObject g : NActs.goals(st, "active")) if (g.optBoolean("pinned")) pins.add(g);
        if (pins.isEmpty()) return;
        add(NUi.sectionHead(c, "Pinned", null, null));
        LinearLayout row = NUi.row(c);
        for (final JSONObject g : pins) {
            int col = NTheme.areaCol(g.optString("area")); boolean pim = NTheme.pushImg(g);
            LinearLayout card = NUi.col(c);
            card.setBackground(NUi.ripple(NCard.bgFor(g, col, 24), 24));
            card.setPadding(NUi.dp(18), NUi.dp(18), NUi.dp(18), NUi.dp(18));
            card.addView(NUi.ell(NUi.label(c, NTheme.areaName(g.optString("area")), NTheme.INK_MUTED), 1));
            View sp = new View(c); card.addView(sp, NUi.lpw(-1, 0, 1));
            TextView t = NUi.ell(NUi.body(c, g.optString("title"), 16, NTheme.INK, 700), 3); card.addView(t, NUi.mt(10));
            card.addView(NBits.bar(c, NActs.pct(st, g) / 100f, NTheme.INK), NUi.mt(10));
            NUi.tap(card, new View.OnClickListener() { public void onClick(View v) { sh.push(new NGoalScreen(sh, g.optString("id"))); } });
            LinearLayout.LayoutParams l = NUi.lp(NUi.dp(210), NUi.dp(156)); l.rightMargin = NUi.dp(12); row.addView(card, l); NTheme.popImg(pim);
        }
        /* web .hscroll: full bleed with room above/below so the halo glow isn't cut off (margins cancel the padding) */
        android.widget.HorizontalScrollView hs = NBits.hscroll(c, row);
        int g = NUi.dp(sh.wide() ? 32 : 16), hr = NUi.dp(40);
        hs.setPadding(g, hr + NUi.dp(8), g, hr + NUi.dp(8)); hs.setClipChildren(false); row.setClipChildren(false);
        LinearLayout.LayoutParams hl = NUi.lp(-1, -2); hl.leftMargin = -g; hl.rightMargin = -g; hl.topMargin = -(hr + NUi.dp(6)); hl.bottomMargin = -(hr + NUi.dp(6));
        body.addView(hs, hl);
    }

    /* ---- your day: one ring for everything due today ---- */
    void yourDay(NForms F) {
        final String td = NDates.ymd();
        List<JSONObject> due = NHabits.today(st.arr("habits"));
        /* web homeDay: habits (in your order), today's goals, then steps due today; "next" is the first not done */
        final List<Object[]> items = new ArrayList<>();
        for (JSONObject h : NStore.list(st.arr("habits"))) if (due.contains(h)) items.add(new Object[]{"h", NHabits.done(h, td), NHabits.icon(h) + " " + h.optString("title"), h, null});
        for (JSONObject d : NActs.daysOn(st, td)) items.add(new Object[]{"d", d.optBoolean("done"), d.optString("title"), d, null});
        for (JSONObject g : NActs.goals(st, "active")) { org.json.JSONArray ss = g.optJSONArray("steps"); for (int i = 0; ss != null && i < ss.length(); i++) { JSONObject x = ss.optJSONObject(i); if (x != null && td.equals(x.optString("due"))) items.add(new Object[]{"s", x.optBoolean("done"), x.optString("title"), x, g}); } }
        if (items.isEmpty()) return;
        int done = 0; Object[] nx = null;
        for (Object[] it : items) { if ((Boolean) it[1]) done++; else if (nx == null) nx = it; }
        float p = done / (float) items.size();
        LinearLayout card = NUi.row(c); card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackground(NUi.card(26));
        card.setPadding(NUi.dp(20), NUi.dp(18), NUi.dp(20), NUi.dp(18));
        NRing ring = new NRing(c); ring.strokeDp = 7; ring.inline = true; ring.set(p, true); ring.text(String.valueOf(done), "/" + items.size());
        card.addView(ring, NUi.lp(NUi.dp(76), NUi.dp(76)));
        LinearLayout r = NUi.col(c);
        r.addView(NJCards.data(c, "Your day · " + (p >= 1 ? "all done" : Math.round(p * 100) + "% done"), NTheme.muted));
        final Object[] n = nx;
        if (n != null) {
            LinearLayout nr = NUi.row(c); nr.setGravity(Gravity.CENTER_VERTICAL);
            TextView t = NUi.ell(NUi.body(c, "Next: " + n[2], 15, NTheme.text, 600), 1); nr.addView(t, NUi.lpw(0, -2, 1));
            LinearLayout b = NUi.row(c); b.setGravity(Gravity.CENTER); b.setPadding(NUi.dp(14), 0, NUi.dp(14), 0);
            b.setBackground(NUi.ripple(NUi.round(NTheme.accent, 12, 0), 12));
            b.addView(NUi.icon(c, "check", 16, NTheme.onAccent), NUi.lp(NUi.dp(16), NUi.dp(16))); TextView bt = NUi.body(c, "Done", 13.5f, NTheme.onAccent, 600); bt.setPadding(NUi.dp(8), 0, 0, 0); b.addView(bt);
            NUi.tap(b, new View.OnClickListener() { public void onClick(View v) {
                NUi.haptic(v);
                JSONObject o = (JSONObject) n[3];
                if (n[0].equals("h")) { boolean full = NActs.habitTap(o); String m = full ? NActs.milestone(st, o) : null; sh.save(); if (m != null) NShell.toast(m); }
                else if (n[0].equals("d")) { NActs.toggleDay(o); sh.save(); }
                else { String m = NActs.toggleStep(st, (JSONObject) n[4], o); sh.save(); NShell.toast(m != null ? m : "Step done · nice"); }
            } });
            LinearLayout.LayoutParams bl = NUi.lp(-2, NUi.dp(38)); bl.leftMargin = NUi.dp(10); nr.addView(b, bl);
            r.addView(nr, NUi.mt(8));
        } else {
            TextView t = NUi.body(c, "Everything for today is done. Well played.", 15, NTheme.text, 600); r.addView(t, NUi.mt(8));
        }
        TextView all = NUi.link(c, "See everything for today ›", new View.OnClickListener() { public void onClick(View v) { sh.push(new NDayScreen(sh, td)); } });
        all.setTextSize(13.5f); all.setPadding(0, NUi.dp(8), 0, 0); r.addView(all);
        LinearLayout.LayoutParams rl = NUi.lpw(0, -2, 1); rl.leftMargin = NUi.dp(18);
        card.addView(r, rl);
        add(card, 0); ((LinearLayout.LayoutParams) card.getLayoutParams()).bottomMargin = NUi.dp(14);
    }

    /* ---- the single most urgent step ---- */
    void upNext() {
        List<JSONObject[]> up = NActs.upNext(st);
        if (up.isEmpty()) return;
        final JSONObject g = up.get(0)[0], s = up.get(0)[1];
        int col = NTheme.areaCol(g.optString("area"));
        LinearLayout card = NUi.col(c);
        boolean upImg = NTheme.pushImg(g);
        card.setBackground(NCard.bgFor(g, col, 30, true));
        card.setPadding(NUi.dp(22), NUi.dp(22), NUi.dp(22), NUi.dp(22));
        card.addView(NUi.ell(NJCards.data(c, "Up next · " + g.optString("title"), NTheme.INK_MUTED), 1));
        TextView t = NJCards.display(c, s.optString("title"), 34, NTheme.INK, 5); t.setLineSpacing(0, .92f);
        t.setMaxWidth(Math.round(t.getPaint().measureText("0") * 18));   /* web .now h2 max-width:18ch */
        LinearLayout.LayoutParams tl = NUi.mt(16); tl.bottomMargin = NUi.dp(12); card.addView(t, tl);
        String due = NDates.dueText(s.optString("due"), s.optString("time"));
        boolean rem = s.opt("remind") != null && !s.isNull("remind") && !String.valueOf(s.opt("remind")).isEmpty() && !"false".equals(String.valueOf(s.opt("remind")));
        card.addView(NJCards.data(c, (due.isEmpty() ? "No date set" : due) + (rem ? " · reminder on" : ""), NTheme.INK_MUTED));
        NFlow acts = new NFlow(c, 8, 8);
        acts.addView(inkB("check", "Mark done", true, new View.OnClickListener() { public void onClick(View v) { NUi.haptic(v); String m = NActs.toggleStep(st, g, s); sh.save(); NShell.toast(m != null ? m : "Step done · nice"); } }), NUi.lp(-2, NUi.dp(46)));
        acts.addView(inkB("timer", "Focus", false, new View.OnClickListener() { public void onClick(View v) { NTimer.pick(sh, g.optString("id"), s.optString("id")); } }), NUi.lp(-2, NUi.dp(46)));
        acts.addView(inkB(null, "Open goal", false, new View.OnClickListener() { public void onClick(View v) { sh.push(new NGoalScreen(sh, g.optString("id"))); } }), NUi.lp(-2, NUi.dp(46)));
        card.addView(acts, NUi.mt(24));
        add(card, 18); NTheme.popImg(upImg);
    }

    /* .btn.ink (filled with the card ink) / .btn.inkline (ink outline) with an optional icon */
    View inkB(String icon, String label, boolean fill, View.OnClickListener l) {
        int fg = fill ? 0xFFF4F2EC : NTheme.INK;
        LinearLayout b = NUi.row(c); b.setGravity(Gravity.CENTER); b.setPadding(NUi.dp(20), 0, NUi.dp(20), 0);
        b.setBackground(NUi.ripple(fill ? NUi.round(NTheme.INK, 15, 0) : NUi.round(0, 15, NTheme.alpha(NTheme.INK, .28f)), 15));
        if (icon != null) { LinearLayout.LayoutParams il = NUi.lp(NUi.dp(18), NUi.dp(18)); il.rightMargin = NUi.dp(8); b.addView(NUi.icon(c, icon, 18, fg), il); }
        b.addView(NUi.body(c, label, 14.5f, fg, 600));
        NUi.tap(b, l);
        return b;
    }

    /* ---- goals for today (the web's "days") ---- */
    void todayGoals(final NForms F) {
        final String td = NDates.ymd();
        LinearLayout tgh = NUi.sectionHead(c, "Today’s goals", "Calendar", new View.OnClickListener() { public void onClick(View v) { sh.tab(2); } });
        List<JSONObject> tdl = NActs.daysOn(st, td);
        if (!tdl.isEmpty()) {
            int dn = 0; for (JSONObject x : tdl) if (x.optBoolean("done")) dn++;
            LinearLayout rg = NUi.row(c); rg.setPadding(NUi.dp(10), 0, 0, 0);
            NRing mr = new NRing(c); mr.strokeDp = 3.5f; mr.set(dn / (float) tdl.size(), false); rg.addView(mr, new LinearLayout.LayoutParams(NUi.dp(22), NUi.dp(22)));
            TextView ct = NBits.meta(c, dn + " OF " + tdl.size(), NTheme.muted); ct.setTextSize(10.5f); ct.setPadding(NUi.dp(10), 0, 0, 0); rg.addView(ct);
            tgh.addView(rg, 1);
            ((LinearLayout.LayoutParams) tgh.getChildAt(0).getLayoutParams()).weight = 0; tgh.getChildAt(0).getLayoutParams().width = -2;
            View gsp = new View(c); tgh.addView(gsp, 2, NUi.lpw(0, 1, 1));
        }
        add(tgh);
        LinearLayout box = NBits.listBox(c);
        /* add field */
        LinearLayout addRow = NUi.row(c); addRow.setPadding(NUi.dp(20), NUi.dp(8), NUi.dp(8), NUi.dp(8));
        final EditText in = NForms.input(c, "Add a goal for today…", "", false); in.setTextSize(15.5f);
        in.setBackground(null); in.setPadding(0, NUi.dp(10), 0, NUi.dp(10));
        in.setImeOptions(EditorInfo.IME_ACTION_DONE); in.setTag("dayIn");
        LinearLayout.LayoutParams inl = NUi.lpw(0, -2, 1); inl.rightMargin = NUi.dp(10); addRow.addView(in, inl);
        final Runnable addIt = new Runnable() { public void run() {
            String t = in.getText().toString().trim(); if (t.isEmpty()) return;
            NForms.addDay(st, t, td, ""); in.setText(""); focusTag = "dayIn"; sh.save();
        } };
        in.setOnEditorActionListener(new TextView.OnEditorActionListener() { public boolean onEditorAction(TextView v, int a, KeyEvent e) { if (e == null || e.getAction() == KeyEvent.ACTION_UP) addIt.run(); return true; } });
        FrameLayout plus = new FrameLayout(c); plus.setBackground(NUi.ripple(NUi.round(NTheme.accent, 14, 0), 14));
        android.widget.ImageView pi = new android.widget.ImageView(c); pi.setImageDrawable(new NIcon("plus", NTheme.onAccent).stroke(2.2f));
        plus.addView(pi, new FrameLayout.LayoutParams(NUi.dp(18), NUi.dp(18), Gravity.CENTER));
        NUi.tap(plus, new View.OnClickListener() { public void onClick(View v) { addIt.run(); } });
        View mic = NVoice.button(c, in);
        if (mic != null) { LinearLayout.LayoutParams ml = NUi.lp(NUi.dp(40), NUi.dp(40)); ml.rightMargin = NUi.dp(10); addRow.addView(mic, ml); }
        addRow.addView(plus, NUi.lp(NUi.dp(40), NUi.dp(40)));
        box.addView(addRow);
        for (final JSONObject x : NActs.daysOn(st, td)) {
            box.addView(NBits.divider(c));
            box.addView(dayRow(x, false));
        }
        /* still open from earlier (last 7 days) */
        List<JSONObject> carry = new ArrayList<>();
        int tn = NDates.today();
        for (JSONObject x : NStore.list(st.arr("days"))) { int n = NDates.dnum(x.optString("date")); if (!x.optBoolean("done") && n < tn && n >= tn - 14) carry.add(x); }
        if (!carry.isEmpty()) {
            box.addView(NBits.divider(c));
            TextView h = NUi.label(c, "Still open from earlier", NTheme.muted); h.setPadding(NUi.dp(20), NUi.dp(14), 0, NUi.dp(2)); box.addView(h);
            for (int i = 0; i < Math.min(5, carry.size()); i++) { if (i > 0) box.addView(NBits.divider(c)); box.addView(dayRow(carry.get(i), true)); }
        }
        add(box);
    }

    View dayRow(final JSONObject x, boolean carry) {
        JSONObject g = null; String gid = NStore.s(x, "goalId"); if (!gid.isEmpty()) g = st.find("goals", gid);
        int gc = g == null ? NTheme.accent : NTheme.areaCol(g.optString("area"));
        View chk = NBits.check(c, x.optBoolean("done"), gc, new View.OnClickListener() { public void onClick(View v) {
            boolean d = NActs.toggleDay(x);
            if (d) { List<JSONObject> all = NActs.daysOn(st, NDates.ymd()); boolean every = all.size() > 1; for (JSONObject y : all) if (!y.optBoolean("done")) every = false; if (every) NShell.toast("Every goal for today is done 🎉"); }
            sh.save();
        } });
        String sub = g != null ? g.optString("title") : "";
        if (carry) sub = (sub.isEmpty() ? "" : sub + " · ") + NDates.dayName(x.optString("date"));
        View t1 = null, t2 = null;
        if (!carry && !x.optString("time").isEmpty()) { TextView tm = NBits.meta(c, NDates.fmtTime(x.optString("time")).toUpperCase(), NTheme.muted); tm.setTextSize(10.5f); t1 = tm; }
        if (carry) {
            TextView mv = NUi.body(c, "Move to today", 13, NTheme.text, 600); mv.setGravity(Gravity.CENTER); mv.setPadding(NUi.dp(14), 0, NUi.dp(14), 0);
            mv.setBackground(NUi.ripple(NUi.round(NTheme.surface, 99, NTheme.line2), 99));
            NUi.tap(mv, new View.OnClickListener() { public void onClick(View v) { try { x.put("date", NDates.ymd()); } catch (Exception ignored) { } sh.save(); NShell.toast("Moved to today"); } });
            mv.setLayoutParams(NUi.lp(-2, NUi.dp(34))); t2 = mv;
        } else {
            final String xid = x.optString("id");
            boolean on = NHome.dayRemOn(x);
            android.widget.ImageView bl = NUi.icon(c, "bell", 17, on ? NTheme.accent : NTheme.muted); bl.setAlpha(on ? 1f : .55f);
            NUi.tap(bl, new View.OnClickListener() { public void onClick(View v) { NReminders.open(sh, "day", xid); } });
            t2 = x.optBoolean("done") ? null : bl;
        }
        LinearLayout r = NBits.li(c, chk, x.optString("title"), x.optBoolean("done"), g == null ? 0 : gc, sub, 12, t1, t2);
        NUi.tap(r, new View.OnClickListener() { public void onClick(View v) { new NForms(sh).dayGoal(x.optString("date"), x); } });
        r.setOnLongClickListener(new View.OnLongClickListener() { public boolean onLongClick(View v) { dayMenu(x); return true; } });
        return r;
    }

    void dayMenu(final JSONObject x) {
        LinearLayout b = new NForms(sh).sheetBody(NDates.dayName(x.optString("date")), x.optString("title"), null);
        b.addView(NForms.actions(c,
            NUi.btn(c, "Delete", false, new View.OnClickListener() { public void onClick(View v) { st.remove("days", x.optString("id")); sh.closeSheet(); sh.save(); NShell.toast("Deleted"); } }),
            NUi.btn(c, "Tomorrow", false, new View.OnClickListener() { public void onClick(View v) { try { x.put("date", NDates.fromN(NDates.today() + 1)); } catch (Exception ignored) { } sh.closeSheet(); sh.save(); NShell.toast("Moved to tomorrow"); } })));
        sh.sheet(b);
    }

    /* ---- today's habits: the web's horizontal strip of pills ---- */
    void habits() {
        final List<JSONObject> hs = todayOrdered(st);
        boolean anyQuit = false;
        for (JSONObject h : NStore.list(st.arr("habits"))) if ("active".equals(NHabits.status(h)) && NHabits.kind(h).equals("quit")) anyQuit = true;
        if (hs.isEmpty() && !anyQuit) return;
        int done = 0; for (JSONObject h : hs) if (NHabits.done(h, NDates.ymd())) done++;
        LinearLayout hh = NUi.sectionHead(c, "Habits", "All habits", new View.OnClickListener() { public void onClick(View v) { sh.tab(1); } });
        if (!hs.isEmpty()) {
            LinearLayout rg = NUi.row(c); rg.setPadding(NUi.dp(10), 0, 0, 0);
            NRing mr = new NRing(c); mr.strokeDp = 3.5f; mr.set(done / (float) hs.size(), false); rg.addView(mr, new LinearLayout.LayoutParams(NUi.dp(22), NUi.dp(22)));
            TextView ct = NBits.meta(c, done + " OF " + hs.size(), NTheme.muted); ct.setTextSize(10.5f); ct.setPadding(NUi.dp(10), 0, 0, 0); rg.addView(ct);
            hh.addView(rg, 1);
            ((LinearLayout.LayoutParams) hh.getChildAt(0).getLayoutParams()).weight = 0; hh.getChildAt(0).getLayoutParams().width = -2;
            View gsp = new View(c); hh.addView(gsp, 2, NUi.lpw(0, 1, 1));
        }
        add(hh);
        LinearLayout row = NUi.row(c); row.setPadding(NUi.dp(2), NUi.dp(2), NUi.dp(2), NUi.dp(10));
        final String td = NDates.ymd();
        for (final JSONObject h : hs) {
            int ac = NTheme.areaCol(h.optString("area"));
            boolean on = NHabits.done(h, td), skip = NHabits.skip(h, td);
            LinearLayout t = NUi.col(c);
            t.setBackground(NUi.ripple(NUi.round(on ? NUi.mix(ac, .07f, NTheme.surface) : NTheme.surface, 22, on ? NTheme.alpha(ac, .45f) : NTheme.line), 22));
            t.setPadding(NUi.dp(14), NUi.dp(14), NUi.dp(14), NUi.dp(14));
            if (skip) t.setAlpha(.6f);
            LinearLayout top = NUi.row(c); top.setMinimumHeight(NUi.dp(50));
            TextView ic = hic(NHabits.icon(h), ac);
            top.addView(ic, NUi.lp(NUi.dp(36), NUi.dp(36)));
            View sp = new View(c); top.addView(sp, NUi.lpw(0, 1, 1));
            final NRing ring = NBits.habitRing(c, h, 44);
            final boolean rt = NHabits.routine(h) && !on;
            if (rt) ring.glyph = "play";
            if (!skip) top.addView(ring);
            top.setGravity(Gravity.CENTER_VERTICAL);
            t.addView(top);
            t.addView(NUi.ell(NUi.body(c, h.optString("title"), 14.5f, NTheme.text, 600), 1), NUi.mt(6));
            int s = NHabits.streak(h);
            String sub = skip ? "Rest day" : s > 0 ? s + (NHabits.freq(h).equals("times") ? " wk" : " d") : NHabits.routine(h) ? NHabits.steps(h).length() + (NHabits.steps(h).length() == 1 ? " step" : " steps") : NHabits.target(h) > 1 ? NHabits.val(h, td) + "/" + NHabits.target(h) + " " + h.optString("unit") : "Start today";
            TextView sd = NJCards.data(c, sub, NTheme.muted); sd.setSingleLine(true); sd.setEllipsize(android.text.TextUtils.TruncateAt.END);
            if (!skip && s > 0) { android.graphics.drawable.Drawable fl = NUi.iconD("flamef", 12, ac); sd.setCompoundDrawablesRelative(fl, null, null, null); sd.setCompoundDrawablePadding(NUi.dp(4)); }
            t.addView(sd, NUi.mt(6));
            final View.OnClickListener tapRing = new View.OnClickListener() { public void onClick(View v) {
                NUi.haptic(v);
                if (rt) { NUrge.routine(sh, h.optString("id")); return; }
                boolean full = NActs.habitTap(h);
                int val = NHabits.val(h, NDates.ymd()), n = NHabits.target(h);
                ring.set(val / (float) n, true); ring.text(n > 1 && val < n ? String.valueOf(val) : "", n > 1 && val < n ? "/" + n : "");
                String m = full ? NActs.milestone(st, h) : null;
                sh.saveQuiet();
                if (m != null) NShell.toast(m);
            } };
            NUi.tap(ring, tapRing);
            NUi.tap(t, new View.OnClickListener() { public void onClick(View v) { sh.push(new NHabitScreen(sh, h.optString("id"))); } });
            LinearLayout.LayoutParams l = NUi.lp(NUi.dp(168), -2); l.rightMargin = NUi.dp(12); row.addView(t, l);
        }
        for (final JSONObject h : NStore.list(st.arr("habits"))) {
            if (!"active".equals(NHabits.status(h)) || !NHabits.kind(h).equals("quit")) continue;
            int ac = NTheme.areaCol(h.optString("area"));
            LinearLayout t = NUi.col(c);
            t.setBackground(NUi.ripple(NUi.round(NTheme.surface, 22, NTheme.line), 22));
            t.setPadding(NUi.dp(14), NUi.dp(14), NUi.dp(14), NUi.dp(14));
            LinearLayout top = NUi.row(c); top.setMinimumHeight(NUi.dp(50)); top.setGravity(Gravity.CENTER_VERTICAL);
            TextView ic = hic(NHabits.icon(h), ac);
            top.addView(ic, NUi.lp(NUi.dp(36), NUi.dp(36)));
            t.addView(top);
            t.addView(NUi.ell(NUi.body(c, h.optString("title"), 14.5f, NTheme.text, 600), 1), NUi.mt(6));
            TextView ck;
            if (NHabits.limitMode(h)) { JSONObject lg = h.optJSONObject("log"); ck = NJCards.data(c, (lg == null ? 0 : lg.optInt(NDates.ymd(), 0)) + " of " + NHabits.limit(h) + " today", NTheme.muted); }
            else {
                /* web clockHTML(short): <b>9</b>d <b>14</b>h, the numbers big */
                long ms = Math.max(0, System.currentTimeMillis() - NHabits.since(h)); long d = ms / 86400000L, hh2 = ms / 3600000L % 24;
                android.text.SpannableStringBuilder sb = new android.text.SpannableStringBuilder();
                big(sb, String.valueOf(d)); sb.append("D "); big(sb, String.valueOf(hh2)); sb.append("H");
                ck = NJCards.data(c, "", NTheme.muted); ck.setText(sb);
            }
            ck.setSingleLine(true); t.addView(ck, NUi.mt(6));
            NUi.tap(t, new View.OnClickListener() { public void onClick(View v) { sh.push(new NHabitScreen(sh, h.optString("id"))); } });
            LinearLayout.LayoutParams l = NUi.lp(NUi.dp(168), -2); l.rightMargin = NUi.dp(12); row.addView(t, l);
        }
        add(NBits.hscroll(c, row));
    }

    /* web hasRem(step): a reminder is set and the step has a date */
    static boolean stepRem(JSONObject s) {
        if (s.optBoolean("done") || !NDates.valid(s.optString("due"))) return false;
        Object r = s.opt("remind"); return r != null && r != JSONObject.NULL && !String.valueOf(r).isEmpty() && !"false".equals(String.valueOf(r));
    }

    /* web dayRem/dayRemAt: no "remind" field + a time = remind at that time; "" = off */
    static boolean dayRemOn(JSONObject x) {
        String r = x.has("remind") && !x.isNull("remind") ? String.valueOf(x.opt("remind")) : (x.optString("time").isEmpty() ? "" : "0");
        return !r.isEmpty() && !"false".equals(r);
    }

    /* web todayHabits(): due today, not quit, by time of day then creation order */
    static List<JSONObject> todayOrdered(NStore st) {
        List<JSONObject> l = new ArrayList<>(NHabits.today(st.arr("habits")));
        java.util.Collections.sort(l, new java.util.Comparator<JSONObject>() { public int compare(JSONObject a, JSONObject b) {
            String ta = a.optString("time", ""), tb = b.optString("time", ""); if (ta.isEmpty()) ta = "99"; if (tb.isEmpty()) tb = "99";
            int c = ta.compareTo(tb); return c != 0 ? c : Long.compare(a.optLong("createdAt"), b.optLong("createdAt"));
        } });
        return l;
    }

    /* .hic.sm: the habit's emoji in a soft tile of its colour */
    TextView hic(String e, int ac) {
        TextView ic = NUi.text(c, e, 18, NTheme.text); ic.setGravity(Gravity.CENTER); ic.setIncludeFontPadding(false);
        ic.setBackground(NUi.round(NTheme.alpha(ac, .16f), 12, NTheme.alpha(ac, .3f)));
        ic.setLayoutParams(NUi.lp(NUi.dp(36), NUi.dp(36)));
        return ic;
    }

    void big(android.text.SpannableStringBuilder sb, String n) {
        int a = sb.length(); sb.append(n);
        sb.setSpan(new android.text.style.AbsoluteSizeSpan(20, true), a, sb.length(), 0);
        sb.setSpan(new android.text.style.ForegroundColorSpan(NTheme.text), a, sb.length(), 0);
        sb.setSpan(new android.text.style.TypefaceSpan(NFont.display(800)), a, sb.length(), 0);
    }

    /* ---- recent threads ---- */
    void threads(final NForms F) {
        List<JSONObject> ts = NJournalPage.sortedThreads(st, true);
        LinearLayout hd = NUi.sectionHead(c, "Threads", "All threads", new View.OnClickListener() { public void onClick(View v) { sh.route("threads"); } });
        if (!ts.isEmpty()) { TextView nw = NUi.link(c, "New", new View.OnClickListener() { public void onClick(View v) { F.thread(null); } }); nw.setPadding(0, 0, NUi.dp(16), 0); hd.addView(nw, hd.getChildCount() - 1); }
        add(hd);
        LinearLayout box = NBits.listBox(c);
        if (ts.isEmpty()) {
            LinearLayout r = NBits.li(c, thrEm("🧵"), "Start a thread", false, 0, "Log a thought or a status, then build on it", 12, NUi.icon(c, "plus", 16, NTheme.muted));
            ((LinearLayout.LayoutParams) r.getChildAt(0).getLayoutParams()).rightMargin = NUi.dp(12);
            NUi.tap(r, new View.OnClickListener() { public void onClick(View v) { F.thread(null); } });
            box.addView(r); add(box); return;
        }
        for (int i = 0; i < Math.min(3, ts.size()); i++) {
            final JSONObject t = ts.get(i); JSONObject u = NJournalPage.lastUp(t);
            if (i > 0) box.addView(NBits.divider(c));
            TextView ago = NJCards.data(c, NDates.ago(NJournalPage.last(t)), NTheme.muted); ago.setTextSize(11);
            View add = NBits.sbtn(c, "plus", new View.OnClickListener() { public void onClick(View v) { F.threadAdd(t); } });
            add.setLayoutParams(NUi.lp(NUi.dp(38), NUi.dp(38)));
            LinearLayout r = NBits.li(c, thrEm(u != null ? NJCards.tag(u.optString("k"))[1] : "🧵"), t.optString("title"), false, 0, u != null ? u.optString("x").replace('\n', ' ') : "Nothing logged yet", 12, ago, add);
            ((LinearLayout.LayoutParams) r.getChildAt(0).getLayoutParams()).rightMargin = NUi.dp(12);
            for (int k = 2; k < r.getChildCount(); k++) ((LinearLayout.LayoutParams) r.getChildAt(k).getLayoutParams()).leftMargin = NUi.dp(12);
            r.setGravity(Gravity.CENTER_VERTICAL);
            NUi.tap(r, new View.OnClickListener() { public void onClick(View v) { sh.route("thread/" + t.optString("id")); } });
            box.addView(r);
        }
        add(box);
    }

    TextView thrEm(String e) {
        TextView em = NUi.text(c, e, 18, NTheme.text); em.setGravity(Gravity.CENTER); em.setIncludeFontPadding(false);
        em.setBackground(NUi.round(NTheme.surface2, 12, 0)); em.setLayoutParams(NUi.lp(NUi.dp(38), NUi.dp(38)));
        return em;
    }

    /* ---- next steps of the other goals ---- */
    void comingUp() {
        List<JSONObject[]> up = NActs.upNext(st);
        if (up.size() < 2) return;
        LinearLayout chd = NUi.sectionHead(c, "Coming up", "All goals", new View.OnClickListener() { public void onClick(View v) { sh.tab(3); } });
        TextView wr = NUi.link(c, "Weekly review", new View.OnClickListener() { public void onClick(View v) { NSheets.weekly(sh); } }); wr.setPadding(0, 0, NUi.dp(16), 0);
        chd.addView(wr, chd.getChildCount() - 1); add(chd);
        LinearLayout box = NBits.listBox(c);
        for (int i = 1; i < Math.min(5, up.size()); i++) {   /* web nx.slice(1,5) */
            final JSONObject g = up.get(i)[0], s = up.get(i)[1];
            if (i > 1) box.addView(NBits.divider(c));
            View chk = NBits.check(c, false, NTheme.areaCol(g.optString("area")), new View.OnClickListener() { public void onClick(View v) { String m = NActs.toggleStep(st, g, s); sh.save(); NShell.toast(m != null ? m : "Step done"); } });
            String due = NDates.dueShort(s.optString("due"));
            int du = NDates.valid(s.optString("due")) ? NDates.daysUntil(s.optString("due")) : 99;
            TextView d = NBits.meta(c, due.toUpperCase(), du < 0 ? NTheme.LATE : du <= 1 ? NTheme.accent : NTheme.muted); d.setTextSize(10.5f); d.setLetterSpacing(.05f);
            if (stepRem(s)) { d.setCompoundDrawablesRelative(null, null, NUi.iconD("bell", 13, NTheme.muted), null); d.setCompoundDrawablePadding(NUi.dp(5)); }
            LinearLayout r = NBits.li(c, chk, s.optString("title"), false, NTheme.areaCol(g.optString("area")), g.optString("title"), 15, d);
            NUi.tap(r, new View.OnClickListener() { public void onClick(View v) { sh.push(new NGoalScreen(sh, g.optString("id"))); } });
            box.addView(r);
        }
        add(box);
    }

    /* ---- activity from the phone (steps, sleep, screen time), as the web engine records it ---- */
    void activity() {
        JSONObject log = st.settings().optJSONObject("actLog");
        if (log == null) return;
        JSONObject t = log.optJSONObject(NDates.ymd()), y = log.optJSONObject(NDates.fromN(NDates.today() - 1));
        if (t == null && y == null) return;
        add(NUi.sectionHead(c, "Activity", "Details", new View.OnClickListener() { public void onClick(View v) { sh.push(new NActivityScreen(sh)); } }));
        GridLayout g = new GridLayout(c); g.setColumnCount(2);
        int steps = t == null ? 0 : t.optInt("s", 0), sleep = t == null ? 0 : t.optInt("z", 0), work = t == null ? 0 : t.optInt("x", 0), screen = t == null ? 0 : t.optInt("m", 0);
        tile(g, "👟", steps > 0 ? String.format(java.util.Locale.US, "%,d", steps) : "—", "steps · goal 8k", steps / 8000f);
        tile(g, "😴", sleep > 0 ? mins(sleep) : "—", sleep > 0 ? "sleep" : "sleep · not recorded", -1);
        tile(g, "🏋️", work > 0 ? mins(work) : "—", "workout today", -1);
        tile(g, "📱", screen > 0 ? mins(screen) : "—", "screen time", -1);
        LinearLayout.LayoutParams gl = NUi.mt(0); gl.leftMargin = gl.rightMargin = -NUi.dp(4);
        body.addView(g, gl);
        if (y != null && y.optInt("s", 0) > 0) {
            TextView yt = NUi.link(c, "Yesterday: " + String.format(java.util.Locale.US, "%,d", y.optInt("s")) + " steps" + (y.optInt("m", 0) > 0 ? " · " + mins(y.optInt("m")) + " screen" : "") + "  ›", new View.OnClickListener() { public void onClick(View v) { sh.push(new NActivityScreen(sh)); } });
            yt.setTextColor(NTheme.muted); add(yt, 6);
        }
    }

    static String mins(int m) { return m >= 60 ? (m / 60) + "h " + String.format(java.util.Locale.US, "%02d", m % 60) + "m" : m + "m"; }

    void tile(GridLayout g, String emoji, String big, String lab, float prog) {
        LinearLayout t = NUi.col(c);
        t.setBackground(NUi.card(22));
        t.setPadding(NUi.dp(16), NUi.dp(14), NUi.dp(16), NUi.dp(14));
        t.addView(NUi.text(c, emoji, 22, NTheme.text));
        TextView b = NUi.text(c, big, 26, NTheme.text); b.setTypeface(NFont.display(800)); t.addView(b, NUi.mt(6));
        t.addView(NUi.ell(NUi.text(c, lab, 13, NTheme.muted), 1));
        if (prog >= 0) t.addView(NBits.bar(c, Math.min(1, prog), NTheme.accent), NUi.mt(8));
        NUi.tap(t, new View.OnClickListener() { public void onClick(View v) { sh.push(new NActivityScreen(sh)); } });
        GridLayout.LayoutParams l = new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f));
        l.width = 0; l.setMargins(NUi.dp(4), NUi.dp(4), NUi.dp(4), NUi.dp(4));
        g.addView(t, l);
    }
}
