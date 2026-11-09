package com.harmony.paymentvoice;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.Color;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
 private LinearLayout root;
 private SharedPreferences prefs;
 private TextView history;
 @Override public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("oding_settings",MODE_PRIVATE);
  ScrollView scroll=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(30,45,30,35);root.setBackgroundColor(Color.rgb(246,249,252));scroll.addView(root);
  TextView title=label("ODING NOTIFICATIONS V2",26);title.setTextColor(Color.rgb(16,65,103));root.addView(title);
  root.addView(label("Voice announcements from supported GCash, Maya and MariBank notifications",15));
  Button access=new Button(this);access.setText("Enable Notification Access");access.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));root.addView(access);
  addSwitch("Voice announcements enabled","enabled");addSwitch("GCash notifications","gcash");addSwitch("Maya notifications","maya");addSwitch("MariBank notifications","maribank");
  Button test=new Button(this);test.setText("Test Voice Announcement");test.setOnClickListener(v->PaymentNotificationService.speak(this,"Test announcement. Amount received, one thousand five hundred pesos via MariBank. Reference number ending in one, two, three, four, five, six."));root.addView(test);
  root.addView(label("Recent detected transactions",20));history=label("",14);root.addView(history);
  Button refresh=new Button(this);refresh.setText("Refresh History");refresh.setOnClickListener(v->refreshHistory());root.addView(refresh);
  Button clear=new Button(this);clear.setText("Clear History");clear.setOnClickListener(v->{prefs.edit().remove("history").apply();refreshHistory();});root.addView(clear);
  TextView privacy=label("Privacy: Notifications are processed on your device only. Announcements are NOT bank-side proof of payment. MariBank notification package and wording may vary; test with genuine incoming notifications. Never trust notification text alone for releasing goods.",13);privacy.setPadding(0,22,0,0);root.addView(privacy);
  setContentView(scroll);refreshHistory();
 }
 private TextView label(String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setPadding(0,12,0,12);return t;}
 private void addSwitch(String name,String key){Switch s=new Switch(this);s.setText(name);s.setTextSize(17);s.setPadding(0,15,0,15);s.setChecked(prefs.getBoolean(key,true));s.setOnCheckedChangeListener((v,on)->prefs.edit().putBoolean(key,on).apply());root.addView(s);}
 private void refreshHistory(){StringBuilder sb=new StringBuilder();try{JSONArray arr=new JSONArray(prefs.getString("history","[]"));SimpleDateFormat fmt=new SimpleDateFormat("MMM dd, yyyy hh:mm a",Locale.getDefault());for(int i=0;i<arr.length();i++){JSONObject o=arr.getJSONObject(i);sb.append(fmt.format(new Date(o.optLong("time")))).append("\n").append(o.optString("source")).append(" · ").append(o.optString("type")).append(" · ₱").append(o.optString("amount"));String ref=o.optString("reference");if(!ref.isEmpty())sb.append(" · Ref ending ").append(ref);sb.append("\n\n");}}catch(Exception ignored){}history.setText(sb.length()==0?"No detected transactions yet.":sb.toString());}
 @Override protected void onResume(){super.onResume();if(history!=null)refreshHistory();}
}
