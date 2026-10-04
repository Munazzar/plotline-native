package com.munazzar.plotline;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/* Habit maths, ported line for line from the web app (hVal, hDone, hDue, hStreak, hStrength…) so both apps
   always agree. A habit is the same JSON object the web app stores; missing fields fall back to its defaults. */
final class NHabits {
    static String kind(JSONObject h) { String k = h.optString("kind", "build"); return k.equals("quit") || k.equals("routine") ? k : "build"; }
    static String icon(JSONObject h) { String i = h.optString("icon", ""); if (!i.isEmpty()) return i; String k = kind(h); return k.equals("quit") ? "🛡️" : k.equals("routine") ? "🔁" : "🌱"; }
    static String status(JSONObject h) { String s = h.optString("status", "active"); return s.equals("paused") || s.equals("archived") ? s : "active"; }
    static String freq(JSONObject h) { String f = h.optString("freq", "daily"); return f.equals("days") || f.equals("times") ? f : "daily"; }
    static int times(JSONObject h) { return Math.min(7, Math.max(1, h.optInt("times", 3))); }
    static String startDate(JSONObject h) { String s = h.optString("startDate", ""); return NDates.valid(s) ? s : NDates.ymd(h.optLong("createdAt", System.currentTimeMillis())); }
    static String part(JSONObject h) { String p = h.optString("part", "any"); return p.equals("morning") || p.equals("afternoon") || p.equals("evening") ? p : "any"; }

    static JSONObject obj(JSONObject h, String k) {
        JSONObject o = h.optJSONObject(k);
        if (o == null) { o = new JSONObject(); try { h.put(k, o); } catch (Exception ignored) { } }
        return o;
    }

    static JSONArray arr(JSONObject h, String k) {
        JSONArray a = h.optJSONArray(k);
        if (a == null) { a = new JSONArray(); try { h.put(k, a); } catch (Exception ignored) { } }
        return a;
    }

    static Set<Integer> days(JSONObject h) {
        Set<Integer> s = new HashSet<>();
        JSONArray a = h.optJSONArray("days");
        if (a != null) for (int i = 0; i < a.length(); i++) { int d = a.optInt(i, -1); if (d >= 0 && d < 7) s.add(d); }
        if (s.isEmpty()) for (int i = 0; i < 7; i++) s.add(i);
        return s;
    }

    static JSONArray steps(JSONObject h) { JSONArray a = h.optJSONArray("steps"); return a == null ? new JSONArray() : a; }

    static boolean routine(JSONObject h) { return kind(h).equals("routine") && steps(h).length() > 0; }

    static int target(JSONObject h) { return routine(h) ? steps(h).length() : Math.max(1, Math.min(999, h.optInt("target", 1))); }

    static boolean paused(JSONObject h, String ds) {
        JSONArray pz = h.optJSONArray("pz");
        if (pz == null) return false;
        for (int i = 0; i < pz.length(); i++) {
            JSONObject p = pz.optJSONObject(i); if (p == null) continue;
            String a = p.optString("a", ""), b = p.optString("b", "");
            if (ds.compareTo(a) >= 0 && (b.isEmpty() || ds.compareTo(b) <= 0)) return true;
        }
        return false;
    }

    static int val(JSONObject h, String ds) {
        if (routine(h)) {
            JSONObject rs = h.optJSONObject("rs"); if (rs == null) return 0;
            JSONArray done = rs.optJSONArray(ds); if (done == null) return 0;
            Set<String> ids = new HashSet<>(); JSONArray st = steps(h);
            for (int i = 0; i < st.length(); i++) { JSONObject s = st.optJSONObject(i); if (s != null) ids.add(s.optString("id")); }
            int n = 0; for (int i = 0; i < done.length(); i++) if (ids.contains(done.optString(i))) n++;
            return n;
        }
        JSONObject log = h.optJSONObject("log");
        return log == null ? 0 : (int) Math.round(log.optDouble(ds, 0));
    }

    static boolean skip(JSONObject h, String ds) { JSONObject s = h.optJSONObject("skip"); return s != null && s.has(ds) && s.optInt(ds, 1) != 0; }

    static boolean done(JSONObject h, String ds) { return !kind(h).equals("quit") && val(h, ds) >= target(h); }

    static boolean due(JSONObject h, String ds) {
        if (kind(h).equals("quit") || status(h).equals("archived") || ds.compareTo(startDate(h)) < 0 || paused(h, ds)) return false;
        return !freq(h).equals("days") || days(h).contains(NDates.dow(ds));
    }

    static int weekCount(JSONObject h, int w) { int n = 0; for (int k = w; k < w + 7; k++) if (done(h, NDates.fromN(k))) n++; return n; }

    static boolean counts(JSONObject h, String ds) {
        if (!due(h, ds) || skip(h, ds)) return false;
        if (freq(h).equals("times") && !done(h, ds) && weekCount(h, NDates.wkStart(NDates.dnum(ds))) >= times(h)) return false;
        return true;
    }

    /* the same change the web app's hSetVal makes */
    static void setVal(JSONObject h, String ds, int v) {
        v = Math.max(0, v);
        try {
            if (v > 0 && ds.compareTo(startDate(h)) < 0 && !kind(h).equals("quit")) h.put("startDate", ds);
            if (routine(h)) {
                JSONObject rs = obj(h, "rs"); JSONArray st = steps(h);
                if (v >= st.length()) { JSONArray a = new JSONArray(); for (int i = 0; i < st.length(); i++) a.put(st.optJSONObject(i).optString("id")); rs.put(ds, a); }
                else if (v == 0) rs.remove(ds);
                else { JSONArray a = new JSONArray(); for (int i = 0; i < v; i++) a.put(st.optJSONObject(i).optString("id")); rs.put(ds, a); }
            } else {
                JSONObject log = obj(h, "log");
                if (v > 0) log.put(ds, v); else log.remove(ds);
            }
            if (v > 0) obj(h, "skip").remove(ds);
        } catch (Exception ignored) { }
    }

    static int streak(JSONObject h) {
        int tn = NDates.today(), st = NDates.dnum(startDate(h));
        if (freq(h).equals("times")) {
            int n = 0, w = NDates.wkStart(tn), t = times(h);
            if (weekCount(h, w) >= t) n++;
            w -= 7;
            while (w + 6 >= st && n < 600) {
                boolean pz = paused(h, NDates.fromN(w + 6));
                if (pz || weekCount(h, w) >= t) { if (!pz) n++; } else break;
                w -= 7;
            }
            return n;
        }
        int n = 0;
        for (int k = tn; k >= st && tn - k < 4000; k--) {
            String ds = NDates.fromN(k);
            if (!due(h, ds) || skip(h, ds)) continue;
            if (done(h, ds)) n++; else if (k == tn) continue; else break;
        }
        return n;
    }

    static int best(JSONObject h) {
        int tn = NDates.today(), st = NDates.dnum(startDate(h)), b = 0, r = 0;
        if (freq(h).equals("times")) {
            for (int w = NDates.wkStart(st); w <= tn; w += 7) { if (weekCount(h, w) >= times(h)) { r++; b = Math.max(b, r); } else if (w + 6 < tn) r = 0; }
            return b;
        }
        for (int k = Math.max(st, tn - 4000); k <= tn; k++) {
            String ds = NDates.fromN(k);
            if (!due(h, ds) || skip(h, ds)) continue;
            if (done(h, ds)) { r++; b = Math.max(b, r); } else if (k < tn) r = 0;
        }
        return b;
    }

    static int strength(JSONObject h) {
        int tn = NDates.today(), st = Math.max(NDates.dnum(startDate(h)), tn - 180);
        double s = 0;
        if (freq(h).equals("times")) {
            for (int w = NDates.wkStart(st); w <= tn; w += 7) { int c = weekCount(h, w); if (w + 6 >= tn && c < times(h)) break; s += (Math.min(1, c / (double) times(h)) - s) * .25; }
            return (int) Math.round(s * 100);
        }
        for (int k = st; k <= tn; k++) {
            String ds = NDates.fromN(k);
            if (!due(h, ds) || skip(h, ds)) continue;
            if (k == tn && !done(h, ds)) break;
            s += (Math.min(1, val(h, ds) / (double) target(h)) - s) * .08;
        }
        return (int) Math.round(s * 100);
    }

    /* % kept over the last N days; -1 when nothing was due */
    static int rate(JSONObject h, int days) {
        int tn = NDates.today(), st = NDates.dnum(startDate(h));
        if (freq(h).equals("times")) {
            int from = Math.max(st, tn - 27), dn = 0;
            for (int k = from; k <= tn; k++) if (done(h, NDates.fromN(k))) dn++;
            int w = Math.max(1, (int) Math.ceil((tn - from + 1) / 7.0));
            return Math.min(100, Math.round(dn * 100f / (times(h) * w)));
        }
        int due = 0, dn = 0;
        for (int k = Math.max(st, tn - days + 1); k <= tn; k++) {
            String ds = NDates.fromN(k);
            if (!due(h, ds) || skip(h, ds)) continue;
            if (k == tn && !done(h, ds)) continue;
            due++; if (done(h, ds)) dn++;
        }
        return due > 0 ? Math.round(dn * 100f / due) : -1;
    }

    static String freqText(JSONObject h) {
        String f = freq(h);
        if (f.equals("times")) return times(h) + "× a week";
        if (f.equals("days")) {
            Set<Integer> d = days(h);
            if (d.size() == 7) return "Every day";
            if (d.size() == 5 && !d.contains(0) && !d.contains(6)) return "Weekdays";
            if (d.size() == 2 && d.contains(0) && d.contains(6)) return "Weekends";
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < 7; i++) if (d.contains(i)) { if (b.length() > 0) b.append(", "); b.append(NDates.DAYS[i]); }
            return b.toString();
        }
        return "Every day";
    }

    /* ---- breaking a habit ---- */
    static boolean limitMode(JSONObject h) { return "limit".equals(h.optString("mode")); }
    static int limit(JSONObject h) { return Math.max(0, h.optInt("limit", 1)); }

    static long since(JSONObject h) {
        long s = h.optLong("start", h.optLong("createdAt", System.currentTimeMillis()));
        JSONArray sl = h.optJSONArray("slips");
        if (sl != null) for (int i = 0; i < sl.length(); i++) { JSONObject x = sl.optJSONObject(i); if (x != null) s = Math.max(s, x.optLong("t")); }
        return s;
    }

    static int cleanDays(JSONObject h) { return (int) Math.max(0, (System.currentTimeMillis() - since(h)) / 86400000L); }

    static boolean limitOk(JSONObject h, String ds) { JSONObject l = h.optJSONObject("log"); return (l == null ? 0 : l.optInt(ds, 0)) <= limit(h); }

    static int limitStreak(JSONObject h) {
        int tn = NDates.today(), st = NDates.dnum(startDate(h)), n = 0;
        for (int k = tn; k >= st && tn - k < 4000; k--) { if (limitOk(h, NDates.fromN(k))) n++; else break; }
        return n;
    }

    static void slip(JSONObject h, String trig, String note) {
        try {
            JSONObject s = new JSONObject(); s.put("t", System.currentTimeMillis()); if (trig != null) s.put("trig", trig); if (note != null && !note.isEmpty()) s.put("note", note);
            arr(h, "slips").put(s); h.put("mile", 0);
        } catch (Exception ignored) { }
    }

    /* active habits that count today, in the web app's part-of-day order */
    static List<JSONObject> today(JSONArray all) {
        List<JSONObject> out = new ArrayList<>();
        String td = NDates.ymd();
        String[] parts = {"morning", "afternoon", "evening", "any"};
        for (String p : parts) for (int i = 0; i < all.length(); i++) {
            JSONObject h = all.optJSONObject(i);
            if (h == null || !status(h).equals("active") || kind(h).equals("quit") || !part(h).equals(p)) continue;
            if (counts(h, td)) out.add(h);
        }
        return out;
    }

    static List<String> keys(JSONObject o) { List<String> k = new ArrayList<>(); if (o == null) return k; Iterator<String> it = o.keys(); while (it.hasNext()) k.add(it.next()); return k; }

    /* habits due on a day and how many were kept: the web's dayRatio -> {due, done} */
    static int[] dayRatio(JSONArray all, String ds) {
        int due = 0, dn = 0; String td = NDates.ymd();
        for (int i = 0; i < all.length(); i++) {
            JSONObject h = all.optJSONObject(i);
            if (h == null || kind(h).equals("quit") || status(h).equals("archived") || !due(h, ds) || skip(h, ds)) continue;
            boolean d = done(h, ds);
            if (freq(h).equals("times") && !d && (!ds.equals(td) || !counts(h, ds))) continue;
            due++; if (d) dn++;
        }
        return new int[]{due, dn};
    }

    static int perfectRun(JSONArray all) {
        int tn = NDates.today(), ms = tn;
        for (int i = 0; i < all.length(); i++) { JSONObject h = all.optJSONObject(i); if (h != null) ms = Math.min(ms, NDates.dnum(startDate(h))); }
        int n = 0;
        for (int k = tn; k >= ms && k > tn - 800; k--) {
            int[] r = dayRatio(all, NDates.fromN(k));
            if (r[0] == 0) continue;
            if (r[1] >= r[0]) n++; else if (k == tn) continue; else break;
        }
        return n;
    }

    static long slipT(JSONObject s) { return s == null ? 0 : s.optLong("t"); }
}
