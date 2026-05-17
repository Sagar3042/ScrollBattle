package com.sagar.scrollbattle;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ProfileActivity extends Activity {
    private EditText etFirstName, etLastName, etInsta;
    private DatabaseReference mRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        mRef = FirebaseDatabase.getInstance().getReference("Users").child(user.getUid());

        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        etInsta = findViewById(R.id.et_insta);
        Button btnContinue = findViewById(R.id.btn_continue);

        // Google থেকে ফার্স্ট ও লাস্ট নেম অটো-ফিল করা
        String fullName = user.getDisplayName();
        if (fullName != null && !fullName.isEmpty()) {
            String[] parts = fullName.split(" ");
            if (parts.length > 0) etFirstName.setText(parts[0]);
            if (parts.length > 1) etLastName.setText(parts[1]);
        }

        btnContinue.setOnClickListener(v -> {
            String fName = etFirstName.getText().toString().trim();
            String lName = etLastName.getText().toString().trim();
            String insta = etInsta.getText().toString().trim();

            if (fName.isEmpty() || insta.isEmpty()) {
                Toast.makeText(this, "Please enter First Name and Instagram ID", Toast.LENGTH_SHORT).show();
                return;
            }

            // ডেটা সেভ করে Permission পেজে পাঠানো
            mRef.child("firstName").setValue(fName);
            mRef.child("lastName").setValue(lName);
            mRef.child("instaUser").setValue(insta);

            startActivity(new Intent(ProfileActivity.this, PermissionActivity.class));
            finish();
        });
    }
}
