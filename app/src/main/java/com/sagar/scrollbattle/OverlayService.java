package com.sagar.scrollbattle;

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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class OverlayService extends Service {
    private WindowManager windowManager;
    private LinearLayout floatingView;
    private TextView tvCount;
    private WindowManager.LayoutParams params;
    private int currentCount = 0;
    
    // টাইমার কন্ট্রোল করার জন্য Handler
    private Handler handler = new Handler();

    // ওভারলে লুকিয়ে ফেলার ম্যাজিক লজিক
    private Runnable hideOverlayRunnable = new Runnable() {
        @Override
        public void run() {
            if (floatingView != null) {
                floatingView.setVisibility(View.GONE); // অদৃশ্য করে দেওয়া
            }
        }
    };

    private final BroadcastReceiver countReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("UPDATE_COUNT")) {
                currentCount++;
                tvCount.setText("Reels: " + currentCount);
                
                // রিলস স্ক্রল হলে ওভারলে আবার দৃশ্যমান হবে
                floatingView.setVisibility(View.VISIBLE);
                
                // আগের টাইমার বাতিল করে নতুন ৭ সেকেন্ডের টাইমার চালু করা
                handler.removeCallbacks(hideOverlayRunnable);
                handler.postDelayed(hideOverlayRunnable, 7000);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        floatingView = new LinearLayout(this);
        floatingView.setOrientation(LinearLayout.HORIZONTAL); // পাশাপাশি রাখার জন্য
        floatingView.setBackgroundResource(R.drawable.glass_card);
        floatingView.setPadding(30, 20, 30, 20);
        floatingView.setGravity(Gravity.CENTER);

        tvCount = new TextView(this);
        tvCount.setText("Reels: 0");
        tvCount.setTextColor(Color.WHITE);
        tvCount.setTextSize(18);
        tvCount.setTypeface(null, Typeface.BOLD);
        
        // কাটার (Close) বাটন তৈরি
        Button btnClose = new Button(this);
        btnClose.setText("X");
        btnClose.setTextColor(Color.parseColor("#FF3B30")); // লাল রঙের 'X'
        btnClose.setBackgroundColor(Color.TRANSPARENT);
        btnClose.setTextSize(18);
        btnClose.setPadding(30, 0, 0, 0);
        btnClose.setOnClickListener(v -> stopSelf()); // ওভারলে পুরোপুরি বন্ধ করা

        floatingView.addView(tvCount);
        floatingView.addView(btnClose);

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 20;
        params.y = 300;

        windowManager.addView(floatingView, params);

        // ড্র্যাগ (টেনে সরানোর) লজিক
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        // টাচ করলে টাইমার রিস্টার্ট হবে যাতে ড্র্যাগ করার সময় লুকিয়ে না যায়
                        handler.removeCallbacks(hideOverlayRunnable);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        // ছেড়ে দিলে আবার ৭ সেকেন্ডের টাইমার চালু হবে
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

        // শুরুতে ২ সেকেন্ড পর ওভারলে অদৃশ্য হয়ে যাবে
        handler.postDelayed(hideOverlayRunnable, 2000);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) windowManager.removeView(floatingView);
        unregisterReceiver(countReceiver);
        handler.removeCallbacks(hideOverlayRunnable);
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
