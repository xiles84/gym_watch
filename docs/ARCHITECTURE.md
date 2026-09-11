# Architecture

Hexagonal (ports and adapters), enforced by the build rather than by discipline.

```
        DRIVING (UI)                 CORE                  DRIVEN (infra)

  Wear Compose UI     ->  +---------------------------+  ->  DataStore
  Foreground service  ->  |  :core:application        |  ->  Samsung Health
  Tile / WFF face     ->  |    use cases              |  ->  Clock / Vibrator
                          |  +---------------------+  |      Ongoing Activity
                          |  |  :core:domain       |  |
                          |  |  entities + ports   |  |
                          |  +---------------------+  |
                          +---------------------------+
                             pure Kotlin JVM, no android.*
```

Every arrow points inward. The core depends on nothing.

## Why the core is `kotlin("jvm")` and not `com.android.library`

Because then the boundary cannot rot. The Android SDK is not on the core's
compile classpath, so `import android.*` is a compile error, not a code-review
note. `ArchitectureTest` adds the rules a compiler cannot express: no adapter
imports, no wall clock, ports stay interfaces.

The practical payoff is test speed and honesty — the whole core suite runs on
the JVM with no emulator, no device, and no Robolectric.

## Modules

| Module | Type | Contains |
|---|---|---|
| `:core:domain` | kotlin-jvm | `Chronometer`, `RestTimer`, `RestPresets`, `Counter`, `WorkoutSetup`, `ScreenLayout`, `Skin`/`Palette`, `Contrast`, `ExerciseKind`, `ResetOutcome`; all port interfaces |
| `:core:application` | kotlin-jvm | `ChronometerUseCase`, `RestTimerUseCase`, `CounterUseCase`, `WorkoutSetupUseCase`, `ScreenLayoutUseCase`, `SkinUseCase` |
| `:adapters:driven:persistence` | android-lib | DataStore implementations of the repository ports |
| `:adapters:driven:platform` | android-lib | `ClockPort`, `HapticsPort`, `WakeUpPort`, `OngoingActivityPort`, `CompanionHealthAppPort`; `SamsungHealthIcons` |
| `:adapters:driving:ui-compose` | android-lib | Screens |
| `:adapters:driving:service` | android-lib | Timer foreground service |
| `:app` | android-app | Composition root, manifest, permissions |
| `:watchface` | android-app | WFF resources only — separate APK |

## Ports

**Driving** (called by the UI): the six use cases in `:core:application`.

**Driven** (implemented by adapters), in `com.gymwatch.core.domain.port`:

| Port | Backed by | Note |
|---|---|---|
| `ClockPort` | `SystemClock.elapsedRealtime()` | Monotonic. Never a wall clock. |
| `CounterRepositoryPort` | DataStore | |
| `WorkoutSetupRepositoryPort` | DataStore | The three workout shortcuts and the rest lengths. Configured lengths only, never a running countdown |
| `ScreenLayoutRepositoryPort` | DataStore | Which screens are shown, and in what order |
| `SkinRepositoryPort` | DataStore | The selected colour scheme, stored by enum name |
| `CompanionHealthAppPort` | `PackageManager`, `startActivity` | Opens Samsung Health, which owns the workout record — on an exercise's start screen, or its home screen. Names no package and no intent; that is the adapter's business (`docs/LESSONS.md` #28) |
| `HapticsPort` | `Vibrator` | |
| `WakeUpPort` | `AlarmManager`, `PowerManager` | Wakes the watch at the rest timer's zero and holds it awake while the alarm rings. A `delay` alone stalls in deep sleep (`docs/LESSONS.md` #31) |
| `OngoingActivityPort` | `androidx.wear.ongoing` | |

`port/` holds interfaces only. Value types such as `Haptic` and
`ResetOutcome` live in `model/`, and `ArchitectureTest` enforces that.

Exercise icons are not a port. They are pure presentation, so the UI module
declares a `WorkoutIcons` function interface and the composition root hands it
`SamsungHealthIcons::bitmap` — the driving adapter never depends on the driven
one, and the core never hears about icons at all.

Skin wallpapers split the same way. A skin's palette is domain (`Skin`), and so
is `Contrast`, the WCAG arithmetic that says whether a palette reads. The
pictures are Android resources, so the UI module maps a skin to them
(`artFor`), and `ScrimSolver` works out from each picture how much black to lay
over it. `WallpaperContrastTest` runs that solver over the shipped JPEGs on the
JVM. The sources are in `skin-images/<theme>/<slot>.png`, where the file name
is the assignment, and `scripts/wallpapers.ps1` crops them into resources
(`docs/LESSONS.md` #30). The prompts that generated them, and the steps from a
generated image to a skin, are in `docs/design/themed-skins/README.md`.

## The one design decision everything else rests on

`Chronometer` and `RestTimer` store a **start mark plus banked time**, not a
tick count:

```kotlin
fun elapsedAt(now: Duration): Duration =
    accumulated + (startMark?.let { now - it } ?: Duration.ZERO)
```

Elapsed time is *derived from the clock on every read*. Nothing has to be
running for the value to stay correct, so the screen can sleep, the process can
be killed, and doze can do whatever it likes. A tick loop would drift, stall, or
need waking — and would be far harder to test.

The rest alarm follows the same rule: the start mark is kept past zero until the
user stops or restarts it, so "is it ringing?" is also a question for the clock.

The one thing the clock cannot do is *act* at a time. Deriving a value needs
nothing running; buzzing at zero needs the CPU awake at zero, which is what
`WakeUpPort` is for (`docs/LESSONS.md` #31).

The same property makes the domain trivially testable: pass a `FakeClock`,
advance it by 45 minutes instantly, assert. See `ChronometerTest`.

## Dependency injection

Manual constructor injection through an `AppContainer` in `:app`. No Hilt:
annotation processors would drag Android into modules deliberately kept pure,
and the object graph here is small enough to read on one screen.

## Adding things

- **A new UI surface** (tile, complication, a different face): new module under
  `adapters/driving/`, depending on `:core:application`. The core does not change.
- **A different watch's health app:** new implementation of
  `CompanionHealthAppPort`, plus its own name and icon mapping for
  `ExerciseKind`. Swap it in `AppContainer`. The package name and intents live in
  the adapter, so nothing else moves.
- **A new domain rule:** it belongs on the entity, with a test in
  `:core:domain`, not in a ViewModel.

## What this app deliberately does not do

It does not record workouts. Health Services streams live metrics but persists
nothing, Health Connect does not run on Wear OS, and Samsung Health has no
third-party write API — so a workout tracked here could never reach the history
the user actually reads. Worse, the platform allows one exercise device-wide, so
tracking ours *ended* Samsung Health's.

Samsung Health records; this app owns the rest timer, the counter and the
chronometer, and its three shortcuts open Samsung Health on the exercise you are
about to do. See `docs/LESSONS.md` #2 and #28.
