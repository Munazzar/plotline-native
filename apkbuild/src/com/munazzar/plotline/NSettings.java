package com.munazzar.plotline;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

/* Settings, the web's hub of areas (You & sync · Look & feel · Notifications · Privacy & lock · Automations ·
   Sharing · AI & voice · Your data). Plain settings are written straight to the store; anything that needs the web
   engine (Drive sign-in, PIN, lock proof, backups, automations, sharing) calls the engine's own action. */
final class NSettings extends NPage {
    final String sec;
    JSONObject eng;
    boolean asked;

    static final String[][] SECS = {
        {"account", "You & sync", "link"}, {"look", "Look & feel", "tune"}, {"notif", "Notifications", "bell"}, {"privacy", "Privacy & lock", "lock"},
        {"auto", "Automations", "flame"}, {"share", "Sharing", "habit"}, {"voice", "AI & voice", "ai"}, {"data", "Your data", "archive"}};
    static final String[][] FPRE = {{"plotline", "Plotline", "bsd", "figtree", "martian"}, {"studio", "Studio", "oswald", "inter", "jetbrains"}, {"editorial", "Editorial", "instrument", "sserif", "plexmono"},
        {"friendly", "Friendly", "barlowc", "nunito", "dmmono"}, {"geometric", "Geometric", "bebas", "outfit", "spacemono"}, {"clean", "Clean", "archivon", "manrope", "geistmono"},
        {"readable", "Easy to read", "sofiac", "atkinson", "plexmono"}, {"bold", "Bold", "anton", "jakarta", "fira"}, {"classic", "Classic", "league", "lora", "plexmono"}, {"tech", "Tech", "antonio", "grotesk", "jetbrains"}};
    static final String[][] CARDS = {{"solid", "Solid"}, {"glow", "Glow"}, {"neon", "Neon"}, {"aurora", "Aurora"}, {"glass", "Glass"}, {"index", "Index card"}};
    static final String[][] BGS = {{"plain", "Plain"}, {"soft", "Soft light"}, {"grid", "Grid"}, {"dots", "Dots"}, {"lines", "Lines"}, {"waves", "Waves"}, {"topo", "Contours"}, {"plus", "Plus"}, {"image", "Your image"}};
    static final String[][] HALOS = {{"off", "Off"}, {"high", "High priority"}, {"pinned", "Pinned"}, {"next", "Up next"}, {"all", "All"}};

    NSettings(NShell sh, String sec) { super(sh); this.sec = sec; }

    /* ---------- store helpers ---------- */
    JSONObject s() { return st.settings(); }
    JSONObject obj(String k) { JSONObject o = s().optJSONObject(k); if (o == null) { o = new JSONObject(); try { s().put(k, o); } catch (Exception ignored) { } } return o; }
    void put(String k, Object v) { try { s().put(k, v); } catch (Exception ignored) { } sh.save(); }
    void putIn(String o, String k, Object v) { try { obj(o).put(k, v); } catch (Exception ignored) { } sh.save(); }
    boolean nb(String k, boolean def) { return obj("notif").optBoolean(k, def); }

    /* ---------- widgets ---------- */
    static final class Sw extends View {
        boolean on; final android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        Sw(android.content.Context c, boolean on) { super(c); this.on = on; }
        @Override protected void onMeasure(int w, int h) { setMeasuredDimension(NUi.dp(48), NUi.dp(30)); }
        @Override protected void onDraw(android.graphics.Canvas cv) {
            float w = getWidth(), h = getHeight(); p.setStyle(android.graphics.Paint.Style.FILL);
            p.setColor(on ? NTheme.accent : NTheme.line2); cv.drawRoundRect(0, 0, w, h, h / 2, h / 2, p);
            if (!on) { p.setStyle(android.graphics.Paint.Style.STROKE); p.setStrokeWidth(NUi.dp(1)); p.setColor(NTheme.line); cv.drawRoundRect(NUi.dp(.5f), NUi.dp(.5f), w - NUi.dp(.5f), h - NUi.dp(.5f), h / 2, h / 2, p); p.setStyle(android.graphics.Paint.Style.FILL); }
            p.setColor(on ? NTheme.onAccent : NTheme.muted); cv.drawCircle(on ? w - h / 2 : h / 2, h / 2, h / 2 - NUi.dp(4), p);
        }
    }

    interface Chg { void on(boolean v); }

    LinearLayout panel(String title, String sub) {
        /* web .panel: radius 26, padding 22; h3 display 700 24px uppercase, 14px below */
        LinearLayout p = NUi.col(c); p.setBackground(NUi.card(26)); p.setPadding(NUi.dp(22), NUi.dp(22), NUi.dp(22), NUi.dp(22));
        if (title != null) p.setTag(title);
        if (title != null) { TextView h = NUi.title(c, title, 24); h.setTypeface(NFont.display(700)); h.setAllCaps(true); NUi.cssLh(h, 1f); p.addView(h); }
        if (sub != null) { TextView t = NUi.text(c, sub, 13.5f, NTheme.muted); t.setLineSpacing(0, 1.2f); p.addView(t, NUi.mt(title != null ? 14 : 0)); }
        return p;
    }

    void sw(LinearLayout p, String title, String sub, boolean on, final Chg ch) {
        LinearLayout r = NUi.row(c); r.setPadding(0, NUi.dp(12), 0, NUi.dp(4));
        LinearLayout tx = NUi.col(c); tx.addView(NUi.body(c, title, 15.5f, NTheme.text, 600));
        if (sub != null && !sub.isEmpty()) { TextView t = NUi.text(c, sub, 12.5f, NTheme.muted); t.setLineSpacing(0, 1.15f); tx.addView(t, NUi.mt(2)); }
        r.addView(tx, NUi.lpw(0, -2, 1));
        final Sw w = new Sw(c, on); r.addView(w, NUi.lp(NUi.dp(48), NUi.dp(30)));
        NUi.tap(r, new View.OnClickListener() { public void onClick(View v) { NUi.haptic(v); w.on = !w.on; w.invalidate(); ch.on(w.on); } });
        p.addView(r);
    }

    interface Pk { void on(String k); }

    void chips(LinearLayout p, String[][] opts, String cur, final Pk pk, int top) {
        NFlow f = new NFlow(c, 8, 8);
        for (final String[] o : opts) {
            ViewGroup.MarginLayoutParams l = new ViewGroup.MarginLayoutParams(-2, NUi.dp(40));
            f.addView(NUi.chip(c, o[1], o[0].equals(cur), new View.OnClickListener() { public void onClick(View v) { pk.on(o[0]); } }), l);
        }
        p.addView(f, NUi.mt(top));
    }

    void fieldLabel(LinearLayout p, String t) { TextView l = NUi.label(c, t, NTheme.muted); l.setPadding(0, NUi.dp(16), 0, NUi.dp(8)); p.addView(l); }

    View btn(String label, boolean pri, View.OnClickListener l) { return NUi.btn(c, label, pri, l); }

    LinearLayout btns(View... bs) {
        NFlow f = new NFlow(c, 8, 8); for (View b : bs) f.addView(b, new ViewGroup.MarginLayoutParams(-2, NUi.dp(46)));
        LinearLayout w = NUi.col(c); w.addView(f); return w;
    }

    void kv(LinearLayout p, String k, String v) {
        LinearLayout r = NUi.row(c); r.setPadding(0, NUi.dp(12), 0, NUi.dp(4));
        r.addView(NUi.text(c, k, 14, NTheme.muted), NUi.lpw(0, -2, 1));
        TextView t = NUi.body(c, v, 14, NTheme.text, 600); t.setGravity(Gravity.END); r.addView(t, NUi.lp(-2, -2));
        p.addView(r);
    }

    static String hhmm(int h, int m) { return String.format(java.util.Locale.US, "%02d:%02d", h, m); }

    void timeRow(LinearLayout p, String label, final String key, String def) {
        final String cur = obj("notif").optString(key, def);
        LinearLayout r = NUi.row(c); r.setPadding(0, NUi.dp(12), 0, NUi.dp(4));
        r.addView(NUi.body(c, label, 15, NTheme.text, 600), NUi.lpw(0, -2, 1));
        TextView t = NUi.body(c, NDates.fmtTime(cur), 15, NTheme.accent, 700); r.addView(t);
        NUi.tap(r, new View.OnClickListener() { public void onClick(View v) {
            int h = 0, m = 0; try { h = Integer.parseInt(cur.substring(0, 2)); m = Integer.parseInt(cur.substring(3, 5)); } catch (Exception ignored) { }
            new TimePickerDialog(sh.a, new TimePickerDialog.OnTimeSetListener() { public void onTimeSet(android.widget.TimePicker tp, int hh, int mm) { putIn("notif", key, hhmm(hh, mm)); refresh(); } }, h, m, false).show();
        } });
        p.addView(r);
    }

    void engineAsk() {
        if (asked) return; asked = true;
        sh.a.jsRet("window.__nsub&&window.__nsub()", new android.webkit.ValueCallback<String>() { @Override public void onReceiveValue(String v) {
            try { String j = v; if (j != null && j.startsWith("\"")) j = new JSONObject("{\"x\":" + j + "}").getString("x"); JSONObject o = j == null || j.equals("null") ? null : new JSONObject(j);
                if (o != null && (eng == null || !o.toString().equals(eng.toString()))) { eng = o; if (sh.stack.contains(NSettings.this)) refresh(); } } catch (Exception ignored) { }
            asked = false;
        } });
    }

    /* ---------- hub ---------- */
    String sub(String k) {
        JSONObject set = s();
        switch (k) {
            case "account": { JSONObject sy = set.optJSONObject("sync"); boolean on = sy != null && sy.optBoolean("on"); if (!on) return "Not syncing · connect Google Drive"; return "auth".equals(sy.optString("err")) ? "Sign in again to keep syncing" : "Syncing with " + (sy.optString("email").isEmpty() ? "Google Drive" : sy.optString("email")); }
            case "look": { String t = set.optString("theme", "night"), nm = "Midnight"; for (int i = 0; i < NGen.THEME_ID.length; i++) if (NGen.THEME_ID[i].equals(t)) nm = NGen.THEME_NAME[i]; return nm + " theme" + ("reduced".equals(set.optString("motion")) ? " · reduced motion" : ""); }
            case "notif": {
                long mu = Notify.muteUntil(sh.a.getApplicationContext()); if (mu > System.currentTimeMillis()) return "Muted until " + NDates.fmtClock(mu);
                int on = 0; for (String x : new String[]{"habits", "steps", "days", "checkins", "wrap"}) if (nb(x, true)) on++;
                return on + " of 5 kinds on" + (nb("quiet", false) ? " · quiet " + NDates.fmtTime(obj("notif").optString("qFrom", "22:30")) + "–" + NDates.fmtTime(obj("notif").optString("qTo", "07:00")) : "");
            }
            case "privacy": if (eng != null && eng.has("lock")) return eng.optString("lock"); { JSONObject l = set.optJSONObject("lock"); boolean on = l != null && l.optBoolean("on"); return !on && set.optString("pinHash").isEmpty() ? "Off · anyone with this device can open Plotline" : "On"; }
            case "auto": return eng != null && eng.has("auto") ? eng.optString("auto") : "Checks things off by themselves";
            case "share": return eng != null && eng.has("share") ? eng.optString("share") : "Goals and habits with friends";
            case "voice": return eng != null && eng.has("voice") ? eng.optString("voice") : "Assistant and mic";
            default: return "Backup, import, sample data";
        }
    }

    @Override void build() {
        engineAsk();
        if (sec == null) { hub(); return; }
        String title = ""; for (String[] x : SECS) if (x[0].equals(sec)) title = x[1];
        header(title);
        TextView sb = NBits.meta(c, sub(sec).toUpperCase(), NTheme.muted); sb.setTextSize(10.5f); add(sb, 28);
        LinearLayout col = NUi.col(c); add(col, 30);
        switch (sec) {
            case "account": account(col); break;
            case "look": look(col); break;
            case "notif": notif(col); break;
            case "privacy": privacy(col); break;
            case "auto": autoSection(col); break;
            case "share": shareSection(col); break;
            case "voice": voice(col); break;
            default: data(col); break;
        }
        fold(col);
    }

    /* web 1.13 foldSettings: when an area has two or more titled sections, each folds to its title (first one open);
       the choice is remembered in settings.fold under "<area>:<title>", the same keys the web app uses */
    void fold(LinearLayout col) {
        java.util.List<LinearLayout> P = new java.util.ArrayList<>();
        for (int i = 0; i < col.getChildCount(); i++) { View v = col.getChildAt(i); if (v instanceof LinearLayout && v.getTag() instanceof String && ((LinearLayout) v).getChildCount() > 0) P.add((LinearLayout) v); }
        if (P.size() < 2) return;
        JSONObject F = s().optJSONObject("fold");
        for (int k = 0; k < P.size(); k++) {
            final LinearLayout p = P.get(k); final String key = sec + ":" + ((String) p.getTag()).trim().substring(0, Math.min(40, ((String) p.getTag()).trim().length()));
            boolean open = F != null && F.has(key) ? F.optBoolean(key) : k == 0;
            final View h = p.getChildAt(0); p.removeViewAt(0);
            final LinearLayout body = NUi.col(c); body.setPadding(0, NUi.dp(12), 0, 0);
            while (p.getChildCount() > 0) { View v = p.getChildAt(0); p.removeViewAt(0); body.addView(v); }
            if (body.getChildCount() > 0 && body.getChildAt(0).getLayoutParams() instanceof LinearLayout.LayoutParams) ((LinearLayout.LayoutParams) body.getChildAt(0).getLayoutParams()).topMargin = 0;
            LinearLayout hr = NUi.row(c); hr.addView(h, NUi.lpw(0, -2, 1));
            final View chev = NUi.icon(c, "chev", 20, NTheme.text); chev.setAlpha(.6f); chev.setRotation(open ? 180 : 0); hr.addView(chev, NUi.lp(NUi.dp(20), NUi.dp(20)));
            p.addView(hr); p.addView(body); body.setVisibility(open ? View.VISIBLE : View.GONE);
            NUi.tap(hr, new View.OnClickListener() { public void onClick(View v) {
                final boolean o = body.getVisibility() != View.VISIBLE;
                chev.animate().rotation(o ? 180 : 0).setDuration(300).start();
                NFx.expand(body, o);
                try { JSONObject f = s().optJSONObject("fold"); if (f == null) { f = new JSONObject(); s().put("fold", f); } f.put(key, o); } catch (Exception ignored) { }
                sh.saveQuiet();
            } });
        }
    }

    void hub() {
        header("Settings");
        TextView sb = NBits.meta(c, "EVERYTHING IN ONE PLACE, ONE AREA AT A TIME", NTheme.muted); sb.setTextSize(10.5f); add(sb, 28);
        /* web .shub: one card per area (gap 8, radius 20), accent icon on a bg-2 tile, title 15/650, one-line sub, chevron */
        for (int i = 0; i < SECS.length; i++) {
            final String[] x = SECS[i];
            boolean warn = x[0].equals("account") && "auth".equals(obj("sync").optString("err"));
            LinearLayout r = NUi.row(c); r.setGravity(Gravity.CENTER_VERTICAL); r.setPadding(NUi.dp(16), NUi.dp(14), NUi.dp(16), NUi.dp(14));
            r.setBackground(NUi.ripple(NUi.round(NTheme.surface, 20, warn ? NTheme.alpha(0xFFFF7A7A, .5f) : NTheme.line), 20));
            android.widget.ImageView ic = NUi.icon(c, x[2], 19, NTheme.accent);
            LinearLayout ib = new LinearLayout(c); ib.setGravity(Gravity.CENTER); ib.setBackground(NUi.round(NTheme.bg2, 12, 0)); ib.addView(ic, NUi.lp(NUi.dp(19), NUi.dp(19)));
            r.addView(ib, NUi.lp(NUi.dp(38), NUi.dp(38)));
            LinearLayout tx = NUi.col(c);
            tx.addView(NUi.body(c, x[1], 15, NTheme.text, 600));
            TextView sub = NUi.ell(NUi.text(c, sub(x[0]), 12.5f, warn ? 0xFFFF9A9A : NTheme.muted), 1); tx.addView(sub, NUi.mt(2));
            LinearLayout.LayoutParams tl = NUi.lpw(0, -2, 1); tl.leftMargin = NUi.dp(14); tl.rightMargin = NUi.dp(14); r.addView(tx, tl);
            r.addView(NUi.icon(c, "next", 16, NTheme.muted), NUi.lp(NUi.dp(16), NUi.dp(16)));
            NUi.tap(r, new View.OnClickListener() { public void onClick(View v) { sh.push(new NSettings(sh, x[0])); } });
            add(r, i == 0 ? 18 : 8);
        }
        /* footer: version · Privacy policy · Take the tour · Setup checklist */
        android.text.SpannableStringBuilder f = new android.text.SpannableStringBuilder("Plotline " + ver() + " · ");
        link(f, "Privacy policy", new Runnable() { public void run() {
            try { sh.a.startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://munazzar.github.io/plotline/privacy.html"))); } catch (Exception ignored) { }
        } });
        f.append(" · "); link(f, "Take the tour", new Runnable() { public void run() { NTour.start(sh); } });
        f.append(" · "); link(f, "Setup checklist", new Runnable() { public void run() { new NWelcome(sh).setup(); } });
        TextView ft = NUi.text(c, f, 13, NTheme.muted); ft.setMovementMethod(android.text.method.LinkMovementMethod.getInstance()); ft.setHighlightColor(0); ft.setLineSpacing(0, 1.3f);
        add(ft, 18);
    }

    String ver() { try { return sh.a.getPackageManager().getPackageInfo(sh.a.getPackageName(), 0).versionName; } catch (Exception e) { return ""; } }

    void link(android.text.SpannableStringBuilder sb, String t, final Runnable r) {
        int a = sb.length(); sb.append(t);
        sb.setSpan(new android.text.style.ClickableSpan() {
            @Override public void onClick(View v) { r.run(); }
            @Override public void updateDrawState(android.text.TextPaint tp) { tp.setColor(NTheme.accent); tp.setUnderlineText(false); tp.setFakeBoldText(true); tp.setTextSize(NUi.sp(12)); }
        }, a, sb.length(), 0);
    }

    /* ---------- You & sync ---------- */
    void account(LinearLayout col) {
        JSONObject sy = obj("sync"); final boolean on = sy.optBoolean("on");
        LinearLayout p = panel("Sync", on ? null : "Keep your phone and the web app in step through a private, hidden folder in your own Google Drive. No servers. Only Plotline can see the file.");
        if (on) {
            kv(p, "Account", sy.optString("email").isEmpty() ? "Google Drive" : sy.optString("email"));
            String msg = eng != null ? eng.optString("msg") : "";
            kv(p, "Status", !msg.isEmpty() ? msg : "auth".equals(sy.optString("err")) ? "Sign in again to keep syncing" : sy.optLong("last") > 0 ? "Last synced " + NDates.ago(sy.optLong("last")) : "Waiting to sync");
            p.addView(btns(btn("Sync now", true, new View.OnClickListener() { public void onClick(View v) { sh.run("syncNow", null); NShell.toast("Syncing…"); } }),
                btn("Turn off sync", false, new View.OnClickListener() { public void onClick(View v) { NEng.syncOff(sh); } })), NUi.mt(14));
        } else p.addView(btns(btn("Connect Google Drive", true, new View.OnClickListener() { public void onClick(View v) { sh.run("syncConnect", null); } })), NUi.mt(14));
        col.addView(p);
        LinearLayout y = panel("You", null);
        y.addView(btns(btn("🏁  Take the tour", false, new View.OnClickListener() { public void onClick(View v) { NTour.start(sh); } })), NUi.mt(12));
        fieldLabel(y, "Your name");
        final android.widget.EditText nm = NForms.input(c, "What should we call you?", s().optString("name"), false);
        nm.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence a, int b, int d, int e) { }
            public void onTextChanged(CharSequence a, int b, int d, int e) { }
            public void afterTextChanged(android.text.Editable e) { try { s().put("name", e.toString()); } catch (Exception ignored) { } sh.saveQuiet(); }
        });
        y.addView(nm);
        col.addView(y, NUi.mt(14));
    }

    /* ---------- Look & feel ---------- */
    void look(LinearLayout col) {
        LinearLayout th = panel("Theme", null);
        for (int g = 0; g < 2; g++) {
            final boolean light = g == 1;
            TextView gl = NBits.meta(c, light ? "LIGHT" : "DARK", NTheme.muted); gl.setTextSize(10.5f); gl.setPadding(0, NUi.dp(light ? 18 : 4), 0, NUi.dp(10)); th.addView(gl);
            GridLayout gr = new GridLayout(c); gr.setColumnCount(3);
            int k = 0;
            for (int i = 0; i < NGen.THEME_ID.length; i++) {
                if (NGen.THEME_LIGHT[i] != light) continue;
                final String id = NGen.THEME_ID[i]; int[] cc = NGen.THEME_COL[i]; boolean on = id.equals(s().optString("theme", "night"));
                /* web .tcard (phone): border 1.5 line-2, radius 16, padding 6/6/9; on = accent border + 3px accent ring */
                LinearLayout t = NUi.col(c); t.setPadding(NUi.dp(6), NUi.dp(6), NUi.dp(6), NUi.dp(9));
                android.graphics.drawable.GradientDrawable cb = new android.graphics.drawable.GradientDrawable(); cb.setCornerRadius(NUi.dp(16)); cb.setStroke(NUi.dp(1.5f), on ? NTheme.accent : NTheme.line2);
                if (on) { android.graphics.drawable.GradientDrawable ring = new android.graphics.drawable.GradientDrawable(); ring.setCornerRadius(NUi.dp(19)); ring.setStroke(NUi.dp(3), (NTheme.accent & 0xFFFFFF) | 0x38000000);
                    android.graphics.drawable.LayerDrawable ld = new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{ring, cb}); ld.setLayerInset(1, NUi.dp(3), NUi.dp(3), NUi.dp(3), NUi.dp(3)); t.setBackground(ld); t.setPadding(NUi.dp(9), NUi.dp(9), NUi.dp(9), NUi.dp(12)); }
                else t.setBackground(cb);
                /* .tprev: bg, three 14px squares top-left, surface bar (left 10, right 40, bottom 10, h 26), 24px accent dot */
                android.widget.FrameLayout pv = new android.widget.FrameLayout(c); pv.setBackground(NUi.round(cc[0], 11, 0)); pv.setClipToOutline(true);
                LinearLayout dots = NUi.row(c); int[] ds = {0xFF46C99B, 0xFF6B9BFF, 0xFFFF8C6B};
                for (int d : ds) { View dv = new View(c); dv.setBackground(NUi.round(d, 4, 0)); LinearLayout.LayoutParams dl = NUi.lp(NUi.dp(14), NUi.dp(14)); dl.rightMargin = NUi.dp(4); dots.addView(dv, dl); }
                android.widget.FrameLayout.LayoutParams dlp = new android.widget.FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START); dlp.setMargins(NUi.dp(10), NUi.dp(10), 0, 0); pv.addView(dots, dlp);
                View bar = new View(c); bar.setBackground(NUi.round(light ? cc[2] : cc[3], 8, 0)); android.widget.FrameLayout.LayoutParams bl = new android.widget.FrameLayout.LayoutParams(-1, NUi.dp(26), Gravity.BOTTOM); bl.setMargins(NUi.dp(10), 0, NUi.dp(40), NUi.dp(10)); pv.addView(bar, bl);
                View ac = new View(c); ac.setBackground(NUi.oval(cc[9], 0, 0)); android.widget.FrameLayout.LayoutParams al = new android.widget.FrameLayout.LayoutParams(NUi.dp(24), NUi.dp(24), Gravity.BOTTOM | Gravity.END); al.setMargins(0, 0, NUi.dp(10), NUi.dp(10)); pv.addView(ac, al);
                t.addView(pv, NUi.lp(-1, NUi.dp(62)));
                TextView n = NUi.body(c, NGen.THEME_NAME[i], 12.5f, NTheme.text, 700); n.setSingleLine(true); n.setEllipsize(android.text.TextUtils.TruncateAt.END); n.setPadding(NUi.dp(4), 0, NUi.dp(4), 0); t.addView(n, NUi.mt(7));
                NUi.tap(t, new View.OnClickListener() { public void onClick(View v) { put("theme", id); refresh(); } });
                GridLayout.LayoutParams l = new GridLayout.LayoutParams(GridLayout.spec(k / 3), GridLayout.spec(k % 3, 1f)); l.width = 0;
                l.setMargins(k % 3 == 0 ? 0 : NUi.dp(4.5f), k >= 3 ? NUi.dp(9) : 0, k % 3 == 2 ? 0 : NUi.dp(4.5f), 0);
                gr.addView(t, l); k++;
            }
            while (k % 3 != 0) { View sp = new View(c); GridLayout.LayoutParams l = new GridLayout.LayoutParams(GridLayout.spec(k / 3), GridLayout.spec(k % 3, 1f)); l.width = 0; l.height = 1; gr.addView(sp, l); k++; }
            th.addView(gr, NUi.lp(-1, -2));
        }
        sw(th, "Match my device", "Use Daylight when your device is in light mode", s().optBoolean("matchSystem"), new Chg() { public void on(boolean v) { put("matchSystem", v); refresh(); } });
        sw(th, "Reduce motion", "Swap flips, slides and page morphs for simple fades", "reduced".equals(s().optString("motion")), new Chg() { public void on(boolean v) { put("motion", v ? "reduced" : "full"); } });
        col.addView(th);

        LinearLayout hm = panel("Home page", "What Today opens with.");
        String home = "studio".equals(s().optString("home")) ? "studio" : "classic";
        chips(hm, new String[][]{{"classic", "Classic · cards and lists"}, {"studio", "Studio · live room"}}, home, new Pk() { public void on(String k) { NStudio.setHome(sh, k); } }, 12);
        col.addView(hm, NUi.mt(14));

        fonts(col);

        LinearLayout cs = panel("Card style", null);
        GridLayout g1 = new GridLayout(c); g1.setColumnCount(2);
        for (final String[] o : CARDS) {
            LinearLayout t = NUi.col(c); t.setPadding(NUi.dp(4), NUi.dp(8), NUi.dp(4), NUi.dp(4));
            t.addView(miniCard(o[0], NTheme.cardBg, 0xFF46C99B, "Health", "07", "Run a 10K race", o[0].equals(s().optString("cards", "solid"))), NUi.lp(-1, NUi.dp(104)));
            TextView n = NUi.body(c, o[1], 13, NTheme.text, 600); n.setPadding(0, NUi.dp(6), 0, 0); t.addView(n);
            NUi.tap(t, new View.OnClickListener() { public void onClick(View v) { put("cards", o[0]); refresh(); } });
            GridLayout.LayoutParams l = new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f)); l.width = 0; g1.addView(t, l);
        }
        cs.addView(g1, NUi.mt(8));
        col.addView(cs, NUi.mt(14));

        LinearLayout bg = panel("Card background", null);
        GridLayout g2 = new GridLayout(c); g2.setColumnCount(3);
        for (final String[] o : BGS) {
            LinearLayout t = NUi.col(c); t.setPadding(NUi.dp(4), NUi.dp(8), NUi.dp(4), NUi.dp(4));
            t.addView(miniCard(NTheme.cards, o[0], 0xFF6B9BFF, "Learning", "03", o[0].equals("image") && s().optString("cardImg").isEmpty() ? "Upload an image" : "Read 12 books", o[0].equals(s().optString("cardBg", "plain"))), NUi.lp(-1, NUi.dp(92)));
            TextView n = NUi.body(c, o[1], 12.5f, NTheme.text, 600); n.setPadding(0, NUi.dp(6), 0, 0); t.addView(n);
            NUi.tap(t, new View.OnClickListener() { public void onClick(View v) {
                if (o[0].equals("image") && s().optString("cardImg").isEmpty()) { sh.a.pickImage("cardImg"); return; }
                put("cardBg", o[0]); refresh();
            } });
            GridLayout.LayoutParams l = new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f)); l.width = 0; g2.addView(t, l);
        }
        bg.addView(g2, NUi.mt(8));
        boolean has = !s().optString("cardImg").isEmpty();
        View up = btn(has ? "Change image" : "Upload your own image", false, new View.OnClickListener() { public void onClick(View v) { sh.a.pickImage("cardImg"); } });
        if (has) bg.addView(btns(up, btn("Remove image", false, new View.OnClickListener() { public void onClick(View v) { put("cardImg", ""); if ("image".equals(s().optString("cardBg"))) put("cardBg", "plain"); refresh(); } })), NUi.mt(10));
        else bg.addView(btns(up), NUi.mt(10));
        TextView note = NUi.text(c, "Any goal can also have its own cover image. Add it from the goal’s edit screen.", 13, NTheme.muted); note.setPadding(0, NUi.dp(10), 0, 0); bg.addView(note);
        col.addView(bg, NUi.mt(14));

        LinearLayout ha = panel("Halo", "A colored glow around cards, map points and timeline tracks.");
        fieldLabel(ha, "Show halo on");
        chips(ha, HALOS, s().optString("halo", "high"), new Pk() { public void on(String k) { put("halo", k); refresh(); } }, 0);
        sw(ha, "Bright halo", "A wider, stronger glow", s().optBoolean("haloBright"), new Chg() { public void on(boolean v) { put("haloBright", v); } });
        sw(ha, "Pulse", "The halo breathes slowly", s().optBoolean("haloPulse"), new Chg() { public void on(boolean v) { put("haloPulse", v); } });
        col.addView(ha, NUi.mt(14));
    }

    View miniCard(String style, String tex, int col, String area, String num, String title, boolean on) {
        LinearLayout f = NUi.col(c); f.setPadding(NUi.dp(11), NUi.dp(10), NUi.dp(11), NUi.dp(10));
        f.setBackground(new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{NCard.preview(col, 16, style, tex), on ? NUi.round(0, 16, NTheme.accent) : new android.graphics.drawable.ColorDrawable(0)}));
        boolean lite = style.equals("glow") || style.equals("neon") || style.equals("glass");
        int ink = tex.equals("image") && !s().optString("cardImg").isEmpty() ? 0xFFFFFFFF : lite ? NTheme.text : style.equals("index") ? 0xFF141A2C : 0xFF0B1020;
        LinearLayout top = NUi.row(c);
        TextView a = NBits.meta(c, area.toUpperCase(), ink); a.setTextSize(8.5f); a.setAlpha(.7f); top.addView(a, NUi.lpw(0, -2, 1));
        TextView n = NUi.text(c, num, 12, ink); n.setTypeface(NFont.display(800)); top.addView(n);
        f.addView(top);
        f.addView(new View(c), NUi.lpw(-1, 0, 1));
        TextView t = NUi.ell(NUi.body(c, title, 12.5f, ink, 700), 2); f.addView(t);
        View bar = NBits.bar(c, .58f, ink); f.addView(bar, NUi.mt(6));
        return f;
    }

    void fonts(LinearLayout col) {
        JSONObject f = obj("font");
        final String d = f.optString("d", "bsd"), b = f.optString("b", "figtree"), m = f.optString("m", "martian");
        LinearLayout p = panel("Fonts", "Pick a ready-made pair, or choose each one. Everything is built in, so it works offline.");
        String preId = null; for (String[] x : FPRE) if (x[2].equals(d) && x[3].equals(b) && x[4].equals(m)) preId = x[0];
        GridLayout g = new GridLayout(c); g.setColumnCount(2);
        for (final String[] x : FPRE) {
            LinearLayout t = NUi.col(c); t.setPadding(NUi.dp(14), NUi.dp(12), NUi.dp(14), NUi.dp(12));
            t.setBackground(NUi.ripple(NUi.round(NTheme.surface2, 16, x[0].equals(preId) ? NTheme.accent : NTheme.line2), 16));
            TextView h = NUi.text(c, x[1].toUpperCase(), 20, NTheme.text); h.setTypeface(NFont.face(x[2], 800, android.graphics.Typeface.DEFAULT_BOLD)); t.addView(h);
            TextView s2 = NUi.text(c, "Run a 10K race", 13, NTheme.muted); s2.setTypeface(NFont.face(x[3], 500, android.graphics.Typeface.DEFAULT)); t.addView(s2, NUi.mt(2));
            TextView s3 = NUi.text(c, "58% · 3 of 5", 11, NTheme.muted); s3.setTypeface(NFont.face(x[4], 500, android.graphics.Typeface.MONOSPACE)); t.addView(s3, NUi.mt(2));
            NUi.tap(t, new View.OnClickListener() { public void onClick(View v) { try { JSONObject o = obj("font"); o.put("d", x[2]); o.put("b", x[3]); o.put("m", x[4]); } catch (Exception ignored) { } sh.save(); refresh(); } });
            GridLayout.LayoutParams l = new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f)); l.width = 0; l.setMargins(NUi.dp(4), NUi.dp(4), NUi.dp(4), NUi.dp(4)); g.addView(t, l);
        }
        p.addView(g, NUi.mt(10));
        String[][] roles = {{"d", "Headings", d}, {"b", "Text", b}, {"m", "Numbers and labels", m}};
        for (final String[] r : roles) {
            fieldLabel(p, r[1]);
            NFlow fl = new NFlow(c, 8, 8);
            for (final String[] fo : NGen.FONTS) {
                if (!fo[2].equals(r[0])) continue;
                TextView ch = NUi.chip(c, fo[1], fo[0].equals(r[2]), new View.OnClickListener() { public void onClick(View v) { try { obj("font").put(r[0], fo[0]); } catch (Exception ignored) { } sh.save(); refresh(); } });
                ch.setTypeface(NFont.face(fo[0], 600, android.graphics.Typeface.DEFAULT));
                fl.addView(ch, new ViewGroup.MarginLayoutParams(-2, NUi.dp(40)));
            }
            p.addView(fl);
        }
        col.addView(p, NUi.mt(14));
    }

    /* ---------- Notifications ---------- */
    void notif(LinearLayout col) {
        final android.content.Context ctx = sh.a.getApplicationContext();
        LinearLayout r0 = panel("Reminders", "Step reminders, goal check-ins and focus timers arrive as phone notifications, even when the app is closed.");
        boolean granted = ((android.app.NotificationManager) sh.a.getSystemService(android.content.Context.NOTIFICATION_SERVICE)).areNotificationsEnabled();
        View ics = btn("📅  Add due steps to calendar", false, new View.OnClickListener() { public void onClick(View v) { NSheets.ics(sh, "icsAll", "", ""); } });
        if (!granted) r0.addView(btns(ics, btn("🔔  Allow notifications", true, new View.OnClickListener() { public void onClick(View v) { sh.a.askNotifications(); if (android.os.Build.VERSION.SDK_INT >= 33) return; try { sh.a.startActivity(new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, sh.a.getPackageName())); } catch (Exception ignored) { } } })), NUi.mt(12));
        else r0.addView(btns(ics), NUi.mt(12));
        col.addView(r0);

        LinearLayout p = panel("Notifications", "Reply right from the notification bar in your own words: “done”, “mute for 2 days”, “remind me again in 5 hrs”, “in meetings till 4:30”, “push it to Friday”, “skipping today, feeling sick”. The on-device AI reads it and does it on your phone, and every reply can be undone.");
        long mu = Notify.muteUntil(ctx); final boolean muted = mu > System.currentTimeMillis();
        LinearLayout mb = NUi.col(c); mb.setBackground(NUi.round(muted ? NUi.mix(NTheme.accent, .12f, NTheme.surface2) : NTheme.surface2, 16, muted ? NTheme.alpha(NTheme.accent, .5f) : NTheme.line2)); mb.setPadding(NUi.dp(14), NUi.dp(12), NUi.dp(14), NUi.dp(12));
        mb.addView(NUi.body(c, muted ? "Muted until " + NDates.fmtClock(mu) : "Focus mode", 15.5f, NTheme.text, 700));
        TextView ms = NUi.text(c, muted ? "Reminders wait and arrive as one summary when this ends." : "Pause every reminder for a while.", 12.5f, NTheme.muted); mb.addView(ms, NUi.mt(2));
        NFlow mf = new NFlow(c, 8, 8);
        if (muted) mf.addView(NUi.btn(c, "Unmute", false, new View.OnClickListener() { public void onClick(View v) { NShell.toast(Notify.muteTo(ctx, 0)); refresh(); } }), new ViewGroup.MarginLayoutParams(-2, NUi.dp(42)));
        else for (final int[] mm : new int[][]{{60}, {120}, {240}}) mf.addView(NUi.btn(c, mm[0] / 60 + "h", false, new View.OnClickListener() { public void onClick(View v) { NShell.toast(Notify.mute(ctx, mm[0])); refresh(); } }), new ViewGroup.MarginLayoutParams(-2, NUi.dp(42)));
        mb.addView(mf, NUi.mt(10));
        p.addView(mb, NUi.mt(14));
        if (android.os.Build.VERSION.SDK_INT >= 31 && !Reminders.canExact(sh.a.getSystemService(android.app.AlarmManager.class))) {
            LinearLayout ex = NUi.col(c); ex.setBackground(NUi.round(NTheme.surface2, 16, NTheme.line2)); ex.setPadding(NUi.dp(14), NUi.dp(12), NUi.dp(14), NUi.dp(12));
            ex.addView(NUi.body(c, "Allow on-time reminders", 15, NTheme.text, 700));
            TextView et = NUi.text(c, "Android can delay reminders by several minutes until you allow “Alarms & reminders” for Plotline.", 12.5f, NTheme.muted); ex.addView(et, NUi.mt(2));
            ex.addView(btns(btn("Allow", false, new View.OnClickListener() { public void onClick(View v) { sh.run("exactOn", null); } })), NUi.mt(10));
            p.addView(ex, NUi.mt(12));
        }
        sw(p, "Habit reminders", "At the time set on each habit", nb("habits", true), new Chg() { public void on(boolean v) { putIn("notif", "habits", v); } });
        sw(p, "Evening wrap-up", "One list of habits still open", nb("wrap", true), new Chg() { public void on(boolean v) { putIn("notif", "wrap", v); refresh(); } });
        if (nb("wrap", true)) timeRow(p, "Wrap-up time", "wrapAt", "21:00");
        sw(p, "Step reminders", "Steps with a reminder", nb("steps", true), new Chg() { public void on(boolean v) { putIn("notif", "steps", v); } });
        sw(p, "Day goals", "Day goals with a time", nb("days", true), new Chg() { public void on(boolean v) { putIn("notif", "days", v); } });
        sw(p, "Goal check-ins", "Daily or weekly, set on each goal", nb("checkins", true), new Chg() { public void on(boolean v) { putIn("notif", "checkins", v); } });
        sw(p, "Milestones", "Clean-time wins on habits you’re breaking", nb("miles", true), new Chg() { public void on(boolean v) { putIn("notif", "miles", v); } });
        sw(p, "Reply and Done buttons", "Act on reminders without opening the app", nb("reply", true), new Chg() { public void on(boolean v) { putIn("notif", "reply", v); refresh(); } });
        if (nb("reply", true)) sw(p, "Understand replies with on-device AI", "Download a model in Ask settings first. Until then a quick reader handles simple replies", nb("ai", true), new Chg() { public void on(boolean v) { putIn("notif", "ai", v); } });
        LinearLayout mr = NUi.row(c); mr.setPadding(0, NUi.dp(14), 0, NUi.dp(4));
        mr.addView(NUi.body(c, "Mute button length", 15, NTheme.text, 600), NUi.lpw(0, -2, 1));
        final int mv = obj("notif").optInt("mute", 120); final TextView mt2 = NUi.body(c, mv >= 60 ? mv / 60 + (mv == 60 ? " hour" : " hours") : mv + " min", 15, NTheme.accent, 700); mr.addView(mt2);
        NUi.tap(mr, new View.OnClickListener() { public void onClick(View v) {
            NForms.picker(c, "Mute button length", new String[][]{{"30", "30 min"}, {"60", "1 hour"}, {"120", "2 hours"}, {"180", "3 hours"}, {"240", "4 hours"}}, String.valueOf(mv), new NForms.Pick() { public void on(String k) { putIn("notif", "mute", Integer.parseInt(k)); refresh(); } });
        } });
        p.addView(mr);
        sw(p, "Quiet hours", "No reminders overnight", nb("quiet", false), new Chg() { public void on(boolean v) { putIn("notif", "quiet", v); refresh(); } });
        if (nb("quiet", false)) { timeRow(p, "From", "qFrom", "22:30"); timeRow(p, "Until", "qTo", "07:00"); }
        col.addView(p, NUi.mt(14));
    }

    /* ---------- Privacy & lock ---------- */
    void privacy(LinearLayout col) {
        boolean pin = !s().optString("pinHash").isEmpty();
        LinearLayout p = panel("Privacy", "A PIN keeps the app private on a shared device. It never leaves this device.");
        View pol = NUi.link(c, "Read the privacy policy", new View.OnClickListener() { public void onClick(View v) { try { sh.a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://munazzar.github.io/plotline/privacy.html"))); } catch (Exception ignored) { } } });
        p.addView(pol);
        if (pin) p.addView(btns(btn("🔒  Change PIN", false, new View.OnClickListener() { public void onClick(View v) { NSheets.setPin(sh); } }), btn("Remove PIN", false, new View.OnClickListener() { public void onClick(View v) { NSheets.removePin(sh); } })), NUi.mt(8));
        else p.addView(btns(btn("🔒  Set a PIN", false, new View.OnClickListener() { public void onClick(View v) { NSheets.setPin(sh); } })), NUi.mt(8));
        col.addView(p);
        JSONObject l = obj("lock"); boolean on = l.optBoolean("on");
        String bio = "{}"; try { bio = new Bridge(sh.a).bioState(); } catch (Exception ignored) { }
        boolean hasBio = false, any = false; try { JSONObject b = new JSONObject(bio); hasBio = b.optBoolean("bio"); any = b.optBoolean("any"); } catch (Exception ignored) { }
        LinearLayout q = panel("App lock", (hasBio ? "Unlock Plotline with your fingerprint or face. Your phone’s screen lock works as the backup." : any ? "Unlock Plotline with your phone’s screen lock (PIN, pattern or password)." : "Set up a screen lock or fingerprint in your phone’s settings first, then turn this on.") + " It hides your plan from anyone who picks up your device.");
        if (any) sw(q, "Lock Plotline", on ? "On" : "Off", on, new Chg() { public void on(boolean v) { sh.run("nLock", NMore.d("f", "on", "v", v ? "1" : "0")); } });
        if (on) {
            fieldLabel(q, "Lock again after");
            chips(q, new String[][]{{"0", "Right away"}, {"1", "1 min"}, {"5", "5 min"}, {"15", "15 min"}, {"60", "1 hour"}}, String.valueOf(l.optInt("after", 0)), new Pk() { public void on(String k) { sh.run("nLock", NMore.d("f", "after", "v", k)); } }, 0);
            sw(q, "Hide in recent apps", "Blank preview in the app switcher, and no screenshots", l.optBoolean("secure"), new Chg() { public void on(boolean v) { sh.run("nLock", NMore.d("f", "secure", "v", v ? "1" : "0")); } });
            q.addView(btns(btn("🔒  Try it now", false, new View.OnClickListener() { public void onClick(View v) { sh.run("lockTest", null); } })), NUi.mt(12));
        }
        col.addView(q, NUi.mt(14));
    }

    /* ---------- Automations / Sharing bodies: state comes from engine hooks, actions run the engine's own ---------- */
    JSONObject auD, shD; boolean auAsk, shAsk;
    void hook(final String fn, final boolean isAuto) {
        if (isAuto ? auAsk : shAsk) return; if (isAuto) auAsk = true; else shAsk = true;
        sh.a.jsRet("window." + fn + "&&window." + fn + "()", new android.webkit.ValueCallback<String>() { @Override public void onReceiveValue(String v) {
            try { String j = v; if (j != null && j.startsWith("\"")) j = new JSONObject("{\"x\":" + j + "}").getString("x");
                JSONObject o = j == null || j.length() < 3 ? null : new JSONObject(j);
                JSONObject old = isAuto ? auD : shD;
                if (o != null && (old == null || !o.toString().equals(old.toString()))) { if (isAuto) auD = o; else shD = o; if (sh.stack.contains(NSettings.this)) refresh(); } } catch (Exception ignored) { }
            if (isAuto) auAsk = false; else shAsk = false;
        } });
    }
    void later(Runnable r, long ms) { new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(r, ms); }
    View okTag(boolean on, String txt) { TextView t = NUi.body(c, (on ? "✓ " : "") + txt, 13, on ? NTheme.accent : NTheme.muted, 700); return t; }

    void srcRow(LinearLayout p, String title, String sub, View right) {
        LinearLayout r = NUi.row(c); r.setPadding(0, NUi.dp(12), 0, NUi.dp(4));
        LinearLayout tx = NUi.col(c); tx.addView(NUi.body(c, title, 15.5f, NTheme.text, 600)); TextView t = NUi.text(c, sub, 12.5f, NTheme.muted); tx.addView(t, NUi.mt(2));
        r.addView(tx, NUi.lpw(0, -2, 1)); r.addView(right); p.addView(r);
    }

    void perm(final String t) { sh.run("autoPerm", NMore.d("t", t)); later(new Runnable() { public void run() { auAsk = false; hook("__nauto", true); } }, 900); later(new Runnable() { public void run() { auAsk = false; hook("__nauto", true); } }, 3500); }

    void autoSection(LinearLayout col) {
        hook("__nauto", true);
        LinearLayout h = panel("How it works", "Tap ⚡ on a habit, step or day goal and pick what checks it off: steps, a workout, sleep, time at a place, or staying under a screen-time limit. Everything is read on this phone. You get a notification with Undo each time.");
        col.addView(h);
        if (auD == null) { col.addView(panel(null, "Loading…"), NUi.mt(14)); return; }
        if (!auD.optBoolean("native", true)) return;
        JSONObject st = auD.optJSONObject("st"); if (st == null) st = new JSONObject();
        JSONObject steps = st.optJSONObject("steps"), hc = st.optJSONObject("hc"), scr = st.optJSONObject("screen"), loc = st.optJSONObject("loc"), ins = st.optJSONObject("inside");
        LinearLayout p = panel("Sources", null);
        View b1, b2, b3, b4;
        if (steps != null && !steps.optBoolean("ok")) b1 = okTag(false, "Not on this phone");
        else if (steps != null && steps.optBoolean("perm")) b1 = okTag(true, "On");
        else b1 = btn("Allow", false, new View.OnClickListener() { public void onClick(View v) { perm("steps"); } });
        srcRow(p, "👟 Steps", "Phone step counter" + (steps != null && steps.optBoolean("perm") ? " · " + String.format(java.util.Locale.US, "%,d", (long) steps.optDouble("today", 0)) + " today" : ""), b1);
        if (hc != null && !hc.optBoolean("ok")) b2 = okTag(false, "Android 14+");
        else if (hc != null && hc.optBoolean("perm")) b2 = okTag(true, "Connected");
        else b2 = btn("Connect", false, new View.OnClickListener() { public void onClick(View v) { perm("workout"); } });
        srcRow(p, "🏋️ Health Connect", "Workouts, sleep and steps from your fitness apps", b2);
        if (scr != null && scr.optBoolean("perm")) b3 = okTag(true, "On");
        else b3 = btn("Allow", false, new View.OnClickListener() { public void onClick(View v) { perm("screen"); } });
        srcRow(p, "📱 Screen time", "Minutes in apps you choose", b3);
        if (loc != null && loc.optBoolean("perm") && loc.optBoolean("bg")) b4 = okTag(true, "All the time");
        else b4 = btn(loc != null && loc.optBoolean("perm") ? "Allow all the time" : "Allow", false, new View.OnClickListener() { public void onClick(View v) { perm("place"); } });
        srcRow(p, "📍 Location", "Only for your saved places", b4);
        col.addView(p, NUi.mt(14));
        LinearLayout pl = panel("Places", null);
        JSONArray pa = auD.optJSONArray("places");
        if (pa == null || pa.length() == 0) pl.addView(NUi.text(c, "No places yet.", 13.5f, NTheme.muted), NUi.mt(6));
        else for (int i = 0; i < pa.length(); i++) {
            final JSONObject o = pa.optJSONObject(i); if (o == null) continue;
            LinearLayout r = NUi.row(c); r.setPadding(0, NUi.dp(10), 0, NUi.dp(2));
            r.addView(NUi.text(c, o.optString("e", "📍"), 20, NTheme.text), NUi.lp(NUi.dp(36), -2));
            LinearLayout tx = NUi.col(c); tx.addView(NUi.body(c, o.optString("name"), 15, NTheme.text, 700));
            tx.addView(NUi.text(c, o.optInt("r") + " m around it" + (ins != null && ins.optBoolean(o.optString("id")) ? " · you’re here now" : ""), 12.5f, NTheme.muted), NUi.mt(2));
            r.addView(tx, NUi.lpw(0, -2, 1));
            r.addView(NUi.ibtn(c, "trash", new View.OnClickListener() { public void onClick(View v) { NSheets.placeDel(sh, o.optString("id"), new Runnable() { public void run() { later(new Runnable() { public void run() { auAsk = false; hook("__nauto", true); } }, 700); } }); } }));
            pl.addView(r);
        }
        pl.addView(btns(btn("＋  Add the place I’m at now", false, new View.OnClickListener() { public void onClick(View v) { NSheets.placeNew(sh, new Runnable() { public void run() { later(new Runnable() { public void run() { auAsk = false; hook("__nauto", true); } }, 700); } }); } })), NUi.mt(12));
        col.addView(pl, NUi.mt(14));
        LinearLayout u = panel("In use", null);
        JSONArray ra = auD.optJSONArray("rules");
        if (ra == null || ra.length() == 0) u.addView(NUi.text(c, "Nothing yet. Look for ⚡ on a habit or step.", 13.5f, NTheme.muted), NUi.mt(6));
        else for (int i = 0; i < ra.length(); i++) {
            JSONObject o = ra.optJSONObject(i); if (o == null) continue;
            LinearLayout r = NUi.row(c); r.setPadding(0, NUi.dp(10), 0, NUi.dp(2));
            r.addView(NUi.text(c, o.optString("e"), 18, NTheme.text), NUi.lp(NUi.dp(34), -2));
            LinearLayout tx = NUi.col(c); tx.addView(NUi.body(c, o.optString("t"), 15, NTheme.text, 700)); tx.addView(NUi.text(c, o.optString("l"), 12.5f, NTheme.muted), NUi.mt(2));
            r.addView(tx, NUi.lpw(0, -2, 1)); u.addView(r);
        }
        col.addView(u, NUi.mt(14));
    }

    View shRow(String lead, String title, String sub, View right) {
        LinearLayout r = NUi.row(c); r.setPadding(0, NUi.dp(10), 0, NUi.dp(2));
        TextView e = NUi.text(c, lead, 18, NTheme.text); e.setGravity(Gravity.CENTER); r.addView(e, NUi.lp(NUi.dp(38), -2));
        LinearLayout tx = NUi.col(c); tx.addView(NUi.body(c, title, 15, NTheme.text, 700)); if (sub != null) tx.addView(NUi.text(c, sub, 12.5f, NTheme.muted), NUi.mt(2));
        r.addView(tx, NUi.lpw(0, -2, 1)); if (right != null) r.addView(right);
        return r;
    }
    void shAct(String a, final String id, boolean one) {
        if (a.equals("shAccept")) { NShare.accept(sh, id); return; }
        if (a.equals("shOpen")) { NShare.group(sh, id); return; }
        if (a.equals("shRedo")) {
            sh.a.jsRet("window.__nshredo(" + JSONObject.quote(id) + ")", new android.webkit.ValueCallback<String>() { public void onReceiveValue(String v) {
                try { JSONObject L = new JSONObject(NSheets.unq(v)); String k = L.optString("kind"); NSheets.share(sh, k, L.optString("id"), k.equals("step") ? L.optString("g") : null); } catch (Exception ignored) { }
                later(new Runnable() { public void run() { shAsk = false; hook("__nshare", false); } }, 900);
            } });
            return;
        }
        sh.run(a, id == null ? null : NMore.d("id", id)); later(new Runnable() { public void run() { shAsk = false; hook("__nshare", false); } }, 1400);
    }

    void shareSection(LinearLayout col) {
        hook("__nshare", false);
        if (shD == null) { col.addView(panel("Sharing", "Loading…")); return; }
        if (!shD.optBoolean("ok")) { col.addView(panel("Sharing", "Sharing isn’t switched on in this version of Plotline yet.")); return; }
        if (!shD.optBoolean("sync")) {
            LinearLayout p = panel("Sharing", "Sharing uses your Google account, so connect sync first.");
            p.addView(btns(btn("Set up sync", true, new View.OnClickListener() { public void onClick(View v) { sh.push(new NSettings(sh, "account")); } })), NUi.mt(14)); col.addView(p); return;
        }
        boolean on = shD.optBoolean("on");
        LinearLayout p = panel("Sharing", "Everything in Plotline stays in your own Google Drive. When you share one goal, habit or step, only that item goes to Plotline’s sharing space, end-to-end encrypted: only the people in it can read it.");
        sw(p, "Sharing and invites", on ? "Invites sent to " + (shD.optString("me").isEmpty() ? "your email" : shD.optString("me")) + " show up here" : "Off · you won’t get invites", on, new Chg() { public void on(boolean v) { sh.run("nShareOn", NMore.d("v", v ? "1" : "0")); later(new Runnable() { public void run() { shAsk = false; hook("__nshare", false); } }, 1500); } });
        if (on && shD.optBoolean("allow")) {
            LinearLayout w = NUi.col(c); w.setPadding(NUi.dp(12), NUi.dp(12), NUi.dp(12), NUi.dp(12)); w.setBackground(NUi.round(NTheme.line2, 16, 0));
            w.addView(NUi.body(c, "🔒  Get invites from people you know", 14.5f, NTheme.text, 700));
            TextView t = NUi.text(c, "Google needs to confirm your email address to Plotline once, so invites sent to it reach you. Plotline sees nothing else.", 12.5f, NTheme.muted); t.setLineSpacing(0, 1.15f); w.addView(t, NUi.mt(4));
            if (shD.optBoolean("allowHide")) w.addView(btns(btn("Allow", true, new View.OnClickListener() { public void onClick(View v) { shAct("shAllow", null, true); } })), NUi.mt(8));
            else w.addView(btns(btn("Allow", true, new View.OnClickListener() { public void onClick(View v) { shAct("shAllow", null, true); } }), btn("Not now", false, new View.OnClickListener() { public void onClick(View v) { shAct("shAllowHide", null, false); } })), NUi.mt(8));
            p.addView(w, NUi.mt(12));
        }
        View b1 = on ? btn("Check for invites", false, new View.OnClickListener() { public void onClick(View v) { shAct("shRefresh", null, false); NShell.toast("Checking…"); } }) : null;
        View b2 = btn("How it works", false, new View.OnClickListener() { public void onClick(View v) { NShare.how(sh); } });
        p.addView(b1 == null ? btns(b2) : btns(b1, b2), NUi.mt(12));
        col.addView(p);
        if (on) {
            LinearLayout rx = panel("Reactions", "In a shared item, tap React next to someone to send an emoji or one of your quick messages. They see it within seconds while Plotline is open, and as a notification on Android. Up to 50 a day, so it stays a nudge, not a chat.");
            NShare.reactions(sh, rx, new View.OnClickListener() { public void onClick(View v) { NShare.quick(sh, null); } });
            col.addView(rx, NUi.mt(14));
        }
        JSONArray inv = shD.optJSONArray("inv");
        if (inv != null && inv.length() > 0) {
            LinearLayout q = panel("Invites", null);
            for (int i = 0; i < inv.length(); i++) {
                final JSONObject o = inv.optJSONObject(i); if (o == null) continue; final String id = o.optString("id");
                String w = o.optString("who"); String ini = w.isEmpty() ? "?" : w.substring(0, 1).toUpperCase();
                LinearLayout bx = NUi.col(c); bx.addView(shRow(ini, w + " invited you", o.optBoolean("locked") ? "🔒 " + o.optString("from") + " · unlocking…" : o.optString("e") + " " + o.optString("m") + " · “" + o.optString("t") + "”", null));
                View v1 = btn("View", true, new View.OnClickListener() { public void onClick(View v) { shAct("shAccept", id, true); } });
                if (o.optBoolean("later")) bx.addView(btns(v1)); else bx.addView(btns(v1, btn("Later", false, new View.OnClickListener() { public void onClick(View v) { shAct("shLater", id, false); } })));
                q.addView(bx, NUi.mt(6));
            }
            col.addView(q, NUi.mt(14));
        }
        JSONArray jn = shD.optJSONArray("on_");
        if (jn != null && jn.length() > 0) {
            LinearLayout q = panel("Shared with others", null);
            for (int i = 0; i < jn.length(); i++) {
                final JSONObject o = jn.optJSONObject(i); if (o == null) continue; final String id = o.optString("id");
                View r = shRow(o.optString("e"), o.optString("t"), o.optString("m") + " · " + (o.optBoolean("own") ? "you shared it" : "from " + o.optString("from")), NUi.icon(c, "next", 18, NTheme.muted));
                NUi.tap(r, new View.OnClickListener() { public void onClick(View v) { shAct("shOpen", id, true); } });
                q.addView(r);
            }
            col.addView(q, NUi.mt(14));
        }
        JSONArray od = shD.optJSONArray("old");
        if (od != null && od.length() > 0) {
            LinearLayout q = panel("Shared before the update", "Sharing works a new, simpler way now: invites just appear for the other person, end-to-end encrypted. Share these again to move them over.");
            for (int i = 0; i < od.length(); i++) {
                final JSONObject o = od.optJSONObject(i); if (o == null) continue; final String id = o.optString("id");
                LinearLayout bx = NUi.col(c); bx.addView(shRow(o.optString("e"), o.optString("t"), o.optString("m"), null));
                bx.addView(btns(btn("Share again", true, new View.OnClickListener() { public void onClick(View v) { shAct("shRedo", id, true); } }), btn("Remove", false, new View.OnClickListener() { public void onClick(View v) { shAct("shDrop", id, false); } })));
                q.addView(bx, NUi.mt(6));
            }
            col.addView(q, NUi.mt(14));
        }
    }

    /* ---------- AI & voice ---------- */
    void voice(LinearLayout col) {
        LinearLayout a = panel("Assistant", "Which AI answers Ask and drafts plans: the model on this phone, your own AI server, or copy a prompt to use anywhere.");
        a.addView(btns(btn("Assistant settings", true, new View.OnClickListener() { public void onClick(View v) { NSheets.assistant(sh); } })), NUi.mt(14));
        col.addView(a);
        JSONObject vs = obj("voice"); boolean on = vs.optBoolean("on", true);
        boolean od; try { od = new JSONObject(Voice.state(sh.a)).optBoolean("onDevice"); } catch (Exception e) { od = false; }
        LinearLayout p = panel("Voice", od ? "This phone turns speech into text on the device, so your voice never leaves it." : "This phone has no on-device speech recognition. Its regular speech service (usually Google’s) sends audio online to turn it into text.");
        sw(p, "Mic buttons", "Talk instead of typing in the journal, Ask, goals and habits", on, new Chg() { public void on(boolean v) { putIn("voice", "on", v); } });
        if (!od) sw(p, "Allow online speech", vs.optBoolean("cloud") ? "Allowed" : "Asked the first time you tap a mic", vs.optBoolean("cloud"), new Chg() { public void on(boolean v) { putIn("voice", "cloud", v); refresh(); } });
        LinearLayout lr = NUi.row(c); lr.setPadding(0, NUi.dp(14), 0, NUi.dp(4));
        lr.addView(NUi.body(c, "Language", 15, NTheme.text, 600), NUi.lpw(0, -2, 1));
        final String[][] LG = {{"", "Same as my phone"}, {"en-US", "English (US)"}, {"en-GB", "English (UK)"}, {"en-IN", "English (India)"}, {"ur-PK", "Urdu"}, {"hi-IN", "Hindi"}, {"ar-SA", "Arabic"}, {"es-ES", "Spanish"}, {"fr-FR", "French"}};
        String ln = LG[0][1]; for (String[] x : LG) if (x[0].equals(vs.optString("lang", ""))) ln = x[1];
        final TextView lt = NUi.body(c, ln, 15, NTheme.accent, 700); lr.addView(lt);
        NUi.tap(lr, new View.OnClickListener() { public void onClick(View v) {
            NForms.picker(c, "Language", LG, vs.optString("lang", ""), new NForms.Pick() { public void on(String k) { putIn("voice", "lang", k); refresh(); } });
        } });
        p.addView(lr);
        col.addView(p, NUi.mt(14));
    }

    /* ---------- Your data ---------- */
    void data(LinearLayout col) {
        LinearLayout p = panel("Your data", "Everything is stored on this device. Export a backup before switching devices or browsers.");
        java.util.List<View> bs = new java.util.ArrayList<>();
        if (eng != null && eng.optInt("demo", 0) > 0) { bs.add(btn("🗑  Remove sample data", false, new View.OnClickListener() { public void onClick(View v) { sh.run("demoRemove", null); } })); }
        bs.add(btn("Export backup", false, new View.OnClickListener() { public void onClick(View v) { sh.run("exportData", null); } }));
        bs.add(btn("Import backup", false, new View.OnClickListener() { public void onClick(View v) { sh.a.pickImport("backup"); } }));
        bs.add(btn("Import MQC journal", false, new View.OnClickListener() { public void onClick(View v) { sh.a.pickImport("mqc"); } }));
        View wipe = btn("Erase everything", false, new View.OnClickListener() { public void onClick(View v) { NEng.wipe(sh); } });
        ((TextView) wipe).setTextColor(NTheme.LATE);
        bs.add(wipe);
        p.addView(btns(bs.toArray(new View[0])), NUi.mt(14));
        col.addView(p);
    }
}
