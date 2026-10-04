package com.munazzar.plotline;

import android.content.Intent;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

/* Ask: the private assistant. The questions, retrieval and AI run in the web engine (same code as the web, so the
   answers match); this page draws the chat and sends what you type. */
final class NAskPage extends NPage {
    JSONObject d; String sig = ""; boolean polling, warm, scrollDown;
    LinearLayout comp; EditText in; FrameLayout go; TextView scope; View goIcon, stopIcon;
    String paste = "";

    NAskPage(NShell sh) { super(sh); }

    @Override View view() {
        boolean first = frame == null;
        View v = super.view();
        if (first) composer();
        return v;
    }

    boolean active() { return sh.stack.isEmpty() && !sh.classic && sh.pages[sh.pager.page] == this; }

    @Override void onShow() { warm = true; super.onShow(); startPoll(); }

    void startPoll() { if (polling) return; polling = true; tick(); }

    void tick() {
        if (!active()) { polling = false; return; }
        final boolean w = warm; warm = false;
        sh.a.jsRet("window.__nask&&window.__nask(" + w + ")", new android.webkit.ValueCallback<String>() { @Override public void onReceiveValue(String v) {
            long next = 1600;
            try {
                if (v != null) {
                    JSONObject o = new JSONObject(v);
                    if (!o.has("err")) {
                        String ns = o.toString();
                        boolean busy = o.optBoolean("busy");
                        if (!ns.equals(sig)) { sig = ns; d = o; refresh(); if (busy || scrollDown) { scrollDown = false; sv.post(new Runnable() { public void run() { sv.fullScroll(View.FOCUS_DOWN); } }); } }
                        sync(o);
                        if (o.optBoolean("sheet") && !sh.classic) sh.showClassicLayer(false);
                        next = busy ? 350 : 1600;
                    }
                }
            } catch (Exception ignored) { }
            if (polling) frame.postDelayed(new Runnable() { public void run() { tick(); } }, next);
        } });
    }

    /* ---- composer, pinned above the tab bar ---- */
    void composer() {
        /* web .ask-comp (phone): full width, frosted page colour behind it, scope pill then the .ask-row box */
        comp = NUi.col(c);
        comp.setBackground(new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM, new int[]{NTheme.alpha(NTheme.bg, 0), NTheme.alpha(NTheme.bg, .72f), NTheme.alpha(NTheme.bg, .92f)}));
        LinearLayout sr = NUi.row(c);
        scope = NUi.body(c, "All time", 12.5f, NTheme.muted, 600);
        LinearLayout pill = NUi.row(c); pill.setPadding(NUi.dp(11), NUi.dp(5), NUi.dp(11), NUi.dp(5)); pill.setBackground(NUi.round(NTheme.alpha(NTheme.bg2, .8f), 99, NTheme.line));
        pill.addView(NUi.icon(c, "search", 12, NTheme.muted), NUi.lp(NUi.dp(12), NUi.dp(12)));
        scope.setPadding(NUi.dp(6), 0, 0, 0); pill.addView(scope);
        NUi.tap(pill, new View.OnClickListener() { public void onClick(View v) { NEng.askScope(sh, new Runnable() { public void run() { sig = ""; } }); } });
        LinearLayout.LayoutParams pl = NUi.lp(-2, -2); pl.leftMargin = NUi.dp(6); pl.bottomMargin = NUi.dp(7); sr.addView(pill, pl); comp.addView(sr);
        LinearLayout row = NUi.row(c); row.setGravity(Gravity.BOTTOM);
        row.setPadding(NUi.dp(16), NUi.dp(7), NUi.dp(7), NUi.dp(7)); row.setBackground(NUi.round(NTheme.alpha(NTheme.bg2, .88f), 24, NTheme.line2));
        in = new EditText(c); in.setHint("Ask anything about your life…"); in.setHintTextColor(NTheme.alpha(NTheme.muted, .9f)); in.setTextColor(NTheme.text); in.setTextSize(16); in.setTypeface(NFont.body(500));
        in.setBackground(null); in.setMaxLines(5); in.setMinHeight(NUi.dp(40)); in.setPadding(0, NUi.dp(9), 0, NUi.dp(9));
        in.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        row.addView(in, NUi.lpw(0, -2, 1));
        NVoice.attach(c, in);
        go = new FrameLayout(c); go.setBackground(NUi.ripple(NUi.round(NTheme.accent, 15, 0), 15));
        goIcon = NUi.icon(c, "up", 20, NTheme.bg); go.addView(goIcon, new FrameLayout.LayoutParams(NUi.dp(20), NUi.dp(20), Gravity.CENTER));
        View st = new View(c); st.setBackground(NUi.round(NTheme.onAccent, 3, 0)); stopIcon = st; st.setVisibility(View.GONE); go.addView(st, new FrameLayout.LayoutParams(NUi.dp(14), NUi.dp(14), Gravity.CENTER));
        NUi.tap(go, new View.OnClickListener() { public void onClick(View v) { send(); } });
        LinearLayout.LayoutParams gl = NUi.lp(NUi.dp(42), NUi.dp(42)); gl.leftMargin = NUi.dp(8); row.addView(go, gl);
        comp.addView(row);
        FrameLayout.LayoutParams l = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        frame.addView(comp, l);
        pad();
    }

    @Override void pad() {
        super.pad();
        if (comp == null) return;
        FrameLayout.LayoutParams l = (FrameLayout.LayoutParams) comp.getLayoutParams();
        l.leftMargin = l.rightMargin = 0; l.bottomMargin = 0;
        int gut = NUi.dp(sh.wide() ? 32 : 16);
        comp.setPadding(gut, NUi.dp(26), gut, sh.kb > 0 ? NUi.dp(8) : sh.bot + NUi.dp(100));
        comp.setLayoutParams(l);
        if (body != null) body.setPadding(body.getPaddingLeft(), body.getPaddingTop(), body.getPaddingRight(), (sh.kb > 0 ? NUi.dp(130) : sh.bot + NUi.dp(230)));
    }

    void sync(JSONObject o) {
        if (scope != null) scope.setText(o.optString("scope", "All time"));
        boolean busy = o.optBoolean("busy");
        if (goIcon != null) { goIcon.setVisibility(busy ? View.GONE : View.VISIBLE); stopIcon.setVisibility(busy ? View.VISIBLE : View.GONE); }
        if (in != null) in.setHint(o.optBoolean("has") ? "Ask a follow-up…" : "Ask anything about your life…");
    }

    void send() {
        if (d != null && d.optBoolean("busy")) { sh.run("insStop", null); return; }
        String q = in.getText().toString().trim();
        if (q.isEmpty()) { NShell.toast("Ask a question or pick one of the suggestions"); return; }
        in.setText(""); scrollDown = true;
        sh.a.js("window.__nasksend&&window.__nasksend(" + JSONObject.quote(q) + ")");
        try { android.view.inputmethod.InputMethodManager im = (android.view.inputmethod.InputMethodManager) c.getSystemService(android.content.Context.INPUT_METHOD_SERVICE); im.hideSoftInputFromWindow(in.getWindowToken(), 0); } catch (Exception ignored) { }
        sig = "";
    }

    void ask(String q) { in.setText(""); scrollDown = true; sh.a.js("window.__nasksend&&window.__nasksend(" + JSONObject.quote(q) + ")"); sig = ""; }

    /* ---- page ---- */
    @Override void build() {
        startPoll();
        boolean has = d != null && d.optBoolean("has");
        java.util.List<View> acts = new java.util.ArrayList<>();
        if (has) acts.add(NUi.ibtn(c, "plus", new View.OnClickListener() { public void onClick(View v) { sh.run("askNew", null); sig = ""; } }));
        acts.add(NUi.ibtn(c, "tune", new View.OnClickListener() { public void onClick(View v) { NSheets.assistant(sh); } }));
        acts.add(gear());
        header("Ask", acts.toArray(new View[0]));
        String lab = d == null ? "" : d.optString("lab");
        if (!lab.isEmpty()) {
            LinearLayout r = NUi.row(c); TextView t = NBits.meta(c, lab.toUpperCase(), NTheme.muted); t.setTextSize(10.5f); r.addView(t); r.addView(NUi.icon(c, "next", 12, NTheme.muted), NUi.lp(NUi.dp(12), NUi.dp(12)));
            NUi.tap(r, new View.OnClickListener() { public void onClick(View v) { NSheets.assistant(sh); } });
            add(r, 6);
        }
        if (d == null) { add(NBits.empty(c, "Getting ready…", "Opening your assistant."), 24); return; }
        if (!has) intro(); else thread();
    }

    void intro() {
        LinearLayout hero = NUi.col(c); hero.setGravity(Gravity.CENTER_HORIZONTAL); hero.setPadding(0, NUi.dp(18), 0, NUi.dp(6));
        /* web .ask-orb (phone): 56px, radius 20, accent radial wash, accent border and glow */
        FrameLayout orb = new FrameLayout(c) {
            final android.graphics.Paint gp = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            { setWillNotDraw(false); setLayerType(LAYER_TYPE_SOFTWARE, null); }
            @Override protected void onDraw(android.graphics.Canvas cv) {
                float w = getWidth(), h = getHeight(), r = NUi.dp(20); android.graphics.RectF rb = new android.graphics.RectF(0, 0, w, h);
                gp.setColor(NTheme.alpha(NTheme.accent, .45f)); gp.setMaskFilter(new android.graphics.BlurMaskFilter(NUi.dp(18), android.graphics.BlurMaskFilter.Blur.OUTER)); cv.drawRoundRect(rb, r, r, gp); gp.setMaskFilter(null);
                gp.setShader(new android.graphics.RadialGradient(w * .3f, h * .25f, w, NUi.mix(NTheme.accent, .34f, NTheme.bg), NUi.mix(NTheme.accent, .06f, NTheme.surface), android.graphics.Shader.TileMode.CLAMP)); cv.drawRoundRect(rb, r, r, gp); gp.setShader(null);
                gp.setStyle(android.graphics.Paint.Style.STROKE); gp.setStrokeWidth(Math.max(1, NUi.dp(1))); gp.setColor(NTheme.alpha(NTheme.accent, .4f)); cv.drawRoundRect(new android.graphics.RectF(.5f, .5f, w - .5f, h - .5f), r, r, gp); gp.setStyle(android.graphics.Paint.Style.FILL);
                super.onDraw(cv);
            }
        };
        orb.addView(NUi.icon(c, "ai", 30, NTheme.accent), new FrameLayout.LayoutParams(NUi.dp(30), NUi.dp(30), Gravity.CENTER));
        hero.addView(orb, NUi.lp(NUi.dp(56), NUi.dp(56)));
        String nm = d.optString("name"); int h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        String gr = h < 5 ? "Still up" : h < 12 ? "Good morning" : h < 17 ? "Good afternoon" : "Good evening";
        TextView t = NUi.title(c, gr + (nm.isEmpty() ? "" : ", " + nm) + ".", 28); t.setGravity(Gravity.CENTER); hero.addView(t, NUi.mt(14));
        TextView sb = NUi.text(c, "Ask anything about your goals, habits and journal. Answers come from your own notes" + ("local".equals(d.optString("eng")) ? ", on this device" : "") + ".", 14.5f, NTheme.muted); sb.setGravity(Gravity.CENTER); sb.setLineSpacing(0, 1.2f); hero.addView(sb, NUi.mt(8));
        add(hero, 8);

        View plan = NHome.aip(sh, "ask");
        add(plan, 0); ((LinearLayout.LayoutParams) plan.getLayoutParams()).bottomMargin = NUi.dp(18);

        /* web .ask-sugs on phones: two columns, gap 8 */
        JSONArray sg = d.optJSONArray("sugs"), id = d.optJSONArray("ideas");
        android.widget.GridLayout box = new android.widget.GridLayout(c); box.setColumnCount(2);
        java.util.List<View> cells = new java.util.ArrayList<>();
        for (int i = 0; sg != null && i < sg.length(); i++) {
            final JSONObject x = sg.optJSONObject(i);
            cells.add(sug(x.optString("n"), x.optString("d"), false, new View.OnClickListener() { public void onClick(View v) { sh.run("askPre", NMore.d("k", x.optString("k"))); sig = ""; scrollDown = true; } }));
        }
        for (int i = 0; id != null && i < id.length(); i++) {
            final String q = id.optString(i);
            cells.add(sug(q, "From your plan", true, new View.OnClickListener() { public void onClick(View v) { ask(q); } }));
        }
        for (View v : cells) {
            android.widget.GridLayout.LayoutParams l = new android.widget.GridLayout.LayoutParams(android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, android.widget.GridLayout.FILL), android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f));
            l.width = 0; l.setMargins(NUi.dp(4), NUi.dp(4), NUi.dp(4), NUi.dp(4)); box.addView(v, l);
        }
        LinearLayout.LayoutParams bl = NUi.mt(4); bl.leftMargin = bl.rightMargin = -NUi.dp(4); body.addView(box, bl);

        JSONArray st = d.optJSONArray("stats");
        if (st != null && st.length() > 0) {
            TextView sl = NUi.text(c, "Signals" + (d.optInt("win") > 0 ? " · last " + d.optInt("win") + " days" : ""), 13, NTheme.muted); sl.setPadding(0, NUi.dp(22), 0, NUi.dp(10)); add(sl);
            android.widget.GridLayout g = new android.widget.GridLayout(c); g.setColumnCount(2);
            for (int i = 0; i < st.length(); i++) {
                JSONObject x = st.optJSONObject(i); boolean hl = x.optBoolean("hl");
                LinearLayout t2 = NUi.col(c); t2.setPadding(NUi.dp(14), NUi.dp(12), NUi.dp(14), NUi.dp(12));
                t2.setBackground(NUi.round(hl ? NUi.mix(NTheme.accent, .1f, NTheme.surface) : NTheme.surface, 18, hl ? NTheme.alpha(NTheme.accent, .4f) : NTheme.line));
                TextView v = NUi.text(c, x.optString("v"), 24, NTheme.text); v.setTypeface(NFont.display(800)); t2.addView(v);
                t2.addView(NUi.ell(NUi.text(c, x.optString("k"), 12.5f, NTheme.muted), 1));
                if (hl && !x.optString("t").isEmpty()) t2.addView(NUi.ell(NUi.text(c, x.optString("t"), 11.5f, NTheme.muted), 2));
                android.widget.GridLayout.LayoutParams l = new android.widget.GridLayout.LayoutParams(android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED), android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f));
                l.width = 0; l.setMargins(NUi.dp(4), NUi.dp(4), NUi.dp(4), NUi.dp(4)); g.addView(t2, l);
            }
            LinearLayout.LayoutParams gl = NUi.mt(0); gl.leftMargin = gl.rightMargin = -NUi.dp(4); body.addView(g, gl);
        }
    }

    View sug(String t, String sub, boolean idea, View.OnClickListener l) {
        /* web .ask-sug (phone): padding 12/13, radius 16, b 13.5/650, small 12 muted; ideas dashed with accent small */
        LinearLayout r = NUi.col(c); r.setPadding(NUi.dp(13), NUi.dp(12), NUi.dp(13), NUi.dp(12));
        r.setBackground(NUi.ripple(idea ? NUi.dashed(NTheme.surface, 16, NTheme.line, 1) : NUi.round(NTheme.surface, 16, NTheme.line), 16));
        TextView tb = NUi.body(c, t, 13.5f, NTheme.text, 600); tb.setLineSpacing(0, 1.2f); r.addView(tb);
        r.addView(NUi.text(c, sub, 12, idea ? NTheme.accent : NTheme.muted), NUi.mt(4));
        NUi.tap(r, l); return r;
    }

    void thread() {
        JSONArray ts = d.optJSONArray("turns"), ss = d.optJSONArray("srcs");
        final int n = d.optInt("n"); boolean pending = d.optBoolean("pending"), busy = d.optBoolean("busy");
        boolean allDone = true; boolean anyAns = false;
        for (int i = 0; ts != null && i < ts.length(); i++) { JSONObject t = ts.optJSONObject(i); if (!t.optBoolean("done")) allDone = false; if (!t.optString("a").isEmpty() && !t.optBoolean("off")) anyAns = true; }
        for (int i = 0; ts != null && i < ts.length(); i++) {
            JSONObject t = ts.optJSONObject(i); boolean last = i == ts.length() - 1;
            TextView me = NUi.text(c, t.optString("q"), 15.5f, NTheme.text); me.setLineSpacing(0, 1.2f); me.setPadding(NUi.dp(16), NUi.dp(12), NUi.dp(16), NUi.dp(12));
            me.setBackground(NUi.round(NTheme.surface2, 20, NTheme.line2));
            LinearLayout.LayoutParams ml = NUi.lp(-2, -2); ml.gravity = Gravity.END; ml.topMargin = NUi.dp(i == 0 ? 20 : 26); ml.leftMargin = NUi.dp(48); body.addView(me, ml);
            LinearLayout ai = NUi.row(c); ai.setGravity(Gravity.TOP);
            FrameLayout av = new FrameLayout(c); av.setBackground(NUi.round(NTheme.alpha(NTheme.accent, .18f), 12, 0)); av.addView(NUi.icon(c, "ai", 16, NTheme.accent), new FrameLayout.LayoutParams(NUi.dp(16), NUi.dp(16), Gravity.CENTER));
            ai.addView(av, NUi.lp(NUi.dp(32), NUi.dp(32)));
            LinearLayout bd = NUi.col(c); bd.setPadding(NUi.dp(12), NUi.dp(4), 0, 0);
            if (pending) { bd.addView(NUi.text(c, d.optString("prog").isEmpty() ? "Searching your notes…" : d.optString("prog"), 14, NTheme.muted)); }
            else {
                if (i == 0 && ss != null && ss.length() > 0) bd.addView(top(ss));
                String eng = d.optString("eng");
                if (eng.equals("copy") && t.optString("a").isEmpty()) bd.addView(copyBox(i, n));
                else {
                    if (!t.optString("err").isEmpty()) { TextView er = NUi.text(c, t.optString("err"), 13, NTheme.LATE); bd.addView(er, NUi.mt(6)); }
                    if (!t.optString("a").isEmpty()) bd.addView(NMd.render(c, t.optString("a"), n, new NMd.Cite() { public void on(int k) { openSrc(k); } }), NUi.mt(6));
                    else if (!t.optBoolean("done")) bd.addView(NUi.text(c, "●  ●  ●", 14, NTheme.muted), NUi.mt(6));
                    if (last && !t.optBoolean("done") && !d.optString("prog").isEmpty()) bd.addView(NUi.text(c, d.optString("prog"), 13, NTheme.muted), NUi.mt(8));
                }
            }
            ai.addView(bd, NUi.lpw(0, -2, 1));
            body.addView(ai, NUi.mt(14));
            if (last && allDone && anyAns && !t.optBoolean("off") && !pending) {
                NFlow f = new NFlow(c, 8, 8);
                f.addView(NUi.btn(c, "Save to journal", false, new View.OnClickListener() { public void onClick(View v) { sh.run("insSave", null); } }), new ViewGroup.MarginLayoutParams(-2, NUi.dp(40)));
                f.addView(NUi.btn(c, "Copy", false, new View.OnClickListener() { public void onClick(View v) { sh.run("insCopyAns", null); } }), new ViewGroup.MarginLayoutParams(-2, NUi.dp(40)));
                if (n > 0) f.addView(NUi.btn(c, n + " source" + (n == 1 ? "" : "s"), false, new View.OnClickListener() { public void onClick(View v) { sources(); } }), new ViewGroup.MarginLayoutParams(-2, NUi.dp(40)));
                LinearLayout.LayoutParams fl = NUi.mt(14); fl.leftMargin = NUi.dp(44); body.addView(f, fl);
            }
        }
    }

    View top(JSONArray ss) {
        LinearLayout box = NUi.col(c);
        TextView h = NUi.text(c, "Best matches in your notes", 12.5f, NTheme.muted); box.addView(h);
        for (int i = 0; i < Math.min(3, ss.length()); i++) {
            final int k = i; JSONObject x = ss.optJSONObject(i);
            LinearLayout r = NUi.col(c); r.setPadding(NUi.dp(14), NUi.dp(10), NUi.dp(14), NUi.dp(10)); r.setBackground(NUi.ripple(NUi.round(NTheme.surface, 16, NTheme.line), 16));
            String kind = x.optString("kind"); String kl = kind.equals("entry") ? "Journal" : kind.equals("goal") ? "Goal" : kind.equals("habit") ? "Habit" : "Day plan";
            String when = (kind.equals("entry") || kind.equals("day")) && NDates.valid(x.optString("date")) ? " · " + NDates.fmtDate(x.optString("date")) : "";
            TextView k1 = NBits.meta(c, (kl + when + (x.optString("mood").isEmpty() ? "" : " · " + x.optString("mood"))).toUpperCase(), NTheme.muted); k1.setTextSize(10); r.addView(k1);
            String lb = x.optString("label"); if (lb.length() > 70) lb = lb.substring(0, 69) + "…";
            r.addView(NUi.ell(NUi.body(c, lb, 14.5f, NTheme.text, 700), 2), NUi.mt(2));
            String tx = x.optString("text"); int ci = tx.indexOf(": "); if (ci >= 0) tx = tx.substring(ci + 2); if (tx.length() > 110) tx = tx.substring(0, 109) + "…";
            if (!tx.isEmpty()) r.addView(NUi.ell(NUi.text(c, tx, 12.5f, NTheme.muted), 2), NUi.mt(2));
            NUi.tap(r, new View.OnClickListener() { public void onClick(View v) { openSrc(k); } });
            box.addView(r, NUi.mt(8));
        }
        return box;
    }

    View copyBox(int i, int n) {
        LinearLayout b = NUi.col(c); b.setPadding(NUi.dp(14), NUi.dp(12), NUi.dp(14), NUi.dp(12)); b.setBackground(NUi.round(NTheme.surface, 18, NTheme.line));
        b.addView(NUi.body(c, i > 0 ? "Follow-up prompt ready" : "Your prompt is ready", 15, NTheme.text, 700));
        TextView p = NUi.text(c, i > 0 ? "Paste it into the same chat." : "It holds your question, an overview of your plan and " + n + " matching note" + (n == 1 ? "" : "s") + ". Nothing is sent by Plotline.", 13, NTheme.muted); p.setLineSpacing(0, 1.15f); b.addView(p, NUi.mt(4));
        NFlow f = new NFlow(c, 8, 8);
        f.addView(NUi.btn(c, "Copy prompt", true, new View.OnClickListener() { public void onClick(View v) { sh.run("insCopy", null); } }), new ViewGroup.MarginLayoutParams(-2, NUi.dp(40)));
        if (i == 0) for (final String[] u : new String[][]{{"Claude", "https://claude.ai/new"}, {"ChatGPT", "https://chatgpt.com/"}, {"Gemini", "https://gemini.google.com/app"}})
            f.addView(NUi.btn(c, u[0], false, new View.OnClickListener() { public void onClick(View v) { try { sh.a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u[1]))); } catch (Exception ignored) { } } }), new ViewGroup.MarginLayoutParams(-2, NUi.dp(40)));
        b.addView(f, NUi.mt(10));
        final EditText pe = NForms.input(c, "Paste the reply here to keep it with its sources", paste, true); pe.setMinLines(3);
        pe.addTextChangedListener(new android.text.TextWatcher() { public void beforeTextChanged(CharSequence a, int x, int y, int z) { } public void onTextChanged(CharSequence a, int x, int y, int z) { } public void afterTextChanged(android.text.Editable e) { paste = e.toString(); } });
        b.addView(pe, NUi.mt(10));
        b.addView(NUi.btn(c, "Show reply", false, new View.OnClickListener() { public void onClick(View v) {
            if (paste.trim().isEmpty()) { NShell.toast("Paste the reply first"); return; }
            sh.a.js("window.__naskpaste&&window.__naskpaste(" + JSONObject.quote(paste) + ")"); paste = ""; sig = "";
        } }), NUi.mt(10));
        return b;
    }

    /* ---- sources ---- */
    void openSrc(int k) {
        JSONArray ss = d == null ? null : d.optJSONArray("srcs"); if (ss == null) return;
        JSONObject x = ss.optJSONObject(k); if (x == null) return;
        String kind = x.optString("kind"), ref = x.optString("ref");
        sh.closeSheet();
        if (kind.equals("entry")) { if (st.find("entries", ref) != null) NEng.viewEntry(sh, ref); }
        else if (kind.equals("goal")) sh.route("goal/" + ref);
        else if (kind.equals("habit")) sh.route("habit/" + ref);
        else if (kind.equals("day")) sh.push(new NDayScreen(sh, ref));
    }

    void sources() {
        JSONArray ss = d == null ? null : d.optJSONArray("srcs"); if (ss == null) return;
        LinearLayout b = new NForms(sh).sheetBody("SOURCES", "Where the answer came from", "Keyword search over your notes, plus an overview of your plan.");
        for (int i = 0; i < ss.length(); i++) {
            final int k = i; JSONObject x = ss.optJSONObject(i); String kind = x.optString("kind");
            String kl = kind.equals("entry") ? "Journal" : kind.equals("goal") ? "Goal" : kind.equals("habit") ? "Habit" : "Day plan";
            String when = (kind.equals("entry") || kind.equals("day")) && NDates.valid(x.optString("date")) ? " · " + NDates.fmtDate(x.optString("date")) : "";
            LinearLayout r = NUi.row(c); r.setPadding(0, NUi.dp(10), 0, NUi.dp(10));
            TextView num = NUi.body(c, String.valueOf(i + 1), 13, NTheme.accent, 700); num.setGravity(Gravity.CENTER); num.setBackground(NUi.round(NTheme.alpha(NTheme.accent, .16f), 10, 0));
            r.addView(num, NUi.lp(NUi.dp(28), NUi.dp(28)));
            LinearLayout tx = NUi.col(c); tx.setPadding(NUi.dp(12), 0, 0, 0);
            String lb = x.optString("label"); if (lb.length() > 70) lb = lb.substring(0, 69) + "…";
            tx.addView(NUi.ell(NUi.body(c, lb, 14.5f, NTheme.text, 600), 2)); tx.addView(NUi.text(c, kl + when, 12, NTheme.muted));
            r.addView(tx, NUi.lpw(0, -2, 1));
            NUi.tap(r, new View.OnClickListener() { public void onClick(View v) { openSrc(k); } });
            b.addView(r);
        }
        sh.sheet(b);
    }
}
