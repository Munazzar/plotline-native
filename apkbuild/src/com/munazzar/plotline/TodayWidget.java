package com.munazzar.plotline;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.view.View;
import android.widget.RemoteViews;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/* Today's goals: tap a row to check it off right from the home screen. */
public class TodayWidget extends AppWidgetProvider {
    static final int ROWS = 5;

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) m.updateAppWidget(id, build(c));
    }

    static void updateAll(Context c) {
        W.update(c, TodayWidget.class, new W.Builder() { public RemoteViews build(Context x) { return TodayWidget.build(x); } });
    }

    static RemoteViews build(Context c) {
        RemoteViews v = W.rv(c, "widget_today");
        v.setOnClickPendingIntent(W.id(c, "w_root"), W.open(c, "today", 21));
        v.setOnClickPendingIntent(W.id(c, "w_add"), W.open(c, "add", 22));
        v.setTextViewText(W.id(c, "w_date"), W.fmt(new Date(), "EEEE, MMM d"));
        List<JSONObject> items = new ArrayList<>();
        String td = W.today();
        JSONArray days = W.data(c).optJSONArray("days");
        if (days != null) for (int i = 0; i < days.length(); i++) {
            JSONObject o = days.optJSONObject(i);
            if (o != null && td.equals(o.optString("d"))) items.add(o);
        }
        int done = 0;
        for (JSONObject o : items) if (o.optBoolean("done")) done++;
        v.setTextViewText(W.id(c, "w_count"), items.isEmpty() ? "0" : done + "/" + items.size());
        v.setImageViewBitmap(W.id(c, "w_ring"), W.ring(items.isEmpty() ? 0 : done / (float) items.size(), W.ACCENT));
        for (int i = 0; i < ROWS; i++) {
            int row = W.id(c, "w_row" + i);
            if (i < items.size()) {
                JSONObject o = items.get(i);
                boolean d = o.optBoolean("done");
                v.setViewVisibility(row, View.VISIBLE);
                W.bgRes(c, v, W.id(c, "w_c" + i), W.id(c, d ? "widget_chk_on" : "widget_chk_off", "drawable"));
                v.setTextViewText(W.id(c, "w_c" + i), d ? "✓" : "");
                v.setTextViewText(W.id(c, "w_t" + i), o.optString("t"));
                v.setTextColor(W.id(c, "w_t" + i), d ? W.DIM : W.TEXT);
                v.setTextViewText(W.id(c, "w_m" + i), o.optString("time"));
                v.setTextColor(W.id(c, "w_d" + i), W.col(o.optString("c", "#FFB547")));
                v.setOnClickPendingIntent(row, W.action(c, WidgetActions.DAY, o.optString("id"), 100 + i, "id", o.optString("id")));
            } else v.setViewVisibility(row, View.GONE);
        }
        v.setViewVisibility(W.id(c, "w_more"), items.size() > ROWS ? View.VISIBLE : View.GONE);
        v.setTextViewText(W.id(c, "w_more"), "+" + Math.max(0, items.size() - ROWS) + " more in the app");
        v.setViewVisibility(W.id(c, "w_empty"), items.isEmpty() ? View.VISIBLE : View.GONE);
        return v;
    }
}
