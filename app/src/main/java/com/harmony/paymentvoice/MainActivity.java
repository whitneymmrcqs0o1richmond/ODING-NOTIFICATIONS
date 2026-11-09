package com.harmony.paymentvoice;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;

public class MainActivity extends Activity {
 @Override public void onCreate(Bundle b) { super.onCreate(b);
  LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(36,56,36,24); root.setBackgroundColor(Color.rgb(247,249,252));
  TextView title=new TextView(this); title.setText("ODING NOTIFICATIONS"); title.setTextSize(25); title.setTextColor(Color.rgb(20,60,90)); root.addView(title);
  TextView info=new TextView(this); info.setText("GCash / Maya successful payment and load notification announcements. Only notifications with recognizable success text are spoken. Amount and reference depend on notification content."); info.setTextSize(16); info.setPadding(0,20,0,30); root.addView(info);
  Button access=new Button(this); access.setText("Enable Notification Access"); access.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))); root.addView(access);
  Button test=new Button(this); test.setText("Test Voice Announcement"); test.setOnClickListener(v->PaymentNotificationService.speak(this,"Test announcement. Amount received, five hundred pesos via GCash. Reference number, last six digits: one, two, three, four, five, six.")); root.addView(test);
  Switch enabled=new Switch(this); enabled.setText("Voice announcements enabled"); enabled.setChecked(getPreferences(0).getBoolean("enabled",true)); enabled.setOnCheckedChangeListener((v,on)->getPreferences(0).edit().putBoolean("enabled",on).apply()); root.addView(enabled);
  TextView foot=new TextView(this); foot.setText("Privacy: notification text is processed locally. This is NOT bank-side payment verification. Never use voice announcements alone as proof of payment."); foot.setTextSize(13); foot.setPadding(0,40,0,0); root.addView(foot);
  setContentView(root);
 }
}
