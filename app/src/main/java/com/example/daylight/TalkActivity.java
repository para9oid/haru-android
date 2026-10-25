package com.example.daylight;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.speech.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

/** Private conversational screen. Chat and deliberately saved memories remain in the local database. */
public class TalkActivity extends Activity {
    final int ink=Color.rgb(41,35,57),muted=Color.rgb(112,106,127),purple=Color.rgb(108,83,163),lavender=Color.rgb(239,233,249),cream=Color.rgb(247,245,249),white=Color.WHITE;
    static final int REQUEST_MIC=51;
    MainActivity.DB db;
    android.content.SharedPreferences prefs;
    LinearLayout root,messages;
    ScrollView transcript;
    EditText composer;
    TextView thinkingLabel;
    SpeechRecognizer recognizer;
    Handler main=new Handler(Looper.getMainLooper());
    boolean busy=false;
    String pendingMessage="";
    int visibleMessageLimit=60,thinkingCueIndex=0;
    Runnable thinkingTicker;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(cream);getWindow().setNavigationBarColor(cream);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        db=new MainActivity.DB(this);prefs=MitrioPreferences.get(this);build();String starter=getIntent().getStringExtra("starter_message");if(starter!=null&&composer!=null){composer.setText(starter);composer.setSelection(composer.length());if(getIntent().getBooleanExtra("send_starter",false))composer.post(this::sendMessage);}
    }
    int dp(float value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}
    String assistantName(){String value=prefs.getString("assistant_name","Mitrio").trim();return value.isEmpty()?"Mitrio":value;}
    GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    void touchFeedback(View view){view.setOnTouchListener((v,event)->{if(!android.animation.ValueAnimator.areAnimatorsEnabled())return false;if(event.getAction()==MotionEvent.ACTION_DOWN)v.animate().scaleX(.975f).scaleY(.975f).setDuration(80).start();else if(event.getAction()==MotionEvent.ACTION_UP||event.getAction()==MotionEvent.ACTION_CANCEL)v.animate().scaleX(1f).scaleY(1f).setDuration(140).setInterpolator(new android.view.animation.OvershootInterpolator(1.25f)).start();return false;});}
    TextView text(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(UiStrings.t(this,value));t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);t.setTypeface(Typeface.create("sans-serif-rounded",bold?Typeface.BOLD:Typeface.NORMAL));return t;}
    LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    Button button(String value){Button b=new Button(this);b.setText(UiStrings.t(this,value));b.setAllCaps(false);b.setTextSize(12);b.setTextColor(purple);b.setBackground(shape(lavender,14));b.setMinHeight(dp(42));b.setPadding(dp(10),0,dp(10),0);touchFeedback(b);return b;}
    void build(){
        root=column();root.setBackgroundColor(cream);setContentView(root);if(android.animation.ValueAnimator.areAnimatorsEnabled()){root.setAlpha(0f);root.setTranslationY(dp(6));root.post(()->root.animate().alpha(1f).translationY(0).setDuration(190).setInterpolator(new android.view.animation.DecelerateInterpolator()).start());}
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(12),dp(8),dp(12),dp(8));bar.setBackgroundColor(white);
        TextView back=text("‹",32,purple,true);back.setGravity(Gravity.CENTER);bar.addView(back,new LinearLayout.LayoutParams(dp(42),dp(48)));touchFeedback(back);back.setOnClickListener(v->{finish();if(android.animation.ValueAnimator.areAnimatorsEnabled())overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);});
        String helper=assistantName();LinearLayout heading=column();heading.addView(text("Talk to "+helper,18,ink,true));heading.addView(text("A private space for unfiltered thoughts",11,muted,false));bar.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        Button teach=button("Teach "+helper);teach.setOnClickListener(v->showMemories());bar.addView(teach);Button clear=button("Clear");clear.setOnClickListener(v->confirmClearChat());LinearLayout.LayoutParams clearLp=new LinearLayout.LayoutParams(-2,dp(42));clearLp.setMargins(dp(5),0,0,0);bar.addView(clear,clearLp);root.addView(bar);
        LinearLayout privacy=column();privacy.setPadding(dp(14),dp(9),dp(14),dp(9));privacy.setBackground(shape(lavender,15));TextView privacyText=text(helper+" uses your saved chats and habit context only on this phone when you allow it. It does not retrain or upload the model. Small models can be wrong—when unsure, "+helper+" should say so and ask before searching.",11,muted,false);privacyText.setLineSpacing(dp(1),1);privacy.addView(privacyText);LinearLayout.LayoutParams privacyLp=new LinearLayout.LayoutParams(-1,-2);privacyLp.setMargins(dp(14),dp(10),dp(14),dp(4));root.addView(privacy,privacyLp);
        transcript=new ScrollView(this);transcript.setFillViewport(true);transcript.setVerticalScrollBarEnabled(false);messages=column();messages.setPadding(dp(14),dp(8),dp(14),dp(16));transcript.addView(messages);root.addView(transcript,new LinearLayout.LayoutParams(-1,0,1));
        renderMessages();
        LinearLayout compose=column();compose.setPadding(dp(12),dp(8),dp(12),dp(8));compose.setBackgroundColor(white);
        String mode=prefs.getString("ai_mode",MainActivity.MODEL_OFF);boolean ready=modelFile(mode).isFile()&&modelFile(mode).length()==modelBytes(mode);
        if(!ready){LinearLayout hint=new LinearLayout(this);hint.setGravity(Gravity.CENTER_VERTICAL);TextView info=text(mode.equals(MainActivity.MODEL_OFF)?"Turn on an on-device model for Mitrio to talk back.":"Your chosen on-device model isn’t downloaded yet.",11,muted,false);hint.addView(info,new LinearLayout.LayoutParams(0,-2,1));Button settings=button("AI options");settings.setOnClickListener(v->{Intent i=new Intent(this,MainActivity.class);i.putExtra("open_more",true);startActivity(i);});hint.addView(settings);compose.addView(hint);}
        LinearLayout entry=new LinearLayout(this);entry.setGravity(Gravity.CENTER_VERTICAL);
        composer=new EditText(this);composer.setHint(UiStrings.t(this,"Say it messy. I’m listening…"));composer.setTextSize(15);composer.setTextColor(ink);composer.setHintTextColor(muted);composer.setGravity(Gravity.TOP|Gravity.LEFT);composer.setMinLines(2);composer.setMaxLines(4);composer.setPadding(dp(12),dp(11),dp(12),dp(11));composer.setBackground(shape(cream,16));entry.addView(composer,new LinearLayout.LayoutParams(0,dp(72),1));
        Button mic=button("🎙");mic.setContentDescription("Dictate on this phone");LinearLayout.LayoutParams micLp=new LinearLayout.LayoutParams(dp(48),dp(48));micLp.setMargins(dp(6),0,dp(4),0);entry.addView(mic,micLp);mic.setOnClickListener(v->startPrivateSpeech());
        Button send=button("Send");send.setTextColor(white);send.setBackground(shape(purple,15));LinearLayout.LayoutParams sendLp=new LinearLayout.LayoutParams(dp(66),dp(48));entry.addView(send,sendLp);send.setOnClickListener(v->sendMessage());compose.addView(entry);root.addView(compose);
    }
    File modelFile(String mode){File d=getExternalFilesDir("models");if(d==null)d=new File(getFilesDir(),"models");return new File(d,mode.equals(MainActivity.MODEL_SMALL)?MainActivity.SMALL_FILE:MainActivity.STANDARD_FILE);}
    long modelBytes(String mode){return mode.equals(MainActivity.MODEL_SMALL)?MainActivity.SMALL_BYTES:MainActivity.STANDARD_BYTES;}
    void renderMessages(){renderMessages(true);}
    void renderMessages(boolean scrollToBottom){
        int oldScroll=transcript==null?0:transcript.getScrollY();
        messages.removeAllViews();int total=db.chatCount();
        if(total>visibleMessageLimit){Button older=button("Show earlier messages · "+(total-visibleMessageLimit));older.setTextSize(11);older.setOnClickListener(v->{visibleMessageLimit=200;renderMessages(false);});messages.addView(older);}
        Cursor c=db.recentChat(Math.max(1,Math.min(visibleMessageLimit,Math.max(total,1))));boolean any=false;
        while(c.moveToNext()){
            any=true;String role=c.getString(1),body=c.getString(2),search=c.getString(3);boolean user=role.equals("user");
            LinearLayout line=new LinearLayout(this);line.setGravity(user?Gravity.RIGHT:Gravity.LEFT);line.setPadding(user?dp(36):0,dp(3),user?0:dp(28),dp(5));
            LinearLayout bubble=column();bubble.setPadding(dp(13),dp(10),dp(13),dp(10));bubble.setBackground(shape(user?purple:white,18));TextView words=text(user?body:cleanAssistantText(body),14,user?white:ink,false);words.setGravity(Gravity.TOP|Gravity.LEFT);words.setLineSpacing(dp(2),1.03f);bubble.addView(words);
            if(!user&&search!=null&&!search.trim().isEmpty()){Button lookup=button("Review a web search  ↗");lookup.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);lookup.setOnClickListener(v->confirmSearch(search));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(8),0,0);bubble.addView(lookup,lp);}
            line.addView(bubble,new LinearLayout.LayoutParams(-1,-2));messages.addView(line);
        }
        c.close();
        if(!any){LinearLayout welcome=column();welcome.setPadding(dp(16),dp(15),dp(16),dp(15));welcome.setBackground(shape(white,20));welcome.addView(text("Hey "+prefs.getString("name","friend")+", I’m "+assistantName()+". Start anywhere—even if it comes out tangled. I’ll listen, offer a small idea when I can, and tell you when I’m unsure.",15,ink,false));messages.addView(welcome);}
        if(busy){LinearLayout thinking= new LinearLayout(this);thinking.setGravity(Gravity.CENTER_VERTICAL);thinking.setPadding(dp(13),dp(11),dp(13),dp(11));thinking.setBackground(shape(white,18));ProgressBar spinner=new ProgressBar(this,null,android.R.attr.progressBarStyleSmall);if(Build.VERSION.SDK_INT>=21)spinner.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(purple));thinking.addView(spinner,new LinearLayout.LayoutParams(dp(22),dp(22)));thinkingLabel=text(thinkingCue(),13,muted,false);LinearLayout.LayoutParams cueLp=new LinearLayout.LayoutParams(0,-2,1);cueLp.setMargins(dp(9),0,0,0);thinking.addView(thinkingLabel,cueLp);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(5),dp(28),dp(5));messages.addView(thinking,lp);}
        if(transcript!=null)transcript.post(()->{if(scrollToBottom)transcript.fullScroll(View.FOCUS_DOWN);else transcript.scrollTo(0,oldScroll);});
    }
    String thinkingCue(){String[] lines={assistantName()+" is untangling one thought at a time…","Your phone is doing the private brainwork. Take one slow breath.","Shoulders down. This isn’t a test.","No cloud detour—just a tiny local think."};return UiStrings.t(this,lines[thinkingCueIndex%lines.length]);}
    void startThinkingTicker(){if(thinkingTicker!=null)main.removeCallbacks(thinkingTicker);thinkingCueIndex=0;thinkingTicker=()->{if(!busy||isFinishing()||isDestroyed()||thinkingLabel==null)return;thinkingCueIndex++;thinkingLabel.setText(thinkingCue());main.postDelayed(thinkingTicker,2300);};main.postDelayed(thinkingTicker,2300);}
    void stopThinkingTicker(){if(thinkingTicker!=null){main.removeCallbacks(thinkingTicker);thinkingTicker=null;}thinkingLabel=null;}
    boolean clearlyRefersBack(String message){return java.util.regex.Pattern.compile("(?is)^\\s*(and|but|so|then|also|why|what about|how about|what if|do that|make it|change it|continue|explain|tell me more|that|this|it|they|them|those|these|same|instead|you said|what did you mean)\\b").matcher(message).find();}
    String localHistory(){StringBuilder b=new StringBuilder();Cursor c=db.recentChat(8);while(c.moveToNext()){String role=c.getString(1),body=role.equals("assistant")?cleanAssistantText(c.getString(2)):c.getString(2);if(body.trim().isEmpty())continue;if(b.length()>0)b.append('\n');b.append(role.equals("user")?"You: ":assistantName()+": ").append(body);if(b.length()>3500)break;}c.close();return b.toString();}
    String localLearningContext(){
        if(!prefs.getBoolean("personal_context",true))return "";
        Calendar since=Calendar.getInstance();since.add(Calendar.DAY_OF_YEAR,-13);String day=new SimpleDateFormat("yyyyMMdd",Locale.US).format(since.getTime());StringBuilder b=new StringBuilder();
        Cursor c=db.learningPatterns(day);while(c.moveToNext()){
            String kind=c.getString(0),title=c.getString(1);if(b.length()>0)b.append('\n');
            if("task".equals(kind)){b.append("Open task: ").append(title);long at=c.getLong(6);if(at>System.currentTimeMillis())b.append(" · reminder ").append(new SimpleDateFormat("EEE h:mm a",Locale.getDefault()).format(new Date(at)));}
            else if("habit".equals(kind)){b.append("Habit: ").append(title).append("; checked in ").append(c.getInt(5)).append(" times in the last 14 days");}
            else if("routine".equals(kind)){b.append("Routine: ").append(title);if(c.getInt(4)!=0)b.append("; daily at ").append(String.format(Locale.US,"%02d:%02d",c.getInt(2),c.getInt(3)));}
            else{b.append("Goal: ").append(title);if(c.getInt(5)>0)b.append("; progress check-ins in the last 14 days: ").append(c.getInt(5));}
            if(b.length()>1800)break;
        }c.close();
        String today=new SimpleDateFormat("yyyyMMdd",Locale.US).format(new Date());if(today.equals(prefs.getString("goal_checkin_day",""))){if(b.length()>0)b.append('\n');b.append("Today's honest goal check-in: ").append(prefs.getBoolean("goal_checkin_done",false)?"they said they made a step":"they said not yet");}
        java.util.regex.Pattern signal=java.util.regex.Pattern.compile("(?i)\\b(i (?:often|usually|tend to|prefer|like|struggle|forget|work best|keep|always|sometimes)|my (?:goal|habit|routine|schedule|energy))\\b");Cursor chats=db.recentChat(30);int learned=0;while(chats.moveToNext()&&learned<4){if(!"user".equals(chats.getString(1)))continue;String prior=chats.getString(2).trim();if(prior.length()>260)prior=prior.substring(0,260);if(signal.matcher(prior).find()){if(b.length()>0)b.append('\n');b.append("From an earlier chat, as a possible clue: ").append(prior);learned++;if(b.length()>2600)break;}}chats.close();String learnedSummary=prefs.getBoolean("background_learning_enabled",false)?prefs.getString("background_learning_summary",""):"";if(!learnedSummary.trim().isEmpty()){if(b.length()>0)b.append("\n");b.append("Optional on-device AI summary; tentative, may be wrong: ").append(learnedSummary);}return b.toString();
    }
    String cleanAssistantText(String raw){String value=raw==null?"":raw.trim();try{int start=value.indexOf('{'),end=value.lastIndexOf('}');if(start>=0&&end>start){JSONObject o=new JSONObject(value.substring(start,end+1));String reply=o.optString("reply","").trim();if(!reply.isEmpty())return reply;}}catch(Exception ignored){}value=value.replaceAll("(?im)^\\s*[\\\"']?(needs_research|search_prompt)[\\\"']?\\s*[:=].*(?:\\R|$)","");value=value.replaceAll("(?is)\\s*[\\\"']?needs_research[\\\"']?\\s*[:=].*$","");value=value.replaceAll("(?im)^\\s*[\\\"']?suggestions[\\\"']?\\s*[:=].*$","");value=value.replaceAll("(?is)^\\s*\\{\\s*[\\\"']?reply[\\\"']?\\s*[:=]\\s*","");value=value.replaceAll("(?m)[,}\\s]+$","").trim();if(value.length()>1&&value.startsWith("\\\"")&&value.endsWith("\\\""))value=value.substring(1,value.length()-1);return value;}
    String localMemories(){StringBuilder b=new StringBuilder();Cursor c=db.memories();int count=0;while(c.moveToNext()&&count<8){if(b.length()>0)b.append('\n');b.append("- ").append(c.getString(1));count++;if(b.length()>1600)break;}c.close();return b.toString();}
    void sendMessage(){
        if(busy)return;String message=composer.getText().toString().trim();if(message.isEmpty()){Toast.makeText(this,UiStrings.t(this,"Put down whatever’s on your mind first."),Toast.LENGTH_SHORT).show();return;}if(message.length()>12000){Toast.makeText(this,UiStrings.t(this,"That thought is a little too long for one message. Try splitting it into two."),Toast.LENGTH_LONG).show();return;}
        hideKeyboard();visibleMessageLimit=60;boolean useContext=prefs.getBoolean("personal_context",true);String history=useContext&&clearlyRefersBack(message)?localHistory():"",memories=useContext?localMemories():"",learningContext=localLearningContext();db.addChatMessage("user",message,"");composer.setText(UiStrings.t(this,""));pendingMessage=message;
        String mode=prefs.getString("ai_mode",MainActivity.MODEL_OFF);File model=modelFile(mode);
        if(mode.equals(MainActivity.MODEL_OFF)||!model.isFile()||model.length()!=modelBytes(mode)){
            String answer=mode.equals(MainActivity.MODEL_OFF)?"I’ve got your message. To have a back-and-forth conversation, turn on an optional on-device model in More. Your words stay here either way.":"I’ve got your message, but the local model isn’t ready yet. You can download it in More; I won’t guess at a reply.";
            db.addChatMessage("assistant",answer,"");renderMessages();return;
        }
        busy=true;renderMessages();startThinkingTicker();Toast.makeText(this,UiStrings.t(this,"Local AI may make your phone feel slower for a few seconds while it thinks here for your privacy."),Toast.LENGTH_LONG).show();
        final String selectedMode=mode,name=prefs.getString("name","");
        LocalAi.submit(()->{
            try{
                String raw=LocalAi.talk(model.getAbsolutePath(),selectedMode,name,assistantName(),prefs.getString("ui_language","english"),prefs.getString("response_style","friendly"),memories,history,learningContext,message);ChatReply reply=parseReply(raw,message);
                main.post(()->{busy=false;stopThinkingTicker();if(isFinishing()||isDestroyed())return;db.addChatMessage("assistant",reply.text,reply.searchPrompt);renderMessages();});
            }catch(Exception e){main.post(()->{busy=false;stopThinkingTicker();if(isFinishing()||isDestroyed())return;db.addChatMessage("assistant","I’m still here. My little local model couldn’t shape a useful reply, so I won’t bluff. Try one smaller piece, or tell me what you’d like help figuring out.","");renderMessages();});}
        });
    }
    static class ChatReply{String text="",searchPrompt="";}
    ChatReply parseReply(String raw,String original){
        ChatReply result=new ChatReply();String value=raw==null?"":raw.trim();
        try{int start=value.indexOf('{'),end=value.lastIndexOf('}');if(start>=0&&end>=start){JSONObject o=new JSONObject(value.substring(start,end+1));result.text=o.optString("reply","").trim();JSONArray ideas=o.optJSONArray("suggestions");if(ideas!=null&&ideas.length()>0){StringBuilder items=new StringBuilder();for(int i=0;i<Math.min(2,ideas.length());i++){String idea=ideas.optString(i,"").trim();if(!idea.isEmpty()){if(items.length()>0)items.append('\n');items.append("• ").append(idea);}}if(items.length()>0)result.text+=(result.text.isEmpty()?"":"\n\nA small idea to consider:\n")+items;}
                if(o.optBoolean("needs_research",false))result.searchPrompt=o.optString("search_prompt","").trim();
            }}catch(Exception ignored){}
        if(result.searchPrompt.isEmpty()&&value.matches("(?is).*needs_research\\s*[:=]\\s*true.*")){java.util.regex.Matcher search=java.util.regex.Pattern.compile("(?is)search_prompt\\s*[:=]\\s*[\\\"']([^\\\"']+)[\\\"']").matcher(value);if(search.find())result.searchPrompt=search.group(1).trim();}
        if(result.text.isEmpty())result.text=cleanAssistantText(value);
        if(result.text.isEmpty())result.text="I’m not sure how to shape that yet. Could you tell me one more detail?";
        String lower=result.text.toLowerCase(Locale.ROOT);
        if(needsFreshInfo(original)){if(result.searchPrompt.isEmpty())result.searchPrompt="General, up-to-date information about "+original;result.text="That needs up-to-date information, and my small local model can’t verify it offline. Want me to look it up?";return result;}
        if(result.searchPrompt.isEmpty()&&(lower.contains("i’m not sure")||lower.contains("i'm not sure")||lower.contains("i don't know")||lower.contains("cannot verify")||lower.contains("can't verify")||lower.contains("may be out of date")))result.searchPrompt="General information about "+original;
        return result;
    }
    boolean needsFreshInfo(String message){return java.util.regex.Pattern.compile("(?i)\\b(latest|current|right now|weather|news|price|stock|score|near me|open now|updated|recent|newest|release date|who is currently|what time is it)\\b").matcher(message).find();}
    void showMemories(){
        LinearLayout body=column();body.setPadding(dp(4),dp(4),dp(4),dp(4));TextView help=text(assistantName()+" remembers these notes on this phone and uses them for future chats. This is preference memory, not model training.",12,muted,false);body.addView(help);body.addView(space(8));
        final AlertDialog[] current={null};Cursor c=db.memories();boolean any=false;
        while(c.moveToNext()){
            any=true;int id=c.getInt(0);String note=c.getString(1);LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);TextView label=text(note,13,ink,false);row.addView(label,new LinearLayout.LayoutParams(0,-2,1));Button remove=button("Remove");remove.setOnClickListener(v->{db.deleteMemory(id);if(current[0]!=null)current[0].dismiss();showMemories();});row.addView(remove);body.addView(row);body.addView(space(6));
        }
        c.close();if(!any)body.addView(text("No saved notes yet.",12,muted,false));
        EditText input=new EditText(this);input.setHint(UiStrings.t(this,"Example: Short plans work better for me."));input.setTextSize(14);input.setTextColor(ink);input.setMinLines(2);input.setGravity(Gravity.TOP|Gravity.LEFT);body.addView(input);
        ScrollView scroll=new ScrollView(this);scroll.addView(body);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Teach "+assistantName()+" about you").setMessage(UiStrings.t(this,"Only save what you want it to remember.")).setView(scroll).setNegativeButton("Done",null).setPositiveButton("Remember on this phone",null).create();current[0]=dialog;
        dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(b->{String note=input.getText().toString().trim();if(note.isEmpty()){input.setError("Write a preference to remember");return;}if(note.length()>1000){input.setError("Keep this note under 1,000 characters");return;}if(db.memoryCount()>=50){input.setError("Mitrio can keep up to 50 memory notes. Remove one first.");return;}db.addMemory(note);dialog.dismiss();Toast.makeText(this,UiStrings.t(this,"Saved on this phone. Mitrio can use it in future chats."),Toast.LENGTH_LONG).show();}));dialog.show();
    }
    void confirmClearChat(){if(busy){Toast.makeText(this,UiStrings.t(this,"Mitrio is still thinking. Give it a moment first."),Toast.LENGTH_SHORT).show();return;}new AlertDialog.Builder(this).setTitle(UiStrings.t(this,"Clear this conversation?")).setMessage(UiStrings.t(this,"This removes the chat history from this phone. Your saved memories stay.")).setNegativeButton("Keep chat",null).setPositiveButton("Clear chat",(d,w)->{db.clearChat();renderMessages();Toast.makeText(this,UiStrings.t(this,"Conversation cleared. Saved memories are still here."),Toast.LENGTH_SHORT).show();}).show();}
    void confirmSearch(String query){
        EditText edit=new EditText(this);edit.setText(query);edit.setTextSize(14);edit.setTextColor(ink);edit.setMinLines(2);edit.setGravity(Gravity.TOP|Gravity.LEFT);
        new AlertDialog.Builder(this).setTitle(UiStrings.t(this,"Check the search first")).setMessage(UiStrings.t(this,"Mitrio isn’t sure. Review or edit this phrase. Only when you tap Search will it open your browser and send the phrase to your chosen search provider. Your chat stays in Mitrio on this phone.")).setView(edit).setNegativeButton("Not now",null).setPositiveButton("Search the web",(d,w)->{String q=edit.getText().toString().trim();if(q.isEmpty())return;try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/search?q="+Uri.encode(q))));}catch(Exception e){Toast.makeText(this,UiStrings.t(this,"No browser is ready for that search."),Toast.LENGTH_LONG).show();}}).show();
    }
    View space(int height){View v=new View(this);v.setLayoutParams(new LinearLayout.LayoutParams(1,dp(height)));return v;}
    void startPrivateSpeech(){
        if(Build.VERSION.SDK_INT<31||!SpeechRecognizer.isOnDeviceRecognitionAvailable(this)){Toast.makeText(this,UiStrings.t(this,"This phone’s speech service doesn’t offer on-device dictation. You can type instead."),Toast.LENGTH_LONG).show();return;}
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQUEST_MIC);return;}beginSpeech();
    }
    void beginSpeech(){
        try{if(recognizer!=null)recognizer.destroy();recognizer=SpeechRecognizer.createOnDeviceSpeechRecognizer(this);recognizer.setRecognitionListener(new RecognitionListener(){
            public void onReadyForSpeech(Bundle b){Toast.makeText(TalkActivity.this,"Listening on this phone…",Toast.LENGTH_SHORT).show();}public void onBeginningOfSpeech(){}public void onRmsChanged(float r){}public void onBufferReceived(byte[] b){}public void onEndOfSpeech(){}public void onError(int e){Toast.makeText(TalkActivity.this,"Dictation ended. You can type or try again.",Toast.LENGTH_SHORT).show();}
            public void onResults(Bundle b){ArrayList<String> r=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(r!=null&&!r.isEmpty()&&composer!=null){String old=composer.getText().toString().trim();composer.setText(old.isEmpty()?r.get(0):old+"\n"+r.get(0));composer.setSelection(composer.length());}}
            public void onPartialResults(Bundle b){}public void onEvent(int e,Bundle b){}
        });Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true);recognizer.startListening(intent);}catch(Exception e){Toast.makeText(this,UiStrings.t(this,"On-device dictation isn’t ready on this phone yet."),Toast.LENGTH_LONG).show();}
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==REQUEST_MIC&&grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED)beginSpeech();}
    void hideKeyboard(){if(composer!=null)((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(composer.getWindowToken(),0);}
    @Override protected void onDestroy(){busy=false;stopThinkingTicker();if(recognizer!=null){recognizer.destroy();recognizer=null;}if(db!=null)db.close();super.onDestroy();}
}
