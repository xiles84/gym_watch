# Gym Watch

A Wear OS app for Samsung Galaxy Watch, built for the gym: a chronometer and
rest timer, a set counter you can drive with the bezel, and one-tap start for
three favourite workouts.

Built with a hexagonal architecture so new UI surfaces — a tile, a watch face,
a different health backend — are new adapters, not a rewrite of the core.

## Status

| Phase | |
|---|---|
| 0 · Toolchain and device ground truth | done (watch not yet paired) |
| 1 · Skeleton, version catalog, guardrails | done |
| 2 · Domain and use cases, pure JVM | done — 57 tests green |
| 3 · Compose UI, persistence, foreground service | next |
| 4 · Workouts via Health Services | |
| 5 · Tile *(optional)* | |
| 6 · WFF watch face *(optional)* | |

## Quick start

```bash
source scripts/env.sh
./gradlew :core:domain:test :core:application:test
```

No emulator or watch needed — the core is pure Kotlin JVM.

## Where things are

- **`CLAUDE.md`** — the working agreement. Read it first.
- **`docs/LESSONS.md`** — every non-obvious thing already learned, with reasons.
- **`docs/ARCHITECTURE.md`** — the hexagon, ports, and why the core is JVM-only.
- **`docs/DEVICE-RUNBOOK.md`** — pairing the watch over Wi-Fi, installing, logs.

## Why an app and not a watch face

A Watch Face Format bundle is resource-only and must be separate from any app
logic; the format has no variables and no persistent state. A watch face can
render the time and launch an app — it cannot run a stopwatch or hold a counter.
Details in `docs/LESSONS.md` entry 1.
