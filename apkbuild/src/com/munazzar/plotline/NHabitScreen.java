package com.munazzar.plotline;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/* One habit: laid out like the web page (hero, tiles, becoming automatic, history heat map, weekdays, slips). */
final class NHabitScreen extends NPage {
    final android.os.Handler shLive = new android.os.Handler(android.os.Looper.getMainLooper());
    final String id;
    static final int INV = 0xFFF4F2EC, PINK = 0x73FF7A7A;
    NHabitScreen(NShell sh, String id) { super(sh); this.id = id; back = true; }

    /* ---- small building blocks ---- */
    TextView data(String s, int color) {
        TextView t = NUi.text(c, s, 10.5f, color); t.setTypeface(NFont.mono(500)); t.setAllCaps(true); t.setLetterSpacing(.05f); return t;
    }

    TextView big(String v, String small, float sp, float smallSp, int color) {
        SpannableStringBuilder b = new SpannableStringBuilder(v);
        if (small != null && !small.isEmpty()) { b.append(small); b.setSpan(new RelativeSizeSpan(smallSp / sp), v.length(), b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); }
        TextView t = NUi.text(c, b, sp, color); t.setTypeface(NFont.display(800)); t.setIncludeFontPadding(false); return t;
    }

    View stH(String title, String meta) {
        LinearLayout r = NUi.row(c); r.setGravity(Gravity.BOTTOM);
        r.setPadding(0, NUi.dp(44), 0, NUi.dp(14));
        TextView t = NUi.text(c, title.toUpperCase(), 24, NTheme.text); t.setTypeface(NFont.display(700)); t.setIncludeFontPadding(false);
        r.addView(t, NUi.lpw(0, -2, 1));
        if (meta != null) r.addView(data(meta, NTheme.muted));
        return r;
    }

    LinearLayout panel() {
        LinearLayout p = NUi.col(c); p.setBackground(NUi.round(NTheme.surface, 26, NTheme.line)); p.setPadding(NUi.dp(22), NUi.dp(22), NUi.dp(22), NUi.dp(22)); return p;
    }

    /* "btn ink" / "btn inkline" on the tinted hero */
    View btn(String icon, String label, boolean solid, View.OnClickListener l) {
        LinearLayout b = NUi.row(c); b.setGravity(Gravity.CENTER);
        b.setPadding(NUi.dp(20), 0, NUi.dp(20), 0);
        b.setBackground(NUi.ripple(solid ? NUi.round(NTheme.INK, 15, 0) : NUi.round(0, 15, NTheme.alpha(NTheme.INK, .28f)), 15));
        int fg = solid ? INV : NTheme.INK;
        if (icon != null) { b.addView(NUi.icon(c, icon, 18, fg)); }
        TextView t = NUi.body(c, label, 14.5f, fg, 600); if (icon != null) t.setPadding(NUi.dp(8), 0, 0, 0); b.addView(t);
        b.setMinimumHeight(NUi.dp(46)); b.setLayoutParams(NUi.lp(-2, NUi.dp(46)));
        NUi.tap(b, l); return b;
    }

    View inkIbtn(String icon, View.OnClickListener l) {
        NUi.Fix f = new NUi.Fix(c, NUi.dp(40), NUi.dp(40));
        f.setBackground(NUi.ripple(NUi.round(NTheme.alpha(NTheme.INK, .10f), 13, NTheme.alpha(NTheme.INK, .25f)), 13));
        f.addView(NUi.icon(c, icon, 18, NTheme.INK), new android.widget.FrameLayout.LayoutParams(NUi.dp(18), NUi.dp(18), Gravity.CENTER));
        f.setLayoutParams(NUi.lp(NUi.dp(40), NUi.dp(40))); NUi.tap(f, l); return f;
    }

    View pill(String icon, String label, boolean on, View.OnClickListener l) {
        LinearLayout p = NUi.row(c); p.setGravity(Gravity.CENTER);
        p.setPadding(NUi.dp(label == null ? 12 : 14), 0, NUi.dp(label == null ? 12 : 14), 0);
        p.setBackground(NUi.ripple(NUi.round(on ? NUi.mix(NTheme.accent, .12f, NTheme.surface) : NTheme.surface, 99, on ? NTheme.alpha(NTheme.accent, .55f) : NTheme.line2), 99));
        p.addView(NUi.icon(c, icon, 17, on ? NTheme.accent : NTheme.text));
        if (label != null) { TextView t = NUi.body(c, label, 12, NTheme.text, 600); t.setPadding(NUi.dp(6), 0, 0, 0); p.addView(t); }
        p.setMinimumHeight(NUi.dp(42)); p.setLayoutParams(NUi.lp(-2, NUi.dp(42))); NUi.tap(p, l); return p;
    }

    void tiles(String[][] L) {
        LinearLayout col = NUi.col(c);
        for (int i = 0; i < L.length; i += 2) {
            LinearLayout row = NUi.row(c); row.setGravity(Gravity.TOP);
            for (int j = 0; j < 2 && i + j < L.length; j++) {
                LinearLayout t = NUi.col(c); t.setBackground(NUi.round(NTheme.surface, 20, NTheme.line)); t.setPadding(NUi.dp(16), NUi.dp(16), NUi.dp(16), NUi.dp(14));
                t.addView(big(L[i + j][0], L[i + j][1], 38, 16, NTheme.text));
                t.addView(data(L[i + j][2], NTheme.muted), NUi.mt(6));
                LinearLayout.LayoutParams lp = NUi.lpw(0, -1, 1); if (j == 0) lp.rightMargin = NUi.dp(5); else lp.leftMargin = NUi.dp(5);
                row.addView(t, lp);
            }
            if (L.length - i == 1) row.addView(new View(c), NUi.lpw(0, 1, 1));
            LinearLayout.LayoutParams rp = NUi.lp(-1, -2); if (i > 0) rp.topMargin = NUi.dp(10);
            col.addView(row, rp);
        }
        add(col, 16);
    }

    View nudge(String title, String text) {
        LinearLayout r = NUi.row(c); r.setGravity(Gravity.TOP);
        r.setBackground(NUi.dashed(NUi.mix(NTheme.accent, .06f, NTheme.bg), 20, NTheme.alpha(NTheme.accent, .45f), 1.5f)); r.setPadding(NUi.dp(18), NUi.dp(16), NUi.dp(18), NUi.dp(16));
        LinearLayout.LayoutParams fl = NUi.lp(NUi.dp(20), NUi.dp(20)); fl.topMargin = NUi.dp(2); fl.rightMargin = NUi.dp(14); r.addView(NUi.icon(c, "shield", 20, NTheme.accent), fl);
        LinearLayout tx = NUi.col(c); tx.addView(NUi.body(c, title, 15, NTheme.text, 700)); tx.addView(NUi.text(c, text, 13.5f, NTheme.muted), NUi.mt(2));
        r.addView(tx, NUi.lpw(0, -2, 1)); return r;
    }

    static String money(JSONObject h, double x) {
        String cur = h.optString("cur", "$");
        return cur + (x >= 100 ? String.format(java.util.Locale.US, "%,d", Math.round(x)) : String.format(java.util.Locale.US, x < 10 ? "%.2f" : "%.0f", x));
    }
    static String hrs(double m) { return m >= 60 ? Math.round(m / 60) + " h" : Math.round(m) + " min"; }
    static String cap(String s) { return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1); }

    double[] saved(JSONObject h) {
        JSONArray sl = h.optJSONArray("slips"); Set<String> sd = new HashSet<>(); long start = h.optLong("start");
        if (sl != null) for (int i = 0; i < sl.length(); i++) { JSONObject s = sl.optJSONObject(i); if (s != null && s.optLong("t") >= start) sd.add(NDates.ymd(s.optLong("t"))); }
        double d = Math.max(0, (System.currentTimeMillis() - start) / 86400000.0 - sd.size());
        return new double[]{d * h.optDouble("cost", 0), d * h.optDouble("mins", 0)};
    }

    int urgesOk(JSONObject h) { JSONArray u = h.optJSONArray("urges"); int n = 0; if (u != null) for (int i = 0; i < u.length(); i++) { JSONObject x = u.optJSONObject(i); if (x != null && NUrge.ok(x)) n++; } return n; }

    /* ---- the page ---- */
    @Override void build() {
        final JSONObject h = st.find("habits", id);
        if (h == null) { header(null); add(NBits.empty(c, "This habit was removed", null), 20); return; }
        final NForms F = new NForms(sh);
        final String hid = h.optString("id"), hroute = "habit/" + hid; final JSONObject hd = NMore.d("id", hid, "k", "habit");
        final boolean active = "active".equals(NHabits.status(h));
        final String kind = NHabits.kind(h); final boolean quit = kind.equals("quit"), lim = NHabits.limitMode(h);
        int col = NTheme.areaCol(h.optString("area"));
        final String td = NDates.ymd();

        /* crumb */
        LinearLayout crumb = NUi.row(c);
        crumb.addView(NUi.ibtn(c, "back", new View.OnClickListener() { public void onClick(View v) { sh.pop(); } }));
        crumb.addView(new View(c), NUi.lpw(0, 1, 1));
        List<View> acts = new ArrayList<>();
        acts.add(NUi.ibtn(c, "link", new View.OnClickListener() { public void onClick(View v) { new NForms(sh).threadsFor("habit", hid); } }));
        if (active && !quit) {
            String bl = h.optBoolean("remind") ? (h.optString("time").isEmpty() ? "On" : NDates.fmtTime(h.optString("time"))) : "Off";
            acts.add(pill("bell", bl, h.optBoolean("remind"), new View.OnClickListener() { public void onClick(View v) { NReminders.open(sh, "habit", h.optString("id")); } }));
            JSONObject au = h.optJSONObject("auto"); boolean ao = au != null && !au.optString("type").isEmpty();
            acts.add(pill("flame", ao ? "Auto" : null, ao, new View.OnClickListener() { public void onClick(View v) { NAutoEdit.open(sh, "habit", hid, null); } }));
        }
        acts.add(NUi.ibtn(c, "edit", new View.OnClickListener() { public void onClick(View v) { F.habit(h); } }));
        acts.add(NUi.ibtn(c, "more", new View.OnClickListener() { public void onClick(View v) { more(h); } }));
        for (View a : acts) { LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(a.getLayoutParams()); l.leftMargin = NUi.dp(6); crumb.addView(a, l); }
        add(crumb);

        /* hero */
        LinearLayout hero = NUi.col(c);
        hero.setBackground(NCard.bg(col, 34));
        hero.setPadding(NUi.dp(22), NUi.dp(22), NUi.dp(22), NUi.dp(22));
        String kindTxt = quit ? (lim ? "Cutting down" : "Breaking") : kind.equals("routine") ? "Routine" : "Building";
        String line = kindTxt + " · " + NTheme.areaName(h.optString("area")) + (!quit ? " · " + NHabits.freqText(h) : "") + (!active ? " · " + cap(NHabits.status(h)) : "");
        hero.addView(data(line, NTheme.INK_MUTED));
        SpannableStringBuilder tb = new SpannableStringBuilder(NHabits.icon(h) + "  "); tb.setSpan(new RelativeSizeSpan(.62f), 0, tb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tb.append(h.optString("title").toUpperCase());
        TextView t = NUi.text(c, tb, 46, NTheme.INK); t.setTypeface(NFont.display(800)); t.setLineSpacing(0, .86f); t.setIncludeFontPadding(false);
        LinearLayout.LayoutParams tp = NUi.mt(16); tp.bottomMargin = NUi.dp(14); hero.addView(t, tp);
        if (!h.optString("why").isEmpty()) { TextView w = NUi.text(c, h.optString("why"), 16.5f, NTheme.alpha(NTheme.INK, .82f)); w.setLineSpacing(0, 1.2f); hero.addView(w); }
        if (!h.optString("cue").isEmpty() && kind.equals("build")) {
            int tg = NHabits.target(h);
            LinearLayout cr = NUi.row(c); cr.setGravity(Gravity.TOP);
            LinearLayout.LayoutParams il = NUi.lp(NUi.dp(14), NUi.dp(14)); il.topMargin = NUi.dp(3); il.rightMargin = NUi.dp(6); cr.addView(NUi.icon(c, "link", 14, NTheme.alpha(NTheme.INK, .85f)), il);
            cr.addView(NUi.text(c, h.optString("cue") + ", I will " + h.optString("title").toLowerCase() + (tg > 1 ? " (" + tg + " " + h.optString("unit") + ")" : "") + ".", 14.5f, NTheme.alpha(NTheme.INK, .85f)), NUi.lpw(0, -2, 1));
            hero.addView(cr, NUi.mt(12));
        }
        if (quit && !lim) {
            long since = NHabits.since(h); long ms = Math.max(0, System.currentTimeMillis() - since);
            hero.addView(clock(ms), NUi.mt(22));
            NFlow a = new NFlow(c, 8, 8);
            a.addView(btn("wave", "I have an urge", true, new View.OnClickListener() { public void onClick(View v) { NUrge.urge(sh, h); } }));
            a.addView(btn(null, "Log a slip", false, new View.OnClickListener() { public void onClick(View v) { NUrge.slip(sh, h); } }));
            hero.addView(a, NUi.mt(18));
        } else if (quit) {
            final int v = h.optJSONObject("log") == null ? 0 : h.optJSONObject("log").optInt(td, 0);
            LinearLayout b = NUi.row(c); b.setGravity(Gravity.BOTTOM);
            b.addView(big(String.valueOf(v), "", 84, 84, NTheme.INK));
            TextView u = NUi.text(c, "of " + NHabits.limit(h) + (h.optString("unit").isEmpty() ? "" : " " + h.optString("unit")) + " today", 15, NTheme.INK); u.setPadding(NUi.dp(10), 0, 0, NUi.dp(6)); b.addView(u);
            hero.addView(b, NUi.mt(22));
            NFlow a = new NFlow(c, 8, 8);
            a.addView(btn("plus", "Log one", true, new View.OnClickListener() { public void onClick(View x) { NUi.haptic(x); try { NHabits.obj(h, "log").put(td, v + 1); } catch (Exception ignored) { } sh.save(); } }));
            if (v > 0) a.addView(btn(null, "Undo", false, new View.OnClickListener() { public void onClick(View x) { try { JSONObject lg = NHabits.obj(h, "log"); if (v - 1 > 0) lg.put(td, v - 1); else lg.remove(td); } catch (Exception ignored) { } sh.save(); } }));
            a.addView(btn("wave", "I have an urge", false, new View.OnClickListener() { public void onClick(View x) { NUrge.urge(sh, h); } }));
            hero.addView(a, NUi.mt(18));
        } else {
            final int v = NHabits.val(h, td), n = NHabits.target(h), s = NHabits.streak(h); String u = NHabits.freq(h).equals("times") ? "wk" : "d";
            LinearLayout hf = NUi.row(c); hf.setGravity(Gravity.BOTTOM);
            LinearLayout left = NUi.col(c);
            left.addView(data("Strength " + NHabits.strength(h) + "%", NTheme.INK_MUTED));
            left.addView(NBits.bar(c, NHabits.strength(h) / 100f, NTheme.INK, 6), NUi.mt(12));
            hf.addView(left, NUi.lpw(0, -2, 1));
            TextView pct = big(String.valueOf(s), u, 76, 30, NTheme.INK); pct.setLineSpacing(0, .78f);
            LinearLayout.LayoutParams pl = NUi.lp(-2, -2); pl.leftMargin = NUi.dp(22); hf.addView(pct, pl);
            hero.addView(hf, NUi.mt(30));
            if (active) {
                if (NHabits.routine(h)) {
                    hero.addView(btn("play", v >= n ? "Run again" : v > 0 ? "Continue · " + v + " of " + n : "Start routine", true, new View.OnClickListener() { public void onClick(View x) { NUrge.routine(sh, hid); } }), NUi.mt(20));
                } else if (n > 1) {
                    LinearLayout hs = NUi.row(c);
                    hs.addView(inkIbtn("minus", new View.OnClickListener() { public void onClick(View x) { NHabits.setVal(h, td, Math.max(0, NHabits.val(h, td) - 1)); sh.save(); } }));
                    LinearLayout mid = NUi.col(c); mid.setGravity(Gravity.CENTER_HORIZONTAL); mid.setMinimumWidth(NUi.dp(110));
                    TextView bv = big(String.valueOf(v), "", 54, 54, NTheme.INK); bv.setGravity(Gravity.CENTER); mid.addView(bv, NUi.lp(-2, -2));
                    mid.addView(NUi.text(c, "of " + n + " " + h.optString("unit") + " today", 13, NTheme.alpha(NTheme.INK, .75f)));
                    LinearLayout.LayoutParams ml = NUi.lp(-2, -2); ml.leftMargin = ml.rightMargin = NUi.dp(18); hs.addView(mid, ml);
                    hs.addView(inkIbtn("plus", new View.OnClickListener() { public void onClick(View x) { NUi.haptic(x); NHabits.setVal(h, td, NHabits.val(h, td) + 1); NActs.milestone(st, h); sh.save(); } }));
                    hero.addView(hs, NUi.mt(20));
                } else {
                    NFlow a = new NFlow(c, 8, 8);
                    a.addView(btn("check", v >= n ? "Done today · undo" : "Mark done for today", v < n, new View.OnClickListener() { public void onClick(View x) {
                        NUi.haptic(x); boolean full = NActs.habitTap(h); String m = full ? NActs.milestone(st, h) : null; sh.save(); if (m != null) NShell.toast(m);
                    } }));
                    if (NHabits.due(h, td) && v < n || NHabits.skip(h, td)) a.addView(btn(null, NHabits.skip(h, td) ? "Undo rest day" : "Rest day", false, new View.OnClickListener() { public void onClick(View x) {
                        try { if (NHabits.skip(h, td)) NHabits.obj(h, "skip").remove(td); else { NHabits.setVal(h, td, 0); NHabits.obj(h, "skip").put(td, 1); } } catch (Exception ignored) { }
                        sh.save(); NShell.toast(NHabits.skip(h, td) ? "Rest day. Your streak is safe." : "Rest day removed");
                    } }));
                    hero.addView(a, NUi.mt(20));
                }
            }
        }
        add(hero, 14);
        { LinearLayout shp = NUi.col(c); shp.setVisibility(View.GONE); add(shp, 12); NShare.panel(sh, "habit", h.optString("id"), shp, shLive); }

        /* tiles and the rest */
        if (quit && !lim) {
            double[] sv = saved(h); int d = NHabits.cleanDays(h); int nx = 0; for (int m : NActs.MILES) if (m > d) { nx = m; break; }
            JSONArray sl = h.optJSONArray("slips"); int slips = 0; if (sl != null) for (int i = 0; i < sl.length(); i++) if (sl.optJSONObject(i) != null && sl.optJSONObject(i).optLong("t") >= h.optLong("start")) slips++;
            tiles(new String[][]{{String.valueOf(bestRun(h)), "d", "Best run"}, {h.optDouble("cost", 0) > 0 ? money(h, sv[0]) : String.valueOf(urgesOk(h)), "", h.optDouble("cost", 0) > 0 ? "Money saved" : "Urges beaten"},
                {h.optDouble("mins", 0) > 0 ? hrs(sv[1]) : String.valueOf(slips), "", h.optDouble("mins", 0) > 0 ? "Time back" : "Slips"}, {nx > 0 ? NHabitsPage.mileName(nx) : "—", "", "Next milestone"}});
            add(stH("Milestones", "current run")); miles(h, d, nx, col);
        } else if (quit) {
            int tn = NDates.today(); double tot = 0; int cnt = 0;
            for (int k = tn - 7; k < tn; k++) { String ds = NDates.fromN(k); if (ds.compareTo(NHabits.startDate(h)) < 0) continue; tot += h.optJSONObject("log") == null ? 0 : h.optJSONObject("log").optDouble(ds, 0); cnt++; }
            tiles(new String[][]{{String.valueOf(NHabits.limitStreak(h)), "d", "On track"}, {String.valueOf(limBest(h)), "d", "Best run"}, {String.valueOf(urgesOk(h)), "", "Urges beaten"}, {cnt > 0 ? String.format(java.util.Locale.US, "%.1f", tot / cnt) : "—", "", "Daily avg · 7d"}});
        } else {
            boolean wk = NHabits.freq(h).equals("times"); String u = wk ? "wk" : "d"; int rt = NHabits.rate(h, 30);
            tiles(new String[][]{{String.valueOf(NHabits.streak(h)), u, "Current streak"}, {String.valueOf(NHabits.best(h)), u, "Best streak"}, {String.valueOf(NHabits.strength(h)), "%", "Strength"}, {rt < 0 ? "—" : String.valueOf(rt), rt < 0 ? "" : "%", wk ? "Last 4 weeks" : "Last 30 days"}});
            if (!h.optString("mini").isEmpty()) add(nudge("On a hard day", h.optString("mini") + ". It still counts."), 14);
            if (kind.equals("routine")) routineSteps(h);
            int age = NDates.today() - NDates.dnum(NHabits.startDate(h)) + 1;
            add(stH("Becoming automatic", "day " + age));
            LinearLayout pn = panel();
            pn.addView(NBits.bar(c, Math.min(1f, age / 66f), col, 6));
            pn.addView(NUi.text(c, age < 66 ? "New habits take about two months to feel automatic, on average, and the range is wide. Day " + age + " of roughly 66. Keep the bar low and show up." : "Past the two-month mark. For most people it feels automatic by now. Protect it on busy days.", 13.5f, NTheme.muted), NUi.mt(10));
            add(pn);
        }
        add(stH("History", quit ? null : "tap a day to change it"));
        LinearLayout hp = panel(); hp.addView(heat(h, col, quit, lim)); add(hp);
        if (!quit) weekdays(h, col);
        if (quit) {
            if (!h.optString("plan").isEmpty()) add(nudge("When the urge hits", h.optString("plan")), 26);
            if (!lim) slipList(h);
        }
        JSONObject g = st.find("goals", h.optString("goalId"));
        if (g != null) {
            add(stH("Supports", null));
            LinearLayout chip = NUi.row(c); chip.setPadding(NUi.dp(12), NUi.dp(9), NUi.dp(16), NUi.dp(9));
            chip.setBackground(NUi.ripple(NUi.round(NTheme.surface, 99, NTheme.line), 99));
            View dot = new View(c); dot.setBackground(NUi.oval(NTheme.areaCol(g.optString("area")), 0, 0)); LinearLayout.LayoutParams dl = NUi.lp(NUi.dp(10), NUi.dp(10)); dl.rightMargin = NUi.dp(9); chip.addView(dot, dl);
            chip.addView(NUi.body(c, g.optString("title"), 14, NTheme.text, 600));
            TextView pc = NBits.meta(c, NActs.pct(st, g) + "%", NTheme.muted); pc.setTextSize(10.5f); pc.setPadding(NUi.dp(9), 0, 0, 0); chip.addView(pc);
            final String gid = g.optString("id"); NUi.tap(chip, new View.OnClickListener() { public void onClick(View v) { sh.push(new NGoalScreen(sh, gid)); } });
            LinearLayout.LayoutParams cp = NUi.lp(-2, -2); add2(chip, cp);
        }
    }

    void add2(View v, LinearLayout.LayoutParams p) { body.addView(v, p); }

    int limBest(JSONObject h) {
        int tn = NDates.today(), st0 = NDates.dnum(NHabits.startDate(h)), b = 0, r = 0;
        for (int k = st0; k <= tn; k++) { if (NHabits.limitOk(h, NDates.fromN(k))) { r++; b = Math.max(b, r); } else r = 0; }
        return b;
    }

    /* big live clock for a quit habit */
    View clock(long ms) {
        long d = ms / 86400000L, hh = ms % 86400000L / 3600000L, mm = ms % 3600000L / 60000L, ss = ms % 60000L / 1000L;
        LinearLayout r = NUi.row(c); r.setGravity(Gravity.BOTTOM);
        r.addView(big(String.valueOf(d), "", 84, 84, NTheme.INK));
        TextView u = NUi.body(c, d == 1 ? "day" : "days", 15, NTheme.INK, 700); u.setPadding(NUi.dp(6), 0, NUi.dp(8), NUi.dp(6)); r.addView(u);
        TextView hms = NUi.text(c, String.format(java.util.Locale.US, "%02d:%02d:%02d", hh, mm, ss), 15, NTheme.alpha(NTheme.INK, .72f)); hms.setTypeface(NFont.mono(500)); hms.setPadding(0, 0, 0, NUi.dp(6)); r.addView(hms);
        return r;
    }

    void miles(JSONObject h, int d, int nx, int col) {
        LinearLayout grid = NUi.col(c); LinearLayout row = null;
        for (int i = 0; i < NActs.MILES.length; i++) {
            if (i % 3 == 0) { row = NUi.row(c); LinearLayout.LayoutParams rp = NUi.lp(-1, -2); if (i > 0) rp.topMargin = NUi.dp(8); grid.addView(row, rp); }
            int m = NActs.MILES[i]; boolean ok = d >= m, nxt = m == nx;
            LinearLayout cell = NUi.row(c); cell.setPadding(NUi.dp(12), NUi.dp(11), NUi.dp(12), NUi.dp(11));
            cell.setBackground(NUi.round(NTheme.surface, 15, nxt ? NTheme.alpha(col, .55f) : NTheme.line));
            FrameLayout22 ck = new FrameLayout22(c, ok, col);
            if (ok) ck.addView(NUi.icon(c, "check", 12, NTheme.on(col)), new android.widget.FrameLayout.LayoutParams(NUi.dp(12), NUi.dp(12), Gravity.CENTER));
            else if (nxt) ck.addView(NUi.icon(c, "flag", 12, NTheme.text), new android.widget.FrameLayout.LayoutParams(NUi.dp(12), NUi.dp(12), Gravity.CENTER));
            LinearLayout.LayoutParams kl = NUi.lp(NUi.dp(22), NUi.dp(22)); kl.rightMargin = NUi.dp(10); cell.addView(ck, kl);
            TextView t = NUi.ell(NUi.body(c, NHabitsPage.mileName(m), 13, ok || nxt ? NTheme.text : NTheme.muted, 700), 1); cell.addView(t, NUi.lpw(0, -2, 1));
            LinearLayout.LayoutParams cl = NUi.lpw(0, -2, 1); if (i % 3 < 2) cl.rightMargin = NUi.dp(8); row.addView(cell, cl);
        }
        add(grid);
    }

    static final class FrameLayout22 extends android.widget.FrameLayout {
        FrameLayout22(android.content.Context c, boolean ok, int col) { super(c); setBackground(ok ? NUi.oval(col, 0, 0) : NUi.oval(0, NTheme.line2, 1.5f)); }
    }

    void routineSteps(final JSONObject h) {
        add(stH("Steps", null));
        LinearLayout box = NBits.listBox(c); box.setPadding(0, 0, 0, 0);
        final String td = NDates.ymd(); JSONArray steps = NHabits.steps(h);
        JSONObject rs = h.optJSONObject("rs"); JSONArray dn = rs == null ? null : rs.optJSONArray(td); Set<String> doneSet = new HashSet<>();
        if (dn != null) for (int i = 0; i < dn.length(); i++) doneSet.add(dn.optString(i));
        for (int i = 0; i < steps.length(); i++) {
            final JSONObject sp = steps.optJSONObject(i); if (sp == null) continue;
            if (i > 0) box.addView(NBits.dividerFull(c));
            LinearLayout r = NUi.row(c); r.setPadding(NUi.dp(18), NUi.dp(15), NUi.dp(18), NUi.dp(15));
            final android.widget.CheckBox cb = new android.widget.CheckBox(c); cb.setChecked(doneSet.contains(sp.optString("id")));
            cb.setButtonTintList(android.content.res.ColorStateList.valueOf(NTheme.accent));
            r.addView(cb, NUi.lp(NUi.dp(40), NUi.dp(40)));
            TextView nb = NBits.meta(c, String.format(java.util.Locale.US, "%02d", i + 1), NTheme.text); nb.setGravity(Gravity.CENTER);
            nb.setBackground(NUi.round(0, 8, NTheme.line2)); LinearLayout.LayoutParams nl = NUi.lp(NUi.dp(34), NUi.dp(24)); nl.leftMargin = NUi.dp(6); nl.rightMargin = NUi.dp(12); r.addView(nb, nl);
            r.addView(NUi.body(c, sp.optString("title"), 16, NTheme.text, 600), NUi.lpw(0, -2, 1));
            if (sp.optInt("min", 0) > 0) r.addView(data(sp.optInt("min") + " min", NTheme.muted));
            final String sid = sp.optString("id");
            cb.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { NUi.haptic(v); toggleStep(h, sid, cb.isChecked(), td); } });
            NUi.tap(r, new View.OnClickListener() { public void onClick(View v) { cb.performClick(); } });
            box.addView(r);
        }
        if (steps.length() == 0) box.addView(NUi.text(c, "No steps yet. Add them in Edit.", 14, NTheme.muted));
        add(box);
    }

    void toggleStep(JSONObject h, String sid, boolean on, String ds) {
        try {
            JSONObject rs = NHabits.obj(h, "rs"); Set<String> L = new HashSet<>(); JSONArray a = rs.optJSONArray(ds);
            if (a != null) for (int i = 0; i < a.length(); i++) L.add(a.optString(i));
            if (on) L.add(sid); else L.remove(sid);
            JSONArray out = new JSONArray(), steps = NHabits.steps(h);
            for (int i = 0; i < steps.length(); i++) { JSONObject s = steps.optJSONObject(i); if (s != null && L.contains(s.optString("id"))) out.put(s.optString("id")); }
            if (out.length() == 0) rs.remove(ds); else { rs.put(ds, out); NHabits.obj(h, "skip").remove(ds); }
        } catch (Exception ignored) { }
        String m = NHabits.done(h, ds) ? NActs.milestone(st, h) : null;
        sh.save(); if (m != null) NShell.toast(m);
    }

    /* the 20-week map: columns are weeks (Sunday at the top), tap a day to change it */
    View heat(final JSONObject h, final int col, boolean quit, boolean lim) {
        final int W = 20, tn = NDates.today(), a = NDates.wkStart(tn) - 7 * (W - 1);
        LinearLayout box = NUi.col(c);
        final String[] mo = new String[W];
        for (int i = 0; i < W; i++) { String ds = NDates.fromN(a + 7 * i); mo[i] = NDates.day(ds) <= 7 ? NDates.MONTHS[NDates.month(ds)].substring(0, 3).toUpperCase() : ""; }
        View months = new View(c) {
            final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onMeasure(int w, int hh) { setMeasuredDimension(MeasureSpec.getSize(w), NUi.dp(14)); }
            @Override protected void onDraw(Canvas cv) {
                p.setTextSize(NUi.dp(9.5f)); p.setColor(NTheme.muted); p.setTypeface(NFont.mono(500));
                float cw = getWidth() / (float) W;
                for (int i = 0; i < W; i++) if (!mo[i].isEmpty()) cv.drawText(mo[i], i * cw, NUi.dp(10), p);
            }
        };
        box.addView(months, NUi.lp(-1, NUi.dp(14)));
        long startMs = h.optLong("start"); String startDay = quit ? NDates.ymd(startMs) : null;
        for (int r = 0; r < 7; r++) {
            LinearLayout row = NUi.row(c);
            for (int w = 0; w < W; w++) {
                int k = a + 7 * w + r; final String ds = NDates.fromN(k);
                char cl;
                if (k > tn) cl = 'f';
                else if (quit) {
                    if (ds.compareTo(startDay) < 0) cl = 'n';
                    else if (lim) { int v = h.optJSONObject("log") == null ? 0 : h.optJSONObject("log").optInt(ds, 0); cl = v > NHabits.limit(h) ? 'x' : v > 0 ? 'p' : 'd'; }
                    else { cl = 'd'; JSONArray sl = h.optJSONArray("slips"); if (sl != null) for (int i = 0; i < sl.length(); i++) if (sl.optJSONObject(i) != null && NDates.ymd(sl.optJSONObject(i).optLong("t")).equals(ds)) { cl = 'x'; break; } }
                } else {
                    int v = NHabits.val(h, ds), n = NHabits.target(h);
                    cl = ds.compareTo(NHabits.startDate(h)) < 0 ? 'n' : NHabits.skip(h, ds) ? 's' : v >= n ? 'd' : v > 0 ? 'p' : (!NHabits.due(h, ds) || k == tn || NHabits.freq(h).equals("times")) ? 'n' : 'x';
                }
                int bg;
                switch (cl) {
                    case 'd': bg = col; break;
                    case 'p': bg = NTheme.alpha(col, .42f); break;
                    case 'x': bg = PINK; break;
                    case 's': bg = NTheme.alpha(NTheme.line2, .6f); break;
                    case 'f': bg = NTheme.alpha(NTheme.line, .18f); break;
                    default: bg = NTheme.alpha(NTheme.line, .45f);
                }
                NUi.Sq sq = new NUi.Sq(c, 40); sq.setBackground(NUi.round(bg, 4, 0));
                if (k == tn) sq.setForeground(NUi.round(0, 4, NTheme.text));
                if (!quit && k <= tn) NUi.tap(sq, new View.OnClickListener() { public void onClick(View v) { NHabitDay.open(sh, h, ds); } });
                LinearLayout.LayoutParams lp = NUi.lpw(0, -2, 1); lp.rightMargin = w < W - 1 ? NUi.dp(3) : 0; row.addView(sq, lp);
            }
            LinearLayout.LayoutParams rp = NUi.lp(-1, -2); rp.topMargin = NUi.dp(r == 0 ? 6 : 3); box.addView(row, rp);
        }
        NFlow leg = new NFlow(c, 14, 6);
        String[] ln = quit ? new String[]{lim ? "Under limit" : "Free", "Some", lim ? "Over" : "Slip"} : new String[]{"Done", "Partly", "Missed", "Rest day"};
        int[] lc = {col, NTheme.alpha(col, .42f), PINK, NTheme.alpha(NTheme.line2, .6f)};
        for (int i = 0; i < ln.length; i++) {
            LinearLayout it = NUi.row(c); View sw = new View(c); sw.setBackground(NUi.round(lc[i], 3, 0)); LinearLayout.LayoutParams sl = NUi.lp(NUi.dp(10), NUi.dp(10)); sl.rightMargin = NUi.dp(6); it.addView(sw, sl);
            it.addView(data(ln[i], NTheme.muted)); leg.addView(it);
        }
        LinearLayout.LayoutParams ll = NUi.lp(-1, -2); ll.topMargin = NUi.dp(6); box.addView(leg, ll);
        return box;
    }

    void weekdays(JSONObject h, int col) {
        int tn = NDates.today(), st0 = Math.max(NDates.dnum(NHabits.startDate(h)), tn - 84);
        int[] due = new int[7], dn = new int[7]; int tot = 0;
        for (int k = st0; k < tn; k++) {
            String ds = NDates.fromN(k); int w = NDates.dow(k);
            if (!NHabits.due(h, ds) || NHabits.skip(h, ds)) continue;
            if (NHabits.freq(h).equals("times") && !NHabits.done(h, ds)) continue;
            due[w]++; tot++; if (NHabits.done(h, ds)) dn[w]++;
        }
        if (tot < 10 || NHabits.freq(h).equals("times")) return;
        add(stH("By weekday", "last 12 weeks"));
        LinearLayout pn = panel();
        LinearLayout bars = NUi.row(c); bars.setGravity(Gravity.BOTTOM);
        int best = -1, worst = -1; double bv = -1, wv = 2;
        for (int i = 0; i < 7; i++) {
            Double r = due[i] > 0 ? dn[i] / (double) due[i] : null;
            if (r != null) { if (r > bv) { bv = r; best = i; } if (r < wv) { wv = r; worst = i; } }
            LinearLayout cc = NUi.col(c); cc.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
            View bar = new View(c); bar.setBackground(NUi.round(r != null && r > 0 ? col : NTheme.line2, 7, 0)); if (r == null) bar.setAlpha(.3f);
            int bh = r == null ? NUi.dp(4) : Math.max(NUi.dp(6), (int) Math.round(NUi.dp(72) * r));
            cc.addView(bar, NUi.lp(NUi.dp(24), bh));
            TextView lb = NBits.meta(c, NDates.DAYS[i].substring(0, 2), NTheme.muted); lb.setTextSize(10); lb.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams ll = NUi.lp(-1, -2); ll.topMargin = NUi.dp(6); cc.addView(lb, ll);
            LinearLayout.LayoutParams cl = NUi.lpw(0, NUi.dp(96), 1); if (i < 6) cl.rightMargin = NUi.dp(8); bars.addView(cc, cl);
        }
        pn.addView(bars);
        String msg = best >= 0 && worst >= 0 && bv - wv >= .2 ? "Strongest on " + NDates.DAYS[best] + " (" + Math.round(bv * 100) + "%). " + NDates.DAYS[worst] + " is the weak spot (" + Math.round(wv * 100) + "%). Plan something specific for that day." : "Steady across the week.";
        pn.addView(NUi.text(c, msg, 13.5f, NTheme.muted), NUi.mt(12));
        add(pn);
    }

    void slipList(JSONObject h) {
        JSONArray sl = h.optJSONArray("slips"); List<JSONObject> l = new ArrayList<>(); long start = h.optLong("start");
        if (sl != null) for (int i = 0; i < sl.length(); i++) { JSONObject s = sl.optJSONObject(i); if (s != null && s.optLong("t") >= start) l.add(s); }
        java.util.Collections.sort(l, new java.util.Comparator<JSONObject>() { public int compare(JSONObject a, JSONObject b) { return Long.compare(b.optLong("t"), a.optLong("t")); } });
        add(stH("Slips", String.valueOf(l.size())));
        if (l.isEmpty()) { add(NUi.text(c, "None in this run. If one happens, log it honestly. Patterns in your slips show you where to plan ahead.", 13.5f, NTheme.muted)); return; }
        LinearLayout box = NBits.listBox(c); box.setPadding(0, 0, 0, 0);
        for (int i = 0; i < Math.min(12, l.size()); i++) {
            JSONObject s = l.get(i); if (i > 0) box.addView(NBits.dividerFull(c));
            String sub = s.optString("trig") + (!s.optString("trig").isEmpty() && !s.optString("note").isEmpty() ? " · " : "") + s.optString("note");
            View lead = NUi.icon(c, "reset", 18, NTheme.muted); lead.setLayoutParams(NUi.lp(NUi.dp(30), NUi.dp(18)));
            box.addView(NBits.row(c, lead, NDates.dayLabel(s.optLong("t")) + " · " + NDates.fmtClock(s.optLong("t")), sub.isEmpty() ? "No note" : sub, NTheme.muted, null, false));
        }
        add(box);
        /* what sets it off */
        if (l.size() >= 2) {
            java.util.Map<String, Integer> tc = new java.util.LinkedHashMap<>(), pc = new java.util.LinkedHashMap<>();
            for (JSONObject s : l) {
                String k = s.optString("trig"); if (!k.isEmpty()) { Integer o = tc.get(k); tc.put(k, o == null ? 1 : o + 1); }
                java.util.Calendar cal = java.util.Calendar.getInstance(); cal.setTimeInMillis(s.optLong("t")); int hh = cal.get(java.util.Calendar.HOUR_OF_DAY);
                String p = hh < 5 ? "late at night" : hh < 12 ? "in the morning" : hh < 17 ? "in the afternoon" : hh < 21 ? "in the evening" : "late at night";
                Integer o = pc.get(p); pc.put(p, o == null ? 1 : o + 1);
            }
            String top = null; int tv = 0; for (java.util.Map.Entry<String, Integer> e : tc.entrySet()) if (e.getValue() > tv) { tv = e.getValue(); top = e.getKey(); }
            String tp = null; int pv = 0; for (java.util.Map.Entry<String, Integer> e : pc.entrySet()) if (e.getValue() > pv) { pv = e.getValue(); tp = e.getKey(); }
            String msg = (top != null ? "Most slips come from " + top.toLowerCase() : "Slips") + (tp != null && pv > 1 ? " and happen most " + tp : "") + ". " + (h.optString("plan").isEmpty() ? "Write a plan for those moments in Edit." : "Keep your plan ready for exactly those moments.");
            add(NUi.text(c, msg, 14, NTheme.text), 12);
        }
    }

    int bestRun(JSONObject h) {
        long start = h.optLong("start", h.optLong("createdAt"));
        java.util.List<Long> ts = new java.util.ArrayList<>(); ts.add(start);
        JSONArray sl = h.optJSONArray("slips");
        if (sl != null) for (int i = 0; i < sl.length(); i++) { long t = sl.optJSONObject(i).optLong("t"); if (t >= start) ts.add(t); }
        java.util.Collections.sort(ts);
        long b = 0;
        for (int i = 0; i < ts.size(); i++) { long e = i + 1 < ts.size() ? ts.get(i + 1) : System.currentTimeMillis(); b = Math.max(b, e - ts.get(i)); }
        return (int) (b / 86400000L);
    }

    void more(final JSONObject h) {
        NForms F = new NForms(sh);
        LinearLayout b = F.sheetBody(h.optString("title"), "Habit options", null);
        final boolean paused = "paused".equals(NHabits.status(h));
        String[][] items = {{paused ? "▶️" : "⏸️", paused ? "Resume" : "Pause", "Keeps the streak safe while it's paused"}, {"🗄️", "archived".equals(NHabits.status(h)) ? "Unarchive" : "Archive", "Hide it without losing its history"}, {"🗑️", "Delete", "Removes the habit and its history"}};
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
                    if (k == 0) { h.put("status", paused ? "active" : "paused"); if (!paused) { JSONObject p = new JSONObject(); p.put("a", NDates.ymd()); NHabits.arr(h, "pz").put(p); } else { JSONArray pz = NHabits.arr(h, "pz"); for (int j = 0; j < pz.length(); j++) { JSONObject p = pz.optJSONObject(j); if (p != null && p.optString("b").isEmpty()) p.put("b", NDates.fromN(NDates.today() - 1)); } } }
                    else if (k == 1) h.put("status", "archived".equals(NHabits.status(h)) ? "active" : "archived");
                    else if (k == 2) { confirmDelete(h); return; }
                    else { sh.closeSheet(); sh.openClassic("habit/" + h.optString("id")); return; }
                } catch (Exception ignored) { }
                sh.closeSheet(); sh.save();
            } });
            b.addView(r, NUi.mt(i == 0 ? 16 : 8));
        }
        String rt = "habit/" + h.optString("id"); JSONObject hd = NMore.d("id", h.optString("id"), "k", "habit"), hi = NMore.d("id", h.optString("id"));
        NMore.add(b, NMore.row(c, "⏰", "Reminders & bells", "When Plotline nudges you", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); final String hid0 = h.optString("id"); v.postDelayed(new Runnable() { public void run() { NReminders.open(sh, "habit", hid0); } }, 280); } }));
        NMore.add(b, NMore.row(c, "🤖", "Automatic check-off", "Tick it when your phone sees it happen", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); final String hid1 = h.optString("id"); v.postDelayed(new Runnable() { public void run() { NAutoEdit.open(sh, "habit", hid1, null); } }, 280); } }));
        NMore.add(b, NMore.row(c, "🧵", "Threads about this habit", "Start or open a thread", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); final String hid2 = h.optString("id"); v.postDelayed(new Runnable() { public void run() { new NForms(sh).threadsFor("habit", hid2); } }, 280); } }));
        NMore.add(b, NMore.row(c, "🤝", "Share or do it together…", "Invite someone, see cheers and replies", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); NSheets.share(sh, "habit", h.optString("id"), null); } }));
        if (NHabits.routine(h) && NHabits.steps(h).length() > 0) NMore.add(b, NMore.row(c, "▶️", "Start routine", "Step by step with a timer", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); final String hid3 = h.optString("id"); v.postDelayed(new Runnable() { public void run() { NUrge.routine(sh, hid3); } }, 300); } }));
        if (NHabits.kind(h).equals("quit")) NMore.add(b, NMore.row(c, "🌊", "I have an urge", "Ride it out with a 5-minute timer", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); v.postDelayed(new Runnable() { public void run() { NUrge.urge(sh, h); } }, 300); } }));
        NMore.add(b, NMore.row(c, "📋", "Duplicate", "A copy with a clean history", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); sh.run("hDup", NMore.d("id", h.optString("id"))); } }));
        NMore.add(b, NMore.row(c, "💬", "Ask AI about this habit", "How am I doing?", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); sh.ask("How is my habit “" + h.optString("title") + "” going? When do I keep it, when do I slip, and how can I make it easier?"); } }));
        sh.sheet(b);
    }

    void confirmDelete(final JSONObject h) {
        NForms F = new NForms(sh);
        LinearLayout b = F.sheetBody(h.optString("title"), "Delete this habit?", "Its history goes too, on every device. This can't be undone.");
        b.addView(NForms.actions(c, NUi.btn(c, "Cancel", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } }),
            NUi.btn(c, "Delete", true, new View.OnClickListener() { public void onClick(View v) { st.remove("habits", h.optString("id")); sh.closeSheet(); sh.save(); sh.pop(); NShell.toast("Habit deleted"); } })));
        sh.sheet(b);
    }
}
