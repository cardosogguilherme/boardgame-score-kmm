# Board Game Score Engine — Implementation Brief

> Hand this to Claude Code as repo context (or merge into `CLAUDE.md`). It captures the
> design decisions already made so they don't get relitigated, documents what exists, and
> specifies the next task. Read "Design law" before changing anything in the domain module.

---

## 1. What this project is

A Kotlin Multiplatform app that lets a user **customize a form that scores board games**.
A user composes a *scoring template* (a list of fields + a list of named scoring rules);
the app then renders a data-entry screen from that template and computes each player's score.

First target game: **Endeavor: Deep Sea** (a point-salad Euro). The engine is game-agnostic;
Deep Sea is just the first template and the end-to-end test fixture.

The central product bet: **users never write equations.** Scoring is built from a small
palette of typed rules, each of which renders as one plain-language line. This is what makes
templates readable and authorable on a phone. (See Design law §3.)

---

## 2. Current state (already built)

A pure-Kotlin `commonMain` domain module exists: **`deepsea-scoring`**. No platform/UI deps,
fully unit-tested without an emulator. Package root: `com.example.scoring` (rename to taste).

```
deepsea-scoring/
├── build.gradle.kts          KMP + kotlin("plugin.serialization"), Kotlin 2.1.0
├── settings.gradle.kts
└── src/
    ├── commonMain/kotlin/com/example/scoring/
    │   ├── model/    Ids · Field · ScoringRule (sealed AST) · Template · Scenario · PlayerInput
    │   ├── engine/   ScoringContext · Scorer · Interpreters · Validation
    │   └── sample/   DeepSea  (Endeavor: Deep Sea as template data)
    └── commonTest/kotlin/com/example/scoring/
        └── DeepSeaScoringTest  (end-to-end: score, breakdown, tie rule, validation, JSON round-trip)
```

**Status:** logic verified (via an equivalent run), but Gradle has not been executed in CI yet.
Run `./gradlew jvmTest` first; if it fails it will be a dependency-version bump, not logic.

---

## 3. Design law (do not relitigate)

These are settled. Treat them as constraints, not suggestions.

1. **Scoring is an AST of dumb data + external interpreters.** `ScoringRule` is a `@Serializable`
   `sealed interface`. Rules carry **no behaviour**. Every operation (score, validate, describe,
   scope) is a top-level `when` over the sealed type in the `engine` package. Adding a rule type
   must force a compile error at each interpreter. **Do not** add methods to `ScoringRule` or move
   evaluation onto the rules.

2. **`field` is per-leaf, never on the interface.** Only `id: RuleId` is hoisted to the interface.
   This keeps the door open for future *combinator* rules (Conditional / Max / Multiplier) that
   contain other rules and have no single field. **Do not** hoist a `field`/`target` onto
   `ScoringRule`.

3. **Typed rules are the authoring surface; a formula is at most a derived view.** The five rule
   types cover the real games: `Lookup`, `PerUnit`, `Flat`, `PerN` (1 pt per N, floor div),
   `Ranking`. **Do not** build free-form equation entry as the primary authoring path. A read-only
   "compiles to" formula view is fine; equation *input* is not.

4. **Reference fields by `FieldId`, never by label.** Renaming a field must never break scoring.

5. **Behaviour stays UI-free and context-pure.** The domain module never imports Compose or
   platform APIs. UI sits on top.

6. **The cross-player fork is real and already modelled.** Most rules are `PER_PLAYER`. `Ranking`
   is `TABLE`-scoped: it needs every player's column, not one sheet. `ScoringContext` always holds
   the whole table; per-player rules simply ignore the rest. Validation enforces that a `TABLE`
   rule sits on a `RANKING` field. **Do not** refactor toward "one isolated form per player."

7. **Flat additive model for now (YAGNI).** Total = sum of leaf rules. Combinators are deferred
   until a game needs them; §3.2 keeps that change to one file + the interpreter `when`s.

---

## 4. Domain model reference

```kotlin
@Serializable @JvmInline value class FieldId(val raw: String)
@Serializable @JvmInline value class PlayerId(val raw: String)
@Serializable @JvmInline value class RuleId(val raw: String)

@Serializable enum class FieldKind { COUNT, TRACK, RANKING }
@Serializable data class Field(val id: FieldId, val label: String, val kind: FieldKind, val max: Int? = null)

@Serializable enum class RuleScope { PER_PLAYER, TABLE }

@Serializable sealed interface ScoringRule {
    val id: RuleId
    @SerialName("lookup")  data class Lookup(override val id: RuleId, val field: FieldId, val table: List<Int>) : ScoringRule
    @SerialName("perUnit") data class PerUnit(override val id: RuleId, val field: FieldId, val points: Int) : ScoringRule
    @SerialName("flat")    data class Flat(override val id: RuleId, val field: FieldId) : ScoringRule
    @SerialName("perN")    data class PerN(override val id: RuleId, val field: FieldId, val divisor: Int) : ScoringRule
    @SerialName("ranking") data class Ranking(override val id: RuleId, val field: FieldId, val awards: List<Int>) : ScoringRule
}

@Serializable data class PlayerInput(val id: PlayerId, val name: String, val values: Map<FieldId, Int> = emptyMap())

@Serializable data class Template(val id: String, val name: String, val fields: List<Field>,
                                   val rules: List<ScoringRule>, val scenarios: List<Scenario> = emptyList())
@Serializable data class Scenario(val id: String, val name: String,
                                   val fields: List<Field> = emptyList(), val rules: List<ScoringRule> = emptyList())

data class ResolvedTemplate(val fields: List<Field>, val rules: List<ScoringRule>) { /* + labels map */ }
fun Template.resolve(scenarioId: String? = null): ResolvedTemplate   // base + chosen scenario, flattened
```

Interpreters (each an exhaustive `when`):
- `Scorer.score(rule, player, ctx): Int`, plus `total(...)` and `breakdown(...): Map<RuleId,Int>`.
- `ScoringRule.scope(): RuleScope`
- `ScoringRule.referencedFields(): Set<FieldId>`
- `ScoringRule.describe(labels): String`  ← plain-language rendering for the UI
- `ResolvedTemplate.validate(): List<String>`  ← empty == valid

**Ranking tie rule (Deep Sea):** tied top players each take the top award; second place is then
not scored. Implemented in `rankAward(value, column, awards)`.

---

## 5. Deep Sea reference data + acceptance numbers

Base rules: 4 attribute tracks (Reputation, Inspiration, Coordination, Ingenuity) as `Lookup`;
impact-board discs in 1-pt / 2-pt spaces as `PerUnit(1)` / `PerUnit(2)`; journal & specialist
points as `Flat`; leftover research+staging discs as `PerN(3)`. Scenario 1 overlay: discs-placed
ranked goal `Ranking([8,4])` + `PerUnit(1)` bonus per disc.

> ⚠️ Track point tables in `DeepSea.kt` are **placeholders** (`[0,1,3,6,10,14,19,25]`). Confirm the
> real thresholds from the physical board / rulebook before treating scores as authoritative.

**Golden test fixture** (3 players; You & Mira tie at top of the ranked goal, Kane trails):

| input (You) | rep 4 · ins 3 · coo 5 · ing 2 · impact1 6 · impact2 4 · journal 9 · specialist 5 · leftover 7 · discsPlaced 12 |
|---|---|
| Mira | discsPlaced 12 |
| Kane | discsPlaced 8 |

Expected: **You total = 83.** Per-rule: rep 10, ins 6, coo 14, ing 3, imp1 6, imp2 8, jrn 9,
spc 5, res 2, goal 8, goalBonus 12. Ranked goal across players = **8 / 8 / 0** (top tie → second
skipped). Any change that breaks these numbers is a regression.

---

## 6. Roadmap (priority order)

1. **Compose Multiplatform UI** on top of the domain module — the immediate task (§7).
2. **Persistence** of templates and in-progress games (kotlinx.serialization JSON →
   multiplatform-settings or SQLDelight; decide and document).
3. **Template authoring screen** wired to a real palette + validation surfacing.
4. **Template sharing** (export/import JSON; the model is already serializable).
5. Combinator rule types **only when** a specific game forces them.

---

## 7. Immediate task: Compose Multiplatform UI

Build a CMP UI in a new source set that depends on `deepsea-scoring`. Mobile-first (target is a
phone). Two prototype screens already exist as React sketches (`DeepSeaCalculator.jsx`,
`TemplateBuilder.jsx`) — use them as **visual reference only**; the behaviours below are the spec.

### Screen A — Score entry (build this first)
- Driven entirely by a `ResolvedTemplate`. Render one section per logical group of rules; render
  each rule as a row: label + an input control + its live points contribution + a small rule-type
  tag (`LOOKUP`, `×n`, `FLAT`, `÷n`, `RANK`).
- **Controls are steppers / counters, not text fields** for numeric input — no on-screen keyboard
  for scoring. (Field *labels* may be typed when authoring; scores never are.)
- Live running total + a per-category breakdown, sourced from `Scorer.breakdown`.
- The **ranked-goal section takes all players' values** (it's `TABLE`-scoped) and resolves rank
  live, demonstrating the shared-state fork. Do not try to score it from one player's sheet.
- State holder: a `ViewModel`/state class in the UI module wrapping `ScoringContext`; the domain
  module stays untouched. Acceptance: entering the §5 fixture shows **83**.

### Screen B — Template builder (second)
- Lists fields (add/remove; label + `FieldKind`) and rules.
- "Add rule" opens a palette of the 5 rule types, each with a one-line "use when". Picking one
  appends a rule with sensible defaults bound to a chosen field.
- Each rule renders via `describe(labels)` as a plain-language line — this is the readability test.
- Surface `validate()` results inline (e.g. attaching a `Ranking` rule to a non-`RANKING` field is
  a visible error, not a crash).
- Optional: a collapsible read-only "compiles to" formula view. Never an editable formula.

### Screen C — Multi-player game flow (later)
- N players, per-player entry, a results view ranking final totals. This is where `TABLE`-scoped
  rules and the tie rule matter most.

---

## 8. Conventions & stack

- Kotlin **2.1.0**, `kotlin("multiplatform")` + `kotlin("plugin.serialization")`.
- `org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3` (bump to latest stable; verify build).
- Targets: `jvm()` + iOS in the domain module; add `androidTarget()` when wiring the app (needs AGP
  + namespace). UI module: Compose Multiplatform (Android + iOS + desktop as desired).
- Official Kotlin code style. Keep the domain module dependency-free beyond serialization.
- Tests: `kotlin.test` in `commonTest`. Every new rule type and every interpreter branch gets a test.

---

## 9. Guardrails (things NOT to do)

- ❌ Don't add behaviour/methods to `ScoringRule` — interpreters only.
- ❌ Don't hoist `field`/`target` onto the `ScoringRule` interface.
- ❌ Don't build editable free-form equation input as the authoring path.
- ❌ Don't reference fields by label anywhere in scoring.
- ❌ Don't import Compose/platform APIs into the domain module.
- ❌ Don't collapse the cross-player model into isolated per-player forms.
- ❌ Don't treat the placeholder track tables as final scoring values.

---

## 10. Deferred decisions (flag, don't silently choose)

- **Persistence layer**: multiplatform-settings vs SQLDelight vs flat JSON files.
- **Grouping metadata**: `breakdown` is keyed by `RuleId`; the UI needs section grouping. Decide
  whether groups live on the `Template` (a `groupId` per rule) or are derived. Currently neither —
  add explicitly if Screen A needs it.
- **`FieldValue` typing**: inputs are `Int` today. If a game needs booleans/enum picks, introduce a
  `sealed FieldValue` and migrate `Map<FieldId, Int>` → `Map<FieldId, FieldValue>`. Not yet.
- **Rule ordering / reordering** in the builder (drag handles) — `RuleId` already supports it.
