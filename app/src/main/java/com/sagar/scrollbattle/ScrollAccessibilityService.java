package com.sagar.scrollbattle;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;
import android.util.Log;

public class ScrollAccessibilityService extends AccessibilityService {

    private int scrollCount = 0;
    private long lastScrollTime = 0;
    private static final long SCROLL_COOLDOWN_MS = 1500; 

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        
        // ১. ১০০% নিখুঁত স্মার্ট ওভারলে ভিজিবিলিটি লজিক
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
            event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            
            CharSequence packageName = event.getPackageName();
            Intent intent = new Intent(this, OverlayService.class);
            
            if (packageName != null && packageName.toString().equals("com.instagram.android")) {
                // ইউজার ইনস্টাগ্রামের ভেতরে থাকলে ওভারলে দেখাবে
                intent.putExtra("ACTION", "SHOW");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent);
                } else {
                    startService(intent);
                }
            } else {
                // মিনিমাইজ করলে, ব্যাক করলে বা প্যাকেজ নেম অন্য কিছু বা null হলে ওভারলে সাথে সাথে লুকিয়ে যাবে
                intent.putExtra("ACTION", "HIDE");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent);
                } else {
                    startService(intent);
                }
            }
        }

        // ২. রিলস স্ক্রল কাউন্টিং লজিক
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            if (event.getPackageName() != null && event.getPackageName().equals("com.instagram.android")) {
                long currentTime = System.currentTimeMillis();
                
                if ((currentTime - lastScrollTime) > SCROLL_COOLDOWN_MS) {
                    scrollCount++;
                    lastScrollTime = currentTime;

                    Intent intent = new Intent(this, OverlayService.class);
                    intent.putExtra("ACTION", "UPDATE_COUNT");
                    intent.putExtra("SCROLL_COUNT", scrollCount);
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent);
                    } else {
                        startService(intent);
                    }
                }
            }
        }
    }

    @Override
    public void onInterrupt() {
        Log.d("ScrollBattle", "Accessibility Interrupted");
    }
}

