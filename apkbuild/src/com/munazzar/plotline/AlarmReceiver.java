package com.munazzar.plotline;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import org.json.JSONObject;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        JSONObject o;
        try { o = new JSONObject(i.getStringExtra("o") == null ? "{}" : i.getStringExtra("o")); } catch (Exception e) { o = new JSONObject(); }
        /* reminders scheduled by an older version carried title/body extras only */
        try { if (!o.has("title") && i.getStringExtra("title") != null) { o.put("title", i.getStringExtra("title")); o.put("body", i.getStringExtra("body")); } } catch (Exception ignored) { }
        Reminders.show(c, o, i.getIntExtra("id", 0));
    }
}
