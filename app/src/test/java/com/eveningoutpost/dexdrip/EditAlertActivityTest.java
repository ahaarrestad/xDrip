package com.eveningoutpost.dexdrip;

import android.content.Context;
import android.graphics.Paint;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests for {@link EditAlertActivity}
 *
 * @author Asbjørn Aarrestad - asbjorn@aarrestad.com - 2018.05
 */
@Config(shadows = {EditAlertActivityTest.ShadowRingtoneManager.class})
public class EditAlertActivityTest extends RobolectricTestWithConfig {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private Ringtone ringtone;

    // ===== isPathRingtone ========================================================================

    @Test
    public void isPathRingtone_True() {
        // :: Setup
        Context context = RuntimeEnvironment.application.getApplicationContext();
        ShadowRingtoneManager.addRingtone(Uri.parse("/ringtone"), ringtone);

        // :: Act
        boolean pathIsRingtone = EditAlertActivity.isPathRingtone(context, "/ringtone");

        // :: Verify
        assertThat(pathIsRingtone).isTrue();
    }

    @Test
    public void isPathRingtone_False() {
        // :: Setup
        Context context = RuntimeEnvironment.application.getApplicationContext();

        // :: Act
        boolean pathIsRingtone = EditAlertActivity.isPathRingtone(context, "/not_a_ringtone");

        // :: Verify
        assertThat(pathIsRingtone).isFalse();
    }

    @Test
    public void isPathRingtone_nullPath_False() {
        // :: Setup
        Context context = RuntimeEnvironment.application.getApplicationContext();

        // :: Act
        boolean pathIsRingtone = EditAlertActivity.isPathRingtone(context, null);

        // :: Verify
        assertThat(pathIsRingtone).isFalse();
    }

    @Test
    public void isPathRingtone_emptyPath_False() {
        // :: Setup
        Context context = RuntimeEnvironment.application.getApplicationContext();

        // :: Act
        boolean pathIsRingtone = EditAlertActivity.isPathRingtone(context, "");

        // :: Verify
        assertThat(pathIsRingtone).isFalse();
    }

    // ===== setDisabledView =======================================================================

    @Test
    public void setDisabledView_disabled_OK() {
        // :: Setup
        EditAlertActivity editAlertActivity = Robolectric.setupActivity(EditAlertActivity.class);
        CheckBox disabledCheckbox = editAlertActivity.findViewById(R.id.view_alert_check_disable);
        disabledCheckbox.setChecked(true);

        List<Integer> textViewIDs = Arrays.asList(R.id.view_alert_text,
                R.id.view_alert_threshold,
                R.id.view_alert_default_snooze,
                R.id.view_alert_mp3_file,
                R.id.view_alert_time_between,
                R.id.view_alert_disable,
                R.id.view_alert_time,
                R.id.view_alert_override_silent,
                R.id.view_alert_vibrate);
        for (Integer textViewId : textViewIDs) {
            TextView textView = editAlertActivity.findViewById(textViewId);
            textView.setPaintFlags(Paint.SUBPIXEL_TEXT_FLAG);
        }

        // :: Act
        editAlertActivity.setDisabledView();

        // :: Verify
        for (Integer textViewId : textViewIDs) {
            TextView textView = editAlertActivity.findViewById(textViewId);
            assertThat(textView.getPaintFlags()).isEqualTo(Paint.STRIKE_THRU_TEXT_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        }
    }

    @Test
    public void setDisabledView_enabled_OK() {
        // :: Setup
        EditAlertActivity editAlertActivity = Robolectric.setupActivity(EditAlertActivity.class);
        CheckBox disabledCheckbox = editAlertActivity.findViewById(R.id.view_alert_check_disable);
        disabledCheckbox.setChecked(false);

        List<Integer> textViewIDs = Arrays.asList(R.id.view_alert_text,
                R.id.view_alert_threshold,
                R.id.view_alert_default_snooze,
                R.id.view_alert_mp3_file,
                R.id.view_alert_time_between,
                R.id.view_alert_disable,
                R.id.view_alert_time,
                R.id.view_alert_override_silent,
                R.id.view_alert_vibrate);
        for (Integer textViewId : textViewIDs) {
            TextView textView = editAlertActivity.findViewById(textViewId);
            textView.setPaintFlags(Paint.STRIKE_THRU_TEXT_FLAG);
        }

        // :: Act
        editAlertActivity.setDisabledView();

        // :: Verify
        for (Integer textViewId : textViewIDs) {
            TextView textView = editAlertActivity.findViewById(textViewId);
            assertThat(textView.getPaintFlags()).isEqualTo(0);
        }
    }

    @Test
    public void setDisabledView_enabled_NoChange() {
        // :: Setup
        EditAlertActivity editAlertActivity = Robolectric.setupActivity(EditAlertActivity.class);
        CheckBox disabledCheckbox = editAlertActivity.findViewById(R.id.view_alert_check_disable);
        disabledCheckbox.setChecked(false);

        List<Integer> textViewIDs = Arrays.asList(R.id.view_alert_text,
                R.id.view_alert_threshold,
                R.id.view_alert_default_snooze,
                R.id.view_alert_mp3_file,
                R.id.view_alert_time_between,
                R.id.view_alert_disable,
                R.id.view_alert_time,
                R.id.view_alert_override_silent,
                R.id.view_alert_vibrate);
        for (Integer textViewId : textViewIDs) {
            TextView textView = editAlertActivity.findViewById(textViewId);
            textView.setPaintFlags(Paint.DEV_KERN_TEXT_FLAG);
        }

        // :: Act
        editAlertActivity.setDisabledView();

        // :: Verify
        for (Integer textViewId : textViewIDs) {
            TextView textView = editAlertActivity.findViewById(textViewId);
            assertThat(textView.getPaintFlags()).isEqualTo(Paint.DEV_KERN_TEXT_FLAG);
        }
    }

    // ===== testAlert ===========================================================================

    @Test
    public void overlapping_testAlert_OK() {
        // :: Setup
        EditAlertActivity editAlertActivity = Robolectric.setupActivity(EditAlertActivity.class);
        EditText alertThreshold = editAlertActivity.findViewById(R.id.edit_alert_threshold);
        alertThreshold.setText("150");

        // :: Act
        editAlertActivity.testAlert();

        // :: Verify
    }

    // ===== Shadows implementations ===============================================================

    @Implements(RingtoneManager.class)
    public static class ShadowRingtoneManager {

        private static Map<Uri, Ringtone> ringtones = new HashMap<>();

        public static void addRingtone(Uri ringtoneUri, Ringtone ringtone) {
            ringtones.put(ringtoneUri, ringtone);
        }

        @Implementation
        public static Ringtone getRingtone(Context context, Uri ringtoneUri) {
            return ringtones.get(ringtoneUri);
        }

    }
}
