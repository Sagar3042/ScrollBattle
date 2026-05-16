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
        if (event == null) return;

        // আমরা শুধু স্ক্রল ইভেন্ট এবং ইনস্টাগ্রাম প্যাকেজ ট্র্যাক করব
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            CharSequence packNameChar = event.getPackageName();
            String currentPackage = (packNameChar != null) ? packNameChar.toString() : "";

            if (currentPackage.equals("com.instagram.android")) {
                long currentTime = System.currentTimeMillis();
                
                // ১.৫ সেকেন্ডের রিয়েল স্ক্রল ডিটেকশন
                if ((currentTime - lastScrollTime) > SCROLL_COOLDOWN_MS) {
                    scrollCount++;
                    lastScrollTime = currentTime;
                    Log.d("ScrollBattle", "Valid Reels Scroll: " + scrollCount);

                    // সরাসরি ওভারলে সার্ভিসে কাউন্ট পাঠানো হচ্ছে যা ওভারলেকে ১০ সেকেন্ডের জন্য পপ-আপ করাবে
                    Intent intent = new Intent(this, OverlayService.class);
                    intent.putExtra("ACTION", "UPDATE_COUNT");
                    intent.putExtra("SCROLL_COUNT", scrollCount);
                    
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(intent);
                        } else {
                            startService(intent);
                        }
                    } catch (Exception e) {
                        Log.e("ScrollBattle", "Error starting service: " + e.getMessage());
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

