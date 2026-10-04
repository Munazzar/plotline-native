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

/* Habits due today: tap the circle to check off (or add one to a counted habit); routines open the guided player. */
public class HabitsWidget extends AppWidgetProvider {
    static final int ROWS = 6;

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) m.updateAppWidget(id, build(c));
    }

    static void updateAll(Context c) {
        W.update(c, HabitsWidget.class, new W.Builder() { public RemoteViews build(Context x) { return HabitsWidget.build(x); } });
    }

    static RemoteViews build(Context c) {
        RemoteViews v = W.rv(c, "widget_habits");
        v.setOnClickPendingIntent(W.id(c, "w_root"), W.open(c, "habits", 61));
        v.setOnClickPendingIntent(W.id(c, "w_add"), W.open(c, "habits", 62));
        v.setTextViewText(W.id(c, "w_date"), W.fmt(new Date(), "EEEE, MMM d"));
        String td = W.today();
        List<JSONObject> items = new ArrayList<>();
        JSONArray hs = W.data(c).optJSONArray("habits");
        if (hs != null) for (int i = 0; i < hs.length(); i++) {
            JSONObject h = hs.optJSONObject(i);
            if (h != null && W.hdue(h, td) && W.hval(h, td) >= 0) items.add(h);
        }
        int done = 0;
        for (JSONObject h : items) if (W.hval(h, td) >= Math.max(1, h.optInt("n", 1))) done++;
        v.setTextViewText(W.id(c, "w_count"), items.isEmpty() ? "0" : done + "/" + items.size());
        v.setImageViewBitmap(W.id(c, "w_ring"), W.ring(items.isEmpty() ? 0 : done / (float) items.size(), W.ACCENT));
        for (int i = 0; i < ROWS; i++) {
            int row = W.id(c, "w_row" + i);
            if (i >= items.size()) { v.setViewVisibility(row, View.GONE); continue; }
            JSONObject h = items.get(i);
            String id = h.optString("id");
            int n = Math.max(1, h.optInt("n", 1)), val = W.hval(h, td);
            boolean dn = val >= n, routine = "routine".equals(h.optString("k"));
            int tw = h.optInt("tw", 0), streak = h.optInt("sb", 0) + (tw == 0 && dn ? 1 : 0);
            v.setViewVisibility(row, View.VISIBLE);
            v.setTextViewText(W.id(c, "w_e" + i), h.optString("e", "🌱"));
            v.setTextViewText(W.id(c, "w_t" + i), h.optString("t"));
            v.setTextColor(W.id(c, "w_t" + i), dn ? W.MUTED : W.TEXT);
            StringBuilder sub = new StringBuilder();
            if (streak > 0) sub.append("🔥 ").append(streak).append(tw > 0 ? " wk" : " d");
            if (n > 1) { if (sub.length() > 0) sub.append("  ·  "); sub.append(val).append(" of ").append(n).append(routine ? " steps" : (h.optString("u").length() > 0 ? " " + h.optString("u") : "")); }
            else if (tw > 0) { if (sub.length() > 0) sub.append("  ·  "); sub.append(tw).append("× a week"); }
            v.setTextViewText(W.id(c, "w_s" + i), sub.length() > 0 ? sub.toString() : "Tap the circle when it’s done");
            int chk = W.id(c, "w_c" + i);
            W.bgRes(c, v, chk, dn ? W.dot(c, "hd_" + h.optString("a", "personal")) : val > 0 ? W.dot(c, "hdp_" + h.optString("a", "personal")) : W.dot(c, "hd_off"));
            v.setTextViewText(chk, dn ? "✓" : routine ? "▶" : n > 1 ? val + "/" + n : "");
            v.setTextColor(chk, dn ? 0xFF0B1020 : W.TEXT);
            v.setOnClickPendingIntent(chk, routine && !dn ? W.open(c, "routine-" + id, 600 + i) : W.habitTap(c, id, td, "toggle", 610 + i));
            v.setOnClickPendingIntent(row, W.open(c, "habit/" + id, 620 + i));
        }
        v.setViewVisibility(W.id(c, "w_more"), items.size() > ROWS ? View.VISIBLE : View.GONE);
        v.setTextViewText(W.id(c, "w_more"), "+" + Math.max(0, items.size() - ROWS) + " more in the app");
        v.setViewVisibility(W.id(c, "w_empty"), items.isEmpty() ? View.VISIBLE : View.GONE);
        return v;
    }
}
