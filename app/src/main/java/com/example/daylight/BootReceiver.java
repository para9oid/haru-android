package com.example.daylight;

import android.content.*;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            MainActivity.scheduleSaved(context);
            MainActivity.scheduleFutureTaskReminders(context);
        }
    }
}
