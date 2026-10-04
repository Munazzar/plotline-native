package com.munazzar.plotline;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.text.TextPaint;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/* Habit Vista: every habit is a lane, its line through time underneath. Done days are stops, streaks bold stretches,
   misses faint, rest hollow, pauses dotted, slips ×. The engine computes each day's state; this draws it. */
final class NHabitVista {
    static final float TOP = 34, LANE = 70, TRACK = 48;
    static final int[][] SPANS = {{7}, {30}, {90}, {365}};
    static final String[] SPAN_L = {"Week", "Month", "3 months", "Year"};

    final NShell sh; final Context c; final NStore st; final NHabitsPage pg;
    final LinearLayout card; JSONObject D; Canvas0 cv; HorizontalScrollView hsv; TextView range; LinearLayout spanRow;
    double days = 30; boolean scaled;

    NHabitVista(NShell sh, NHabitsPage pg) {
        this.sh = sh; this.c = sh.a; this.st = sh.st; this.pg = pg;
        JSONObject l = st.settings().optJSONObject("layout"); if (l != null && l.optDouble("hvd", 0) > 0) days = Math.max(7, Math.min(400, l.optDouble("hvd")));
        card = NUi.col(c);
        card.setBackground(new android.graphics.drawable.Drawable() {
            final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override public void draw(Canvas k) { RectF b = new RectF(getBounds()); float r = NUi.dp(30); p.setStyle(Paint.Style.FILL); p.setColor(NTheme.surface); k.drawRoundRect(b, r, r, p);
                k.save(); android.graphics.Path cp = new android.graphics.Path(); cp.addRoundRect(b, r, r, android.graphics.Path.Direction.CW); k.clipPath(cp);
                p.setShader(new android.graphics.RadialGradient(b.right, b.top, b.width() * 1.1f, NTheme.alpha(NTheme.accent, .09f), 0, Shader.TileMode.CLAMP)); k.drawRect(b, p); p.setShader(null); k.restore();
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(NUi.dp(1)); p.setColor(NTheme.line); b.inset(NUi.dp(.5f), NUi.dp(.5f)); k.drawRoundRect(b, r, r, p); }
            @Override public void setAlpha(int a) { } @Override public void setColorFilter(android.graphics.ColorFilter f) { } @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
        });
        card.setPadding(0, NUi.dp(18), 0, NUi.dp(16));
        TextView ld = NUi.text(c, "Loading…", 14, NTheme.muted); ld.setPadding(NUi.dp(18), NUi.dp(20), NUi.dp(18), NUi.dp(20)); card.addView(ld);
        sh.a.jsRet("(window.__nhv?window.__nhv():'')", new android.webkit.ValueCallback<String>() { public void onReceiveValue(String v) {
            try { if (v == null || v.isEmpty()) return; D = new JSONObject(v); fill(); } catch (Exception e) { NCrash.log(c, "habit vista", e); }
        } });
    }

    void fill() {
        card.removeAllViews();
        JSONArray lanes = D.optJSONArray("lanes"); if (lanes == null || lanes.length() == 0) return;
        /* hero */
        LinearLayout hero = NUi.row(c); hero.setPadding(NUi.dp(18), 0, NUi.dp(18), 0);
        int due = D.optInt("due"), dn = D.optInt("dn"); float f = due > 0 ? dn / (float) due : 0;
        NRing ring = new NRing(c); ring.strokeDp = 7; ring.set(f, true); ring.text(String.valueOf(Math.round(f * 100)), "%");
        hero.addView(ring, NUi.lp(NUi.dp(76), NUi.dp(76)));
        LinearLayout hs = NUi.col(c); hs.setPadding(NUi.dp(16), 0, 0, 0);
        hs.addView(NBits.meta(c, ("Kept this week · " + dn + " of " + due).toUpperCase(), NTheme.muted));
        NFlow chips = new NFlow(c, 6, 6);
        JSONObject best = D.optJSONObject("best");
        if (best != null) chips.addView(chip("flame", best.optInt("n") + "", " " + trunc(best.optString("t"), 22)));
        chips.addView(chip("check", D.optInt("m30") + "", " done in 30 days"));
        if (D.optInt("pr") > 1) chips.addView(chip(null, "★ " + D.optInt("pr"), " perfect days"));
        hs.addView(chips, NUi.mt(8));
        hero.addView(hs, NUi.lpw(0, -2, 1));
        card.addView(hero);
        /* span + range */
        NFlow bar = new NFlow(c, 12, 6); bar.setPadding(NUi.dp(18), NUi.dp(16), NUi.dp(18), NUi.dp(4));
        spanRow = NUi.row(c); bar.addView(spanRow);
        range = NBits.meta(c, "", NTheme.muted); bar.addView(range);
        card.addView(bar);
        paintSpans();
        /* canvas */
        cv = new Canvas0(c, lanes);
        hsv = new HorizontalScrollView(c) {
            final ScaleGestureDetector sg = new ScaleGestureDetector(c, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override public boolean onScale(ScaleGestureDetector d) { zoomBy(d.getScaleFactor(), d.getFocusX() / NUi.density); scaled = true; return true; }
            });
            @Override public boolean onInterceptTouchEvent(MotionEvent e) { if (e.getPointerCount() > 1) return true; return super.onInterceptTouchEvent(e); }
            @Override public boolean onTouchEvent(MotionEvent e) {
                sg.onTouchEvent(e);
                if (sg.isInProgress() || e.getPointerCount() > 1) return true;
                if (e.getActionMasked() == MotionEvent.ACTION_UP && scaled) { scaled = false; save(); }
                return super.onTouchEvent(e);
            }
            @Override protected void onScrollChanged(int l, int t, int ol, int ot) { super.onScrollChanged(l, t, ol, ot); cv.invalidate(); updateRange(); }
        };
        hsv.setHorizontalScrollBarEnabled(false); hsv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        hsv.addView(cv, new android.widget.FrameLayout.LayoutParams(-2, -2));
        card.addView(hsv, NUi.mt(4));
        hsv.post(new Runnable() { public void run() { cv.setVw(hsv.getWidth() / NUi.density); cv.relayout(); hsv.scrollTo(cv.getWidth(), 0); updateRange(); } });
        /* legend */
        NFlow lg = new NFlow(c, 14, 6); lg.setPadding(NUi.dp(22), NUi.dp(12), NUi.dp(22), 0);
        String[][] L = {{"d", "Done"}, {"ln", "Streak"}, {"x", "Missed"}, {"s", "Rest"}, {"sl", "Slip"}, {"t", "Due today, tap to log"}};
        for (String[] it : L) { LinearLayout r = NUi.row(c); r.addView(new Glyph(c, it[0]), NUi.lp(NUi.dp(it[0].equals("ln") ? 18 : 12), NUi.dp(12))); TextView t = NBits.meta(c, it[1].toUpperCase(), NTheme.muted); t.setTextSize(10); t.setPadding(NUi.dp(5), 0, 0, 0); r.addView(t); lg.addView(r); }
        card.addView(lg);
    }

    static String trunc(String s, int n) { return s.length() > n ? s.substring(0, n - 1) + "…" : s; }

    View chip(String icon, String b, String rest) {
        LinearLayout r = NUi.row(c); r.setBackground(NUi.round(NTheme.bg2, 99, NTheme.line)); r.setPadding(NUi.dp(10), NUi.dp(5), NUi.dp(10), NUi.dp(5));
        if (icon != null) { r.addView(NUi.icon(c, icon, 13, NTheme.accent)); }
        TextView t = NUi.body(c, b, 12, NTheme.text, 700); t.setPadding(NUi.dp(icon != null ? 5 : 0), 0, 0, 0); r.addView(t);
        TextView u = NUi.body(c, rest, 12, NTheme.muted, 500); u.setSingleLine(true); u.setEllipsize(android.text.TextUtils.TruncateAt.END); r.addView(u);
        return r;
    }

    int nearSpan() { int bi = 0; double bd = 1e9; for (int i = 0; i < SPANS.length; i++) { double d = Math.abs(Math.log(SPANS[i][0] / days)); if (d < bd) { bd = d; bi = i; } } return bi; }

    void paintSpans() {
        spanRow.removeAllViews(); spanRow.setBackground(NUi.round(NTheme.bg2, 99, NTheme.line)); spanRow.setPadding(NUi.dp(3), NUi.dp(3), NUi.dp(3), NUi.dp(3));
        int near = nearSpan();
        for (int i = 0; i < SPANS.length; i++) {
            final int to = SPANS[i][0]; TextView t = NUi.body(c, SPAN_L[i], 12.5f, i == near ? NTheme.text : NTheme.muted, 600);
            t.setPadding(NUi.dp(NUi.narrow ? 10 : 12), NUi.dp(7), NUi.dp(NUi.narrow ? 10 : 12), NUi.dp(7));
            if (i == near) t.setBackground(NUi.round(NTheme.surface2, 99, 0));
            NUi.tap(t, new View.OnClickListener() { public void onClick(View v) { animateTo(to); } });
            spanRow.addView(t);
        }
    }

    void animateTo(final double to) {
        final double from = days;
        if ("reduced".equals(st.settings().optString("motion"))) { setDays(to, -1); save(); return; }
        android.animation.ValueAnimator va = android.animation.ValueAnimator.ofFloat(0f, 1f); va.setDuration(320); va.setInterpolator(NUi.EASE);
        va.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() { public void onAnimationUpdate(android.animation.ValueAnimator a) { float k = (Float) a.getAnimatedValue(); setDays(Math.exp(Math.log(from) + (Math.log(to) - Math.log(from)) * k), -1); } });
        va.addListener(new android.animation.AnimatorListenerAdapter() { public void onAnimationEnd(android.animation.Animator a) { save(); } });
        va.start();
    }

    void zoomBy(float factor, float anchorDp) { setDays(days / factor, anchorDp); }

    /* keep the day under the anchor (or the right edge) still while the scale changes */
    void setDays(double d, float anchor) {
        if (cv == null) return;
        d = Math.max(7, Math.min(400, d));
        float vw = cv.vw; float oldP = cv.ppd, oldPad = cv.pad, sx = hsv.getScrollX() / NUi.density;
        boolean atEnd = sx + vw >= cv.wdp - 3; float anc = anchor < 0 ? vw : anchor;
        double day = cv.a + (sx + anc - oldPad) / oldP;
        days = d; cv.relayout();
        float nx = atEnd && anchor < 0 ? cv.wdp : (float) ((day - cv.a) * cv.ppd + cv.pad - anc);
        final int px = Math.max(0, Math.round(nx * NUi.density));
        hsv.post(new Runnable() { public void run() { hsv.scrollTo(px, 0); updateRange(); } });
        paintSpans();
    }

    void save() { try { JSONObject l = st.settings().optJSONObject("layout"); if (l == null) { l = new JSONObject(); st.settings().put("layout", l); } l.put("hvd", Math.round(days * 100) / 100.0); } catch (Exception ignored) { } sh.saveQuiet(); }

    void updateRange() {
        if (cv == null || hsv == null || range == null) return;
        float sx = hsv.getScrollX() / NUi.density, p = cv.ppd;
        int k0 = (int) Math.round(cv.a + (sx - cv.pad) / p), k1 = Math.min(cv.tn, (int) Math.round(cv.a + (sx + cv.vw - cv.pad) / p));
        String f0 = NDates.fmtDate(NDates.fromN(Math.max(cv.a, k0))), f1 = k1 >= cv.tn ? "today" : NDates.fmtDate(NDates.fromN(k1));
        range.setText((f0 + " – " + f1).toUpperCase());
    }

    static int hvCol(int i) { return NGen.AREA_COL[i % NGen.AREA_COL.length]; }

    /* ---------------- the drawing ---------------- */
    final class Canvas0 extends View {
        final JSONArray lanes; final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); final TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        final List<Object[]> hits = new ArrayList<>();
        int a, tn, a0; float ppd = 7, pad = 30, vw = 360, wdp, hdp; final float den = NUi.density;
        Canvas0(Context c, JSONArray lanes) { super(c); this.lanes = lanes; tn = D.optInt("tn"); a0 = D.optInt("a"); setLayerType(LAYER_TYPE_HARDWARE, null); }
        void setVw(float w) { vw = w; }
        void relayout() {
            ppd = (float) Math.max(.9, Math.min(64, vw / days));
            int stm1 = a0; /* the engine already returned from max(tn-730, earliest start - 1) */
            a = Math.max(tn - 730, Math.min(stm1, tn - (int) Math.ceil(vw / ppd) + 1));
            pad = Math.max(30, ppd * .6f); wdp = Math.round((tn - a) * ppd + pad * 2); hdp = TOP + lanes.length() * LANE + 6;
            requestLayout(); invalidate();
        }
        @Override protected void onMeasure(int w, int h) { setMeasuredDimension(Math.round(wdp * den), Math.round(hdp * den)); }
        float x(int k) { return (k - a) * ppd + pad; }
        char state(JSONObject l, int k) { if (k < a0) return 'n'; String s = l.optString("st"); int i = k - a0; return i >= 0 && i < s.length() ? s.charAt(i) : 'n'; }

        @Override protected void onDraw(Canvas g) {
            hits.clear(); g.scale(den, den);
            float sx = hsv == null ? 0 : hsv.getScrollX() / den;
            int n = lanes.length();
            /* lane backgrounds stay put */
            for (int i = 0; i < n; i++) {
                int col = hvCol(lanes.optJSONObject(i).optInt("ci")); float y = TOP + i * LANE + 4;
                p.setStyle(Paint.Style.FILL); p.setShader(new android.graphics.LinearGradient(sx + 10, 0, sx + vw - 10, 0, new int[]{NTheme.alpha(col, .09f), NTheme.alpha(col, .03f), NTheme.alpha(col, .07f)}, new float[]{0, .7f, 1}, Shader.TileMode.CLAMP));
                g.drawRoundRect(sx + 10, y, sx + vw - 10, y + LANE - 8, 20, 20, p); p.setShader(null);
            }
            axis(g);
            chapters(g);
            /* today */
            float tx = x(tn);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.5f); p.setColor(NTheme.alpha(NTheme.accent, .8f)); p.setPathEffect(new DashPathEffect(new float[]{3, 4}, 0)); g.drawLine(tx, TOP - 4, tx, hdp, p); p.setPathEffect(null);
            p.setStyle(Paint.Style.FILL); p.setColor(NTheme.accent); g.drawRoundRect(tx - 21, TOP - 14 - 9, tx + 21, TOP - 14 + 8, 8.5f, 8.5f, p);
            tp.setTypeface(NFont.mono(700)); tp.setTextSize(9); tp.setLetterSpacing(.06f); tp.setColor(NTheme.onAccent); float w = tp.measureText("TODAY"); g.drawText("TODAY", tx - w / 2, TOP - 14 + 3.5f + 0.5f, tp);
            for (int i = 0; i < n; i++) lane(g, i, sx);
            labels(g, sx);
        }

        void axis(Canvas g) {
            float last = -1e9f; tp.setLetterSpacing(.08f);
            for (int k = a; k <= tn; k++) {
                String ds = NDates.fromN(k); int dm = NDates.day(ds), dw = NDates.dow(k); float xk = x(k);
                if (dm == 1) {
                    p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1); p.setColor(NTheme.line2); g.drawLine(xk, TOP - 6, xk, hdp, p);
                    if (xk - last > 46 && x(tn) - xk > 58 && xk > 40) {
                        tp.setTypeface(NFont.mono(600)); tp.setTextSize(10); tp.setColor(NTheme.muted);
                        int m = NDates.month(ds); g.drawText((NDates.MON[m] + (m == 0 ? " " + NDates.year(ds) : "")).toUpperCase(), xk + 5, 15, tp); last = xk;
                    }
                } else if (ppd >= 9 && dw == 1) {
                    p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1); p.setColor(NTheme.line); p.setPathEffect(new DashPathEffect(new float[]{2, 5}, 0)); g.drawLine(xk, TOP - 2, xk, hdp, p); p.setPathEffect(null);
                    tp.setTypeface(NFont.mono(500)); tp.setTextSize(9); tp.setColor(NTheme.alpha(NTheme.muted, .8f)); String t = String.valueOf(dm); g.drawText(t, xk - tp.measureText(t) / 2, 28, tp);
                } else if (ppd >= 30) {
                    tp.setTypeface(NFont.mono(500)); tp.setTextSize(9); tp.setColor(NTheme.alpha(NTheme.muted, .8f)); String t = NDates.DAYS[dw].substring(0, 1) + dm; g.drawText(t, xk - tp.measureText(t) / 2, 28, tp);
                }
            }
            tp.setLetterSpacing(0);
        }

        void chapters(Canvas g) {
            for (JSONObject ch : NChapters.all(st)) {
                int s = Math.max(a, NDates.dnum(ch.optString("start"))), e = Math.min(tn, NDates.dnum(ch.optString("end").isEmpty() ? NDates.ymd() : ch.optString("end")));
                if (e < a || s > tn) continue;
                int cc = NUi.col(ch.optString("color"), 0xFF6B9BFF); float wd = Math.max(3, (e - s + 1) * ppd);
                p.setStyle(Paint.Style.FILL); p.setColor(NTheme.alpha(cc, .08f)); g.drawRect(x(s) - ppd / 2, TOP - 6, x(s) - ppd / 2 + wd, hdp, p);
                if (ppd < 9 && (e - s + 1) * ppd > 60) { tp.setTypeface(NFont.mono(500)); tp.setTextSize(9); tp.setLetterSpacing(0); tp.setColor(cc); g.drawText((ch.optString("emoji").isEmpty() ? "" : ch.optString("emoji") + " ") + ch.optString("title"), x(s) + 2, 28, tp); }
            }
        }

        void lane(Canvas g, int i, float sx) {
            JSONObject l = lanes.optJSONObject(i); int col = hvCol(l.optInt("ci")); float y = TOP + i * LANE + TRACK; boolean paused = l.optBoolean("paused"), quit = l.optBoolean("quit");
            float r = Math.max(2.4f, Math.min(6f, ppd * .32f)); boolean dense = ppd < 5; String hid = l.optString("id");
            if (paused) g.saveLayerAlpha(0, 0, wdp, hdp, 115);
            int first = -1; for (int k = a; k <= tn; k++) if (state(l, k) != 'n') { first = k - a; break; }
            p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND);
            if (first < 0) { p.setStrokeWidth(2); p.setColor(NTheme.line); p.setPathEffect(new DashPathEffect(new float[]{2, 6}, 0)); g.drawLine(x(a), y, x(tn), y, p); p.setPathEffect(null); if (paused) g.restore(); return; }
            p.setStrokeWidth(3); p.setColor(NTheme.alpha(col, .26f)); g.drawLine(x(a + first), y, x(tn), y, p);
            if (quit) {
                int s0 = first; int N = tn - a + 1;
                for (int j = first; j <= N; j++) {
                    if (j == N || state(l, a + j) == 'k') {
                        if (j - 1 >= s0) run(g, x(a + s0), x(a + j - 1), y, col, j - s0 >= 7);
                        if (j < N) { float q = Math.max(3, r * .9f), cx = x(a + j);
                            p.setStyle(Paint.Style.FILL); p.setColor(NTheme.surface); g.drawCircle(cx, y, q + 3, p);
                            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.2f); p.setColor(0xFFFF7A7A); g.drawCircle(cx, y, q + 3, p);
                            p.setStrokeWidth(2); g.drawLine(cx - q, y - q, cx + q, y + q, p); g.drawLine(cx + q, y - q, cx - q, y + q, p); }
                        s0 = j + 1;
                    }
                }
                head(g, x(tn), y, r + 1, col);
            } else {
                List<int[]> runs = new ArrayList<>(); List<Integer> cur = new ArrayList<>(); int N = tn - a + 1;
                for (int j = first; j < N; j++) { char v = state(l, a + j); if (v == 'd') cur.add(j); else if (v == 'x' || v == 'p') { if (!cur.isEmpty()) runs.add(new int[]{cur.get(0), cur.get(cur.size() - 1)}); cur.clear(); } }
                if (!cur.isEmpty()) runs.add(new int[]{cur.get(0), cur.get(cur.size() - 1)});
                boolean[] inRun = new boolean[N];
                for (int[] R : runs) {
                    int len = 0; for (int j = R[0]; j <= R[1]; j++) if (state(l, a + j) == 'd') len++;
                    if (len > 1) { for (int j = R[0]; j <= R[1]; j++) if (state(l, a + j) == 'd') inRun[j] = true; run(g, x(a + R[0]), x(a + R[1]), y, col, len >= 7); }
                    boolean live = a + R[1] >= tn - 1;
                    if (len >= 5 && !live && (x(a + R[1]) - x(a + R[0])) > 30) {
                        float cx = (x(a + R[0]) + x(a + R[1])) / 2;
                        p.setStyle(Paint.Style.FILL); p.setColor(NUi.mix(col, .18f, NTheme.surface)); g.drawRoundRect(cx - 13, y + r + 12 - 8, cx + 13, y + r + 12 + 6, 7, 7, p);
                        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1); p.setColor(NTheme.alpha(col, .5f)); g.drawRoundRect(cx - 13, y + r + 12 - 8, cx + 13, y + r + 12 + 6, 7, 7, p);
                        tp.setTypeface(NFont.mono(700)); tp.setTextSize(8.5f); tp.setColor(col); String t = String.valueOf(len); g.drawText(t, cx - tp.measureText(t) / 2, y + r + 12 + 2.8f, tp);
                    }
                }
                for (int j = first; j < N; j++) {
                    char v = state(l, a + j); int k = a + j; float cx = x(k); if (v == 'n') continue;
                    String ds = NDates.fromN(k);
                    if (dense) {
                        if (inRun[j]) continue;
                        p.setStyle(Paint.Style.FILL);
                        if (v == 'd' || v == 'p') { p.setColor(NTheme.alpha(col, v == 'p' ? .45f : 1f)); g.drawRoundRect(cx - ppd / 2, y - 5, cx - ppd / 2 + Math.max(1, ppd - .4f), y + 5, 1, 1, p); }
                        else if (v == 's') { p.setColor(NTheme.alpha(col, .4f)); g.drawRect(cx - ppd / 2, y - 2, cx - ppd / 2 + Math.max(1, ppd - .4f), y + 2, p); }
                        continue;
                    }
                    p.setStyle(Paint.Style.FILL);
                    if (v == 'd') {
                        if (!inRun[j] || ppd >= 16 || k == tn) { p.setColor(col); g.drawCircle(cx, y, r, p); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(k == tn ? 2 : 1.5f); p.setColor(k == tn ? 0xFFFFFFFF : NTheme.surface); g.drawCircle(cx, y, r, p); }
                        else if (ppd >= 6) { p.setColor(NTheme.alpha(NTheme.bg, .55f)); g.drawCircle(cx, y, 1.3f, p); }
                    } else if (v == 'p') { p.setColor(NUi.mix(col, .4f, NTheme.surface)); g.drawCircle(cx, y, r, p); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.5f); p.setColor(col); g.drawCircle(cx, y, r, p); }
                    else if (v == 'x') { p.setColor(NTheme.alpha(NTheme.text, .22f)); g.drawCircle(cx, y, Math.max(1.6f, r * .45f), p); }
                    else if (v == 's') { p.setColor(NTheme.surface); g.drawCircle(cx, y, r * .78f, p); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.4f); p.setColor(col); p.setPathEffect(new DashPathEffect(new float[]{2, 2}, 0)); g.drawCircle(cx, y, r * .78f, p); p.setPathEffect(null); }
                    else if (v == 'z') { p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3); p.setColor(NTheme.muted); p.setPathEffect(new DashPathEffect(new float[]{1, 4}, 0)); g.drawLine(cx - ppd / 2, y, cx + ppd / 2, y, p); p.setPathEffect(null); }
                    if (ppd >= 6) hits.add(new Object[]{new RectF(cx - ppd / 2, y - 14, cx + ppd / 2, y + 14), "day", hid, ds});
                }
                if (l.optBoolean("todo") && state(l, tn) != 'd') {
                    float pulse = 1f + .25f * (float) Math.abs(Math.sin(System.currentTimeMillis() / 2400.0 * Math.PI)); float rr = (r + 1.5f) * pulse;
                    p.setStyle(Paint.Style.FILL); p.setColor(NTheme.surface); g.drawCircle(x(tn), y, rr, p);
                    p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2); p.setColor(col); p.setPathEffect(new DashPathEffect(new float[]{3, 2.5f}, 0)); g.drawCircle(x(tn), y, rr, p); p.setPathEffect(null);
                    hits.add(0, new Object[]{new RectF(x(tn) - 16, y - 16, x(tn) + 16, y + 16), "tap", hid, ""});
                    postInvalidateOnAnimation();
                }
            }
            p.setStyle(Paint.Style.FILL);
            if (paused) g.restore();
        }

        void run(Canvas g, float x0, float x1, float y, int col, boolean glow) {
            p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND);
            if (glow) { p.setStrokeWidth(14); p.setColor(NTheme.alpha(col, .14f)); g.drawLine(x0, y, x1, y, p); p.setStrokeWidth(10); p.setColor(NTheme.alpha(col, .22f)); g.drawLine(x0, y, x1, y, p); }
            p.setStrokeWidth(6); p.setColor(col); g.drawLine(x0, y, x1, y, p);
        }
        void head(Canvas g, float cx, float y, float r, int col) {
            p.setStyle(Paint.Style.FILL); p.setColor(NTheme.alpha(col, .25f)); g.drawCircle(cx, y, r + 6, p);
            p.setColor(col); g.drawCircle(cx, y, r, p); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2); p.setColor(NTheme.surface); g.drawCircle(cx, y, r, p);
        }

        /* names and live streaks stay in view while the lines scroll */
        void labels(Canvas g, float sx) {
            int n = lanes.length();
            for (int i = 0; i < n; i++) {
                JSONObject l = lanes.optJSONObject(i); int col = hvCol(l.optInt("ci")); float top = TOP + i * LANE + 12; float left = sx + (NUi.narrow ? 18 : 22), right = sx + vw - (NUi.narrow ? 18 : 22);
                if (l.optBoolean("paused")) g.saveLayerAlpha(sx, top - 12, sx + vw, top + 40, 115);
                /* stats on the right */
                int big = l.optInt("big"); String unit = l.optString("unit"); boolean q = l.optBoolean("quit");
                tp.setTypeface(NFont.mono(500)); tp.setTextSize(9.5f); tp.setLetterSpacing(.04f); float uw = tp.measureText(unit);
                tp.setTypeface(NFont.display(800)); tp.setTextSize(15); tp.setLetterSpacing(0); String bs = String.valueOf(big); float bw = tp.measureText(bs);
                float kw = 0; String kept = l.isNull("kept") || !l.has("kept") ? null : l.optInt("kept") + "%";
                TextPaint kp = new TextPaint(Paint.ANTI_ALIAS_FLAG); kp.setTypeface(NFont.mono(600)); kp.setTextSize(10); if (kept != null) kw = kp.measureText(kept) + 12 + 6;
                float sw = bw + 4 + uw + kw + (!q && big > 0 ? 17 : 0), sx0 = right - sw; int sc = big > 0 ? col : NTheme.muted;
                float cx0 = sx0;
                if (!q && big > 0) { NIcon ic = new NIcon("flame", sc); ic.setBounds(Math.round(cx0 * den), Math.round((top + 2) * den), Math.round((cx0 + 13) * den), Math.round((top + 15) * den)); g.save(); g.scale(1 / den, 1 / den); ic.draw(g); g.restore(); cx0 += 17; }
                tp.setTypeface(NFont.display(800)); tp.setTextSize(15); tp.setColor(sc); g.drawText(bs, cx0, top + 15, tp); cx0 += bw + 4;
                tp.setTypeface(NFont.mono(500)); tp.setTextSize(9.5f); tp.setLetterSpacing(.04f); tp.setColor(NTheme.muted); g.drawText(unit, cx0, top + 15, tp); cx0 += uw; tp.setLetterSpacing(0);
                if (kept != null) { cx0 += 6; p.setStyle(Paint.Style.FILL); p.setColor(NTheme.bg2); g.drawRoundRect(cx0, top + 2, cx0 + kw - 6, top + 17, 6, 6, p); kp.setColor(NTheme.muted); g.drawText(kept, cx0 + 6, top + 13, kp); }
                /* name on the left */
                p.setStyle(Paint.Style.FILL); p.setColor(NTheme.alpha(col, .22f)); g.drawRoundRect(left, top, left + 22, top + 22, 8, 8, p);
                tp.setTypeface(Typeface0.sys()); tp.setTextSize(12); tp.setColor(NTheme.text); String ic = l.optString("i"); g.drawText(ic, left + 11 - tp.measureText(ic) / 2, top + 15.5f, tp);
                float nameL = left + 30, avail = sx0 - 10 - nameL; String nm = l.optString("t"); boolean pz = l.optBoolean("paused");
                tp.setTypeface(NFont.body(650)); tp.setTextSize(13.5f); tp.setColor(NTheme.text);
                if (pz) { avail -= 46; }
                String shown = android.text.TextUtils.ellipsize(nm, tp, Math.max(20, avail), android.text.TextUtils.TruncateAt.END).toString();
                g.drawText(shown, nameL, top + 16, tp);
                if (pz) { tp.setTypeface(NFont.mono(500)); tp.setTextSize(9.5f); tp.setLetterSpacing(.06f); tp.setColor(NTheme.muted); g.drawText("PAUSED", nameL + tp.measureText(shown.length() == 0 ? "" : "") + 0, top + 16, tp); tp.setLetterSpacing(0); }
                if (l.optBoolean("paused")) g.restore();
                hits.add(new Object[]{new RectF(left, top - 4, sx0 - 8, top + 26), "open", l.optString("id"), ""});
            }
        }

        float dx, dy; boolean moved;
        @Override public boolean onTouchEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: dx = e.getX(); dy = e.getY(); moved = false; return true;
                case MotionEvent.ACTION_MOVE: if (Math.abs(e.getX() - dx) > 12 * den || Math.abs(e.getY() - dy) > 12 * den) moved = true; return false;
                case MotionEvent.ACTION_UP: {
                    if (moved) return false;
                    float x = e.getX() / den, y = e.getY() / den;
                    for (Object[] h : hits) {
                        if (!((RectF) h[0]).contains(x, y)) continue;
                        String kind = (String) h[1], id = (String) h[2];
                        if (kind.equals("open")) sh.push(new NHabitScreen(sh, id));
                        else if (kind.equals("tap")) { JSONObject hb = st.find("habits", id); if (hb != null) { boolean full = NActs.habitTap(hb); String m = full ? NActs.milestone(st, hb) : null; sh.save(); if (m != null) NShell.toast(m); } }
                        else NHabitDay.open(sh, st.find("habits", id), (String) h[3]);
                        return true;
                    }
                    return true;
                }
            }
            return true;
        }
    }

    static final class Typeface0 { static android.graphics.Typeface sys() { return android.graphics.Typeface.DEFAULT; } }

    /* legend glyphs */
    final class Glyph extends View {
        final String k; final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Glyph(Context c, String k) { super(c); this.k = k; }
        @Override protected void onDraw(Canvas g) {
            float w = getWidth(), h = getHeight(), cx = w / 2, cy = h / 2, d = NUi.density; p.setStyle(Paint.Style.FILL);
            switch (k) {
                case "d": p.setColor(NTheme.accent); g.drawCircle(cx, cy, 4.5f * d, p); break;
                case "ln": p.setColor(NTheme.accent); g.drawRoundRect(0, cy - 2.5f * d, w, cy + 2.5f * d, 2.5f * d, 2.5f * d, p); break;
                case "x": p.setColor(NTheme.alpha(NTheme.text, .22f)); g.drawCircle(cx, cy, 3 * d, p); break;
                case "s": p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.4f * d); p.setColor(NTheme.accent); p.setPathEffect(new DashPathEffect(new float[]{3 * d, 2 * d}, 0)); g.drawCircle(cx, cy, 4 * d, p); break;
                case "sl": p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.6f * d); p.setColor(0xFFFF7A7A); g.drawLine(cx - 3.5f * d, cy - 3.5f * d, cx + 3.5f * d, cy + 3.5f * d, p); g.drawLine(cx + 3.5f * d, cy - 3.5f * d, cx - 3.5f * d, cy + 3.5f * d, p); break;
                default: p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2 * d); p.setColor(NTheme.accent); p.setPathEffect(new DashPathEffect(new float[]{3 * d, 2.5f * d}, 0)); g.drawCircle(cx, cy, 4.5f * d, p);
            }
        }
    }
}
