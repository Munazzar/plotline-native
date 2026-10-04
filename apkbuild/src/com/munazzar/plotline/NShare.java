package com.munazzar.plotline;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/* 2.0.8: sharing, natively: invites (accept / later / decline / join), the live group view, reactions (picker,
   quick messages, banner, floating emoji), the shared-item panel on goals and habits, and the Today rows.
   The engine still does the encryption and network calls (hooks __nsh* in scripts/v110.js). */
final class NShare {
    private NShare() { }

    static String q(String s) { return JSONObject.quote(s == null ? "" : s); }

    /* poll an async engine result (window.__nsr[key]) */
    static void await(final NShell sh, final String key, final int n, final NEng.Got g) {
        sh.root.postDelayed(new Runnable() { public void run() {
            sh.a.jsRet("window.__nsrGet(" + q(key) + ")", new android.webkit.ValueCallback<String>() { public void onReceiveValue(String v) {
                String t = NSheets.unq(v);
                if (t == null || t.isEmpty() || t.equals("null")) { if (n < 60) await(sh, key, n + 1, g); else NShell.toast("That took too long. Try again"); return; }
                try { g.on(new JSONObject(t)); } catch (Exception e) { NCrash.log(sh.a, "share " + key, e); }
            } });
        } }, n == 0 ? 250 : 400);
    }

    static TextView ava(Context c, String s, boolean on) {
        TextView a = NUi.body(c, s, 16, on ? NTheme.onAccent : NTheme.text, 800); a.setGravity(Gravity.CENTER);
        a.setBackground(NUi.round(on ? NTheme.accent : NTheme.surface2, 99, NTheme.line2));
        return a;
    }

    static LinearLayout row(Context c, String ava, String title, String sub, View right) {
        LinearLayout r = NUi.row(c); r.setGravity(Gravity.CENTER_VERTICAL); r.setPadding(0, NUi.dp(8), 0, NUi.dp(8));
        r.addView(ava(c, ava, false), NUi.lp(NUi.dp(40), NUi.dp(40)));
        LinearLayout tx = NUi.col(c); tx.setPadding(NUi.dp(12), 0, NUi.dp(8), 0);
        tx.addView(NUi.ell(NUi.body(c, title, 15, NTheme.text, 700), 2));
        if (sub != null && !sub.isEmpty()) tx.addView(NUi.ell(NUi.text(c, sub, 12.5f, NTheme.muted), 2), NUi.mt(2));
        r.addView(tx, NUi.lpw(0, -2, 1));
        if (right != null) r.addView(right);
        return r;
    }

    static View lockBox(Context c, String title, String text) {
        LinearLayout w = NUi.row(c); w.setBackground(NUi.round(NTheme.surface, 16, NTheme.line2)); w.setPadding(NUi.dp(12), NUi.dp(12), NUi.dp(12), NUi.dp(12));
        w.addView(NUi.text(c, "🔒", 16, NTheme.text), NUi.lp(NUi.dp(28), -2));
        LinearLayout t = NUi.col(c); t.addView(NUi.body(c, title, 14.5f, NTheme.text, 700));
        TextView s = NUi.text(c, text, 12.5f, NTheme.muted); s.setLineSpacing(0, 1.18f); t.addView(s, NUi.mt(3));
        w.addView(t, NUi.lpw(0, -2, 1));
        return w;
    }

    static View danger(Context c, String label, View.OnClickListener l) { TextView b = NUi.btn(c, label, false, l); b.setTextColor(NTheme.LATE); return b; }

    static View flowBtns(Context c, View... vs) {
        NFlow f = new NFlow(c, 8, 8);
        for (View v : vs) if (v != null) f.addView(v, new ViewGroup.MarginLayoutParams(-2, NUi.dp(44)));
        LinearLayout w = NUi.row(c); w.setGravity(Gravity.END); w.addView(f);
        LinearLayout.LayoutParams l = NUi.mt(18); w.setLayoutParams(l);
        return w;
    }

    static View loading(Context c, String kicker, String title, String what) {
        LinearLayout b = NUi.col(c);
        b.addView(NUi.label(c, kicker, NTheme.accent));
        TextView t = NUi.title(c, title, 28); t.setPadding(0, NUi.dp(6), 0, 0); b.addView(t);
        b.addView(NUi.text(c, what, 13.5f, NTheme.muted), NUi.mt(8));
        return b;
    }

    static void error(final NShell sh, String title, String msg) {
        Context c = sh.a; LinearLayout b = NUi.col(c);
        b.addView(NUi.title(c, title, 28));
        TextView t = NUi.text(c, msg, 14.5f, NTheme.muted); t.setPadding(0, NUi.dp(8), 0, 0); b.addView(t);
        b.addView(flowBtns(c, NUi.btn(c, "Close", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } })));
        sh.sheet(b);
    }

    /* ---------- How it works (web shHow) ---------- */
    static void how(final NShell sh) {
        sh.a.jsRet("window.__nshhow()", new android.webkit.ValueCallback<String>() { public void onReceiveValue(String v) {
            Context c = sh.a; LinearLayout b = NUi.col(c);
            b.addView(NUi.title(c, "How sharing works", 28));
            TextView t = NUi.text(c, NSheets.unq(v), 14, NTheme.text); t.setLineSpacing(0, 1.28f); b.addView(t, NUi.mt(12));
            TextView n = NUi.text(c, "To get invites, Google confirms your email to Plotline’s sharing service. You can turn invites off here any time.", 12.5f, NTheme.muted); n.setLineSpacing(0, 1.18f); b.addView(n, NUi.mt(12));
            b.addView(flowBtns(c, NUi.btn(c, "Got it", true, new View.OnClickListener() { public void onClick(View x) { sh.closeSheet(); } })));
            sh.sheet(b);
        } });
    }

    /* ---------- an invite (web shAcceptView / SH_LOCKED) ---------- */
    static void accept(final NShell sh, final String id) {
        sh.sheet(loading(sh.a, "Invite", "Invite", "Loading…"));
        sh.a.js("window.__nshacc(" + q(id) + ")");
        await(sh, "acc:" + id, 0, new NEng.Got() { public void on(JSONObject o) { acceptSheet(sh, id, o); } });
    }

    static void acceptSheet(final NShell sh, final String id, JSONObject o) {
        final Context c = sh.a;
        if (o.has("err")) { error(sh, "Invite not available", o.optString("err")); return; }
        View.OnClickListener decline = new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); sh.run("shDecline", NMore.d("id", id)); } };
        LinearLayout b = NUi.col(c);
        b.addView(NUi.label(c, o.optBoolean("locked") ? "Invite" : o.optString("e") + " " + o.optString("n"), NTheme.accent));
        if (o.optBoolean("locked")) {
            TextView t = NUi.title(c, o.optString("who") + " invited you", 28); t.setPadding(0, NUi.dp(6), 0, 0); b.addView(t);
            String fr = o.optString("from"); TextView s = NUi.text(c, "It’s end-to-end encrypted, so it unlocks once " + (fr.isEmpty() ? "their" : fr) + " Plotline is online and hands your copy of the key over (usually within the hour, nothing for you to do). You’ll see what it is before you join.", 14.5f, NTheme.muted);
            s.setLineSpacing(0, 1.2f); b.addView(s, NUi.mt(8));
            b.addView(flowBtns(c, danger(c, "Decline", decline), NUi.btn(c, "OK", true, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } })));
            sh.sheet(b); return;
        }
        TextView t = NUi.title(c, o.optString("title"), 28); t.setPadding(0, NUi.dp(6), 0, 0); b.addView(t);
        TextView s = NUi.text(c, o.optString("ownerName") + " (" + o.optString("owner") + ") invited you. " + o.optString("x"), 14.5f, NTheme.muted); s.setLineSpacing(0, 1.2f); b.addView(s, NUi.mt(8));
        JSONArray st = o.optJSONArray("steps");
        if (st != null && st.length() > 0) b.addView(stepList(c, st, false), NUi.mt(12));
        int mine = o.optInt("mine");
        b.addView(lockBox(c, "If you join", "Plotline adds “" + o.optString("title") + "” to your plan" + (mine > 0 ? " with the " + mine + " step" + (mine == 1 ? "" : "s") + " assigned to you" : "") + ". Your name, progress, streak and check-in days for this one item sync live to the people in it, end-to-end encrypted. Nothing else from your plan is shared, and you can leave any time."), NUi.mt(14));
        b.addView(howLink(sh), NUi.mt(6));
        final TextView join = NUi.btn(c, "Join", true, null);
        join.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
            join.setEnabled(false); join.setText("Joining…");
            sh.a.js("window.__nshjoin(" + q(id) + ")");
            await(sh, "join:" + id, 0, new NEng.Got() { public void on(JSONObject r) { if (r.optInt("ok") == 1) sh.closeSheet(); else { join.setEnabled(true); join.setText("Join"); } } });
        } });
        b.addView(flowBtns(c, danger(c, "Decline", decline), NUi.btn(c, "Later", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); sh.run("shLater", NMore.d("id", id)); } }), join));
        sh.sheet(b);
    }

    static View howLink(final NShell sh) {
        return NUi.link(sh.a, "How it works  ›", new View.OnClickListener() { public void onClick(View v) {
            sh.a.jsRet("window.__nshhow()", new android.webkit.ValueCallback<String>() { public void onReceiveValue(String x) {
                if (v.getParent() instanceof LinearLayout) { LinearLayout p = (LinearLayout) v.getParent(); int i = p.indexOfChild(v); TextView t = NUi.text(sh.a, NSheets.unq(x), 13, NTheme.text); t.setLineSpacing(0, 1.25f); p.removeView(v); p.addView(t, i, NUi.mt(6)); }
            } });
        } });
    }

    static View stepList(Context c, JSONArray st, boolean group) {
        LinearLayout l = NUi.col(c); l.setBackground(NUi.round(NTheme.surface, 16, NTheme.line2)); l.setPadding(NUi.dp(12), NUi.dp(6), NUi.dp(12), NUi.dp(6));
        for (int i = 0; i < st.length(); i++) {
            JSONObject s = st.optJSONObject(i); boolean me = s.optBoolean("me"), d = s.optBoolean("d");
            LinearLayout r = NUi.row(c); r.setGravity(Gravity.CENTER_VERTICAL); r.setPadding(0, NUi.dp(7), 0, NUi.dp(7));
            View dot = new View(c); dot.setBackground(NUi.round(d ? NTheme.accent : me ? NTheme.alpha(NTheme.accent, .45f) : NTheme.line2, 99, 0)); r.addView(dot, NUi.lp(NUi.dp(9), NUi.dp(9)));
            TextView t = NUi.body(c, s.optString("t"), 14, d ? NTheme.muted : NTheme.text, me ? 700 : 500); t.setPadding(NUi.dp(10), 0, NUi.dp(8), 0);
            if (d) t.setPaintFlags(t.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            r.addView(t, NUi.lpw(0, -2, 1));
            String w = group ? s.optString("who") : s.optString("to");
            if (!w.isEmpty()) r.addView(NUi.label(c, w, me ? NTheme.accent : NTheme.muted));
            l.addView(r);
        }
        return l;
    }

    /* ---------- the group view (web shOpen / shGroupHTML), live every 6 s ---------- */
    static final class Group {
        final NShell sh; final String id; final LinearLayout body; boolean open = true, first = true; String sig = "";
        final Handler hd = new Handler(Looper.getMainLooper());
        Group(NShell sh, String id) { this.sh = sh; this.id = id; this.body = NUi.col(sh.a); }
        void start(String title) {
            body.addView(loading(sh.a, "Shared", title, "Loading the group…"));
            sh.sheet(body);
            sh.a.js("window.__nshgopen(" + q(id) + ")");
            sh.onSheetClose = new Runnable() { public void run() { open = false; hd.removeCallbacksAndMessages(null); sh.a.js("window.__nshgopen('')"); } };
            load();
        }
        void load() {
            if (!open) return;
            sh.a.js("window.__nshgrp(" + q(id) + ")");
            await(sh, "grp:" + id, 0, new NEng.Got() { public void on(JSONObject o) {
                if (!open) return;
                if (o.has("err")) { if (first) { open = false; error(sh, o.optString("title", "Shared"), o.optString("err")); } return; }
                first = false;
                String s = o.toString(); if (!s.equals(sig)) { sig = s; draw(o); }
                hd.postDelayed(new Runnable() { public void run() { load(); } }, 6000);
            } });
        }
        void draw(final JSONObject o) {
            final Context c = sh.a; body.removeAllViews();
            body.addView(NUi.label(c, o.optString("e") + " " + o.optString("n") + " · 🔒 live", NTheme.accent));
            TextView t = NUi.title(c, o.optString("title"), 28); t.setPadding(0, NUi.dp(6), 0, 0); body.addView(t);
            boolean habit = "habit".equals(o.optString("kind"));
            if ("together".equals(o.optString("mode"))) {
                LinearLayout tot = NUi.row(c); tot.setGravity(Gravity.BOTTOM);
                TextView n = NUi.text(c, o.optInt("tot") + (habit ? "" : "%"), 40, NTheme.accent); n.setTypeface(NFont.display(800)); tot.addView(n);
                TextView l = NUi.text(c, habit ? "check-ins together this week" : "done together", 13.5f, NTheme.muted); l.setPadding(NUi.dp(10), 0, 0, NUi.dp(8)); tot.addView(l);
                body.addView(tot, NUi.mt(10));
            }
            JSONArray rows = o.optJSONArray("rows");
            LinearLayout list = NUi.col(c);
            for (int i = 0; rows != null && i < rows.length(); i++) {
                final JSONObject m = rows.optJSONObject(i);
                LinearLayout r = NUi.row(c); r.setGravity(Gravity.CENTER_VERTICAL); r.setPadding(NUi.dp(10), NUi.dp(10), NUi.dp(10), NUi.dp(10));
                if (m.optBoolean("mine")) r.setBackground(NUi.round(NTheme.alpha(NTheme.accent, .08f), 16, NTheme.alpha(NTheme.accent, .3f)));
                r.addView(ava(c, m.optString("ava"), m.optBoolean("mine")), NUi.lp(NUi.dp(40), NUi.dp(40)));
                LinearLayout tx = NUi.col(c); tx.setPadding(NUi.dp(12), 0, NUi.dp(8), 0);
                tx.addView(NUi.ell(NUi.body(c, m.optString("name"), 15, NTheme.text, 700), 1));
                tx.addView(NUi.ell(NUi.text(c, m.optString("sub"), 12.5f, NTheme.muted), 2), NUi.mt(2));
                JSONArray wk = m.optJSONArray("wk");
                if (wk != null) { LinearLayout w = NUi.row(c); for (int k = 0; k < wk.length(); k++) { View d = new View(c); d.setBackground(NUi.round(wk.optBoolean(k) ? NTheme.accent : NTheme.line2, 3, 0)); LinearLayout.LayoutParams dl = NUi.lp(NUi.dp(14), NUi.dp(8)); dl.rightMargin = NUi.dp(4); w.addView(d, dl); } tx.addView(w, NUi.mt(6)); }
                else tx.addView(NEng.bar(c, m.optInt("pct"), NTheme.accent), NUi.mt(6));
                r.addView(tx, NUi.lpw(0, -2, 1));
                if (!m.optBoolean("mine")) r.addView(NUi.btn(c, "👏 React", false, new View.OnClickListener() { public void onClick(View v) { react(sh, id, m.optString("email"), m.optString("nm"), true); } }), NUi.lp(-2, NUi.dp(40)));
                list.addView(r, NUi.mt(i == 0 ? 0 : 6));
            }
            JSONArray pend = o.optJSONArray("pending");
            for (int i = 0; pend != null && i < pend.length(); i++) { JSONObject p = pend.optJSONObject(i); View r = row(c, p.optString("ava"), p.optString("e"), p.optString("sub"), null); r.setAlpha(.7f); r.setPadding(NUi.dp(10), NUi.dp(6), NUi.dp(10), NUi.dp(6)); list.addView(r); }
            body.addView(list, NUi.mt(14));
            JSONArray feed = o.optJSONArray("feed");
            if (feed == null || feed.length() == 0) { TextView n = NUi.text(c, "No reactions yet. Tap React next to someone to cheer them on.", 12.5f, NTheme.muted); body.addView(n, NUi.mt(14)); }
            else {
                LinearLayout f = NUi.col(c); f.setBackground(NUi.round(NTheme.surface, 16, NTheme.line2)); f.setPadding(NUi.dp(12), NUi.dp(10), NUi.dp(12), NUi.dp(10));
                f.addView(NUi.label(c, "Reactions · live", NTheme.muted));
                for (int i = 0; i < feed.length(); i++) {
                    JSONObject r = feed.optJSONObject(i);
                    LinearLayout x = NUi.row(c); x.setGravity(Gravity.CENTER_VERTICAL); x.setPadding(0, NUi.dp(6), 0, 0);
                    x.addView(NUi.text(c, r.optString("e"), 20, NTheme.text), NUi.lp(NUi.dp(32), -2));
                    LinearLayout tx = NUi.col(c); tx.addView(NUi.body(c, r.optString("who"), 13.5f, NTheme.text, 700)); if (!r.optString("m").isEmpty()) tx.addView(NUi.text(c, r.optString("m"), 13, NTheme.text));
                    x.addView(tx, NUi.lpw(0, -2, 1));
                    x.addView(NUi.text(c, r.optString("when"), 11.5f, NTheme.muted));
                    f.addView(x);
                }
                body.addView(f, NUi.mt(14));
            }
            JSONArray st = o.optJSONArray("steps");
            if (st != null && st.length() > 0) {
                final View sl = stepList(c, st, true); sl.setVisibility(View.GONE);
                final TextView tg = NUi.link(c, "Steps and who’s on them  ▾", null);
                tg.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { boolean on = sl.getVisibility() != View.VISIBLE; sl.setVisibility(on ? View.VISIBLE : View.GONE); tg.setText(on ? "Steps and who’s on them  ▴" : "Steps and who’s on them  ▾"); } });
                body.addView(tg, NUi.mt(10)); body.addView(sl, NUi.mt(4));
            }
            boolean owner = "owner".equals(o.optString("role"));
            View a1 = owner ? danger(c, "Stop sharing", new View.OnClickListener() { public void onClick(View v) { sh.run("shStop", NMore.d("id", id)); } })
                : danger(c, "Leave", new View.OnClickListener() { public void onClick(View v) { sh.run("shLeave", NMore.d("id", id)); } });
            View a2 = owner ? NUi.btn(c, "Invite more", false, new View.OnClickListener() { public void onClick(View v) { more(sh, id); } }) : null;
            body.addView(flowBtns(c, a1, a2, NUi.btn(c, "Done", true, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } })));
        }
    }

    static void group(NShell sh, String id) {
        JSONObject x = sh.st.find("shares", id); if (x == null) return;
        new Group(sh, id).start(x.optString("title"));
    }

    /* ---------- invite more people (web shInvite / shMore) ---------- */
    static void more(final NShell sh, final String id) {
        final Context c = sh.a; LinearLayout b = NUi.col(c);
        b.addView(NUi.title(c, "Invite more people", 28));
        b.addView(NForms.fieldLabel(c, "Their Google email"));
        final EditText em = NForms.input(c, "address@gmail.com", "", false); em.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS); b.addView(em);
        final TextView go = NUi.btn(c, "Invite", true, null);
        go.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
            if (em.getText().toString().trim().isEmpty()) { em.setError("Add an email"); return; }
            go.setEnabled(false); go.setText("Inviting…");
            sh.a.js("window.__nshmore(" + q(id) + "," + q(em.getText().toString()) + ")");
            await(sh, "more:" + id, 0, new NEng.Got() { public void on(JSONObject r) {
                if (r.has("err")) { go.setEnabled(true); go.setText("Invite"); NShell.toast(r.optString("err")); return; }
                NSheets.sent(sh, r);
            } });
        } });
        b.addView(flowBtns(c, NUi.btn(c, "Cancel", false, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } }), go));
        sh.sheet(b); NForms.focus(em);
    }

    /* ---------- the reaction picker (web shReactSheet) ---------- */
    static void react(final NShell sh, final String id, final String to, final String nm, final boolean back) {
        sh.a.jsRet("window.__nshrx()", new android.webkit.ValueCallback<String>() { public void onReceiveValue(String v) {
            try { reactSheet(sh, id, to, nm, back, new JSONObject(NSheets.unq(v))); } catch (Exception e) { NCrash.log(sh.a, "react", e); }
        } });
    }

    static void reactSheet(final NShell sh, final String id, final String to, final String nm, final boolean back, JSONObject o) {
        final Context c = sh.a; JSONObject x = sh.st.find("shares", id);
        final int left = o.optInt("left"), max = o.optInt("max", 50);
        LinearLayout b = NUi.col(c);
        b.addView(NUi.label(c, modeEmoji(x) + " " + (x == null ? "" : x.optString("title")), NTheme.accent));
        TextView t = NUi.title(c, "React to " + nm, 28); t.setPadding(0, NUi.dp(6), 0, 0); b.addView(t);
        LinearLayout er = NUi.row(c); JSONArray emo = o.optJSONArray("emo");
        for (int i = 0; emo != null && i < emo.length(); i++) {
            final String e = emo.optString(i);
            TextView eb = NUi.text(c, e, 28, NTheme.text); eb.setGravity(Gravity.CENTER); eb.setBackground(NUi.ripple(NUi.round(NTheme.surface, 18, NTheme.line2), 18)); eb.setEnabled(left > 0); eb.setAlpha(left > 0 ? 1f : .4f);
            NUi.tap(eb, new View.OnClickListener() { public void onClick(View v) { send(sh, id, to, nm, e, "", "", back); } });
            LinearLayout.LayoutParams l = NUi.lpw(0, NUi.dp(56), 1); if (i > 0) l.leftMargin = NUi.dp(8); er.addView(eb, l);
        }
        b.addView(er, NUi.mt(16));
        LinearLayout own = NUi.row(c); own.setGravity(Gravity.CENTER_VERTICAL);
        final EditText m = NForms.input(c, "Type your own message…", "", false); m.setEnabled(left > 0);
        m.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(120)});
        own.addView(m, NUi.lpw(0, -2, 1));
        LinearLayout.LayoutParams sl = NUi.lp(NUi.dp(52), NUi.dp(52)); sl.leftMargin = NUi.dp(8);
        View sendB = NUi.btn(c, "➤", true, new View.OnClickListener() { public void onClick(View v) { String s = m.getText().toString().trim(); if (s.isEmpty()) return; send(sh, id, to, nm, "", "", s, back); } }); sendB.setEnabled(left > 0);
        own.addView(sendB, sl);
        b.addView(own, NUi.mt(12));
        b.addView(NUi.label(c, "Quick messages", NTheme.muted), NUi.mt(16));
        NFlow qf = new NFlow(c, 8, 8); JSONArray qs = o.optJSONArray("quick");
        for (int i = 0; qs != null && i < qs.length(); i++) { final int k = i; TextView qb = NUi.chip(c, qs.optString(i), false, new View.OnClickListener() { public void onClick(View v) { send(sh, id, to, nm, "", String.valueOf(k), "", back); } }); qb.setEnabled(left > 0); qb.setAlpha(left > 0 ? 1f : .4f); qf.addView(qb, new ViewGroup.MarginLayoutParams(-2, -2)); }
        b.addView(qf, NUi.mt(8));
        b.addView(NUi.text(c, left > 0 ? left + " of " + max + " left today" : "That’s " + max + " for today. Reactions open again tomorrow.", 12.5f, NTheme.muted), NUi.mt(12));
        b.addView(flowBtns(c, NUi.btn(c, "Edit messages", false, new View.OnClickListener() { public void onClick(View v) { quick(sh, new Runnable() { public void run() { react(sh, id, to, nm, back); } }); } }),
            NUi.btn(c, "Open the group", true, new View.OnClickListener() { public void onClick(View v) { group(sh, id); } })));
        sh.sheet(b);
    }

    static String modeEmoji(JSONObject x) {
        String m = x == null ? "" : x.optString("mode");
        return m.equals("compete") ? "🏁" : m.equals("watch") ? "👀" : m.equals("together") ? "🤝" : "";
    }

    static void send(final NShell sh, final String id, String to, final String nm, String e, String i, String m, final boolean back) {
        sh.a.js("window.__nshreact(" + q(id) + "," + q(to) + "," + q(e) + "," + q(i) + "," + q(m) + ")");
        await(sh, "rx", 0, new NEng.Got() { public void on(JSONObject r) {
            if (r.has("err")) { NShell.toast(r.optString("err")); return; }
            if (r.optInt("ok") != 1) return;
            NShell.toast("Sent to " + nm + " " + (!r.optString("emoji").isEmpty() ? r.optString("emoji") : "· “" + r.optString("m") + "”"));
            if (back) group(sh, id); else sh.closeSheet();
        } });
    }

    /* panel quick react: one emoji, no sheet (web shQuick) */
    static void quickSend(final NShell sh, String id, String to, final String nm, final String e, final View btn) {
        if (btn != null) { btn.animate().scaleX(1.3f).scaleY(1.3f).setDuration(120).withEndAction(new Runnable() { public void run() { btn.animate().scaleX(1f).scaleY(1f).setDuration(180).start(); } }).start(); }
        sh.a.js("window.__nshreact(" + q(id) + "," + q(to) + "," + q(e) + ",''," + q("") + ")");
        await(sh, "rx", 0, new NEng.Got() { public void on(JSONObject r) {
            if (r.has("err")) { NShell.toast(r.optString("err")); return; }
            if (r.optInt("ok") == 1) { NShell.toast("Sent " + r.optString("emoji") + " to " + nm); }
        } });
    }

    /* ---------- quick messages (web shQuickForm) ---------- */
    static void quick(final NShell sh, final Runnable back) {
        sh.a.jsRet("window.__nshrx()", new android.webkit.ValueCallback<String>() { public void onReceiveValue(String v) {
            try {
                JSONObject o = new JSONObject(NSheets.unq(v)); final Context c = sh.a; JSONArray q = o.optJSONArray("quick"), d = o.optJSONArray("defs");
                LinearLayout b = NUi.col(c);
                b.addView(NUi.title(c, "Quick messages", 28));
                TextView s = NUi.text(c, "Short messages you can send with one tap in a shared item. Up to 8, 40 characters each. You can also type your own message each time.", 13.5f, NTheme.muted); s.setLineSpacing(0, 1.18f); s.setPadding(0, NUi.dp(6), 0, 0); b.addView(s);
                final List<EditText> ins = new ArrayList<>();
                for (int i = 0; i < 8; i++) {
                    EditText e = NForms.input(c, d != null && i < d.length() ? d.optString(i) : "Add one", q != null && i < q.length() ? q.optString(i) : "", false);
                    e.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(40)});
                    ins.add(e); b.addView(e, NUi.mt(i == 0 ? 14 : 8));
                }
                b.addView(flowBtns(c,
                    NUi.btn(c, "Reset to defaults", false, new View.OnClickListener() { public void onClick(View v) { sh.a.js("window.__nshquick('')"); NShell.toast("Back to the defaults"); sh.root.postDelayed(new Runnable() { public void run() { quick(sh, back); } }, 150); } }),
                    NUi.btn(c, "Save", true, new View.OnClickListener() { public void onClick(View v) {
                        JSONArray out = new JSONArray(); java.util.Set<String> seen = new java.util.LinkedHashSet<>();
                        for (EditText e : ins) { String t = e.getText().toString().replaceAll("\\s+", " ").trim(); if (t.length() > 40) t = t.substring(0, 40); if (!t.isEmpty()) seen.add(t); }
                        for (String t : seen) out.put(t);
                        sh.a.js("window.__nshquick(" + q(out.toString()) + ")"); NShell.toast("Quick messages saved");
                        if (back != null) sh.root.postDelayed(back, 150); else sh.closeSheet();
                    } })));
                sh.sheet(b);
            } catch (Exception e) { NCrash.log(sh.a, "quick", e); }
        } });
    }

    /* ---------- Settings → Reactions panel data ---------- */
    static void reactions(final NShell sh, final LinearLayout into, final View.OnClickListener edit) {
        sh.a.jsRet("window.__nshrx()", new android.webkit.ValueCallback<String>() { public void onReceiveValue(String v) {
            try {
                JSONObject o = new JSONObject(NSheets.unq(v)); Context c = sh.a; JSONArray q = o.optJSONArray("quick");
                NFlow f = new NFlow(c, 6, 6);
                for (int i = 0; q != null && i < q.length(); i++) { TextView t = NUi.body(c, q.optString(i), 13, NTheme.text, 600); t.setPadding(NUi.dp(12), NUi.dp(6), NUi.dp(12), NUi.dp(6)); t.setBackground(NUi.round(NTheme.surface2, 99, NTheme.line2)); f.addView(t, new ViewGroup.MarginLayoutParams(-2, -2)); }
                into.addView(f, NUi.mt(12));
                LinearLayout r = NUi.row(c); r.setGravity(Gravity.CENTER_VERTICAL);
                r.addView(NUi.btn(c, "Edit quick messages", false, edit));
                TextView l = NUi.text(c, Math.max(0, o.optInt("left")) + " of " + o.optInt("max") + " left today", 12.5f, NTheme.muted); l.setPadding(NUi.dp(12), 0, 0, 0); r.addView(l);
                into.addView(r, NUi.mt(12));
            } catch (Exception e) { NCrash.log(sh.a, "reactions", e); }
        } });
    }

    /* ---------- the shared-item panel under a goal / habit hero (web shPanel) + the Shared chip ---------- */
    static void panel(final NShell sh, final String kind, final String id, final LinearLayout into, final Handler live) {
        sh.a.jsRet("window.__nshpanel(" + q(kind) + "," + q(id) + ")", new android.webkit.ValueCallback<String>() { public void onReceiveValue(String v) {
            String t = NSheets.unq(v); into.removeAllViews();
            if (t == null || t.isEmpty()) { into.setVisibility(View.GONE); return; }
            try { drawPanel(sh, kind, new JSONObject(t), into); into.setVisibility(View.VISIBLE); } catch (Exception e) { into.setVisibility(View.GONE); return; }
            if (live != null) { live.removeCallbacksAndMessages(null); live.postDelayed(new Runnable() { public void run() { if (into.isAttachedToWindow()) panel(sh, kind, id, into, live); } }, 4500); }
        } });
    }

    static void drawPanel(final NShell sh, String kind, final JSONObject o, LinearLayout p) {
        final Context c = sh.a; final String cid = o.optString("id");
        p.setBackground(NUi.round(NTheme.alpha(NTheme.accent, .07f), 22, NTheme.alpha(NTheme.accent, .35f))); p.setPadding(NUi.dp(14), NUi.dp(12), NUi.dp(14), NUi.dp(12));
        LinearLayout h = NUi.row(c); h.setGravity(Gravity.CENTER_VERTICAL);
        TextView ht = NUi.text(c, "", 14, NTheme.text); ht.setText(android.text.Html.fromHtml(o.optString("e") + " <b>Shared</b> · " + android.text.TextUtils.htmlEncode(o.optString("n")) + (o.optInt("unread") > 0 ? "  •" + o.optInt("unread") : ""), android.text.Html.FROM_HTML_MODE_LEGACY));
        h.addView(ht, NUi.lpw(0, -2, 1));
        h.addView(NUi.btn(c, "Group ›", false, new View.OnClickListener() { public void onClick(View v) { group(sh, cid); } }), NUi.lp(-2, NUi.dp(38)));
        p.addView(h);
        if (o.optInt("panel") != 1) return;
        if (!o.optBoolean("loaded")) { p.addView(NUi.text(c, "Loading who’s in it…", 12.5f, NTheme.muted), NUi.mt(8)); return; }
        JSONArray others = o.optJSONArray("others");
        if (others == null || others.length() == 0) { p.addView(NUi.text(c, o.optBoolean("owner") ? "Nobody has joined yet. Once they do, you’ll see their progress here and can react with one tap." : "Waiting for the others.", 12.5f, NTheme.muted), NUi.mt(8)); return; }
        final int left = o.optInt("left"); JSONArray emo = o.optJSONArray("emo");
        for (int i = 0; i < others.length(); i++) {
            final JSONObject m = others.optJSONObject(i);
            LinearLayout who = NUi.row(c); who.setGravity(Gravity.CENTER_VERTICAL);
            who.addView(ava(c, m.optString("ava"), false), NUi.lp(NUi.dp(36), NUi.dp(36)));
            LinearLayout tx = NUi.col(c); tx.setPadding(NUi.dp(10), 0, 0, 0);
            tx.addView(NUi.body(c, m.optString("nm"), 14.5f, NTheme.text, 700)); tx.addView(NUi.text(c, m.optString("st"), 12.5f, NTheme.muted));
            if (!m.optString("last").isEmpty()) tx.addView(NUi.text(c, m.optString("last"), 12, NTheme.accent));
            who.addView(tx, NUi.lpw(0, -2, 1));
            p.addView(who, NUi.mt(10));
            LinearLayout r = NUi.row(c);
            for (int k = 0; emo != null && k < emo.length(); k++) {
                final String e = emo.optString(k);
                final TextView eb = NUi.text(c, e, 20, NTheme.text); eb.setGravity(Gravity.CENTER); eb.setBackground(NUi.ripple(NUi.round(NTheme.surface, 14, NTheme.line2), 14)); eb.setAlpha(left > 0 ? 1f : .4f);
                if (left > 0) NUi.tap(eb, new View.OnClickListener() { public void onClick(View v) { quickSend(sh, cid, m.optString("email"), m.optString("nm"), e, eb); } });
                LinearLayout.LayoutParams l = NUi.lpw(0, NUi.dp(42), 1); if (k > 0) l.leftMargin = NUi.dp(6); r.addView(eb, l);
            }
            TextView more = NUi.body(c, "💬 More", 13, NTheme.text, 700); more.setGravity(Gravity.CENTER); more.setBackground(NUi.ripple(NUi.round(NTheme.surface, 14, NTheme.line2), 14)); more.setAlpha(left > 0 ? 1f : .4f);
            if (left > 0) NUi.tap(more, new View.OnClickListener() { public void onClick(View v) { react(sh, cid, m.optString("email"), m.optString("nm"), false); } });
            LinearLayout.LayoutParams ml = NUi.lpw(0, NUi.dp(42), 1.4f); ml.leftMargin = NUi.dp(6); r.addView(more, ml);
            p.addView(r, NUi.mt(8));
        }
        p.addView(NUi.text(c, left > 0 ? "Tap to send · " + left + " left today" : "That’s " + o.optInt("max") + " for today", 12, NTheme.muted), NUi.mt(10));
    }

    /* ---------- Today: invites, unread reactions, the allow card (web shTodayCard) ---------- */
    static void today(final NShell sh, final LinearLayout into) {
        sh.a.jsRet("window.__nshtoday()", new android.webkit.ValueCallback<String>() { public void onReceiveValue(String v) {
            into.removeAllViews(); String t = NSheets.unq(v);
            if (t == null || t.isEmpty()) { into.setVisibility(View.GONE); return; }
            try {
                JSONObject o = new JSONObject(t); Context c = sh.a; into.setVisibility(View.VISIBLE);
                into.setBackground(NUi.card(22)); into.setPadding(NUi.dp(14), NUi.dp(8), NUi.dp(14), NUi.dp(8));
                JSONArray inv = o.optJSONArray("inv");
                for (int i = 0; inv != null && i < inv.length(); i++) {
                    final JSONObject x = inv.optJSONObject(i); final String id = x.optString("id");
                    LinearLayout acts = NUi.row(c);
                    acts.addView(NUi.btn(c, "View", true, new View.OnClickListener() { public void onClick(View v) { accept(sh, id); } }), NUi.lp(-2, NUi.dp(38)));
                    if (!x.optBoolean("later")) { LinearLayout.LayoutParams l = NUi.lp(-2, NUi.dp(38)); l.leftMargin = NUi.dp(6); acts.addView(NUi.btn(c, "Later", false, new View.OnClickListener() { public void onClick(View v) { sh.run("shLater", NMore.d("id", id)); } }), l); }
                    LinearLayout bx = NUi.col(c);
                    bx.addView(row(c, x.optString("ava"), x.optString("who") + " invited you", x.optBoolean("locked") ? "🔒 " + x.optString("from") + " · unlocking…" : x.optString("e") + " " + x.optString("m") + " · “" + x.optString("t") + "”", null));
                    LinearLayout.LayoutParams al = NUi.mt(2); al.leftMargin = NUi.dp(52); bx.addView(acts, al);
                    into.addView(bx, NUi.mt(6));
                }
                JSONArray rx = o.optJSONArray("rx");
                for (int i = 0; rx != null && i < rx.length(); i++) {
                    final JSONObject x = rx.optJSONObject(i);
                    into.addView(row(c, x.optString("e"), x.optString("t"), x.optString("sub"), NUi.btn(c, "Open", true, new View.OnClickListener() { public void onClick(View v) { group(sh, x.optString("id")); } })), NUi.mt(6));
                }
                if (o.optBoolean("allow")) {
                    LinearLayout w = NUi.col(c); w.setPadding(NUi.dp(4), NUi.dp(10), NUi.dp(4), NUi.dp(8));
                    w.addView(NUi.body(c, "🔒  Get invites from people you know", 14.5f, NTheme.text, 700));
                    TextView s = NUi.text(c, "Google needs to confirm your email address to Plotline once, so invites sent to it reach you. Plotline sees nothing else.", 12.5f, NTheme.muted); s.setLineSpacing(0, 1.15f); w.addView(s, NUi.mt(4));
                    LinearLayout bs = NUi.row(c);
                    bs.addView(NUi.btn(c, "Allow", true, new View.OnClickListener() { public void onClick(View v) { sh.run("shAllow", null); } }), NUi.lp(-2, NUi.dp(38)));
                    if (!o.optBoolean("allowHide")) { LinearLayout.LayoutParams l = NUi.lp(-2, NUi.dp(38)); l.leftMargin = NUi.dp(6); bs.addView(NUi.btn(c, "Not now", false, new View.OnClickListener() { public void onClick(View v) { sh.run("shAllowHide", null); } }), l); }
                    w.addView(bs, NUi.mt(8));
                    into.addView(w);
                }
            } catch (Exception e) { into.setVisibility(View.GONE); }
        } });
    }

    /* ---------- reaction banner + floating emoji (web reactShow / reactBurst) ---------- */
    static View banner;

    static void banner(final NShell sh, final JSONObject o) {
        final FrameLayout root = sh.root;
        if (banner != null) { final View old = banner; banner = null; old.animate().translationY(-NUi.dp(160)).alpha(0f).setDuration(260).withEndAction(new Runnable() { public void run() { root.removeView(old); } }).start(); }
        if (!o.has("cid")) return;
        final Context c = sh.a; final String cid = o.optString("cid");
        LinearLayout b = NUi.row(c); b.setGravity(Gravity.CENTER_VERTICAL);
        b.setBackground(NUi.round(NTheme.surface, 22, NTheme.accent)); b.setElevation(NUi.dp(14)); b.setPadding(NUi.dp(8), NUi.dp(8), NUi.dp(8), NUi.dp(8));
        TextView e = NUi.text(c, o.optString("e"), 28, NTheme.text); e.setGravity(Gravity.CENTER); e.setBackground(NUi.round(NTheme.alpha(NTheme.accent, .16f), 15, 0));
        b.addView(e, NUi.lp(NUi.dp(46), NUi.dp(46)));
        LinearLayout tx = NUi.col(c); tx.setPadding(NUi.dp(10), 0, NUi.dp(6), 0);
        tx.addView(NUi.body(c, o.optString("from"), 14, NTheme.text, 700));
        tx.addView(NUi.ell(NUi.text(c, o.optString("m"), 14, NTheme.text), 1));
        if (!o.optString("sub").isEmpty()) tx.addView(NUi.ell(NUi.text(c, o.optString("sub"), 12, NTheme.muted), 1));
        b.addView(tx, NUi.lpw(0, -2, 1));
        NUi.tap(tx, new View.OnClickListener() { public void onClick(View v) { sh.a.js("RB.q=[];reactShow()"); group(sh, cid); } });
        NUi.tap(e, new View.OnClickListener() { public void onClick(View v) { sh.a.js("RB.q=[];reactShow()"); group(sh, cid); } });
        b.addView(NUi.btn(c, "React", true, new View.OnClickListener() { public void onClick(View v) { sh.a.js("RB.q=[];reactShow()"); react(sh, cid, o.optString("fe"), o.optString("from"), false); } }), NUi.lp(-2, NUi.dp(40)));
        LinearLayout.LayoutParams xl = NUi.lp(NUi.dp(36), NUi.dp(36)); xl.leftMargin = NUi.dp(4);
        TextView xb = NUi.text(c, "✕", 14, NTheme.muted); xb.setGravity(Gravity.CENTER); NUi.tap(xb, new View.OnClickListener() { public void onClick(View v) { sh.a.js("RB.q=[];reactShow()"); } }); b.addView(xb, xl);
        FrameLayout.LayoutParams l = new FrameLayout.LayoutParams(Math.min(NUi.dp(520), c.getResources().getDisplayMetrics().widthPixels - NUi.dp(24)), -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        l.topMargin = sh.top + NUi.dp(10);
        root.addView(b, l); banner = b;
        b.setTranslationY(-NUi.dp(160)); b.setAlpha(0f);
        b.animate().translationY(0).alpha(1f).setDuration(550).setInterpolator(new android.view.animation.OvershootInterpolator(1.1f)).start();
        e.setScaleX(.3f); e.setScaleY(.3f); e.animate().scaleX(1f).scaleY(1f).setDuration(600).setInterpolator(new android.view.animation.OvershootInterpolator(1.6f)).start();
        NFx.vib(c, new long[]{20, 60, 20});
    }

    static void burst(final NShell sh, String emo) {
        if (NFx.reduced(sh)) return;
        final FrameLayout root = sh.root; final Context c = sh.a;
        int w = root.getWidth(), h = root.getHeight();
        for (int i = 0; i < 7; i++) {
            final TextView t = NUi.text(c, emo, 34 * (float) (.8 + Math.random() * .7), NTheme.text);
            root.addView(t, new FrameLayout.LayoutParams(-2, -2));
            t.measure(0, 0);
            final float x0 = w / 2f - t.getMeasuredWidth() / 2f, y0 = h * .82f - t.getMeasuredHeight(), dx = (float) ((Math.random() - .5) * NUi.dp(220)), dy = -h * .46f;
            t.setX(x0); t.setY(y0); t.setAlpha(0f); t.setScaleX(.4f); t.setScaleY(.4f);
            ValueAnimator a = ValueAnimator.ofFloat(0f, 1f); a.setDuration(1800); a.setStartDelay(i * 70L); a.setInterpolator(new android.view.animation.PathInterpolator(.2f, .7f, .3f, 1f));
            a.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { public void onAnimationUpdate(ValueAnimator an) {
                float p = (Float) an.getAnimatedValue(); t.setX(x0 + dx * p); t.setY(y0 + dy * p); float s = .4f + .7f * p; t.setScaleX(s); t.setScaleY(s); t.setRotation(12 * p); t.setAlpha(p < .15f ? p / .15f : 1f - (p - .15f) / .85f);
            } });
            a.addListener(new android.animation.AnimatorListenerAdapter() { @Override public void onAnimationEnd(android.animation.Animator an) { root.removeView(t); } });
            a.start();
        }
    }

    /* ---------- opening an invite from its notification (web shJoinRoute) ---------- */
    static void joinRoute(final NShell sh, final String id) {
        sh.a.js("window.__nshjr(" + q(id) + ")");
        await(sh, "jr:" + id, 0, new NEng.Got() { public void on(JSONObject o) {
            String k = o.optString("k");
            if (k.equals("off")) { sh.push(new NSettings(sh, "share")); return; }
            if (k.equals("acc")) { accept(sh, id); return; }
            if (k.equals("open")) { group(sh, id); return; }
            Context c = sh.a; LinearLayout b = NUi.col(c);
            b.addView(NUi.label(c, "Invite", NTheme.accent));
            TextView t = NUi.title(c, "Invite not found", 28); t.setPadding(0, NUi.dp(6), 0, 0); b.addView(t);
            TextView s = NUi.text(c, "It may have been stopped, or it’s from an older version of Plotline. Ask them to share it again: it will show up here by itself.", 14.5f, NTheme.muted); s.setLineSpacing(0, 1.2f); b.addView(s, NUi.mt(8));
            b.addView(flowBtns(c, NUi.btn(c, "OK", true, new View.OnClickListener() { public void onClick(View v) { sh.closeSheet(); } })));
            sh.sheet(b);
        } });
    }
}
