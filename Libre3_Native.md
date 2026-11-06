---
title: xDrip & Libre 3/3+ native setup
description: "Guide to connecting xDrip directly to a Libre 3 or Libre 3 Plus sensor."
---

# xDrip & Libre 3/3+ (native)
[xDrip](../../) >> [Features](../Features_page.md) >> [Libre](./Libre_Page.md) >> xDrip & Libre 3/3+ native setup

> **Test build.** This connection has not yet been confirmed on a real sensor.  Use it alongside a meter or another way to see your glucose, not as your only source.
<br/>

xDrip talks to the sensor directly, by NFC and then Bluetooth.  No helper app (like OOP2) and no official app are needed.
If you only want to follow the official app, use [Web follower Libre](../Libre3_Follow.md) instead.
<br/>

### What you need

- An xDrip build that includes Libre 3 native.  [How to Download & Install](../Installation_page.md)
- A phone with NFC.
- Internet once, to download the app credential (see step 5).
<br/>

### Setup, once

1- Set [Hardware Data Source](../HardwareDataSource.md) to `Libre 3 / 3 Plus (native)`.
2- Settings >> Hardware Data Source >> NFC Scan features: turn on `Use NFC feature`.
3- Android settings: Bluetooth on and location on.  Allow xDrip location "All the time".  Android does not let xDrip find the sensor without it.
4- Set xDrip to unrestricted: allowed to run in background and excluded from [Battery optimization](../BatteryOpt.md).
5- xDrip shows the notification "No app credential".  Tap it, or open System Status >> Libre 3 >> Open, and tap `Download app credential`.  The screen should then say "App credential: downloaded".
6- Only if you will take over a sensor already running in the official app: Settings >> Less common settings >> Other misc options >> Libre 3 (native).  Enter your LibreView account ID and choose the official app that started the sensor (FreeStyle Libre 3 or Libre by Abbott).
The account ID is not your email.  It is the internal ID of the LibreView account that started the sensor, not a follower or caregiver account.  To find it, open System Status >> Libre 3 >> Open, tap `Look up account ID` and log in once with that account (needs internet).  xDrip fills in the ID and keeps neither the email nor the password.
<br/>

### Start or take over a sensor

Scan the sensor with NFC from xDrip's home screen.  xDrip explains what will happen and asks you to press OK and scan once more.  Nothing is sent to the sensor before that second scan.

- **New sensor:** xDrip starts it.  You see "Libre 3 sensor ready, connecting by Bluetooth", then the warm-up counts down on the home screen.
- **Sensor running in the official app:** xDrip takes it over.  The official app stops getting readings and alarms.  This needs the account ID from step 6.

Without an account ID, a new sensor belongs to this xDrip install only.  It can never move to the official app, and it is lost if xDrip is reinstalled or its data is cleared.
<br/>

### What to expect

- Readings every 5 minutes.  With `Libre 1-minute` on (Less common settings >> Other misc options), every minute.  Restart the phone after changing it.
- The values are the sensor's own.  xDrip does not calibrate them.  Setting `Smooth Libre 3 readings` (in Libre 3 (native)) is off by default.
- Gaps while the phone was out of range are filled from the sensor's history when it comes back.
- Do not run the official app on the same sensor at the same time.  Only one of them can be connected.
<br/>

### Troubleshooting

Open System Status >> Libre 3 >> Open.  It shows the sensor, the current stage and the last error.

| You see | Do this |
|---|---|
| No app credential | Tap `Download app credential` (needs internet). |
| Location is needed to find the sensor | Tap the notification and allow location "All the time". |
| The sensor did not accept the command | The account ID or official app in step 6 does not match the one that started the sensor. |
| xDrip already uses this sensor | Tap `Forget sensor`, then scan again. |
| Lost contact with the sensor | Scan again and hold the phone still. |
| No readings after a restart | Wait 10 minutes; xDrip reconnects by itself. |

To report a problem: on the Libre 3 screen tap `Copy diagnostics` and paste it into your post in [xDrip GitHub Discussions](https://github.com/NightscoutFoundation/xDrip/discussions), with a description of what you saw and when.  Copy it right away; it holds only the last day of log.
The diagnostics never contain your PIN, keys or account ID.  Do not post your exported settings file or your LibreView password.
