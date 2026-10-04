package com.munazzar.plotline;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import java.net.URLEncoder;
import org.json.JSONArray;
import org.json.JSONObject;

/* "Instant reactions" (opt-in, Settings → Sharing). Plotline has no server to push from, so while you are in at least
   one shared item this foreground service asks the sharing space every 30 seconds and posts new reactions right away
   (same decryption, daily cap and de-duplication as ShareCheck.reactions). It shows a quiet, silent notification while it
   runs, as Android requires. Turn it off and it stops; it also stops by itself when you leave your last shared item. */
public class LiveShare extends Service {
    static final String CH = "live";
    static volatile boolean run = false;
    static String tok = null; static long tokAt = 0;

    static boolean on(Context c) { return ShareCheck.prefs(c).getBoolean("live", false); }
    static boolean wanted(Context c) { JSONObject cf = ShareCheck.cfg(c); JSONArray j = cf.optJSONArray("joined"); return on(c) && cf.optBoolean("on") && cf.optString("rt").length() > 0 && j != null && j.length() > 0; }
    static void set(Context c, boolean v) { ShareCheck.prefs(c).edit().putBoolean("live", v).apply(); if (v) start(c); else { run = false; try { c.stopService(new Intent(c, LiveShare.class)); } catch (Exception ignored) { } } }
    static void start(Context c) { if (!wanted(c) || run) return; try { c.startForegroundService(new Intent(c, LiveShare.class)); } catch (Exception ignored) { } }

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CH, "Instant reactions", NotificationManager.IMPORTANCE_MIN));
        Notification n = new Notification.Builder(this, CH).setSmallIcon(W.id(this, "ic_stat", "drawable"))
                .setContentTitle("Instant reactions are on").setContentText("Checking your shared goals and habits every 30 seconds")
                .setContentIntent(W.open(this, "settings/share", 2903)).setOngoing(true).build();
        try { startForeground(2902, n, 1 /* FOREGROUND_SERVICE_TYPE_DATA_SYNC */); } catch (Exception e) { try { startForeground(2902, n); } catch (Exception ignored) { } }
        if (run) return START_STICKY;
        run = true;
        final Context c = getApplicationContext();
        new Thread(new Runnable() { public void run() {
            while (run && wanted(c)) {
                try { tick(c); } catch (Exception ignored) { }
                try { Thread.sleep(30000); } catch (InterruptedException e) { break; }
            }
            run = false;
            stopSelf();
        } }).start();
        return START_STICKY;
    }

    @Override public void onDestroy() { run = false; super.onDestroy(); }

    static void tick(Context c) throws Exception {
        JSONObject cf = ShareCheck.cfg(c);
        String key = cf.optString("key"), pid = cf.optString("pid"), rt = cf.optString("rt"), me = cf.optString("email").toLowerCase();
        if (key.isEmpty() || pid.isEmpty() || rt.isEmpty() || me.isEmpty()) return;
        if (tok == null || System.currentTimeMillis() - tokAt > 45 * 60000L) {
            JSONObject t = new JSONObject(ShareCheck.http("POST", "https://securetoken.googleapis.com/v1/token?key=" + URLEncoder.encode(key, "UTF-8"),
                    "application/x-www-form-urlencoded", "grant_type=refresh_token&refresh_token=" + URLEncoder.encode(rt, "UTF-8"), null));
            tok = t.optString("id_token"); tokAt = System.currentTimeMillis();
            if (tok.isEmpty()) { tok = null; return; }
        }
        String base = "https://firestore.googleapis.com/v1/projects/" + pid + "/databases/(default)/documents";
        ShareCheck.reactions(c, cf, base, tok, me, System.currentTimeMillis() + 10000);
    }
}
