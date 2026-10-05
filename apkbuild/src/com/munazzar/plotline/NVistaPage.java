package com.munazzar.plotline;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONObject;

/* Vista: the whole plan two ways. Timeline (every goal flows across the months, steps as points) and Map (you, areas, goals). */
final class NVistaPage extends NPage {
    String zoom = "quarter", show = "active";
    final Set<String> open = new HashSet<>();
    HorizontalScrollView hsv; NRoad road; NMapView map; FrameLayout mapCard;
    boolean toToday = true, draw = true; String lastKey = "";
    static final java.util.Map<String, Float> PPD = new java.util.HashMap<>();
    static { PPD.put("month", 24f); PPD.put("quarter", 7f); PPD.put("year", 2.2f); }

    NVistaPage(NShell sh) { super(sh); }

    boolean isMap() { JSONObject l = st.settings().optJSONObject("layout"); return l != null && "map".equals(l.optString("vista")); }

    void setMode(String m) {
        try { JSONObject l = st.settings().optJSONObject("layout"); if (l == null) { l = new JSONObject(); st.settings().put("layout", l); } l.put("vista", m); } catch (Exception ignored) { }
        toToday = true; sh.save();
    }

    /* a route like "map" or "road" picks the view without a rebuild round-trip */
    void setModeQuiet(String m) {
        if (m == null) return;
        try { JSONObject l = st.settings().optJSONObject("layout"); if (l == null) { l = new JSONObject(); st.settings().put("layout", l); } if (!m.equals(l.optString("vista"))) { l.put("vista", m); stale = true; toToday = true; sh.saveQuiet(); } } catch (Exception ignored) { }
    }

    View modeSeg() {
        return NBits.seg(c, new String[][]{{"road", "Timeline"}, {"map", "Map"}}, isMap() ? "map" : "road", new NBits.Pick() { public void on(String k) { if ((k.equals("map")) != isMap()) setMode(k); } });
    }

    @Override void build() {
        if (hsv != null && !toToday) { savedX = hsv.getScrollX(); }
        if (overlay != null) { detach(overlay); overlay = null; }
        hsv = null; road = null; map = null;
        if (isMap()) buildMap(); else buildRoad();
    }
    int savedX; FrameLayout overlay;

    void buildRoad() {
        header("Vista",
            NUi.ibtn(c, "flag", new View.OnClickListener() { public void onClick(View v) { NChapters.list(sh); } }),
            NUi.ibtn(c, "ai", new View.OnClickListener() { public void onClick(View v) { NYear.open(sh, 0); } }),
            gear());
        LinearLayout r1 = NUi.row(c);
        r1.addView(modeSeg(), NUi.lpw(0, -2, 1));
        LinearLayout.LayoutParams l1 = NUi.lp(-2, -2); l1.leftMargin = NUi.dp(8);
        r1.addView(NUi.ibtn(c, "target", new View.OnClickListener() { public void onClick(View v) { jumpToday(true); } }), l1);
        LinearLayout.LayoutParams l2 = NUi.lp(-2, -2); l2.leftMargin = NUi.dp(8);
        r1.addView(NUi.ibtn(c, open.isEmpty() ? "vexpand" : "vcollapse", !open.isEmpty(), new View.OnClickListener() { public void onClick(View v) {
            if (!open.isEmpty()) open.clear(); else for (JSONObject g : NStore.list(st.arr("goals"))) open.add(g.optString("id"));
            draw = false; refresh();
        } }), l2);
        add(r1, 18);   /* web .vtool 18 under the header */
        LinearLayout r2 = NUi.row(c);
        r2.addView(NBits.seg(c, new String[][]{{"month", "Weeks"}, {"quarter", "Months"}, {"year", "Years"}}, zoom, new NBits.Pick() { public void on(String k) { zoom = k; toToday = true; draw = true; refresh(); } }), NUi.lp(-2, -2));
        View sp = new View(c); r2.addView(sp, NUi.lpw(0, 1, 1));
        r2.addView(NBits.seg(c, new String[][]{{"active", "Active"}, {"all", "All"}}, show, new NBits.Pick() { public void on(String k) { show = k; toToday = true; draw = true; refresh(); } }), NUi.lp(-2, -2));
        add(r2, 10);

        final List<JSONObject> gs = new ArrayList<>();
        for (JSONObject g : NStore.list(st.arr("goals"))) if (show.equals("all") || "active".equals(g.optString("status"))) gs.add(g);
        Collections.sort(gs, new Comparator<JSONObject>() { public int compare(JSONObject a, JSONObject b) {
            int d = NRoad.span(a)[0] - NRoad.span(b)[0]; return d != 0 ? d : a.optInt("priority", 2) - b.optInt("priority", 2); } });
        if (gs.isEmpty()) {
            LinearLayout e = NUi.col(c); e.setGravity(Gravity.CENTER_HORIZONTAL);
            e.setBackground(NUi.round(0, 22, NTheme.line2)); e.setPadding(NUi.dp(20), NUi.dp(28), NUi.dp(20), NUi.dp(24));
            TextView t = NUi.body(c, "Nothing on the timeline", 18, NTheme.text, 700); e.addView(t);
            TextView s = NUi.text(c, "Create a goal and it gets its own track here.", 14, NTheme.muted); s.setGravity(Gravity.CENTER); s.setPadding(0, NUi.dp(6), 0, NUi.dp(14)); e.addView(s);
            e.addView(NUi.btn(c, "New goal", true, new View.OnClickListener() { public void onClick(View v) { new NForms(sh).goal(null); } }));
            add(e, 18); return;
        }
        String key = zoom + show; boolean play = draw || !key.equals(lastKey); lastKey = key; draw = true;
        road = new NRoad(c, st, gs, PPD.get(zoom), false, open, false, true, new NRoad.Cb() {
            public void toggle(String gid) { if (!open.remove(gid)) open.add(gid); draw = false; refresh(); }
            public void step(JSONObject g, JSONObject s) { new NForms(sh).step(g, s); }
            public void open(JSONObject g) { sh.push(new NGoalScreen(sh, g.optString("id"))); }
            public void chapter(String id) { JSONObject ch = st.find("chapters", id); if (ch != null) NChapters.form(sh, ch); }
        });
        hsv = new HorizontalScrollView(c) {
            @Override protected void onScrollChanged(int l, int t, int ol, int ot) { super.onScrollChanged(l, t, ol, ot); if (road != null) road.invalidate(); }
        };
        hsv.setHorizontalScrollBarEnabled(false); hsv.setOverScrollMode(View.OVER_SCROLL_NEVER); hsv.setClipChildren(false);
        hsv.addView(road, new FrameLayout.LayoutParams(-2, -2));
        /* the road bleeds to the screen edges like the web's .road */
        LinearLayout.LayoutParams hl = NUi.mt(6); int side = NUi.dp(sh.wide() ? 32 : 16); hl.leftMargin = -side; hl.rightMargin = -side;
        body.addView(hsv, hl);
        if (play) road.play();
        final boolean tt = toToday; final int sx = savedX; toToday = false;
        hsv.post(new Runnable() { public void run() {
            if (hsv == null || road == null) return;
            if (tt) jumpToday(false); else hsv.scrollTo(sx, 0);
        } });
    }

    void jumpToday(boolean smooth) {
        if (hsv == null || road == null) return;
        int x = Math.max(0, Math.round((road.todayX - .25f * (hsv.getWidth() / NUi.density)) * NUi.density));
        if (smooth) hsv.smoothScrollTo(x, 0); else hsv.scrollTo(x, 0);
    }

    /* ---- map ---- */
    void buildMap() {
        header("Vista", gear());
        LinearLayout vs = NUi.row(c); vs.addView(modeSeg());
        add(vs, 18);
        int hdp = (int) Math.max(280, c.getResources().getConfiguration().screenHeightDp - (sh.top + sh.bot) / NUi.density - 150);
        final FrameLayout wrap = new FrameLayout(c);
        final boolean full = sh.focus;
        wrap.setBackground(NUi.round(NTheme.bg2, full ? 0 : 26, full ? 0 : NTheme.line));
        wrap.setClipToOutline(true);
        wrap.setOutlineProvider(new android.view.ViewOutlineProvider() { public void getOutline(View v, android.graphics.Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), full ? 0 : NUi.dp(26)); } });
        if (st.arr("goals").length() == 0) {
            LinearLayout e = NUi.col(c); e.setGravity(Gravity.CENTER); e.setPadding(NUi.dp(24), NUi.dp(24), NUi.dp(24), NUi.dp(24));
            e.addView(NUi.body(c, "Nothing on the map yet", 18, NTheme.text, 700));
            TextView s = NUi.text(c, "Create a goal and it appears around you.", 14, NTheme.muted); s.setGravity(Gravity.CENTER); s.setPadding(0, NUi.dp(6), 0, NUi.dp(14)); e.addView(s);
            e.addView(NUi.btn(c, "New goal", true, new View.OnClickListener() { public void onClick(View v) { new NForms(sh).goal(null); } }));
            wrap.addView(e, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
            add(wrap, 16); wrap.getLayoutParams().height = NUi.dp(hdp); return;
        }
        mapCard = new FrameLayout(c);
        map = new NMapView(c, st, new NMapView.Cb() {
            public void select(String id) { paintCard(id); }
            public void open(String id) { sh.push(new NGoalScreen(sh, id)); }
            public void persisted() { sh.saveQuiet(); }
        });
        wrap.addView(map, new FrameLayout.LayoutParams(-1, -1));
        /* controls, top right */
        LinearLayout ctl = NUi.col(c);
        ctl.addView(NUi.ibtn(c, "target", new View.OnClickListener() { public void onClick(View v) { map.fit(true); } }));
        LinearLayout.LayoutParams a = NUi.lp(-2, -2); a.topMargin = NUi.dp(6);
        ctl.addView(NUi.ibtn(c, "plus", new View.OnClickListener() { public void onClick(View v) { map.zoomBy(1.3f); } }), a);
        LinearLayout.LayoutParams b = NUi.lp(-2, -2); b.topMargin = NUi.dp(6);
        ctl.addView(NUi.ibtn(c, "minus", new View.OnClickListener() { public void onClick(View v) { map.zoomBy(.77f); } }), b);
        FrameLayout.LayoutParams cl = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END); cl.topMargin = NUi.dp(12) + (full ? sh.top : 0); cl.rightMargin = NUi.dp(12);
        wrap.addView(ctl, cl);
        /* spacing dock, bottom left */
        LinearLayout dock = NUi.row(c);
        dock.setBackground(NUi.round(NTheme.alpha(NTheme.bg2, .86f), 18, NTheme.line2)); dock.setPadding(NUi.dp(16), NUi.dp(6), NUi.dp(6), NUi.dp(6));
        TextView sl = NUi.label(c, "SPACING", NTheme.muted); dock.addView(sl);
        SeekBar sb = NUi.range(c); sb.setMax(24);
        double cur = st.settings().optDouble("mapSpace", 1); sb.setProgress((int) Math.round((cur - .8) / .05));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean fu) { if (!fu) return; try { st.settings().put("mapSpace", Math.round((.8 + p * .05) * 100) / 100.0); } catch (Exception ignored) { } map.rebuild(false); }
            public void onStartTrackingTouch(SeekBar s) { s.getParent().requestDisallowInterceptTouchEvent(true); }
            public void onStopTrackingTouch(SeekBar s) { sh.saveQuiet(); }
        });
        LinearLayout.LayoutParams sbl = NUi.lp(NUi.dp(136), NUi.dp(26)); sbl.leftMargin = NUi.dp(6); dock.addView(sb, sbl);
        LinearLayout.LayoutParams rl = NUi.lp(-2, -2); rl.leftMargin = NUi.dp(4);
        dock.addView(NUi.ibtn(c, "reset", new View.OnClickListener() { public void onClick(View v) {
            try { st.settings().put("mapSpace", 1); } catch (Exception ignored) { }
            map.reset(); sh.saveQuiet(); NShell.toast("Layout reset"); refresh();
        } }), rl);
        FrameLayout.LayoutParams dl = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.START); dl.leftMargin = NUi.dp(12); dl.bottomMargin = NUi.dp(full ? 70 : 12);
        wrap.addView(dock, dl);
        FrameLayout.LayoutParams ml = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM); ml.bottomMargin = NUi.dp(full ? 124 : 66); ml.leftMargin = NUi.dp(12); ml.rightMargin = NUi.dp(12);
        wrap.addView(mapCard, ml);
        if (full) { overlay = wrap; frame.addView(wrap, new FrameLayout.LayoutParams(-1, -1)); return; }
        add(wrap, 16); wrap.getLayoutParams().height = NUi.dp(hdp);
    }

    void paintCard(final String id) {
        mapCard.removeAllViews();
        JSONObject g = id == null ? null : st.find("goals", id);
        if (g == null) return;
        int col = NTheme.areaCol(g.optString("area"));
        LinearLayout card = NUi.row(c); card.setBackground(NCard.bg(col, 22)); card.setPadding(NUi.dp(18), NUi.dp(12), NUi.dp(12), NUi.dp(12));
        LinearLayout tx = NUi.col(c);
        JSONObject n = NActs.nextStep(g);
        tx.addView(NUi.label(c, (NTheme.areaName(g.optString("area")) + " · " + NActs.pct(st, g) + "%").toUpperCase(), NTheme.INK_MUTED));
        TextView t = NUi.title(c, g.optString("title").toUpperCase(), 19); t.setTextColor(NTheme.INK); t.setSingleLine(true); t.setEllipsize(android.text.TextUtils.TruncateAt.END); tx.addView(t);
        TextView s = NUi.label(c, n != null ? "Next: " + n.optString("title") : "done".equals(g.optString("status")) ? "Achieved" : "No open steps", NTheme.INK_MUTED); s.setSingleLine(true); s.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams sl = NUi.lp(-2, -2); sl.topMargin = NUi.dp(4); tx.addView(s, sl);
        card.addView(tx, NUi.lpw(0, -2, 1));
        TextView ob = NUi.btn(c, "Open", true, new View.OnClickListener() { public void onClick(View v) { sh.push(new NGoalScreen(sh, id)); } });
        LinearLayout.LayoutParams ol = NUi.lp(-2, -2); ol.leftMargin = NUi.dp(14); card.addView(ob, ol);
        mapCard.addView(card, new FrameLayout.LayoutParams(-1, -2));
    }
}
