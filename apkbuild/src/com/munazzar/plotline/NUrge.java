package com.munazzar.plotline;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Calendar;
import org.json.JSONArray;
import org.json.JSONObject;

/* 2.0.8: quit-habit urge timer, slip log and the routine runner, natively (web urge / slip / routineGo). */
final class NUrge {
    private NUrge() { }

    static final String[] TRIGS = {"Stress", "Boredom", "Tired", "Social", "Hungry", "A usual cue", "Other"};

    static boolean ok(JSONObject u) { Object o = u.opt("ok"); return o instanceof Boolean ? (Boolean) o : u.optInt("ok") != 0; }

    static int urgesBeaten(JSONObject h) { JSONArray u = h.optJSONArray("urges"); int n = 0; for (int i = 0; u != null && i < u.length(); i++) { JSONObject x = u.optJSONObject(i); if (x != null && ok(x)) n++; } return n; }

    /* web qBest: the longest clean stretch (limit habits: the longest run within the limit) */
    static int best(JSONObject h) {
        if (NHabits.limitMode(h)) {
            int tn = NDates.today(), st0 = NDates.dnum(NHabits.startDate(h)), b = 0, r = 0;
            for (int k = st0; k <= tn; k++) { if (NHabits.limitOk(h, NDates.fromN(k))) { r++; b = Math.max(b, r); } else r = 0; }
            return b;
        }
        long start = h.optLong("start", h.optLong("createdAt"));
        java.util.List<Long> ts = new java.util.ArrayList<>(); ts.add(start);
        JSONArray sl = h.optJSONArray("slips");
        if (sl != null) for (int i = 0; i < sl.length(); i++) { long t = sl.optJSONObject(i).optLong("t"); if (t >= start) ts.add(t); }
        java.util.Collections.sort(ts);
        long b = 0;
        for (int i = 0; i < ts.size(); i++) { long e = i + 1 < ts.size() ? ts.get(i + 1) : System.currentTimeMillis(); b = Math.max(b, e - ts.get(i)); }
        return (int) (b / 86400000L);
    }

    static String clock(long ms) { long s = Math.max(0, ms) / 1000; return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60); }

    /* ---------- web hLimit: one more today ---------- */
    static void limitAdd(NShell sh, JSONObject h, int n) {
        try {
            String td = NDates.ymd(); JSONObject log = NHabits.obj(h, "log");
            int v = Math.max(0, log.optInt(td, 0) + n), lim = NHabits.limit(h);
            if (v > 0) log.put(td, v); else log.remove(td);
            NFx.vib(sh.a); sh.save();
            if (n > 0) NShell.toast(v > lim ? "Over today’s limit of " + lim + ". Tomorrow resets." : v == lim ? "That’s today’s limit" : (lim - v) + " left today");
        } catch (Exception ignored) { }
    }

    /* ---------- log a slip (web slip / saveSlip) ---------- */
    static void slip(final NShell sh, final JSONObject h) {
        final Context c = sh.a; NForms F = new NForms(sh);
        LinearLayout b = F.sheetBody(NHabits.icon(h) + " " + h.optString("title"), "Log a slip", "A slip is information, not failure. Note what set it off, then start again. Your best run stays on record.");
        b.addView(NForms.fieldLabel(c, "When"));
        Calendar now = Calendar.getInstance();
        final String[] d = {NDates.ymd()}, tm = {String.format(java.util.Locale.US, "%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))};
        LinearLayout when = NUi.row(c);
        when.addView(NForms.datePick(c, d, "Today"), NUi.lpw(0, -2, 1));
        LinearLayout.LayoutParams tl = NUi.lpw(0, -2, 1); tl.leftMargin = NUi.dp(10);
        when.addView(NForms.timePick(c, tm, "Now"), tl);
        b.addView(when);
        b.addView(NForms.fieldLabel(c, "What set it off?"));
        final String[] trig = {""};
        String[][] to = new String[TRIGS.length][]; for (int i = 0; i < TRIGS.length; i++) to[i] = new String[]{TRIGS[i], TRIGS[i]};
        b.addView(NForms.choice(c, to, trig, null));
        b.addView(NForms.fieldLabel(c, "Note · optional"));
        final EditText note = NForms.input(c, "Where were you, how did you feel?", "", true); note.setMinLines(2); b.addView(note);
        NVoice.attach(c, note);
        b.addView(NForms.actions(c, NUi.btn(c, "Cancel", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } }),
            NUi.btn(c, "Restart the clock", true, new View.OnClickListener() { public void onClick(View v) {
                long t = System.currentTimeMillis();
                try { Calendar k = NDates.valid(d[0]) ? NDates.cal(d[0]) : Calendar.getInstance(); String[] p = (tm[0] == null || tm[0].isEmpty() ? "00:00" : tm[0]).split(":"); k.set(Calendar.HOUR_OF_DAY, Integer.parseInt(p[0])); k.set(Calendar.MINUTE, Integer.parseInt(p[1])); k.set(Calendar.SECOND, 0); t = Math.min(t, k.getTimeInMillis()); } catch (Exception ignored) { }
                int bst = best(h);
                try { JSONObject s = new JSONObject(); s.put("t", t); s.put("trig", trig[0]); s.put("note", note.getText().toString().trim()); NHabits.arr(h, "slips").put(s); h.put("mile", 0); } catch (Exception ignored) { }
                sh.closeSheet(); sh.save();
                NShell.toast("Clock restarted. Your best run is " + bst + " day" + (bst == 1 ? "" : "s") + ". You can beat it.");
            } })));
        sh.sheet(b);
    }

    /* ---------- I have an urge: 5-minute breathing timer (web urge) ---------- */
    static final class Breath extends View {
        final int col; final long t0; final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG), g = new Paint(Paint.ANTI_ALIAS_FLAG);
        final android.view.animation.PathInterpolator ease = new android.view.animation.PathInterpolator(.45f, 0f, .55f, 1f);
        boolean still;
        Breath(Context c, int col, long t0, boolean still) { super(c); this.col = col; this.t0 = t0; this.still = still; setLayerType(LAYER_TYPE_SOFTWARE, null); }
        @Override protected void onDraw(Canvas cv) {
            float w = getWidth(), h = getHeight(), cx = w / 2, cy = h / 2, R = Math.min(w, h) / 2;
            float ph = ((System.currentTimeMillis() - t0) % 10000) / 10000f, sc;
            if (still) sc = .5f; else if (ph < .4f) sc = .5f + .5f * ease.getInterpolation(ph / .4f); else if (ph < .55f) sc = 1f; else sc = 1f - .5f * ease.getInterpolation((ph - .55f) / .45f);
            float r = R * sc;
            g.setColor(NTheme.alpha(col, .5f)); g.setMaskFilter(new android.graphics.BlurMaskFilter(NUi.dp(30), android.graphics.BlurMaskFilter.Blur.NORMAL));
            cv.drawCircle(cx, cy, r * .92f, g);
            p.setShader(new RadialGradient(cx, cy, Math.max(1, r), new int[]{NTheme.alpha(col, .6f), NTheme.alpha(col, .1f), NTheme.alpha(col, .1f)}, new float[]{0, .7f, 1}, Shader.TileMode.CLAMP));
            cv.drawCircle(cx, cy, r, p);
            if (!still) postInvalidateOnAnimation();
        }
    }

    static void urge(final NShell sh, final JSONObject h) {
        final Context c = sh.a; NForms F = new NForms(sh);
        final long t0 = System.currentTimeMillis(), end = t0 + 5 * 60000L;
        final int col = NTheme.areaCol(h.optString("area"));
        LinearLayout b = F.sheetBody(NHabits.icon(h) + " " + h.optString("title"), "Ride it out", "An urge rises, peaks and fades, usually within minutes. You don’t have to act on it. Breathe with the circle.");
        FrameLayout br = new FrameLayout(c);
        br.addView(new Breath(c, col, t0, NFx.reduced(sh)), new FrameLayout.LayoutParams(-1, -1));
        final TextView bt = NUi.body(c, "Breathe in", 17, NTheme.text, 700); bt.setGravity(Gravity.CENTER);
        br.addView(bt, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        LinearLayout.LayoutParams bl = NUi.lp(NUi.dp(190), NUi.dp(190)); bl.gravity = Gravity.CENTER_HORIZONTAL; bl.topMargin = NUi.dp(18); bl.bottomMargin = NUi.dp(8);
        b.addView(br, bl);
        final TextView tv = NUi.label(c, "5:00", NTheme.muted); tv.setGravity(Gravity.CENTER); tv.setTextSize(13);
        b.addView(tv, NUi.lp(-1, -2));
        if (!h.optString("why").isEmpty()) b.addView(quote(c, col, "Why you started", h.optString("why")), NUi.mt(12));
        if (!h.optString("plan").isEmpty()) b.addView(quote(c, col, "Your plan", h.optString("plan")), NUi.mt(12));
        int ub = urgesBeaten(h); boolean lim = NHabits.limitMode(h); int qd = NHabits.cleanDays(h);
        String line = (lim ? NHabits.obj(h, "log").optInt(NDates.ymd(), 0) + " of " + NHabits.limit(h) + " today" : qd + " day" + (qd == 1 ? "" : "s") + " free") + (ub > 0 ? " · " + ub + " urge" + (ub > 1 ? "s" : "") + " beaten so far" : "");
        TextView st = NUi.label(c, line, NTheme.muted); st.setGravity(Gravity.CENTER); b.addView(st, NUi.mt(12));
        final String hid = h.optString("id");
        b.addView(NForms.actions(c,
            NUi.btn(c, lim ? "I gave in" : "I slipped", false, new View.OnClickListener() { public void onClick(View v) {
                final JSONObject x = sh.st.find("habits", hid); if (x == null) return;
                if (NHabits.limitMode(x)) { push(x, false); sh.closeSheet(); limitAdd(sh, x, 1); }
                else { sh.closeSheet(); v.postDelayed(new Runnable() { public void run() { slip(sh, x); } }, 300); }
            } }),
            NUi.btn(c, "✓ It passed", true, new View.OnClickListener() { public void onClick(View v) {
                JSONObject x = sh.st.find("habits", hid); if (x == null) return;
                push(x, true); sh.closeSheet(); sh.save(); NFx.burst(sh);
                int n = urgesBeaten(x); NShell.toast(n == 1 ? "First urge beaten. Remember how that felt." : "Urge beaten. That’s " + n + " so far.");
            } })));
        sh.sheet(b);
        final Handler hd = new Handler(Looper.getMainLooper());
        final Runnable tick = new Runnable() { public void run() {
            long now = System.currentTimeMillis(), left = Math.max(0, end - now), ph = (now - t0) % 10000;
            tv.setText(left > 0 ? clock(left) : "You rode it out");
            bt.setText(ph < 4000 ? "Breathe in" : ph < 5500 ? "Hold" : "Breathe out");
            hd.postDelayed(this, 1000 - (now % 1000));
        } };
        tick.run();
        sh.onSheetClose = new Runnable() { public void run() { hd.removeCallbacks(tick); } };
    }

    static void push(JSONObject h, boolean won) {
        try {
            JSONArray u = NHabits.arr(h, "urges"); JSONObject o = new JSONObject(); o.put("t", System.currentTimeMillis()); o.put("ok", won ? 1 : 0); u.put(o);
            if (u.length() > 400) { JSONArray k = new JSONArray(); for (int i = u.length() - 400; i < u.length(); i++) k.put(u.get(i)); h.put("urges", k); }
        } catch (Exception ignored) { }
    }

    static View quote(Context c, int col, String k, String text) {
        LinearLayout q = NUi.col(c);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable(); bg.setColor(NTheme.surface);
        float r = NUi.dp(14), r2 = NUi.dp(4); bg.setCornerRadii(new float[]{r2, r2, r, r, r, r, r2, r2});
        android.graphics.drawable.GradientDrawable bar = new android.graphics.drawable.GradientDrawable(); bar.setColor(col);
        android.graphics.drawable.LayerDrawable ld = new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{bg, bar});
        ld.setLayerWidth(1, NUi.dp(3)); ld.setLayerGravity(1, Gravity.LEFT | Gravity.FILL_VERTICAL);
        q.setBackground(ld); q.setPadding(NUi.dp(16), NUi.dp(12), NUi.dp(16), NUi.dp(12));
        q.addView(NUi.label(c, k, NTheme.muted));
        TextView t = NUi.text(c, text, 14.5f, NTheme.text); t.setPadding(0, NUi.dp(2), 0, 0); q.addView(t);
        return q;
    }

    /* ---------- routine runner (web routineGo / rpDraw / rpNext) ---------- */
    static final class Ring extends View {
        float frac; int col; final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Ring(Context c, int col) { super(c); this.col = col; p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND); }
        @Override protected void onDraw(Canvas cv) {
            float s = Math.min(getWidth(), getHeight()), k = s / 200f, sw = 9 * k, r = 88 * k, cx = getWidth() / 2f, cy = getHeight() / 2f;
            p.setStrokeWidth(sw); p.setColor(NTheme.line2); cv.drawCircle(cx, cy, r, p);
            if (frac > 0) { p.setColor(col); cv.drawArc(new RectF(cx - r, cy - r, cx + r, cy + r), -90, 360 * frac, false, p); }
        }
    }

    static final class Run {
        final NShell sh; final String hid; int i; long end, left; boolean paused, beeped, open = true;
        final Handler hd = new Handler(Looper.getMainLooper());
        LinearLayout body; Ring ring; TextView tt, tl;
        Run(NShell sh, String hid) { this.sh = sh; this.hid = hid; }
        JSONObject h() { return sh.st.find("habits", hid); }
        java.util.Set<String> done() { java.util.Set<String> d = new java.util.HashSet<>(); JSONObject h = h(); JSONArray a = h == null ? null : NHabits.obj(h, "rs").optJSONArray(NDates.ymd()); for (int k = 0; a != null && k < a.length(); k++) d.add(a.optString(k)); return d; }

        void start(int k) { JSONObject s = NHabits.steps(h()).optJSONObject(k); i = k; left = (long) (s.optDouble("min", 0) * 60000); end = System.currentTimeMillis() + left; paused = false; beeped = false; draw(); }

        void draw() {
            final JSONObject h = h(); if (h == null) { end(); return; }
            final Context c = sh.a; JSONArray st = NHabits.steps(h); JSONObject s = st.optJSONObject(i); int N = st.length(); java.util.Set<String> dn = done();
            long tot = (long) (s.optDouble("min", 0) * 60000), lf = paused ? left : Math.max(0, end - System.currentTimeMillis());
            int col = NTheme.areaCol(h.optString("area"));
            boolean first = body == null;
            if (first) body = NUi.col(c); else body.removeAllViews();
            body.addView(NUi.label(c, NHabits.icon(h) + " " + h.optString("title") + " · step " + (i + 1) + " of " + N, NTheme.accent));
            FrameLayout rf = new FrameLayout(c);
            ring = new Ring(c, col); ring.frac = tot > 0 ? 1f - lf / (float) tot : 0; rf.addView(ring, new FrameLayout.LayoutParams(-1, -1));
            LinearLayout tc = NUi.col(c); tc.setGravity(Gravity.CENTER);
            tt = NUi.text(c, tot > 0 ? clock(lf) : "✓", 56, tot > 0 ? NTheme.text : col); tt.setTypeface(NFont.display(800)); tt.setGravity(Gravity.CENTER); tt.setFontFeatureSettings("tnum");
            tc.addView(tt);
            tl = NUi.label(c, tot > 0 ? (paused ? "Paused" : "Remaining") : "No timer", NTheme.muted); tl.setGravity(Gravity.CENTER); tc.addView(tl, NUi.mt(4));
            rf.addView(tc, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
            int rs = Math.min(NUi.dp(220), (int) (c.getResources().getDisplayMetrics().widthPixels * .62f));
            LinearLayout.LayoutParams rl = NUi.lp(rs, rs); rl.gravity = Gravity.CENTER_HORIZONTAL; rl.topMargin = NUi.dp(16);
            body.addView(rf, rl);
            TextView t = NUi.title(c, s.optString("title"), 26); t.setGravity(Gravity.CENTER); body.addView(t, NUi.mt(14));
            NFlow dots = new NFlow(c, 6, 6);
            for (int k = 0; k < N; k++) { View d = new View(c); d.setBackground(NUi.round(dn.contains(st.optJSONObject(k).optString("id")) ? col : k == i ? NTheme.text : NTheme.line2, 3, 0)); dots.addView(d, new android.view.ViewGroup.MarginLayoutParams(NUi.dp(26), NUi.dp(6))); }
            LinearLayout dw = NUi.row(c); dw.setGravity(Gravity.CENTER_HORIZONTAL); dw.addView(dots);
            body.addView(dw, NUi.mt(12));
            if (i + 1 < N) { TextView nx = NUi.label(c, "Next: " + st.optJSONObject(i + 1).optString("title"), NTheme.muted); nx.setGravity(Gravity.CENTER); body.addView(nx, NUi.mt(10)); }
            boolean more = i + 1 < N; for (int k = 0; k < N && !more; k++) if (k != i && !dn.contains(st.optJSONObject(k).optString("id"))) more = true;
            NFlow a = new NFlow(c, 8, 8);
            a.addView(NUi.btn(c, "Close", false, new View.OnClickListener() { public void onClick(View v) { close(); } }), new android.view.ViewGroup.MarginLayoutParams(-2, NUi.dp(44)));
            if (tot > 0) a.addView(NUi.btn(c, paused ? "▶ Resume" : "❚❚ Pause", false, new View.OnClickListener() { public void onClick(View v) { pause(); } }), new android.view.ViewGroup.MarginLayoutParams(-2, NUi.dp(44)));
            a.addView(NUi.btn(c, "⏭ Skip", false, new View.OnClickListener() { public void onClick(View v) { next(false); } }), new android.view.ViewGroup.MarginLayoutParams(-2, NUi.dp(44)));
            a.addView(NUi.btn(c, more ? "✓ Done" : "✓ Finish", true, new View.OnClickListener() { public void onClick(View v) { NFx.vib(sh.a); next(true); } }), new android.view.ViewGroup.MarginLayoutParams(-2, NUi.dp(44)));
            LinearLayout aw = NUi.row(c); aw.setGravity(Gravity.CENTER_HORIZONTAL); aw.addView(a);
            body.addView(aw, NUi.mt(18));
            if (first) {
                sh.sheet(body); sh.sheetLock = true;
                sh.sheetBack = new Runnable() { public void run() { close(); } };
                sh.onSheetClose = new Runnable() { public void run() { open = false; hd.removeCallbacksAndMessages(null); } };
                tick();
            }
        }

        void tick() {
            hd.removeCallbacksAndMessages(null);
            if (!open) return;
            hd.postDelayed(new Runnable() { public void run() { tick(); } }, 1000 - System.currentTimeMillis() % 1000);
            JSONObject h = h(); if (h == null || paused) return;
            JSONObject s = NHabits.steps(h).optJSONObject(i); long tot = (long) (s.optDouble("min", 0) * 60000); if (tot <= 0) return;
            long lf = Math.max(0, end - System.currentTimeMillis());
            if (tt != null) tt.setText(clock(lf));
            if (ring != null) { ring.frac = 1f - lf / (float) tot; ring.invalidate(); }
            if (lf <= 0 && !beeped) { beeped = true; NFx.beep(); NFx.vib(sh.a); hd.postDelayed(new Runnable() { public void run() { if (open && beeped) next(true); } }, 900); }
        }

        void pause() { if (paused) { end = System.currentTimeMillis() + left; paused = false; } else { left = Math.max(0, end - System.currentTimeMillis()); paused = true; } draw(); }

        void next(boolean markDone) {
            JSONObject h = h(); if (h == null) { end(); return; }
            JSONArray st = NHabits.steps(h); String td = NDates.ymd();
            if (markDone) {
                try {
                    java.util.Set<String> L = done(); L.add(st.optJSONObject(i).optString("id"));
                    JSONArray o = new JSONArray(); for (int k = 0; k < st.length(); k++) if (L.contains(st.optJSONObject(k).optString("id"))) o.put(st.optJSONObject(k).optString("id"));
                    NHabits.obj(h, "rs").put(td, o); NHabits.obj(h, "skip").remove(td);
                    sh.saveQuiet();
                } catch (Exception ignored) { }
            }
            java.util.Set<String> dn = done(); boolean all = true; for (int k = 0; k < st.length(); k++) if (!dn.contains(st.optJSONObject(k).optString("id"))) all = false;
            if (all) {
                end(); NFx.burst(sh); int s = NHabits.streak(h);
                NShell.toast(h.optString("title") + " complete" + (s > 1 ? " · " + s + (NHabits.freq(h).equals("times") ? "-week" : "-day") + " streak" : ""));
                return;
            }
            int j = -1; for (int k = i + 1; k < st.length() && j < 0; k++) if (!dn.contains(st.optJSONObject(k).optString("id"))) j = k;
            if (j < 0) for (int k = 0; k < st.length() && j < 0; k++) if (!dn.contains(st.optJSONObject(k).optString("id"))) j = k;
            if (j == i && !markDone) { int n = dn.size(); end(); if (n > 0) NShell.toast("Saved · " + n + " of " + st.length() + " steps done"); return; }
            start(j);
        }

        void close() { JSONObject h = h(); int n = done().size(), N = h == null ? 0 : NHabits.steps(h).length(); end(); if (h != null && n > 0 && n < N) NShell.toast("Saved · " + n + " of " + N + " steps done"); }

        void end() { open = false; hd.removeCallbacksAndMessages(null); sh.sheetLock = false; sh.sheetBack = null; sh.closeSheet(); sh.save(); }
    }

    static void routine(NShell sh, String hid) {
        JSONObject h = sh.st.find("habits", hid); if (h == null) return;
        JSONArray st = NHabits.steps(h);
        if (st.length() == 0) return;
        Run r = new Run(sh, hid); java.util.Set<String> dn = r.done();
        int i = -1; for (int k = 0; k < st.length() && i < 0; k++) if (!dn.contains(st.optJSONObject(k).optString("id"))) i = k;
        if (i < 0) { NHabits.obj(h, "rs").remove(NDates.ymd()); i = 0; }
        r.start(i);
    }
}
