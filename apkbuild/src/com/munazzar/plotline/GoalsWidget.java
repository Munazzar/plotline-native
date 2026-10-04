package com.munazzar.plotline;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.view.View;
import android.widget.RemoteViews;
import org.json.JSONArray;
import org.json.JSONObject;

/* Goal progress: a ring per goal in its own color; tap one to open that goal. */
public class GoalsWidget extends AppWidgetProvider {
    static final int ROWS = 4;

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) m.updateAppWidget(id, build(c));
    }

    static void updateAll(Context c) {
        W.update(c, GoalsWidget.class, new W.Builder() { public RemoteViews build(Context x) { return GoalsWidget.build(x); } });
    }

    static RemoteViews build(Context c) {
        RemoteViews v = W.rv(c, "widget_goals");
        v.setOnClickPendingIntent(W.id(c, "w_root"), W.open(c, "goals", 41));
        JSONArray gs = W.data(c).optJSONArray("goals");
        int shown = 0;
        for (int i = 0; i < ROWS; i++) {
            int row = W.id(c, "w_row" + i);
            JSONObject o = gs != null && i < gs.length() ? gs.optJSONObject(i) : null;
            if (o == null) { v.setViewVisibility(row, View.GONE); continue; }
            int col = W.col(o.optString("c", "#FFB547"));
            int p = o.optInt("p");
            v.setViewVisibility(row, View.VISIBLE);
            v.setImageViewBitmap(W.id(c, "w_gr" + i), W.ring(p / 100f, col));
            v.setTextViewText(W.id(c, "w_gp" + i), p + "%");
            v.setTextColor(W.id(c, "w_gp" + i), col);
            v.setTextViewText(W.id(c, "w_ga" + i), o.optString("a").toUpperCase());
            v.setTextColor(W.id(c, "w_ga" + i), col);
            v.setTextViewText(W.id(c, "w_gt" + i), o.optString("t"));
            v.setTextViewText(W.id(c, "w_gn" + i), "Next: " + o.optString("n"));
            if (o.optString("id").length() > 0) v.setOnClickPendingIntent(row, W.open(c, "goal/" + o.optString("id"), 400 + i));
            shown++;
        }
        v.setTextViewText(W.id(c, "w_sum"), gs == null ? "" : gs.length() + " shown");
        v.setViewVisibility(W.id(c, "w_empty"), shown == 0 ? View.VISIBLE : View.GONE);
        return v;
    }
}
