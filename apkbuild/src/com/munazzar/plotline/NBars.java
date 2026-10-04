package com.munazzar.plotline;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/* A small bar chart: one bar per day, a goal line, today highlighted; tap a bar to open that day. */
final class NBars extends View {
    interface OnBar { void on(int i); }
    float[] vals = new float[0]; String[] labs = new String[0]; boolean[] today = new boolean[0];
    float goal; OnBar cb;
    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG), t = new Paint(Paint.ANTI_ALIAS_FLAG);
    final RectF r = new RectF();

    NBars(Context c) { super(c); t.setTextAlign(Paint.Align.CENTER); setMinimumHeight(NUi.dp(90)); }

    void set(float[] v, String[] l, boolean[] td, float g) { vals = v; labs = l; today = td; goal = g; invalidate(); }

    @Override protected void onMeasure(int w, int h) { setMeasuredDimension(MeasureSpec.getSize(w), NUi.dp(92)); }

    @Override protected void onDraw(Canvas c) {
        int n = vals.length; if (n == 0) return;
        float W = getWidth(), top = NUi.dp(6), base = NUi.dp(70), mh = base - top, cw = W / n, bw = Math.min(NUi.dp(n > 14 ? 9 : 16), cw * .7f);
        float mx = goal; for (float v : vals) mx = Math.max(mx, v); if (mx <= 0) mx = 1;
        for (int i = 0; i < n; i++) {
            float v = vals[i], h = v > 0 ? Math.max(NUi.dp(3), v / mx * mh) : NUi.dp(2), x = i * cw + (cw - bw) / 2;
            boolean ok = goal > 0 && v >= goal;
            p.setColor(v <= 0 ? NTheme.line2 : today[i] || ok ? NTheme.accent : NTheme.alpha(NTheme.accent, .42f));
            r.set(x, base - h, x + bw, base);
            c.drawRoundRect(r, NUi.dp(4), NUi.dp(4), p);
            t.setColor(today[i] ? NTheme.text : NTheme.muted); t.setTextSize(NUi.dp(10)); t.setTypeface(NFont.mono(500));
            if (!labs[i].isEmpty()) c.drawText(labs[i], i * cw + cw / 2, getHeight() - NUi.dp(6), t);
        }
        if (goal > 0) {
            p.setColor(NTheme.alpha(NTheme.muted, .6f)); p.setStrokeWidth(NUi.dp(1));
            float y = base - goal / mx * mh;
            for (float x = 0; x < W; x += NUi.dp(8)) c.drawLine(x, y, Math.min(W, x + NUi.dp(4)), y, p);
        }
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_UP && cb != null && vals.length > 0) {
            int i = (int) (e.getX() / (getWidth() / (float) vals.length));
            if (i >= 0 && i < vals.length) cb.on(i);
        }
        return true;
    }
}
