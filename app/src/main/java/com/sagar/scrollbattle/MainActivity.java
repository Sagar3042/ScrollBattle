package com.sagar.scrollbattle;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends Activity {

    private LinearLayout layoutPermissions;
    private ListView listLeaderboard;
    private ArrayList<String> leaderboardData;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        layoutPermissions = findViewById(R.id.layout_permissions);
        listLeaderboard = findViewById(R.id.list_leaderboard);
        Button btnOverlay = findViewById(R.id.btn_overlay_permission);
        Button btnAccessibility = findViewById(R.id.btn_accessibility_permission);
        Button btnStart = findViewById(R.id.btn_start_app);

        // লিডারবোর্ড লিস্ট সেটআপ
        leaderboardData = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, leaderboardData);
        listLeaderboard.setAdapter(adapter);

        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
        }

        btnOverlay.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        });

        btnAccessibility.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });

        btnStart.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(MainActivity.this)) {
                Toast.makeText(MainActivity.this, "আগে Overlay Permission দিন!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.instagram.android");
            if (launchIntent != null) {
                startActivity(launchIntent);
                Toast.makeText(MainActivity.this, "Scroll Battle শুরু হয়েছে!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Instagram অ্যাপটি খুঁজে পাওয়া যায়নি!", Toast.LENGTH_LONG).show();
            }
        });

        // ফায়ারবেস থেকে রিয়েল-টাইম লিডারবোর্ড লোড করা
        loadLeaderboard();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions(); // পেজে এলেই চেক করবে পারমিশন দেওয়া আছে কি না
    }

    // স্মার্ট পারমিশন হাইডিং লজিক
    private void checkPermissions() {
        boolean hasOverlay = (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) || Settings.canDrawOverlays(this);
        
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        
        boolean hasAccessibility = false;
        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null && settingValue.contains(getPackageName())) {
                hasAccessibility = true;
            }
        }

        // দুটো পারমিশন দেওয়া থাকলে বক্স হাইড করে দাও!
        if (hasOverlay && hasAccessibility) {
            layoutPermissions.setVisibility(View.GONE);
        } else {
            layoutPermissions.setVisibility(View.VISIBLE);
        }
    }

    private void loadLeaderboard() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Leaderboard");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                leaderboardData.clear();
                ArrayList<UserScore> tempScores = new ArrayList<>();
                
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String email = snapshot.child("email").getValue(String.class);
                    Integer score = snapshot.child("score").getValue(Integer.class);
                    if (email != null && score != null) {
                        tempScores.add(new UserScore(email, score));
                    }
                }

                // সর্বোচ্চ স্কোর অনুযায়ী (Descending Order) সাজানো
                Collections.sort(tempScores, (a, b) -> b.score.compareTo(a.score));

                int rank = 1;
                for (UserScore user : tempScores) {
                    String displayName = user.email.split("@")[0]; // ইমেইলের নামের অংশটুকু দেখাবে
                    leaderboardData.add("🏆 #" + rank + "  |  " + displayName + "  =>  " + user.score + " Reels");
                    rank++;
                }
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    // ডেটা সাজানোর জন্য একটি ছোট্ট হেল্পার ক্লাস
    class UserScore {
        String email;
        Integer score;
        UserScore(String email, Integer score) {
            this.email = email;
            this.score = score;
        }
    }
}

