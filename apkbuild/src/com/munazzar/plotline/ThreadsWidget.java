package com.munazzar.plotline;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

/* Threads: a small stack of cards, one per thread (newest first) plus a "New thread" card at the end.
   Swipe up or down inside the widget to pick a thread, then + Add to type or the mic to speak an update;
   tapping the card opens the thread. Everything typed or said is queued (W.enqueue) and applied by the app the
   next time it opens; the card shows it right away. */
public class ThreadsWidget extends AppWidgetProvider {
    static final int FLAG_MUTABLE = 0x02000000; // PendingIntent.FLAG_MUTABLE (API 31), needed for fill-in intents

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) m.updateAppWidget(id, build(c));
        m.notifyAppWidgetViewDataChanged(ids, W.id(c, "w_stack"));
    }

    static void updateAll(Context c) {
        W.update(c, ThreadsWidget.class, new W.Builder() { public RemoteViews build(Context x) { return ThreadsWidget.build(x); } });
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, ThreadsWidget.class));
        if (ids != null && ids.length > 0) m.notifyAppWidgetViewDataChanged(ids, W.id(c, "w_stack"));
    }

    static PendingIntent quick(Context c, String thread, boolean voice, int req) {
        Intent i = new Intent(c, QuickLog.class);
        i.setAction("com.munazzar.plotline.QUICK." + req);
        if (thread != null) i.putExtra("thread", thread);
        if (voice) i.putExtra("voice", true);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return PendingIntent.getActivity(c, req, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static RemoteViews build(Context c) {
        RemoteViews v = W.rv(c, "widget_threads");
        int stack = W.id(c, "w_stack");
        Intent svc = new Intent(c, ThreadsCards.class);
        svc.setData(Uri.parse("plotline://threads-cards"));
        v.setRemoteAdapter(stack, svc);
        v.setEmptyView(stack, W.id(c, "w_empty"));
        Intent t = new Intent(c, QuickLog.class);
        t.setAction("com.munazzar.plotline.QUICK.CARD");
        t.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        v.setPendingIntentTemplate(stack, PendingIntent.getActivity(c, 66, t, FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        v.setOnClickPendingIntent(W.id(c, "w_add"), quick(c, "", false, 62));
        v.setOnClickPendingIntent(W.id(c, "w_thr_title"), W.open(c, "threads", 61));
        return v;
    }
}
