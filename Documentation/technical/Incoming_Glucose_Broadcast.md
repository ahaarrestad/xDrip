# Integration with xDrip via Broadcast Intent

## Overview

This page is for developers of Android apps and sensors that send glucose readings to xDrip. It
covers the Nightscout Emulation receiver, and only the insertion of glucose records.

**Address the broadcast to xDrip explicitly.** That is the one thing worth taking from this page.

On the xDrip side the user sets `Hardware Data Source` to `640G / EverSense`. That is their setup
rather than yours — you need it only to test against a real install — but until it is done the
receiver rejects everything.

## Sending the broadcast

### Address it to xDrip

```java
Intent intent = new Intent("com.eveningoutpost.dexdrip.NS_EMULATOR");
intent.setPackage("com.eveningoutpost.dexdrip");
```

`setPackage` is required. Since Android 8 (API 26) manifest-declared receivers no longer get
implicit broadcasts, and xDrip targets API 26.

An implicit broadcast — one sent without `setPackage` — will appear to work, because xDrip also
registers this receiver at runtime. It stops working the moment the xDrip process is not running,
and the reading then goes nowhere with no error on either side. That is what makes an integration
look intermittent rather than broken. Always name the package.

If your app targets API 30 or later, package visibility rules apply and `setPackage` alone is not
enough. Declare xDrip in your manifest, as a direct child of `<manifest>`:

```xml
<queries>
    <package android:name="com.eveningoutpost.dexdrip" />
</queries>
```

No permission is required to send this broadcast.

### Extras

- `"collection"` — set to `"entries"`. **Without this extra the broadcast is discarded silently.**
- `"data"` — the reading, as a JSON array **string**.

### The reading

| Attribute | Required | Value |
|-----------|----------|-------|
| `"type"` | Yes | `"sgv"` for a sensor glucose value record. |
| `"date"` | Yes | Timestamp of the reading, in milliseconds since the Unix epoch, on the phone's wall clock. |
| `"sgv"` | Yes | The glucose value in **mg/dL**. |
| `"direction"` | No | Trend, as one of the strings below. |

If `"type"`, `"date"` or `"sgv"` is missing or unparseable, the reading is dropped. An unrecognised
`"direction"` is not fatal — the reading is stored with a flat trend.

| Value | Slope |
|-------|-------|
| `DoubleUp` | +4 |
| `SingleUp` | +3.5 |
| `FortyFiveUp` | +2 |
| `Flat` | 0 |
| `FortyFiveDown` | -1 |
| `SingleDown` | -2 |
| `DoubleDown` | -3.5 |

`NONE`, `NOT_COMPUTABLE`, `OUT_OF_RANGE` and their space-separated forms are accepted and map to
zero. These are fixed representative slopes in mg/dL per minute, not range boundaries; see
`BgReading.slopefromName()` for the current mapping.

Two rules that are easy to miss:

- **Send mg/dL.** The value is stored as given, as both the calculated and the raw value. xDrip does
  not calibrate it and does not range-check it, so a mmol/L value is stored unchanged and displays
  as a critical low.
- **One reading per broadcast.** The array length is what distinguishes this payload from an
  internal one: more than one element is routed to a different parser and nothing is inserted.

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

The same call, wired up against the receiver, is in the unit test source tree in
`NSEmulatorReceiverTest.bgReadingExampleBroadcast()`.

## How often to send

xDrip discards a reading whose timestamp falls too close to one it already holds. The margin is four
fifths of the sample period, applied either side of the timestamp:

| xDrip setting | Sample period | Readings rejected if closer than |
|---|---|---|
| default | 5 minutes | 4 minutes |
| `640G/Eversense 1-minute` enabled | 1 minute | 48 seconds |

Send at most one reading per sample period. A sensor reading faster than that loses readings
silently, and the result looks like a slow trace rather than an error.

Backfill works — one broadcast per reading, spaced wider than the margin — but every inserted
reading is treated as live: it fires alerts and reaches followers and watches as though it had just
arrived. Do not send timestamps in the future.

## When nothing arrives

Most failures on this path are silent, and your app gets no error either way:

| Cause | Result |
|-------|--------|
| `"collection"` extra missing | Discarded |
| More than one reading in the array | Routed to another parser, nothing inserted |
| Sent implicitly, xDrip process not running | Never delivered |
| App targets API 30 or later without the `<queries>` entry | Never delivered |
| Reading too close in time to an existing one | Discarded as a duplicate |
| xDrip not set up to accept the broadcast | Rejected, recorded in the Event Log |
| Malformed JSON, or a missing `"date"` / `"sgv"` | Rejected, recorded in the Event Log |

xDrip's **Event Log**, visible to the user in the app, is the only thing that distinguishes *not
delivered* from *delivered and rejected*. Ask a user to check it before assuming nothing arrived.

To debug against a live install:

- The logcat tag for this receiver is `jamorham nsemulator`.
- **Extra tags for logging** in Advanced Settings promotes that tag's debug lines into the in-app
  log. This is the only way to see duplicate rejections.
- On a successful insert xDrip broadcasts `com.eveningoutpost.dexdrip.BgEstimate`. Listening for it
  confirms from your own app that a reading actually landed.

One visible side effect is not a failure: if xDrip has no active sensor, your broadcast produces the
toast *"Please use: Start Sensor from the menu for best results!"*. The reading is still stored, but
users may see the toast and report it against your app.
