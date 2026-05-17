package com.sagar.scrollbattle;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;

public class ScrollAccessibilityService extends AccessibilityService {
    
    private long lastScrollTime = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            CharSequence packageName = event.getPackageName();
            if (packageName != null && packageName.toString().equals("com.instagram.android")) {
                
                long currentTime = System.currentTimeMillis();
                // একটি রিলস সম্পূর্ণ দেখতে/স্ক্রল করতে সময় লাগে। 
                // তাই আগের স্ক্রল থেকে অন্তত ২৫০০ মিলিসেকেন্ড (২.৫ সেকেন্ড) পার হলে তবেই কাউন্ট হবে।
                if (currentTime - lastScrollTime > 1000) {
                    lastScrollTime = currentTime;
                    Intent intent = new Intent("UPDATE_COUNT");
                    intent.setPackage(getPackageName());
                    sendBroadcast(intent);
                }
            }
        }
    }

    @Override
    public void onInterrupt() {}
}
