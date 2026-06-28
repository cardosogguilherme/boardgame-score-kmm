# Board Game Scorer (Kotlin Multiplatform)

A Kotlin Multiplatform app for scoring board games from **author-defined rule templates**. Build a
scoring template (fields + typed scoring rules), start a game, add players, and enter scores with live
per-category breakdowns and cross-player ranked goals. Today it ships an Android app; the shared core
is structured for an iOS target (see [the iOS plan](docs/superpowers/plans/2026-06-28-ios-xcode-target.md)).

## Features

- **Rule builder** — author a template: name it, add fields (`COUNT` / `TRACK` / `RANKING`), and add any
  of five scoring-rule types from a palette, each rendered as a plain-language line with live validation:
  - `Lookup` (track value → points table), `PerUnit` (× points/unit), `Flat` (face value),
    `PerN` (1 point per N), `Ranking` (table-scoped award by rank across all players).
- **Game flow** — start a game from a saved template + optional scenario, add players, and play.
- **Score entry** — steppers only (never text fields for scores), a live running total, per-category
  subtotals, and a cross-player ranked-goal section that resolves rank live. Edits **autosave**.
- **Library** — browse saved templates and games; resume a game; tap a template to edit it.
- **Local persistence** via Room; **edge-to-edge** UI that respects the status bar / camera cutout.

## Architecture

A hybrid clean architecture with **anti-corruption layers** — distinct models per layer, mappers at
every boundary — and **MVVM with interactors** (use cases).

```
:deepsea-scoring  (commonMain)   DOMAIN — pure Kotlin: models, scoring engine (dumb-data rules +
                                 external interpreters), repository interfaces, interactors
:data             (Android lib)  DATA  — Room entities/DAOs/DB + entity↔domain mappers +
                                 Room-backed repository implementations
:ui               (commonMain)   PRESENTATION — Compose Multiplatform: immutable UiState models +
                                 domain→UI mappers/reducers + stateless screens
:androidApp       (Android host) ViewModels (StateFlow<UiState>), Navigation-Compose, Koin DI,
                                 edge-to-edge MainActivity
```

Data flow: `Composable → callback → ViewModel → Interactor → Repository (interface) → Room impl`, and
back as `Room Flow → domain model → ViewModel maps to UiState → StateFlow → Composable`.

**Design notes**
- **Scoring rules are dumb data (an AST); every operation is an external `when`** in the engine
  (`score` / `scope` / `validate` / `describe` / `group`). Adding a rule type is a compile-error-driven
  change across the interpreters — no behaviour lives on the rule.
- **DI is Koin** (no compiler plugin / KSP, plain Kotlin module DSL) — chosen so the wiring can move to
  `commonMain` for a future iOS target. The graph is verified by a `verify()` unit test.
- **The domain is UI- and framework-free**; `:ui` never sees Room/Android types.

## Tech stack

Kotlin 2.1.0 · AGP 8.7.3 · Gradle 8.14 · Compose Multiplatform 1.7.3 · Room 2.6.1 + KSP ·
Navigation-Compose · Koin 4.0 · kotlinx-coroutines / serialization · compileSdk 35 / minSdk 24.

## Building

> **JDK requirement:** the Gradle 8.14 daemon needs **JDK 21**. The foojay resolver auto-provisions the
> compile toolchain; if your host JDK is newer, pin the daemon JDK in your **user-global**
> `~/.gradle/gradle.properties` (`org.gradle.java.home=…/jdk-21`).

```bash
./gradlew :androidApp:assembleDebug        # build the Android debug APK
./gradlew jvmTest \
          :ui:jvmTest \
          :data:testDebugUnitTest \
          :androidApp:testDebugUnitTest    # full test suite (no emulator)
```

All tests run on the JVM (domain + UI reducers via `kotlin.test`; Room/repository tests via Robolectric;
ViewModels via `kotlinx-coroutines-test`). On Windows use `./gradlew` from Git Bash or `gradlew.bat`.

## Testing & the regression anchor

Every layer ships focused tests. The **Deep Sea §5 fixture** anchors scoring correctness at three
layers (domain engine, the score-entry reducer, and the score-entry state holder): **total 83**,
**ranked goal 8 / 8 / 0**. Treat any change to those numbers as a regression.

## Roadmap

- **iOS / Xcode** — a full Compose-Multiplatform app on iPhone with Room KMP persistence. Sequenced plan
  (macOS-gated build) in [`docs/superpowers/plans/2026-06-28-ios-xcode-target.md`](docs/superpowers/plans/2026-06-28-ios-xcode-target.md).
