# Device runbook — Galaxy Watch

Everything here assumes `source scripts/env.sh` has been run first.

## Ground truth (fill in on first connect)

| | |
|---|---|
| Watch model | _not yet recorded_ |
| `ro.build.version.sdk` | _not yet recorded_ |
| `ro.build.version.release` | _not yet recorded_ |
| Samsung Health (watch) version | _not yet recorded_ |
| First paired | _not yet_ |

Record them with:

```bash
adb shell getprop ro.product.model
adb shell getprop ro.build.version.sdk
adb shell getprop ro.build.version.release
adb shell dumpsys package com.samsung.android.wear.shealth | grep versionName
```

`compileSdk`/`targetSdk` in `gradle/libs.versions.toml` are currently **37**,
because `android-37.0` is the only platform installed. If the watch reports a
lower level, only `targetSdk` needs to change — `compileSdk` may stay ahead.

## Pairing over Wi-Fi (Galaxy Watch has no USB port)

On the watch: **Settings > About watch > Software** — tap *Software version*
five times to unlock Developer options. Then **Settings > Developer options**
and turn on *ADB debugging* and *Debug over Wi-Fi*. It shows an IP.

Watch and PC must be on the same Wi-Fi network.

```bash
# One-time pairing (Wear OS 4+). Use the pairing port and code the watch shows
# under "Pair new device", NOT the connect port.
adb pair 192.168.x.x:PORT

# Then connect, using the port shown on the Debug over Wi-Fi screen.
adb connect 192.168.x.x:5555

adb devices -l
```

If `adb connect` succeeds but the device shows `unauthorized`, accept the
prompt on the watch face.

## Install and run

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch without touching the watch
adb shell monkey -p com.gymwatch -c android.intent.category.LAUNCHER 1
```

Uninstall: `adb uninstall com.gymwatch`

## Permissions

Health permissions cannot be pre-granted reliably from adb on Wear — grant them
in the on-watch dialog the first time. To reset and re-test the flow:

```bash
adb shell pm reset-permissions com.gymwatch
```

## Logs

```bash
adb logcat -c                                              # clear first
adb logcat -v time GymWatch:D HealthServices:D AndroidRuntime:E '*:S'
```

Health Services specifically:

```bash
adb logcat -v time | grep -iE 'healthservices|exerciseclient|whs'
```

## Manual checks that matter

These are the ones that catch real regressions; automated tests cannot.

1. **Chronometer survives sleep.** Start it, cover the screen until it sleeps,
   wait ~2 min, wake it. Elapsed time must be right. If it is short, something
   reintroduced a tick loop.
2. **Counter survives death.** Count to 12, `adb shell am force-stop com.gymwatch`,
   reopen. Must still read 12.
3. **Counter does not lose data at launch.** Set it to 12, force-stop, reopen and
   press "+" immediately. Must read 13, not 1. See lesson 10.
4. **Rotary bezel steps the counter.**
5. **Workout conflict is surfaced.** Start a workout in Samsung Health, then try
   to start one in this app. It must ask, not silently take over.
6. **Where does the record land?** After a Health Services workout, check both
   Health Connect and the Samsung Health app. Write the answer into
   `docs/LESSONS.md` — this is a known open question.
