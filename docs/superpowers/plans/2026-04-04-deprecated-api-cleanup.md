# Deprecated API Cleanup — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace deprecated Android APIs with modern equivalents, file by file, with characterization tests written before each change — as preparation for future targetSdkVersion upgrades.

**Architecture:** Each category of deprecated API is handled in order: a pattern PR establishes the migration for the simplest file and creates a migration note in `docs/cleanup/`; then cleanup PRs handle the remaining files one small batch at a time. All branches are cut from `master` independently. No functional changes are made.

**Tech Stack:** Java, compileSdk 34 / targetSdkVersion 24 / minSdkVersion 24, Robolectric 4.11.1, Mockito, `androidx.core.content.ContextCompat`, `androidx.preference.PreferenceManager`

> **HARD CONSTRAINT — API 24 compatibility:**
> `targetSdkVersion` and `minSdkVersion` are both **24**. The app must run correctly on API 24 devices.
> Every replacement in this plan must work on API 24+. Do NOT use APIs introduced above API 24 in replacement code.
> `compileSdk 34` only means we compile against SDK 34 headers to surface deprecation warnings — it does **not** raise the minimum runtime API level.
> All `androidx.*` libraries in use handle their own backcompat and are safe to use.

---

## How to Read This Plan

This plan is organized by category. Each category follows **the same TDD workflow**:

1. Cut a branch from `master`
2. Write characterization tests that pass on the original code
3. Replace the deprecated API
4. Confirm tests still pass
5. Commit `docs/cleanup/<category>.md` (pattern PRs only)
6. Open PR

Category 1 is documented in full detail. Subsequent categories follow the same steps with different files and replacements, as noted in each section.

---

## Test Base Classes

All new test classes extend one of two existing base classes:

- `RobolectricTestNoConfig` — for pure logic, no Android context needed
- `RobolectricTestWithConfig` — for anything needing Activities, Views, Services, or Preferences

Test files go in `app/src/test/java/` mirroring the package structure of `app/src/main/java/`.

---

## Category Summary

| # | Category | Files | Pattern file |
|---|---|---|---|
| 1 | `resources.getColor(int)` | 1 | `PercentileView.java` |
| 2 | `android.preference.PreferenceManager` | 65 | `Agreement.java` |
| 3 | `android.preference.Preference*` UI classes | 8 | `BasePreferenceActivity.java` |
| 4 | `AsyncTask` | 14 | `UploaderTask.java` |
| 5 | `new Thread` (direct) | 44 | `ChartView.java` or `NanoStatus.java` (assess first) |
| 6 | `startActivityForResult` / `onActivityResult` | 15 | TBD after Cat. 4 established |
| 7 | `PendingIntent` flags (missing `FLAG_IMMUTABLE`) | 22 | TBD after Cat. 5 established |
| 8 | BLE APIs | ~39 | TBD last |

---

## Category 1: `resources.getColor(int)` → `ContextCompat.getColor(context, int)`

**Rule:** `resources.getColor(R.color.X)` is deprecated since API 23 (no theme parameter). Replace with `ContextCompat.getColor(getContext(), R.color.X)` inside a View, or `ContextCompat.getColor(context, R.color.X)` elsewhere.

**Files:** `stats/PercentileView.java` (only file — pattern PR = only PR for this category)

---

### Task 1.1: Write characterization tests for PercentileView

**Files:**
- Create: `app/src/test/java/com/eveningoutpost/dexdrip/stats/PercentileViewTest.java`

- [ ] **Step 1: Create the test file**

```java
package com.eveningoutpost.dexdrip.stats;

import android.graphics.Paint;

import androidx.core.content.ContextCompat;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;

import java.lang.reflect.Field;

import static com.google.common.truth.Truth.assertThat;

public class PercentileViewTest extends RobolectricTestWithConfig {

    // --- Constructor ---

    /** Characterization: outerPaint color is initialized from percentile_outer resource */
    @Test
    public void constructor_setsOuterPaintColor() throws Exception {
        PercentileView view = new PercentileView(xdrip.getAppContext());

        int expected = ContextCompat.getColor(xdrip.getAppContext(), R.color.percentile_outer);
        assertThat(paintColor(view, "outerPaint")).isEqualTo(expected);
    }

    /** Characterization: outerPaintLabel color is initialized from percentile_outer resource */
    @Test
    public void constructor_setsOuterPaintLabelColor() throws Exception {
        PercentileView view = new PercentileView(xdrip.getAppContext());

        int expected = ContextCompat.getColor(xdrip.getAppContext(), R.color.percentile_outer);
        assertThat(paintColor(view, "outerPaintLabel")).isEqualTo(expected);
    }

    /** Characterization: innerPaint color is initialized from percentile_inner resource */
    @Test
    public void constructor_setsInnerPaintColor() throws Exception {
        PercentileView view = new PercentileView(xdrip.getAppContext());

        int expected = ContextCompat.getColor(xdrip.getAppContext(), R.color.percentile_inner);
        assertThat(paintColor(view, "innerPaint")).isEqualTo(expected);
    }

    /** Characterization: innerPaintLabel color is initialized from percentile_inner resource */
    @Test
    public void constructor_setsInnerPaintLabelColor() throws Exception {
        PercentileView view = new PercentileView(xdrip.getAppContext());

        int expected = ContextCompat.getColor(xdrip.getAppContext(), R.color.percentile_inner);
        assertThat(paintColor(view, "innerPaintLabel")).isEqualTo(expected);
    }

    /** Characterization: medianPaint color is initialized from percentile_median resource */
    @Test
    public void constructor_setsMedianPaintColor() throws Exception {
        PercentileView view = new PercentileView(xdrip.getAppContext());

        int expected = ContextCompat.getColor(xdrip.getAppContext(), R.color.percentile_median);
        assertThat(paintColor(view, "medianPaint")).isEqualTo(expected);
    }

    /** Characterization: medianPaintLabel color is initialized from percentile_median resource */
    @Test
    public void constructor_setsMedianPaintLabelColor() throws Exception {
        PercentileView view = new PercentileView(xdrip.getAppContext());

        int expected = ContextCompat.getColor(xdrip.getAppContext(), R.color.percentile_median);
        assertThat(paintColor(view, "medianPaintLabel")).isEqualTo(expected);
    }

    // --- Helpers ---

    private int paintColor(PercentileView view, String fieldName) throws Exception {
        Field field = PercentileView.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return ((Paint) field.get(view)).getColor();
    }
}
```

- [ ] **Step 2: Run the tests — confirm they pass on original code**

```bash
cd /path/to/xDrip
./gradlew :app:testFastDebugUnitTest --tests "com.eveningoutpost.dexdrip.stats.PercentileViewTest" 2>&1 | tail -20
```

Expected: All 6 tests PASS. If any fail, investigate before proceeding — the test is wrong, not the code.

---

### Task 1.2: Replace deprecated API in PercentileView

**Files:**
- Modify: `app/src/main/java/com/eveningoutpost/dexdrip/stats/PercentileView.java`

- [ ] **Step 1: Add the ContextCompat import**

In `PercentileView.java`, add after the existing imports:
```java
import androidx.core.content.ContextCompat;
```

- [ ] **Step 2: Replace all 6 `resources.getColor()` calls**

Replace each of these 6 lines (lines ~50, 57, 63, 69, 76, 83):

```java
// BEFORE (6 occurrences, each with a different color resource):
outerPaint.setColor(resources.getColor(R.color.percentile_outer));
outerPaintLabel.setColor(resources.getColor(R.color.percentile_outer));
innerPaint.setColor(resources.getColor(R.color.percentile_inner));
innerPaintLabel.setColor(resources.getColor(R.color.percentile_inner));
medianPaint.setColor(resources.getColor(R.color.percentile_median));
medianPaintLabel.setColor(resources.getColor(R.color.percentile_median));

// AFTER:
outerPaint.setColor(ContextCompat.getColor(getContext(), R.color.percentile_outer));
outerPaintLabel.setColor(ContextCompat.getColor(getContext(), R.color.percentile_outer));
innerPaint.setColor(ContextCompat.getColor(getContext(), R.color.percentile_inner));
innerPaintLabel.setColor(ContextCompat.getColor(getContext(), R.color.percentile_inner));
medianPaint.setColor(ContextCompat.getColor(getContext(), R.color.percentile_median));
medianPaintLabel.setColor(ContextCompat.getColor(getContext(), R.color.percentile_median));
```

Note: The `resources` field is still needed for `dp2px()` — do NOT remove it or the field declaration.

- [ ] **Step 3: Run tests again — confirm still green**

```bash
./gradlew :app:testFastDebugUnitTest --tests "com.eveningoutpost.dexdrip.stats.PercentileViewTest" 2>&1 | tail -20
```

Expected: All 6 tests still PASS.

---

### Task 1.3: Write migration note and commit

**Files:**
- Create: `docs/cleanup/context-resources.md`

- [ ] **Step 1: Create the migration note**

```markdown
# Migration: resources.getColor / resources.getDrawable

## What is deprecated

`Resources.getColor(int)` and `Resources.getDrawable(int)` (without a `Theme` parameter)
are deprecated since API 23.

## Replacement

Use `ContextCompat` from `androidx.core.content`:

| Before | After |
|--------|-------|
| `resources.getColor(R.color.X)` | `ContextCompat.getColor(context, R.color.X)` |
| `resources.getDrawable(R.drawable.X)` | `ContextCompat.getDrawable(context, R.drawable.X)` |

Inside a `View` subclass, use `getContext()` as the context argument.
In an `Activity` or `Service`, use `this` or `getApplicationContext()`.

## Import

```java
import androidx.core.content.ContextCompat;
```

## Example (from PercentileView.java pattern PR)

```java
// Before
outerPaint.setColor(resources.getColor(R.color.percentile_outer));

// After
outerPaint.setColor(ContextCompat.getColor(getContext(), R.color.percentile_outer));
```
```

- [ ] **Step 2: Commit everything**

```bash
git add app/src/test/java/com/eveningoutpost/dexdrip/stats/PercentileViewTest.java
git add app/src/main/java/com/eveningoutpost/dexdrip/stats/PercentileView.java
git add docs/cleanup/context-resources.md
git commit -m "[pattern] Replace resources.getColor with ContextCompat.getColor in PercentileView"
```

- [ ] **Step 3: Open PR targeting `master`**

Title: `[pattern] Replace resources.getColor with ContextCompat.getColor in PercentileView`

---

## Category 2: `android.preference.PreferenceManager` → `androidx.preference.PreferenceManager`

**Rule:** `android.preference.PreferenceManager` is deprecated in API 29. Replace the import with `androidx.preference.PreferenceManager`. The API is identical — this is a pure import swap.

**Dependency:** `androidx.preference:preference:1.0.0` is already in `app/build.gradle`. No dependency changes needed.

**Pattern file:** `Agreement.java` (52 lines, single usage)

**All 65 files:**
```
com/eveningoutpost/dexdrip/Agreement.java
com/eveningoutpost/dexdrip/AlertList.java
com/eveningoutpost/dexdrip/BestGlucose.java
com/eveningoutpost/dexdrip/BGHistory.java
com/eveningoutpost/dexdrip/BluetoothScan.java
com/eveningoutpost/dexdrip/EditAlertActivity.java
com/eveningoutpost/dexdrip/ErrorsActivity.java
com/eveningoutpost/dexdrip/FollowerManagementActivity.java
com/eveningoutpost/dexdrip/GcmActivity.java
com/eveningoutpost/dexdrip/GoogleDriveInterface.java
com/eveningoutpost/dexdrip/Home.java
com/eveningoutpost/dexdrip/importedlibraries/dexcom/SyncingService.java
com/eveningoutpost/dexdrip/influxdb/InfluxDBUploader.java
com/eveningoutpost/dexdrip/LibreAlarmReceiver.java
com/eveningoutpost/dexdrip/LicenseAgreementActivity.java
com/eveningoutpost/dexdrip/localeTasker/receiver/FireReceiver.java
com/eveningoutpost/dexdrip/MissedReadingActivity.java
com/eveningoutpost/dexdrip/models/AlertType.java
com/eveningoutpost/dexdrip/models/BgReading.java
com/eveningoutpost/dexdrip/models/Calibration.java
com/eveningoutpost/dexdrip/models/Profile.java
com/eveningoutpost/dexdrip/NavDrawerBuilder.java
com/eveningoutpost/dexdrip/NavigationDrawerFragment.java
com/eveningoutpost/dexdrip/NewSensorLocation.java
com/eveningoutpost/dexdrip/NFCReaderX.java
com/eveningoutpost/dexdrip/NSClientReceiver.java
com/eveningoutpost/dexdrip/NSEmulatorReceiver.java
com/eveningoutpost/dexdrip/ParakeetHelper.java
com/eveningoutpost/dexdrip/receivers/aidex/AidexReceiver.java
com/eveningoutpost/dexdrip/RegistrationIntentService.java
com/eveningoutpost/dexdrip/services/broadcastservice/BroadcastService.java
com/eveningoutpost/dexdrip/services/DexCollectionService.java
com/eveningoutpost/dexdrip/services/DexShareCollectionService.java
com/eveningoutpost/dexdrip/services/DoNothingService.java
com/eveningoutpost/dexdrip/services/G5CollectionService.java
com/eveningoutpost/dexdrip/services/MissedReadingService.java
com/eveningoutpost/dexdrip/services/Ob1G5CollectionService.java
com/eveningoutpost/dexdrip/services/PlusSyncService.java
com/eveningoutpost/dexdrip/services/SnoozeOnNotificationDismissService.java
com/eveningoutpost/dexdrip/services/WifiCollectionService.java
com/eveningoutpost/dexdrip/sharemodels/ShareRest.java
com/eveningoutpost/dexdrip/ShareTest.java
com/eveningoutpost/dexdrip/SnoozeActivity.java
com/eveningoutpost/dexdrip/stats/DBSearchUtil.java
com/eveningoutpost/dexdrip/stats/FirstPageFragment.java
com/eveningoutpost/dexdrip/stats/PercentileView.java
com/eveningoutpost/dexdrip/SystemStatusFragment.java
com/eveningoutpost/dexdrip/utilitymodels/AlertPlayer.java
com/eveningoutpost/dexdrip/utilitymodels/BgGraphBuilder.java
com/eveningoutpost/dexdrip/utilitymodels/CollectionServiceStarter.java
com/eveningoutpost/dexdrip/utilitymodels/ColorCache.java
com/eveningoutpost/dexdrip/utilitymodels/IdempotentMigrations.java
com/eveningoutpost/dexdrip/utilitymodels/NightscoutUploader.java
com/eveningoutpost/dexdrip/utilitymodels/Notifications.java
com/eveningoutpost/dexdrip/utilitymodels/pebble/PebbleDisplayAbstract.java
com/eveningoutpost/dexdrip/utilitymodels/pebble/PebbleDisplayTrend.java
com/eveningoutpost/dexdrip/utilitymodels/pebble/PebbleDisplayTrendOld.java
com/eveningoutpost/dexdrip/utilitymodels/Pref.java
com/eveningoutpost/dexdrip/utilitymodels/UpdateActivity.java
com/eveningoutpost/dexdrip/utils/DisplayQRCode.java
com/eveningoutpost/dexdrip/utils/Preferences.java
com/eveningoutpost/dexdrip/utils/Telemetry.java
com/eveningoutpost/dexdrip/wearintegration/Amazfitservice.java
com/eveningoutpost/dexdrip/wearintegration/WatchUpdaterService.java
com/eveningoutpost/dexdrip/xdrip.java
```

---

### Task 2.1: Pattern PR — Agreement.java

**Files:**
- Create: `app/src/test/java/com/eveningoutpost/dexdrip/AgreementTest.java`
- Modify: `app/src/main/java/com/eveningoutpost/dexdrip/Agreement.java`
- Create: `docs/cleanup/shared-preferences.md`

- [ ] **Step 1: Create characterization test**

```java
package com.eveningoutpost.dexdrip;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import org.junit.Test;
import org.robolectric.Robolectric;

import static com.google.common.truth.Truth.assertThat;

public class AgreementTest extends RobolectricTestWithConfig {

    // --- onCreate ---

    /** Characterization: IUnderstand defaults to false when preference is not set */
    @Test
    public void onCreate_iUnderstandDefaultsFalse() {
        Agreement activity = Robolectric.buildActivity(Agreement.class).create().get();

        assertThat(activity.IUnderstand).isFalse();
    }

    /** Characterization: IUnderstand is true when preference has been set */
    @Test
    public void onCreate_iUnderstandTrueWhenPreferenceSet() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                xdrip.getAppContext());
        prefs.edit().putBoolean(Agreement.prefmarker, true).commit();

        Agreement activity = Robolectric.buildActivity(Agreement.class).create().get();

        assertThat(activity.IUnderstand).isTrue();
    }
}
```

- [ ] **Step 2: Run tests — confirm green on original code**

```bash
./gradlew :app:testFastDebugUnitTest --tests "com.eveningoutpost.dexdrip.AgreementTest" 2>&1 | tail -20
```

Expected: Both tests PASS.

- [ ] **Step 3: Swap the import in Agreement.java**

```java
// Remove:
import android.preference.PreferenceManager;

// Add:
import androidx.preference.PreferenceManager;
```

No other changes.

- [ ] **Step 4: Run tests again — confirm still green**

```bash
./gradlew :app:testFastDebugUnitTest --tests "com.eveningoutpost.dexdrip.AgreementTest" 2>&1 | tail -20
```

Expected: Both tests still PASS.

- [ ] **Step 5: Create migration note**

Write the file `docs/cleanup/shared-preferences.md` with this content (use 4-space-indented code blocks instead of backtick fences to avoid nesting issues):

    # Migration: android.preference.PreferenceManager → androidx.preference.PreferenceManager

    ## What is deprecated

    `android.preference.PreferenceManager` is deprecated in API 29. The entire
    `android.preference.*` package was replaced by `androidx.preference.*`.

    ## Replacement (PreferenceManager only)

    This is a **pure import swap**. The API is identical.

    | Before | After |
    |--------|-------|
    | `import android.preference.PreferenceManager;` | `import androidx.preference.PreferenceManager;` |

    ## Dependency

    `androidx.preference:preference:1.0.0` is already in `app/build.gradle`. No changes needed.

    ## Example (from Agreement.java pattern PR)

    Before:

        import android.preference.PreferenceManager;

    After:

        import androidx.preference.PreferenceManager;

    Call sites are unchanged:

        PreferenceManager.getDefaultSharedPreferences(context)

- [ ] **Step 6: Commit and open PR**

```bash
git add app/src/test/java/com/eveningoutpost/dexdrip/AgreementTest.java
git add app/src/main/java/com/eveningoutpost/dexdrip/Agreement.java
git add docs/cleanup/shared-preferences.md
git commit -m "[pattern] Replace android.preference.PreferenceManager with androidx in Agreement"
```

PR title: `[pattern] Replace android.preference.PreferenceManager with androidx in Agreement`

---

### Tasks 2.2–2.N: Cleanup PRs (remaining 64 files)

For each batch of up to 5 files from the list above:

1. Cut branch from `master`: `git checkout -b cleanup/preference-manager-<ShortName>`
2. For each file in the batch:
   - Check if a test file already exists for this class (in `app/src/test/java/...`)
   - If no test exists: create one extending `RobolectricTestWithConfig`, with at least one test per method that calls `PreferenceManager.getDefaultSharedPreferences()`
   - Confirm tests pass on original code
   - Swap `import android.preference.PreferenceManager` → `import androidx.preference.PreferenceManager`
   - Confirm tests still pass
3. Commit all files in the batch together
4. Open PR targeting `master`

PR title pattern: `[cleanup] Replace android.preference.PreferenceManager with androidx in <ClassName1>, <ClassName2>, ...`

---

## Category 3: `android.preference.Preference*` UI Classes

**Rule:** `android.preference.PreferenceActivity`, `android.preference.Preference`, `android.preference.ListPreference`, etc. are deprecated. Migration to `PreferenceFragmentCompat` is architectural and more involved than a simple import swap. Each file requires individual assessment.

**Files (8):**
```
com/eveningoutpost/dexdrip/BasePreferenceActivity.java        (extends PreferenceActivity)
com/eveningoutpost/dexdrip/calibrations/PluggableCalibration.java
com/eveningoutpost/dexdrip/ui/LockScreenWallPaper.java
com/eveningoutpost/dexdrip/utils/ExampleChartPreferenceView.java
com/eveningoutpost/dexdrip/utils/Preferences.java
com/eveningoutpost/dexdrip/utils/TimePreference.java
com/eveningoutpost/dexdrip/watch/miband/MiBandEntry.java
com/eveningoutpost/dexdrip/watch/thinjam/BlueJayAdapter.java
```

**Pattern file:** `BasePreferenceActivity.java` (16 lines, simplest)

**Note:** `PreferenceActivity` → `AppCompatActivity` + `PreferenceFragmentCompat` involves creating a Fragment. Assess each file individually during the pattern PR to determine the minimal safe replacement.

Follow the same TDD workflow as categories 1 and 2. Create `docs/cleanup/preference-ui-classes.md` in the pattern PR.

---

## Category 4: `AsyncTask` → `Executor`

**Rule:** `android.os.AsyncTask` is deprecated in API 30. Replace with `java.util.concurrent.Executor` (preferred for fire-and-forget background work) or Kotlin coroutines. Since these are Java files, use `Executor`.

**Pattern:** Replace `new AsyncTask<A, B, C>() { doInBackground / onPostExecute }` with an `Executor` that posts results back to the main thread via `new Handler(Looper.getMainLooper()).post(...)`.

**Files (14):**
```
com/eveningoutpost/dexdrip/Home.java
com/eveningoutpost/dexdrip/ImportDatabaseActivity.java
com/eveningoutpost/dexdrip/models/BgReading.java
com/eveningoutpost/dexdrip/models/UserError.java
com/eveningoutpost/dexdrip/NFCReaderX.java
com/eveningoutpost/dexdrip/services/LibreWifiReader.java
com/eveningoutpost/dexdrip/services/WifiCollectionService.java
com/eveningoutpost/dexdrip/services/WixelReader.java
com/eveningoutpost/dexdrip/sharemodels/ShareRest.java
com/eveningoutpost/dexdrip/utilitymodels/UpdateActivity.java
com/eveningoutpost/dexdrip/utilitymodels/UploaderTask.java
com/eveningoutpost/dexdrip/utils/WebAppHelper.java
com/eveningoutpost/dexdrip/wearintegration/SendToDataLayerThread.java
com/eveningoutpost/dexdrip/wearintegration/WatchUpdaterService.java
```

**Pattern file:** `UploaderTask.java` (211 lines, self-contained)

**Replacement template:**
```java
// Before (AsyncTask):
new AsyncTask<Void, Void, Void>() {
    @Override
    protected Void doInBackground(Void... params) {
        // background work
        return null;
    }
    @Override
    protected void onPostExecute(Void result) {
        // UI work
    }
}.execute();

// After (Executor):
Executors.newSingleThreadExecutor().execute(() -> {
    // background work
    new Handler(Looper.getMainLooper()).post(() -> {
        // UI work
    });
});
```

**Imports to add:**
```java
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;
```

Follow the same TDD workflow. Create `docs/cleanup/async-task.md` in the pattern PR.

---

## Category 5: `new Thread` (direct)

**Rule:** Direct `new Thread() { ... }.start()` is an uncontrolled thread creation. Replace with `Executors.newSingleThreadExecutor().execute(...)` for fire-and-forget tasks. For repeating background calculations in Views, consider a dedicated background thread field with a clear lifecycle.

**Note:** `PercentileView.java` has a `new Thread` for its data calculation (in `getMaybeCalculatedData()`). This will be handled in a cleanup PR for this category — separate from the Category 1 pattern PR which only touches `resources.getColor`.

**Pattern file:** `stats/ChartView.java` or `utilitymodels/NanoStatus.java` — check which has the simpler, most isolated `new Thread` usage before starting.

**All 44 files:**
```
com/eveningoutpost/dexdrip/AddCalibration.java
com/eveningoutpost/dexdrip/cloud/jamcm/JamCm.java
com/eveningoutpost/dexdrip/cloud/jamcm/Pusher.java
com/eveningoutpost/dexdrip/deposit/DepositActivity.java
com/eveningoutpost/dexdrip/EventLogActivity.java
com/eveningoutpost/dexdrip/g5model/Ob1G5StateMachine.java
com/eveningoutpost/dexdrip/GcmActivity.java
com/eveningoutpost/dexdrip/GoogleDriveInterface.java
com/eveningoutpost/dexdrip/Home.java
com/eveningoutpost/dexdrip/importedlibraries/usbserial/driver/ProlificSerialDriver.java
com/eveningoutpost/dexdrip/LibreAlarmReceiver.java
com/eveningoutpost/dexdrip/LibreReceiver.java
com/eveningoutpost/dexdrip/models/DesertSync.java
com/eveningoutpost/dexdrip/models/JoH.java
com/eveningoutpost/dexdrip/models/NSClientChat.java
com/eveningoutpost/dexdrip/nfc/NFControl.java
com/eveningoutpost/dexdrip/NFCReaderX.java
com/eveningoutpost/dexdrip/NightscoutBackfillActivity.java
com/eveningoutpost/dexdrip/NSEmulatorReceiver.java
com/eveningoutpost/dexdrip/receivers/aidex/AidexReceiver.java
com/eveningoutpost/dexdrip/services/DexCollectionService.java
com/eveningoutpost/dexdrip/services/DoNothingService.java
com/eveningoutpost/dexdrip/services/JamBaseBluetoothService.java
com/eveningoutpost/dexdrip/services/Ob1G5CollectionService.java
com/eveningoutpost/dexdrip/services/PlusSyncService.java
com/eveningoutpost/dexdrip/stats/ChartView.java
com/eveningoutpost/dexdrip/stats/FirstPageFragment.java
com/eveningoutpost/dexdrip/stats/PercentileView.java
com/eveningoutpost/dexdrip/utilitymodels/desertsync/DesertComms.java
com/eveningoutpost/dexdrip/utilitymodels/Inevitable.java
com/eveningoutpost/dexdrip/utilitymodels/NanoStatus.java
com/eveningoutpost/dexdrip/utilitymodels/NightscoutUploader.java
com/eveningoutpost/dexdrip/utilitymodels/PlusAsyncExecutor.java
com/eveningoutpost/dexdrip/utilitymodels/SendFeedBack.java
com/eveningoutpost/dexdrip/utilitymodels/SpeechUtil.java
com/eveningoutpost/dexdrip/utilitymodels/UpdateActivity.java
com/eveningoutpost/dexdrip/utils/DisplayQRCode.java
com/eveningoutpost/dexdrip/utils/Mdns.java
com/eveningoutpost/dexdrip/utils/Preferences.java
com/eveningoutpost/dexdrip/watch/thinjam/BlueJayAsset.java
com/eveningoutpost/dexdrip/watch/thinjam/BlueJayService.java
com/eveningoutpost/dexdrip/wearintegration/WatchUpdaterService.java
com/eveningoutpost/dexdrip/webservices/XdripWebService.java
com/eveningoutpost/dexdrip/xdrip.java
```

Create `docs/cleanup/new-thread.md` in the pattern PR.

---

## Category 6: `startActivityForResult` / `onActivityResult`

**Rule:** `startActivityForResult` is deprecated. Replace with the Activity Result API: `registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), callback)`.

**Files (15):**
```
com/eveningoutpost/dexdrip/AlertList.java
com/eveningoutpost/dexdrip/BluetoothScan.java
com/eveningoutpost/dexdrip/cloud/backup/BackupActivity.java
com/eveningoutpost/dexdrip/cloud/backup/BackupBaseActivity.java
com/eveningoutpost/dexdrip/eassist/EmergencyAssistActivity.java
com/eveningoutpost/dexdrip/EditAlertActivity.java
com/eveningoutpost/dexdrip/GoogleDriveInterface.java
com/eveningoutpost/dexdrip/healthconnect/HealthGamut.java
com/eveningoutpost/dexdrip/Home.java
com/eveningoutpost/dexdrip/MissedReadingActivity.java
com/eveningoutpost/dexdrip/Reminders.java
com/eveningoutpost/dexdrip/ui/activities/NumberWallPreview.java
com/eveningoutpost/dexdrip/ui/activities/ThinJamActivity.java
com/eveningoutpost/dexdrip/utils/Preferences.java
com/eveningoutpost/dexdrip/utils/QrCodeFromFile.java
```

**Pattern file:** Select the file with the fewest `startActivityForResult` calls (check each file before starting).

**Replacement template:**
```java
// Before:
startActivityForResult(intent, REQUEST_CODE);
// ... and ...
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) { ... }
}

// After: register in onCreate
private ActivityResultLauncher<Intent> launcher = registerForActivityResult(
    new ActivityResultContracts.StartActivityForResult(),
    result -> {
        if (result.getResultCode() == RESULT_OK) { ... }
    }
);
// ... and call:
launcher.launch(intent);
```

**Import to add:**
```java
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
```

Follow the same TDD workflow. Create `docs/cleanup/activity-result.md` in the pattern PR.

---

## Category 7: `PendingIntent` flags (missing `FLAG_IMMUTABLE`)

**Rule:** From API 31, `PendingIntent` creation requires either `FLAG_IMMUTABLE` or `FLAG_MUTABLE` to be specified. Add `FLAG_IMMUTABLE` to all existing `PendingIntent.get*()` calls that don't already include it. Since `minSdk` is 24, use:
```java
PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0)
```
Or, since `minSdk` is 24 (>= 23), simplify to:
```java
PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
```

**Files (22):**
```
com/eveningoutpost/dexdrip/alert/UpdateAvailable.java
com/eveningoutpost/dexdrip/cloud/backup/BackupActivity.java
com/eveningoutpost/dexdrip/GcmListenerSvc.java
com/eveningoutpost/dexdrip/Home.java
com/eveningoutpost/dexdrip/MegaStatus.java
com/eveningoutpost/dexdrip/models/JoH.java
com/eveningoutpost/dexdrip/ParakeetHelper.java
com/eveningoutpost/dexdrip/Reminders.java
com/eveningoutpost/dexdrip/services/ActivityRecognizedService.java
com/eveningoutpost/dexdrip/services/DailyIntentService.java
com/eveningoutpost/dexdrip/services/DexShareCollectionService.java
com/eveningoutpost/dexdrip/services/JamBaseBluetoothSequencer.java
com/eveningoutpost/dexdrip/services/Ob1G5CollectionService.java
com/eveningoutpost/dexdrip/services/SyncService.java
com/eveningoutpost/dexdrip/utilitymodels/AlertPlayer.java
com/eveningoutpost/dexdrip/utilitymodels/CollectionServiceStarter.java
com/eveningoutpost/dexdrip/utilitymodels/CompatibleApps.java
com/eveningoutpost/dexdrip/utilitymodels/Notifications.java
com/eveningoutpost/dexdrip/utils/CheckBridgeBattery.java
com/eveningoutpost/dexdrip/utils/framework/WakeLockTrampoline.java
com/eveningoutpost/dexdrip/watch/thinjam/BackgroundScanReceiver.java
com/eveningoutpost/dexdrip/wearintegration/WatchUpdaterService.java
```

**Pattern file:** `UpdateAvailable.java` (assess size before starting)

Follow the same TDD workflow. Create `docs/cleanup/notifications.md` in the pattern PR.

---

## Category 8: BLE APIs

**Scope:** To be detailed after all previous categories are complete and the team is confident in the process. BLE is medical-critical code — do not start until categories 1–7 are fully merged.

Run the following to get the file list when ready:
```bash
grep -rn "BluetoothAdapter\|BluetoothGatt\b\|BluetoothDevice\|BluetoothLeScanner" \
  app/src/main --include="*.java" | grep -v "//" | cut -d: -f1 | sort -u
```

Create `docs/cleanup/ble.md` as part of the BLE pattern PR.

---

## Branch and Commit Conventions

| Type | Branch name | Commit/PR prefix |
|---|---|---|
| Pattern PR | `cleanup/<category>-pattern` | `[pattern]` |
| Cleanup PR | `cleanup/<category>-<ClassName>` | `[cleanup]` |

All branches cut from `master`. Never stack branches on each other.
