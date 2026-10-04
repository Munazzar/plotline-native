package com.munazzar.plotline;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

/* The guided tour, on the real native screens. It runs on a sandbox copy of example data: the engine pauses saving,
   syncing and reminders, nothing is written, and your own plan is put back when it ends. */
final class NTour {
    final NShell sh; final android.content.Context c;
    FrameLayout root, cardHost; int i = -1; boolean ended;

    /* step: title, text, centered?, kind(widget|pull|"") */
    static final String[][] STEPS = {
        {"Welcome to Plotline", "A two-minute walk through everything the app does. The tour runs on example data, so your own plan stays exactly as it is.", "1", ""},
        {"Today’s goals", "Type what you want to finish today and press +. Tap a circle to check it off. Anything unfinished from earlier waits below, one tap from moving to today.", "", ""},
        {"Up next", "The most urgent step across all your goals. Mark it done, or start a focus timer that chimes when the time is up.", "", ""},
        {"Habits at a glance", "Today’s habits sit right on the Today page. Tap a ring to log one, and swipe for the rest.", "", ""},
        {"Habits", "Build good habits, break bad ones and run routines. The ring is today, the bars are the last seven days.", "", ""},
        {"Log a habit", "Tap the ring to log it. Counted habits, like 8 glasses of water, fill one tap at a time. The dots are the last seven days: tap one to fix a day you forgot.", "", ""},
        {"Break a habit", "A live clock counts your time free, with milestones along the way. When a craving hits, tap “I have an urge” for a guided breathing timer. Slips are logged honestly and the clock starts again.", "", ""},
        {"Routines", "A routine is a short sequence you run the same way each time, like a morning start. Press Start and a timer walks you through each step.", "", ""},
        {"Templates", "Ready-made habits, habit breakers and routines. Tap one, adjust it, and you’re going. They’re always one tap away under Templates.", "", ""},
        {"Goals", "Every goal is a card. Tap one to flip it and see the next step and due date. Sub-goals sit right after the goal they belong to.", "", ""},
        {"Filter and switch views", "Show active, short-term, long-term or achieved goals, filter by life area, or switch to a swipeable carousel.", "", ""},
        {"A goal up close", "Why it matters, the dates, and progress that includes every sub-goal beneath it. Pin it to Today with the pin, edit it with the pencil.", "", ""},
        {"Three ways to see the path", "Cards, a path, or a flowing timeline. Steps and sub-goals, at any depth, appear in all three.", "", ""},
        {"Swipe through steps", "Swipe the cards. Tap a card to flip it for notes and actions, and tap its dot to mark the step done. Sub-goal cards open that sub-goal.", "", ""},
        {"Calendar", "Day goals, steps and goal targets together. Switch between Day, Week and Month, and swipe to move through dates.", "", ""},
        {"Plan the day by the hour", "Tap an empty slot to add something at that time. Pinch, or use the zoom buttons, to see more or less of the day.", "", ""},
        {"Timeline", "Every goal flows on its own path across the months, with steps as points along it. Tap a path to open it up.", "", ""},
        {"Life map", "Goals grouped by life area, with lines where goals support each other. Drag anything, pinch to zoom, tap to focus.", "", ""},
        {"Journal", "Moments, photos, milestones and achieved goals collect here as the story of your year.", "", ""},
        {"Plan with AI", "Describe what you want in your own words. Copy the prompt into any AI assistant, paste its reply back, then review one main goal with everything nested under it before anything is created.", "", ""},
        {"Sync and settings", "Connect your own Google Drive to keep phone and web in step. Themes, card styles, reminders, a PIN and backups are here too.", "", ""},
        {"Pull down to sync", "On a phone, pull down at the top of any page to sync or refresh.", "1", "pull"},
        {"Home-screen widgets", "Long-press your home screen, choose Widgets, then Plotline. Check off goals and habits, see your week, or watch a live clean-time clock without opening the app.", "1", "widget"},
        {"You’re all set", "Your own plan is back, exactly as you left it. Replay this tour any time from Settings.", "1", ""},
    };

    NTour(NShell sh) { this.sh = sh; this.c = sh.a; }

    static void start(final NShell sh) {
        if (sh.tour != null) return;
        sh.a.jsRet("(window.__ntourBegin?window.__ntourBegin():'')", new android.webkit.ValueCallback<String>() { public void onReceiveValue(String v) {
            try {
                String j = v;
                if (j != null && j.startsWith("\"")) j = new JSONObject("{\"x\":" + j + "}").getString("x");
                if (j == null || j.length() < 10) { NShell.toast("Couldn’t start the tour"); return; }
                sh.st.sandboxBegin(j);
                NTour t = new NTour(sh); sh.tour = t; t.show();
            } catch (Exception e) { NCrash.log(sh.a, "tour start", e); NShell.toast("Couldn’t start the tour"); }
        } });
    }

    void show() {
        sh.closeSheet(); if (sh.classic) sh.hideClassic(); sh.popAll();
        root = new FrameLayout(c); root.setClickable(true);
        cardHost = new FrameLayout(c);
        root.addView(cardHost, new FrameLayout.LayoutParams(-1, -1));
        TextView badge = NUi.label(c, "TOUR · EXAMPLE DATA", NTheme.accent); badge.setBackground(NUi.round(NTheme.bg2, 99, NTheme.line2)); badge.setPadding(NUi.dp(12), NUi.dp(6), NUi.dp(12), NUi.dp(6));
        FrameLayout.LayoutParams bl = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL); bl.topMargin = sh.top + NUi.dp(8);
        root.addView(badge, bl);
        sh.nroot.addView(root, new FrameLayout.LayoutParams(-1, -1));
        go(0);
    }

    JSONObject layout() { JSONObject l = sh.st.settings().optJSONObject("layout"); if (l == null) { l = new JSONObject(); try { sh.st.settings().put("layout", l); } catch (Exception ignored) { } } return l; }
    void lay(String k, String v) { try { layout().put(k, v); } catch (Exception ignored) { } sh.st.fire(); }

    String demoGoal() {
        JSONArray a = sh.st.arr("goals"); String first = "";
        for (int k = 0; k < a.length(); k++) {
            JSONObject g = a.optJSONObject(k); if (g == null) continue; if (first.isEmpty()) first = g.optString("id");
            for (int m = 0; m < a.length(); m++) { JSONObject h = a.optJSONObject(m); if (h != null && g.optString("id").equals(h.optString("parent"))) return g.optString("id"); }
        }
        return first;
    }

    void habits(String f) { NHabitsPage p = (NHabitsPage) sh.pages[1]; p.filter = f; sh.tabTo(1); p.refresh(); }

    void navigate(int n) {
        sh.closeSheet(); if (!sh.stack.isEmpty()) sh.popAll();
        switch (n) {
            case 0: case 1: case 2: case 3: sh.tabTo(0); break;
            case 4: case 5: habits("today"); break;
            case 6: habits("quit"); break;
            case 7: habits("routine"); break;
            case 8: habits("today"); sh.tabTo(1); sh.pager.postDelayed(new Runnable() { public void run() { if (!ended && i == 8) NHabitForm.templates(sh); } }, 450); break;
            case 9: case 10: lay("goals", "grid"); sh.tabTo(3); break;
            case 11: lay("goal", "cards"); sh.push(new NGoalScreen(sh, demoGoal())); break;
            case 12: lay("goal", "path"); sh.st.fire(); sh.push(new NGoalScreen(sh, demoGoal())); break;
            case 13: lay("goal", "cards"); sh.push(new NGoalScreen(sh, demoGoal())); break;
            case 14: lay("cal", "month"); sh.tabTo(2); break;
            case 15: lay("cal", "day"); sh.tabTo(2); break;
            case 16: ((NVistaPage) sh.pages[4]).setModeQuiet("road"); sh.tabTo(4); break;
            case 17: ((NVistaPage) sh.pages[4]).setModeQuiet("map"); sh.tabTo(4); break;
            case 18: sh.tabTo(5); break;
            case 19: sh.push(new NAiPlan(sh)); break;
            case 20: sh.openSettings(); break;
            default: sh.tabTo(0);
        }
    }

    void go(int n) {
        if (ended || n < 0) return;
        if (n >= STEPS.length) { end(true); return; }
        i = n; navigate(n); card();
    }

    void card() {
        cardHost.removeAllViews();
        String[] s = STEPS[i]; boolean mid = !s[2].isEmpty(), last = i == STEPS.length - 1;
        cardHost.setBackgroundColor(mid ? 0x99000000 : 0);
        LinearLayout k = NUi.col(c); k.setBackground(NUi.round(NTheme.bg2, 24, NTheme.line2)); k.setPadding(NUi.dp(18), NUi.dp(14), NUi.dp(18), NUi.dp(14)); k.setElevation(NUi.dp(12));
        LinearLayout bar = NUi.row(c); bar.setBackground(NUi.round(NTheme.line, 3, 0));
        View fill = new View(c); fill.setBackground(NUi.round(NTheme.accent, 3, 0)); bar.addView(fill, new LinearLayout.LayoutParams(0, NUi.dp(3), (i + 1f)));
        bar.addView(new View(c), new LinearLayout.LayoutParams(0, NUi.dp(3), STEPS.length - i - 1f));
        k.addView(bar, NUi.lp(-1, NUi.dp(3)));
        TextView n = NUi.label(c, "Step " + (i + 1) + " of " + STEPS.length, NTheme.muted); n.setPadding(0, NUi.dp(10), 0, 0); k.addView(n);
        if ("widget".equals(s[3])) k.addView(widgetMock(), NUi.mt(10));
        k.addView(NUi.title(c, s[0], 24), NUi.mt(6));
        TextView t = NUi.text(c, s[1], 14.5f, NTheme.muted); t.setLineSpacing(0, 1.25f); k.addView(t, NUi.mt(6));
        LinearLayout a = NUi.row(c); a.setGravity(Gravity.CENTER_VERTICAL);
        if (!last) a.addView(NUi.link(c, "Skip tour", new View.OnClickListener() { public void onClick(View v) { end(false); } }));
        a.addView(new View(c), NUi.lpw(0, 1, 1));
        if (i > 0) { LinearLayout.LayoutParams l = NUi.lp(-2, NUi.dp(44)); l.rightMargin = NUi.dp(8); a.addView(NUi.btn(c, "Back", false, new View.OnClickListener() { public void onClick(View v) { go(i - 1); } }), l); }
        a.addView(NUi.btn(c, last ? "Finish" : i > 0 ? "Next" : "Start", true, new View.OnClickListener() { public void onClick(View v) { go(i + 1); } }), NUi.lp(-2, NUi.dp(44)));
        k.addView(a, NUi.mt(14));
        FrameLayout.LayoutParams l = new FrameLayout.LayoutParams(-1, -2, mid ? Gravity.CENTER : Gravity.BOTTOM);
        l.leftMargin = l.rightMargin = NUi.dp(14); l.bottomMargin = mid ? 0 : sh.bot + NUi.dp(86);
        cardHost.addView(k, l);
    }

    View widgetMock() {
        LinearLayout w = NUi.col(c); w.setBackground(NUi.round(NTheme.surface, 16, NTheme.line)); w.setPadding(NUi.dp(12), NUi.dp(10), NUi.dp(12), NUi.dp(10));
        LinearLayout h = NUi.row(c); h.addView(NUi.label(c, "HABITS TODAY", NTheme.muted), NUi.lpw(0, -2, 1)); h.addView(NUi.body(c, "2/4", 12, NTheme.accent, 700)); w.addView(h);
        String[][] r = {{"💧", "Drink water", "3/8", "0"}, {"📖", "Read before bed", "✓", "1"}, {"💪", "Exercise", "", "0"}, {"🙏", "Pray on time", "✓", "1"}};
        for (String[] x : r) {
            LinearLayout row = NUi.row(c); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(0, NUi.dp(5), 0, 0);
            row.addView(NUi.text(c, x[0], 16, NTheme.text), NUi.lp(NUi.dp(28), -2));
            row.addView(NUi.body(c, x[1], 13.5f, NTheme.text, 600), NUi.lpw(0, -2, 1));
            TextView v = NUi.body(c, x[2].isEmpty() ? " " : x[2], 12, x[3].equals("1") ? NTheme.onAccent : NTheme.muted, 700); v.setGravity(Gravity.CENTER);
            v.setBackground(NUi.round(x[3].equals("1") ? NTheme.accent : NTheme.line, 99, 0)); v.setPadding(NUi.dp(10), NUi.dp(2), NUi.dp(10), NUi.dp(2));
            row.addView(v); w.addView(row);
        }
        return w;
    }

    void end(boolean done) {
        if (ended) return; ended = true;
        if (root != null && root.getParent() != null) ((android.view.ViewGroup) root.getParent()).removeView(root);
        sh.tour = null;
        sh.closeSheet(); if (!sh.stack.isEmpty()) sh.popAll();
        sh.st.sandboxEnd();
        sh.a.js("window.__ntourEnd&&window.__ntourEnd()");
        sh.tabTo(0);
        NShell.toast("Tour finished. Your own plan is back.");
    }
}
