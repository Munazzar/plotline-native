package com.munazzar.plotline;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        String a = i.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(a) || "android.intent.action.MY_PACKAGE_REPLACED".equals(a)
                || "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED".equals(a)) {
            Reminders.rearm(c);
            Notify.rearmSnoozes(c);
            Auto.arm(c);
            ShareCheck.arm(c);
            LiveShare.start(c);
        }
    }
}
