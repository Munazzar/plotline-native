package com.munazzar.plotline;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import android.widget.FrameLayout;

/* small shared effects the web app uses: confetti burst, two-tone beep, short vibration, reduced motion */
final class NFx {
    private NFx() { }

    static boolean reduced(NShell sh) {
        if ("reduced".equals(sh.st.settings().optString("motion"))) return true;
        try { return android.provider.Settings.Global.getFloat(sh.a.getContentResolver(), android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f; } catch (Exception e) { return false; }
    }

    static void vib(Context c, long[] pat) {
        try {
            android.os.Vibrator v = (android.os.Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
            if (v == null || !v.hasVibrator()) return;
            if (pat.length == 1) v.vibrate(android.os.VibrationEffect.createOneShot(pat[0], android.os.VibrationEffect.DEFAULT_AMPLITUDE));
            else { long[] p = new long[pat.length + 1]; p[0] = 0; System.arraycopy(pat, 0, p, 1, pat.length); v.vibrate(android.os.VibrationEffect.createWaveform(p, -1)); }
        } catch (Exception ignored) { }
    }

    static void vib(Context c) { vib(c, new long[]{10}); }

    /* the web beep(): 880 Hz then 1320 Hz, each ~0.4 s with a quick attack and exponential fade */
    static void beep() {
        new Thread(new Runnable() { public void run() {
            try {
                int sr = 22050, n = (int) (sr * .63);
                short[] buf = new short[n];
                double[] fs = {880, 1320}; double[] at = {0, .18};
                for (int k = 0; k < 2; k++) {
                    int s0 = (int) (at[k] * sr), len = (int) (.45 * sr);
                    for (int i = 0; i < len && s0 + i < n; i++) {
                        double t = i / (double) sr, g = t < .02 ? .16 * (t / .02) : .16 * Math.pow(.0001 / .16, (t - .02) / .38);
                        if (t > .4) g = 0;
                        buf[s0 + i] += (short) (Math.sin(2 * Math.PI * fs[k] * t) * g * 32767);
                    }
                }
                android.media.AudioTrack tr = new android.media.AudioTrack.Builder()
                    .setAudioAttributes(new android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    .setAudioFormat(new android.media.AudioFormat.Builder().setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sr).setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(n * 2).setTransferMode(android.media.AudioTrack.MODE_STATIC).build();
                tr.write(buf, 0, n); tr.play();
                Thread.sleep(800); tr.release();
            } catch (Exception ignored) { }
        } }).start();
    }

    /* the web burst(): 34 dots fly out from 40% down the screen and fade */
    static void burst(final NShell sh) {
        if (reduced(sh)) return;
        final FrameLayout root = sh.root;
        final int[] cols = {NTheme.accent, 0xFF46C99B, 0xFF6B9BFF, 0xFFFF8C6B, 0xFFF0C862};
        final float[] dx = new float[34], dy = new float[34];
        for (int i = 0; i < 34; i++) { double a = Math.random() * Math.PI * 2, d = 80 + Math.random() * 180; dx[i] = (float) (Math.cos(a) * d); dy[i] = (float) (Math.sin(a) * d); }
        final float[] p = {0f};
        final Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
        final View v = new View(sh.a) {
            @Override protected void onDraw(Canvas cv) {
                float cx = getWidth() / 2f, cy = getHeight() * .4f, e = p[0], r = NUi.dp(3.5f) * (1f - .7f * e);
                for (int i = 0; i < 34; i++) { pt.setColor(cols[i % 5]); pt.setAlpha((int) (255 * (1f - e))); cv.drawCircle(cx + NUi.dp(dx[i]) * e, cy + NUi.dp(dy[i]) * e, r, pt); }
            }
        };
        root.addView(v, new FrameLayout.LayoutParams(-1, -1));
        ValueAnimator an = ValueAnimator.ofFloat(0f, 1f); an.setDuration(1300); an.setInterpolator(new android.view.animation.PathInterpolator(.32f, .72f, 0f, 1f));
        an.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { public void onAnimationUpdate(ValueAnimator a) { p[0] = (Float) a.getAnimatedValue(); v.invalidate(); } });
        an.addListener(new android.animation.AnimatorListenerAdapter() { @Override public void onAnimationEnd(android.animation.Animator a) { root.removeView(v); } });
        an.start();
    }
}
