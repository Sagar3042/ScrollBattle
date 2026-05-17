package com.sagar.scrollbattle;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;

public class ScrollAccessibilityService extends AccessibilityService {
    
    // আগের দেখা রিলস-এর সিরিয়াল নম্বর (Index) মনে রাখার জন্য ভ্যারিয়েবল
    private int lastItemIndex = -1;
    private long lastScrollTime = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            
            CharSequence packageName = event.getPackageName();
            CharSequence className = event.getClassName();
            
            if (packageName != null && packageName.toString().equals("com.instagram.android")) {
                
                // শুধুমাত্র মেইন রিলস কন্টেইনার স্ক্রল হলেই চেক করবে
                if (className != null && (className.toString().contains("RecyclerView") || className.toString().contains("ViewPager"))) {
                    
                    // অ্যান্ড্রয়েড নিজে থেকে বলে দেবে এখন লিস্টের কত নম্বর রিলস চলছে
                    int currentIndex = event.getFromIndex();
                    long currentTime = System.currentTimeMillis();
                    
                    // লজিক: 
                    // ১. যদি Index পাওয়া যায় (currentIndex != -1)
                    // ২. এবং যদি নতুন রিলসের নম্বর আগের রিলসের নম্বরের চেয়ে আলাদা হয় (currentIndex != lastItemIndex)
                    if (currentIndex != -1 && currentIndex != lastItemIndex) {
                        
                        // ডবল স্ক্রল গ্লিচ বা বাউন্স আটকাতে মাত্র ৪০০ মিলি-সেকেন্ডের একটি ছোট্ট গ্যাপ
                        if (currentTime - lastScrollTime > 400) {
                            
                            lastItemIndex = currentIndex; // নতুন রিলসের নম্বর সেভ করে রাখা হলো
                            lastScrollTime = currentTime;
                            
                            // একদম কনফার্ম ১ কাউন্ট পাঠানো হলো
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
