# Gym Watch — working agreement

A Wear OS app for a Samsung Galaxy Watch, used at the gym: chronometer, a rest
timer with three one-tap presets and an alarm at zero, a set counter, and three
workout shortcuts that open Samsung Health on that exercise. A media screen
opens Spotify or the phone's media controls, and lists the last ten Audible
books, which start *on the phone* through a companion app in `:phone`. Screens
can be reordered or turned off.

**Read `docs/LESSONS.md` before doing anything non-trivial.** It exists so the
same problem is not solved twice. Most of what looks like an arbitrary choice in
this repo is written down there with its reason.

---

## The rule that keeps this repo cheap to work in

> Any non-obvious problem that costs more than one attempt gets an entry
> appended to `docs/LESSONS.md` **before the task is called done.**

Entry format: date, symptom, root cause, fix, how to avoid it. Newest last.
If a lesson turns out to be wrong, correct it in place — do not leave both.

---

## Hard constraints — settled, do not relitigate

1. **A watch face cannot hold the counter or the chronometer.** Watch Face
   Format bundles are resource-only (`android:hasCode="false"`) and must be a
   *separate* app bundle from Wear OS app logic. WFF is declarative XML with no
   variables and no persistent state. This is why the deliverable is an app.
2. **The core is pure Kotlin JVM.** `:core:domain` and `:core:application` are
   `kotlin("jvm")` modules, not Android libraries. `import android.*` will not
   compile there, and `ArchitectureTest` fails the build if it ever does.
3. **Time enters the core only through `ClockPort`,** backed by
   `SystemClock.elapsedRealtime()`. Never a wall clock — it jumps.
4. **This app does not record workouts — Samsung Health does.** A third-party
   watch app cannot write one: `ExerciseClient` persists nothing, Health Connect
   does not run on Wear OS, and Samsung Health has no write API. Tracking our own
   exercise also *ended* Samsung Health's, because the platform allows one
   device-wide. Do not reintroduce this. See lesson 2. What we *can* do is open
   Samsung Health on a specific exercise's start screen, through the
   undocumented intent its own complication uses — see lesson 28.
5. **The watch has no rotating bezel.** SM-L705F is a Galaxy Watch **Ultra**;
   its bezel is static. Every value must be settable by touch. Rotary support is
   additive only, never the sole way to do anything. See lesson 24.

## Module map

```
:core:domain        kotlin("jvm")  entities + port interfaces. No dependencies.
:core:application   kotlin("jvm")  use cases. Depends only on :core:domain.
:adapters:driven:*  Android        DataStore, platform (clock, haptics,
                                   notifications, Samsung Health and media
                                   launchers), wearsync (Data Layer, both apps),
                                   audible (phone only)
:adapters:driving:* Android        Wear Compose UI, foreground service, tile
:app                               watch composition root — the only module
                                   that knows every watch adapter
:phone                             phone companion composition root. Same
                                   applicationId and release key as :app, or
                                   the Data Layer never connects them (lesson 33)
:watchface                         WFF, resource-only, separate APK
```

Dependency rule: **adapters depend on core, never the reverse.**
Adding a UI or a health backend means adding an adapter module. If a change
requires editing `:core` to add a UI, the design has gone wrong.

## Build and test

```bash
./gradlew :core:domain:test :core:application:test   # pure JVM, no device needed
./gradlew :adapters:driven:platform:testDebugUnitTest # Samsung Health name mapping
./gradlew :adapters:driven:wearsync:testDebugUnitTest :adapters:driven:audible:testDebugUnitTest
./gradlew build
```

`JAVA_HOME` must point at Android Studio's bundled JDK; `scripts/env.sh` sets it.
Device commands live in `docs/DEVICE-RUNBOOK.md`.

## Conventions

- Domain models are immutable `data class`es with pure transition methods.
  Behaviour belongs on the model; use cases orchestrate, they do not calculate.
- `port/` contains interfaces only. Value types (enums) live in `model/`.
- Never base a write on a `stateIn` StateFlow's seeded default — read the
  repository. See lesson 10.
- Comments explain *why*, not what. Match the density already in the file.
