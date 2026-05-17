package com.sagar.scrollbattle;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

public class ScrollAccessibilityService extends AccessibilityService {
    
    private int lastItemIndex = -1;
    private long lastScrollTime = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // শুধুমাত্র স্ক্রলিং ইভেন্ট ট্র্যাক করা হবে
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            
            CharSequence packageName = event.getPackageName();
            CharSequence className = event.getClassName();
            
            if (packageName != null && packageName.toString().equals("com.instagram.android")) {
                
                // ভিউ কন্টেইনার ভ্যালিডেশন
                if (className != null && (className.toString().contains("ViewPager") || className.toString().contains("RecyclerView"))) {
                    
                    // উইন্ডোর রুট নোড নেওয়া হচ্ছে পুরো স্ক্রিন স্ক্যান করার জন্য
                    AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                    if (rootNode == null) return;

                    // ১. হোম স্ক্রিন এবং প্রোফাইল স্ক্রিন ডিটেকশন ফিল্টার
                    // ইনস্টাগ্রাম হোম স্ক্রিনে "Search", "Direct" বা "Create" বাটন/টেক্সট থাকে।
                    // প্রোফাইল স্ক্রিনে "Edit profile", "Share profile" বা "Posts" ট্যাব থাকে।
                    // কিন্তু মেইন রিলস ট্যাবে এগুলো কিচ্ছু থাকে না!
                    boolean isHomeScreen = !rootNode.findAccessibilityNodeInfosByText("Search").isEmpty() 
                                        || !rootNode.findAccessibilityNodeInfosByText("Direct").isEmpty();
                                        
                    boolean isProfileScreen = !rootNode.findAccessibilityNodeInfosByText("Edit profile").isEmpty() 
                                           || !rootNode.findAccessibilityNodeInfosByText("Share profile").isEmpty()
                                           || !rootNode.findAccessibilityNodeInfosByText("Posts").isEmpty();

                    // ২. রিলস ট্যাব কনফার্মেশন চেক
                    // ইনস্টাগ্রাম রিলস স্ক্রিনের অডিও সেকশনে সবসময় "Original audio" বা "Remix" অথবা ক্যামেরা আইকন থাকে।
                    List<AccessibilityNodeInfo> audioNodes = rootNode.findAccessibilityNodeInfosByText("Original audio");
                    boolean hasReelsElements = !audioNodes.isEmpty() || className.toString().contains("ViewPager");

                    // যদি হোম স্ক্রিন বা প্রোফাইল স্ক্রিন ডিটেক্ট হয়, অথবা রিলসের মেইন এলিমেন্ট না থাকে—তবে কাউন্ট ব্লক হবে
                    if (isHomeScreen || isProfileScreen || !hasReelsElements) {
                        rootNode.recycle();
                        return; // এখানেই কোড আটকে যাবে, কোনো কাউন্ট হবে না
                    }
                    rootNode.recycle();

                    // ৩. ইনডেক্স ট্র্যাকিং এবং কাউন্টিং লজিক
                    int currentIndex = event.getFromIndex();
                    long currentTime = System.currentTimeMillis();
                    
                    if (currentIndex != -1 && currentIndex != lastItemIndex) {
                        // গ্লিচ বা বাউন্স স্ক্রল আটকাতে ৪০০ মিলি-সেকেন্ডের বাফার
                        if (currentTime - lastScrollTime > 400) {
                            
                            lastItemIndex = currentIndex;
                            lastScrollTime = currentTime;
                            
                            // ১০০% জেনুইন রিলস কাউন্ট আপডেট পাঠানো হলো
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
