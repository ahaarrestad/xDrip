package com.eveningoutpost.dexdrip.utilitymodels;

import static com.eveningoutpost.dexdrip.utilitymodels.NotificationChannels.GENERAL_CHANNEL;
import static com.eveningoutpost.dexdrip.utilitymodels.NotificationChannels.SENSOR_EXPIRY_CHANNEL;
import static com.google.common.truth.Truth.assertWithMessage;

import android.app.Notification;

import androidx.core.app.NotificationCompat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import lombok.val;

public class NotificationChannelsTest extends RobolectricTestWithConfig {
    private static final long[] pattern = {123, 456, 789};
    private static final String GENERAL_NAME = "General";
    private static final String SENSOR_EXPIRY_NAME = "Sensor expiry";

    @Test
    public void getNotificationFromInsideBuilderTest() {
        val builder = new NotificationCompat.Builder(RuntimeEnvironment.getApplication().getApplicationContext(), (String)null);
        builder.setVibrate(pattern);
        val mNotification = NotificationChannels.getNotificationFromInsideBuilder(builder);
        assertWithMessage("got builder by reflection 1").that(mNotification).isNotNull();
        assertWithMessage("got builder by reflection 2").that(mNotification.getClass()).isEqualTo(Notification.class);
        assertWithMessage("got builder by reflection 3").that(mNotification.vibrate).isEqualTo(pattern);
    }

    // display names: a channel missing from the name map falls back to showing its raw id

    @Test
    public void getStringReturnsDisplayNameForSensorExpiryChannel() {
        // :: Act
        val name = NotificationChannels.getString(SENSOR_EXPIRY_CHANNEL);

        // :: Verify
        assertWithMessage("sensor expiry channel display name").that(name).isEqualTo(SENSOR_EXPIRY_NAME);
    }

    @Test
    public void getStringReturnsDisplayNameForGeneralChannel() {
        // :: Act
        val name = NotificationChannels.getString(GENERAL_CHANNEL);

        // :: Verify
        assertWithMessage("general channel display name").that(name).isEqualTo(GENERAL_NAME);
    }

    // the channel a notification is actually posted to: getChan appends a hash of the
    // sound / vibration / lights to both the id and the name of the channel it creates

    @Test
    public void getChanNamesSensorExpiryChannelAfterItsDisplayName() {
        // :: Setup
        val builder = notificationOn(SENSOR_EXPIRY_CHANNEL);

        // :: Act
        val channel = NotificationChannels.getChan(builder);

        // :: Verify
        assertWithMessage("sensor expiry channel id").that(channel.getId()).startsWith(SENSOR_EXPIRY_CHANNEL);
        assertWithMessage("sensor expiry channel name").that(channel.getName().toString()).startsWith(SENSOR_EXPIRY_NAME);
    }

    @Test
    public void getChanNamesGeneralChannelAfterItsDisplayName() {
        // :: Setup
        val builder = notificationOn(GENERAL_CHANNEL);

        // :: Act
        val channel = NotificationChannels.getChan(builder);

        // :: Verify
        assertWithMessage("general channel id").that(channel.getId()).startsWith(GENERAL_CHANNEL);
        assertWithMessage("general channel name").that(channel.getName().toString()).startsWith(GENERAL_NAME);
    }

    /** Builds a notification the way {@code JoH.showNotification} does, with vibration and lights. */
    private NotificationCompat.Builder notificationOn(final String channelId) {
        val builder = new NotificationCompat.Builder(RuntimeEnvironment.getApplication().getApplicationContext(), channelId);
        builder.setContentTitle("title");
        builder.setContentText("content");
        builder.setVibrate(pattern);
        builder.setLights(0xff00ff00, 300, 1000);
        return builder;
    }
}
