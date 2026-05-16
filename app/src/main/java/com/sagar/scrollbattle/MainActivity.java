package com.sagar.scrollbattle;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LinearLayout mainLayout = findViewById(R.id.main_layout);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        mainLayout.startAnimation(slideUp);

        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        Button btnOverlay = findViewById(R.id.btn_overlay_permission);
        Button btnAccessibility = findViewById(R.id.btn_accessibility_permission);
        Button btnStart = findViewById(R.id.btn_start_app);

        btnOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.canDrawOverlays(MainActivity.this)) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    } else {
                        Toast.makeText(MainActivity.this, "Overlay Permission দেওয়াই আছে!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        btnAccessibility.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
                Toast.makeText(MainActivity.this, "Scroll Battle অন করুন", Toast.LENGTH_LONG).show();
            }
        });

        // App Start and Instagram Launch Logic
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(MainActivity.this)) {
                    Toast.makeText(MainActivity.this, "আগে Overlay Permission দিন!", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // ১. ওভারলে সার্ভিস স্টার্ট করা হচ্ছে এবং সরাসরি SHOW অ্যাকশন পাঠানো হচ্ছে
                Intent serviceIntent = new Intent(MainActivity.this, OverlayService.class);
                serviceIntent.putExtra("ACTION", "SHOW");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }

                // ২. সরাসরি ইনস্টাগ্রাম অ্যাপ ওপেন করার প্রফেশনাল লজিক
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.instagram.android");
                if (launchIntent != null) {
                    startActivity(launchIntent);
                    Toast.makeText(MainActivity.this, "Scroll Battle শুরু হয়েছে!", Toast.LENGTH_SHORT).show();
                } else {
                    // যদি ফোনে ইনস্টাগ্রাম ইন্সটল না থাকে
                    Toast.makeText(MainActivity.this, "ইনস্টাগ্রাম অ্যাপটি খুঁজে পাওয়া যায়নি!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}

