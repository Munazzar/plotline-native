package com.munazzar.plotline;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.widget.RemoteViews;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/* Shared helpers for all Plotline home-screen widgets. Data arrives from the web app through Bridge.widget(). */
final class W {
    static final int LATE = 0xFFFF7A7A;
    /* Theme colours. Defaults are the original dark look; loadTheme() replaces them with the app's current theme
       (sent with the widget data as "th") on Android 12+, where RemoteViews can tint backgrounds. */
    static int TEXT = 0xFFECEAE3, MUTED = 0xFF8F96AC, DIM = 0xFF4F5876, ACCENT = 0xFFFFB547, BG = 0xFA121933, ON = 0xFF1B1300;
    static boolean LIGHT = false, THEMED = false;

    static void loadTheme(Context c) {
        TEXT = 0xFFECEAE3; MUTED = 0xFF8F96AC; DIM = 0xFF4F5876; ACCENT = 0xFFFFB547; BG = 0xFA121933; ON = 0xFF1B1300; LIGHT = false; THEMED = false;
        if (android.os.Build.VERSION.SDK_INT < 31) return;
        try {
            JSONObject th = new JSONObject(prefs(c).getString("data", "{}")).optJSONObject("th");
            if (th == null) return;
            TEXT = Color.parseColor(th.getString("text")); MUTED = Color.parseColor(th.getString("muted")); DIM = Color.parseColor(th.getString("dim"));
            ACCENT = Color.parseColor(th.getString("accent")); BG = Color.parseColor(th.getString("bg")); ON = Color.parseColor(th.getString("on"));
            LIGHT = th.optBoolean("light"); THEMED = true;
        } catch (Exception e) { /* keep defaults */ }
    }

    /* Which theme colour a drawable is tinted with (null = keep its own colours) */
    static Integer tintFor(String name) {
        if (!THEMED || name == null) return null;
        switch (name) {
            case "widget_bg": return BG;
            case "widget_card": return blend(BG | 0xFF000000, TEXT, LIGHT ? 0.05f : 0.08f);
            case "widget_row": case "widget_chip": case "widget_btn_line": case "hd_off": case "hd_na": case "hd_skip": case "hd_today": return TEXT;
            case "widget_chk_off": return DIM;
            case "widget_btn": case "widget_chk_on": case "widget_pill": case "widget_add": return ACCENT;
            default: return null;
        }
    }

    static int blend(int a, int b, float t) {
        int r = Math.round(Color.red(a) + (Color.red(b) - Color.red(a)) * t), g = Math.round(Color.green(a) + (Color.green(b) - Color.green(a)) * t), bl = Math.round(Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t);
        return Color.rgb(r, g, bl);
    }

    static java.lang.reflect.Method CSL;
    static void tint(RemoteViews v, int id, Integer color) {
        if (android.os.Build.VERSION.SDK_INT < 31) return;
        try {
            if (CSL == null) CSL = RemoteViews.class.getMethod("setColorStateList", int.class, String.class, android.content.res.ColorStateList.class);
            CSL.invoke(v, id, "setBackgroundTintList", color == null ? null : android.content.res.ColorStateList.valueOf(color));
        } catch (Throwable ignored) { }
    }

    /* Set a background drawable and the matching theme tint (or clear the tint for coloured ones) */
    static void bgRes(Context c, RemoteViews v, int id, int res) {
        v.setInt(id, "setBackgroundResource", res);
        if (!THEMED) return;
        String n = null; try { if (res != 0) n = c.getResources().getResourceEntryName(res); } catch (Exception ignored) { }
        tint(v, id, tintFor(n));
    }

    static final java.util.HashMap<Integer, int[][]> PAINT = new java.util.HashMap<>();
    static final String NS = "http://schemas.android.com/apk/res/android";

    /* A widget layout with the theme applied: text colours and backgrounds from the layout XML are mapped to
       the app's theme. Builders then set their own per-item colours on top. */
    static RemoteViews rv(Context c, String layout) {
        loadTheme(c);
        int lid = id(c, layout, "layout");
        RemoteViews v = new RemoteViews(c.getPackageName(), lid);
        if (!THEMED) return v;
        int[][] plan = PAINT.get(lid);
        if (plan == null) {
            java.util.ArrayList<int[]> L = new java.util.ArrayList<>();
            try {
                android.content.res.XmlResourceParser x = c.getResources().getLayout(lid);
                for (int ev = x.getEventType(); ev != org.xmlpull.v1.XmlPullParser.END_DOCUMENT; ev = x.next()) {
                    if (ev != org.xmlpull.v1.XmlPullParser.START_TAG) continue;
                    int vid = x.getAttributeResourceValue(NS, "id", 0);
                    if (vid == 0) continue;
                    String tc = x.getAttributeValue(NS, "textColor");
                    int role = 0;
                    if (tc != null) { tc = tc.toLowerCase(Locale.US);
                        if (tc.equals("#ffeceae3")) role = 1; else if (tc.equals("#ff8f96ac")) role = 2; else if (tc.equals("#ff5a6384") || tc.equals("#ff4f5876")) role = 3;
                        else if (tc.equals("#ffffb547")) role = 4; else if (tc.equals("#ff1b1300")) role = 5; }
                    int bg = x.getAttributeResourceValue(NS, "background", 0);
                    L.add(new int[]{vid, role, bg});
                }
                x.close();
            } catch (Exception ignored) { }
            plan = L.toArray(new int[0][]);
            PAINT.put(lid, plan);
        }
        for (int[] p : plan) {
            switch (p[1]) { case 1: v.setTextColor(p[0], TEXT); break; case 2: v.setTextColor(p[0], MUTED); break; case 3: v.setTextColor(p[0], DIM); break; case 4: v.setTextColor(p[0], ACCENT); break; case 5: v.setTextColor(p[0], ON); break; default: }
            if (p[2] != 0) { String n = null; try { n = c.getResources().getResourceEntryName(p[2]); } catch (Exception ignored) { } Integer t = tintFor(n); if (t != null) tint(v, p[0], t); }
        }
        return v;
    }

    interface Builder { RemoteViews build(Context c); }

    static android.content.SharedPreferences prefs(Context c) { return c.getSharedPreferences("plotline_widget", 0); }

    /* Latest data from the app, with any not-yet-applied widget taps layered on top */
    static JSONObject data(Context c) {
        JSONObject o;
        try { o = new JSONObject(prefs(c).getString("data", "{}")); } catch (Exception e) { o = new JSONObject(); }
        JSONArray q = queue(c);
        for (int i = 0; i < q.length(); i++) {
            JSONObject a = q.optJSONObject(i);
            if (a == null) continue;
            if ("day".equals(a.optString("k"))) {
                JSONArray days = o.optJSONArray("days");
                if (days != null) for (int j = 0; j < days.length(); j++) {
                    JSONObject d = days.optJSONObject(j);
                    if (d != null && a.optString("id").equals(d.optString("id"))) try { d.put("done", a.optBoolean("done")); } catch (Exception ignored) { }
                }
                JSONArray up = o.optJSONArray("up");
                if (up != null && a.optBoolean("done")) remove(up, "id", a.optString("id"));
            } else if ("habit".equals(a.optString("k"))) {
                for (String arr : new String[]{"habits", "quits"}) {
                    JSONArray hs = o.optJSONArray(arr);
                    if (hs != null) for (int j = 0; j < hs.length(); j++) {
                        JSONObject h = hs.optJSONObject(j);
                        if (h != null && a.optString("id").equals(h.optString("id"))) try {
                            JSONObject l = h.optJSONObject("l");
                            if (l == null) { l = new JSONObject(); h.put("l", l); }
                            l.put(a.optString("d"), a.optInt("v"));
                        } catch (Exception ignored) { }
                    }
                }
            } else if ("thr".equals(a.optString("k")) || "thrnew".equals(a.optString("k"))) {
                /* typed or said in the Threads widget: show it at the top straight away */
                try {
                    JSONArray ts = o.optJSONArray("thr"); if (ts == null) { ts = new JSONArray(); o.put("thr", ts); }
                    String tid = "thrnew".equals(a.optString("k")) ? a.optString("nid") : a.optString("id");
                    JSONObject t = null; int at = -1;
                    for (int j = 0; j < ts.length(); j++) { JSONObject x = ts.optJSONObject(j); if (x != null && tid.equals(x.optString("id"))) { t = x; at = j; } }
                    if (t == null && "thr".equals(a.optString("k"))) continue;
                    if (t == null) t = new JSONObject().put("id", tid).put("t", a.optString("title", "Thread"));
                    else ts.remove(at);
                    String x = a.optString("x"); if (!x.isEmpty()) t.put("x", x.length() > 90 ? x.substring(0, 89) + "…" : x);
                    String[][] tg = QuickLog.TAGS; for (String[] g : tg) if (g[0].equals(a.optString("kind"))) t.put("e", g[1]);
                    JSONArray nt = new JSONArray(); nt.put(t); for (int j = 0; j < ts.length(); j++) nt.put(ts.opt(j)); o.put("thr", nt);
                } catch (Exception ignored) { }
            } else if ("step".equals(a.optString("k"))) {
                JSONArray items = o.optJSONArray("items");
                if (items != null) remove(items, "sid", a.optString("s"));
                JSONArray up = o.optJSONArray("up");
                if (up != null) remove(up, "sid", a.optString("s"));
            }
        }
        return o;
    }

    static void remove(JSONArray arr, String key, String val) {
        if (val == null || val.length() == 0) return;
        for (int j = arr.length() - 1; j >= 0; j--) {
            JSONObject x = arr.optJSONObject(j);
            if (x != null && val.equals(x.optString(key))) arr.remove(j);
        }
    }

    static JSONArray queue(Context c) {
        try { return new JSONArray(prefs(c).getString("queue", "[]")); } catch (Exception e) { return new JSONArray(); }
    }

    static synchronized void enqueue(Context c, JSONObject a) {
        JSONArray q = queue(c);
        if ("day".equals(a.optString("k"))) remove(q, "id", a.optString("id"));
        if ("habit".equals(a.optString("k"))) for (int j = q.length() - 1; j >= 0; j--) {
            JSONObject x = q.optJSONObject(j);
            if (x != null && "habit".equals(x.optString("k")) && a.optString("id").equals(x.optString("id")) && a.optString("d").equals(x.optString("d"))) q.remove(j);
        }
        q.put(a);
        prefs(c).edit().putString("queue", q.toString()).commit();
    }

    static synchronized int unqueue(Context c, long rid) {
        if (rid == 0) return 0;
        JSONArray q = queue(c); int n = 0;
        for (int j = q.length() - 1; j >= 0; j--) { JSONObject x = q.optJSONObject(j); if (x != null && x.optLong("rid") == rid) { q.remove(j); n++; } }
        prefs(c).edit().putString("queue", q.toString()).commit();
        return n;
    }

    /* Hands queued taps to the app once, then clears them */
    static synchronized String takeQueue(Context c) {
        String s = prefs(c).getString("queue", "[]");
        prefs(c).edit().putString("queue", "[]").commit();
        return s;
    }

    static PendingIntent action(Context c, String act, String key, int req, String... kv) {
        Intent i = new Intent(c, WidgetActions.class);
        i.setAction(act);
        i.setData(android.net.Uri.parse("plotline://w/" + act + "/" + key));
        for (int k = 0; k + 1 < kv.length; k += 2) i.putExtra(kv[k], kv[k + 1]);
        return PendingIntent.getBroadcast(c, req, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /* A progress ring as a bitmap */
    static Bitmap ring(float f, int color) {
        int n = 120; float sw = 13;
        Bitmap b = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(sw); p.setStrokeCap(Paint.Cap.ROUND);
        RectF r = new RectF(sw / 2 + 1, sw / 2 + 1, n - sw / 2 - 1, n - sw / 2 - 1);
        p.setColor((TEXT & 0x00FFFFFF) | 0x2E000000);
        cv.drawArc(r, 0, 360, false, p);
        if (f > 0) { p.setColor(color); cv.drawArc(r, -90, 360 * Math.min(1f, f), false, p); }
        return b;
    }

    static int id(Context c, String name, String type) {
        return c.getResources().getIdentifier(name, type, c.getPackageName());
    }

    static int id(Context c, String name) { return id(c, name, "id"); }

    static PendingIntent open(Context c, String route, int req) {
        Intent i = new Intent(c, MainActivity.class);
        i.setAction("com.munazzar.plotline.ROUTE." + route);
        i.putExtra("route", route);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(c, req, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static int col(String s) {
        try { return Color.parseColor(s); } catch (Exception e) { return ACCENT; }
    }

    static String ymd(Date d) { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d); }

    static String today() { return ymd(new Date()); }

    static String fmt(Date d, String p) { return new SimpleDateFormat(p, Locale.getDefault()).format(d); }

    /* "Today", "Tomorrow" or a short weekday for a yyyy-MM-dd date */
    static String dayLabel(String ds) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(ds);
            Calendar a = Calendar.getInstance(); Calendar b = Calendar.getInstance(); b.setTime(d);
            a.set(Calendar.HOUR_OF_DAY, 12); b.set(Calendar.HOUR_OF_DAY, 12);
            long n = Math.round((b.getTimeInMillis() - a.getTimeInMillis()) / 86400000.0);
            if (n == 0) return "Today";
            if (n == 1) return "Tomorrow";
            if (n < 7) return fmt(d, "EEE");
            return fmt(d, "MMM d");
        } catch (Exception e) { return ""; }
    }

    /* A rounded progress bar drawn as a bitmap, since RemoteViews can't tint progress bars on older Android */
    static Bitmap bar(int pct, int color) {
        int w = 600, h = 14;
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor((color & 0x00FFFFFF) | 0x38000000);
        cv.drawRoundRect(new RectF(0, 0, w, h), h / 2f, h / 2f, p);
        float f = Math.max(0, Math.min(100, pct)) / 100f;
        if (f > 0) {
            p.setColor(color);
            cv.drawRoundRect(new RectF(0, 0, Math.max(h, w * f), h), h / 2f, h / 2f, p);
        }
        return b;
    }

    static void update(Context c, Class<?> cls, Builder bld) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, cls));
        if (ids == null || ids.length == 0) return;
        RemoteViews v = bld.build(c);
        for (int i : ids) m.updateAppWidget(i, v);
    }

    /* ---- habits ---- */
    static JSONObject habit(Context c, String id) {
        JSONObject o = data(c);
        for (String arr : new String[]{"habits", "quits"}) {
            JSONArray hs = o.optJSONArray(arr);
            if (hs != null) for (int j = 0; j < hs.length(); j++) {
                JSONObject h = hs.optJSONObject(j);
                if (h != null && id.equals(h.optString("id"))) return h;
            }
        }
        return null;
    }

    /* value logged on a date; -1 means a rest day */
    static int hval(JSONObject h, String ds) {
        JSONObject l = h.optJSONObject("l");
        return l == null ? 0 : l.optInt(ds, 0);
    }

    static boolean hdue(JSONObject h, String ds) {
        if (ds.compareTo(h.optString("sd", "0000")) < 0) return false;
        String m = h.optString("m", "1111111");
        try {
            Calendar k = Calendar.getInstance();
            k.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(ds));
            int d = k.get(Calendar.DAY_OF_WEEK) - 1;
            return m.length() == 7 && m.charAt(d) == '1';
        } catch (Exception e) { return true; }
    }

    static String daysAgo(int n) { Calendar k = Calendar.getInstance(); k.add(Calendar.DATE, -n); return ymd(k.getTime()); }

    static int dot(Context c, String name) { return id(c, name, "drawable"); }

    /* the drawable for a habit on a date: filled when done, ring when partly done, faint when not due */
    static int cell(Context c, JSONObject h, String ds) {
        String a = h.optString("a", "personal");
        int v = hval(h, ds), n = Math.max(1, h.optInt("n", 1));
        boolean today = ds.equals(today());
        if (ds.compareTo(h.optString("sd", "0000")) < 0) return dot(c, "hd_na");
        if (v < 0) return dot(c, "hd_skip");
        if (v >= n) return dot(c, "hd_" + a);
        if (v > 0) return dot(c, "hdp_" + a);
        if (!hdue(h, ds)) return dot(c, "hd_na");
        if (today) return dot(c, "hd_today");
        return h.optInt("tw", 0) > 0 ? dot(c, "hd_na") : dot(c, "hd_miss");
    }

    static PendingIntent habitTap(Context c, String id, String ds, String mode, int req) {
        return action(c, WidgetActions.HABIT, id + "/" + ds + "/" + mode, req, "id", id, "d", ds, "mode", mode);
    }

    static void updateAll(Context c) {
        HabitsWidget.updateAll(c);
        HabitGridWidget.updateAll(c);
        HabitVistaWidget.updateAll(c);
        StreakWidget.updateAll(c);
        UpNextWidget.updateAll(c);
        TodayWidget.updateAll(c);
        WeekWidget.updateAll(c);
        GoalsWidget.updateAll(c);
        ThreadsWidget.updateAll(c);
    }
}
