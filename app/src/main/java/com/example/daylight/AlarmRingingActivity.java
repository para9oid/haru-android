package com.example.daylight;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;

/** Full-screen controls shown when Android allows alarm full-screen intents. */
public class AlarmRingingActivity extends Activity {
    final int ink=Color.rgb(41,35,57),purple=Color.rgb(108,83,163),cream=Color.rgb(247,245,249),white=Color.WHITE;
    int alarmId;String title,body;boolean testMode;
    @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setStatusBarColor(purple);getWindow().setNavigationBarColor(purple);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);if(Build.VERSION.SDK_INT>=27){setShowWhenLocked(true);setTurnScreenOn(true);}else getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);handleAction(getIntent());}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);handleAction(intent);}
    void handleAction(Intent intent){alarmId=intent.getIntExtra("id",1);title=intent.getStringExtra("title");body=intent.getStringExtra("body");testMode=intent.getBooleanExtra("test",false);if(AlarmSoundService.ACTION_STOP.equals(intent.getAction())){AlarmSoundService.stop(this,alarmId);finish();return;}if(AlarmSoundService.ACTION_SNOOZE.equals(intent.getAction())){AlarmSoundService.snooze(this,alarmId,title,body);finish();return;}showAlarm();}
    void showAlarm(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setPadding(dp(28),dp(28),dp(28),dp(28));root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(98,75,148),Color.rgb(44,37,62)}));
        TextView mark=text(testMode?"☼  HARU SOUND CHECK":"☼  HARU ALARM",13,Color.rgb(230,220,248),true);mark.setGravity(Gravity.CENTER);root.addView(mark);TextView ringing=text(testMode?"Testing the alarm tone":"It’s time",17,Color.rgb(230,220,248),false);ringing.setGravity(Gravity.CENTER);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,dp(54));rp.topMargin=dp(24);root.addView(ringing,rp);
        TextView label=text(title==null||title.trim().isEmpty()?"Your reminder":title,30,white,true);label.setGravity(Gravity.CENTER);label.setPadding(dp(8),dp(8),dp(8),dp(8));root.addView(label);TextView detail=text(body==null?"One small step is enough.":body,15,Color.rgb(238,233,247),false);detail.setGravity(Gravity.CENTER);LinearLayout.LayoutParams dpLp=new LinearLayout.LayoutParams(-1,-2);dpLp.topMargin=dp(14);root.addView(detail,dpLp);
        TextView volume=text("If you can’t hear it, check your Alarm volume and Haru’s alarm access.",12,Color.rgb(220,211,237),false);volume.setGravity(Gravity.CENTER);LinearLayout.LayoutParams vlp=new LinearLayout.LayoutParams(-1,-2);vlp.topMargin=dp(20);root.addView(volume,vlp);
        if(!testMode){Button snooze=action("Snooze 5 minutes",white,purple);LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(-1,dp(58));slp.topMargin=dp(40);root.addView(snooze,slp);snooze.setOnClickListener(v->{AlarmSoundService.snooze(this,alarmId,title,body);finish();});}
        Button stop=action(testMode?"Stop test":"Stop alarm",Color.rgb(255,233,229),Color.rgb(123,48,57));LinearLayout.LayoutParams stp=new LinearLayout.LayoutParams(-1,dp(58));stp.topMargin=dp(testMode?40:12);root.addView(stop,stp);stop.setOnClickListener(v->{AlarmSoundService.stop(this,alarmId);finish();});setContentView(root);
    }
    int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.create("sans-serif-rounded",bold?Typeface.BOLD:Typeface.NORMAL));return t;}
    Button action(String label,int background,int foreground){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(17);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setTextColor(foreground);GradientDrawable bg=new GradientDrawable();bg.setColor(background);bg.setCornerRadius(dp(18));b.setBackground(bg);return b;}
}
