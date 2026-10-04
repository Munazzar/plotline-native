package com.munazzar.plotline;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/* The web's Life Map: you in the middle, areas around you, goals around their area. Drag nodes, pan, pinch to zoom.
   World units are dp, exactly like the web's SVG px. */
final class NMapView extends View {
    interface Cb { void select(String id); void open(String id); void persisted(); }

    final NStore st; final Cb cb; final float den;
    final List<JSONObject> goals = new ArrayList<>();
    final Map<String, float[]> P = new LinkedHashMap<>(), AP = new LinkedHashMap<>();
    final List<String> areaIds = new ArrayList<>(), areaNames = new ArrayList<>();
    float sp = 1f;
    float tx, ty, k = 1f;
    String sel;
    float grow = 1f; android.animation.ValueAnimator anim;
    Map<String, float[]> P0, A0;
    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); final TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    final Path path = new Path();
    boolean laidOut, fitted;

    /* touch */
    float lx, ly, mx, my; boolean dragging; String dragKind, dragId; float moved; long lastTap; String lastTapId;
    boolean pinch; float d0, k0;

    NMapView(Context c, NStore st, Cb cb) {
        super(c); this.st = st; this.cb = cb; den = c.getResources().getDisplayMetrics().density;
        layout(); relax(null, 90);
    }

    static float nodeR(JSONObject g) { return 15 + (4 - g.optInt("priority", 2)) * 5; }

    JSONObject goal(String id) { for (JSONObject g : goals) if (g.optString("id").equals(id)) return g; return null; }
    static String areaOf(JSONObject g) { return NTheme.areaKey(g.optString("area")); }

    void layout() {
        goals.clear(); P.clear(); AP.clear(); areaIds.clear(); areaNames.clear();
        for (JSONObject g : NStore.list(st.arr("goals"))) goals.add(g);
        JSONObject set = st.settings();
        sp = (float) set.optDouble("mapSpace", 1);
        if (sp <= 0) sp = 1;
        JSONObject ap = set.optJSONObject("areaPos"); if (ap == null) ap = new JSONObject();
        for (String id : NGen.AREA_ID) { for (JSONObject g : goals) if (areaOf(g).equals(id)) { areaIds.add(id); areaNames.add(NTheme.areaName(id)); break; } }
        for (JSONObject g : goals) { String a = areaOf(g); if (!areaIds.contains(a)) { areaIds.add(a); areaNames.add(NTheme.areaName(a)); } }
        int na = areaIds.size();
        for (int i = 0; i < na; i++) {
            String aid = areaIds.get(i);
            double ang = -Math.PI / 2 + i * 2 * Math.PI / na;
            JSONObject o = ap.optJSONObject(aid);
            float[] A = o != null ? new float[]{(float) o.optDouble("x") * sp, (float) o.optDouble("y") * sp} : new float[]{(float) Math.cos(ang) * 260 * sp, (float) Math.sin(ang) * 260 * sp};
            AP.put(aid, A);
            List<JSONObject> gs = new ArrayList<>();
            for (JSONObject g : goals) if (areaOf(g).equals(aid)) gs.add(g);
            java.util.Collections.sort(gs, new java.util.Comparator<JSONObject>() { public int compare(JSONObject a, JSONObject b) { return a.optInt("priority", 2) - b.optInt("priority", 2); } });
            int n = gs.size(); float R2 = (130 + Math.max(0, n - 4) * 18) * sp; double spread = Math.min(Math.PI * 1.4, .62 * (n - 1));
            for (int j = 0; j < n; j++) {
                JSONObject x = gs.get(j); double t = n == 1 ? 0 : (j / (double) (n - 1) - .5) * spread;
                JSONObject mp = x.optJSONObject("mapPos");
                float dx = mp != null ? (float) mp.optDouble("dx") * sp : (float) Math.cos(ang + t) * R2, dy = mp != null ? (float) mp.optDouble("dy") * sp : (float) Math.sin(ang + t) * R2;
                P.put(x.optString("id"), new float[]{A[0] + dx, A[1] + dy});
            }
        }
        laidOut = true;
    }

    void relax(String fixed, int iters) {
        List<String> ids = new ArrayList<>(P.keySet()); Map<String, Float> R = new HashMap<>();
        for (String id : ids) { JSONObject g = goal(id); R.put(id, g != null ? nodeR(g) + 30 * sp : 40f); }
        List<float[]> obs = new ArrayList<>(); obs.add(new float[]{0, 0, 62});
        for (float[] a : AP.values()) obs.add(new float[]{a[0], a[1], 44});
        for (int it = 0; it < iters; it++) {
            int mv = 0;
            for (int a = 0; a < ids.size(); a++) {
                float[] pa = P.get(ids.get(a));
                for (int b = a + 1; b < ids.size(); b++) {
                    float[] q = P.get(ids.get(b)); float dx = q[0] - pa[0], dy = q[1] - pa[1];
                    float ex = dx / 1.7f, d = (float) Math.hypot(ex, dy); if (d == 0) d = .01f; float min = R.get(ids.get(a)) + R.get(ids.get(b));
                    if (d < min) {
                        float push = (min - d) / 2, ux = ex / d * 1.7f, uy = dy / d;
                        boolean fa = ids.get(a).equals(fixed), fb = ids.get(b).equals(fixed);
                        int ka = fa ? 0 : fb ? 2 : 1, kb = fb ? 0 : fa ? 2 : 1;
                        pa[0] -= ux * push * ka; pa[1] -= uy * push * ka; q[0] += ux * push * kb; q[1] += uy * push * kb; mv++;
                    }
                }
                if (!ids.get(a).equals(fixed)) for (float[] o : obs) {
                    float dx = pa[0] - o[0], dy = pa[1] - o[1], d = (float) Math.hypot(dx, dy); if (d == 0) d = .01f; float min = o[2] + R.get(ids.get(a)) * .8f;
                    if (d < min) { pa[0] += dx / d * (min - d); pa[1] += dy / d * (min - d); mv++; }
                }
            }
            if (mv == 0) break;
        }
    }

    /* ---- public controls ---- */
    void setSelected(String id) { sel = id; invalidate(); }

    void rebuild(boolean refit) { layout(); relax(null, 90); if (sel != null && goal(sel) == null) sel = null; if (refit) fit(false); invalidate(); }

    void reset() {
        for (JSONObject g : goals) g.remove("mapPos");
        st.settings().remove("areaPos");
        rebuild(true);
    }

    void zoomAt(float cx, float cy, float f) { float nk = Math.min(3f, Math.max(.25f, k * f)); f = nk / k; tx = cx - (cx - tx) * f; ty = cy - (cy - ty) * f; k = nk; }
    void zoomBy(float f) { zoomAt(getWidth() / den / 2f, getHeight() / den / 2f, f); invalidate(); }

    void fit(boolean animate) {
        float w = getWidth() / den, h = getHeight() / den; if (w <= 0) return;
        float x0 = -60, x1 = 60, y0 = -60, y1 = 60;
        for (JSONObject g : goals) { float[] q = P.get(g.optString("id")); if (q == null) continue; float r = nodeR(g) + 30; x0 = Math.min(x0, q[0] - Math.max(r, 70)); x1 = Math.max(x1, q[0] + Math.max(r, 70)); y0 = Math.min(y0, q[1] - r); y1 = Math.max(y1, q[1] + r + 18); }
        for (float[] a : AP.values()) { x0 = Math.min(x0, a[0] - 60); x1 = Math.max(x1, a[0] + 60); y0 = Math.min(y0, a[1] - 50); y1 = Math.max(y1, a[1] + 50); }
        final float bw = x1 - x0, bh = y1 - y0;
        final float nk = Math.min(Math.min(w / (bw + 50), (h - 70) / (bh + 50)), 1.3f);
        final float ntx = w / 2 - (x0 + bw / 2) * nk, nty = (h - 56) / 2 - (y0 + bh / 2) * nk;
        if (!animate) { k = nk; tx = ntx; ty = nty; invalidate(); return; }
        final float sk = k, stx = tx, sty = ty;
        android.animation.ValueAnimator va = android.animation.ValueAnimator.ofFloat(0f, 1f); va.setDuration(reduced() ? 0 : 700); va.setInterpolator(NUi.EASE);
        va.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() { public void onAnimationUpdate(android.animation.ValueAnimator a) { float t = (Float) a.getAnimatedValue(); k = sk + (nk - sk) * t; tx = stx + (ntx - stx) * t; ty = sty + (nty - sty) * t; invalidate(); } });
        va.start();
    }

    void focusOn(String id) {
        float[] q = P.get(id); if (q == null) return;
        final float nk = Math.max(k, 1f), h = getHeight() / den, w = getWidth() / den;
        final float ntx = w / 2 - q[0] * nk, nty = h * .38f - q[1] * nk, sk = k, stx = tx, sty = ty;
        android.animation.ValueAnimator va = android.animation.ValueAnimator.ofFloat(0f, 1f); va.setDuration(reduced() ? 0 : 700); va.setInterpolator(NUi.EASE);
        va.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() { public void onAnimationUpdate(android.animation.ValueAnimator a) { float t = (Float) a.getAnimatedValue(); k = sk + (nk - sk) * t; tx = stx + (ntx - stx) * t; ty = sty + (nty - sty) * t; invalidate(); } });
        va.start();
    }

    /* opening animation: areas unfold from you, then goals grow out of their area */
    void play() {
        if (reduced()) { grow = 1f; return; }
        P0 = new HashMap<>(); A0 = new HashMap<>();
        for (Map.Entry<String, float[]> e : P.entrySet()) P0.put(e.getKey(), e.getValue().clone());
        for (Map.Entry<String, float[]> e : AP.entrySet()) A0.put(e.getKey(), e.getValue().clone());
        if (anim != null) anim.cancel();
        grow = 0f; anim = android.animation.ValueAnimator.ofFloat(0f, 1f); anim.setDuration(1700);
        anim.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() { public void onAnimationUpdate(android.animation.ValueAnimator a) { grow = (Float) a.getAnimatedValue(); invalidate(); } });
        anim.addListener(new android.animation.AnimatorListenerAdapter() { public void onAnimationEnd(android.animation.Animator a) { grow = 1f; invalidate(); } });
        anim.start();
    }

    boolean reduced() { return "reduced".equals(st.settings().optString("motion")); }
    static float ez(float x) { x = Math.min(1f, Math.max(0f, x)); return 1f - (float) Math.pow(1 - x, 3); }

    /* positions while growing */
    float[] apos(String id) {
        float[] a = AP.get(id); if (a == null) return null;
        if (grow >= 1f) return a;
        float ea = ez(grow / .5f); return new float[]{a[0] * ea, a[1] * ea};
    }
    float[] gpos(JSONObject g) {
        float[] q = P.get(g.optString("id")); if (q == null) return null;
        if (grow >= 1f) return q;
        float eg = ez((grow - .22f) / .78f); float[] a0 = AP.get(areaOf(g)), a = apos(areaOf(g));
        return a0 != null && a != null ? new float[]{a[0] + (q[0] - a0[0]) * eg, a[1] + (q[1] - a0[1]) * eg} : new float[]{q[0] * eg, q[1] * eg};
    }

    /* ---- drawing ---- */
    @Override protected void onSizeChanged(int w, int h, int ow, int oh) { super.onSizeChanged(w, h, ow, oh); if (!fitted && w > 0) { fitted = true; fit(false); play(); } }

    @Override protected void onDraw(Canvas cv) {
        float w = getWidth(), h = getHeight();
        p.setStyle(Paint.Style.FILL); p.setShader(new RadialGradient(w * .5f, h * .46f, Math.max(w, h) * .62f, NTheme.alpha(NTheme.accent, .10f), 0, Shader.TileMode.CLAMP)); cv.drawRect(0, 0, w, h, p); p.setShader(null);
        cv.save(); cv.scale(den, den); cv.translate(tx, ty); cv.scale(k, k);
        Set<String> rel = null; JSONObject sg = sel == null ? null : goal(sel);
        if (sg != null) { rel = new HashSet<>(); rel.add(sel); JSONArray l = sg.optJSONArray("links"); for (int i = 0; l != null && i < l.length(); i++) rel.add(l.optString(i)); }
        final float a15 = grow >= 1f ? 1f : Math.min(1f, Math.max(0f, (grow - .6f) / .4f));
        /* area → you */
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1);
        for (String aid : areaIds) { float[] a = apos(aid); p.setColor(NTheme.alpha(NTheme.line2, rel != null ? .35f : 1f)); cv.drawLine(0, 0, a[0], a[1], p); }
        /* goal → area */
        for (JSONObject g : goals) {
            float[] a = apos(areaOf(g)), q = gpos(g); if (a == null || q == null) continue;
            int c = NTheme.areaCol(g.optString("area")); boolean dim = rel != null && !rel.contains(g.optString("id"));
            p.setStrokeWidth(1.5f); p.setColor(NTheme.alpha(c, (dim ? .07f : .55f)));
            path.reset(); path.moveTo(a[0], a[1]); path.quadTo((a[0] + q[0]) / 2 + (q[1] - a[1]) * .18f, (a[1] + q[1]) / 2 - (q[0] - a[0]) * .18f, q[0], q[1]); cv.drawPath(path, p);
        }
        /* links */
        Set<String> seen = new HashSet<>();
        p.setStrokeWidth(1.6f); p.setPathEffect(new DashPathEffect(new float[]{5, 6}, (System.currentTimeMillis() % 1400) / 1400f * -11));
        for (JSONObject g : goals) {
            JSONArray l = g.optJSONArray("links"); String gid = g.optString("id");
            for (int i = 0; l != null && i < l.length(); i++) {
                String o = l.optString(i); String[] pr = {gid, o}; java.util.Arrays.sort(pr); String key = pr[0] + "," + pr[1];
                JSONObject og = goal(o); if (seen.contains(key) || og == null) continue; seen.add(key);
                float[] a = gpos(g), b = gpos(og); if (a == null || b == null) continue;
                boolean on = rel == null || gid.equals(sel) || o.equals(sel);
                p.setColor(NTheme.alpha(NTheme.accent, on ? .8f : .07f));
                path.reset(); path.moveTo(a[0], a[1]); path.quadTo((a[0] + b[0]) * .25f, (a[1] + b[1]) * .25f, b[0], b[1]); cv.drawPath(path, p);
            }
        }
        p.setPathEffect(null);
        /* you */
        p.setStyle(Paint.Style.FILL); p.setColor(NTheme.alpha(NTheme.accent, .08f + .04f * (float) Math.sin(System.currentTimeMillis() / 800.0))); cv.drawCircle(0, 0, 50, p);
        p.setColor(NTheme.accent); cv.drawCircle(0, 0, 34, p);
        String nm = st.settings().optString("name", "").trim(); if (nm.isEmpty()) nm = "You"; if (nm.length() > 8) nm = nm.substring(0, 7) + "…";
        tp.setTypeface(NFont.display(800)); tp.setTextSize(15); tp.setColor(NTheme.onAccent); tp.setLetterSpacing(0);
        cv.drawText(nm.toUpperCase(), -tp.measureText(nm.toUpperCase()) / 2, 5.4f, tp);
        /* areas */
        for (int i = 0; i < areaIds.size(); i++) {
            String aid = areaIds.get(i); float[] a = apos(aid); int c = NTheme.areaCol(aid);
            float al = rel != null ? .18f : 1f;
            p.setStyle(Paint.Style.FILL); p.setColor(NTheme.alpha(c, .12f * al)); cv.drawCircle(a[0], a[1], 20, p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.5f); p.setColor(NTheme.alpha(c, al)); cv.drawCircle(a[0], a[1], 20, p);
            p.setStyle(Paint.Style.FILL); cv.drawCircle(a[0], a[1], 5, p);
            tp.setTypeface(NFont.mono(500)); tp.setTextSize(9.5f); tp.setLetterSpacing(.1f); tp.setColor(NTheme.alpha(c, al * a15));
            String nme = areaNames.get(i).toUpperCase(); cv.drawText(nme, a[0] - tp.measureText(nme) / 2, a[1] + (a[1] < 0 ? -32 : 40), tp);
        }
        /* goals */
        long now = System.currentTimeMillis();
        int hm = haloMode();
        for (JSONObject g : goals) {
            float[] q = gpos(g); if (q == null) continue;
            String id = g.optString("id"); int c = NTheme.areaCol(g.optString("area")); float r = nodeR(g); float pr = (float) NActs.prog(st, g, 0); boolean done = "done".equals(g.optString("status"));
            float al = rel != null && !rel.contains(id) ? .18f : 1f;
            p.setStyle(Paint.Style.STROKE);
            if (id.equals(sel)) { p.setStrokeWidth(2); p.setColor(NTheme.alpha(c, al)); cv.drawCircle(q[0], q[1], r + 12, p); }
            if (g.optBoolean("pinned")) { p.setStrokeWidth(1.2f); p.setColor(NTheme.alpha(c, .6f * al)); p.setPathEffect(new DashPathEffect(new float[]{2, 4}, 0)); cv.drawCircle(q[0], q[1], r + 10, p); p.setPathEffect(null); }
            p.setStrokeWidth(2.5f); p.setColor(NTheme.alpha(NTheme.line2, al)); cv.drawCircle(q[0], q[1], r + 5, p);
            p.setStrokeCap(Paint.Cap.ROUND); p.setColor(NTheme.alpha(c, al)); cv.drawArc(new RectF(q[0] - r - 5, q[1] - r - 5, q[0] + r + 5, q[1] + r + 5), -90, 360 * pr, false, p); p.setStrokeCap(Paint.Cap.BUTT);
            boolean halo = haloOn(g, hm);
            p.setStyle(Paint.Style.FILL);
            if (halo) { p.setColor(NTheme.alpha(c, .12f * al)); cv.drawCircle(q[0], q[1], r + 22, p); }
            p.setColor(NTheme.alpha(c, .22f * al)); cv.drawCircle(q[0], q[1], r + (halo ? (st.settings().optBoolean("haloBright") ? 8 : 5) : 1.5f), p);
            p.setColor(NTheme.alpha(c, al)); cv.drawCircle(q[0], q[1], r, p);
            int onc = NTheme.alpha(NTheme.on(c), al);
            if (done) {
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2.6f); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND); p.setColor(onc);
                path.reset(); path.moveTo(q[0] - r * .42f, q[1]); path.rLineTo(r * .3f, r * .3f); path.rLineTo(r * .52f, -r * .56f); cv.drawPath(path, p); p.setStrokeCap(Paint.Cap.BUTT); p.setStyle(Paint.Style.FILL);
            } else {
                tp.setTypeface(NFont.display(800)); tp.setTextSize(15); tp.setLetterSpacing(0); tp.setColor(onc);
                String t = String.valueOf(Math.round(pr * 100)); cv.drawText(t, q[0] - tp.measureText(t) / 2, q[1] + 5.4f, tp);
            }
            String lt = g.optString("title"); if (lt.length() > 26) lt = lt.substring(0, 25) + "…";
            tp.setTypeface(NFont.body(600)); tp.setTextSize(14); tp.setLetterSpacing(0);
            float lw = tp.measureText(lt), lx0 = q[0] - lw / 2, ly0 = q[1] + r + 26 + 5;
            tp.setColor(NTheme.alpha(NTheme.bg2, al * a15)); tp.setStyle(Paint.Style.STROKE); tp.setStrokeWidth(5); tp.setStrokeJoin(Paint.Join.ROUND); cv.drawText(lt, lx0, ly0, tp);
            tp.setStyle(Paint.Style.FILL); tp.setColor(NTheme.alpha(NTheme.text, al * a15)); cv.drawText(lt, lx0, ly0, tp);
        }
        cv.restore();
        if (!dragging && !pinch) postInvalidateOnAnimation();   /* links march, halo breathes */
    }

    int haloMode() { String m = st.settings().optString("halo", "none"); return m.equals("all") ? 1 : m.equals("high") ? 2 : m.equals("pinned") ? 3 : 0; }
    static boolean haloOn(JSONObject g, int m) { return m == 1 || (m == 2 && g.optInt("priority", 2) == 1) || (m == 3 && g.optBoolean("pinned")); }

    /* ---- touch ---- */
    float wx(float sx) { return (sx / den - tx) / k; }
    float wy(float sy) { return (sy / den - ty) / k; }

    String hitGoal(float sx, float sy) {
        float x = wx(sx), y = wy(sy); String best = null; float bd = 1e9f;
        for (JSONObject g : goals) { float[] q = gpos(g); if (q == null) continue; float d = (float) Math.hypot(x - q[0], y - q[1]); if (d <= nodeR(g) + 18 && d < bd) { bd = d; best = g.optString("id"); } }
        return best;
    }
    String hitArea(float sx, float sy) {
        float x = wx(sx), y = wy(sy);
        for (String a : areaIds) { float[] q = apos(a); if (Math.hypot(x - q[0], y - q[1]) <= 30) return a; }
        return null;
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                getParent().requestDisallowInterceptTouchEvent(true);
                if (anim != null && anim.isRunning()) anim.end();
                lx = e.getX(); ly = e.getY(); moved = 0; pinch = false;
                String g = hitGoal(lx, ly);
                if (g != null) { dragKind = "goal"; dragId = g; }
                else { String a = hitArea(lx, ly); if (a != null) { dragKind = "area"; dragId = a; } else { dragKind = null; dragId = null; } }
                dragging = true; return true;
            }
            case MotionEvent.ACTION_POINTER_DOWN: {
                if (e.getPointerCount() == 2) { pinch = true; dragKind = null; dragId = null; d0 = (float) Math.hypot(e.getX(0) - e.getX(1), e.getY(0) - e.getY(1)); if (d0 < 1) d0 = 1; k0 = k; }
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (pinch && e.getPointerCount() >= 2) {
                    float d = (float) Math.hypot(e.getX(0) - e.getX(1), e.getY(0) - e.getY(1));
                    float cx = (e.getX(0) + e.getX(1)) / 2 / den, cy = (e.getY(0) + e.getY(1)) / 2 / den;
                    zoomAt(cx, cy, k0 * d / d0 / k); moved += 10; invalidate(); return true;
                }
                if (pinch) return true;
                float dx = (e.getX() - lx) / den, dy = (e.getY() - ly) / den; lx = e.getX(); ly = e.getY();
                moved += Math.abs(dx) + Math.abs(dy);
                if (dragKind != null) {
                    if (dragKind.equals("goal")) { float[] q = P.get(dragId); if (q != null) { q[0] += dx / k; q[1] += dy / k; } }
                    else {
                        float[] q = AP.get(dragId); if (q != null) { q[0] += dx / k; q[1] += dy / k; for (JSONObject g : goals) if (areaOf(g).equals(dragId)) { float[] gq = P.get(g.optString("id")); if (gq != null) { gq[0] += dx / k; gq[1] += dy / k; } } }
                    }
                    relax(dragKind.equals("goal") ? dragId : null, 4);
                } else { tx += dx; ty += dy; }
                invalidate(); return true;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                if (pinch && e.getPointerCount() <= 2) { pinch = false; int rem = e.getActionIndex() == 0 ? 1 : 0; lx = e.getX(rem); ly = e.getY(rem); dragKind = null; moved = 99; }
                return true;
            }
            case MotionEvent.ACTION_UP: case MotionEvent.ACTION_CANCEL: {
                dragging = false; getParent().requestDisallowInterceptTouchEvent(false);
                boolean tap = moved <= 6 && e.getActionMasked() == MotionEvent.ACTION_UP;
                if (dragKind != null && moved > 6) persist();
                else if (tap) {
                    String g = hitGoal(e.getX(), e.getY());
                    if (g != null) {
                        long t = System.currentTimeMillis();
                        if (g.equals(lastTapId) && t - lastTap < 350) { lastTap = 0; lastTapId = null; cb.open(g); }
                        else { lastTap = t; lastTapId = g; sel = g.equals(sel) ? null : g; cb.select(sel); if (sel != null) focusOn(sel); }
                    } else if (sel != null) { sel = null; cb.select(null); }
                }
                dragKind = null; dragId = null; invalidate(); return true;
            }
        }
        return true;
    }

    void persist() {
        for (JSONObject g : goals) {
            float[] A = AP.get(areaOf(g)), q = P.get(g.optString("id"));
            if (A != null && q != null) { try { JSONObject mp = new JSONObject(); mp.put("dx", (q[0] - A[0]) / sp); mp.put("dy", (q[1] - A[1]) / sp); g.put("mapPos", mp); } catch (Exception ignored) { } }
        }
        try { JSONObject ap = new JSONObject(); for (Map.Entry<String, float[]> en : AP.entrySet()) { JSONObject o = new JSONObject(); o.put("x", en.getValue()[0] / sp); o.put("y", en.getValue()[1] / sp); ap.put(en.getKey(), o); } st.settings().put("areaPos", ap); } catch (Exception ignored) { }
        cb.persisted();
    }
}
