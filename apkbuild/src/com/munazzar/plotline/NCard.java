package com.munazzar.plotline;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/* The web's "Card style" (settings.cards): solid · glow · neon · aurora · glass · index, drawn for a tinted card. */
final class NCard extends Drawable {
    final int col, c2, c3; final float r; final String style, tex;
    static android.graphics.Bitmap img; static int imgSig;
    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); final Path clip = new Path(); final RectF rf = new RectF();

    android.graphics.Bitmap own; boolean halo;
    static final java.util.Map<String, Object[]> OWN = new java.util.HashMap<>();
    static Drawable bgFor(org.json.JSONObject g, int col, float rDp, boolean cur) {
        Drawable d = bgFor(g, col, rDp);
        if (cur && d instanceof NCard && NTheme.halo.equals("next")) ((NCard) d).halo = true;
        return d;
    }
    static Drawable bgFor(org.json.JSONObject g, int col, float rDp) {
        String u = g == null ? "" : g.optString("img");
        NCard d = new NCard(col, NUi.dp(rDp), NTheme.cards, u.isEmpty() ? NTheme.cardBg : "image");
        d.halo = g != null && NTheme.haloOn(g, false);
        if (u.isEmpty()) return d;
        Object[] e = OWN.get(g.optString("id"));
        if (e == null || (Integer) e[0] != u.length()) {
            android.graphics.Bitmap bm = null;
            try { int i = u.indexOf("base64,"); if (i >= 0) { byte[] by = android.util.Base64.decode(u.substring(i + 7), android.util.Base64.DEFAULT); bm = android.graphics.BitmapFactory.decodeByteArray(by, 0, by.length); } } catch (Exception ignored) { }
            e = new Object[]{u.length(), bm}; OWN.put(g.optString("id"), e);
        }
        d.own = (android.graphics.Bitmap) e[1];
        if (d.own == null) { NCard e2 = new NCard(col, NUi.dp(rDp), NTheme.cards, NTheme.cardBg); e2.halo = d.halo; return e2; }
        return d;
    }
    static Drawable bg(int col, float rDp) { return new NCard(col, NUi.dp(rDp), NTheme.cards, NTheme.cardBg); }
    static Drawable preview(int col, float rDp, String style, String tex) { return new NCard(col, NUi.dp(rDp), style, tex); }

    NCard(int col, float r, String style, String tex) {
        this.col = col; this.r = r; this.style = style; this.tex = tex == null ? "plain" : tex;
        int i = -1; for (int k = 0; k < NGen.AREA_COL.length; k++) if (NGen.AREA_COL[k] == col) i = k;
        int n = NGen.AREA_COL.length;
        c2 = i < 0 ? col : NGen.AREA_COL[(i + 2) % n]; c3 = i < 0 ? col : NGen.AREA_COL[(i + 5) % n];
    }

    static int a(int c, float al) { return NTheme.alpha(c | 0xFF000000, al); }

    void rad(Canvas cv, RectF b, float cx, float cy, float rx, int color) {
        p.setStyle(Paint.Style.FILL); p.setShader(new RadialGradient(b.left + b.width() * cx, b.top + b.height() * cy, Math.max(1, rx), color, a(color, 0f), Shader.TileMode.CLAMP));
        cv.drawRect(b, p); p.setShader(null);
    }


    static android.graphics.Bitmap cover() {
        String u = NTheme.cardImg; if (u == null || u.isEmpty()) return null;
        if (img != null && imgSig == u.length()) return img;
        try {
            int i = u.indexOf("base64,"); if (i < 0) return null;
            byte[] by = android.util.Base64.decode(u.substring(i + 7), android.util.Base64.DEFAULT);
            img = android.graphics.BitmapFactory.decodeByteArray(by, 0, by.length); imgSig = u.length();
        } catch (Exception e) { img = null; }
        return img;
    }

    void texture(Canvas cv, RectF b) {
        float w = b.width(), h = b.height(); int ink = NTheme.alpha(style.equals("glow") || style.equals("neon") || style.equals("glass") ? NTheme.text : 0xFF0B1020, .14f);
        p.setShader(null); p.setPathEffect(null); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(Math.max(1, NUi.dp(1))); p.setColor(ink);
        float g = NUi.dp(16);
        switch (tex) {
            case "soft": p.setStyle(Paint.Style.FILL); p.setShader(new LinearGradient(b.left, b.top, b.right, b.bottom, 0x30FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)); cv.drawRect(b, p); p.setShader(null); break;
            case "grid": for (float x = b.left + g; x < b.right; x += g) cv.drawLine(x, b.top, x, b.bottom, p); for (float y = b.top + g; y < b.bottom; y += g) cv.drawLine(b.left, y, b.right, y, p); break;
            case "dots": p.setStyle(Paint.Style.FILL); for (float x = b.left + g / 2; x < b.right; x += g) for (float y = b.top + g / 2; y < b.bottom; y += g) cv.drawCircle(x, y, NUi.dp(1.2f), p); break;
            case "lines": for (float x = b.left - h; x < b.right; x += NUi.dp(12)) cv.drawLine(x, b.bottom, x + h, b.top, p); break;
            case "plus": for (float x = b.left + g; x < b.right; x += g * 1.4f) for (float y = b.top + g; y < b.bottom; y += g * 1.4f) { cv.drawLine(x - NUi.dp(3), y, x + NUi.dp(3), y, p); cv.drawLine(x, y - NUi.dp(3), x, y + NUi.dp(3), p); } break;
            case "waves": for (int k = 0; k < 6; k++) { Path q = new Path(); float y0 = b.top + h * (k + 1) / 7f; q.moveTo(b.left, y0); for (float x = b.left; x < b.right; x += w / 4) q.quadTo(x + w / 8, y0 - NUi.dp(8), x + w / 4, y0); cv.drawPath(q, p); } break;
            case "topo": for (int k = 1; k < 9; k++) { rf.set(b.right - w * .1f - k * NUi.dp(16) * 1.5f, b.top + h * .15f - k * NUi.dp(12), b.right - w * .1f + k * NUi.dp(16) * 1.5f, b.top + h * .15f + k * NUi.dp(12)); cv.drawOval(rf, p); } break;
            case "image": {
                android.graphics.Bitmap bm = own != null ? own : cover(); if (bm == null) break;
                float sc = Math.max(w / bm.getWidth(), h / bm.getHeight()); android.graphics.Matrix m = new android.graphics.Matrix();
                m.postScale(sc, sc); m.postTranslate(b.left + (w - bm.getWidth() * sc) / 2, b.top + (h - bm.getHeight() * sc) / 2);
                p.setStyle(Paint.Style.FILL); cv.drawBitmap(bm, m, p);
                p.setShader(new LinearGradient(0, b.top, 0, b.bottom, 0x14000000, 0xA8000000, Shader.TileMode.CLAMP)); cv.drawRect(b, p); p.setShader(null); break;
            }
            default: break;
        }
        p.setStyle(Paint.Style.FILL);
    }

    @Override public void draw(Canvas cv) {
        RectF b = new RectF(getBounds()); float w = b.width(), h = b.height();
        if (halo) haloGlow(cv, b);
        clip.reset(); clip.addRoundRect(b, r, r, Path.Direction.CW);
        cv.save(); cv.clipPath(clip);
        p.setStyle(Paint.Style.FILL);
        float bd = Math.max(1, NUi.dp(1)); int stroke = 0; float sw = bd;
        switch (style) {
            case "glow":
                p.setColor(NTheme.mix(NTheme.surface, col | 0xFF000000, .07f)); cv.drawRect(b, p);
                rad(cv, b, 0, 0, w * .85f, a(col, .40f)); rad(cv, b, 1, 1, w * .55f, a(col, .18f));
                stroke = a(col, .45f); break;
            case "neon":
                p.setShader(new LinearGradient(0, b.top, 0, b.bottom, new int[]{NTheme.mix(NTheme.bg, col | 0xFF000000, .07f), NTheme.bg, NTheme.bg}, new float[]{0, .7f, 1}, Shader.TileMode.CLAMP));
                cv.drawRect(b, p); p.setShader(null);
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(NUi.dp(14)); p.setColor(a(col, .16f)); cv.drawRoundRect(b, r, r, p);
                p.setStrokeWidth(NUi.dp(5)); p.setColor(a(col, .25f)); cv.drawRoundRect(b, r, r, p);
                stroke = a(col, 1f); sw = NUi.dp(1.5f); break;
            case "aurora":
                p.setColor(col | 0xFF000000); cv.drawRect(b, p);
                rad(cv, b, .12f, .10f, w * .75f, NTheme.mix(col | 0xFF000000, 0xFFFFFFFF, .3f) | 0xFF000000);
                rad(cv, b, 1f, 0f, w * .85f, c2 | 0xFF000000);
                rad(cv, b, .3f, 1.05f, w * .9f, c3 | 0xFF000000);
                rad(cv, b, .9f, .9f, w * .7f, c2 | 0xFF000000);
                break;
            case "glass":
                p.setColor(NTheme.bg2); cv.drawRect(b, p);
                p.setShader(new LinearGradient(b.left, b.top, b.right, b.bottom, new int[]{a(col, .30f), a(col, .07f), a(col, .07f)}, new float[]{0, .7f, 1}, Shader.TileMode.CLAMP));
                cv.drawRect(b, p); p.setShader(null);
                stroke = NTheme.mix(0x24FFFFFF, a(col, .4f), .5f); break;
            case "index":
                p.setColor(0xFFFAFBFD); cv.drawRect(b, p);
                p.setColor(col | 0xFF000000); cv.drawRect(b.left, b.top, b.left + NUi.dp(8), b.bottom, p);
                stroke = 0x12141A2C; break;
            default:
                p.setColor(col); cv.drawRect(b, p);
        }
        texture(cv, b);
        cv.restore();
        if (stroke != 0) {
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(sw); p.setColor(stroke);
            rf.set(b.left + sw / 2, b.top + sw / 2, b.right - sw / 2, b.bottom - sw / 2);
            cv.drawRoundRect(rf, r - sw / 2, r - sw / 2, p);
        }
    }

    /* the web's .tint.halo: a 2px ring plus a soft glow around the card (wider when "Bright halo") */
    void haloGlow(Canvas cv, RectF b) {
        float hr = NUi.dp(NTheme.haloBright ? 80 : 40), amp = NTheme.haloBright ? 1f : .85f;
        float k = 1f;
        if (NTheme.haloPulse) { k = .8f + .2f * (float) Math.sin(android.os.SystemClock.uptimeMillis() / 2800.0 * 2 * Math.PI); invalidateSelf(); }
        int cc = col | 0xFF000000; int N = 9; float reach = hr * .62f;
        p.setStyle(Paint.Style.STROKE);
        for (int i = N; i >= 1; i--) {
            float f = i / (float) N, off = reach * f;
            p.setStrokeWidth(off * 2 + 1); p.setColor(NTheme.alpha(cc, amp * k * .10f * (1f - f) * (1f - f) + .012f));
            rf.set(b.left - off / 2, b.top - off / 2, b.right + off / 2, b.bottom + off / 2); cv.drawRoundRect(rf, r + off / 2, r + off / 2, p);
        }
        p.setStrokeWidth(NUi.dp(2)); p.setColor(cc);
        rf.set(b.left - NUi.dp(1), b.top - NUi.dp(1), b.right + NUi.dp(1), b.bottom + NUi.dp(1)); cv.drawRoundRect(rf, r + NUi.dp(1), r + NUi.dp(1), p);
        p.setStyle(Paint.Style.FILL);
    }

    @Override public void setAlpha(int x) { }
    @Override public void setColorFilter(ColorFilter f) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
