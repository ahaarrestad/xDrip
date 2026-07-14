package com.eveningoutpost.dexdrip.utilitymodels;

import static com.eveningoutpost.dexdrip.utilitymodels.NotificationChannels.GENERAL_CHANNEL;
import static com.eveningoutpost.dexdrip.utilitymodels.NotificationChannels.SENSOR_EXPIRY_CHANNEL;
import static com.google.common.truth.Truth.assertWithMessage;

import android.app.NotificationManager;
import android.content.Context;

import androidx.core.app.NotificationCompat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import lombok.val;

/**
 * Tests for {@link XdripNotificationCompat}.
 * <p>
 * From API 26 a notification posted to a channel that does not exist is silently dropped, so every
 * notification {@link XdripNotificationCompat#build} hands back must carry a channel that has
 * actually been created.
 *
 * @author Asbjørn Aarrestad - 2026.07
 */
public class XdripNotificationCompatTest extends RobolectricTestWithConfig {

    private static final long[] pattern = {123, 456, 789};

    @Test
    public void buildUsesTheChannelDerivedFromTheBuilder() {
        // :: Setup
        val builder = builderOn(SENSOR_EXPIRY_CHANNEL);

        // :: Act
        val notification = XdripNotificationCompat.build(builder);

        // :: Verify
        assertWithMessage("channel derived from the builder")
                .that(notification.getChannelId()).startsWith(SENSOR_EXPIRY_CHANNEL);
        assertChannelExists(notification.getChannelId());
    }

    // fallback: the builder carries no channel, so getChan() cannot derive one

    @Test
    public void buildFallsBackToTheGeneralChannelWhenTheBuilderHasNoChannel() {
        // :: Setup
        val builder = builderOn(null);

        // :: Act
        val notification = XdripNotificationCompat.build(builder);

        // :: Verify
        assertWithMessage("fallback channel")
                .that(notification.getChannelId()).isEqualTo(GENERAL_CHANNEL);
    }

    @Test
    public void buildCreatesTheFallbackChannelItPostsTo() {
        // :: Setup
        val builder = builderOn(null);

        // :: Act
        val notification = XdripNotificationCompat.build(builder);

        // :: Verify
        assertChannelExists(notification.getChannelId());
    }

    private NotificationCompat.Builder builderOn(final String channelId) {
        val builder = new NotificationCompat.Builder(RuntimeEnvironment.getApplication().getApplicationContext(), channelId);
        builder.setContentTitle("title");
        builder.setContentText("content");
        builder.setVibrate(pattern);
        return builder;
    }

    private void assertChannelExists(final String channelId) {
        val manager = (NotificationManager) RuntimeEnvironment.getApplication()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        assertWithMessage("channel '" + channelId + "' exists, otherwise the notification is dropped")
                .that(manager.getNotificationChannel(channelId)).isNotNull();
    }
}
