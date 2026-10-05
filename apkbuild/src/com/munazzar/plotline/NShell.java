package com.munazzar.plotline;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsetsController;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

/* The native app: pager of main pages + tab bar + a stack for detail screens + bottom sheets + toasts.
   It sits on top of the web engine (MainActivity.web), which stays hidden and keeps doing sync, reminders,
   widgets and the screens that are not native yet (Vista, Ask, Settings…); those open as a "classic" layer
   under the native tab bar. */
final class NShell implements NStore.Listener {
    static NShell I;
    final MainActivity a;
    final FrameLayout root;
    final NStore st;
    FrameLayout nroot, content, stackLayer, sheetLayer;
    NPager pager;
    LinearLayout tabbar;
    View tabInd;
    final List<View> tabs = new ArrayList<>();
    NPage[] pages;
    final List<NPage> stack = new ArrayList<>();
    int top, bot, kb;
    boolean classic, quiet, waitOnboard, indPending;
    String day = NDates.ymd();
    String look = "";
    TextView toastV;

    /* tab: icon, label, page index (-1 = classic route) */
    static final String[][] TABS = {{"home", "Home", "0"}, {"habit", "Habits", "1"}, {"cal", "Calendar", "2"}, {"goals", "Goals", "3"}, {"map", "Vista", "4"}, {"journal", "Journal", "5"}, {"ai", "Ask", "6"}};

    NShell(MainActivity a, FrameLayout root) {
        this.a = a; this.root = root; I = this;
        NUi.density = a.getResources().getDisplayMetrics().density;
        NUi.narrow = a.getResources().getConfiguration().screenWidthDp <= 400;
        st = NStore.get(a);
        st.listeners.add(this);
        st.onLocalChange = new Runnable() { public void run() { a.js("window.__nativeChanged&&window.__nativeChanged()"); } };
        build();
    }

    boolean wide() { return a.getResources().getConfiguration().screenWidthDp >= 600; }

    String lookKey() { JSONObject s = st.settings(); return s.optString("theme") + "|" + s.optString("cards") + "|" + s.optString("cardBg") + "|" + s.optString("cardImg").length() + "|" + s.optString("accent") + "|" + s.optString("halo", "high") + s.optBoolean("haloBright") + s.optBoolean("haloPulse") + "|" + String.valueOf(s.optJSONObject("font")) + "|" + s.optBoolean("matchSystem") + "|" + NTheme.systemNight(a); }

    void build() {
        JSONObject set = st.settings();
        NTheme.load(set, a); NFont.load(a, set); look = lookKey();
        int keepPage = pager != null ? pager.page : 0;
        if (nroot != null) root.removeView(nroot);
        nroot = new FrameLayout(a);
        nroot.setBackgroundColor(NTheme.bg);
        content = new FrameLayout(a);
        nroot.addView(content, new FrameLayout.LayoutParams(-1, -1));
        pager = new NPager(a);
        pager.locked = true;   /* 1.13 has no swipe between tabs: a slightly sideways tap must never change the page */
        pages = new NPage[]{new NHome(this), new NHabitsPage(this), new NCalPage(this), new NGoalsPage(this), new NVistaPage(this), new NJournalPage(this), new NAskPage(this)};
        for (NPage p : pages) pager.addView(p.view());
        content.addView(pager, new FrameLayout.LayoutParams(-1, -1));
        stackLayer = new FrameLayout(a);
        content.addView(stackLayer, new FrameLayout.LayoutParams(-1, -1));
        for (NPage p : stack) { NPage.detach(p.view()); stackLayer.addView(p.view()); p.pad(); p.refresh(); }
        buildTabbar();
        sheetLayer = new FrameLayout(a);
        sheetLayer.setVisibility(View.GONE);
        nroot.addView(sheetLayer, new FrameLayout.LayoutParams(-1, -1));
        toastV = NUi.body(a, "", 15, NTheme.bg, 600);
        toastV.setBackground(NUi.round(NTheme.text, 16, 0));
        toastV.setPadding(NUi.dp(18), NUi.dp(13), NUi.dp(18), NUi.dp(13));
        toastV.setAlpha(0f); toastV.setVisibility(View.GONE); toastV.setElevation(NUi.dp(40));
        FrameLayout.LayoutParams tl = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        nroot.addView(toastV, tl);
        root.addView(nroot, new FrameLayout.LayoutParams(-1, -1));
        pager.listener = new NPager.Listener() {
            public void onPos(float pos) { moveInd(pos); }
            public void onPage(int i) { pages[i].onShow(); markTab(i); }
        };
        applyInsets();
        pager.page = keepPage;
        pages[keepPage].refresh();
        pager.go(keepPage, false);
        bars();
        if (classic) showClassicLayer(true);
        if (set.optBoolean("focus")) { final NShell me = this; root.post(new Runnable() { public void run() { me.focusMode(true, false); } }); }
    }

    void bars() {
        try {
            a.getWindow().getDecorView().setBackgroundColor(NTheme.bg);
            if (Build.VERSION.SDK_INT >= 30) {
                WindowInsetsController ic = a.getWindow().getInsetsController();
                int m = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                if (ic != null) ic.setSystemBarsAppearance(NTheme.light ? m : 0, m);
            } else {
                View d = a.getWindow().getDecorView(); int f = d.getSystemUiVisibility();
                int lf = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                d.setSystemUiVisibility(NTheme.light ? (f | lf) : (f & ~lf));
            }
        } catch (Exception ignored) { }
    }

    /* ---- tab bar ---- */
    void buildTabbar() {
        FrameLayout holder = new FrameLayout(a);
        tabbar = NUi.row(a);
        tabbar.setPadding(NUi.dp(6), NUi.dp(6), NUi.dp(6), NUi.dp(6));
        GradientDrawable bg = NUi.round(NTheme.alpha(NTheme.bg2, .94f), 30, NTheme.line2);
        holder.setBackground(bg);
        holder.setElevation(NUi.dp(12));
        tabInd = new View(a);
        tabInd.setBackground(NUi.round(NTheme.surface2, 24, NTheme.line2));
        holder.addView(tabInd, new FrameLayout.LayoutParams(0, NUi.dp(58), Gravity.CENTER_VERTICAL));
        tabs.clear();
        for (int i = 0; i < TABS.length; i++) {
            final int k = i;
            LinearLayout t = NUi.col(a); t.setGravity(Gravity.CENTER);
            ImageView ic = new ImageView(a); ic.setImageDrawable(new NIcon(TABS[i][0], NTheme.muted));
            t.addView(ic, new LinearLayout.LayoutParams(NUi.dp(23), NUi.dp(23)));
            TextView l = NUi.body(a, TABS[i][1], 11, NTheme.muted, 500); l.setGravity(Gravity.CENTER); l.setSingleLine(true);
            l.setAutoSizeTextTypeUniformWithConfiguration(8, 11, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
            LinearLayout.LayoutParams ll = NUi.lp(-2, -2); ll.topMargin = NUi.dp(3); t.addView(l, ll);
            t.setContentDescription(TABS[i][1]);
            NUi.tap(t, new View.OnClickListener() { public void onClick(View v) { NUi.haptic(v); tab(k); } });
            tabs.add(t);
            tabbar.addView(t, NUi.lpw(0, NUi.dp(58), 1));
        }
        holder.addView(tabbar, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER_VERTICAL));
        FrameLayout.LayoutParams hl = new FrameLayout.LayoutParams(wide() ? NUi.dp(560) : -1, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        nroot.addView(holder, hl);
        holder.setTag("tabbar");
        markTab(pager == null ? 0 : pager.page);
    }

    View tabHolder() { return (View) tabbar.getParent(); }

    int tabOfPage(int p) { for (int i = 0; i < TABS.length; i++) if (TABS[i][2].equals(String.valueOf(p))) return i; return 0; }

    void moveInd(float pos) {
        if (classic || tabs.isEmpty()) return;
        int lo = (int) Math.floor(pos), hi = Math.min(pages.length - 1, lo + 1);
        float f = pos - lo;
        View a1 = tabs.get(tabOfPage(Math.max(0, lo))), a2 = tabs.get(tabOfPage(hi));
        if (a1.getWidth() == 0) {
            if (!indPending) { indPending = true; tabbar.addOnLayoutChangeListener(new View.OnLayoutChangeListener() { public void onLayoutChange(View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) { v.removeOnLayoutChangeListener(this); indPending = false; if (pager != null) moveInd(pager.page); } }); }
            return;
        }
        float x = a1.getLeft() + (a2.getLeft() - a1.getLeft()) * f + tabbar.getPaddingLeft() * 0;
        ViewGroup.LayoutParams l = tabInd.getLayoutParams();
        if (l.width != a1.getWidth()) { l.width = a1.getWidth(); tabInd.setLayoutParams(l); }
        tabInd.setTranslationX(x);
    }

    void markTab(int page) {
        if (focus && fnav != null) buildFnav();
        int t = classic ? classicTab : tabOfPage(page);
        for (int i = 0; i < tabs.size(); i++) {
            LinearLayout v = (LinearLayout) tabs.get(i);
            int c = i == t ? NTheme.text : NTheme.muted;
            ((NIcon) ((ImageView) v.getChildAt(0)).getDrawable()).setColor(i == t ? NTheme.accent : NTheme.muted);
            ((TextView) v.getChildAt(1)).setTextColor(c);
            ((TextView) v.getChildAt(1)).setTypeface(NFont.body(i == t ? 700 : 500));
        }
        tabInd.animate().alpha(t < 0 ? 0f : 1f).setDuration(160).start();
        if (classic && t >= 0 && t < tabs.size()) {
            final View tv = tabs.get(t);
            tabbar.post(new Runnable() { public void run() { tabInd.animate().translationX(tv.getLeft()).setDuration(260).setInterpolator(NUi.EASE).start(); } });
        }
    }

    int classicTab = 4;

    /* web nav highlight for detail routes: goal→Goals, habit→Habits, day→Calendar, activity→Home, ai→Ask,
       thread(s)→Journal; Settings highlights nothing */
    int navPage(NPage p) {
        if (p instanceof NGoalScreen) return 3; if (p instanceof NHabitScreen) return 1; if (p instanceof NDayScreen) return 2;
        if (p instanceof NActivityScreen) return 0; if (p instanceof NAiPlan) return 6; if (p instanceof NThreadScreen) return 5;
        if (p instanceof NSettings) return -1;
        return pager.page;
    }
    void syncNav() {
        if (classic || tabs.isEmpty()) return;
        int pg = stack.isEmpty() ? pager.page : navPage(stack.get(stack.size() - 1));
        final int t = pg < 0 ? -1 : tabOfPage(pg);
        for (int i = 0; i < tabs.size(); i++) {
            LinearLayout v = (LinearLayout) tabs.get(i);
            ((NIcon) ((ImageView) v.getChildAt(0)).getDrawable()).setColor(i == t ? NTheme.accent : NTheme.muted);
            ((TextView) v.getChildAt(1)).setTextColor(i == t ? NTheme.text : NTheme.muted);
            ((TextView) v.getChildAt(1)).setTypeface(NFont.body(i == t ? 700 : 500));
        }
        tabInd.animate().alpha(t < 0 ? 0f : 1f).setDuration(160).start();
        if (t >= 0) { final View tv = tabs.get(t); tabbar.post(new Runnable() { public void run() { tabInd.animate().translationX(tv.getLeft()).setDuration(260).setInterpolator(NUi.EASE).start(); } }); }
    }

    void tab(int i) {
        if (a.lockShown) return;
        String p = TABS[i][2];
        closeSheet();
        if (!p.matches("\\d")) { classicTab = i; openClassic(p); return; }
        int page = Integer.parseInt(p);
        boolean wasClassic = classic;
        if (classic) hideClassic();
        if (!stack.isEmpty()) popAll();
        if (pager.page == page && !wasClassic) { NPage cur = pages[page]; cur.view(); cur.sv.smoothScrollTo(0, 0); return; }
        pager.go(page, !wasClassic);
        if (wasClassic) { pages[page].onShow(); markTab(page); moveInd(page); }
    }

    /* back in the app: a new day means every page shows the new day */
    void resume() {
        String d = NDates.ymd();
        if (d.equals(day)) return;
        day = d;
        for (NPage p : pages) p.stale = true;
        ((NCalPage) pages[2]).sel = d;
        if (!stack.isEmpty()) stack.get(stack.size() - 1).refresh();
        else if (!classic) pages[pager.page].refresh();
    }

    /* ---- insets ---- */
    void insets(int topPx, int botPx, int kbPx) { top = topPx; bot = botPx; kb = kbPx; applyInsets(); }

    void applyInsets() {
        if (nroot == null) return;
        for (NPage p : pages) p.pad();
        for (NPage p : stack) p.pad();
        View h = tabHolder();
        FrameLayout.LayoutParams hl = (FrameLayout.LayoutParams) h.getLayoutParams();
        hl.leftMargin = hl.rightMargin = NUi.dp(12); hl.bottomMargin = bot + NUi.dp(10);
        h.setLayoutParams(hl);
        h.setVisibility(kb > 0 ? View.GONE : a.lockShown ? View.INVISIBLE : View.VISIBLE);
        FrameLayout.LayoutParams tl = (FrameLayout.LayoutParams) toastV.getLayoutParams();
        tl.bottomMargin = bot + NUi.dp(kb > 0 ? 16 : 96); toastV.setLayoutParams(tl);
    }

    /* ---- data changes ---- */
    @Override public void onStore() {
        JSONObject set = st.settings();
        try { NTimer.of(this).sync(); } catch (Exception e) { NCrash.log(a, "timer sync", e); }
        if (waitOnboard && set.optBoolean("onboarded") && (set.optBoolean("setupDone") || !st.arr("goals").toString().equals("[]") || !st.arr("habits").toString().equals("[]"))) { waitOnboard = false; if (!a.lockShown) { if (a.lockOn()) a.showLock(); else hideClassic(); } }
        if (!lookKey().equals(look)) { build(); return; }
        for (NPage p : pages) p.stale = true;
        if (quiet) { quiet = false; return; }
        if (!stack.isEmpty()) stack.get(stack.size() - 1).refresh();
        else if (!classic) pages[pager.page].refresh();
    }

    /* after an in-place UI update: save, but don't rebuild the visible page */
    void saveQuiet() { quiet = true; try { st.changed(); } finally { quiet = false; } }
    void save() { st.changed(); }

    /* ---- detail screens ---- */
    void push(final NPage p) {
        p.back = true;
        closeSheet();
        if (classic) hideClassic();
        View v = p.view();
        p.pad(); p.refresh();
        v.setBackgroundColor(NTheme.bg);
        stackLayer.addView(v, new FrameLayout.LayoutParams(-1, -1));
        stack.add(p);
        syncNav();
        if (focus) buildFnav();
        v.setTranslationX(root.getWidth() > 0 ? root.getWidth() : 1000); v.setAlpha(1f);
        v.animate().translationX(0).setDuration(340).setInterpolator(NUi.EASE).start();
        final View under = stack.size() > 1 ? stack.get(stack.size() - 2).view() : pager;
        /* once covered, the screen underneath stops drawing (less overdraw; hidden from accessibility) */
        under.animate().translationX(-root.getWidth() * .22f).alpha(.6f).setDuration(340).setInterpolator(NUi.EASE).withEndAction(new Runnable() { public void run() {
            if (!stack.isEmpty() && stack.get(stack.size() - 1) == p) under.setVisibility(View.INVISIBLE);
        } }).start();
    }
    View underTop() { return stack.size() > 1 ? stack.get(stack.size() - 2).view() : pager; }

    void pop() {
        if (stack.isEmpty()) return;
        final NPage p = stack.remove(stack.size() - 1);
        hideKeyboard();
        final View v = p.view();
        v.animate().translationX(root.getWidth()).setDuration(280).setInterpolator(NUi.EASE).setListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator an) { v.animate().setListener(null); stackLayer.removeView(v); }
        }).start();
        View under = stack.isEmpty() ? pager : stack.get(stack.size() - 1).view();
        under.setVisibility(View.VISIBLE);
        under.animate().translationX(0).alpha(1f).setDuration(280).setInterpolator(NUi.EASE).start();
        if (stack.isEmpty()) { NPage cur = pages[pager.page]; if (cur.stale) cur.refresh(); }
        else { NPage t = stack.get(stack.size() - 1); t.refresh(); }
        syncNav();
        if (focus) buildFnav();
    }

    void popAll() {
        for (NPage p : stack) stackLayer.removeView(p.view());
        stack.clear(); pager.setTranslationX(0); pager.setAlpha(1f); pager.setVisibility(View.VISIBLE);
        NPage cur = pages[pager.page]; if (cur.stale) cur.refresh();
        syncNav();
    }

    /* swipe from the left edge to go back, like a native stack */
    void edgeSwipe(final View v) {
        v.setOnTouchListener(null);
        final float[] s = new float[2]; final boolean[] on = new boolean[1];
        if (!(v instanceof FrameLayout)) return;
        FrameLayout f = (FrameLayout) v;
        View edge = new View(a);
        f.addView(edge, new FrameLayout.LayoutParams(NUi.dp(22), -1, Gravity.START));
        edge.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View e, MotionEvent ev) {
                switch (ev.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN: s[0] = ev.getRawX(); on[0] = true; if (!stack.isEmpty()) underTop().setVisibility(View.VISIBLE); return true;
                    case MotionEvent.ACTION_MOVE: if (on[0]) v.setTranslationX(Math.max(0, ev.getRawX() - s[0])); return true;
                    default:
                        if (on[0]) { on[0] = false; if (v.getTranslationX() > v.getWidth() * .28f) pop(); else v.animate().translationX(0).setDuration(220).setInterpolator(NUi.EASE).start(); }
                        return true;
                }
            }
        });
    }

    /* ---- classic layer (web screens not ported yet) ---- */
    void openClassic(String route) {
        if (route.equals("ask")) { tabTo(6); return; }
        if (route.equals("ai")) { push(new NAiPlan(this)); return; }
        if (route.startsWith("vista") || route.startsWith("road") || route.startsWith("map")) { if (!route.equals("vista")) ((NVistaPage) pages[4]).setModeQuiet(route.startsWith("map") ? "map" : "road"); tabTo(4); return; }
        if (!stack.isEmpty()) popAll();
        classicTab = route.startsWith("ask") ? 6 : -1;
        a.js("window.__route&&window.__route(" + JSONObject.quote(route) + ")");
        showClassicLayer(false);
    }

    void showClassicLayer(boolean instant) {
        if (sheetLayer != null) { sheetLayer.setVisibility(View.GONE); sheetLayer.removeAllViews(); sheetPanel = null; }
        classic = true;
        a.web.setVisibility(View.VISIBLE);
        nroot.setBackgroundColor(Color.TRANSPARENT);
        if (instant) content.setVisibility(View.GONE);
        else content.animate().alpha(0f).setDuration(180).setListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator an) { content.animate().setListener(null); if (classic) content.setVisibility(View.GONE); }
        }).start();
        markTab(pager.page);
    }

    void hideClassic() {
        if (!classic || a.lockShown) return;
        classic = false;
        hideKeyboard();
        nroot.setBackgroundColor(NTheme.bg);
        content.setVisibility(View.VISIBLE); content.setAlpha(0f);
        content.animate().alpha(1f).setDuration(200).setListener(null).start();
        a.web.postDelayed(new Runnable() { public void run() { if (!classic) a.web.setVisibility(View.INVISIBLE); } }, 220);
        pages[pager.page].onShow(); markTab(pager.page); moveInd(pager.page);
    }

    /* full screen: hide the tab bar; a small pill brings it back */
    boolean focus; TextView focusPill; NYear year; NTour tour; Runnable settingsSave; NTimer timer;
    /* web FOCUS mode: the menus, page titles and gear go away, Android hides the system bars, and a floating pill
       at the top opens navigation. Kept in settings.focus, like the web. */
    LinearLayout fnav, fnPanel;
    void focusMode(boolean on) { focusMode(on, true); }
    void focusMode(boolean on, boolean tell) {
        focus = on;
        try { st.settings().put("focus", on); } catch (Exception ignored) { }
        if (tell) st.changed();   /* saves; every page rebuilds without its title */
        try { new Bridge(a).immersive(on); } catch (Exception ignored) { }
        View th = tabHolder(); if (th != null) th.setVisibility(on ? View.GONE : View.VISIBLE);
        if (focusPill != null) focusPill.setVisibility(View.GONE);
        if (on) buildFnav(); else if (fnav != null) { nroot.removeView(fnav); fnav = null; fnPanel = null; }
        for (NPage p : pages) { p.stale = true; p.pad(); }
        for (NPage p : stack) { p.pad(); p.refresh(); }
        if (stack.isEmpty()) pages[pager.page].refresh();
        if (tell) toast(on ? "Full screen · use the menu at the top to move around" : "Full screen off");
    }

    static final String[][] FN = {{"today", "Today", "today"}, {"habits", "Habits", "habit"}, {"cal", "Calendar", "cal"}, {"goals", "Goals", "goals"}, {"road", "Timeline", "road"}, {"map", "Map", "map"}, {"journal", "Journal", "journal"}, {"ask", "Ask", "ai"}, {"settings", "Settings", "settings"}};

    String fnCur() {
        if (!stack.isEmpty()) {
            NPage t = stack.get(stack.size() - 1);
            if (t instanceof NGoalScreen) return "goals"; if (t instanceof NHabitScreen) return "habits"; if (t instanceof NSettings) return "settings";
            if (t instanceof NThreadScreen) return "journal"; if (t instanceof NAiPlan) return "ask";
            return "today";
        }
        switch (pager.page) { case 1: return "habits"; case 2: return "cal"; case 3: return "goals"; case 4: return ((NVistaPage) pages[4]).isMap() ? "map" : "road"; case 5: return "journal"; case 6: return "ask"; default: return "today"; }
    }

    void buildFnav() {
        if (fnav != null) nroot.removeView(fnav);
        fnav = NUi.col(a);
        String on = fnCur(); String[] cur = FN[0]; for (String[] x : FN) if (x[0].equals(on)) cur = x;
        LinearLayout t = NUi.row(a); t.setGravity(Gravity.CENTER_VERTICAL);
        t.setBackground(NUi.round(NTheme.alpha(NTheme.bg2, .92f), 23, NTheme.line2)); t.setElevation(NUi.dp(10));
        t.setPadding(NUi.dp(13), 0, NUi.dp(14), 0);
        t.addView(NUi.icon(a, cur[2], 19, NTheme.accent), NUi.lp(NUi.dp(19), NUi.dp(19)));
        TextView tl = NUi.body(a, cur[1], 14, NTheme.text, 600); tl.setPadding(NUi.dp(9), 0, NUi.dp(9), 0); t.addView(tl);
        final View mi = NUi.icon(a, "menu", 17, NTheme.muted); t.addView(mi, NUi.lp(NUi.dp(17), NUi.dp(17)));
        fnav.addView(t, NUi.lp(-2, NUi.dp(46)));
        fnPanel = NUi.col(a); fnPanel.setVisibility(View.GONE);
        fnPanel.setBackground(NUi.round(NTheme.alpha(NTheme.bg2, .96f), 24, NTheme.line2)); fnPanel.setElevation(NUi.dp(16));
        fnPanel.setPadding(NUi.dp(8), NUi.dp(8), NUi.dp(8), NUi.dp(8));
        android.widget.GridLayout g = new android.widget.GridLayout(a); g.setColumnCount(2);
        int cw = (Math.min(NUi.dp(300), root.getWidth() > 0 ? root.getWidth() - NUi.dp(24) : NUi.dp(300)) - NUi.dp(22)) / 2;
        for (final String[] x : FN) {
            LinearLayout it = NUi.row(a); it.setGravity(Gravity.CENTER_VERTICAL); it.setPadding(NUi.dp(12), NUi.dp(11), NUi.dp(12), NUi.dp(11));
            boolean sel = x[0].equals(on);
            it.setBackground(NUi.ripple(sel ? NUi.round(NTheme.surface2, 15, NTheme.line2) : NUi.round(0, 15, 0), 15));
            it.addView(NUi.icon(a, x[2], 19, sel ? NTheme.accent : NTheme.muted), NUi.lp(NUi.dp(19), NUi.dp(19)));
            TextView l = NUi.body(a, x[1], 14, NTheme.text, 600); l.setPadding(NUi.dp(10), 0, 0, 0); it.addView(l);
            NUi.tap(it, new View.OnClickListener() { public void onClick(View v) { fnClose(); if (x[0].equals("settings")) { if (!(stack.size() > 0 && stack.get(stack.size() - 1) instanceof NSettings)) openSettings(); } else route(x[0]); buildFnav(); } });
            android.widget.GridLayout.LayoutParams gl = new android.widget.GridLayout.LayoutParams(); gl.width = cw; gl.setMargins(NUi.dp(3), NUi.dp(3), NUi.dp(3), NUi.dp(3));
            g.addView(it, gl);
        }
        fnPanel.addView(g);
        View sep = new View(a); sep.setBackgroundColor(NTheme.line); LinearLayout.LayoutParams sl = NUi.lp(-1, NUi.dp(1)); sl.topMargin = NUi.dp(4); fnPanel.addView(sep, sl);
        LinearLayout x = NUi.row(a); x.setGravity(Gravity.CENTER); x.setPadding(0, NUi.dp(11), 0, NUi.dp(11));
        x.addView(NUi.icon(a, "unfull", 19, NTheme.muted), NUi.lp(NUi.dp(19), NUi.dp(19)));
        TextView xl = NUi.body(a, "Exit full screen", 14, NTheme.muted, 600); xl.setPadding(NUi.dp(10), 0, 0, 0); x.addView(xl);
        NUi.tap(x, new View.OnClickListener() { public void onClick(View v) { fnClose(); focusMode(false); } });
        fnPanel.addView(x, NUi.lp(-1, -2));
        LinearLayout.LayoutParams pl = NUi.lp(-2, -2); pl.topMargin = NUi.dp(8); fnav.addView(fnPanel, pl);
        NUi.tap(t, new View.OnClickListener() { public void onClick(View v) {
            boolean o = fnPanel.getVisibility() != View.VISIBLE;
            if (o) { fnPanel.setVisibility(View.VISIBLE); fnPanel.setScaleX(.6f); fnPanel.setScaleY(.6f); fnPanel.setAlpha(0f); fnPanel.setPivotX(NUi.dp(22)); fnPanel.setPivotY(0); fnPanel.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(380).setInterpolator(new android.view.animation.OvershootInterpolator(1.2f)).start(); mi.animate().rotation(90).setDuration(300).start(); }
            else { fnClose(); mi.animate().rotation(0).setDuration(300).start(); }
        } });
        FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
        fl.leftMargin = NUi.dp(12); fl.topMargin = top + NUi.dp(12);
        nroot.addView(fnav, fl);
        fnav.bringToFront(); if (sheetLayer != null) sheetLayer.bringToFront(); if (toastV != null) toastV.bringToFront();
    }

    void fnClose() { if (fnPanel != null) fnPanel.setVisibility(View.GONE); }

    void openSettings() { push(new NSettings(this, null)); }

    /* run any web action (reminders, auto check-off, focus timer, routines, sharing…) in the engine, shown over the
       native screens. The native stack stays underneath, so back returns here. one = return to native when its sheet closes. */
    void act(String route, String act, JSONObject data, boolean one) {
        closeSheet();
        classicTab = -1;
        a.js("window.__nact&&window.__nact(" + JSONObject.quote(route == null ? "" : route) + "," + JSONObject.quote(act) + "," + (data == null ? "{}" : data.toString()) + "," + one + ")");
        showClassicLayer(false);
    }

    /* run a web action in the engine without showing the web layer (2.0.8) */
    void run(String act, JSONObject data) { a.js("window.__nrun&&window.__nrun(" + JSONObject.quote(act) + "," + (data == null ? "{}" : data.toString()) + ")"); }

    /* ask the assistant a question on the native Ask page (web askWith) */
    void ask(String q) { tabTo(6); a.js("(function(){if(IN.busy)return toast('Still answering the last question');if(IN.res){IN.ctl&&IN.ctl.abort();IN.res=null}window.__nasksend(" + JSONObject.quote(q) + ")})()"); }

    /* open a web page without dropping the native stack */
    void classicKeep(String route) { classicTab = -1; a.js("window.__route&&window.__route(" + JSONObject.quote(route) + ")"); showClassicLayer(false); }

    /* links from widgets, notifications and the web engine */
    boolean route(String r) {
        if (r == null) return false;
        String[] p = r.split("/");
        try {
            /* widget and notification shortcuts (web __route) */
            java.util.regex.Matcher hu = java.util.regex.Pattern.compile("^(urge|routine|slip)-([a-z0-9]+)$").matcher(r);
            if (hu.find()) {
                final String kind = hu.group(1), hid = hu.group(2); final JSONObject h = st.find("habits", hid);
                if (h == null) return false;
                tabTo(1); push(new NHabitScreen(this, hid));
                root.postDelayed(new Runnable() { public void run() { if (kind.equals("urge")) NUrge.urge(NShell.this, h); else if (kind.equals("slip")) NUrge.slip(NShell.this, h); else NUrge.routine(NShell.this, hid); } }, 450);
                return true;
            }
            java.util.regex.Matcher cd = java.util.regex.Pattern.compile("^cal-(\\d{4}-\\d{2}-\\d{2})$").matcher(r);
            if (cd.find()) { NCalPage cp = (NCalPage) pages[2]; cp.sel = cd.group(1); cp.setView("day"); cp.stale = true; tabTo(2); return true; }
            if (r.equals("hvista")) { NHabitsPage hp = (NHabitsPage) pages[1]; hp.filter = "vista"; hp.stale = true; tabTo(1); return true; }
            if (r.equals("add")) {
                tabTo(0);
                root.postDelayed(new Runnable() { public void run() { NPage hp = pages[0]; if (hp.body == null) return; View in = hp.body.findViewWithTag("dayIn"); if (in != null) { in.requestFocus(); hp.sv.smoothScrollTo(0, Math.max(0, in.getTop() - NUi.dp(200))); NForms.focus((android.widget.EditText) in); } } }, 450);
                return true;
            }
            switch (p[0]) {
                case "today": case "": tabTo(0); return true;
                case "habits": tabTo(1); return true;
                case "cal": tabTo(2); return true;
                case "ask": tabTo(6); return true;
                case "ai": push(new NAiPlan(this)); return true;
                case "vista": case "road": case "map": ((NVistaPage) pages[4]).setModeQuiet(p[0].equals("map") ? "map" : p[0].equals("road") ? "road" : null); tabTo(4); return true;
                case "goals": tabTo(3); return true;
                case "journal": case "threads": tabTo(5); ((NJournalPage) pages[5]).showThreads(p[0].equals("threads")); return true;
                case "habit": if (p.length > 1 && st.find("habits", p[1]) != null) { tabTo(1); push(new NHabitScreen(this, p[1])); return true; } return false;
                case "goal": if (p.length > 1 && st.find("goals", p[1]) != null) { tabTo(3); push(new NGoalScreen(this, p[1])); return true; } return false;
                case "day": push(new NDayScreen(this, p.length > 1 ? p[1] : NDates.ymd())); return true;
                case "activity": push(new NActivityScreen(this)); return true;
                case "settings": push(new NSettings(this, p.length > 1 ? p[1] : null)); return true;
                case "join": if (p.length > 1) { NShare.joinRoute(this, p[1]); return true; } return false;
                case "thread": if (p.length > 1 && st.find("threads", p[1]) != null) { tabTo(5); push(new NThreadScreen(this, p[1])); return true; } return false;
                default: return false;
            }
        } catch (Exception e) { NCrash.log(a, "route " + r, e); return false; }
    }

    void tabTo(int page) {
        closeSheet();
        if (classic) hideClassic();
        if (!stack.isEmpty()) popAll();
        pager.go(page, false); pages[page].onShow(); markTab(page);
    }

    boolean back() {
        if (tour != null) { tour.end(false); return true; }
        if (year != null) { year.close(); return true; }
        if (sheetLayer.getVisibility() == View.VISIBLE) { if (sheetBack != null) sheetBack.run(); else if (!sheetLock) closeSheet(); return true; }
        if (classic) return false;   /* MainActivity asks the web page first, then calls backFromClassic */
        if (!stack.isEmpty()) { pop(); return true; }
        if (pager.page != 0) { tab(0); return true; }
        return false;
    }

    /* ---- bottom sheets ---- */
    View sheetPanel;
    /* web SHEET_LOCK: no closing by tapping outside or dragging; back runs sheetBack instead */
    boolean sheetLock; Runnable sheetBack, onSheetClose;
    void sheet(View contentView) {
        hideKeyboardNow();
        if (onSheetClose != null) { Runnable r = onSheetClose; onSheetClose = null; try { r.run(); } catch (Exception ignored) { } }
        sheetLock = false; sheetBack = null;
        sheetLayer.removeAllViews();
        View dim = new View(a); dim.setBackgroundColor(0x73000000);
        dim.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { if (!sheetLock) closeSheet(); } });
        sheetLayer.addView(dim, new FrameLayout.LayoutParams(-1, -1));
        final LinearLayout panel = NUi.col(a);
        GradientDrawable g = new GradientDrawable(); g.setColor(NTheme.bg2);
        float r = NUi.dp(28); g.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0}); g.setStroke(NUi.dp(1), NTheme.line2);
        panel.setBackground(g);
        panel.setClickable(true);
        FrameLayout grabBox = new FrameLayout(a);
        View grab = new View(a); grab.setBackground(NUi.round(NTheme.line2, 3, 0));
        grabBox.addView(grab, new FrameLayout.LayoutParams(NUi.dp(42), NUi.dp(5), Gravity.CENTER));
        panel.addView(grabBox, new LinearLayout.LayoutParams(-1, NUi.dp(30)));
        ScrollView sv = new ScrollView(a); sv.setVerticalScrollBarEnabled(false);
        contentView.setPadding(NUi.dp(20), NUi.dp(6), NUi.dp(20), NUi.dp(24) + bot);
        sv.addView(contentView);
        panel.addView(sv, new LinearLayout.LayoutParams(-1, -2));
        FrameLayout.LayoutParams pl = new FrameLayout.LayoutParams(wide() ? NUi.dp(560) : -1, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        pl.topMargin = top + NUi.dp(24);
        sheetLayer.addView(panel, pl);
        sheetPanel = panel;
        sheetLayer.setElevation(NUi.dp(30));   /* above the floating tab bar (elevation 12) */
        sheetLayer.setVisibility(View.VISIBLE); sheetLayer.bringToFront(); if (toastV != null) toastV.bringToFront();
        dim.setAlpha(0f); dim.animate().alpha(1f).setDuration(220).start();
        panel.setTranslationY(NUi.dp(600));
        panel.animate().translationY(0).setDuration(360).setInterpolator(NUi.EASE).start();
        /* drag the top of the sheet down to close it */
        final float[] s = new float[1];
        panel.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN: s[0] = e.getRawY(); return !sheetLock && e.getY() < NUi.dp(40);
                    case MotionEvent.ACTION_MOVE: panel.setTranslationY(Math.max(0, e.getRawY() - s[0])); return true;
                    case MotionEvent.ACTION_UP: case MotionEvent.ACTION_CANCEL: if (panel.getTranslationY() > NUi.dp(110)) closeSheet(); else panel.animate().translationY(0).setDuration(220).setInterpolator(NUi.EASE).start(); return true;
                    default: return true;
                }
            }
        });
    }

    void closeSheet() {
        if (sheetLayer == null || sheetLayer.getVisibility() != View.VISIBLE || sheetPanel == null) return;
        if (onSheetClose != null) { Runnable r = onSheetClose; onSheetClose = null; try { r.run(); } catch (Exception ignored) { } }
        sheetLock = false; sheetBack = null;
        hideKeyboard();
        final View panel = sheetPanel;
        if (panel != null) panel.animate().translationY(panel.getHeight() + NUi.dp(40)).setDuration(240).setInterpolator(NUi.EASE).start();
        if (sheetLayer.getChildCount() > 0) sheetLayer.getChildAt(0).animate().alpha(0f).setDuration(240).start();
        sheetLayer.postDelayed(new Runnable() { public void run() { if (sheetPanel != panel) return; sheetLayer.setVisibility(View.GONE); sheetLayer.removeAllViews(); sheetPanel = null; } }, 250);
    }

    void hideKeyboard() {
        hideKeyboardNow();
    }

    void hideKeyboardNow() {
        try { InputMethodManager im = (InputMethodManager) a.getSystemService(Context.INPUT_METHOD_SERVICE); im.hideSoftInputFromWindow(root.getWindowToken(), 0); } catch (Exception ignored) { }
    }

    /* ---- toasts ---- */
    Runnable toastHide;
    static void toast(String msg) { if (I != null) I.showToast(msg, null, null); }

    void showToast(String msg, String action, final Runnable onAction) {
        toastV.setText(action == null ? msg : msg + "   ·   " + action);
        toastV.setOnClickListener(onAction == null ? null : new View.OnClickListener() { public void onClick(View v) { onAction.run(); toastV.setVisibility(View.GONE); } });
        toastV.setClickable(onAction != null);
        toastV.setVisibility(View.VISIBLE); toastV.bringToFront();
        toastV.setTranslationY(NUi.dp(20));
        toastV.animate().alpha(1f).translationY(0).setDuration(240).setInterpolator(NUi.EASE).start();
        if (toastHide != null) toastV.removeCallbacks(toastHide);
        toastHide = new Runnable() { public void run() { toastV.animate().alpha(0f).translationY(NUi.dp(12)).setDuration(240).withEndAction(new Runnable() { public void run() { toastV.setVisibility(View.GONE); } }).start(); } };
        toastV.postDelayed(toastHide, onAction == null ? 2600 : 4200);
    }

    static View errorCard(final Context c, final Throwable e) {
        LinearLayout l = NUi.col(c);
        l.setBackground(NUi.round(NTheme.surface, 20, NTheme.LATE));
        l.setPadding(NUi.dp(18), NUi.dp(16), NUi.dp(18), NUi.dp(16));
        l.addView(NUi.body(c, "This part couldn't load", 16, NTheme.text, 700));
        TextView m = NUi.text(c, String.valueOf(e), 13, NTheme.muted); l.addView(m, NUi.mt(6));
        TextView b = NUi.link(c, "Copy details for support", new View.OnClickListener() { public void onClick(View v) {
            ClipboardManager cm = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("Plotline error", NCrash.trace(e))); toast("Copied");
        } });
        l.addView(b, NUi.mt(6));
        LinearLayout.LayoutParams p = NUi.mt(16); l.setLayoutParams(p);
        return l;
    }
}
