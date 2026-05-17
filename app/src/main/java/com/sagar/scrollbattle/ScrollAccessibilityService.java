package com.sagar.scrollbattle;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class ScrollAccessibilityService extends AccessibilityService {
    private int lastItemIndex = -1;
    private long lastScrollTime = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            CharSequence packageName = event.getPackageName();
            CharSequence className = event.getClassName();
            
            if (packageName != null && packageName.toString().equals("com.instagram.android")) {
                if (className != null && (className.toString().contains("RecyclerView") || className.toString().contains("ViewPager"))) {
                    
                    AccessibilityNodeInfo source = event.getSource();
                    if (source == null) return;

                    // 🧠 ম্যাজিক স্ট্রাকচারাল ফিল্টার:
                    // রিলস ফিড ফুল-স্ক্রিন হওয়ায় এর ভেতরে একসাথে ১ বা ২টির বেশি চাইল্ড আইটেম থাকতে পারে না।
                    // কিন্তু প্রোফাইল গ্রিড বা হোম ফিডে একসাথে অনেকগুলো আইটেম (৩-এর বেশি) থাকে।
                    int childCount = source.getChildCount();
                    source.recycle();

                    // যদি স্ক্রিনে ৩টির বেশি আইটেম থাকে, তার মানে ওটা প্রোফাইল বা হোম স্ক্রিন। কাউন্ট ব্লক!
                    if (childCount == 0 || childCount > 3) {
                        return; 
                    }

                    // তার মানে এটি ১০০% ফুল-স্ক্রিন রিলস ভিউ! এবার কাউন্ট শুরু হবে...
                    int currentIndex = event.getFromIndex();
                    long currentTime = System.currentTimeMillis();
                    
                    if (currentIndex != -1 && currentIndex != lastItemIndex) {
                        // ৫০০ মিলি-সেকেন্ডের বাউন্স প্রোটেকশন (যাতে একই রিলস বারবার কাউন্ট না হয়)
                        if (currentTime - lastScrollTime > 500) {
                            lastItemIndex = currentIndex;
                            lastScrollTime = currentTime;
                            
                            Intent intent = new Intent("UPDATE_COUNT");
                            intent.setPackage(getPackageName());
                            sendBroadcast(intent);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onInterrupt() {}
}
