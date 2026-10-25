package com.example.daylight;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

/** Gives channels Mitrio IDs while carrying forward Android's saved user settings. */
final class MitrioNotificationChannels {
    static final String RINGING = "mitrio_ringing_alarm";
    static final String FALLBACK = "mitrio_alarm_fallback";
    private static final String OLD_RINGING = "haru_ringing_alarm";
    private static final String OLD_FALLBACK = "haru_alarm_fallback";

    static void createRinging(Context context) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        createOrCarry(manager, RINGING, OLD_RINGING, "Mitrio ringing alarms",
                "Ongoing alarm tone. Stop or snooze it from the alarm screen or notification.",
                NotificationManager.IMPORTANCE_HIGH, null, null, false, null);
    }

    static void createFallback(Context context, boolean quiet, Uri sound, AudioAttributes audio) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        createOrCarry(manager, FALLBACK, OLD_FALLBACK,
                quiet ? "Mitrio gentle reminders" : "Mitrio alarm fallback",
                quiet ? "Quiet notification-only reminders" : "Audible fallback when precise alarm access is off",
                quiet ? NotificationManager.IMPORTANCE_DEFAULT : NotificationManager.IMPORTANCE_HIGH,
                quiet ? null : sound, audio, !quiet, new long[]{0,500,500,500});
    }

    private static void createOrCarry(NotificationManager manager, String id, String oldId,
                                      String name, String description, int importance,
                                      Uri sound, AudioAttributes audio, boolean vibrate, long[] pattern) {
        NotificationChannel existing = manager.getNotificationChannel(id);
        if (existing != null) return;
        NotificationChannel previous = manager.getNotificationChannel(oldId);
        NotificationChannel channel = new NotificationChannel(id, name,
                previous == null ? importance : previous.getImportance());
        channel.setDescription(description);
        if (previous != null) {
            channel.setSound(previous.getSound(), previous.getAudioAttributes());
            channel.enableVibration(previous.shouldVibrate());
            long[] previousPattern = previous.getVibrationPattern();
            if (previousPattern != null) channel.setVibrationPattern(previousPattern);
            channel.enableLights(previous.shouldShowLights());
            channel.setLightColor(previous.getLightColor());
            channel.setLockscreenVisibility(previous.getLockscreenVisibility());
            channel.setShowBadge(previous.canShowBadge());
        } else {
            channel.setSound(sound, audio);
            channel.enableVibration(vibrate);
            if (pattern != null) channel.setVibrationPattern(pattern);
        }
        manager.createNotificationChannel(channel);
        if (previous != null) manager.deleteNotificationChannel(oldId);
    }

    private MitrioNotificationChannels() {}
}
