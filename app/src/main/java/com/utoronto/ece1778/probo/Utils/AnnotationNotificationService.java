package com.utoronto.ece1778.probo.Utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.utoronto.ece1778.probo.MainActivity;
import com.utoronto.ece1778.probo.R;
import com.utoronto.ece1778.probo.User.User;

import java.util.ArrayList;
import java.util.Random;

import androidx.annotation.RequiresApi;

public class AnnotationNotificationService extends FirebaseMessagingService {
    private NotificationManager notificationManager;
    private LocalBroadcastManager broadcastManager;
    private String ADMIN_CHANNEL_ID = "adminChannelId";

    private ArrayList<String> receivedIds = new ArrayList<>();

    @Override
    public void onCreate() {
        broadcastManager = LocalBroadcastManager.getInstance(this);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        User user = new User();
        String annotationId = remoteMessage.getData().get("annotationId");

        if (remoteMessage.getData().get("annotationUser").equals(user.getUid()) ||
            receivedIds.contains(annotationId)) {

            return;
        }

        receivedIds.add(annotationId);

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            setupChannels();
        }

        int notificationId = new Random().nextInt(60000);

        String[] dataKeys = {
                "annotationId",
                "articleId",
                "annotationType",
                "annotationUser",
                "annotationStartIndex",
                "annotationEndIndex"
        };

        final Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        for (String dataKey : dataKeys) {
            intent.putExtra(dataKey, remoteMessage.getData().get(dataKey));
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT
        );

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, ADMIN_CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(remoteMessage.getNotification().getTitle())
                .setContentText(remoteMessage.getNotification().getBody())
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        notificationManager.notify(notificationId, notificationBuilder.build());
        sendToActivity(remoteMessage);
    }

    private void sendToActivity(RemoteMessage remoteMessage) {
        Intent intent = new Intent("notification");
        intent.putExtra("annotationId", remoteMessage.getData().get("annotationId"));
        broadcastManager.sendBroadcast(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setupChannels() {
        CharSequence adminChannelName = "Probo Channel";
        String adminChannelDescription = "Probo channel description";

        NotificationChannel adminChannel;
        adminChannel = new NotificationChannel(
                ADMIN_CHANNEL_ID,
                adminChannelName,
                NotificationManager.IMPORTANCE_LOW
        );
        adminChannel.setDescription(adminChannelDescription);
        adminChannel.enableLights(true);
        adminChannel.setLightColor(Color.RED);
        adminChannel.enableVibration(true);

        if (notificationManager != null) {
            notificationManager.createNotificationChannel(adminChannel);
        }
    }
}
