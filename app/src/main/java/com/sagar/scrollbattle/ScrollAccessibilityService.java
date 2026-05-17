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
        // শুধুমাত্র ভিউ স্ক্রল ইভেন্ট ফিল্টার করা হচ্ছে
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            CharSequence packageName = event.getPackageName();
            CharSequence className = event.getClassName();
            
            if (packageName != null && packageName.toString().equals("com.instagram.android")) {
                // ইনস্টাগ্রামের মেইন সোয়াইপিং উইজেট (ViewPager/RecyclerView) ভ্যালিডেশন
                if (className != null && (className.toString().contains("ViewPager") || className.toString().contains("RecyclerView"))) {
                    
                    AccessibilityNodeInfo sourceNode = event.getSource();
                    if (sourceNode == null) return;

                    String viewId = sourceNode.getViewIdResourceName();
                    
                    // আর্কিটেকচার লেভেল ফিল্টার:
                    // ১. প্রোফাইল স্ক্রিন ডিটেকশন (View ID তে profile বা user_profile থাকলে ব্লক)
                    if (viewId != null && (viewId.contains("profile") || viewId.contains("user_profile") || viewId.contains("self_profile"))) {
                        sourceNode.recycle();
                        return;
                    }

                    // ২. হোম স্ক্রিন বনাম মেইন রিলস স্ক্রিন ডিটেকশন:
                    // ইনস্টাগ্রামের হোম স্ক্রিনের মেইন ফিড কন্টেইনার আইডি সাধারণত "unknown_view" বা "feed" রিলেটেড হয়।
                    // কিন্তু মেইন রিলস ট্যাবের সোয়াইপার আইডিতে সবসময় "reels" বা "cliptach" অথবা সরাসরি ফুল স্ক্রিন ভিউপোর্ট থাকে।
                    // কোনো কোনো ডিভাইসে আইডি নাল (Null) আসলে আমরা চাইল্ড নোড কাউন্ট দিয়ে ফুল-স্ক্রিন রিলস প্লেয়ার ভ্যালিডেশন করব।
                    boolean isReelsFeed = false;
                    
                    if (viewId != null && (viewId.contains("reels") || viewId.contains("reel") || viewId.contains("clips"))) {
                        isReelsFeed = true;
                    } else if (viewId == null && sourceNode.getChildCount() > 0) {
                        // যদি ভিউ আইডি না থাকে, তবে নোড স্ট্রাকচার চেক করবে (রিলস প্লেয়ারে চাইল্ড নোড ডেনসিটি বেশি থাকে)
                        isReelsFeed = true;
                    }

                    sourceNode.recycle();

                    // যদি রিলস ফিড কনফার্ম না হয়, তবে কাউন্ট হবে না (হোম স্ক্রিন ফিল্টার)
                    if (!isReelsFeed) return;

                    // ৩. রিয়েল-টাইম স্ক্রল ইনডেক্স ট্র্যাকিং এবং বাউন্স ফিল্টার
                    int currentIndex = event.getFromIndex();
                    long currentTime = System.currentTimeMillis();
                    
                    if (currentIndex != -1 && currentIndex != lastItemIndex) {
                        // ৪50 মিলি-সেকেন্ডের সেফটি উইন্ডো (যাতে একটা রিলস স্ক্রল করলে ১ বারই কাউন্ট হয়)
                        if (currentTime - lastScrollTime > 450) {
                            lastItemIndex = currentIndex;
                            lastScrollTime = currentTime;
                            
                            // ব্রডকাস্ট ম্যানেজারকে কনফার্মড রিলস কাউন্ট পাঠানো হলো
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
