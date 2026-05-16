package com.sagar.scrollbattle;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;
import android.util.Log;

public class ScrollAccessibilityService extends AccessibilityService {

    private int scrollCount = 0;
    private long lastScrollTime = 0;
    private static final long SCROLL_COOLDOWN_MS = 1500; 

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        int eventType = event.getEventType();
        CharSequence packNameChar = event.getPackageName();
        String currentPackage = (packNameChar != null) ? packNameChar.toString() : "";

        // ১. উইন্ডো চেঞ্জ ডিটেকশন (ইনস্টাগ্রাম ওপেন/মিনিমাইজ/ক্লোজ)
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Intent intent = new Intent(this, OverlayService.class);
            
            if (currentPackage.equals("com.instagram.android")) {
                intent.putExtra("ACTION", "SHOW");
                Log.d("ScrollBattle", "Instagram View Detected - Overlay Show");
            } else if (!currentPackage.equals("com.sagar.scrollbattle") && !currentPackage.isEmpty()) {
                // ইউজার যদি আমাদের অ্যাপ বা ইনস্টাগ্রাম ছাড়া অন্য কোনো লিজিট অ্যাপে (যেমন হোম স্ক্রিন বা ফেসবুক) যায়
                intent.putExtra("ACTION", "HIDE");
                Log.d("ScrollBattle", "Left Instagram - Overlay Hide");
            }
            
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent);
                } else {
                    startService(intent);
                }
            } catch (Exception e) {
                Log.e("ScrollBattle", "Error starting service for window change: " + e.getMessage());
            }
        }

        // ২. নিখুঁত স্ক্রল কাউন্টিং লজিক
        if (eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            if (currentPackage.equals("com.instagram.android")) {
                long currentTime = System.currentTimeMillis();
                
                if ((currentTime - lastScrollTime) > SCROLL_COOLDOWN_MS) {
                    scrollCount++;
                    lastScrollTime = currentTime;
                    Log.d("ScrollBattle", "Valid Scroll Counted: " + scrollCount);

                    Intent intent = new Intent(this, OverlayService.class);
                    intent.putExtra("ACTION", "UPDATE_COUNT");
                    intent.putExtra("SCROLL_COUNT", scrollCount);
                    
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(intent);
                        } else {
                            startService(intent);
                        }
                    } catch (Exception e) {
                        Log.e("ScrollBattle", "Error starting service for scroll: " + e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    public void onInterrupt() {
        Log.d("ScrollBattle", "Accessibility Interrupted");
    }
}

