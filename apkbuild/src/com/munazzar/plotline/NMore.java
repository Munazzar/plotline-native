package com.munazzar.plotline;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import org.json.JSONObject;

/* Menu rows for the options sheets, and the shortcuts that run the web app's own actions (reminders, auto check-off,
   focus timer, routines, sharing…) so every 1.13 feature stays one tap away from the native screens. */
final class NMore {
    static View row(Context c, String emoji, String title, String sub, boolean danger, View.OnClickListener l) {
        LinearLayout r = NUi.row(c);
        r.setBackground(NUi.ripple(NUi.round(NTheme.surface, 18, NTheme.line), 18));
        r.setPadding(NUi.dp(16), NUi.dp(14), NUi.dp(16), NUi.dp(14));
        r.addView(NUi.text(c, emoji, 22, NTheme.text), NUi.lp(NUi.dp(40), -2));
        LinearLayout tx = NUi.col(c); tx.addView(NUi.body(c, title, 16, danger ? NTheme.LATE : NTheme.text, 700));
        if (sub != null && !sub.isEmpty()) tx.addView(NUi.text(c, sub, 13, NTheme.muted));
        r.addView(tx, NUi.lpw(0, -2, 1));
        NUi.tap(r, l);
        return r;
    }

    static JSONObject d(String... kv) {
        JSONObject o = new JSONObject();
        try { for (int i = 0; i + 1 < kv.length; i += 2) o.put(kv[i], kv[i + 1]); } catch (Exception ignored) { }
        return o;
    }


    static void add(LinearLayout b, View v) { b.addView(v, NUi.mt(8)); }
}
