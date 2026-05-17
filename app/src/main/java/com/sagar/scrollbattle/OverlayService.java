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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class OverlayService extends Service {
    private WindowManager windowManager;
    private LinearLayout floatingView;
    private TextView tvCount;
    private WindowManager.LayoutParams params;
    private int currentCount = 0;
    private Handler handler = new Handler();
    private DatabaseReference mRef;

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
                
                // ফায়ারবেসে স্কোর আপডেট করা
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
            // আগের স্কোর ফায়ারবেস থেকে নিয়ে আসা
            mRef.child("score").get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().exists()) {
                    currentCount = task.getResult().getValue(Integer.class);
                    if(tvCount != null) tvCount.setText("Reels: " + currentCount);
                }
            });
        }

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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) windowManager.removeView(floatingView);
        unregisterReceiver(countReceiver);
        handler.removeCallbacks(hideOverlayRunnable);
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
