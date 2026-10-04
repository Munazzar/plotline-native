package com.munazzar.plotline;

import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/* The web's full goal form: area, horizon, parent, priority, start/target, first steps (+pace) or shifting steps,
   check-in reminder, cover image, pin. */
final class NGoalForm {
    static final String[][] HZ = {{"week", "This week"}, {"month", "This month"}, {"quarter", "3 months"}, {"year", "This year"}, {"multi", "1–5 years"}, {"life", "Lifetime"}};

    static List<String[]> parentList(NStore st, JSONObject me) {
        List<JSONObject> all = NStore.list(st.arr("goals")), pool = new ArrayList<>();
        Set<String> bad = new HashSet<>();
        if (me != null) { bad.add(me.optString("id")); for (JSONObject d : NRoad.tree(st, me)) bad.add(d.optString("id")); }
        for (JSONObject g : all) if (!bad.contains(g.optString("id"))) pool.add(g);
        Set<String> ids = new HashSet<>(); for (JSONObject g : pool) ids.add(g.optString("id"));
        List<String[]> out = new ArrayList<>(); Set<String> seen = new HashSet<>();
        for (JSONObject g : pool) if (!ids.contains(g.optString("parent"))) walk(g, pool, 0, out, seen);
        for (JSONObject g : pool) walk(g, pool, 0, out, seen);
        return out;
    }
    static void walk(JSONObject g, List<JSONObject> pool, int d, List<String[]> out, Set<String> seen) {
        if (!seen.add(g.optString("id"))) return;
        StringBuilder sb = new StringBuilder(); for (int i = 0; i < d; i++) sb.append("    "); if (d > 0) sb.append("↳ ");
        String t = g.optString("title"); if (t.length() > 48) t = t.substring(0, 47) + "…";
        out.add(new String[]{g.optString("id"), sb + t});
        for (JSONObject x : pool) if (g.optString("id").equals(x.optString("parent"))) walk(x, pool, d + 1, out, seen);
    }

    static void open(final NShell sh, final JSONObject g, final String parentIn) {
        final Context c = sh.a; final NForms F = new NForms(sh); final boolean edit = g != null;
        LinearLayout b = F.sheetBody("Goal", edit ? "Edit goal" : "New goal", edit ? null : "Name what you want, and why it matters. Add steps after.");
        b.addView(NForms.fieldLabel(c, "What do you want to achieve?"));
        final EditText title = NForms.input(c, "Run a 10K race", edit ? g.optString("title") : "", false); b.addView(title);
        NVoice.attach(c, title);
        b.addView(NForms.fieldLabel(c, "Why it matters"));
        final EditText why = NForms.input(c, "Your future self will read this on hard days.", edit ? g.optString("why") : "", true); why.setMinLines(2); b.addView(why);
        NVoice.attach(c, why);
        /* area: type any, or tap one */
        b.addView(NForms.fieldLabel(c, "Life area"));
        final EditText area = NForms.input(c, "Health, Home, Community…", NTheme.areaName(edit ? g.optString("area") : "personal"), false); b.addView(area);
        List<String[]> as = new ArrayList<>(); Set<String> seen = new HashSet<>();
        for (int i = 0; i < NGen.AREA_ID.length; i++) { as.add(new String[]{NGen.AREA_NAME[i], NGen.AREA_NAME[i]}); seen.add(NGen.AREA_ID[i]); }
        for (JSONObject x : NStore.list(st(sh).arr("goals"))) { String k = NTheme.areaKey(x.optString("area")); if (seen.add(k)) as.add(new String[]{NTheme.areaName(k), NTheme.areaName(k)}); }
        for (JSONObject x : NStore.list(st(sh).arr("habits"))) { String k = NTheme.areaKey(x.optString("area")); if (!k.isEmpty() && seen.add(k)) as.add(new String[]{NTheme.areaName(k), NTheme.areaName(k)}); }
        android.widget.HorizontalScrollView ah = new android.widget.HorizontalScrollView(c); ah.setHorizontalScrollBarEnabled(false);
        LinearLayout ar = NUi.row(c); ah.addView(ar);
        for (final String[] o : as) { LinearLayout.LayoutParams l = NUi.lp(-2, -2); l.rightMargin = NUi.dp(8); ar.addView(NUi.chip(c, o[1], false, new View.OnClickListener() { public void onClick(View v) { area.setText(o[0]); area.setSelection(o[0].length()); } }), l); }
        b.addView(ah, NUi.mt(8));
        b.addView(NForms.fieldLabel(c, "Horizon"));
        final String[] hz = {edit ? g.optString("horizon", "month") : "month"};
        b.addView(NForms.choice(c, HZ, hz, null));
        /* parent */
        final String[] par = {edit ? g.optString("parent") : parentIn == null ? "" : parentIn};
        final List<String[]> pl = parentList(st(sh), g);
        if (!pl.isEmpty()) {
            b.addView(NForms.fieldLabel(c, "Part of a bigger goal · optional"));
            final TextView pv = NUi.body(c, "", 15, NTheme.text, 600);
            pv.setBackground(NUi.ripple(NUi.round(NTheme.surface, 14, NTheme.line2), 14)); pv.setPadding(NUi.dp(16), NUi.dp(13), NUi.dp(16), NUi.dp(13)); pv.setSingleLine(true); pv.setEllipsize(android.text.TextUtils.TruncateAt.END);
            final Runnable paint = new Runnable() { public void run() {
                String t = "None · a main goal"; for (String[] o : pl) if (o[0].equals(par[0])) t = o[1].trim();
                pv.setText(t + "   ▾");
            } };
            paint.run();
            NUi.tap(pv, new View.OnClickListener() { public void onClick(View v) {
                final String[] items = new String[pl.size() + 1]; items[0] = "None · a main goal"; for (int i = 0; i < pl.size(); i++) items[i + 1] = pl.get(i)[1];
                new android.app.AlertDialog.Builder(c).setItems(items, new android.content.DialogInterface.OnClickListener() { public void onClick(android.content.DialogInterface d, int i) { par[0] = i == 0 ? "" : pl.get(i - 1)[0]; paint.run(); } }).show();
            } });
            b.addView(pv);
        }
        b.addView(NForms.fieldLabel(c, "Priority"));
        final String[] pri = {edit ? String.valueOf(g.optInt("priority", 2)) : "2"};
        b.addView(NForms.choice(c, new String[][]{{"1", "High"}, {"2", "Medium"}, {"3", "Low"}}, pri, null));
        b.addView(NForms.fieldLabel(c, "Start date"));
        final String[] start = {edit && NDates.valid(g.optString("startDate")) ? g.optString("startDate") : NDates.ymd()};
        b.addView(NForms.datePick(c, start, "Pick a date"));
        b.addView(NForms.fieldLabel(c, "Target date"));
        final String[] target = {edit ? g.optString("targetDate") : ""};
        b.addView(NForms.datePick(c, target, "No target date (long-press to clear)"));
        final NSettings.Sw shift = new NSettings.Sw(c, true);
        final EditText steps; final String[] pace = {"7"};
        if (edit) {
            LinearLayout r = NUi.row(c); r.setPadding(0, NUi.dp(18), 0, 0);
            LinearLayout tx = NUi.col(c);
            tx.addView(NUi.body(c, "Move steps with the start date", 15, NTheme.text, 600));
            TextView sub = NUi.text(c, "Open steps and the target date shift by the same number of days", 12.5f, NTheme.muted); sub.setLineSpacing(0, 1.2f); tx.addView(sub);
            r.addView(tx, NUi.lpw(0, -2, 1)); r.addView(shift);
            NUi.tap(r, new View.OnClickListener() { public void onClick(View v) { shift.on = !shift.on; shift.invalidate(); } });
            b.addView(r); steps = null;
        } else {
            b.addView(NForms.fieldLabel(c, "First steps · one per line"));
            steps = NForms.input(c, "Get fitted for running shoes\nRun 20 minutes, 3 times this week", "", true); steps.setMinLines(3); b.addView(steps);
            b.addView(NForms.fieldLabel(c, "Space those steps"));
            b.addView(NForms.choice(c, new String[][]{{"0", "No dates yet"}, {"1", "One per day"}, {"3", "Every 3 days"}, {"7", "Every week"}, {"14", "Every 2 weeks"}, {"30", "Every month"}}, pace, null));
        }
        /* check-in */
        b.addView(NForms.fieldLabel(c, "Check-in reminder"));
        final String[] ci = {edit ? g.optString("checkin") : ""}; final String[] cday = {edit ? String.valueOf(g.optInt("checkinDay", 1)) : "1"};
        final String[] ctime = {edit && !g.optString("checkinTime").isEmpty() ? g.optString("checkinTime") : "19:00"};
        final LinearLayout weekly = NUi.col(c);
        weekly.addView(NForms.fieldLabel(c, "On"));
        String[][] dd = new String[7][2]; for (int i = 0; i < 7; i++) { dd[i][0] = String.valueOf(i); dd[i][1] = NDates.DAYS[i]; }
        weekly.addView(NForms.choice(c, dd, cday, null));
        weekly.setVisibility(ci[0].equals("weekly") ? View.VISIBLE : View.GONE);
        b.addView(NForms.choice(c, new String[][]{{"", "Off"}, {"daily", "Every day"}, {"weekly", "Every week"}}, ci, new Runnable() { public void run() { weekly.setVisibility(ci[0].equals("weekly") ? View.VISIBLE : View.GONE); } }));
        b.addView(NForms.fieldLabel(c, "At"));
        b.addView(NForms.timePick(c, ctime, "19:00"));
        b.addView(weekly);
        /* cover */
        b.addView(NForms.fieldLabel(c, "Cover image · optional"));
        final String[] cover = {edit ? g.optString("img") : ""};
        final LinearLayout cr = NUi.col(c); b.addView(cr);
        final Runnable[] cpaint = new Runnable[1];
        cpaint[0] = new Runnable() { public void run() {
            cr.removeAllViews();
            LinearLayout row = NUi.row(c);
            row.addView(NUi.btn(c, cover[0].isEmpty() ? "Add a cover image" : "Change cover", false, new View.OnClickListener() { public void onClick(View v) {
                sh.a.imgCb = new MainActivity.ImgCb() { public void got(String url) { cover[0] = url; cpaint[0].run(); } };
                sh.a.pickImage("goalcover");
            } }));
            if (!cover[0].isEmpty()) { LinearLayout.LayoutParams l = NUi.lp(-2, -2); l.leftMargin = NUi.dp(10); row.addView(NUi.btn(c, "Remove", false, new View.OnClickListener() { public void onClick(View v) { cover[0] = ""; cpaint[0].run(); } }), l); }
            cr.addView(row);
            if (!cover[0].isEmpty()) {
                try {
                    int i = cover[0].indexOf("base64,"); byte[] by = android.util.Base64.decode(cover[0].substring(i + 7), android.util.Base64.DEFAULT);
                    android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeByteArray(by, 0, by.length);
                    ImageView iv = new ImageView(c); iv.setImageBitmap(bm); iv.setScaleType(ImageView.ScaleType.CENTER_CROP); iv.setClipToOutline(true);
                    iv.setOutlineProvider(new android.view.ViewOutlineProvider() { public void getOutline(View v, android.graphics.Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), NUi.dp(16)); } });
                    cr.addView(iv, NUi.lp(-1, NUi.dp(130)) );
                    ((LinearLayout.LayoutParams) iv.getLayoutParams()).topMargin = NUi.dp(10);
                } catch (Exception ignored) { }
            }
        } };
        cpaint[0].run();
        /* pin */
        final NSettings.Sw pin = new NSettings.Sw(c, edit && g.optBoolean("pinned"));
        LinearLayout pr = NUi.row(c); pr.setPadding(0, NUi.dp(18), 0, 0);
        pr.addView(NUi.body(c, "Pin to Today", 15, NTheme.text, 600), NUi.lpw(0, -2, 1)); pr.addView(pin);
        NUi.tap(pr, new View.OnClickListener() { public void onClick(View v) { pin.on = !pin.on; pin.invalidate(); } });
        b.addView(pr);
        View cancel = NUi.btn(c, "Cancel", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } });
        View save = NUi.btn(c, edit ? "Save changes" : "Create goal", true, new View.OnClickListener() { public void onClick(View v) {
            String t = title.getText().toString().trim();
            if (t.isEmpty()) { title.setError("Give it a name"); return; }
            try {
                String ak = NTheme.areaKey(area.getText().toString().trim()); if (ak.isEmpty()) ak = "personal";
                String sd = NDates.valid(start[0]) ? start[0] : NDates.ymd(), td = NDates.valid(target[0]) ? target[0] : "";
                if (!ci[0].isEmpty()) sh.a.askNotifications();
                if (edit) {
                    int delta = NDates.dnum(sd) - NDates.dnum(NDates.valid(g.optString("startDate")) ? g.optString("startDate") : sd); int moved = 0;
                    String otd = g.optString("targetDate");
                    if (delta != 0 && shift.on) {
                        JSONArray sa = g.optJSONArray("steps");
                        for (int i = 0; sa != null && i < sa.length(); i++) { JSONObject s = sa.optJSONObject(i); if (s != null && !s.optBoolean("done") && NDates.valid(s.optString("due"))) { s.put("due", NDates.fromN(NDates.dnum(s.optString("due")) + delta)); s.put("reminded", false); moved++; } }
                        if (!td.isEmpty() && td.equals(otd)) td = NDates.fromN(NDates.dnum(td) + delta);
                    }
                    int cd = Integer.parseInt(cday[0]);
                    if (!ci[0].equals(g.optString("checkin")) || !ctime[0].equals(g.optString("checkinTime")) || cd != g.optInt("checkinDay", 1)) g.put("lastCheckin", NDates.ymd());
                    g.put("title", t); g.put("why", why.getText().toString().trim()); g.put("area", ak); g.put("horizon", hz[0]); g.put("priority", Integer.parseInt(pri[0]));
                    g.put("startDate", sd); g.put("targetDate", td); g.put("pinned", pin.on); g.put("checkin", ci[0]); g.put("checkinTime", ctime[0] == null || ctime[0].isEmpty() ? "19:00" : ctime[0]); g.put("checkinDay", cd);
                    g.put("img", cover[0]); if (!pl.isEmpty()) g.put("parent", par[0]);
                    sh.closeSheet(); sh.save();
                    NShell.toast(moved > 0 ? "Saved · " + moved + " step" + (moved > 1 ? "s" : "") + " moved " + Math.abs(delta) + " day" + (Math.abs(delta) > 1 ? "s" : "") + (delta > 0 ? " later" : " earlier") : "Saved");
                } else {
                    JSONObject o = NForms.newGoal(t);
                    o.put("why", why.getText().toString().trim()); o.put("area", ak); o.put("horizon", hz[0]); o.put("priority", Integer.parseInt(pri[0]));
                    o.put("startDate", sd); o.put("targetDate", td); o.put("pinned", pin.on); o.put("checkin", ci[0]); o.put("checkinTime", ctime[0] == null || ctime[0].isEmpty() ? "19:00" : ctime[0]); o.put("checkinDay", Integer.parseInt(cday[0]));
                    o.put("img", cover[0]); o.put("parent", par[0] == null ? "" : par[0]);
                    int pc = Integer.parseInt(pace[0]); int i = 0; JSONArray sa = o.getJSONArray("steps");
                    for (String line : steps.getText().toString().split("\n")) { String s = line.trim(); if (s.isEmpty()) continue; i++; sa.put(NForms.newStep(s, pc > 0 ? NDates.fromN(NDates.dnum(sd) + pc * i) : "", "")); }
                    sh.st.arr("goals").put(o); NActs.log(sh.st, "goal-new", o.optString("id"), o.optString("title"), null);
                    sh.closeSheet(); sh.save(); sh.push(new NGoalScreen(sh, o.optString("id"))); NShell.toast("Goal created");
                }
            } catch (Exception e) { NCrash.log(c, "goal save", e); }
        } });
        b.addView(NForms.actions(c, cancel, save));
        sh.sheet(b);
        if (!edit) NForms.focus(title);
    }

    static NStore st(NShell sh) { return sh.st; }
}
