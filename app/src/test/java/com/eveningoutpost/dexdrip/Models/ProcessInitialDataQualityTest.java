package com.eveningoutpost.dexdrip.Models;

import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.MockModel;
import com.eveningoutpost.dexdrip.TestingApplication;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertWithMessage;


/**
 * Test for initial data quality
 * <p>
 * Created by jamorham on 01/10/2017.
 */
@RunWith(RobolectricTestRunner.class)
@Config(
        constants = BuildConfig.class,
        application = TestingApplication.class
)
public class ProcessInitialDataQualityTest {

    // if we have a record which is on an exact millisecond boundary and test it and it passes the test
    // 1ms later it will fail the test resulting in the assertion sometimes incorrectly labelling as a
    // mismatched result because the clock can tick over 1ms between the time we first tested and when we
    // compare the results of that test. To avoid this (even on slow systems) we add in a grace period
    private static final long COMPUTATION_GRACE_TIME = Constants.SECOND_IN_MS;

    private static void log(String msg) {
        System.out.println(msg);
    }

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testGetInitialDataQuality() throws Exception {

        // check we can mock ActiveAndroid which depends on Android framework
        MockModel m = new MockModel();
        assertWithMessage("ActiveAndroid Mock Model can be created").that(m).isNotNull();
        assertWithMessage("ActiveAndroid Mock Model has been created").that(m.intField).isEqualTo(0);

        // result object store
        ProcessInitialDataQuality.InitialDataQuality test;

        // try with null data set
        test = ProcessInitialDataQuality.getInitialDataQuality(null);

        assertWithMessage("Result object not null").that(test).isNotNull();
        assertWithMessage("Null input should fail").that(test.pass).isFalse();

        // try with empty data set
        List<BgReading> bgReadingList = new ArrayList<>();
        test = ProcessInitialDataQuality.getInitialDataQuality(bgReadingList);
        assertWithMessage("Result object not null").that(test).isNotNull();
        assertWithMessage("Empty input should fail").that(test.pass).isFalse();

        // create an assortment of data sets with data spaced out by different frequencies
        for (int frequency = 5; frequency < 21; frequency = frequency + 1) {
            bgReadingList.clear();
            for (int i = 1; i <= 3; i++) {
                // add an element
                bgReadingList.add(getGoodMockBgReading(i * Constants.MINUTE_IN_MS * frequency)); // we add older readings to the end
                test = ProcessInitialDataQuality.getInitialDataQuality(bgReadingList);
                log("Frequency: " + frequency + " Loop " + i + " size:" + bgReadingList.size()
                        + " Newest age: " + JoH.niceTimeScalar(JoH.msSince(bgReadingList.get(0).timestamp))
                        + " Oldest age: " + JoH.niceTimeScalar(JoH.msSince(bgReadingList.get(bgReadingList.size() - 1).timestamp))
                        + " / Mock Advice: " + test.advice + " VERDICT: " + (test.pass ? "PASS" : "NOT PASSED"));

                assertWithMessage("Result object not null").that(test).isNotNull();
                if (i < 3) {
                    assertWithMessage("Empty input should fail").that(test.pass).isFalse();
                }
                assertWithMessage("There should be some advice on loop " + i).that(test.advice).isNotEmpty();

                final long ms_since = (JoH.msSince(bgReadingList.get(bgReadingList.size() - 1).timestamp));
                if ((ms_since > Constants.STALE_CALIBRATION_CUT_OFF + COMPUTATION_GRACE_TIME) || (i < 3)) {
                    assertWithMessage("Stale data should fail: i:" + i + " tm:" + ms_since).that(test.pass).isFalse();
                }
                if ((ms_since <= Constants.STALE_CALIBRATION_CUT_OFF) && (bgReadingList.size() >= 3)) {
                    assertWithMessage("Good data should pass").that(test.pass).isTrue();
                }

            }
        }
    }

    // Timestamps from this will not be realistic, this could become an issue if other filtering
    // mechanisms failed to prevent duplicates etc. A better mock bg reading creator could be
    // produced but I have left this as flexible as possible at this point.
    private static BgReading getGoodMockBgReading(long ago) {
        final BgReading bg = new BgReading();
        bg.raw_data = 123;
        bg.timestamp = JoH.tsl() - ago; // current timestamp
        bg.noise = "MOCK DATA - DO NOT USE";
        return bg;
    }

}