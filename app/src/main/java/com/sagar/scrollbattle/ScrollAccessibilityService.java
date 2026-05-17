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
                
                // ১. বেসিক ভ্যালিডেশন: শুধুমাত্র রিলস বা ভিডিও স্ক্রলিং কন্টেইনার চেক হবে
                if (className != null && (className.toString().contains("ViewPager") || className.toString().contains("RecyclerView"))) {
                    
                    // ২. কড়া সিকিউরিটি চেক: প্রোফাইল বা অন্য স্ক্রিন আটকাতে উইন্ডো সোর্স চেক করা
                    AccessibilityNodeInfo source = event.getSource();
                    if (source != null) {
                        String viewId = source.getViewIdResourceName();
                        
                        // ইনস্টাগ্রামের মেইন রিলস ট্যাবের স্পেসিফিক ভিউ আইডি বা প্যাটার্ন লক করা
                        // প্রোফাইল পেজের স্ক্রলিং আইডিতে 'profile' বা 'user' টেক্সট থাকে, সেগুলোকে আমরা ব্লক করব
                        if (viewId != null && (viewId.contains("profile") || viewId.contains("user_profile") || viewId.contains("self_profile"))) {
                            source.recycle();
                            return; // কারো প্রোফাইলে স্ক্রল করলে এখানেই কোড আটকে যাবে, কাউন্ট হবে না
                        }
                        source.recycle();
                    }

                    int currentIndex = event.getFromIndex();
                    long currentTime = System.currentTimeMillis();
                    
                    // ৩. রিলস ইনডেক্স ট্র্যাকিং লজিক
                    if (currentIndex != -1 && currentIndex != lastItemIndex) {
                        
                        // ডবল বা ফেক স্ক্রল আটকাতে ৪০০ মিলি-সেকেন্ডের সেফটি বাফার
                        if (currentTime - lastScrollTime > 400) {
                            
                            lastItemIndex = currentIndex;
                            lastScrollTime = currentTime;
                            
                            // ১০০% কনফার্ম রিলস কাউন্ট ব্রডকাস্ট পাঠানো হলো
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
