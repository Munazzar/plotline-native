package com.munazzar.plotline;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.view.View;
import android.widget.RemoteViews;
import java.text.SimpleDateFormat;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/* A week-at-a-glance tracker: one row per habit, the last 7 days as circles. Tap any circle to check that day off. */
public class HabitGridWidget extends AppWidgetProvider {
    static final int ROWS = 5, DAYS = 7;

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) m.updateAppWidget(id, build(c));
    }

    static void updateAll(Context c) {
        W.update(c, HabitGridWidget.class, new W.Builder() { public RemoteViews build(Context x) { return HabitGridWidget.build(x); } });
    }

    static RemoteViews build(Context c) {
        RemoteViews v = W.rv(c, "widget_hgrid");
        v.setOnClickPendingIntent(W.id(c, "w_root"), W.open(c, "habits", 71));
        String[] ds = new String[DAYS];
        for (int k = 0; k < DAYS; k++) {
            ds[k] = W.daysAgo(DAYS - 1 - k);
            String letter = "";
            try { letter = W.fmt(new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(ds[k]), "EEEEE"); } catch (Exception ignored) { }
            v.setTextViewText(W.id(c, "w_h" + k), letter);
            v.setTextColor(W.id(c, "w_h" + k), k == DAYS - 1 ? W.TEXT : W.MUTED);
        }
        JSONArray hs = W.data(c).optJSONArray("habits");
        int shown = 0, due = 0, done = 0;
        for (int r = 0; r < ROWS; r++) {
            int row = W.id(c, "w_row" + r);
            JSONObject h = hs != null && r < hs.length() ? hs.optJSONObject(r) : null;
            if (h == null) { v.setViewVisibility(row, View.GONE); continue; }
            shown++;
            String id = h.optString("id");
            int n = Math.max(1, h.optInt("n", 1));
            v.setViewVisibility(row, View.VISIBLE);
            v.setTextViewText(W.id(c, "w_n" + r), h.optString("e", "🌱") + "  " + h.optString("t"));
            v.setOnClickPendingIntent(W.id(c, "w_n" + r), W.open(c, "habit/" + id, 700 + r));
            for (int k = 0; k < DAYS; k++) {
                int cid = W.id(c, "w_g" + r + k), val = W.hval(h, ds[k]);
                W.bgRes(c, v, cid, W.cell(c, h, ds[k]));
                v.setTextViewText(cid, val >= n ? "✓" : "");
                v.setOnClickPendingIntent(cid, W.habitTap(c, id, ds[k], "full", 710 + r * 10 + k));
                if (W.hdue(h, ds[k]) && ds[k].compareTo(h.optString("sd", "0000")) >= 0 && val >= 0 && h.optInt("tw", 0) == 0) { due++; if (val >= n) done++; }
            }
        }
        v.setTextViewText(W.id(c, "w_sum"), due > 0 ? Math.round(done * 100f / due) + "% kept · last 7 days" : "Last 7 days");
        v.setViewVisibility(W.id(c, "w_empty"), shown == 0 ? View.VISIBLE : View.GONE);
        return v;
    }
}
