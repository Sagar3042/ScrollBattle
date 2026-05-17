package com.sagar.scrollbattle;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends Activity {

    private static final int OVERLAY_PERMISSION_REQ_CODE = 1234;
    private EditText etFirstName, etLastName, etInsta;
    private DatabaseReference mRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        
        // Jodi user login na thake, LoginActivity te pathiye dibe
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        mRef = FirebaseDatabase.getInstance().getReference("Users").child(user.getUid());

        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        etInsta = findViewById(R.id.et_insta);

        // Google theke auto Name niye asha
        String fullName = user.getDisplayName();
        if (fullName != null && !fullName.isEmpty()) {
            String[] parts = fullName.split(" ");
            if (parts.length > 0) etFirstName.setText(parts[0]);
            if (parts.length > 1) etLastName.setText(parts[1]);
        }

        // Save Profile Button
        findViewById(R.id.btn_save_profile).setOnClickListener(v -> {
            mRef.child("firstName").setValue(etFirstName.getText().toString());
            mRef.child("lastName").setValue(etLastName.getText().toString());
            mRef.child("instaUser").setValue(etInsta.getText().toString());
            Toast.makeText(this, "Profile Saved Successfully!", Toast.LENGTH_SHORT).show();
        });

        // Start Battle Button
        findViewById(R.id.btn_start_battle).setOnClickListener(v -> checkPermissionAndStart());

        // Share App Button
        findViewById(R.id.btn_share).setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Join Scroll Battle and track your Instagram Reels! Download now: https://your-app-link.com");
            startActivity(Intent.createChooser(shareIntent, "Share App via"));
        });

        // Logout Button
        findViewById(R.id.btn_logout).setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void checkPermissionAndStart() {
        // ১. Overlay Permission Check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please allow 'Display over other apps' permission", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
            return;
        }

        // ২. Accessibility Permission Check
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, "Enable Scroll Battle in Accessibility. (Allow Restricted Settings if disabled)", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            return;
        }

        // ৩. Sab permission thakle service chalabe aur Insta open korbe
        startService(new Intent(MainActivity.this, OverlayService.class));
        launchInstagram();
    }

    // Accessibility on ache ki na check korar master logic
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

    private void launchInstagram() {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.instagram.android");
        if (launchIntent != null) {
            startActivity(launchIntent);
        } else {
            Toast.makeText(this, "Instagram is not installed!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                checkPermissionAndStart(); // Overlay dile abar accessibility check korbe
            }
        }
    }
}
