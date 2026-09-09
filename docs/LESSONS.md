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

## 2 — There is no supported deep link into a specific Samsung Health exercise
*2026-09-09 · platform*

**Cause:** community attempts at `health://` and similar URI schemes do not
work. Only `getLaunchIntentForPackage("com.samsung.android.wear.shealth")` is
documented, and it lands on the app's home screen.

**Fix:** our app records the workout itself with Health Services
`ExerciseClient`, which is officially supported.

**Avoid it by:** not spending time reverse-engineering Samsung Health intents.
If this is ever revisited, the probe is
`adb shell dumpsys package com.samsung.android.wear.shealth`.

---

## 3 — Health Services allows exactly one exercise at a time, device-wide
*2026-09-09 · platform*

**Cause:** the constraint is global, not per-app. If Samsung Health is tracking
a workout and we call `startExerciseAsync`, theirs ends.

**Fix:** `WorkoutUseCase.requestStart` calls `ownership()` first and returns
`StartOutcome.NeedsConfirmation` rather than starting. Only `forceStart` takes
the slot, and only after the user confirms.

**Avoid it by:** never calling `session.start()` directly from UI code.

---

## 4 — The foreground service must be alive through the *prepare* phase too
*2026-09-09 · platform*

**Cause:** Health Services ends the exercise with
`ExerciseEndReason.AUTO_ENDED_PERMISSION_LOST` if no foreground service is
running — and the requirement starts at `prepareExerciseAsync`, not at
`startExerciseAsync`.

**Fix:** start the service before preparing sensors, stop it after
`endExerciseAsync` completes.

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
