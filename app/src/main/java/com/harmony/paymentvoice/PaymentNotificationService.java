package com.harmony.paymentvoice;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.speech.tts.TextToSpeech;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;
import java.util.regex.*;

public class PaymentNotificationService extends NotificationListenerService {
 private static TextToSpeech tts;
 private static final LinkedHashMap<String,Long> recent=new LinkedHashMap<>();
 private static final Pattern AMOUNT=Pattern.compile("(?i)(?:₱|PHP\\s*|P\\s+)([0-9][0-9,]*(?:\\.[0-9]{1,2})?)");
 private static final Pattern REF=Pattern.compile("(?i)(?:reference(?:\\s*(?:number|no\\.?|#))?|ref(?:\\s*(?:no\\.?|#))?|transaction\\s*(?:id|no\\.?))\\s*[:#-]?\\s*([A-Z0-9-]{6,})");
 private static final Pattern BAD=Pattern.compile("(?i)\\b(failed|declined|reversed|refunded|pending|unsuccessful|cancelled|canceled|request|otp|verification code|sent|you paid|paid to|debited|withdrawal|withdrawn|cash out|transferred to|payment due|bill due)\\b");
 private static final Pattern INCOMING=Pattern.compile("(?i)(?:\\b(?:received|credited|deposited|incoming|you got|sent you)\\b|payment received|money received|has been credited|has been deposited|transfer received)");
 private static final Pattern LOAD=Pattern.compile("(?i)(?:load|top[ -]?up|recharge|prepaid)");
 private static final Pattern SUCCESS=Pattern.compile("(?i)\\b(successful|successfully|completed|confirmed|processed)\\b");
 private static final String PREFS="oding_settings";
 @Override public void onNotificationPosted(StatusBarNotification sbn){
  SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);
  if(!p.getBoolean("enabled",true))return;
  String pkg=sbn.getPackageName().toLowerCase(Locale.ROOT);
  String source;String pref;
  if(pkg.equals("com.globe.gcash.android")){source="GCash";pref="gcash";}
  else if(pkg.equals("com.paymaya")){source="Maya";pref="maya";}
  else if(pkg.equals("ph.com.maribank.ph")){source="MariBank";pref="maribank";}
  else return;
  if(!p.getBoolean(pref,true))return;
  Notification n=sbn.getNotification();if(n==null)return;
  Bundle e=n.extras;if(e==null)return;
  StringBuilder sb=new StringBuilder();
  append(sb,e.getCharSequence(Notification.EXTRA_TITLE));append(sb,e.getCharSequence(Notification.EXTRA_TEXT));append(sb,e.getCharSequence(Notification.EXTRA_BIG_TEXT));
  CharSequence[] lines=e.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);if(lines!=null)for(CharSequence line:lines)append(sb,line);
  String message=sb.toString().trim();if(message.isEmpty()||BAD.matcher(message).find())return;
  boolean incoming=INCOMING.matcher(message).find();
  boolean load=LOAD.matcher(message).find()&&SUCCESS.matcher(message).find()&&Pattern.compile("(?i)(?:load|top[ -]?up|recharge).{0,80}(?:successful|successfully|completed|confirmed)|(?:successful|successfully|completed|confirmed).{0,80}(?:load|top[ -]?up|recharge)").matcher(message).find();
  if(!incoming&&!load)return;
  Matcher amount=AMOUNT.matcher(message);if(!amount.find())return;
  String value=amount.group(1).replace(",","");
  Matcher ref=REF.matcher(message);String last="";
  if(ref.find()){String digits=ref.group(1).replaceAll("[^0-9]","");if(digits.length()>=6)last=digits.substring(digits.length()-6);}
  String key=source+"|"+message.replaceAll("\\s+"," ");long now=System.currentTimeMillis();
  synchronized(recent){recent.entrySet().removeIf(x->now-x.getValue()>600000);if(recent.containsKey(key))return;recent.put(key,now);while(recent.size()>100)recent.remove(recent.keySet().iterator().next());}
  String phrase=(incoming?"Amount received, ":"Successful load top up, ")+amountWords(value)+" pesos via "+source+".";
  if(!last.isEmpty()){phrase+=" Reference number, last six digits: ";for(char d:last.toCharArray())phrase+=d+", ";}
  else phrase+=" Reference number not available in notification.";
  saveHistory(source,value,last,incoming?"Incoming payment":"Load top-up",now);
  speak(this,phrase);
 }
 private void saveHistory(String source,String amount,String last,String type,long now){
  SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);
  try{JSONArray old=new JSONArray(p.getString("history","[]"));JSONArray updated=new JSONArray();JSONObject item=new JSONObject();item.put("source",source);item.put("amount",amount);item.put("reference",last);item.put("type",type);item.put("time",now);updated.put(item);for(int i=0;i<old.length()&&i<99;i++)updated.put(old.getJSONObject(i));p.edit().putString("history",updated.toString()).apply();}catch(Exception ignored){}
 }
 private static void append(StringBuilder sb,CharSequence v){if(v!=null)sb.append(' ').append(v);}
 private static String amountWords(String value){try{java.math.BigDecimal bd=new java.math.BigDecimal(value);long whole=bd.longValue();int cents=bd.remainder(java.math.BigDecimal.ONE).movePointRight(2).abs().intValue();String s=number(whole);return cents>0?s+" and "+number(cents)+" centavos":s;}catch(Exception e){return value;}}
 private static String number(long n){if(n==0)return "zero";if(n<0)return "minus "+number(-n);String[] ones={"","one","two","three","four","five","six","seven","eight","nine","ten","eleven","twelve","thirteen","fourteen","fifteen","sixteen","seventeen","eighteen","nineteen"};String[] tens={"","","twenty","thirty","forty","fifty","sixty","seventy","eighty","ninety"};if(n<20)return ones[(int)n];if(n<100)return tens[(int)(n/10)]+(n%10>0?" "+number(n%10):"");if(n<1000)return number(n/100)+" hundred"+(n%100>0?" "+number(n%100):"");if(n<1000000)return number(n/1000)+" thousand"+(n%1000>0?" "+number(n%1000):"");if(n<1000000000)return number(n/1000000)+" million"+(n%1000000>0?" "+number(n%1000000):"");return number(n/1000000000)+" billion"+(n%1000000000>0?" "+number(n%1000000000):"");}
 public static synchronized void speak(Context ctx,String message){
  if(tts==null){tts=new TextToSpeech(ctx.getApplicationContext(),status->{if(status==TextToSpeech.SUCCESS){tts.setLanguage(Locale.US);tts.setSpeechRate(.92f);tts.speak(message,TextToSpeech.QUEUE_ADD,null,UUID.randomUUID().toString());}});}
  else tts.speak(message,TextToSpeech.QUEUE_ADD,null,UUID.randomUUID().toString());
 }
}
