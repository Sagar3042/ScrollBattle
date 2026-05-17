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
import android.widget.LinearLayout;
import android.widget.TextView;

public class OverlayService extends Service {
    private WindowManager windowManager;
    private LinearLayout floatingView;
    private TextView tvCount;
    private WindowManager.LayoutParams params;
    private int currentCount = 0;
    private Handler handler = new Handler();

    // Overlay lukiye felyar automatic runnable
    private Runnable hideOverlayRunnable = new Runnable() {
        @Override
        public void run() {
            if (floatingView != null) {
                floatingView.setVisibility(View.GONE);
            }
        }
    };

    private final BroadcastReceiver countReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("UPDATE_COUNT")) {
                currentCount++;
                tvCount.setText("Reels: " + currentCount);
                
                // Reels scroll hole overlay drishoman hobe
                floatingView.setVisibility(View.VISIBLE);
                
                // 7 second por abar automatic hide hoye jabe
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
        floatingView.setOrientation(LinearLayout.VERTICAL);
        floatingView.setBackgroundResource(R.drawable.glass_card); // Glassmorphism look
        floatingView.setPadding(45, 25, 45, 25);
        floatingView.setGravity(Gravity.CENTER);

        tvCount = new TextView(this);
        tvCount.setText("Reels: 0");
        tvCount.setTextColor(Color.WHITE);
        tvCount.setTextSize(18);
        tvCount.setTypeface(null, Typeface.BOLD);
        floatingView.addView(tvCount);

        // Screen er opor majhkhane (Top-Center) thakar layout params
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.x = 0;
        params.y = 150; // Status bar theke ektu niche namano thakbe

        windowManager.addView(floatingView, params);

        // User jate soriye (drag kore) jekono khane rakhte pare tar logic
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
                        handler.removeCallbacks(hideOverlayRunnable); // Drag korar somoy jano hide na hoy
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        handler.postDelayed(hideOverlayRunnable, 7000); // Chere dile 7 second por hide hobe
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

        // Prothome app chalu hole 2 second por automatic hide hoye jabe
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
