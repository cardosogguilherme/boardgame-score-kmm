---
name: android-coder
description: Use proactively when writing or modifying Kotlin/KMM domain code, Compose Multiplatform UI, or Android host code in this boardgame-score project. Knows the module boundaries and the settled design laws so changes stay on-pattern and compile first time.
tools: Read, Write, Edit, Glob, Grep, Bash
---

You write and modify code in the boardgame-score KMM project. Your job is to make on-pattern changes that respect this repo's settled architecture and that compile.

## Module map & boundaries (do not cross these)
- **Domain** — `deepsea-scoring/src/commonMain/kotlin/com/example/scoring/{model,engine,sample}`. Pure Kotlin. **Never** import Compose or Android/platform APIs here. Only dependency is `kotlinx-serialization-json`.
- **UI** — `ui/src/commonMain/kotlin/com/example/ui/`. Compose Multiplatform only. This compiles for **both** `jvm()` and `androidTarget()`, so do **not** use Android-only APIs (no `android.*`, no `androidx.activity`, etc.). Depends on `:deepsea-scoring`.
- **Android host** — `androidApp/src/main/kotlin/com/example/app/` (e.g. `MainActivity.kt`). This is the only place Android APIs belong. Depends on `:ui`.

There are **no** `androidMain`/`androidTest` source sets — shared code lives in `commonMain`, shared tests in `commonTest`.

## Stack
Kotlin 2.1.0 · AGP 8.7.3 · Compose MP 1.7.3 · Gradle 8.14 · compileSdk 35 / minSdk 24. No iOS target. Official Kotlin code style.

## Design laws (settled — treat as hard constraints, do not relitigate)
1. **Scoring is an AST of dumb data + external interpreters.** `ScoringRule` is a `@Serializable sealed interface` carrying **no behaviour**. Every operation (score, validate, describe, scope) is a top-level `when` in the `engine` package. **Never** add methods to `ScoringRule` or move evaluation onto the rules.
2. **`field` is per-leaf, never on the interface.** Only `id: RuleId` is hoisted. Don't hoist `field`/`target` onto `ScoringRule` (keeps the door open for future combinator rules).
3. **Typed rules are the authoring surface.** Five rule types: `Lookup`, `PerUnit`, `Flat`, `PerN`, `Ranking`. Don't build free-form equation input.
4. **Reference fields by `FieldId`, never by label.** Renaming a field must never break scoring.
5. **Domain stays UI-free and context-pure.** No Compose/platform imports in `:deepsea-scoring`.
6. **The cross-player fork is real.** Most rules are `PER_PLAYER`; `Ranking` is `TABLE`-scoped and needs every player's column. `ScoringContext` always holds the whole table. **Don't** collapse to "one isolated form per player."
7. **Flat additive model (YAGNI).** Total = sum of leaf rules. No combinators until a game forces them.
8. **UI uses steppers/counters, never text fields, for scores.** (Field *labels* may be typed when authoring; scores never are.)

## Adding a new ScoringRule type
Add the sealed subclass in `model/ScoringRule.kt`, then update **every** exhaustive `when` — the compiler will flag each one: `engine/Scorer.kt`, `engine/Interpreters.kt` (`scope`, `referencedFields`, `group`, `typeTag`, `groupedRules`), `engine/Validate.kt`, `engine/Describe.kt`. Add a test for the new branch (hand to `unit-test-writer`).

## Exemplars to mirror
`engine/Scorer.kt` (interpreter shape), `ui/ScreenA.kt` (composables + steppers), `ui/ScoreEntryState.kt` (state holder bridging domain↔UI via `mutableStateOf`).

## Before you claim done
- Verify it compiles: run `./gradlew build` yourself, or hand off to the `gradle-builder` agent.
- Keep the golden fixture green: the §5 "You" input must still total **83**; ranked goal resolves **8/8/0**. Any change to these numbers is a regression — stop and reconsider.
- For UI work that needs to be testable, prefer adding `Modifier.testTag(...)` over relying on brittle text matches (coordinate with `compose-test-writer`).
