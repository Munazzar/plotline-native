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

/* One thread: add an update at the top, the log below (latest first). */
final class NThreadScreen extends NPage {
    final String id;
    final String[] tag = {"note"};
    NThreadScreen(NShell sh, String id) { super(sh); this.id = id; back = true; }

    /* web vThread: back · edit · done/reopen · gear; the big title, its line and link; the composer; then the
       updates, latest first, as cards side by side or on the timeline */
    @Override void build() {
        final JSONObject t = st.find("threads", id);
        if (t == null) { header("Not found", gear()); add(NUi.btn(c, "Back to threads", false, new View.OnClickListener() { public void onClick(View v) { sh.pop(); sh.route("threads"); } }), 16); return; }
        final NForms F = new NForms(sh);
        final boolean done = "done".equals(t.optString("status"));
        header(null, NUi.ibtn(c, "edit", new View.OnClickListener() { public void onClick(View v) { F.thread(t); } }),
            NUi.ibtn(c, done ? "rep" : "check", new View.OnClickListener() { public void onClick(View v) {
                try { t.put("status", done ? "open" : "done"); t.put("u", System.currentTimeMillis()); } catch (Exception ignored) { }
                sh.save(); if (!done) NShell.toast("Thread marked done");
            } }), gear());
        final List<JSONObject> U = NJCards.ups(t);
        TextView h1 = NUi.title(c, t.optString("title"), Math.max(30, Math.min(58, c.getResources().getConfiguration().screenWidthDp * .054f)));
        h1.setMaxLines(6); h1.setLineSpacing(0, .95f);
        add(h1, 18);
        long cr = t.optLong("created", t.optLong("u"));
        java.util.Calendar cal = java.util.Calendar.getInstance(); cal.setTimeInMillis(cr);
        String started = NDates.MONTHS[cal.get(java.util.Calendar.MONTH)].substring(0, 3) + " " + cal.get(java.util.Calendar.DAY_OF_MONTH) + ", " + cal.get(java.util.Calendar.YEAR);
        TextView meta = NJCards.data(c, "Thread · started " + started + " · " + U.size() + " update" + (U.size() == 1 ? "" : "s") + (done ? " · done" : ""), NTheme.muted);
        add(meta, 6);
        final String[] lk = NJCards.link(sh, t);
        if (lk != null) {
            final boolean gone = lk[1].endsWith(" removed");
            LinearLayout b = NUi.row(c); b.setGravity(Gravity.CENTER_VERTICAL); b.setPadding(NUi.dp(14), NUi.dp(7), NUi.dp(14), NUi.dp(7));
            b.setBackground(NUi.ripple(NUi.round(NTheme.surface, 999, NTheme.line2), 999));
            b.addView(NUi.ell(NUi.body(c, lk[0] + " " + NJCards.trunc(lk[1], 40), 13.5f, NTheme.text, 600), 1));
            if (!gone) { LinearLayout.LayoutParams il = NUi.lp(NUi.dp(14), NUi.dp(14)); il.leftMargin = NUi.dp(6); b.addView(NUi.icon(c, "next", 14, NTheme.text), il); }
            if (!gone) NUi.tap(b, new View.OnClickListener() { public void onClick(View v) {
                JSONObject l = t.optJSONObject("link");
                if (l != null && "entry".equals(l.optString("k"))) NEng.viewEntry(sh, l.optString("id")); else sh.route(lk[2]);
            } });
            LinearLayout.LayoutParams bl = NUi.lp(-2, -2); bl.topMargin = NUi.dp(12); body.addView(b, bl);
        }

        /* composer (web .thr-comp): tag emojis, a growing box and a send button */
        if (!done) {
            LinearLayout comp = NUi.col(c);
            comp.setBackground(NUi.card(22));
            comp.setPadding(NUi.dp(12), NUi.dp(10), NUi.dp(10), NUi.dp(10));
            LinearLayout tags = NUi.row(c);
            final List<TextView> chips = new ArrayList<>();
            for (final String[] tg : NForms.TAGS) {
                final TextView ch = NUi.text(c, NJCards.tag(tg[0])[1], 16, NTheme.text); ch.setGravity(Gravity.CENTER);
                ch.setContentDescription(NJCards.tag(tg[0])[2]);
                chips.add(ch);
                NUi.tap(ch, new View.OnClickListener() { public void onClick(View v) { tag[0] = tg[0]; paintTags(chips); } });
                LinearLayout.LayoutParams cl = NUi.lp(NUi.dp(40), NUi.dp(32)); if (tags.getChildCount() > 0) cl.leftMargin = NUi.dp(6);
                tags.addView(ch, cl);
            }
            paintTags(chips);
            comp.addView(tags);
            LinearLayout row = NUi.row(c); row.setGravity(Gravity.BOTTOM);
            final EditText in = NForms.input(c, "Add an update…", "", true); in.setMinHeight(NUi.dp(44)); in.setMaxHeight(NUi.dp(160)); in.setMinLines(1);
            in.setContentDescription("Add an update");
            row.addView(in, NUi.lpw(0, -2, 1));
            android.widget.FrameLayout send = new NUi.Fix(c, NUi.dp(46), NUi.dp(46));
            send.setBackground(NUi.ripple(NUi.round(NTheme.accent, 14, 0), 14));
            send.addView(NUi.icon(c, "send", 20, NTheme.onAccent), new android.widget.FrameLayout.LayoutParams(NUi.dp(20), NUi.dp(20), Gravity.CENTER));
            send.setContentDescription("Add update");
            NUi.tap(send, new View.OnClickListener() { public void onClick(View v) {
                String x = in.getText().toString().trim();
                if (x.isEmpty()) { in.setError("Write something"); return; }
                try { NForms.addUpdate(t, tag[0], x); t.put("u", System.currentTimeMillis()); } catch (Exception e) { NCrash.log(c, "thread add", e); }
                in.setText(""); sh.hideKeyboard(); NUi.haptic(v); sh.save(); NShell.toast("Added");
            } });
            LinearLayout.LayoutParams sl = NUi.lp(NUi.dp(46), NUi.dp(46)); sl.leftMargin = NUi.dp(8); row.addView(send, sl);
            comp.addView(row, NUi.mt(8));
            add(comp, 18);
        }

        /* jbar: "Latest first" and the cards/timeline switch */
        LinearLayout bar = NUi.row(c); bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.addView(NJCards.data(c, "Latest first", NTheme.muted), NUi.lpw(0, -2, 1));
        final boolean H0 = !"v".equals(jl());
        bar.addView(NBits.iconSeg(c, new String[][]{{"h", "horz"}, {"v", "vert"}}, H0 ? "h" : "v", new NBits.Pick() { public void on(String k) { try { JSONObject l = st.settings().optJSONObject("layout"); if (l == null) { l = new JSONObject(); st.settings().put("layout", l); } l.put("journal", k); } catch (Exception ignored) { } sh.save(); refresh(); } }));
        add(bar, done ? 18 : 4);
        if (U.isEmpty()) { TextView p = muted("Add the first line above. Come back any time and keep building on it."); p.setGravity(Gravity.CENTER); p.setPadding(NUi.dp(12), NUi.dp(36), NUi.dp(12), NUi.dp(36)); add(p); return; }
        if (H0) {
            List<NHs.Item> items = new ArrayList<>();
            for (final JSONObject u : U) items.add(NHs.item(NJCards.updBig(sh, t, u, new View.OnClickListener() { public void onClick(View v) { editUp(t, u); } }), NJCards.thrCol(sh, t), true, true, NDates.dayLabel(u.optLong("t"))));
            add(NHs.build(sh, items, 0, "thread-" + id), 14); return;
        }
        List<NSpine.Row> rows = new ArrayList<>();
        for (final JSONObject u : U) rows.add(NSpine.row(NDates.ymd(u.optLong("t")), NDates.dayLabel(u.optLong("t")), NJCards.updCard(sh, t, u, new View.OnClickListener() { public void onClick(View v) { editUp(t, u); } }), NJCards.thrCol(sh, t)));
        add(NSpine.build(c, rows, null), 14);
    }

    String jl() { JSONObject l = st.settings().optJSONObject("layout"); return l == null ? "h" : l.optString("journal", "h"); }

    void paintTags(List<TextView> chips) {
        for (int i = 0; i < chips.size(); i++) {
            boolean on = NForms.TAGS[i][0].equals(tag[0]);
            chips.get(i).setBackground(NUi.ripple(on ? NUi.round(NUi.mix(NTheme.accent, .16f, NTheme.surface), 99, NTheme.accent) : NUi.round(NTheme.surface, 99, NTheme.line2), 99));
        }
    }

    void editUp(final JSONObject t, final JSONObject u) {
        NForms F = new NForms(sh);
        LinearLayout b = F.sheetBody(t.optString("title"), "Edit update", null);
        final String[] k = {u.optString("k", "note")};
        b.addView(NForms.choice(c, NForms.TAGS, k, null), NUi.mt(12));
        final EditText in = NForms.input(c, "", u.optString("x"), true); in.setMinLines(4);
        b.addView(in, NUi.mt(12));
        b.addView(NForms.actions(c,
            NUi.btn(c, "Delete", false, new View.OnClickListener() { public void onClick(View v) {
                /* soft delete, as the web app does, so the next merge doesn't bring it back */
                try { long now = System.currentTimeMillis(); u.put("del", 1); u.put("u", now); t.put("u", now); } catch (Exception ignored) { }
                sh.closeSheet(); sh.save();
            } }),
            NUi.btn(c, "Save", true, new View.OnClickListener() { public void onClick(View v) {
                String x = in.getText().toString().trim(); if (x.isEmpty()) return;
                try { long now = System.currentTimeMillis(); u.put("x", x); u.put("k", k[0]); u.put("u", now); } catch (Exception ignored) { }
                sh.closeSheet(); sh.save();
            } })));
        sh.sheet(b);
    }
}
