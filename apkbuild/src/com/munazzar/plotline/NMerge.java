package com.munazzar.plotline;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/* The web app's mergeDocs(), ported: every item carries u (last changed), deletions are tombstones in dead.
   Newer u wins; a tombstone newer than an item removes it; thread updates merge one by one. */
final class NMerge {
    static final String[] COLS = {"goals", "entries", "days", "habits", "chapters", "shares", "threads"};

    static JSONObject dead(JSONObject a, JSONObject b) throws Exception {
        JSONObject d = new JSONObject();
        for (JSONObject src : new JSONObject[]{a.optJSONObject("dead"), b.optJSONObject("dead")}) {
            if (src == null) continue;
            Iterator<String> it = src.keys();
            while (it.hasNext()) { String k = it.next(); d.put(k, Math.max(d.optLong(k, 0), src.optLong(k, 0))); }
        }
        long cut = System.currentTimeMillis() - 120L * 86400000L;
        List<String> rm = new ArrayList<>(); Iterator<String> it = d.keys();
        while (it.hasNext()) { String k = it.next(); if (d.optLong(k) < cut) rm.add(k); }
        for (String k : rm) d.remove(k);
        return d;
    }

    static JSONArray pick(JSONArray xs, JSONArray ys, JSONObject dead) {
        Map<String, JSONObject> m = new LinkedHashMap<>();
        if (xs != null) for (int i = 0; i < xs.length(); i++) { JSONObject o = xs.optJSONObject(i); if (o != null && o.has("id")) m.put(o.optString("id"), o); }
        if (ys != null) for (int i = 0; i < ys.length(); i++) {
            JSONObject o = ys.optJSONObject(i); if (o == null || !o.has("id")) continue;
            JSONObject c = m.get(o.optString("id"));
            if (c == null || o.optLong("u", 0) > c.optLong("u", 0)) m.put(o.optString("id"), o);
        }
        JSONArray out = new JSONArray();
        for (JSONObject o : m.values()) if (!(dead.optLong(o.optString("id"), -1) >= o.optLong("u", 0))) out.put(o);
        return out;
    }

    static JSONArray threads(JSONArray xs, JSONArray ys, JSONObject dead) throws Exception {
        Map<String, JSONObject> m = new LinkedHashMap<>();
        if (xs != null) for (int i = 0; i < xs.length(); i++) { JSONObject o = xs.optJSONObject(i); if (o != null) m.put(o.optString("id"), o); }
        if (ys != null) for (int i = 0; i < ys.length(); i++) {
            JSONObject o = ys.optJSONObject(i); if (o == null) continue;
            JSONObject c = m.get(o.optString("id"));
            if (c == null) { m.put(o.optString("id"), o); continue; }
            JSONObject win = o.optLong("u", 0) > c.optLong("u", 0) ? o : c, lose = win == o ? c : o;
            Map<String, JSONObject> ups = new LinkedHashMap<>();
            for (JSONObject t : new JSONObject[]{lose, win}) {
                JSONArray a = t.optJSONArray("ups"); if (a == null) continue;
                for (int j = 0; j < a.length(); j++) {
                    JSONObject x = a.optJSONObject(j); if (x == null) continue;
                    JSONObject e = ups.get(x.optString("id"));
                    if (e == null || x.optLong("u", 0) >= e.optLong("u", 0)) ups.put(x.optString("id"), x);
                }
            }
            List<JSONObject> l = new ArrayList<>(ups.values());
            java.util.Collections.sort(l, new java.util.Comparator<JSONObject>() { public int compare(JSONObject p, JSONObject q) { return Long.compare(p.optLong("t"), q.optLong("t")); } });
            JSONObject w = new JSONObject(win.toString()); JSONArray ua = new JSONArray(); for (JSONObject x : l) ua.put(x); w.put("ups", ua);
            m.put(o.optString("id"), w);
        }
        JSONArray out = new JSONArray();
        for (JSONObject o : m.values()) if (!(dead.optLong(o.optString("id"), -1) >= o.optLong("u", 0))) out.put(o);
        return out;
    }

    /* merged copy of the synced parts of a and b; settings are not touched */
    static JSONObject merge(JSONObject a, JSONObject b) throws Exception {
        JSONObject dead = dead(a, b), out = new JSONObject();
        JSONArray goals = pick(a.optJSONArray("goals"), b.optJSONArray("goals"), dead);
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < goals.length(); i++) ids.add(goals.optJSONObject(i).optString("id"));
        for (int i = 0; i < goals.length(); i++) {
            JSONObject g = goals.optJSONObject(i); JSONArray l = g.optJSONArray("links"); if (l == null) continue;
            JSONArray k = new JSONArray(); for (int j = 0; j < l.length(); j++) if (ids.contains(l.optString(j))) k.put(l.optString(j));
            if (k.length() != l.length()) g.put("links", k);
        }
        out.put("goals", goals);
        for (String c : new String[]{"entries", "days", "habits", "chapters", "shares"}) out.put(c, pick(a.optJSONArray(c), b.optJSONArray(c), dead));
        out.put("threads", threads(a.optJSONArray("threads"), b.optJSONArray("threads"), dead));
        JSONObject ia = a.optJSONObject("ident"), ib = b.optJSONObject("ident");
        out.put("ident", ia == null ? (ib == null ? JSONObject.NULL : ib) : ib == null ? ia : (ia.optLong("c", 0) <= ib.optLong("c", 0) ? ia : ib));
        out.put("dead", dead);
        return out;
    }
}
