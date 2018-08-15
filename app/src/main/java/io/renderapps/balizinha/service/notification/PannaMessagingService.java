package io.renderapps.balizinha.service.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import io.renderapps.balizinha.R;
import io.renderapps.balizinha.ui.splash.SplashActivity;
import io.renderapps.balizinha.util.NotificationUtils;

import static io.renderapps.balizinha.util.Constants.REF_PLAYERS;

public class PannaMessagingService extends FirebaseMessagingService {

    private static final String TAG = "PannaMessagingService";


    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
//        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
//        if (remoteMessage.getData().size() > 0) {
//            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
//        }


        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            sendNotification(this, remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody());
        }
    }


    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    public static void sendNotification(Context context, String title, String messageBody) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Build.VERSION_CODES.O
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationUtils.createNotificationChannel(context);
        }

        Intent intent = new Intent(context, SplashActivity.class);
//        intent.putExtra("eventId", eventId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0 /* Request code */, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, context.getString(R.string.notification_channel_id))
                .setSmallIcon(R.drawable.ic_stat_pi)
                .setColor(Color.parseColor("#577965"))  // small icon background color
//                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_logo))
                .setContentTitle(title)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setPriority(Notification.PRIORITY_MAX)
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setContentText(messageBody);

        if (notificationManager != null) {
            notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
        }
    }


    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    @Override
    public void onNewToken(String token) {
        sendRegistrationToServer(token);
    }


    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {

        final String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || uid.isEmpty()) return;

        FirebaseDatabase.getInstance().getReference().child(REF_PLAYERS).child(uid)
                .child("fcmToken").setValue(token);
    }
}
