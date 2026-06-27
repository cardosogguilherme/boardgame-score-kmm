# Design: Rule builder, game-setup flow & layered architecture

**Date:** 2026-06-27
**Status:** Approved (design); pending implementation plan

## Context & goals

The app today launches straight into a single hard-coded score-entry screen
(`DeepSeaScreenA`) backed by `ScoreEntryState`, a state holder that wraps domain
models directly. There is no persistence, no ViewModel/interactor layering, and no
navigation. The screen also renders behind the device camera cutout because the host
activity is not inset-aware.

This work adds the two authoring/play screens from the implementation brief (§7 Screen B
and Screen C), introduces a clean layered architecture, and persists data locally.
Specifically the user wants:

1. A **rule-builder** screen to author a game-scoring template (fields + rules).
2. A **new-game** screen to start a scoring session and add players, flowing into score entry.
3. Local **persistence** (Room).
4. **MVVM with interactors** (use cases).
5. **Anti-corruption layers** — distinct models/DTOs per layer with mappers between them.
6. Correct handling of the **toolbar / cutout insets** (edge-to-edge).

### Decisions taken during brainstorming
- **Architecture target: Hybrid.** Interactors, repository *interfaces*, and domain/UI
  models live in `commonMain` (JVM-testable, no emulator). ViewModels and the Room
  implementation live Android-side.
- **Persistence: Room** (relational; observable `Flow` queries; good for browsing a
  library of templates and a list of games).
- **Navigation: Navigation-Compose** in `:androidApp`; `:ui` composables stay
  navigation-agnostic.
- **Rule builder: full brief §7 scope** (all 5 rule types, `describe()` rendering,
  inline `validate()`).
- **Game flow: setup → reuse existing score entry** with autosave.

## Architecture

Three model sets, mapped at every boundary (the anti-corruption requirement):

| Layer | Module / source set | Models |
|-------|---------------------|--------|
| Domain | `:deepsea-scoring` `commonMain` | `Template`, `Field`, `ScoringRule`, `ResolvedTemplate`, `PlayerInput`, **new** `Game` |
| Data (DTO) | `:data` (new Android library) | Room `@Entity` classes |
| Presentation (UI) | `:ui` `commonMain` | `*UiState` classes |

```
:deepsea-scoring (commonMain) ── DOMAIN
  model/        existing + new Game, GameId, GameStatus
  engine/       existing Scorer / Validate / Describe / Interpreters …
  repository/   TemplateRepository, GameRepository   (interfaces; return domain models as Flow)
  interactors/  use cases (operator invoke) depending on the repo interfaces

:data (NEW Android library, depends on :deepsea-scoring) ── DATA
  entity/       Room @Entity DTOs
  dao/          Room DAOs (Flow queries)
  AppDatabase   RoomDatabase
  mapper/       entity ↔ domain
  repository/   Room-backed implementations of the domain repo interfaces

:ui (commonMain, Compose MP) ── PRESENTATION (models + dumb UI)
  model/        UiState classes
  mapper/       domain → UI
  screens/      stateless composables (take UiState + callbacks)

:androidApp ── PRESENTATION edges + composition root
  viewmodel/    androidx.lifecycle ViewModels: call interactors, expose StateFlow<UiState>, autosave
  nav/          Navigation-Compose NavHost
  di/           manual AppContainer + ViewModelFactory (no Hilt/Koin)
  MainActivity  edge-to-edge host
```

**Data flow:** `Composable → callback → ViewModel → Interactor → Repository (interface)
→ Room impl`, and back: `Room Flow → domain model → ViewModel maps to UiState →
StateFlow → Composable`.

**Module dependency direction:** `:androidApp → {:ui, :data, :deepsea-scoring}`,
`:data → :deepsea-scoring`, `:ui → :deepsea-scoring`. The domain module depends on
nothing new; `:data` and `:ui` never depend on each other.

### Design choices & rationale
- **Dedicated `:data` module** rather than burying Room inside `:androidApp` — keeps the
  data layer reusable and the anti-corruption boundary explicit. It is an Android library
  (Room is Android-side in the hybrid model).
- **Manual DI** (`AppContainer` constructed in `Application`/`MainActivity`, a
  `ViewModelProvider.Factory`) rather than Hilt/Koin — avoids a new framework; can be
  swapped later.
- **Repository interfaces in the domain module** so interactors stay multiplatform and
  unit-testable against fakes.

## Domain additions (`:deepsea-scoring`)

```kotlin
@Serializable @JvmInline value class GameId(val raw: String)
@Serializable enum class GameStatus { IN_PROGRESS, FINISHED }

@Serializable
data class Game(
    val id: GameId,
    val templateId: String,
    val scenarioId: String? = null,
    val name: String,
    val status: GameStatus = GameStatus.IN_PROGRESS,
    val players: List<PlayerInput> = emptyList(),
)
```

Repository interfaces (return domain models; reads are `Flow`):

```kotlin
interface TemplateRepository {
    fun observeTemplates(): Flow<List<Template>>
    suspend fun getTemplate(id: String): Template?
    suspend fun saveTemplate(template: Template)
    suspend fun deleteTemplate(id: String)
}

interface GameRepository {
    fun observeGames(): Flow<List<Game>>
    suspend fun getGame(id: GameId): Game?
    suspend fun createGame(game: Game): GameId
    suspend fun updatePlayerValue(id: GameId, player: PlayerId, field: FieldId, value: Int)
    suspend fun addPlayer(id: GameId, player: PlayerInput)
    suspend fun removePlayer(id: GameId, player: PlayerId)
    suspend fun setStatus(id: GameId, status: GameStatus)
    suspend fun deleteGame(id: GameId)
}
```

Interactors — thin use cases (one responsibility, `operator fun invoke`):
`ObserveTemplates`, `GetTemplate`, `SaveTemplate` (runs `ResolvedTemplate.validate()`
and rejects on errors), `DeleteTemplate`, `ObserveGames`, `GetGame`, `CreateGame`,
`UpdatePlayerValue`, `ScoreGame` (delegates to `Scorer.breakdown`/`total`), `FinishGame`.

This adds the `kotlinx-coroutines-core` dependency to the domain module for `Flow`.

> **Convention note (flagged, not silently chosen):** brief §8 says "keep the domain
> module dependency-free beyond serialization." Putting `Flow`-returning repository
> interfaces in the domain relaxes that with a single lightweight multiplatform
> dependency (`kotlinx-coroutines-core`). The accepted alternative is a separate
> use-case/contracts module so `:deepsea-scoring` stays purely serialization-only. We take
> the coroutines-in-domain path for simplicity; raise it if you'd rather keep §8 strict.

## Data layer (`:data`, Room)

Tables (entities are the data-layer DTOs):

```
template(id PK, name)
field(id, templateId FK, label, kind, max, position)
rule(id, templateId FK, type, payloadJson, position)        -- ScoringRule serialized per-type
game(id PK, templateId, scenarioId, name, status, createdAt)
player(id, gameId FK, name, position)
player_value(gameId, playerId, fieldId, value)              -- composite PK
```

- `rule.payloadJson` stores the type-specific fields of a `ScoringRule` via
  kotlinx-serialization; `rule.type` is the discriminator. (Avoids a column-per-rule-type
  while keeping the table queryable by template.)
- DAOs expose `Flow<List<…>>` for the library lists.
- **Mappers** (`entity ↔ domain`) live here and are pure functions, unit-tested.
- Repository implementations assemble/disassemble the aggregate graphs (a `Template` with
  its fields+rules; a `Game` with its players+values) inside `@Transaction` methods.

## Presentation layer

### UI state models (`:ui` `commonMain`)
Immutable data classes, one per screen, plus row/item models, e.g.
`LibraryUiState`, `RuleBuilderUiState` (fields, rules-as-`describe()` lines, validation
messages, palette), `NewGameUiState`, `ScoreEntryUiState` (sections, running total,
per-category breakdown). Domain→UI mappers live alongside them.

### ViewModels (`:androidApp`)
`androidx.lifecycle.ViewModel` exposing `StateFlow<UiState>`; collect repository `Flow`s,
call interactors on events, map domain→UI. One per screen: `LibraryViewModel`,
`RuleBuilderViewModel`, `NewGameViewModel`, `ScoreEntryViewModel`.

### Existing score-entry refactor
`ScoreEntryScreen` becomes **stateless** (takes `ScoreEntryUiState` + callbacks). The
existing score-reduction logic currently in `ScoreEntryState` is kept as a shared,
JVM-tested presentation reducer in `:ui` `commonMain` (it already produces sections/total
from a `ScoringContext`); `ScoreEntryViewModel` wraps it and autosaves each edit through
`GameRepository`. **The §5 fixture must still resolve to a total of 83** — the existing
`ScoreEntryStateTest`/`DeepSeaScoringTest` assertions stay green.

## Screens

- **Home / Library** — two Room-backed lists: saved templates (tap → edit; overflow →
  start game / delete) and saved games (tap → resume). FAB → new template. Empty states.
- **Rule builder** (brief §7) — template name; add/remove fields (`label` + `FieldKind`
  COUNT/TRACK/RANKING + optional `max`); "Add rule" palette of all five types
  (`Lookup`, `PerUnit`, `Flat`, `PerN`, `Ranking`) each with a one-line "use when" hint;
  each added rule rendered as its plain-language `describe(labels)` line with a type tag
  and delete; `validate()` results shown inline (e.g. a `Ranking` rule on a non-`RANKING`
  field is a visible error, not a crash); Save → Room (blocked while invalid).
- **New game** — pick a saved template + optional scenario, add/name N players (list with
  remove), Start → persists `Game`+players → navigates to score entry.
- **Score entry** — the existing `ScoreEntryScreen`, now stateless and bound to a
  persisted `Game` via `ScoreEntryViewModel`; edits autosave. Steppers only (no text entry
  for scores), live total + per-category breakdown, and the TABLE-scoped ranked section
  across all players — all unchanged behaviours.

## Navigation & insets

- `MainActivity` calls `enableEdgeToEdge()`.
- A Navigation-Compose `NavHost` in `:androidApp`; routes: `Home`, `RuleBuilder(templateId?)`,
  `NewGame(templateId?)`, `ScoreEntry(gameId)`. `:ui` screens receive state + callbacks;
  routing/ViewModel wiring is owned by `:androidApp`.
- Every screen is wrapped in a Material3 `Scaffold` with a `TopAppBar` (the toolbar) and
  consumes `innerPadding` + `WindowInsets.safeDrawing`, so content no longer draws behind
  the camera cutout / status bar.

## Testing (all JVM, no emulator)

- **Interactors** — JVM unit tests against in-memory fake repositories (`commonTest`).
- **Mappers** — JVM tests for `entity ↔ domain` and `domain → UI` (round-trip where
  applicable).
- **Room DAOs / repository impls** — in-memory Room + **Robolectric** unit tests in
  `:data` (off the emulator).
- **Compose screens** — `runComposeUiTest` (JVM) feeding fixed `UiState` to the stateless
  screens; assert rendering and that callbacks fire (use the `compose-test-writer` infra).
- **ViewModels** — JVM tests with `kotlinx-coroutines-test` + fake interactors.
- **Regression anchor** — the Deep Sea §5 fixture still totals **83**; ranked goal resolves
  **8 / 8 / 0**.

## New dependencies
- `kotlinx-coroutines-core` (domain, for `Flow`).
- Room (`room-runtime`, `room-ktx`, KSP `room-compiler`) in `:data`.
- `androidx.navigation:navigation-compose` and `androidx.lifecycle:lifecycle-viewmodel-compose`
  in `:androidApp`.
- `kotlinx-coroutines-test`, Robolectric, and `room-testing` for tests.
- All versions added to `gradle/libs.versions.toml`; verify against Kotlin 2.1.0 / AGP 8.7.3.

## Implementation order (three plans)

The work is large; the implementation plan(s) should split it into three sequential
increments, each independently buildable and tested:

- **A — Foundation:** new `:data` module + Room schema/DAOs/mappers; domain `Game` model,
  repository interfaces, interactors; manual DI; `enableEdgeToEdge()` + `Scaffold`/`TopAppBar`
  insets; Navigation-Compose `NavHost` skeleton with a Home/Library screen. Migrate
  `DeepSeaScreenA` to run under the inset-aware host.
- **B — Rule builder:** the full §7 authoring screen + `RuleBuilderViewModel`, saving
  templates to Room, inline validation.
- **C — Game flow:** new-game/add-players screen + `NewGameViewModel`; refactor
  `ScoreEntryScreen` to stateless + `ScoreEntryViewModel` with autosave; resume from the
  library.

## Out of scope (deferred — flag, don't silently add)
- Template export/import/sharing (the model is serializable; a later step).
- iOS target / fully-multiplatform ViewModels & persistence.
- Combinator rule types (Conditional/Max/Multiplier) — only when a game needs them.
- Confirming the placeholder Deep Sea track tables against the physical board.
- Authentication / cloud sync.
