package com.munazzar.plotline;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/* A row that wraps onto the next line when it runs out of room (like CSS flex-wrap, align-items:center). */
final class NFlow extends ViewGroup {
    final int hg, vg;
    boolean center;   /* justify-content:center */
    NFlow(Context c, int hgDp, int vgDp) { super(c); hg = NUi.dp(hgDp); vg = NUi.dp(vgDp); }

    static int spec(int size, int max) {
        if (size > 0) return MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
        if (size == LayoutParams.MATCH_PARENT && max > 0) return MeasureSpec.makeMeasureSpec(max, MeasureSpec.EXACTLY);
        return max > 0 ? MeasureSpec.makeMeasureSpec(max, MeasureSpec.AT_MOST) : MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
    }

    @Override protected void onMeasure(int wSpec, int hSpec) {
        int maxW = MeasureSpec.getSize(wSpec), x = 0, y = 0, lh = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i); if (v.getVisibility() == GONE) continue;
            LayoutParams lp = v.getLayoutParams();
            v.measure(spec(lp == null ? LayoutParams.WRAP_CONTENT : lp.width, maxW), spec(lp == null ? LayoutParams.WRAP_CONTENT : (lp.height == LayoutParams.MATCH_PARENT ? LayoutParams.WRAP_CONTENT : lp.height), 0));
            int w = v.getMeasuredWidth(), h = v.getMeasuredHeight();
            if (x > 0 && x + w > maxW) { x = 0; y += lh + vg; lh = 0; }
            x += w + hg; lh = Math.max(lh, h);
        }
        setMeasuredDimension(maxW, y + lh);
    }

    @Override protected void onLayout(boolean ch, int l, int t, int r, int b) {
        int maxW = r - l, n = getChildCount();
        int[] line = new int[n], lineH = new int[n + 1], lineW = new int[n + 1];
        int x = 0, k = 0;
        for (int i = 0; i < n; i++) {
            View v = getChildAt(i); if (v.getVisibility() == GONE) continue;
            int w = v.getMeasuredWidth();
            if (x > 0 && x + w > maxW) { x = 0; k++; }
            line[i] = k; lineH[k] = Math.max(lineH[k], v.getMeasuredHeight()); x += w + hg; lineW[k] = x - hg;
        }
        x = center ? Math.max(0, (maxW - lineW[0]) / 2) : 0; int y = 0, cur = 0;
        for (int i = 0; i < n; i++) {
            View v = getChildAt(i); if (v.getVisibility() == GONE) continue;
            if (line[i] != cur) { y += lineH[cur] + vg; cur = line[i]; x = center ? Math.max(0, (maxW - lineW[cur]) / 2) : 0; }
            int w = v.getMeasuredWidth(), h = v.getMeasuredHeight(), oy = (lineH[cur] - h) / 2;
            v.layout(x, y + oy, x + w, y + oy + h);
            x += w + hg;
        }
    }
}
