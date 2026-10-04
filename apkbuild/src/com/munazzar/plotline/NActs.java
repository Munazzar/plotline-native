package com.munazzar.plotline;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/* The web app's actions (toggleStep, toggleDay, hTap…) with the same side effects: journal log lines,
   repeating steps, goals completing when their last step is done. */
final class NActs {
    static List<JSONObject> goals(NStore st, String status) {
        List<JSONObject> l = new ArrayList<>();
        for (JSONObject g : NStore.list(st.arr("goals"))) if (status == null || status.equals(g.optString("status", "active"))) l.add(g);
        return l;
    }

    static List<JSONObject> kids(NStore st, JSONObject g) {
        List<JSONObject> l = new ArrayList<>();
        for (JSONObject x : NStore.list(st.arr("goals"))) if (g.optString("id").equals(x.optString("parent")) && x != g) l.add(x);
        return l;
    }

    static double prog(NStore st, JSONObject g, int dep) {
        List<JSONObject> k = dep < 8 ? kids(st, g) : new ArrayList<JSONObject>();
        JSONArray steps = g.optJSONArray("steps"); int sn = steps == null ? 0 : steps.length();
        int n = sn + k.size();
        if (n == 0) return "done".equals(g.optString("status")) ? 1 : 0;
        double d = 0;
        for (int i = 0; i < sn; i++) if (steps.optJSONObject(i).optBoolean("done")) d++;
        for (JSONObject x : k) d += "done".equals(x.optString("status")) ? 1 : prog(st, x, dep + 1);
        return d / n;
    }

    static int pct(NStore st, JSONObject g) { return (int) Math.round(prog(st, g, 0) * 100); }

    static JSONObject nextStep(JSONObject g) {
        if (!"active".equals(g.optString("status", "active"))) return null;
        JSONArray s = g.optJSONArray("steps"); if (s == null) return null;
        for (int i = 0; i < s.length(); i++) { JSONObject x = s.optJSONObject(i); if (x != null && !x.optBoolean("done")) return x; }
        return null;
    }

    static int doneSteps(JSONObject g) { JSONArray s = g.optJSONArray("steps"); int n = 0; if (s != null) for (int i = 0; i < s.length(); i++) if (s.optJSONObject(i).optBoolean("done")) n++; return n; }

    static int dueKey(JSONObject s) { String d = s.optString("due"); return NDates.valid(d) ? NDates.daysUntil(d) : 9999; }

    /* next step of every active goal, most urgent first (the web's urgency sort) */
    static List<JSONObject[]> upNext(NStore st) {
        List<JSONObject[]> l = new ArrayList<>();
        for (JSONObject g : goals(st, "active")) { JSONObject s = nextStep(g); if (s != null) l.add(new JSONObject[]{g, s}); }
        Collections.sort(l, new Comparator<JSONObject[]>() { public int compare(JSONObject[] a, JSONObject[] b) {
            int d = dueKey(a[1]) - dueKey(b[1]); if (d != 0) return d;
            int p = (b[0].optBoolean("pinned") ? 1 : 0) - (a[0].optBoolean("pinned") ? 1 : 0); if (p != 0) return p;
            return a[0].optInt("priority", 2) - b[0].optInt("priority", 2);
        } });
        return l;
    }

    static void log(NStore st, String type, String goalId, String text, String sid) {
        try {
            JSONObject e = new JSONObject();
            e.put("id", NStore.uid()); e.put("t", System.currentTimeMillis()); e.put("type", type); e.put("goalId", goalId == null ? JSONObject.NULL : goalId);
            e.put("text", text); e.put("sid", sid == null ? JSONObject.NULL : sid); e.put("img", JSONObject.NULL);
            st.arr("entries").put(e);
        } catch (Exception ignored) { }
    }

    /* returns a message to show, or null */
    static String toggleStep(NStore st, JSONObject g, JSONObject s) {
        String msg = null;
        try {
            boolean done = !s.optBoolean("done");
            s.put("done", done); s.put("doneAt", done ? (Object) System.currentTimeMillis() : JSONObject.NULL);
            JSONArray steps = g.optJSONArray("steps");
            String rep = s.optString("repeat", "");
            if (done && !rep.isEmpty()) {
                boolean has = false;
                for (int i = 0; i < steps.length(); i++) if (s.optString("id").equals(steps.optJSONObject(i).optString("from"))) has = true;
                if (!has) {
                    String base = NDates.valid(s.optString("due")) && NDates.daysUntil(s.optString("due")) >= 0 ? s.optString("due") : NDates.ymd();
                    Calendar k = NDates.cal(base);
                    if (rep.equals("daily")) k.add(Calendar.DATE, 1); else if (rep.equals("weekly")) k.add(Calendar.DATE, 7); else k.add(Calendar.MONTH, 1);
                    JSONObject n = NForms.newStep(s.optString("title"), NDates.ymd(k), s.optString("time"));
                    n.put("from", s.optString("id")); n.put("remind", s.opt("remind") == null ? "" : s.opt("remind")); n.put("note", s.optString("note")); n.put("repeat", rep);
                    int at = 0; for (int i = 0; i < steps.length(); i++) if (steps.optJSONObject(i) == s) at = i;
                    insert(steps, at + 1, n);
                    msg = "Done. Next one is " + NDates.dueShort(NDates.ymd(k)).toLowerCase();
                }
            }
            if (done) {
                log(st, "step", g.optString("id"), s.optString("title"), s.optString("id"));
                if ("active".equals(g.optString("status", "active")) && prog(st, g, 0) >= 1) { completeGoal(st, g); msg = "Goal achieved: " + g.optString("title"); }
            } else {
                /* undo: drop the journal line, and reopen the goal if it had been completed (the web app does the same) */
                JSONArray es = st.arr("entries");
                boolean reopen = "done".equals(g.optString("status"));
                for (int i = es.length() - 1; i >= 0; i--) {
                    JSONObject e = es.optJSONObject(i); if (e == null) continue;
                    if ("step".equals(e.optString("type")) && s.optString("id").equals(NStore.s(e, "sid"))) es.remove(i);
                    else if (reopen && "goal-done".equals(e.optString("type")) && g.optString("id").equals(NStore.s(e, "goalId"))) es.remove(i);
                }
                if (reopen) { g.put("status", "active"); g.put("completedAt", JSONObject.NULL); }
            }
        } catch (Exception e) { NCrash.log(st.ctx, "toggleStep", e); }
        return msg;
    }

    static void insert(JSONArray a, int at, Object o) throws Exception {
        List<Object> l = new ArrayList<>(); for (int i = 0; i < a.length(); i++) l.add(a.get(i));
        l.add(Math.min(at, l.size()), o);
        while (a.length() > 0) a.remove(a.length() - 1);
        for (Object x : l) a.put(x);
    }

    static void completeGoal(NStore st, JSONObject g) {
        try {
            g.put("status", "done"); g.put("completedAt", System.currentTimeMillis());
            log(st, "goal-done", g.optString("id"), g.optString("title"), null);
            JSONObject p = st.find("goals", NStore.s(g, "parent"));
            if (p != null && p != g && "active".equals(p.optString("status")) && prog(st, p, 0) >= 1) completeGoal(st, p);
        } catch (Exception ignored) { }
    }

    static boolean toggleDay(JSONObject x) {
        try { boolean d = !x.optBoolean("done"); x.put("done", d); x.put("doneAt", d ? (Object) System.currentTimeMillis() : JSONObject.NULL); return d; } catch (Exception e) { return false; }
    }

    /* tap on a habit's check: +1 (or undo when complete) */
    static boolean habitTap(JSONObject h) {
        String td = NDates.ymd();
        int v = NHabits.val(h, td), n = NHabits.target(h);
        if (v >= n) { NHabits.setVal(h, td, 0); return false; }
        NHabits.setVal(h, td, v + 1);
        return v + 1 >= n;
    }

    /* past day toggle (habit calendar) */
    static void habitDay(JSONObject h, String ds) {
        if (ds.compareTo(NDates.ymd()) > 0) return;
        int v = NHabits.val(h, ds), n = NHabits.target(h);
        NHabits.setVal(h, ds, v >= n ? 0 : n);
    }

    /* the web app's milestone check: a streak (or clean-time) milestone writes a journal line */
    static final int[] MILES = {1, 3, 7, 14, 21, 30, 60, 90, 180, 365, 730, 1095}, SMILES = {3, 7, 14, 21, 30, 50, 66, 100, 150, 200, 365}, WMILES = {2, 4, 8, 12, 26, 52};
    static int floorMile(int[] L, int v) { int m = 0; for (int x : L) if (x <= v) m = x; return m; }

    static String milestone(NStore st, JSONObject h) {
        try {
            if (!"active".equals(NHabits.status(h)) || NHabits.kind(h).equals("quit")) return null;
            int s = NHabits.streak(h);
            boolean weeks = NHabits.freq(h).equals("times");
            int m = floorMile(weeks ? WMILES : SMILES, s);
            if (m > h.optInt("smile", 0)) {
                h.put("smile", m);
                String lab = weeks ? m + "-week streak" : m + "-day streak";
                JSONObject e = new JSONObject();
                e.put("id", NStore.uid()); e.put("t", System.currentTimeMillis()); e.put("type", "habit"); e.put("goalId", NStore.s(h, "goalId").isEmpty() ? JSONObject.NULL : NStore.s(h, "goalId"));
                e.put("hid", h.optString("id")); e.put("text", lab + " · " + h.optString("title")); e.put("sid", JSONObject.NULL); e.put("img", JSONObject.NULL);
                st.arr("entries").put(e);
                return NHabits.icon(h) + " Milestone: " + lab;
            } else if (m < h.optInt("smile", 0)) h.put("smile", m);
        } catch (Exception ignored) { }
        return null;
    }

    static List<JSONObject> daysOn(NStore st, String date) {
        List<JSONObject> l = new ArrayList<>();
        for (JSONObject x : NStore.list(st.arr("days"))) if (date.equals(x.optString("date"))) l.add(x);
        Collections.sort(l, new Comparator<JSONObject>() { public int compare(JSONObject a, JSONObject b) {
            String ta = a.optString("time"), tb = b.optString("time"); if (ta.isEmpty()) ta = "99"; if (tb.isEmpty()) tb = "99";
            int c = ta.compareTo(tb); return c != 0 ? c : Long.compare(a.optLong("createdAt"), b.optLong("createdAt"));
        } });
        return l;
    }

    /* steps due on a date */
    static List<JSONObject[]> stepsOn(NStore st, String date) {
        List<JSONObject[]> l = new ArrayList<>();
        for (JSONObject g : goals(st, "active")) {
            JSONArray s = g.optJSONArray("steps"); if (s == null) continue;
            for (int i = 0; i < s.length(); i++) { JSONObject x = s.optJSONObject(i); if (x != null && date.equals(x.optString("due"))) l.add(new JSONObject[]{g, x}); }
        }
        return l;
    }

    static int stepsDoneThisWeek(NStore st) {
        int ws = NDates.wkStart(NDates.today()); int n = 0;
        for (JSONObject g : NStore.list(st.arr("goals"))) {
            JSONArray s = g.optJSONArray("steps"); if (s == null) continue;
            for (int i = 0; i < s.length(); i++) { JSONObject x = s.optJSONObject(i); if (x != null && x.optBoolean("done") && x.optLong("doneAt", 0) > 0 && NDates.dnum(NDates.ymd(x.optLong("doneAt"))) >= ws) n++; }
        }
        return n;
    }
}
