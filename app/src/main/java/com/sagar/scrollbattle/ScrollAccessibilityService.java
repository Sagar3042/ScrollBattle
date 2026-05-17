package com.sagar.scrollbattle;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;

public class ScrollAccessibilityService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // যখন স্ক্রিনে কোনো কিছু স্ক্রল হয়, তখন এই অংশটা ট্রিগার হবে
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            
            CharSequence packageName = event.getPackageName();
            
            // আমরা চেক করছি যে ইউজার শুধু ইন্সটাগ্রামেই (Instagram) স্ক্রল করছে কি না
            if (packageName != null && packageName.toString().equals("com.instagram.android")) {
                
                // স্ক্রল হলেই আমরা "UPDATE_COUNT" নামের একটি ব্রডকাস্ট সিগন্যাল পাঠাবো
                Intent intent = new Intent("UPDATE_COUNT");
                
                // সিকিউরিটির জন্য শুধুমাত্র আমাদের অ্যাপেই এই সিগন্যাল রিসিভ হবে
                intent.setPackage(getPackageName()); 
                
                sendBroadcast(intent);
            }
        }
    }

    @Override
    public void onInterrupt() {
        // সার্ভিস কোনো কারণে বাধা পেলে এই মেথড কল হয়, আপাতত এখানে কিছু করার দরকার নেই
    }
    
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        // সার্ভিস সাকসেসফুলি কানেক্ট হলে চাইলে লগ প্রিন্ট করা যায়
    }
}

