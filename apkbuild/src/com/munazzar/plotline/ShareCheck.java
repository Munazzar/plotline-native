package com.munazzar.plotline;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/* About once an hour, even with Plotline closed:
   1. asks the sharing space (Firestore) whether anyone invited you, and posts "sara@gmail.com invited you".
      Shared items are end-to-end encrypted, so the notification only knows who, not what.
   2. reads reactions sent to you on items you've joined (it has those items' keys, so it can decrypt them) and posts
      "Ann: 🔥 Proud of you · on “Gym”". At most 50 a day from one person count, like in the app.
   3. for items you share: if someone you invited has since joined Plotline (published a public key), locks the
      item's key for them (E2E.wrap), so their invite unlocks without you opening the app. */
public class ShareCheck extends BroadcastReceiver {
    static final String PREF = "plotline_share", CH = "shared";

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREF, 0); }

    /* from the page: {on, key, pid, rt, email, seen:[circle ids the app already knows], priv, owned:[{cid,k}],
       joined:[{cid,k,title}], rx:{cid: newest reaction time the app already showed}} */
    static void config(Context c, String json) {
        try {
            JSONObject o = new JSONObject(json);
            SharedPreferences.Editor e = prefs(c).edit().putString("cfg", o.toString());
            JSONArray seen = o.optJSONArray("seen");
            if (seen != null) {
                JSONArray told = new JSONArray(prefs(c).getString("told", "[]"));
                for (int i = 0; i < seen.length(); i++) if (!has(told, seen.optString(i))) told.put(seen.optString(i));
                while (told.length() > 300) told.remove(0);
                e.putString("told", told.toString());
            }
            e.apply();
        } catch (Exception ignored) { }
        arm(c);
        LiveShare.start(c);
    }

    static boolean has(JSONArray a, String s) { for (int i = 0; i < a.length(); i++) if (s.equals(a.optString(i))) return true; return false; }

    static void arm(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        PendingIntent pi = PendingIntent.getBroadcast(c, 971, new Intent(c, ShareCheck.class).setAction("com.munazzar.plotline.SHARE_CHECK"),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        JSONObject cf = cfg(c);
        JSONArray j = cf.optJSONArray("joined");
        boolean live = j != null && j.length() > 0;  // reactions: check every 15 minutes; invites alone: hourly
        if (cf.optBoolean("on") && cf.optString("rt").length() > 0) am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (live ? 5 : 20) * 60000L, live ? AlarmManager.INTERVAL_FIFTEEN_MINUTES : AlarmManager.INTERVAL_HOUR, pi);
        else am.cancel(pi);
    }

    static JSONObject cfg(Context c) { try { return new JSONObject(prefs(c).getString("cfg", "{}")); } catch (Exception e) { return new JSONObject(); } }

    @Override
    public void onReceive(final Context c, Intent i) {
        final PendingResult pr = goAsync();
        new Thread(new Runnable() { public void run() {
            try { check(c.getApplicationContext()); } catch (Exception ignored) { } finally { pr.finish(); }
        } }).start();
    }

    static void check(Context c) throws Exception {
        JSONObject cf = cfg(c);
        String key = cf.optString("key"), pid = cf.optString("pid"), rt = cf.optString("rt"), me = cf.optString("email").toLowerCase();
        if (!cf.optBoolean("on") || key.isEmpty() || pid.isEmpty() || rt.isEmpty() || me.isEmpty()) return;
        JSONObject tok = new JSONObject(http("POST", "https://securetoken.googleapis.com/v1/token?key=" + URLEncoder.encode(key, "UTF-8"),
                "application/x-www-form-urlencoded", "grant_type=refresh_token&refresh_token=" + URLEncoder.encode(rt, "UTF-8"), null));
        String id = tok.optString("id_token");
        if (id.isEmpty()) return;
        JSONObject q = new JSONObject().put("structuredQuery", new JSONObject()
                .put("from", new JSONArray().put(new JSONObject().put("collectionId", "circles")))
                .put("where", new JSONObject().put("fieldFilter", new JSONObject()
                        .put("field", new JSONObject().put("fieldPath", "members"))
                        .put("op", "ARRAY_CONTAINS")
                        .put("value", new JSONObject().put("stringValue", me))))
                .put("limit", 100));
        String base = "https://firestore.googleapis.com/v1/projects/" + pid + "/databases/(default)/documents";
        long deadline = System.currentTimeMillis() + 8000;
        JSONArray res = new JSONArray(http("POST", base + ":runQuery", "application/json", q.toString(), id));
        JSONArray told = new JSONArray(prefs(c).getString("told", "[]"));
        boolean changed = false;
        for (int i = 0; i < res.length(); i++) {
            JSONObject d = res.getJSONObject(i).optJSONObject("document");
            if (d == null) continue;
            String name = d.optString("name"), cid = name.substring(name.lastIndexOf('/') + 1);
            JSONObject f = d.optJSONObject("fields");
            if (f == null || has(told, cid)) continue;
            String owner = str(f, "owner").toLowerCase();
            told.put(cid); changed = true;
            if (owner.equals(me)) continue;
            post(c, cid, owner + " invited you", "A shared goal or habit on Plotline · tap to see it and join");
        }
        /* hand the item key to people who joined Plotline after being invited */
        JSONArray owned = cf.optJSONArray("owned");
        String priv = cf.optString("priv");
        if (owned != null && !priv.isEmpty()) {
            for (int i = 0; i < res.length() && System.currentTimeMillis() < deadline; i++) {
                JSONObject d = res.getJSONObject(i).optJSONObject("document");
                if (d == null) continue;
                String name = d.optString("name"), cid = name.substring(name.lastIndexOf('/') + 1);
                JSONObject f = d.optJSONObject("fields");
                if (f == null || !str(f, "owner").toLowerCase().equals(me)) continue;
                String k = null;
                for (int j = 0; j < owned.length(); j++) if (cid.equals(owned.getJSONObject(j).optString("cid"))) k = owned.getJSONObject(j).optString("k");
                if (k == null || k.isEmpty()) continue;
                JSONObject keys;
                try { keys = new JSONObject(str(f, "locks")); } catch (Exception e) { keys = new JSONObject(); }
                JSONObject mv = f.optJSONObject("members");
                JSONArray ms = mv == null || mv.optJSONObject("arrayValue") == null ? new JSONArray() : mv.optJSONObject("arrayValue").optJSONArray("values");
                boolean add = false;
                for (int j = 0; ms != null && j < ms.length() && System.currentTimeMillis() < deadline; j++) {
                    String m = ms.getJSONObject(j).optString("stringValue").toLowerCase();
                    if (m.isEmpty() || m.equals(me) || keys.has(m)) continue;
                    String kd;
                    try { kd = http("GET", base + "/keys/" + URLEncoder.encode(m, "UTF-8"), null, null, id); } catch (Exception e) { continue; }
                    JSONObject kf = new JSONObject(kd).optJSONObject("fields");
                    if (kf == null) continue;
                    String pub = str(kf, "pub");
                    if (pub.isEmpty()) continue;
                    try { keys.put(m, new JSONObject().put("fp", E2E.fp(pub)).put("w", E2E.wrap(k, priv, pub))); add = true; } catch (Exception ignored) { }
                }
                if (add) {
                    JSONObject body = new JSONObject().put("fields", new JSONObject()
                            .put("locks", new JSONObject().put("stringValue", keys.toString()))
                            .put("u", new JSONObject().put("integerValue", String.valueOf(System.currentTimeMillis()))));
                    try { http("PATCH", base + "/circles/" + cid + "?updateMask.fieldPaths=locks&updateMask.fieldPaths=u", "application/json", body.toString(), id); } catch (Exception ignored) { }
                }
            }
        }
        try { reactions(c, cf, base, id, me, System.currentTimeMillis() + 6000); } catch (Exception ignored) { }
        if (changed) {
            while (told.length() > 300) told.remove(0);
            prefs(c).edit().putString("told", told.toString()).apply();
        }
    }

    static void reactions(Context c, JSONObject cf, String base, String id, String me, long deadline) throws Exception {
        JSONArray joined = cf.optJSONArray("joined");
        if (joined == null || joined.length() == 0) return;
        JSONObject rx;
        try { rx = new JSONObject(prefs(c).getString("rx", "{}")); } catch (Exception e) { rx = new JSONObject(); }
        JSONObject app = cf.optJSONObject("rx");
        HashMap<String, Integer> perDay = new HashMap<String, Integer>();
        SimpleDateFormat day = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        boolean ch = false;
        for (int j = 0; j < joined.length() && System.currentTimeMillis() < deadline; j++) {
            JSONObject jo = joined.optJSONObject(j);
            if (jo == null) continue;
            String cid = jo.optString("cid"), k = jo.optString("k"), title = jo.optString("title");
            if (!cid.matches("[A-Za-z0-9]{1,50}") || k.isEmpty()) continue;
            long seen = Math.max(rx.optLong(cid, 0), app == null ? 0 : app.optLong(cid, 0));
            if (seen == 0) { rx.put(cid, System.currentTimeMillis()); ch = true; continue; }  // first look: nothing old
            JSONArray docs;
            try { docs = new JSONObject(http("GET", base + "/circles/" + cid + "/ms?pageSize=100", null, null, id)).optJSONArray("documents"); } catch (Exception e) { continue; }
            if (docs == null) continue;
            byte[] key = E2E.b64d(k);
            long max = seen; int n = 0; String who = "", what = "";
            for (int d = 0; d < docs.length(); d++) {
                JSONObject doc = docs.optJSONObject(d);
                JSONObject f = doc == null ? null : doc.optJSONObject("fields");
                if (f == null) continue;
                String dn = doc.optString("name"), em = URLDecoder.decode(dn.substring(dn.lastIndexOf('/') + 1), "UTF-8").toLowerCase();
                String enc = str(f, "enc");
                if (em.equals(me) || enc.isEmpty()) continue;
                JSONObject p;
                try { p = new JSONObject(new String(E2E.unseal(key, enc), "UTF-8")); } catch (Exception e) { continue; }
                JSONArray cs = p.optJSONArray("cheers");
                if (cs == null) continue;
                String name = p.optString("name", em.split("@")[0]);
                for (int q = 0; q < cs.length(); q++) {
                    JSONObject x = cs.optJSONObject(q);
                    if (x == null || !me.equals(x.optString("to").toLowerCase())) continue;
                    long t = x.optLong("t");
                    if (t <= 0 || t > System.currentTimeMillis() + 600000L) continue;
                    String dk = em + "|" + day.format(new Date(t));
                    Integer was = perDay.get(dk);
                    int cnt = was == null ? 1 : was + 1;
                    perDay.put(dk, cnt);
                    if (cnt > 50 || t <= seen) continue;
                    n++;
                    if (t > max) {
                        max = t; who = name;
                        String e = x.optString("emoji"), m = x.optString("m");
                        what = (e + " " + (m.length() > 60 ? m.substring(0, 60) : m)).trim();
                    }
                }
            }
            if (n > 0) {
                rx.put(cid, max); ch = true;
                postReact(c, cid, who + (what.isEmpty() ? " reacted" : ": " + what),
                        (n > 1 ? n + " new reactions · " : "") + (title.isEmpty() ? "" : "on “" + title + "” · ") + "tap to react back");
            }
        }
        if (ch) prefs(c).edit().putString("rx", rx.toString()).apply();
    }

    static void postReact(Context c, String cid, String title, String body) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        NotificationChannel ch = new NotificationChannel("reactions", "Reactions", NotificationManager.IMPORTANCE_DEFAULT);
        ch.setDescription("Emoji and quick messages from people you share a goal or habit with");
        nm.createNotificationChannel(ch);
        int nid = 8000 + Math.abs(cid.hashCode() % 900);
        Notification.Builder b = new Notification.Builder(c, "reactions")
                .setSmallIcon(W.id(c, "ic_stat", "drawable"))
                .setContentTitle(title)
                .setContentText(body)
                .setColor(0xFFFFB547)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_SOCIAL)
                .setContentIntent(W.open(c, "join/" + cid, nid));
        nm.notify(nid, b.build());
    }

    static String str(JSONObject f, String k) { JSONObject v = f.optJSONObject(k); return v == null ? "" : v.optString("stringValue"); }

    static void post(Context c, String cid, String title, String body) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        NotificationChannel ch = new NotificationChannel(CH, "Shared with you", NotificationManager.IMPORTANCE_DEFAULT);
        ch.setDescription("Invites from people who share a goal or habit with you");
        nm.createNotificationChannel(ch);
        String route = cid.matches("[A-Za-z0-9]{1,50}") ? "join/" + cid : "settings/share";
        Notification.Builder b = new Notification.Builder(c, CH)
                .setSmallIcon(W.id(c, "ic_stat", "drawable"))
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setColor(0xFFFFB547)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_SOCIAL)
                .setContentIntent(W.open(c, route, 7000 + Math.abs(cid.hashCode() % 900)));
        nm.notify(7000 + Math.abs(cid.hashCode() % 900), b.build());
    }

    /* HttpURLConnection has no PATCH: Google APIs accept POST + X-HTTP-Method-Override */
    static String http(String method, String url, String type, String body, String bearer) throws Exception {
        HttpURLConnection h = (HttpURLConnection) new URL(url).openConnection();
        try {
            h.setConnectTimeout(5000); h.setReadTimeout(5000);
            if ("GET".equals(method)) h.setRequestMethod("GET");
            else { h.setRequestMethod("POST"); h.setDoOutput(true); if (!"POST".equals(method)) h.setRequestProperty("X-HTTP-Method-Override", method); }
            if (type != null) h.setRequestProperty("Content-Type", type);
            if (bearer != null) h.setRequestProperty("Authorization", "Bearer " + bearer);
            if (body != null) { OutputStream os = h.getOutputStream(); os.write(body.getBytes("UTF-8")); os.close(); }
            int rc = h.getResponseCode();
            InputStream in = rc < 400 ? h.getInputStream() : h.getErrorStream();
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192]; int n;
            if (in != null) { while ((n = in.read(buf)) > 0) bo.write(buf, 0, n); in.close(); }
            if (rc >= 400) throw new Exception("http " + rc);
            return bo.toString("UTF-8");
        } finally { h.disconnect(); }
    }
}
