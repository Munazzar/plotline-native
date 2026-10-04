package com.munazzar.plotline;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

/* The same font choices as the web app (Look & feel → Fonts), loaded from assets/nfonts (TTF copies of the
   bundled web fonts). Roles: d = headings, b = text, m = numbers and labels. */
final class NFont {
    static AssetManager am;
    static String d = "bsd", b = "figtree", m = "martian";
    static final Map<String, Typeface> cache = new HashMap<>();

    static void load(Context c, JSONObject st) {
        am = c.getAssets();
        JSONObject f = st.optJSONObject("font");
        String nd = f != null ? f.optString("d", "bsd") : "bsd", nb = f != null ? f.optString("b", "figtree") : "figtree", nm = f != null ? f.optString("m", "martian") : "martian";
        if (!nd.equals(d) || !nb.equals(b) || !nm.equals(m)) cache.clear();
        d = nd; b = nb; m = nm;
    }

    static Typeface display(int w) { return face(d, w, Typeface.create("sans-serif-condensed", Typeface.BOLD)); }
    static Typeface body(int w) { return face(b, w, Typeface.create("sans-serif", w >= 600 ? Typeface.BOLD : Typeface.NORMAL)); }
    static Typeface mono(int w) { return face(m, w, Typeface.MONOSPACE); }

    static Typeface face(String id, int w, Typeface fallback) {
        String key = id + ":" + w;
        Typeface t = cache.get(key);
        if (t != null) return t;
        t = fallback;
        try {
            for (String[] f : NGen.FONTS) {
                if (!f[0].equals(id) || f[3].isEmpty()) continue;
                String best = null; int bestD = 9999; boolean variable = false;
                for (String part : f[3].split(";")) {
                    String[] wf = part.split(":");
                    if (wf[0].contains(" ")) { best = wf[1]; variable = true; break; }
                    int ww = Integer.parseInt(wf[0].trim()), dd = Math.abs(ww - w);
                    if (dd < bestD) { bestD = dd; best = wf[1]; }
                }
                if (best == null) break;
                String path = "nfonts/" + best + ".ttf";
                if (variable) t = new Typeface.Builder(am, path).setFontVariationSettings("'wght' " + w).build();
                else t = Typeface.createFromAsset(am, path);
                if (t == null) t = fallback;
                break;
            }
        } catch (Exception e) { t = fallback; }
        cache.put(key, t);
        return t;
    }
}
