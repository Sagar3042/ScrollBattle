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
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            CharSequence packageName = event.getPackageName();
            CharSequence className = event.getClassName();
            
            if (packageName != null && packageName.toString().equals("com.instagram.android")) {
                if (className != null && (className.toString().contains("RecyclerView") || className.toString().contains("ViewPager"))) {
                    
                    AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                    if (rootNode == null) return;

                    // স্মার্ট নেগেটিভ ফিল্টার: হোম বা প্রোফাইল স্ক্রিনের নির্দিষ্ট টেক্সট খুঁজবে
                    boolean isFakeScreen = hasText(rootNode, "Edit profile") || 
                                           hasText(rootNode, "Share profile") ||
                                           hasText(rootNode, "Message") ||
                                           hasText(rootNode, "Your story") ||
                                           hasText(rootNode, "Search");

                    rootNode.recycle();

                    // যদি প্রোফাইল বা হোম স্ক্রিন হয়, তবে কাউন্টার এখানেই আটকে যাবে!
                    if (isFakeScreen) {
                        return;
                    }

                    // তার মানে এটি ১০০% রিলস স্ক্রিন! এবার কাউন্ট শুরু হবে...
                    int currentIndex = event.getFromIndex();
                    long currentTime = System.currentTimeMillis();
                    
                    if (currentIndex != -1 && currentIndex != lastItemIndex) {
                        // ৫০০ মিলি-সেকেন্ডের বাউন্স প্রোটেকশন
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

    // স্ক্রিনে নির্দিষ্ট কোনো লেখা আছে কি না, তা চেক করার ফাংশন
    private boolean hasText(AccessibilityNodeInfo root, String text) {
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(text);
        return nodes != null && !nodes.isEmpty();
    }

    @Override
    public void onInterrupt() {}
}
