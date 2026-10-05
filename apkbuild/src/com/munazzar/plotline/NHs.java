package com.munazzar.plotline;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* The web hsShell: one big 4:5 card at a time, centred and snapping, neighbours turned and faded (hsFx), a stem and
   a glowing node under each card on a dashed line, a label, and ‹ 01 / 23 › underneath. Used by Goals and Journal. */
final class NHs {
    static final class Item { View card; int col; boolean done, stat, add, check; String label; View.OnClickListener tap; }

    static final Map<String, Integer> HSP = new HashMap<>();

    static Item item(View card, int col, boolean done, boolean stat, String label) { Item i = new Item(); i.card = card; i.col = col; i.done = done; i.stat = stat; i.label = label; return i; }

    static final class Node extends View {
        final int col; final boolean on; final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Node(Context c, int col, boolean on) { super(c); this.col = col; this.on = on; setLayerType(LAYER_TYPE_SOFTWARE, null); }
        @Override protected void onDraw(Canvas cv) {
            float s = Math.min(getWidth(), getHeight()), cx = getWidth() / 2f, cy = getHeight() / 2f, r = NUi.dp(14) - NUi.dp(1);
            r = Math.min(r, s / 2 - NUi.dp(6));
            p.setStyle(Paint.Style.FILL); p.setShader(null);
            /* web .done .node: box-shadow 0 0 12px c, 0 0 32px c 55%; open node 0 0 12px -3px c 75% */
            if (on) {
                p.setColor(NTheme.alpha(col, .55f)); p.setMaskFilter(new android.graphics.BlurMaskFilter(NUi.dp(16), android.graphics.BlurMaskFilter.Blur.OUTER)); cv.drawCircle(cx, cy, r, p);
                p.setColor(col); p.setMaskFilter(new android.graphics.BlurMaskFilter(NUi.dp(6), android.graphics.BlurMaskFilter.Blur.OUTER)); cv.drawCircle(cx, cy, r, p);
            } else { p.setColor(NTheme.alpha(col, .45f)); p.setMaskFilter(new android.graphics.BlurMaskFilter(NUi.dp(5), android.graphics.BlurMaskFilter.Blur.OUTER)); cv.drawCircle(cx, cy, r, p); }
            p.setMaskFilter(null);
            if (on) p.setShader(new RadialGradient(cx - r * .24f, cy - r * .36f, r * 1.4f, new int[]{0xFFFFFFFF, col, NUi.mix(col, .55f, 0xFF000000)}, new float[]{0, .44f, 1}, Shader.TileMode.CLAMP));
            else p.setShader(new RadialGradient(cx, cy, r, new int[]{NUi.mix(col, .22f, NTheme.bg), NTheme.bg}, new float[]{0, .72f}, Shader.TileMode.CLAMP));
            cv.drawCircle(cx, cy, r, p); p.setShader(null);
            if (!on) { p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(NUi.dp(2)); p.setColor(NTheme.alpha(col, .75f)); cv.drawCircle(cx, cy, r - NUi.dp(1), p); }
        }
    }

    /* the scroller: snaps one card per swipe, like scroll-snap-align:center */
    static final class Snap extends HorizontalScrollView {
        int step, pad; Runnable fx;
        Snap(Context c) { super(c); }
        int idx() { return step <= 0 ? 0 : Math.round(getScrollX() / (float) step); }
        void goTo(int i, boolean anim) { int n = count(); i = Math.max(0, Math.min(n - 1, i)); if (anim) smoothScrollTo(i * step, 0); else scrollTo(i * step, 0); }
        int count() { LinearLayout row = (LinearLayout) getChildAt(0); return row == null ? 0 : row.getChildCount() - 2; }
        @Override public void fling(int vx) { int i = idx(); if (Math.abs(vx) > 400) i = Math.round(getScrollX() / (float) step + (vx > 0 ? .5f : -.5f)); goTo(i, true); }
        @Override public boolean onTouchEvent(MotionEvent e) {
            boolean r = super.onTouchEvent(e);
            if (e.getActionMasked() == MotionEvent.ACTION_UP || e.getActionMasked() == MotionEvent.ACTION_CANCEL) postDelayed(new Runnable() { public void run() { if (getScrollX() % Math.max(1, step) != 0) goTo(idx(), true); } }, 60);
            return r;
        }
        @Override protected void onScrollChanged(int l, int t, int ol, int ot) { super.onScrollChanged(l, t, ol, ot); if (fx != null) fx.run(); }
    }

    /* web ACT.flip: a tap on a card that isn't the centred one brings it to the centre instead of turning it */
    static final class Gate extends FrameLayout {
        final Snap hs; final int i; float x0, y0;
        Gate(Context c, Snap hs, int i) { super(c); this.hs = hs; this.i = i; setClipChildren(false); }
        @Override public boolean onInterceptTouchEvent(MotionEvent e) { return hs.idx() != i; }
        @Override public boolean onTouchEvent(MotionEvent e) {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) { x0 = e.getX(); y0 = e.getY(); return true; }
            if (e.getActionMasked() == MotionEvent.ACTION_UP && Math.abs(e.getX() - x0) < NUi.dp(10) && Math.abs(e.getY() - y0) < NUi.dp(10)) hs.goTo(i, true);
            return true;
        }
    }

    static View build(final NShell sh, final List<Item> items, int start, final String key) {
        final Context c = sh.a;
        float sw = c.getResources().getConfiguration().screenWidthDp;
        final int W = NUi.dp(Math.min(Math.round(sw * .76f), 344)), gap = NUi.dp(18), node = NUi.dp(28), cardH = Math.round(W * 1.25f);
        int screenW = c.getResources().getDisplayMetrics().widthPixels;
        final int pad = Math.max(0, (screenW - W) / 2 - gap);
        final Snap hs = new Snap(c); hs.setHorizontalScrollBarEnabled(false); hs.setClipToPadding(false); hs.setOverScrollMode(View.OVER_SCROLL_NEVER);
        hs.setPadding(0, NUi.dp(34), 0, NUi.dp(10));   /* web .hs padding:34px 0 10px */
        hs.step = W + gap; hs.pad = pad;
        final int lineY = cardH + NUi.dp(26) + node / 2;   /* inside the row (the scroller's top padding is outside it) */
        /* the dashed line and the glowing fill behind the nodes */
        final int[] fillTo = {0};
        final LinearLayout row = new LinearLayout(c) {
            final Paint lp = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void dispatchDraw(Canvas cv) {
                lp.setStrokeWidth(NUi.dp(2)); lp.setColor(NTheme.line2);
                lp.setPathEffect(new android.graphics.DashPathEffect(new float[]{NUi.dp(4), NUi.dp(7)}, 0));
                cv.drawLine(0, lineY, getWidth(), lineY, lp); lp.setPathEffect(null);
                if (fillTo[0] > 0) { lp.setColor(NTheme.accent); lp.setStrokeCap(Paint.Cap.ROUND); cv.drawLine(0, lineY, fillTo[0], lineY, lp); }
                super.dispatchDraw(cv);
            }
        };
        row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.TOP); row.setWillNotDraw(false); row.setClipChildren(false);
        row.addView(new View(c), new LinearLayout.LayoutParams(pad + gap, 1));
        final List<View> cards = new ArrayList<>(), its = new ArrayList<>();
        int lastDone = -1;
        for (int k = 0; k < items.size(); k++) {
            Item x = items.get(k);
            LinearLayout it = NUi.col(c); it.setGravity(Gravity.CENTER_HORIZONTAL); it.setClipChildren(false);
            x.card.setPivotX(W / 2f); x.card.setPivotY(cardH * .6f); x.card.setCameraDistance(1400 * c.getResources().getDisplayMetrics().density * 3);
            Gate gt = new Gate(c, hs, k); gt.addView(x.card, new FrameLayout.LayoutParams(-1, -1));
            it.addView(gt, new LinearLayout.LayoutParams(W, cardH));
            if (x.add) { LinearLayout.LayoutParams il = new LinearLayout.LayoutParams(W, -2); il.rightMargin = gap; row.addView(it, il); cards.add(x.card); its.add(it); continue; }
            View stem = new View(c); stem.setBackground(new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM, new int[]{NTheme.alpha(x.col, .85f), 0}));
            LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(NUi.dp(2), NUi.dp(24)); sl.topMargin = NUi.dp(2); it.addView(stem, sl);
            FrameLayout nd = new FrameLayout(c); nd.addView(new Node(c, x.col, x.done), new FrameLayout.LayoutParams(-1, -1));
            if (x.check && x.done) nd.addView(NUi.icon(c, "check", 14, NTheme.INK), new FrameLayout.LayoutParams(NUi.dp(14), NUi.dp(14), Gravity.CENTER));
            if (x.tap != null) NUi.tap(nd, x.tap);
            it.addView(nd, new LinearLayout.LayoutParams(node + NUi.dp(40), node + NUi.dp(40)));
            ((LinearLayout.LayoutParams) it.getChildAt(2).getLayoutParams()).topMargin = -NUi.dp(20); ((LinearLayout.LayoutParams) it.getChildAt(2).getLayoutParams()).bottomMargin = -NUi.dp(20);
            TextView lb = NBits.meta(c, x.label == null ? "" : x.label.toUpperCase(), NTheme.muted); lb.setGravity(Gravity.CENTER); lb.setSingleLine(true); lb.setPadding(0, NUi.dp(10), 0, 0);
            it.addView(lb, new LinearLayout.LayoutParams(W, -2));
            LinearLayout.LayoutParams il = new LinearLayout.LayoutParams(W, -2); il.rightMargin = gap;
            row.addView(it, il);
            cards.add(x.card); its.add(it);
            if (x.done && !x.stat) lastDone = k;
        }
        row.addView(new View(c), new LinearLayout.LayoutParams(pad, 1));
        hs.addView(row);
        final int ld = lastDone; int real0 = 0; for (Item x : items) if (!x.add) real0++; final int real = Math.max(1, real0);
        final TextView count = NBits.meta(c, "", NTheme.muted); count.setGravity(Gravity.CENTER);
        final boolean red = NFx.reduced(sh);
        hs.fx = new Runnable() { public void run() {
            float cx = hs.getScrollX() + hs.getWidth() / 2f; int best = 0; float bd = 1e9f;
            for (int k = 0; k < its.size(); k++) {
                View it = its.get(k); float d = (it.getLeft() + W / 2f - cx) / W, a = Math.min(1, Math.abs(d));
                if (Math.abs(d) < bd) { bd = Math.abs(d); best = k; }
                View cd = cards.get(k);
                if (!red) { cd.setRotationY(Math.max(-1, Math.min(1, d)) * -10); float s = 1 - a * .1f; cd.setScaleX(s); cd.setScaleY(s); cd.setAlpha(1 - a * .45f); }
            }
            HSP.put(key, best);
            count.setText(String.format(java.util.Locale.US, "%02d / %02d", Math.min(best + 1, real), real));   /* web hs-count leaves out "Add" cards */
            if (ld >= 0 && fillTo[0] == 0) { View it = its.get(ld); fillTo[0] = it.getLeft() + W / 2; row.invalidate(); }
        } };
        final int st = HSP.containsKey(key) ? HSP.get(key) : start;
        hs.post(new Runnable() { public void run() { hs.goTo(st, false); hs.fx.run(); } });
        LinearLayout wrap = NUi.col(c); wrap.setClipChildren(false);
        wrap.addView(hs, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout foot = NUi.row(c); foot.setGravity(Gravity.CENTER);
        foot.addView(NBits.sbtn(c, "back", new View.OnClickListener() { public void onClick(View v) { hs.goTo(hs.idx() - 1, true); } }));
        foot.addView(count, new LinearLayout.LayoutParams(NUi.dp(90), -2));
        foot.addView(NBits.sbtn(c, "next", new View.OnClickListener() { public void onClick(View v) { hs.goTo(hs.idx() + 1, true); } }));
        wrap.addView(foot, NUi.mt(16));
        /* full bleed: the shell spans the page gutter (web .hs-shell{margin:0 calc(-1 * var(--gutter))}) */
        FrameLayout f = new FrameLayout(c); f.setClipChildren(false); f.setTag(hs);
        FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(-1, -2); int gutter = NUi.dp(sh.wide() ? 32 : 16); fl.leftMargin = -gutter; fl.rightMargin = -gutter;
        f.addView(wrap, fl);
        return f;
    }
}
