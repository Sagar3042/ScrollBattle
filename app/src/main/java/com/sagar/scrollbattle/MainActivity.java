package com.sagar.scrollbattle;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int OVERLAY_PERMISSION_REQ_CODE = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // তোমার মেইন লেআউট

        Button btnStartBattle = findViewById(R.id.btn_start_battle); // তোমার বাটনের আইডি

        btnStartBattle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissionAndStart();
            }
        });
    }

    private void checkPermissionAndStart() {
        // ১. ওভারলে পারমিশন চেক করা
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please allow 'Display over other apps' permission", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
        } else {
            // ২. পারমিশন থাকলে Overlay Service চালু করা
            startService(new Intent(MainActivity.this, OverlayService.class));
            
            // ৩. Instagram ওপেন করা
            launchInstagram();
        }
    }

    private void launchInstagram() {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.instagram.android");
        if (launchIntent != null) {
            startActivity(launchIntent);
        } else {
            Toast.makeText(MainActivity.this, "Instagram is not installed!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                // পারমিশন দেওয়ার পর ফিরে আসলে সার্ভিস ও ইনস্টাগ্রাম চালু হবে
                startService(new Intent(this, OverlayService.class));
                launchInstagram();
            } else {
                Toast.makeText(this, "Permission denied. Overlay will not work.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

