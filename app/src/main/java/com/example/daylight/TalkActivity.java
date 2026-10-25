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
    SpeechRecognizer recognizer;
    Handler main=new Handler(Looper.getMainLooper());
    boolean busy=false;
    String pendingMessage="";

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(cream);getWindow().setNavigationBarColor(cream);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        db=new MainActivity.DB(this);prefs=getSharedPreferences("haru_private",MODE_PRIVATE);build();
    }
    int dp(float value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}
    GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    TextView text(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);t.setTypeface(Typeface.create("sans-serif-rounded",bold?Typeface.BOLD:Typeface.NORMAL));return t;}
    LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    Button button(String value){Button b=new Button(this);b.setText(value);b.setAllCaps(false);b.setTextSize(12);b.setTextColor(purple);b.setBackground(shape(lavender,14));b.setMinHeight(dp(42));b.setPadding(dp(10),0,dp(10),0);return b;}
    void build(){
        root=column();root.setBackgroundColor(cream);setContentView(root);
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(12),dp(8),dp(12),dp(8));bar.setBackgroundColor(white);
        TextView back=text("‹",32,purple,true);back.setGravity(Gravity.CENTER);bar.addView(back,new LinearLayout.LayoutParams(dp(42),dp(48)));back.setOnClickListener(v->finish());
        LinearLayout heading=column();heading.addView(text("Talk to Haru",18,ink,true));heading.addView(text("A private space for unfiltered thoughts",11,muted,false));bar.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        Button teach=button("Teach Haru");teach.setOnClickListener(v->showMemories());bar.addView(teach);Button clear=button("Clear");clear.setOnClickListener(v->confirmClearChat());LinearLayout.LayoutParams clearLp=new LinearLayout.LayoutParams(-2,dp(42));clearLp.setMargins(dp(5),0,0,0);bar.addView(clear,clearLp);root.addView(bar);
        LinearLayout privacy=column();privacy.setPadding(dp(14),dp(9),dp(14),dp(9));privacy.setBackground(shape(lavender,15));TextView privacyText=text("Haru keeps chat here. It uses recent messages and notes you choose to remember; it does not retrain or upload the model. Small models can be wrong—when unsure, Haru should say so and ask before searching.",11,muted,false);privacyText.setLineSpacing(dp(1),1);privacy.addView(privacyText);LinearLayout.LayoutParams privacyLp=new LinearLayout.LayoutParams(-1,-2);privacyLp.setMargins(dp(14),dp(10),dp(14),dp(4));root.addView(privacy,privacyLp);
        transcript=new ScrollView(this);transcript.setFillViewport(true);transcript.setVerticalScrollBarEnabled(false);messages=column();messages.setPadding(dp(14),dp(8),dp(14),dp(16));transcript.addView(messages);root.addView(transcript,new LinearLayout.LayoutParams(-1,0,1));
        renderMessages();
        LinearLayout compose=column();compose.setPadding(dp(12),dp(8),dp(12),dp(8));compose.setBackgroundColor(white);
        String mode=prefs.getString("ai_mode",MainActivity.MODEL_OFF);boolean ready=modelFile(mode).isFile()&&modelFile(mode).length()==modelBytes(mode);
        if(!ready){LinearLayout hint=new LinearLayout(this);hint.setGravity(Gravity.CENTER_VERTICAL);TextView info=text(mode.equals(MainActivity.MODEL_OFF)?"Turn on an on-device model for Haru to talk back.":"Your chosen on-device model isn’t downloaded yet.",11,muted,false);hint.addView(info,new LinearLayout.LayoutParams(0,-2,1));Button settings=button("AI options");settings.setOnClickListener(v->{Intent i=new Intent(this,MainActivity.class);i.putExtra("open_more",true);startActivity(i);});hint.addView(settings);compose.addView(hint);}
        LinearLayout entry=new LinearLayout(this);entry.setGravity(Gravity.CENTER_VERTICAL);
        composer=new EditText(this);composer.setHint("Say it messy. I’m listening…");composer.setTextSize(15);composer.setTextColor(ink);composer.setHintTextColor(muted);composer.setGravity(Gravity.TOP|Gravity.LEFT);composer.setMinLines(2);composer.setMaxLines(4);composer.setPadding(dp(12),dp(11),dp(12),dp(11));composer.setBackground(shape(cream,16));entry.addView(composer,new LinearLayout.LayoutParams(0,dp(72),1));
        Button mic=button("🎙");mic.setContentDescription("Dictate on this phone");LinearLayout.LayoutParams micLp=new LinearLayout.LayoutParams(dp(48),dp(48));micLp.setMargins(dp(6),0,dp(4),0);entry.addView(mic,micLp);mic.setOnClickListener(v->startPrivateSpeech());
        Button send=button("Send");send.setTextColor(white);send.setBackground(shape(purple,15));LinearLayout.LayoutParams sendLp=new LinearLayout.LayoutParams(dp(66),dp(48));entry.addView(send,sendLp);send.setOnClickListener(v->sendMessage());compose.addView(entry);root.addView(compose);
    }
    File modelFile(String mode){File d=getExternalFilesDir("models");if(d==null)d=new File(getFilesDir(),"models");return new File(d,mode.equals(MainActivity.MODEL_SMALL)?MainActivity.SMALL_FILE:MainActivity.STANDARD_FILE);}
    long modelBytes(String mode){return mode.equals(MainActivity.MODEL_SMALL)?MainActivity.SMALL_BYTES:MainActivity.STANDARD_BYTES;}
    void renderMessages(){
        messages.removeAllViews();Cursor c=db.chatMessages();boolean any=false;
        while(c.moveToNext()){
            any=true;String role=c.getString(1),body=c.getString(2),search=c.getString(3);boolean user=role.equals("user");
            LinearLayout line=new LinearLayout(this);line.setGravity(user?Gravity.RIGHT:Gravity.LEFT);line.setPadding(user?dp(36):0,dp(3),user?0:dp(28),dp(5));
            LinearLayout bubble=column();bubble.setPadding(dp(13),dp(10),dp(13),dp(10));bubble.setBackground(shape(user?purple:white,18));TextView words=text(body,14,user?white:ink,false);words.setGravity(Gravity.TOP|Gravity.LEFT);words.setLineSpacing(dp(2),1.03f);bubble.addView(words);
            if(!user&&search!=null&&!search.trim().isEmpty()){Button lookup=button("Review a web search  ↗");lookup.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);lookup.setOnClickListener(v->confirmSearch(search));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(8),0,0);bubble.addView(lookup,lp);}
            line.addView(bubble,new LinearLayout.LayoutParams(-1,-2));messages.addView(line);
        }
        c.close();
        if(!any){LinearLayout welcome=column();welcome.setPadding(dp(16),dp(15),dp(16),dp(15));welcome.setBackground(shape(white,20));welcome.addView(text("Hey "+prefs.getString("name","friend")+", I’m here. Start anywhere—even if it comes out tangled. I’ll listen, offer a small idea when I can, and tell you when I’m unsure.",15,ink,false));messages.addView(welcome);}
        if(busy){LinearLayout thinking=column();thinking.setPadding(dp(13),dp(11),dp(13),dp(11));thinking.setBackground(shape(white,18));thinking.addView(text("Haru’s thinking on this phone…",13,muted,false));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(5),dp(28),dp(5));messages.addView(thinking,lp);}
        transcript.post(()->transcript.fullScroll(View.FOCUS_DOWN));
    }
    String localHistory(){StringBuilder b=new StringBuilder();Cursor c=db.recentChat(10);while(c.moveToNext()){if(b.length()>0)b.append('\n');b.append(c.getString(1).equals("user")?"You: ":"Haru: ").append(c.getString(2));if(b.length()>5000)break;}c.close();return b.toString();}
    String localMemories(){StringBuilder b=new StringBuilder();Cursor c=db.memories();int count=0;while(c.moveToNext()&&count<8){if(b.length()>0)b.append('\n');b.append("- ").append(c.getString(1));count++;if(b.length()>1600)break;}c.close();return b.toString();}
    void sendMessage(){
        if(busy)return;String message=composer.getText().toString().trim();if(message.isEmpty()){Toast.makeText(this,"Put down whatever’s on your mind first.",Toast.LENGTH_SHORT).show();return;}if(message.length()>12000){Toast.makeText(this,"That thought is a little too long for one message. Try splitting it into two.",Toast.LENGTH_LONG).show();return;}
        hideKeyboard();String history=localHistory(),memories=localMemories();db.addChatMessage("user",message,"");composer.setText("");pendingMessage=message;
        String mode=prefs.getString("ai_mode",MainActivity.MODEL_OFF);File model=modelFile(mode);
        if(mode.equals(MainActivity.MODEL_OFF)||!model.isFile()||model.length()!=modelBytes(mode)){
            String answer=mode.equals(MainActivity.MODEL_OFF)?"I’ve got your message. To have a back-and-forth conversation, turn on an optional on-device model in More. Your words stay here either way.":"I’ve got your message, but the local model isn’t ready yet. You can download it in More; I won’t guess at a reply.";
            db.addChatMessage("assistant",answer,"");renderMessages();return;
        }
        busy=true;renderMessages();Toast.makeText(this,"Local AI may make your phone feel slower for a few seconds while it thinks here for your privacy.",Toast.LENGTH_LONG).show();
        final String selectedMode=mode,name=prefs.getString("name","");
        new Thread(()->{
            try{
                String raw=LocalAi.talk(model.getAbsolutePath(),selectedMode,name,memories,history,message);ChatReply reply=parseReply(raw,message);
                main.post(()->{busy=false;if(isFinishing()||isDestroyed())return;db.addChatMessage("assistant",reply.text,reply.searchPrompt);renderMessages();});
            }catch(Exception e){main.post(()->{busy=false;if(isFinishing()||isDestroyed())return;db.addChatMessage("assistant","I’m still here. My little local model couldn’t shape a useful reply, so I won’t bluff. Try one smaller piece, or tell me what you’d like help figuring out.","");renderMessages();});}
        }).start();
    }
    static class ChatReply{String text="",searchPrompt="";}
    ChatReply parseReply(String raw,String original){
        ChatReply result=new ChatReply();String value=raw==null?"":raw.trim();
        try{int start=value.indexOf('{'),end=value.lastIndexOf('}');if(start>=0&&end>=start){JSONObject o=new JSONObject(value.substring(start,end+1));result.text=o.optString("reply","").trim();JSONArray ideas=o.optJSONArray("suggestions");if(ideas!=null&&ideas.length()>0){StringBuilder items=new StringBuilder();for(int i=0;i<Math.min(2,ideas.length());i++){String idea=ideas.optString(i,"").trim();if(!idea.isEmpty()){if(items.length()>0)items.append('\n');items.append("• ").append(idea);}}if(items.length()>0)result.text+=(result.text.isEmpty()?"":"\n\nA small idea to consider:\n")+items;}
                if(o.optBoolean("needs_research",false))result.searchPrompt=o.optString("search_prompt","").trim();
            }}catch(Exception ignored){}
        if(result.text.isEmpty())result.text=value;
        if(result.text.isEmpty())result.text="I’m not sure how to shape that yet. Could you tell me one more detail?";
        String lower=result.text.toLowerCase(Locale.ROOT);
        if(needsFreshInfo(original)){if(result.searchPrompt.isEmpty())result.searchPrompt="General, up-to-date information about "+original;result.text="That needs up-to-date information, and my small local model can’t verify it offline. Want me to look it up?";return result;}
        if(result.searchPrompt.isEmpty()&&(lower.contains("i’m not sure")||lower.contains("i'm not sure")||lower.contains("i don't know")||lower.contains("cannot verify")||lower.contains("can't verify")||lower.contains("may be out of date")))result.searchPrompt="General information about "+original;
        return result;
    }
    boolean needsFreshInfo(String message){return java.util.regex.Pattern.compile("(?i)\\b(latest|current|right now|weather|news|price|stock|score|near me|open now|updated|recent|newest|release date|who is currently|what time is it)\\b").matcher(message).find();}
    void showMemories(){
        LinearLayout body=column();body.setPadding(dp(4),dp(4),dp(4),dp(4));TextView help=text("Haru remembers these notes on this phone and uses them for future chats. This is preference memory, not model training.",12,muted,false);body.addView(help);body.addView(space(8));
        final AlertDialog[] current={null};Cursor c=db.memories();boolean any=false;
        while(c.moveToNext()){
            any=true;int id=c.getInt(0);String note=c.getString(1);LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);TextView label=text(note,13,ink,false);row.addView(label,new LinearLayout.LayoutParams(0,-2,1));Button remove=button("Remove");remove.setOnClickListener(v->{db.deleteMemory(id);if(current[0]!=null)current[0].dismiss();showMemories();});row.addView(remove);body.addView(row);body.addView(space(6));
        }
        c.close();if(!any)body.addView(text("No saved notes yet.",12,muted,false));
        EditText input=new EditText(this);input.setHint("Example: Short plans work better for me.");input.setTextSize(14);input.setTextColor(ink);input.setMinLines(2);input.setGravity(Gravity.TOP|Gravity.LEFT);body.addView(input);
        ScrollView scroll=new ScrollView(this);scroll.addView(body);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Teach Haru about you").setMessage("Only save what you want it to remember.").setView(scroll).setNegativeButton("Done",null).setPositiveButton("Remember on this phone",null).create();current[0]=dialog;
        dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(b->{String note=input.getText().toString().trim();if(note.isEmpty()){input.setError("Write a preference to remember");return;}if(note.length()>1000){input.setError("Keep this note under 1,000 characters");return;}if(db.memoryCount()>=50){input.setError("Haru can keep up to 50 memory notes. Remove one first.");return;}db.addMemory(note);dialog.dismiss();Toast.makeText(this,"Saved on this phone. Haru can use it in future chats.",Toast.LENGTH_LONG).show();}));dialog.show();
    }
    void confirmClearChat(){if(busy){Toast.makeText(this,"Haru is still thinking. Give it a moment first.",Toast.LENGTH_SHORT).show();return;}new AlertDialog.Builder(this).setTitle("Clear this conversation?").setMessage("This removes the chat history from this phone. Your saved memories stay.").setNegativeButton("Keep chat",null).setPositiveButton("Clear chat",(d,w)->{db.clearChat();renderMessages();Toast.makeText(this,"Conversation cleared. Saved memories are still here.",Toast.LENGTH_SHORT).show();}).show();}
    void confirmSearch(String query){
        EditText edit=new EditText(this);edit.setText(query);edit.setTextSize(14);edit.setTextColor(ink);edit.setMinLines(2);edit.setGravity(Gravity.TOP|Gravity.LEFT);
        new AlertDialog.Builder(this).setTitle("Check the search first").setMessage("Haru isn’t sure. Review or edit this phrase. Only when you tap Search will it open your browser and send the phrase to your chosen search provider. Your chat stays in Haru on this phone.").setView(edit).setNegativeButton("Not now",null).setPositiveButton("Search the web",(d,w)->{String q=edit.getText().toString().trim();if(q.isEmpty())return;try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/search?q="+Uri.encode(q))));}catch(Exception e){Toast.makeText(this,"No browser is ready for that search.",Toast.LENGTH_LONG).show();}}).show();
    }
    View space(int height){View v=new View(this);v.setLayoutParams(new LinearLayout.LayoutParams(1,dp(height)));return v;}
    void startPrivateSpeech(){
        if(Build.VERSION.SDK_INT<31||!SpeechRecognizer.isOnDeviceRecognitionAvailable(this)){Toast.makeText(this,"This phone’s speech service doesn’t offer on-device dictation. You can type instead.",Toast.LENGTH_LONG).show();return;}
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQUEST_MIC);return;}beginSpeech();
    }
    void beginSpeech(){
        try{if(recognizer!=null)recognizer.destroy();recognizer=SpeechRecognizer.createOnDeviceSpeechRecognizer(this);recognizer.setRecognitionListener(new RecognitionListener(){
            public void onReadyForSpeech(Bundle b){Toast.makeText(TalkActivity.this,"Listening on this phone…",Toast.LENGTH_SHORT).show();}public void onBeginningOfSpeech(){}public void onRmsChanged(float r){}public void onBufferReceived(byte[] b){}public void onEndOfSpeech(){}public void onError(int e){Toast.makeText(TalkActivity.this,"Dictation ended. You can type or try again.",Toast.LENGTH_SHORT).show();}
            public void onResults(Bundle b){ArrayList<String> r=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(r!=null&&!r.isEmpty()&&composer!=null){String old=composer.getText().toString().trim();composer.setText(old.isEmpty()?r.get(0):old+"\n"+r.get(0));composer.setSelection(composer.length());}}
            public void onPartialResults(Bundle b){}public void onEvent(int e,Bundle b){}
        });Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true);recognizer.startListening(intent);}catch(Exception e){Toast.makeText(this,"On-device dictation isn’t ready on this phone yet.",Toast.LENGTH_LONG).show();}
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==REQUEST_MIC&&grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED)beginSpeech();}
    void hideKeyboard(){if(composer!=null)((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(composer.getWindowToken(),0);}
    @Override protected void onDestroy(){if(recognizer!=null){recognizer.destroy();recognizer=null;}if(db!=null)db.close();super.onDestroy();}
}
