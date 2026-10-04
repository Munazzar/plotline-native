package com.munazzar.plotline;

import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.json.JSONObject;

/* Goals: colourful cards, newest plans first; filter by active / achieved and by area. */
final class NGoalsPage extends NPage {
    String filter = "active", area = "", q = ""; boolean search;
    NGoalsPage(NShell sh) { super(sh); }

    @Override void build() {
        final NForms F = new NForms(sh);
        header("Goals", NUi.ibtn(c, "search", new View.OnClickListener() { public void onClick(View v) { search = !search; if (!search) q = ""; refresh(); } }), NUi.ibtn(c, "plus", new View.OnClickListener() { public void onClick(View v) { F.goal(null); } }), gear());
        List<JSONObject> all = NStore.list(st.arr("goals"));
        int act = 0, ach = 0; for (JSONObject g : all) { if ("active".equals(g.optString("status", "active"))) act++; else if ("done".equals(g.optString("status"))) ach++; }
        add(NUi.label(c, act + " active · " + ach + " achieved · pinch the cards to see more", NTheme.muted), 14);

        add(NBits.seg(c, new String[][]{{"active", "Active"}, {"short", "Short-term"}, {"long", "Long-term"}, {"done", "Achieved"}}, filter, new NBits.Pick() { public void on(String k) { filter = k; refresh(); } }), 18);
        LinearLayout ar = NUi.row(c);
        String an = "All areas"; for (int i = 0; i < NGen.AREA_ID.length; i++) if (NGen.AREA_ID[i].equals(area)) an = NGen.AREA_NAME[i];
        LinearLayout sel = NUi.row(c); sel.setPadding(NUi.dp(14), 0, NUi.dp(12), 0); sel.setBackground(NUi.ripple(NUi.round(NTheme.surface, 15, NTheme.line), 15));
        sel.addView(NUi.body(c, an, 14, NTheme.text, 600));
        View dn = NUi.icon(c, "down", 14, NTheme.muted); dn.setPadding(NUi.dp(10), 0, 0, 0); sel.addView(dn);
        NUi.tap(sel, new View.OnClickListener() { public void onClick(View v) {
            android.widget.PopupMenu pm = new android.widget.PopupMenu(c, v);
            pm.getMenu().add(0, 0, 0, "All areas");
            for (int i = 0; i < NGen.AREA_ID.length; i++) pm.getMenu().add(0, i + 1, i + 1, NGen.AREA_NAME[i]);
            pm.setOnMenuItemClickListener(new android.widget.PopupMenu.OnMenuItemClickListener() { public boolean onMenuItemClick(android.view.MenuItem m) { int i = m.getItemId(); area = i == 0 ? "" : NGen.AREA_ID[i - 1]; refresh(); return true; } });
            pm.show();
        } });
        ar.addView(sel, NUi.lp(-2, NUi.dp(46)));
        View sp = new View(c); ar.addView(sp, NUi.lpw(0, 1, 1));
        ar.addView(NBits.iconSeg(c, new String[][]{{"grid", "grid"}, {"h", "horz"}}, layout(), new NBits.Pick() { public void on(String k) { setLayout(k); } }));
        add(ar, 10);
        if (search) {
            final android.widget.EditText e = NForms.input(c, "Search goals and steps", q, false);
            e.addTextChangedListener(new android.text.TextWatcher() {
                public void beforeTextChanged(CharSequence a, int b, int d, int f) { }
                public void onTextChanged(CharSequence a, int b, int d, int f) { }
                public void afterTextChanged(android.text.Editable a) { q = a.toString(); }
            });
            e.setOnEditorActionListener(new TextView.OnEditorActionListener() { public boolean onEditorAction(TextView v, int id, android.view.KeyEvent k) { q = v.getText().toString(); refresh(); return true; } });
            e.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH); e.setSingleLine(true);
            add(e, 12);
        }

        List<JSONObject> l = new ArrayList<>();
        for (JSONObject g : all) {
            String s = g.optString("status", "active"), hz = g.optString("horizon", "month");
            boolean ok = filter.equals("done") ? s.equals("done") : s.equals("active") && (filter.equals("active") || (filter.equals("short") ? (hz.equals("week") || hz.equals("month") || hz.equals("quarter")) : !(hz.equals("week") || hz.equals("month") || hz.equals("quarter"))));
            if (ok && !q.isEmpty() && !(g.optString("title") + " " + g.optString("why")).toLowerCase().contains(q.toLowerCase())) ok = false;
            if (ok && (area.isEmpty() || NTheme.areaKey(g.optString("area")).equals(area))) l.add(g);
        }
        Collections.sort(l, new Comparator<JSONObject>() { public int compare(JSONObject a, JSONObject b) {
            int p = (b.optBoolean("pinned") ? 1 : 0) - (a.optBoolean("pinned") ? 1 : 0); if (p != 0) return p;
            int q = a.optInt("priority", 2) - b.optInt("priority", 2); if (q != 0) return q;
            return Long.compare(b.optLong("createdAt"), a.optLong("createdAt"));
        } });
        if (l.isEmpty()) {
            View e = NBits.empty(c, filter.equals("done") ? "Nothing achieved yet" : "No goals here", filter.equals("done") ? "Finished goals land here." : "Tap + to add a goal, or plan one with AI.");
            NUi.tap(e, new View.OnClickListener() { public void onClick(View v) { F.goal(null); } });
            add(e, 18); return;
        }
        if (layout().equals("h")) { carousel(l); return; }
        GridLayout g = new GridLayout(c); g.setColumnCount(sh.wide() ? 3 : 2);
        int i = 1;
        for (JSONObject x : l) {
            View card = NBits.flipCard(sh, x, i++);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f));
            lp.width = 0; lp.height = NUi.dp(sh.wide() ? 264 : 226); lp.setMargins(NUi.dp(5), NUi.dp(5), NUi.dp(5), NUi.dp(5));
            g.addView(card, lp);
        }
        LinearLayout.LayoutParams gl = NUi.mt(14); gl.leftMargin = gl.rightMargin = -NUi.dp(5);
        body.addView(g, gl);
    }

    String layout() { JSONObject l = st.settings().optJSONObject("layout"); return l != null && "h".equals(l.optString("goals")) ? "h" : "grid"; }
    void setLayout(String k) { try { JSONObject l = st.settings().optJSONObject("layout"); if (l == null) { l = new JSONObject(); st.settings().put("layout", l); } l.put("goals", k); } catch (Exception ignored) { } sh.save(); refresh(); }

    static String hzName(String h) { String[][] H = {{"week", "This week"}, {"month", "This month"}, {"quarter", "3 months"}, {"year", "This year"}, {"multi", "1–5 years"}, {"life", "Lifetime"}}; for (String[] x : H) if (x[0].equals(h)) return x[1]; return "This month"; }

    /* one big card at a time (web goalsCarousel → hsShell) */
    void carousel(List<JSONObject> l) {
        List<NHs.Item> it = new java.util.ArrayList<>(); int i = 1;
        for (JSONObject x : l) {
            String tgt = x.optString("targetDate");
            it.add(NHs.item(NBits.flipCard(sh, x, i++), NTheme.areaCol(x.optString("area")), "done".equals(x.optString("status")), true, NDates.valid(tgt) ? "Target " + NDates.fmtDate(tgt) : hzName(x.optString("horizon", "month"))));
        }
        add(NHs.build(sh, it, 0, "goals-" + l.size()), 18);
    }
}
