package com.harmony.paymentvoice;

import android.app.Notification;
import android.content.Context;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.speech.tts.TextToSpeech;
import java.util.*;
import java.util.regex.*;

public class PaymentNotificationService extends NotificationListenerService {
 private static TextToSpeech tts;
 private static final LinkedHashMap<String,Long> recent=new LinkedHashMap<>();
 private static final Pattern AMOUNT=Pattern.compile("(?i)(?:₱|PHP\\s*|P\\s*)([0-9][0-9,]*(?:\\.[0-9]{1,2})?)");
 private static final Pattern REF=Pattern.compile("(?i)(?:reference(?:\\s*(?:number|no\\.?|#))?|ref(?:\\s*(?:no\\.?|#))?|transaction\\s*(?:id|no\\.?))\\s*[:#-]?\\s*([A-Z0-9-]{6,})");
 @Override public void onNotificationPosted(StatusBarNotification sbn) {
  if(!getSharedPreferences("MainActivity",MODE_PRIVATE).getBoolean("enabled",true))return;
  String pkg=sbn.getPackageName().toLowerCase(Locale.ROOT);
  // Allowlist only official app IDs. Verify IDs on the target phone before deployment.
  String source;
  if(pkg.equals("com.globe.gcash.android"))source="GCash";
  else if(pkg.equals("com.paymaya"))source="Maya";
  else return;
  Bundle extras=sbn.getNotification().extras;
  StringBuilder sb=new StringBuilder();
  append(sb,extras.getCharSequence(Notification.EXTRA_TITLE));
  append(sb,extras.getCharSequence(Notification.EXTRA_TEXT));
  append(sb,extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
  CharSequence[] lines=extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
  if(lines!=null)for(CharSequence line:lines)append(sb,line);
  String message=sb.toString(); String lower=message.toLowerCase(Locale.ROOT);
  if(!Pattern.compile("(?i)\\b(successful|successfully|completed|received|credited)\\b").matcher(message).find())return;
  if(Pattern.compile("(?i)\\b(failed|declined|reversed|refunded|pending|unsuccessful|cancelled|canceled|request|otp|verification code)\\b").matcher(message).find())return;
  boolean load=Pattern.compile("(?i)\\b(load|top[ -]?up|recharge|prepaid)\\b").matcher(message).find();
  boolean incoming=Pattern.compile("(?i)\\b(received|you got|you have received|credited|payment received|sent you)\\b").matcher(message).find();
  if(!load&&!incoming)return; // Do not announce outgoing transfers as received payments.
  Matcher amount=AMOUNT.matcher(message); if(!amount.find())return;
  String value=amount.group(1).replace(",","");
  Matcher ref=REF.matcher(message); String last="";
  if(ref.find()){String digits=ref.group(1).replaceAll("[^0-9]","");if(digits.length()>=6)last=digits.substring(digits.length()-6);}
  String key=source+"|"+message;
  long now=System.currentTimeMillis();
  synchronized(recent){recent.entrySet().removeIf(e->now-e.getValue()>600000);if(recent.containsKey(key))return;recent.put(key,now);while(recent.size()>100)recent.remove(recent.keySet().iterator().next());}
  String words=amountWords(value);
  String phrase=load?"Successful load or top up notification. "+words+" pesos via "+source+".":"Amount received, "+words+" pesos via "+source+".";
  if(!last.isEmpty()){phrase+=" Reference number, last six digits: ";for(char d:last.toCharArray())phrase+=d+", ";}
  else phrase+=" Reference number not available in notification.";
  speak(this,phrase);
 }
 private static void append(StringBuilder sb,CharSequence v){if(v!=null)sb.append(' ').append(v);}
 private static String amountWords(String value){try{java.math.BigDecimal bd=new java.math.BigDecimal(value);long whole=bd.longValue();int cents=bd.remainder(java.math.BigDecimal.ONE).movePointRight(2).abs().intValue();String s=number(whole);return cents>0?s+" and "+number(cents)+" centavos":s;}catch(Exception e){return value;}}
 private static String number(long n){if(n==0)return "zero";if(n<0)return "minus "+number(-n);String[] ones={"","one","two","three","four","five","six","seven","eight","nine","ten","eleven","twelve","thirteen","fourteen","fifteen","sixteen","seventeen","eighteen","nineteen"};String[] tens={"","","twenty","thirty","forty","fifty","sixty","seventy","eighty","ninety"};if(n<20)return ones[(int)n];if(n<100)return tens[(int)(n/10)]+(n%10>0?" "+number(n%10):"");if(n<1000)return number(n/100)+" hundred"+(n%100>0?" "+number(n%100):"");if(n<1000000)return number(n/1000)+" thousand"+(n%1000>0?" "+number(n%1000):"");if(n<1000000000)return number(n/1000000)+" million"+(n%1000000>0?" "+number(n%1000000):"");return number(n/1000000000)+" billion"+(n%1000000000>0?" "+number(n%1000000000):"");}
 public static synchronized void speak(Context ctx,String message){
  if(tts==null){tts=new TextToSpeech(ctx.getApplicationContext(),status->{if(status==TextToSpeech.SUCCESS){tts.setLanguage(Locale.US);tts.setSpeechRate(.92f);tts.speak(message,TextToSpeech.QUEUE_ADD,null,UUID.randomUUID().toString());}});}
  else tts.speak(message,TextToSpeech.QUEUE_ADD,null,UUID.randomUUID().toString());
 }
}
