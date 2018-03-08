package com.eveningoutpost.dexdrip.Models;

import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.UUID;

import static com.google.common.truth.Truth.assertThat;

/**
 * Various tests for {@link Calibration} class
 *
 * @author asbjorn aarrestad - asbjorn@aarrestad.com on 01.03.2018.
 */
@RunWith(RobolectricTestRunner.class)
//@Config(constants = BuildConfig.class, manifest = "../../../../app/src/test/java/com/eveningoutpost/dexdrip/TestingManifest.xml") // use this config inside android studio 3 or set Android JUnit default working directory to $MODULE_DIR$
@Config(constants = BuildConfig.class, manifest = "../../../../../src/test/java/com/eveningoutpost/dexdrip/TestingManifest.xml")
// use this config for CI test hosts
public class CalibrationTest {

    @Before
    public void setUp() throws Exception {
        xdrip.checkAppContext(RuntimeEnvironment.application);
    }

    @Test
    public void initialCalibration_raisingBg_OK() {
        // :: Setup
        // Add mock sensor
        Sensor mockSensor = new Sensor();
        mockSensor.started_at = System.currentTimeMillis() - (1000 * 60 * 20);
        mockSensor.uuid = UUID.randomUUID().toString();
        mockSensor.save();

        // Add mock bg readings
        addMockBgReading(125, 11, mockSensor);
        addMockBgReading(130, 6, mockSensor);
        addMockBgReading(135, 1, mockSensor);

        // :: Act
        Calibration.initialCalibration(140, 145, RuntimeEnvironment.application.getApplicationContext());

        // :: Verify
        List<Calibration> calibrations = Calibration.getCalibrationsForSensor(Sensor.currentSensor(), 3);
        assertThat(calibrations).hasSize(2);
        Calibration calibration1 = calibrations.get(0);
        assertThat(calibration1.bg).isWithin(0.01).of(145);
        assertThat(calibration1.raw_value).isWithin(0.01).of(135);

        Calibration calibration2 = calibrations.get(1);
        assertThat(calibration2.bg).isWithin(0.01).of(140);
        assertThat(calibration2.raw_value).isWithin(0.01).of(130);
    }

    @Test
    public void initialCalibration_sinkingBg_OK() {
        // :: Setup
        // Add mock sensor
        Sensor mockSensor = new Sensor();
        mockSensor.started_at = System.currentTimeMillis() - (1000 * 60 * 20);
        mockSensor.uuid = UUID.randomUUID().toString();
        mockSensor.save();

        // Add mock bg readings
        addMockBgReading(135, 11, mockSensor);
        addMockBgReading(130, 6, mockSensor);
        addMockBgReading(125, 1, mockSensor);

        // :: Act
        Calibration.initialCalibration(145, 140, RuntimeEnvironment.application);

        // :: Verify
        List<Calibration> calibrations = Calibration.getCalibrationsForSensor(Sensor.currentSensor(), 3);
        assertThat(calibrations).hasSize(2);
        Calibration calibration1 = calibrations.get(0);
        assertThat(calibration1.bg).isWithin(0.01).of(145);
        assertThat(calibration1.raw_value).isWithin(0.01).of(130);

        Calibration calibration2 = calibrations.get(1);
        assertThat(calibration2.bg).isWithin(0.01).of(140);
        assertThat(calibration2.raw_value).isWithin(0.01).of(125);
    }

    // ===== Internal Helpers ======================================================================

    private void addMockBgReading(int raw_data, int minutes, Sensor sensor) {
        BgReading mockReading = new BgReading();
        mockReading.raw_data = raw_data;
        mockReading.timestamp = System.currentTimeMillis() - (1000 * 60 * minutes);
        mockReading.sensor = sensor;
        mockReading.save();
    }
}
