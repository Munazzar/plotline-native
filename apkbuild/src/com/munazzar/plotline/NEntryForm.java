package com.munazzar.plotline;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/* The journal "moment" writer, matching the web writeSheet: siblings for the day, title, mood, B/I/U/highlight toolbar
   (stored as the same html the web shows), Prompt, date (not future), goal, photo, private, autosave. */
final class NEntryForm {
    static final int HILITE = 0x66FFD54F;
    final NShell sh; final Context c; final NForms F;
    JSONObject e; String gid, day; String photo = ""; boolean priv; String mood = "";
    EditText title, ed; TextView stat;
    final Handler hd = new Handler(Looper.getMainLooper());
    final Runnable saver = new Runnable() { public void run() { persist(); } };
    boolean closed;

    NEntryForm(NShell sh, JSONObject e, String gid, String date) {
        this.sh = sh; this.c = sh.a; this.F = new NForms(sh); this.e = e;
        this.gid = e != null ? NStore.s(e, "goalId") : (gid == null ? "" : gid);
        this.day = e != null ? NDates.ymd(e.optLong("t")) : (date == null || !NDates.valid(date) ? NDates.ymd() : date);
        if (e != null) { photo = NStore.s(e, "img"); priv = e.optBoolean("private"); mood = NStore.s(e, "mood"); }
    }

    static void open(NShell sh, JSONObject e, String gid, String date) { new NEntryForm(sh, e, gid, date).show(); }

    /* ---- html <-> spans ---- */
    static CharSequence fromHtml(JSONObject e) {
        String h = e.optString("html");
        if (h.isEmpty()) return e.optString("text");
        h = h.replaceAll("(?i)<mark>", "<span style=\"background-color:#FFD54F\">").replaceAll("(?i)</mark>", "</span>");
        Spanned s = Html.fromHtml(h, Html.FROM_HTML_MODE_LEGACY);
        int n = s.length(); while (n > 0 && s.charAt(n - 1) == '\n') n--;
        return s.subSequence(0, n);
    }

    static String toHtml(Editable t) {
        StringBuilder sb = new StringBuilder(); int n = t.length(), i = 0;
        while (i < n) {
            int j = t.nextSpanTransition(i, n, Object.class); if (j <= i) j = n;
            boolean b = false, it = false, u = false, m = false;
            for (Object o : t.getSpans(i, j, Object.class)) {
                if (o instanceof StyleSpan) { int st = ((StyleSpan) o).getStyle(); if ((st & Typeface.BOLD) != 0) b = true; if ((st & Typeface.ITALIC) != 0) it = true; }
                else if (o instanceof UnderlineSpan) u = true; else if (o instanceof BackgroundColorSpan) m = true;
            }
            String seg = android.text.TextUtils.htmlEncode(t.subSequence(i, j).toString()).replace("\n", "<br>");
            if (m) sb.append("<mark>"); if (u) sb.append("<u>"); if (it) sb.append("<i>"); if (b) sb.append("<b>");
            sb.append(seg);
            if (b) sb.append("</b>"); if (it) sb.append("</i>"); if (u) sb.append("</u>"); if (m) sb.append("</mark>");
            i = j;
        }
        return sb.toString();
    }

    void soon() { if (stat != null) stat.setText("Saving…"); hd.removeCallbacks(saver); hd.postDelayed(saver, 650); }

    /* writes the entry exactly like the web wrSave; quiet so the page behind isn't rebuilt while writing */
    void persist() {
        hd.removeCallbacks(saver);
        if (ed == null) return;
        try {
            String text = ed.getText().toString().trim(), t = title.getText().toString().trim(), html = toHtml(ed.getText());
            if (e == null && text.isEmpty() && t.isEmpty() && photo.isEmpty()) { if (stat != null) stat.setText("New entry · starts saving as you write"); return; }
            long when = whenFor(e == null ? System.currentTimeMillis() : e.optLong("t"));
            if (e == null) {
                e = new JSONObject(); e.put("id", NStore.uid()); e.put("t", when); e.put("type", "note"); e.put("sid", JSONObject.NULL);
                sh.st.arr("entries").put(e);
            } else if (!NDates.ymd(e.optLong("t")).equals(day)) e.put("t", when);
            e.put("title", t); e.put("text", text); e.put("html", html); e.put("mood", mood); e.put("private", priv);
            e.put("goalId", gid.isEmpty() ? JSONObject.NULL : gid); e.put("img", photo.isEmpty() ? JSONObject.NULL : photo);
            sh.saveQuiet();
            if (stat != null) stat.setText("Saved");
        } catch (Exception ex) { NCrash.log(c, "entry autosave", ex); }
    }

    long whenFor(long prev) {
        try {
            java.util.Calendar b = java.util.Calendar.getInstance(); b.setTimeInMillis(prev);
            java.util.Calendar k = NDates.cal(day); k.set(java.util.Calendar.HOUR_OF_DAY, b.get(java.util.Calendar.HOUR_OF_DAY)); k.set(java.util.Calendar.MINUTE, b.get(java.util.Calendar.MINUTE));
            return k.getTimeInMillis();
        } catch (Exception ex) { return prev; }
    }

    void finish() { closed = true; persist(); sh.closeSheet(); sh.save(); }

    void reopen(final JSONObject x, final String d) {
        persist(); sh.closeSheet(); sh.save();
        hd.postDelayed(new Runnable() { public void run() { open(sh, x, null, d); } }, 280);
    }

    void fmt(final int kind) {
        int a = ed.getSelectionStart(), z = ed.getSelectionEnd();
        if (a < 0 || z < 0) return; if (a > z) { int q = a; a = z; z = q; }
        if (a == z) { NShell.toast("Select some text first"); return; }
        Editable t = ed.getText();
        if (kind == 4) { for (Object o : t.getSpans(a, z, Object.class)) if (o instanceof StyleSpan || o instanceof UnderlineSpan || o instanceof BackgroundColorSpan) t.removeSpan(o); soon(); return; }
        if (kind == 5) { /* bullet list: prefix each selected line */
            int ls = t.toString().lastIndexOf('\n', a - 1) + 1; String seg = t.subSequence(ls, z).toString(); String[] lines = seg.split("\n", -1); StringBuilder o = new StringBuilder();
            for (int i = 0; i < lines.length; i++) { if (i > 0) o.append('\n'); o.append(lines[i].startsWith("• ") ? lines[i] : "• " + lines[i]); }
            t.replace(ls, z, o.toString()); soon(); return;
        }
        Class<?> cl = kind == 3 ? BackgroundColorSpan.class : kind == 2 ? UnderlineSpan.class : StyleSpan.class;
        int sty = kind == 0 ? Typeface.BOLD : Typeface.ITALIC; boolean has = false;
        for (Object o : t.getSpans(a, z, cl)) { if (kind <= 1 && !(((StyleSpan) o).getStyle() == sty)) continue; int s0 = t.getSpanStart(o), s1 = t.getSpanEnd(o); if (s0 <= a && s1 >= z) has = true; }
        if (has) { for (Object o : t.getSpans(a, z, cl)) { if (kind <= 1 && ((StyleSpan) o).getStyle() != sty) continue; int s0 = t.getSpanStart(o), s1 = t.getSpanEnd(o); t.removeSpan(o); if (s0 < a) t.setSpan(clone(o, kind, sty), s0, a, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); if (s1 > z) t.setSpan(clone(o, kind, sty), z, s1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); } }
        else t.setSpan(clone(null, kind, sty), a, z, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        soon();
    }

    static Object clone(Object o, int kind, int sty) { return kind == 3 ? new BackgroundColorSpan(HILITE) : kind == 2 ? new UnderlineSpan() : new StyleSpan(sty); }

    TextView tool(String label, int style, final Runnable r) {
        TextView t = NUi.body(c, label, 15, NTheme.text, 700);
        if (style != 0) t.setTypeface(t.getTypeface(), style);
        t.setGravity(Gravity.CENTER); t.setBackground(NUi.ripple(NUi.round(NTheme.surface, 10, NTheme.line2), 10));
        t.setPadding(NUi.dp(14), 0, NUi.dp(14), 0);
        NUi.tap(t, new View.OnClickListener() { public void onClick(View v) { r.run(); } });
        return t;
    }

    void show() {
        final boolean ed0 = e != null;
        LinearLayout b = F.sheetBody("Journal", ed0 ? "Edit moment" : "Write a moment", null);
        /* top: status + same-day entries */
        stat = NUi.body(c, ed0 ? "Saved" : "New entry · starts saving as you write", 12, NTheme.muted, 600); b.addView(stat);
        HorizontalScrollView hs = new HorizontalScrollView(c); hs.setHorizontalScrollBarEnabled(false);
        LinearLayout pr = NUi.row(c); hs.addView(pr);
        List<JSONObject> sib = new ArrayList<>(); JSONArray all = sh.st.arr("entries");
        for (int i = 0; i < all.length(); i++) { JSONObject x = all.optJSONObject(i); if (x != null && "note".equals(x.optString("type", "note")) && NDates.ymd(x.optLong("t")).equals(day)) sib.add(x); }
        java.util.Collections.sort(sib, new java.util.Comparator<JSONObject>() { public int compare(JSONObject p, JSONObject q) { return Long.compare(p.optLong("t"), q.optLong("t")); } });
        for (int i = 0; i < sib.size(); i++) {
            final JSONObject x = sib.get(i); String t0 = x.optString("title"); if (t0.isEmpty()) t0 = "Entry " + (i + 1); if (t0.length() > 18) t0 = t0.substring(0, 17) + "…";
            String m0 = NStore.s(x, "mood");
            LinearLayout.LayoutParams l = NUi.lp(-2, -2); l.rightMargin = NUi.dp(8);
            pr.addView(NUi.chip(c, (m0.isEmpty() ? "" : m0 + " ") + t0, e != null && x.optString("id").equals(e.optString("id")), new View.OnClickListener() { public void onClick(View v) { if (e == null || !x.optString("id").equals(e.optString("id"))) reopen(x, null); } }), l);
        }
        pr.addView(NUi.chip(c, "+ New", false, new View.OnClickListener() { public void onClick(View v) { reopen(null, day); } }));
        b.addView(hs, NUi.mt(8));
        title = NForms.input(c, "Title · e.g. Morning gratitude, Late-night vent", ed0 ? e.optString("title") : "", false);
        b.addView(title, NUi.mt(12));
        title.addTextChangedListener(new TextWatcher() { public void beforeTextChanged(CharSequence s, int a, int b2, int c2) { } public void onTextChanged(CharSequence s, int a, int b2, int c2) { } public void afterTextChanged(Editable s) { soon(); } });
        /* mood */
        final String[] mo = {mood};
        b.addView(F.fieldLabel(c, "Mood"));
        b.addView(NForms.choice(c, new String[][]{{"", "None"}, {"😊", "😊 Calm"}, {"😄", "😄 Energized"}, {"🙏", "🙏 Grateful"}, {"😐", "😐 Okay"}, {"😔", "😔 Low"}, {"😟", "😟 Anxious"}, {"😡", "😡 Frustrated"}, {"😴", "😴 Tired"}}, mo, new Runnable() { public void run() { mood = mo[0]; soon(); } }));
        /* toolbar */
        HorizontalScrollView th = new HorizontalScrollView(c); th.setHorizontalScrollBarEnabled(false);
        LinearLayout tb = NUi.row(c); th.addView(tb);
        final int[] ks = {0, 1, 2, 3, 5, 4}; String[] ls = {"B", "I", "U", "H", "•", "Tx"}; int[] sy = {Typeface.BOLD, Typeface.ITALIC, 0, 0, 0, 0};
        for (int i = 0; i < ks.length; i++) { final int k = ks[i]; LinearLayout.LayoutParams l = NUi.lp(NUi.dp(46), NUi.dp(40)); l.rightMargin = NUi.dp(8);
            TextView t = tool(ls[i], sy[i], new Runnable() { public void run() { fmt(k); } }); t.setPadding(0, 0, 0, 0); tb.addView(t, l); }
        LinearLayout.LayoutParams pl = NUi.lp(-2, NUi.dp(40)); pl.leftMargin = NUi.dp(6);
        tb.addView(tool("✨ Prompt", 0, new Runnable() { public void run() { prompt(); } }), pl);
        b.addView(th, NUi.mt(14));
        ed = NForms.input(c, "Write freely. Bold it, underline it, highlight the parts that matter.", "", true); ed.setMinLines(6);
        if (ed0) ed.setText(fromHtml(e));
        b.addView(ed, NUi.mt(10));
        NVoice.attach(c, ed);
        ed.addTextChangedListener(new TextWatcher() { public void beforeTextChanged(CharSequence s, int a, int b2, int c2) { } public void onTextChanged(CharSequence s, int a, int b2, int c2) { } public void afterTextChanged(Editable s) { soon(); } });
        /* date + goal */
        b.addView(F.fieldLabel(c, "Date"));
        final String[] dt = {day};
        final TextView dv = NForms.datePick(c, dt, "Pick a date");
        b.addView(dv);
        dv.setOnClickListener(null);
        NUi.tap(dv, new View.OnClickListener() { public void onClick(View v) {
            java.util.Calendar k = NDates.valid(dt[0]) ? NDates.cal(dt[0]) : java.util.Calendar.getInstance();
            android.app.DatePickerDialog dlg = new android.app.DatePickerDialog(c, new android.app.DatePickerDialog.OnDateSetListener() { public void onDateSet(android.widget.DatePicker p, int y, int m, int d) {
                java.util.Calendar x = java.util.Calendar.getInstance(); x.clear(); x.set(y, m, d); String n = NDates.ymd(x); if (n.compareTo(NDates.ymd()) > 0) n = NDates.ymd(); day = n; dt[0] = n; dv.setText(NDates.longDate(n) + "   ✕"); soon();
            } }, k.get(java.util.Calendar.YEAR), k.get(java.util.Calendar.MONTH), k.get(java.util.Calendar.DAY_OF_MONTH));
            dlg.getDatePicker().setMaxDate(System.currentTimeMillis()); dlg.show();
        } });
        dv.setOnLongClickListener(null);
        b.addView(F.fieldLabel(c, "Goal · optional"));
        List<String[]> go = new ArrayList<>(); go.add(new String[]{"", "None"});
        for (JSONObject g : NActs.goals(sh.st, null)) go.add(new String[]{g.optString("id"), g.optString("title")});
        final String[] gs = {gid};
        b.addView(NForms.choice(c, go.toArray(new String[0][]), gs, new Runnable() { public void run() { gid = gs[0]; soon(); } }));
        /* photo + private */
        final LinearLayout pc = NUi.col(c); b.addView(pc, NUi.mt(14));
        final Runnable[] paint = new Runnable[1];
        paint[0] = new Runnable() { public void run() {
            pc.removeAllViews(); LinearLayout row = NUi.row(c); row.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(NUi.btn(c, photo.isEmpty() ? "📷 Add a photo" : "📷 Change photo", false, new View.OnClickListener() { public void onClick(View v) {
                sh.a.imgCb = new MainActivity.ImgCb() { public void got(String url) { photo = url; soon(); paint[0].run(); } };
                sh.a.pickImage("goalcover");
            } }));
            if (!photo.isEmpty()) { LinearLayout.LayoutParams l = NUi.lp(-2, -2); l.leftMargin = NUi.dp(8); row.addView(NUi.btn(c, "Remove", false, new View.OnClickListener() { public void onClick(View v) { photo = ""; soon(); paint[0].run(); } }), l); }
            View sp = new View(c); row.addView(sp, new LinearLayout.LayoutParams(0, 1, 1));
            row.addView(NUi.body(c, "🔒 Private", 14, NTheme.muted, 600));
            final NSettings.Sw sw = new NSettings.Sw(c, priv); LinearLayout.LayoutParams sl = NUi.lp(NUi.dp(48), NUi.dp(30)); sl.leftMargin = NUi.dp(8);
            sw.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { priv = !priv; sw.on = priv; sw.invalidate(); soon(); } });
            row.addView(sw, sl); pc.addView(row);
            if (!photo.isEmpty()) {
                try {
                    int i = photo.indexOf("base64,"); byte[] by = android.util.Base64.decode(photo.substring(i + 7), android.util.Base64.DEFAULT);
                    android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeByteArray(by, 0, by.length);
                    ImageView iv = new ImageView(c); iv.setImageBitmap(bm); iv.setScaleType(ImageView.ScaleType.CENTER_CROP); iv.setClipToOutline(true);
                    iv.setOutlineProvider(new android.view.ViewOutlineProvider() { public void getOutline(View v, android.graphics.Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), NUi.dp(16)); } });
                    LinearLayout.LayoutParams il = NUi.lp(-1, NUi.dp(160)); il.topMargin = NUi.dp(10); pc.addView(iv, il);
                } catch (Exception ignored) { }
            }
        } };
        paint[0].run();
        List<View> acts = new ArrayList<>();
        if (ed0) acts.add(NUi.btn(c, "Delete", false, new View.OnClickListener() { public void onClick(View v) { closed = true; hd.removeCallbacks(saver); sh.st.remove("entries", e.optString("id")); sh.closeSheet(); sh.save(); NShell.toast("Moment deleted"); } }));
        acts.add(NUi.btn(c, "Done", true, new View.OnClickListener() { public void onClick(View v) { finish(); } }));
        b.addView(NForms.actions(c, acts.toArray(new View[0])));
        sh.sheet(b);
        if (!ed0) NForms.focus(ed);
    }

    void prompt() {
        sh.a.jsRet("(window.__nprompt?window.__nprompt():'')", new android.webkit.ValueCallback<String>() { public void onReceiveValue(String v) {
            String p = NSheets.unq(v); if (p == null || p.isEmpty() || ed == null) return;
            Editable t = ed.getText(); int st = t.length();
            if (!t.toString().trim().isEmpty() && !t.toString().endsWith("\n")) t.append("\n\n");
            st = t.length(); t.append(p).append("\n\n");
            t.setSpan(new StyleSpan(Typeface.BOLD), st, st + p.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ed.setSelection(t.length()); ed.requestFocus(); soon();
        } });
    }
}
