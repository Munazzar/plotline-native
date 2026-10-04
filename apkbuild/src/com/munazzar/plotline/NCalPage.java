package com.munazzar.plotline;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;

/* Calendar, same as the web: Day / Week / Month, titled chips in the month, a peek at the picked day,
   a week list, a day timeline, and the agenda under the card. */
final class NCalPage extends NPage {
    static final int K_DAY = 0, K_STEP = 1, K_REP = 2, K_TARGET = 3;
    static final int PINK = 0xFFFF7A7A;
    String sel = NDates.ymd();
    NForms F;

    NCalPage(NShell sh) { super(sh); }

    /* ---------- items ---------- */
    static final class It { int k; JSONObject g, o; String t, time, sub, id; boolean done; }

    String calView() { JSONObject l = st.settings().optJSONObject("layout"); String v = l == null ? "month" : l.optString("cal", "month"); return v.equals("day") || v.equals("week") ? v : "month"; }
    void setView(String v) { try { JSONObject l = st.settings().optJSONObject("layout"); if (l == null) { l = new JSONObject(); st.settings().put("layout", l); } l.put("cal", v); } catch (Exception ignored) { } sh.save(); refresh(); }
    float ppm() { JSONObject l = st.settings().optJSONObject("layout"); double p = l == null ? 1 : l.optDouble("calPpm", 1); if (Double.isNaN(p) || p <= 0) p = 1; return (float) Math.min(3.2, Math.max(.5, p)); }
    void setPpm(float p) { try { JSONObject l = st.settings().optJSONObject("layout"); if (l == null) { l = new JSONObject(); st.settings().put("layout", l); } l.put("calPpm", Math.round(Math.min(3.2f, Math.max(.5f, p)) * 1000) / 1000.0); } catch (Exception ignored) { } sh.save(); refresh(); }

    JSONObject goal(String id) { if (id == null || id.isEmpty()) return null; for (JSONObject g : NStore.list(st.arr("goals"))) if (id.equals(g.optString("id"))) return g; return null; }
    static int colOf(JSONObject g) { return g == null ? NTheme.accent : NTheme.areaCol(g.optString("area")); }

    Map<Integer, List<It>> items(int a, int b) {
        Map<Integer, List<It>> M = new TreeMap<>();
        for (JSONObject x : NStore.list(st.arr("days"))) {
            String d = x.optString("date"); if (!NDates.valid(d)) continue; int n = NDates.dnum(d); if (n < a || n > b) continue;
            It i = new It(); i.k = K_DAY; i.o = x; i.g = goal(NStore.s(x, "goalId")); i.t = x.optString("title"); i.time = x.optString("time"); i.done = x.optBoolean("done"); i.sub = i.g == null ? "" : i.g.optString("title"); i.id = x.optString("id");
            put(M, n, i);
        }
        for (JSONObject g : NStore.list(st.arr("goals"))) {
            JSONArray ss = g.optJSONArray("steps");
            if (ss != null) for (int q = 0; q < ss.length(); q++) {
                JSONObject s = ss.optJSONObject(q); if (s == null) continue; String d = s.optString("due"); if (!NDates.valid(d)) continue;
                int n0 = NDates.dnum(d);
                if (n0 >= a && n0 <= b) { It i = new It(); i.k = K_STEP; i.g = g; i.o = s; i.t = s.optString("title"); i.time = s.optString("time"); i.done = s.optBoolean("done"); i.sub = g.optString("title"); i.id = s.optString("id"); put(M, n0, i); }
                String rp = s.optString("repeat");
                if (!rp.isEmpty() && !s.optBoolean("done") && "active".equals(g.optString("status", "active")) && !hasFollow(g, s.optString("id"))) {
                    Calendar k = Calendar.getInstance(TimeZone.getTimeZone("UTC")); k.setTimeInMillis(n0 * 86400000L);
                    for (int z = 0; z < 1500; z++) {
                        if (rp.equals("daily")) k.add(Calendar.DAY_OF_MONTH, 1); else if (rp.equals("weekly")) k.add(Calendar.DAY_OF_MONTH, 7); else k.add(Calendar.MONTH, 1);
                        int n = (int) Math.round(k.getTimeInMillis() / 86400000.0); if (n > b) break;
                        if (n < a) continue;
                        It i = new It(); i.k = K_REP; i.g = g; i.o = s; i.t = s.optString("title"); i.time = s.optString("time"); i.done = false; i.sub = g.optString("title"); i.id = s.optString("id"); put(M, n, i);
                    }
                }
            }
            String td = g.optString("targetDate");
            if (NDates.valid(td)) { int n = NDates.dnum(td); if (n >= a && n <= b) { It i = new It(); i.k = K_TARGET; i.g = g; i.t = g.optString("title"); i.time = ""; i.done = "done".equals(g.optString("status")); i.sub = i.done ? "Achieved" : NActs.pct(st, g) + "% there"; i.id = g.optString("id"); put(M, n, i); } }
        }
        for (List<It> l : M.values()) Collections.sort(l, new Comparator<It>() { public int compare(It x, It y) {
            int t = (x.k == K_TARGET ? 1 : 0) - (y.k == K_TARGET ? 1 : 0); if (t != 0) return t;
            String a1 = x.time.isEmpty() ? "99" : x.time, b1 = y.time.isEmpty() ? "99" : y.time; return a1.compareTo(b1);
        } });
        return M;
    }
    static boolean hasFollow(JSONObject g, String id) { JSONArray ss = g.optJSONArray("steps"); if (ss == null) return false; for (int i = 0; i < ss.length(); i++) { JSONObject y = ss.optJSONObject(i); if (y != null && id.equals(y.optString("from")) && !y.optBoolean("done")) return true; } return false; }
    static void put(Map<Integer, List<It>> M, int n, It i) { List<It> l = M.get(n); if (l == null) { l = new ArrayList<>(); M.put(n, l); } l.add(i); }

    /* ---------- dates ---------- */
    static String fmt(int n, String pat) { SimpleDateFormat f = new SimpleDateFormat(pat, Locale.getDefault()); f.setTimeZone(TimeZone.getTimeZone("UTC")); return f.format(new Date(n * 86400000L)); }
    static int[] monthOf(int n) { Calendar k = Calendar.getInstance(TimeZone.getTimeZone("UTC")); k.setTimeInMillis(n * 86400000L); k.set(Calendar.DAY_OF_MONTH, 1); int f = (int) Math.round(k.getTimeInMillis() / 86400000.0); k.add(Calendar.MONTH, 1); return new int[]{f, (int) Math.round(k.getTimeInMillis() / 86400000.0) - 1}; }
    int[] span(String v) { int n = NDates.dnum(sel); if (v.equals("day")) return new int[]{n, n}; if (v.equals("week")) { int a = NDates.wkStart(n); return new int[]{a, a + 6}; } return monthOf(n); }

    void step(int k) {
        String v = calView(); int n = NDates.dnum(sel);
        if (v.equals("day")) sel = NDates.fromN(n + k);
        else if (v.equals("week")) sel = NDates.fromN(n + 7 * k);
        else {
            Calendar c1 = Calendar.getInstance(TimeZone.getTimeZone("UTC")); c1.setTimeInMillis(n * 86400000L); c1.set(Calendar.DAY_OF_MONTH, 1); c1.add(Calendar.MONTH, k);
            int f = (int) Math.round(c1.getTimeInMillis() / 86400000.0), l = monthOf(f)[1], tn = NDates.today();
            sel = NDates.fromN(tn >= f && tn <= l ? tn : f);
        }
        refresh();
    }

    /* ---------- page ---------- */
    @Override void build() {
        F = new NForms(sh);
        final String v = calView();
        int[] sp = span(v); Map<Integer, List<It>> M = items(sp[0], sp[1]);
        int cnt = 0, dn = 0; for (List<It> l : M.values()) for (It i : l) if (i.k == K_DAY || i.k == K_STEP) { cnt++; if (i.done) dn++; }
        header("Calendar", NUi.ibtn(c, "plus", new View.OnClickListener() { public void onClick(View x) { F.dayGoal(sel); } }), gear());
        add(NUi.label(c, "Day goals, steps and targets together" + (cnt > 0 ? " · " + dn + " of " + cnt + " done" : ""), NTheme.muted), 14);

        LinearLayout bar = NUi.row(c);
        View seg = NBits.seg(c, new String[][]{{"day", "Day"}, {"week", "Week"}, {"month", "Month"}}, v, new NBits.Pick() { public void on(String k) { setView(k); } });
        bar.addView(seg, NUi.lpw(0, -2, 1));
        LinearLayout today = NUi.row(c); today.setGravity(Gravity.CENTER); today.setPadding(NUi.dp(14), 0, NUi.dp(16), 0);
        today.setBackground(NUi.ripple(NUi.round(NTheme.surface2, 14, NTheme.line2), 14));
        today.addView(NUi.icon(c, "cal", 17, NTheme.text));
        TextView tt = NUi.body(c, "Today", 15, NTheme.text, 600); tt.setPadding(NUi.dp(8), 0, 0, 0); today.addView(tt);
        NUi.tap(today, new View.OnClickListener() { public void onClick(View x) { sel = NDates.ymd(); refresh(); } });
        LinearLayout.LayoutParams tl = NUi.lp(-2, NUi.dp(46)); tl.leftMargin = NUi.dp(10); bar.addView(today, tl);
        add(bar, 14);

        LinearLayout card = new LinearLayout(c) {
            float dx0, dy0; boolean sw; final int slop = android.view.ViewConfiguration.get(c).getScaledTouchSlop();
            @Override public boolean onInterceptTouchEvent(MotionEvent e) {
                if (v.equals("day")) return false;
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN: dx0 = e.getX(); dy0 = e.getY(); sw = false; break;
                    case MotionEvent.ACTION_MOVE: if (Math.abs(e.getX() - dx0) > slop * 2 && Math.abs(e.getX() - dx0) > 2 * Math.abs(e.getY() - dy0)) { sw = true; getParent().requestDisallowInterceptTouchEvent(true); return true; } break;
                }
                return false;
            }
            @Override public boolean onTouchEvent(MotionEvent e) {
                if (e.getActionMasked() == MotionEvent.ACTION_UP && sw) { float d = e.getX() - dx0; sw = false; if (Math.abs(d) > NUi.dp(60)) step(d < 0 ? 1 : -1); return true; }
                return sw || super.onTouchEvent(e);
            }
        };
        card.setOrientation(LinearLayout.VERTICAL); card.setBackground(NUi.card(30)); card.setPadding(NUi.dp(14), NUi.dp(14), NUi.dp(14), NUi.dp(14));
        String title;
        int n = NDates.dnum(sel);
        if (v.equals("week")) { int a = NDates.wkStart(n); title = fmt(a, "MMM d") + " – " + fmt(a + 6, "MMM d"); } else title = fmt(n, "MMMM yyyy");
        LinearLayout ct = NUi.row(c); ct.setPadding(NUi.dp(4), NUi.dp(2), NUi.dp(4), NUi.dp(16));
        TextView h2 = NUi.title(c, title.toUpperCase(Locale.getDefault()), 28); h2.setSingleLine(true); h2.setEllipsize(android.text.TextUtils.TruncateAt.END);
        ct.addView(h2, NUi.lpw(0, -2, 1));
        ct.addView(NBits.sbtn(c, "back", new View.OnClickListener() { public void onClick(View x) { step(-1); } }));
        LinearLayout.LayoutParams nl = NUi.lp(-2, -2); nl.leftMargin = NUi.dp(8);
        ct.addView(NBits.sbtn(c, "next", new View.OnClickListener() { public void onClick(View x) { step(1); } }), nl);
        card.addView(ct);
        if (v.equals("month")) month(card, n); else if (v.equals("week")) week(card, n, M); else day(card, n);
        add(card, 0);

        /* agenda */
        LinearLayout hd = NUi.sectionHead(c, v.equals("day") ? "Plan for the day" : v.equals("week") ? "This week" : "This month", "Add for " + NDates.dayName(sel), new View.OnClickListener() { public void onClick(View x) { F.dayGoal(sel); } });
        hd.setPadding(0, NUi.dp(28), 0, NUi.dp(14));
        add(hd);
        agenda(M, v);
    }

    /* ---------- month ---------- */
    void month(LinearLayout card, int n) {
        int[] mo = monthOf(n); int f = mo[0], l = mo[1];
        int a = f - NDates.dow(f), b = l + 6 - NDates.dow(l), tn = NDates.today();
        Map<Integer, List<It>> M = items(a, b);
        Map<Integer, String> mood = new TreeMap<>(); Map<Integer, Long> moodT = new TreeMap<>();
        for (JSONObject e : NStore.list(st.arr("entries"))) {
            if (!"note".equals(e.optString("type")) || e.optString("mood").isEmpty() || e.optBoolean("private")) continue;
            int k = NDates.dnum(NDates.ymd(e.optLong("t"))); if (k < a || k > b) continue;
            Long pt = moodT.get(k); if (pt == null || pt < e.optLong("t")) { mood.put(k, e.optString("mood")); moodT.put(k, e.optLong("t")); }
        }
        LinearLayout dows = NUi.row(c);
        for (int i = 0; i < 7; i++) { TextView d = NUi.text(c, NDates.DAYS[i].toUpperCase(), 10, NTheme.muted); d.setTypeface(NFont.mono(500)); d.setGravity(Gravity.CENTER); d.setLetterSpacing(.08f); dows.addView(d, NUi.lpw(0, -2, 1)); }
        card.addView(dows);
        int wDp = a(); float cellH = Math.max(58, Math.min(76, Math.round((wDp - 40) / 7f * 1.25f))) - 4;
        LinearLayout row = null;
        for (int k = a; k <= b; k++) {
            if ((k - a) % 7 == 0) { row = NUi.row(c); row.setBaselineAligned(false); card.addView(row, NUi.mt(4)); }
            final String ds = NDates.fromN(k);
            List<It> it = M.get(k); if (it == null) it = new ArrayList<>();
            List<It> pl = new ArrayList<>(), tg = new ArrayList<>(); boolean late = false;
            for (It i : it) { if (i.k == K_TARGET) tg.add(i); else { pl.add(i); if (i.k != K_REP && !i.done && k < tn) late = true; } }
            FrameLayout cell = new FrameLayout(c);
            GradientDrawableHolder.set(cell, k == n ? NTheme.text : 0, NTheme.bg2, 12);
            cell.setAlpha(k < f || k > l ? .45f : 1f);
            LinearLayout col = NUi.col(c); col.setPadding(NUi.dp(3), NUi.dp(4), NUi.dp(3), NUi.dp(8));
            LinearLayout top = NUi.row(c);
            TextView num = NUi.text(c, String.valueOf(NDates.day(ds)), 11, k == tn ? NTheme.onAccent : NTheme.text); num.setTypeface(NFont.mono(600)); num.setGravity(Gravity.CENTER);
            if (k == tn) num.setBackground(NUi.oval(NTheme.accent, 0, 0)); else if (late) num.setBackground(NUi.oval(0, PINK, 1.5f));
            top.addView(num, new LinearLayout.LayoutParams(NUi.dp(20), NUi.dp(20)));
            View sp = new View(c); top.addView(sp, NUi.lpw(0, 1, 1));
            if (mood.containsKey(k)) top.addView(NUi.text(c, mood.get(k), 10, NTheme.text));
            if (!tg.isEmpty()) { TextView fl = NUi.text(c, "⚑", 9.5f, colOf(tg.get(0).g)); fl.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); top.addView(fl); }
            col.addView(top);
            for (int j = 0; j < Math.min(2, pl.size()); j++) {
                It x = pl.get(j); int cc = colOf(x.g);
                LinearLayout chip = NUi.row(c); chip.setBackground(NUi.round(NUi.mix(cc, .17f, NTheme.bg2), 4, 0));
                View bar = new View(c); bar.setBackgroundColor(cc); chip.addView(bar, new LinearLayout.LayoutParams(NUi.dp(1.5f), -1));
                TextView tx = NUi.ell(NUi.body(c, x.t, 8.5f, NTheme.text, 600), 1); tx.setSingleLine(true); tx.setPadding(NUi.dp(3), NUi.dp(1.5f), NUi.dp(3), NUi.dp(1.5f));
                if (x.done) tx.setPaintFlags(tx.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                chip.addView(tx, new LinearLayout.LayoutParams(0, -2, 1));
                if (x.done) chip.setAlpha(.5f);
                LinearLayout.LayoutParams cl = NUi.lp(-1, -2); cl.topMargin = NUi.dp(2); col.addView(chip, cl);
            }
            if (pl.size() > 2) { TextView m = NUi.body(c, "+" + (pl.size() - 2) + " more", 8.5f, NTheme.muted, 500); m.setPadding(NUi.dp(3), NUi.dp(2), 0, 0); m.setSingleLine(true); col.addView(m); }
            cell.addView(col, new FrameLayout.LayoutParams(-1, -1));
            int[] r = k <= tn ? NHabits.dayRatio(st.arr("habits"), ds) : new int[]{0, 0};
            if (r[0] > 0) {
                FrameLayout hb = new FrameLayout(c); hb.setBackground(NUi.round(NTheme.alpha(NTheme.text, .10f), 2, 0));
                View fill = new View(c); fill.setBackground(NUi.round(r[1] >= r[0] ? NTheme.accent : NTheme.alpha(NTheme.accent, .75f), 2, 0));
                hb.addView(fill, new FrameLayout.LayoutParams(0, -1));
                final View ff = fill; final float frac = Math.min(1f, r[1] / (float) r[0]);
                hb.addOnLayoutChangeListener(new View.OnLayoutChangeListener() { public void onLayoutChange(View v, int l1, int t1, int r1, int b1, int ol, int ot, int or, int ob) {
                    int w = Math.round((r1 - l1) * frac); ViewGroup.LayoutParams p = ff.getLayoutParams(); if (p.width != w) { p.width = w; ff.setLayoutParams(p); } } });
                FrameLayout.LayoutParams hl = new FrameLayout.LayoutParams(-1, Math.max(2, NUi.dp(2.5f)), Gravity.BOTTOM); hl.leftMargin = hl.rightMargin = NUi.dp(4); hl.bottomMargin = NUi.dp(3);
                cell.addView(hb, hl);
            }
            NUi.tap(cell, new View.OnClickListener() { public void onClick(View x) { if (ds.equals(sel)) { setView("day"); } else { sel = ds; refresh(); } } });
            LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(0, NUi.dp(cellH), 1); cl.leftMargin = cl.rightMargin = NUi.dp(2);
            row.addView(cell, cl);
        }
        peek(card);
    }
    int a() { return sh.a.getResources().getConfiguration().screenWidthDp; }

    /* the picked day under the month */
    void peek(LinearLayout card) {
        final String ds = sel; int k = NDates.dnum(ds), tn = NDates.today();
        List<It> it = items(k, k).get(k); if (it == null) it = new ArrayList<>();
        List<JSONObject> hs = new ArrayList<>();
        for (JSONObject h : NStore.list(st.arr("habits"))) if (!NHabits.kind(h).equals("quit") && !"archived".equals(NHabits.status(h)) && NHabits.due(h, ds)) hs.add(h);
        int[] r = NHabits.dayRatio(st.arr("habits"), ds);
        List<JSONObject> mo = new ArrayList<>();
        for (JSONObject e : NStore.list(st.arr("entries"))) if ("note".equals(e.optString("type")) && NDates.ymd(e.optLong("t")).equals(ds)) mo.add(e);
        LinearLayout p = NUi.col(c); p.setBackground(NUi.round(NTheme.bg2, 20, 0)); p.setPadding(NUi.dp(12), NUi.dp(12), NUi.dp(12), NUi.dp(12));
        LinearLayout h = NUi.row(c);
        LinearLayout tx = NUi.col(c);
        tx.addView(NUi.body(c, fmt(k, "EEEE, MMM d"), 15, NTheme.text, 700));
        TextView sub = NBits.meta(c, (k == tn ? "Today · " : "") + (it.size() > 0 ? it.size() + " planned" : "Nothing planned") + (r[0] > 0 && k <= tn ? " · habits " + r[1] + " of " + r[0] : "") + (mo.size() > 0 ? " · " + mo.size() + " journal entr" + (mo.size() == 1 ? "y" : "ies") : ""), NTheme.muted);
        sub.setTextSize(10.5f); sub.setAllCaps(true); sub.setPadding(0, NUi.dp(2), 0, 0); tx.addView(sub);
        h.addView(tx, NUi.lpw(0, -2, 1));
        h.addView(NBits.sbtn(c, "plus", new View.OnClickListener() { public void onClick(View x) { F.dayGoal(ds); } }));
        LinearLayout.LayoutParams nl = NUi.lp(-2, -2); nl.leftMargin = NUi.dp(6);
        h.addView(NBits.sbtn(c, "next", new View.OnClickListener() { public void onClick(View x) { setView("day"); } }), nl);
        p.addView(h);
        if (!it.isEmpty()) { LinearLayout l = NUi.col(c); for (It i : it) { LinearLayout.LayoutParams rl = NUi.mt(6); l.addView(agr(i, k < tn, NTheme.surface), rl); } p.addView(l, NUi.mt(4)); }
        if (!hs.isEmpty()) {
            NFlow fl = new NFlow(c, 6, 6);
            for (final JSONObject hb : hs) {
                boolean d = NHabits.done(hb, ds), fut = k > tn; int cc = NTheme.areaCol(hb.optString("area"));
                LinearLayout pill = NUi.row(c); pill.setPadding(NUi.dp(11), NUi.dp(7), NUi.dp(11), NUi.dp(7));
                pill.setBackground(NUi.round(d ? NUi.mix(cc, .22f, NTheme.surface) : NTheme.surface, 99, d ? NTheme.alpha(cc, .6f) : NTheme.line));
                pill.addView(NUi.text(c, NHabits.icon(hb), 12.5f, NTheme.text));
                TextView nm = NUi.body(c, hb.optString("title"), 12.5f, NTheme.text, 600); nm.setPadding(NUi.dp(6), 0, 0, 0); nm.setSingleLine(true); nm.setEllipsize(android.text.TextUtils.TruncateAt.END); pill.addView(nm);
                if (d) { View ck = NUi.icon(c, "check", 13, NTheme.text); ck.setPadding(NUi.dp(6), 0, 0, 0); pill.addView(ck); }
                if (fut) pill.setAlpha(.5f); else NUi.tap(pill, new View.OnClickListener() { public void onClick(View x) { NActs.habitDay(hb, ds); NActs.milestone(st, hb); sh.save(); } });
                fl.addView(pill, new ViewGroup.MarginLayoutParams(-2, -2));
            }
            p.addView(fl, NUi.mt(10));
        }
        if (!mo.isEmpty()) {
            for (int i = Math.max(0, mo.size() - 2); i < mo.size(); i++) {
                final JSONObject e = mo.get(i);
                LinearLayout j = NUi.row(c); j.setPadding(NUi.dp(12), NUi.dp(9), NUi.dp(12), NUi.dp(9)); j.setBackground(NUi.round(NTheme.surface, 14, 0));
                if (!e.optString("mood").isEmpty() && !e.optBoolean("private")) { TextView m = NUi.text(c, e.optString("mood"), 14, NTheme.text); m.setPadding(0, 0, NUi.dp(8), 0); j.addView(m); }
                String t = e.optBoolean("private") ? "Private entry" : (e.optString("title").isEmpty() ? e.optString("text") : e.optString("title"));
                j.addView(NUi.ell(NUi.body(c, t.length() > 90 ? t.substring(0, 89) + "…" : t, 13.5f, NTheme.text, 400), 1), NUi.lpw(0, -2, 1));
                NUi.tap(j, new View.OnClickListener() { public void onClick(View x) { NEng.viewEntry(sh, e.optString("id")); } });
                p.addView(j, NUi.mt(6));
            }
        }
        if (it.isEmpty() && hs.isEmpty() && mo.isEmpty()) { TextView e = NUi.text(c, "A free day. Tap + to plan something.", 13, NTheme.muted); e.setPadding(NUi.dp(2), NUi.dp(6), 0, NUi.dp(2)); p.addView(e); }
        card.addView(p, NUi.mt(12));
    }

    /* ---------- week ---------- */
    void week(LinearLayout card, int n, Map<Integer, List<It>> M) {
        int a = NDates.wkStart(n), tn = NDates.today();
        for (int k = a; k <= a + 6; k++) {
            final String ds = NDates.fromN(k);
            LinearLayout w = NUi.row(c); w.setGravity(Gravity.TOP); w.setPadding(NUi.dp(10), NUi.dp(8), NUi.dp(10), NUi.dp(8));
            w.setBackground(NUi.round(NTheme.bg2, 18, 0));
            if (k == n) GradientDrawableHolder.set(w, NTheme.text, NTheme.bg2, 18);
            LinearLayout hd = NUi.col(c); hd.setGravity(Gravity.CENTER_HORIZONTAL); hd.setPadding(0, NUi.dp(4), 0, NUi.dp(4));
            TextView dw = NUi.text(c, NDates.DAYS[NDates.dow(k)].toUpperCase(), 10, NTheme.muted); dw.setTypeface(NFont.mono(500)); dw.setLetterSpacing(.08f); hd.addView(dw);
            TextView dn = NUi.title(c, String.valueOf(NDates.day(ds)), 26); dn.setTypeface(NFont.display(700)); dn.setTextColor(k == tn ? NTheme.accent : NTheme.text); dn.setGravity(Gravity.CENTER); dn.setPadding(0, NUi.dp(3), 0, 0); hd.addView(dn, NUi.lp(-2, -2));
            NUi.tap(hd, new View.OnClickListener() { public void onClick(View x) { if (ds.equals(sel)) setView("day"); else { sel = ds; refresh(); } } });
            w.addView(hd, new LinearLayout.LayoutParams(NUi.dp(46), -2));
            LinearLayout col = NUi.col(c); col.setPadding(0, NUi.dp(2), 0, 0);
            List<It> it = M.get(k);
            if (it == null || it.isEmpty()) { TextView e = NUi.text(c, "Free", 12, NTheme.dim); e.setPadding(0, NUi.dp(12), 0, 0); col.addView(e); }
            else for (It i : it) { LinearLayout.LayoutParams l = NUi.lp(-1, -2); if (col.getChildCount() > 0) l.topMargin = NUi.dp(5); col.addView(chip(i), l); }
            w.addView(col, new LinearLayout.LayoutParams(0, -2, 1));
            LinearLayout.LayoutParams wl = NUi.lp(-1, -2); wl.topMargin = NUi.dp(k == a ? 0 : 6);
            card.addView(w, wl);
        }
    }

    /* the web's .cchip */
    View chip(final It i) {
        int cc = colOf(i.g);
        LinearLayout box = NUi.row(c); box.setGravity(Gravity.TOP);
        box.setBackground(NUi.round(NUi.mix(cc, i.k == K_TARGET ? .30f : i.k == K_REP ? 0f : .17f, NTheme.bg2), 10, i.k == K_REP ? NTheme.alpha(cc, .4f) : 0));
        View bar = new View(c); bar.setBackgroundColor(cc); box.addView(bar, new LinearLayout.LayoutParams(NUi.dp(3), -1));
        LinearLayout tx = NUi.col(c); tx.setPadding(NUi.dp(7), NUi.dp(7), NUi.dp(8), NUi.dp(7));
        TextView t = NUi.ell(NUi.body(c, (i.k == K_TARGET ? "⚑ " : "") + i.t, 12, NTheme.text, 600), 2);
        if (i.done) t.setPaintFlags(t.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        tx.addView(t);
        String sub = (i.time.isEmpty() ? "" : NDates.fmtTime(i.time)) + (i.k == K_REP ? (i.time.isEmpty() ? "" : " · ") + "repeats" : "");
        if (!sub.isEmpty()) { TextView m = NUi.text(c, sub, 9.5f, NTheme.muted); m.setTypeface(NFont.mono(500)); m.setAllCaps(true); m.setLetterSpacing(.04f); m.setPadding(0, NUi.dp(3), 0, 0); tx.addView(m); }
        box.addView(tx, new LinearLayout.LayoutParams(0, -2, 1));
        if (i.done) box.setAlpha(.5f);
        NUi.tap(box, new View.OnClickListener() { public void onClick(View x) { open(i); } });
        return box;
    }

    void open(It i) {
        if (i.k == K_DAY) F.dayGoal(NStore.s(i.o, "date"), i.o);
        else if (i.k == K_TARGET) sh.push(new NGoalScreen(sh, i.g.optString("id")));
        else F.step(i.g, i.o);
    }

    /* the web's .agr row */
    LinearLayout agr(final It i, boolean late, int bg) {
        int cc = colOf(i.g);
        LinearLayout r = NUi.row(c); r.setGravity(Gravity.TOP); r.setPadding(NUi.dp(10), NUi.dp(9), NUi.dp(10), NUi.dp(9));
        r.setBackground(NUi.ripple(NUi.round(bg, 14, 0), 14));
        View lead;
        if (i.k == K_DAY || i.k == K_STEP) {
            lead = NBits.check(c, i.done, cc, new View.OnClickListener() { public void onClick(View v) {
                if (i.k == K_DAY) NActs.toggleDay(i.o); else { String m = NActs.toggleStep(st, i.g, i.o); if (m != null) NShell.toast(m); }
                sh.save(); } });
        } else {
            FrameLayout f = new FrameLayout(c); f.setBackground(NUi.oval(NUi.mix(cc, .16f, bg), 0, 0));
            f.addView(NUi.icon(c, i.k == K_TARGET ? "flag" : "rep", 14, cc), new FrameLayout.LayoutParams(NUi.dp(14), NUi.dp(14), Gravity.CENTER));
            f.setLayoutParams(new LinearLayout.LayoutParams(NUi.dp(26), NUi.dp(26))); lead = f;
        }
        LinearLayout.LayoutParams ll = (LinearLayout.LayoutParams) lead.getLayoutParams(); ll.width = ll.height = NUi.dp(26); ll.rightMargin = NUi.dp(12); r.addView(lead, ll);
        LinearLayout tx = NUi.col(c);
        TextView t = NUi.body(c, i.t, 15, i.done ? NTheme.muted : NTheme.text, 600); t.setLineSpacing(0, 1.1f);
        if (i.done) t.setPaintFlags(t.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        tx.addView(t);
        LinearLayout s = NUi.row(c); s.setPadding(0, NUi.dp(3), 0, 0);
        String kind = i.k == K_DAY ? "Day goal" : i.k == K_STEP ? "Step" : i.k == K_REP ? "Repeats " + (i.o == null ? "" : i.o.optString("repeat")) : i.done ? "Goal achieved" : "Goal target";
        TextView kd = NUi.text(c, kind, 9.5f, cc); kd.setTypeface(NFont.mono(500)); kd.setAllCaps(true); kd.setLetterSpacing(.06f); s.addView(kd);
        if (!i.sub.isEmpty()) { TextView sb = NUi.ell(NUi.text(c, i.sub, 12.5f, NTheme.muted), 1); sb.setPadding(NUi.dp(8), 0, 0, 0); s.addView(sb, NUi.lpw(0, -2, 1)); }
        tx.addView(s);
        if (i.k == K_STEP && i.o != null && !i.o.optString("note").isEmpty()) { String nt = i.o.optString("note"); TextView n = NUi.text(c, nt.length() > 140 ? nt.substring(0, 139) + "…" : nt, 12.5f, NTheme.muted); n.setLineSpacing(0, 1.2f); n.setPadding(0, NUi.dp(5), 0, 0); tx.addView(n); }
        r.addView(tx, new LinearLayout.LayoutParams(0, -2, 1));
        String tm = i.time.isEmpty() ? (late && !i.done && (i.k == K_DAY || i.k == K_STEP) ? "LATE" : "") : NDates.fmtTime(i.time);
        if (!tm.isEmpty()) { TextView d = NUi.text(c, tm, 11, late && !i.done && (i.k == K_DAY || i.k == K_STEP) ? PINK : NTheme.muted); d.setTypeface(NFont.mono(500)); d.setLetterSpacing(.05f); d.setPadding(NUi.dp(8), NUi.dp(4), 0, 0); r.addView(d); }
        NUi.tap(r, new View.OnClickListener() { public void onClick(View v) { open(i); } });
        return r;
    }

    /* ---------- agenda ---------- */
    void agenda(Map<Integer, List<It>> M, String v) {
        if (M.isEmpty()) {
            LinearLayout e = NUi.col(c); e.setGravity(Gravity.CENTER_HORIZONTAL); e.setPadding(NUi.dp(20), NUi.dp(26), NUi.dp(20), NUi.dp(26)); e.setBackground(NUi.dashed(0, 22, NTheme.line2, 1.5f));
            TextView p = NUi.text(c, "Nothing planned " + (v.equals("day") ? "for this day" : v.equals("week") ? "this week" : "this month") + " yet.", 15, NTheme.muted); p.setGravity(Gravity.CENTER); e.addView(p, NUi.lp(-2, -2));
            e.addView(NUi.btn(c, "Add a goal for " + NDates.dayName(sel), true, new View.OnClickListener() { public void onClick(View x) { F.dayGoal(sel); } }), NUi.mt(14));
            add(e); return;
        }
        int selN = NDates.dnum(sel), tn = NDates.today();
        for (Map.Entry<Integer, List<It>> en : M.entrySet()) {
            final int k = en.getKey(); List<It> it = en.getValue();
            LinearLayout box = NUi.col(c); box.setBackground(k == selN && !v.equals("day") ? NUi.round(NTheme.surface, 22, NTheme.line2) : NUi.card(22)); box.setPadding(NUi.dp(12), NUi.dp(12), NUi.dp(12), NUi.dp(12));
            LinearLayout h = NUi.row(c); h.setGravity(Gravity.BOTTOM); h.setPadding(NUi.dp(10), NUi.dp(2), NUi.dp(10), NUi.dp(4));
            TextView num = NUi.title(c, String.valueOf(NDates.day(NDates.fromN(k))), 26); num.setTypeface(NFont.display(800)); num.setTextColor(k == tn ? NTheme.accent : NTheme.text); h.addView(num, NUi.lp(-2, -2));
            TextView wd = NUi.body(c, NDates.dayName(NDates.fromN(k)), 13, NTheme.text, 600); wd.setPadding(NUi.dp(10), 0, 0, NUi.dp(3)); h.addView(wd, NUi.lpw(0, -2, 1));
            TextView cn = NUi.text(c, it.size() + " item" + (it.size() > 1 ? "s" : ""), 10.5f, NTheme.muted); cn.setTypeface(NFont.mono(500)); cn.setAllCaps(true); cn.setPadding(0, 0, 0, NUi.dp(3)); h.addView(cn);
            box.addView(h);
            for (It i : it) box.addView(agr(i, k < tn, NTheme.surface));
            add(box, 10);
        }
    }

    /* ---------- day ---------- */
    void day(LinearLayout card, final int n) {
        int tn = NDates.today();
        Map<Integer, List<It>> M = items(n - 3, n + 3);
        List<It> it = M.get(n); if (it == null) it = new ArrayList<>();
        int tog = 0, dn = 0; for (It i : it) if (i.k == K_DAY || i.k == K_STEP) { tog++; if (i.done) dn++; }
        LinearLayout hd = NUi.row(c); hd.setPadding(NUi.dp(4), NUi.dp(2), NUi.dp(4), NUi.dp(18));
        LinearLayout tx = NUi.col(c);
        TextView w = NBits.meta(c, (fmt(n, "EEEE") + (n == tn ? " · today" : "")).toUpperCase(), NTheme.muted); w.setTextSize(10.5f); tx.addView(w);
        TextView big = NUi.title(c, fmt(n, "MMM d").toUpperCase(), 44); big.setTypeface(NFont.display(800)); big.setPadding(0, NUi.dp(8), 0, NUi.dp(10)); tx.addView(big);
        TextView sub = NBits.meta(c, it.isEmpty() ? "NOTHING PLANNED YET" : (it.size() + " planned" + (tog > 0 ? " · " + dn + " of " + tog + " done" : "")).toUpperCase(), NTheme.muted); sub.setTextSize(10.5f); tx.addView(sub);
        hd.addView(tx, NUi.lpw(0, -2, 1));
        hd.addView(new Ring(c, tog == 0 ? 0 : dn / (float) tog), new LinearLayout.LayoutParams(NUi.dp(76), NUi.dp(76)));
        card.addView(hd);
        LinearLayout strip = NUi.row(c);
        for (int i = 0; i < 7; i++) {
            final int k = n - 3 + i; List<It> li = M.get(k); int cnt = li == null ? 0 : li.size();
            LinearLayout b = NUi.col(c); b.setGravity(Gravity.CENTER_HORIZONTAL); b.setPadding(0, NUi.dp(10), 0, NUi.dp(9));
            boolean pk = k == n;
            b.setBackground(NUi.round(pk ? NTheme.text : NTheme.bg2, 16, 0));
            TextView dw = NUi.text(c, NDates.DAYS[NDates.dow(k)].toUpperCase(), 10, pk ? NTheme.alpha(NTheme.bg, .7f) : NTheme.muted); dw.setTypeface(NFont.mono(500)); dw.setLetterSpacing(.08f); dw.setGravity(Gravity.CENTER); b.addView(dw, NUi.lp(-2, -2));
            TextView num = NUi.title(c, String.valueOf(NDates.day(NDates.fromN(k))), 21); num.setTypeface(NFont.display(700)); num.setTextColor(pk ? NTheme.bg : k == tn ? NTheme.accent : NTheme.text); num.setPadding(0, NUi.dp(5), 0, NUi.dp(5)); num.setGravity(Gravity.CENTER); b.addView(num, NUi.lp(-2, -2));
            LinearLayout dots = NUi.row(c); dots.setGravity(Gravity.CENTER);
            for (int j = 0; j < Math.min(cnt, 3); j++) { View d = new View(c); d.setBackground(NUi.oval(pk ? NTheme.bg : NTheme.muted, 0, 0)); LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(NUi.dp(5), NUi.dp(5)); dl.leftMargin = dl.rightMargin = NUi.dp(1.5f); dots.addView(d, dl); }
            b.addView(dots, new LinearLayout.LayoutParams(-1, NUi.dp(5)));
            NUi.tap(b, new View.OnClickListener() { public void onClick(View x) { sel = NDates.fromN(k); refresh(); } });
            LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(0, -2, 1); if (i > 0) bl.leftMargin = NUi.dp(6);
            strip.addView(b, bl);
        }
        card.addView(strip);
        List<It> any = new ArrayList<>(), ev = new ArrayList<>();
        for (It i : it) { if (i.time.isEmpty()) any.add(i); else ev.add(i); }
        if (!any.isEmpty()) {
            LinearLayout ar = NUi.row(c); ar.setGravity(Gravity.TOP);
            TextView lb = NBits.meta(c, "ANYTIME", NTheme.muted); lb.setTextSize(10.5f); lb.setPadding(NUi.dp(2), NUi.dp(8), NUi.dp(10), 0); ar.addView(lb);
            NFlow fl = new NFlow(c, 6, 6);
            for (It i : any) fl.addView(chip(i), new ViewGroup.MarginLayoutParams(-2, -2));
            ar.addView(fl, new LinearLayout.LayoutParams(0, -2, 1));
            card.addView(ar, NUi.mt(16));
        }
        LinearLayout tools = NUi.row(c); tools.setPadding(NUi.dp(2), 0, NUi.dp(2), 0);
        TextView td = NBits.meta(c, ev.isEmpty() ? "TAP A TIME SLOT TO PLAN IT" : ev.size() + " SCHEDULED", NTheme.muted); td.setTextSize(10.5f); tools.addView(td, NUi.lpw(0, -2, 1));
        final float pp = ppm();
        tools.addView(NBits.sbtn(c, "minus", new View.OnClickListener() { public void onClick(View x) { setPpm(pp / 1.25f); } }));
        TextView z = NBits.meta(c, Math.round(pp * 100) + "%", NTheme.muted); z.setGravity(Gravity.CENTER); tools.addView(z, new LinearLayout.LayoutParams(NUi.dp(48), -2));
        tools.addView(NBits.sbtn(c, "plus", new View.OnClickListener() { public void onClick(View x) { setPpm(pp * 1.25f); } }));
        card.addView(tools, NUi.mt(18));
        int hDp = Math.max(300, Math.min(560, sh.a.getResources().getConfiguration().screenHeightDp - 330));
        final ScrollView gs = new ScrollView(c); gs.setVerticalScrollBarEnabled(false); gs.setBackground(NUi.round(NTheme.bg2, 20, 0));
        final float[] zoom = {1f};
        final android.view.ScaleGestureDetector sd = new android.view.ScaleGestureDetector(c, new android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(android.view.ScaleGestureDetector d) { zoom[0] *= d.getScaleFactor(); return true; }
            @Override public void onScaleEnd(android.view.ScaleGestureDetector d) { float z = zoom[0]; zoom[0] = 1f; if (Math.abs(z - 1f) > .06f) setPpm(pp * z); }
        });
        gs.setOnTouchListener(new View.OnTouchListener() { public boolean onTouch(View v, MotionEvent e) { v.getParent().requestDisallowInterceptTouchEvent(true); sd.onTouchEvent(e); return false; } });
        final TG tg = new TG(c, pp, n == tn, ev);
        gs.addView(tg, new FrameLayout.LayoutParams(-1, -2));
        card.addView(gs, NUi.lp(-1, NUi.dp(hDp)));
        final float[] ty0 = {0};
        tg.setOnTouchListener(new View.OnTouchListener() { public boolean onTouch(View v, MotionEvent e) { if (e.getActionMasked() == MotionEvent.ACTION_DOWN) ty0[0] = e.getY(); return false; } });
        tg.setOnClickListener(new View.OnClickListener() { public void onClick(View x) {
            int m = Math.round((ty0[0] - NUi.dp(12)) / tg.ppx / 30f) * 30; m = Math.max(0, Math.min(23 * 60 + 30, m));
            F.dayGoal(NDates.fromN(n), null, String.format(Locale.US, "%02d:%02d", m / 60, m % 60));
        } });
        int first = 450;
        if (n == tn) { Calendar k = Calendar.getInstance(); first = k.get(Calendar.HOUR_OF_DAY) * 60 + k.get(Calendar.MINUTE) - 90; }
        else if (!ev.isEmpty()) { String t = ev.get(0).time; first = Integer.parseInt(t.substring(0, 2)) * 60 + Integer.parseInt(t.substring(3, 5)) - 45; }
        final int ty = Math.max(0, Math.round(first * pp * NUi.dp(1)));
        gs.post(new Runnable() { public void run() { gs.scrollTo(0, ty); } });
    }

    static final class Ring extends View {
        final float p; final Paint q = new Paint(Paint.ANTI_ALIAS_FLAG); final RectF r = new RectF();
        Ring(Context c, float p) { super(c); this.p = p; q.setStyle(Paint.Style.STROKE); q.setStrokeCap(Paint.Cap.ROUND); }
        @Override protected void onDraw(Canvas cv) {
            float sw = NUi.dp(6), s = Math.min(getWidth(), getHeight()), in = sw / 2 + NUi.dp(2) * 0;
            r.set(in, in, s - in, s - in);
            q.setStrokeWidth(sw); q.setColor(NTheme.line2); cv.drawOval(r, q);
            if (p > 0) { q.setColor(NTheme.accent); cv.drawArc(r, -90, 360 * p, false, q); }
        }
    }

    /* the day's time grid; events are laid out in lanes when they overlap */
    final class TG extends ViewGroup {
        final float ppx; final boolean today; final List<It> ev; final int[][] geo; final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        TG(Context ctx, float ppm, boolean today, List<It> ev) {
            super(ctx); this.ppx = ppm * NUi.dp(1); this.today = today; this.ev = ev; setWillNotDraw(false);
            geo = new int[ev.size()][4];
            List<Integer> order = new ArrayList<>(); for (int i = 0; i < ev.size(); i++) order.add(i);
            for (int i = 0; i < ev.size(); i++) {
                It x = ev.get(i); int s = mins(x.time); int du = x.k == K_DAY && x.o != null ? Math.max(1, x.o.optInt("dur", 30)) : 30;
                geo[i][0] = s; geo[i][1] = Math.min(1440, s + du);
            }
            Collections.sort(order, new Comparator<Integer>() { public int compare(Integer a, Integer b) { int d = geo[a][0] - geo[b][0]; return d != 0 ? d : geo[b][1] - geo[a][1]; } });
            List<Integer> cl = new ArrayList<>(); int end = -1; List<List<Integer>> groups = new ArrayList<>();
            for (int idx : order) { int oe = Math.max(geo[idx][1], geo[idx][0] + 26); if (geo[idx][0] >= end && !cl.isEmpty()) { groups.add(cl); cl = new ArrayList<>(); } cl.add(idx); end = cl.size() == 1 ? oe : Math.max(end, oe); }
            if (!cl.isEmpty()) groups.add(cl);
            for (List<Integer> g : groups) {
                List<Integer> lanes = new ArrayList<>();
                for (int idx : g) { int k = -1; for (int j = 0; j < lanes.size(); j++) if (lanes.get(j) <= geo[idx][0]) { k = j; break; } int oe = Math.max(geo[idx][1], geo[idx][0] + 26); if (k < 0) { k = lanes.size(); lanes.add(oe); } else lanes.set(k, oe); geo[idx][2] = k; }
                for (int idx : g) geo[idx][3] = lanes.size();
            }
            for (int i = 0; i < ev.size(); i++) addView(eventView(ev.get(i), geo[i]));
        }
        int mins(String t) { try { return Integer.parseInt(t.substring(0, 2)) * 60 + Integer.parseInt(t.substring(3, 5)); } catch (Exception e) { return 0; } }
        String mm(int m) { return NDates.fmtTime(String.format(Locale.US, "%02d:%02d", (m / 60) % 24, m % 60)); }
        View eventView(final It x, int[] g) {
            int cc = colOf(x.g);
            LinearLayout b = NUi.row(c); b.setGravity(Gravity.TOP); b.setPadding(NUi.dp(10), NUi.dp(5), NUi.dp(8), NUi.dp(5));
            b.setBackground(NUi.round(NUi.mix(cc, .22f, NTheme.bg2), 10, 0));
            if (x.k == K_DAY || x.k == K_STEP) {
                View ck = NBits.check(c, x.done, cc, new View.OnClickListener() { public void onClick(View v) {
                    if (x.k == K_DAY) NActs.toggleDay(x.o); else { String m = NActs.toggleStep(st, x.g, x.o); if (m != null) NShell.toast(m); } sh.save(); } });
                LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(NUi.dp(20), NUi.dp(20)); l.rightMargin = NUi.dp(7); b.addView(ck, l);
            }
            LinearLayout tx = NUi.col(c);
            TextView t = NUi.ell(NUi.body(c, x.t, 12.5f, x.done ? NTheme.muted : NTheme.text, 700), g[1] - g[0] >= 60 ? 2 : 1);
            if (x.done) t.setPaintFlags(t.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tx.addView(t);
            TextView m = NUi.ell(NUi.text(c, mm(g[0]) + " – " + mm(g[1]) + (x.sub.isEmpty() ? "" : " · " + x.sub), 9.5f, NTheme.muted), 1); m.setTypeface(NFont.mono(500)); m.setAllCaps(true); tx.addView(m);
            b.addView(tx, new LinearLayout.LayoutParams(0, -2, 1));
            NUi.tap(b, new View.OnClickListener() { public void onClick(View v) { open(x); } });
            return b;
        }
        @Override protected void onMeasure(int w, int h) {
            int W = MeasureSpec.getSize(w), bodyW = W - NUi.dp(72);
            for (int i = 0; i < getChildCount(); i++) {
                int[] g = geo[i]; int cw = Math.max(NUi.dp(40), bodyW / g[3] - NUi.dp(4)), ch = Math.max(NUi.dp(26), Math.round((g[1] - g[0]) * ppx - NUi.dp(3)));
                getChildAt(i).measure(MeasureSpec.makeMeasureSpec(cw, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(ch, MeasureSpec.EXACTLY));
            }
            setMeasuredDimension(W, Math.round(1440 * ppx) + NUi.dp(24));
        }
        @Override protected void onLayout(boolean ch, int l, int t, int r, int b) {
            int bodyW = (r - l) - NUi.dp(72);
            for (int i = 0; i < getChildCount(); i++) {
                int[] g = geo[i]; View v = getChildAt(i);
                int x = NUi.dp(62) + g[2] * bodyW / g[3], y = NUi.dp(12) + Math.round(g[0] * ppx) + NUi.dp(1);
                v.layout(x, y, x + v.getMeasuredWidth(), y + v.getMeasuredHeight());
            }
        }
        @Override protected void onDraw(Canvas cv) {
            int W = getWidth(), top = NUi.dp(12);
            Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG); tp.setTypeface(NFont.mono(500)); tp.setTextSize(NUi.dp(9.5f)); tp.setColor(NTheme.muted); tp.setLetterSpacing(.04f);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(Math.max(1, NUi.dp(1)));
            for (int i = 0; i < 24; i++) {
                float y = top + i * 60 * ppx;
                p.setColor(NTheme.line); p.setPathEffect(null); cv.drawLine(0, y, W, y, p);
                p.setColor(NTheme.alpha(NTheme.line, .8f)); p.setPathEffect(new android.graphics.DashPathEffect(new float[]{NUi.dp(4), NUi.dp(3)}, 0));
                float hy = top + (i * 60 + 30) * ppx; cv.drawLine(NUi.dp(62), hy, W, hy, p); p.setPathEffect(null);
                String lbl = NDates.fmtTime(String.format(Locale.US, "%02d:00", i)).toUpperCase();
                float tw = tp.measureText(lbl); Paint bg = new Paint(); bg.setColor(NTheme.bg2);
                cv.drawRect(NUi.dp(10), y - NUi.dp(7), NUi.dp(10) + tw + NUi.dp(6), y + NUi.dp(6), bg);
                cv.drawText(lbl, NUi.dp(10), y + NUi.dp(3), tp);
            }
        }
        @Override protected void dispatchDraw(Canvas cv) {
            super.dispatchDraw(cv);
            if (today) {
                Calendar k = Calendar.getInstance(); float y = NUi.dp(12) + (k.get(Calendar.HOUR_OF_DAY) * 60 + k.get(Calendar.MINUTE)) * ppx;
                Paint q = new Paint(Paint.ANTI_ALIAS_FLAG); q.setColor(0xFFFF5A5A); q.setStrokeWidth(NUi.dp(2));
                cv.drawLine(NUi.dp(62), y, getWidth() - NUi.dp(10), y, q); cv.drawCircle(NUi.dp(62), y, NUi.dp(5), q);
            }
        }
    }

    /* tiny helper: rounded fill with an optional 1.5dp stroke */
    static final class GradientDrawableHolder {
        static void set(View v, int stroke, int fill, float r) { v.setBackground(NUi.round(fill, r, 0)); if (stroke != 0) v.setBackground(NUi.round(fill, r, stroke)); }
    }
}
