# Architecture

Hexagonal (ports and adapters), enforced by the build rather than by discipline.

```
        DRIVING (UI)                 CORE                  DRIVEN (infra)

  Wear Compose UI     ->  +---------------------------+  ->  DataStore
  Foreground service  ->  |  :core:application        |  ->  Health Services
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
| `:core:domain` | kotlin-jvm | `Chronometer`, `RestTimer`, `Counter`, `Favourites`, `ExerciseKind`, `WorkoutSnapshot`; all port interfaces |
| `:core:application` | kotlin-jvm | `ChronometerUseCase`, `RestTimerUseCase`, `CounterUseCase`, `WorkoutUseCase` |
| `:adapters:driven:persistence` | android-lib | DataStore implementations of the repository ports |
| `:adapters:driven:health` | android-lib | `WorkoutSessionPort` over Health Services `ExerciseClient` |
| `:adapters:driven:platform` | android-lib | `ClockPort`, `HapticsPort`, `OngoingActivityPort`, `PermissionsPort` |
| `:adapters:driving:ui-compose` | android-lib | Screens and ViewModels |
| `:adapters:driving:service` | android-lib | Exercise foreground service |
| `:app` | android-app | Composition root, manifest, permissions |
| `:watchface` | android-app | WFF resources only — separate APK |

## Ports

**Driving** (called by the UI): the four use cases in `:core:application`.

**Driven** (implemented by adapters), in `com.gymwatch.core.domain.port`:

| Port | Backed by | Note |
|---|---|---|
| `ClockPort` | `SystemClock.elapsedRealtime()` | Monotonic. Never a wall clock. |
| `CounterRepositoryPort` | DataStore | |
| `FavouritesRepositoryPort` | DataStore | |
| `RestTimerSettingsPort` | DataStore | Stores the configured length only, never a running countdown |
| `WorkoutSessionPort` | Health Services | The whole health backend, behind one interface |
| `HapticsPort` | `Vibrator` | |
| `OngoingActivityPort` | `androidx.wear.ongoing` | |
| `PermissionsPort` | Android runtime permissions | Hides the API-36 permission split |

`port/` holds interfaces only. Value types such as `Haptic` and
`HealthPermission` live in `model/`, and `ArchitectureTest` enforces that.

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

The same property makes the domain trivially testable: pass a `FakeClock`,
advance it by 45 minutes instantly, assert. See `ChronometerTest`.

## Dependency injection

Manual constructor injection through an `AppContainer` in `:app`. No Hilt:
annotation processors would drag Android into modules deliberately kept pure,
and the object graph here is small enough to read on one screen.

## Adding things

- **A new UI surface** (tile, complication, a different face): new module under
  `adapters/driving/`, depending on `:core:application`. The core does not change.
- **A different health backend:** new implementation of `WorkoutSessionPort`.
  Swap it in `AppContainer`. Nothing else moves.
- **A new domain rule:** it belongs on the entity, with a test in
  `:core:domain`, not in a ViewModel.
