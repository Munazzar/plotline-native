package com.munazzar.plotline;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/* The goal's steps the way the web shows them: a row of big flip cards (Cards) or a zig-zag spine (Path). */
final class NGoalSteps {
    final NShell sh; final Context c; final NStore st; final JSONObject g; final NForms F; final String route;
    final int col; final int INK, INKM;

    NGoalSteps(NShell sh, JSONObject g) {
        this.sh = sh; this.c = sh.a; this.st = sh.st; this.g = g; this.F = new NForms(sh); this.route = "goal/" + g.optString("id");
        col = NTheme.areaCol(g.optString("area")); INK = NTheme.hasImg(g) ? 0xFFFFFFFF : NTheme.INK; INKM = NTheme.hasImg(g) ? 0xCCFFFFFF : NTheme.INK_MUTED;
    }

    static String remLabel(JSONObject o) {
        String r = o.opt("remind") == null || o.isNull("remind") ? "" : String.valueOf(o.opt("remind"));
        if (r.isEmpty()) return "Off";
        if (r.equals("m")) return "Morning of";
        if (r.equals("c")) return NDates.valid(o.optString("remDate")) && !o.optString("remTime").isEmpty() ? NDates.fmtDate(o.optString("remDate")) + " · " + NDates.fmtTime(o.optString("remTime")) : "Custom";
        if (r.equals("0")) return "At the time";
        String m = r.equals("5") ? "5 min" : r.equals("15") ? "15 min" : r.equals("30") ? "30 min" : r.equals("60") ? "1 hour" : r.equals("120") ? "2 hours" : r.equals("1440") ? "1 day" : r.equals("10080") ? "1 week" : r + " min";
        return m + " before";
    }

    List<JSONObject[]> subs() { List<JSONObject[]> l = new ArrayList<>(); walk(g, 1, l); return l; }
    void walk(JSONObject p, int d, List<JSONObject[]> out) { for (JSONObject k : NActs.kids(st, p)) { out.add(new JSONObject[]{k, new JSONObject()}); walk(k, d + 1, out); } }

    String status(JSONObject s, JSONObject next) {
        if (s.optBoolean("done")) return "Done";
        String d = s.optString("due");
        if (NDates.valid(d) && NDates.daysUntil(d) < 0) return "Late";
        return s == next ? "Up next" : "Planned";
    }
    String dueLine(JSONObject s) {
        String r = s.optBoolean("done") ? "Done " + (s.optLong("doneAt") > 0 ? NDates.fmtDate(NDates.ymd(s.optLong("doneAt"))) : "") : NDates.dueText(s.optString("due"), s.optString("time"));
        if (r.isEmpty() && !s.optBoolean("done")) r = "No date";
        if (!s.optString("repeat").isEmpty()) r += " · ↻ " + s.optString("repeat");
        return r;
    }

    /* ---------- little builders ---------- */
    TextView mono(String s, float sp, int color) { TextView t = NUi.text(c, s.toUpperCase(), sp, color); t.setTypeface(NFont.mono(500)); t.setLetterSpacing(.05f); return t; }
    TextView badge(String s, int size) {
        TextView n = NUi.text(c, s, size == 0 ? 22 : 14, INK); n.setTypeface(NFont.display(800)); n.setGravity(Gravity.CENTER); n.setIncludeFontPadding(false);
        n.setBackground(NUi.round(0, size == 0 ? 14 : 10, NTheme.alpha(INK, .24f))); n.setMinWidth(NUi.dp(size == 0 ? 42 : 30)); n.setMinimumHeight(NUi.dp(size == 0 ? 42 : 30)); n.setPadding(NUi.dp(8), 0, NUi.dp(8), 0);
        return n;
    }
    View kv(String k, String v, boolean first, int ink, int muted) {
        LinearLayout r = NUi.row(c); r.setPadding(0, NUi.dp(first ? 0 : 7), 0, NUi.dp(7));
        if (!first) { r.setBackground(null); }
        r.addView(NUi.text(c, k, 13, muted), NUi.lpw(0, -2, 1));
        r.addView(NUi.ell(NUi.body(c, v, 13, ink, 600), 1));
        return r;
    }
    TextView inkBtn(String label, boolean filled, View.OnClickListener l) {
        TextView t = NUi.body(c, label, 14, filled ? 0xFFF4F2EC : INK, 600); t.setGravity(Gravity.CENTER); t.setPadding(NUi.dp(12), 0, NUi.dp(12), 0);
        t.setBackground(NUi.ripple(filled ? NUi.round(INK, 12, 0) : NUi.round(0, 12, NTheme.alpha(INK, .35f)), 12));
        NUi.tap(t, l); return t;
    }
    FrameLayout flip(final View front, final View back) {
        final FrameLayout w = new FrameLayout(c);
        back.setVisibility(View.GONE);
        w.addView(front, new FrameLayout.LayoutParams(-1, -1)); w.addView(back, new FrameLayout.LayoutParams(-1, -1));
        w.setCameraDistance(NUi.density * 9000);
        final boolean[] fl = {false};
        final Runnable go = new Runnable() { public void run() {
            if (w.getRotationY() != 0) return;
            w.animate().rotationY(90).setDuration(170).withEndAction(new Runnable() { public void run() {
                fl[0] = !fl[0]; front.setVisibility(fl[0] ? View.GONE : View.VISIBLE); back.setVisibility(fl[0] ? View.VISIBLE : View.GONE);
                w.setRotationY(-90); w.animate().rotationY(0).setDuration(200).start();
            } }).start();
        } };
        front.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { go.run(); } });
        back.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { go.run(); } });
        return w;
    }
    View node(boolean done, boolean sub, float prog, View.OnClickListener l) {
        FrameLayout f = new NUi.Fix(c, NUi.dp(34), NUi.dp(34));
        f.setBackground(NUi.oval(NUi.mix(col, .22f, NTheme.bg), NTheme.alpha(col, .75f), 2));
        if (done) f.addView(NUi.icon(c, "check", 16, INK), new FrameLayout.LayoutParams(NUi.dp(16), NUi.dp(16), Gravity.CENTER));
        else if (sub) { View d = new View(c); d.setBackground(NUi.oval(col, 0, 0)); int s = Math.max(NUi.dp(6), Math.round(NUi.dp(18) * prog)); f.addView(d, new FrameLayout.LayoutParams(s, s, Gravity.CENTER)); }
        f.setLayoutParams(new LinearLayout.LayoutParams(NUi.dp(34), NUi.dp(34)));
        if (l != null) NUi.tap(f, l);
        return f;
    }
    View.OnClickListener toggle(final JSONObject s) { return new View.OnClickListener() { public void onClick(View v) { String m = NActs.toggleStep(st, g, s); sh.save(); if (m != null) NShell.toast(m); } }; }

    /* ---------- Cards: big flip cards in a row ---------- */
    View cards() {
        final JSONArray steps = g.optJSONArray("steps"); final int N = steps == null ? 0 : steps.length();
        final JSONObject next = NActs.nextStep(g);
        final int wDp = Math.min(Math.round(sh.a.getResources().getConfiguration().screenWidthDp * .76f), 344);
        final int itemW = NUi.dp(wDp), cardH = Math.round(itemW * 1.25f), gap = NUi.dp(18), gutter = NUi.dp(sh.wide() ? 32 : 16);
        final HorizontalScrollView hs = new HorizontalScrollView(c); hs.setHorizontalScrollBarEnabled(false); hs.setClipToPadding(false);
        hs.setOverScrollMode(View.OVER_SCROLL_NEVER);
        final int total0 = N;
        LinearLayout row = new LinearLayout(c) {
            @Override protected void dispatchDraw(Canvas cv) {
                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(NUi.dp(2)); p.setColor(NTheme.line2); p.setPathEffect(new DashPathEffect(new float[]{NUi.dp(4), NUi.dp(7)}, 0));
                float y = cardH + NUi.dp(26 + 17); cv.drawLine(0, y, getWidth(), y, p);
                super.dispatchDraw(cv);
            }
        };
        row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.TOP); row.setWillNotDraw(false);
        int sidePad = Math.max(NUi.dp(24), (Math.round(sh.a.getResources().getConfiguration().screenWidthDp * NUi.density) - itemW) / 2);
        row.setPadding(sidePad, NUi.dp(30), sidePad - gap, NUi.dp(10));
        int nItems = 0, startIdx = 0;
        for (int i = 0; i < N; i++) {
            final JSONObject s = steps.optJSONObject(i); if (s == null) continue;
            if (s == next) startIdx = nItems;
            LinearLayout it = NUi.col(c); it.setGravity(Gravity.CENTER_HORIZONTAL);
            it.addView(flip(bigFront(s, i, N, next), bigBack(s, i, N, next)), new LinearLayout.LayoutParams(-1, cardH));
            it.addView(stem(), new LinearLayout.LayoutParams(NUi.dp(2), NUi.dp(24)));
            it.addView(node(s.optBoolean("done"), false, 0, toggle(s)));
            TextView lb = mono(s.optBoolean("done") ? "Done" : NDates.valid(s.optString("due")) ? NDates.dueShort(s.optString("due")) : "No date", 10.5f, NTheme.muted); lb.setPadding(0, NUi.dp(10), 0, 0); lb.setSingleLine(true); it.addView(lb);
            LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(itemW, -2); l.rightMargin = gap; row.addView(it, l); nItems++;
        }
        for (final JSONObject[] k : subs()) {
            final JSONObject x = k[0]; int xc = NTheme.areaCol(x.optString("area")); boolean xd = "done".equals(x.optString("status")); int p = NActs.pct(st, x);
            LinearLayout it = NUi.col(c); it.setGravity(Gravity.CENTER_HORIZONTAL);
            LinearLayout f = NUi.col(c); f.setBackground(NUi.ripple(NUi.round(xd ? NUi.mix(xc, .6f, NTheme.bg) : xc, 30, 0), 30)); f.setPadding(NUi.dp(22), NUi.dp(22), NUi.dp(22), NUi.dp(20));
            LinearLayout top = NUi.row(c); top.addView(mono("Sub-goal", 10.5f, INKM), NUi.lpw(0, -2, 1));
            TextView b = badge(xd ? "✓" : p + "%", 0); top.addView(b); f.addView(top);
            f.addView(new View(c), NUi.lpw(-1, 0, 1));
            TextView t = NUi.ell(NUi.text(c, x.optString("title").toUpperCase(), 30, INK), 4); t.setTypeface(NFont.display(800)); t.setLineSpacing(0, .96f); t.setIncludeFontPadding(false); f.addView(t);
            f.addView(NBits.bar(c, p / 100f, INK, 4), NUi.mt(16));
            JSONObject nx = NActs.nextStep(x);
            TextView ft = mono(xd ? "Achieved" : nx != null ? "Next: " + nx.optString("title") : "No steps yet", 10.5f, INKM); ft.setSingleLine(true); ft.setEllipsize(android.text.TextUtils.TruncateAt.END); f.addView(ft, NUi.mt(12));
            NUi.tap(f, new View.OnClickListener() { public void onClick(View v) { sh.push(new NGoalScreen(sh, x.optString("id"))); } });
            it.addView(f, new LinearLayout.LayoutParams(-1, cardH));
            it.addView(stem(), new LinearLayout.LayoutParams(NUi.dp(2), NUi.dp(24)));
            it.addView(node(xd, true, p / 100f, new View.OnClickListener() { public void onClick(View v) { sh.push(new NGoalScreen(sh, x.optString("id"))); } }));
            TextView lb = mono(NDates.valid(x.optString("targetDate")) ? "Target " + NDates.fmtDate(x.optString("targetDate")) : "Sub-goal", 10.5f, NTheme.muted); lb.setPadding(0, NUi.dp(10), 0, 0); lb.setSingleLine(true); it.addView(lb);
            LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(itemW, -2); l.rightMargin = gap; row.addView(it, l); nItems++;
        }
        for (int a = 0; a < 2; a++) {
            final boolean step = a == 0;
            LinearLayout it = NUi.col(c); it.setGravity(Gravity.CENTER_HORIZONTAL);
            LinearLayout f = NUi.col(c); f.setGravity(Gravity.CENTER); f.setBackground(NUi.dashed(0, 30, NTheme.line2, 1.5f));
            f.addView(NUi.icon(c, "plus", 36, NTheme.muted), new LinearLayout.LayoutParams(NUi.dp(36), NUi.dp(36)));
            TextView tx = NUi.body(c, step ? "Add a step" : "Add a sub-goal", 15, NTheme.muted, 600); tx.setPadding(0, NUi.dp(10), 0, 0); f.addView(tx, NUi.lp(-2, -2));
            NUi.tap(f, new View.OnClickListener() { public void onClick(View v) { if (step) F.step(g, null); else F.goal(null, g.optString("id")); } });
            it.addView(f, new LinearLayout.LayoutParams(-1, cardH));
            LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(itemW, -2); l.rightMargin = gap; row.addView(it, l); nItems++;
        }
        hs.addView(row);
        final int total = nItems, startAt = startIdx;
        final TextView count = mono("", 10.5f, NTheme.muted); count.setGravity(Gravity.CENTER);
        final Runnable upd = new Runnable() { public void run() { int i = Math.min(total - 1, Math.max(0, Math.round(hs.getScrollX() / (float) (itemW + gap)))); count.setText(String.format(java.util.Locale.US, "%02d / %02d", i + 1, total)); } };
        hs.setOnScrollChangeListener(new View.OnScrollChangeListener() { public void onScrollChange(View v, int x, int y, int ox, int oy) { upd.run(); } });
        hs.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, android.view.MotionEvent e) {
                if (e.getActionMasked() == android.view.MotionEvent.ACTION_UP || e.getActionMasked() == android.view.MotionEvent.ACTION_CANCEL) {
                    final int x = hs.getScrollX(); hs.postDelayed(new Runnable() { public void run() { if (Math.abs(hs.getScrollX() - x) < NUi.dp(3)) hs.smoothScrollTo(Math.round(hs.getScrollX() / (float) (itemW + gap)) * (itemW + gap), 0); else hs.postDelayed(this, 80); } }, 80);
                }
                return false;
            }
        });
        hs.post(new Runnable() { public void run() { hs.scrollTo(startAt * (itemW + gap), 0); upd.run(); } });
        LinearLayout wrap = NUi.col(c);
        wrap.addView(hs, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout foot = NUi.row(c); foot.setGravity(Gravity.CENTER);
        foot.addView(NBits.sbtn(c, "back", new View.OnClickListener() { public void onClick(View v) { hs.smoothScrollTo(Math.max(0, Math.round(hs.getScrollX() / (float) (itemW + gap)) - 1) * (itemW + gap), 0); } }));
        foot.addView(count, new LinearLayout.LayoutParams(NUi.dp(90), -2));
        foot.addView(NBits.sbtn(c, "next", new View.OnClickListener() { public void onClick(View v) { hs.smoothScrollTo((Math.round(hs.getScrollX() / (float) (itemW + gap)) + 1) * (itemW + gap), 0); } }));
        wrap.addView(foot, NUi.mt(16));
        LinearLayout.LayoutParams wl = NUi.lp(-1, -2); return wrapNeg(wrap, gutter);
    }
    View wrapNeg(LinearLayout wrap, int gutter) { FrameLayout f = new FrameLayout(c); f.setClipChildren(false); FrameLayout.LayoutParams l = new FrameLayout.LayoutParams(-1, -2); l.leftMargin = -gutter; l.rightMargin = -gutter; f.addView(wrap, l); return f; }
    View stem() { View v = new View(c); v.setBackground(new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM, new int[]{NTheme.alpha(col, .85f), 0})); return v; }

    View bigFront(JSONObject s, int i, int N, JSONObject next) {
        LinearLayout f = NUi.col(c); f.setBackground(NUi.round(s.optBoolean("done") ? NUi.mix(col, .85f, NTheme.bg) : col, 30, 0)); f.setPadding(NUi.dp(22), NUi.dp(22), NUi.dp(22), NUi.dp(20));
        LinearLayout top = NUi.row(c);
        TextView lab = mono(status(s, next) + " · step " + (i + 1) + " of " + N, 10.5f, INKM); lab.setSingleLine(true); lab.setEllipsize(android.text.TextUtils.TruncateAt.END);
        top.addView(lab, NUi.lpw(0, -2, 1)); top.addView(badge(String.format(java.util.Locale.US, "%02d", i + 1), 0)); f.addView(top);
        f.addView(new View(c), NUi.lpw(-1, 0, 1));
        TextView t = NUi.ell(NUi.text(c, s.optString("title").toUpperCase(), 30, s.optBoolean("done") ? INKM : INK), 4); t.setTypeface(NFont.display(800)); t.setLineSpacing(0, .96f); t.setIncludeFontPadding(false);
        if (s.optBoolean("done")) t.setPaintFlags(t.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        f.addView(t);
        LinearLayout foot = NUi.row(c);
        TextView d = mono(dueLine(s), 10.5f, INKM); d.setSingleLine(true); d.setEllipsize(android.text.TextUtils.TruncateAt.END); foot.addView(d, NUi.lpw(0, -2, 1));
        FrameLayout hint = new FrameLayout(c); hint.setBackground(NUi.oval(0, NTheme.alpha(INK, .24f), 1)); hint.addView(NUi.icon(c, "flipi", 16, INK), new FrameLayout.LayoutParams(NUi.dp(16), NUi.dp(16), Gravity.CENTER));
        foot.addView(hint, new LinearLayout.LayoutParams(NUi.dp(36), NUi.dp(36)));
        f.addView(foot, NUi.mt(18));
        return f;
    }

    View bigBack(final JSONObject s, int i, int N, JSONObject next) {
        LinearLayout f = NUi.col(c); f.setBackground(NCard.bgFor(g, col, 30)); f.setPadding(NUi.dp(22), NUi.dp(20), NUi.dp(22), NUi.dp(18));
        f.addView(mono("Step " + (i + 1) + " of " + N + " · " + status(s, next), 10.5f, INKM));
        TextView h = NUi.ell(NUi.text(c, s.optString("title").toUpperCase(), 24, INK), 2); h.setTypeface(NFont.display(800)); h.setLineSpacing(0, .95f); h.setIncludeFontPadding(false); h.setPadding(0, NUi.dp(6), 0, NUi.dp(4)); f.addView(h);
        f.addView(kv("Due", NDates.valid(s.optString("due")) ? NDates.fmtDate(s.optString("due")) + (s.optString("time").isEmpty() ? "" : " · " + NDates.fmtTime(s.optString("time"))) : "No date", true, INK, INKM));
        f.addView(kv("Reminder", s.optBoolean("done") ? remLabel(s) : remLabel(s), false, INK, INKM));
        if (!s.optString("repeat").isEmpty()) f.addView(kv("Repeats", s.optString("repeat").substring(0, 1).toUpperCase() + s.optString("repeat").substring(1), false, INK, INKM));
        f.addView(kv("Focus", s.optInt("focus") > 0 ? s.optInt("focus") + " min" : "None yet", false, INK, INKM));
        TextView nt = NUi.ell(NUi.text(c, s.optString("note").isEmpty() ? "No notes yet." : s.optString("note"), 14, INKM), 2); nt.setLineSpacing(0, 1.15f); nt.setPadding(0, NUi.dp(4), 0, 0); f.addView(nt);
        f.addView(new View(c), NUi.lpw(-1, 0, 1));
        LinearLayout act = NUi.col(c);
        if (s.optBoolean("done")) act.addView(inkBtn("Mark not done", false, toggle(s)), NUi.lp(-1, NUi.dp(38)));
        else {
            act.addView(inkBtn("✓  Mark done", true, toggle(s)), NUi.lp(-1, NUi.dp(38)));
            LinearLayout r2 = NUi.row(c);
            r2.addView(inkBtn("Focus timer", false, new View.OnClickListener() { public void onClick(View v) { NTimer.pick(sh, g.optString("id"), s.optString("id")); } }), NUi.lpw(0, NUi.dp(38), 1));
            LinearLayout.LayoutParams ll = NUi.lpw(0, NUi.dp(38), 1); ll.leftMargin = NUi.dp(8);
            r2.addView(inkBtn("Edit step", false, new View.OnClickListener() { public void onClick(View v) { F.step(g, s); } }), ll);
            act.addView(r2, NUi.mt(8));
        }
        if (s.optBoolean("done")) { LinearLayout.LayoutParams el = NUi.mt(8); act.addView(inkBtn("Edit step", false, new View.OnClickListener() { public void onClick(View v) { F.step(g, s); } }), new LinearLayout.LayoutParams(-1, NUi.dp(38))); }
        f.addView(act);
        return f;
    }

    /* ---------- Path: zig-zag spine ---------- */
    final class Spine extends LinearLayout {
        View fillTo;
        Spine(Context ctx) { super(ctx); setOrientation(VERTICAL); setWillNotDraw(false); setPadding(0, NUi.dp(14), 0, NUi.dp(10)); }
        @Override protected void dispatchDraw(Canvas cv) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(NUi.dp(2)); p.setColor(NTheme.line2); p.setPathEffect(new DashPathEffect(new float[]{NUi.dp(4), NUi.dp(7)}, 0));
            float x = getWidth() / 2f; cv.drawLine(x, 0, x, getHeight(), p);
            if (fillTo != null) {
                float y = 0; View v = fillTo; while (v != this && v != null) { y += v.getTop() + v.getHeight() / 2f * 0; v = (View) v.getParent(); }
                float cy = nodeY(fillTo);
                Paint q = new Paint(Paint.ANTI_ALIAS_FLAG); q.setStrokeWidth(NUi.dp(2)); q.setColor(col); q.setStrokeCap(Paint.Cap.ROUND); cv.drawLine(x, 0, x, cy, q);
            }
            super.dispatchDraw(cv);
        }
        float nodeY(View n) { float y = n.getHeight() / 2f; View v = n; while (v != this && v != null) { y += v.getTop(); v = v.getParent() instanceof View ? (View) v.getParent() : null; } return y; }
    }

    View path() {
        final JSONArray steps = g.optJSONArray("steps"); final int N = steps == null ? 0 : steps.length();
        JSONObject next = NActs.nextStep(g);
        Spine sp = new Spine(c);
        int idx = 0;
        for (int i = 0; i < N; i++) {
            final JSONObject s = steps.optJSONObject(i); if (s == null) continue;
            View card = flip(pathFront(s, i), pathBack(s));
            View nd = node(s.optBoolean("done"), false, 0, toggle(s));
            row(sp, card, nd, idx++ % 2 == 1, s.optBoolean("done"), col);
            if (s.optBoolean("done")) sp.fillTo = nd;
        }
        for (final JSONObject[] k : subs()) {
            final JSONObject x = k[0]; int xc = NTheme.areaCol(x.optString("area")); boolean xd = "done".equals(x.optString("status")); int p = NActs.pct(st, x);
            LinearLayout f = NUi.col(c); f.setBackground(NUi.ripple(NUi.round(xd ? NUi.mix(xc, .6f, NTheme.bg) : xc, 22, 0), 22)); f.setPadding(NUi.dp(18), NUi.dp(16), NUi.dp(18), NUi.dp(16)); f.setMinimumHeight(NUi.dp(118));
            f.addView(badge(xd ? "✓" : p + "%", 1));
            f.addView(mono(NActs.kids(st, x).size() > 0 ? "Sub-goal" : "Sub-goal", 10.5f, INKM), NUi.mt(6));
            TextView t = NUi.ell(NUi.body(c, x.optString("title"), 15.5f, INK, 700), 3); t.setPadding(0, NUi.dp(4), 0, NUi.dp(8)); f.addView(t);
            f.addView(NBits.bar(c, p / 100f, INK, 4), NUi.lp(-1, NUi.dp(4)));
            NUi.tap(f, new View.OnClickListener() { public void onClick(View v) { sh.push(new NGoalScreen(sh, x.optString("id"))); } });
            View nd = node(xd, true, p / 100f, new View.OnClickListener() { public void onClick(View v) { sh.push(new NGoalScreen(sh, x.optString("id"))); } });
            row(sp, f, nd, idx++ % 2 == 1, xd, xc);
        }
        LinearLayout add = NUi.row(c); add.setGravity(Gravity.CENTER);
        TextView a1 = chipBtn("Add step", new View.OnClickListener() { public void onClick(View v) { F.step(g, null); } });
        TextView a2 = chipBtn("Add sub-goal", new View.OnClickListener() { public void onClick(View v) { F.goal(null, g.optString("id")); } });
        add.addView(a1); LinearLayout.LayoutParams l2 = NUi.lp(-2, -2); l2.leftMargin = NUi.dp(8); add.addView(a2, l2);
        sp.addView(add, NUi.mt(18));
        return sp;
    }
    TextView chipBtn(String s, View.OnClickListener l) {
        TextView t = NUi.body(c, "+  " + s, 14.5f, NTheme.text, 600); t.setGravity(Gravity.CENTER); t.setPadding(NUi.dp(16), 0, NUi.dp(16), 0); t.setMinHeight(NUi.dp(44));
        t.setBackground(NUi.ripple(NUi.round(NTheme.surface2, 14, NTheme.line2), 14)); NUi.tap(t, l); return t;
    }
    void row(Spine sp, View card, View node, boolean right, boolean done, int cc) {
        LinearLayout r = NUi.row(c); r.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout left = new FrameLayout(c), mid = new FrameLayout(c), rt = new FrameLayout(c);
        View line = new View(c); line.setBackgroundColor(NTheme.alpha(cc, .55f));
        FrameLayout.LayoutParams ll = new FrameLayout.LayoutParams(NUi.dp(21), NUi.dp(2), Gravity.CENTER_VERTICAL | (right ? Gravity.RIGHT : Gravity.LEFT));
        mid.addView(line, ll);
        mid.addView(node, new FrameLayout.LayoutParams(NUi.dp(34), NUi.dp(34), Gravity.CENTER));
        (right ? rt : left).addView(card, new FrameLayout.LayoutParams(-1, -2));
        r.addView(left, new LinearLayout.LayoutParams(0, -2, 1)); r.addView(mid, new LinearLayout.LayoutParams(NUi.dp(76), NUi.dp(40))); r.addView(rt, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams rl = NUi.lp(-1, -2); if (sp.getChildCount() > 0) rl.topMargin = -NUi.dp(26);
        sp.addView(r, rl);
    }
    View pathFront(JSONObject s, int i) {
        LinearLayout f = NUi.col(c); f.setBackground(NUi.round(s.optBoolean("done") ? NUi.mix(col, .85f, NTheme.bg) : col, 22, 0)); f.setPadding(NUi.dp(18), NUi.dp(16), NUi.dp(18), NUi.dp(16)); f.setMinimumHeight(NUi.dp(118));
        f.addView(badge(String.format(java.util.Locale.US, "%02d", i + 1), 1));
        TextView t = NUi.ell(NUi.body(c, s.optString("title"), 15.5f, INK, 700), 4); t.setLineSpacing(0, 1.05f); t.setPadding(0, NUi.dp(10), 0, NUi.dp(8));
        if (s.optBoolean("done")) t.setPaintFlags(t.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        f.addView(t);
        f.addView(new View(c), NUi.lpw(-1, 0, 1));
        TextView d = mono(dueLine(s), 10, INKM); f.addView(d);
        return f;
    }
    View pathBack(final JSONObject s) {
        LinearLayout f = NUi.col(c); f.setBackground(NUi.round(NTheme.surface2, 22, NUi.mix(col, .5f, NTheme.line))); f.setPadding(NUi.dp(18), NUi.dp(14), NUi.dp(18), NUi.dp(14)); f.setMinimumHeight(NUi.dp(118));
        int fg = NTheme.text, mu = NTheme.muted;
        if (NDates.valid(s.optString("due"))) f.addView(kv("Due", NDates.fmtDate(s.optString("due")) + (s.optString("time").isEmpty() ? "" : " · " + NDates.fmtTime(s.optString("time"))), true, fg, mu));
        f.addView(kv("Reminder", remLabel(s), !NDates.valid(s.optString("due")), fg, mu));
        if (s.optInt("focus") > 0) f.addView(kv("Focus", s.optInt("focus") + " min", false, fg, mu));
        if (!s.optString("note").isEmpty()) { TextView n = NUi.ell(NUi.text(c, s.optString("note"), 13.5f, mu), 4); n.setPadding(0, NUi.dp(4), 0, 0); f.addView(n); }
        LinearLayout act = NUi.row(c); act.setPadding(0, NUi.dp(8), 0, 0);
        TextView e = NUi.btn(c, "Edit", false, new View.OnClickListener() { public void onClick(View v) { F.step(g, s); } }); e.setMinHeight(NUi.dp(36)); e.setTextSize(13); act.addView(e, NUi.lpw(0, NUi.dp(36), 1));
        if (!s.optBoolean("done")) { TextView fo = NUi.btn(c, "Focus", false, new View.OnClickListener() { public void onClick(View v) { NTimer.pick(sh, g.optString("id"), s.optString("id")); } }); fo.setMinHeight(NUi.dp(36)); fo.setTextSize(13); LinearLayout.LayoutParams fl = NUi.lpw(0, NUi.dp(36), 1); fl.leftMargin = NUi.dp(6); act.addView(fo, fl); }
        f.addView(act);
        return f;
    }
}
