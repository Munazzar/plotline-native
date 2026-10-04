package com.munazzar.plotline;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/* A native page: a scrolling column rebuilt from the store when its data changes.
   Main pages live in the pager; detail screens (habit, goal, thread…) are pushed on top with back = true. */
abstract class NPage {
    final NShell sh;
    final Context c;
    final NStore st;
    ScrollView sv;
    LinearLayout body;
    FrameLayout frame;
    boolean stale = true, back;
    String focusTag;

    NPage(NShell sh) { this.sh = sh; this.c = sh.a; this.st = sh.st; }

    View view() {
        if (frame != null) return frame;
        final int slop = android.view.ViewConfiguration.get(c).getScaledTouchSlop();
        frame = new FrameLayout(c) {
            float sx, sy, rx; boolean drag, edge;
            /* detail screens: a sideways swipe that starts at the left edge goes back */
            @Override public boolean onInterceptTouchEvent(android.view.MotionEvent e) {
                if (!back) return false;
                switch (e.getActionMasked()) {
                    case android.view.MotionEvent.ACTION_DOWN: sx = e.getX(); sy = e.getY(); rx = e.getRawX(); drag = false; edge = sx < NUi.dp(28); return false;
                    case android.view.MotionEvent.ACTION_MOVE:
                        float dx = e.getX() - sx, dy = e.getY() - sy;
                        if (edge && dx > slop && dx > 2 * Math.abs(dy)) { drag = true; return true; }
                        return false;
                    default: return false;
                }
            }
            @Override public boolean onTouchEvent(android.view.MotionEvent e) {
                if (!drag) return super.onTouchEvent(e);
                switch (e.getActionMasked()) {
                    case android.view.MotionEvent.ACTION_MOVE: setTranslationX(Math.max(0, e.getRawX() - rx)); return true;
                    case android.view.MotionEvent.ACTION_UP: case android.view.MotionEvent.ACTION_CANCEL:
                        drag = false;
                        if (getTranslationX() > getWidth() * .28f) sh.pop(); else animate().translationX(0).setDuration(220).setInterpolator(NUi.EASE).start();
                        return true;
                    default: return true;
                }
            }
        };
        sv = new ScrollView(c);
        sv.setVerticalScrollBarEnabled(false); sv.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS); sv.setClipToPadding(false);
        sv.setFillViewport(true);
        body = NUi.col(c); body.setClipToPadding(false); body.setClipChildren(false); sv.setClipChildren(false);
        sv.addView(body, new FrameLayout.LayoutParams(-1, -2));
        frame.addView(sv, new FrameLayout.LayoutParams(-1, -1));
        pad();
        return frame;
    }

    void pad() {
        if (body == null) return;
        int side = NUi.dp(sh.wide() ? 32 : 16);
        body.setPadding(side, sh.top + NUi.dp(back ? (sh.focus ? 68 : 10) : 18), side, sh.bot + NUi.dp(back ? 40 : sh.focus ? 40 : 120));
    }

    /* rebuild keeping the scroll position */
    void refresh() {
        view();
        final int y = sv.getScrollY();
        body.removeAllViews();
        try { build(); }
        catch (Exception e) {
            NCrash.log(c, getClass().getSimpleName(), e);
            body.addView(NShell.errorCard(c, e));
        }
        unclip(body);
        stale = false;
        final String ft = focusTag; focusTag = null;
        sv.post(new Runnable() { public void run() {
            sv.scrollTo(0, y);
            if (ft != null) { View f = body.findViewWithTag(ft); if (f != null) f.requestFocus(); }
        } });
    }

    /* let halo glows spill past their row, like the web */
    static void unclip(View v) {
        /* a scroller inside a page must still clip what it scrolls (the day grid drew its hours over the whole card) */
        if (v instanceof android.widget.ScrollView) { v.setOutlineProvider(v.getBackground() != null ? android.view.ViewOutlineProvider.BACKGROUND : android.view.ViewOutlineProvider.BOUNDS); v.setClipToOutline(true); return; }
        NCard.hostHalo(v);
        if (!(v instanceof ViewGroup) || v instanceof android.widget.ScrollView || v instanceof NPager) return;
        if (v instanceof android.widget.HorizontalScrollView) { ((ViewGroup) v).setClipChildren(false); unclip(((ViewGroup) v).getChildAt(0)); return; }
        ((ViewGroup) v).setClipChildren(false);
        for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) unclip(((ViewGroup) v).getChildAt(i));
    }

    abstract void build();

    void onShow() { if (stale) refresh(); }

    /* page header: big title + action buttons (main pages), or back button + actions (detail screens) */
    LinearLayout header(String title, View... actions) {
        LinearLayout r = NUi.row(c);
        r.setGravity(Gravity.CENTER_VERTICAL);
        if (back) {
            r.addView(NUi.ibtn(c, "back", new View.OnClickListener() { public void onClick(View v) { sh.pop(); } }));
            View sp = new View(c); r.addView(sp, NUi.lpw(0, 1, 1));
            for (View a : actions) { LinearLayout.LayoutParams l = NUi.lp(-2, -2); l.leftMargin = NUi.dp(NUi.narrow ? 6 : 8); r.addView(a, l); }
            body.addView(r);
            if (title != null) { TextView t = NUi.title(c, title, 40); t.setPadding(0, NUi.dp(18), 0, 0); body.addView(t); }
            return r;
        }
        if (sh.focus) {   /* web [data-focus=on] .ph>div:first-child{display:none}: only the page's own buttons, right-aligned */
            r.setGravity(Gravity.END | Gravity.CENTER_VERTICAL); r.setMinimumHeight(NUi.dp(46));
            View sp = new View(c); r.addView(sp, NUi.lpw(0, 1, 1));
            for (View a : actions) { if (a == null) continue; LinearLayout.LayoutParams l = NUi.lp(-2, -2); l.leftMargin = NUi.dp(NUi.narrow ? 6 : 8); r.addView(a, l); }
            body.addView(r);
            return r;
        }
        /* web fitH1: the title never breaks inside a word. It shrinks to fit beside the buttons (down to 30px);
           if it would get smaller than that, the buttons move to their own row above and the title gets the width */
        TextView t = NUi.title(c, title, 40);
        t.setLineSpacing(0, .88f);   /* web .ph h1: 800 40px/.88, wraps between words like the web */
        int avail = c.getResources().getDisplayMetrics().widthPixels - 2 * NUi.dp(sh.wide() ? 32 : 16), aw = 0;
        for (View a : actions) { a.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED); aw += a.getMeasuredWidth() + NUi.dp(NUi.narrow ? 6 : 8); }
        android.graphics.Paint pm = new android.graphics.Paint(t.getPaint()); pm.setTextSize(30 * c.getResources().getDisplayMetrics().scaledDensity);
        boolean stack = widest(pm, t.getText().toString()) > avail - aw - NUi.dp(4);
        if (stack) {
            LinearLayout ar = NUi.row(c); ar.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            for (View a : actions) { LinearLayout.LayoutParams l = NUi.lp(-2, -2); l.leftMargin = NUi.dp(NUi.narrow ? 6 : 8); ar.addView(a, l); }
            body.addView(ar);
            t.setTextSize(fit(t, 22, avail));
            body.addView(t, NUi.mt(10));
            return ar;
        }
        t.setTextSize(fit(t, 30, avail - aw - NUi.dp(4)));
        r.setGravity(Gravity.TOP);   /* web .ph: the buttons sit at the top of a multi-line title */
        r.addView(t, NUi.lpw(0, -2, 1));
        for (View a : actions) { LinearLayout.LayoutParams l = NUi.lp(-2, -2); l.leftMargin = NUi.dp(NUi.narrow ? 6 : 8); r.addView(a, l); }
        body.addView(r);
        return r;
    }

    /* the web fits the longest single word (h1 wraps between words; fitH1 only stops a word breaking) */
    static float widest(android.graphics.Paint pm, String s) { float m = 0; for (String l : s.split("\\s+")) m = Math.max(m, pm.measureText(l)); return m; }

    /* largest size (40sp down to min) at which the one-line title fits the width */
    float fit(TextView t, float min, int w) {
        android.graphics.Paint pm = new android.graphics.Paint(t.getPaint()); float sd = c.getResources().getDisplayMetrics().scaledDensity;
        for (float sp = 40; sp > min; sp -= 1) { pm.setTextSize(sp * sd); if (widest(pm, t.getText().toString()) <= w) return sp; }
        return min;
    }

    /* full screen + settings, like the web's gear() */
    View gear() {
        LinearLayout g = NUi.row(c);
        if (sh.focus) return g;   /* web hides .fsb and .gearb in full screen */
        boolean fs = !(back && c.getResources().getConfiguration().screenWidthDp <= 560);   /* web: .crumb .fsb hidden on phones */
        if (fs) g.addView(NUi.ibtn(c, "full", new View.OnClickListener() { public void onClick(View v) { sh.focusMode(!sh.focus); } }));
        LinearLayout.LayoutParams l = NUi.lp(-2, -2); l.leftMargin = fs ? NUi.dp(NUi.narrow ? 6 : 8) : 0;
        g.addView(NUi.ibtn(c, "settings", new View.OnClickListener() { public void onClick(View v) { sh.openSettings(); } }), l);
        return g;
    }

    void add(View v) { body.addView(v); }
    void add(View v, int top) { body.addView(v, NUi.mt(top)); }

    TextView muted(String s) { TextView t = NUi.text(c, s, 14, NTheme.muted); t.setLineSpacing(0, 1.25f); return t; }

    static void detach(View v) { if (v.getParent() instanceof ViewGroup) ((ViewGroup) v.getParent()).removeView(v); }
}
