package com.example.daylight;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.database.Cursor;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Runs only when the user opts in and Android reports the phone idle and charging. */
public final class LocalLearningJobService extends JobService {
    private static final int JOB_ID = 52009;
    private static final long LEARNING_INTERVAL_MS = 24L * 60L * 60L * 1000L;
    private static final long LEARNING_FLEX_MS = 20L * 60L * 60L * 1000L;
    private volatile boolean stopped;

    public static void schedule(Context context) {
        android.content.SharedPreferences prefs=MitrioPreferences.get(context);
        if(!prefs.getBoolean("background_learning_enabled",false)||!prefs.getBoolean("personal_context",true))return;
        String mode=prefs.getString("ai_mode",MainActivity.MODEL_OFF);
        if(MainActivity.MODEL_OFF.equals(mode))return;
        File dir=context.getExternalFilesDir("models");if(dir==null)dir=new File(context.getFilesDir(),"models");
        File model=new File(dir,MainActivity.MODEL_SMALL.equals(mode)?MainActivity.SMALL_FILE:MainActivity.STANDARD_FILE);
        long expected=MainActivity.MODEL_SMALL.equals(mode)?MainActivity.SMALL_BYTES:MainActivity.STANDARD_BYTES;
        if(!model.isFile()||model.length()!=expected)return;
        JobScheduler scheduler=(JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if(scheduler==null||scheduler.getPendingJob(JOB_ID)!=null)return;
        JobInfo job=new JobInfo.Builder(JOB_ID,new ComponentName(context,LocalLearningJobService.class))
                .setRequiresCharging(true)
                .setRequiresDeviceIdle(true)
                .setPeriodic(LEARNING_INTERVAL_MS,LEARNING_FLEX_MS)
                .setPersisted(true)
                .build();
        scheduler.schedule(job);
    }

    public static void disableAndClear(Context context) {
        JobScheduler scheduler=(JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if(scheduler!=null)scheduler.cancel(JOB_ID);
        MitrioPreferences.get(context).edit()
                .putBoolean("background_learning_enabled",false)
                .remove("background_learning_summary")
                .remove("background_learning_updated")
                .remove("background_learning_last_attempt")
                .apply();
    }

    public static void pause(Context context) {
        JobScheduler scheduler=(JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if(scheduler!=null)scheduler.cancel(JOB_ID);
    }

    @Override public boolean onStartJob(JobParameters params) {
        stopped=false;
        android.content.SharedPreferences prefs=MitrioPreferences.get(this);
        if(!prefs.getBoolean("background_learning_enabled",false)||!prefs.getBoolean("personal_context",true))return false;
        String mode=prefs.getString("ai_mode",MainActivity.MODEL_OFF);
        File dir=getExternalFilesDir("models");if(dir==null)dir=new File(getFilesDir(),"models");
        File model=new File(dir,MainActivity.MODEL_SMALL.equals(mode)?MainActivity.SMALL_FILE:MainActivity.STANDARD_FILE);
        long expected=MainActivity.MODEL_SMALL.equals(mode)?MainActivity.SMALL_BYTES:MainActivity.STANDARD_BYTES;
        if(MainActivity.MODEL_OFF.equals(mode)||!model.isFile()||model.length()!=expected)return false;

        long now=System.currentTimeMillis();
        long previousAttempt=prefs.getLong("background_learning_last_attempt",0L);
        if(previousAttempt>0L&&now-previousAttempt<LEARNING_INTERVAL_MS)return false;
        prefs.edit().putLong("background_learning_last_attempt",now).apply();

        String conversations=recentUserMessages();
        String routines=localRoutineSummary();
        LocalAi.submitBackground(()->{
            try{
                if(stopped||!MitrioPreferences.get(this).getBoolean("background_learning_enabled",false)||!MitrioPreferences.get(this).getBoolean("personal_context",true))return;
                String summary=LocalAi.learnSummary(model.getAbsolutePath(),mode,conversations,routines).trim();
                if(!stopped&&MitrioPreferences.get(this).getBoolean("background_learning_enabled",false)&&MitrioPreferences.get(this).getBoolean("personal_context",true)){
                    if(summary.equalsIgnoreCase("NONE")||summary.startsWith("{")||summary.length()>350)summary="";
                    MitrioPreferences.get(this).edit().putString("background_learning_summary",summary).putLong("background_learning_updated",System.currentTimeMillis()).apply();
                }
            }catch(Exception ignored){
                // Keep the last local summary when this optional background run cannot finish.
            }finally{
                if(!stopped)jobFinished(params,false);
            }
        });
        return true;
    }

    private String recentUserMessages(){
        MainActivity.DB db=new MainActivity.DB(this);StringBuilder out=new StringBuilder();
        Cursor c=db.recentChat(36);
        try{while(c.moveToNext())if("user".equals(c.getString(1))){String line=c.getString(2).trim();if(line.length()>220)line=line.substring(0,220);if(out.length()+line.length()>2600)break;if(out.length()>0)out.append('\n');out.append(line);}}
        finally{c.close();db.close();}
        return out.toString();
    }

    private String localRoutineSummary(){
        MainActivity.DB db=new MainActivity.DB(this);StringBuilder out=new StringBuilder();
        Calendar since=Calendar.getInstance();since.add(Calendar.DAY_OF_YEAR,-13);String sinceDay=new SimpleDateFormat("yyyyMMdd",Locale.US).format(since.getTime());
        Cursor c=db.learningPatterns(sinceDay);
        try{while(c.moveToNext()){String kind=c.getString(0);if("task".equals(kind)||"goal".equals(kind))continue;String title=c.getString(1);if(title.length()>90)title=title.substring(0,90);if(out.length()>0)out.append('\n');out.append(kind).append(": ").append(title).append("; recent check-ins: ").append(c.getInt(5));if(out.length()>900)break;}}
        finally{c.close();db.close();}
        return out.toString();
    }

    @Override public boolean onStopJob(JobParameters params) {
        stopped=true;
        android.content.SharedPreferences prefs=MitrioPreferences.get(this);
        return prefs.getBoolean("background_learning_enabled",false)&&prefs.getBoolean("personal_context",true);
    }
}
