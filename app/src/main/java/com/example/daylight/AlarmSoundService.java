package com.example.daylight;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;

/** Plays the selected Android alarm tone continuously until the person stops or snoozes it. */
public class AlarmSoundService extends Service {
    static final String ACTION_RING="com.example.daylight.action.RING";
    static final String ACTION_STOP="com.example.daylight.action.STOP_ALARM";
    static final String ACTION_SNOOZE="com.example.daylight.action.SNOOZE_ALARM";
    static final String CHANNEL="haru_ringing_alarm";
    static final int NOTIFICATION_OFFSET=0x30000000;
    MediaPlayer player;
    Vibrator vibrator;
    int alarmId=1;
    boolean testMode;

    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null){stopSelf();return START_NOT_STICKY;}
        alarmId=intent.getIntExtra("id",1);
        testMode=intent.getBooleanExtra("test",false);
        String action=intent.getAction();
        if(ACTION_STOP.equals(action)){stopAlarm();return START_NOT_STICKY;}
        if(ACTION_SNOOZE.equals(action)){scheduleSnooze(this,alarmId,intent.getStringExtra("title"),intent.getStringExtra("body"));stopAlarm();return START_NOT_STICKY;}
        String title=intent.getStringExtra("title");if(title==null||title.trim().isEmpty())title="A gentle reminder";
        String body=intent.getStringExtra("body");if(body==null)body="One small step is enough.";
        createChannel();Notification notification=buildNotification(alarmId,title,body);
        if(Build.VERSION.SDK_INT>=29)startForeground(NOTIFICATION_OFFSET+alarmId,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        else startForeground(NOTIFICATION_OFFSET+alarmId,notification);
        startTone();startVibration();return START_NOT_STICKY;
    }
    void createChannel(){
        if(Build.VERSION.SDK_INT>=26){NotificationManager manager=getSystemService(NotificationManager.class);NotificationChannel channel=new NotificationChannel(CHANNEL,"Mitrio ringing alarms",NotificationManager.IMPORTANCE_HIGH);channel.setDescription("Ongoing alarm tone. Stop or snooze it from the alarm screen or notification.");channel.setSound(null,null);channel.enableVibration(false);manager.createNotificationChannel(channel);}
    }
    Notification buildNotification(int id,String title,String body){
        boolean test=testMode;
        Intent open=new Intent(this,AlarmRingingActivity.class);open.putExtra("id",id);open.putExtra("title",title);open.putExtra("body",body);open.putExtra("test",test);
        PendingIntent screen=PendingIntent.getActivity(this,id+210000,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Intent stop=new Intent(this,AlarmRingingActivity.class);stop.setAction(ACTION_STOP);stop.putExtra("id",id);stop.putExtra("title",title);stop.putExtra("body",body);stop.putExtra("test",test);
        PendingIntent stopPi=PendingIntent.getActivity(this,id+310000,stop,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Intent snooze=new Intent(this,AlarmRingingActivity.class);snooze.setAction(ACTION_SNOOZE);snooze.putExtra("id",id);snooze.putExtra("title",title);snooze.putExtra("body",body);
        PendingIntent snoozePi=PendingIntent.getActivity(this,id+410000,snooze,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle(title).setContentText(body).setCategory(Notification.CATEGORY_ALARM).setPriority(Notification.PRIORITY_MAX).setOngoing(true).setAutoCancel(false).setContentIntent(screen).setFullScreenIntent(screen,true);
        if(!test)b.addAction(android.R.drawable.ic_media_pause,"Snooze 5 min",snoozePi);
        return b.addAction(android.R.drawable.ic_menu_close_clear_cancel,test?"Stop test":"Stop",stopPi).build();
    }
    void startTone(){
        try{
            Uri tone=RingtoneManager.getActualDefaultRingtoneUri(this,RingtoneManager.TYPE_ALARM);
            if(tone==null)tone=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if(tone==null)tone=Settings.System.DEFAULT_ALARM_ALERT_URI;
            if(tone==null)tone=Settings.System.DEFAULT_NOTIFICATION_URI;
            player=new MediaPlayer();player.setWakeMode(this,PowerManager.PARTIAL_WAKE_LOCK);player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());player.setDataSource(this,tone);player.setLooping(true);player.setOnPreparedListener(mp->mp.start());player.setOnErrorListener((mp,what,extra)->{ReminderReceiver.showFallback(this,alarmId,"Mitrio alarm","The alarm tone could not start. Check your alarm sound settings.");stopAlarm();return true;});player.prepareAsync();
        }catch(Exception e){ReminderReceiver.showFallback(this,alarmId,"Mitrio alarm","The alarm tone could not start. Check your alarm sound settings.");stopAlarm();}
    }
    void startVibration(){
        try{vibrator=(Vibrator)getSystemService(VIBRATOR_SERVICE);if(vibrator!=null&&vibrator.hasVibrator()){long[] pattern={0,500,500,500};vibrator.vibrate(VibrationEffect.createWaveform(pattern,0));}}catch(Exception ignored){}
    }
    static void stop(Context context,int id){context.stopService(new Intent(context,AlarmSoundService.class));((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(NOTIFICATION_OFFSET+id);}
    static void snooze(Context context,int id,String title,String body){scheduleSnooze(context,id,title,body);stop(context,id);}
    static void scheduleSnooze(Context context,int id,String title,String body){scheduleSnoozeMinutes(context,id,title,body,5);}
    static void scheduleSnoozeMinutes(Context context,int id,String title,String body,int minutes){AlarmManager am=(AlarmManager)context.getSystemService(ALARM_SERVICE);long at=System.currentTimeMillis()+Math.max(1,minutes)*60*1000L;MainActivity.scheduleAlarm(context,id,title,body==null?"Snooze is over. One small step.":body,false,0,0,at,"com.example.daylight.action.SNOOZE_TRIGGER."+id);}
    void stopAlarm(){
        try{if(player!=null){if(player.isPlaying())player.stop();player.release();player=null;}}catch(Exception ignored){}
        try{if(vibrator!=null){vibrator.cancel();vibrator=null;}}catch(Exception ignored){}
        stopForeground(STOP_FOREGROUND_REMOVE);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(NOTIFICATION_OFFSET+alarmId);stopSelf();
    }
    @Override public void onDestroy(){try{if(player!=null){if(player.isPlaying())player.stop();player.release();}}catch(Exception ignored){}try{if(vibrator!=null)vibrator.cancel();}catch(Exception ignored){}super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
