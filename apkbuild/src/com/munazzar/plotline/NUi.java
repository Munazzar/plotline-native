package com.munazzar.plotline;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/* Building blocks for the native screens: sizes, text styles, cards, buttons and the press feel. */
final class NUi {
    static float density = 3f;
    static final PathInterpolator EASE = new PathInterpolator(.2f, .8f, .2f, 1f);
    static final PathInterpolator SPRING = new PathInterpolator(.34f, 1.4f, .64f, 1f);

    static float sp(float v) { return v * android.content.res.Resources.getSystem().getDisplayMetrics().scaledDensity; }
    static int dp(float v) { return Math.round(v * density); }

    static LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w, h); }
    static LinearLayout.LayoutParams lpw(int w, int h, float weight) { LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(w, h); l.weight = weight; return l; }
    static LinearLayout.LayoutParams mt(int top) { LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-1, -2); l.topMargin = dp(top); return l; }

    static LinearLayout col(Context c) { LinearLayout l = new LinearLayout(c); l.setOrientation(LinearLayout.VERTICAL); return l; }
    static LinearLayout row(Context c) { LinearLayout l = new LinearLayout(c); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }

    static TextView text(Context c, CharSequence s, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(s); t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp); t.setTextColor(color);
        t.setTypeface(NFont.body(400)); t.setIncludeFontPadding(true);
        return t;
    }

    static TextView body(Context c, CharSequence s, float sp, int color, int weight) { TextView t = text(c, s, sp, color); t.setTypeface(NFont.body(weight)); return t; }

    static TextView title(Context c, CharSequence s, float sp) {
        TextView t = text(c, s.toString().toUpperCase(), sp, NTheme.text);
        t.setTypeface(NFont.display(800)); t.setLineSpacing(0, .9f); t.setIncludeFontPadding(false);
        return t;
    }

    static TextView label(Context c, CharSequence s, int color) {
        TextView t = text(c, s.toString().toUpperCase(), 10.5f, color);   /* web .data: 500 10.5px/1.5 mono, .05em */
        t.setTypeface(NFont.mono(500)); t.setLetterSpacing(.05f); t.setLineSpacing(0, 1.2f);
        return t;
    }

    static TextView ell(TextView t, int lines) { t.setMaxLines(lines); t.setEllipsize(TextUtils.TruncateAt.END); return t; }

    static GradientDrawable round(int fill, float r, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill); g.setCornerRadius(dp(r));
        if (stroke != 0) g.setStroke(Math.max(1, dp(1)), stroke);
        return g;
    }

    static GradientDrawable oval(int fill, int stroke, float sw) {
        GradientDrawable g = new GradientDrawable(); g.setShape(GradientDrawable.OVAL); g.setColor(fill);
        if (stroke != 0) g.setStroke(dp(sw), stroke);
        return g;
    }

    static Drawable ripple(Drawable content, float r) {
        int rc = NTheme.light ? 0x1F000000 : 0x26FFFFFF;
        return new RippleDrawable(ColorStateList.valueOf(rc), content, round(0xFFFFFFFF, r, 0));
    }

    /* a surface card with the theme's line */
    static Drawable card(float r) { return round(NTheme.surface, r, NTheme.line); }

    static View tap(final View v, final View.OnClickListener l) {
        v.setClickable(true); v.setFocusable(true);
        v.setOnClickListener(new View.OnClickListener() { public void onClick(View x) { try { l.onClick(x); } catch (Exception e) { NCrash.log(x.getContext(), "tap", e); NShell.toast("Something went wrong: " + e.getMessage()); } } });
        press(v);
        return v;
    }

    /* the soft squash on touch that makes buttons feel physical */
    static void press(final View v) {
        v.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View x, MotionEvent e) {
                int a = e.getActionMasked();
                if (a == MotionEvent.ACTION_DOWN) x.animate().scaleX(.965f).scaleY(.965f).setDuration(110).setInterpolator(EASE).start();
                else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) x.animate().scaleX(1f).scaleY(1f).setDuration(320).setInterpolator(SPRING).start();
                return false;
            }
        });
    }

    static void haptic(View v) { try { v.performHapticFeedback(HapticFeedbackConstants.CONFIRM); } catch (Exception e) { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); } }

    static android.graphics.drawable.Drawable iconD(String name, int sizeDp, int color) { NIcon d = new NIcon(name, color); d.setBounds(0, 0, dp(sizeDp), dp(sizeDp)); return d; }

    static ImageView icon(Context c, String name, int sizeDp, int color) {
        ImageView i = new ImageView(c);
        i.setImageDrawable(new NIcon(name, color));
        i.setLayoutParams(new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)));
        return i;
    }

    static boolean narrow = true;

    /* a frame with a fixed size that ignores whatever layout params it is added with */
    static final class Fix extends FrameLayout {
        int w, h;
        Fix(Context c, int w, int h) { super(c); this.w = w; this.h = h; }
        @Override protected void onMeasure(int a, int b) { super.onMeasure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY)); }
    }

    /* square icon button like the web's .ibtn (40 on phones, 46 on wider screens) */
    static FrameLayout ibtn(Context c, String iconName, View.OnClickListener l) { return ibtn(c, iconName, false, l); }
    static FrameLayout ibtn(Context c, String iconName, boolean on, View.OnClickListener l) {
        int sz = narrow ? 40 : 46, rad = narrow ? 13 : 15, ic = narrow ? 18 : 20;
        Fix f = new Fix(c, dp(sz), dp(sz));
        f.setBackground(ripple(round(NTheme.surface, rad, on ? NTheme.alpha(NTheme.accent, .55f) : NTheme.line), rad));
        ImageView i = new ImageView(c); i.setImageDrawable(new NIcon(iconName, on ? NTheme.accent : NTheme.text));
        f.addView(i, new FrameLayout.LayoutParams(dp(ic), dp(ic), Gravity.CENTER));
        f.setLayoutParams(new LinearLayout.LayoutParams(dp(sz), dp(sz)));
        f.setContentDescription(iconName);
        tap(f, l);
        return f;
    }

    /* the web's header .btn.pri with an icon: on phones (<560 wide) only the icon shows in a 44 (40 under 400) wide button */
    static View pbtn(Context c, String iconName, String label, View.OnClickListener l) {
        int wDp = c.getResources().getConfiguration().screenWidthDp;
        if (wDp <= 560) {
            Fix f = new Fix(c, dp(wDp <= 400 ? 40 : 44), dp(narrow ? 40 : 46));
            f.setBackground(ripple(round(NTheme.accent, narrow ? 13 : 15, 0), narrow ? 13 : 15));
            ImageView i = new ImageView(c); i.setImageDrawable(new NIcon(iconName, NTheme.onAccent));
            f.addView(i, new FrameLayout.LayoutParams(dp(narrow ? 18 : 20), dp(narrow ? 18 : 20), Gravity.CENTER));
            f.setContentDescription(label); tap(f, l); return f;
        }
        LinearLayout b = row(c); b.setGravity(Gravity.CENTER);
        b.setBackground(ripple(round(NTheme.accent, 15, 0), 15));
        ImageView i = new ImageView(c); i.setImageDrawable(new NIcon(iconName, NTheme.onAccent));
        b.addView(i, new LinearLayout.LayoutParams(dp(20), dp(20)));
        b.setContentDescription(label);
        {
            TextView t = body(c, label, 14.5f, NTheme.onAccent, 600); t.setSingleLine(true);
            LinearLayout.LayoutParams tl = lp(-2, -2); tl.leftMargin = dp(8); b.addView(t, tl);
            b.setPadding(dp(20), 0, dp(20), 0); b.setMinimumHeight(dp(46));
            b.setLayoutParams(new LinearLayout.LayoutParams(-2, dp(46)));
        }
        tap(b, l);
        return b;
    }

    /* a box that stays as tall as it is wide (up to maxDp) */
    static final class Sq extends LinearLayout {
        final int max;
        Sq(Context c, int maxDp) { super(c); max = dp(maxDp); setOrientation(VERTICAL); setGravity(Gravity.CENTER); }
        @Override protected void onMeasure(int w, int h) { int sw = MeasureSpec.getSize(w); super.onMeasure(w, MeasureSpec.makeMeasureSpec(Math.min(sw, max), MeasureSpec.EXACTLY)); }
    }

    static GradientDrawable dashed(int fill, float r, int stroke, float sw) {
        GradientDrawable g = new GradientDrawable(); g.setColor(fill); g.setCornerRadius(dp(r));
        g.setStroke(Math.max(1, dp(sw)), stroke, dp(5), dp(4));
        return g;
    }

    static int mix(int over, float a, int under) {
        int r = Math.round(Color.red(over) * a + Color.red(under) * (1 - a)), g = Math.round(Color.green(over) * a + Color.green(under) * (1 - a)), b = Math.round(Color.blue(over) * a + Color.blue(under) * (1 - a));
        return Color.rgb(r, g, b);
    }

    /* pill button: primary = accent fill */
    static TextView btn(Context c, String label, boolean primary, View.OnClickListener l) {
        TextView t = body(c, label, 15, primary ? NTheme.onAccent : NTheme.text, 600);
        t.setGravity(Gravity.CENTER); t.setPadding(dp(18), 0, dp(18), 0); t.setMinHeight(dp(46));
        t.setBackground(ripple(primary ? round(NTheme.accent, 14, 0) : round(NTheme.surface2, 14, NTheme.line2), 14));
        tap(t, l);
        return t;
    }

    /* .btn.sm: 38 high, 14 side padding, radius 12, 13.5px (primary or plain surface) */
    static TextView btnSm(Context c, String label, boolean primary, View.OnClickListener l) {
        TextView t = body(c, label, 13.5f, primary ? NTheme.onAccent : NTheme.text, 600);
        t.setSingleLine(true); t.setGravity(Gravity.CENTER); t.setPadding(dp(14), 0, dp(14), 0); t.setMinHeight(dp(38));   /* singleLine first: it resets the min height to one line */
        t.setBackground(ripple(primary ? round(NTheme.accent, 12, 0) : round(NTheme.surface, 12, NTheme.line2), 12));
        tap(t, l);
        return t;
    }

    static TextView chip(Context c, String label, boolean on, View.OnClickListener l) {
        TextView t = body(c, label, 14, on ? NTheme.onAccent : NTheme.text, 600);
        t.setGravity(Gravity.CENTER); t.setPadding(dp(14), dp(8), dp(14), dp(8));
        t.setBackground(ripple(on ? round(NTheme.accent, 99, 0) : round(NTheme.surface, 99, NTheme.line2), 99));
        tap(t, l);
        return t;
    }

    static TextView link(Context c, String label, View.OnClickListener l) {
        TextView t = body(c, label, 15, NTheme.accent, 600); t.setPadding(dp(6), dp(8), dp(6), dp(8)); tap(t, l); return t;
    }

    /* section header: big title on the left, an optional link on the right */
    static LinearLayout sectionHead(Context c, String title, String linkLabel, View.OnClickListener l) {
        LinearLayout r = row(c); r.setPadding(0, dp(44), 0, dp(14)); r.setGravity(Gravity.BOTTOM);
        TextView t = title(c, title, 24); t.setTypeface(NFont.display(700)); r.addView(t, lpw(0, -2, 1));
        if (linkLabel != null) { TextView k = link(c, linkLabel, l); k.setPadding(0, 0, 0, 0); r.addView(k); }
        return r;
    }

    static View space(Context c, int h) { View v = new View(c); v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(h))); return v; }

    static void fadeIn(View v, long delay) {
        v.setAlpha(0f); v.setTranslationY(dp(10));
        v.animate().alpha(1f).translationY(0).setStartDelay(delay).setDuration(260).setInterpolator(EASE).start();
    }

    static ValueAnimator anim(float a, float b, long ms, ValueAnimator.AnimatorUpdateListener l) {
        ValueAnimator va = ValueAnimator.ofFloat(a, b); va.setDuration(ms); va.setInterpolator(EASE); va.addUpdateListener(l); va.start(); return va;
    }

    static int textOn(int bg) { return NTheme.on(bg); }

    static void clear(ViewGroup g) { g.removeAllViews(); }

    static int col(String hex, int def) { try { return Color.parseColor(hex); } catch (Exception e) { return def; } }

    /* CSS line-height for a TextView (e.g. font: 800 84px/.82): every line box is exactly lh x font size,
       with the extra (or missing) space split above and below like CSS half-leading. Glyphs may overflow. */
    static final class CssLh implements android.text.style.LineHeightSpan {
        final int px; CssLh(int px) { this.px = px; }
        @Override public void chooseHeight(CharSequence t, int s, int e, int sv, int v, android.graphics.Paint.FontMetricsInt fm) {
            int d = (fm.descent - fm.ascent) - px; fm.ascent += d / 2; fm.descent = fm.ascent + px; fm.top = fm.ascent; fm.bottom = fm.descent;
        }
    }
    static TextView cssLh(TextView t, float lh) {
        android.text.SpannableString ss = new android.text.SpannableString(t.getText());
        ss.setSpan(new CssLh(Math.round(t.getTextSize() * lh)), 0, ss.length(), android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        t.setIncludeFontPadding(false); t.setFallbackLineSpacing(false); t.setLineSpacing(0, 1f); t.setText(ss); return t;
    }
}
