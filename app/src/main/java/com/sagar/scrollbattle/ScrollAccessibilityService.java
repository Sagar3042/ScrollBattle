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
                if (className != null && (className.toString().contains("ViewPager") || className.toString().contains("RecyclerView"))) {
                    
                    AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                    if (rootNode == null) return;

                    boolean isHomeScreen = !rootNode.findAccessibilityNodeInfosByText("Search").isEmpty() 
                                        || !rootNode.findAccessibilityNodeInfosByText("Direct").isEmpty();
                                        
                    boolean isProfileScreen = !rootNode.findAccessibilityNodeInfosByText("Edit profile").isEmpty() 
                                           || !rootNode.findAccessibilityNodeInfosByText("Share profile").isEmpty()
                                           || !rootNode.findAccessibilityNodeInfosByText("Posts").isEmpty();

                    List<AccessibilityNodeInfo> audioNodes = rootNode.findAccessibilityNodeInfosByText("Original audio");
                    boolean hasReelsElements = !audioNodes.isEmpty() || className.toString().contains("ViewPager");

                    if (isHomeScreen || isProfileScreen || !hasReelsElements) {
                        rootNode.recycle();
                        return;
                    }
                    rootNode.recycle();

                    int currentIndex = event.getFromIndex();
                    long currentTime = System.currentTimeMillis();
                    
                    if (currentIndex != -1 && currentIndex != lastItemIndex) {
                        if (currentTime - lastScrollTime > 400) {
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
    @Override public void onInterrupt() {}
}
