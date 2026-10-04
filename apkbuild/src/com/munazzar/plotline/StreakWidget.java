package com.munazzar.plotline;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;
import org.json.JSONArray;
import org.json.JSONObject;

/* A habit you're breaking: live clean-time clock (a Chronometer, so it ticks without updates) or today's count against a limit. */
public class StreakWidget extends AppWidgetProvider {
    static final String CYCLE = "com.munazzar.plotline.STREAK_CYCLE", TICK = "com.munazzar.plotline.STREAK_TICK";

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) m.updateAppWidget(id, build(c));
    }

    @Override
    public void onReceive(Context c, Intent i) {
        if (CYCLE.equals(i.getAction())) {
            W.prefs(c).edit().putInt("streak_i", W.prefs(c).getInt("streak_i", 0) + 1).apply();
            updateAll(c);
        } else if (TICK.equals(i.getAction())) updateAll(c);
        else super.onReceive(c, i);
    }

    static void updateAll(Context c) {
        W.update(c, StreakWidget.class, new W.Builder() { public RemoteViews build(Context x) { return StreakWidget.build(x); } });
    }

    static PendingIntent self(Context c, String act, int req) {
        Intent i = new Intent(c, StreakWidget.class);
        i.setAction(act);
        return PendingIntent.getBroadcast(c, req, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static RemoteViews build(Context c) {
        RemoteViews v = W.rv(c, "widget_streak");
        JSONArray qs = W.data(c).optJSONArray("quits");
        int n = qs == null ? 0 : qs.length();
        boolean empty = n == 0;
        int[] body = {W.id(c, "w_days"), W.id(c, "w_du"), W.id(c, "w_clock"), W.id(c, "w_sub"), W.id(c, "w_btns"), W.id(c, "w_e")};
        for (int b : body) v.setViewVisibility(b, empty ? View.GONE : View.VISIBLE);
        v.setViewVisibility(W.id(c, "w_empty"), empty ? View.VISIBLE : View.GONE);
        v.setOnClickPendingIntent(W.id(c, "w_root"), W.open(c, "habits", 81));
        if (empty) { v.setTextViewText(W.id(c, "w_t"), "Nothing to break yet"); v.setTextViewText(W.id(c, "w_l"), "CLEAN STREAK"); v.setViewVisibility(W.id(c, "w_next"), View.GONE); return v; }
        int idx = Math.abs(W.prefs(c).getInt("streak_i", 0)) % n;
        JSONObject h = qs.optJSONObject(idx);
        String id = h.optString("id");
        int col = W.col(h.optString("c", "#FFB547"));
        v.setTextViewText(W.id(c, "w_e"), h.optString("e", "🛡️"));
        v.setTextViewText(W.id(c, "w_t"), h.optString("t"));
        v.setTextColor(W.id(c, "w_l"), col);
        v.setViewVisibility(W.id(c, "w_next"), n > 1 ? View.VISIBLE : View.GONE);
        v.setOnClickPendingIntent(W.id(c, "w_next"), self(c, CYCLE, 82));
        v.setOnClickPendingIntent(W.id(c, "w_root"), W.open(c, "habit/" + id, 83));
        v.setOnClickPendingIntent(W.id(c, "w_urge"), W.open(c, "urge-" + id, 84));
        if ("limit".equals(h.optString("mode"))) {
            String td = W.today();
            int val = Math.max(0, W.hval(h, td)), lim = h.optInt("lim", 0);
            v.setTextViewText(W.id(c, "w_l"), "CUTTING DOWN");
            v.setTextViewText(W.id(c, "w_days"), String.valueOf(val));
            v.setTextColor(W.id(c, "w_days"), val > lim ? W.LATE : W.TEXT);
            v.setTextViewText(W.id(c, "w_du"), "of " + lim + (h.optString("u").length() > 0 ? " " + h.optString("u") : "") + " today");
            v.setViewVisibility(W.id(c, "w_clock"), View.GONE);
            int ls = h.optInt("ls", 0);
            v.setTextViewText(W.id(c, "w_sub"), val > lim ? "Over today · tomorrow is a fresh start" : (lim - val) + " left today" + (ls > 1 ? " · " + ls + " days on track" : ""));
            v.setTextViewText(W.id(c, "w_act"), "+1");
            v.setOnClickPendingIntent(W.id(c, "w_act"), W.habitTap(c, id, td, "inc", 85));
        } else {
            long since = h.optLong("since", System.currentTimeMillis()), now = System.currentTimeMillis(), ms = Math.max(0, now - since);
            long days = ms / 86400000L;
            v.setTextViewText(W.id(c, "w_l"), "BREAKING");
            v.setTextColor(W.id(c, "w_days"), W.TEXT);
            v.setTextViewText(W.id(c, "w_days"), String.valueOf(days));
            v.setTextViewText(W.id(c, "w_du"), days == 1 ? "day" : "days");
            v.setViewVisibility(W.id(c, "w_clock"), View.VISIBLE);
            v.setChronometer(W.id(c, "w_clock"), SystemClock.elapsedRealtime() - (ms % 86400000L), null, true);
            StringBuilder sub = new StringBuilder("Best " + h.optInt("best") + " d");
            if (h.optString("saved").length() > 0) sub.append(" · ").append(h.optString("saved"));
            if (h.optString("next").length() > 0) sub.append(" · next ").append(h.optString("next"));
            v.setTextViewText(W.id(c, "w_sub"), sub.toString());
            v.setTextViewText(W.id(c, "w_act"), "Log a slip");
            v.setOnClickPendingIntent(W.id(c, "w_act"), W.open(c, "slip-" + id, 86));
            armRollover(c, since + (days + 1) * 86400000L + 1000);
        }
        return v;
    }

    /* the Chronometer shows hours within the day; re-render when the day count ticks over */
    static void armRollover(Context c, long at) {
        try {
            AlarmManager am = c.getSystemService(AlarmManager.class);
            PendingIntent pi = self(c, TICK, 87);
            if (Build.VERSION.SDK_INT < 31 || Reminders.canExact(am)) am.setExactAndAllowWhileIdle(AlarmManager.RTC, at, pi);
            else am.setAndAllowWhileIdle(AlarmManager.RTC, at, pi);
        } catch (Exception ignored) { }
    }
}
