package com.munazzar.plotline;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/*
 * Interactive reminders. Every reminder carries what it is about (k = habit | step | day | wrap | checkin | …)
 * and gets up to three actions: Done, Reply and Mute. A typed reply is read here on the phone:
 * "done", "finished it", "busy at work for 3 hours", "snooze 20 min", "skip today", "focus till tomorrow" and so on.
 * Check-offs go through the same queue as widget taps, so the app applies them next time it opens.
 * Anything this reader can't place is queued as a reply for the app, which asks the on-device AI what it means.
 */
public class Notify extends BroadcastReceiver {
    static final String DONE = "com.munazzar.plotline.N_DONE", MUTE = "com.munazzar.plotline.N_MUTE", REPLY = "com.munazzar.plotline.N_REPLY",
            SNOOZED = "com.munazzar.plotline.N_SNOOZED", UNDO = "com.munazzar.plotline.N_UNDO", SUMMARY = "com.munazzar.plotline.N_SUMMARY", KEY = "reply";
    static final String PREF = "plotline_notif";
    static final int FLAG_MUTABLE = 0x02000000;

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREF, 0); }
    static long muteUntil(Context c) { return prefs(c).getLong("muteUntil", 0); }

    static String state(Context c) {
        try {
            JSONObject o = new JSONObject();
            long m = muteUntil(c);
            o.put("muteUntil", m > System.currentTimeMillis() ? m : 0);
            o.put("missed", new JSONArray(prefs(c).getString("missed", "[]")).length());
            JSONObject z = snz(c), out = new JSONObject(); long now = System.currentTimeMillis();
            java.util.Iterator<String> it = z.keys();
            while (it.hasNext()) { String k = it.next(); JSONObject x = z.optJSONObject(k); if (x != null && x.optLong("at") > now) out.put(k, x); }
            o.put("snoozes", out);
            return o.toString();
        } catch (Exception e) { return "{}"; }
    }

    /* ---------- history per item: what was sent, what you replied, what was done ---------- */
    static String key(JSONObject o) {
        String k = o.optString("k");
        if ("habit".equals(k)) return "h:" + o.optString("hid");
        if ("step".equals(k)) return "s:" + o.optString("g") + "/" + o.optString("s");
        if ("day".equals(k)) return "d:" + o.optString("id");
        if ("checkin".equals(k)) return "g:" + o.optString("g");
        if ("wrap".equals(k)) return "w";
        return k;
    }

    static synchronized void log(Context c, JSONObject o, String kind, String text, String res) {
        try {
            JSONArray L = new JSONArray(prefs(c).getString("log", "[]"));
            JSONObject e = new JSONObject();
            e.put("t", System.currentTimeMillis()); e.put("key", key(o)); e.put("kind", kind); e.put("title", o.optString("title"));
            if (text != null && !text.isEmpty()) e.put("text", text);
            if (res != null && !res.isEmpty()) e.put("res", res);
            JSONArray hs = o.optJSONArray("hs");
            if (hs != null) { JSONArray ks = new JSONArray(); for (int j = 0; j < hs.length(); j++) ks.put("h:" + hs.optJSONObject(j).optString("id")); e.put("also", ks); }
            JSONArray out = new JSONArray(); out.put(e);
            for (int j = 0; j < L.length() && out.length() < 400; j++) out.put(L.get(j));
            prefs(c).edit().putString("log", out.toString()).apply();
        } catch (Exception ignored) { }
    }

    /* newest first; prefix "g:<id>" also returns that goal's steps */
    static String history(Context c, String k) {
        try {
            JSONArray L = new JSONArray(prefs(c).getString("log", "[]")), out = new JSONArray();
            String stepPre = k.startsWith("g:") ? "s:" + k.substring(2) + "/" : null;
            for (int j = 0; j < L.length(); j++) {
                JSONObject e = L.getJSONObject(j); String ek = e.optString("key"); boolean hit = "*".equals(k) || ek.equals(k) || stepPre != null && ek.startsWith(stepPre);
                JSONArray also = e.optJSONArray("also"); if (!hit && also != null) for (int i = 0; i < also.length(); i++) if (k.equals(also.optString(i))) hit = true;
                if (hit) out.put(e);
            }
            return out.toString();
        } catch (Exception e) { return "[]"; }
    }

    static JSONObject snz(Context c) { try { return new JSONObject(prefs(c).getString("snz", "{}")); } catch (Exception e) { return new JSONObject(); } }
    static synchronized void snzPut(Context c, int nid, JSONObject x) { JSONObject z = snz(c); try { if (x == null) z.remove(String.valueOf(nid)); else z.put(String.valueOf(nid), x); } catch (Exception ignored) { } prefs(c).edit().putString("snz", z.toString()).apply(); }

    /* ---------- building the notification ---------- */
    static boolean doable(String k) { return "habit".equals(k) || "step".equals(k) || "day".equals(k) || "wrap".equals(k); }

    static String route(JSONObject o) {
        String k = o.optString("k");
        if ("habit".equals(k)) return "habit/" + o.optString("hid");
        if ("step".equals(k) || "checkin".equals(k)) return "goal/" + o.optString("g");
        if ("day".equals(k) && o.optString("d").matches("\\d{4}-\\d{2}-\\d{2}")) return "cal-" + o.optString("d");
        if ("wrap".equals(k)) return "habits";
        return "today";
    }

    static Intent act(Context c, String action, JSONObject o, int nid) {
        Intent i = new Intent(c, Notify.class);
        i.setAction(action);
        i.setData(android.net.Uri.parse("plotline://n/" + action + "/" + nid));
        i.putExtra("o", o.toString()); i.putExtra("nid", nid);
        return i;
    }

    static void addActions(Context c, Notification.Builder b, JSONObject o, int nid) {
        if (!o.optBoolean("rp", true)) return;
        String k = o.optString("k");
        int fl = PendingIntent.FLAG_UPDATE_CURRENT;
        if (doable(k)) b.addAction(new Notification.Action.Builder(null, "wrap".equals(k) ? "All done" : "Done",
                PendingIntent.getBroadcast(c, 7000 + nid, act(c, DONE, o, nid), fl | PendingIntent.FLAG_IMMUTABLE)).build());
        RemoteInput ri = new RemoteInput.Builder(KEY).setLabel("wrap".equals(k) ? "What did you get done?" : "done · busy 2h · snooze 30m · skip").build();
        b.addAction(new Notification.Action.Builder(null, "Reply",
                PendingIntent.getBroadcast(c, 8000 + nid, act(c, REPLY, o, nid), fl | FLAG_MUTABLE)).addRemoteInput(ri).setAllowGeneratedReplies(true).build());
        int mm = Math.max(15, o.optInt("mute", 120));
        b.addAction(new Notification.Action.Builder(null, "Mute " + dur(mm),
                PendingIntent.getBroadcast(c, 9000 + nid, act(c, MUTE, o, nid), fl | PendingIntent.FLAG_IMMUTABLE)).build());
    }

    /* while muted, reminders are kept and summed up in one quiet notification when the mute ends */
    static boolean holdIfMuted(Context c, JSONObject o) {
        if (o.optBoolean("force") || System.currentTimeMillis() >= muteUntil(c)) return false;
        try {
            JSONArray m = new JSONArray(prefs(c).getString("missed", "[]"));
            if (m.length() < 30) m.put(o.optString("title"));
            prefs(c).edit().putString("missed", m.toString()).apply();
        } catch (Exception ignored) { }
        return true;
    }

    /* ---------- receiving actions ---------- */
    @Override
    public void onReceive(Context c, Intent i) {
        String a = i.getAction();
        int nid = i.getIntExtra("nid", 0);
        JSONObject o;
        try { o = new JSONObject(i.getStringExtra("o") == null ? "{}" : i.getStringExtra("o")); } catch (Exception e) { o = new JSONObject(); }
        if (SNOOZED.equals(a)) { snzPut(c, nid, null); log(c, o, "again", "", ""); Reminders.show(c, o, nid >= 1000 ? nid - 1000 : nid, false); return; }
        if (SUMMARY.equals(a)) { summary(c); return; }
        String msg;
        if (DONE.equals(a)) { long rid = System.currentTimeMillis(); RID = rid; done(c, o); RID = 0; try { prefs(c).edit().putString("undo" + nid, new JSONObject().put("rid", rid).toString()).apply(); } catch (Exception ignored) { } msg = "✓ Marked done"; log(c, o, "done", "", msg); W.updateAll(c); }
        else if (MUTE.equals(a)) { long prev = muteUntil(c); msg = mute(c, o.optInt("mute", 120)); try { prefs(c).edit().putString("undo" + nid, new JSONObject().put("muted", true).put("mutePrev", prev).toString()).apply(); } catch (Exception ignored) { } log(c, o, "mute", "", msg); }
        else if (REPLY.equals(a)) {
            Bundle r = RemoteInput.getResultsFromIntent(i);
            CharSequence t = r == null ? null : r.getCharSequence(KEY);
            String text = t == null ? "" : t.toString().trim();
            if (text.isEmpty()) msg = "Nothing typed";
            else if (ReplyService.ready(c)) {
                thinking(c, nid, o, text);
                try {
                    Intent s = new Intent(c, ReplyService.class);
                    s.putExtra("o", o.toString()); s.putExtra("nid", nid); s.putExtra("text", text);
                    c.startForegroundService(s);
                    return;
                } catch (Exception e) { msg = execute(c, o, nid, rules(o.optString("k"), text, o.optInt("mute", 120)), text); log(c, o, "reply", text, msg); }
            } else { msg = execute(c, o, nid, rules(o.optString("k"), text, o.optInt("mute", 120)), text); log(c, o, "reply", text, msg); }
            W.updateAll(c);
        } else if (UNDO.equals(a)) { msg = undo(c, nid); log(c, o, "undo", "", msg); W.updateAll(c); }
        else return;
        confirm(c, nid, o, msg, null, !UNDO.equals(a) && prefs(c).contains("undo" + nid));
    }

    /* the reply has to replace the notification, or Android keeps showing a spinner */
    static void confirm(Context c, int nid, JSONObject o, String msg) { confirm(c, nid, o, msg, null, false); }

    static void confirm(Context c, int nid, JSONObject o, String msg, String sub, boolean canUndo) {
        try {
            Reminders.createChannel(c);
            String body = sub == null || sub.isEmpty() ? msg : msg + "\n" + sub;
            Notification.Builder b = new Notification.Builder(c, Reminders.CH)
                    .setSmallIcon(W.id(c, "ic_stat", "drawable")).setColor(0xFFFFB547)
                    .setContentTitle(o.optString("title", "Plotline")).setContentText(msg)
                    .setStyle(new Notification.BigTextStyle().bigText(body))
                    .setOnlyAlertOnce(true).setAutoCancel(true).setTimeoutAfter(canUndo ? 30000 : 8000)
                    .setContentIntent(W.open(c, route(o), 6000 + nid));
            if (canUndo) b.addAction(new Notification.Action.Builder(null, "Undo",
                    PendingIntent.getBroadcast(c, 9500 + nid, act(c, UNDO, o, nid), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE)).build());
            c.getSystemService(NotificationManager.class).notify(nid, b.build());
        } catch (Exception ignored) { }
    }

    /* shown while the on-device model reads the reply */
    static void thinking(Context c, int nid, JSONObject o, String text) {
        try {
            Reminders.createChannel(c);
            Notification.Builder b = new Notification.Builder(c, Reminders.CH)
                    .setSmallIcon(W.id(c, "ic_stat", "drawable")).setColor(0xFFFFB547)
                    .setContentTitle(o.optString("title", "Plotline")).setContentText("Reading “" + text + "”…")
                    .setProgress(0, 0, true).setOnlyAlertOnce(true);
            c.getSystemService(NotificationManager.class).notify(nid, b.build());
        } catch (Exception ignored) { }
    }

    static void done(Context c, JSONObject o) {
        try {
            String k = o.optString("k");
            if ("habit".equals(k)) { JSONObject a = q("habit"); a.put("id", o.optString("hid")); a.put("d", o.optString("hd")); a.put("v", Math.max(1, o.optInt("hv", 1))); W.enqueue(c, a); Reminders.dropFor(c, "habit", o.optString("hid"), o.optString("hd")); }
            else if ("step".equals(k)) { JSONObject a = q("step"); a.put("g", o.optString("g")); a.put("s", o.optString("s")); W.enqueue(c, a); Reminders.dropFor(c, "step", o.optString("g"), o.optString("s")); }
            else if ("day".equals(k)) { JSONObject a = q("day"); a.put("id", o.optString("id")); a.put("done", true); W.enqueue(c, a); Reminders.dropFor(c, "day", o.optString("id"), ""); }
            else if ("wrap".equals(k)) {
                JSONArray hs = o.optJSONArray("hs");
                if (hs != null) for (int j = 0; j < hs.length(); j++) {
                    JSONObject h = hs.optJSONObject(j); if (h == null) continue;
                    JSONObject a = q("habit"); a.put("id", h.optString("id")); a.put("d", o.optString("hd")); a.put("v", Math.max(1, h.optInt("v", 1))); W.enqueue(c, a); Reminders.dropFor(c, "habit", h.optString("id"), o.optString("hd"));
                }
            }
        } catch (Exception ignored) { }
    }

    static long RID = 0;
    static JSONObject q(String k) throws Exception { JSONObject a = new JSONObject(); a.put("at", System.currentTimeMillis()); a.put("k", k); if (RID != 0) a.put("rid", RID); return a; }

    static String mute(Context c, int minutes) {
        long until = System.currentTimeMillis() + minutes * 60000L;
        return muteTo(c, until);
    }

    static String muteTo(Context c, long until) {
        prefs(c).edit().putLong("muteUntil", until).apply();
        AlarmManager am = c.getSystemService(AlarmManager.class);
        Intent s = new Intent(c, Notify.class); s.setAction(SUMMARY);
        PendingIntent pi = PendingIntent.getBroadcast(c, 299, s, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (until <= System.currentTimeMillis()) { am.cancel(pi); summary(c); return "Reminders are back on"; }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, until, pi);
        return "🔕 All reminders muted until " + when(until);
    }

    /* "5:51 PM", "tomorrow 8:00 AM", "Sat, Oct 3 · 12:51 PM" */
    static String when(long t) {
        String tm = DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(t));
        if (sameDay(t)) return tm;
        Calendar k = Calendar.getInstance(); k.add(Calendar.DAY_OF_YEAR, 1);
        Calendar b = Calendar.getInstance(); b.setTimeInMillis(t);
        if (k.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR) && k.get(Calendar.YEAR) == b.get(Calendar.YEAR)) return "tomorrow " + tm;
        return new java.text.SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(new Date(t)) + " · " + tm;
    }

    static boolean sameDay(long t) {
        Calendar a = Calendar.getInstance(), b = Calendar.getInstance(); b.setTimeInMillis(t);
        return a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR) && a.get(Calendar.YEAR) == b.get(Calendar.YEAR);
    }

    static void summary(Context c) {
        try {
            JSONArray m = new JSONArray(prefs(c).getString("missed", "[]"));
            prefs(c).edit().putString("missed", "[]").putLong("muteUntil", 0).apply();
            if (m.length() == 0) return;
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < m.length() && j < 6; j++) { if (j > 0) sb.append(" · "); sb.append(m.optString(j)); }
            if (m.length() > 6) sb.append(" · +").append(m.length() - 6);
            JSONObject o = new JSONObject(); o.put("title", "While you were focused: " + m.length() + " reminder" + (m.length() == 1 ? "" : "s")); o.put("body", sb.toString()); o.put("k", "summary"); o.put("rp", false);
            Reminders.show(c, o, 298);
        } catch (Exception ignored) { }
    }

    static void snooze(Context c, JSONObject o, int nid, int minutes) { snoozeAt(c, o, nid, System.currentTimeMillis() + minutes * 60000L); }

    /* an item you asked to hear about again comes back even while everything else is muted */
    static void snoozeAt(Context c, JSONObject o, int nid, long at) {
        try {
            JSONObject x = new JSONObject(o.toString()); x.put("force", true);
            AlarmManager am = c.getSystemService(AlarmManager.class);
            PendingIntent pi = PendingIntent.getBroadcast(c, 300 + (nid % 200), act(c, SNOOZED, x, nid), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            JSONObject r = new JSONObject(); r.put("at", at); r.put("key", key(o)); r.put("title", o.optString("title")); r.put("o", o); snzPut(c, nid, r);
            boolean exact = Reminders.canExact(am);
            try { if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi); else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi); }
            catch (SecurityException se) { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi); }
        } catch (Exception ignored) { }
    }

    /* after a reboot or update, snoozes you asked for are set again */
    static void rearmSnoozes(Context c) {
        JSONObject z = snz(c); long now = System.currentTimeMillis();
        java.util.Iterator<String> it = z.keys(); java.util.List<String> ks = new java.util.ArrayList<>();
        while (it.hasNext()) ks.add(it.next());
        for (String k : ks) {
            JSONObject x = z.optJSONObject(k);
            if (x == null || x.optLong("at") <= now || x.optJSONObject("o") == null) { snzPut(c, Integer.parseInt(k), null); continue; }
            snoozeAt(c, x.optJSONObject("o"), Integer.parseInt(k), x.optLong("at"));
        }
        long mu = muteUntil(c); if (mu > now) muteTo(c, mu);
    }

    static void cancelSnooze(Context c, int nid) {
        PendingIntent p = PendingIntent.getBroadcast(c, 300 + (nid % 200), new Intent(c, Notify.class).setAction(SNOOZED).setData(android.net.Uri.parse("plotline://n/" + SNOOZED + "/" + nid)), PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (p != null) { c.getSystemService(AlarmManager.class).cancel(p); p.cancel(); }
        snzPut(c, nid, null);
    }

    /* ---------- reading a typed reply ---------- */
    static final Pattern NEG = Pattern.compile("\\b(not|didn'?t|did not|haven'?t|have not|hasn'?t|no|nope|can'?t|cannot|won'?t|couldn'?t|wasn'?t|yet to|forgot)\\b");
    static final Pattern DONE_RX = Pattern.compile("(\\b(done|did( it| that)?|finished|finish|completed?|complete|yes|yep|yeah|yup|ya|already|checked|ticked|nailed( it)?|got it done|all good|sorted|handled|accomplished|ok(ay)? done|was done|went|ran|read it|drank|prayed|walked|worked out|logged)\\b)|[✓✔✅👍💪]");
    static final Pattern ALL_RX = Pattern.compile("\\b(all|everything|every one|each|both)\\b");
    static final Pattern MUTE_RX = Pattern.compile("\\b(mute|silence|silent|quiet|shush|hush|focus(ed|ing)?|busy|working|at work|in (a )?meeting|meeting|on a call|driving|commuting|do not disturb|don'?t disturb|dnd|leave me|stop|pause( reminders| notifications)?|deep work|heads ?down|in class|studying|sleeping|asleep|off today|no more|no (more )?(reminders|notifications|alerts)|stop (all )?(the )?(reminders|notifications))\\b");
    static final Pattern SNOOZE_RX = Pattern.compile("\\b(snooze|later|remind me|again in|in a bit|in a while|after( this)?|not yet|soon|give me|tonight|this evening|after work|after lunch|after dinner)\\b");
    static final Pattern SKIP_RX = Pattern.compile("\\b(skip|skipping|rest day|resting|\\brest\\b(?! of)|not today|day off|off day|sick|unwell|ill|too tired|pass|can'?t today|travell?ing|holiday|vacation)\\b");
    static final Pattern DUR = Pattern.compile("\\b(?:(\\d+(?:[.,]\\d+)?)\\s*|(an?|one|two|three|four|five|six|seven|eight|nine|ten|twelve|fifteen|twenty|thirty|forty[- ]?five|half an?|a couple(?: of)?|couple(?: of)?|few)\\s+)(h|hr|hrs|hour|hours|m|min|mins|minute|minutes|d|day|days|w|wk|wks|week|weeks)\\b");
    static final Pattern BARE = Pattern.compile("\\b(?:for|next|in)\\s+(?:the\\s+)?(?:next\\s+)?(\\d{1,3})\\b(?!\\s*(?:am|pm|:|\\.|h|m|d|w))");
    static final Pattern CLOCK = Pattern.compile("\\b(?:until|till|til|to|at|by|around|after)\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm|a\\.m\\.|p\\.m\\.)?(?![\\d])");
    static final Pattern CLOCK2 = Pattern.compile("\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)\\b");
    static final String[] WD = {"sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday"};

    static double num(String w) {
        w = w.replace(',', '.');
        switch (w) { case "a": case "an": case "one": return 1; case "two": case "a couple": case "a couple of": case "couple": case "couple of": return 2; case "three": case "few": return 3;
            case "four": return 4; case "five": return 5; case "six": return 6; case "seven": return 7; case "eight": return 8; case "nine": return 9; case "ten": return 10; case "twelve": return 12;
            case "fifteen": return 15; case "twenty": return 20; case "thirty": return 30; }
        if (w.startsWith("forty")) return 45;
        if (w.startsWith("half")) return 0.5;
        try { return Double.parseDouble(w); } catch (Exception e) { return 0; }
    }

    /* the moment a reply points to, as epoch ms, or 0: "for 2 days", "in 5 hrs", "half an hour", "until 4:30pm",
       "tomorrow morning", "friday at 9", "tonight", "rest of the day" */
    static long at(String t) { return at(t, System.currentTimeMillis()); }

    static long at(String t, long nowMs) {
        Calendar n = Calendar.getInstance(); n.setTimeInMillis(nowMs);
        Matcher d = DUR.matcher(t);
        if (d.find()) {
            double x = num((d.group(1) != null ? d.group(1) : d.group(2)).trim());
            char u = d.group(3).charAt(0);
            double mins = u == 'h' ? x * 60 : u == 'd' ? x * 1440 : u == 'w' ? x * 10080 : x;
            return nowMs + Math.max(1, Math.round(mins)) * 60000L;
        }
        int day = -1;  /* days from today */
        if (t.matches(".*\\b(rest of (the )?day|for (the )?day|for today|all day)\\b.*")) { Calendar e = (Calendar) n.clone(); e.add(Calendar.DAY_OF_YEAR, 1); e.set(Calendar.HOUR_OF_DAY, 7); e.set(Calendar.MINUTE, 0); e.set(Calendar.SECOND, 0); return e.getTimeInMillis(); }
        if (t.matches(".*\\b(tomorrow|tmrw|tmr|tomorow)\\b.*")) day = 1;
        else if (t.matches(".*\\b(today|tonight|this (morning|afternoon|evening))\\b.*")) day = 0;
        else for (int w = 0; w < 7; w++) if (t.matches(".*\\b" + WD[w] + "\\b.*") || t.matches(".*\\b" + WD[w].substring(0, 3) + "\\b.*")) { int cur = n.get(Calendar.DAY_OF_WEEK) - 1; day = (w - cur + 7) % 7; if (day == 0) day = 7; break; }
        int hh = -1, mm = 0;
        Matcher c = CLOCK.matcher(t); Matcher c2 = CLOCK2.matcher(t);
        Matcher m = c.find() ? c : c2.find() ? c2 : null;
        if (m != null) {
            hh = Integer.parseInt(m.group(1)); mm = m.group(2) == null ? 0 : Integer.parseInt(m.group(2));
            String ap = m.group(3) == null ? "" : m.group(3).replace(".", "");
            if (hh > 23 || mm > 59) hh = -1;
            else if (ap.equals("pm") && hh < 12) hh += 12; else if (ap.equals("am") && hh == 12) hh = 0;
            else if (ap.isEmpty() && hh < 12) {   /* "till 5" means 5 pm in the afternoon, "at 9" tomorrow means 9 am */
                if (t.matches(".*\\b(evening|tonight|night|afternoon|pm)\\b.*")) hh += 12;
                else if (day <= 0 && hh < 8) hh += 12;
                else if (day <= 0 && hh + 12 > n.get(Calendar.HOUR_OF_DAY) && hh <= n.get(Calendar.HOUR_OF_DAY)) hh += 12;
            }
        }
        if (hh < 0) {
            if (t.matches(".*\\bmorning\\b.*")) hh = 8; else if (t.matches(".*\\b(noon|lunch)\\b.*")) hh = 12; else if (t.matches(".*\\bafternoon\\b.*")) hh = 14;
            else if (t.matches(".*\\b(after work)\\b.*")) hh = 17; else if (t.matches(".*\\b(evening|after dinner)\\b.*")) hh = 19; else if (t.matches(".*\\b(tonight|night)\\b.*")) hh = 20;
        }
        if (hh < 0 && day < 0) {
            Matcher b = BARE.matcher(t);
            if (b.find()) { int v = Integer.parseInt(b.group(1)); return nowMs + (v <= 12 ? v * 60L : v) * 60000L; }
            return 0;
        }
        Calendar e = (Calendar) n.clone(); e.set(Calendar.SECOND, 0); e.set(Calendar.MILLISECOND, 0);
        if (day > 0) e.add(Calendar.DAY_OF_YEAR, day);
        e.set(Calendar.HOUR_OF_DAY, hh < 0 ? 8 : hh); e.set(Calendar.MINUTE, hh < 0 ? 0 : mm);
        if (e.getTimeInMillis() <= nowMs) { if (day <= 0) e.add(Calendar.DAY_OF_YEAR, 1); }
        return e.getTimeInMillis();
    }

    static String dur(int m) {
        if (m < 60) return m + " min";
        if (m % 60 == 0) return (m / 60) + (m == 60 ? " hour" : " hours");
        return String.format(Locale.US, "%.1f hours", m / 60.0);
    }

    /* ---------- what a reply asks for ---------- */
    static final class Act {
        boolean done, skip, unmute, ask, off; long remindAt, muteUntil; String moveTo = "", note = "", by = "quick";
        java.util.List<String> habits = new java.util.ArrayList<>();
        boolean any() { return done || skip || unmute || off || remindAt > 0 || muteUntil > 0 || moveTo.length() > 0 || habits.size() > 0 || note.length() > 0; }
        public String toString() { return (off ? "off " : "") + (done ? "done " : "") + (skip ? "skip " : "") + (unmute ? "unmute " : "") + (remindAt > 0 ? "remind@" + remindAt + " " : "") + (muteUntil > 0 ? "mute@" + muteUntil + " " : "") + (moveTo.isEmpty() ? "" : "move:" + moveTo + " ") + (habits.isEmpty() ? "" : "habits:" + habits + " ") + (note.isEmpty() ? "" : "note ") + (ask ? "ask" : ""); }
    }

    static String low(String text) { return text.toLowerCase(Locale.ROOT).replace('’', '\'').trim(); }

    /* the quick reader on the phone, used when no on-device model is downloaded (or it fails) */
    static Act rules(String k, String text, int muteDef) { return rules(k, text, muteDef, System.currentTimeMillis()); }

    static Act rules(String k, String text, int muteDef, long now) {
        String t = low(text); Act a = new Act();
        if (t.isEmpty()) return a;
        boolean neg = NEG.matcher(t).find(), dn = DONE_RX.matcher(t).find() && !neg, mu = MUTE_RX.matcher(t).find(),
                sn = SNOOZE_RX.matcher(t).find(), sk = SKIP_RX.matcher(t).find(), all = ALL_RX.matcher(t).find(), un = t.matches(".*\\b(unmute|un-mute|turn (them |reminders |notifications )?(back )?on)\\b.*");
        long when = at(t, now);
        if ("wrap".equals(k) && dn && !all) { a.ask = true; return a; }
        if (dn && doable(k)) a.done = true;
        else if (sk && "habit".equals(k)) a.skip = true;
        if (t.matches(".*\\b(stop reminding me|don'?t remind me|do not remind me|no more reminders) (about|for|of) (this|it|that)\\b.*") || t.matches(".*\\bturn (this|it) off\\b.*") || t.matches(".*\\b(turn off|disable) (this|the) reminder\\b.*")) { a.off = true; return a; }
        if (un) a.unmute = true;
        else if (mu && !(sn && t.matches(".*\\bremind\\b.*") && !t.matches(".*\\b(mute|silence)\\b.*"))) a.muteUntil = when > now ? when : now + Math.max(15, muteDef) * 60000L;
        else if (sn || when > now && !a.skip && !a.done || neg && doable(k) && !sk && !a.done) a.remindAt = when > now ? when : now + 3600000L;
        if (t.matches(".*\\b(move|reschedule|push|postpone|shift)\\b.*") && ("step".equals(k) || "day".equals(k)) && when > now) { a.moveTo = ymd(when); a.remindAt = 0; }
        if (!a.any()) a.ask = true;
        return a;
    }

    static String ymd(long t) { return new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(t)); }

    /* kept for the parser harness: the actions as text */
    static String plan(String k, String text, int muteDef) { return rules(k, text, muteDef).toString().trim(); }

    /* ---------- doing it ---------- */
    static String execute(Context c, JSONObject o, int nid, Act a, String text) {
        String k = o.optString("k");
        if (a.ask && !a.any()) { prefs(c).edit().remove("undo" + nid).apply(); queueReply(c, o, text); return "wrap".equals(k) ? "Got it. Plotline will tick off what you did when you open it." : "Saved. Plotline will sort it out when you open it."; }
        long rid = System.currentTimeMillis(); RID = rid;
        long mutePrev = muteUntil(c);
        StringBuilder out = new StringBuilder();
        try {
            if (a.done) { done(c, o); add(out, "✓ Marked done"); }
            if (!a.habits.isEmpty()) {
                StringBuilder names = new StringBuilder(); JSONArray hs = o.optJSONArray("hs");
                for (String id : a.habits) {
                    JSONObject h = null; if (hs != null) for (int j = 0; j < hs.length(); j++) if (id.equals(hs.optJSONObject(j).optString("id"))) h = hs.optJSONObject(j);
                    if (h == null) continue;
                    JSONObject x = q("habit"); x.put("id", id); x.put("d", o.optString("hd")); x.put("v", Math.max(1, h.optInt("v", 1))); W.enqueue(c, x);
                    Reminders.dropFor(c, "habit", id, o.optString("hd"));
                    if (names.length() > 0) names.append(", "); names.append(h.optString("t"));
                }
                if (names.length() > 0) add(out, "✓ Done: " + names);
            }
            if (a.skip) { JSONObject x = q("hskip"); x.put("id", o.optString("hid")); x.put("d", o.optString("hd")); W.enqueue(c, x); Reminders.dropFor(c, "habit", o.optString("hid"), o.optString("hd")); add(out, "Rest day noted. Your streak is safe"); }
            if (!a.moveTo.isEmpty()) { JSONObject x = q("move"); x.put("to", a.moveTo); x.put("o", o); W.enqueue(c, x); add(out, "📅 Moved to " + new java.text.SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(a.moveTo))); }
            if (a.off) { JSONObject x = q("remoff"); x.put("o", o); W.enqueue(c, x); Reminders.dropFor(c, k, "habit".equals(k) ? o.optString("hid") : "step".equals(k) ? o.optString("g") : o.optString("id"), "habit".equals(k) ? o.optString("hd") : o.optString("s")); add(out, "🔕 No more reminders for this one. Turn it back on with its 🔔 in the app"); }
            if (a.unmute) add(out, muteTo(c, 0));
            else if (a.muteUntil > System.currentTimeMillis()) add(out, muteTo(c, a.muteUntil));
            if (a.remindAt > System.currentTimeMillis()) { snoozeAt(c, o, nid, a.remindAt); add(out, "⏰ I'll remind you again " + (sameDay(a.remindAt) ? "at " : "") + when(a.remindAt)); }
            if (!a.note.isEmpty() && out.length() == 0) add(out, "📝 Noted in this reminder's history");  // replies stay out of the journal
        } catch (Exception ignored) { }
        RID = 0;
        try { JSONObject u = new JSONObject(); u.put("rid", rid); u.put("mutePrev", mutePrev); u.put("snooze", a.remindAt > 0); u.put("muted", a.muteUntil > 0 || a.unmute); prefs(c).edit().putString("undo" + nid, u.toString()).apply(); } catch (Exception ignored) { }
        return out.length() == 0 ? "Got it" : out.toString();
    }

    static void add(StringBuilder b, String s) { if (b.length() > 0) b.append(". "); b.append(s); }

    static String undo(Context c, int nid) {
        try {
            JSONObject u = new JSONObject(prefs(c).getString("undo" + nid, "{}"));
            prefs(c).edit().remove("undo" + nid).apply();
            if (u.length() == 0) return "Nothing to undo";
            int gone = W.unqueue(c, u.optLong("rid"));
            boolean other = u.optBoolean("snooze") || u.optBoolean("muted");
            if (gone == 0 && !other) return "Already saved in the app. Change it there";
            if (u.optBoolean("snooze")) cancelSnooze(c, nid);
            if (u.optBoolean("muted")) {
                long p = u.optLong("mutePrev");
                if (p > System.currentTimeMillis()) muteTo(c, p);
                else { prefs(c).edit().putLong("muteUntil", 0).apply(); Intent s = new Intent(c, Notify.class); s.setAction(SUMMARY);
                    PendingIntent pi = PendingIntent.getBroadcast(c, 299, s, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE); c.getSystemService(AlarmManager.class).cancel(pi); }
            }
            return "↩ Undone";
        } catch (Exception e) { return "Couldn't undo"; }
    }

    static void queueReply(Context c, JSONObject o, String text) {
        try { JSONObject a = q("reply"); a.put("text", text); a.put("o", o); W.enqueue(c, a); } catch (Exception ignored) { }
    }

}
