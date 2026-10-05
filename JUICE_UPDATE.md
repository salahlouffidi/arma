# ARMASNAKE — Juice & Special-Food Update

Makes eating fruit dramatically more satisfying and adds a set of rare, exciting power-up fruits —
while preserving the existing armadillo-vs-snake gameplay, the 16 stages, progression, sound system,
and **the AdMob integration and package name (unchanged)**.

Core architecture was kept intact: Kotlin + Jetpack Compose + Canvas (`DrawScope`) rendering,
procedural PCM `SoundManager`, the existing particle list, and the "7araka" overdrive ability.

---

## What changed (gameplay)

**Satisfying eating ("juice")**
- **Combo system** — chain bites within ~2.2 s to build **x2 → x5**, which multiplies the points
  each fruit awards. A small `🔥 COMBO xN` indicator shows under the HUD; combos rise in pitch via a
  new SFX and expire when you wait too long.
- **Floating score popups** — every bite spawns a rising, fading `+N` label (and a combo label) right
  at the fruit, drawn with `TextMeasurer`. Bigger pickups use a larger label.
- **Eat-bounce** — the armadillo does a brief squash-and-stretch pop on each bite.
- Richer collection particles, and the score pill still animates on change.

**Special foods (4–6 polished, fitting ARMASNAKE)**
- ⚡ **Speed / Overdrive** — the existing star-power ability (kept and reused).
- 💎 **Diamond** — large score bonus (+15 before combo/multiplier) with a lavish crystalline burst.
- 🧲 **Magnet** — for a few seconds, nearby ordinary fruit is pulled toward the armadillo; shown by
  orbiting magnetic particles and a faint field ring around it.
- 🌈 **Rainbow** — activates a temporary **x2 score multiplier** (stacks on top of the combo
  multiplier) with a shifting multicolor aura.

Rare fruit spawn **one at a time, on a randomized timer** (first ~14 s, then ~13–23 s apart), live for
~9 s, **blink as they're about to expire**, and are flagged by a pulsing **beacon + chevron** so you
can find and chase them. A `✨ … DETECTED!` banner and a dedicated twinkle SFX announce each one.

**First-run teaching** — a one-line hint appears on Round 1 for a few seconds ("Drag to move • Grab
🍎 • Chain bites for COMBOS • Catch glowing ⚡💎🧲🌈 power fruit!"), then auto-dismisses.

**Performance** — the simultaneous particle count is now bounded (cap 180) so heavy moments stay
smooth on mid-range phones. No new dependencies, no assets, no network — all visuals/sounds remain
procedural and offline.

---

## Pass 2 — Graphics, rounds & styling

**Graphics / atmosphere**
- **Animated themed backdrop** behind the table for all 8 themes: radial lighting, drifting motes
  (rising embers on Volcano/Temple/Arena, falling snow on Ice/Mountains, floating dust/fireflies
  elsewhere) and a soft vignette. Allocation-free — mote positions are derived from index + time.
- **Table lighting**: warm key light from the upper-left and a gentle shadow toward the far edge.
- **Readability**: each armadillo gets a soft golden/blue ground glow (instantly identifiable at
  speed); each snake head gets a pulsing red threat glow and glowing eyes (danger reads at a glance).
- **Richer particles**: larger sparks get a soft glow halo (small ones stay cheap).
- **Environment reaction**: big moments trigger a coloured glowing rim around the arena plus a short,
  controlled camera impulse — cyan for overdrive, white for rainbow, cyan-white for diamond, magenta
  for magnet, red when a snake catches you, gold on round clear. Normal bites stay subtle (no shake).

**Rounds**
- **Animated round-intro card** at the start of every round: stage icon, `ROUND n / 16`, name,
  subtitle, mechanics badge and fruit goal — scales/fades in and out (~2.6 s).
- **Fixed: in-game banners were never displayed.** The engine has always produced messages (round
  goal, "POWER EXPIRED", "LAVA HAZARD", "SPECIAL FRUIT EVENT", the new power-up alerts…) but the
  screen never rendered them. They now appear as a fading toast under the HUD.

**Styling / HUD**
- **Active-effect timer pills** (top-left) for 🧲 Magnet and 🌈 Score x2 with a live countdown and a
  draining bar.
- Overlays (combo chip, timer pills, round intro, banner, first-run hint) live in an isolated
  `JuiceOverlays` composable that subscribes to the frame counter, so they refresh every frame
  without recomposing the whole HUD.

---

## Files changed

- `game/GameModels.kt` — new `FruitType`s (DIAMOND/MAGNET/RAINBOW), `FloatingText`, and additive
  effect/lifetime/points fields (all defaulted, so existing constructors are unaffected).
- `game/GameEngine.kt` — combo state + multiplier, floating-text system, rare-fruit spawn/expiry +
  pointer target, magnet attraction, rainbow multiplier, per-pickup scoring helper, eat-bounce,
  particle cap, and new optional callbacks.
- `game/TableRenderer.kt` — renderers for the three new fruits (distinct glow/hierarchy), armadillo
  magnet/rainbow auras + eat-bounce scale, and a floating-text drawer.
- `game/SoundManager.kt` — 5 new procedural SFX (combo, diamond, magnet, rainbow, rare-spawn).
- `ui/ArmadilloEscapeScreen.kt` — wires the new callbacks to sound/haptics, draws floating texts +
  the rare-fruit beacon, adds the combo indicator and the first-run hint.

**Not touched:** `ads/AdManager.kt`, `AndroidManifest.xml`, `build.gradle.kts`, `metadata.json`,
`.env*`, and the package `com.aistudio.armadilloescape.xwqrpz`.

---

## Build & verification status — honest

These changes were implemented and **self-reviewed for compile-safety** (brace/paren balance, no
orphaned symbols, exhaustive `when`s, correct imports), but they were **not compiled or run** in this
environment — it has no Android SDK and Google's Maven isn't reachable, so Gradle can't resolve the
Android plugin here. Build in **Android Studio** (or a machine with the Android SDK):

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Please device-test:** normal movement/eating/scoring, combo build + expiry, each special fruit
(spawn → collect → effect → smooth return), magnet pull, rainbow x2, diamond bonus, pause/resume and
app background during an active effect, round completion, game over, restart, saved progression, and
rewarded/interstitial ads. Tuning knobs (combo window, durations, spawn cadence, point values) are
constants at the bottom of `GameEngine.kt`.

---

## Pass 3 — Console-style redesign

Original console-inspired presentation (no Sony assets, logos, icons, fonts or sounds). Gameplay,
16 rounds, progression/locks, save system, scoring, snake AI, controls, collision, combos, specials,
AdMob, package name and Gradle/production config are unchanged.

**New files**
- `game/WorldRenderer.kt` — procedural full-screen worlds per theme (sky, sun/moon, 3 parallax
  silhouette layers, themed particles, vignette, arena decor). Shared by menus and gameplay.
- `ui/ConsoleKit.kt` — palette, living `WorldBackdrop` (crossfades on selection), focus menu items,
  glass panels, difficulty meter, power chips.
- `ui/ConsoleHomeScreen.kt` — cinematic home: live world, hero armadillo + distant snake, ARMASNAKE
  title, dominant PLAY, ROUNDS / HOW TO PLAY / SETTINGS / VERSUS with spring focus.
- `ui/RoundLibraryScreen.kt` — perspective carousel (ROUND xx/16, name, world art, difficulty,
  target, best, COMPLETE/AVAILABLE/LOCKED), swipe + tap, ALL/SOLO/TWIN filter, launch surge.
- `ui/GameplayOverlays.kt` — minimal HUD, power chips, rare-fruit toast ("RARE FRUIT DETECTED"),
  ≤2.4 s cinematic round intro, Round Complete (count-up, best combo, NEW WORLD UNLOCKED card,
  CONTINUE/REPLAY/ROUND SELECT), Game Over (RETRY/WATCH AD/ROUND SELECT/HOME).

**Gameplay presentation**
- 12 procedural normal fruits; specials drawn 1.3× with aura.
- New specials: **Shield** (blocks one bite), **Freeze** (snakes slowed 5 s), **Golden** (+25).
  Ghost / Fruit Rain intentionally skipped to keep balance simple.
- Camera zoom pulse on combo milestones, celebration zoom, danger edge glow, freeze tint, parallax.
- Screen transitions crossfade; Round Complete/Game Over keep particles animating.
- Round names refreshed (Emerald Garden … Colosseum of Kings).

**Verification (honest status)**
- All changed Kotlin was **type-checked with the real Kotlin compiler (2.0.21)** against hand-written
  Compose/Android API stubs — 0 errors. This catches syntax, types, missing imports and
  non-exhaustive `when`s.
- **Not built or run on Android.** This environment cannot reach Google Maven / Maven Central, so
  Gradle/AGP and the Android SDK are unavailable. Build and device-test in Android Studio:

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Please check on device:** frame rate on a low-end phone, carousel swipe feel, small-screen layout
of Round Complete / Game Over, locked-round behaviour, unlock card, shield/freeze/golden, ads.
