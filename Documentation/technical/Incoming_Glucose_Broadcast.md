# Integration with xDrip+ via Broadcast Intent

## Overview

This page describes how a third-party Android application sends a glucose sensor reading to xDrip+
using a broadcast intent, and what xDrip+ does with what it receives. It is written for app
developers and sensor manufacturers.

xDrip+ has several broadcast receivers. This page documents the Nightscout Emulation receiver, and
only the insertion of glucose records.

## Before anything else: xDrip+ must be set up to accept the broadcast

The receiver rejects everything unless xDrip+ is configured to expect it. This is the xDrip+ user's
setup rather than something your app controls, but nothing works until it is done, so it belongs at
the start rather than at the end.

The broadcast is accepted when any one of these holds:

- `Hardware Data Source` is set to **`640G / EverSense`** — the normal choice for a broadcasting app,
  or
- `Hardware Data Source` is set to **`xDrip+ Sync Follower`**, or
- the **`Out of process Libre algorithm.`** option is enabled (Advanced Settings).

Two warnings about that list:

- **`xDrip+ Sync Follower` is the only "follower" that qualifies.** `Nightscout Follower`,
  `Dex Share Follower`, `Web Follower` and `CareLink Follower` are different settings and the
  receiver rejects broadcasts under all of them. Do not tell a user "you are a follower, this will
  work."
- **`Hardware Data Source` is single-choice.** Selecting `640G / EverSense` disconnects whatever
  collector the user had. Warn users before telling them to change it.

Settings are cached when the xDrip+ process starts. After changing any of them, the phone must be
rebooted, or xDrip+ force-stopped — closing and reopening the app is not enough.

## Sending a Broadcast Intent

### Intent action

```java
Intent intent = new Intent("com.eveningoutpost.dexdrip.NS_EMULATOR");
```

### Target xDrip+ explicitly

Set the package on every broadcast:

```java
intent.setPackage("com.eveningoutpost.dexdrip");
```

This is required. From Android 8 (API 26) onward, manifest-declared receivers no longer receive
implicit broadcasts, and xDrip+ targets API 26, so the restriction applies.

An implicit broadcast — one sent without `setPackage` — does still work while the xDrip+ process is
running, because xDrip+ also registers this receiver at runtime. It stops working the moment the
process is not running, and the reading is then delivered nowhere. Always name the package.

### Package visibility (sending app targets API 30 or later)

If your app's `targetSdkVersion` is 30 or higher, Android's package visibility rules apply and
`setPackage` alone is not enough — xDrip+ must also be visible to your app. Declare it in your
manifest, as a direct child of `<manifest>`:

```xml
<queries>
    <package android:name="com.eveningoutpost.dexdrip" />
</queries>
```

Without this, xDrip+ is not visible to your app and the broadcast is not delivered.

No permission is required to send this broadcast.

### Extra parameters

Two extras are required:

- `"collection"` — set to `"entries"`. **If this extra is missing the broadcast is discarded
  without any error.**
- `"data"` — the reading, as a JSON array **string**.

### JSON payload structure

| Attribute | Required | Value |
|-----------|----------|-------|
| `"type"` | Yes | `"sgv"` for a sensor glucose value record. |
| `"date"` | Yes | Timestamp of the reading, in milliseconds since the Unix epoch, on the phone's wall clock. |
| `"sgv"` | Yes | The glucose value in **mg/dL**. |
| `"direction"` | No | Trend, as one of the strings below. |

If `"type"`, `"date"` or `"sgv"` is missing or unparseable, the whole reading is dropped. If
`"direction"` is missing, unparseable, or not one of the recognised strings, the trend slope silently
defaults to zero — the reading is still stored, and will display as flat.

Recognised `"direction"` values, with the slope each maps to in mg/dL per minute:

| Value | Slope | Value | Slope |
|-------|-------|-------|-------|
| `DoubleUp` | +4 | `DoubleDown` | -3.5 |
| `SingleUp` | +3.5 | `SingleDown` | -2 |
| `FortyFiveUp` | +2 | `FortyFiveDown` | -1 |
| `Flat` | 0 | | |

`NONE`, `NOT_COMPUTABLE`, `NOT COMPUTABLE`, `OUT_OF_RANGE` and `OUT OF RANGE` are accepted and map
to zero. These are fixed representative slopes rather than range boundaries; see
`BgReading.slopefromName()` for the current mapping.

**Values are taken as given.** The value you send is stored as both the calculated and the raw
value. xDrip+ does not apply its own calibration to it, and does not range-check it — a value in
mmol/L is stored as mmol/L and displayed as a critical low. Send mg/dL.

**Send exactly one reading per broadcast.** The array length is used to distinguish this payload
from an internal one: an array with more than one element is treated as an out-of-process algorithm
result and handed to a different parser, and your readings are not inserted. Put one object in the
array and send another broadcast for the next reading.

### Complete example

`JSONObject.put()` throws `JSONException`, so this needs to sit inside a method that handles it.

```java
void sendReading(Context context, long timestamp, int mgdl, String direction) throws JSONException {
    Intent intent = new Intent("com.eveningoutpost.dexdrip.NS_EMULATOR");
    intent.setPackage("com.eveningoutpost.dexdrip");

    // indicate which collection this payload belongs to - required
    intent.putExtra("collection", "entries");

    final JSONObject sgv_object = new JSONObject();
    sgv_object.put("type", "sgv");
    sgv_object.put("date", timestamp);   // ms since epoch, phone wall clock
    sgv_object.put("sgv", mgdl);         // mg/dL
    sgv_object.put("direction", direction);

    final JSONArray sgv_array = new JSONArray();
    sgv_array.put(sgv_object);           // exactly one reading per broadcast

    intent.putExtra("data", sgv_array.toString());

    context.sendBroadcast(intent);
}
```

A similar example, wired up against the receiver, is in the unit test source tree in
`NSEmulatorReceiverTest.bgReadingExampleBroadcast()`.

## How often you can send

xDrip+ discards an incoming reading whose timestamp falls too close to one it already holds. The
margin is derived from the sample period configured on the xDrip+ side, and is applied either side
of the timestamp — currently four fifths of the period, so **4 minutes either side on the default
5-minute sample period**.

For this receiver the sample period is not freely selectable. There are two values:

| xDrip+ setting | Sample period | Readings rejected if closer than |
|---|---|---|
| default | 5 minutes | 4 minutes |
| `640G/Eversense 1-minute` enabled | 1 minute | 48 seconds |

The `640G/Eversense 1-minute` checkbox lives in Advanced Settings, depends on **engineering mode**
being enabled first, and its own summary warns that the feature is under development. The
`Sample period` dropdown in xDrip+ settings applies to Nightscout Follower mode and has no effect
here.

**Design for this.** A sensor that reads more often than the margin allows will have readings
dropped, silently, and the result looks like a working but slower trace rather than an error. A
3-minute sensor on the default setting loses every second reading: at t=0, 3, 6, 9, 12 minutes, the
readings at 3 and 9 fall inside the 4-minute window of the one before and are discarded.

If your sensor's interval is shorter than 4 minutes, either send at most one reading per 5 minutes
and drop the rest yourself, or document for your users that they must enable engineering mode and
the 1-minute option — and that the phone needs a reboot afterwards.

Note also that the duplicate check is not scoped to a sensor session: a reading from a previous
session at the same clock time will block the insert.

### Backfilling

Backfill works — send one broadcast per reading, spaced further apart than the margin. Two things to
know before you do:

- Every inserted reading is treated as live: it triggers alert evaluation and is forwarded to
  followers and watches. Backfilling an hour of history fires alerts as though each old reading had
  just arrived.
- Timestamps in the future are stored, but xDrip+ records a high-severity entry in its event log for
  each one. Do not send them.

## How failures surface

Most failures on this path are silent, which makes the two that are not worth knowing about first.

**These are recorded in xDrip+'s Event Log**, visible to the user in the app — ask a user to check
there before assuming nothing arrived:

| Cause | What appears |
|-------|--------------|
| xDrip+ not set up to accept the broadcast | `Received NSEmulator data but we are not a follower or emulator receiver` |
| Malformed JSON, or a missing `date` / `sgv` | `Got JSON exception: …` |
| `"type"` other than `"sgv"` | `Unknown entries type: …` |
| Timestamp in the future | A high-severity entry naming the timestamp |

That first row is the most common failure of all, and the event log distinguishes *not delivered*
from *delivered and rejected*, which nothing else does.

**These produce nothing the user can see:**

| Cause | Result |
|-------|--------|
| `"collection"` extra missing | Broadcast discarded |
| More than one reading in the array | Handed to another parser, reading not inserted |
| Broadcast sent implicitly, xDrip+ process not running | Never delivered |
| Sending app targets API 30+ without the `<queries>` entry | Never delivered |
| Reading too close in time to an existing one | Discarded as a duplicate |
| Unrecognised `"direction"` | Stored with a flat trend |

There is one visible side effect that is not a failure: if xDrip+ has no active sensor, your
broadcast causes the toast *"Please use: Start Sensor from the menu for best results!"*. The reading
is still stored — a sensor record is created automatically — but users may see the toast and report
it against your app.

### Debugging

- The logcat tag for this receiver is `jamorham nsemulator`.
- Debug-level lines from that tag can be promoted into the in-app log with the **Extra tags for
  logging** setting in Advanced Settings. This is the way to see duplicate-rejection messages, which
  are otherwise invisible.
- On a successful insert xDrip+ broadcasts `com.eveningoutpost.dexdrip.BgEstimate`. Listening for it
  is a reliable way to confirm from your own app that a reading actually landed.
- When watching logcat you may see a broadcast processed twice. xDrip+ registers this receiver both
  in its manifest and at runtime, and a `setPackage` broadcast matches both. The duplicate is removed
  by the timestamp check above, so only one reading is stored.
