#!/bin/bash

echo "🚀 Scroll Battle অটো-ফিক্সার এবং গিটহাব পুশার শুরু হচ্ছে..."
echo "--------------------------------------------------------"

# ১. MainActivity.java আপডেট (লোডিং অ্যানিমেশন, পারমিশন রিলক এবং স্মার্ট রাউটার)
echo "📝 MainActivity.java আপডেট করা হচ্ছে..."
cat << 'EOF' > app/src/main/java/com/sagar/scrollbattle/MainActivity.java
package com.sagar.scrollbattle;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;
    private String currentInstaUser = "";
    private LinearLayout leaderboardContainer;

    private class UserScore implements Comparable<UserScore> {
        String name, instaUser;
        int score;
        UserScore(String name, String instaUser, int score) {
            this.name = name; this.instaUser = instaUser; this.score = score;
        }
        @Override
        public int compareTo(UserScore other) {
            return Integer.compare(other.score, this.score);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        ImageView splashLogo = findViewById(R.id.splash_logo);
        TextView splashTitle = findViewById(R.id.splash_title);
        
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        fadeIn.setDuration(1200);
        if (splashLogo != null) splashLogo.startAnimation(fadeIn);
        if (splashTitle != null) splashTitle.startAnimation(fadeIn);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        new Handler().postDelayed(() -> {
            if (user == null) {
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
                return;
            }

            FirebaseMessaging.getInstance().subscribeToTopic("all_users");
            usersRef = FirebaseDatabase.getInstance().getReference("Users");
            checkPermissionsAndRoute(user);
        }, 2000); 
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && usersRef != null) {
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) || 
                !isAccessibilityServiceEnabled()) {
                startActivity(new Intent(MainActivity.this, PermissionActivity.class));
                finish();
            }
        }
    }

    private void checkPermissionsAndRoute(FirebaseUser user) {
        usersRef.child(user.getUid()).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (!task.getResult().hasChild("instaUser") || 
                    task.getResult().child("instaUser").getValue(String.class).isEmpty()) {
                    startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                    finish();
                    return;
                }

                if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) || 
                    !isAccessibilityServiceEnabled()) {
                    startActivity(new Intent(MainActivity.this, PermissionActivity.class));
                    finish();
                    return;
                }

                initMainUI(user);
            } else {
                initMainUI(user);
            }
        });
    }

    private void initMainUI(FirebaseUser user) {
        setContentView(R.layout.activity_main);
        leaderboardContainer = findViewById(R.id.leaderboard_container);

        usersRef.child(user.getUid()).child("instaUser").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) currentInstaUser = snapshot.getValue(String.class);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        findViewById(R.id.btn_menu).setOnClickListener(v -> showProfileMenu(user));
        findViewById(R.id.btn_start_battle).setOnClickListener(v -> {
            startService(new Intent(MainActivity.this, OverlayService.class));
            launchInstagram();
        });

        loadLeaderboard();
    }

    private boolean isAccessibilityServiceEnabled() {
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                return settingValue.contains(getPackageName() + "/" + ScrollAccessibilityService.class.getName());
            }
        }
        return false;
    }

    private void loadLeaderboard() {
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<UserScore> userList = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String fName = ds.child("firstName").getValue(String.class);
                    String lName = ds.child("lastName").getValue(String.class);
                    String insta = ds.child("instaUser").getValue(String.class);
                    Integer score = ds.child("score").getValue(Integer.class);
                    
                    if (fName != null && score != null) {
                        String fullName = fName + (lName != null ? " " + lName : "");
                        userList.add(new UserScore(fullName, insta, score));
                    }
                }
                
                Collections.sort(userList);
                
                if (leaderboardContainer != null) {
                    leaderboardContainer.removeAllViews();
                    LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
                    
                    for (int i = 0; i < userList.size(); i++) {
                        View rowView = inflater.inflate(R.layout.list_item_leaderboard, leaderboardContainer, false);
                        
                        TextView tvRank = rowView.findViewById(R.id.tv_rank);
                        TextView tvName = rowView.findViewById(R.id.tv_name);
                        TextView tvScore = rowView.findViewById(R.id.tv_score);
                        Button btnInsta = rowView.findViewById(R.id.btn_insta_link);
                        
                        UserScore us = userList.get(i);
                        tvRank.setText("#" + (i + 1));
                        tvName.setText(us.name);
                        tvScore.setText("Reels: " + us.score);
                        
                        if (i < 3 && us.instaUser != null && !us.instaUser.isEmpty()) {
                            btnInsta.setVisibility(View.VISIBLE);
                            btnInsta.setOnClickListener(v -> {
                                String url = "https://instagram.com/" + us.instaUser.replace("@", "");
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                startActivity(intent);
                            });
                        }
                        leaderboardContainer.addView(rowView);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showProfileMenu(FirebaseUser user) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_profile_menu);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView tvName = dialog.findViewById(R.id.tv_menu_name);
        TextView tvEmail = dialog.findViewById(R.id.tv_menu_email);
        EditText etInsta = dialog.findViewById(R.id.et_menu_insta);
        Button btnUpdate = dialog.findViewById(R.id.btn_menu_update);
        Button btnLogout = dialog.findViewById(R.id.btn_menu_logout);

        tvName.setText(user.getDisplayName() != null ? user.getDisplayName() : "User");
        tvEmail.setText(user.getEmail());
        etInsta.setText(currentInstaUser);

        btnUpdate.setOnClickListener(v -> {
            String newInsta = etInsta.getText().toString().trim();
            if (!newInsta.isEmpty()) {
                usersRef.child(user.getUid()).child("instaUser").setValue(newInsta);
                Toast.makeText(this, "Instagram Username Updated!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            dialog.dismiss();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        dialog.show();
    }

    private void launchInstagram() {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.instagram.android");
        if (launchIntent != null) {
            startActivity(launchIntent);
        } else {
            Toast.makeText(this, "Instagram is not installed!", Toast.LENGTH_SHORT).show();
        }
    }
}
EOF

# ২. ScrollAccessibilityService.java আপডেট (হোম স্ক্রিন ও প্রোফাইল স্ক্রিন ট্র্যাকিং লক)
echo "📝 ScrollAccessibilityService.java আপডেট করা হচ্ছে..."
cat << 'EOF' > app/src/main/java/com/sagar/scrollbattle/ScrollAccessibilityService.java
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
EOF

# ৩. activity_splash.xml আপডেট (রিসোর্স এরর ফিক্স করা হয়েছে ইউনিভার্সাল আইকন দিয়ে)
echo "📝 activity_splash.xml তৈরি করা হচ্ছে..."
cat << 'EOF' > app/src/main/res/layout/activity_splash.xml
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#FDFBF7">

    <LinearLayout
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:gravity="center"
        android:layout_centerInParent="true">

        <ImageView
            android:id="@+id/splash_logo"
            android:layout_width="120dp"
            android:layout_height="120dp"
            android:src="@android:drawable/sym_def_app_icon"
            android:contentDescription="App Logo" />

        <TextView
            android:id="@+id/splash_title"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Scroll Battle"
            android:textSize="32sp"
            android:textStyle="bold"
            android:textColor="#2C2C2C"
            android:layout_marginTop="20dp"
            android:fontFamily="sans-serif-medium"/>

        <ProgressBar
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:layout_marginTop="30dp"
            android:indeterminateTint="#2C2C2C" />
            
    </LinearLayout>
</RelativeLayout>
EOF

# ৪. গিটহাবে পুশ প্রসেস
echo "📤 লেটেস্ট কোড গিটহাবে পুশ করা হচ্ছে..."
git add .
git commit -m "Auto-fix applied: Perfect Reels lock, Splash Screen & Permission Re-lock"
git push

echo "--------------------------------------------------------"
echo "✅ অল ডান! সবকিছু নিখুঁতভাবে ফিক্সড এবং গিটহাবে পুশ হয়ে গেছে। গিটহাব অ্যাকশনস চেক করো।"
