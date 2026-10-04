package com.munazzar.plotline;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* The web app's line icons (24×24, 1.8 stroke) drawn natively. Paths come pre-flattened from native/gen.py
   as absolute M/L/C/Z commands; a leading "f" marks a filled shape, "s" a stroked one. */
final class NIcon extends Drawable {
    static final Map<String, Path[]> CACHE = new HashMap<>();
    static final Map<String, boolean[]> FILL = new HashMap<>();
    final Path[] paths; final boolean[] fill; final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Path tmp = new Path(); final Matrix mx = new Matrix();
    float stroke = 1.8f;

    static boolean has(String name) { for (String k : NGen.ICON_ID) if (k.equals(name)) return true; return false; }

    NIcon(String name, int color) {
        load(name);
        paths = CACHE.get(name); fill = FILL.get(name);
        p.setColor(color); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND);
    }

    NIcon stroke(float w) { stroke = w; return this; }

    void setColor(int c) { p.setColor(c); invalidateSelf(); }

    static void load(String name) {
        if (CACHE.containsKey(name)) return;
        String d = null;
        for (int i = 0; i < NGen.ICON_ID.length; i++) if (NGen.ICON_ID[i].equals(name)) d = NGen.ICON_D[i];
        if (d == null) { CACHE.put(name, new Path[0]); FILL.put(name, new boolean[0]); return; }
        String[] parts = d.split("\\|");
        Path[] ps = new Path[parts.length]; boolean[] fs = new boolean[parts.length];
        for (int i = 0; i < parts.length; i++) { fs[i] = parts[i].charAt(0) == 'f'; ps[i] = parse(parts[i].substring(1)); }
        CACHE.put(name, ps); FILL.put(name, fs);
    }

    static Path parse(String s) {
        Path path = new Path();
        String[] t = s.trim().split("\\s+");
        int i = 0;
        while (i < t.length) {
            String c = t[i++];
            switch (c) {
                case "M": path.moveTo(f(t[i]), f(t[i + 1])); i += 2; break;
                case "L": path.lineTo(f(t[i]), f(t[i + 1])); i += 2; break;
                case "C": path.cubicTo(f(t[i]), f(t[i + 1]), f(t[i + 2]), f(t[i + 3]), f(t[i + 4]), f(t[i + 5])); i += 6; break;
                case "Z": path.close(); break;
                default: break;
            }
        }
        return path;
    }

    static float f(String s) { return Float.parseFloat(s); }

    @Override public void draw(Canvas c) {
        Rect b = getBounds();
        float sc = Math.min(b.width(), b.height()) / 24f;
        mx.reset(); mx.setScale(sc, sc); mx.postTranslate(b.left + (b.width() - 24 * sc) / 2f, b.top + (b.height() - 24 * sc) / 2f);
        p.setStrokeWidth(stroke * sc);
        for (int i = 0; i < paths.length; i++) {
            tmp.reset(); paths[i].transform(mx, tmp);
            p.setStyle(fill[i] ? Paint.Style.FILL : Paint.Style.STROKE);
            c.drawPath(tmp, p);
        }
    }

    @Override public void setAlpha(int a) { p.setAlpha(a); }
    @Override public void setColorFilter(ColorFilter cf) { p.setColorFilter(cf); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
