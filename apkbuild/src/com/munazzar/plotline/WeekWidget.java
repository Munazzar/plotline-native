package com.munazzar.plotline;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.view.View;
import android.widget.RemoteViews;
import java.util.Calendar;
import org.json.JSONArray;
import org.json.JSONObject;

/* This week: tap a day to open it in the calendar's day view; the next three things below. */
public class WeekWidget extends AppWidgetProvider {
    static final int ROWS = 3;

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) m.updateAppWidget(id, build(c));
    }

    static void updateAll(Context c) {
        W.update(c, WeekWidget.class, new W.Builder() { public RemoteViews build(Context x) { return WeekWidget.build(x); } });
    }

    static RemoteViews build(Context c) {
        RemoteViews v = W.rv(c, "widget_week");
        v.setOnClickPendingIntent(W.id(c, "w_root"), W.open(c, "cal", 31));
        JSONObject data = W.data(c);
        JSONObject counts = data.optJSONObject("counts");
        String td = W.today();
        Calendar k = Calendar.getInstance();
        v.setTextViewText(W.id(c, "w_month"), W.fmt(k.getTime(), "MMMM"));
        k.add(Calendar.DAY_OF_MONTH, -(k.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY));
        int pill = W.id(c, "widget_pill", "drawable");
        for (int i = 0; i < 7; i++) {
            String ds = W.ymd(k.getTime());
            boolean now = ds.equals(td);
            int n = counts != null ? counts.optInt(ds, 0) : 0;
            v.setTextViewText(W.id(c, "w_dn" + i), W.fmt(k.getTime(), "EEEEE"));
            v.setTextViewText(W.id(c, "w_dd" + i), String.valueOf(k.get(Calendar.DAY_OF_MONTH)));
            v.setTextColor(W.id(c, "w_dd" + i), now ? W.ACCENT : (ds.compareTo(td) < 0 ? W.DIM : W.TEXT));
            StringBuilder dots = new StringBuilder();
            for (int j = 0; j < Math.min(n, 3); j++) dots.append('•');
            v.setTextViewText(W.id(c, "w_dc" + i), dots.length() > 0 ? dots.toString() : " ");
            W.bgRes(c, v, W.id(c, "w_d" + i), now ? pill : 0);
            v.setOnClickPendingIntent(W.id(c, "w_d" + i), W.open(c, "cal-" + ds, 300 + i));
            k.add(Calendar.DAY_OF_MONTH, 1);
        }
        JSONArray up = data.optJSONArray("up");
        int shown = 0;
        if (up != null) for (int i = 0; i < up.length() && shown < ROWS; i++) {
            JSONObject o = up.optJSONObject(i);
            if (o == null || o.optString("d").compareTo(td) < 0) continue;
            v.setViewVisibility(W.id(c, "w_row" + shown), View.VISIBLE);
            v.setInt(W.id(c, "w_ub" + shown), "setBackgroundColor", W.col(o.optString("c", "#FFB547")));
            v.setTextViewText(W.id(c, "w_ut" + shown), o.optString("t"));
            v.setTextViewText(W.id(c, "w_ud" + shown), W.dayLabel(o.optString("d")).toUpperCase());
            v.setTextColor(W.id(c, "w_ud" + shown), td.equals(o.optString("d")) ? W.ACCENT : W.MUTED);
            String tm = o.optString("time"), g = o.optString("g");
            v.setTextViewText(W.id(c, "w_um" + shown), tm.length() > 0 && g.length() > 0 ? tm + "  ·  " + g : tm + g);
            v.setOnClickPendingIntent(W.id(c, "w_row" + shown), W.open(c, "cal-" + o.optString("d"), 320 + shown));
            shown++;
        }
        for (int i = shown; i < ROWS; i++) v.setViewVisibility(W.id(c, "w_row" + i), View.GONE);
        v.setViewVisibility(W.id(c, "w_empty"), shown == 0 ? View.VISIBLE : View.GONE);
        return v;
    }
}
