# Gym Watch

A Wear OS app for Samsung Galaxy Watch, built for the gym: a chronometer, a rest
timer with three one-tap presets, a set counter, and three workout profiles that
reconfigure it all per workout. Screens can be reordered or turned off.

Built with a hexagonal architecture so new UI surfaces — a tile, a watch face —
are new adapters, not a rewrite of the core.

## Status

| Phase | |
|---|---|
| 0 · Toolchain and device ground truth | done — SM-L705F (Galaxy Watch **Ultra**), Wear OS 6, API 36 |
| 1 · Skeleton, version catalog, guardrails | done |
| 2 · Domain and use cases, pure JVM | done |
| 3 · Compose UI, persistence, foreground service | done — verified on the watch |
| 4 · Workouts via Health Services | **removed** — it could never reach Samsung Health |
| 5 · Rest presets, profiles, configurable screens | **done — 77 tests green** |
| 6 · Tile *(optional)* | next |
| 7 · WFF watch face *(optional)* | |

## Workouts and Samsung Health

Samsung Health records the workout; this app does not try to. A third-party watch
app has no way to write one — `ExerciseClient` streams live metrics but persists
nothing, Health Connect does not run on Wear OS, and Samsung Health has no
third-party write API. Tracking our own exercise also *ended* whatever Samsung
Health was recording, because the platform allows one at a time device-wide.

So the three profiles configure what this app can genuinely own — rest lengths
and what the counter counts — and a button opens Samsung Health to do the
recording. The full reasoning is in `docs/LESSONS.md` entry 2.

## Quick start

```bash
source scripts/env.sh
./gradlew :core:domain:test :core:application:test
```

No emulator or watch needed — the core is pure Kotlin JVM.

Build the watch APK:

```bash
./gradlew :app:assembleDebug
```

## Where things are

- **`CLAUDE.md`** — the working agreement. Read it first.
- **`docs/LESSONS.md`** — every non-obvious thing already learned, with reasons.
- **`docs/ARCHITECTURE.md`** — the hexagon, ports, and why the core is JVM-only.
- **`docs/DEVICE-RUNBOOK.md`** — pairing the watch over Wi-Fi, installing, logs.
- **`docs/design/themed-skins/README.md`** — the prompts that generated the skin
  wallpapers, and how a picture becomes part of a skin.

## Why an app and not a watch face

A Watch Face Format bundle is resource-only and must be separate from any app
logic; the format has no variables and no persistent state. A watch face can
render the time and launch an app — it cannot run a stopwatch or hold a counter.
Details in `docs/LESSONS.md` entry 1.
