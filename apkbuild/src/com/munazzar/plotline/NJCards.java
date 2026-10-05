package com.munazzar.plotline;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONObject;

/* 1.13 journal cards, drawn natively: momentCard / entryCard / journalBig (flip cards with a tinted front and back),
   the small spine cards, and the thread cards (thrBig / thrFlip). Text sizes and spacing follow the web CSS. */
final class NJCards {
    private NJCards() { }

    static final String[][] JK = {{"habit", "Habit milestone"}, {"step", "Step done"}, {"goal-done", "Goal achieved"}, {"goal-new", "Goal set"}, {"note", "Moment"}, {"focus", "Focus session"}};
    static String kind(String t) { for (String[] k : JK) if (k[0].equals(t)) return k[1]; return "Moment"; }
    static String trunc(String s, int n) { return NForms.trunc(s == null ? "" : s, n); }

    /* .data: 10.5px mono, uppercase, letter-spaced */
    static TextView data(Context c, String s, int col) {
        TextView t = NUi.text(c, s.toUpperCase(), 10.5f, col); t.setTypeface(NFont.mono(500)); t.setLetterSpacing(.05f); t.setLineSpacing(0, 1.2f);
        return t;
    }

    static TextView display(Context c, String s, float sp, int col, int lines) {
        TextView t = NUi.text(c, s.toUpperCase(), sp, col); t.setTypeface(NFont.display(800)); NUi.cssLh(t, .9f);
        t.setMaxLines(lines); t.setEllipsize(android.text.TextUtils.TruncateAt.END);
        return t;
    }

    static View flipHint(Context c) {
        FrameLayout f = new FrameLayout(c);
        f.setBackground(NUi.round(0, 99, NTheme.alpha(NTheme.INK, .24f)));
        f.addView(NUi.icon(c, "flipi", 16, NTheme.INK), new FrameLayout.LayoutParams(NUi.dp(16), NUi.dp(16), Gravity.CENTER));
        f.setLayoutParams(new LinearLayout.LayoutParams(NUi.dp(36), NUi.dp(36)));
        return f;
    }

    /* .btn.inkline.sm: transparent, ink border; mobile shows only the icon when the label sits in a span */
    static View inkBtn(Context c, String icon, String label, View.OnClickListener l) {
        LinearLayout b = NUi.row(c); b.setGravity(Gravity.CENTER); b.setPadding(NUi.dp(10), 0, NUi.dp(10), 0);
        b.setBackground(NUi.ripple(NUi.round(0, 12, NTheme.alpha(NTheme.INK, .28f)), 12));
        if (icon != null) b.addView(NUi.icon(c, icon, 16, NTheme.INK), NUi.lp(NUi.dp(16), NUi.dp(16)));
        if (label != null) b.addView(NUi.body(c, label, 13.5f, NTheme.INK, 600));
        NUi.tap(b, l);
        return b;
    }

    static LinearLayout acts(Context c, View... bs) {
        LinearLayout r = NUi.row(c);
        for (int i = 0; i < bs.length; i++) { if (bs[i] == null) continue; LinearLayout.LayoutParams l = NUi.lpw(0, NUi.dp(38), 1); if (r.getChildCount() > 0) l.leftMargin = NUi.dp(6); r.addView(bs[i], l); }
        return r;
    }

    static LinearLayout face(Context c, int col, boolean big) {
        LinearLayout f = NUi.col(c); f.setBackground(NCard.bg(col, big ? 30 : 22));
        int p = NUi.dp(big ? 22 : 13); f.setPadding(p, p, p, p); f.setMinimumHeight(NUi.dp(big ? 0 : 170));
        return f;
    }

    static View grow(Context c) { View v = new View(c); v.setLayoutParams(NUi.lpw(-1, 0, 1)); return v; }

    static Bitmap bmp(String data) {
        try { int i = data.indexOf("base64,"); byte[] by = android.util.Base64.decode(data.substring(i + 7), android.util.Base64.DEFAULT); return android.graphics.BitmapFactory.decodeByteArray(by, 0, by.length); }
        catch (Exception e) { return null; }
    }

    static String when(JSONObject e) { long t = e.optLong("t"); return NDates.dayLabel(t) + " · " + NDates.fmtClock(t); }

    static int colOf(NShell sh, JSONObject e) {
        JSONObject g = sh.st.find("goals", NStore.s(e, "goalId"));
        if (g != null) return NTheme.areaCol(g.optString("area"));
        JSONObject h = habitOf(sh, e);
        return h != null ? NTheme.areaCol(h.optString("area")) : NTheme.accent;
    }

    static JSONObject habitOf(NShell sh, JSONObject e) {
        String hid = NStore.s(e, "hid"); JSONObject h = hid.isEmpty() ? null : sh.st.find("habits", hid); if (h != null) return h;
        if ("habit".equals(e.optString("type")) && !e.optString("text").isEmpty()) for (JSONObject x : NStore.list(sh.st.arr("habits"))) if (e.optString("text").endsWith("· " + x.optString("title"))) return x;
        return null;
    }

    /* a photo face: picture, dark fade at the bottom, white text over it */
    static FrameLayout photo(Context c, String img, boolean big, View... over) {
        FrameLayout f = new FrameLayout(c);
        f.setBackground(NUi.round(0xFF000000, big ? 30 : 22, 0)); f.setClipToOutline(true);
        ImageView iv = new ImageView(c); iv.setScaleType(ImageView.ScaleType.CENTER_CROP); Bitmap b = bmp(img); if (b != null) iv.setImageBitmap(b);
        f.addView(iv, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout ov = NUi.col(c); int p = NUi.dp(big ? 22 : 18);
        ov.setPadding(p, NUi.dp(70), p, p);
        ov.setBackground(new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0, 0xCC000000}));
        for (View v : over) if (v != null) ov.addView(v);
        f.addView(ov, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        f.setMinimumHeight(NUi.dp(big ? 0 : 170));
        return f;
    }

    /* web momentCard(e, big) */
    static View moment(final NShell sh, final JSONObject e, boolean big) {
        final Context c = sh.a; final JSONObject g = sh.st.find("goals", NStore.s(e, "goalId"));
        int col = g != null ? NTheme.areaCol(g.optString("area")) : NTheme.accent;
        boolean priv = e.optBoolean("private");
        String x = e.optString("text");
        final String title = priv ? "Private entry" : !e.optString("title").isEmpty() ? e.optString("title") : !x.isEmpty() ? trunc(x.split("\n")[0], 70) : "Moment";
        String wd = NDates.dayLabel(e.optLong("t"));
        View front;
        if (!NStore.s(e, "img").isEmpty()) {
            TextView d = data(c, wd + (g != null ? " · " + trunc(g.optString("title"), 28) : ""), 0xD1FFFFFF);
            TextView t = display(c, title, big ? 44 : 20, 0xFFFFFFFF, big ? 5 : 4); t.setPadding(0, NUi.dp(4), 0, 0);
            front = photo(c, NStore.s(e, "img"), big, d, t);
        } else {
            LinearLayout f = face(c, col, big);
            LinearLayout top = NUi.row(c); top.setGravity(Gravity.CENTER_VERTICAL);
            top.addView(data(c, (priv ? "🔒 " : "") + "Moment · " + wd, NTheme.INK_MUTED), NUi.lpw(0, -2, 1));
            top.addView(flipHint(c));
            f.addView(top);
            if (!NStore.s(e, "mood").isEmpty()) {
                TextView m = NUi.text(c, NStore.s(e, "mood"), 22, NTheme.INK); m.setGravity(Gravity.CENTER);
                m.setBackground(NUi.round(NTheme.alpha(NTheme.INK, .12f), 14, 0));
                LinearLayout.LayoutParams ml = NUi.lp(NUi.dp(40), NUi.dp(40)); ml.topMargin = NUi.dp(10); f.addView(m, ml);
            }
            f.addView(grow(c));
            f.addView(display(c, title, big ? 44 : 20, NTheme.INK, big ? 5 : 4));
            if (g != null) { TextView gt = data(c, trunc(g.optString("title"), 40), NTheme.INK_MUTED); gt.setPadding(0, NUi.dp(10), 0, 0); f.addView(gt); }
            front = f;
        }
        LinearLayout b = face(c, col, big);
        b.addView(data(c, wd, NTheme.INK_MUTED));
        TextView h4 = display(c, title, 22, NTheme.INK, 3); h4.setPadding(0, NUi.dp(6), 0, NUi.dp(8)); b.addView(h4);
        String note = priv ? "Private. Tap View to read it." : !x.isEmpty() && (!e.optString("title").isEmpty() || x.length() > 70) ? trunc(x, 260) : "No description yet.";
        TextView nt = NUi.text(c, note, 14, NTheme.INK_MUTED); nt.setLineSpacing(0, 1.3f); if (priv || x.isEmpty() || note.equals("No description yet.")) nt.setAlpha(.75f);
        nt.setLayoutParams(NUi.lpw(-1, 0, 1)); nt.setEllipsize(android.text.TextUtils.TruncateAt.END);
        b.addView(nt);
        if (g != null) b.addView(jg(sh, g));
        final String id = e.optString("id");
        LinearLayout a = acts(c,
            inkBtn(c, "eye", null, new View.OnClickListener() { public void onClick(View v) { NEng.viewEntry(sh, id); } }),
            inkBtn(c, "edit", null, new View.OnClickListener() { public void onClick(View v) { JSONObject en = sh.st.find("entries", id); if (en != null) new NForms(sh).entry(en); } }),
            inkBtn(c, "trash", null, new View.OnClickListener() { public void onClick(View v) { sh.run("delEntry", NMore.d("id", id)); } }));
        b.addView(a, NUi.mt(10));
        return NBits.flipWrap(c, front, b);
    }

    static View jg(final NShell sh, final JSONObject g) {
        LinearLayout r = NUi.row(sh.a); r.setGravity(Gravity.CENTER_VERTICAL); r.setAlpha(.75f); r.setPadding(0, NUi.dp(10), 0, 0);
        r.addView(NUi.body(sh.a, trunc(g.optString("title"), 40), 13, NTheme.INK, 600));
        LinearLayout.LayoutParams il = NUi.lp(NUi.dp(14), NUi.dp(14)); il.leftMargin = NUi.dp(6); r.addView(NUi.icon(sh.a, "next", 14, NTheme.INK), il);
        NUi.tap(r, new View.OnClickListener() { public void onClick(View v) { sh.route("goal/" + g.optString("id")); } });
        return r;
    }

    static View kv(Context c, String k, String v) {
        LinearLayout r = NUi.row(c); r.setPadding(0, NUi.dp(8), 0, 0);
        r.addView(NUi.text(c, k, 13, NTheme.INK_MUTED), NUi.lpw(0, -2, 1));
        r.addView(NUi.text(c, v, 13, NTheme.INK));
        LinearLayout w = NUi.col(c); View line = new View(c); line.setBackgroundColor(NTheme.alpha(NTheme.INK, .14f)); w.addView(line, NUi.lp(-1, 1)); w.addView(r);
        LinearLayout.LayoutParams l = NUi.mt(8); w.setLayoutParams(l);
        return w;
    }

    /* web entryCard(e, big): steps, focus, goal set / achieved, habit milestones */
    static View entry(final NShell sh, final JSONObject e, boolean big) {
        final Context c = sh.a; final JSONObject g = sh.st.find("goals", NStore.s(e, "goalId")); final JSONObject h = habitOf(sh, e);
        String ty = e.optString("type", "note"), kind = kind(ty), wh = when(e);
        int col = g != null ? NTheme.areaCol(g.optString("area")) : h != null ? NTheme.areaCol(h.optString("area")) : NTheme.accent;
        LinearLayout f = face(c, col, big);
        LinearLayout top = NUi.row(c); top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(data(c, kind + " · " + wh, NTheme.INK_MUTED), NUi.lpw(0, -2, 1)); top.addView(flipHint(c));
        f.addView(top); f.addView(grow(c));
        f.addView(display(c, trunc(e.optString("text", kind).isEmpty() ? kind : e.optString("text"), 90), big ? 44 : 20, NTheme.INK, big ? 5 : 4));
        if (g != null && !ty.startsWith("goal-")) { TextView gt = data(c, trunc(g.optString("title"), 40), NTheme.INK_MUTED); gt.setPadding(0, NUi.dp(10), 0, 0); f.addView(gt); }
        LinearLayout b = face(c, col, big);
        b.addView(data(c, kind + " · " + wh, NTheme.INK_MUTED));
        TextView h4 = display(c, trunc(e.optString("text", kind).isEmpty() ? kind : e.optString("text"), 70), 22, NTheme.INK, 3); h4.setPadding(0, NUi.dp(6), 0, NUi.dp(8)); b.addView(h4);
        if (ty.equals("step") || ty.equals("focus")) {
            if (g != null) b.addView(kv(c, "Goal", trunc(g.optString("title"), 30) + " · " + NActs.pct(sh.st, g) + "%"));
            JSONObject s = null; if (g != null && !NStore.s(e, "sid").isEmpty()) { org.json.JSONArray ss = g.optJSONArray("steps"); for (int i = 0; ss != null && i < ss.length(); i++) if (NStore.s(e, "sid").equals(ss.optJSONObject(i).optString("id"))) s = ss.optJSONObject(i); }
            if (s != null) b.addView(kv(c, "Step", (s.optBoolean("done") ? "Done" : "Open") + (s.optInt("focus") > 0 ? " · " + s.optInt("focus") + " min focus" : "")));
        } else if (ty.startsWith("goal-")) {
            if (g != null) {
                int dn = NActs.doneSteps(g), n = g.optJSONArray("steps") == null ? 0 : g.optJSONArray("steps").length();
                b.addView(kv(c, "Progress", "done".equals(g.optString("status")) ? "Achieved" : NActs.pct(sh.st, g) + "%"));
                b.addView(kv(c, "Steps", dn + " of " + n));
                JSONObject nx = NActs.nextStep(g); if (nx != null) b.addView(kv(c, "Next", trunc(nx.optString("title"), 28)));
            } else { TextView nt = NUi.text(c, "This goal was deleted.", 14, NTheme.INK_MUTED); b.addView(nt); }
        } else if (ty.equals("habit") && h != null) {
            b.addView(kv(c, "Habit", NHabits.icon(h) + " " + trunc(h.optString("title"), 26)));
            if (!NHabits.kind(h).equals("quit")) b.addView(kv(c, "Streak now", NHabits.streak(h) + (NHabits.freq(h).equals("times") ? " wk" : " d")));
            else b.addView(kv(c, "Free for", NHabits.cleanDays(h) + " d"));
        }
        b.addView(grow(c));
        final String id = e.optString("id");
        LinearLayout a = acts(c,
            inkBtn(c, null, "View", new View.OnClickListener() { public void onClick(View v) { NEng.viewEntry(sh, id); } }),
            g == null ? null : inkBtn(c, null, "Open goal", new View.OnClickListener() { public void onClick(View v) { sh.route("goal/" + g.optString("id")); } }),
            h == null ? null : inkBtn(c, null, "Open habit", new View.OnClickListener() { public void onClick(View v) { sh.route("habit/" + h.optString("id")); } }));
        b.addView(a, NUi.mt(6));
        return NBits.flipWrap(c, f, b);
    }

    /* web journalBig: the big card in the side-by-side view */
    static View big(final NShell sh, final JSONObject e) {
        String ty = e.optString("type", "note");
        if (ty.equals("note")) return moment(sh, e, true);
        if (NStore.s(e, "img").isEmpty()) return entry(sh, e, true);
        Context c = sh.a; JSONObject g = sh.st.find("goals", NStore.s(e, "goalId"));
        TextView d = data(c, kind(ty) + " · " + when(e), 0xC7FFFFFF);
        TextView q = null; if (!e.optString("text").isEmpty()) { q = NUi.text(c, e.optString("text"), 21, 0xFFFFFFFF); q.setLineSpacing(0, 1.38f); q.setPadding(0, NUi.dp(8), 0, 0); }
        return photo(c, NStore.s(e, "img"), true, d, q, g == null ? null : jg(sh, g));
    }

    /* web journalCard(e, false): the smaller card on the timeline */
    static View small(NShell sh, JSONObject e) { return "note".equals(e.optString("type", "note")) ? moment(sh, e, false) : entry(sh, e, false); }
    /* ---- threads (web thrBig / thrFlip) ---- */
    static String[] tag(String k) { String[][] T = {{"note", "💭", "Thought"}, {"idea", "💡", "Idea"}, {"prog", "🚧", "Progress"}, {"block", "⛔", "Blocked"}, {"done", "✅", "Done"}}; for (String[] t : T) if (t[0].equals(k)) return t; return T[0]; }

    static java.util.List<JSONObject> ups(JSONObject t) {
        java.util.List<JSONObject> l = new java.util.ArrayList<>(); org.json.JSONArray a = t.optJSONArray("ups");
        for (int i = 0; a != null && i < a.length(); i++) { JSONObject u = a.optJSONObject(i); if (u != null && !NStore.isDel(u) && u.optInt("del") == 0) l.add(u); }
        java.util.Collections.sort(l, new java.util.Comparator<JSONObject>() { public int compare(JSONObject x, JSONObject y) { return Long.compare(y.optLong("t"), x.optLong("t")); } });
        return l;
    }

    /* thrLink: what a thread is linked to (emoji, name, colour) */
    static String[] link(NShell sh, JSONObject t) {
        JSONObject l = t.optJSONObject("link"); if (l == null) return null;
        String k = l.optString("k"), id = l.optString("id");
        if (k.equals("goal")) { JSONObject g = sh.st.find("goals", id); return g != null ? new String[]{"🎯", g.optString("title"), "goal/" + id, String.valueOf(NTheme.areaCol(g.optString("area")))} : new String[]{"🎯", "Goal removed", "", ""}; }
        if (k.equals("habit")) { JSONObject h = sh.st.find("habits", id); return h != null ? new String[]{NHabits.icon(h), h.optString("title"), "habit/" + id, String.valueOf(NTheme.areaCol(h.optString("area")))} : new String[]{"✅", "Habit removed", "", ""}; }
        if (k.equals("entry")) { JSONObject e = sh.st.find("entries", id); return e != null ? new String[]{"📓", !e.optString("title").isEmpty() ? e.optString("title") : trunc(e.optString("text", "Journal entry").split("\n")[0], 40), "", ""} : new String[]{"📓", "Entry removed", "", ""}; }
        return null;
    }

    static int thrCol(NShell sh, JSONObject t) { String[] l = link(sh, t); return l != null && !l[3].isEmpty() ? Integer.parseInt(l[3]) : NTheme.accent; }

    static View thrBig(final NShell sh, final JSONObject t) {
        final Context c = sh.a; java.util.List<JSONObject> U = ups(t); JSONObject u = U.isEmpty() ? null : U.get(0); boolean done = "done".equals(t.optString("status"));
        LinearLayout f = face(c, thrCol(sh, t), true); if (done) f.setAlpha(.82f);
        LinearLayout jh = NUi.row(c); jh.setGravity(Gravity.CENTER_VERTICAL);
        String[] tg = u == null ? null : tag(u.optString("k"));
        jh.addView(data(c, (done ? "✅ Done" : tg != null ? tg[1] + " " + tg[2] : "🧵 New") + " · " + U.size() + " update" + (U.size() == 1 ? "" : "s"), NTheme.INK_MUTED), NUi.lpw(0, -2, 1));
        if (!done) { View x = NUi.icon(c, "plus", 16, NTheme.INK); x.setAlpha(.55f); x.setPadding(NUi.dp(2), NUi.dp(2), NUi.dp(2), NUi.dp(2)); NUi.tap(x, new View.OnClickListener() { public void onClick(View v) { new NForms(sh).threadAdd(t); } }); jh.addView(x, NUi.lp(NUi.dp(22), NUi.dp(22))); }
        f.addView(jh);
        TextView ag = data(c, NDates.ago(NJournalPage.last(t)), NTheme.INK_MUTED); ag.setPadding(0, NUi.dp(4), 0, 0); f.addView(ag);
        f.addView(grow(c));
        f.addView(display(c, t.optString("title"), 44, NTheme.INK, 4));
        TextView q = NUi.text(c, u != null ? trunc(u.optString("x"), 180) : "Nothing logged yet", 19, NTheme.INK); q.setLineSpacing(0, 1.3f); q.setMaxLines(4); q.setEllipsize(android.text.TextUtils.TruncateAt.END); q.setPadding(0, NUi.dp(10), 0, 0); if (u == null) q.setAlpha(.65f);
        f.addView(q);
        String[] lk = link(sh, t);
        if (lk != null) { TextView jg = NUi.body(c, lk[0] + " " + trunc(lk[1], 34), 13, NTheme.INK, 600); jg.setAlpha(.72f); jg.setPadding(0, NUi.dp(14), 0, 0); f.addView(jg); }
        NUi.tap(f, new View.OnClickListener() { public void onClick(View v) { sh.route("thread/" + t.optString("id")); } });
        return f;
    }

    static View thrFlip(final NShell sh, final JSONObject t) {
        final Context c = sh.a; java.util.List<JSONObject> U = ups(t); JSONObject u = U.isEmpty() ? null : U.get(0); int col = thrCol(sh, t);
        String wh = NDates.ago(NJournalPage.last(t)); String[] tg = u == null ? null : tag(u.optString("k")); String[] lk = link(sh, t);
        LinearLayout f = face(c, col, false);
        LinearLayout top = NUi.row(c); top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(data(c, (tg != null ? tg[1] + " " : "") + "Thread · " + wh, NTheme.INK_MUTED), NUi.lpw(0, -2, 1)); top.addView(flipHint(c));
        f.addView(top); f.addView(grow(c));
        f.addView(display(c, t.optString("title"), 20, NTheme.INK, 4));
        TextView d = data(c, U.size() + " update" + (U.size() == 1 ? "" : "s") + (lk != null ? " · " + trunc(lk[1], 28) : ""), NTheme.INK_MUTED); d.setPadding(0, NUi.dp(10), 0, 0); f.addView(d);
        LinearLayout b = face(c, col, false);
        b.addView(data(c, wh, NTheme.INK_MUTED));
        TextView h4 = display(c, t.optString("title"), 22, NTheme.INK, 3); h4.setPadding(0, NUi.dp(6), 0, NUi.dp(8)); b.addView(h4);
        if (U.isEmpty()) { TextView n = NUi.text(c, "Nothing logged yet.", 14, NTheme.INK_MUTED); n.setAlpha(.7f); b.addView(n); }
        for (int i = 0; i < Math.min(3, U.size()); i++) { TextView n = NUi.text(c, tag(U.get(i).optString("k"))[1] + " " + trunc(U.get(i).optString("x"), 90), 14, NTheme.INK_MUTED); n.setPadding(0, 0, 0, NUi.dp(4)); b.addView(n); }
        b.addView(grow(c));
        final String id = t.optString("id"); boolean done = "done".equals(t.optString("status"));
        b.addView(acts(c, inkBtn(c, "eye", null, new View.OnClickListener() { public void onClick(View v) { sh.route("thread/" + id); } }),
            done ? null : inkBtn(c, "plus", null, new View.OnClickListener() { public void onClick(View v) { new NForms(sh).threadAdd(t); } })), NUi.mt(10));
        return NBits.flipWrap(c, f, b);
    }

    /* .thr-ub/.thr-uc[data-k=done|block]: a 3px coloured line along the top edge */
    static void topLine(View v, String k) {
        final int col = "done".equals(k) ? 0xFF4FAF7A : "block".equals(k) ? 0xFFD9534F : 0;
        if (col == 0) return;
        v.setForeground(new android.graphics.drawable.Drawable() {
            final android.graphics.Paint p = new android.graphics.Paint();
            @Override public void draw(android.graphics.Canvas cv) { p.setColor(col); cv.drawRect(getBounds().left, getBounds().top, getBounds().right, getBounds().top + NUi.dp(3), p); }
            @Override public void setAlpha(int a) { } @Override public void setColorFilter(android.graphics.ColorFilter f) { }
            @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
        });
        v.setClipToOutline(true);
    }

    static View editX(Context c, int col, float alpha, View.OnClickListener l) {
        android.widget.ImageView x = new android.widget.ImageView(c); x.setImageDrawable(new NIcon("edit", col)); x.setAlpha(alpha);
        x.setPadding(NUi.dp(2), NUi.dp(2), NUi.dp(2), NUi.dp(2)); x.setContentDescription("Edit update");
        NUi.tap(x, l); x.setLayoutParams(NUi.lp(NUi.dp(20), NUi.dp(20)));
        return x;
    }

    /* web updBig: one update as a big tinted card */
    static View updBig(NShell sh, JSONObject t, JSONObject u, View.OnClickListener edit) {
        Context c = sh.a; String[] g = tag(u.optString("k"));
        LinearLayout f = face(c, thrCol(sh, t), true);
        LinearLayout jh = NUi.row(c); jh.setGravity(Gravity.CENTER_VERTICAL);
        jh.addView(data(c, g[1] + " " + g[2] + " · " + NDates.fmtClock(u.optLong("t")), NTheme.INK_MUTED), NUi.lpw(0, -2, 1));
        jh.addView(editX(c, NTheme.INK, .55f, edit));
        f.addView(jh);
        TextView d = data(c, NDates.dayLabel(u.optLong("t")), NTheme.INK_MUTED); d.setPadding(0, NUi.dp(4), 0, 0); f.addView(d);
        android.widget.ScrollView sc = new android.widget.ScrollView(c); sc.setVerticalScrollBarEnabled(false); sc.setOverScrollMode(View.OVER_SCROLL_NEVER);
        TextView x = NUi.body(c, u.optString("x"), 17, NTheme.INK, 500); x.setLineSpacing(0, 1.5f);
        sc.addView(x);
        LinearLayout.LayoutParams sl = NUi.lpw(-1, 0, 1); sl.topMargin = NUi.dp(14); f.addView(sc, sl);
        topLine(f, g[0]);
        return f;
    }

    /* web updCard: one update on the timeline (.jc with the colour along the top) */
    static View updCard(NShell sh, JSONObject t, JSONObject u, View.OnClickListener edit) {
        Context c = sh.a; String[] g = tag(u.optString("k")); int col = thrCol(sh, t);
        LinearLayout f = NUi.col(c); f.setBackground(jcBg(col)); f.setPadding(NUi.dp(13), NUi.dp(12) + NUi.dp(3), NUi.dp(13), NUi.dp(12));
        LinearLayout jh = NUi.row(c); jh.setGravity(Gravity.CENTER_VERTICAL);
        jh.addView(data(c, g[1] + " " + g[2] + " · " + NDates.fmtClock(u.optLong("t")), NTheme.muted), NUi.lpw(0, -2, 1));
        jh.addView(editX(c, NTheme.dim, 1, edit));
        f.addView(jh);
        TextView x = NUi.text(c, u.optString("x"), 13.5f, NTheme.text); x.setLineSpacing(0, 1.45f); x.setPadding(0, NUi.dp(6), 0, 0);
        f.addView(x);
        topLine(f, g[0]);
        return f;
    }

    /* .jc: surface, 1px line, radius 20, a 3px top border in the card colour */
    static android.graphics.drawable.Drawable jcBg(int col) {
        android.graphics.drawable.GradientDrawable base = NUi.round(NTheme.surface, 20, NTheme.line);
        android.graphics.drawable.GradientDrawable top = NUi.round(col, 20, 0);
        android.graphics.drawable.LayerDrawable ld = new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{top, base});
        ld.setLayerInset(1, 0, NUi.dp(3), 0, 0);
        return ld;
    }
}
