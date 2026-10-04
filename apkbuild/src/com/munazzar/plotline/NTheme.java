package com.munazzar.plotline;

import android.graphics.Color;
import org.json.JSONObject;

/* The current theme's colours (generated from the web app's CSS by native/gen.py), plus the person's accent. */
final class NTheme {
    static String id = "night";
    static boolean light;
    static int bg, bg2, surface, surface2, line, line2, text, muted, dim, accent, onAccent;
    static int INK = 0xFF0B1020, INK_MUTED = 0xB30B1020; static final int LATE = 0xFFFF7A7A;
    static String halo = "high"; static boolean haloBright, haloPulse;
    static boolean haloOn(org.json.JSONObject g, boolean curStep) { return halo.equals("all") || (halo.equals("high") && g.optInt("priority", 2) == 1) || (halo.equals("pinned") && g.optBoolean("pinned")) || (halo.equals("next") && curStep); }
    static String cards = "solid", cardBg = "plain", cardImg = "";

    static boolean systemNight(android.content.Context c) {
        return (c.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    static void load(JSONObject st, android.content.Context ctx) {
        String t = st.optString("theme", "night");
        /* "Match my device": a dark theme switches to Daylight while the phone is in light mode (as on the web) */
        if (st.optBoolean("matchSystem") && ctx != null && !systemNight(ctx)) {
            boolean lt = false; for (int k = 0; k < NGen.THEME_ID.length; k++) if (NGen.THEME_ID[k].equals(t)) lt = NGen.THEME_LIGHT[k];
            if (!lt) t = "daylight";
        }
        int i = 0;
        for (int k = 0; k < NGen.THEME_ID.length; k++) if (NGen.THEME_ID[k].equals(t)) i = k;
        id = NGen.THEME_ID[i]; light = NGen.THEME_LIGHT[i];
        int[] c = NGen.THEME_COL[i];
        bg = c[0]; bg2 = c[1]; surface = c[2]; surface2 = c[3]; line = c[4]; line2 = c[5]; text = c[6]; muted = c[7]; dim = c[8]; accent = c[9]; onAccent = c[10];
        /* glass themes: translucent surfaces become solid tints over the base (native draws no live blur) */
        if (Color.alpha(surface) < 255) surface = over(surface, bg2);
        if (Color.alpha(surface2) < 255) surface2 = over(surface2, bg2);
        if (Color.alpha(muted) < 255) muted = over(muted, bg);
        if (Color.alpha(dim) < 255) dim = over(dim, bg);
        String cs = st.optString("cards", "solid");
        cards = cs.equals("glow") || cs.equals("neon") || cs.equals("aurora") || cs.equals("glass") || cs.equals("index") ? cs : "solid";
        if (cards.equals("glow") || cards.equals("neon") || cards.equals("glass")) { INK = text; INK_MUTED = muted; }
        else if (cards.equals("index")) { INK = 0xFF141A2C; INK_MUTED = 0xFF5E667C; }
        else { INK = 0xFF0B1020; INK_MUTED = 0xA30B1020; }
        halo = st.optString("halo", "high"); haloBright = st.optBoolean("haloBright"); haloPulse = st.optBoolean("haloPulse");
        String cb = st.optString("cardBg", "plain"); cardImg = st.optString("cardImg", "");
        cardBg = cb.matches("soft|grid|dots|lines|waves|topo|plus") ? cb : cb.equals("image") && !cardImg.isEmpty() ? "image" : "plain";
        if (cardBg.equals("image")) { INK = 0xFFFFFFFF; INK_MUTED = 0xCCFFFFFF; }
        String a = st.optString("accent", "");
        if (a.matches("#[0-9a-fA-F]{6}")) { accent = Color.parseColor(a); onAccent = lum(accent) > .4 ? 0xFF0B0F1A : 0xFFFFFFFF; }
    }

    static int over(int fg, int bgc) {
        float a = Color.alpha(fg) / 255f;
        return Color.rgb(Math.round(Color.red(fg) * a + Color.red(bgc) * (1 - a)), Math.round(Color.green(fg) * a + Color.green(bgc) * (1 - a)), Math.round(Color.blue(fg) * a + Color.blue(bgc) * (1 - a)));
    }

    static int mix(int a, int b, float t) {
        return Color.argb(Math.round(Color.alpha(a) + (Color.alpha(b) - Color.alpha(a)) * t), Math.round(Color.red(a) + (Color.red(b) - Color.red(a)) * t), Math.round(Color.green(a) + (Color.green(b) - Color.green(a)) * t), Math.round(Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t));
    }

    static int alpha(int c, float a) { return (c & 0x00FFFFFF) | (Math.round(Color.alpha(c) * a) << 24); }

    static double lum(int c) {
        double r = ch(Color.red(c)), g = ch(Color.green(c)), b = ch(Color.blue(c));
        return .2126 * r + .7152 * g + .0722 * b;
    }
    static double ch(int v) { double x = v / 255.0; return x <= .03928 ? x / 12.92 : Math.pow((x + .055) / 1.055, 2.4); }

    /* text colour that reads on top of c */
    static int on(int c) { return lum(c) > .36 ? INK : 0xFFFFFFFF; }

    /* ---- life areas ---- */
    static int hashN(String s) { int h = 7; for (int i = 0; i < s.length(); i++) h = (h * 31 + s.charAt(i)); return h == Integer.MIN_VALUE ? 0 : Math.abs(h); }

    static String areaKey(String v) {
        if (v == null) return "personal";
        String t = v.trim(); if (t.isEmpty()) return "personal";
        String l = t.toLowerCase();
        for (int i = 0; i < NGen.AREA_ID.length; i++) if (NGen.AREA_ID[i].equals(l) || NGen.AREA_NAME[i].toLowerCase().equals(l)) return NGen.AREA_ID[i];
        return t;
    }

    static int areaIndex(String v) {
        String k = areaKey(v);
        for (int i = 0; i < NGen.AREA_ID.length; i++) if (NGen.AREA_ID[i].equals(k)) return i;
        return hashN(k.toLowerCase()) % NGen.AREA_ID.length;
    }

    /* a goal with its own cover image draws white ink on its cards, whatever the card style */
    static boolean hasImg(org.json.JSONObject g) { return g != null && !g.optString("img").isEmpty(); }
    static int[] saved;
    static boolean pushImg(org.json.JSONObject g) {
        if (!hasImg(g)) return false;
        saved = new int[]{INK, INK_MUTED}; INK = 0xFFFFFFFF; INK_MUTED = 0xCCFFFFFF; return true;
    }
    static void popImg(boolean on) { if (on && saved != null) { INK = saved[0]; INK_MUTED = saved[1]; saved = null; } }

    static int areaCol(String v) { return NGen.AREA_COL[areaIndex(v)]; }

    static String areaName(String v) {
        String k = areaKey(v);
        for (int i = 0; i < NGen.AREA_ID.length; i++) if (NGen.AREA_ID[i].equals(k)) return NGen.AREA_NAME[i];
        return k.isEmpty() ? "Personal" : Character.toUpperCase(k.charAt(0)) + k.substring(1);
    }
}
