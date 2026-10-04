package com.munazzar.plotline;

import android.content.Context;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.OverScroller;

/* Swipe between the main pages. Every page stays alive (no rebuild on switch), so moving between tabs is instant;
   a fling or tap settles with a spring-like ease, and the tab bar follows the finger. */
final class NPager extends ViewGroup {
    interface Listener { void onPos(float pos); void onPage(int i); }

    Listener listener;
    final OverScroller sc;
    final int slop, minFling;
    VelocityTracker vt;
    float downX, downY, lastX;
    boolean dragging, locked;
    int page;

    NPager(Context c) {
        super(c);
        sc = new OverScroller(c, NUi.EASE);
        ViewConfiguration vc = ViewConfiguration.get(c);
        slop = vc.getScaledTouchSlop(); minFling = vc.getScaledMinimumFlingVelocity() * 4;
        setClipChildren(true);
    }

    @Override protected void onMeasure(int ws, int hs) {
        int w = MeasureSpec.getSize(ws), h = MeasureSpec.getSize(hs);
        for (int i = 0; i < getChildCount(); i++) getChildAt(i).measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
        setMeasuredDimension(w, h);
    }

    @Override protected void onLayout(boolean ch, int l, int t, int r, int b) {
        int w = r - l;
        for (int i = 0; i < getChildCount(); i++) getChildAt(i).layout(i * w, 0, (i + 1) * w, b - t);
        if (!sc.isFinished() || dragging) return;
        scrollTo(page * w, 0);
    }

    void go(int i, boolean animate) {
        i = Math.max(0, Math.min(getChildCount() - 1, i));
        boolean changed = i != page;
        page = i;
        int w = getWidth();
        if (w == 0) { if (listener != null) { listener.onPos(i); listener.onPage(i); } return; }
        if (!animate) { sc.forceFinished(true); scrollTo(i * w, 0); if (listener != null) listener.onPos(i); }
        else { sc.forceFinished(true); int dx = i * w - getScrollX(); sc.startScroll(getScrollX(), 0, dx, 0, Math.min(420, 220 + Math.abs(dx) / 6)); postInvalidateOnAnimation(); }
        if (changed && listener != null) listener.onPage(i);
    }

    @Override public void computeScroll() {
        if (sc.computeScrollOffset()) { scrollTo(sc.getCurrX(), 0); report(); postInvalidateOnAnimation(); }
    }

    void report() { if (listener != null && getWidth() > 0) listener.onPos(getScrollX() / (float) getWidth()); }

    @Override public boolean onInterceptTouchEvent(MotionEvent e) {
        if (locked) return false;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = lastX = e.getX(); downY = e.getY(); dragging = !sc.isFinished();
                if (dragging) sc.forceFinished(true);
                track(e);
                return dragging;
            case MotionEvent.ACTION_MOVE:
                float dx = e.getX() - downX, dy = e.getY() - downY;
                if (Math.abs(dx) > slop * 1.4f && Math.abs(dx) > Math.abs(dy) * 1.6f && !horizontalChild(e)) {
                    dragging = true; lastX = e.getX(); getParent().requestDisallowInterceptTouchEvent(true);
                }
                track(e);
                return dragging;
            default:
                return false;
        }
    }

    /* let inner horizontal scrollers (chips, week strips) keep their own swipes */
    boolean horizontalChild(MotionEvent e) {
        View cur = getChildAt(page);
        return cur != null && findH(cur, e.getRawX(), e.getRawY(), e.getX() < downX ? 1 : -1);
    }

    static boolean findH(View v, float rx, float ry, int dir) {
        int[] loc = new int[2]; v.getLocationOnScreen(loc);
        if (rx < loc[0] || rx > loc[0] + v.getWidth() || ry < loc[1] || ry > loc[1] + v.getHeight()) return false;
        if (v instanceof android.widget.HorizontalScrollView && v.canScrollHorizontally(dir)) return true;
        if (v instanceof ViewGroup) { ViewGroup g = (ViewGroup) v; for (int i = 0; i < g.getChildCount(); i++) if (findH(g.getChildAt(i), rx, ry, dir)) return true; }
        return false;
    }

    void track(MotionEvent e) { if (vt == null) vt = VelocityTracker.obtain(); vt.addMovement(e); }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (locked) return false;
        track(e);
        int w = getWidth();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: return true;
            case MotionEvent.ACTION_MOVE:
                if (!dragging) {
                    if (Math.abs(e.getX() - downX) > slop) { dragging = true; lastX = e.getX(); } else return true;
                }
                float d = lastX - e.getX(); lastX = e.getX();
                float nx = getScrollX() + d, max = (getChildCount() - 1) * w;
                if (nx < 0 || nx > max) nx = getScrollX() + d * .35f;   /* rubber band at the ends */
                scrollTo((int) nx, 0); report();
                return true;
            case MotionEvent.ACTION_UP: case MotionEvent.ACTION_CANCEL:
                if (dragging) {
                    vt.computeCurrentVelocity(1000);
                    float v = vt.getXVelocity(), pos = getScrollX() / (float) w;
                    int target = Math.round(pos);
                    if (Math.abs(v) > minFling) target = v < 0 ? (int) Math.floor(pos) + 1 : (int) Math.ceil(pos) - 1;
                    go(target, true);
                }
                dragging = false;
                if (vt != null) { vt.recycle(); vt = null; }
                return true;
            default: return true;
        }
    }
}
