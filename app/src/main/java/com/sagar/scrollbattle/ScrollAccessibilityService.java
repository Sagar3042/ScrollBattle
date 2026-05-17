package com.sagar.scrollbattle;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class ScrollAccessibilityService extends AccessibilityService {
    private int lastScrollEventId = -1;
    private long lastTriggerTime = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            AccessibilityNodeInfo source = event.getSource();
            if (source == null) return;

            // শুধুমাত্র রিলস ফিডের মেইন ভিউপোর্ত ধরবে
            String className = String.valueOf(event.getClassName());
            if (className.contains("RecyclerView") || className.contains("ViewPager")) {
                
                // হার্ড-কোর ফিল্টার: শুধুমাত্র ভিডিও প্লেয়ার কম্পোনেন্ট চেক করবে
                // প্রোফাইল বা হোম স্ক্রিনে ভিডিও এলিমেন্টগুলো "Playing" স্টেটে থাকে না
                if (isReelsFeedActive(source)) {
                    int eventId = event.hashCode();
                    long currentTime = System.currentTimeMillis();

                    // বাউন্স প্রোটেকশন: দ্রুত স্ক্রল করলেও যেন একই ভিডিও বারবার কাউন্ট না হয়
                    if (eventId != lastScrollEventId && (currentTime - lastTriggerTime > 600)) {
                        lastScrollEventId = eventId;
                        lastTriggerTime = currentTime;

                        Intent intent = new Intent("UPDATE_COUNT");
                        intent.setPackage(getPackageName());
                        sendBroadcast(intent);
                    }
                }
            }
            source.recycle();
        }
    }

    private boolean isReelsFeedActive(AccessibilityNodeInfo node) {
        // ইনস্টাগ্রাম রিলস ফিডে সবসময় একটি "Video" বা "Player" টাইপ এলিমেন্ট থাকে 
        // যা হোম বা প্রোফাইল স্ক্রিনে থাকে না।
        return node.toString().contains("Video") || node.toString().contains("Player");
    }

    @Override
    public void onInterrupt() {}
}
