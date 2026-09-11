# Device runbook — Galaxy Watch

Everything here assumes `source scripts/env.sh` has been run first.

## Ground truth (verified 2026-09-09)

| | |
|---|---|
| Watch model | **SM-L705F** — Galaxy Watch **Ultra** (47mm, LTE) |
| Rotating bezel | **None.** The bezel is static; there is a *touch* bezel only |
| `ro.build.version.release` | **16** — Wear OS 6 |
| `ro.build.version.sdk` | **36** |
| ABI | **armeabi-v7a** (32-bit ARM) |
| Samsung Health (watch) | **7.00.0.131** (targetSdk 37) |
| adb address | `192.168.15.140`; the connect port **changes on every reconnect** — use `adb mdns services` |
| adb serial | `adb-RXGL40B4V8M-O5Py0F` |

So `targetSdk = 36`. `compileSdk` stays **37** because `android-37.0` is the only
platform installed — compiling ahead of the target is fine, targeting ahead of
a device you cannot test is not.

**This entry said "Galaxy Watch 8 Classic" until 2026-09-09 and that was wrong.**
The Classic is the model with the physical rotating bezel; the Ultra has none.
The mistake was inherited by every UI decision that assumed one could be turned.
See `docs/LESSONS.md` #24. Verify the marketing name against the model number
rather than assuming it:

```bash
adb shell getprop ro.product.model      # SM-L705F
```

No health permissions are requested any more — the app reads no health data and
Samsung Health owns the workout record (`docs/LESSONS.md` #2). The only runtime
permission is `POST_NOTIFICATIONS`.

**A phone is often connected too** (SM-S918B, `192.168.15.122`). With two
devices attached, every adb command needs `-s`:

```bash
adb -s <watch> shell ...
```

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

The watch holds the **release** build. The debug APK is signed with a different
key and will not install over it (`docs/LESSONS.md` #27). Never `adb uninstall`
to get round that — it deletes the DataStore with every setting in it.

```bash
./gradlew build
adb install -r app/build/outputs/apk/release/app-release.apk

# Launch without touching the watch
adb shell monkey -p com.gymwatch -c android.intent.category.LAUNCHER 1
```

Open the app straight on a screen — by **name**, not index, since screens can
be reordered and hidden:

```bash
adb -s <watch> shell am start -n com.gymwatch/.MainActivity \
  --es screen REST_TIMER
```

Valid names: `CHRONOMETER`, `REST_TIMER`, `COUNTER`, `REST_AND_COUNTER`,
`WORKOUTS`, `MEDIA`. `PROFILES`, the old name, still lands on the workouts screen. An unknown or hidden one falls back
to the first visible screen.

## Permissions

Only `POST_NOTIFICATIONS`, asked for once on first launch. Declining it costs
the watch-face indicator and nothing else — the timers derive from the clock and
keep perfect time either way.

```bash
adb shell pm reset-permissions com.gymwatch    # re-test the first-launch prompt
```

## Logs

```bash
adb logcat -c                                              # clear first
adb logcat -v time GymWatch:D AndroidRuntime:E '*:S'
```

## Samsung Health shortcuts

The workouts screen opens Samsung Health with an **undocumented** intent — the
one its own watch-face complication sends (`docs/LESSONS.md` #28). If a Samsung
Health update breaks it, a tap falls back to Samsung Health's home screen rather
than doing nothing. Re-check it by hand after any Samsung Health update:

```bash
adb shell am start \
  -a com.samsung.android.wear.shealth.intent.action.START_WORKOUT \
  -n com.samsung.android.wear.shealth/.app.exercise.view.ExerciseActivity \
  -f 0x10008000 --es exercise.type WEIGHT_MACHINE
```

Expect Samsung Health's start screen for that exercise, play button showing.
Nothing is recorded unless start is pressed. If the names changed, refresh the
snapshot in `SamsungExercisesTest` from `aapt2 dump resources` and `dexdump`,
as #28 describes.

## Phone companion (media screen audiobooks)

The media screen's audiobook list comes from the phone app in `:phone`. It must
be the same app to the Data Layer as the watch's: `applicationId com.gymwatch`,
**release**-signed with the same key. A debug-signed phone app sees nothing and
reports nothing (`docs/LESSONS.md` #33). The phone is an SM-S918B (S23 Ultra),
also on Wi-Fi adb; with both attached every command needs `-s`.

```bash
./gradlew :app:assembleRelease :phone:assembleRelease
adb -s <phone> install -r phone/build/outputs/apk/release/phone-release.apk
adb -s <watch> install -r app/build/outputs/apk/release/app-release.apk
```

On the phone, open Gym Watch and tap **Allow access**. Notification access is
what lets it see Audible's session (#32). It's a security setting, so the user
switches it on, never a script. Check it:

```bash
adb -s <phone> shell settings get secure enabled_notification_listeners | tr ':' '\n' | grep gymwatch
adb -s <phone> shell dumpsys media_session | grep -A12 com.audible.application   # state + loaded title
adb -s <phone> shell dumpsys activity service com.google.android.gms/.wearable.service.WearableService \
  | grep "com.gymwatch:"                                                           # DataItem SET count
```

If Audible isn't running there's no session and nothing to record. Opening
Audible restores its loaded book paused, and that is enough to seed an empty list.

`adb shell input keyevent 126/127` plays and pauses the active session. The
session event log attributes the key to whatever app is in the foreground, not
to adb.

## Manual checks that matter

These are the ones that catch real regressions; automated tests cannot.

1. **Chronometer survives sleep.** Start it, cover the screen until it sleeps,
   wait ~2 min, wake it. Elapsed time must be right. If it is short, something
   reintroduced a tick loop.
2. **Counter survives death.** Count to 12, `adb shell am force-stop com.gymwatch`,
   reopen. Must still read 12.
3. **Counter does not lose data at launch.** Set it to 12, force-stop, reopen and
   press "+" immediately. Must read 13, not 1. See lesson 10.
4. **Each shortcut opens its exercise.** Tap each circle on WORKOUTS; Samsung
   Health must show that exercise's start screen, not its home screen.
5. **Reset asks only while counting.** Running chronometer: ↺ shows the dialog,
   ✕ keeps it running. Paused chronometer: ↺ resets with no dialog. Rest
   countdown: ↺ asks "Restart rest?" and ■ asks "Stop rest?", ✕ keeps it
   counting; left open past zero, the dialog closes.
6. **The rest alarm holds and repeats.** Needs someone wearing the watch: at zero
   it stays on REST OVER and buzzes every 3 s — *including with the screen
   covered* — until ■ or ↺, neither of which asks. ■ goes back to the presets;
   ↺ starts the same length again from full.
7. **The alarm is on time with the screen off.** Start a 1:00 rest, let the
   screen go off, keep the wrist still. The first buzz must come at 1:00, not
   when the wrist is next raised (`docs/LESSONS.md` #31). Pending alarm:
   `adb shell dumpsys alarm | grep -A3 com.gymwatch`.
8. **Back closes an editor, not the app.** Hold a preset, then press the back
   button: the presets come back with the old length.
9. **Rest + Sets stays in sync.** Start a rest and tap + there, swipe to REST
   and COUNTER: same countdown, same count. Stop it on REST, swipe back: idle.

## Verified on device — 2026-09-09

| Check | Result |
|---|---|
| App launches, all 4 screens render | pass |
| Chronometer counts | pass — 0:06 after 6 s |
| **Chronometer survives doze** | **pass — 0:06 → 1:32 across 60 s screen-off** (`mWakefulness=Dozing`) |
| Counter persists across force-stop | pass — 12 before, 12 after |
| Rest timer counts down with ring | pass — 1:30 → 1:25 |
| Foreground service starts | pass — `types=0x40000000` (SPECIAL_USE) |
| Fatal exceptions in session | 0 |

Not yet verified: haptics, the rest-timer buzz at zero, and the Ongoing Activity
indicator on the watch face. All need a human wearing the watch — adb cannot
feel a vibration. (Since 2026-09-10 the buzz timing is checked from
`dumpsys vibrator_manager` instead; see below.)

## Verified on device — themed skins, 2026-09-10

Release build with the Dragon Ball, Sailor Moon and Spy × Family skins.

| Check | Result |
|---|---|
| Dragon Ball: rest presets, countdown, REST OVER, counter, workouts, workout picker, rest editor, settings, stop dialog | pass — every label readable over the dimmed art |
| Sailor Moon: the same, plus the chronometer | pass |
| Rest editor pickers over a wallpaper | pass after a fix — the pickers' black fade drew two dark bars across the art; it is transparent over a wallpaper now |
| REST OVER ring and ■ | pass — full ring on its black band, ■ in the rest colour |
| Original skin unchanged | pass |
| Spy × Family on the watch | **not verified on device** — scripted runs kept falling out of the app (`docs/LESSONS.md` #23). Covered by `WallpaperContrastTest` and the review crops only |

Switch skins by hand under **Settings > SKIN**; there is no deep link for it.

## Verified on device — media screen and phone companion, 2026-09-10

Release 0.3.0 on the watch and the phone (same signing certificate,
SHA-256 `0907add2…4a48d6`), Audible 26.34.07 on the phone.

| Check | Result |
|---|---|
| Media screen appended as the 6th page, Spotify and Phone icons read from the apps | pass |
| Empty list seeded with the book loaded in Audible (paused) | pass — *Mimic & Me, 5h 19m left* on phone and watch |
| Tap a book that isn't loaded | pass — watch shows *starting on phone…*; Audible switched to it in ~11 s |
| Tap the book already loaded | pass after a fix — a search for it put Audible in ERROR; now a plain play, PLAYING in 3 s |
| Spotify shortcut | pass — `com.spotify.music/com.spotify.wear.main.MainActivity` |
| Phone shortcut | pass — `com.samsung.android.mediacontroller/…SelectDeviceActivity` |
| A book counted after a minute of listening updates the watch live | pass, 2026-09-11 — *Galaxy Outlaws* went on top of the watch's list while the user listened |
| …and the phone's own screen | **fail, then fixed, not yet re-verified** — the phone got no event for its own write (`docs/LESSONS.md` #33) |
| A book recorded while Audible reported no length | lost its "time left"; a gap now keeps the known value (`RecentAudiobooksTest`) |
| **Capture** button on the phone | installed 2026-09-11, **not verified on device** — the phone was in use; covered by `ListeningTrackerUseCaseTest` |
| Audible not running when a book is tapped (media-button wake-up) | **not verified on device** |
| Media screen over a themed skin | **not verified on device** — borrows the workouts wallpaper; `WallpaperContrastTest` passes |

The switch that landed on PAUSED rather than PLAYING was not ours. Audible
took audio focus and lost it 0.2 s later to a transient request from another
app, and Audible pauses on a duck because it plays speech (`dumpsys audio`
focus history).

## Verified on device — late alarm, back button, Rest + Sets, 2026-09-10

Release build, Sailor Moon skin, watch off the charger (`mIsPowered=false`).

| Check | Result |
|---|---|
| Rest + Sets renders, appended as the 5th page on an upgraded install | pass |
| 1:00 rest books an exact wake-up | pass — `ELAPSED_WAKEUP … WakeUpReceiver`, `exactAllowReason=policy_permission` |
| **Buzz at zero with the screen off** | **pass — alarm delivered 21:15:25.149, first vibration 21:15:25.200, then every 3.0 s** (`dumpsys vibrator_manager`: `usage: ALARM`, `finished`) |
| REST OVER on Rest + Sets: half ring full, big ■ in the rest colour | pass |
| ■ stops it: idle, no wake lock, no pending alarm | pass — `mWakeLockSummary=0x0` |
| + on Rest + Sets shows on COUNTER | pass — 1 on both |
| Back in the rest editor returns to the page, app stays resumed | pass — `input keyevent KEYCODE_BACK` |

The vibration history is the useful proof here: `dumpsys vibrator_manager`
lists every vibration with its start time and whether it `finished` or was
`ignored_*`, which is as close as adb gets to feeling one. The screen was
dozing when the rest started; whether it was still off at zero is not proven.

## Pairing, as actually done

mDNS discovery removes the need to read the IP off the watch:

```bash
adb mdns services      # shows _adb-tls-pairing._tcp while "Pair new device" is open
adb pair 192.168.15.140:<pairing-port> <6-digit-code>
adb connect 192.168.15.140:<connect-port>
```

The pairing port and the connect port are **different**, and the connect port
changes each time the watch reconnects: `40647`, then `46027`, then `36367` on
2026-09-10 alone.

## Health Services — removed 2026-09-09

Phase 4 did work on device: a Weights exercise started, live heart rate arrived
(**0:51, 72 bpm**), and the foreground service switched to `types=0x00000100`
(HEALTH). It was all deleted anyway.

The open question in this section used to be *"does the workout appear in
Samsung Health?"* The answer, from wearing it: **no, and it cannot.** Three
independent blocks — see `docs/LESSONS.md` #2. Samsung Health owns the workout
record, and the app opens it on the exercise.

So there are no health permissions to grant, no exercise slot to contend for,
and `pm grant android.permission.health.READ_HEART_RATE` no longer applies.

### Still not verified

- Haptics, the rest-timer alarm, and the watch-face indicator — all need a human
  wearing the watch.
- Whether the **touch** bezel feeds `onRotaryScrollEvent` on this model. Nothing
  depends on it: the counter's rotary support is additive, and `Picker` handles
  both swipe and rotary itself. Worth knowing, not worth blocking on.
