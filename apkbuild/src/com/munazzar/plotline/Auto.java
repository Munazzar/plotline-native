package com.munazzar.plotline;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;

/*
 * Automatic check-offs, all on the phone and each one opt-in per item:
 *  - steps from the phone's step counter (no account, no cloud)
 *  - workouts and sleep from Health Connect (Android 14+, read only)
 *  - screen time in chosen apps (Usage access)
 *  - places: at a place for N minutes, or a reminder when you arrive / leave (proximity alerts)
 * The app sends the rules with setRules(); this receiver checks them every ~30 minutes and on place events,
 * queues the check-off for the app (same queue as widgets), logs it in the item's notification history and
 * tells you with an Undo button.
 */
public class Auto extends BroadcastReceiver {
    static final String TICK = "com.munazzar.plotline.A_TICK", PLACE = "com.munazzar.plotline.A_PLACE", DWELL = "com.munazzar.plotline.A_DWELL", PREF = "plotline_auto";

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREF, 0); }
    static JSONObject cfg(Context c) { try { return new JSONObject(prefs(c).getString("cfg", "{}")); } catch (Exception e) { return new JSONObject(); } }
    static String today() { return new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date()); }

    /* ---------- set up from the app ---------- */
    static synchronized void setRules(Context c, String json) {
        prefs(c).edit().putString("cfg", json).apply();
        arm(c);
    }

    static void arm(Context c) {
        JSONObject cf = cfg(c);
        JSONArray rules = cf.optJSONArray("rules");
        boolean periodic = false;
        if (rules != null) for (int i = 0; i < rules.length(); i++) { String t = rules.optJSONObject(i).optString("type"); if (!"place".equals(t)) periodic = true; }
        AlarmManager am = c.getSystemService(AlarmManager.class);
        PendingIntent tick = PendingIntent.getBroadcast(c, 610, new Intent(c, Auto.class).setAction(TICK), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (periodic || cf.optBoolean("steps")) am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 60000, AlarmManager.INTERVAL_HALF_HOUR, tick);
        else am.cancel(tick);
        armPlaces(c, cf);
    }

    /* ---------- places ---------- */
    static void armPlaces(Context c, JSONObject cf) {
        LocationManager lm = c.getSystemService(LocationManager.class);
        SharedPreferences p = prefs(c);
        Set<String> old = new HashSet<>(p.getStringSet("placeIds", new HashSet<String>()));
        for (String id : old) { PendingIntent pi = placePI(c, id, PendingIntent.FLAG_NO_CREATE); if (pi != null) { try { lm.removeProximityAlert(pi); } catch (Exception ignored) { } pi.cancel(); } }
        Set<String> now = new HashSet<>();
        JSONArray places = cf.optJSONArray("places"), rules = cf.optJSONArray("rules");
        Set<String> used = new HashSet<>();
        if (rules != null) for (int i = 0; i < rules.length(); i++) { JSONObject r = rules.optJSONObject(i); if ("place".equals(r.optString("type"))) used.add(r.optString("place")); }
        if (places != null && hasLoc(c)) for (int i = 0; i < places.length(); i++) {
            JSONObject pl = places.optJSONObject(i); String id = pl.optString("id");
            /* every saved place is watched: rules use it, and the Activity page counts time there */
            try { lm.addProximityAlert(pl.optDouble("lat"), pl.optDouble("lng"), (float) Math.max(60, pl.optDouble("r", 150)), -1, placePI(c, id, PendingIntent.FLAG_UPDATE_CURRENT)); now.add(id); }
            catch (SecurityException se) { } catch (Exception ignored) { }
        }
        p.edit().putStringSet("placeIds", now).apply();
    }

    static PendingIntent placePI(Context c, String id, int flag) {
        Intent i = new Intent(c, Auto.class).setAction(PLACE).setData(android.net.Uri.parse("plotline://place/" + id)).putExtra("place", id);
        return PendingIntent.getBroadcast(c, 620 + Math.abs(id.hashCode() % 300), i, flag | FLAG_MUTABLE_OR_IMMUTABLE());
    }
    /* proximity alerts add an extra to the intent, so it must be mutable on Android 12+ */
    static int FLAG_MUTABLE_OR_IMMUTABLE() { return Build.VERSION.SDK_INT >= 31 ? Notify.FLAG_MUTABLE : 0; }

    static boolean hasLoc(Context c) { return c.checkSelfPermission("android.permission.ACCESS_FINE_LOCATION") == PackageManager.PERMISSION_GRANTED; }
    static boolean hasBgLoc(Context c) { return Build.VERSION.SDK_INT < 29 || c.checkSelfPermission("android.permission.ACCESS_BACKGROUND_LOCATION") == PackageManager.PERMISSION_GRANTED; }

    static void hereAsync(final MainActivity a) { new Thread(new Runnable() { public void run() { String r = here(a); a.js("window.__here&&window.__here(" + JSONObject.quote(r) + ")"); } }).start(); }

    static String here(Context c) {
        try {
            if (!hasLoc(c)) return "{\"err\":\"perm\"}";
            LocationManager lm = c.getSystemService(LocationManager.class);
            Location best = null;
            for (String pr : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER}) {
                try { Location l = lm.getLastKnownLocation(pr); if (l != null && (best == null || l.getTime() > best.getTime())) best = l; } catch (Exception ignored) { }
            }
            if (best == null || System.currentTimeMillis() - best.getTime() > 10 * 60000) {
                final Location[] got = {null}; final CountDownLatch done = new CountDownLatch(1);
                if (Build.VERSION.SDK_INT >= 30) {
                    String pr = lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER;
                    lm.getCurrentLocation(pr, null, c.getMainExecutor(), new java.util.function.Consumer<Location>() { public void accept(Location l) { got[0] = l; done.countDown(); } });
                    done.await(20, TimeUnit.SECONDS);
                    if (got[0] != null) best = got[0];
                }
            }
            if (best == null) return "{\"err\":\"nofix\"}";
            return new JSONObject().put("lat", best.getLatitude()).put("lng", best.getLongitude()).put("acc", best.getAccuracy()).toString();
        } catch (Exception e) { return "{\"err\":\"" + e.getClass().getSimpleName() + "\"}"; }
    }

    /* ---------- receiver ---------- */
    @Override
    public void onReceive(final Context c, final Intent i) {
        final PendingResult pr = goAsync();
        new Thread(new Runnable() { public void run() {
            try {
                String a = i.getAction();
                if (TICK.equals(a)) { sampleSteps(c); check(c, null); }
                else if (PLACE.equals(a)) place(c, i.getStringExtra("place"), i.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false));
                else if (DWELL.equals(a)) dwell(c, i.getStringExtra("place"));
            } catch (Exception ignored) { }
            finally { pr.finish(); }
        } }).start();
    }

    static void place(Context c, String id, boolean entering) {
        if (id == null) return;
        SharedPreferences p = prefs(c);
        if (entering) {
            p.edit().putLong("in:" + id, System.currentTimeMillis()).apply();
            JSONArray rules = cfg(c).optJSONArray("rules");
            long minDwell = Long.MAX_VALUE;
            if (rules != null) for (int k = 0; k < rules.length(); k++) {
                JSONObject r = rules.optJSONObject(k);
                if (!"place".equals(r.optString("type")) || !id.equals(r.optString("place")) || !dueToday(r)) continue;
                int mins = r.optInt("mins", 0);
                if ("arrive".equals(r.optString("when"))) remind(c, r, "You’re at " + placeName(c, id));
                else if (mins <= 0) fire(c, r, "You arrived at " + placeName(c, id));
                else minDwell = Math.min(minDwell, mins);
            }
            if (minDwell != Long.MAX_VALUE) {
                AlarmManager am = c.getSystemService(AlarmManager.class);
                Intent d = new Intent(c, Auto.class).setAction(DWELL).setData(android.net.Uri.parse("plotline://dwell/" + id)).putExtra("place", id);
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + minDwell * 60000L, PendingIntent.getBroadcast(c, 950 + Math.abs(id.hashCode() % 40), d, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            }
        } else {
            long in = p.getLong("in:" + id, 0);
            p.edit().remove("in:" + id).apply();
            if (in > 0) { String pk = "pv:" + dayOf(in) + ":" + id; long add = (System.currentTimeMillis() - in) / 60000; if (add > 0) prefs(c).edit().putLong(pk, prefs(c).getLong(pk, 0) + add).apply(); }
            JSONArray rules = cfg(c).optJSONArray("rules");
            if (rules != null) for (int k = 0; k < rules.length(); k++) {
                JSONObject r = rules.optJSONObject(k);
                if (!"place".equals(r.optString("type")) || !id.equals(r.optString("place"))) continue;
                if ("leave".equals(r.optString("when"))) remind(c, r, "You left " + placeName(c, id));
                else if (in > 0 && r.optInt("mins", 0) > 0 && System.currentTimeMillis() - in >= r.optInt("mins") * 60000L) fire(c, r, "You spent " + ((System.currentTimeMillis() - in) / 60000) + " min at " + placeName(c, id));
            }
        }
    }

    static void dwell(Context c, String id) {
        long in = prefs(c).getLong("in:" + id, 0);
        if (in == 0) return;
        long stayed = (System.currentTimeMillis() - in) / 60000;
        JSONArray rules = cfg(c).optJSONArray("rules");
        if (rules != null) for (int k = 0; k < rules.length(); k++) {
            JSONObject r = rules.optJSONObject(k);
            if ("place".equals(r.optString("type")) && id.equals(r.optString("place")) && !"arrive".equals(r.optString("when")) && !"leave".equals(r.optString("when")) && stayed >= r.optInt("mins", 0))
                fire(c, r, stayed + " min at " + placeName(c, id));
        }
    }

    static String placeName(Context c, String id) {
        JSONArray pl = cfg(c).optJSONArray("places");
        if (pl != null) for (int i = 0; i < pl.length(); i++) if (id.equals(pl.optJSONObject(i).optString("id"))) return pl.optJSONObject(i).optString("name", "the place");
        return "the place";
    }

    /* ---------- the periodic check ---------- */
    static void check(Context c, String only) {
        JSONArray rules = cfg(c).optJSONArray("rules");
        if (rules == null) return;
        Calendar now = Calendar.getInstance();
        int minute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        Map<String, Long> hc = null;
        for (int k = 0; k < rules.length(); k++) {
            JSONObject r = rules.optJSONObject(k); String t = r.optString("type");
            if (only != null && !only.equals(r.optString("key"))) continue;
            if (!dueToday(r)) continue;
            try {
                if ("steps".equals(t)) { long s = stepsToday(c); if (s >= r.optLong("n", 8000)) fire(c, r, String.format(java.util.Locale.getDefault(), "%,d steps today", s)); }
                else if ("screen".equals(t)) {
                    int at = hm(r.optString("at", "21:00"));
                    if (minute < at) continue;
                    long used = screenMinutes(c, r.optJSONArray("apps"));
                    if (used < 0) continue;
                    if (r.optBoolean("log")) logValue(c, r, (int) used);
                    else if (used <= r.optLong("max", 60)) fire(c, r, used + " min in those apps today");
                } else if ("workout".equals(t) || "sleep".equals(t) || "hcsteps".equals(t)) {
                    if (hc == null) hc = health(c);
                    if (hc == null) continue;
                    if ("workout".equals(t)) { Long m = hc.get("ex:" + r.optString("kind", "any")); if (m != null && m >= r.optLong("mins", 20)) fire(c, r, m + " min of exercise today"); }
                    else if ("sleep".equals(t)) { Long m = hc.get("sleep"); if (m != null && m >= Math.round(r.optDouble("hours", 7) * 60)) fire(c, r, String.format(java.util.Locale.getDefault(), "%.1f h of sleep last night", m / 60.0)); }
                    else { Long s = hc.get("steps"); if (s != null && s >= r.optLong("n", 8000)) fire(c, r, String.format(java.util.Locale.getDefault(), "%,d steps today", s)); }
                }
            } catch (Exception ignored) { }
        }
    }

    static boolean dueToday(JSONObject r) {
        String on = r.optString("date", "");
        if (!on.isEmpty() && !on.equals(today())) return false;
        String m = r.optString("m", "1111111");
        int d = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;
        return m.length() != 7 || m.charAt(d) == '1';
    }

    static int hm(String s) { try { String[] p = s.split(":"); return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]); } catch (Exception e) { return 21 * 60; } }

    /* mark it done (once a day per item), queue for the app, log, and tell the person with Undo */
    static void fire(Context c, JSONObject r, String why) {
        String key = r.optString("key"), d = today(), mark = "done:" + key + ":" + d;
        SharedPreferences p = prefs(c);
        if (p.getBoolean(mark, false)) return;
        p.edit().putBoolean(mark, true).apply();
        try {
            long rid = System.currentTimeMillis();
            JSONObject a = new JSONObject(); a.put("at", rid); a.put("rid", rid);
            String k = r.optString("kind");
            if ("habit".equals(k)) { a.put("k", "habit"); a.put("id", r.optString("hid")); a.put("d", d); a.put("v", Math.max(1, r.optInt("hv", 1))); }
            else if ("step".equals(k)) { a.put("k", "step"); a.put("g", r.optString("g")); a.put("s", r.optString("s")); }
            else if ("day".equals(k)) { a.put("k", "day"); a.put("id", r.optString("id")); a.put("done", true); }
            else return;
            W.enqueue(c, a);
            JSONObject o = item(r, d);
            int nid = 1700 + Math.abs(key.hashCode() % 200);
            Notify.prefs(c).edit().putString("undo" + nid, new JSONObject().put("rid", rid).toString()).apply();
            String msg = "⚡ Done automatically · " + why;
            Notify.log(c, o, "auto", "", msg);
            if (r.optBoolean("tell", true)) Notify.confirm(c, nid, o, msg, null, true);
            W.updateAll(c);
        } catch (Exception ignored) { }
    }

    static void logValue(Context c, JSONObject r, int v) {
        try {
            String d = today();
            JSONObject a = new JSONObject(); a.put("at", System.currentTimeMillis()); a.put("k", "habit"); a.put("id", r.optString("hid")); a.put("d", d); a.put("v", v); a.put("set", true);
            W.enqueue(c, a);
            String mark = "logged:" + r.optString("key") + ":" + d;
            if (!prefs(c).getBoolean(mark, false)) { prefs(c).edit().putBoolean(mark, true).apply(); Notify.log(c, item(r, d), "auto", "", "⚡ Logged " + v + " min of screen time"); }
        } catch (Exception ignored) { }
    }

    static void remind(Context c, JSONObject r, String why) {
        try {
            JSONObject o = item(r, today());
            o.put("body", why);
            Reminders.show(c, o, 700 + Math.abs(r.optString("key").hashCode() % 200));
        } catch (Exception ignored) { }
    }

    static JSONObject item(JSONObject r, String d) throws Exception {
        JSONObject o = new JSONObject();
        String k = r.optString("kind");
        o.put("k", k); o.put("title", r.optString("title"));
        if ("habit".equals(k)) { o.put("hid", r.optString("hid")); o.put("hd", d); o.put("hv", r.optInt("hv", 1)); }
        else if ("step".equals(k)) { o.put("g", r.optString("g")); o.put("s", r.optString("s")); }
        else if ("day".equals(k)) { o.put("id", r.optString("id")); o.put("d", d); }
        return o;
    }

    /* ---------- steps from the phone's step counter ---------- */
    static boolean stepsSupported(Context c) { SensorManager sm = c.getSystemService(SensorManager.class); return sm != null && sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null; }
    static boolean stepsPerm(Context c) { return Build.VERSION.SDK_INT < 29 || c.checkSelfPermission("android.permission.ACTIVITY_RECOGNITION") == PackageManager.PERMISSION_GRANTED; }

    /* the counter counts since the phone started; each day's steps = now − the last reading of yesterday (or the first of today) */
    static synchronized void sampleSteps(Context c) {
        if (!stepsSupported(c) || !stepsPerm(c)) return;
        final SensorManager sm = c.getSystemService(SensorManager.class);
        final float[] v = {-1}; final CountDownLatch done = new CountDownLatch(1);
        SensorEventListener l = new SensorEventListener() {
            public void onSensorChanged(SensorEvent e) { v[0] = e.values[0]; done.countDown(); }
            public void onAccuracyChanged(Sensor s, int a) { }
        };
        sm.registerListener(l, sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER), SensorManager.SENSOR_DELAY_NORMAL);
        try { done.await(8, TimeUnit.SECONDS); } catch (InterruptedException ignored) { }
        sm.unregisterListener(l);
        if (v[0] < 0) return;
        SharedPreferences p = prefs(c); String d = today();
        long cur = (long) v[0], last = p.getLong("stLast", -1), base = p.getLong("stBase:" + d, -1);
        SharedPreferences.Editor e = p.edit();
        if (base < 0) { base = (last >= 0 && last <= cur && p.getString("stLastDay", "").length() > 0) ? last : cur; e.putLong("stBase:" + d, base); }
        if (cur < last) { long carried = p.getLong("stDay:" + d, 0); e.putLong("stBase:" + d, cur - carried); base = cur - carried; }   /* the phone restarted */
        e.putLong("stDay:" + d, Math.max(0, cur - base)).putLong("stLast", cur).putString("stLastDay", d).apply();
    }

    static long stepsToday(Context c) { return prefs(c).getLong("stDay:" + today(), 0); }

    /* ---------- screen time ---------- */
    static boolean usageAccess(Context c) {
        try {
            android.app.AppOpsManager ops = c.getSystemService(android.app.AppOpsManager.class);
            int m = ops.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), c.getPackageName());
            return m == android.app.AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) { return false; }
    }

    /* minutes in the given apps since midnight, or −1 without access */
    static long screenMinutes(Context c, JSONArray apps) {
        if (!usageAccess(c) || apps == null) return -1;
        Set<String> want = new HashSet<>(); for (int i = 0; i < apps.length(); i++) want.add(apps.optString(i));
        Calendar m = Calendar.getInstance(); m.set(Calendar.HOUR_OF_DAY, 0); m.set(Calendar.MINUTE, 0); m.set(Calendar.SECOND, 0);
        UsageStatsManager us = c.getSystemService(UsageStatsManager.class);
        UsageEvents ev = us.queryEvents(m.getTimeInMillis(), System.currentTimeMillis());
        Map<String, Long> open = new HashMap<>(); long total = 0; UsageEvents.Event e = new UsageEvents.Event();
        while (ev.hasNextEvent()) {
            ev.getNextEvent(e); String pk = e.getPackageName(); if (!want.contains(pk)) continue;
            int t = e.getEventType();
            if (t == 1) open.put(pk, e.getTimeStamp());          /* ACTIVITY_RESUMED */
            else if (t == 2 || t == 23) { Long s = open.remove(pk); if (s != null) total += e.getTimeStamp() - s; }  /* PAUSED / STOPPED */
        }
        for (Long s : open.values()) total += System.currentTimeMillis() - s;
        return total / 60000;
    }

    static String apps(Context c) {
        try {
            PackageManager pm = c.getPackageManager();
            Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> L = pm.queryIntentActivities(i, 0);
            JSONArray out = new JSONArray(); Set<String> seen = new HashSet<>();
            for (ResolveInfo r : L) {
                String pk = r.activityInfo.packageName; if (seen.contains(pk) || pk.equals(c.getPackageName())) continue; seen.add(pk);
                ApplicationInfo ai = r.activityInfo.applicationInfo;
                int cat = Build.VERSION.SDK_INT >= 26 ? ai.category : -1;
                out.put(new JSONObject().put("pk", pk).put("name", String.valueOf(r.loadLabel(pm))).put("cat", cat));
            }
            return out.toString();
        } catch (Exception e) { return "[]"; }
    }

    /* ---------- Health Connect (Android 14+, platform API through reflection) ---------- */
    static final String[] HC_PERMS = {"android.permission.health.READ_EXERCISE", "android.permission.health.READ_SLEEP", "android.permission.health.READ_STEPS"};
    static boolean hcSupported(Context c) { if (Build.VERSION.SDK_INT < 34) return false; try { Class.forName("android.health.connect.HealthConnectManager"); return true; } catch (Exception e) { return false; } }
    static boolean hcGranted(Context c) { if (!hcSupported(c)) return false; for (String p : HC_PERMS) if (c.checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) return false; return true; }

    /* today's exercise minutes per type ("ex:any", "ex:56" …), last night's sleep minutes, and today's steps */
    static Map<String, Long> health(Context c) {
        if (!hcGranted(c)) return null;
        Map<String, Long> out = new HashMap<>();
        try {
            Calendar m = Calendar.getInstance(); m.set(Calendar.HOUR_OF_DAY, 0); m.set(Calendar.MINUTE, 0); m.set(Calendar.SECOND, 0);
            long t0 = m.getTimeInMillis(), now = System.currentTimeMillis();
            for (Object r : hcRead(c, "android.health.connect.datatypes.ExerciseSessionRecord", t0, now)) {
                long s = inst(r, "getStartTime"), e = inst(r, "getEndTime"); long mins = (Math.min(e, now) - Math.max(s, t0)) / 60000; if (mins <= 0) continue;
                int type = (Integer) r.getClass().getMethod("getExerciseType").invoke(r);
                out.put("ex:any", (out.containsKey("ex:any") ? out.get("ex:any") : 0) + mins);
                out.put("ex:" + type, (out.containsKey("ex:" + type) ? out.get("ex:" + type) : 0) + mins);
            }
            long sleep = 0;
            for (Object r : hcRead(c, "android.health.connect.datatypes.SleepSessionRecord", t0 - 12 * 3600000L, now)) {
                long s = inst(r, "getStartTime"), e = inst(r, "getEndTime"); if (e < t0 - 6 * 3600000L) continue; sleep += Math.max(0, e - s) / 60000;
            }
            out.put("sleep", sleep);
            long steps = 0;
            for (Object r : hcRead(c, "android.health.connect.datatypes.StepsRecord", t0, now)) steps += (Long) r.getClass().getMethod("getCount").invoke(r);
            out.put("steps", steps);
        } catch (Throwable e) { return out.isEmpty() ? null : out; }
        return out;
    }

    static long inst(Object r, String m) throws Exception { Object i = r.getClass().getMethod(m).invoke(r); return (Long) i.getClass().getMethod("toEpochMilli").invoke(i); }

    @SuppressWarnings("unchecked")
    static List<Object> hcRead(Context c, String type, long from, long to) throws Exception {
        Class<?> rec = Class.forName(type);
        Class<?> tf = Class.forName("android.health.connect.TimeInstantRangeFilter$Builder");
        Object fb = tf.getConstructor().newInstance();
        Class<?> instant = Class.forName("java.time.Instant");
        Method ofMs = instant.getMethod("ofEpochMilli", long.class);
        fb.getClass().getMethod("setStartTime", instant).invoke(fb, ofMs.invoke(null, from));
        fb.getClass().getMethod("setEndTime", instant).invoke(fb, ofMs.invoke(null, to));
        Object filter = fb.getClass().getMethod("build").invoke(fb);
        Class<?> rb = Class.forName("android.health.connect.ReadRecordsRequestUsingFilters$Builder");
        Object b = rb.getConstructor(Class.class).newInstance(rec);
        b.getClass().getMethod("setTimeRangeFilter", Class.forName("android.health.connect.TimeRangeFilter")).invoke(b, filter);
        Object req = b.getClass().getMethod("build").invoke(b);
        Object mgr = c.getSystemService(Class.forName("android.health.connect.HealthConnectManager"));
        final Object[] res = {null}; final Throwable[] err = {null}; final CountDownLatch done = new CountDownLatch(1);
        Class<?> or = Class.forName("android.os.OutcomeReceiver");
        Object cb = Proxy.newProxyInstance(or.getClassLoader(), new Class<?>[]{or}, new InvocationHandler() {
            public Object invoke(Object p, Method m, Object[] a) {
                if ("onResult".equals(m.getName())) { res[0] = a[0]; done.countDown(); }
                else if ("onError".equals(m.getName())) { err[0] = a != null && a.length > 0 && a[0] instanceof Throwable ? (Throwable) a[0] : new Exception("error"); done.countDown(); }
                return null;
            }
        });
        Method read = null;
        for (Method m : mgr.getClass().getMethods()) if (m.getName().equals("readRecords") && m.getParameterTypes().length == 3) read = m;
        read.invoke(mgr, req, c.getMainExecutor(), cb);
        done.await(15, TimeUnit.SECONDS);
        if (err[0] != null) throw new Exception(err[0]);
        if (res[0] == null) return new java.util.ArrayList<>();
        return (List<Object>) res[0].getClass().getMethod("getRecords").invoke(res[0]);
    }

    /* ---------- status for the app ---------- */
    static String status(Context c) {
        try {
            JSONObject o = new JSONObject();
            o.put("steps", new JSONObject().put("ok", stepsSupported(c)).put("perm", stepsPerm(c)).put("today", stepsToday(c)));
            o.put("hc", new JSONObject().put("ok", hcSupported(c)).put("perm", hcGranted(c)));
            o.put("screen", new JSONObject().put("perm", usageAccess(c)));
            o.put("loc", new JSONObject().put("perm", hasLoc(c)).put("bg", hasBgLoc(c)));
            JSONObject inside = new JSONObject(); for (String k : prefs(c).getAll().keySet()) if (k.startsWith("in:")) inside.put(k.substring(3), prefs(c).getLong(k, 0));
            o.put("inside", inside);
            return o.toString();
        } catch (Exception e) { return "{}"; }
    }

    /* ---------- the Activity page: a few days of steps, sleep, workouts, screen time and time at places ---------- */
    static String dayOf(long t) { return new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date(t)); }

    static void activityAsync(final MainActivity a, final int days) {
        new Thread(new Runnable() { public void run() { String r = activity(a, days); a.js("window.__act&&window.__act(" + JSONObject.quote(r) + ")"); } }).start();
    }

    static String activity(Context c, int days) {
        try {
            days = Math.max(1, Math.min(30, days));
            sampleSteps(c);
            SharedPreferences p = prefs(c);
            long now = System.currentTimeMillis();
            Calendar m = Calendar.getInstance(); m.set(Calendar.HOUR_OF_DAY, 0); m.set(Calendar.MINUTE, 0); m.set(Calendar.SECOND, 0); m.set(Calendar.MILLISECOND, 0);
            long today0 = m.getTimeInMillis(); m.add(Calendar.DAY_OF_YEAR, -(days - 1)); long from = m.getTimeInMillis();
            JSONObject o = new JSONObject();
            boolean hc = hcGranted(c), us = usageAccess(c);
            o.put("ok", new JSONObject().put("steps", stepsSupported(c) && stepsPerm(c)).put("hc", hc).put("hcSupported", hcSupported(c)).put("screen", us).put("loc", hasLoc(c)).put("bg", hasBgLoc(c)));
            JSONObject D = new JSONObject();   /* day -> {steps, sleep, ex, scr, pl{}} */
            for (long t = from; t <= today0; t += 86400000L) {
                String ds = dayOf(t + 3600000L);
                D.put(ds, new JSONObject().put("steps", p.getLong("stDay:" + ds, 0)));
            }
            JSONArray work = new JSONArray(); JSONObject hci = new JSONObject();
            if (hc) {
                try {
                    for (Object r : hcRead(c, "android.health.connect.datatypes.StepsRecord", from, now)) {
                        JSONObject d = D.optJSONObject(dayOf(inst(r, "getStartTime"))); if (d == null) continue;
                        d.put("hsteps", d.optLong("hsteps", 0) + (Long) r.getClass().getMethod("getCount").invoke(r)); hci.put("steps", hci.optInt("steps") + 1);
                    }
                } catch (Throwable e) { hci.put("errSteps", String.valueOf(e.getCause() != null ? e.getCause() : e)); }
                try {
                    for (Object r : hcRead(c, "android.health.connect.datatypes.SleepSessionRecord", from - 12 * 3600000L, now)) {
                        long s0 = inst(r, "getStartTime"), e0 = inst(r, "getEndTime"); JSONObject d = D.optJSONObject(dayOf(e0)); if (d == null) continue;
                        d.put("sleep", d.optLong("sleep", 0) + Math.max(0, e0 - s0) / 60000);
                        JSONArray ss = d.optJSONArray("ss"); if (ss == null) { ss = new JSONArray(); d.put("ss", ss); } ss.put(new JSONArray().put(s0).put(e0)); hci.put("sleep", hci.optInt("sleep") + 1);
                    }
                } catch (Throwable e) { hci.put("errSleep", String.valueOf(e.getCause() != null ? e.getCause() : e)); }
                try {
                    for (Object r : hcRead(c, "android.health.connect.datatypes.ExerciseSessionRecord", from, now)) {
                        long s0 = inst(r, "getStartTime"), e0 = inst(r, "getEndTime"); long mins = (e0 - s0) / 60000; if (mins <= 0) continue;
                        JSONObject d = D.optJSONObject(dayOf(s0)); if (d == null) continue;
                        d.put("ex", d.optLong("ex", 0) + mins);
                        int type = (Integer) r.getClass().getMethod("getExerciseType").invoke(r);
                        work.put(new JSONObject().put("t", s0).put("m", mins).put("k", type));
                        JSONArray wd = d.optJSONArray("w"); if (wd == null) { wd = new JSONArray(); d.put("w", wd); } wd.put(new JSONObject().put("t", s0).put("m", mins).put("k", type)); hci.put("ex", hci.optInt("ex") + 1);
                    }
                } catch (Throwable e) { hci.put("errEx", String.valueOf(e.getCause() != null ? e.getCause() : e)); }
            }
            JSONArray top = new JSONArray();
            if (us) {
                try {
                    PackageManager pm = c.getPackageManager();
                    Set<String> skip = new HashSet<>(); skip.add("com.android.systemui");
                    for (ResolveInfo r : pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)) skip.add(r.activityInfo.packageName);
                    UsageStatsManager usm = c.getSystemService(UsageStatsManager.class);
                    UsageEvents ev = usm.queryEvents(from, now);
                    Map<String, Long> open = new HashMap<>(), todayPk = new HashMap<>(); Map<String, Map<String, Long>> dayPk = new HashMap<>(); UsageEvents.Event e = new UsageEvents.Event();
                    while (ev.hasNextEvent()) {
                        ev.getNextEvent(e); String pk = e.getPackageName(); if (skip.contains(pk)) continue;
                        int t = e.getEventType();
                        if (t == 1) open.put(pk, e.getTimeStamp());
                        else if (t == 2 || t == 23) {
                            Long s0 = open.remove(pk); if (s0 == null) continue;
                            long len = e.getTimeStamp() - s0; if (len <= 0 || len > 6 * 3600000L) continue;
                            JSONObject d = D.optJSONObject(dayOf(e.getTimeStamp())); if (d == null) continue;
                            d.put("scr", d.optLong("scr", 0) + len / 60000);
                            if (e.getTimeStamp() >= today0) todayPk.put(pk, (todayPk.containsKey(pk) ? todayPk.get(pk) : 0L) + len);
                            String dk = dayOf(e.getTimeStamp()); Map<String, Long> mp = dayPk.get(dk); if (mp == null) { mp = new HashMap<>(); dayPk.put(dk, mp); } mp.put(pk, (mp.containsKey(pk) ? mp.get(pk) : 0L) + len);
                        }
                    }
                    List<Map.Entry<String, Long>> L = new java.util.ArrayList<>(todayPk.entrySet());
                    java.util.Collections.sort(L, new java.util.Comparator<Map.Entry<String, Long>>() { public int compare(Map.Entry<String, Long> x, Map.Entry<String, Long> y) { return Long.compare(y.getValue(), x.getValue()); } });
                    for (int i = 0; i < L.size() && i < 6; i++) {
                        long mins = L.get(i).getValue() / 60000; if (mins < 1) continue;
                        String nm = L.get(i).getKey(); try { nm = String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(nm, 0))); } catch (Exception ignored) { }
                        top.put(new JSONObject().put("pk", L.get(i).getKey()).put("name", nm).put("min", mins));
                    }
                    /* top apps for every day, so old days can be opened later */
                    Map<String, String> names = new HashMap<>();
                    for (Map.Entry<String, Map<String, Long>> de : dayPk.entrySet()) {
                        JSONObject d = D.optJSONObject(de.getKey()); if (d == null) continue;
                        List<Map.Entry<String, Long>> L2 = new java.util.ArrayList<>(de.getValue().entrySet());
                        java.util.Collections.sort(L2, new java.util.Comparator<Map.Entry<String, Long>>() { public int compare(Map.Entry<String, Long> x, Map.Entry<String, Long> y) { return Long.compare(y.getValue(), x.getValue()); } });
                        JSONArray ap = new JSONArray();
                        for (int i = 0; i < L2.size() && ap.length() < 5; i++) {
                            long mins = L2.get(i).getValue() / 60000; if (mins < 1) continue; String pk = L2.get(i).getKey();
                            String nm = names.get(pk); if (nm == null) { nm = pk; try { nm = String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(pk, 0))); } catch (Exception ignored) { } names.put(pk, nm); }
                            ap.put(new JSONArray().put(nm).put(mins));
                        }
                        d.put("ap", ap);
                    }
                } catch (Throwable ignored) { }
            }
            /* time at saved places (kept on the phone; 30 days) */
            JSONObject inside = new JSONObject();
            for (Map.Entry<String, ?> en : new HashMap<String, Object>(p.getAll()).entrySet()) {
                String k = en.getKey();
                if (k.startsWith("in:")) inside.put(k.substring(3), p.getLong(k, 0));
                else if (k.startsWith("pv:")) {
                    String[] q = k.split(":"); if (q.length < 3) continue;
                    JSONObject d = D.optJSONObject(q[1]);
                    if (d == null) { if (q[1].compareTo(dayOf(now - 30L * 86400000L)) < 0) p.edit().remove(k).apply(); continue; }
                    JSONObject pl = d.optJSONObject("pl"); if (pl == null) { pl = new JSONObject(); d.put("pl", pl); }
                    pl.put(q[2], pl.optLong(q[2], 0) + p.getLong(k, 0));
                }
            }
            JSONObject td = D.optJSONObject(dayOf(now));
            for (java.util.Iterator<String> it = inside.keys(); it.hasNext();) {
                String id = it.next(); long since = inside.optLong(id); if (since <= 0 || td == null) continue;
                JSONObject pl = td.optJSONObject("pl"); if (pl == null) { pl = new JSONObject(); td.put("pl", pl); }
                pl.put(id, pl.optLong(id, 0) + (now - Math.max(since, today0)) / 60000);
            }
            o.put("days", D).put("hc", hci).put("work", work).put("top", top).put("inside", inside);
            return o.toString();
        } catch (Throwable e) { return "{\"err\":\"" + e.getClass().getSimpleName() + "\"}"; }
    }

    /* screen-time minutes for apps right now (for the item sheet preview) */
    static long screenNow(Context c, String csv) { JSONArray a = new JSONArray(); for (String s : csv.split(",")) if (!s.isEmpty()) a.put(s); return screenMinutes(c, a); }
}
