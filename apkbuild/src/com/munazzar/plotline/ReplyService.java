package com.munazzar.plotline;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

/*
 * Reads a reply typed into a notification with the on-device model (llama.cpp, the same one Ask uses),
 * right away, with the app closed. The model turns the words into actions under a JSON schema
 * (done, skip, remind in/at, mute for/until, unmute, move to a day, habits done, note); the phone
 * then does them. Exact times typed in the reply ("for 2 days", "till 4:30pm") are read by the
 * phone's own parser and win over the model's arithmetic. If the model is missing or fails,
 * the quick reader in Notify handles it.
 */
public class ReplyService extends Service {
    static final ExecutorService EX = Executors.newSingleThreadExecutor();
    static final String CH = "working";

    static boolean ready(Context c) {
        String m = Notify.prefs(c).getString("aiModel", "");
        return !m.isEmpty() && LocalLlm.supported(c) && new File(LocalLlm.dir(c), m).exists();
    }

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public int onStartCommand(final Intent i, int flags, final int startId) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CH, "Reading replies", NotificationManager.IMPORTANCE_MIN));
        Notification n = new Notification.Builder(this, CH).setSmallIcon(W.id(this, "ic_stat", "drawable")).setContentTitle("Reading your reply").setProgress(0, 0, true).build();
        try { startForeground(2901, n, 1 /* FOREGROUND_SERVICE_TYPE_DATA_SYNC */); } catch (Exception e) { try { startForeground(2901, n); } catch (Exception ignored) { } }
        EX.execute(new Runnable() { public void run() {
            try { handle(i); } catch (Exception ignored) { }
            finally { try { stopForeground(1); } catch (Exception ignored) { } stopSelf(startId); }
        } });
        return START_NOT_STICKY;
    }

    void handle(Intent i) throws Exception {
        if (i == null) return;
        JSONObject o = new JSONObject(i.getStringExtra("o") == null ? "{}" : i.getStringExtra("o"));
        int nid = i.getIntExtra("nid", 0);
        String text = i.getStringExtra("text") == null ? "" : i.getStringExtra("text");
        /* the model gets 75 seconds at most (first load included); after that the quick reader answers */
        final Notify.Act[] box = new Notify.Act[1]; final JSONObject fo = o; final String ft = text;
        Thread w = new Thread(new Runnable() { public void run() { try { box[0] = ai(ReplyService.this, fo, ft, System.currentTimeMillis()); } catch (Exception ignored) { } } });
        w.start(); try { w.join(75000); } catch (InterruptedException ignored) { }
        Notify.Act a = w.isAlive() ? null : box[0];
        boolean quick = a == null || !a.any();
        if (quick) a = Notify.rules(o.optString("k"), text, o.optInt("mute", 120));
        String msg = Notify.execute(this, o, nid, a, text);
        Notify.log(this, o, quick ? "reply" : "reply-ai", text, msg);
        Notify.confirm(this, nid, o, msg, null, !a.ask);
        W.updateAll(this);
    }

    /* ---------- the model ---------- */
    static final String SYS = "You read the reply a person typed to a reminder from their habits and goals app, and turn it into actions. Reply with JSON only.\n"
        + "Actions:\n"
        + "- done: they did the thing in the reminder.\n"
        + "- skip_today: they are skipping or resting today.\n"
        + "- remind_in {amount, unit}: remind about THIS reminder again after a while.\n"
        + "- remind_at {day, time}: remind about THIS reminder at a day and a 24-hour time.\n"
        + "- mute_for {amount, unit}: silence ALL reminders for a while.\n"
        + "- mute_until {day, time}: silence ALL reminders until a day and a 24-hour time.\n"
        + "- unmute: turn all reminders back on.\n"
        + "- stop_this: no more reminders for THIS item at all.\n"
        + "- move_to {day}: move this step or day goal to another day.\n"
        + "- habits_done {ids}: for an evening list of habits, the ones they say they did.\n"
        + "- note {text}: anything else (how they feel, why). Kept with this reminder, not in the journal.\n"
        + "Use only the actions the reply asks for. Units: minutes, hours, days, weeks. Days: today, tomorrow, monday ... sunday.\n"
        + "Examples:\n"
        + "\"done ✅\" -> {\"actions\":[{\"do\":\"done\"}]}\n"
        + "\"mute for 2 days\" -> {\"actions\":[{\"do\":\"mute_for\",\"amount\":2,\"unit\":\"days\"}]}\n"
        + "\"remind me again in 5 hrs\" -> {\"actions\":[{\"do\":\"remind_in\",\"amount\":5,\"unit\":\"hours\"}]}\n"
        + "\"in meetings till 4:30pm\" -> {\"actions\":[{\"do\":\"mute_until\",\"day\":\"today\",\"time\":\"16:30\"}]}\n"
        + "\"remind me tomorrow at 8\" -> {\"actions\":[{\"do\":\"remind_at\",\"day\":\"tomorrow\",\"time\":\"08:00\"}]}\n"
        + "\"not today, feeling sick\" -> {\"actions\":[{\"do\":\"skip_today\"},{\"do\":\"note\",\"text\":\"Feeling sick\"}]}\n"
        + "\"push it to friday\" -> {\"actions\":[{\"do\":\"move_to\",\"day\":\"friday\"}]}\n"
        + "\"did it! felt great after\" -> {\"actions\":[{\"do\":\"done\"},{\"do\":\"note\",\"text\":\"Felt great after\"}]}\n"
        + "\"turn reminders back on\" -> {\"actions\":[{\"do\":\"unmute\"}]}\n"
        + "\"stop reminding me about this\" -> {\"actions\":[{\"do\":\"stop_this\"}]}";

    static final String[] DAYS = {"today", "tomorrow", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};

    static JSONObject obj(String json) { try { return new JSONObject(json); } catch (Exception e) { return new JSONObject(); } }

    static JSONObject schema(JSONObject o) throws Exception {
        JSONArray any = new JSONArray();
        String day = new JSONArray(DAYS).toString(), unit = "[\"minutes\",\"hours\",\"days\",\"weeks\"]", time = "{\"type\":\"string\",\"pattern\":\"^([01][0-9]|2[0-3]):[0-5][0-9]$\"}";
        String[] simple = {"done", "skip_today", "unmute", "stop_this"};
        for (String d : simple) any.put(obj("{\"type\":\"object\",\"properties\":{\"do\":{\"const\":\"" + d + "\"}},\"required\":[\"do\"],\"additionalProperties\":false}"));
        for (String d : new String[]{"remind_in", "mute_for"})
            any.put(obj("{\"type\":\"object\",\"properties\":{\"do\":{\"const\":\"" + d + "\"},\"amount\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":999},\"unit\":{\"enum\":" + unit + "}},\"required\":[\"do\",\"amount\",\"unit\"],\"additionalProperties\":false}"));
        for (String d : new String[]{"remind_at", "mute_until"})
            any.put(obj("{\"type\":\"object\",\"properties\":{\"do\":{\"const\":\"" + d + "\"},\"day\":{\"enum\":" + day + "},\"time\":" + time + "},\"required\":[\"do\",\"day\",\"time\"],\"additionalProperties\":false}"));
        any.put(obj("{\"type\":\"object\",\"properties\":{\"do\":{\"const\":\"move_to\"},\"day\":{\"enum\":" + day + "}},\"required\":[\"do\",\"day\"],\"additionalProperties\":false}"));
        JSONArray hs = o.optJSONArray("hs");
        if (hs != null && hs.length() > 0) {
            JSONArray ids = new JSONArray(); for (int j = 0; j < hs.length(); j++) ids.put(hs.optJSONObject(j).optString("id"));
            JSONObject it = new JSONObject(); it.put("enum", ids);
            JSONObject p = obj("{\"type\":\"object\",\"properties\":{\"do\":{\"const\":\"habits_done\"},\"ids\":{\"type\":\"array\",\"maxItems\":12}},\"required\":[\"do\",\"ids\"],\"additionalProperties\":false}");
            p.getJSONObject("properties").getJSONObject("ids").put("items", it);
            any.put(p);
        }
        any.put(obj("{\"type\":\"object\",\"properties\":{\"do\":{\"const\":\"note\"},\"text\":{\"type\":\"string\",\"maxLength\":160}},\"required\":[\"do\",\"text\"],\"additionalProperties\":false}"));
        JSONObject items = new JSONObject(); items.put("anyOf", any);
        JSONObject arr = new JSONObject(); arr.put("type", "array"); arr.put("minItems", 1); arr.put("maxItems", 4); arr.put("items", items);
        JSONObject props = new JSONObject(); props.put("actions", arr);
        JSONObject s = new JSONObject(); s.put("type", "object"); s.put("properties", props); s.put("required", new JSONArray("[\"actions\"]")); s.put("additionalProperties", false);
        return s;
    }

    static String kindText(String k) {
        switch (k) { case "habit": return "a habit due today"; case "step": return "a step of a goal"; case "day": return "a goal for the day"; case "checkin": return "a goal check-in";
            case "wrap": return "the evening list of habits still open"; default: return "a reminder"; }
    }

    static String userMsg(JSONObject o, String text, long now) {
        StringBuilder b = new StringBuilder();
        b.append("Now: ").append(new SimpleDateFormat("EEEE yyyy-MM-dd HH:mm", Locale.US).format(new Date(now))).append('\n');
        b.append("Reminder (").append(kindText(o.optString("k"))).append("): \"").append(o.optString("title")).append("\"");
        if (!o.optString("body").isEmpty()) b.append(" · ").append(o.optString("body"));
        b.append('\n');
        JSONArray hs = o.optJSONArray("hs");
        if (hs != null && hs.length() > 0) { b.append("Habits in the list:\n"); for (int j = 0; j < hs.length(); j++) b.append("- id ").append(hs.optJSONObject(j).optString("id")).append(": ").append(hs.optJSONObject(j).optString("t")).append('\n'); }
        b.append("Reply: \"").append(text.replace('"', '\'')).append("\"");
        return b.toString();
    }

    static Notify.Act ai(Context c, JSONObject o, String text, long now) throws Exception {
        SharedPrefsView p = new SharedPrefsView(c);
        String err = LocalLlm.ensure(c, p.model, p.ctx, p.threads);
        if (!err.isEmpty()) throw new Exception(err.toLowerCase(Locale.ROOT));
        JSONObject req = new JSONObject();
        JSONArray msgs = new JSONArray();
        msgs.put(new JSONObject().put("role", "system").put("content", SYS));
        msgs.put(new JSONObject().put("role", "user").put("content", userMsg(o, text, now)));
        req.put("messages", msgs); req.put("max_tokens", 160); req.put("temperature", 0);
        req.put("response_format", new JSONObject().put("type", "json_schema").put("json_schema", new JSONObject().put("name", "actions").put("schema", schema(o))));
        String out = LocalLlm.complete(req.toString(), 60000);
        return fromModel(o, text, out, now);
    }

    static final class SharedPrefsView {
        final String model; final int ctx, threads;
        SharedPrefsView(Context c) { android.content.SharedPreferences s = Notify.prefs(c); model = s.getString("aiModel", ""); ctx = s.getInt("aiCtx", 4096); threads = s.getInt("aiThreads", 4); }
    }

    /* the model's actions, checked against what the words plainly say */
    static Notify.Act fromModel(JSONObject o, String text, String out, long now) throws Exception {
        String j = out.substring(out.indexOf('{'), out.lastIndexOf('}') + 1);
        JSONArray acts = new JSONObject(j).getJSONArray("actions");
        Notify.Act a = new Notify.Act(); a.by = "ai";
        String t = Notify.low(text);
        long exact = Notify.at(t, now);
        boolean saysRemind = t.matches(".*\\b(remind|snooze)\\b.*"), saysMute = Notify.MUTE_RX.matcher(t).find() || t.matches(".*\\b(silence|no (more )?(reminders|notifications))\\b.*");
        for (int i = 0; i < acts.length(); i++) {
            JSONObject x = acts.getJSONObject(i); String d = x.optString("do");
            switch (d) {
                case "done": a.done = true; break;
                case "skip_today": a.skip = "habit".equals(o.optString("k")); if (!a.skip) a.note = a.note.isEmpty() ? text : a.note; break;
                case "unmute": a.unmute = true; break;
                case "stop_this": a.off = true; break;
                case "remind_in": case "mute_for": {
                    long w = exact > now ? exact : now + span(x.optInt("amount", 1), x.optString("unit"));
                    if (d.equals("mute_for") && saysRemind && !saysMute) d = "remind_in"; else if (d.equals("remind_in") && saysMute && !saysRemind) d = "mute_for";
                    if (d.equals("remind_in")) a.remindAt = w; else a.muteUntil = w; break; }
                case "remind_at": case "mute_until": {
                    long w = exact > now ? exact : dayTime(x.optString("day"), x.optString("time"), now);
                    if (d.equals("mute_until") && saysRemind && !saysMute) d = "remind_at"; else if (d.equals("remind_at") && saysMute && !saysRemind) d = "mute_until";
                    if (d.equals("remind_at")) a.remindAt = w; else a.muteUntil = w; break; }
                case "move_to": if ("step".equals(o.optString("k")) || "day".equals(o.optString("k"))) a.moveTo = Notify.ymd(dayTime(x.optString("day"), "09:00", now)); break;
                case "habits_done": { JSONArray ids = x.optJSONArray("ids"); if (ids != null) for (int k = 0; k < ids.length(); k++) if (!a.habits.contains(ids.optString(k))) a.habits.add(ids.optString(k)); break; }
                case "note": { String n = x.optString("text").trim(); if (n.length() > 1) a.note = n; break; }
            }
        }
        /* "lets skip today" on a habit is a rest day, even if the model only heard a note */
        if ("habit".equals(o.optString("k")) && !a.done && !a.skip && Notify.SKIP_RX.matcher(t).find()) a.skip = true;
        if (Notify.rules(o.optString("k"), text, 120, now).off) { a.off = true; a.muteUntil = 0; a.remindAt = 0; }
        if (!Notify.doable(o.optString("k"))) a.done = false;
        if ("wrap".equals(o.optString("k")) && a.done && a.habits.isEmpty()) { JSONArray hs = o.optJSONArray("hs"); if (hs != null) for (int k = 0; k < hs.length(); k++) a.habits.add(hs.optJSONObject(k).optString("id")); a.done = false; }
        return a;
    }

    static long span(int amount, String unit) {
        long m = "weeks".equals(unit) ? 10080 : "days".equals(unit) ? 1440 : "hours".equals(unit) ? 60 : 1;
        return Math.max(1, amount) * m * 60000L;
    }

    static long dayTime(String day, String time, long now) {
        Calendar n = Calendar.getInstance(); n.setTimeInMillis(now);
        Calendar e = (Calendar) n.clone(); e.set(Calendar.SECOND, 0); e.set(Calendar.MILLISECOND, 0);
        int add = 0;
        if ("tomorrow".equals(day)) add = 1;
        else for (int w = 0; w < 7; w++) if (Notify.WD[w].equals(day)) { add = (w - (n.get(Calendar.DAY_OF_WEEK) - 1) + 7) % 7; if (add == 0) add = 7; }
        e.add(Calendar.DAY_OF_YEAR, add);
        int hh = 9, mm = 0;
        try { String[] p = time.split(":"); hh = Integer.parseInt(p[0]); mm = Integer.parseInt(p[1]); } catch (Exception ignored) { }
        e.set(Calendar.HOUR_OF_DAY, hh); e.set(Calendar.MINUTE, mm);
        if (e.getTimeInMillis() <= now) e.add(Calendar.DAY_OF_YEAR, 1);
        return e.getTimeInMillis();
    }
}
