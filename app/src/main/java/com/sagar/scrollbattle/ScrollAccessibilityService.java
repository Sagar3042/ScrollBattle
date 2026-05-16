package com.sagar.scrollbattle;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ScrollAccessibilityService extends AccessibilityService {

    private int scrollCount = 0;
    private long lastScrollTime = 0;
    private static final long SCROLL_COOLDOWN_MS = 1500; 
    
    private DatabaseReference userRef;
    private String userEmail = "Unknown";

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        
        // সার্ভিস স্টার্ট হওয়ার সাথে সাথে ফায়ারবেস থেকে লগিন করা ইউজারের তথ্য নেওয়া
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userEmail = currentUser.getEmail();
            // ইউজারের ইউনিক ID (UID) দিয়ে ডাটাবেসে তার আলাদা জায়গা তৈরি করা
            userRef = FirebaseDatabase.getInstance().getReference("Leaderboard").child(currentUser.getUid());

            // ডাটাবেস থেকে ইউজারের আগের জমানো স্কোর টেনে আনা
            userRef.child("score").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        scrollCount = snapshot.getValue(Integer.class);
                    } else {
                        // নতুন ইউজার হলে ডাটাবেসে প্রথমবার এন্ট্রি করা
                        userRef.child("email").setValue(userEmail);
                        userRef.child("score").setValue(0);
                    }
                }
                @Override
                public void onCancelled(DatabaseError error) {}
            });
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            CharSequence packNameChar = event.getPackageName();
            String currentPackage = (packNameChar != null) ? packNameChar.toString() : "";

            if (currentPackage.equals("com.instagram.android")) {
                long currentTime = System.currentTimeMillis();
                
                if ((currentTime - lastScrollTime) > SCROLL_COOLDOWN_MS) {
                    scrollCount++;
                    lastScrollTime = currentTime;
                    Log.d("ScrollBattle", "Valid Reels Scroll: " + scrollCount);

                    // ১. ফায়ারবেস লিডারবোর্ডে স্কোর রিয়েল-টাইমে আপডেট করা
                    if (userRef != null) {
                        userRef.child("email").setValue(userEmail);
                        userRef.child("score").setValue(scrollCount);
                    }

                    // ২. ওভারলে সার্ভিসে কাউন্ট পাঠানো (১০ সেকেন্ড পপ-আপের জন্য)
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
                        Log.e("ScrollBattle", "Error starting service: " + e.getMessage());
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

