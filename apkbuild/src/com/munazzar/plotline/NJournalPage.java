package com.munazzar.plotline;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/* Journal: moments (newest first, grouped by day) and threads. */
final class NJournalPage extends NPage {
    boolean threads;
    String thrFilter = "open", jf = "all", jq = ""; boolean jsearch;
    int shown = 40;
    NJournalPage(NShell sh) { super(sh); }

    String jlayout() { JSONObject l = st.settings().optJSONObject("layout"); return l != null && "v".equals(l.optString("journal")) ? "v" : "h"; }
    String jfilter() { String f = st.settings().optString("jFilter", ""); return f.equals("mine") || f.equals("goals") || f.equals("habits") || f.equals("steps") || f.equals("all") ? f : "high"; }
    static boolean pass(JSONObject e, String f) {
        String ty = e.optString("type", "note");
        if (f.equals("all")) return true;
        if (f.equals("high")) return !ty.equals("step") && !ty.equals("focus");
        if (f.equals("mine")) return ty.equals("note");
        if (f.equals("goals")) return ty.startsWith("goal");
        if (f.equals("habits")) return ty.equals("habit");
        return ty.equals("step") || ty.equals("focus");
    }

    void showThreads(boolean on) { threads = on; refresh(); }

    /* web vJournalW / vThreads: the header, then Journal · Threads with the cards/timeline switch */
    @Override void build() {
        final NForms F = new NForms(sh);
        if (threads) header("Journal", NUi.pbtn(c, "plus", "New thread", new View.OnClickListener() { public void onClick(View v) { F.thread(null); } }), gear());
        else header("Journal", NUi.ibtn(c, "ai", new View.OnClickListener() { public void onClick(View v) { NYear.open(sh, 0); } }), NUi.pbtn(c, "edit", "Write", new View.OnClickListener() { public void onClick(View v) { F.entry(null); } }), gear());
        LinearLayout seg = NUi.row(c); seg.setGravity(Gravity.CENTER_VERTICAL);
        seg.addView(NBits.seg(c, new String[][]{{"j", "Journal"}, {"t", "Threads"}}, threads ? "t" : "j", new NBits.Pick() { public void on(String k) { threads = k.equals("t"); refresh(); } }), NUi.lpw(0, -2, 1));
        LinearLayout.LayoutParams il = NUi.lp(-2, -2); il.leftMargin = NUi.dp(10);
        seg.addView(NBits.iconSeg(c, new String[][]{{"h", "horz"}, {"v", "vert"}}, jlayout(), new NBits.Pick() { public void on(String k) { try { JSONObject l = st.settings().optJSONObject("layout"); if (l == null) { l = new JSONObject(); st.settings().put("layout", l); } l.put("journal", k); } catch (Exception ignored) { } sh.save(); refresh(); } }), il);
        add(seg, 16);   /* web .jbar sits 16 under the header */
        if (threads) threadList(F); else moments(F);
    }

    /* ---- moments ---- */
    static List<JSONObject> entries(NStore st, String goalId) {
        List<JSONObject> l = new ArrayList<>();
        for (JSONObject e : NStore.list(st.arr("entries"))) if (goalId == null || goalId.equals(NStore.s(e, "goalId"))) l.add(e);
        Collections.sort(l, new Comparator<JSONObject>() { public int compare(JSONObject a, JSONObject b) { return Long.compare(b.optLong("t"), a.optLong("t")); } });
        return l;
    }

    static String entryTitle(NStore st, JSONObject e) {
        String type = e.optString("type", "note");
        if (!e.optString("title").isEmpty()) return e.optString("title");
        switch (type) {
            case "step": return "✓ " + e.optString("text");
            case "goal-done": return "🏆 Achieved: " + e.optString("text");
            case "goal": return "🎯 New goal: " + e.optString("text");
            case "habit": return "🔥 " + e.optString("text");
            default: { String x = e.optString("text"); int nl = x.indexOf('\n'); return nl > 0 ? x.substring(0, nl) : x; }
        }
    }

    static View entryRow(final NShell sh, final JSONObject e) {
        String type = e.optString("type", "note");
        boolean note = type.equals("note");
        String body = note && !e.optString("title").isEmpty() ? e.optString("text") : "";
        if (note && e.optString("title").isEmpty()) { String x = e.optString("text"); int nl = x.indexOf('\n'); body = nl > 0 ? x.substring(nl + 1) : ""; }
        JSONObject g = sh.st.find("goals", NStore.s(e, "goalId"));
        boolean priv = e.optBoolean("private");
        if (priv) body = "";
        String sub = NDates.fmtClock(e.optLong("t")) + (g != null ? " · " + g.optString("title") : "") + (body.isEmpty() ? "" : " · " + body.replace('\n', ' '));
        TextView mood = null;
        if (!NStore.s(e, "mood").isEmpty()) { mood = NUi.text(sh.a, NStore.s(e, "mood"), 22, NTheme.text); }
        View lead = NUi.text(sh.a, note ? "✍️" : type.equals("step") ? "✅" : type.equals("goal-done") ? "🏆" : type.equals("habit") ? "🔥" : "📌", 20, NTheme.text);
        lead.setLayoutParams(NUi.lp(NUi.dp(30), -2));
        LinearLayout r = NBits.row(sh.a, lead, priv ? "🔒 Private entry" : entryTitle(sh.st, e), sub, NTheme.muted, mood, false);
        if (note && !priv) NUi.tap(r, new View.OnClickListener() { public void onClick(View v) { new NForms(sh).entry(e); } });
        return r;
    }

    /* ---- 7 days at a time, newest first (web jwRange / jwBar / jwCards) ---- */
    int JW;
    void moments(final NForms F) {
        jdays.clear(); jhs = null; jasc = null;
        final String jf = jfilter();
        final int tn = NDates.today(), rb = tn - 7 * JW, ra = rb - 6;
        List<JSONObject> all = entries(st, null), l = new ArrayList<>();
        JSONObject before = null;
        for (JSONObject e : all) {
            if (!pass(e, jf)) continue;
            int k = NDates.dnum(NDates.ymd(e.optLong("t")));
            if (k >= ra && k <= rb) l.add(e); else if (k < ra && before == null) before = e;
        }
        weekBar(ra, rb, l);
        add(NBits.seg(c, new String[][]{{"high", "Highlights"}, {"mine", "My moments"}, {"goals", "Goals"}, {"habits", "Habits"}, {"steps", "Steps"}, {"all", "All"}}, jf, new NBits.Pick() { public void on(String k) { try { st.settings().put("jFilter", k); } catch (Exception ignored) { } sh.save(); refresh(); } }), 12);
        if (l.isEmpty()) {
            if (!all.isEmpty()) {
                View go = null;
                if (before != null) { final String bd = NDates.ymd(before.optLong("t")); go = NUi.btn(c, "Go to " + NDates.dayName(bd), false, new View.OnClickListener() { public void onClick(View v) { jumpTo(bd); } }); }
                View second = JW > 0 ? NUi.btn(c, "Back to this week", false, new View.OnClickListener() { public void onClick(View v) { week(-JW); } })
                    : NBits.ibtnText(c, "edit", "Write", true, new View.OnClickListener() { public void onClick(View v) { F.entry(null); } });
                add(NBits.empty(c, null, "Nothing in these 7 days" + (jf.equals("all") ? "" : " for this filter") + ".", go, second), 0);
            } else add(NBits.empty(c, "Your story starts here", "Goals you set and reach, notes and photos land on this line.", NBits.ibtnText(c, "camera", "Add a moment", true, new View.OnClickListener() { public void onClick(View v) { F.entry(null); } })), 0);
            return;
        }
        if (jlayout().equals("h")) {
            List<NHs.Item> items = new ArrayList<>();
            for (JSONObject e : l) items.add(NHs.item(NJCards.big(sh, e), NJCards.colOf(sh, e), true, true, NDates.dayLabel(e.optLong("t"))));
            View shell = NHs.build(sh, items, 0, "journal-w");
            jhs = (android.widget.HorizontalScrollView) shell.getTag(); jasc = l; jStep = jhs == null ? 1 : ((NHs.Snap) jhs).step;
            add(shell, 0); slideIn(shell); return;
        }
        List<NSpine.Row> rows = new ArrayList<>();
        for (JSONObject e : l) rows.add(NSpine.row(NDates.ymd(e.optLong("t")), NDates.dayLabel(e.optLong("t")), NJCards.small(sh, e), NJCards.colOf(sh, e)));
        View sp = NSpine.build(c, rows, jdays);
        add(sp, 0); slideIn(sp);
    }

    int slide;
    void slideIn(View v) {
        if (slide == 0 || NFx.reduced(sh)) { slide = 0; return; }
        v.setAlpha(0); v.setTranslationX(NUi.dp(slide > 0 ? -28 : 28)); slide = 0;
        v.animate().alpha(1).translationX(0).setDuration(380).setInterpolator(new android.view.animation.DecelerateInterpolator(2f)).start();
    }

    void week(int d) { int n = Math.max(0, JW + d); if (n == JW) return; slide = d; JW = n; refresh(); }

    /* ‹ seven day cells › and the date jump */
    void weekBar(final int ra, final int rb, List<JSONObject> shown) {
        final String td = NDates.ymd();
        LinearLayout bar = new LinearLayout(c) {
            float x0, y0; boolean tr;
            @Override public boolean onInterceptTouchEvent(android.view.MotionEvent e) { track(e); return false; }
            @Override public boolean onTouchEvent(android.view.MotionEvent e) { track(e); return true; }
            void track(android.view.MotionEvent e) {
                if (e.getActionMasked() == android.view.MotionEvent.ACTION_DOWN) { x0 = e.getX(); y0 = e.getY(); tr = true; }
                else if (e.getActionMasked() == android.view.MotionEvent.ACTION_UP && tr) { tr = false; float dx = e.getX() - x0, dy = e.getY() - y0; if (Math.abs(dx) > NUi.dp(55) && Math.abs(dx) > Math.abs(dy) * 1.4f) week(dx < 0 ? -1 : 1); }
                else if (e.getActionMasked() == android.view.MotionEvent.ACTION_CANCEL) tr = false;
            }
        };
        bar.setOrientation(LinearLayout.HORIZONTAL); bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackground(NUi.round(NTheme.surface, 22, NTheme.line)); bar.setPadding(NUi.dp(6), NUi.dp(6), NUi.dp(6), NUi.dp(6));
        bar.addView(wbtn("back", true, new View.OnClickListener() { public void onClick(View v) { week(1); } }));
        LinearLayout days = NUi.row(c);
        for (int k = ra; k <= rb; k++) {
            final String ds = NDates.fromN(k); int n = 0; JSONObject md = null;
            for (JSONObject e : shown) if (NDates.ymd(e.optLong("t")).equals(ds)) n++;
            for (JSONObject e : NStore.list(st.arr("entries"))) if (NDates.ymd(e.optLong("t")).equals(ds) && !NStore.s(e, "mood").isEmpty() && (md == null || e.optLong("t") > md.optLong("t"))) md = e;
            LinearLayout d = NUi.col(c); d.setGravity(Gravity.CENTER_HORIZONTAL); d.setPadding(0, NUi.dp(4), 0, NUi.dp(4));
            boolean today = ds.equals(td);
            if (n > 0 || today) d.setBackground(NUi.round(n > 0 ? NUi.mix(NTheme.accent, .10f, NTheme.surface) : NTheme.surface, 12, today ? NUi.mix(NTheme.accent, .6f, NTheme.surface) : 0));
            TextView wd = NBits.meta(c, NDates.DAYS[NDates.dow(k)].substring(0, 1), NTheme.muted); wd.setTextSize(9.5f); wd.setGravity(Gravity.CENTER); d.addView(wd);
            TextView num = NUi.body(c, String.valueOf(Integer.parseInt(ds.substring(8))), 14, n > 0 ? NTheme.text : NTheme.muted, n > 0 ? 700 : 600); num.setGravity(Gravity.CENTER); d.addView(num);
            FrameLayout ic = new FrameLayout(c);
            if (md != null) { TextView m = NUi.text(c, md.optString("mood"), 13, NTheme.text); m.setGravity(Gravity.CENTER); ic.addView(m, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER)); }
            else if (n > 0) { View dot = new View(c); dot.setBackground(NUi.round(NTheme.accent, 99, 0)); ic.addView(dot, new FrameLayout.LayoutParams(NUi.dp(5), NUi.dp(5), Gravity.CENTER)); }
            d.addView(ic, new LinearLayout.LayoutParams(-1, NUi.dp(16)));
            d.setContentDescription(NDates.dayName(ds));
            if (n > 0) NUi.tap(d, new View.OnClickListener() { public void onClick(View v) { scrollToDay(ds); } });
            LinearLayout.LayoutParams dl = NUi.lpw(0, -2, 1); if (k > ra) dl.leftMargin = NUi.dp(2);
            days.addView(d, dl);
        }
        LinearLayout.LayoutParams dsl = NUi.lpw(0, -2, 1); dsl.leftMargin = NUi.dp(6); dsl.rightMargin = NUi.dp(6);
        bar.addView(days, dsl);
        View nx = wbtn("next", JW > 0, new View.OnClickListener() { public void onClick(View v) { week(-1); } });
        bar.addView(nx);
        LinearLayout.LayoutParams cl = NUi.lp(-2, -2); cl.leftMargin = NUi.dp(6);
        bar.addView(wbtn("cal", true, new View.OnClickListener() { public void onClick(View v) { jumpDate(); } }), cl);
        add(bar, 0);
        if (JW > 0) {
            LinearLayout cap = NUi.row(c); cap.setGravity(Gravity.CENTER);
            String a0 = NDates.fromN(ra), b0 = NDates.fromN(rb);
            TextView t = NBits.meta(c, (NDates.MONTHS[NDates.month(a0)].substring(0, 3) + " " + NDates.day(a0) + " – " + NDates.MONTHS[NDates.month(b0)].substring(0, 3) + " " + NDates.day(b0) + " · ").toUpperCase(), NTheme.muted); t.setTextSize(11);
            cap.addView(t);
            TextView bk = NUi.link(c, "Back to this week", new View.OnClickListener() { public void onClick(View v) { week(-JW); } }); bk.setTextSize(13); bk.setPadding(0, NUi.dp(4), 0, NUi.dp(4));
            cap.addView(bk);
            add(cap, 4);
        }
    }

    View wbtn(String icon, boolean on, View.OnClickListener l) {
        FrameLayout f = new NUi.Fix(c, NUi.dp(34), NUi.dp(34));
        f.setBackground(NUi.ripple(NUi.round(NTheme.surface, 12, NTheme.line), 12));
        android.widget.ImageView i = new android.widget.ImageView(c); i.setImageDrawable(new NIcon(icon, NTheme.text));
        f.addView(i, new FrameLayout.LayoutParams(NUi.dp(17), NUi.dp(17), Gravity.CENTER));
        f.setContentDescription(icon);
        if (on) NUi.tap(f, l); else f.setAlpha(.3f);
        return f;
    }

    /* web jwScrollTo: bring that day's first card into view */
    void scrollToDay(String ds) {
        View h = jdays.get(ds);
        if (h != null) { int y = 0; View v = h; while (v != null && v != sv) { y += v.getTop(); v = v.getParent() instanceof View ? (View) v.getParent() : null; } sv.smoothScrollTo(0, Math.max(0, y - NUi.dp(8))); return; }
        if (jhs != null && jasc != null) for (int i = 0; i < jasc.size(); i++) if (NDates.ymd(jasc.get(i).optLong("t")).equals(ds)) { ((NHs.Snap) jhs).goTo(i, !NFx.reduced(sh)); return; }
    }

    static final String[][] KIND = {{"habit", "Habit milestone"}, {"step", "Step done"}, {"goal-done", "Goal achieved"}, {"goal-new", "Goal set"}, {"goal", "Goal set"}, {"note", "Moment"}, {"focus", "Focus session"}};
    static String kind(String t) { for (String[] k : KIND) if (k[0].equals(t)) return k[1]; return "Moment"; }



    JSONObject hbOf(JSONObject e) {
        String hid = NStore.s(e, "hid"); JSONObject h = hid.isEmpty() ? null : st.find("habits", hid); if (h != null) return h;
        if ("habit".equals(e.optString("type"))) for (JSONObject x : NStore.list(st.arr("habits"))) if (e.optString("text").endsWith("· " + x.optString("title"))) return x;
        return null;
    }

    /* ---- mood strip: the last 14 days ---- */
    final java.util.Map<String, View> jdays = new java.util.HashMap<>();
    android.widget.HorizontalScrollView jhs; List<JSONObject> jasc; int jStep;

    void jumpDate() {
        String t0 = NDates.ymd(); final int[] d = {Integer.parseInt(t0.substring(0, 4)), Integer.parseInt(t0.substring(5, 7)), Integer.parseInt(t0.substring(8, 10))};
        android.app.DatePickerDialog dlg = new android.app.DatePickerDialog(sh.a, new android.app.DatePickerDialog.OnDateSetListener() { public void onDateSet(android.widget.DatePicker p, int y, int m, int dd) {
            jumpTo(String.format(java.util.Locale.US, "%04d-%02d-%02d", y, m + 1, dd));
        } }, d[0], d[1] - 1, d[2]);
        dlg.getDatePicker().setMaxDate(System.currentTimeMillis());
        dlg.show();
    }

    /* web jwTo: open the week holding that date, then bring the day into view */
    void jumpTo(final String date) {
        threads = false;
        JW = Math.max(0, (int) Math.floor((NDates.today() - NDates.dnum(date)) / 7.0));
        refresh();
        sv.postDelayed(new Runnable() { public void run() { scrollToDay(date); } }, 120);
    }


    /* ---- threads ---- */
    static long last(JSONObject t) {
        long m = t.optLong("created", t.optLong("u"));
        JSONArray u = t.optJSONArray("ups");
        if (u != null) for (int i = 0; i < u.length(); i++) { JSONObject x = u.optJSONObject(i); if (x != null && !NStore.isDel(x)) m = Math.max(m, x.optLong("t")); }
        return m;
    }

    static JSONObject lastUp(JSONObject t) {
        JSONArray u = t.optJSONArray("ups"); JSONObject b = null;
        if (u != null) for (int i = 0; i < u.length(); i++) { JSONObject x = u.optJSONObject(i); if (x != null && !NStore.isDel(x) && (b == null || x.optLong("t") >= b.optLong("t"))) b = x; }
        return b;
    }

    static List<JSONObject> sortedThreads(NStore st, boolean openOnly) {
        List<JSONObject> l = new ArrayList<>();
        for (JSONObject t : NStore.list(st.arr("threads"))) if (!openOnly || !"done".equals(t.optString("status"))) l.add(t);
        Collections.sort(l, new Comparator<JSONObject>() { public int compare(JSONObject a, JSONObject b) { return Long.compare(last(b), last(a)); } });
        return l;
    }

    static View threadRow(final NShell sh, final JSONObject t) {
        JSONObject u = lastUp(t);
        TextView em = NUi.text(sh.a, u != null ? NForms.tagEmoji(u.optString("k")) : "🧵", 20, NTheme.text); em.setGravity(Gravity.CENTER);
        em.setBackground(NUi.round(NTheme.surface2, 12, 0)); em.setLayoutParams(NUi.lp(NUi.dp(40), NUi.dp(40)));
        TextView when = NBits.meta(sh.a, NDates.ago(last(t)).toUpperCase(), NTheme.muted);
        LinearLayout r = NBits.row(sh.a, em, t.optString("title"), u != null ? u.optString("x").replace('\n', ' ') : "Nothing logged yet", NTheme.muted, when, false);
        NUi.tap(r, new View.OnClickListener() { public void onClick(View v) { sh.push(new NThreadScreen(sh, t.optString("id"))); } });
        return r;
    }

    void threadList(final NForms F) {
        List<JSONObject> all = sortedThreads(st, false), l = new ArrayList<>(); int open = 0;
        for (JSONObject t : all) { boolean d = "done".equals(t.optString("status")); if (!d) open++; if (thrFilter.equals("all") || (thrFilter.equals("done") == d)) l.add(t); }
        if (!all.isEmpty()) add(NBits.seg(c, new String[][]{{"open", "Active · " + open}, {"done", "Done"}, {"all", "All"}}, thrFilter, new NBits.Pick() { public void on(String k) { thrFilter = k; refresh(); } }), 12);
        if (l.isEmpty()) {
            if (!all.isEmpty()) add(NBits.empty(c, null, "Nothing here.", NUi.btn(c, "Show all threads", false, new View.OnClickListener() { public void onClick(View v) { thrFilter = "all"; refresh(); } })), 0);
            else add(NBits.empty(c, "Think out loud, one step at a time", "A thread is a running log for an idea, a project or anything you’re working through. Add a line whenever something changes. Link it to a goal or habit only if you want to.",
                NBits.ibtnText(c, "plus", "Start a thread", true, new View.OnClickListener() { public void onClick(View v) { F.thread(null); } })), 0);
            return;
        }
        if (jlayout().equals("h")) {
            List<NHs.Item> items = new ArrayList<>();
            for (JSONObject t : l) items.add(NHs.item(NJCards.thrBig(sh, t), NJCards.thrCol(sh, t), true, true, NDates.dayLabel(last(t))));
            add(NHs.build(sh, items, 0, "threads-" + thrFilter), 0); return;
        }
        List<NSpine.Row> rows = new ArrayList<>();
        for (JSONObject t : l) rows.add(NSpine.row(NDates.ymd(last(t)), NDates.dayLabel(last(t)), NJCards.thrFlip(sh, t), NJCards.thrCol(sh, t)));
        add(NSpine.build(c, rows, null), 0);
    }
}
