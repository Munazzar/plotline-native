package com.munazzar.plotline;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

/* Shared rows and cards used across the native screens. */
final class NBits {
    /* round check button (day goals, steps) that springs when ticked */
    static FrameLayout check(Context c, boolean on, int color, final View.OnClickListener l) {
        final FrameLayout f = new FrameLayout(c);
        f.setBackground(on ? NUi.oval(color, 0, 0) : NUi.oval(0, NTheme.line2, 2));
        ImageView i = new ImageView(c); i.setImageDrawable(new NIcon("check", NTheme.on(color)).stroke(2.6f)); i.setVisibility(on ? View.VISIBLE : View.INVISIBLE);
        f.addView(i, new FrameLayout.LayoutParams(NUi.dp(16), NUi.dp(16), Gravity.CENTER));
        f.setLayoutParams(new LinearLayout.LayoutParams(NUi.dp(30), NUi.dp(30)));
        NUi.tap(f, new View.OnClickListener() { public void onClick(View v) {
            NUi.haptic(v);
            v.animate().scaleX(1.18f).scaleY(1.18f).setDuration(120).withEndAction(new Runnable() { public void run() { f.animate().scaleX(1f).scaleY(1f).setDuration(260).setInterpolator(NUi.SPRING).start(); } }).start();
            l.onClick(v);
        } });
        return f;
    }

    /* small round icon button for list rows */
    static FrameLayout sbtn(Context c, String iconName, View.OnClickListener l) {
        FrameLayout f = new NUi.Fix(c, NUi.dp(36), NUi.dp(36));
        f.setBackground(NUi.ripple(NUi.round(NTheme.surface, 12, NTheme.line), 12));
        ImageView i = new ImageView(c); i.setImageDrawable(new NIcon(iconName, NTheme.text));
        f.addView(i, new FrameLayout.LayoutParams(NUi.dp(17), NUi.dp(17), Gravity.CENTER));
        f.setLayoutParams(new LinearLayout.LayoutParams(NUi.dp(36), NUi.dp(36)));
        f.setContentDescription(iconName);
        NUi.tap(f, l);
        return f;
    }

    /* small stat chip: emoji, value, label */
    static View chipStat(Context c, String emoji, String value, String label) {
        LinearLayout t = NUi.col(c); t.setGravity(Gravity.CENTER_HORIZONTAL);
        t.setBackground(NUi.card(18)); t.setPadding(NUi.dp(14), NUi.dp(10), NUi.dp(14), NUi.dp(10));
        t.addView(NUi.text(c, emoji, 18, NTheme.text));
        TextView v = NUi.body(c, value, 16, NTheme.text, 800); t.addView(v);
        t.addView(NUi.label(c, label, NTheme.muted));
        return t;
    }

    static LinearLayout listBox(Context c) {
        LinearLayout l = NUi.col(c);
        l.setBackground(NUi.card(22));
        l.setPadding(0, NUi.dp(4), 0, NUi.dp(4));
        return l;
    }

    static View dividerFull(Context c) { View v = new View(c); v.setBackgroundColor(NTheme.line); v.setLayoutParams(new LinearLayout.LayoutParams(-1, Math.max(1, NUi.dp(1)))); return v; }

    interface Pick { void on(String key); }

    /* the web's segmented control: one rounded tray, the chosen item raised */
    static View seg(Context c, String[][] items, String sel, final Pick cb) {
        android.widget.HorizontalScrollView hs = new android.widget.HorizontalScrollView(c);
        hs.setHorizontalScrollBarEnabled(false); hs.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout tray = NUi.row(c);
        tray.setBackground(NUi.round(NTheme.surface, 15, NTheme.line));
        tray.setPadding(NUi.dp(4), NUi.dp(4), NUi.dp(4), NUi.dp(4));
        for (final String[] it : items) {
            boolean on = it[0].equals(sel);
            TextView t = NUi.body(c, it[1], 13.5f, on ? NTheme.text : NTheme.muted, 600);
            t.setGravity(Gravity.CENTER); t.setSingleLine(true);
            int px = NUi.narrow ? 9 : 15; t.setPadding(NUi.dp(px + 0), NUi.dp(9), NUi.dp(px + 0), NUi.dp(9));
            if (on) t.setBackground(NUi.round(NTheme.surface2, 11, NTheme.line2));
            NUi.tap(t, new View.OnClickListener() { public void onClick(View v) { cb.on(it[0]); } });
            tray.addView(t);
        }
        hs.addView(tray);
        return hs;
    }

    /* two faces that turn over on tap */
    static FrameLayout flipWrap(Context c, final View front, final View back) {
        final FrameLayout w = new FrameLayout(c);
        back.setVisibility(View.GONE);
        w.addView(front, new FrameLayout.LayoutParams(-1, -1)); w.addView(back, new FrameLayout.LayoutParams(-1, -1));
        w.setCameraDistance(NUi.density * 9000);
        final boolean[] fl = {false};
        View.OnClickListener go = new View.OnClickListener() { public void onClick(View v) {
            if (w.getRotationY() != 0) return;
            w.animate().rotationY(90).setDuration(170).withEndAction(new Runnable() { public void run() {
                fl[0] = !fl[0]; front.setVisibility(fl[0] ? View.GONE : View.VISIBLE); back.setVisibility(fl[0] ? View.VISIBLE : View.GONE);
                w.setRotationY(-90); w.animate().rotationY(0).setDuration(200).start();
            } }).start();
        } };
        front.setOnClickListener(go); back.setOnClickListener(go);
        return w;
    }

    /* the web's icon-only segmented control */
    static View iconSeg(Context c, String[][] items, String sel, final Pick cb) {
        LinearLayout tray = NUi.row(c);
        tray.setBackground(NUi.round(NTheme.surface, 15, NTheme.line));
        tray.setPadding(NUi.dp(4), NUi.dp(4), NUi.dp(4), NUi.dp(4));
        for (final String[] it : items) {
            boolean on = it[0].equals(sel);
            FrameLayout f = new FrameLayout(c);
            if (on) f.setBackground(NUi.round(NTheme.surface2, 11, NTheme.line2));
            f.addView(NUi.icon(c, it[1], 18, on ? NTheme.text : NTheme.muted), new FrameLayout.LayoutParams(NUi.dp(18), NUi.dp(18), Gravity.CENTER));
            f.setContentDescription(it[1]);
            NUi.tap(f, new View.OnClickListener() { public void onClick(View v) { cb.on(it[0]); } });
            tray.addView(f, new LinearLayout.LayoutParams(NUi.dp(38), NUi.dp(38)));
        }
        return tray;
    }

    /* a goal card that flips to its next step, due date and Open / pin */
    static View flipCard(final NShell sh, final JSONObject g, int num) {
        final Context c = sh.a;
        final FrameLayout wrap = new FrameLayout(c);
        final View front = goalCard(sh, g, num);
        int col = NTheme.areaCol(g.optString("area"));
        final LinearLayout back = NUi.col(c);
        back.setBackground(NUi.round(NTheme.surface2, 22, NUi.mix(col, .5f, NTheme.line)));
        back.setPadding(NUi.dp(15), NUi.dp(15), NUi.dp(15), NUi.dp(15));
        LinearLayout bt = NUi.row(c);
        View dot = new View(c); dot.setBackground(NUi.oval(col, 0, 0)); LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(NUi.dp(10), NUi.dp(10)); dl.rightMargin = NUi.dp(8); bt.addView(dot, dl);
        String tt = g.optString("title"); bt.addView(NUi.ell(NUi.body(c, tt.length() > 48 ? tt.substring(0, 47) + "…" : tt, 15, NTheme.text, 700), 2), NUi.lpw(0, -2, 1));
        back.addView(bt);
        JSONObject nx = NActs.nextStep(g); boolean done = "done".equals(g.optString("status"));
        TextView k = meta(c, "NEXT STEP", NTheme.muted); k.setTextSize(10.5f); back.addView(k, NUi.mt(8));
        back.addView(NUi.ell(NUi.text(c, nx != null ? nx.optString("title") : done ? "Everything done" : "No steps yet", 14, NTheme.text), 2), NUi.mt(3));
        LinearLayout kv = NUi.row(c); kv.setPadding(0, NUi.dp(8), 0, 0);
        kv.setBackground(new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{new android.graphics.drawable.ColorDrawable(NTheme.line)}) { { setLayerHeight(0, Math.max(1, NUi.dp(1))); setLayerGravity(0, Gravity.TOP); } });
        kv.addView(NUi.text(c, "Due", 13, NTheme.muted), NUi.lpw(0, -2, 1));
        boolean hasDue = nx != null && NDates.valid(nx.optString("due"));
        String dueS = hasDue ? NDates.dueShort(nx.optString("due")) : "—";
        int du = hasDue ? NDates.daysUntil(nx.optString("due")) : 99;
        kv.addView(NUi.text(c, dueS, 13, du < 0 ? NTheme.LATE : du <= 1 ? NTheme.accent : NTheme.text));
        back.addView(kv, NUi.mt(8));
        View sp = new View(c); back.addView(sp, NUi.lpw(-1, 0, 1));
        LinearLayout act = NUi.row(c);
        TextView open = NUi.btn(c, "Open", true, new View.OnClickListener() { public void onClick(View v) { sh.push(new NGoalScreen(sh, g.optString("id"))); } });
        open.setMinHeight(NUi.dp(38)); act.addView(open, NUi.lpw(0, NUi.dp(38), 1));
        LinearLayout.LayoutParams pl = NUi.lp(-2, -2); pl.leftMargin = NUi.dp(8);
        View pin = NUi.ibtn(c, "pin", g.optBoolean("pinned"), new View.OnClickListener() { public void onClick(View v) { try { g.put("pinned", !g.optBoolean("pinned")); } catch (Exception ignored) { } sh.save(); } });
        ((NUi.Fix) pin).w = NUi.dp(38); ((NUi.Fix) pin).h = NUi.dp(38);
        act.addView(pin, pl);
        back.addView(act);
        back.setVisibility(View.GONE);
        wrap.addView(front, new FrameLayout.LayoutParams(-1, -1)); wrap.addView(back, new FrameLayout.LayoutParams(-1, -1));
        wrap.setCameraDistance(NUi.density * 9000);
        final boolean[] flipped = {false};
        View.OnClickListener flip = new View.OnClickListener() { public void onClick(View v) {
            if (wrap.getRotationY() != 0) return;
            wrap.animate().rotationY(90).setDuration(170).withEndAction(new Runnable() { public void run() {
                flipped[0] = !flipped[0]; front.setVisibility(flipped[0] ? View.GONE : View.VISIBLE); back.setVisibility(flipped[0] ? View.VISIBLE : View.GONE);
                wrap.setRotationY(-90); wrap.animate().rotationY(0).setDuration(200).start();
            } }).start();
        } };
        front.setOnClickListener(flip);
        back.setOnClickListener(flip);
        return wrap;
    }

    static View divider(Context c) { return dividerFull(c); }

    /* a list row: leading view, title, subtitle, trailing view */
    static LinearLayout row(Context c, View lead, String title, String sub, int subColor, View trail, boolean struck) {
        LinearLayout r = NUi.row(c);
        r.setPadding(NUi.dp(16), NUi.dp(12), NUi.dp(14), NUi.dp(12));
        r.setMinimumHeight(NUi.dp(60));
        if (lead != null) { LinearLayout.LayoutParams l = (LinearLayout.LayoutParams) lead.getLayoutParams(); if (l == null) l = NUi.lp(-2, -2); l.rightMargin = NUi.dp(14); r.addView(lead, l); }
        LinearLayout tx = NUi.col(c);
        TextView t = NUi.ell(NUi.body(c, title, 16, struck ? NTheme.muted : NTheme.text, 600), 2);
        if (struck) t.setPaintFlags(t.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        tx.addView(t);
        if (sub != null && !sub.isEmpty()) { TextView s = NUi.ell(NUi.text(c, sub, 13, subColor), 2); s.setPadding(0, NUi.dp(2), 0, 0); tx.addView(s); }
        r.addView(tx, NUi.lpw(0, -2, 1));
        if (trail != null) { LinearLayout.LayoutParams l = NUi.lp(-2, -2); l.leftMargin = NUi.dp(10); r.addView(trail, l); }
        r.setBackground(NUi.ripple(new GradientDrawable(), 0));
        return r;
    }

    /* the web's .li row: check/lead, one-line title, a small dotted sub line, trailing bits */
    static LinearLayout li(Context c, View lead, String title, boolean struck, int dot, String sub, int padV, View... trails) {
        LinearLayout r = NUi.row(c);
        r.setPadding(NUi.dp(18), NUi.dp(padV), NUi.dp(18), NUi.dp(padV));
        if (lead != null) { LinearLayout.LayoutParams l = (LinearLayout.LayoutParams) lead.getLayoutParams(); if (l == null) l = NUi.lp(-2, -2); l.rightMargin = NUi.dp(14); r.addView(lead, l); }
        LinearLayout tx = NUi.col(c);
        TextView t = NUi.ell(NUi.body(c, title, 16, struck ? NTheme.muted : NTheme.text, 600), 1);
        if (struck) t.setPaintFlags(t.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        tx.addView(t);
        if (sub != null && !sub.isEmpty() || dot != 0) {
            LinearLayout sr = NUi.row(c);
            if (dot != 0) { View d = new View(c); d.setBackground(NUi.oval(dot, 0, 0)); LinearLayout.LayoutParams dl = NUi.lp(NUi.dp(8), NUi.dp(8)); dl.rightMargin = NUi.dp(7); sr.addView(d, dl); }
            TextView st = NUi.ell(NUi.text(c, sub == null ? "" : sub, 13, NTheme.muted), 1); sr.addView(st);
            tx.addView(sr);
        }
        r.addView(tx, NUi.lpw(0, -2, 1));
        for (View v : trails) { if (v == null) continue; LinearLayout.LayoutParams l = v.getLayoutParams() instanceof LinearLayout.LayoutParams ? (LinearLayout.LayoutParams) v.getLayoutParams() : NUi.lp(-2, -2); l.leftMargin = NUi.dp(14); r.addView(v, l); }
        r.setBackground(NUi.ripple(new GradientDrawable(), 0));
        return r;
    }

    static TextView meta(Context c, String s, int color) { TextView t = NUi.text(c, s, 12, color); t.setTypeface(NFont.mono(500)); return t; }

    /* the habit check ring (tap = +1, or undo when done) */
    static NRing habitRing(Context c, JSONObject h, int sizeDp) {
        NRing r = new NRing(c);
        int v = NHabits.val(h, NDates.ymd()), n = NHabits.target(h);
        r.color = NTheme.areaCol(h.optString("area")); r.check = true; r.strokeDp = 3.5f; r.mono = true;
        r.set(v / (float) n, false);
        if (n > 1 && v < n) r.text(String.valueOf(v), "/" + n);
        r.setLayoutParams(new LinearLayout.LayoutParams(NUi.dp(sizeDp), NUi.dp(sizeDp)));
        return r;
    }

    static String habitSub(JSONObject h) {
        StringBuilder b = new StringBuilder();
        if ("paused".equals(NHabits.status(h))) b.append("Paused");
        int s = NHabits.streak(h);
        if (s > 0) { if (b.length() > 0) b.append(" · "); b.append("🔥 ").append(s).append(NHabits.freq(h).equals("times") ? " wk" : " d"); }
        if (NHabits.freq(h).equals("times")) { if (b.length() > 0) b.append(" · "); b.append(NHabits.weekCount(h, NDates.wkStart(NDates.today()))).append(" of ").append(NHabits.times(h)).append(" this week"); }
        else if (NHabits.freq(h).equals("days") && NHabits.days(h).size() < 7) { if (b.length() > 0) b.append(" · "); b.append(NHabits.freqText(h)); }
        int n = NHabits.target(h);
        if (n > 1 && !NHabits.routine(h)) { if (b.length() > 0) b.append(" · "); b.append(NHabits.val(h, NDates.ymd())).append(" of ").append(n).append(h.optString("unit").isEmpty() ? "" : " " + h.optString("unit")); }
        if (NHabits.routine(h)) { int k = NHabits.steps(h).length(); if (b.length() > 0) b.append(" · "); b.append(k).append(k == 1 ? " step" : " steps"); }
        String tm = NDates.fmtTime(h.optString("time"));
        if (!tm.isEmpty()) { if (b.length() > 0) b.append(" · "); b.append(tm); }
        return b.length() == 0 ? NHabits.freqText(h) : b.toString();
    }

    /* the last 7 days as tappable dots (same states as the web's .hd) */
    static LinearLayout week(final Context c, final JSONObject h, final Runnable onChange) {
        LinearLayout r = new LinearLayout(c) { @Override protected void onMeasure(int w, int hh) { super.onMeasure(MeasureSpec.makeMeasureSpec(Math.min(MeasureSpec.getSize(w), NUi.dp(178)), MeasureSpec.EXACTLY), hh); } };
        r.setOrientation(LinearLayout.HORIZONTAL);
        int tn = NDates.today(), col = NTheme.areaCol(h.optString("area"));
        String sd = NHabits.startDate(h);
        for (int k = tn - 6; k <= tn; k++) {
            final String ds = NDates.fromN(k);
            int v = NHabits.val(h, ds), n = NHabits.target(h);
            char st;
            if (ds.compareTo(sd) < 0) st = 'n'; else if (NHabits.skip(h, ds)) st = 's'; else if (v >= n) st = 'd'; else if (v > 0) st = 'p';
            else if (!NHabits.due(h, ds)) st = 'n'; else if (k == tn) st = 't'; else st = NHabits.freq(h).equals("times") ? 'n' : 'x';
            LinearLayout cell = NUi.col(c); cell.setGravity(Gravity.CENTER_HORIZONTAL);
            View dot = new View(c); int sz = 12;
            switch (st) {
                case 'd': dot.setBackground(NUi.oval(col, 0, 0)); break;
                case 'p': dot.setBackground(NUi.oval(0, NTheme.alpha(col, .7f), 3)); break;
                case 'x': dot.setBackground(NUi.oval(0, NTheme.alpha(0xFFD9534F, .55f), 1.5f)); break;
                case 's': dot.setBackground(NUi.oval(NTheme.alpha(NTheme.line2, .55f), NTheme.line2, 1)); break;
                case 'n': dot.setBackground(NUi.oval(NTheme.line2, 0, 0)); sz = 6; break;
                default: dot.setBackground(NUi.oval(0, col, 1.5f)); break;
            }
            FrameLayout box = new FrameLayout(c);
            box.addView(dot, new FrameLayout.LayoutParams(NUi.dp(sz), NUi.dp(sz), Gravity.CENTER));
            cell.addView(box, new LinearLayout.LayoutParams(NUi.dp(12), NUi.dp(12)));
            TextView l = NUi.text(c, NDates.DAYS[NDates.dow(k)].substring(0, 1), 8.5f, st == 't' ? NTheme.text : NTheme.muted); l.setTypeface(NFont.mono(500)); l.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams ll = NUi.lp(-2, -2); ll.topMargin = NUi.dp(3); cell.addView(l, ll);
            cell.setPadding(0, NUi.dp(2), 0, NUi.dp(2));
            if (!NHabits.kind(h).equals("quit")) NUi.tap(cell, new View.OnClickListener() { public void onClick(View v) { NUi.haptic(v); NActs.habitDay(h, ds); onChange.run(); } });
            LinearLayout.LayoutParams cl = NUi.lpw(0, -2, 1); if (k < tn) cl.rightMargin = NUi.dp(4);
            r.addView(cell, cl);
        }
        r.setGravity(Gravity.TOP);
        return r;
    }

    static View empty(Context c, String title, String sub) {
        LinearLayout l = NUi.col(c);
        l.setGravity(Gravity.CENTER_HORIZONTAL);
        l.setBackground(NUi.round(0, 22, NTheme.line2));
        l.setPadding(NUi.dp(20), NUi.dp(26), NUi.dp(20), NUi.dp(26));
        TextView t = NUi.body(c, title, 16, NTheme.text, 700); t.setGravity(Gravity.CENTER); l.addView(t);
        if (sub != null) { TextView s = NUi.text(c, sub, 14, NTheme.muted); s.setGravity(Gravity.CENTER); s.setPadding(0, NUi.dp(6), 0, 0); l.addView(s); }
        return l;
    }

    /* colourful goal card (grid tile) */
    static String hzShort(String h) {
        switch (h) { case "week": return "Week"; case "month": return "Month"; case "quarter": return "3 mo"; case "year": return "Year"; case "multi": return "1–5 yr"; case "life": return "Life"; default: return "Month"; }
    }

    static View goalCard(final NShell sh, final JSONObject g, int num) {
        boolean im = NTheme.pushImg(g);
        try { return goalCard0(sh, g, num); } finally { NTheme.popImg(im); }
    }

    static View goalCard0(final NShell sh, final JSONObject g, int num) {
        Context c = sh.a;
        int col = NTheme.areaCol(g.optString("area")), ink = NTheme.INK;
        boolean done = "done".equals(g.optString("status"));
        LinearLayout card = NUi.col(c);
        card.setBackground(NUi.ripple(done ? NUi.round(NUi.mix(col, .6f, NTheme.bg), 22, 0) : NCard.bgFor(g, col, 22), 22));
        card.setPadding(NUi.dp(15), NUi.dp(15), NUi.dp(15), NUi.dp(15));
        LinearLayout top = NUi.row(c);
        TextView area = NUi.text(c, (NTheme.areaName(g.optString("area")) + " · " + hzShort(g.optString("horizon", "month"))).toUpperCase(), 10.5f, NTheme.INK_MUTED);
        area.setTypeface(NFont.mono(500)); area.setLetterSpacing(.05f); area.setSingleLine(true); area.setEllipsize(android.text.TextUtils.TruncateAt.END);
        top.addView(area, NUi.lpw(0, -2, 1));
        if (g.optBoolean("pinned")) { LinearLayout.LayoutParams pl = NUi.lp(NUi.dp(16), NUi.dp(16)); pl.leftMargin = NUi.dp(8); top.addView(NUi.icon(c, "pin", 16, ink), pl); }
        TextView n = NUi.text(c, String.format(java.util.Locale.US, "%02d", num), 22, ink); n.setTypeface(NFont.display(800)); n.setGravity(Gravity.CENTER); n.setIncludeFontPadding(false);
        n.setBackground(NUi.round(0, 14, NTheme.alpha(ink, .24f))); n.setMinWidth(NUi.dp(42)); n.setMinimumHeight(NUi.dp(42)); n.setPadding(NUi.dp(9), 0, NUi.dp(9), 0);
        LinearLayout.LayoutParams nl = NUi.lp(-2, NUi.dp(42)); nl.leftMargin = NUi.dp(8); top.addView(n, nl);
        card.addView(top);
        String par = g.optString("parent");
        if (!par.isEmpty()) { JSONObject pg = sh.st.find("goals", par); if (pg != null) { TextView pt = NUi.text(c, "↳ " + pg.optString("title"), 10.5f, NTheme.INK_MUTED); pt.setTypeface(NFont.mono(500)); pt.setAllCaps(true); pt.setSingleLine(true); pt.setEllipsize(android.text.TextUtils.TruncateAt.END); card.addView(pt, NUi.mt(10)); } }
        View sp = new View(c); card.addView(sp, NUi.lpw(-1, 0, 1));
        TextView t = NUi.ell(NUi.text(c, g.optString("title").toUpperCase(), 24, ink), 4); t.setTypeface(NFont.display(800)); t.setLineSpacing(0, .92f); t.setIncludeFontPadding(false);
        LinearLayout.LayoutParams tl = NUi.mt(0); tl.bottomMargin = NUi.dp(14); card.addView(t, tl);
        int p = NActs.pct(sh.st, g);
        card.addView(bar(c, p / 100f, ink), NUi.lp(-1, NUi.dp(4)));
        JSONArray s = g.optJSONArray("steps"); int ns = s == null ? 0 : s.length();
        int kids = 0; for (JSONObject x : NStore.list(sh.st.arr("goals"))) if (g.optString("id").equals(x.optString("parent"))) kids++;
        String ptxt = done ? "Achieved" : p + "% · " + (kids > 0 ? kids + " sub-goal" + (kids > 1 ? "s" : "") + (ns > 0 ? " · " : "") : "") + (ns > 0 || kids == 0 ? NActs.doneSteps(g) + " of " + ns : "");
        TextView m = NUi.text(c, ptxt.toUpperCase(), 10.5f, NTheme.INK_MUTED); m.setTypeface(NFont.mono(500)); m.setLetterSpacing(.05f);
        card.addView(m, NUi.mt(8));
        NUi.tap(card, new View.OnClickListener() { public void onClick(View v) { sh.push(new NGoalScreen(sh, g.optString("id"))); } });
        return card;
    }

    static View bar(Context c, final float f, final int color) { return bar(c, f, color, 4); }

    static View bar(Context c, final float f, final int color, final int hDp) {
        FrameLayout b = new FrameLayout(c) { @Override protected void onMeasure(int w, int h) { super.onMeasure(w, MeasureSpec.makeMeasureSpec(NUi.dp(hDp), MeasureSpec.EXACTLY)); } };
        b.setBackground(NUi.round(NTheme.alpha(color, .22f), hDp / 2f, 0));
        final View fill = new View(c); fill.setBackground(NUi.round(color, hDp / 2f, 0));
        b.addView(fill, new FrameLayout.LayoutParams(0, NUi.dp(hDp)));
        b.setLayoutParams(new LinearLayout.LayoutParams(-1, NUi.dp(hDp)));
        b.addOnLayoutChangeListener(new View.OnLayoutChangeListener() { public void onLayoutChange(View v, int l, int t, int r, int bt, int ol, int ot, int or, int ob) {
            int w = Math.round((r - l) * Math.max(0, Math.min(1, f)));
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) fill.getLayoutParams();
            int want = f > 0 ? Math.max(NUi.dp(hDp), w) : 0;
            if (lp.width != want || lp.height != NUi.dp(hDp)) { lp.width = want; lp.height = NUi.dp(hDp); fill.setLayoutParams(lp); }
        } });
        return b;
    }

    /* horizontally scrolling row */
    static android.widget.HorizontalScrollView hscroll(Context c, LinearLayout inner) {
        android.widget.HorizontalScrollView hs = new android.widget.HorizontalScrollView(c);
        hs.setHorizontalScrollBarEnabled(false); hs.setClipToPadding(false); hs.setOverScrollMode(View.OVER_SCROLL_NEVER);
        hs.addView(inner);
        return hs;
    }

    /* .btn.ink.sm / .btn.inkline.sm on a tinted card: 38 high, radius 12, optional icon */
    static View inkSm(Context c, String icon, String label, boolean fill, View.OnClickListener l) {
        int fg = fill ? 0xFFF4F2EC : NTheme.INK;
        LinearLayout b = NUi.row(c); b.setGravity(Gravity.CENTER); b.setPadding(NUi.dp(14), 0, NUi.dp(14), 0); b.setMinimumHeight(NUi.dp(38));
        b.setBackground(NUi.ripple(fill ? NUi.round(NTheme.INK, 12, 0) : NUi.round(0, 12, NTheme.alpha(NTheme.INK, .28f)), 12));
        if (icon != null) { LinearLayout.LayoutParams il = NUi.lp(NUi.dp(16), NUi.dp(16)); il.rightMargin = NUi.dp(8); b.addView(NUi.icon(c, icon, 16, fg), il); }
        b.addView(NUi.body(c, label, 13.5f, fg, 600));
        NUi.tap(b, l);
        b.setLayoutParams(new LinearLayout.LayoutParams(-2, NUi.dp(38)));
        return b;
    }
}
