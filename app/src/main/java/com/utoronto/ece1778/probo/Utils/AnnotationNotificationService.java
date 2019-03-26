package com.utoronto.ece1778.probo.Utils;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class AnnotationNotificationService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d("PROBO_APP", "From: " + remoteMessage.getFrom());

        if (remoteMessage.getData().size() > 0) {
            Log.d("PROBO_APP", "Payload: " + remoteMessage.getData());
        }

        if (remoteMessage.getNotification() != null) {
            Log.d("PROBO_APP", "Notification: " + remoteMessage.getNotification().getBody());
        }
    }
}
