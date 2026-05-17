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
        
        // প্রথমে প্রফেশনাল লোডিং/স্প্ল্যাশ স্ক্রিন লেআউট সেট করা হচ্ছে
        setContentView(R.layout.activity_splash);
        
        // লোডিং স্ক্রিনের লোগো এবং টেক্সটে সুন্দর Fade-In এবং Bounce অ্যানিমেশন
        ImageView splashLogo = findViewById(R.id.splash_logo);
        TextView splashTitle = findViewById(R.id.splash_title);
        
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        fadeIn.setDuration(1200);
        if (splashLogo != null) splashLogo.startAnimation(fadeIn);
        if (splashTitle != null) splashTitle.startAnimation(fadeIn);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        // ২ সেকেন্ডের সুন্দর অ্যানিমেশন হোল্ড করার পর পারমিশন ও রাউটিং চেক হবে
        new Handler().postDelayed(() -> {
            if (user == null) {
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
                return;
            }

            FirebaseMessaging.getInstance().subscribeToTopic("all_users");
            usersRef = FirebaseDatabase.getInstance().getReference("Users");

            // কড়া সিকিউরিটি এবং পারমিশন রিলক লজিক
            checkPermissionsAndRoute(user);
        }, 2000); 
    }

    // প্রতিবার অ্যাপে ঢোকার সময় বা অ্যাপ মিনিমাইজ থেকে ওপেন হওয়ার সময় পারমিশন চেক করবে
    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && usersRef != null) {
            // যদি ইউজার মেইন স্ক্রিনে থাকে এবং হঠাৎ পারমিশন বন্ধ করে দেয়, তবে আবার লক স্ক্রিনে পাঠাবে
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

                // যদি কোনো পারমিশন বন্ধ থাকে, তবে মেইন অ্যাপ খুলবেই না, ডাইরেক্ট পারমিশন পেজে লক হবে
                if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) || 
                    !isAccessibilityServiceEnabled()) {
                    startActivity(new Intent(MainActivity.this, PermissionActivity.class));
                    finish();
                    return;
                }

                // সব ঠিক থাকলে মেইন ইউজার ইন্টারফেস লোড হবে
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

