package com.munazzar.plotline;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/* A progress ring that animates to its value. Optional big number + small label in the middle,
   or a check mark when complete (used as the habit check button). */
final class NRing extends View {
    float value, shown;
    int track, color, centerFill;
    String big = "", small = "";
    boolean check;
    float strokeDp = 4;
    boolean inline;
    String glyph;   /* an icon in the middle (web hChk: play for a routine not yet done) */   /* web .hday-r: "1/10" on one baseline, the /10 small and faded */
    final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG), txt = new Paint(Paint.ANTI_ALIAS_FLAG), fillP = new Paint(Paint.ANTI_ALIAS_FLAG);
    final RectF r = new RectF();
    ValueAnimator va;

    NRing(Context c) {
        super(c);
        arc.setStyle(Paint.Style.STROKE); arc.setStrokeCap(Paint.Cap.ROUND);
        txt.setTextAlign(Paint.Align.CENTER);
        track = NTheme.line2; color = NTheme.accent;
    }

    NRing set(float v, boolean animate) {
        v = Math.max(0, Math.min(1, v));
        if (va != null) va.cancel();
        value = v;
        if (!animate) { shown = v; invalidate(); return this; }
        va = ValueAnimator.ofFloat(shown, v); va.setDuration(520); va.setInterpolator(NUi.EASE);
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { public void onAnimationUpdate(ValueAnimator a) { shown = (float) a.getAnimatedValue(); invalidate(); } });
        va.start();
        return this;
    }

    NRing text(String b, String s) { big = b == null ? "" : b; small = s == null ? "" : s; invalidate(); return this; }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), sz = Math.min(w, h), sw = NUi.dp(strokeDp);
        r.set((w - sz) / 2 + sw / 2, (h - sz) / 2 + sw / 2, (w + sz) / 2 - sw / 2, (h + sz) / 2 - sw / 2);
        if (centerFill != 0 || (check && shown >= 1)) {
            fillP.setColor(check && shown >= 1 ? color : centerFill);
            c.drawOval(r, fillP);
        }
        arc.setStrokeWidth(sw);
        arc.setColor(track); c.drawArc(r, 0, 360, false, arc);
        if (shown > 0) { arc.setColor(color); c.drawArc(r, -90, 360 * shown, false, arc); }
        float cx = w / 2, cy = h / 2;
        if (check && shown >= 1) {
            NIcon ic = new NIcon("check", NTheme.on(color)); ic.stroke(2.6f);
            int s = (int) (sz * .46f);
            ic.setBounds((int) (cx - s / 2f), (int) (cy - s / 2f), (int) (cx + s / 2f), (int) (cy + s / 2f)); ic.draw(c);
            return;
        }
        if (glyph != null) {
            NIcon ic = new NIcon(glyph, NTheme.text); int s = NUi.dp(18);
            ic.setBounds((int) (cx - s / 2f), (int) (cy - s / 2f), (int) (cx + s / 2f), (int) (cy + s / 2f)); ic.draw(c);
            return;
        }
        if (inline && !big.isEmpty()) {
            txt.setTextAlign(Paint.Align.LEFT);
            txt.setTypeface(NFont.display(800)); txt.setTextSize(NUi.sp(24)); float bw = txt.measureText(big);
            Paint sp = new Paint(txt); sp.setTypeface(NFont.body(600)); sp.setTextSize(NUi.sp(13)); float sw2 = sp.measureText(small);
            float x0 = cx - (bw + sw2) / 2, by = cy + NUi.sp(24) * .35f;
            txt.setColor(NTheme.text); c.drawText(big, x0, by, txt);
            sp.setColor(NTheme.alpha(NTheme.text, .6f)); c.drawText(small, x0 + bw, by, sp);
            txt.setTextAlign(Paint.Align.CENTER);
            return;
        }
        if (!big.isEmpty()) {
            txt.setColor(NTheme.text); txt.setTypeface(NFont.display(800));
            float bs = sz * (small.isEmpty() ? .34f : .3f); txt.setTextSize(bs);
            float by = small.isEmpty() ? cy + bs * .36f : cy + bs * .2f;
            c.drawText(big, cx, by, txt);
            if (!small.isEmpty()) {
                txt.setColor(NTheme.muted); txt.setTypeface(NFont.mono(500)); txt.setTextSize(Math.max(NUi.dp(9), sz * .085f));
                c.drawText(small.toUpperCase(), cx, by + sz * .16f, txt);
            }
        }
    }
}
