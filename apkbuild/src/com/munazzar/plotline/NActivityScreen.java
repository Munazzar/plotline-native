package com.munazzar.plotline;

import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/* Steps, sleep, workouts, screen time and places from the phone: 7/14/30 day bars (tap one to open that day),
   goals, and the day-by-day list. The web engine reads the phone and keeps settings.actLog; this shows it. */
final class NActivityScreen extends NPage {
    int range = 7, more = 14;
    NActivityScreen(NShell sh) { super(sh); }   /* web: a top-level page, no back button */

    @Override void onShow() { super.onShow(); load(false); }

    void load(boolean force) { sh.a.js("try{actLoad(" + force + ")}catch(e){}"); }

    /* engine-side extras: permissions, Health Connect state, places you're at now (web vActivity) */
    JSONObject av = new JSONObject(); String avSig = "";
    void loadAv() {
        sh.a.jsRet("(window.__nactv?window.__nactv():'')", new android.webkit.ValueCallback<String>() { public void onReceiveValue(String v) {
            String t = NSheets.unq(v); if (t == null || t.isEmpty() || t.equals(avSig)) return;
            try { JSONObject o = new JSONObject(t); if (o.has("err")) return; av = o; avSig = t; refresh(); } catch (Exception ignored) { }
        } });
    }

    View need(final String t, String text, String btn) {
        LinearLayout w = NUi.col(c);
        TextView x = NUi.text(c, text, 13, NTheme.muted); x.setLineSpacing(0, 1.18f); w.addView(x);
        w.addView(NUi.btn(c, btn, true, new View.OnClickListener() { public void onClick(View v) { sh.run("autoPerm", NMore.d("t", t)); sh.root.postDelayed(new Runnable() { public void run() { load(true); loadAv(); } }, 2500); } }), NUi.lp(-2, NUi.dp(40)));
        ((LinearLayout.LayoutParams) w.getChildAt(1).getLayoutParams()).topMargin = NUi.dp(10);
        LinearLayout.LayoutParams l = NUi.mt(10); w.setLayoutParams(l);
        return w;
    }

    View hcNote(String what, int n, String err) {
        JSONObject ok = av.optJSONObject("ok"); if (ok == null || !ok.optBoolean("hc") || !av.optBoolean("loaded")) return null;
        if (err != null && !err.isEmpty()) { TextView t = NUi.text(c, "Couldn’t read " + what + " from Health Connect: " + NForms.trunc(err, 120), 12.5f, NTheme.muted); t.setPadding(0, NUi.dp(8), 0, 0); return t; }
        if (n > 0) return null;
        LinearLayout w = NUi.col(c); w.setPadding(0, NUi.dp(8), 0, 0);
        TextView t = NUi.text(c, "Health Connect is connected but has no " + what + " from the last 30 days. If you use Samsung Health, Fitbit, Google Fit or a watch app, turn on syncing to Health Connect in that app.", 12.5f, NTheme.muted); t.setLineSpacing(0, 1.18f); w.addView(t);
        w.addView(NUi.btn(c, "Open Health Connect", false, new View.OnClickListener() { public void onClick(View v) { sh.run("hcOpen", null); } }), NUi.lp(-2, NUi.dp(40)));
        return w;
    }

    JSONObject act() { JSONObject a = st.settings().optJSONObject("act"); return a == null ? new JSONObject() : a; }
    JSONObject log() { JSONObject l = st.settings().optJSONObject("actLog"); return l == null ? new JSONObject() : l; }

    @Override void build() {
        load(false); loadAv();
        final JSONObject A = act(), L = log();
        final JSONObject ok = av.optJSONObject("ok") == null ? new JSONObject() : av.optJSONObject("ok"), hc = av.optJSONObject("hc") == null ? new JSONObject() : av.optJSONObject("hc");
        final int goal = A.optInt("goal", 8000), scr = A.optInt("scr", 180); final double sleepG = A.optDouble("sleep", 7.5);
        header("Activity",
            NUi.ibtn(c, "rep", new View.OnClickListener() { public void onClick(View v) { load(true); NShell.toast("Refreshing…"); } }),
            NUi.ibtn(c, "target", new View.OnClickListener() { public void onClick(View v) { goals(); } }),
            gear());
        add(NUi.label(c, (av.optString("at").isEmpty() ? "" : "Updated " + av.optString("at") + " · ") + "read on this phone, never uploaded", NTheme.muted), 8);

        LinearLayout seg = NUi.row(c);
        int[] rs = {7, 14, 30};
        for (final int n : rs) {
            LinearLayout.LayoutParams l = NUi.lp(-2, -2); l.rightMargin = NUi.dp(8);
            seg.addView(NUi.chip(c, n + " days", range == n, new View.OnClickListener() { public void onClick(View v) { range = n; refresh(); } }), l);
        }
        add(seg, 14);
        add(muted("Tap a bar to open that day"), 8);

        final int tn = NDates.today();
        final String[] ds = new String[range]; final float[] steps = new float[range], sleep = new float[range], work = new float[range], scrn = new float[range];
        final String[] labs = new String[range]; final boolean[] td = new boolean[range];
        float sSum = 0, zSum = 0, mSum = 0; int sN = 0, zN = 0, mN = 0, hit = 0, wkDays = 0;
        for (int i = 0; i < range; i++) {
            ds[i] = NDates.fromN(tn - range + 1 + i);
            JSONObject o = L.optJSONObject(ds[i]); if (o == null) o = new JSONObject();
            steps[i] = o.optInt("s", 0); sleep[i] = o.optInt("z", 0); work[i] = o.optInt("x", 0); scrn[i] = o.optInt("m", 0);
            td[i] = i == range - 1;
            labs[i] = range > 14 ? (i % 5 == 0 || td[i] ? String.valueOf(NDates.day(ds[i])) : "") : NDates.DAYS[NDates.dow(ds[i])].substring(0, 1);
            if (steps[i] > 0) { sSum += steps[i]; sN++; } if (steps[i] >= goal) hit++;
            if (sleep[i] > 0) { zSum += sleep[i]; zN++; } if (scrn[i] > 0) { mSum += scrn[i]; mN++; } if (work[i] > 0) wkDays++;
        }
        JSONObject today = L.optJSONObject(ds[range - 1]); if (today == null) today = new JSONObject();

        /* steps */
        LinearLayout t1 = tile("👟", "Steps");
        boolean hasS = ok.optBoolean("steps"); for (float v : steps) if (v > 0) hasS = true;
        if (!hasS) t1.addView(need("steps", "Allow physical activity so Plotline can read your phone’s step counter.", "Allow"));
        else {
        LinearLayout big = NUi.row(c); big.setGravity(Gravity.BOTTOM);
        TextView bn = NUi.text(c, String.format(java.util.Locale.US, "%,d", (int) steps[range - 1]), 40, NTheme.text); bn.setTypeface(NFont.display(800)); bn.setIncludeFontPadding(false); big.addView(bn);
        TextView bu = NUi.text(c, "  steps today", 14, NTheme.muted); big.addView(bu);
        t1.addView(big, NUi.mt(10));
        t1.addView(NBits.bar(c, Math.min(1, steps[range - 1] / goal), NTheme.accent), NUi.mt(10));
        t1.addView(NUi.text(c, Math.min(100, Math.round(steps[range - 1] * 100 / goal)) + "% of " + String.format(java.util.Locale.US, "%,d", goal) + " · avg " + String.format(java.util.Locale.US, "%,d", sN == 0 ? 0 : Math.round(sSum / sN)) + " · goal met " + hit + " of " + range + " days", 13, NTheme.muted), NUi.mt(8));
        t1.addView(bars(steps, labs, td, goal, ds), NUi.mt(8));
        }
        add(t1, 14);

        /* sleep */
        LinearLayout t2 = tile("😴", "Sleep");
        float lastZ = 0; String lastZd = ""; for (int i = range - 1; i >= 0; i--) if (sleep[i] > 0) { lastZ = sleep[i]; lastZd = ds[i]; break; }
        boolean hasZ = ok.optBoolean("hc") || lastZ > 0;
        if (!hasZ) { boolean old = ok.has("hcSupported") && !ok.optBoolean("hcSupported"); t2.addView(need("sleep", old ? "Sleep and workouts come from Health Connect, built into Android 14 and newer." : "Connect Health Connect to see sleep and workouts from your fitness apps.", old ? "OK" : "Connect")); }
        else {
        t2.addView(bigText(lastZ > 0 ? NDates.fmtMin((int) lastZ) : "—"), NUi.mt(10));
        t2.addView(NUi.text(c, (sleep[range - 1] > 0 ? "last night" : lastZ > 0 ? "last recorded · " + NDates.dayName(lastZd) : "no sleep recorded yet") + " · avg " + NDates.fmtMin(zN == 0 ? 0 : Math.round(zSum / zN)), 13, NTheme.muted), NUi.mt(4));
        t2.addView(bars(sleep, labs, td, (float) (sleepG * 60), ds), NUi.mt(8));
        View hn1 = hcNote("sleep", hc.optInt("sleep", 0), hc.optString("errSleep")); if (hn1 != null) t2.addView(hn1);
        }
        add(t2, 12);

        /* workouts */
        LinearLayout t3 = tile("🏋️", "Workouts");
        JSONArray places0 = st.settings().optJSONArray("places"); JSONArray wkf = av.optJSONArray("wk");
        List<JSONObject> WP = new ArrayList<>();
        for (int i = 0; places0 != null && i < places0.length(); i++) { JSONObject p = places0.optJSONObject(i); if (p != null && (wkf != null && i < wkf.length() ? wkf.optBoolean(i) : p.optBoolean("wk"))) WP.add(p); }
        JSONArray W = av.optJSONArray("work");
        boolean hasW = ok.optBoolean("hc") || (W != null && W.length() > 0) || wkDays > 0 || !WP.isEmpty();
        if (!hasW) t3.addView(need("workout", "Workouts come from any app that writes to Health Connect, like Samsung Health, Fitbit or Strava.", "Connect"));
        else {
            t3.addView(bigText(work[range - 1] > 0 ? NDates.fmtMin((int) work[range - 1]) : "—"), NUi.mt(10));
            t3.addView(NUi.text(c, "today · active " + wkDays + " of " + range + " days", 13, NTheme.muted), NUi.mt(4));
            t3.addView(bars(work, labs, td, 0, ds), NUi.mt(8));
            LinearLayout lb = NUi.col(c);
            if (W != null && W.length() > 0) {
                for (int i = 0; i < W.length(); i++) { JSONObject w = W.optJSONObject(i); final String d = w.optString("d"); lb.addView(listRow(w.optString("e"), w.optString("n"), NDates.fmtMin(w.optInt("m")) + " · " + NDates.dayName(d), new View.OnClickListener() { public void onClick(View v) { sh.push(new NDayScreen(sh, d)); } })); }
            } else {
                List<JSONObject> ws = new ArrayList<>();
                for (int i = range - 1; i >= 0; i--) { JSONObject o = L.optJSONObject(ds[i]); JSONArray w = o == null ? null : o.optJSONArray("w"); if (w != null) for (int j = 0; j < w.length(); j++) { JSONObject x = w.optJSONObject(j); if (x != null) ws.add(x); } }
                for (int i = 0; i < Math.min(6, ws.size()); i++) { JSONObject w = ws.get(i); final String d = NDates.ymd(w.optLong("t")); lb.addView(listRow(NDayScreen.exIcon(w.optInt("k")), NDayScreen.exName(w.optInt("k")), NDates.fmtMin(w.optInt("m")) + " · " + NDates.dayName(d), new View.OnClickListener() { public void onClick(View v) { sh.push(new NDayScreen(sh, d)); } })); }
            }
            for (JSONObject p : WP) { int m = 0; for (String d : ds) { JSONObject o = L.optJSONObject(d); JSONObject pl = o == null ? null : o.optJSONObject("p"); if (pl != null) m += pl.optInt(p.optString("id"), 0); } lb.addView(listRow(p.optString("emoji", "📍"), p.optString("name"), m > 0 ? NDates.fmtMin(m) + " in " + range + " days" : "counted when you leave", null)); }
            if (lb.getChildCount() > 0) t3.addView(lb, NUi.mt(8)); else { TextView nw = NUi.text(c, "No workouts in this period.", 13, NTheme.muted); nw.setPadding(0, NUi.dp(10), 0, 0); t3.addView(nw); }
            View hn2 = hcNote("workouts", hc.optInt("ex", 0), hc.optString("errEx")); if (hn2 != null) t3.addView(hn2);
        }
        add(t3, 12);

        /* screen time */
        LinearLayout t4 = tile("📱", "Screen time");
        boolean hasM = ok.optBoolean("screen"); for (float v : scrn) if (v > 0) hasM = true;
        if (!hasM) t4.addView(need("screen", "Turn on Usage access for Plotline to see how long you spend in apps. Only minutes are read.", "Allow"));
        else {
            t4.addView(bigText(today.has("m") ? NDates.fmtMin(today.optInt("m")) : "—"), NUi.mt(10));
            t4.addView(NUi.text(c, "today · avg " + NDates.fmtMin(mN == 0 ? 0 : Math.round(mSum / mN)) + " a day" + (scr > 0 ? " · limit " + NDates.fmtMin(scr) : ""), 13, NTheme.muted), NUi.mt(4));
            t4.addView(bars(scrn, labs, td, scr, ds), NUi.mt(8));
            LinearLayout lb = NUi.col(c); JSONArray top = av.optJSONArray("top");
            if (top != null && top.length() > 0) for (int i = 0; i < top.length(); i++) { JSONObject a = top.optJSONObject(i); lb.addView(listRow("📱", a.optString("n"), NDates.fmtMin(a.optInt("m")) + " today", null)); }
            else { JSONArray ap = today.optJSONArray("a"); for (int i = 0; ap != null && i < ap.length(); i++) { JSONArray p = ap.optJSONArray(i); if (p != null) lb.addView(listRow("📱", p.optString(0), NDates.fmtMin(p.optInt(1)) + " today", null)); } }
            if (lb.getChildCount() > 0) t4.addView(lb, NUi.mt(8));
        }
        add(t4, 12);

        /* places */
        LinearLayout t5 = tile("📍", "Places");
        JSONArray places = st.settings().optJSONArray("places");
        JSONObject inside = av.optJSONObject("inside") == null ? new JSONObject() : av.optJSONObject("inside");
        if (places != null && places.length() > 0) {
            List<String> at = new ArrayList<>(); for (int i = 0; i < places.length(); i++) { JSONObject p = places.optJSONObject(i); if (p != null && inside.optBoolean(p.optString("id"))) at.add(p.optString("name")); }
            t5.addView(bigText(at.isEmpty() ? "Away" : "At " + android.text.TextUtils.join(", ", at)), NUi.mt(10));
        }
        if (places == null || places.length() == 0) {
            t5.addView(muted("Save the places you go often (home, work, the gym) and Plotline counts the time you spend at each. Location never leaves the phone."), NUi.mt(8));
        } else {
            t5.addView(muted("Time is counted when you leave a saved place. Places you mark count as workouts."), NUi.mt(8));
            for (int i = 0; i < places.length(); i++) {
                final JSONObject p = places.optJSONObject(i); if (p == null) continue;
                int wm = 0; for (String d : ds) { JSONObject o = L.optJSONObject(d); JSONObject pl = o == null ? null : o.optJSONObject("p"); if (pl != null) wm += pl.optInt(p.optString("id"), 0); }
                int tm = 0; { JSONObject pl = today.optJSONObject("p"); if (pl != null) tm = pl.optInt(p.optString("id"), 0); }
                boolean wk = p.has("wk") ? p.optBoolean("wk") : java.util.regex.Pattern.compile("gym|fitness|work ?out|pool|swim|yoga|pilates|crossfit|dojo|boxing|martial|climb|sport|court|field|track|club|studio|🏋|💪|🏊|🧘|🥊|🧗|⚽|🏀|🎾", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(p.optString("name") + " " + p.optString("emoji")).find();
                LinearLayout row = NUi.row(c); row.setPadding(0, NUi.dp(8), 0, NUi.dp(8));
                row.addView(NUi.text(c, p.optString("emoji", "📍"), 20, NTheme.text), NUi.lp(NUi.dp(32), -2));
                LinearLayout mid = NUi.col(c);
                mid.addView(NUi.body(c, p.optString("name"), 15, NTheme.text, 700));
                mid.addView(NUi.text(c, (inside.optBoolean(p.optString("id")) ? "here now · " : "") + (tm > 0 ? NDates.fmtMin(tm) + " today · " : "") + (wm > 0 ? NDates.fmtMin(wm) + " in " + range + " days" : "no time yet"), 13, NTheme.muted));
                row.addView(mid, NUi.lpw(0, -2, 1));
                final boolean fwk = wk;
                row.addView(NUi.chip(c, wk ? "✓ Counts as workout" : "Count as workout", wk, new View.OnClickListener() { public void onClick(View v) { try { p.put("wk", !fwk); } catch (Exception ignored) { } sh.save(); } }));
                t5.addView(row);
            }
        }
        if (places != null && places.length() > 0 && !ok.optBoolean("bg") && av.optBoolean("loaded")) t5.addView(need("place", "Allow location “all the time” so visits count with the app closed.", "Allow"));
        if (places == null || places.length() == 0) t5.addView(NUi.btn(c, "📍  Add the place I’m at now", true, new View.OnClickListener() { public void onClick(View v) { NSheets.placeNew(sh, null); } }), NUi.mt(10));
        add(t5, 12);

        /* day by day */
        List<String> keys = new ArrayList<>();
        for (String k : NHabits.keys(L)) if (k.compareTo(NDates.ymd()) <= 0) keys.add(k);
        java.util.Collections.sort(keys, java.util.Collections.<String>reverseOrder());
        add(NUi.sectionHead(c, "Day by day", null, null));
        add(NUi.label(c, keys.size() + " day" + (keys.size() == 1 ? "" : "s") + " kept on this phone", NTheme.muted), -6);
        LinearLayout box = NBits.listBox(c);
        for (int i = 0; i < Math.min(more, keys.size()); i++) {
            final String k = keys.get(i); JSONObject o = L.optJSONObject(k);
            if (i > 0) box.addView(NBits.divider(c));
            String sub = (o.has("s") ? "👟 " + String.format(java.util.Locale.US, "%,d", o.optInt("s")) : "👟 —") + "   " + (o.optInt("z") > 0 ? "😴 " + NDates.fmtMin(o.optInt("z")) : "😴 —") + "   " + (o.optInt("x") > 0 ? "🏋️ " + NDates.fmtMin(o.optInt("x")) : "🏋️ —") + "   " + (o.has("m") ? "📱 " + NDates.fmtMin(o.optInt("m")) : "📱 —");
            LinearLayout row = NBits.row(c, null, NDates.dayName(k), sub, NTheme.muted, null, false);
            NUi.tap(row, new View.OnClickListener() { public void onClick(View v) { sh.push(new NDayScreen(sh, k)); } });
            box.addView(row);
        }
        add(box);
        if (keys.size() > more) add(NUi.btn(c, "Show older days", false, new View.OnClickListener() { public void onClick(View v) { more += 30; refresh(); } }), 12);
        add(muted("Habits can use these too: tap ⚡ on a habit to check it off by itself."), 18);
    }

    LinearLayout tile(String emoji, String title) {
        LinearLayout t = NUi.col(c);
        t.setBackground(NUi.card(24)); t.setPadding(NUi.dp(18), NUi.dp(16), NUi.dp(18), NUi.dp(16));
        LinearLayout h = NUi.row(c);
        h.addView(NUi.text(c, emoji, 20, NTheme.text), NUi.lp(NUi.dp(32), -2));
        h.addView(NUi.body(c, title, 17, NTheme.text, 700));
        t.addView(h);
        return t;
    }

    TextView bigText(String s) { TextView t = NUi.text(c, s, 36, NTheme.text); t.setTypeface(NFont.display(800)); t.setIncludeFontPadding(false); return t; }

    View bars(float[] v, String[] labs, boolean[] td, float goal, final String[] ds) {
        NBars b = new NBars(c); b.set(v, labs, td, goal);
        b.cb = new NBars.OnBar() { public void on(int i) { sh.push(new NDayScreen(sh, ds[i])); } };
        return b;
    }

    View listRow(String e, String t, String s, View.OnClickListener l) {
        LinearLayout r = NUi.row(c); r.setPadding(0, NUi.dp(7), 0, NUi.dp(7));
        r.addView(NUi.text(c, e, 18, NTheme.text), NUi.lp(NUi.dp(32), -2));
        r.addView(NUi.body(c, t, 15, NTheme.text, 600), NUi.lpw(0, -2, 1));
        r.addView(NBits.meta(c, s, NTheme.muted));
        if (l != null) NUi.tap(r, l);
        return r;
    }

    void goals() {
        final JSONObject A = act();
        LinearLayout b = new NForms(sh).sheetBody("Activity", "Daily goals", null);
        b.addView(NForms.fieldLabel(c, "Steps a day"));
        final EditText g = NForms.input(c, "8000", String.valueOf(A.optInt("goal", 8000)), false); g.setInputType(android.text.InputType.TYPE_CLASS_NUMBER); b.addView(g);
        b.addView(NForms.fieldLabel(c, "Sleep, hours a night"));
        final EditText z = NForms.input(c, "7.5", String.valueOf(A.optDouble("sleep", 7.5)), false); z.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL); b.addView(z);
        b.addView(NForms.fieldLabel(c, "Screen time limit, minutes a day (0 = none)"));
        final EditText s = NForms.input(c, "180", String.valueOf(A.optInt("scr", 180)), false); s.setInputType(android.text.InputType.TYPE_CLASS_NUMBER); b.addView(s);
        b.addView(NForms.actions(c, NUi.btn(c, "Cancel", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } }),
            NUi.btn(c, "Save", true, new View.OnClickListener() { public void onClick(View v) {
                try {
                    JSONObject o = new JSONObject();
                    o.put("goal", Math.max(500, parseI(g.getText().toString(), 8000)));
                    double zh = 7.5; try { zh = Double.parseDouble(z.getText().toString().trim()); } catch (Exception ignored) { }
                    o.put("sleep", Math.min(12, Math.max(3, zh)));
                    o.put("scr", Math.max(0, parseI(s.getText().toString(), 0)));
                    st.settings().put("act", o);
                } catch (Exception ignored) { }
                sh.closeSheet(); sh.save(); NShell.toast("Saved");
            } })));
        sh.sheet(b);
    }

    static int parseI(String x, int d) { try { return Integer.parseInt(x.trim()); } catch (Exception e) { return d; } }
}
