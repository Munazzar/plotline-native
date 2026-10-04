package com.munazzar.plotline;

import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/* Chapters: named seasons of your life, shown as bands behind the Vista timeline. List + edit sheets. */
final class NChapters {
    static final String[] COLS = {"#46C99B", "#6B9BFF", "#AC93FF", "#FF8C6B", "#F0C862", "#F585B8", "#A9D14A", "#5ED0E6"};

    static List<JSONObject> all(NStore st) {
        List<JSONObject> l = NStore.list(st.arr("chapters"));
        Collections.sort(l, new Comparator<JSONObject>() { public int compare(JSONObject a, JSONObject b) { return a.optString("start").compareTo(b.optString("start")); } });
        return l;
    }

    static void list(final NShell sh) {
        final NForms F = new NForms(sh); final android.content.Context c = sh.a;
        LinearLayout b = F.sheetBody("Chapters", "Your chapters", null);
        List<JSONObject> L = all(sh.st);
        if (L.isEmpty()) {
            TextView t = NUi.text(c, "No chapters yet. Name a season of your life and it appears on Vista.", 14, NTheme.muted); t.setLineSpacing(0, 1.2f); t.setPadding(0, NUi.dp(10), 0, 0); b.addView(t);
        }
        for (final JSONObject ch : L) {
            LinearLayout r = NUi.row(c); r.setBackground(NUi.ripple(NUi.round(NTheme.surface, 16, NTheme.line), 16)); r.setPadding(NUi.dp(14), NUi.dp(12), NUi.dp(14), NUi.dp(12));
            int col = NUi.col(ch.optString("color"), NTheme.accent);
            View dot = new View(c); dot.setBackground(NUi.round(col, 5, 0)); r.addView(dot, NUi.lp(NUi.dp(10), NUi.dp(10)));
            LinearLayout tx = NUi.col(c); tx.setPadding(NUi.dp(12), 0, 0, 0);
            TextView t = NUi.body(c, (ch.optString("emoji").isEmpty() ? "" : ch.optString("emoji") + " ") + ch.optString("title"), 15, NTheme.text, 600); t.setSingleLine(true); tx.addView(t);
            tx.addView(NUi.label(c, (NDates.fmtDate(ch.optString("start")) + " – " + (ch.optString("end").isEmpty() ? "now" : NDates.fmtDate(ch.optString("end")))).toUpperCase(), NTheme.muted));
            r.addView(tx, NUi.lpw(0, -2, 1));
            r.addView(new android.widget.ImageView(c) {{ setImageDrawable(new NIcon("edit", NTheme.muted)); }}, NUi.lp(NUi.dp(18), NUi.dp(18)));
            NUi.tap(r, new View.OnClickListener() { public void onClick(View v) { form(sh, ch); } });
            b.addView(r, NUi.mt(8));
        }
        b.addView(NForms.actions(c, NUi.btn(c, "Done", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } }),
            NUi.btn(c, "+  New chapter", true, new View.OnClickListener() { public void onClick(View v) { form(sh, null); } })));
        sh.sheet(b);
    }

    static String oneEmoji(String s) {
        s = s == null ? "" : s.trim(); if (s.isEmpty()) return "";
        java.text.BreakIterator it = java.text.BreakIterator.getCharacterInstance(); it.setText(s); int e = it.next();
        return e == java.text.BreakIterator.DONE ? s : s.substring(0, e);
    }

    static void form(final NShell sh, final JSONObject ch) {
        final NForms F = new NForms(sh); final android.content.Context c = sh.a; final boolean edit = ch != null;
        LinearLayout b = F.sheetBody("Chapters", edit ? "Edit chapter" : "New chapter", "A season of your life, like “New job” or “Training for my first marathon”. It shows as a band on Vista and frames your year in review.");
        b.addView(NForms.fieldLabel(c, "Name"));
        final EditText title = NForms.input(c, "New job", edit ? ch.optString("title") : "", false); b.addView(title);
        b.addView(NForms.fieldLabel(c, "Emoji · optional"));
        final EditText emo = NForms.input(c, "Type any emoji", edit ? ch.optString("emoji") : "", false); b.addView(emo);
        b.addView(NForms.fieldLabel(c, "From"));
        final String[] st = {edit ? ch.optString("start") : NDates.ymd()};
        b.addView(NForms.datePick(c, st, "Pick a date"));
        b.addView(NForms.fieldLabel(c, "Until · empty if it’s still going"));
        final String[] en = {edit ? ch.optString("end") : ""};
        b.addView(NForms.datePick(c, en, "Still going (long-press to clear)"));
        b.addView(NForms.fieldLabel(c, "Colour"));
        final String[] col = {edit && !ch.optString("color").isEmpty() ? ch.optString("color") : COLS[sh.st.arr("chapters").length() % COLS.length]};
        final LinearLayout sw = NUi.row(c);
        final Runnable[] paint = new Runnable[1];
        paint[0] = new Runnable() { public void run() {
            sw.removeAllViews();
            for (final String k : COLS) {
                View d = new View(c); boolean on = k.equals(col[0]);
                android.graphics.drawable.GradientDrawable g = NUi.round(NUi.col(k, NTheme.accent), 14, on ? NTheme.text : 0); d.setBackground(g);
                NUi.tap(d, new View.OnClickListener() { public void onClick(View v) { col[0] = k; paint[0].run(); } });
                LinearLayout.LayoutParams l = NUi.lp(NUi.dp(28), NUi.dp(28)); l.rightMargin = NUi.dp(10); sw.addView(d, l);
            }
        } };
        paint[0].run();
        android.widget.HorizontalScrollView hs = new android.widget.HorizontalScrollView(c); hs.setHorizontalScrollBarEnabled(false); hs.addView(sw); b.addView(hs);
        List<View> acts = new ArrayList<>();
        if (edit) acts.add(NUi.btn(c, "Delete", false, new View.OnClickListener() { public void onClick(View v) { confirmDelete(sh, ch); } }));
        acts.add(NUi.btn(c, "Cancel", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } }));
        acts.add(NUi.btn(c, edit ? "Save" : "Add chapter", true, new View.OnClickListener() { public void onClick(View v) {
            String t = title.getText().toString().trim();
            if (t.isEmpty()) { title.setError("Name the chapter"); return; }
            if (!NDates.valid(st[0])) { NShell.toast("Pick a start date"); return; }
            if (NDates.valid(en[0]) && en[0].compareTo(st[0]) < 0) { NShell.toast("The end is before the start"); return; }
            try {
                JSONObject o = edit ? ch : new JSONObject();
                if (!edit) o.put("id", NStore.uid());
                o.put("title", t); o.put("emoji", oneEmoji(emo.getText().toString())); o.put("start", st[0]); o.put("end", NDates.valid(en[0]) ? en[0] : ""); o.put("color", col[0]); o.put("u", System.currentTimeMillis());
                if (!edit) sh.st.arr("chapters").put(o);
                sh.closeSheet(); sh.save(); NShell.toast(edit ? "Chapter saved" : "Chapter added");
            } catch (Exception e) { NCrash.log(c, "chapter save", e); }
        } }));
        b.addView(NForms.actions(c, acts.toArray(new View[0])));
        sh.sheet(b);
        if (!edit) NForms.focus(title);
    }

    static void confirmDelete(final NShell sh, final JSONObject ch) {
        NForms F = new NForms(sh);
        LinearLayout b = F.sheetBody(ch.optString("title"), "Delete this chapter?", "Your goals, habits and journal stay exactly as they are.");
        b.addView(NForms.actions(sh.a, NUi.btn(sh.a, "Cancel", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } }),
            NUi.btn(sh.a, "Delete", true, new View.OnClickListener() { public void onClick(View v) {
                sh.st.remove("chapters", ch.optString("id")); sh.closeSheet(); sh.save(); NShell.toast("Chapter deleted"); } })));
        sh.sheet(b);
    }
}
