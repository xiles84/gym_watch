# Device runbook — Galaxy Watch

Everything here assumes `source scripts/env.sh` has been run first.

## Ground truth (verified 2026-09-09)

| | |
|---|---|
| Watch model | **SM-L705F** (Galaxy Watch 8 Classic) |
| `ro.build.version.release` | **16** — Wear OS 6 |
| `ro.build.version.sdk` | **36** |
| ABI | **armeabi-v7a** (32-bit ARM) |
| Samsung Health (watch) | **7.00.0.131** (targetSdk 37) |
| adb address | `192.168.15.140`, connect port `40647` |
| adb serial | `adb-RXGL40B4V8M-O5Py0F` |

So `targetSdk = 36`. `compileSdk` stays **37** because `android-37.0` is the only
platform installed — compiling ahead of the target is fine, targeting ahead of
a device you cannot test is not.

The API-36 split in `AndroidPermissions` is therefore the *live* path on this
watch: heart rate is `android.permission.health.READ_HEART_RATE`, not
`BODY_SENSORS`.

**A phone is often connected too** (SM-S918B, `192.168.15.122`). With two
devices attached, every adb command needs `-s`:

```bash
adb -s 192.168.15.140:40647 shell ...
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

Not yet verified: rotary bezel stepping, haptics, the rest-timer buzz at zero,
and the Ongoing Activity indicator on the watch face. All four need a human
wearing the watch — adb cannot feel a vibration or turn a bezel.

## Pairing, as actually done

mDNS discovery removes the need to read the IP off the watch:

```bash
adb mdns services      # shows _adb-tls-pairing._tcp while "Pair new device" is open
adb pair 192.168.15.140:<pairing-port> <6-digit-code>
adb connect 192.168.15.140:40647
```

The pairing port and the connect port are **different** — confirmed here:
pairing `35649`, connect `40647`.

## Phase 4 verified on device — 2026-09-09

| Check | Result |
|---|---|
| Permission prompt appears on first workout | pass — `GrantPermissionsActivity`, "access your physical activity" |
| API-36 permission path is the live one | pass — `health.READ_HEART_RATE`, not `BODY_SENSORS` |
| Weights workout starts via Health Services | pass |
| Live metrics | pass — **0:51 elapsed, 72 bpm** from the sensor |
| Foreground service switches type for a workout | pass — `types=0x00000100` (HEALTH), was `0x40000000` (SPECIAL_USE) |
| Ending returns to the pager, service stops | pass — `Exercise ended: ENDED reason=4` (USER_END), 0 service records |

Granting the permissions for testing (reversible):

```bash
adb -s <watch> shell pm grant com.gymwatch android.permission.ACTIVITY_RECOGNITION
adb -s <watch> shell pm grant com.gymwatch android.permission.health.READ_HEART_RATE
# undo:
adb -s <watch> shell pm revoke com.gymwatch android.permission.health.READ_HEART_RATE
```

Open the app straight on a page (0 chrono, 1 rest, 2 counter, 3 workouts):

```bash
adb -s <watch> shell am start -n com.gymwatch/.MainActivity --ei page 3
```

### Still not verified

- **Does the workout appear in Samsung Health?** Health Services recorded it, but
  where it surfaces is undocumented. Check Samsung Health on the phone for a
  Weights session around the test time and write the answer into LESSONS.md.
- **The conflict guard against a real Samsung Health workout.** The code path is
  unit-tested, but taking the slot from Samsung Health for real has not been
  tried. Start a workout in Samsung Health, then start one here: it must ask.
- Rotary bezel, haptics, the rest-timer buzz, and the watch-face indicator —
  all need a human wearing the watch.
