package com.sagar.scrollbattle;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;
    private String currentInstaUser = "";
    private LinearLayout leaderboardContainer;

    // লিডারবোর্ডের জন্য একটি ছোট্ট ক্লাস
    private class UserScore implements Comparable<UserScore> {
        String name, instaUser;
        int score;
        UserScore(String name, String instaUser, int score) {
            this.name = name; this.instaUser = instaUser; this.score = score;
        }
        @Override
        public int compareTo(UserScore other) {
            return Integer.compare(other.score, this.score); // বেশি স্কোর ওপরে থাকবে
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        leaderboardContainer = findViewById(R.id.leaderboard_container);
        
        // নিজের ইন্সটাগ্রাম আইডি লোড করা
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

        // লিডারবোর্ড লোড করার ম্যাজিক লজিক
        loadLeaderboard();
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
                
                Collections.sort(userList); // স্কোর অনুযায়ী সাজানো (Top to Bottom)
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
                    
                    // প্রথম ৩ জনের জন্য ইন্সটাগ্রাম বাটন দৃশ্যমান হবে
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
