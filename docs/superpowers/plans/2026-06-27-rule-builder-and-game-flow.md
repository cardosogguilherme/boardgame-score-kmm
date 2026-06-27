# Implementation Plan — Increments B & C: Rule Builder + Game Flow

Design: docs/superpowers/specs/2026-06-27-scoring-builder-and-game-flow-design.md
Branch: feature/scoring-builder-and-game-flow
Builds on: Foundation increment (Tasks 1–10) — :data + Room + repos, domain Game + interactors,
Koin DI, edge-to-edge host + Navigation-Compose skeleton, stateless LibraryScreen.

## Why this increment
The foundation wired data/DI/nav but stubbed the create flows: there is no "+" affordance, and
`onNewTemplate`/`onOpenTemplate` are no-ops; `SCORE_ENTRY` renders the demo `DeepSeaScreenA`. This
increment delivers the two screens from the original brief — **author scoring rules** and **start a
game + add players** — with working navigation throughout. User chose **full scope** for both.

## Global Constraints (bind every task)
- **Anti-corruption layers stay intact.** Domain (`:deepsea-scoring` commonMain) ↔ UI state (`:ui`
  commonMain `model/`) via pure mappers; ViewModels (`:androidApp`) call interactors and expose
  `StateFlow<UiState>`. No Room/Android types in `:ui`; no Compose in domain.
- **DI is Koin.** New ViewModels get `viewModel { }` bindings in `appModule`; screens obtain them via
  `koinViewModel()`. No Hilt, no manual container.
- **Domain is read-only here.** Reuse existing models/engine/interactors — do NOT modify
  `:deepsea-scoring` except to add a tiny id-generation helper IF unavoidable (flag it). All 5 rule
  types already exist: `Lookup(field, table)`, `PerUnit(field, points)`, `Flat(field)`,
  `PerN(field, divisor)`, `Ranking(field, awards)`; `FieldKind` = COUNT/TRACK/RANKING.
- **Steppers only for scores** (never a text field for a numeric score). Text fields ARE allowed for
  template/field/player NAMES and for rule numeric params in the builder (points/divisor/table/awards).
- **Validation reuses the engine.** Rule-builder validity = `template.resolve().validate()` (already
  enforces "TABLE/Ranking rule needs a RANKING field", non-empty Lookup table, PerN divisor≠0,
  non-empty Ranking awards, unknown-field refs). `SaveTemplate` already validates and rejects.
- **Regression anchor (non-negotiable):** the Deep Sea §5 fixture still totals **83**, ranked goal
  **8/8/0**. `DeepSeaScoringTest` + `ScoreEntryStateTest` stay green. Any change to these numbers is a
  regression — STOP and report.
- **TDD per task** (RED→GREEN), JVM tests only (no emulator). Each task: implement → test → commit →
  task-review → fix loop. Build via `./gradlew` (Git Bash), daemon JDK pinned to 21.

## Real API reference (verified against source — use these exact shapes)
- `Template(id: String, name, fields: List<Field>, rules: List<ScoringRule>, scenarios: List<Scenario> = [])`
- `Field(id: FieldId, label: String, kind: FieldKind, max: Int? = null)`
- `ScoringRule.describe(labels: Map<FieldId,String>): String`; `ScoringRule.typeTag(): String`
  (LOOKUP/×n/FLAT/÷n/RANK); `ScoringRule.scope()`; `ResolvedTemplate.validate(): List<String>`;
  `Template.resolve(scenarioId: String? = null): ResolvedTemplate`.
- Ids are `@JvmInline value class …(val raw: String)`: `FieldId`, `RuleId`, `PlayerId`. `GameId(raw)`.
- `PlayerInput(id: PlayerId, name: String, values: Map<FieldId,Int> = {})`.
- `Game(id: GameId, templateId: String, scenarioId: String? = null, name: String,
  status: GameStatus = IN_PROGRESS, players: List<PlayerInput> = [])`.
- Interactors (all `operator invoke`): `ObserveTemplates()→Flow<List<Template>>`,
  `GetTemplate(id)→Template?`, `SaveTemplate(template)→List<String>` (errors; persists iff empty),
  `DeleteTemplate(id)`, `ObserveGames()→Flow<List<Game>>`, `GetGame(GameId)→Game?`,
  `CreateGame(Game)→GameId`, `UpdatePlayerValue(GameId, PlayerId, FieldId, Int)`, `FinishGame(GameId)`.
- Existing score reducer logic lives in `ui/.../ScoreEntryState.kt` (sections/total/breakdown from a
  `ScoringContext`); existing stateful `ScoreEntryScreen(state: ScoreEntryState)` in `ui/.../ScreenA.kt`,
  wrapped by `ui/.../DeepSeaScreenA.kt`.

---

## INCREMENT B — Rule Builder

### Task B1: RuleBuilderUiState + pure edit helpers + mapper (`:ui` commonMain)
**Files:** Create `ui/src/commonMain/kotlin/com/example/ui/model/RuleBuilderUiState.kt`;
Test `ui/src/commonTest/kotlin/com/example/ui/model/RuleBuilderUiStateTest.kt`.

Produce:
- `enum class RuleTypeOption { LOOKUP, PER_UNIT, FLAT, PER_N, RANKING }` each with a `hint: String`
  ("use when…") and a `label`.
- `data class FieldRowUi(val id: String, val label: String, val kind: FieldKind, val max: Int?)`
- `data class RuleRowUi(val id: String, val describe: String, val tag: String)`
- `data class RuleBuilderUiState(val templateId: String, val name: String,
  val fields: List<FieldRowUi>, val rules: List<RuleRowUi>, val palette: List<RuleTypeOption>,
  val errors: List<String>, val canSave: Boolean)`
- `fun ruleBuilderUiState(draft: Template): RuleBuilderUiState` — renders each rule via
  `describe(draft.resolve().labels)` + `typeTag()`, errors via `draft.resolve().validate()`,
  `canSave = errors.isEmpty() && draft.name.isNotBlank() && draft.fields.isNotEmpty() && draft.rules.isNotEmpty()`.
- Pure edit helpers operating on domain `Template` (returning a new Template), generating fresh ids:
  `fun Template.renamed(name: String)`, `fun Template.withFieldAdded(label, kind, max)`,
  `fun Template.withFieldRemoved(fieldId: String)`, `fun Template.withRuleRemoved(ruleId: String)`,
  and `fun Template.withRuleAdded(type: RuleTypeOption, fieldId: FieldId, params: RuleParams)` where
  `data class RuleParams(val points: Int = 1, val divisor: Int = 1, val table: List<Int> = emptyList(),
  val awards: List<Int> = emptyList(), val group: String? = null)`. Generate ids like
  `"field-${'$'}{fields.size+1}"` / `"rule-${'$'}{rules.size+1}"` (uniqueness within the draft is enough).
- A factory `fun emptyTemplateDraft(id: String): Template` (blank name, no fields/rules).

**TDD:** RED test asserting (a) `ruleBuilderUiState(DeepSea.template)` renders the 9 base rules' describe
lines + tags and `canSave == true`; (b) a draft with a `Ranking` rule on a COUNT field yields a
non-empty `errors` and `canSave == false`; (c) edit helpers: add a field + a `PerUnit` rule then map →
the new rule appears with tag `×n`. Run `./gradlew :ui:jvmTest --tests "*RuleBuilderUiStateTest"`.

### Task B2: stateless RuleBuilderScreen (`:ui` commonMain)
**Files:** Create `ui/src/commonMain/kotlin/com/example/ui/screens/RuleBuilderScreen.kt`.
Stateless `@Composable fun RuleBuilderScreen(state: RuleBuilderUiState, onNameChange: (String)->Unit,
onAddField: (label: String, kind: FieldKind, max: Int?)->Unit, onRemoveField: (String)->Unit,
onAddRule: (RuleTypeOption, fieldId: String, RuleParams)->Unit, onRemoveRule: (String)->Unit,
onSave: ()->Unit, modifier: Modifier = Modifier)`. Renders: name `TextField`; fields editor (list with
delete + an add-field row: label text, `FieldKind` selector, optional max); "Add rule" palette (the 5
options each showing its hint) that, on pick, lets the user choose a target field + params then calls
`onAddRule`; rule list each as its `describe` line + type tag + delete; an inline errors panel
(`state.errors`); a Save button enabled only when `state.canSave`. Compose MP imports only (foundation,
material3, runtime, ui). Keep sub-composables `private`. No ViewModel, no domain mutation inside.
**Verify:** `./gradlew :ui:jvmTest` compiles + existing tests stay green. (Compose UI test optional —
include a `runComposeUiTest` smoke test only if the `compose.uiTest` infra is already wired; otherwise
note it deferred and rely on B1's logic coverage.)

### Task B3: RuleBuilderViewModel + Koin binding (`:androidApp`)
**Files:** Create `androidApp/src/main/kotlin/com/example/app/viewmodel/RuleBuilderViewModel.kt`;
Modify `di/AppModule.kt`; Test `androidApp/src/test/kotlin/com/example/app/viewmodel/RuleBuilderViewModelTest.kt`.
- `class RuleBuilderViewModel(private val getTemplate: GetTemplate, private val saveTemplate: SaveTemplate,
  templateId: String?) : ViewModel()` — holds `draft: Template` (load via `getTemplate(templateId)` in an
  `init` coroutine if non-null, else `emptyTemplateDraft(newId)`), exposes
  `uiState: StateFlow<RuleBuilderUiState>` (map draft → `ruleBuilderUiState`). Edit callbacks
  (`onNameChange/onAddField/onRemoveField/onAddRule/onRemoveRule`) apply the pure helpers to `draft` and
  re-emit. `save(onDone: () -> Unit)` calls `saveTemplate(draft)`; if returned errors empty, invoke
  `onDone` (navigation), else surface errors in state (`saveErrors`).
- Koin: `viewModel { (templateId: String?) -> RuleBuilderViewModel(get(), get(), templateId) }`
  (parameter-injected; import the Koin viewModel DSL as used in foundation).
**TDD:** RED VM test with fake interactors (reuse the duplicated `com.example.app.testing` fakes or the
interactors over fakes): construct VM (templateId null) → onNameChange("T") → onAddField → onAddRule →
`save{}` → assert SaveTemplate persisted (fake repo now has the template) OR, for an invalid draft,
assert errors surfaced and nothing persisted. Use `runTest` + `Dispatchers.setMain(UnconfinedTestDispatcher())`
(as in LibraryViewModelTest). Run `./gradlew :androidApp:testDebugUnitTest --tests "*RuleBuilderViewModelTest"`.

### Task B4: Navigation restructure + "+" FAB + RuleBuilder route
**Files:** Modify `androidApp/.../MainActivity.kt`, `androidApp/.../nav/AppNavHost.kt`; optionally add
`androidApp/.../nav/Routes.kt`.
- Restructure so each route owns its **own** `Scaffold` + `TopAppBar` (the design's intent), and
  `MainActivity` only provides `enableEdgeToEdge()` + `MaterialTheme` + `AppNavHost()` (remove the single
  shared Scaffold). Sub-screens get a back arrow; Home gets the title + a **FAB ("+")** → navigate to the
  RuleBuilder route (new template). Apply `innerPadding`/`WindowInsets.safeDrawing` on every route so the
  cutout fix is preserved per screen.
- Add `RULE_BUILDER` route with an optional `templateId` nav arg (`"rule_builder?templateId={templateId}"`).
  Home's `onNewTemplate` → `rule_builder` (no arg); `onOpenTemplate(id)` → `rule_builder?templateId=id`.
  In the route, obtain `RuleBuilderViewModel` via `koinViewModel { parametersOf(templateId) }`, collect
  state, render `RuleBuilderScreen`, and on successful save `navController.popBackStack()`.
**Verify:** `./gradlew :androidApp:assembleDebug` BUILD SUCCESSFUL. Manual QA note: "+" appears on Home,
opens the builder; back returns; saving a valid template returns to Home where it now appears in the list.

---

## INCREMENT C — Game Flow

### Task C1: NewGameUiState + reducer (`:ui` commonMain)
**Files:** Create `ui/src/commonMain/kotlin/com/example/ui/model/NewGameUiState.kt`;
Test `ui/src/commonTest/.../NewGameUiStateTest.kt`.
- `data class TemplateOptionUi(val id: String, val name: String)`,
  `data class ScenarioOptionUi(val id: String, val name: String)`,
  `data class PlayerDraftUi(val tempId: String, val name: String)`,
  `data class NewGameUiState(val templates: List<TemplateOptionUi>, val selectedTemplateId: String?,
  val scenarios: List<ScenarioOptionUi>, val selectedScenarioId: String?, val players: List<PlayerDraftUi>,
  val gameName: String, val canStart: Boolean)`.
- `fun newGameUiState(templates: List<Template>, selectedTemplateId: String?, selectedScenarioId: String?,
  players: List<PlayerDraftUi>, gameName: String): NewGameUiState` — scenarios come from the selected
  template's `scenarios`; `canStart = selectedTemplateId != null && players.isNotEmpty() &&
  players.all { it.name.isNotBlank() } && gameName.isNotBlank()`.
**TDD:** RED test: with `listOf(DeepSea.template)` selected, scenarios include `scenario-1`; with ≥1 named
player + game name, `canStart == true`; empty players → `canStart == false`.

### Task C2: stateless NewGameScreen (`:ui` commonMain)
**Files:** Create `ui/src/commonMain/kotlin/com/example/ui/screens/NewGameScreen.kt`.
Stateless screen: template picker (radio/dropdown), optional scenario picker, game-name text field, player
list (add row, name text, remove), Start button enabled by `canStart`. Callbacks: `onSelectTemplate(id)`,
`onSelectScenario(id?)`, `onGameNameChange`, `onAddPlayer`, `onPlayerNameChange(tempId, name)`,
`onRemovePlayer(tempId)`, `onStart`. Compose MP only; `private` sub-composables.
**Verify:** `./gradlew :ui:jvmTest` compiles + green.

### Task C3: NewGameViewModel + Koin binding (`:androidApp`)
**Files:** Create `androidApp/.../viewmodel/NewGameViewModel.kt`; modify `di/AppModule.kt`;
Test `androidApp/.../viewmodel/NewGameViewModelTest.kt`.
- `class NewGameViewModel(observeTemplates: ObserveTemplates, private val createGame: CreateGame) : ViewModel()`
  — collects templates into state, holds selection/players/gameName, maps via `newGameUiState`. `start(onCreated:
  (GameId) -> Unit)` builds a domain `Game` (fresh `GameId`, selected templateId/scenarioId, name, players as
  `PlayerInput(PlayerId(fresh), name)`), calls `createGame(game)`, then `onCreated(id)`.
- Koin `viewModel { NewGameViewModel(get(), get()) }`.
**TDD:** RED VM test with fakes: seed a template via fake repo → select it → add a named player + game name →
`start{}` → assert a Game was persisted to the fake game repo with the right template + 1 player. `runTest` +
setMain pattern.

### Task C4: ScoreEntry → stateless + reducer + ScoreEntryViewModel (autosave)  ⚠ regression-critical
**Files:** Create `ui/src/commonMain/kotlin/com/example/ui/model/ScoreEntryUiState.kt` (pure reducer +
UI-state types); refactor `ui/.../ScreenA.kt` `ScoreEntryScreen` to stateless; update `ui/.../DeepSeaScreenA.kt`;
Create `androidApp/.../viewmodel/ScoreEntryViewModel.kt`; modify `di/AppModule.kt`; Tests:
`ui/src/commonTest/.../ScoreEntryUiStateTest.kt` (83 anchor) + `androidApp/.../viewmodel/ScoreEntryViewModelTest.kt`.
- Extract the section/total/breakdown derivation currently in `ScoreEntryState` into a **pure** function
  `fun scoreEntryUiState(template: ResolvedTemplate, players: List<PlayerInput>, activeId: PlayerId):
  ScoreEntryUiState` in `:ui` commonMain (move/adapt the `Section`/`RuleRow`/`Ranked*` types into the
  `model` package as plain UI-state data). It computes the same sections + `total` (via `Scorer.total`) +
  `categoryBreakdown` as today.
- Make `ScoreEntryScreen` **stateless**: `ScoreEntryScreen(state: ScoreEntryUiState, onSelectPlayer:
  (PlayerId)->Unit, onIncrement: (FieldId, PlayerId)->Unit, onDecrement: (FieldId, PlayerId)->Unit,
  title: String, modifier)`. Move stepper/clamp wiring to callbacks.
- Keep the OLD stateful `ScoreEntryState` ONLY if needed to keep `ScoreEntryStateTest` green; preferred:
  re-point `ScoreEntryState` to delegate to the new reducer (so its existing test still asserts 83), OR
  migrate the 83 assertion into `ScoreEntryUiStateTest`. Either way **`./gradlew :ui:jvmTest` must still
  prove total==83 and ranked 8/8/0** with the Deep Sea fixture (`deepSeaSampleState()`/`DeepSea`).
- `ScoreEntryViewModel(gameId: GameId, getGame: GetGame, getTemplate: GetTemplate,
  private val updatePlayerValue: UpdatePlayerValue, private val finishGame: FinishGame) : ViewModel()` —
  loads the game + its template, resolves (`template.resolve(scenarioId)`), holds player values, exposes
  `StateFlow<ScoreEntryUiState>`; `onIncrement/onDecrement` clamp (respect `Field.max`), update local state,
  recompute, and **autosave** via `updatePlayerValue(gameId, player, field, newValue)`; `finish()` →
  `finishGame(gameId)`. Koin `viewModel { (gameId: GameId) -> ScoreEntryViewModel(gameId, get(), get(), get(), get()) }`.
- `DeepSeaScreenA` updates to build a `ScoreEntryUiState` from the sample and render the now-stateless
  screen (keeps the demo working until C5 routes a real game).
**TDD:** RED reducer test asserting 83 + 8/8/0 from the Deep Sea fixture; RED VM test with fakes: seed a game,
increment a field → assert UpdatePlayerValue called with the new value AND uiState total reflects it. Full
regression `./gradlew jvmTest :data:testDebugUnitTest :androidApp:testDebugUnitTest`.

### Task C5: Game-flow navigation (New Game route + real ScoreEntry + resume + finish)
**Files:** Modify `androidApp/.../nav/AppNavHost.kt` (+ Routes), `MainActivity` if needed.
- Add a `NEW_GAME` route with its own Scaffold/back; reach it from Home (a "New game" action — second FAB
  or a menu; pick the cleaner of the two and note it). Wire `NewGameViewModel` via `koinViewModel()`;
  `onStart` → `start { gameId -> navController.navigate("score_entry/${'$'}{gameId.raw}") { popUpTo(HOME) } }`.
- Change `SCORE_ENTRY` to `"score_entry/{gameId}"`; obtain `ScoreEntryViewModel` via
  `koinViewModel { parametersOf(GameId(gameId)) }`, collect state, render stateless `ScoreEntryScreen`,
  add a "Finish" action (TopAppBar) → `finish()` → `popBackStack`.
- Home's `onResumeGame(id)` → `score_entry/{id}` (real game now, not the demo). The bare `DeepSeaScreenA`
  demo destination is removed (or kept behind a debug entry — prefer removing).
**Verify:** `./gradlew :androidApp:assembleDebug` + full regression green (83 holds). Manual QA: + → build a
template → Home → New game → add players → Start → score entry persists across resume; Finish marks it done.

---

## Closeout
After C5: whole-branch review (most-capable model) over the full feature branch
(`c1d9cd7..HEAD`), then triage the accumulated Minors (foundation + this increment), then the
finishing-a-development-branch flow. Regression anchor (83 / 8-8-0) must hold at closeout.
