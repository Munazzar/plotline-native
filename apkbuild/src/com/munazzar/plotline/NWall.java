package com.munazzar.plotline;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import org.json.JSONObject;

/* web 1.13 "Colours & background": the page wallpaper (html[data-wall] body::before). Four soft colour blobs over the
   theme background, at the chosen strength (wallStr, default 55), optionally drifting slowly (wallMove).
   Glass themes default to Aurora; every other theme defaults to none. */
final class NWall extends Drawable {
    static final String[][] WALLS = {{"off", "None"}, {"aurora", "Aurora"}, {"ocean", "Ocean"}, {"sunset", "Sunset"}, {"forest", "Forest"}, {"rose", "Rose"}, {"mono", "Mono"}, {"accent", "Accent"}};

    static String wallOf(JSONObject st) {
        String w = st.optString("wall", ""), t = st.optString("theme", "night");
        for (String[] x : WALLS) if (x[0].equals(w)) return w;
        return t.equals("glass") || t.equals("glasslight") ? "aurora" : "off";
    }

    static int[] cols(String w) {
        switch (w) {
            case "aurora": return new int[]{0xFF5B6CFF, 0xFFA35CF0, 0xFF18B0A8, 0xFFE0745A};
            case "ocean": return new int[]{0xFF2E6BFF, 0xFF18A8D8, 0xFF22B894, 0xFF4C5FD6};
            case "sunset": return new int[]{0xFFFF7A59, 0xFFE8507E, 0xFFF5A83A, 0xFF9B5CE0};
            case "forest": return new int[]{0xFF2E9D63, 0xFF7DB043, 0xFF178F8F, 0xFF4F7F35};
            case "rose": return new int[]{0xFFF27FA6, 0xFFB977F2, 0xFFF2A493, 0xFF7F9BF2};
            case "mono": return new int[]{0xFF7D869A, 0xFF5A6275, 0xFFA1A8B6, 0xFF687287};
            case "accent": { int a = NTheme.accent | 0xFF000000; return new int[]{a, NTheme.mix(0xFF8A5CF0, a, .6f), NTheme.mix(0xFF18B0A8, a, .55f), NTheme.mix(NTheme.bg | 0xFF000000, a, .7f)}; }
            default: return null;
        }
    }

    /* a small preview tile (the picker's .lk-w i): same blobs, square, no strength */
    static Drawable tile(final String w) {
        return new Drawable() {
            final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override public void draw(Canvas cv) {
                Rect b = getBounds(); p.setShader(null); p.setColor(NTheme.bg); cv.drawRect(b, p);
                int[] c = cols(w); if (c == null) return;
                float[][] at = {{.25f, .25f, .60f}, {.80f, .20f, .60f}, {.75f, .80f, .60f}, {.20f, .85f, .55f}};
                for (int i = 0; i < 4; i++) {
                    float cx = b.left + b.width() * at[i][0], cy = b.top + b.height() * at[i][1], r = Math.max(1, b.width() * at[i][2] * .72f);
                    p.setShader(new RadialGradient(cx, cy, r, new int[]{c[i], c[i] & 0x00FFFFFF}, null, Shader.TileMode.CLAMP)); cv.drawRect(b, p);
                }
                p.setShader(null);
            }
            @Override public void setAlpha(int a) { } @Override public void setColorFilter(ColorFilter f) { }
            @Override public int getOpacity() { return PixelFormat.OPAQUE; }
        };
    }

    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    String wall = "off"; float op = .55f; boolean move; int[] c;
    static long t0 = android.os.SystemClock.uptimeMillis();

    NWall(JSONObject st, boolean reduced) {
        wall = wallOf(st); c = cols(wall);
        op = (st.has("wallStr") && !st.isNull("wallStr") ? (float) st.optDouble("wallStr", 55) : 55f) / 100f;
        move = st.optBoolean("wallMove") && !reduced;
    }

    boolean on() { return c != null; }

    @Override public void draw(Canvas cv) {
        Rect b = getBounds();
        p.setShader(null); p.setColor(NTheme.bg); cv.drawRect(b, p);
        if (c == null) return;
        float w = b.width(), h = b.height();
        cv.save();
        if (move) {   /* @keyframes lgDrift: to translate(-3%, 2%) scale(1.06), 60s, alternate */
            double t = ((android.os.SystemClock.uptimeMillis() - t0) % 120000L) / 60000.0; float k = (float) (t <= 1 ? t : 2 - t); k = k * k * (3 - 2 * k);
            cv.translate(-.03f * w * k, .02f * h * k); float s = 1 + .06f * k; cv.scale(s, s, w / 2, h / 2);
            invalidateSelf();
        }
        /* body::before is inset -12%: the blobs are placed on a box 24% larger than the screen */
        float L = b.left - w * .12f, T = b.top - h * .12f, W = w * 1.24f, H = h * 1.24f;
        float[][] at = {{.18f, .22f, .40f, .32f}, {.82f, .18f, .36f, .30f}, {.75f, .78f, .42f, .36f}, {.22f, .82f, .38f, .32f}};
        int a = Math.round(255 * op);
        for (int i = 0; i < 4; i++) {
            int col = NTheme.mix(NTheme.bg | 0xFF000000, c[i], .62f);   /* color-mix(w 62%, bg) */
            float cx = L + W * at[i][0], cy = T + H * at[i][1], rx = W * at[i][2], ry = H * at[i][3];
            cv.save(); cv.scale(1, ry / rx, cx, cy);
            p.setShader(new RadialGradient(cx, cy, rx, new int[]{(col & 0x00FFFFFF) | (a << 24), col & 0x00FFFFFF}, new float[]{0, .72f}, Shader.TileMode.CLAMP));
            cv.drawCircle(cx, cy, rx, p); cv.restore();
        }
        p.setShader(null); cv.restore();
    }

    @Override public void setAlpha(int x) { }
    @Override public void setColorFilter(ColorFilter f) { }
    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
