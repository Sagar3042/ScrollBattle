package com.sagar.scrollbattle;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;
import android.util.Log;

public class ScrollAccessibilityService extends AccessibilityService {

    // স্ক্রল গোনার জন্য একটি ভেরিয়েবল
    private int scrollCount = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // চেক করছি ইভেন্টটা স্ক্রল কি না
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            
            // চেক করছি এটা ইনস্টাগ্রাম অ্যাপের ভেতর হচ্ছে কি না
            if (event.getPackageName() != null && event.getPackageName().equals("com.instagram.android")) {
                scrollCount++;
                Log.d("ScrollBattle", "Reels Scrolled: " + scrollCount);

                // ফ্লোটিং উইন্ডো আপডেট করার জন্য OverlayService-এ ডেটা পাঠানো হচ্ছে
                Intent intent = new Intent(this, OverlayService.class);
                intent.putExtra("SCROLL_COUNT", scrollCount);
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent);
                } else {
                    startService(intent);
                }
            }
        }
    }

    @Override
    public void onInterrupt() {
        // সার্ভিস কোনো কারণে বন্ধ বা ইন্টারাপ্ট হলে এখানে লজিক লেখা যায়
        Log.d("ScrollBattle", "Accessibility Service Interrupted");
    }
}
