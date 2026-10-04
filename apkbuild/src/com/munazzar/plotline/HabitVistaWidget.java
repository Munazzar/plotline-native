package com.munazzar.plotline;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextPaint;
import android.text.TextUtils;
import android.widget.RemoteViews;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/*
 * Habit Vista on the home screen: each habit is a line through the last weeks, done days are stops,
 * streaks are bold glowing segments, misses thin the line, breaking habits run clean until a slip (×).
 * Drawn as one bitmap sized to the widget, so it stays sharp at any size.
 */
public class HabitVistaWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) { for (int id : ids) m.updateAppWidget(id, build(c, m, id)); }

    @Override
    public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle o) { m.updateAppWidget(id, build(c, m, id)); }

    static void updateAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, HabitVistaWidget.class));
        if (ids != null) for (int id : ids) m.updateAppWidget(id, build(c, m, id));
    }

    static RemoteViews build(Context c, AppWidgetManager m, int id) {
        RemoteViews v = W.rv(c, "widget_hvista");
        v.setOnClickPendingIntent(W.id(c, "w_root"), W.open(c, "hvista", 81));
        float dp = c.getResources().getDisplayMetrics().density;
        int wdp = 300, hdp = 180;
        try {
            Bundle o = m.getAppWidgetOptions(id);
            int a = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0), b = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0);
            if (a > 0) wdp = a; if (b > 0) hdp = b;
        } catch (Exception ignored) { }
        int W0 = Math.max(160, wdp - 28), H0 = Math.max(60, hdp - 28 - 38);
        float s = Math.min(1f, 1100f / (W0 * dp));      /* keep the bitmap small enough for the launcher */
        JSONObject d = W.data(c);
        int[] sum = new int[2];
        Bitmap bm = draw(d, (int) (W0 * dp * s), (int) (H0 * dp * s), dp * s, sum);
        v.setImageViewBitmap(W.id(c, "w_img"), bm);
        v.setTextViewText(W.id(c, "w_sum"), sum[1] > 0 ? Math.round(sum[0] * 100f / sum[1]) + "% kept · last " + days(W0) + " days" : "Your habits over time");
        return v;
    }

    static int days(int wdp) { return Math.max(14, Math.min(35, (int) ((wdp * 0.62f) / 7.5f))); }

    static List<JSONObject> lanes(JSONObject d) {
        List<JSONObject> L = new ArrayList<>();
        for (String k : new String[]{"habits", "quits"}) {
            JSONArray a = d.optJSONArray(k);
            if (a != null) for (int j = 0; j < a.length(); j++) { JSONObject h = a.optJSONObject(j); if (h != null) { try { h.put("_q", k.equals("quits")); } catch (Exception ignored) { } L.add(h); } }
        }
        return L;
    }

    static Bitmap draw(JSONObject d, int BW, int BH, float dp, int[] sum) {
        Bitmap b = Bitmap.createBitmap(Math.max(1, BW), Math.max(1, BH), Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(b);
        List<JSONObject> L = lanes(d);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        if (L.isEmpty()) {
            tp.setColor(W.MUTED); tp.setTextSize(13 * dp);
            cv.drawText("Add a habit in Plotline to see it here", 4 * dp, BH / 2f, tp);
            return b;
        }
        float laneH = Math.max(22 * dp, Math.min(40 * dp, BH / (float) L.size()));
        int n = Math.min(L.size(), (int) Math.floor(BH / laneH));
        float top = (BH - n * laneH) / 2f;
        float labW = Math.min(BW * 0.36f, 118 * dp);
        int N = days((int) (BW / dp));
        float x0 = labW + 6 * dp, x1 = BW - 6 * dp, step = (x1 - x0) / Math.max(1, N - 1);
        String[] ds = new String[N];
        for (int k = 0; k < N; k++) ds[k] = W.daysAgo(N - 1 - k);
        float r = Math.max(1.8f * dp, Math.min(4.2f * dp, step * 0.36f));
        /* week guides and today */
        p.setStrokeWidth(1); p.setColor(0x1FC8D2F0);
        for (int k = N - 1; k >= 0; k -= 7) cv.drawLine(x0 + k * step, top, x0 + k * step, top + n * laneH, p);
        Paint now = new Paint(Paint.ANTI_ALIAS_FLAG); now.setColor(W.ACCENT); now.setStrokeWidth(1.3f * dp); now.setAlpha(150);
        for (float y = top; y < top + n * laneH; y += 6 * dp) cv.drawLine(x1, y, x1, Math.min(top + n * laneH, y + 3 * dp), now);
        int done = 0, due = 0;
        for (int i = 0; i < n; i++) {
            JSONObject h = L.get(i);
            float y = top + i * laneH + laneH / 2f;
            int col = W.col(h.optString("c", "#FFB547"));
            boolean quit = h.optBoolean("_q");
            /* label */
            tp.setTypeface(Typeface.DEFAULT_BOLD); tp.setTextSize(Math.min(12.5f * dp, laneH * 0.42f)); tp.setColor(W.TEXT);
            String lab = h.optString("e", "") + "  " + h.optString("t");
            cv.drawText(TextUtils.ellipsize(lab, tp, labW - 4 * dp, TextUtils.TruncateAt.END).toString(), 0, y + tp.getTextSize() * 0.36f, tp);
            /* base line */
            Paint base = new Paint(Paint.ANTI_ALIAS_FLAG); base.setColor(col); base.setAlpha(70); base.setStrokeWidth(2 * dp); base.setStrokeCap(Paint.Cap.ROUND);
            int first = 0; String sd = quit ? h.optString("st", "0000") : h.optString("sd", "0000");
            while (first < N && ds[first].compareTo(sd) < 0) first++;
            if (first >= N) continue;
            cv.drawLine(x0 + first * step, y, x1, y, base);
            Paint run = new Paint(Paint.ANTI_ALIAS_FLAG); run.setColor(col); run.setStrokeWidth(3.6f * dp); run.setStrokeCap(Paint.Cap.ROUND);
            Paint glow = new Paint(run); glow.setStrokeWidth(6 * dp); glow.setAlpha(90); glow.setMaskFilter(new BlurMaskFilter(4 * dp, BlurMaskFilter.Blur.NORMAL));
            Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG); dot.setColor(col);
            Paint hole = new Paint(Paint.ANTI_ALIAS_FLAG); hole.setStyle(Paint.Style.STROKE); hole.setStrokeWidth(1 * dp); hole.setColor(0x66C8D2F0);
            if (quit) {
                JSONArray sl = h.optJSONArray("sl"); java.util.HashSet<String> slips = new java.util.HashSet<>();
                if (sl != null) for (int j = 0; j < sl.length(); j++) slips.add(sl.optString(j));
                boolean lim = "limit".equals(h.optString("mode"));
                int s0 = first;
                for (int k = first; k <= N; k++) {
                    boolean bad = k < N && (lim ? W.hval(h, ds[k]) > h.optInt("lim", 0) : slips.contains(ds[k]));
                    if (k == N || bad) {
                        if (k - 1 >= s0) cv.drawLine(x0 + s0 * step, y, x0 + (k - 1) * step, y, run);
                        if (bad) { Paint x = new Paint(Paint.ANTI_ALIAS_FLAG); x.setColor(W.LATE); x.setStrokeWidth(1.8f * dp); x.setStrokeCap(Paint.Cap.ROUND); float cx = x0 + k * step, q = r;
                            cv.drawLine(cx - q, y - q, cx + q, y + q, x); cv.drawLine(cx + q, y - q, cx - q, y + q, x); }
                        s0 = k + 1;
                    }
                }
                continue;
            }
            int nn = Math.max(1, h.optInt("n", 1));
            int[] st = new int[N];  /* 2 done · 1 partly · -1 missed · 3 rest · 0 not due */
            for (int k = first; k < N; k++) {
                int val = W.hval(h, ds[k]);
                boolean dueK = W.hdue(h, ds[k]) && h.optInt("tw", 0) == 0;
                if (val < 0) st[k] = 3; else if (val >= nn) st[k] = 2; else if (val > 0) st[k] = 1;
                else st[k] = dueK && k < N - 1 ? -1 : 0;
                if (dueK && val >= 0 && (k < N - 1 || val >= nn)) { due++; if (val >= nn) done++; }
            }
            /* streak runs, ignoring not-due and rest days between done days */
            int rs = -1, last = -1;
            for (int k = first; k <= N; k++) {
                int v = k < N ? st[k] : -1;
                if (v == 2) { if (rs < 0) rs = k; last = k; }
                else if (v == -1 || v == 1) {
                    if (rs >= 0 && last > rs) { if (last - rs >= 6) cv.drawLine(x0 + rs * step, y, x0 + last * step, y, glow); cv.drawLine(x0 + rs * step, y, x0 + last * step, y, run); }
                    rs = -1;
                }
            }
            for (int k = first; k < N; k++) {
                float cx = x0 + k * step;
                if (st[k] == 2) cv.drawCircle(cx, y, r, dot);
                else if (st[k] == 1) { Paint pp = new Paint(dot); pp.setAlpha(110); cv.drawCircle(cx, y, r, pp); }
                else if (st[k] == -1) cv.drawCircle(cx, y, Math.max(1.2f * dp, r * 0.5f), hole);
                else if (st[k] == 3) { Paint dsh = new Paint(hole); dsh.setColor(col); cv.drawCircle(cx, y, r * 0.75f, dsh); }
            }
        }
        sum[0] = done; sum[1] = due;
        return b;
    }
}
