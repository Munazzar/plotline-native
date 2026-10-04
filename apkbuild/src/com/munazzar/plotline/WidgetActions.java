package com.munazzar.plotline;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import org.json.JSONArray;
import org.json.JSONObject;

/* Check-offs tapped on a home-screen widget. They update the widget at once and wait in a queue until the app next opens. */
public class WidgetActions extends BroadcastReceiver {
    static final String DAY = "com.munazzar.plotline.W_DAY", STEP = "com.munazzar.plotline.W_STEP", HABIT = "com.munazzar.plotline.W_HABIT";

    @Override
    public void onReceive(Context c, Intent i) {
        try {
            JSONObject a = new JSONObject();
            a.put("at", System.currentTimeMillis());
            if (DAY.equals(i.getAction())) {
                String id = i.getStringExtra("id");
                boolean done = false;
                JSONArray days = W.data(c).optJSONArray("days");
                if (days != null) for (int k = 0; k < days.length(); k++) {
                    JSONObject d = days.optJSONObject(k);
                    if (d != null && id.equals(d.optString("id"))) done = d.optBoolean("done");
                }
                a.put("k", "day"); a.put("id", id); a.put("done", !done);
            } else if (STEP.equals(i.getAction())) {
                a.put("k", "step"); a.put("g", i.getStringExtra("g")); a.put("s", i.getStringExtra("s"));
            } else if (HABIT.equals(i.getAction())) {
                String id = i.getStringExtra("id"), d = i.getStringExtra("d"), mode = i.getStringExtra("mode");
                if (id == null || d == null) return;
                JSONObject h = W.habit(c, id);
                int cur = h == null ? 0 : Math.max(0, W.hval(h, d)), n = h == null ? 1 : Math.max(1, h.optInt("n", 1)), v;
                if ("set".equals(mode)) v = i.getIntExtra("v", n);
                else if ("inc".equals(mode)) v = cur + 1;
                else if ("full".equals(mode)) v = cur >= n ? 0 : n;
                else v = cur >= n ? 0 : cur + 1;
                a.put("k", "habit"); a.put("id", id); a.put("d", d); a.put("v", v);
                int nid = i.getIntExtra("nid", 0);
                if (nid != 0) try { c.getSystemService(android.app.NotificationManager.class).cancel(nid); } catch (Exception ignored) { }
            } else return;
            W.enqueue(c, a);
        } catch (Exception ignored) { }
        W.updateAll(c);
    }
}
