package com.sagar.scrollbattle;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnOverlay = findViewById(R.id.btn_overlay_permission);
        Button btnAccessibility = findViewById(R.id.btn_accessibility_permission);

        // Overlay Permission Button Logic
        btnOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.canDrawOverlays(MainActivity.this)) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    } else {
                        Toast.makeText(MainActivity.this, "Overlay Permission আগে থেকেই দেওয়া আছে!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // Accessibility Permission Button Logic
        btnAccessibility.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
                Toast.makeText(MainActivity.this, "Scroll Battle-এর জন্য Accessibility অন করুন", Toast.LENGTH_LONG).show();
            }
        });
    }
}
