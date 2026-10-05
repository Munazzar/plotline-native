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

    /* web writeSheet: status + same-day pills, a big display-font title on a line, mood tiles, a toolbar box,
       the editor, date + goal, photo + private, Delete / Done */
    void show() {
        final boolean ed0 = e != null;
        LinearLayout b = NUi.col(c);
        /* top: status + same-day entries + New */
        LinearLayout top = NUi.col(c);
        stat = NJCards.data(c, ed0 ? "Saved" : "New entry · starts saving as you write", NTheme.muted); top.addView(stat);
        HorizontalScrollView hs = new HorizontalScrollView(c); hs.setHorizontalScrollBarEnabled(false);
        LinearLayout pr = NUi.row(c); hs.addView(pr);
        List<JSONObject> sib = new ArrayList<>(); JSONArray all = sh.st.arr("entries");
        for (int i = 0; i < all.length(); i++) { JSONObject x = all.optJSONObject(i); if (x != null && "note".equals(x.optString("type", "note")) && NDates.ymd(x.optLong("t")).equals(day)) sib.add(x); }
        java.util.Collections.sort(sib, new java.util.Comparator<JSONObject>() { public int compare(JSONObject p, JSONObject q) { return Long.compare(p.optLong("t"), q.optLong("t")); } });
        for (int i = 0; i < sib.size(); i++) {
            final JSONObject x = sib.get(i); String t0 = x.optString("title"); if (t0.isEmpty()) t0 = "Entry " + (i + 1); if (t0.length() > 18) t0 = t0.substring(0, 17) + "…";
            String m0 = NStore.s(x, "mood"); boolean on = e != null && x.optString("id").equals(e.optString("id"));
            TextView pl = NUi.body(c, (m0.isEmpty() ? "" : m0 + " ") + t0, 12.5f, on ? NTheme.bg : NTheme.text, 600); pl.setGravity(Gravity.CENTER); pl.setPadding(NUi.dp(12), 0, NUi.dp(12), 0);
            pl.setBackground(NUi.ripple(on ? NUi.round(NTheme.text, 99, 0) : NUi.round(NTheme.surface, 99, NTheme.line2), 99));
            NUi.tap(pl, new View.OnClickListener() { public void onClick(View v) { if (e == null || !x.optString("id").equals(e.optString("id"))) reopen(x, null); } });
            LinearLayout.LayoutParams l = NUi.lp(-2, NUi.dp(32)); l.rightMargin = NUi.dp(6); pr.addView(pl, l);
        }
        TextView add = NUi.body(c, "New", 12.5f, NTheme.accent, 600); add.setGravity(Gravity.CENTER); add.setPadding(NUi.dp(12), 0, NUi.dp(12), 0);
        add.setCompoundDrawablesRelative(NUi.iconD("plus", 14, NTheme.accent), null, null, null); add.setCompoundDrawablePadding(NUi.dp(4));
        add.setBackground(NUi.ripple(NUi.dashed(NTheme.surface, 99, NTheme.line2, 1), 99));
        NUi.tap(add, new View.OnClickListener() { public void onClick(View v) { reopen(null, day); } });
        pr.addView(add, NUi.lp(-2, NUi.dp(32)));
        top.addView(hs, NUi.mt(8));
        b.addView(top);
        /* title: display font, uppercase, a line under it */
        title = new EditText(c); title.setText(ed0 ? e.optString("title") : "");
        title.setHint("TITLE · E.G. MORNING GRATITUDE, LATE-NIGHT VENT"); title.setHintTextColor(NTheme.muted); title.setTextColor(NTheme.text);
        title.setTypeface(NFont.display(800)); title.setTextSize(26); title.setSingleLine(true);
        title.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(90)});
        title.setAllCaps(true);   /* shown in capitals like the web's text-transform; stored as typed */
        title.setPadding(NUi.dp(2), NUi.dp(6), NUi.dp(2), NUi.dp(6));
        title.setBackground(new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{new android.graphics.drawable.ColorDrawable(NTheme.line)}) { { setLayerHeight(0, Math.max(1, NUi.dp(1))); setLayerGravity(0, Gravity.BOTTOM); } });
        b.addView(title, NUi.mt(12));
        title.addTextChangedListener(new TextWatcher() { public void beforeTextChanged(CharSequence s, int a, int b2, int c2) { } public void onTextChanged(CharSequence s, int a, int b2, int c2) { } public void afterTextChanged(Editable s) { soon(); } });
        /* mood tiles (tap again to clear) */
        final String[][] MO = {{"😊", "Calm"}, {"😄", "Energized"}, {"🙏", "Grateful"}, {"😐", "Okay"}, {"😔", "Low"}, {"😟", "Anxious"}, {"😡", "Frustrated"}, {"😴", "Tired"}};
        HorizontalScrollView mh = new HorizontalScrollView(c); mh.setHorizontalScrollBarEnabled(false);
        final LinearLayout mr = NUi.row(c); mh.addView(mr);
        final List<LinearLayout> tiles = new ArrayList<>();
        final Runnable paintM = new Runnable() { public void run() {
            for (int i = 0; i < tiles.size(); i++) { boolean on = MO[i][0].equals(mood); LinearLayout t = tiles.get(i);
                t.setBackground(NUi.ripple(NUi.round(on ? NUi.mix(NTheme.accent, .14f, NTheme.surface) : NTheme.surface, 16, on ? NTheme.accent : NTheme.line), 16));
                ((TextView) t.getChildAt(0)).setScaleX(on ? 1.18f : 1f); ((TextView) t.getChildAt(0)).setScaleY(on ? 1.18f : 1f);
                ((TextView) t.getChildAt(1)).setTextColor(on ? NTheme.text : NTheme.muted); }
        } };
        for (final String[] m : MO) {
            LinearLayout t = NUi.col(c); t.setGravity(Gravity.CENTER_HORIZONTAL); t.setPadding(0, NUi.dp(8), 0, NUi.dp(6));
            TextView em = NUi.text(c, m[0], 22, NTheme.text); em.setIncludeFontPadding(false); t.addView(em);
            TextView lb = NUi.body(c, m[1], 10.5f, NTheme.muted, 600); lb.setPadding(0, NUi.dp(4), 0, 0); t.addView(lb);
            NUi.tap(t, new View.OnClickListener() { public void onClick(View v) { mood = mood.equals(m[0]) ? "" : m[0]; paintM.run(); NUi.haptic(v); soon(); } });
            LinearLayout.LayoutParams l = NUi.lp(NUi.dp(62), -2); l.rightMargin = NUi.dp(6); mr.addView(t, l); tiles.add(t);
        }
        paintM.run();
        b.addView(mh, NUi.mt(12));
        /* toolbar box */
        LinearLayout tb = NUi.row(c); tb.setGravity(Gravity.CENTER_VERTICAL); tb.setPadding(NUi.dp(4), NUi.dp(4), NUi.dp(4), NUi.dp(4));
        tb.setBackground(NUi.round(NTheme.surface, 14, NTheme.line));
        final int[] ks = {0, 1, 2, 3, 5, 4};
        for (int i = 0; i < ks.length; i++) {
            final int k = ks[i]; View t;
            if (k == 5) { android.widget.FrameLayout f = new android.widget.FrameLayout(c); f.addView(NUi.icon(c, "menu", 17, NTheme.text), new android.widget.FrameLayout.LayoutParams(NUi.dp(17), NUi.dp(17), Gravity.CENTER)); t = f; }
            else {
                TextView tv = NUi.body(c, k == 0 ? "B" : k == 1 ? "I" : k == 2 ? "U" : k == 3 ? "H" : "Tx", 15, NTheme.text, k == 0 || k == 3 ? 800 : 600); tv.setGravity(Gravity.CENTER);
                if (k == 1) tv.setTypeface(tv.getTypeface(), Typeface.ITALIC);
                if (k == 2) tv.setPaintFlags(tv.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
                if (k == 3) { android.text.SpannableString hs2 = new android.text.SpannableString(" H "); hs2.setSpan(new BackgroundColorSpan(0xFFFFE36E), 0, 3, 0); hs2.setSpan(new android.text.style.ForegroundColorSpan(0xFF1B1300), 0, 3, 0); tv.setText(hs2); }
                t = tv;
            }
            t.setBackground(NUi.ripple(NUi.round(0, 10, 0), 10));
            NUi.tap(t, new View.OnClickListener() { public void onClick(View v) { fmt(k); } });
            tb.addView(t, NUi.lp(NUi.dp(38), NUi.dp(36)));
        }
        tb.addView(new View(c), NUi.lpw(0, 1, 1));
        TextView pr2 = NUi.body(c, "Prompt", 13.5f, NTheme.accent, 600); pr2.setGravity(Gravity.CENTER); pr2.setPadding(NUi.dp(8), 0, NUi.dp(8), 0);
        pr2.setCompoundDrawablesRelative(NUi.iconD("ai", 17, NTheme.accent), null, null, null); pr2.setCompoundDrawablePadding(NUi.dp(6));
        pr2.setBackground(NUi.ripple(NUi.round(0, 10, 0), 10));
        NUi.tap(pr2, new View.OnClickListener() { public void onClick(View v) { prompt(); } });
        tb.addView(pr2, NUi.lp(-2, NUi.dp(36)));
        b.addView(tb, NUi.mt(12));
        /* editor: 34% of the screen tall, accent border while writing */
        ed = NForms.input(c, "Write freely. Bold it, underline it, highlight the parts that matter.", "", true);
        ed.setTextSize(16.5f); ed.setLineSpacing(0, 1.4f); ed.setGravity(Gravity.TOP | Gravity.START);
        ed.setMinHeight(Math.round(c.getResources().getDisplayMetrics().heightPixels * .34f));
        ed.setPadding(NUi.dp(16), NUi.dp(14), NUi.dp(16), NUi.dp(14));
        final EditText edf = ed;
        final Runnable edBg = new Runnable() { public void run() { edf.setBackground(NUi.round(NTheme.surface, 16, edf.hasFocus() ? NTheme.accent : NTheme.line2)); } };
        edBg.run();
        ed.setOnFocusChangeListener(new View.OnFocusChangeListener() { public void onFocusChange(View v, boolean f) { edBg.run(); } });
        if (ed0) ed.setText(fromHtml(e));
        b.addView(ed, NUi.mt(12));
        NVoice.attach(c, ed);
        ed.addTextChangedListener(new TextWatcher() { public void beforeTextChanged(CharSequence s, int a, int b2, int c2) { } public void onTextChanged(CharSequence s, int a, int b2, int c2) { } public void afterTextChanged(Editable s) { soon(); } });
        /* date + goal */
        b.addView(F.fieldLabel(c, "Date"));
        final String[] dt = {day};
        final TextView dv = NForms.datePick(c, dt, "mm/dd/yyyy");
        b.addView(dv);
        dv.setOnClickListener(null);
        NUi.tap(dv, new View.OnClickListener() { public void onClick(View v) {
            java.util.Calendar k = NDates.valid(dt[0]) ? NDates.cal(dt[0]) : java.util.Calendar.getInstance();
            android.app.DatePickerDialog dlg = new android.app.DatePickerDialog(c, new android.app.DatePickerDialog.OnDateSetListener() { public void onDateSet(android.widget.DatePicker p, int y, int m, int d) {
                java.util.Calendar x = java.util.Calendar.getInstance(); x.clear(); x.set(y, m, d); String n = NDates.ymd(x); if (n.compareTo(NDates.ymd()) > 0) n = NDates.ymd(); day = n; dt[0] = n;
                dv.setText(new java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.US).format(NDates.cal(n).getTime())); soon();
            } }, k.get(java.util.Calendar.YEAR), k.get(java.util.Calendar.MONTH), k.get(java.util.Calendar.DAY_OF_MONTH));
            dlg.getDatePicker().setMaxDate(System.currentTimeMillis()); dlg.show();
        } });
        dv.setOnLongClickListener(null);
        b.addView(F.fieldLabel(c, "Goal · optional"));
        List<String[]> go = new ArrayList<>(); go.add(new String[]{"", "None"});
        for (JSONObject g : NGoalsPage.treeOrder(NStore.list(sh.st.arr("goals")))) go.add(new String[]{g.optString("id"), NForms.trunc(g.optString("title"), 40)});
        final String[] gs = {gid};
        b.addView(NForms.select(c, go.toArray(new String[0][]), gs, new Runnable() { public void run() { gid = gs[0]; soon(); } }));
        /* photo + private */
        final LinearLayout pc = NUi.col(c); b.addView(pc, NUi.mt(12));
        final Runnable[] paint = new Runnable[1];
        paint[0] = new Runnable() { public void run() {
            pc.removeAllViews(); LinearLayout row = NUi.row(c); row.setGravity(Gravity.CENTER_VERTICAL);
            TextView ph = NUi.btnSm(c, photo.isEmpty() ? "Add a photo" : "Change photo", false, new View.OnClickListener() { public void onClick(View v) {
                sh.a.imgCb = new MainActivity.ImgCb() { public void got(String url) { photo = url; soon(); paint[0].run(); } };
                sh.a.pickImage("goalcover");
            } });
            ph.setCompoundDrawablesRelative(NUi.iconD("camera", 16, NTheme.text), null, null, null); ph.setCompoundDrawablePadding(NUi.dp(8));
            row.addView(ph);
            if (!photo.isEmpty()) { LinearLayout.LayoutParams l = NUi.lp(-2, -2); l.leftMargin = NUi.dp(8); TextView rm = NUi.btnSm(c, "Remove", false, new View.OnClickListener() { public void onClick(View v) { photo = ""; soon(); paint[0].run(); } }); rm.setTextColor(NTheme.LATE); rm.setBackground(NUi.ripple(NUi.round(0, 12, 0), 12)); row.addView(rm, l); }
            View sp = new View(c); row.addView(sp, new LinearLayout.LayoutParams(0, 1, 1));
            TextView pv = NUi.body(c, "Private", 15, NTheme.text, 500); pv.setCompoundDrawablesRelative(NUi.iconD("lock", 15, NTheme.text), null, null, null); pv.setCompoundDrawablePadding(NUi.dp(6));
            row.addView(pv);
            final NSettings.Sw sw = new NSettings.Sw(c, priv); LinearLayout.LayoutParams sl = NUi.lp(NUi.dp(48), NUi.dp(30)); sl.leftMargin = NUi.dp(10);
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
        /* actions: Delete on the left (when editing), Done on the right */
        LinearLayout acts = NUi.row(c); acts.setGravity(Gravity.CENTER_VERTICAL);
        if (ed0) { TextView del = NUi.btn(c, "Delete", false, new View.OnClickListener() { public void onClick(View v) { closed = true; hd.removeCallbacks(saver); sh.st.remove("entries", e.optString("id")); sh.closeSheet(); sh.save(); NShell.toast("Moment deleted"); } });
            del.setTextColor(NTheme.LATE); del.setBackground(NUi.ripple(NUi.round(0, 14, 0), 14)); del.setCompoundDrawablesRelative(NUi.iconD("trash", 17, NTheme.LATE), null, null, null); del.setCompoundDrawablePadding(NUi.dp(8)); acts.addView(del); }
        acts.addView(new View(c), NUi.lpw(0, 1, 1));
        acts.addView(NUi.btn(c, "Done", true, new View.OnClickListener() { public void onClick(View v) { finish(); } }));
        b.addView(acts, NUi.mt(20));
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
