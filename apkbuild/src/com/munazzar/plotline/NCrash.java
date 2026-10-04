package com.munazzar.plotline;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.io.PrintWriter;
import java.io.StringWriter;

/* Keeps the last errors so a problem in a native screen can be shown (and copied) instead of closing the app.
   A crash during start-up switches the next launch to the classic screens until the person turns native back on. */
final class NCrash {
    static SharedPreferences p(Context c) { return c.getSharedPreferences("plotline_native", 0); }

    static String trace(Throwable e) { StringWriter w = new StringWriter(); e.printStackTrace(new PrintWriter(w)); return w.toString(); }

    static void log(Context c, String where, Throwable e) {
        Log.e("PlotlineNative", where, e);
        try {
            String prev = p(c).getString("log", "");
            String add = System.currentTimeMillis() + " " + where + "\n" + trace(e);
            String all = add + "\n" + prev;
            if (all.length() > 12000) all = all.substring(0, 12000);
            p(c).edit().putString("log", all).apply();
        } catch (Exception ignored) { }
    }

    static void install(final Context c) {
        final Thread.UncaughtExceptionHandler def = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                try {
                    p(c).edit().putString("crash", trace(e)).putLong("crashAt", System.currentTimeMillis())
                        .putInt("crashes", p(c).getInt("crashes", 0) + 1).commit();
                } catch (Exception ignored) { }
                if (def != null) def.uncaughtException(t, e);
            }
        });
    }

    static String lastCrash(Context c) { return p(c).getString("crash", ""); }

    static void clear(Context c) { p(c).edit().remove("crash").putInt("crashes", 0).apply(); }

    /* two crashes in a row start the next launch in classic mode */
    static boolean nativeOn(Context c) { return p(c).getBoolean("native", true) && p(c).getInt("crashes", 0) < 2; }

    static void setNative(Context c, boolean on) { p(c).edit().putBoolean("native", on).putInt("crashes", 0).apply(); }
}
