package io.renderapps.balizinha.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;

import io.renderapps.balizinha.R;


/*
 * Creates a notification channel that all notifications will be assigned to
 * starting Android 8.0 (API level 26)+
 *
 */
public class NotificationUtils {

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void createNotificationChannel(Context context) {
        if (context == null) return;

        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        NotificationChannel notificationChannel = new NotificationChannel(context.getString(R.string.notification_channel_id),
                context.getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_HIGH);

        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build();

        // Configure the notification channel.
//        notificationChannel.setDescription("");
        notificationChannel.enableLights(true);
        notificationChannel.enableVibration(true);
        notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        notificationChannel.setLightColor(Color.parseColor("#577965"));
        notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
        notificationChannel.setSound(sound, attributes);
        notificationManager.createNotificationChannel(notificationChannel);
    }
}