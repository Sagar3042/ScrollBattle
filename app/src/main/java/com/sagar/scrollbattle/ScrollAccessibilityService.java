package com.sagar.scrollbattle;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;
import android.util.Log;

public class ScrollAccessibilityService extends AccessibilityService {

    private int scrollCount = 0;
    private long lastScrollTime = 0;
    // একটি রিলস থেকে আরেকটিতে যেতে অন্তত ১.৫ সেকেন্ড (1500 ms) সময় লাগে, এই লজিক দিয়ে আমরা ফেক স্ক্রল আটকাবো
    private static final long SCROLL_COOLDOWN_MS = 1500; 

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        
        // ১. Smart Overlay Logic (ইনস্টাগ্রাম খোলা আছে কি না চেক করা)
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence packageName = event.getPackageName();
            if (packageName != null) {
                Intent intent = new Intent(this, OverlayService.class);
                
                if (packageName.toString().equals("com.instagram.android")) {
                    // ইনস্টাগ্রাম ওপেন হলে ওভারলে দেখানোর নির্দেশ
                    intent.putExtra("ACTION", "SHOW");
                    Log.d("ScrollBattle", "Instagram Opened");
                } else if (!packageName.toString().equals("com.sagar.scrollbattle") && 
                           !packageName.toString().equals("com.android.systemui")) {
                    // অন্য অ্যাপ ওপেন হলে ওভারলে লুকানোর নির্দেশ
                    intent.putExtra("ACTION", "HIDE");
                    Log.d("ScrollBattle", "Instagram Closed");
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent);
                } else {
                    startService(intent);
                }
            }
        }

        // ২. Perfect Scroll Counting Logic (ফেক কাউন্ট বা ডাবল কাউন্ট বন্ধ করা)
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            if (event.getPackageName() != null && event.getPackageName().equals("com.instagram.android")) {
                
                long currentTime = System.currentTimeMillis();
                
                // যদি আগের স্ক্রল এবং বর্তমান স্ক্রলের মধ্যে ১.৫ সেকেন্ডের গ্যাপ থাকে, তবেই কাউন্ট হবে
                if ((currentTime - lastScrollTime) > SCROLL_COOLDOWN_MS) {
                    scrollCount++;
                    lastScrollTime = currentTime;
                    Log.d("ScrollBattle", "Real Reels Scrolled: " + scrollCount);

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

