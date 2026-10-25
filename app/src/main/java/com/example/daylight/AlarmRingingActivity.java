package com.example.daylight;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Full-screen controls shown when Android allows alarm full-screen intents. */
public class AlarmRingingActivity extends Activity {
    final int ink=Color.rgb(41,35,57),purple=Color.rgb(108,83,163),cream=Color.rgb(247,245,249),white=Color.WHITE;
    MainActivity.DB db;
    int alarmId;String title,body;boolean testMode;
    @Override public void onCreate(Bundle state){super.onCreate(state);db=new MainActivity.DB(this);getWindow().setStatusBarColor(purple);getWindow().setNavigationBarColor(purple);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);if(Build.VERSION.SDK_INT>=27){setShowWhenLocked(true);setTurnScreenOn(true);}else getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);handleAction(getIntent());}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);handleAction(intent);}
    void handleAction(Intent intent){alarmId=intent.getIntExtra("id",1);title=intent.getStringExtra("title");body=intent.getStringExtra("body");testMode=intent.getBooleanExtra("test",false);if(AlarmSoundService.ACTION_STOP.equals(intent.getAction())){AlarmSoundService.stop(this,alarmId);if(testMode){finish();return;}showAlarm();return;}if(AlarmSoundService.ACTION_SNOOZE.equals(intent.getAction())){AlarmSoundService.snooze(this,alarmId,title,body);finish();return;}showAlarm();}
    void showAlarm(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setPadding(dp(28),dp(28),dp(28),dp(28));root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(98,75,148),Color.rgb(44,37,62)}));
        TextView mark=text(testMode?"☼  MITRIO SOUND CHECK":"☼  MITRIO ALARM",13,Color.rgb(230,220,248),true);mark.setGravity(Gravity.CENTER);root.addView(mark);TextView ringing=text(testMode?"Testing the alarm tone":"It’s time",17,Color.rgb(230,220,248),false);ringing.setGravity(Gravity.CENTER);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,dp(54));rp.topMargin=dp(24);root.addView(ringing,rp);
        TextView label=text(title==null||title.trim().isEmpty()?"Your reminder":title,30,white,true);label.setGravity(Gravity.CENTER);label.setPadding(dp(8),dp(8),dp(8),dp(8));root.addView(label);TextView detail=text(body==null?"One small step is enough.":body,15,Color.rgb(238,233,247),false);detail.setGravity(Gravity.CENTER);LinearLayout.LayoutParams dpLp=new LinearLayout.LayoutParams(-1,-2);dpLp.topMargin=dp(14);root.addView(detail,dpLp);
        boolean lowKey=getSharedPreferences("haru_private",MODE_PRIVATE).getBoolean("low_key_reminders",false);if(!testMode&&!lowKey){TextView volume=text("If you can’t hear it, check your Alarm volume and Mitrio’s alarm access.",12,Color.rgb(220,211,237),false);volume.setGravity(Gravity.CENTER);LinearLayout.LayoutParams vlp=new LinearLayout.LayoutParams(-1,-2);vlp.topMargin=dp(12);root.addView(volume,vlp);}
        if(!testMode){TextView question=text("A quick honest check — did you get to it?",16,Color.rgb(244,238,251),true);question.setGravity(Gravity.CENTER);LinearLayout.LayoutParams qlp=new LinearLayout.LayoutParams(-1,-2);qlp.topMargin=dp(22);root.addView(question,qlp);Button yes=action("Yes, I did a step ✓",Color.rgb(218,241,229),Color.rgb(43,94,66));LinearLayout.LayoutParams ylp=new LinearLayout.LayoutParams(-1,dp(56));ylp.topMargin=dp(16);root.addView(yes,ylp);yes.setOnClickListener(v->answer(true));Button notYet=action("Not yet — keep it for later",white,purple);LinearLayout.LayoutParams nlp=new LinearLayout.LayoutParams(-1,dp(56));nlp.topMargin=dp(9);root.addView(notYet,nlp);notYet.setOnClickListener(v->answer(false));Button snooze=action("Snooze 5 minutes",Color.rgb(222,213,238),purple);LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(-1,dp(52));slp.topMargin=dp(9);root.addView(snooze,slp);snooze.setOnClickListener(v->{AlarmSoundService.snooze(this,alarmId,title,body);finish();});}
        else{Button stop=action("Stop test",Color.rgb(255,233,229),Color.rgb(123,48,57));LinearLayout.LayoutParams stp=new LinearLayout.LayoutParams(-1,dp(58));stp.topMargin=dp(40);root.addView(stop,stp);stop.setOnClickListener(v->{AlarmSoundService.stop(this,alarmId);finish();});}setContentView(root);
    }
    void answer(boolean done){String kind=db.itemKind(alarmId),today=new SimpleDateFormat("yyyyMMdd",Locale.US).format(new Date());if(alarmId==MainActivity.GOAL_NUDGE_ID){getSharedPreferences("haru_private",MODE_PRIVATE).edit().putString("goal_checkin_day",today).putBoolean("goal_checkin_done",done).apply();db.addDailyWin(today,"goal-checkin",alarmId);}else if(!done)db.addDailyWin(today,"honesty",alarmId);if(done){if("task".equals(kind)){db.completeTask(alarmId);MainActivity.cancelReminder(this,alarmId);}else if(!kind.isEmpty()&&!db.done(alarmId,today))db.check(alarmId,today);AlarmSoundService.stop(this,alarmId);Toast.makeText(this,UiStrings.t(this,"Thanks for being straight with me. That effort counts—and your spark is yours ✦."),Toast.LENGTH_LONG).show();finish();return;}AlarmSoundService.stop(this,alarmId);String prompt="I haven’t made progress on ‘"+(title==null?"my goal":title)+"’ yet. Help me find a realistic next step based on my actual time, energy, and feelings. Ask one question, then help without guilt.";new AlertDialog.Builder(this).setTitle(UiStrings.t(this,"Thanks for being honest")).setMessage(UiStrings.t(this,"No guilt. What would make the next step more realistic for your time, energy, or mood?")).setNegativeButton("Leave it for later",(d,w)->finish()).setNeutralButton("Remind me in 30 min",(d,w)->{AlarmSoundService.scheduleSnoozeMinutes(this,alarmId,title,body,30);finish();}).setPositiveButton("Talk it through",(d,w)->{Intent i=new Intent(this,TalkActivity.class);i.putExtra("starter_message",prompt);i.putExtra("send_starter",true);startActivity(i);finish();}).show();}
    int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(UiStrings.t(this,s));t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.create("sans-serif-rounded",bold?Typeface.BOLD:Typeface.NORMAL));return t;}
    Button action(String label,int background,int foreground){Button b=new Button(this);b.setText(UiStrings.t(this,label));b.setAllCaps(false);b.setTextSize(17);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setTextColor(foreground);GradientDrawable bg=new GradientDrawable();bg.setColor(background);bg.setCornerRadius(dp(18));b.setBackground(bg);return b;}
    @Override protected void onDestroy(){if(db!=null)db.close();super.onDestroy();}
}
