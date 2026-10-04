package com.munazzar.plotline;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/* The one copy of the user's data on the phone: the same JSON document the web app keeps (goals, habits, days,
   entries, threads… plus settings), saved to files/state.json.
   - Native screens change the JSON objects in place and call changed(): every item whose content changed gets a
     fresh u (last changed) and removed items become tombstones, exactly as the web app's stamp() does, so Google
     Drive sync merges correctly with the web version.
   - The web engine (sync, reminders, widgets, AI) reads it through Bridge.storeGet and writes through storeSet,
     which merges instead of overwriting so a native edit made meanwhile is never lost. */
final class NStore {
    interface Listener { void onStore(); }

    static NStore I;
    final Context ctx;
    final File file;
    JSONObject S;
    final Map<String, String> sig = new HashMap<>();
    final List<Listener> listeners = new ArrayList<>();
    final Handler ui = new Handler(Looper.getMainLooper());
    Runnable onLocalChange;   /* set by the activity: tells the web engine to reload */
    boolean dirty;

    static NStore get(Context c) { if (I == null) I = new NStore(c.getApplicationContext()); return I; }

    NStore(Context c) {
        ctx = c; file = new File(c.getFilesDir(), "state.json");
        S = read();
        prime();
    }

    boolean empty() { return !file.exists() || arr("goals").length() + arr("habits").length() + arr("entries").length() + arr("days").length() + arr("threads").length() == 0 && !S.optJSONObject("settings").optBoolean("onboarded"); }
    boolean exists() { return written || file.exists(); }

    JSONObject read() {
        try {
            if (file.exists()) {
                FileInputStream in = new FileInputStream(file); ByteArrayOutputStream b = new ByteArrayOutputStream();
                byte[] buf = new byte[65536]; int n; while ((n = in.read(buf)) > 0) b.write(buf, 0, n); in.close();
                JSONObject o = new JSONObject(b.toString("UTF-8"));
                fill(o); return o;
            }
        } catch (Exception e) { NCrash.log(ctx, "store read", e); }
        JSONObject o = new JSONObject(); fill(o); return o;
    }

    static void fill(JSONObject o) {
        try {
            o.put("v", 2);
            for (String c : NMerge.COLS) if (o.optJSONArray(c) == null) o.put(c, new JSONArray());
            if (o.optJSONObject("dead") == null) o.put("dead", new JSONObject());
            if (o.optJSONObject("settings") == null) o.put("settings", new JSONObject());
        } catch (Exception ignored) { }
    }

    JSONArray arr(String k) { JSONArray a = S.optJSONArray(k); if (a == null) { a = new JSONArray(); try { S.put(k, a); } catch (Exception ignored) { } } return a; }
    JSONObject settings() { JSONObject s = S.optJSONObject("settings"); if (s == null) { s = new JSONObject(); try { S.put("settings", s); } catch (Exception ignored) { } } return s; }
    JSONObject dead() { JSONObject d = S.optJSONObject("dead"); if (d == null) { d = new JSONObject(); try { S.put("dead", d); } catch (Exception ignored) { } } return d; }

    JSONObject find(String col, String id) {
        if (id == null) return null;
        JSONArray a = arr(col);
        for (int i = 0; i < a.length(); i++) { JSONObject o = a.optJSONObject(i); if (o != null && id.equals(o.optString("id"))) return o; }
        return null;
    }

    void remove(String col, String id) {
        JSONArray a = arr(col);
        for (int i = a.length() - 1; i >= 0; i--) { JSONObject o = a.optJSONObject(i); if (o != null && id.equals(o.optString("id"))) a.remove(i); }
    }

    static final Random R = new Random();
    static String uid() {
        String a = Long.toString(Math.abs(R.nextLong()), 36);
        while (a.length() < 8) a = "0" + a;
        String t = Long.toString(System.currentTimeMillis(), 36);
        return a.substring(0, 8) + t.substring(Math.max(0, t.length() - 4));
    }

    /* ---- change tracking (the web app's primeSig/stamp) ---- */
    static String sigOf(JSONObject o) {
        Object u = o.opt("u"); o.remove("u");
        String s = o.toString();
        if (u != null) try { o.put("u", u); } catch (Exception ignored) { }
        return s;
    }

    void prime() {
        sig.clear();
        long now = System.currentTimeMillis();
        for (String c : NMerge.COLS) {
            JSONArray a = arr(c);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i); if (o == null) continue;
                if (!o.has("u")) try { o.put("u", o.optLong("createdAt", o.optLong("t", now))); } catch (Exception ignored) { }
                sig.put(o.optString("id"), sigOf(o));
            }
        }
    }

    boolean stamp() {
        long now = System.currentTimeMillis(); Set<String> seen = new HashSet<>(); boolean ch = false;
        for (String c : NMerge.COLS) {
            JSONArray a = arr(c);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i); if (o == null) continue;
                String id = o.optString("id"); seen.add(id);
                String k = sigOf(o);
                if (!k.equals(sig.get(id))) { try { o.put("u", now); } catch (Exception ignored) { } sig.put(id, sigOf(o)); ch = true; }
            }
        }
        for (String id : new ArrayList<>(sig.keySet())) if (!seen.contains(id)) { try { dead().put(id, now); } catch (Exception ignored) { } sig.remove(id); ch = true; }
        return ch;
    }

    /* call after any native edit */
    void changed() {
        boolean items = stamp();
        if (items) dirty = true;
        write();
        fire();
        if (onLocalChange != null) onLocalChange.run();
    }

    void fire() { for (Listener l : new ArrayList<>(listeners)) try { l.onStore(); } catch (Exception e) { NCrash.log(ctx, "listener", e); } }

    /* snapshot on the UI thread, write on a background thread (in order) */
    final java.util.concurrent.ExecutorService io = java.util.concurrent.Executors.newSingleThreadExecutor();
    boolean written;
    /* the guided tour runs on a sandbox copy of example data: nothing is written while it is on */
    String backup; boolean sandbox;
    void sandboxBegin(String demo) {
        try { JSONObject d = new JSONObject(demo); fill(d); backup = S.toString(); S = d; sandbox = true; prime(); fire(); }
        catch (Exception e) { NCrash.log(ctx, "tour begin", e); }
    }
    void sandboxEnd() {
        if (!sandbox) return;
        try { S = new JSONObject(backup); } catch (Exception e) { NCrash.log(ctx, "tour end", e); }
        backup = null; sandbox = false; prime(); fire();
    }

    void write() {
        if (sandbox) return;
        final String snap = S.toString();
        written = true;
        io.execute(new Runnable() { public void run() { writeNow(snap); } });
    }

    synchronized void writeNow(String snap) {
        try {
            File tmp = new File(file.getPath() + ".tmp");
            FileOutputStream out = new FileOutputStream(tmp);
            out.write(snap.getBytes("UTF-8")); out.getFD().sync(); out.close();
            if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file); }
        } catch (Exception e) { NCrash.log(ctx, "store write", e); }
    }

    String json() { return S.toString(); }

    /* from the web engine (its save, its Drive sync). Merge so neither side loses edits; settings come from the web. */
    boolean fromWeb(String json) {
        if (sandbox) return true;
        try {
            JSONObject w = new JSONObject(json); fill(w);
            stamp();
            JSONObject m = NMerge.merge(S, w);
            JSONObject next = new JSONObject();
            for (String c : NMerge.COLS) next.put(c, m.getJSONArray(c));
            next.put("dead", m.getJSONObject("dead"));
            next.put("ident", m.opt("ident"));
            next.put("settings", w.optJSONObject("settings") != null ? w.getJSONObject("settings") : settings());
            next.put("v", 2);
            /* keep the very same objects for items that didn't change, so screens and open sheets that hold them
               keep editing the live copy */
            for (String c : NMerge.COLS) {
                JSONArray old = arr(c), na = next.getJSONArray(c);
                java.util.HashMap<String, JSONObject> om = new java.util.HashMap<>();
                for (int i = 0; i < old.length(); i++) { JSONObject o = old.optJSONObject(i); if (o != null) om.put(o.optString("id"), o); }
                for (int i = 0; i < na.length(); i++) {
                    JSONObject o = na.optJSONObject(i); if (o == null) continue;
                    JSONObject prev = om.get(o.optString("id"));
                    if (prev != null && prev != o && prev.optLong("u", 0) == o.optLong("u", 0) && (!c.equals("threads") || upsKey(prev).equals(upsKey(o)))) na.put(i, prev);
                }
            }
            /* compare versions (id -> u), not text: the web reorders keys without changing anything */
            String nk = versions(next), before = versions(S) + S.optJSONObject("settings");
            boolean sameAsWeb = nk.equals(versions(w));
            boolean changedHere = !before.equals(nk + next.optJSONObject("settings"));
            S = next; prime(); write();
            if (changedHere) ui.post(new Runnable() { public void run() { fire(); } });
            return sameAsWeb;
        } catch (Exception e) { NCrash.log(ctx, "fromWeb", e); return true; }
    }

    static String upsKey(JSONObject t) {
        java.util.TreeSet<String> ups = new java.util.TreeSet<>();
        JSONArray u = t.optJSONArray("ups");
        if (u != null) for (int j = 0; j < u.length(); j++) { JSONObject x = u.optJSONObject(j); if (x != null) ups.add(x.optString("id") + "@" + x.optLong("u", 0)); }
        return ups.toString();
    }

    static String versions(JSONObject d) {
        StringBuilder b = new StringBuilder();
        for (String c : NMerge.COLS) {
            java.util.TreeMap<String, String> m = new java.util.TreeMap<>();
            JSONArray a = d.optJSONArray(c);
            if (a != null) for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i); if (o == null) continue;
                String v = String.valueOf(o.optLong("u", 0));
                if (c.equals("threads")) {
                    java.util.TreeSet<String> ups = new java.util.TreeSet<>();
                    JSONArray u = o.optJSONArray("ups");
                    if (u != null) for (int j = 0; j < u.length(); j++) { JSONObject x = u.optJSONObject(j); if (x != null) ups.add(x.optString("id") + "@" + x.optLong("u", 0)); }
                    v += ups.toString();
                }
                m.put(o.optString("id"), v);
            }
            b.append(c).append(m);
        }
        JSONObject dd = d.optJSONObject("dead"); b.append("dead").append(dd == null ? 0 : dd.length());
        return b.toString();
    }

    /* a thread update the web app soft-deleted (del: 1 / true) */
    static boolean isDel(JSONObject u) {
        Object d = u.opt("del");
        return d != null && d != JSONObject.NULL && !Boolean.FALSE.equals(d) && !(d instanceof Number && ((Number) d).intValue() == 0) && !"".equals(d);
    }

    /* ---- convenience ---- */
    /* Android's optString turns a JSON null into "null"; this returns "" instead */
    static String s(JSONObject o, String k) { return o == null || o.isNull(k) ? "" : o.optString(k, ""); }
    static List<JSONObject> list(JSONArray a) { List<JSONObject> l = new ArrayList<>(); for (int i = 0; i < a.length(); i++) { JSONObject o = a.optJSONObject(i); if (o != null) l.add(o); } return l; }
}
