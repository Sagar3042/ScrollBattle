package com.sagar.scrollbattle;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FCMService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        Intent intent;
        // যদি নোটিফিকেশনে কোনো লিংক (URL) থাকে, তবে সেখানে ডাইরেক্ট করবে
        if (message.getData().containsKey("url")) {
            String url = message.getData().get("url");
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        } else {
            intent = new Intent(this, MainActivity.class);
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        String channelId = "ScrollBattle_Notifications";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(message.getNotification() != null ? message.getNotification().getTitle() : "Scroll Battle")
                .setContentText(message.getNotification() != null ? message.getNotification().getBody() : "New Update!")
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "App Updates", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }
        manager.notify(101, builder.build());
    }
}
