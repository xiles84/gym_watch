# Lessons learned

Append-only. Newest last. One entry per problem that cost more than one attempt.
If an entry turns out to be wrong, correct it in place rather than adding a
contradicting entry.

Entries 1-7 were established by research before any code was written, so they
never had to be discovered the expensive way.

---

## 1 — A watch face cannot hold a counter or a chronometer
*2026-09-09 · platform*

**Symptom (anticipated):** the obvious design is "put it all on the watch face".

**Cause:** Watch Face Format bundles are resource-only. Google's setup docs
require `android:hasCode="false"` and state the bundle must be *"completely
separate from the Android App Bundle that contains your Wear OS app's logic"*.
WFF is declarative XML with no variables, no persistent state, and no way to
mutate a value on tap. Legacy AndroidX canvas watch faces could do it, but they
cannot be installed from Play as of 2026-01-14.

**Fix:** the app is the deliverable. A WFF face is a separate, optional APK that
shows the time and uses `Launch` (Wear OS 4+) to open the app.

**Avoid it by:** treating "can the watch face do X" as answered — it can render
and it can launch. Nothing else.

---

## 2 — A third-party watch app cannot get a workout into Samsung Health
*2026-09-09 · platform · corrected 2026-09-09 after testing on the watch*

**This entry originally said** "our app records the workout itself with Health
Services `ExerciseClient`, which is officially supported." That was wrong, and
it cost a whole phase of work. `ExerciseClient` is a *live metrics* API. It
persists nothing.

**Symptom:** workouts started in the app never appeared in Samsung Health.

**Cause:** three separate blocks, each sufficient on its own.

1. **`ExerciseClient` saves nothing.** Google's own guide says to persist the
   stream yourself with Room and upload it with WorkManager. Ending an exercise
   discards it.
2. **Health Connect does not run on Wear OS.** Samsung's Health Connect FAQ:
   *"The Health Connect application can be installed on Android mobile devices.
   It does not support Wear OS devices."* The bridge into Samsung Health exists
   only on the phone.
3. **Samsung Health has no third-party write API.** Integration is curated
   partnerships (Strava, Technogym). There is nothing to call.

There is no *documented* way into a specific exercise, and community attempts
at `health://` and similar URI schemes do not work. There is an undocumented
one, though — the intent Samsung Health's own watch-face complication sends —
found on 2026-09-10 by reading the APK (#28). Opening an exercise's start screen
is still not recording it: the user presses start, and Samsung Health records.

**Worse than useless:** the platform allows one exercise device-wide (#3). If
Samsung Health was recording and the user tapped a workout in our app, our
conflict dialog offered to *end Samsung Health's session* — the feature was
wired to destroy the very history it was supposed to create.

**Fix:** the app stopped tracking exercises. Samsung Health owns the workout
record; `CompanionHealthAppPort` just opens it. The three slots became
*profiles* that configure what we can actually own — rest lengths and the
counter label. `:adapters:driven:health` was deleted, and with it the health
foreground-service type (#19), the heart-rate permission split (#5) and the
`RestrictedApi` suppressions (#21).

Since 2026-09-10 the profiles are gone as well: one set of rest presets, and the
three slots are shortcuts that open Samsung Health *on that exercise* (#28).

**Avoid it by:** asking "what persists this, and who can read it?" *before*
building on a platform API. A streaming API and a recording API are not the same
thing, however similar the names look.

---

## 3 — Health Services allows exactly one exercise at a time, device-wide
*2026-09-09 · platform*

**Cause:** the constraint is global, not per-app. If Samsung Health is tracking
a workout and we call `startExerciseAsync`, theirs ends.

**Fix (now):** the app does not start exercises at all, so it can never take the
slot. This constraint is *why* — see #2. The rule survives its own
implementation: if a health backend is ever reintroduced, the slot belongs to
whoever is already using it.

**Originally:** `WorkoutUseCase.requestStart` checked `ownership()` and returned
`StartOutcome.NeedsConfirmation` rather than starting. That code is gone, along
with the conflict dialog that offered to end Samsung Health's session.

---

## 4 — The foreground service must be alive through the *prepare* phase too
*2026-09-09 · platform*

**Cause:** Health Services ends the exercise with
`ExerciseEndReason.AUTO_ENDED_PERMISSION_LOST` if no foreground service is
running — and the requirement starts at `prepareExerciseAsync`, not at
`startExerciseAsync`.

**Fix:** start the service before preparing sensors, stop it after
`endExerciseAsync` completes.

**Note (2026-09-09):** the app no longer runs exercises (#2), so nothing here is
live code. Worth keeping because it was also violated: `GymApp` awaited
`requestStart` and only *then* started the service, i.e. exactly backwards. If a
health backend ever returns, that ordering is the first thing to get right.

---

## 5 — Health permissions split at API 36
*2026-09-09 · platform*

API 33-35: `BODY_SENSORS`, `BODY_SENSORS_BACKGROUND`.
API 36+: `android.permission.health.READ_HEART_RATE`,
`android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND`, and the legacy two
declared with `android:maxSdkVersion="35"`.
`ACTIVITY_RECOGNITION` is needed at every level for steps/calories/distance.
`ACCESS_FINE_LOCATION` only if requesting location.

**Avoid it by:** declaring both sets with `maxSdkVersion` on the legacy pair,
and resolving which to *request* at runtime from `Build.VERSION.SDK_INT`.

**Note (2026-09-09):** none of these are declared any more. The app reads no
health data (#2), so `AndroidPermissions` and `HealthPermission` were deleted and
`POST_NOTIFICATIONS` is the only runtime permission left. Kept for the API-level
split itself, which is the part that is easy to get wrong.

---

## 6 — Read elapsed exercise time from the checkpoint, not a local counter
*2026-09-09 · platform*

Use `(now - checkpoint.time) + checkpoint.activeDuration` from
`ExerciseUpdate.activeDurationCheckpoint`. A local tick counter drifts and
double-counts across pause/resume.

---

## 7 — Wear OS to API level, and do not assume the newest
*2026-09-09 · platform*

Wear OS 4 = 33, 5 = 34, 5.1 = 35, 6 = 36, **7 = 37 (Android 17)**.

**Avoid it by:** reading `adb shell getprop ro.build.version.sdk` from the actual
watch rather than assuming. See `docs/DEVICE-RUNBOOK.md`.

---

## 8 — This machine's toolchain, verified
*2026-09-09 · build*

Nothing is on `PATH`; everything ships inside Android Studio.

- Android Studio `2026.1.4`, JDK is its bundled JBR: **OpenJDK 25.0.3**
- SDK at `%LOCALAPPDATA%/Android/Sdk`, **only `android-37.0` installed** — so
  `compileSdk = 37`. Build-tools 36.0.0.
- Gradle **9.5.0** was already in the wrapper cache; AGP latest stable **9.4.0**.
- Kotlin 2.4.20 compiles fine on JDK 25 when targeting `JvmTarget.JVM_17`.
  Do **not** set `jvmToolchain(17)` — no JDK 17 is installed and auto-provision
  is not configured, so it would fail.

**Avoid it by:** running `source scripts/env.sh` before any gradle command, and
never hardcoding a library version from memory — resolve it against the repo.

---

## 9 — Bash heredocs here collapse a doubled backslash
*2026-09-09 · tooling*

**Symptom:** Kotlin failed with `Unsupported escape sequence` and
`Incorrect character literal` on code written via `cat > file <<'EOF'`.

**Cause:** a doubled backslash written into a quoted heredoc arrived in the file
as a single one, so `replace(BACKSLASH BACKSLASH, "/")` became an invalid
literal. The resulting parse error cascaded into a dozen misleading
"unresolved reference" errors elsewhere in the file.

**Fix:** avoid backslash escapes entirely in generated Kotlin. For Konsist,
scoping by *package* (`hasPackage("com.gymwatch.core..")`) instead of by file
path removed the need for path separators altogether — and is OS-independent,
which is better anyway.

**Avoid it by:** when a compile produces many unresolved-reference errors at
once, look for a syntax error higher up first.

---

## 10 — Never base a write on a stateIn StateFlow's seeded default
*2026-09-09 · correctness*

**Symptom:** `CounterUseCaseTest` — reset a counter holding 12, got 12 back
instead of 0.

**Cause:** `repository.counter.stateIn(scope, Eagerly, Counter())` seeds the
flow with a fabricated `Counter()` (value 0) until the first real emission
arrives. `mutate` read `state.value`, so a tap landing before that emission
transformed the *default* rather than the stored value.

On the watch this is data loss, not a test artifact: pressing "+" a beat after
launch would have written 1 over a saved 12.

**Fix:** `mutate` now reads `repository.counter.first()` inside the coroutine
and transforms that. `WorkoutUseCase.toggleFavourite` had the identical bug and
got the identical fix.

**Avoid it by:** treating a `stateIn` StateFlow as display-only. Any read that
feeds a write goes to the source.

---

## 11 — An architecture rule must not police itself
*2026-09-09 · testing*

**Symptom:** the "core never reaches for a wall clock" rule failed on
`ClockPort.kt`, whose KDoc names `System.currentTimeMillis()` in order to
explain why it is banned.

**Cause:** the rule matched raw file text, comments included.

**Fix:** strip comment lines before matching, and scope the rule to the `main`
source set so the test file cannot match itself either.

**Also learned:** `KoClassDeclaration` has no `hasPackage` — reach classes via
`file.classes()` on already-scoped files. And a rule that scopes to nothing
passes vacuously, so `ArchitectureTest` asserts the scan found files at all.

---

## 12 — The compiler is the real enforcer; Konsist is the backstop
*2026-09-09 · testing*

Verified by deliberately adding `import android.os.SystemClock` to
`core/domain/.../Counter.kt`. The build failed at
`:core:domain:compileKotlin` with `Unresolved reference 'android'` — the
architecture test never even ran, because the module could not compile.

That is the intended behaviour and the reason the core is `kotlin("jvm")`
rather than an Android library: the boundary is enforced by the classpath, and
the feedback arrives in seconds.

`ArchitectureTest`'s android-import rule therefore covers a narrower case: it
fires if someone adds an Android dependency to `core/*/build.gradle.kts`,
making such an import compile. Both are worth keeping; do not mistake one for
the other when reporting what caught a regression.

The other rules are not redundant — `no wall clock`, `ports are interfaces`,
and `no adapter imports` are all things the compiler will happily allow. Two of
them fired during initial development, which is how they earned their place.

---

## 13 — AGP 9 has built-in Kotlin; applying the Kotlin Android plugin is an error
*2026-09-09 · build*

**Symptom:** every Android module failed at configuration with
`The 'org.jetbrains.kotlin.android' plugin is no longer required for Kotlin
support since AGP 9.0.`

**Fix:** Android modules declare only `com.android.library` /
`com.android.application`. Kotlin comes from AGP. The Compose compiler plugin
(`org.jetbrains.kotlin.plugin.compose`) is still applied separately.

`:core:domain` and `:core:application` are unaffected — they use
`org.jetbrains.kotlin.jvm`, which is a different plugin and still required.

**Also:** `android { kotlinOptions { jvmTarget = ... } }` does **not** exist in
AGP 9's new DSL — that block is gone, not merely deprecated. Setting
`compileOptions` source/target to 17 is enough; AGP aligns the Kotlin target
itself. Do not reintroduce a `kotlinOptions` or `kotlin { compilerOptions }`
block in an Android module.

---

## 14 — local.properties needs forward slashes on Windows
*2026-09-09 · build*

**Symptom:** `Could not determine the dependencies of task
':...:extractDebugAnnotations'. java.io.IOException: Invalid file path`, thrown
from `SdkLocator.validateSdkPath`.

**Cause:** `local.properties` is a Java `.properties` file, where a single
backslash is an escape character. `sdk.dir=C:\Users\xiles\...` parsed as
`C:Usersxiles...` and failed validation. The error names an annotation task, not
the SDK path, which sends you looking in the wrong place.

**Fix:** `sdk.dir=C:/Users/xiles/AppData/Local/Android/Sdk`. Forward slashes
work on Windows and sidestep escaping entirely.

---

## 15 — AGP and Gradle versions are tightly coupled; check the pair
*2026-09-09 · build*

AGP **9.4.0** requires Gradle **9.6.0** minimum — not the 9.1.0 that the AGP
9.0.0 release notes quote, and not the 9.5.0 that happened to be in the wrapper
cache. The failure is explicit and tells you the exact version to use, so read
it rather than downgrading AGP by guesswork.

Current pinned pair: **AGP 9.4.0 + Gradle 9.6.0**, on JDK 25 (Android Studio's
JBR), `compileSdk`/`targetSdk` 37, `minSdk` 33.

---

## 16 — Lint's ObsoleteSdkInt advice on the launcher icon breaks the build
*2026-09-09 · build*

**Symptom:** lint warned that `res/mipmap-anydpi-v26` is unnecessary because
minSdk is 33 and said to merge it into `mipmap-anydpi`. Doing so failed the
build with `AAPT: error: resource mipmap/ic_launcher not found` — on *release*
resource processing as well as debug, so it was not a stale-cache artifact
(verified with a clean build).

**Cause:** AAPT2 does not resolve an `adaptive-icon` XML from a plain
`mipmap-anydpi` folder. The `-v26` qualifier is required regardless of minSdk.

**Fix:** keep `mipmap-anydpi-v26`, and suppress the rule *only for that path* in
`app/lint.xml`. Do not disable `ObsoleteSdkInt` project-wide — it correctly
caught a dead `SDK_INT >= S` branch in `AndroidHaptics` in the same run.

**Avoid it by:** treating lint suggestions as hypotheses. Build after taking one.

---

## 17 — FLAG_ACTIVITY_NEW_TASK / CLEAR_TOP break Recents on Wear OS
*2026-09-09 · correctness*

Lint's `WearRecents` check flagged both places we built an Intent for a
`PendingIntent`. On Wear these flags interfere with the Recents behaviour that
the Ongoing Activity indicator depends on — and a `PendingIntent.getActivity`
target does not need them in the first place.

**Fix:** removed the flags. Only add `NEW_TASK` when actually calling
`startActivity` from a non-Activity context.

---

## 18 — Declare a permission in the module that uses it
*2026-09-09 · build*

**Symptom:** `:adapters:driven:platform:lintDebug` failed with
`Missing permissions required by Vibrator.vibrate: android.permission.VIBRATE`,
even though `:app` declared it.

**Cause:** lint analyses each module in isolation. A library that calls a
permission-guarded API must declare the permission in its own manifest;
manifest merging still carries it up to the app.

**Fix:** `VIBRATE` and `POST_NOTIFICATIONS` moved into the platform adapter's
manifest, `foregroundServiceType="health"` lives in the service adapter's. The
app manifest keeps only what the app itself needs. This is better design anyway
— a module that cannot forget its own requirements.

**Related:** the persistence module keeps `gymDataStore` and the three
`DataStore*` classes `internal`, exposing only a `PersistenceAdapters` factory
that returns ports. The composition root never learns DataStore exists, so
swapping storage is genuinely a one-module change. The compiler enforced this
by rejecting the first attempt at wiring it.

---

## 19 — FGS type "health" needs a *granted* runtime permission, and a timer must not use it
*2026-09-09 · correctness · found on device*

**Symptom:** first launch on the watch looked fine, but tapping start on the
chronometer crashed the app straight back to the watch face, and
`dumpsys activity services` showed `Restarting ServiceRecord`.

**Cause:**

```
java.lang.SecurityException: Starting FGS with type health targetSDK=36
  requires permissions: allOf=[FOREGROUND_SERVICE_HEALTH]
  anyOf=[ACTIVITY_RECOGNITION, HIGH_SAMPLING_RATE_SENSORS,
         health.READ_HEART_RATE, health.READ_SKIN_TEMPERATURE,
         health.READ_OXYGEN_SATURATION]
```

Declaring the permissions in the manifest is **not** enough — one of the `anyOf`
set must be *granted at runtime* before `startForeground(..., TYPE_HEALTH)`. We
had granted nothing yet.

The deeper mistake was design, not configuration: a chronometer and a rest timer
have nothing to do with health, so hard-coding `health` coupled every timer to
permissions it does not need.

**Fix:** the service takes the type per session — `SPECIAL_USE` for timers
(with the required `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` property in the manifest),
`HEALTH` only while a Health Services exercise runs, in phase 4, after
permissions are granted. Verified on device: `types=0x40000000` is
`FOREGROUND_SERVICE_TYPE_SPECIAL_USE`.

**Update (2026-09-09):** the health branch is gone — the app no longer runs
exercises (#2), so the type is unconditionally `SPECIAL_USE` and
`FOREGROUND_SERVICE_HEALTH` is no longer declared. The design point stands and
was the right call twice over: a chronometer and a rest timer are not health
tracking, and saying so in the manifest is what made removing the health path a
one-line change instead of an audit.

**And:** `startForeground` is now wrapped in try/catch. If the platform refuses,
the service stops itself and the app carries on — the timers are clock-derived
and keep perfect time regardless; only the notification is lost. Never let a
notification failure take down the app.

---

## 20 — A black screencap usually means the display slept, not a render failure
*2026-09-09 · tooling*

`adb exec-out screencap -p` returns an all-black PNG (~2 KB for 480x480) when
the watch display is off. The watch's default `screen_off_timeout` is **30 s**,
so this happens constantly while testing.

Check `adb shell dumpsys power | grep mWakefulness` — `Dozing` means the screen,
not your UI, is the problem. A real capture of these screens is ~10 KB.

While testing:

```bash
adb -s <watch> shell settings put system screen_off_timeout 600000   # 10 min
# ... and put it back to 30000 when done
```

Also: batching `input swipe; input swipe; input tap` in one shell call fires
them faster than Compose can settle, and the taps land on the wrong page. Put a
real pause between UI events.

---

## 21 — Health Services' status constants are @RestrictTo(LIBRARY)
*2026-09-09 · build*

**Symptom:** ten lint `RestrictedApi` errors, e.g.
`Companion.OWNED_EXERCISE_IN_PROGRESS can only be accessed from within the same
library (androidx.health:health-services-client)`.

**Cause:** in 1.1.0-rc02 the *Kotlin companion objects* of
`ExerciseTrackedStatus` and `ExerciseEndReason` carry
`@RestrictTo(Scope.LIBRARY)`, even though the values are plain
`public static final int` on the interfaces and Google's own documentation uses
them by name. Reading `exerciseTrackedStatus` and comparing it is the only way
to use the API. A packaging bug, not a real boundary.

**Fix:** `@SuppressLint("RestrictedApi")` on the two members that compare them,
with the reason inline. Do not disable the check project-wide — it is a useful
rule everywhere else.

**Note (2026-09-09):** moot here — `:adapters:driven:health` was deleted with the
exercise tracking (#2), and lint is now clean with no suppressions anywhere.

---

## 22 — Git Bash rewrites device paths in adb arguments
*2026-09-09 · tooling*

**Symptom:** `adb pull /sdcard/w1.png dest.png` failed with
`failed to stat remote object 'C:/Program Files/Git/sdcard/w1.png'` — even
though `adb shell ls /sdcard/w1.png` showed the file.

**Cause:** MSYS path conversion rewrites any argument that looks like a Unix
absolute path into a Windows path. It applies to the *device* path, which is not
a host path at all. Paths inside a quoted `adb shell "..."` survive, which makes
it look inconsistent.

**Fix:** `export MSYS_NO_PATHCONV=1` before adb commands that take device paths
(or write `//sdcard/...`).

---

## 23 — Driving Wear UI over adb needs one shell call, not many
*2026-09-09 · tooling*

Three things fight you when automating a real watch:

1. **Doze.** The screen sleeps in ~30 s and the watch returns to its face, so
   later taps land on the watch face — one launched the weather app mid-test.
   `settings put system screen_off_timeout` is **not** honoured on Wear;
   `svc power stayon true` only helps while charging.
2. **Samsung Freecess** freezes the app process between commands
   (`FZ : com.gymwatch, reason: LEV`).
3. **Notifications steal focus.** A Google survey card appeared over the app and
   silently ate several swipes.

**What works:** put wake, launch, navigation, tap and `screencap` into a *single*
`adb shell "...; sleep 1; ..."` so the whole interaction happens inside one wake
window, then pull the PNG afterwards.

**Better still:** `MainActivity` now accepts `--ei page N` to open straight to a
screen, which removed three fragile swipes from every test. That is not test-only
scaffolding — the Ongoing Activity indicator and the tile both need it, so it
earns its place in the app.

**Also:** a black 1975-byte screencap means the screen is off (lesson 20); a real
one here is 10–70 KB, and 110–290 KB over a themed skin's wallpaper.

**And wait after waking (found 2026-09-10).** A gesture sent straight after
`input keyevent KEYCODE_WAKEUP` is dropped while the screen and the frozen
process come back, and nothing reports it. In one run a skin tap was lost, and
every screenshot after it showed the *previous* step's screen — so the whole
run looked plausible frame by frame. Wake, `sleep 2`, then act. Identical byte
counts for screenshots of different screens mean the UI did not move.

---

## 24 — The watch has no rotating bezel; the runbook named the wrong model
*2026-09-09 · platform · found by the user*

**Symptom:** the user reported "my watch doesnt have a bezel" — after the rest
timer and counter had both been built around turning one.

**Cause:** `docs/DEVICE-RUNBOOK.md` recorded **SM-L705F** as a "Galaxy Watch 8
Classic". It is a **Galaxy Watch Ultra**. The Classic is the model with the
physical rotating bezel; the Ultra's bezel is static. It has a *touch* bezel —
a fingertip dragged around the rim — which is a different interaction and a poor
one with sweaty hands mid-set.

The model name was never verified; only `ro.build.version.sdk` and friends were
read off the device, and the marketing name was filled in by assumption. Every
later decision inherited it.

**Consequence:** `Rotary.kt` and `onRotaryScrollEvent` were the *only* way to
change the rest length. On this hardware there was effectively no way to set it
at all, which is what prompted the rest-preset feature in the first place.

**Fix:** every value is now settable by touch. Rest lengths are three tappable
presets, edited with a two-column `Picker`/`PickerGroup` wheel. `rotaryStepper`
survives only as an additive bonus on the counter — if the touch bezel does feed
rotary events it works, and nothing breaks if it does not. `Picker` brings
`PickerDefaults.rotarySnapBehavior` for free, so that screen needs no rotary code
of its own. The `↻ bezel` on-screen hints were removed: they described hardware
that is not there.

**Avoid it by:** verifying the marketing name against the model number, not the
other way round, and treating "which physical inputs does this device have" as
device ground truth to be checked (#7) rather than inferred from a product line.
`ro.product.model` gives the number; the number has to be looked up.

---

## 25 — Wear layout only fails on a round screen, and Picker eats the screen
*2026-09-09 · ui · found on device*

Three separate faults, all invisible until the APK was on the watch.

**Picker fills whatever height it is given.** The rest preset editor rendered
its title jammed against the top, the columns shoved off-centre, and the hint
and confirm button nowhere at all. `Picker` is a scrollable list: inside a
`Column` with `Arrangement.Center` it takes the whole viewport and evicts its
siblings. **Always give a Picker an explicit height** — `Modifier.height(92.dp)`
shows the selected option plus one either side.

**PickerGroup centres the *selected* column.** With minutes selected, the pair
sat right of centre and the read-only seconds column was vertically misaligned
against it. That is the system time-picker idiom, and it is wrong for setting a
duration where both fields matter equally. Two plain `Picker`s in a `Row` with a
`Text(":")` between them keeps both columns live, aligned and flickable, and
costs only the focus handling that a touch-driven watch does not need anyway
(#24).

**A round screen clips full-width rows at top and bottom.** A `Column` +
`verticalScroll` looks correct in a preview and loses its first and last rows on
the device. `ScalingLazyColumn` is the component that knows about the curve — it
pads for it and scales items toward the rim. Use it for any list on Wear.
Set `autoCentering = null` for a short fixed list, or it centres the first item
and leaves half a screen of black above it.

**Avoid it by:** never trusting a Wear layout that has not been screenshotted on
the device, and reaching for the Wear component before the generic Compose one.
`adb exec-out screencap` costs seconds; see #20 and #23 for driving it.

---

## 26 — A running Activity only sees a new intent if it is `singleTop`
*2026-09-09 · correctness · found on device · corrected and verified 2026-09-10*

**Symptom:** with the app already open,
`am start -n com.gymwatch/.MainActivity --es screen REST_TIMER` printed
`Activity not started, intent has been delivered to currently running top-most
instance` (result code 3) and the pager did not move. The same extra on a cold
start worked. No exception, no crash, screenshots byte-identical.

**Cause:** `MainActivity` had the default `standard` launch mode. Relaunching a
task's root Activity with an intent that `filterEquals` the original — and
extras are *not* part of that comparison — only brings the task forward.
`ActivityStarter.complyActivityFlags` hands the intent to the live instance only
when the launch is single-top (`FLAG_ACTIVITY_SINGLE_TOP` or
`launchMode="singleTop"`). So `onNewIntent` never ran.

The console message is the trap: `recycleTask` returns `START_DELIVERED_TO_TOP`
whenever the task was already in front, whether or not anything was delivered.
It is not evidence that `onNewIntent` fired.

The same launch mode also stacked copies. When the intent *does* differ from the
task's root intent — a launcher tap versus `am start -n`, or the indicator's
bare `Intent(this, MainActivity::class.java)` — Android puts a new `MainActivity`
on top instead. The watch had ten in one task
(`dumpsys activity activities | grep 'Hist .*MainActivity'`).

The first version of this lesson blamed reading the extra only in `onCreate`,
and fixed only that: the requested screen held in `mutableStateOf`, set from
both `onCreate` and `onNewIntent` (calling `setIntent`), with a `LaunchedEffect`
that scrolls the pager and then clears it. That half is still required —
clearing is what lets the same screen be requested twice — but without
single-top it was dead code.

This is not a test-harness detail. The Ongoing Activity indicator and any tile
are `PendingIntent`s into this same Activity, and they exist *because*
something of ours is running, so the warm path is the common one. (Today the
indicator's intent carries no screen extra and just brings the app forward.)

**Fix:** `android:launchMode="singleTop"` on `MainActivity`, keeping the
`onNewIntent` + `LaunchedEffect` path. It covers every caller — adb, the
indicator, a future tile — without each one adding flags, and brings none of
the `NEW_TASK`/`CLEAR_TOP` Recents trouble noted in `MainActivity`.

**Verified on the watch** (0.2.0 release). On the old build, adding
`-f 0x20000000` (`FLAG_ACTIVITY_SINGLE_TOP`) by hand moved the pager where the
plain `am start` did not. On the fixed build, a warm `--es screen COUNTER`, a
launcher-shaped intent and a repeated request all moved the pager, logcat
reported `LAUNCH_SINGLE_TOP`, and the task stayed at one instance.

**Avoid it by:** declaring `singleTop` on any Activity a deep link can reach
while it is running, and confirming `onNewIntent` with a log line or an
on-screen change — never with `am start`'s console message. When testing,
request a page *other* than the one showing: asking for the current page leaves
the screenshot unchanged whether or not the fix works.

---

## 27 — The watch holds the *release* build; the debug APK will not install over it
*2026-09-09 · tooling · found on device*

**Symptom:** the runbook's install line failed outright:

```
adb install -r app/build/outputs/apk/debug/app-debug.apk
Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE: Existing package com.gymwatch
signatures do not match newer version; ignoring!]
```

**Cause:** what is on the watch was installed from `app-release.apk`, signed
with the release keystore. The debug APK is signed with the local debug key, and
Android refuses to replace a package with one signed by a different key. Nothing
about the build was wrong — the two APKs simply cannot overwrite each other.

**Fix:** install the release APK, which `./gradlew build` has already produced:

```bash
./gradlew build
adb install -r app/build/outputs/apk/release/app-release.apk
```

**Do not reach for `adb uninstall` first.** It works, and it silently deletes
the DataStore — the profiles, rest presets, screen layout and skin the watch has
been configured with. Reinstalling the matching variant keeps all of it, which
also makes "did the setting survive a reinstall?" a thing you can actually
check.

**Avoid it by:** matching the variant already installed rather than defaulting
to debug. `adb shell dumpsys package com.gymwatch | grep versionName` confirms
something is installed; the signature is only discovered by trying.

---

## 28 — Samsung Health *can* be opened on a specific exercise; read the APK, not the forums
*2026-09-10 · platform · found by reading Samsung Health 7.00.0.131*

**Symptom:** the user wanted three buttons that open one workout in Samsung
Health, "like the complication on my watch face". Lesson #2 and every forum
thread said only the home screen was reachable.

**Cause:** the claim was never checked against the thing that plainly did it.
Samsung Health's own complication opens a specific exercise, so *some* intent
does; the only question was whether a third party may send it. It may:

- `…/.app.exercise.view.ExerciseActivity` is `exported="true"` with no
  permission and handles
  `com.samsung.android.wear.shealth.intent.action.START_WORKOUT`;
- its intent check reads a String extra `exercise.type` and passes it to
  `Exercise.ExerciseType.valueOf`, so the value is the enum *name*
  (`WEIGHT_MACHINE`, `TREADMILL`);
- `StartExerciseBaseComplicationProviderService` builds exactly that, with
  `NEW_TASK|CLEAR_TASK` and two complication-only extras we do not need.

Verified with `am start` from the shell, a different uid with no special access:
Weight machines, Treadmill and Bench press each opened on their start screen with
the play button showing, and nothing started by itself. A SOCCER capture came
back black twice with the screen awake — probably a location or secure screen on
an outdoor type. Not investigated.

**Fix:** `SamsungHealthLauncher.startWorkout` sends that intent. If the Activity
stops resolving after an update, `WorkoutSetupUseCase` falls back to the home
screen. Icons are Samsung Health's `b_exercise_*_icon` drawables, looked up by
name in its resources at runtime and never bundled; they are white glyphs, so
they tint to the skin. Samsung's spellings, typos included (`treadmil`,
`horseback_riging`, `ICE_HOCKING`), are pinned by `SamsungExercisesTest` against
a snapshot of the real names.

An unused second route exists: exported `ExerciseListActivity`, action
`…EXERCISE_ACTIVITY_TYPES_WIDGET`, String extra `key_string_extra` holding the
numeric code (weight machine `15002`, treadmill `15005`). Try it if the first
one breaks.

**The method:**

```bash
adb shell pm path com.samsung.android.wear.shealth       # then adb pull the base.apk
aapt2 dump xmltree base.apk --file AndroidManifest.xml   # exported? permission? actions
aapt2 dump resources base.apk                            # drawable and string names
dexdump -d classes3.dex > classes3.txt                   # then awk out whole methods
```

Three traps cost attempts. `dexdump` prints large constants such as resource ids
in hex, in a trailing comment (`// #7f0801d8`), so searching for the decimal id
finds nothing. An enum's `<clinit>` loads each value's ordinal and code *before*
its name, so pairing a name with the numbers after it is off by one. And the
watch's Wi-Fi adb port changes on every reconnect — `adb mdns services` shows the
live one; a port written in the runbook goes stale.

**Avoid it by:** when a first-party surface does something, read its intent
before accepting "impossible". The APK is on the device and the tools ship with
the SDK.

---

## 29 — CRLF and LF files live side by side here; a rewrite flips every line
*2026-09-10 · tooling*

**Symptom:** a four-line manifest change committed as a 106-line diff, and a
64-line lesson correction as 1116 lines. `git diff --ignore-cr-at-eol --stat`
showed the real change.

**Cause:** there is no `.gitattributes`, and `core.autocrlf=true` comes from Git
for Windows' system gitconfig. The tree is split — roughly half the files are
stored with CRLF and half with LF, and until the 0.2.0 merge this file itself
was CRLF up to #24 and LF after. Git leaves CRs alone on `git add` when the
stored copy already has them, so the line endings a tool writes are the ones
that get committed. `perl -pi`, or any tool that rewrites a whole file as LF,
flips every line of a CRLF file.

Two things hid it:

- Git Bash's `grep` and `sed` strip CRs before printing, so
  `grep ... | od -c` shows bare `\n` on a CRLF file. Count bytes instead:
  `perl -ne '$c++ if /\r/; END{print $c+0}' file`.
- A perl regex spelled with literal `\n` silently fails to match a CRLF file,
  which looks like a typo in the pattern.

**Fix:** rebuild the commit with each file's original endings — write the
exact bytes with `git hash-object -w --no-filters`, stage them into a temporary
index (`GIT_INDEX_FILE`, `git update-index --cacheinfo`), `git commit-tree`.

**Avoid it by:** `git ls-files --eol <path>` before editing a file, and before
any commit comparing `git diff --cached --stat` with
`git diff --cached --ignore-cr-at-eol --stat`. If they differ, fix the endings
first. A `.gitattributes` would end this for good, but it means a repo-wide
renormalising commit — its own change, not a side effect of another.

---

## 30 — Dimming a wallpaper: model sRGB blending, and find the art by rays
*2026-09-10 · ui · tooling*

The themed skins put a picture behind every screen and a black overlay on it,
solved per picture so the dimmest text role reaches 4.5:1. Four things took a
second attempt.

**The overlay estimate was wrong in linear light.** Scaling luminance by
`(1 − a)` said a pale picture needs ~84% black under `#E8E8E8` text. Compose
(Skia) blends in the *encoded* sRGB space: each 8-bit channel becomes
`c · (1 − a)`, and only then is it linearised. Modelled that way the same
pictures need 60–64%, and the art stays visible. `ScrimSolver` and
`WallpaperContrastTest` both blend in sRGB. Do not "correct" them to linear.

**`·` came out as `Â·` from `scripts/wallpapers.ps1`.** The file was UTF-8
without a BOM, and Windows PowerShell 5.1 (`powershell -File`) decodes a
BOM-less script as the ANSI code page. Nothing errors; only the drawn text is
wrong. Fixed by saving with a BOM. A tool that rewrites the whole file can drop
it again — `Format-Hex scripts/wallpapers.ps1 -Count 3` must show `EF BB BF`.

**The art circle was found too early, then in the wrong place.** Every source is
painted as a watch: margin, near-black bezel, art. Scanning the centre row for
"the last dark pixel" stopped on Sailor Moon's glossy bezel highlight, and a
centre-row scan also runs into dark art that touches the rim (Yor's hair, Luna).
What works: 48 rays inward; on a light margin, trust nothing as art until the
dark ring has been crossed (the painted drop shadow is neither margin nor ring);
require 10 px of art so a highlight does not count; fit a circle, drop points
that fall inside it, fit again.

**`System.Drawing` will not mix rectangle types.** `DrawImage(img, Rectangle,
RectangleF, unit)` fails overload resolution in PowerShell with a conversion
error that looks like a value problem. Use `RectangleF` for both.

**Avoid it by:** checking any "how much to dim" number against the way the GPU
blends rather than against the luminance formula, and saving any `.ps1` that
contains non-ASCII text with a BOM.
