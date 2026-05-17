package com.sagar.scrollbattle;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

public class OverlayService extends Service {
    private WindowManager windowManager;
    private ImageView floatingIcon;
    private WindowManager.LayoutParams params;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        floatingIcon = new ImageView(this);
        // কিউট একটি ডিফল্ট আইকন সেট করা হলো
        floatingIcon.setImageResource(android.R.drawable.ic_menu_camera); 
        floatingIcon.setBackgroundResource(android.R.drawable.btn_default); 

        params = new WindowManager.LayoutParams(
                150, 150, // ওভারলে সাইজ (ছোট এবং সুন্দর)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 200;

        // ড্র্যাগ (টেনে সরানোর) লজিক এবং লাভ অ্যানিমেশন
        floatingIcon.setOnTouchListener(new View.OnTouchListener() {
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
                        windowManager.updateViewLayout(floatingIcon, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        // ক্লিক করলে কিউট লাভ মেসেজ
                        Toast.makeText(OverlayService.this, "💖 +1 Reels Scrolled!", Toast.LENGTH_SHORT).show();
                        return true;
                }
                return false;
            }
        });
        windowManager.addView(floatingIcon, params);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingIcon != null) windowManager.removeView(floatingIcon);
    }
    
    @Override
    public IBinder onBind(Intent intent) { return null; }
}

