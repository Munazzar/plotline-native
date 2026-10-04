package com.munazzar.plotline;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/* The sheet for one past day of a habit: steps (routine), count (+/−), rest day, mark done. */
final class NHabitDay {
    static void open(final NShell sh, final JSONObject h, final String ds) {
        if (h == null || ds.compareTo(NDates.ymd()) > 0) return;
        final android.content.Context c = sh.a; final NForms F = new NForms(sh);
        final int n = NHabits.target(h); final boolean rt = NHabits.routine(h) && NHabits.steps(h).length() > 0;
        LinearLayout b = F.sheetBody(NDates.dayName(ds), (h.optString("icon").isEmpty() ? "" : h.optString("icon") + " ") + h.optString("title"), null);
        if (rt) {
            final JSONArray steps = NHabits.steps(h); JSONObject rs = NHabits.obj(h, "rs"); JSONArray a = rs.optJSONArray(ds); final Set<String> L = new HashSet<>();
            if (a != null) for (int i = 0; i < a.length(); i++) L.add(a.optString(i));
            for (int i = 0; i < steps.length(); i++) {
                final JSONObject s = steps.optJSONObject(i); if (s == null) continue;
                final String sid = s.optString("id");
                LinearLayout r = NUi.row(c); r.setBackground(NUi.ripple(NUi.round(NTheme.surface, 16, NTheme.line), 16)); r.setPadding(NUi.dp(14), NUi.dp(12), NUi.dp(14), NUi.dp(12));
                final NSettings.Sw sw = new NSettings.Sw(c, L.contains(sid));
                TextView t = NUi.body(c, s.optString("title"), 15, NTheme.text, 600); r.addView(t, NUi.lpw(0, -2, 1));
                if (s.optInt("min") > 0) { TextView m = NBits.meta(c, s.optInt("min") + " MIN", NTheme.muted); m.setPadding(NUi.dp(8), 0, NUi.dp(10), 0); r.addView(m); }
                r.addView(sw);
                NUi.tap(r, new View.OnClickListener() { public void onClick(View v) {
                    sw.on = !sw.on; sw.invalidate();
                    if (sw.on) L.add(sid); else L.remove(sid);
                    try {
                        JSONObject rs2 = NHabits.obj(h, "rs"); JSONArray out = new JSONArray();
                        for (int k = 0; k < steps.length(); k++) { JSONObject x = steps.optJSONObject(k); if (x != null && L.contains(x.optString("id"))) out.put(x.optString("id")); }
                        if (out.length() == 0) rs2.remove(ds); else { rs2.put(ds, out); NHabits.obj(h, "skip").remove(ds); }
                    } catch (Exception ignored) { }
                    String m = NHabits.done(h, ds) ? NActs.milestone(sh.st, h) : null; sh.save(); if (m != null) NShell.toast(m);
                } });
                b.addView(r, NUi.mt(i == 0 ? 14 : 8));
            }
        } else if (n > 1) {
            LinearLayout r = NUi.row(c); r.setGravity(android.view.Gravity.CENTER); r.setPadding(0, NUi.dp(14), 0, 0);
            final TextView val = NUi.title(c, String.valueOf(NHabits.val(h, ds)), 44); val.setGravity(android.view.Gravity.CENTER);
            final TextView of = NUi.text(c, "of " + n + " " + h.optString("unit"), 13, NTheme.muted); of.setGravity(android.view.Gravity.CENTER);
            LinearLayout mid = NUi.col(c); mid.setGravity(android.view.Gravity.CENTER); mid.addView(val); mid.addView(of);
            View.OnClickListener adj = null;
            r.addView(NUi.ibtn(c, "minus", new View.OnClickListener() { public void onClick(View v) { adjust(sh, h, ds, -1, val); } }));
            r.addView(mid, NUi.lp(NUi.dp(160), -2));
            r.addView(NUi.ibtn(c, "plus", new View.OnClickListener() { public void onClick(View v) { adjust(sh, h, ds, 1, val); } }));
            b.addView(r);
        } else {
            int v = NHabits.val(h, ds);
            TextView t = NUi.text(c, v > 0 ? "Marked done." : NHabits.skip(h, ds) ? "Rest day." : "Not done.", 14, NTheme.muted); t.setPadding(0, NUi.dp(14), 0, 0); b.addView(t);
        }
        java.util.List<View> acts = new java.util.ArrayList<>();
        acts.add(NUi.btn(c, NHabits.skip(h, ds) ? "Undo rest day" : "Rest day", false, new View.OnClickListener() { public void onClick(View v) {
            try {
                JSONObject sk = NHabits.obj(h, "skip");
                if (NHabits.skip(h, ds)) sk.remove(ds); else { sk.put(ds, 1); NHabits.setVal(h, ds, 0); sk.put(ds, 1); }
                boolean on = NHabits.skip(h, ds);
                sh.closeSheet(); sh.save(); NShell.toast(on ? "Rest day. Your streak is safe." : "Rest day removed");
            } catch (Exception ignored) { }
        } }));
        if (rt || n > 1) acts.add(NUi.btn(c, "Done", true, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } }));
        else {
            final boolean done = NHabits.val(h, ds) > 0;
            acts.add(NUi.btn(c, done ? "Mark not done" : "Mark done", true, new View.OnClickListener() { public void onClick(View v) {
                NHabits.setVal(h, ds, done ? 0 : n); sh.closeSheet(); String m = NActs.milestone(sh.st, h); sh.save(); if (m != null && !done) NShell.toast(m);
            } }));
        }
        b.addView(NForms.actions(c, acts.toArray(new View[0])));
        sh.sheet(b);
    }

    static void adjust(NShell sh, JSONObject h, String ds, int d, TextView val) {
        int v = NHabits.val(h, ds), n = NHabits.target(h), nv = Math.max(0, v + d);
        NHabits.setVal(h, ds, nv); val.setText(String.valueOf(NHabits.val(h, ds)));
        String m = nv >= n && v < n && ds.equals(NDates.ymd()) ? NActs.milestone(sh.st, h) : null;
        sh.save(); if (m != null) NShell.toast(m);
    }
}
