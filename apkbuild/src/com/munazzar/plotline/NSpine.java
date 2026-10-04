package com.munazzar.plotline;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/* The web "flight-path spine": a dashed line down the middle, a date pill when the day changes, and cards that
   alternate left and right of the line, each joined to a glowing dot (mobile: columns 1fr 46px 1fr, cards overlap 22px). */
final class NSpine {
    static final class Row { String date, label; View card; int col; }

    static Row row(String date, String label, View card, int col) { Row r = new Row(); r.date = date; r.label = label; r.card = card; r.col = col; return r; }

    static final class Dot extends View {
        final int col; final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Dot(Context c, int col) { super(c); this.col = col; setLayerType(LAYER_TYPE_SOFTWARE, null); }
        @Override protected void onDraw(Canvas cv) {
            float cx = getWidth() / 2f, cy = getHeight() / 2f, r = NUi.dp(7);
            p.setShader(null); p.setColor(NTheme.alpha(col, .55f)); p.setMaskFilter(new android.graphics.BlurMaskFilter(NUi.dp(8), android.graphics.BlurMaskFilter.Blur.OUTER)); cv.drawCircle(cx, cy, r, p); p.setMaskFilter(null);
            p.setShader(new RadialGradient(cx - r * .24f, cy - r * .36f, r * 1.3f, new int[]{0xFFFFFFFF, col}, new float[]{0, .5f}, Shader.TileMode.CLAMP)); cv.drawCircle(cx, cy, r, p);
        }
    }

    static View build(Context c, List<Row> rows, final java.util.Map<String, View> dayMarks) {
        final LinearLayout col = new LinearLayout(c) {
            final Paint lp = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void dispatchDraw(Canvas cv) {
                lp.setStrokeWidth(NUi.dp(2)); lp.setColor(NTheme.line2); lp.setPathEffect(new android.graphics.DashPathEffect(new float[]{NUi.dp(4), NUi.dp(7)}, 0));
                cv.drawLine(getWidth() / 2f, 0, getWidth() / 2f, getHeight(), lp);
                super.dispatchDraw(cv);
            }
        };
        col.setOrientation(LinearLayout.VERTICAL); col.setWillNotDraw(false); col.setPadding(0, NUi.dp(14), 0, NUi.dp(10)); col.setClipChildren(false);
        String last = ""; int i = 0; boolean afterDate = false;
        for (Row r : rows) {
            if (!r.date.equals(last)) {
                last = r.date;
                FrameLayout dw = new FrameLayout(c);
                TextView d = NJCards.data(c, r.label, NTheme.muted); d.setPadding(NUi.dp(14), NUi.dp(5), NUi.dp(14), NUi.dp(5));
                d.setBackground(NUi.round(NTheme.bg, 999, NTheme.line2));
                dw.addView(d, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
                LinearLayout.LayoutParams dl = NUi.lp(-1, -2); dl.topMargin = NUi.dp(26); dl.bottomMargin = NUi.dp(14);
                col.addView(dw, dl); if (dayMarks != null) dayMarks.put(r.date, dw);
                afterDate = true;
            }
            boolean left = i++ % 2 == 0;
            LinearLayout item = NUi.row(c); item.setGravity(Gravity.CENTER_VERTICAL); item.setClipChildren(false);
            FrameLayout a = new FrameLayout(c), b = new FrameLayout(c);
            FrameLayout card = new FrameLayout(c); card.addView(r.card, new FrameLayout.LayoutParams(-1, -2));
            /* the short connector from the card to the line */
            View con = new View(c); con.setBackgroundColor(NTheme.alpha(r.col, .55f));
            FrameLayout cw = left ? a : b; cw.setClipChildren(false); cw.addView(card, new FrameLayout.LayoutParams(-1, -2));
            FrameLayout.LayoutParams cl = new FrameLayout.LayoutParams(NUi.dp(10), NUi.dp(2), Gravity.CENTER_VERTICAL | (left ? Gravity.END : Gravity.START));
            if (left) cl.rightMargin = -NUi.dp(10); else cl.leftMargin = -NUi.dp(10);
            cw.addView(con, cl);
            item.addView(a, new LinearLayout.LayoutParams(0, -2, 1));
            FrameLayout mid = new FrameLayout(c); mid.addView(new Dot(c, r.col), new FrameLayout.LayoutParams(NUi.dp(30), NUi.dp(30), Gravity.CENTER));
            item.addView(mid, new LinearLayout.LayoutParams(NUi.dp(46), NUi.dp(30)));
            item.addView(b, new LinearLayout.LayoutParams(0, -2, 1));
            LinearLayout.LayoutParams il = NUi.lp(-1, -2); if (!afterDate) il.topMargin = -NUi.dp(22);
            col.addView(item, il); afterDate = false;
        }
        return col;
    }
}
