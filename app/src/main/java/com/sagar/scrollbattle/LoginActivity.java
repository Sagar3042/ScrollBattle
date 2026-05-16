package com.sagar.scrollbattle;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends Activity {

    private FirebaseAuth mAuth;
    private EditText editEmail, editPassword;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        // অটো-লগিন চেক: ইউজার আগে থেকেই লগিন থাকলে সরাসরি মেইন পেজে নিয়ে যাবে
        if (mAuth.getCurrentUser() != null) {
            goToMainActivity();
            return;
        }

        editEmail = findViewById(R.id.edit_email);
        editPassword = findViewById(R.id.edit_password);
        Button btnLoginSignup = findViewById(R.id.btn_login_signup);
        Button btnGoogleLogin = findViewById(R.id.btn_google_login);

        // Google Sign-In Setup (google-services.json থেকে অটোমেটিক Client ID নিয়ে নেবে)
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Email/Password Login/Signup Logic
        btnLoginSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = editEmail.getText().toString().trim();
                String password = editPassword.getText().toString().trim();

                if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                    Toast.makeText(LoginActivity.this, "Email এবং Password দিন!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // প্রথমে লগিন করার চেষ্টা করবে
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(LoginActivity.this, task -> {
                            if (task.isSuccessful()) {
                                goToMainActivity();
                            } else {
                                // লগিন ফেইল হলে নতুন অ্যাকাউন্ট তৈরি করবে (Sign Up)
                                mAuth.createUserWithEmailAndPassword(email, password)
                                        .addOnCompleteListener(LoginActivity.this, task2 -> {
                                            if (task2.isSuccessful()) {
                                                goToMainActivity();
                                            } else {
                                                Toast.makeText(LoginActivity.this, "Error: " + task2.getException().getMessage(), Toast.LENGTH_LONG).show();
                                            }
                                        });
                            }
                        });
            }
        });

        // Google Login Button Click
        btnGoogleLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Google Sign-In Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        goToMainActivity();
                    } else {
                        Toast.makeText(LoginActivity.this, "Firebase Auth Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void goToMainActivity() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish(); // লগিন পেজ ক্লোজ করে দেওয়া হলো, যাতে ব্যাক করলে আবার এখানে না আসে
    }
}
