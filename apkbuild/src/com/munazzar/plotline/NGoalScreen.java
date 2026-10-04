package com.munazzar.plotline;

import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.ViewGroup;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/* One goal: hero card, the step list (tick, edit, add), sub-goals, and options. */
final class NGoalScreen extends NPage {
    final android.os.Handler shLive = new android.os.Handler(android.os.Looper.getMainLooper());
    final String id;
    NGoalScreen(NShell sh, String id) { super(sh); this.id = id; back = true; }

    @Override void build() {
        final JSONObject g = st.find("goals", id);
        if (g == null) { header(null); add(NBits.empty(c, "This goal was removed", null), 20); return; }
        final NForms F = new NForms(sh);
        final String groute = "goal/" + g.optString("id"); final JSONObject gd = NMore.d("id", g.optString("id"), "k", "goal");
        header(null,
            NUi.ibtn(c, "link", new View.OnClickListener() { public void onClick(View v) { new NForms(sh).threadsFor("goal", g.optString("id")); } }),
            NUi.ibtn(c, "bell", new View.OnClickListener() { public void onClick(View v) { NReminders.open(sh, "goal", g.optString("id")); } }),
            NUi.ibtn(c, "pin", g.optBoolean("pinned"), new View.OnClickListener() { public void onClick(View v) { try { g.put("pinned", !g.optBoolean("pinned")); } catch (Exception ignored) { } sh.save(); } }),
            NUi.ibtn(c, "edit", new View.OnClickListener() { public void onClick(View v) { F.goal(g); } }),
            NUi.ibtn(c, "ai", new View.OnClickListener() { public void onClick(View v) { sh.ask("How am I doing with my goal “" + g.optString("title") + "”? What’s working, what’s stuck, and what should I do next?"); } }),
            NUi.ibtn(c, "more", new View.OnClickListener() { public void onClick(View v) { more(g); } }));
        int col = NTheme.areaCol(g.optString("area"));
        boolean done = "done".equals(g.optString("status"));

        List<JSONObject> anc = new java.util.ArrayList<>(); JSONObject pp = st.find("goals", g.optString("parent")); int guard = 0;
        while (pp != null && guard++ < 12) { anc.add(0, pp); pp = st.find("goals", pp.optString("parent")); }
        if (!anc.isEmpty()) {
            NFlow cr = new NFlow(c, 6, 6);
            for (int ai = 0; ai < anc.size(); ai++) {
                final JSONObject a = anc.get(ai);
                if (ai > 0) { NUi.Fix nx = new NUi.Fix(c, NUi.dp(12), NUi.dp(12)); nx.addView(NUi.icon(c, "next", 12, NTheme.muted), new FrameLayout.LayoutParams(NUi.dp(12), NUi.dp(12))); cr.addView(nx, new ViewGroup.MarginLayoutParams(NUi.dp(12), NUi.dp(12))); }
                LinearLayout pill = NUi.row(c); pill.setPadding(NUi.dp(10), NUi.dp(6), NUi.dp(12), NUi.dp(6)); pill.setBackground(NUi.ripple(NUi.round(NTheme.surface, 99, NTheme.line), 99));
                View d = new View(c); d.setBackground(NUi.oval(NTheme.areaCol(a.optString("area")), 0, 0)); LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(NUi.dp(8), NUi.dp(8)); dl.rightMargin = NUi.dp(7); pill.addView(d, dl);
                String at = a.optString("title"); pill.addView(NUi.body(c, at.length() > 32 ? at.substring(0, 31) + "…" : at, 12.5f, NTheme.text, 600));
                NUi.tap(pill, new View.OnClickListener() { public void onClick(View v) { sh.push(new NGoalScreen(sh, a.optString("id"))); } });
                cr.addView(pill, new ViewGroup.MarginLayoutParams(-2, -2));
            }
            add(cr, 14);
        }
        boolean heroImg = NTheme.pushImg(g);
        LinearLayout hero = NUi.col(c);
        hero.setBackground(NCard.bgFor(g, col, 30));
        hero.setPadding(NUi.dp(22), NUi.dp(22), NUi.dp(22), NUi.dp(22));
        String hz = g.optString("horizon", "month");
        String[][] H = {{"week", "This week"}, {"month", "This month"}, {"quarter", "3 months"}, {"year", "This year"}, {"multi", "1–5 years"}, {"life", "Lifetime"}};
        String hzl = "This month"; for (String[] x : H) if (x[0].equals(hz)) hzl = x[1];
        String pri = g.optInt("priority", 2) == 1 ? " · High priority" : g.optInt("priority", 2) == 3 ? " · Low priority" : "";
        hero.addView(NUi.ell(NUi.label(c, NTheme.areaName(g.optString("area")) + " · " + hzl + pri, NTheme.INK_MUTED), 2));
        TextView t = NUi.text(c, g.optString("title").toUpperCase(), 40, NTheme.INK); t.setTypeface(NFont.display(800)); t.setLineSpacing(0, .9f); t.setIncludeFontPadding(false);
        hero.addView(t, NUi.mt(12));
        if (!g.optString("why").isEmpty()) { TextView w = NUi.text(c, g.optString("why"), 16, NTheme.INK_MUTED); w.setLineSpacing(0, 1.2f); hero.addView(w, NUi.mt(10)); }
        LinearLayout pr = NUi.row(c); pr.setGravity(Gravity.BOTTOM);
        LinearLayout left = NUi.col(c);
        JSONArray steps = g.optJSONArray("steps"); int n = steps == null ? 0 : steps.length();
        String dates = (NDates.valid(g.optString("startDate")) ? NDates.fmtDate(g.optString("startDate")) : "") + (NDates.valid(g.optString("targetDate")) ? " → " + NDates.fmtDate(g.optString("targetDate")) : "");
        left.addView(NUi.label(c, (dates.isEmpty() ? "" : NDates.valid(g.optString("targetDate")) ? dates + " · " : dates + " → open-ended · ") + (NActs.kids(st, g).size() > 0 ? NActs.kids(st, g).size() + " sub-goal" + (NActs.kids(st, g).size() > 1 ? "s" : "") + (n > 0 ? " · " : "") : "") + (n > 0 || NActs.kids(st, g).isEmpty() ? NActs.doneSteps(g) + " of " + n + " steps" : ""), NTheme.INK_MUTED));
        int p = NActs.pct(st, g);
        left.addView(NBits.bar(c, p / 100f, NTheme.INK), NUi.mt(10));
        pr.addView(left, NUi.lpw(0, -2, 1));
        android.text.SpannableString ps = new android.text.SpannableString(done ? "✓" : p + "%"); if (!done) ps.setSpan(new android.text.style.RelativeSizeSpan(.4f), String.valueOf(p).length(), ps.length(), 0);
        TextView pct = NUi.text(c, ps, 54, NTheme.INK); pct.setTypeface(NFont.display(800)); pct.setIncludeFontPadding(false); pct.setPadding(NUi.dp(16), 0, 0, 0);
        pr.addView(pct);
        hero.addView(pr, NUi.mt(18));
        if (done) hero.addView(NUi.label(c, "Achieved " + NDates.dayLabel(g.optLong("completedAt")), NTheme.INK), NUi.mt(10));
        add(hero, 14); NTheme.popImg(heroImg);
        { LinearLayout shp = NUi.col(c); shp.setVisibility(View.GONE); add(shp, 12); NShare.panel(sh, "goal", g.optString("id"), shp, shLive); }

        if (NActs.kids(st, g).isEmpty()) {
            TextView sa = NUi.body(c, "+  Add a sub-goal", 14.5f, NTheme.accent, 600); sa.setPadding(NUi.dp(2), NUi.dp(14), NUi.dp(8), NUi.dp(2));
            NUi.tap(sa, new View.OnClickListener() { public void onClick(View v) { F.goal(null, g.optString("id")); } });
            add(sa, 0);
        }
        /* Cards / Path / Timeline */
        JSONObject lay = st.settings().optJSONObject("layout"); String mode = lay == null ? "cards" : lay.optString("goal", "cards"); if (!mode.equals("path") && !mode.equals("orbit")) mode = "cards";
        add(NBits.seg(c, new String[][]{{"cards", "Cards"}, {"path", "Path"}, {"orbit", "Timeline"}}, mode, new NBits.Pick() { public void on(String k) {
            try { JSONObject l = st.settings().optJSONObject("layout"); if (l == null) { l = new JSONObject(); st.settings().put("layout", l); } l.put("goal", k); } catch (Exception ignored) { }
            sh.save(); refresh();
        } }), 18);
        boolean stImg = NTheme.pushImg(g); NGoalSteps S = new NGoalSteps(sh, g); NTheme.popImg(stImg);
        boolean any = n > 0 || !NActs.kids(st, g).isEmpty();
        if (!any) {
            LinearLayout e = NUi.col(c); e.setBackground(NUi.dashed(0, 22, NTheme.line2, 1.5f)); e.setPadding(NUi.dp(20), NUi.dp(26), NUi.dp(20), NUi.dp(26));
            e.addView(NUi.title(c, "Plot the path", 24));
            TextView ep = muted("Break this goal into steps you can finish in a day or a week."); ep.setPadding(0, NUi.dp(6), 0, NUi.dp(14)); e.addView(ep);
            e.addView(NUi.btn(c, "+  Add the first step", true, new View.OnClickListener() { public void onClick(View v) { F.step(g, null); } }), NUi.lp(-2, -2));
            add(e, 18);
        } else if (mode.equals("orbit")) timeline(F, g);
        else if (mode.equals("path")) add(S.path(), 18); else add(S.cards(), 8);

        /* habits for this goal */
        List<JSONObject> gh = new java.util.ArrayList<>();
        for (JSONObject h : NStore.list(st.arr("habits"))) if (g.optString("id").equals(NStore.s(h, "goalId")) && !"archived".equals(NHabits.status(h))) gh.add(h);
        if (!gh.isEmpty()) {
            add(NUi.sectionHead(c, "Habits for this goal", "All habits", new View.OnClickListener() { public void onClick(View v) { sh.tabTo(1); } }));
            LinearLayout hb = NBits.listBox(c);
            for (final JSONObject h : gh) {
                if (hb.getChildCount() > 0) hb.addView(NBits.divider(c));
                TextView ic = NUi.text(c, NHabits.icon(h), 20, NTheme.text);
                LinearLayout r = NBits.li(c, ic, h.optString("title"), false, 0, NBits.habitSub(h), 12);
                NUi.tap(r, new View.OnClickListener() { public void onClick(View v) { sh.push(new NHabitScreen(sh, h.optString("id"))); } });
                hb.addView(r);
            }
            add(hb);
        }

        /* connected goals */
        JSONArray lk = g.optJSONArray("links"); List<JSONObject> links = new java.util.ArrayList<>();
        if (lk != null) for (int i = 0; i < lk.length(); i++) { JSONObject o = st.find("goals", lk.optString(i)); if (o != null) links.add(o); }
        add(NUi.sectionHead(c, "Connected goals", links.isEmpty() ? "Connect" : "Edit", new View.OnClickListener() { public void onClick(View v) { new NForms(sh).connect(g); } }));
        if (links.isEmpty()) add(muted("Link goals that feed each other. The links show up as lines on your life map."));
        else {
            NFlow cf = new NFlow(c, 8, 8);
            for (final JSONObject o : links) {
                LinearLayout pill = NUi.row(c); pill.setPadding(NUi.dp(12), NUi.dp(9), NUi.dp(16), NUi.dp(9)); pill.setBackground(NUi.ripple(NUi.round(NTheme.surface, 99, NTheme.line), 99));
                View d = new View(c); d.setBackground(NUi.oval(NTheme.areaCol(o.optString("area")), 0, 0)); LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(NUi.dp(10), NUi.dp(10)); dl.rightMargin = NUi.dp(9); pill.addView(d, dl);
                pill.addView(NUi.body(c, o.optString("title"), 14, NTheme.text, 600));
                TextView pc = NUi.text(c, NActs.pct(st, o) + "%", 10.5f, NTheme.muted); pc.setTypeface(NFont.mono(500)); pc.setPadding(NUi.dp(9), 0, 0, 0); pill.addView(pc);
                NUi.tap(pill, new View.OnClickListener() { public void onClick(View v) { sh.push(new NGoalScreen(sh, o.optString("id"))); } });
                cf.addView(pill, new ViewGroup.MarginLayoutParams(-2, -2));
            }
            add(cf);
        }

        /* moments for this goal */
        List<JSONObject> ents = NJournalPage.entries(st, g.optString("id"));
        add(NUi.sectionHead(c, "Moments", "Add", new View.OnClickListener() { public void onClick(View v) { new NForms(sh).entry(null, g.optString("id"), null); } }));
        if (ents.isEmpty()) add(muted("Notes and photos about this goal. They also appear in your journal."));
        if (!ents.isEmpty()) {
            LinearLayout eb = NBits.listBox(c);
            for (int i = 0; i < Math.min(8, ents.size()); i++) { if (i > 0) eb.addView(NBits.divider(c)); eb.addView(NJournalPage.entryRow(sh, ents.get(i))); }
            add(eb);
        }
    }

    void more(final JSONObject g) {
        NForms F = new NForms(sh);
        LinearLayout b = F.sheetBody(g.optString("title"), "Goal options", null);
        final boolean done = "done".equals(g.optString("status"));
        final boolean pinned = g.optBoolean("pinned");
        String[][] items = {{"📌", pinned ? "Unpin" : "Pin to the top", "Pinned goals come first"}, {done ? "↩️" : "🏆", done ? "Mark as active again" : "Mark as achieved", done ? "Move it back to active" : "Celebrate and file it under Achieved"}, {"🗑️", "Delete goal", "Removes the goal and its steps"}};
        for (int i = 0; i < items.length; i++) {
            final int k = i;
            LinearLayout r = NUi.row(c);
            r.setBackground(NUi.ripple(NUi.round(NTheme.surface, 18, NTheme.line), 18));
            r.setPadding(NUi.dp(16), NUi.dp(14), NUi.dp(16), NUi.dp(14));
            r.addView(NUi.text(c, items[i][0], 22, NTheme.text), NUi.lp(NUi.dp(40), -2));
            LinearLayout tx = NUi.col(c); tx.addView(NUi.body(c, items[i][1], 16, k == 2 ? NTheme.LATE : NTheme.text, 700)); tx.addView(NUi.text(c, items[i][2], 13, NTheme.muted));
            r.addView(tx, NUi.lpw(0, -2, 1));
            NUi.tap(r, new View.OnClickListener() { public void onClick(View v) {
                try {
                    if (k == 0) g.put("pinned", !pinned);
                    else if (k == 1) { if (done) { g.put("status", "active"); g.put("completedAt", org.json.JSONObject.NULL); } else { NActs.completeGoal(st, g); NShell.toast("Goal achieved 🏆"); } }
                    else if (k == 2) { confirmDelete(g); return; }
                    else { sh.closeSheet(); sh.openClassic("goal/" + g.optString("id")); return; }
                } catch (Exception ignored) { }
                sh.closeSheet(); sh.save();
            } });
            b.addView(r, NUi.mt(i == 0 ? 16 : 8));
        }
        String rt = "goal/" + g.optString("id"); JSONObject gd = NMore.d("id", g.optString("id"), "k", "goal"), gi = NMore.d("id", g.optString("id"));
        NMore.add(b, NMore.row(c, "➕", "Add a sub-goal", "Break it into smaller goals", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); new NForms(sh).goal(null, g.optString("id")); } }));
        NMore.add(b, NMore.row(c, "⏰", "Reminders", "Check-ins and step nudges", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); final String gid0 = g.optString("id"); v.postDelayed(new Runnable() { public void run() { NReminders.open(sh, "goal", gid0); } }, 280); } }));
        NMore.add(b, NMore.row(c, "🤖", "Auto check-off", "Tick steps from what your phone sees", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); final String gid1 = g.optString("id"); v.postDelayed(new Runnable() { public void run() { NAutoEdit.pickStep(sh, gid1); } }, 280); } }));
        NMore.add(b, NMore.row(c, "🔗", "Connect to other goals", "What it supports or depends on", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); final JSONObject gg0 = g; v.postDelayed(new Runnable() { public void run() { new NForms(sh).connect(gg0); } }, 280); } }));
        NMore.add(b, NMore.row(c, "🧵", "Threads about this goal", "Start or open a thread", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); final String gid2 = g.optString("id"); v.postDelayed(new Runnable() { public void run() { new NForms(sh).threadsFor("goal", gid2); } }, 280); } }));
        NMore.add(b, NMore.row(c, "🤝", "Share or do it together…", "Invite someone, or share as a plan", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); NSheets.share(sh, "goal", g.optString("id"), null); } }));
        NMore.add(b, NMore.row(c, "📅", "Add dated steps to calendar", "Export to your calendar app", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); NSheets.ics(sh, "icsGoal", g.optString("id"), ""); } }));
        if (!g.optString("checkin").isEmpty()) NMore.add(b, NMore.row(c, "🔔", "Add check-ins to calendar", null, false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); NSheets.ics(sh, "icsCheckin", g.optString("id"), ""); } }));
        NMore.add(b, NMore.row(c, "📋", "Duplicate as template", "Same goal, fresh steps", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); sh.run("dupGoal", NMore.d("id", g.optString("id"))); } }));
        NMore.add(b, NMore.row(c, "💬", "Ask AI about this goal", "What's working, what's stuck", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); sh.ask("How am I doing with my goal “" + g.optString("title") + "”? What’s working, what’s stuck, and what should I do next?"); } }));
        sh.sheet(b);
    }

    void confirmDelete(final JSONObject g) {
        NForms F = new NForms(sh);
        LinearLayout b = F.sheetBody(g.optString("title"), "Delete this goal?", "Its steps go too, on every device. This can't be undone.");
        b.addView(NForms.actions(c, NUi.btn(c, "Cancel", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } }),
            NUi.btn(c, "Delete", true, new View.OnClickListener() { public void onClick(View v) { st.remove("goals", g.optString("id")); sh.closeSheet(); sh.save(); sh.pop(); NShell.toast("Goal deleted"); } })));
        sh.sheet(b);
    }

    /* Timeline mode: the goal (and each sub-goal) flowing on its own path; tap a planet to edit a step */
    void timeline(final NForms F, JSONObject g) {
        final boolean hasK = !NActs.kids(st, g).isEmpty();
        List<JSONObject> L = hasK ? NRoad.tree(st, g) : java.util.Collections.singletonList(g);
        final NRoad road = new NRoad(c, st, L, NRoad.ppdFit(L), true, new java.util.HashSet<String>(), !hasK, false, new NRoad.Cb() {
            public void toggle(String gid) { }
            public void step(JSONObject gg, JSONObject s) { F.step(gg, s); }
            public void open(JSONObject gg) { sh.push(new NGoalScreen(sh, gg.optString("id"))); }
            public void chapter(String id) { }
        });
        final android.widget.HorizontalScrollView hs = new android.widget.HorizontalScrollView(c) {
            @Override protected void onScrollChanged(int l, int t, int ol, int ot) { super.onScrollChanged(l, t, ol, ot); road.invalidate(); }
        };
        hs.setHorizontalScrollBarEnabled(false); hs.setOverScrollMode(View.OVER_SCROLL_NEVER);
        hs.addView(road, new android.widget.FrameLayout.LayoutParams(-2, -2));
        add(hs, 18); road.play();
        hs.post(new Runnable() { public void run() { hs.scrollTo(Math.max(0, Math.round((road.firstX() - 30) * NUi.density)), 0); } });
        TextView n = NUi.text(c, hasK ? "The goal and each sub-goal flow on their own path. Tap a planet to edit a step, or a name to open that sub-goal." : "Tap a planet to edit a step. Move the goal’s start date and the whole path moves with it.", 13, NTheme.muted);
        n.setLineSpacing(0, 1.25f); add(n, 12);
    }
}
