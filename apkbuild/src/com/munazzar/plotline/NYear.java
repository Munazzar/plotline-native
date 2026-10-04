package com.munazzar.plotline;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/* Your year in review: a short story built from your own data (the engine computes it, this draws it). */
final class NYear {
    final NShell sh; final android.content.Context c; final JSONObject D;
    FrameLayout root, stage; LinearLayout bar; int i = 0; final List<View> slides = new ArrayList<>();
    static final String[] MONTHS = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};

    NYear(NShell sh, JSONObject D) { this.sh = sh; this.c = sh.a; this.D = D; }

    static void open(final NShell sh, final int year) {
        sh.a.jsRet("(window.__nyear?window.__nyear(" + (year > 0 ? year : "0") + "):'')", new android.webkit.ValueCallback<String>() { public void onReceiveValue(String v) {
            try { if (v == null || v.isEmpty()) { NShell.toast("Couldn’t build your year yet"); return; } new NYear(sh, new JSONObject(v)).show(); }
            catch (Exception e) { NCrash.log(sh.a, "year", e); NShell.toast("Couldn’t build your year"); }
        } });
    }

    /* ---- text helpers ---- */
    static final int FG = 0xFFF2EEE6, SOFT = 0xD1F2EEE6, DIM = 0xB3F2EEE6;
    CharSequence rich(String s) {
        SpannableStringBuilder b = new SpannableStringBuilder(); String[] p = s.split("\\*\\*", -1);
        for (int k = 0; k < p.length; k++) { int st = b.length(); b.append(p[k]); if (k % 2 == 1) { b.setSpan(new StyleSpan(Typeface.BOLD), st, b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); b.setSpan(new ForegroundColorSpan(0xFFFFFFFF), st, b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); } }
        return b;
    }
    TextView kicker(String s) { TextView t = new TextView(c); t.setText(s.toUpperCase()); t.setTypeface(NFont.mono(600)); t.setTextSize(12); t.setLetterSpacing(.14f); t.setTextColor(NTheme.accent); return t; }
    TextView h(String s, float sp) { TextView t = new TextView(c); t.setText(s.toUpperCase()); t.setTypeface(NFont.display(800)); t.setTextSize(sp); t.setTextColor(FG); t.setLineSpacing(0, .92f); t.setIncludeFontPadding(false); return t; }
    TextView p(String s) { TextView t = new TextView(c); t.setText(rich(s)); t.setTypeface(NFont.body(400)); t.setTextSize(18); t.setTextColor(SOFT); t.setLineSpacing(0, 1.28f); return t; }
    View big(String n, String label) {
        LinearLayout l = NUi.col(c);
        TextView b = new TextView(c); b.setText(n); b.setTypeface(NFont.display(800)); b.setTextSize(Math.min(128, Math.max(64, NUi.narrow ? 66 : 72))); b.setTextColor(FG); b.setIncludeFontPadding(false); l.addView(b);
        TextView s = new TextView(c); s.setText(label); s.setTypeface(NFont.body(400)); s.setTextSize(17); s.setTextColor(DIM); l.addView(s);
        return l;
    }
    static String pl(int n, String one, String many) { return n == 1 ? one : many; }
    static String num(long n) { return java.text.NumberFormat.getIntegerInstance().format(n); }

    /* ---- slides ---- */
    LinearLayout slide() { LinearLayout l = NUi.col(c); l.setGravity(Gravity.CENTER_VERTICAL); l.setPadding(0, 0, 0, NUi.dp(40)); return l; }
    void add(LinearLayout l, View v) { l.addView(v, NUi.mt(18)); }

    void build() {
        String nm = D.optString("name"); int y = D.optInt("y"); JSONArray chs = D.optJSONArray("chs"); int nc = chs == null ? 0 : chs.length();
        /* 1 */
        LinearLayout s = slide(); s.addView(kicker("Your " + y));
        add(s, h((nm.isEmpty() ? "Your" : nm + "’s") + "\nplotline", NUi.narrow ? 56 : 62));
        add(s, p((nc > 0 ? "A year in " + nc + " chapter" + (nc > 1 ? "s" : "") + "." : "A year of small steps.") + " Tap to see it."));
        if (nc > 0) {
            NFlow fl = new NFlow(c, 8, 8);
            for (int k = 0; k < nc; k++) { JSONObject ch = chs.optJSONObject(k); TextView t = NUi.body(c, (ch.optString("e").isEmpty() ? "" : ch.optString("e") + " ") + ch.optString("t"), 15, FG, 600);
                t.setBackground(NUi.round(NTheme.alpha(NUi.col(ch.optString("c"), NTheme.accent), .3f), 99, 0)); t.setPadding(NUi.dp(14), NUi.dp(7), NUi.dp(14), NUi.dp(7));
                fl.addView(t); }
            add(s, fl);
        }
        slides.add(s);
        /* 2 */
        int dn = D.optInt("done"); s = slide(); s.addView(kicker("Goals"));
        add(s, big(String.valueOf(dn), dn == 1 ? "goal reached" : "goals reached")); add(s, big(String.valueOf(D.optInt("steps")), "steps done"));
        add(s, p(dn > 0 ? "Biggest win: **" + D.optString("doneTop") + "**" : D.optInt("started") + " goal" + (D.optInt("started") == 1 ? "" : "s") + " started. The finish lines are coming."));
        slides.add(s);
        /* 3 */
        s = slide(); s.addView(kicker("Habits")); add(s, big(num(D.optLong("checks")), "check-ins"));
        JSONObject th = D.optJSONObject("topH"), sd = D.optJSONObject("steady"), fr = D.optJSONObject("free");
        if (th != null) add(s, p("Longest streak: **" + th.optInt("best") + " days** of " + th.optString("t") + " " + th.optString("i")));
        if (sd != null) add(s, p("Most steady: **" + sd.optString("t") + "**, kept " + sd.optInt("r") + "% of the days it was due."));
        if (fr != null) add(s, p("Longest run free of " + fr.optString("t") + ": **" + fr.optInt("b") + " days**."));
        slides.add(s);
        /* 4 */
        int best = D.optInt("best", -1); s = slide(); s.addView(kicker("Your best month")); add(s, h(best >= 0 ? MONTHS[best] : "Still to come", NUi.narrow ? 40 : 46));
        add(s, new Bars(c, D.optJSONArray("M"), best)); ((LinearLayout.LayoutParams) s.getChildAt(s.getChildCount() - 1).getLayoutParams()).bottomMargin = NUi.dp(14);
        add(s, p("Steps, habits and journal entries, month by month."));
        slides.add(s);
        /* 5 */
        int notes = D.optInt("notes");
        if (notes > 0) {
            s = slide(); s.addView(kicker("Your journal")); add(s, big(String.valueOf(notes), notes == 1 ? "entry" : "entries")); add(s, big(num(D.optLong("words")), "words"));
            if (!D.optString("mood").isEmpty()) add(s, p("You felt **" + D.optString("mood").toLowerCase() + "** most often" + (D.has("pos") && !D.isNull("pos") ? ", and " + D.optInt("pos") + "% of your moods were positive" : "") + "."));
            slides.add(s);
        }
        /* 6 */
        JSONArray qs = D.optJSONArray("qs");
        if (qs != null && qs.length() > 0) {
            s = slide(); s.addView(kicker("In your words"));
            for (int k = 0; k < qs.length(); k++) {
                JSONObject q = qs.optJSONObject(k); LinearLayout row = NUi.row(c); row.setGravity(Gravity.TOP);
                View ln = new View(c); ln.setBackgroundColor(NTheme.accent); row.addView(ln, NUi.lp(NUi.dp(3), -1));
                LinearLayout tx = NUi.col(c); tx.setPadding(NUi.dp(16), 0, 0, 0);
                TextView t = new TextView(c); t.setText("“" + q.optString("s") + "”"); t.setTypeface(NFont.body(400), Typeface.ITALIC); t.setTextSize(20); t.setTextColor(FG); t.setLineSpacing(0, 1.22f); tx.addView(t);
                TextView ci = new TextView(c); ci.setText(new java.text.SimpleDateFormat("MMMM d", java.util.Locale.getDefault()).format(new java.util.Date(q.optLong("t")))); ci.setTypeface(NFont.mono(500)); ci.setTextSize(12); ci.setTextColor(0x8CFFFFFF); ci.setPadding(0, NUi.dp(6), 0, 0); tx.addView(ci);
                row.addView(tx, NUi.lpw(0, -2, 1)); add(s, row);
            }
            slides.add(s);
        }
        /* 7 */
        int act = D.optInt("active"); s = slide(); s.addView(kicker("Next")); add(s, h(act + " goal" + (act == 1 ? "" : "s") + " in motion", NUi.narrow ? 40 : 46));
        add(s, p("Keep the plot moving. Small steps, a clear path."));
        LinearLayout acts = NUi.row(c);
        acts.addView(NUi.btn(c, "Share my year", true, new View.OnClickListener() { public void onClick(View v) { sh.a.js("window.ACT&&ACT.yearShare({y:" + D.optInt("y") + "})"); } }));
        LinearLayout.LayoutParams cl = NUi.lp(-2, -2); cl.leftMargin = NUi.dp(10);
        acts.addView(NUi.btn(c, "Close", false, new View.OnClickListener() { public void onClick(View v) { close(); } }), cl);
        add(s, acts);
        slides.add(s);
    }

    final class Bars extends View {
        final JSONArray M; final int best; final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Bars(android.content.Context c, JSONArray M, int best) { super(c); this.M = M; this.best = best; }
        @Override protected void onMeasure(int w, int h) { setMeasuredDimension(MeasureSpec.getSize(w), NUi.dp(206)); }
        @Override protected void onDraw(Canvas cv) {
            int n = 12; float W = getWidth(), gap = NUi.dp(6), bw = (W - gap * (n - 1)) / n, base = NUi.dp(180); int mx = 1; int[] sc = new int[n];
            for (int k = 0; k < n; k++) { JSONObject m = M.optJSONObject(k); sc[k] = m == null ? 0 : m.optInt("steps") * 2 + m.optInt("checks") + m.optInt("notes"); mx = Math.max(mx, sc[k]); }
            for (int k = 0; k < n; k++) {
                float h = Math.max(NUi.dp(6), base * sc[k] / mx), x = k * (bw + gap); float r = NUi.dp(6);
                p.setColor(k == best ? NTheme.accent : 0x38FFFFFF); p.setStyle(Paint.Style.FILL);
                cv.drawRoundRect(new RectF(x, base - h, x + bw, base), r, r, p); cv.drawRect(x, base - Math.min(h, r * 2) / 2 - NUi.dp(1), x + bw, base, p);
                p.setColor(0x99FFFFFF); p.setTypeface(NFont.mono(500)); p.setTextSize(NUi.dp(11)); String l = MONTHS[k].substring(0, 1); cv.drawText(l, x + (bw - p.measureText(l)) / 2, base + NUi.dp(18), p);
            }
        }
    }

    void paint(boolean anim) {
        stage.removeAllViews();
        View v = slides.get(i); stage.addView(v, new FrameLayout.LayoutParams(-1, -1));
        if (anim && !"reduced".equals(sh.st.settings().optString("motion"))) { v.setAlpha(0); v.setTranslationY(NUi.dp(16)); v.animate().alpha(1).translationY(0).setDuration(450).setInterpolator(NUi.EASE).start(); }
        for (int k = 0; k < bar.getChildCount(); k++) { View b = bar.getChildAt(k); b.setBackground(NUi.round(k <= i ? 0xFFFFFFFF : 0x2EFFFFFF, 2, 0)); }
    }

    void show() {
        build();
        root = new FrameLayout(c);
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0xFF0E1430, 0xFF05070F}); root.setBackground(g);
        View glow = new View(c) { final Paint p = new Paint(); @Override protected void onDraw(Canvas cv) { p.setShader(new android.graphics.RadialGradient(getWidth() * .8f, 0, getWidth() * 1.0f, NTheme.alpha(NTheme.accent, .22f), 0, android.graphics.Shader.TileMode.CLAMP)); cv.drawRect(0, 0, getWidth(), getHeight(), p); } };
        root.addView(glow, new FrameLayout.LayoutParams(-1, -1));
        /* tap zones: left 40% back, right 60% forward — under the buttons */
        View nav = new View(c);
        nav.setOnTouchListener(new View.OnTouchListener() { public boolean onTouch(View v, MotionEvent e) {
            if (e.getActionMasked() == MotionEvent.ACTION_UP) { if (e.getX() < v.getWidth() * .4f) step(-1); else step(1); }
            return true;
        } });
        FrameLayout.LayoutParams nl = new FrameLayout.LayoutParams(-1, -1); nl.topMargin = sh.top + NUi.dp(70); nl.bottomMargin = NUi.dp(90);
        root.addView(nav, nl);
        LinearLayout col = NUi.col(c); col.setPadding(NUi.dp(20), sh.top + NUi.dp(14), NUi.dp(20), sh.bot > 0 ? NUi.dp(20) : NUi.dp(20));
        bar = NUi.row(c);
        for (int k = 0; k < slides.size(); k++) { View b = new View(c); LinearLayout.LayoutParams l = NUi.lpw(0, NUi.dp(3), 1); if (k > 0) l.leftMargin = NUi.dp(4); bar.addView(b, l); }
        col.addView(bar);
        stage = new FrameLayout(c); col.addView(stage, NUi.lpw(-1, 0, 1));
        root.addView(col, new FrameLayout.LayoutParams(-1, -1));
        FrameLayout x = NUi.ibtn(c, "minus", new View.OnClickListener() { public void onClick(View v) { close(); } });
        x.setBackground(NUi.round(0x14FFFFFF, 20, 0));
        FrameLayout.LayoutParams xl = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END); xl.topMargin = sh.top + NUi.dp(26); xl.rightMargin = NUi.dp(16);
        root.addView(x, xl);
        sh.nroot.addView(root, new FrameLayout.LayoutParams(-1, -1)); sh.year = this;
        paint(true);
    }

    void step(int d) { int n = i + d; if (n < 0) return; if (n >= slides.size()) { close(); return; } i = n; paint(true); }

    void close() { if (root != null && root.getParent() != null) ((android.view.ViewGroup) root.getParent()).removeView(root); if (sh.year == this) sh.year = null; }
}
