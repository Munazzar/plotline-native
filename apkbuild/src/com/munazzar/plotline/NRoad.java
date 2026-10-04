package com.munazzar.plotline;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import org.json.JSONObject;

/* The web's flowing timeline ("road"): one wavy track per goal across time, with the steps as planets on it,
   the ship at today's progress, a NOW line, month ticks and chapters. Drawn in dp on one canvas. */
final class NRoad extends View {
    interface Cb { void toggle(String gid); void step(JSONObject g, JSONObject s); void open(JSONObject g); void chapter(String id); }

    static final float FLW = 320, GUT = 16, AX = 44;
    final NStore st; final List<JSONObject> goals; final float ppd; final boolean single; final Set<String> open; final boolean openAll; final boolean chapters; final Cb cb;
    final int tn; int x0; float W; float[] top; float[] hh; float total;
    float reveal = 1f; android.animation.ValueAnimator anim;
    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); final TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    final List<Object[]> hits = new ArrayList<>();   /* {RectF(dp), kind, gid, sid} */
    final float den;
    float todayX;

    NRoad(Context c, NStore st, List<JSONObject> goals, float ppd, boolean openAll, Set<String> open, boolean single, boolean chapters, Cb cb) {
        super(c);
        this.st = st; this.goals = goals; this.ppd = ppd; this.openAll = openAll; this.open = open; this.single = single; this.chapters = chapters; this.cb = cb;
        den = c.getResources().getDisplayMetrics().density; tn = NDates.today();
        int lo = tn - 14, hi = tn + 60;
        for (JSONObject g : goals) { int[] sp = span(g); lo = Math.min(lo, sp[0] - 7); hi = Math.max(hi, sp[1] + 24); }
        Calendar k = Calendar.getInstance(TimeZone.getTimeZone("UTC")); k.setTimeInMillis(lo * 86400000L); k.set(Calendar.DAY_OF_MONTH, 1);
        x0 = (int) Math.round(k.getTimeInMillis() / 86400000.0); W = Math.round((hi - x0) * ppd);
        top = new float[goals.size()]; hh = new float[goals.size()]; float y = AX;
        for (int i = 0; i < goals.size(); i++) { boolean o = isOpen(goals.get(i)); hh[i] = single ? (o ? 250 : 120) : (o ? 264 : 98); top[i] = y; y += hh[i]; }
        total = y + 22; todayX = GUT + (tn - x0) * ppd;
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    static float ppdFit(List<JSONObject> L) {
        int tn = NDates.today(), lo = tn - 14, hi = tn + 45;
        for (JSONObject g : L) { int[] sp = span(g); lo = Math.min(lo, sp[0] - 7); hi = Math.max(hi, sp[1] + 21); }
        return Math.min(30f, Math.max(2.4f, 760f / (hi - lo)));
    }

    /* the goal first, then its sub-goals in tree order (web: [g, ...subTree(g)]) */
    static List<JSONObject> tree(NStore st, JSONObject g) {
        final List<JSONObject> out = new ArrayList<>(); List<JSONObject> q = new ArrayList<>(); q.add(g);
        while (!q.isEmpty() && out.size() < 500) { JSONObject x = q.remove(0); for (JSONObject k : NActs.kids(st, x)) if (!out.contains(k) && k != g) { out.add(k); q.add(k); } }
        java.util.Collections.sort(out, new java.util.Comparator<JSONObject>() { public int compare(JSONObject a, JSONObject b) { int d = span(a)[0] - span(b)[0]; return d != 0 ? d : a.optInt("priority", 2) - b.optInt("priority", 2); } });
        Set<String> ids = new java.util.HashSet<>(); for (JSONObject x : out) ids.add(x.optString("id"));
        List<JSONObject> res = new ArrayList<>(); res.add(g); Set<String> seen = new java.util.HashSet<>();
        for (JSONObject x : out) if (!ids.contains(x.optString("parent"))) walk(x, out, res, seen);
        for (JSONObject x : out) walk(x, out, res, seen);
        return res;
    }
    static void walk(JSONObject g, List<JSONObject> list, List<JSONObject> res, Set<String> seen) {
        if (!seen.add(g.optString("id"))) return; res.add(g);
        for (JSONObject x : list) if (g.optString("id").equals(x.optString("parent"))) walk(x, list, res, seen);
    }

    float firstX() { return goals.isEmpty() ? 0 : (span(goals.get(0))[0] - x0) * ppd; }

    boolean isOpen(JSONObject g) { return openAll || open.contains(g.optString("id")); }

    static final java.util.Map<String, Integer> HZ = new java.util.HashMap<>();
    static { HZ.put("week", 7); HZ.put("month", 30); HZ.put("quarter", 90); HZ.put("year", 365); HZ.put("multi", 1095); HZ.put("life", 3650); }

    /* gSpan: {start, end} in day numbers */
    static int[] span(JSONObject g) {
        String sd = g.optString("startDate");
        int s = NDates.valid(sd) ? NDates.dnum(sd) : NDates.dnum(NDates.ymd(g.optLong("createdAt", System.currentTimeMillis())));
        int mnDue = Integer.MAX_VALUE, mxDue = Integer.MIN_VALUE;
        org.json.JSONArray ar = g.optJSONArray("steps");
        for (int i = 0; ar != null && i < ar.length(); i++) { JSONObject x = ar.optJSONObject(i); if (x != null && NDates.valid(x.optString("due"))) { int d = NDates.dnum(x.optString("due")); mnDue = Math.min(mnDue, d); mxDue = Math.max(mxDue, d); } }
        Integer hz = HZ.get(g.optString("horizon", "month")); int e = NDates.valid(g.optString("targetDate")) ? NDates.dnum(g.optString("targetDate")) : (mxDue != Integer.MIN_VALUE ? mxDue : s + (hz == null ? 30 : hz));
        if (mxDue != Integer.MIN_VALUE) e = Math.max(e, mxDue);
        return new int[]{mnDue != Integer.MAX_VALUE ? Math.min(s, mnDue) : s, Math.max(e, s + 1)};
    }

    float[] stepXs(JSONObject g, int[] sp) {
        org.json.JSONArray ar = g.optJSONArray("steps"); int n = ar == null ? 0 : ar.length(); float[] X = new float[n];
        for (int i = 0; i < n; i++) { JSONObject x = ar.optJSONObject(i); X[i] = x != null && NDates.valid(x.optString("due")) ? NDates.dnum(x.optString("due")) : sp[0] + (i + 1f) / (n + 1) * (sp[1] - sp[0]); }
        return X;
    }

    static float flY(float x, float xs, float xe, float yc, float A, float ph) {
        float env = Math.max(0, Math.min(1, Math.min((x - xs) / 80f, (xe - x) / 80f)));
        return yc + A * env * (float) Math.sin((x - xs) / FLW * 2 * Math.PI + ph);
    }

    Path flPath(float a, float b, float xs, float xe, float yc, float A, float ph) {
        if (b < a) b = a; Path q = new Path(); float step = Math.max(3, Math.min(9, (b - a) / 30)); boolean first = true;
        for (float x = a; ; x += step) { if (x > b) x = b; float y = flY(x, xs, xe, yc, A, ph); if (first) { q.moveTo(x, y); first = false; } else q.lineTo(x, y); if (x >= b) break; }
        return q;
    }

    void play() {
        if (anim != null) anim.cancel();
        if ("reduced".equals(st.settings().optString("motion"))) { reveal = 1f; return; }
        reveal = 0f;
        anim = android.animation.ValueAnimator.ofFloat(0f, 1f); anim.setDuration(1900 + 120L * goals.size());
        anim.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() { public void onAnimationUpdate(android.animation.ValueAnimator a) { reveal = (Float) a.getAnimatedValue(); invalidate(); } });
        anim.start();
    }

    @Override protected void onMeasure(int w, int h) { setMeasuredDimension(Math.round((W + GUT * 2) * den), Math.round(total * den)); }

    float lane(int i) { float t = reveal * (1900 + 120f * goals.size()) - 120f * i; float f = Math.max(0, Math.min(1, t / 1700f)); return 1 - (float) Math.pow(1 - f, 3); }

    @Override protected void onDraw(Canvas cv) {
        hits.clear();
        cv.scale(den, den);
        int hsvX = 0; if (getParent() instanceof android.widget.HorizontalScrollView) hsvX = ((android.widget.HorizontalScrollView) getParent()).getScrollX();
        float sx = hsvX / den;
        axis(cv);
        if (chapters) chapterBands(cv, sx);
        for (int i = 0; i < goals.size(); i++) laneDraw(cv, i, sx);
        todayLine(cv);
        /* the name pills sit above the NOW line, like the web's HTML labels over the SVG */
        for (int i = 0; i < goals.size(); i++) {
            JSONObject g = goals.get(i); if (single && goals.size() <= 1) break;
            cv.save(); cv.translate(0, top[i]);
            chip(cv, g, i, sx, span(g), NActs.pct(st, g) / 100f, isOpen(g), NTheme.areaCol(g.optString("area")), top[i]);
            cv.restore();
        }
        if (reveal < 1f) postInvalidateOnAnimation();
    }

    void axis(Canvas cv) {
        tp.setTypeface(NFont.mono(500)); tp.setTextSize(10.5f * 1f); tp.setLetterSpacing(.08f);
        Calendar d = Calendar.getInstance(TimeZone.getTimeZone("UTC")); d.setTimeInMillis(x0 * 86400000L);
        for (int k = 0; k < 400; k++) {
            int n = (int) Math.round(d.getTimeInMillis() / 86400000.0); if (n > x0 + W / ppd) break;
            int m = d.get(Calendar.MONTH);
            if (ppd >= 2.5 || m % 3 == 0 || k == 0) {
                float x = GUT + (n - x0) * ppd; boolean yr = m == 0;
                String s = NDates.MON[m].toUpperCase(Locale.US) + (m == 0 || k == 0 ? " " + d.get(Calendar.YEAR) : "");
                tp.setColor(yr ? NTheme.text : NTheme.muted); cv.drawText(s, x + 10, 12 + 12, tp);
                p.setStyle(Paint.Style.FILL); p.setColor(NTheme.dim); cv.drawCircle(x + 2, 12 + 7, 2, p);
            }
            d.set(Calendar.DAY_OF_MONTH, 1); d.add(Calendar.MONTH, 1);
        }
        if (ppd >= 14) {
            tp.setTextSize(9.5f); tp.setColor(NTheme.dim);
            for (int n = x0; n <= x0 + W / ppd; n++) { Calendar c2 = Calendar.getInstance(TimeZone.getTimeZone("UTC")); c2.setTimeInMillis(n * 86400000L); if (c2.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY && c2.get(Calendar.DAY_OF_MONTH) > 4) { String s = String.valueOf(c2.get(Calendar.DAY_OF_MONTH)); float x = GUT + (n - x0) * ppd; cv.drawText(s, x - tp.measureText(s) / 2, 14 + 10, tp); } }
        }
        tp.setLetterSpacing(0);
    }

    void chapterBands(Canvas cv, float sx) {
        org.json.JSONArray ar = st.arr("chapters"); if (ar == null) return;
        for (int i = 0; i < ar.length(); i++) {
            JSONObject c = ar.optJSONObject(i); if (c == null || !NDates.valid(c.optString("start"))) continue;
            int s = Math.max(x0, NDates.dnum(c.optString("start"))), e = NDates.valid(c.optString("end")) ? NDates.dnum(c.optString("end")) : tn;
            if (e < x0) continue; float l = GUT + (s - x0) * ppd, w = Math.max(6, (e - s + 1) * ppd); if (l > W + GUT) continue;
            int col = NUi.col(c.optString("color"), 0xFF46C99B);
            p.setShader(new LinearGradient(0, 30, 0, total, NTheme.alpha(col, .16f), NTheme.alpha(col, .04f), Shader.TileMode.CLAMP)); p.setStyle(Paint.Style.FILL); cv.drawRect(l, 30, l + w, total, p); p.setShader(null);
            p.setColor(NTheme.alpha(col, .45f)); p.setStrokeWidth(1); cv.drawLine(l, 30, l, total, p);
            String lb = (c.optString("emoji").isEmpty() ? "" : c.optString("emoji") + " ") + c.optString("title") + (NDates.valid(c.optString("end")) ? "" : " · now");
            tp.setTypeface(NFont.body(600)); tp.setTextSize(11); tp.setColor(NTheme.text);
            float tw = Math.min(tp.measureText(lb), w - 28), px = Math.max(l + 8, Math.min(sx + 8, l + w - tw - 26));
            RectF r = new RectF(px, 36, px + tw + 18, 56); p.setColor(NUi.mix(col, .22f, NTheme.surface)); cv.drawRoundRect(r, 10, 10, p);
            cv.save(); cv.clipRect(r); cv.drawText(android.text.TextUtils.ellipsize(lb, tp, tw, android.text.TextUtils.TruncateAt.END).toString(), px + 9, 50, tp); cv.restore();
            hits.add(new Object[]{new RectF(l, 30, l + w, total), "chap", c.optString("id"), ""});
        }
    }

    void laneDraw(Canvas cv, int i, float sx) {
        JSONObject g = goals.get(i); boolean open = isOpen(g); float H = hh[i], y0 = top[i];
        int col = NTheme.areaCol(g.optString("area")); int[] sp = span(g); float pr = NActs.pct(st, g) / 100f;
        float xs = GUT + (sp[0] - x0) * ppd, w = Math.max(8, (sp[1] - sp[0]) * ppd), xe = xs + w, xp = xs + w * pr;
        float yc = single ? H / 2 : (open ? 152 : 66), A = open ? (single ? 30 : 24) : 11, ph = (float) ((i * 1.9 + .6) % (2 * Math.PI));
        float f = lane(i);
        cv.save(); cv.translate(0, y0);
        /* track */
        Path base = flPath(xs, xe, xs, xe, yc, A, ph);
        p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND); p.setPathEffect(null);
        boolean hal = NTheme.haloOn(g, false);
        p.setStrokeWidth(5); p.setColor(NTheme.alpha(col, hal ? .3f : .2f));
        Path drawn = base; if (f < 1f) { PathMeasure pm = new PathMeasure(base, false); drawn = new Path(); pm.getSegment(0, pm.getLength() * f, drawn, true); }
        cv.drawPath(drawn, p);
        if (pr > 0) {
            Path pg = flPath(xs, xp, xs, xe, yc, A, ph); if (f < 1f) { PathMeasure pm = new PathMeasure(pg, false); Path q = new Path(); pm.getSegment(0, pm.getLength() * f, q, true); pg = q; }
            if (hal) { p.setStrokeWidth(NTheme.haloBright ? 40 : 26); p.setColor(NTheme.alpha(col, .08f)); cv.drawPath(pg, p); p.setStrokeWidth(NTheme.haloBright ? 24 : 16); p.setColor(NTheme.alpha(col, .14f)); cv.drawPath(pg, p); }
            p.setStrokeWidth(12); p.setColor(NTheme.alpha(col, .18f)); cv.drawPath(pg, p);
            p.setShader(new LinearGradient(xs, 0, Math.max(xp, xs + 1), 0, NTheme.alpha(col, .3f), col, Shader.TileMode.CLAMP)); p.setStrokeWidth(5); cv.drawPath(pg, p); p.setShader(null);
        }
        if (!single) hits.add(new Object[]{new RectF(xs - 14, 0, xe + 14, H), "lane", g.optString("id"), ""});
        float fx = xs + (xe - xs) * f;
        p.setStyle(Paint.Style.FILL);
        /* start dot */
        halo(cv, xs, flY(xs, xs, xe, yc, A, ph), 12, col);
        p.setColor(col); cv.drawCircle(xs, flY(xs, xs, xe, yc, A, ph), 6, p);
        /* steps */
        org.json.JSONArray ar = g.optJSONArray("steps"); int n = ar == null ? 0 : ar.length(); float[] X = stepXs(g, sp);
        int[] slot = new int[n];
        if (open) {
            float[] last = {-1e9f, -1e9f, -1e9f, -1e9f};
            Integer[] ord = new Integer[n]; for (int k = 0; k < n; k++) ord[k] = k;
            final float[] XX = X; java.util.Arrays.sort(ord, new java.util.Comparator<Integer>() { public int compare(Integer a, Integer b) { return Float.compare(XX[a], XX[b]); } });
            for (int k : ord) {
                float px = (X[k] - x0) * ppd; int q = -1; for (int t = 0; t < 4; t++) if (px - last[t] >= 142) { q = t; break; }
                if (q < 0) { q = 0; for (int t = 1; t < 4; t++) if (last[t] < last[q]) q = t; }
                slot[k] = q; last[q] = px;
            }
        }
        for (int k = 0; k < n; k++) {
            JSONObject s = ar.optJSONObject(k); if (s == null) continue;
            float px = GUT + (X[k] - x0) * ppd; if (px > fx + 2 && f < 1f) continue;
            float py = flY(px, xs, xe, yc, A, ph); boolean dn = s.optBoolean("done"), has = NDates.valid(s.optString("due")), over = !dn && has && NDates.dnum(s.optString("due")) < tn;
            if (open) { spl(cv, s, px, py, slot[k], dn, has, over, col); hits.add(new Object[]{new RectF(px - 22, py - 22, px + 22, py + 22), "step", g.optString("id"), s.optString("id")}); }
            else {
                p.setStyle(Paint.Style.FILL); p.setColor(dn ? col : NTheme.bg); cv.drawCircle(px, py, 3.5f, p);
                if (!dn) { p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.5f); p.setColor(col); cv.drawCircle(px, py, 4.2f, p); p.setStyle(Paint.Style.FILL); }
            }
        }
        /* end ring */
        if (f > .98f) {
            float ey = flY(xe, xs, xe, yc, A, ph);
            halo(cv, xe, ey, 18, col); p.setStyle(Paint.Style.FILL); p.setColor(NTheme.bg); cv.drawCircle(xe, ey, 9, p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3); p.setColor(col); cv.drawCircle(xe, ey, 7.5f, p); p.setStyle(Paint.Style.FILL);
        }
        /* ship */
        if ("active".equals(g.optString("status")) && f > .6f) {
            float shy = flY(xp, xs, xe, yc, A, ph);
            halo(cv, xp, shy, 24, col); p.setStyle(Paint.Style.FILL); p.setColor(NTheme.alpha(col, .45f)); cv.drawCircle(xp, shy, 10, p);
            p.setColor(0xFFFFFFFF); cv.drawCircle(xp, shy, 6, p);
        }
        cv.restore();
    }

    void halo(Canvas cv, float x, float y, float r, int col) {
        p.setStyle(Paint.Style.FILL); p.setShader(new RadialGradient(x, y, r * 1.1f, NTheme.alpha(col, .5f), NTheme.alpha(col, 0f), Shader.TileMode.CLAMP)); cv.drawCircle(x, y, r * 1.1f, p); p.setShader(null);
    }

    void spl(Canvas cv, JSONObject s, float px, float py, int slot, boolean done, boolean has, boolean over, int col) {
        boolean up = slot == 0 || slot == 2, far = slot >= 2;
        if (far) { p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1); p.setColor(NTheme.line2); if (up) cv.drawLine(px, py - 10, px, py - 54, p); else cv.drawLine(px, py + 10, px, py + 54, p); }
        p.setStyle(Paint.Style.FILL);
        if (done) { halo(cv, px, py, 18, col); p.setShader(new RadialGradient(px - 2, py - 3, 10, 0xFFFFFFFF, col, Shader.TileMode.CLAMP)); cv.drawCircle(px, py, 9, p); p.setShader(null); }
        else {
            p.setColor(NTheme.bg2); cv.drawCircle(px, py, 9, p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2.5f); p.setColor(over ? 0xFFFF7A7A : col);
            if (!has) p.setPathEffect(new DashPathEffect(new float[]{4, 3}, 0)); cv.drawCircle(px, py, 7.75f, p); p.setPathEffect(null); p.setStyle(Paint.Style.FILL);
        }
        tp.setTypeface(NFont.body(600)); tp.setTextSize(12); tp.setColor(done ? NTheme.muted : NTheme.text); tp.setLetterSpacing(0);
        String t = s.optString("title"); if (t.length() > 38) t = t.substring(0, 37) + "…";
        StaticLayout sl = new StaticLayout(t, tp, 136, Layout.Alignment.ALIGN_CENTER, 1f, 0, false);
        TextPaint mp = new TextPaint(Paint.ANTI_ALIAS_FLAG); mp.setTypeface(NFont.mono(500)); mp.setTextSize(9.5f); mp.setColor(NTheme.muted); mp.setLetterSpacing(.04f);
        String em = done ? "DONE" : has ? (NDates.fmtDate(s.optString("due")) + (s.optString("time").isEmpty() ? "" : " · " + NDates.fmtTime(s.optString("time")))).toUpperCase() : "NO DATE";
        float h = sl.getHeight() + 3 + 12;
        float oy = up ? py - (far ? 58 : 17) - h : py + (far ? 58 : 17);
        cv.save(); cv.translate(px - 68, oy); sl.draw(cv); cv.translate(0, sl.getHeight() + 3); float ew = mp.measureText(em); cv.drawText(em, (136 - ew) / 2, 10, mp); cv.restore();
    }

    void chip(Canvas cv, JSONObject g, int i, float sx, int[] sp, float pr, boolean open, int col, float y0) {
        String title = g.optString("title").toUpperCase(Locale.getDefault());
        tp.setTypeface(NFont.display(700)); tp.setTextSize(15); tp.setLetterSpacing(.02f); tp.setColor(NTheme.text);
        float maxT = Math.min(340, getResources().getConfiguration().screenWidthDp * .74f) - 7 - 28 - 10 - 14 - (open ? 44 : 0);
        String t = android.text.TextUtils.ellipsize(title, tp, maxT, android.text.TextUtils.TruncateAt.END).toString();
        String sub = NActs.pct(st, g) + "% · " + NDates.fmtDate(NDates.fromN(sp[0])) + " → " + (NDates.valid(g.optString("targetDate")) ? NDates.fmtDate(g.optString("targetDate")) : NDates.fmtDate(NDates.fromN(sp[1])));
        TextPaint mp = new TextPaint(Paint.ANTI_ALIAS_FLAG); mp.setTypeface(NFont.mono(500)); mp.setTextSize(9.5f); mp.setColor(NTheme.muted); mp.setLetterSpacing(.05f);
        float w = Math.max(tp.measureText(t), mp.measureText(sub.toUpperCase())) + 7 + 28 + 10 + 14 + (open ? 44 : 0);
        float cx = sx + GUT, cy = 6; RectF r = new RectF(cx, cy, cx + Math.min(w, maxT + 80), cy + 40);
        p.setStyle(Paint.Style.FILL); p.setColor(NTheme.alpha(NTheme.bg, .9f)); cv.drawRoundRect(r, 20, 20, p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1); p.setColor(open ? NTheme.alpha(col, .45f) : NTheme.line); cv.drawRoundRect(r, 20, 20, p);
        float rcx = cx + 7 + 14, rcy = cy + 20; RectF ar = new RectF(rcx - 10, rcy - 10, rcx + 10, rcy + 10);
        p.setStrokeWidth(3.5f); p.setStrokeCap(Paint.Cap.ROUND); p.setColor(NTheme.alpha(col, .22f)); cv.drawCircle(rcx, rcy, 10, p);
        p.setColor(col); cv.drawArc(ar, -90, 360 * pr, false, p);
        p.setStyle(Paint.Style.FILL);
        float tx = cx + 7 + 28 + 10; cv.drawText(t, tx, cy + 19, tp);
        mp.setTextSize(9.5f); cv.drawText(sub.toUpperCase(), tx, cy + 32, mp);
        if (open) { tp.setTypeface(NFont.body(600)); tp.setTextSize(12.5f); tp.setLetterSpacing(0); tp.setColor(NTheme.accent); cv.drawText("Open", r.right - 44, cy + 24, tp); hits.add(new Object[]{new RectF(r.right - 56, cy, r.right, cy + 40), "open", g.optString("id"), ""}); }
        hits.add(0, new Object[]{new RectF(r.left, y0 + r.top - y0, r.right, r.bottom), "chip", g.optString("id"), ""});
        tp.setLetterSpacing(0);
    }

    void todayLine(Canvas cv) {
        float x = todayX;
        p.setStyle(Paint.Style.FILL); p.setShader(new LinearGradient(0, 36, 0, total - 14, new int[]{NTheme.accent, NTheme.alpha(NTheme.accent, .35f), NTheme.alpha(NTheme.accent, 0f)}, new float[]{0, .65f, 1}, Shader.TileMode.CLAMP));
        cv.drawRect(x - 1, 36, x + 1, total - 14, p); p.setShader(null);
        tp.setTypeface(NFont.mono(500)); tp.setTextSize(10); tp.setLetterSpacing(.08f); float tw = tp.measureText("NOW");
        RectF r = new RectF(x - tw / 2 - 8, 36 - 28, x + tw / 2 + 8, 36 - 28 + 20); p.setColor(NTheme.accent); cv.drawRoundRect(r, 10, 10, p);
        tp.setColor(NTheme.onAccent); cv.drawText("NOW", x - tw / 2, r.top + 14, tp); tp.setLetterSpacing(0);
    }

    float dx, dy; boolean moved;
    @Override public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: dx = e.getX(); dy = e.getY(); moved = false; return true;
            case MotionEvent.ACTION_MOVE: if (Math.abs(e.getX() - dx) > 12 * den || Math.abs(e.getY() - dy) > 12 * den) moved = true; return false;
            case MotionEvent.ACTION_UP:
                if (moved) return false;
                float x = e.getX() / den, y = e.getY() / den;
                /* inner lane coordinates are relative to the lane top; hits are stored in lane space except chapters (page space) */
                for (Object[] h : hits) {
                    RectF r = (RectF) h[0]; String kind = (String) h[1], gid = (String) h[2];
                    if (kind.equals("chap")) { if (r.contains(x, y)) { cb.chapter(gid); return true; } continue; }
                    int li = -1; for (int i = 0; i < goals.size(); i++) if (goals.get(i).optString("id").equals(gid)) li = i;
                    if (li < 0) continue; float ly = y - top[li];
                    if (r.contains(x, ly)) {
                        JSONObject g = goals.get(li);
                        if (kind.equals("step")) { JSONObject s = null; org.json.JSONArray sa = g.optJSONArray("steps"); if (sa != null) for (int q = 0; q < sa.length(); q++) { JSONObject sq = sa.optJSONObject(q); if (sq != null && sq.optString("id").equals((String) h[3])) { s = sq; break; } } if (s != null) cb.step(g, s); }
                        else if (kind.equals("open")) cb.open(g);
                        else cb.toggle(gid);
                        return true;
                    }
                }
                return true;
        }
        return true;
    }
}
