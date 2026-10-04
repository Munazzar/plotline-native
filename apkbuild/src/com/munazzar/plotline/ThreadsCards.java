package com.munazzar.plotline;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/* Supplies the cards for the Threads widget's stack. */
public class ThreadsCards extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) { return new F(getApplicationContext()); }

    static String ago(long t) {
        if (t <= 0) return "";
        long m = (System.currentTimeMillis() - t) / 60000;
        if (m < 1) return "just now";
        if (m < 60) return m + "m ago";
        if (m < 60 * 24) return (m / 60) + "h ago";
        long d = m / (60 * 24);
        return d == 1 ? "yesterday" : d < 7 ? d + " days ago" : W.fmt(new java.util.Date(t), "MMM d");
    }

    static final class F implements RemoteViewsFactory {
        final Context c; final List<JSONObject> items = new ArrayList<>();
        F(Context c) { this.c = c; }
        public void onCreate() { }
        public void onDataSetChanged() {
            items.clear();
            JSONArray ts = W.data(c).optJSONArray("thr");
            if (ts != null) for (int i = 0; i < ts.length(); i++) { JSONObject o = ts.optJSONObject(i); if (o != null) items.add(o); }
        }
        public void onDestroy() { items.clear(); }
        public int getCount() { return items.size() + 1; }
        public RemoteViews getLoadingView() { return null; }
        public int getViewTypeCount() { return 1; }
        public long getItemId(int p) { return p; }
        public boolean hasStableIds() { return false; }

        public RemoteViews getViewAt(int p) {
            RemoteViews v = W.rv(c, "widget_thr_card");
            boolean isNew = p >= items.size();
            String id = "";
            if (isNew) {
                v.setTextViewText(W.id(c, "c_t"), "＋  New thread");
                v.setTextViewText(W.id(c, "c_x"), "Start a thread by typing or speaking. Name it and add the first line in one go.");
                v.setTextViewText(W.id(c, "c_when"), items.isEmpty() ? "" : (items.size() + " threads · swipe"));
                v.setTextViewText(W.id(c, "c_add"), "+ New");
            } else {
                JSONObject o = items.get(p); id = o.optString("id");
                v.setTextViewText(W.id(c, "c_t"), o.optString("e", "🧵") + "  " + o.optString("t"));
                String x = o.optString("x");
                v.setTextViewText(W.id(c, "c_x"), x.isEmpty() ? "Nothing logged yet" : x);
                String when = ago(o.optLong("at"));
                v.setTextViewText(W.id(c, "c_when"), (p + 1) + "/" + items.size() + (when.isEmpty() ? "" : " · " + when));
                v.setTextViewText(W.id(c, "c_add"), "+ Add");
                Intent open = new Intent(); open.putExtra("route", "thread/" + id);
                v.setOnClickFillInIntent(W.id(c, "c_card"), open);
            }
            Intent add = new Intent(); add.putExtra("thread", id);
            v.setOnClickFillInIntent(W.id(c, "c_add"), add);
            Intent mic = new Intent(); mic.putExtra("thread", id); mic.putExtra("voice", true);
            v.setOnClickFillInIntent(W.id(c, "c_mic"), mic);
            if (isNew) v.setOnClickFillInIntent(W.id(c, "c_card"), add);
            return v;
        }
    }
}
