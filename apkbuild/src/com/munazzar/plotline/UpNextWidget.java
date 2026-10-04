package com.munazzar.plotline;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.view.View;
import android.widget.RemoteViews;
import org.json.JSONArray;
import org.json.JSONObject;

/* Up next: the most urgent step, big, with a Done button; the next two with quick check buttons. */
public class UpNextWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) m.updateAppWidget(id, build(c));
    }

    static void updateAll(Context c) {
        W.update(c, UpNextWidget.class, new W.Builder() { public RemoteViews build(Context x) { return UpNextWidget.build(x); } });
    }

    static RemoteViews build(Context c) {
        RemoteViews v = W.rv(c, "widget_up_next");
        v.setOnClickPendingIntent(W.id(c, "w_root"), W.open(c, "today", 7));
        v.setOnClickPendingIntent(W.id(c, "w_open"), W.open(c, "today", 8));
        JSONArray items = W.data(c).optJSONArray("items");
        int n = items == null ? 0 : items.length();
        v.setViewVisibility(W.id(c, "w_empty"), n == 0 ? View.VISIBLE : View.GONE);
        v.setViewVisibility(W.id(c, "w_hero"), n == 0 ? View.GONE : View.VISIBLE);
        v.setViewVisibility(W.id(c, "w_due"), n == 0 ? View.GONE : View.VISIBLE);
        if (n > 0) {
            JSONObject it = items.optJSONObject(0);
            v.setTextViewText(W.id(c, "w_g0"), it.optString("g"));
            v.setTextColor(W.id(c, "w_g0"), W.col(it.optString("c", "#FFB547")));
            v.setTextViewText(W.id(c, "w_t0"), it.optString("t"));
            String due = it.optString("d"), tm = it.optString("time");
            String chip = due.length() > 0 ? due + (tm.length() > 0 ? " · " + tm : "") : (tm.length() > 0 ? tm : "No date");
            v.setTextViewText(W.id(c, "w_due"), chip);
            v.setTextColor(W.id(c, "w_due"), it.optBoolean("late") ? W.LATE : W.TEXT);
            v.setOnClickPendingIntent(W.id(c, "w_done0"), W.action(c, WidgetActions.STEP, it.optString("sid"), 60, "g", it.optString("gid"), "s", it.optString("sid")));
        }
        for (int i = 1; i <= 2; i++) {
            int row = W.id(c, "w_row" + i);
            if (i < n) {
                JSONObject it = items.optJSONObject(i);
                v.setViewVisibility(row, View.VISIBLE);
                v.setInt(W.id(c, "w_b" + i), "setBackgroundColor", W.col(it.optString("c", "#FFB547")));
                v.setTextViewText(W.id(c, "w_t" + i), it.optString("t"));
                String due = it.optString("d");
                v.setTextViewText(W.id(c, "w_s" + i), due.length() > 0 ? it.optString("g") + "  ·  " + due : it.optString("g"));
                v.setTextColor(W.id(c, "w_s" + i), it.optBoolean("late") ? W.LATE : W.MUTED);
                v.setOnClickPendingIntent(W.id(c, "w_k" + i), W.action(c, WidgetActions.STEP, it.optString("sid"), 60 + i, "g", it.optString("gid"), "s", it.optString("sid")));
            } else v.setViewVisibility(row, View.GONE);
        }
        return v;
    }
}
