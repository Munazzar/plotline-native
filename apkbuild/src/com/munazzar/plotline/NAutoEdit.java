package com.munazzar.plotline;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/* The ⚡ sheet: pick what checks a habit, step or day goal off by itself (steps, workout, sleep, place, screen time). */
final class NAutoEdit {
    final NShell sh; final android.content.Context c; final String k, id, gid;
    JSONObject D = new JSONObject(); String pick = ""; LinearLayout box; JSONObject status = new JSONObject();
    JSONArray apps;

    static final String[][] T = {{"steps", "👟", "Steps", "When I walk enough steps", "habit,step,day"}, {"workout", "🏋️", "Workout", "When I log a workout (Health Connect)", "habit,step,day"},
        {"sleep", "😴", "Sleep", "When I sleep long enough (Health Connect)", "habit"}, {"place", "📍", "Place", "When I’m at a place, or arrive / leave", "habit,step,day"}, {"screen", "📱", "Screen time", "When I stay under a limit in chosen apps", "habit"}};
    static final String[][] EX = {{"any", "Any workout"}, {"56", "Running"}, {"79", "Walking"}, {"8", "Cycling"}, {"70", "Strength"}, {"83", "Yoga"}, {"74", "Swimming"}, {"64", "Soccer"}, {"10", "Boxing"}, {"36", "HIIT"}};

    NAutoEdit(NShell sh, String k, String id, String gid) { this.sh = sh; this.c = sh.a; this.k = k; this.id = id; this.gid = gid; }
    static void open(NShell sh, String k, String id, String gid) { new NAutoEdit(sh, k, id, gid).show(); }

    /* a goal: choose which step */
    static void pickStep(final NShell sh, final String gid) {
        JSONObject g = sh.st.find("goals", gid); if (g == null) return;
        android.content.Context c = sh.a; NForms F = new NForms(sh);
        LinearLayout b = F.sheetBody("Automatic check-off", g.optString("title"), "Pick a step to check off by itself.");
        JSONArray a = g.optJSONArray("steps"); int n = 0;
        for (int i = 0; a != null && i < a.length(); i++) {
            final JSONObject s = a.optJSONObject(i); if (s == null || s.optBoolean("done") || NStore.isDel(s)) continue; n++;
            JSONObject au = s.optJSONObject("auto");
            b.addView(NMore.row(c, "⚡", s.optString("title"), au != null && au.has("type") ? label(sh, au) : "Off", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); v.postDelayed(new Runnable() { public void run() { open(sh, "step", s.optString("id"), gid); } }, 280); } }), NUi.mt(8));
        }
        if (n == 0) b.addView(NUi.text(c, "No open steps.", 14, NTheme.muted), NUi.mt(12));
        sh.sheet(b);
    }

    JSONObject item() {
        if (k.equals("habit")) return sh.st.find("habits", id);
        if (k.equals("day")) return sh.st.find("days", id);
        JSONObject g = sh.st.find("goals", gid); if (g == null) return null;
        JSONArray a = g.optJSONArray("steps"); for (int i = 0; a != null && i < a.length(); i++) { JSONObject s = a.optJSONObject(i); if (s != null && id.equals(s.optString("id"))) return s; }
        return null;
    }

    static String guess(String t) {
        t = t.toLowerCase();
        if (t.matches(".*(step|walk).*")) return "steps"; if (t.matches(".*(sleep|bed).*")) return "sleep";
        if (t.matches(".*(gym|workout|exercis|run|lift|train|yoga|swim|cycl|soccer|football|cricket).*")) return t.matches(".*(gym|soccer|football|cricket).*") ? "place" : "workout";
        if (t.matches(".*(social|screen|phone|instagram|tiktok|youtube|scroll).*")) return "screen"; if (t.matches(".*(office|work|mosque|masjid|church|class|school).*")) return "place";
        return "";
    }

    static JSONArray places(NShell sh) { JSONArray p = sh.st.settings().optJSONArray("places"); return p == null ? new JSONArray() : p; }
    static String placeName(NShell sh, String pid) { JSONArray p = places(sh); for (int i = 0; i < p.length(); i++) { JSONObject o = p.optJSONObject(i); if (o != null && pid.equals(o.optString("id"))) return o.optString("emoji", "📍") + " " + o.optString("name"); } return ""; }

    static String label(NShell sh, JSONObject a) {
        String t = a.optString("type");
        if (t.equals("steps")) return String.format(java.util.Locale.US, "%,d steps", a.optLong("n"));
        if (t.equals("workout")) { String kd = a.optString("kind", "any"), nm = "workout"; if (!kd.equals("any")) for (String[] e : EX) if (e[0].equals(kd)) nm = e[1].toLowerCase(); return a.optInt("mins", 20) + " min " + nm; }
        if (t.equals("sleep")) return a.optDouble("hours", 7) + " h sleep";
        if (t.equals("screen")) return a.optBoolean("log") ? "Logs screen time" : "Under " + a.optInt("max", 60) + " min";
        String pn = placeName(sh, a.optString("place")); String w = a.optString("when");
        return pn.isEmpty() ? "Place" : (w.equals("arrive") ? "Arrive at " : w.equals("leave") ? "Leave " : "At ") + pn + (a.optInt("mins") > 0 && w.isEmpty() ? " · " + a.optInt("mins") + " min" : "");
    }

    void show() {
        JSONObject o = item(); if (o == null) { NShell.toast("This item is gone"); return; }
        JSONObject a = o.optJSONObject("auto");
        try { if (a != null && a.has("type")) D = new JSONObject(a.toString()); } catch (Exception ignored) { }
        pick = D.optString("type"); if (pick.isEmpty()) { pick = guess(o.optString("title")); if (!pick.isEmpty()) try { D.put("type", pick); } catch (Exception ignored) { } }
        try { apps = new JSONArray(Auto.apps(sh.a)); } catch (Exception e) { apps = new JSONArray(); }
        NForms F = new NForms(sh);
        LinearLayout b = F.sheetBody("Automatic check-off", o.optString("title"), null);
        box = NUi.col(c); b.addView(box); sh.sheet(b); paint();
    }

    void refreshStatus() { try { status = new JSONObject(new Bridge(sh.a).autoStatus()); } catch (Exception e) { status = new JSONObject(); } }

    void set(String f, Object v) { try { D.put(f, v); if (f.equals("when") && "now".equals(v)) { D.put("when", "now"); D.put("mins", 0); } } catch (Exception ignored) { } paint(); }

    void chips(String f, String[][] opts, String cur) {
        NFlow fl = new NFlow(c, 8, 8);
        for (final String[] o : opts) { final String ff = f; fl.addView(NUi.chip(c, o[1], o[0].equals(cur), new View.OnClickListener() { public void onClick(View v) { Object val = o[0]; try { val = o[0].isEmpty() ? "" : Double.valueOf(o[0]); if (((Double) val) % 1 == 0) val = Long.valueOf(((Double) val).longValue()); } catch (Exception ignored) { val = o[0]; } set(ff, val); } }), new ViewGroup.MarginLayoutParams(-2, NUi.dp(40))); }
        box.addView(fl);
    }
    void label(String t) { box.addView(NForms.fieldLabel(c, t)); }

    void perm(final String t) {
        sh.a.js("window.__nact('', 'autoPerm', {t:" + JSONObject.quote(t) + "}, false)");
        for (int i = 1; i <= 3; i++) box.postDelayed(new Runnable() { public void run() { paint(); } }, 1200L * i);
    }

    JSONObject need(String t) {
        try {
            JSONObject st = status;
            if (t.equals("steps") && st.has("steps")) { JSONObject s = st.getJSONObject("steps"); if (!s.optBoolean("ok")) return o3("No step counter on this phone", "Try Health Connect workouts instead.", "OK"); if (!s.optBoolean("perm")) return o3("Allow physical activity", "Plotline reads the phone’s step counter. Nothing leaves the phone.", "Allow"); }
            if ((t.equals("workout") || t.equals("sleep")) && st.has("hc")) { JSONObject s = st.getJSONObject("hc"); if (!s.optBoolean("ok")) return o3("Needs Android 14", "Health Connect is built into Android 14 and newer.", "OK"); if (!s.optBoolean("perm")) return o3("Connect Health Connect", "Plotline only reads workouts, sleep and steps, never writes.", "Connect"); }
            if (t.equals("screen") && st.has("screen") && !st.getJSONObject("screen").optBoolean("perm")) return o3("Allow usage access", "Android lists apps by name; turn on Plotline. It only counts minutes in the apps you pick.", "Open");
            if (t.equals("place") && st.has("loc")) { JSONObject s = st.getJSONObject("loc"); if (!s.optBoolean("perm")) return o3("Allow location", "Used only to know when you’re at your saved places.", "Allow"); if (!s.optBoolean("bg")) return o3("Allow location all the time", "So places work with the app closed. Choose “Allow all the time”.", "Open"); }
        } catch (Exception ignored) { }
        return null;
    }
    JSONObject o3(String a, String b, String c) { try { return new JSONObject().put("t", a).put("x", b).put("b", c); } catch (Exception e) { return null; } }

    boolean ready() {
        String t = pick; if (t.equals("place")) return !D.optString("place").isEmpty() && !placeName(sh, D.optString("place")).isEmpty();
        if (t.equals("screen")) return D.optJSONArray("apps") != null && D.optJSONArray("apps").length() > 0; return !t.isEmpty();
    }

    void paint() {
        box.removeAllViews(); refreshStatus();
        final JSONObject o = item(); if (o == null) return;
        final boolean has = o.optJSONObject("auto") != null && o.optJSONObject("auto").has("type");
        for (final String[] t : T) {
            if (!(","+t[4]+",").contains("," + k + ",")) continue;
            boolean on = t[0].equals(pick);
            LinearLayout r = NUi.row(c); r.setBackground(NUi.round(on ? NTheme.alpha(NTheme.accent, .12f) : NTheme.surface, 16, on ? NTheme.accent : NTheme.line)); r.setPadding(NUi.dp(12), NUi.dp(10), NUi.dp(12), NUi.dp(10));
            r.addView(NUi.text(c, t[1], 22, NTheme.text), NUi.lp(NUi.dp(38), -2));
            LinearLayout tx = NUi.col(c); tx.addView(NUi.body(c, t[2], 15, NTheme.text, 700)); tx.addView(NUi.text(c, t[3], 12.5f, NTheme.muted), NUi.mt(2)); r.addView(tx, NUi.lpw(0, -2, 1));
            NUi.tap(r, new View.OnClickListener() { public void onClick(View v) { pick = t[0]; try { String old = D.optString("type"); if (!old.equals(t[0])) { D = new JSONObject(); D.put("type", t[0]); } } catch (Exception ignored) { } paint(); } });
            box.addView(r, NUi.mt(8));
        }
        if (pick.isEmpty()) { box.addView(NUi.text(c, "Pick what should check it off. You can change or remove it any time.", 13, NTheme.muted), NUi.mt(12)); }
        else {
            JSONObject nd = need(pick);
            if (nd != null) {
                LinearLayout w = NUi.row(c); w.setBackground(NUi.round(NTheme.surface, 16, NTheme.line)); w.setPadding(NUi.dp(12), NUi.dp(10), NUi.dp(12), NUi.dp(10)); w.setGravity(android.view.Gravity.CENTER_VERTICAL);
                LinearLayout tx = NUi.col(c); tx.addView(NUi.body(c, nd.optString("t"), 14.5f, NTheme.text, 700)); TextView s = NUi.text(c, nd.optString("x"), 12.5f, NTheme.muted); s.setLineSpacing(0, 1.15f); tx.addView(s, NUi.mt(2));
                w.addView(tx, NUi.lpw(0, -2, 1));
                final String bt = nd.optString("b"); if (!bt.equals("OK")) w.addView(NUi.btn(c, bt, true, new View.OnClickListener() { public void onClick(View v) { perm(pick); } }));
                box.addView(w, NUi.mt(14));
            }
            fields();
            final JSONObject dd = D;
            LinearLayout sr = NUi.row(c); sr.setPadding(0, NUi.dp(14), 0, NUi.dp(4));
            LinearLayout tx = NUi.col(c); tx.addView(NUi.body(c, "Tell me when it’s done", 15.5f, NTheme.text, 600)); tx.addView(NUi.text(c, "A notification with Undo", 12.5f, NTheme.muted), NUi.mt(2)); sr.addView(tx, NUi.lpw(0, -2, 1));
            final NSettings.Sw sw = new NSettings.Sw(c, D.optBoolean("tell", true)); sr.addView(sw, NUi.lp(NUi.dp(48), NUi.dp(30)));
            NUi.tap(sr, new View.OnClickListener() { public void onClick(View v) { NUi.haptic(v); sw.on = !sw.on; sw.invalidate(); try { D.put("tell", sw.on); } catch (Exception ignored) { } } });
            box.addView(sr);
        }
        LinearLayout acts = NUi.row(c); acts.setGravity(android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL); acts.setPadding(0, NUi.dp(18), 0, 0);
        if (has) acts.addView(NUi.btn(c, "Turn off", false, new View.OnClickListener() { public void onClick(View v) { try { o.remove("auto"); o.put("u", System.currentTimeMillis()); touchGoal(); } catch (Exception ignored) { } sh.save(); sh.closeSheet(); NShell.toast("Automatic check-off off"); } }));
        LinearLayout.LayoutParams l = NUi.lp(-2, -2); l.leftMargin = NUi.dp(10);
        View save = NUi.btn(c, has ? "Save" : "Turn on", true, new View.OnClickListener() { public void onClick(View v) { save(o); } });
        save.setAlpha(ready() ? 1f : .4f); acts.addView(save, l);
        box.addView(acts);
    }

    void touchGoal() { if (k.equals("step")) { JSONObject g = sh.st.find("goals", gid); if (g != null) try { g.put("u", System.currentTimeMillis()); } catch (Exception ignored) { } } }

    void save(JSONObject o) {
        if (!ready()) return;
        try {
            JSONObject d = new JSONObject(D.toString()); d.put("type", pick);
            JSONObject def = new JSONObject();
            switch (pick) { case "steps": def.put("n", 8000); break; case "workout": def.put("mins", 20); def.put("kind", "any"); break; case "sleep": def.put("hours", 7); break; case "screen": def.put("max", 60); def.put("at", "21:00"); break; default: def.put("mins", 20); }
            java.util.Iterator<String> it = def.keys(); while (it.hasNext()) { String f = it.next(); if (!d.has(f) || d.optString(f).isEmpty()) d.put(f, def.get(f)); }
            if ("now".equals(d.optString("when"))) { d.put("when", ""); d.put("mins", 0); }
            d.put("tell", d.optBoolean("tell", true));
            o.put("auto", d); o.put("u", System.currentTimeMillis()); touchGoal();
            sh.save(); sh.closeSheet(); NShell.toast("⚡ " + label(sh, d) + " · it checks itself off");
        } catch (Exception e) { NCrash.log(c, "auto save", e); }
    }

    void fields() {
        switch (pick) {
            case "steps": {
                label("Steps in a day");
                chips("n", new String[][]{{"3000", "3,000"}, {"5000", "5,000"}, {"8000", "8,000"}, {"10000", "10,000"}, {"12000", "12,000"}}, String.valueOf(D.optLong("n", 8000)));
                JSONObject s = status.optJSONObject("steps"); if (s != null && s.optBoolean("perm")) box.addView(NUi.text(c, "Today so far: " + String.format(java.util.Locale.US, "%,d", (long) s.optDouble("today", 0)) + " steps", 12.5f, NTheme.muted), NUi.mt(8));
                break; }
            case "workout": {
                label("Kind"); chips("kind", EX, D.optString("kind", "any"));
                label("At least"); chips("mins", new String[][]{{"10", "10 min"}, {"20", "20 min"}, {"30", "30 min"}, {"45", "45 min"}, {"60", "1 hour"}}, String.valueOf(D.optInt("mins", 20)));
                box.addView(NUi.text(c, "From any app that writes to Health Connect, like Samsung Health, Fitbit or Strava.", 12.5f, NTheme.muted), NUi.mt(8)); break; }
            case "sleep": {
                label("At least"); chips("hours", new String[][]{{"6", "6 h"}, {"6.5", "6½ h"}, {"7", "7 h"}, {"7.5", "7½ h"}, {"8", "8 h"}}, trim(D.optDouble("hours", 7)));
                box.addView(NUi.text(c, "Last night’s sleep, from Health Connect.", 12.5f, NTheme.muted), NUi.mt(8)); break; }
            case "place": {
                label("Place"); JSONArray p = places(sh);
                if (p.length() > 0) { NFlow fl = new NFlow(c, 8, 8); for (int i = 0; i < p.length(); i++) { final JSONObject o = p.optJSONObject(i); if (o == null) continue; fl.addView(NUi.chip(c, o.optString("emoji", "📍") + " " + o.optString("name"), o.optString("id").equals(D.optString("place")), new View.OnClickListener() { public void onClick(View v) { set("place", o.optString("id")); } }), new ViewGroup.MarginLayoutParams(-2, NUi.dp(40))); } box.addView(fl); }
                box.addView(NUi.btn(c, "＋  Add the place I’m at now", false, new View.OnClickListener() { public void onClick(View v) { NSheets.placeNew(sh, null); box.postDelayed(new Runnable() { public void run() { if (box.isAttachedToWindow()) { try { JSONArray q = places(sh); if (q.length() > 0 && D.optString("place").isEmpty()) D.put("place", q.optJSONObject(q.length() - 1).optString("id")); } catch (Exception ignored) { } paint(); } } }, 9000); } }), NUi.mt(8));
                String w = D.optString("when"); String cw = w.equals("now") ? "now" : w.equals("arrive") || w.equals("leave") ? "x" : "";
                label("Check it off"); chips("when", new String[][]{{"", "After I’ve been there a while"}, {"now", "As soon as I arrive"}}, cw.equals("x") ? "?" : cw);
                if (!w.equals("now") && !w.equals("arrive") && !w.equals("leave")) { label("Time there"); chips("mins", new String[][]{{"10", "10 min"}, {"20", "20 min"}, {"30", "30 min"}, {"45", "45 min"}, {"60", "1 hour"}}, String.valueOf(D.optInt("mins", 20))); }
                label("Or just remind me"); chips("when", new String[][]{{"arrive", "When I arrive"}, {"leave", "When I leave"}}, w);
                break; }
            case "screen": {
                label("Apps"); JSONArray sel = D.optJSONArray("apps"); StringBuilder sb = new StringBuilder();
                for (int i = 0; sel != null && i < sel.length(); i++) sb.append(i > 0 ? ", " : "").append(appName(sel.optString(i)));
                box.addView(NUi.text(c, sb.length() == 0 ? "None picked yet" : sb.toString(), 13.5f, NTheme.text));
                box.addView(NUi.btn(c, "Choose apps", false, new View.OnClickListener() { public void onClick(View v) { chooseApps(); } }), NUi.mt(8));
                JSONObject o = item(); boolean lim = o != null && "quit".equals(o.optString("kind")) && "limit".equals(o.optString("mode"));
                if (lim) box.addView(NUi.text(c, "This habit counts minutes: Plotline logs the time you spend in these apps each day, and the limit does the rest.", 12.5f, NTheme.muted), NUi.mt(10));
                else { label("Done if I stay under"); chips("max", new String[][]{{"15", "15 min"}, {"30", "30 min"}, {"60", "1 hour"}, {"90", "1½ h"}, {"120", "2 h"}}, String.valueOf(D.optInt("max", 60)));
                    label("Check at"); final String at = D.optString("at", "21:00");
                    TextView t = NUi.body(c, NDates.fmtTime(at), 15, NTheme.accent, 700); t.setPadding(0, NUi.dp(4), 0, NUi.dp(4)); box.addView(t);
                    NUi.tap(t, new View.OnClickListener() { public void onClick(View v) { int h = 21, m = 0; try { h = Integer.parseInt(at.substring(0, 2)); m = Integer.parseInt(at.substring(3, 5)); } catch (Exception ignored) { }
                        new android.app.TimePickerDialog(sh.a, new android.app.TimePickerDialog.OnTimeSetListener() { public void onTimeSet(android.widget.TimePicker tp, int hh, int mm) { set("at", NSettings.hhmm(hh, mm)); } }, h, m, false).show(); } }); }
                JSONObject s = status.optJSONObject("screen"); JSONArray ap = D.optJSONArray("apps");
                if (s != null && s.optBoolean("perm") && ap != null && ap.length() > 0) { StringBuilder pk = new StringBuilder(); for (int i = 0; i < ap.length(); i++) pk.append(i > 0 ? "," : "").append(ap.optString(i)); String mn = "0"; try { mn = String.valueOf(new Bridge(sh.a).screenNow(pk.toString())); } catch (Exception ignored) { } box.addView(NUi.text(c, "Today so far: " + mn + " min", 12.5f, NTheme.muted), NUi.mt(8)); }
                break; }
        }
    }

    static String trim(double d) { return d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d); }

    String appName(String pk) { for (int i = 0; i < apps.length(); i++) { JSONObject a = apps.optJSONObject(i); if (a != null && pk.equals(a.optString("pk"))) return a.optString("name"); } return pk.substring(pk.lastIndexOf('.') + 1); }

    void chooseApps() {
        NForms F = new NForms(sh);
        LinearLayout b = F.sheetBody("Automatic check-off", "Choose apps", "Social apps are first.");
        final java.util.Set<String> sel = new java.util.HashSet<>(); JSONArray cur = D.optJSONArray("apps"); for (int i = 0; cur != null && i < cur.length(); i++) sel.add(cur.optString(i));
        List<JSONObject> L = new ArrayList<>(); for (int i = 0; i < apps.length(); i++) { JSONObject a = apps.optJSONObject(i); if (a != null) L.add(a); }
        java.util.Collections.sort(L, new java.util.Comparator<JSONObject>() { public int compare(JSONObject x, JSONObject y) { int sx = x.optInt("cat") == 4 ? 0 : 1, sy = y.optInt("cat") == 4 ? 0 : 1; return sx != sy ? sx - sy : x.optString("name").compareToIgnoreCase(y.optString("name")); } });
        if (L.isEmpty()) b.addView(NUi.text(c, "No apps found.", 13.5f, NTheme.muted), NUi.mt(12));
        for (final JSONObject a : L) {
            final TextView ck = NUi.body(c, sel.contains(a.optString("pk")) ? "✓" : "", 14, NTheme.onAccent, 800); ck.setGravity(android.view.Gravity.CENTER);
            final LinearLayout r = NUi.row(c); r.setPadding(0, NUi.dp(9), 0, NUi.dp(9)); r.setGravity(android.view.Gravity.CENTER_VERTICAL);
            ck.setBackground(NUi.round(sel.contains(a.optString("pk")) ? NTheme.accent : 0, 7, sel.contains(a.optString("pk")) ? NTheme.accent : NTheme.line2)); r.addView(ck, NUi.lp(NUi.dp(24), NUi.dp(24)));
            TextView nm = NUi.body(c, a.optString("name") + (a.optInt("cat") == 4 ? "  · social" : ""), 15, NTheme.text, 600); nm.setPadding(NUi.dp(12), 0, 0, 0); r.addView(nm, NUi.lpw(0, -2, 1));
            NUi.tap(r, new View.OnClickListener() { public void onClick(View v) { String pk = a.optString("pk"); if (!sel.remove(pk)) sel.add(pk); boolean on = sel.contains(pk); ck.setText(on ? "✓" : ""); ck.setBackground(NUi.round(on ? NTheme.accent : 0, 7, on ? NTheme.accent : NTheme.line2)); } });
            b.addView(r);
        }
        b.addView(NForms.actions(c, NUi.btn(c, "Cancel", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); sh.pager.postDelayed(new Runnable() { public void run() { show(); } }, 250); } }),
            NUi.btn(c, "Done", true, new View.OnClickListener() { public void onClick(View v) { try { D.put("apps", new JSONArray(sel)); D.put("type", "screen"); } catch (Exception ignored) { } pick = "screen"; sh.closeSheet(); sh.pager.postDelayed(new Runnable() { public void run() { show2(); } }, 250); } })));
        sh.sheet(b);
    }

    /* reopen the sheet keeping the draft */
    void show2() {
        JSONObject o = item(); if (o == null) return;
        NForms F = new NForms(sh); LinearLayout b = F.sheetBody("Automatic check-off", o.optString("title"), null);
        box = NUi.col(c); b.addView(box); sh.sheet(b); paint();
    }
}
