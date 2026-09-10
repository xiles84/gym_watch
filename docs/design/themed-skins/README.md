# Themed skins — design notes and wallpaper prompts

Two different sets of pictures sit around the themed skins. Do not confuse them:

| Where | What | Status |
|---|---|---|
| `docs/design/themed-skins/v1`–`v3` | Early skin studies with the app's own UI drawn on top: character fragments above the numbers on flat, dark grounds. `v2` compares where the character sits, `v3` how much eye detail it needs, `v1/references` holds reference art. | Concepts, not implemented |
| `skin-images/<theme>/` | The wallpapers the app ships. Backgrounds only, no UI, generated with the two prompts below. | In the app since 2026-09-10 |

## How the wallpapers were generated

Two prompts, one after the other in the same conversation with an image
generator, once per theme.

### 1. The concept sheet

Replace `[REFERENCE SERIES]` with the series — Dragon Ball, Sailor Moon,
Spy × Family and Pokémon so far.

```text
Create a polished concept sheet of smartwatch wallpapers inspired by **[REFERENCE SERIES]**.

The goal is to create **background wallpapers only**, not complete watch-face UIs.

Important:
- **Do not render any watch elements** such as time, date, battery, steps, widgets, complications, labels, or numbers.
- However, compose each wallpaper **as if a smartwatch UI will later be placed on top**.
- Keep the design clean and readable by preserving **clear negative-space zones** for future watch information.

### Watch-layout awareness
Design everything specifically for a **round smartwatch screen**.

Reserve these areas so the future watch UI has space:
- a **large clean area in the upper center** for the main time
- a **smaller clean area below it** for date or secondary information
- optional **small uncluttered zones near the edges** for complications/widgets
- avoid putting important facial features, props, or focal details in those reserved zones
- keep the most important art slightly away from the outer rim of the circle

The wallpaper should feel balanced even with these empty zones.

### Design language
- minimalist and elegant
- clean flat-vector or poster-like illustration
- simplified but recognizable characters
- strong silhouettes and iconic visual cues
- soft muted colors or carefully chosen character palettes
- minimal shading
- no clutter
- no unnecessary texture
- strong negative space
- not hyper-abstract: keep enough detail to recognize the character
- not detailed anime rendering
- not photorealistic

### Character treatment
Represent the main characters using only the most iconic features possible, such as:
- hairstyle or silhouette
- clothing shape or color block
- accessory
- pose
- symbolic object

You may omit facial details partially or entirely if the character remains clearly recognizable.

### Composition goals
Create approximately **12 circular smartwatch wallpaper concepts**.

Include:
- several **individual character wallpapers**
- at least one **group composition**
- a few **symbolic or prop-based wallpapers**
- different compositions with varied negative-space placement
- some wallpapers with the subject lower on the screen
- some with the subject off-center
- some with only a partial portrait or upper body
- some with very sparse storytelling elements in the background

### Color direction
Give each major character a **distinctive palette** of about 2–4 main colors.
The palette should help identify the character immediately.

### Presentation
Show all designs together as a clean professional **concept sheet**.
Each wallpaper should appear inside a **round smartwatch screen frame** for comparison.

Do not add text, logos, time, date, or any watch interface elements inside the wallpaper.

### Style intent
The result should feel like:
- a refined smartwatch wallpaper collection
- clean, elegant, minimal, character-based
- visually compatible with watch overlays
- designed to look good once time/date are placed on top later
```

The result is one sheet of about twelve round wallpapers, each inside a drawn
watch frame. `skin-images/pokemon/` still holds such a sheet beside its first
individual images.

### 2. Splitting the sheet into single images

```text
please break this in circular individual images.
I am aware you will need more than one iteration.
```

As the prompt expects, this takes several turns. Each result is a 1254×1254 PNG
of one wallpaper, **still inside the sheet's watch frame**: a light or black
margin and, on most, a painted dark bezel ring. That frame is expected —
`scripts/wallpapers.ps1` cuts it off.

## From a generated image to the app

1. Keep the pictures you want in `skin-images/<theme>/` and name each one after
   the screen it goes behind. The file name *is* the assignment; swapping two
   pictures means swapping two names.

   | File | Screen | |
   |---|---|---|
   | `chrono.png` | Chronometer | required |
   | `rest.png` | Rest presets | required |
   | `rest-running.png` | Rest counting down | required |
   | `counter.png` | Set counter | required |
   | `workouts.png` | Workout shortcuts | required |
   | `settings.png` | Settings | required |
   | `rest-over.png` | REST OVER alarm | optional — uses `rest-running.png` |
   | `rest-editor.png` | Editing a rest preset | optional — uses `rest.png` |
   | `workout-picker.png` | Changing a workout shortcut | optional — uses `workouts.png` |

   The confirm dialog never has a wallpaper.

2. Run `powershell -File scripts/wallpapers.ps1`. It finds the art circle, crops
   to it and writes 480×480 JPEGs to
   `adapters/driving/ui-compose/src/main/res/drawable-nodpi/wp_<theme>_<slot>.jpg`.
   Add `-ReviewDir <folder>` for one review sheet per theme, to judge an
   assignment before any code uses it. A new theme's folder name also needs an
   entry in `$ThemeKeys` at the top of the script.

3. A new theme needs a `Skin` entry with its palette in
   `core/domain/src/main/kotlin/com/gymwatch/core/domain/model/Skin.kt`, and its
   pictures in `artFor` in
   `adapters/driving/ui-compose/src/main/kotlin/com/gymwatch/adapters/driving/ui/Wallpapers.kt`.
   An optional picture for an existing theme is one argument in `artFor`.

4. `./gradlew :adapters:driving:ui-compose:testDebugUnitTest`.
   `WallpaperContrastTest` fails if a picture cannot be made readable with at
   most 80% black over it, or if a file is not used by any skin.

How the dimming is solved, and the traps met building it: `docs/LESSONS.md` #30
and `docs/ARCHITECTURE.md`.

## What the first set showed

Measured on Dragon Ball, Sailor Moon and Spy × Family on 2026-09-10 — facts to
weigh when writing the next theme's prompt, not changes to the one above.

- **The prompt reserves a watch face's layout, not this app's.** It keeps the
  upper centre free for a time and a small area below for a date. This app puts
  its largest number in the dead centre, with buttons and a hint just below. Its
  text zone is a centred disc about 62% of the screen's radius — and the whole
  screen on Workouts, Settings and the two editors. Art inside that zone is what
  gets dimmed.
- **What sits behind the numbers decides how dim a picture gets.** Centre
  brightness ranged from 0.01 to 0.98 (0 black, 1 white), most of it above 0.4,
  and every picture solved to 62–67% black — the navy Sailor Moon sky too,
  because its crescent falls behind the clock. Pastel grounds (Chibiusa's pink,
  Anya's, the peanuts) look noticeably greyer on the watch than in the source.
- **The painted frame costs resolution.** The art circle is 1046–1210 px across
  inside the 1254 px image, so up to a sixth of the width is thrown away before
  scaling to 480.
- **A theme needs six to nine pictures.** Six were generated per theme; the
  table above lists which screens take them.
