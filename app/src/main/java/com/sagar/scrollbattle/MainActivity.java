package com.sagar.scrollbattle;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
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

public class MainActivity extends Activity {
    private FirebaseAuth mAuth;
    private DatabaseReference mRef;
    private String currentInstaUser = "";

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

        mRef = FirebaseDatabase.getInstance().getReference("Users").child(user.getUid());
        
        // ইনস্টাগ্রাম আইডি ব্যাকগ্রাউন্ডে লোড করে রাখা
        mRef.child("instaUser").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) currentInstaUser = snapshot.getValue(String.class);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // 3-Line Menu Button Clicks
        findViewById(R.id.btn_menu).setOnClickListener(v -> showProfileMenu(user));

        // Start Battle Button
        findViewById(R.id.btn_start_battle).setOnClickListener(v -> {
            startService(new Intent(MainActivity.this, OverlayService.class));
            launchInstagram();
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
                mRef.child("instaUser").setValue(newInsta);
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
