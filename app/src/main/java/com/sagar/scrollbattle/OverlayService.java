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
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class OverlayService extends Service {
    private WindowManager windowManager;
    private LinearLayout floatingView;
    private TextView tvCount;
    private WindowManager.LayoutParams params;
    private int currentCount = 0;

    // ব্রডকাস্ট রিসিভার (রিলস স্ক্রল হলে কাউন্ট রিসিভ করবে)
    private final BroadcastReceiver countReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("UPDATE_COUNT")) {
                currentCount++;
                tvCount.setText("Reels: " + currentCount);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // ফ্লোটিং ওভারলে-র প্রফেশনাল ডিজাইন (Glassmorphism)
        floatingView = new LinearLayout(this);
        floatingView.setOrientation(LinearLayout.VERTICAL);
        floatingView.setBackgroundResource(R.drawable.glass_card);
        floatingView.setPadding(40, 20, 40, 20);

        tvCount = new TextView(this);
        tvCount.setText("Reels: 0");
        tvCount.setTextColor(Color.WHITE);
        tvCount.setTextSize(18);
        tvCount.setTypeface(null, Typeface.BOLD);
        tvCount.setGravity(Gravity.CENTER);
        floatingView.addView(tvCount);

        // ওভারলে স্ক্রিনে সেট করার নিয়ম
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

        // ড্র্যাগ (টেনে সরানোর) এবং লাভ (💖) অ্যানিমেশন লজিক
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
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        
                        // সরানোর সময় লাভ ইমোজি দেখাবে
                        tvCount.setText("Reels: " + currentCount + " 💖");
                        return true;
                    case MotionEvent.ACTION_UP:
                        // ছেড়ে দিলে আবার নরমাল হয়ে যাবে
                        tvCount.setText("Reels: " + currentCount);
                        return true;
                }
                return false;
            }
        });

        // রিসিভার চালু করা
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(countReceiver, new IntentFilter("UPDATE_COUNT"), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(countReceiver, new IntentFilter("UPDATE_COUNT"));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) windowManager.removeView(floatingView);
        unregisterReceiver(countReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}

