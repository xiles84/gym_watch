# Gym Watch — working agreement

A Wear OS app for a Samsung Galaxy Watch, used at the gym: chronometer + rest
timer, a set counter, and one-tap start for three favourite workouts.

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
4. **Health Services allows one exercise device-wide.** Always check ownership
   before starting; never take the slot from Samsung Health without asking.

## Module map

```
:core:domain        kotlin("jvm")  entities + port interfaces. No dependencies.
:core:application   kotlin("jvm")  use cases. Depends only on :core:domain.
:adapters:driven:*  Android        DataStore, Health Services, platform
:adapters:driving:* Android        Wear Compose UI, foreground service, tile
:app                               composition root — the only module that
                                   knows every adapter
:watchface                         WFF, resource-only, separate APK
```

Dependency rule: **adapters depend on core, never the reverse.**
Adding a UI or a health backend means adding an adapter module. If a change
requires editing `:core` to add a UI, the design has gone wrong.

## Build and test

```bash
./gradlew :core:domain:test :core:application:test   # pure JVM, no device needed
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
