package com.munazzar.plotline;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;

public class Reminders {
    static final String CH = "reminders", PREF = "plotline_alarms";
    static final int MAX = 60;

    static void createChannel(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        NotificationChannel ch = new NotificationChannel(CH, "Reminders", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Step reminders, goal check-ins and focus timers");
        ch.enableVibration(true);
        nm.createNotificationChannel(ch);
    }

    static synchronized void schedule(Context c, String json) {
        cancelAll(c);
        c.getSharedPreferences(PREF, 0).edit().putString("list", json).apply();
        arm(c, json);
    }

    /* after a Done from the notification bar, later reminders for the same thing (nudges, second times) are dropped */
    static synchronized void dropFor(Context c, String key, String a, String b) {
        try {
            JSONArray L = new JSONArray(c.getSharedPreferences(PREF, 0).getString("list", "[]")), out = new JSONArray();
            for (int i = 0; i < L.length(); i++) {
                JSONObject o = L.getJSONObject(i);
                boolean same = "habit".equals(key) ? "habit".equals(o.optString("k")) && a.equals(o.optString("hid")) && b.equals(o.optString("hd"))
                        : "step".equals(key) ? "step".equals(o.optString("k")) && a.equals(o.optString("g")) && b.equals(o.optString("s"))
                        : "day".equals(key) && "day".equals(o.optString("k")) && a.equals(o.optString("id"));
                if (!same) out.put(o);
            }
            if (out.length() != L.length()) schedule(c, out.toString());
        } catch (Exception ignored) { }
    }

    static void rearm(Context c) {
        String json = c.getSharedPreferences(PREF, 0).getString("list", "[]");
        cancelAll(c);
        arm(c, json);
    }

    static void arm(Context c, String json) {
        try {
            JSONArray a = new JSONArray(json);
            AlarmManager am = c.getSystemService(AlarmManager.class);
            long now = System.currentTimeMillis();
            boolean exact = Build.VERSION.SDK_INT < 31 || canExact(am);
            int n = 0;
            for (int i = 0; i < a.length() && n < MAX; i++) {
                JSONObject o = a.getJSONObject(i);
                long at = o.getLong("at");
                if (at < now) continue;
                PendingIntent pi = pending(c, n, o);
                try {
                    if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                    else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                } catch (SecurityException se) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                }
                n++;
            }
        } catch (Exception ignored) { }
    }

    static PendingIntent pending(Context c, int req, JSONObject o) {
        Intent it = new Intent(c, AlarmReceiver.class);
        it.putExtra("o", o.toString());
        it.putExtra("id", req);
        return PendingIntent.getBroadcast(c, req, it, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void cancelAll(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        for (int i = 0; i < MAX; i++) {
            PendingIntent p = PendingIntent.getBroadcast(c, i, new Intent(c, AlarmReceiver.class), PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (p != null) { am.cancel(p); p.cancel(); }
        }
    }

    /* one reminder: tapping opens what it's about; Done / Reply / Mute act on it right from the notification bar */
    static void show(Context c, JSONObject o, int id) { show(c, o, id, true); }

    static void show(Context c, JSONObject o, int id, boolean logIt) {
        boolean sum = "summary".equals(o.optString("k"));
        if (!sum && Notify.holdIfMuted(c, o)) { Notify.log(c, o, "held", "", ""); return; }
        if (!sum && logIt) Notify.log(c, o, "sent", o.optString("body"), "");
        createChannel(c);
        int nid = 1000 + id;
        String title = o.optString("title", "Plotline"), body = o.optString("body", "");
        Notification.Builder b = new Notification.Builder(c, CH)
                .setSmallIcon(W.id(c, "ic_stat", "drawable"))
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setColor(0xFFFFB547)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setContentIntent(W.open(c, Notify.route(o), 4000 + id));
        Notify.addActions(c, b, o, nid);
        c.getSystemService(NotificationManager.class).notify(nid, b.build());
    }

    /* AlarmManager.canScheduleExactAlarms() is API 31+; called reflectively so the app compiles against android-30.jar */
    static boolean canExact(android.app.AlarmManager am) {
        try { return (Boolean) am.getClass().getMethod("canScheduleExactAlarms").invoke(am); }
        catch (Exception e) { return false; }
    }
}
