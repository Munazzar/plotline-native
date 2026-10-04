package com.munazzar.plotline;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/* Date helpers that match the web app exactly: dates are "yyyy-MM-dd" strings in local time,
   day numbers are days since 1970-01-01 (UTC), day of week 0 = Sunday. */
final class NDates {
    static final String[] DAYS = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
    static final String[] MONTHS = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
    static final String[] MON = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    static String ymd() { return ymd(Calendar.getInstance()); }

    static String ymd(Calendar c) {
        return String.format(Locale.US, "%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    static String ymd(long t) { Calendar c = Calendar.getInstance(); c.setTimeInMillis(t); return ymd(c); }

    static boolean valid(String s) { return s != null && s.matches("\\d{4}-\\d{2}-\\d{2}"); }

    static int dnum(String s) {
        if (!valid(s)) return 0;
        int y = Integer.parseInt(s.substring(0, 4)), m = Integer.parseInt(s.substring(5, 7)), d = Integer.parseInt(s.substring(8, 10));
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.clear(); c.set(y, m - 1, d);
        return (int) Math.round(c.getTimeInMillis() / 86400000.0);
    }

    static String fromN(int n) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.setTimeInMillis(n * 86400000L);
        return String.format(Locale.US, "%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    static int today() { return dnum(ymd()); }

    /* 0 = Sunday */
    static int dow(int n) { return (int) (((n + 4) % 7 + 7) % 7); }

    static int dow(String s) { return dow(dnum(s)); }

    static int wkStart(int n) { return n - dow(n); }

    static int daysUntil(String s) { return dnum(s) - today(); }

    static Calendar cal(String s) {
        Calendar c = Calendar.getInstance(); c.clear();
        c.set(Integer.parseInt(s.substring(0, 4)), Integer.parseInt(s.substring(5, 7)) - 1, Integer.parseInt(s.substring(8, 10)));
        return c;
    }

    static int day(String s) { return Integer.parseInt(s.substring(8, 10)); }

    static int month(String s) { return Integer.parseInt(s.substring(5, 7)) - 1; }

    static int year(String s) { return Integer.parseInt(s.substring(0, 4)); }

    static String fmtDate(String s) {
        if (!valid(s)) return "";
        String r = MON[month(s)] + " " + day(s);
        if (year(s) != Calendar.getInstance().get(Calendar.YEAR)) r += ", " + year(s);
        return r;
    }

    /* "Today", "Tomorrow", "Yesterday" or "Mon, Oct 5" */
    static String dayName(String s) {
        if (!valid(s)) return "";
        int n = daysUntil(s);
        if (n == 0) return "Today";
        if (n == 1) return "Tomorrow";
        if (n == -1) return "Yesterday";
        return DAYS[dow(s)] + ", " + MON[month(s)] + " " + day(s);
    }

    static String longDate(String s) {
        String[] full = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        return full[dow(s)] + ", " + MONTHS[month(s)] + " " + day(s);
    }

    /* "18:00" -> "6:00 PM" */
    static String fmtTime(String t) {
        if (t == null || !t.matches("\\d{1,2}:\\d{2}")) return "";
        String[] p = t.split(":");
        int h = Integer.parseInt(p[0]), m = Integer.parseInt(p[1]);
        Calendar c = Calendar.getInstance(); c.set(2000, 0, 1, h, m);
        return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(c.getTime());
    }

    static String fmtClock(long t) { return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date(t)); }

    /* "Today", "Yesterday" or "Mon, Oct 5" for a timestamp */
    static String dayLabel(long t) { return dayName(ymd(t)); }

    static String ago(long t) {
        if (t <= 0) return "";
        long m = (System.currentTimeMillis() - t) / 60000;
        if (m < 1) return "just now";
        if (m < 60) return m + " min ago";
        if (m < 60 * 24) return (m / 60) + " h ago";
        long d = m / (60 * 24);
        return d == 1 ? "yesterday" : d < 7 ? d + " days ago" : fmtDate(ymd(t));
    }

    static String greeting() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return h < 5 ? "Good night" : h < 12 ? "Good morning" : h < 17 ? "Good afternoon" : "Good evening";
    }

    static String dueShort(String due) {
        if (!valid(due)) return "";
        int n = daysUntil(due);
        return n < 0 ? (-n) + "d late" : n == 0 ? "Today" : n == 1 ? "Tomorrow" : n < 7 ? "In " + n + "d" : fmtDate(due);
    }

    static String dueText(String due, String time) {
        if (!valid(due)) return "";
        int n = daysUntil(due);
        String r = n < 0 ? (-n) + " day" + (n == -1 ? "" : "s") + " late" : n == 0 ? "Due today" : n == 1 ? "Due tomorrow" : n < 7 ? "Due in " + n + " days" : "Due " + fmtDate(due);
        String tm = fmtTime(time);
        return tm.isEmpty() ? r : r + " · " + tm;
    }

    static String fmtMin(int m) {
        m = Math.max(0, m);
        if (m < 60) return m + "m";
        int h = m / 60, r = m % 60;
        return r > 0 ? h + "h " + String.format(Locale.US, "%02d", r) + "m" : h + "h";
    }

    static String weekday(String s) {
        String[] full = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        return full[dow(s)];
    }

    static String longDateYear(String s) { return longDate(s) + ", " + year(s); }
}
