package com.eveningoutpost.dexdrip.utilitymodels;

import android.app.Notification;
import androidx.core.app.NotificationCompat;

import com.eveningoutpost.dexdrip.models.UserError;

/**
 * Created by jamorham on 18/10/2017.
 */

public class XdripNotificationCompat extends NotificationCompat {

    private final static String TAG = XdripNotificationCompat.class.getSimpleName();

    public static Notification build(NotificationCompat.Builder builder) {
        String id;
        try {
            id = NotificationChannels.getChan(builder).getId();
        } catch (Exception e) {
            // Fallback to the general channel if the guesser fails. It has to be created here:
            // from API 26 a notification posted to a channel that does not exist is silently dropped.
            NotificationChannels.setupGeneralChannel();
            id = NotificationChannels.GENERAL_CHANNEL;
        }
        builder.setChannelId(id);

        // Ensure alerts are independent and not summaries
        builder.setGroup(null);
        builder.setGroupSummary(false);

        builder.setCategory(NotificationCompat.CATEGORY_ALARM);

        final Notification n = builder.build();

        UserError.Log.d(TAG, "NotifCompat: chan=" + id +
                " group=" + NotificationCompat.getGroup(n) +
                " summary=" + ((n.flags & Notification.FLAG_GROUP_SUMMARY) != 0));

        return n;
    }
}
