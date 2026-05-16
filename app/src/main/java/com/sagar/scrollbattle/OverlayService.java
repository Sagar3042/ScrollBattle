package com.sagar.scrollbattle;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class OverlayService extends Service {

    private WindowManager windowManager;
    private View floatingView;
    private TextView textScrollCount;
    private boolean isOverlayAdded = false;

    // ১০ সেকেন্ড পর অটো-হাইড করার জন্য টাইমার এবং হ্যান্ডলার
    private final Handler hideHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideRunnable = new Runnable() {
        @Override
        public void run() {
            if (floatingView != null) {
                floatingView.setVisibility(View.GONE);
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Foreground Service Notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "ScrollBattleChannel", "Scroll Battle", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
            
            Notification notification = new Notification.Builder(this, "ScrollBattleChannel")
                    .setContentTitle("Scroll Battle Running")
                    .setContentText("Ready to track reels...")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .build();
            startForeground(1, notification);
        }

        // Floating View Setup
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_layout, null);
        textScrollCount = floatingView.findViewById(R.id.text_scroll_count);

        // শুরুতে ওভারলে পুরোপুরি লুকিয়ে থাকবে
        floatingView.setVisibility(View.GONE);

        int layoutFlag = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ? 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.y = 150; // স্ক্রিনের সামান্য নিচে যাতে নোটিফিকেশন বারে ধাক্কা না খায়

        windowManager.addView(floatingView, params);
        isOverlayAdded = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("ACTION")) {
            String action = intent.getStringExtra("ACTION");
            
            if (action.equals("UPDATE_COUNT")) {
                int count = intent.getIntExtra("SCROLL_COUNT", 0);
                if (textScrollCount != null) {
                    textScrollCount.setText("Reels Scrolled: " + count);
                }

                // ১. স্ক্রল করার সাথে সাথে ওভারলে শো করো
                if (floatingView != null) {
                    floatingView.setVisibility(View.VISIBLE);
                }

                // ২. আগের কোনো রানিং টাইমার থাকলে তা বাতিল (Reset) করো
                hideHandler.removeCallbacks(hideRunnable);

                // ৩. নতুন করে ঠিক ১০ সেকেন্ড (10,000 মিলিসেকেন্ড) পর হাইড করার টাইমার সেট করো
                hideHandler.postDelayed(hideRunnable, 10000);
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        hideHandler.removeCallbacks(hideRunnable);
        if (floatingView != null && isOverlayAdded) {
            windowManager.removeView(floatingView);
        }
    }
}

