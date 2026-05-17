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
            CharSequence className = event.getClassName();
            
            if (packageName != null && packageName.toString().equals("com.instagram.android")) {
                
                // শুধুমাত্র মেইন রিলস কন্টেইনার (RecyclerView বা ViewPager) স্ক্রল হলেই কাউন্ট হবে।
                // ভিডিওর প্রোগ্রেস বার বা অন্য কোনো ফেক স্ক্রল ইগনোর করা হবে।
                if (className != null && (className.toString().contains("RecyclerView") || className.toString().contains("ViewPager"))) {
                    
                    long currentTime = System.currentTimeMillis();
                    
                    // ২ সেকেন্ডের কুলডাউন (একটি রিলস স্ক্রল করার স্বাভাবিক সময়)
                    if (currentTime - lastScrollTime > 2000) {
                        lastScrollTime = currentTime;
                        
                        Intent intent = new Intent("UPDATE_COUNT");
                        intent.setPackage(getPackageName());
                        sendBroadcast(intent);
                    }
                }
            }
        }
    }

    @Override
    public void onInterrupt() {}
}
