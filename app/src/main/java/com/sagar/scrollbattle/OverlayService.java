package com.sagar.scrollbattle;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class OverlayService extends Service {
    private WindowManager windowManager;
    private LinearLayout floatingView;
    private TextView tvCount;
    private WindowManager.LayoutParams params;
    private int currentCount = 0;
    private Handler handler = new Handler();
    private DatabaseReference mRef;
    private DatabaseReference notiRef;

    private Runnable hideOverlayRunnable = new Runnable() {
        @Override
        public void run() {
            if (floatingView != null) floatingView.setVisibility(View.GONE);
        }
    };

    private final BroadcastReceiver countReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("UPDATE_COUNT")) {
                currentCount++;
                tvCount.setText("Reels: " + currentCount);
                floatingView.setVisibility(View.VISIBLE);
                
                if (mRef != null) {
                    mRef.child("score").setValue(currentCount);
                }
                
                handler.removeCallbacks(hideOverlayRunnable);
                handler.postDelayed(hideOverlayRunnable, 7000);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            mRef = FirebaseDatabase.getInstance().getReference("Users").child(user.getUid());
            mRef.child("score").get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().exists()) {
                    currentCount = task.getResult().getValue(Integer.class);
                    if(tvCount != null) tvCount.setText("Reels: " + currentCount);
                }
            });
        }

        // ================== নোটিফিকেশন লজিক শুরু ==================
        createNotificationChannel();
        notiRef = FirebaseDatabase.getInstance().getReference("GlobalNotification");
        notiRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String msg = snapshot.child("message").getValue(String.class);
                    Long time = snapshot.child("timestamp").getValue(Long.class);
                    
                    // মেসেজ যদি গত ১৫ সেকেন্ডের মধ্যে পাঠানো হয়, তবেই নোটিফিকেশন দেখাবে
                    if (msg != null && time != null && (System.currentTimeMillis() - time < 15000)) {
                        showSystemNotification(msg);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
        // ================== নোটিফিকেশন লজিক শেষ ==================

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatingView = new LinearLayout(this);
        floatingView.setOrientation(LinearLayout.VERTICAL);
        floatingView.setBackgroundResource(R.drawable.glass_card); 
        floatingView.setPadding(45, 25, 45, 25);
        floatingView.setGravity(Gravity.CENTER);

        tvCount = new TextView(this);
        tvCount.setText("Reels: 0");
        tvCount.setTextColor(Color.WHITE);
        tvCount.setTextSize(18);
        tvCount.setTypeface(null, Typeface.BOLD);
        floatingView.addView(tvCount);

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.x = 0;
        params.y = 150; 

        windowManager.addView(floatingView, params);

        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x; initialY = params.y;
                        initialTouchX = event.getRawX(); initialTouchY = event.getRawY();
                        handler.removeCallbacks(hideOverlayRunnable); 
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        handler.postDelayed(hideOverlayRunnable, 7000); 
                        return true;
                }
                return false;
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(countReceiver, new IntentFilter("UPDATE_COUNT"), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(countReceiver, new IntentFilter("UPDATE_COUNT"));
        }
        handler.postDelayed(hideOverlayRunnable, 2000);
    }

    // নোটিফিকেশন চ্যানেল তৈরি করা (Android 8+ এর জন্য জরুরি)
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("admin_channel", "Admin Alerts", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    // আসল সিস্টেম নোটিফিকেশন তৈরি করা
    private void showSystemNotification(String message) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        // নোটিফিকেশনে ক্লিক করলে অ্যাপ ওপেন হবে
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, "admin_channel");
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setContentTitle("📢 Admin Announcement")
               .setContentText(message)
               .setSmallIcon(android.R.drawable.ic_dialog_info) // নোটিফিকেশন আইকন
               .setAutoCancel(true)
               .setContentIntent(pendingIntent);

        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) windowManager.removeView(floatingView);
        unregisterReceiver(countReceiver);
        handler.removeCallbacks(hideOverlayRunnable);
    }
    
    @Override public IBinder onBind(Intent intent) { return null; }
}
