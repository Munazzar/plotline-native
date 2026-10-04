package com.munazzar.plotline;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* The web's mdLite for assistant answers: **bold**, *italic*, bullets and numbered lines, # headings, and [1] / [2,3]
   citations that open the matching source. */
final class NMd {
    interface Cite { void on(int i); }

    static View render(Context c, String s, int n, Cite cite) {
        LinearLayout box = NUi.col(c);
        s = (s == null ? "" : s).replaceAll("(?s)<think>.*?(</think>|$)", "").trim();
        Pattern bullet = Pattern.compile("^\\s*(?:[-*•]|\\d+[.)])\\s+(.*)");
        for (String line : s.split("\n")) {
            Matcher m = bullet.matcher(line);
            boolean li = m.matches();
            String t = li ? m.group(1) : line;
            boolean h = !li && line.matches("^\\s*#{1,4}\\s+.*");
            if (h) t = line.replaceFirst("^\\s*#+\\s+", "");
            if (t.trim().isEmpty()) continue;
            TextView tv = NUi.text(c, "", h ? 17 : 15.5f, NTheme.text);
            tv.setLineSpacing(0, 1.25f);
            SpannableStringBuilder sb = inline(t, n, cite, h || t.startsWith("Next step:"));
            if (li) { sb.insert(0, "•  "); tv.setPadding(NUi.dp(4), 0, 0, 0); }
            tv.setText(sb); tv.setMovementMethod(LinkMovementMethod.getInstance());
            if (h) tv.setTypeface(NFont.display(800));
            LinearLayout.LayoutParams l = NUi.mt(h ? 14 : li ? 6 : 10);
            box.addView(tv, l);
        }
        return box;
    }

    static SpannableStringBuilder inline(String t, final int n, final Cite cite, boolean allBold) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        Pattern p = Pattern.compile("\\*\\*(.+?)\\*\\*|(?<![\\w*])\\*(?!\\s)([^*\\n]+?)\\*(?![\\w*])|\\[(\\d{1,2}(?:\\s*[,–-]\\s*\\d{1,2})*)\\]");
        Matcher m = p.matcher(t); int last = 0;
        while (m.find()) {
            sb.append(t, last, m.start()); last = m.end();
            int a = sb.length();
            if (m.group(1) != null) { sb.append(m.group(1)); sb.setSpan(new StyleSpan(Typeface.BOLD), a, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); }
            else if (m.group(2) != null) { sb.append(m.group(2)); sb.setSpan(new StyleSpan(Typeface.ITALIC), a, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); }
            else {
                boolean any = false;
                for (String part : m.group(3).split("\\s*,\\s*")) {
                    String[] r = part.split("\\s*[–-]\\s*"); int lo = Integer.parseInt(r[0].trim()), hi = r.length > 1 ? Integer.parseInt(r[1].trim()) : lo;
                    for (int k = lo; k <= hi && k < lo + 8; k++) {
                        if (k < 1 || k > n) continue;
                        final int idx = k - 1; int s0 = sb.length(); sb.append(any ? " " : "").append(" ").append(String.valueOf(k)).append(" "); any = true;
                        sb.setSpan(new ClickableSpan() { @Override public void onClick(View v) { if (cite != null) cite.on(idx); }
                            @Override public void updateDrawState(TextPaint ds) { ds.setColor(NTheme.accent); ds.setUnderlineText(false); ds.setFakeBoldText(true); ds.bgColor = NUi.mix(NTheme.accent, .16f, NTheme.bg); } }, s0, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
                if (!any) sb.append(m.group(0));
            }
        }
        sb.append(t.substring(last));
        if (allBold) sb.setSpan(new StyleSpan(Typeface.BOLD), 0, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb;
    }
}
