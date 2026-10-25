package com.example.daylight;

import android.app.*;
import android.content.*;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

public class ReminderReceiver extends BroadcastReceiver {
    static final String FALLBACK_CHANNEL = "haru_alarm_fallback";
    @Override public void onReceive(Context context, Intent intent) {
        int id=intent.getIntExtra("id",1);
        String title=intent.getStringExtra("title");
        String body=intent.getStringExtra("body");
        boolean daily=intent.getBooleanExtra("daily",false);
        int hour=intent.getIntExtra("hour",0),minute=intent.getIntExtra("minute",0);
        boolean exact=intent.getBooleanExtra("exact",false);
        if(daily)MainActivity.scheduleRoutine(context,id,title==null?"Daily routine":title,hour,minute);
        if(exact){
            try{
                Intent ring=new Intent(context,AlarmSoundService.class);
                ring.setAction(AlarmSoundService.ACTION_RING);
                ring.putExtra("id",id);ring.putExtra("title",title);ring.putExtra("body",body);ring.putExtra("daily",daily);ring.putExtra("hour",hour);ring.putExtra("minute",minute);
                if(Build.VERSION.SDK_INT>=26)context.startForegroundService(ring);else context.startService(ring);
                return;
            }catch(RuntimeException ignored){
                // If Android blocks foreground audio, still show a normal audible reminder below.
            }
        }
        showFallback(context,id,title,body);
    }
    static void showFallback(Context context,int id,String title,String body){
        NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel channel=new NotificationChannel(FALLBACK_CHANNEL,"Haru alarm fallback",NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Audible fallback when precise alarm access is off");
            Uri tone=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if(tone==null)tone=Settings.System.DEFAULT_NOTIFICATION_URI;
            channel.setSound(tone,new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            channel.enableVibration(true);channel.setVibrationPattern(new long[]{0,500,500,500});manager.createNotificationChannel(channel);
        }
        Intent open=new Intent(context,AlarmRingingActivity.class);open.putExtra("id",id);open.putExtra("title",title);open.putExtra("body",body);
        PendingIntent tap=PendingIntent.getActivity(context,id,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(context,FALLBACK_CHANNEL):new Notification.Builder(context);
        Notification n=b.setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle(title==null?"Haru reminder":title).setContentText(body==null?"Take one small step.":body).setCategory(Notification.CATEGORY_ALARM).setPriority(Notification.PRIORITY_MAX).setContentIntent(tap).setAutoCancel(true).build();
        manager.notify(id,n);
    }
}
