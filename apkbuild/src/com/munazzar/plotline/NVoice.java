package com.munazzar.plotline;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONObject;

/* The mic on a native text field. Same rules as the web: on-device speech first, online speech only after one OK
   (settings.voice.cloud), words appear as you talk, tap again to stop. */
final class NVoice {
    static NVoice cur;
    final MainActivity a; final EditText e; final Mic mic; String base = ""; boolean live;
    static int seq;
    String id;

    static final class Mic extends Drawable {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); final RectF r = new RectF();
        Mic(int col) { p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND); p.setColor(col); }
        void color(int c) { p.setColor(c); invalidateSelf(); }
        @Override public void draw(Canvas cv) {
            float s = getBounds().width() / 24f; cv.save(); cv.translate(getBounds().left, getBounds().top); cv.scale(s, s);
            p.setStrokeWidth(1.8f);
            r.set(9, 3, 15, 14); cv.drawRoundRect(r, 3, 3, p);
            r.set(5, 4, 19, 18); cv.drawArc(r, 0, 180, false, p);
            cv.drawLine(12, 18, 12, 21, p);
            cv.restore();
        }
        @Override public void setAlpha(int x) { p.setAlpha(x); }
        @Override public void setColorFilter(ColorFilter f) { p.setColorFilter(f); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    static boolean enabled(MainActivity a) {
        if (a == null || a.shell == null) return false;
        JSONObject v = a.shell.st.settings().optJSONObject("voice");
        if (v != null && !v.optBoolean("on", true)) return false;
        try { JSONObject s = new JSONObject(Voice.state(a)); return s.optBoolean("onDevice") || s.optBoolean("any"); } catch (Exception x) { return false; }
    }

    /* put the mic at the right end of a field */
    static void attach(android.content.Context c, final EditText e) {
        if (!(c instanceof MainActivity)) return;
        final MainActivity a = (MainActivity) c;
        if (!enabled(a)) return;
        final NVoice nv = new NVoice(a, e);
        int sz = NUi.dp(20);
        nv.mic.setBounds(0, 0, sz, sz);
        e.setCompoundDrawables(null, null, nv.mic, null);
        e.setCompoundDrawablePadding(NUi.dp(8));
        e.setPadding(e.getPaddingLeft(), e.getPaddingTop(), NUi.dp(14), e.getPaddingBottom());
        e.setOnTouchListener(new View.OnTouchListener() { public boolean onTouch(View v, MotionEvent ev) {
            boolean hit = ev.getX() > v.getWidth() - NUi.dp(14) - NUi.dp(20) - NUi.dp(14);
            if (!hit) return false;
            if (ev.getAction() == MotionEvent.ACTION_UP) nv.toggle();
            return true;
        } });
    }

    NVoice(MainActivity a, EditText e) { this.a = a; this.e = e; mic = new Mic(NTheme.muted); }

    JSONObject cfg() { JSONObject v = a.shell.st.settings().optJSONObject("voice"); return v == null ? new JSONObject() : v; }

    void paint(boolean on) { live = on; mic.color(on ? NTheme.accent : NTheme.muted); }

    void toggle() {
        if (live) { Voice.stop(a); return; }
        if (cur != null && cur != this) { cur.paint(false); Voice.stopNow(); }
        if (a.checkSelfPermission("android.permission.RECORD_AUDIO") != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            MainActivity.nativeMic = this;
            a.askPerms("nmic", new String[]{"android.permission.RECORD_AUDIO"});
            return;
        }
        begin();
    }

    void permResult(boolean ok) { if (ok) begin(); else NShell.toast("Plotline needs the microphone to hear you. You can allow it in Android settings."); }

    void begin() {
        JSONObject s; try { s = new JSONObject(Voice.state(a)); } catch (Exception x) { s = new JSONObject(); }
        final JSONObject cf = cfg();
        boolean od = s.optBoolean("onDevice");
        if (!od && !cf.has("cloud")) { consent(); return; }
        if (!od && !cf.optBoolean("cloud")) { NShell.toast("This phone can’t turn speech into text on the device. Turn on online speech in Settings."); return; }
        go(cf.optBoolean("cloud"));
    }

    void consent() {
        final NForms F = new NForms(a.shell);
        LinearLayout b = F.sheetBody("VOICE", "Use online speech?", "This phone can’t turn speech into text on the device. Its speech service (usually Google’s) can, but it sends what you say online to do it. Plotline itself never stores or sends your audio.");
        b.addView(NForms.actions(a,
            NUi.btn(a, "Not now", false, new View.OnClickListener() { public void onClick(View v) { setCloud(false); a.shell.closeSheet(); } }),
            NUi.btn(a, "Allow", true, new View.OnClickListener() { public void onClick(View v) { setCloud(true); a.shell.closeSheet(); go(true); } })));
        a.shell.sheet(b);
    }

    void setCloud(boolean v) { try { JSONObject o = a.shell.st.settings().optJSONObject("voice"); if (o == null) { o = new JSONObject(); a.shell.st.settings().put("voice", o); } o.put("cloud", v); } catch (Exception ignored) { } a.shell.saveQuiet(); }

    void go(boolean cloud) {
        base = e.getText().toString(); id = "n" + (++seq); cur = this; paint(true);
        Voice.nat = this;
        Voice.start(a, id, cfg().optString("lang", ""), cloud);
    }

    void join(String t) {
        String b = base.trim().isEmpty() ? "" : base.replaceAll("\\s+$", "") + " ";
        e.setText(b + t); e.setSelection(e.getText().length());
    }

    /* from Voice.send, on the main thread */
    void on(String kind, String text) {
        if ("partial".equals(kind) || "final".equals(kind)) { join(text); if ("final".equals(kind)) base = e.getText().toString(); }
        else if ("end".equals(kind)) { paint(false); if (Voice.nat == this) Voice.nat = null; }
        else if ("err".equals(kind)) {
            paint(false); Voice.nat = null;
            String m = "7".equals(text) ? "Didn’t catch that. Tap the mic and try again" : "6".equals(text) ? "Didn’t hear anything" : ("9".equals(text) || "perm".equals(text)) ? "Allow the microphone to use voice" : "13".equals(text) ? "Speech isn’t available on this phone right now" : null;
            if (m != null) NShell.toast(m);
        }
    }
}
