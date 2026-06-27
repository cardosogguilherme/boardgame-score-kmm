# Foundation: Data Layer, Domain Repos & Inset-Aware Navigation — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stand up the layered foundation — a Room-backed `:data` module, domain `Game` model + repository interfaces + interactors, manual DI, and an edge-to-edge Navigation-Compose host with a Library screen — without breaking the existing 83-point score-entry behaviour.

**Architecture:** Hybrid clean architecture. Domain (`:deepsea-scoring`, `commonMain`) holds models, repository *interfaces*, and interactors (JVM-testable against fakes). A new Android-library `:data` module implements those interfaces with Room (entities = data DTOs, mappers to domain). Presentation lives in `:ui` (stateless composables + UiState models) and `:androidApp` (ViewModels, NavHost, manual DI).

**Tech Stack:** Kotlin 2.1.0, Compose Multiplatform 1.7.3, AGP 8.7.3, Gradle 8.14, Room 2.6.1 (+KSP), Navigation-Compose, androidx.lifecycle, kotlinx-coroutines, Robolectric.

## Global Constraints

- **JDK:** build runs on JDK 21; host JDK 26 is too new for the Gradle 8.14 daemon — daemon JDK is pinned in user-global `~/.gradle/gradle.properties`, not the repo. On Windows invoke `./gradlew` from Git Bash.
- **Kotlin 2.1.0 / AGP 8.7.3 / compileSdk 35 / minSdk 24.** All new modules use `jvmToolchain(21)` and `JavaVersion.VERSION_21`.
- **Domain stays UI-free.** No Compose/Android/Room imports in `:deepsea-scoring` source.
- **Anti-corruption:** never leak a Room `@Entity` or a UiState type across a layer boundary; map at the edge.
- **Regression anchor:** the Deep Sea §5 fixture must still total **83**; ranked goal **8 / 8 / 0**. Existing `DeepSeaScoringTest` and `ScoreEntryStateTest` must stay green.
- **Package roots:** domain `com.example.scoring`, ui `com.example.ui`, app `com.example.app`, new data module `com.example.data`.
- **Commands:** all unit tests `./gradlew jvmTest`; data module tests `./gradlew :data:testDebugUnitTest`; app build `./gradlew :androidApp:assembleDebug`.

---

### Task 1: Add new dependencies to the version catalog

Pure wiring; verified by the modules that consume it (later tasks). No standalone test.

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts` (root — declare KSP plugin `apply false`)

- [ ] **Step 1: Add versions, libraries, and plugins to `gradle/libs.versions.toml`**

Add under `[versions]`:
```toml
coroutines = "1.9.0"
room = "2.6.1"
ksp = "2.1.0-1.0.29"
navigationCompose = "2.8.4"
lifecycleViewmodelCompose = "2.8.7"
robolectric = "4.14.1"
```

Add under `[libraries]`:
```toml
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
androidx-room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
androidx-room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
androidx-room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
androidx-room-testing = { module = "androidx.room:room-testing", version.ref = "room" }
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigationCompose" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycleViewmodelCompose" }
robolectric = { module = "org.robolectric:robolectric", version.ref = "robolectric" }
```

Add under `[plugins]`:
```toml
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

- [ ] **Step 2: Declare KSP plugin in the root `build.gradle.kts`**

Add this line inside the `plugins { }` block:
```kotlin
    alias(libs.plugins.ksp) apply false
```

- [ ] **Step 3: Verify the catalog resolves**

Run: `./gradlew help`
Expected: `BUILD SUCCESSFUL` (no catalog parse errors).

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml build.gradle.kts
git commit -m "build: add Room, coroutines, navigation, lifecycle, robolectric to catalog"
```

---

### Task 2: Domain — `Game` model + coroutines dependency

**Files:**
- Create: `deepsea-scoring/src/commonMain/kotlin/com/example/scoring/model/Game.kt`
- Modify: `deepsea-scoring/build.gradle.kts` (add coroutines-core to `commonMain`)
- Test: `deepsea-scoring/src/commonTest/kotlin/com/example/scoring/GameModelTest.kt`

**Interfaces:**
- Produces: `GameId(raw: String)`, `GameStatus { IN_PROGRESS, FINISHED }`, `Game(id: GameId, templateId: String, scenarioId: String?, name: String, status: GameStatus, players: List<PlayerInput>)`.

- [ ] **Step 1: Add coroutines-core to the domain module**

In `deepsea-scoring/build.gradle.kts`, add to `commonMain.dependencies { }`:
```kotlin
            implementation(libs.kotlinx.coroutines.core)
```

- [ ] **Step 2: Write the failing test**

`deepsea-scoring/src/commonTest/kotlin/com/example/scoring/GameModelTest.kt`:
```kotlin
package com.example.scoring

import com.example.scoring.model.Game
import com.example.scoring.model.GameId
import com.example.scoring.model.GameStatus
import com.example.scoring.model.PlayerId
import com.example.scoring.model.PlayerInput
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class GameModelTest {
    @Test
    fun game_round_trips_through_json() {
        val game = Game(
            id = GameId("g1"),
            templateId = "deepSea",
            scenarioId = "scenario1",
            name = "Friday night",
            status = GameStatus.IN_PROGRESS,
            players = listOf(PlayerInput(PlayerId("you"), "You", emptyMap())),
        )
        val json = Json { prettyPrint = false }
        val decoded = json.decodeFromString(Game.serializer(), json.encodeToString(Game.serializer(), game))
        assertEquals(game, decoded)
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :deepsea-scoring:jvmTest --tests "com.example.scoring.GameModelTest"`
Expected: FAIL — `Game` / `GameId` / `GameStatus` unresolved.

- [ ] **Step 4: Create the model**

`deepsea-scoring/src/commonMain/kotlin/com/example/scoring/model/Game.kt`:
```kotlin
package com.example.scoring.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class GameId(val raw: String)

@Serializable
enum class GameStatus { IN_PROGRESS, FINISHED }

/**
 * A scoring session: a chosen template/scenario plus the players and their entered values.
 * `templateId`/`scenarioId` reference a persisted Template; `players` carries the live inputs.
 */
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

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :deepsea-scoring:jvmTest --tests "com.example.scoring.GameModelTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add deepsea-scoring/build.gradle.kts deepsea-scoring/src/commonMain/kotlin/com/example/scoring/model/Game.kt deepsea-scoring/src/commonTest/kotlin/com/example/scoring/GameModelTest.kt
git commit -m "feat(domain): add Game model + coroutines dependency"
```

---

### Task 3: Domain — repository interfaces + interactors

**Files:**
- Create: `deepsea-scoring/src/commonMain/kotlin/com/example/scoring/repository/Repositories.kt`
- Create: `deepsea-scoring/src/commonMain/kotlin/com/example/scoring/interactors/TemplateInteractors.kt`
- Create: `deepsea-scoring/src/commonMain/kotlin/com/example/scoring/interactors/GameInteractors.kt`
- Test: `deepsea-scoring/src/commonTest/kotlin/com/example/scoring/interactors/InteractorsTest.kt`
- Test: `deepsea-scoring/src/commonTest/kotlin/com/example/scoring/interactors/FakeRepositories.kt`

**Interfaces:**
- Consumes: `Template`, `Game`, `GameId`, `PlayerId`, `FieldId`, `PlayerInput`, `ResolvedTemplate.validate()`, `Scorer`.
- Produces:
  - `interface TemplateRepository { fun observeTemplates(): Flow<List<Template>>; suspend fun getTemplate(id: String): Template?; suspend fun saveTemplate(template: Template); suspend fun deleteTemplate(id: String) }`
  - `interface GameRepository { fun observeGames(): Flow<List<Game>>; suspend fun getGame(id: GameId): Game?; suspend fun createGame(game: Game): GameId; suspend fun updatePlayerValue(id: GameId, player: PlayerId, field: FieldId, value: Int); suspend fun addPlayer(id: GameId, player: PlayerInput); suspend fun removePlayer(id: GameId, player: PlayerId); suspend fun setStatus(id: GameId, status: GameStatus); suspend fun deleteGame(id: GameId) }`
  - Interactors (each `operator fun invoke`): `ObserveTemplates`, `GetTemplate`, `SaveTemplate` (returns `List<String>` validation errors; persists only when empty), `DeleteTemplate`, `ObserveGames`, `GetGame`, `CreateGame`, `UpdatePlayerValue`, `FinishGame`.

- [ ] **Step 1: Create the repository interfaces**

`deepsea-scoring/src/commonMain/kotlin/com/example/scoring/repository/Repositories.kt`:
```kotlin
package com.example.scoring.repository

import com.example.scoring.model.FieldId
import com.example.scoring.model.Game
import com.example.scoring.model.GameId
import com.example.scoring.model.GameStatus
import com.example.scoring.model.PlayerId
import com.example.scoring.model.PlayerInput
import com.example.scoring.model.Template
import kotlinx.coroutines.flow.Flow

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

- [ ] **Step 2: Write the fake repositories used by the test**

`deepsea-scoring/src/commonTest/kotlin/com/example/scoring/interactors/FakeRepositories.kt`:
```kotlin
package com.example.scoring.interactors

import com.example.scoring.model.FieldId
import com.example.scoring.model.Game
import com.example.scoring.model.GameId
import com.example.scoring.model.GameStatus
import com.example.scoring.model.PlayerId
import com.example.scoring.model.PlayerInput
import com.example.scoring.model.Template
import com.example.scoring.repository.GameRepository
import com.example.scoring.repository.TemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeTemplateRepository(initial: List<Template> = emptyList()) : TemplateRepository {
    private val state = MutableStateFlow(initial.associateBy { it.id })
    override fun observeTemplates(): Flow<List<Template>> = state.map { it.values.toList() }
    override suspend fun getTemplate(id: String): Template? = state.value[id]
    override suspend fun saveTemplate(template: Template) {
        state.value = state.value + (template.id to template)
    }
    override suspend fun deleteTemplate(id: String) { state.value = state.value - id }
}

class FakeGameRepository(initial: List<Game> = emptyList()) : GameRepository {
    private val state = MutableStateFlow(initial.associateBy { it.id })
    override fun observeGames(): Flow<List<Game>> = state.map { it.values.toList() }
    override suspend fun getGame(id: GameId): Game? = state.value[id]
    override suspend fun createGame(game: Game): GameId {
        state.value = state.value + (game.id to game); return game.id
    }
    override suspend fun updatePlayerValue(id: GameId, player: PlayerId, field: FieldId, value: Int) {
        val game = state.value[id] ?: return
        val players = game.players.map {
            if (it.id == player) it.copy(values = it.values + (field to value)) else it
        }
        state.value = state.value + (id to game.copy(players = players))
    }
    override suspend fun addPlayer(id: GameId, player: PlayerInput) {
        val game = state.value[id] ?: return
        state.value = state.value + (id to game.copy(players = game.players + player))
    }
    override suspend fun removePlayer(id: GameId, player: PlayerId) {
        val game = state.value[id] ?: return
        state.value = state.value + (id to game.copy(players = game.players.filterNot { it.id == player }))
    }
    override suspend fun setStatus(id: GameId, status: GameStatus) {
        val game = state.value[id] ?: return
        state.value = state.value + (id to game.copy(status = status))
    }
    override suspend fun deleteGame(id: GameId) { state.value = state.value - id }
}
```

- [ ] **Step 3: Write the failing interactor test**

`deepsea-scoring/src/commonTest/kotlin/com/example/scoring/interactors/InteractorsTest.kt`:
```kotlin
package com.example.scoring.interactors

import com.example.scoring.model.Game
import com.example.scoring.model.GameId
import com.example.scoring.model.GameStatus
import com.example.scoring.model.PlayerId
import com.example.scoring.model.PlayerInput
import com.example.scoring.model.resolve
import com.example.scoring.sample.DeepSea
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InteractorsTest {
    @Test
    fun save_template_rejects_invalid_and_persists_valid() = runTest {
        val repo = FakeTemplateRepository()
        val save = SaveTemplate(repo)
        // DeepSea base template is valid (validate() == emptyList()).
        val errors = save(DeepSea.template)
        assertEquals(emptyList(), errors)
        assertEquals(DeepSea.template, repo.getTemplate(DeepSea.template.id))
    }

    @Test
    fun create_game_then_update_value_is_observable() = runTest {
        val repo = FakeGameRepository()
        val create = CreateGame(repo)
        val update = UpdatePlayerValue(repo)
        val id = create(
            Game(
                id = GameId("g1"),
                templateId = DeepSea.template.id,
                scenarioId = DeepSea.SCENARIO_1,
                name = "Test",
                players = listOf(PlayerInput(PlayerId("you"), "You", emptyMap())),
            ),
        )
        update(id, PlayerId("you"), DeepSea.journal, 9)
        val game = ObserveGames(repo)().first().single()
        assertEquals(9, game.players.single().values[DeepSea.journal])
    }

    @Test
    fun finish_game_sets_status() = runTest {
        val repo = FakeGameRepository(
            listOf(Game(GameId("g1"), DeepSea.template.id, null, "T", GameStatus.IN_PROGRESS)),
        )
        FinishGame(repo)(GameId("g1"))
        assertTrue(repo.getGame(GameId("g1"))!!.status == GameStatus.FINISHED)
    }
}
```

- [ ] **Step 4: Run test to verify it fails**

Run: `./gradlew :deepsea-scoring:jvmTest --tests "com.example.scoring.interactors.InteractorsTest"`
Expected: FAIL — interactor classes unresolved. (Add `kotlinx-coroutines-test` to `commonTest` if `runTest` is unresolved — see Step 5.)

- [ ] **Step 5: Add coroutines-test to the domain test source set**

In `deepsea-scoring/build.gradle.kts`, add to `commonTest.dependencies { }`:
```kotlin
            implementation(libs.kotlinx.coroutines.test)
```

- [ ] **Step 6: Create the template interactors**

`deepsea-scoring/src/commonMain/kotlin/com/example/scoring/interactors/TemplateInteractors.kt`:
```kotlin
package com.example.scoring.interactors

import com.example.scoring.engine.validate
import com.example.scoring.model.Template
import com.example.scoring.model.resolve
import com.example.scoring.repository.TemplateRepository
import kotlinx.coroutines.flow.Flow

class ObserveTemplates(private val repo: TemplateRepository) {
    operator fun invoke(): Flow<List<Template>> = repo.observeTemplates()
}

class GetTemplate(private val repo: TemplateRepository) {
    suspend operator fun invoke(id: String): Template? = repo.getTemplate(id)
}

/** Validates the template; persists only when there are no errors. Returns the error list. */
class SaveTemplate(private val repo: TemplateRepository) {
    suspend operator fun invoke(template: Template): List<String> {
        val errors = template.resolve().validate()
        if (errors.isEmpty()) repo.saveTemplate(template)
        return errors
    }
}

class DeleteTemplate(private val repo: TemplateRepository) {
    suspend operator fun invoke(id: String) = repo.deleteTemplate(id)
}
```

> Note: confirm the exact import for `validate()` and `resolve()` against `engine/Validate.kt` and `model/ResolvedTemplate.kt` (they may be top-level functions in a different package). Adjust the `import` lines to match; the call sites stay the same.

- [ ] **Step 7: Create the game interactors**

`deepsea-scoring/src/commonMain/kotlin/com/example/scoring/interactors/GameInteractors.kt`:
```kotlin
package com.example.scoring.interactors

import com.example.scoring.model.FieldId
import com.example.scoring.model.Game
import com.example.scoring.model.GameId
import com.example.scoring.model.GameStatus
import com.example.scoring.model.PlayerId
import com.example.scoring.repository.GameRepository
import kotlinx.coroutines.flow.Flow

class ObserveGames(private val repo: GameRepository) {
    operator fun invoke(): Flow<List<Game>> = repo.observeGames()
}

class GetGame(private val repo: GameRepository) {
    suspend operator fun invoke(id: GameId): Game? = repo.getGame(id)
}

class CreateGame(private val repo: GameRepository) {
    suspend operator fun invoke(game: Game): GameId = repo.createGame(game)
}

class UpdatePlayerValue(private val repo: GameRepository) {
    suspend operator fun invoke(id: GameId, player: PlayerId, field: FieldId, value: Int) =
        repo.updatePlayerValue(id, player, field, value)
}

class FinishGame(private val repo: GameRepository) {
    suspend operator fun invoke(id: GameId) = repo.setStatus(id, GameStatus.FINISHED)
}
```

- [ ] **Step 8: Run the test to verify it passes**

Run: `./gradlew :deepsea-scoring:jvmTest --tests "com.example.scoring.interactors.InteractorsTest"`
Expected: PASS (3 tests).

- [ ] **Step 9: Commit**

```bash
git add deepsea-scoring/
git commit -m "feat(domain): add repository interfaces + interactors (fake-tested)"
```

---

### Task 4: Create the `:data` Android-library module skeleton

Wiring task; verified by build. The Room `@Database` is created here so later tasks have a home.

**Files:**
- Modify: `settings.gradle.kts` (include `:data`)
- Create: `data/build.gradle.kts`
- Create: `data/src/main/AndroidManifest.xml`

**Interfaces:**
- Produces: module `:data`, namespace `com.example.data`, with Room + KSP configured and depending on `:deepsea-scoring`.

- [ ] **Step 1: Include the module**

In `settings.gradle.kts`, change the `include(...)` line to:
```kotlin
include(":deepsea-scoring", ":ui", ":androidApp", ":data")
```

- [ ] **Step 2: Create `data/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
}

kotlin {
    jvmToolchain(21)
}

android {
    namespace = "com.example.data"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":deepsea-scoring"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.robolectric)
}
```

- [ ] **Step 3: Create `data/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest />
```

- [ ] **Step 4: Verify the module configures**

Run: `./gradlew :data:help`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add settings.gradle.kts data/build.gradle.kts data/src/main/AndroidManifest.xml
git commit -m "build: add :data Android library module (Room + KSP)"
```

---

### Task 5: Room entities, DAOs & database

**Files:**
- Create: `data/src/main/kotlin/com/example/data/entity/Entities.kt`
- Create: `data/src/main/kotlin/com/example/data/dao/TemplateDao.kt`
- Create: `data/src/main/kotlin/com/example/data/dao/GameDao.kt`
- Create: `data/src/main/kotlin/com/example/data/AppDatabase.kt`
- Test: `data/src/test/kotlin/com/example/data/dao/DaoTest.kt`

**Interfaces:**
- Produces: entities `TemplateEntity`, `FieldEntity`, `RuleEntity`, `GameEntity`, `PlayerEntity`, `PlayerValueEntity`; `TemplateDao`, `GameDao`; `AppDatabase` with `Room.inMemoryDatabaseBuilder` usable in tests.

- [ ] **Step 1: Create the entities**

`data/src/main/kotlin/com/example/data/entity/Entities.kt`:
```kotlin
package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "template")
data class TemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
)

@Entity(tableName = "field")
data class FieldEntity(
    @PrimaryKey val id: String,
    val templateId: String,
    val label: String,
    val kind: String,
    val max: Int?,
    val position: Int,
)

@Entity(tableName = "rule")
data class RuleEntity(
    @PrimaryKey val id: String,
    val templateId: String,
    val type: String,
    val payloadJson: String,
    val position: Int,
)

@Entity(tableName = "game")
data class GameEntity(
    @PrimaryKey val id: String,
    val templateId: String,
    val scenarioId: String?,
    val name: String,
    val status: String,
    val createdAt: Long,
)

@Entity(tableName = "player")
data class PlayerEntity(
    @PrimaryKey val id: String,
    val gameId: String,
    val name: String,
    val position: Int,
)

@Entity(tableName = "player_value", primaryKeys = ["gameId", "playerId", "fieldId"])
data class PlayerValueEntity(
    val gameId: String,
    val playerId: String,
    val fieldId: String,
    val value: Int,
)
```

- [ ] **Step 2: Create the DAOs**

`data/src/main/kotlin/com/example/data/dao/TemplateDao.kt`:
```kotlin
package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.entity.FieldEntity
import com.example.data.entity.RuleEntity
import com.example.data.entity.TemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Query("SELECT * FROM template ORDER BY name")
    fun observeTemplates(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM template WHERE id = :id")
    suspend fun getTemplate(id: String): TemplateEntity?

    @Query("SELECT * FROM field WHERE templateId = :id ORDER BY position")
    suspend fun fields(id: String): List<FieldEntity>

    @Query("SELECT * FROM rule WHERE templateId = :id ORDER BY position")
    suspend fun rules(id: String): List<RuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTemplate(template: TemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFields(fields: List<FieldEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRules(rules: List<RuleEntity>)

    @Query("DELETE FROM field WHERE templateId = :id")
    suspend fun clearFields(id: String)

    @Query("DELETE FROM rule WHERE templateId = :id")
    suspend fun clearRules(id: String)

    @Query("DELETE FROM template WHERE id = :id")
    suspend fun deleteTemplate(id: String)

    @Transaction
    suspend fun saveTemplate(template: TemplateEntity, fields: List<FieldEntity>, rules: List<RuleEntity>) {
        upsertTemplate(template)
        clearFields(template.id); upsertFields(fields)
        clearRules(template.id); upsertRules(rules)
    }
}
```

`data/src/main/kotlin/com/example/data/dao/GameDao.kt`:
```kotlin
package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.GameEntity
import com.example.data.entity.PlayerEntity
import com.example.data.entity.PlayerValueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM game ORDER BY createdAt DESC")
    fun observeGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM game WHERE id = :id")
    suspend fun getGame(id: String): GameEntity?

    @Query("SELECT * FROM player WHERE gameId = :id ORDER BY position")
    suspend fun players(id: String): List<PlayerEntity>

    @Query("SELECT * FROM player_value WHERE gameId = :id")
    suspend fun values(id: String): List<PlayerValueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGame(game: GameEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlayers(players: List<PlayerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlayer(player: PlayerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertValue(value: PlayerValueEntity)

    @Query("UPDATE game SET status = :status WHERE id = :id")
    suspend fun setStatus(id: String, status: String)

    @Query("DELETE FROM player WHERE gameId = :gameId AND id = :playerId")
    suspend fun deletePlayer(gameId: String, playerId: String)

    @Query("DELETE FROM game WHERE id = :id")
    suspend fun deleteGame(id: String)
}
```

- [ ] **Step 3: Create the database**

`data/src/main/kotlin/com/example/data/AppDatabase.kt`:
```kotlin
package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.dao.GameDao
import com.example.data.dao.TemplateDao
import com.example.data.entity.FieldEntity
import com.example.data.entity.GameEntity
import com.example.data.entity.PlayerEntity
import com.example.data.entity.PlayerValueEntity
import com.example.data.entity.RuleEntity
import com.example.data.entity.TemplateEntity

@Database(
    entities = [
        TemplateEntity::class, FieldEntity::class, RuleEntity::class,
        GameEntity::class, PlayerEntity::class, PlayerValueEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun templateDao(): TemplateDao
    abstract fun gameDao(): GameDao
}
```

- [ ] **Step 4: Write the failing DAO test (Robolectric, in-memory Room)**

`data/src/test/kotlin/com/example/data/dao/DaoTest.kt`:
```kotlin
package com.example.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.entity.GameEntity
import com.example.data.entity.PlayerEntity
import com.example.data.entity.PlayerValueEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class DaoTest {
    private lateinit var db: AppDatabase
    private lateinit var games: GameDao

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        games = db.gameDao()
    }

    @After fun tearDown() = db.close()

    @Test fun game_with_players_and_values_round_trips() = runTest {
        games.upsertGame(GameEntity("g1", "deepSea", "scenario1", "Test", "IN_PROGRESS", 1000L))
        games.upsertPlayers(listOf(PlayerEntity("you", "g1", "You", 0)))
        games.upsertValue(PlayerValueEntity("g1", "you", "journal", 9))

        assertEquals("Test", games.observeGames().first().single().name)
        assertEquals(1, games.players("g1").size)
        assertEquals(9, games.values("g1").single().value)
    }
}
```

- [ ] **Step 5: Run test to verify it fails, then passes after compile**

Run: `./gradlew :data:testDebugUnitTest --tests "com.example.data.dao.DaoTest"`
Expected: first FAIL if any DAO/entity is missing; once Steps 1-3 compile, PASS. (Robolectric downloads its runtime on first run — allow time.)

- [ ] **Step 6: Commit**

```bash
git add data/
git commit -m "feat(data): Room entities, DAOs, database (in-memory DAO test)"
```

---

### Task 6: Mappers (entity ↔ domain)

**Files:**
- Create: `data/src/main/kotlin/com/example/data/mapper/RuleJson.kt`
- Create: `data/src/main/kotlin/com/example/data/mapper/TemplateMappers.kt`
- Create: `data/src/main/kotlin/com/example/data/mapper/GameMappers.kt`
- Test: `data/src/test/kotlin/com/example/data/mapper/MapperTest.kt`

**Interfaces:**
- Consumes: domain `Template`, `Field`, `ScoringRule`, `Game`, `PlayerInput`; entities from Task 5.
- Produces:
  - `fun Template.toEntities(): Triple<TemplateEntity, List<FieldEntity>, List<RuleEntity>>`
  - `fun templateFrom(t: TemplateEntity, fields: List<FieldEntity>, rules: List<RuleEntity>): Template`
  - `fun Game.toEntities(createdAt: Long): Triple<GameEntity, List<PlayerEntity>, List<PlayerValueEntity>>`
  - `fun gameFrom(g: GameEntity, players: List<PlayerEntity>, values: List<PlayerValueEntity>): Game`

- [ ] **Step 1: Create the rule (de)serialization helper**

`data/src/main/kotlin/com/example/data/mapper/RuleJson.kt`:
```kotlin
package com.example.data.mapper

import com.example.scoring.model.ScoringRule
import kotlinx.serialization.json.Json

/** ScoringRule is a @Serializable sealed interface; persist the whole rule as JSON. */
internal val ruleJson = Json { ignoreUnknownKeys = true }

internal fun ScoringRule.toJson(): String = ruleJson.encodeToString(ScoringRule.serializer(), this)
internal fun ruleFromJson(json: String): ScoringRule = ruleJson.decodeFromString(ScoringRule.serializer(), json)
```

> The polymorphic discriminator (`lookup`/`perUnit`/…) is already carried by the `@SerialName` annotations on each `ScoringRule` subtype, so `RuleEntity.type` is informational/queryable and the JSON is the source of truth.

- [ ] **Step 2: Write the failing mapper test**

`data/src/test/kotlin/com/example/data/mapper/MapperTest.kt`:
```kotlin
package com.example.data.mapper

import com.example.scoring.sample.DeepSea
import kotlin.test.Test
import kotlin.test.assertEquals

class MapperTest {
    @Test fun template_round_trips_through_entities() {
        val (t, fields, rules) = DeepSea.template.toEntities()
        val restored = templateFrom(t, fields, rules)
        assertEquals(DeepSea.template, restored)
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :data:testDebugUnitTest --tests "com.example.data.mapper.MapperTest"`
Expected: FAIL — `toEntities` / `templateFrom` unresolved.

- [ ] **Step 4: Create the template mappers**

`data/src/main/kotlin/com/example/data/mapper/TemplateMappers.kt`:
```kotlin
package com.example.data.mapper

import com.example.data.entity.FieldEntity
import com.example.data.entity.RuleEntity
import com.example.data.entity.TemplateEntity
import com.example.scoring.model.Field
import com.example.scoring.model.FieldId
import com.example.scoring.model.FieldKind
import com.example.scoring.model.Template

fun Template.toEntities(): Triple<TemplateEntity, List<FieldEntity>, List<RuleEntity>> {
    val t = TemplateEntity(id = id, name = name)
    val fieldEntities = fields.mapIndexed { i, f ->
        FieldEntity(f.id.raw, id, f.label, f.kind.name, f.max, i)
    }
    val ruleEntities = rules.mapIndexed { i, r ->
        RuleEntity(r.id.raw, id, r::class.simpleName ?: "rule", r.toJson(), i)
    }
    return Triple(t, fieldEntities, ruleEntities)
}

fun templateFrom(t: TemplateEntity, fields: List<FieldEntity>, rules: List<RuleEntity>): Template =
    Template(
        id = t.id,
        name = t.name,
        fields = fields.sortedBy { it.position }.map {
            Field(FieldId(it.id), it.label, FieldKind.valueOf(it.kind), it.max)
        },
        rules = rules.sortedBy { it.position }.map { ruleFromJson(it.payloadJson) },
    )
```

> Confirm the `Template` constructor parameters (it has a `scenarios` list defaulting to empty — `DeepSea.template` may set it). If `DeepSea.template` carries scenarios, persist them too or assert against `template.copy(scenarios = emptyList())`. Adjust the test fixture/mapper to match the real shape so the round-trip is exact.

- [ ] **Step 5: Create the game mappers**

`data/src/main/kotlin/com/example/data/mapper/GameMappers.kt`:
```kotlin
package com.example.data.mapper

import com.example.data.entity.GameEntity
import com.example.data.entity.PlayerEntity
import com.example.data.entity.PlayerValueEntity
import com.example.scoring.model.FieldId
import com.example.scoring.model.Game
import com.example.scoring.model.GameId
import com.example.scoring.model.GameStatus
import com.example.scoring.model.PlayerId
import com.example.scoring.model.PlayerInput

fun Game.toEntities(createdAt: Long): Triple<GameEntity, List<PlayerEntity>, List<PlayerValueEntity>> {
    val g = GameEntity(id.raw, templateId, scenarioId, name, status.name, createdAt)
    val playerEntities = players.mapIndexed { i, p -> PlayerEntity(p.id.raw, id.raw, p.name, i) }
    val valueEntities = players.flatMap { p ->
        p.values.map { (field, v) -> PlayerValueEntity(id.raw, p.id.raw, field.raw, v) }
    }
    return Triple(g, playerEntities, valueEntities)
}

fun gameFrom(g: GameEntity, players: List<PlayerEntity>, values: List<PlayerValueEntity>): Game =
    Game(
        id = GameId(g.id),
        templateId = g.templateId,
        scenarioId = g.scenarioId,
        name = g.name,
        status = GameStatus.valueOf(g.status),
        players = players.sortedBy { it.position }.map { p ->
            PlayerInput(
                id = PlayerId(p.id),
                name = p.name,
                values = values.filter { it.playerId == p.id }
                    .associate { FieldId(it.fieldId) to it.value },
            )
        },
    )
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew :data:testDebugUnitTest --tests "com.example.data.mapper.MapperTest"`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add data/
git commit -m "feat(data): entity<->domain mappers (round-trip tested)"
```

---

### Task 7: Room-backed repository implementations

**Files:**
- Create: `data/src/main/kotlin/com/example/data/repository/RoomTemplateRepository.kt`
- Create: `data/src/main/kotlin/com/example/data/repository/RoomGameRepository.kt`
- Test: `data/src/test/kotlin/com/example/data/repository/RoomRepositoriesTest.kt`

**Interfaces:**
- Consumes: `TemplateDao`, `GameDao`, mappers, domain repo interfaces.
- Produces: `class RoomTemplateRepository(dao: TemplateDao) : TemplateRepository`; `class RoomGameRepository(dao: GameDao, now: () -> Long = System::currentTimeMillis) : GameRepository`.

- [ ] **Step 1: Write the failing repository test**

`data/src/test/kotlin/com/example/data/repository/RoomRepositoriesTest.kt`:
```kotlin
package com.example.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.scoring.model.Game
import com.example.scoring.model.GameId
import com.example.scoring.model.PlayerId
import com.example.scoring.model.PlayerInput
import com.example.scoring.sample.DeepSea
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class RoomRepositoriesTest {
    private lateinit var db: AppDatabase

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AppDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After fun tearDown() = db.close()

    @Test fun saved_template_is_observable_and_equal() = runTest {
        val repo = RoomTemplateRepository(db.templateDao())
        repo.saveTemplate(DeepSea.template)
        assertEquals(DeepSea.template, repo.observeTemplates().first().single())
    }

    @Test fun created_game_then_updated_value_persists() = runTest {
        val repo = RoomGameRepository(db.gameDao()) { 1000L }
        val id = repo.createGame(
            Game(GameId("g1"), DeepSea.template.id, DeepSea.SCENARIO_1, "Test",
                players = listOf(PlayerInput(PlayerId("you"), "You", emptyMap()))),
        )
        repo.updatePlayerValue(id, PlayerId("you"), DeepSea.journal, 9)
        assertEquals(9, repo.getGame(id)!!.players.single().values[DeepSea.journal])
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :data:testDebugUnitTest --tests "com.example.data.repository.RoomRepositoriesTest"`
Expected: FAIL — repository classes unresolved.

- [ ] **Step 3: Create `RoomTemplateRepository`**

`data/src/main/kotlin/com/example/data/repository/RoomTemplateRepository.kt`:
```kotlin
package com.example.data.repository

import com.example.data.dao.TemplateDao
import com.example.data.mapper.templateFrom
import com.example.data.mapper.toEntities
import com.example.scoring.model.Template
import com.example.scoring.repository.TemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTemplateRepository(private val dao: TemplateDao) : TemplateRepository {
    override fun observeTemplates(): Flow<List<Template>> =
        dao.observeTemplates().map { rows ->
            rows.map { templateFrom(it, dao.fields(it.id), dao.rules(it.id)) }
        }

    override suspend fun getTemplate(id: String): Template? {
        val t = dao.getTemplate(id) ?: return null
        return templateFrom(t, dao.fields(id), dao.rules(id))
    }

    override suspend fun saveTemplate(template: Template) {
        val (t, fields, rules) = template.toEntities()
        dao.saveTemplate(t, fields, rules)
    }

    override suspend fun deleteTemplate(id: String) {
        dao.clearFields(id); dao.clearRules(id); dao.deleteTemplate(id)
    }
}
```

> Note: calling suspend `dao.fields()`/`dao.rules()` inside `map` requires that `map` block be suspend-capable; Flow's `map` operator IS suspend-capable, so this compiles. If Room flags the `observeTemplates().map { ... dao.fields() ... }` as running off the query thread, switch to assembling via a `@Transaction`-annotated DAO method returning a relation instead. Verify with the test.

- [ ] **Step 4: Create `RoomGameRepository`**

`data/src/main/kotlin/com/example/data/repository/RoomGameRepository.kt`:
```kotlin
package com.example.data.repository

import com.example.data.dao.GameDao
import com.example.data.entity.PlayerEntity
import com.example.data.entity.PlayerValueEntity
import com.example.data.mapper.gameFrom
import com.example.data.mapper.toEntities
import com.example.scoring.model.FieldId
import com.example.scoring.model.Game
import com.example.scoring.model.GameId
import com.example.scoring.model.GameStatus
import com.example.scoring.model.PlayerId
import com.example.scoring.model.PlayerInput
import com.example.scoring.repository.GameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomGameRepository(
    private val dao: GameDao,
    private val now: () -> Long = System::currentTimeMillis,
) : GameRepository {
    override fun observeGames(): Flow<List<Game>> =
        dao.observeGames().map { rows ->
            rows.map { gameFrom(it, dao.players(it.id), dao.values(it.id)) }
        }

    override suspend fun getGame(id: GameId): Game? {
        val g = dao.getGame(id.raw) ?: return null
        return gameFrom(g, dao.players(id.raw), dao.values(id.raw))
    }

    override suspend fun createGame(game: Game): GameId {
        val (g, players, values) = game.toEntities(now())
        dao.upsertGame(g); dao.upsertPlayers(players)
        values.forEach { dao.upsertValue(it) }
        return game.id
    }

    override suspend fun updatePlayerValue(id: GameId, player: PlayerId, field: FieldId, value: Int) {
        dao.upsertValue(PlayerValueEntity(id.raw, player.raw, field.raw, value))
    }

    override suspend fun addPlayer(id: GameId, player: PlayerInput) {
        val position = dao.players(id.raw).size
        dao.upsertPlayer(PlayerEntity(player.id.raw, id.raw, player.name, position))
    }

    override suspend fun removePlayer(id: GameId, player: PlayerId) {
        dao.deletePlayer(id.raw, player.raw)
    }

    override suspend fun setStatus(id: GameId, status: GameStatus) {
        dao.setStatus(id.raw, status.name)
    }

    override suspend fun deleteGame(id: GameId) {
        dao.deleteGame(id.raw)
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :data:testDebugUnitTest --tests "com.example.data.repository.RoomRepositoriesTest"`
Expected: PASS (2 tests).

- [ ] **Step 6: Commit**

```bash
git add data/
git commit -m "feat(data): Room-backed Template/Game repositories (Robolectric-tested)"
```

---

### Task 8: Library UiState + domain→UI mapper + stateless LibraryScreen

**Files:**
- Create: `ui/src/commonMain/kotlin/com/example/ui/model/LibraryUiState.kt`
- Create: `ui/src/commonMain/kotlin/com/example/ui/screens/LibraryScreen.kt`
- Modify: `ui/build.gradle.kts` (add coroutines-core to `commonMain` for any Flow types used in UI models — only if needed; the UiState itself is plain data)
- Test: `ui/src/commonTest/kotlin/com/example/ui/model/LibraryUiStateTest.kt`

**Interfaces:**
- Consumes: domain `Template`, `Game`.
- Produces: `data class TemplateRow(id, name)`, `data class GameRow(id, name, templateName, status)`, `data class LibraryUiState(templates: List<TemplateRow>, games: List<GameRow>)`, `fun libraryUiState(templates: List<Template>, games: List<Game>): LibraryUiState`, and `@Composable fun LibraryScreen(state, onNewTemplate, onOpenTemplate, onResumeGame)`.

- [ ] **Step 1: Write the failing mapper test**

`ui/src/commonTest/kotlin/com/example/ui/model/LibraryUiStateTest.kt`:
```kotlin
package com.example.ui.model

import com.example.scoring.model.Game
import com.example.scoring.model.GameId
import com.example.scoring.model.GameStatus
import com.example.scoring.sample.DeepSea
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryUiStateTest {
    @Test fun maps_templates_and_games_to_rows() {
        val games = listOf(
            Game(GameId("g1"), DeepSea.template.id, DeepSea.SCENARIO_1, "Friday", GameStatus.IN_PROGRESS),
        )
        val state = libraryUiState(listOf(DeepSea.template), games)
        assertEquals(DeepSea.template.name, state.templates.single().name)
        assertEquals("Friday", state.games.single().name)
        assertEquals(DeepSea.template.name, state.games.single().templateName)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :ui:jvmTest --tests "com.example.ui.model.LibraryUiStateTest"`
Expected: FAIL — `libraryUiState` unresolved.

- [ ] **Step 3: Create the UiState + mapper**

`ui/src/commonMain/kotlin/com/example/ui/model/LibraryUiState.kt`:
```kotlin
package com.example.ui.model

import com.example.scoring.model.Game
import com.example.scoring.model.GameStatus
import com.example.scoring.model.Template

data class TemplateRow(val id: String, val name: String)

data class GameRow(
    val id: String,
    val name: String,
    val templateName: String,
    val status: GameStatus,
)

data class LibraryUiState(
    val templates: List<TemplateRow> = emptyList(),
    val games: List<GameRow> = emptyList(),
)

fun libraryUiState(templates: List<Template>, games: List<Game>): LibraryUiState {
    val nameById = templates.associate { it.id to it.name }
    return LibraryUiState(
        templates = templates.map { TemplateRow(it.id, it.name) },
        games = games.map {
            GameRow(it.id.raw, it.name, nameById[it.templateId] ?: it.templateId, it.status)
        },
    )
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :ui:jvmTest --tests "com.example.ui.model.LibraryUiStateTest"`
Expected: PASS.

- [ ] **Step 5: Create the stateless LibraryScreen composable**

`ui/src/commonMain/kotlin/com/example/ui/screens/LibraryScreen.kt`:
```kotlin
package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.model.GameRow
import com.example.ui.model.LibraryUiState
import com.example.ui.model.TemplateRow

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onNewTemplate: () -> Unit,
    onOpenTemplate: (String) -> Unit,
    onResumeGame: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Text("Templates") }
        if (state.templates.isEmpty()) {
            item { Text("No templates yet — tap + to create one.") }
        } else {
            items(state.templates, key = { it.id }) { row -> TemplateCard(row, onOpenTemplate) }
        }
        item { Text("Games") }
        if (state.games.isEmpty()) {
            item { Text("No games yet.") }
        } else {
            items(state.games, key = { it.id }) { row -> GameCard(row, onResumeGame) }
        }
    }
}

@Composable
private fun TemplateCard(row: TemplateRow, onOpen: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onOpen(row.id) }) {
        Text(row.name, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun GameCard(row: GameRow, onResume: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onResume(row.id) }) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(row.name)
            Text("${row.templateName} · ${row.status}")
        }
    }
}
```

> `onNewTemplate` is consumed by the host's `TopAppBar`/FAB (Task 10), not inside the list; it stays in the signature so the screen is the single contract for the route.

- [ ] **Step 6: Verify it compiles**

Run: `./gradlew :ui:jvmTest`
Expected: `BUILD SUCCESSFUL`, all `:ui` tests (existing + new) green.

- [ ] **Step 7: Commit**

```bash
git add ui/
git commit -m "feat(ui): LibraryUiState mapper + stateless LibraryScreen"
```

---

### Task 9: Manual DI container

**Files:**
- Create: `androidApp/src/main/kotlin/com/example/app/di/AppContainer.kt`
- Modify: `androidApp/build.gradle.kts` (depend on `:data`, `:deepsea-scoring`, lifecycle/nav/coroutines-android)
- Create: `androidApp/src/main/kotlin/com/example/app/App.kt` (Application holding the container)
- Modify: `androidApp/src/main/AndroidManifest.xml` (register the Application)

**Interfaces:**
- Produces: `class AppContainer(context: Context)` exposing `templateRepository: TemplateRepository`, `gameRepository: GameRepository`, and interactor instances; `class App : Application()` with `lateinit var container`.

- [ ] **Step 1: Add dependencies to `androidApp/build.gradle.kts`**

Add to the `dependencies { }` block:
```kotlin
    implementation(project(":deepsea-scoring"))
    implementation(project(":data"))
    implementation(compose.foundation)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
```

- [ ] **Step 2: Create the container**

`androidApp/src/main/kotlin/com/example/app/di/AppContainer.kt`:
```kotlin
package com.example.app.di

import android.content.Context
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.repository.RoomGameRepository
import com.example.data.repository.RoomTemplateRepository
import com.example.scoring.interactors.CreateGame
import com.example.scoring.interactors.DeleteTemplate
import com.example.scoring.interactors.FinishGame
import com.example.scoring.interactors.GetGame
import com.example.scoring.interactors.ObserveGames
import com.example.scoring.interactors.ObserveTemplates
import com.example.scoring.interactors.SaveTemplate
import com.example.scoring.interactors.UpdatePlayerValue
import com.example.scoring.repository.GameRepository
import com.example.scoring.repository.TemplateRepository

/** Manual composition root. One database, repositories, and the interactors screens need. */
class AppContainer(context: Context) {
    private val db = Room.databaseBuilder(
        context.applicationContext, AppDatabase::class.java, "boardgame-score.db",
    ).build()

    val templateRepository: TemplateRepository = RoomTemplateRepository(db.templateDao())
    val gameRepository: GameRepository = RoomGameRepository(db.gameDao())

    val observeTemplates = ObserveTemplates(templateRepository)
    val saveTemplate = SaveTemplate(templateRepository)
    val deleteTemplate = DeleteTemplate(templateRepository)
    val observeGames = ObserveGames(gameRepository)
    val getGame = GetGame(gameRepository)
    val createGame = CreateGame(gameRepository)
    val updatePlayerValue = UpdatePlayerValue(gameRepository)
    val finishGame = FinishGame(gameRepository)
}
```

- [ ] **Step 3: Create the Application**

`androidApp/src/main/kotlin/com/example/app/App.kt`:
```kotlin
package com.example.app

import android.app.Application
import com.example.app.di.AppContainer

class App : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
```

- [ ] **Step 4: Register the Application in the manifest**

In `androidApp/src/main/AndroidManifest.xml`, add `android:name=".App"` to the `<application>` tag.

- [ ] **Step 5: Verify it compiles**

Run: `./gradlew :androidApp:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add androidApp/
git commit -m "feat(app): manual DI container + Application wiring"
```

---

### Task 10: Edge-to-edge host + Navigation-Compose + Library route

**Files:**
- Modify: `androidApp/src/main/kotlin/com/example/app/MainActivity.kt`
- Create: `androidApp/src/main/kotlin/com/example/app/nav/AppNavHost.kt`
- Create: `androidApp/src/main/kotlin/com/example/app/viewmodel/LibraryViewModel.kt`
- Test: `androidApp/src/test/kotlin/com/example/app/viewmodel/LibraryViewModelTest.kt`
- Modify: `androidApp/build.gradle.kts` (add coroutines-test + junit to `testImplementation`)

**Interfaces:**
- Consumes: `AppContainer`, `LibraryScreen`, `libraryUiState`, `ObserveTemplates`, `ObserveGames`.
- Produces: `class LibraryViewModel(observeTemplates, observeGames) : ViewModel` exposing `uiState: StateFlow<LibraryUiState>`; `@Composable fun AppNavHost(container: AppContainer)`; an inset-aware `MainActivity`.

- [ ] **Step 1: Add test deps to `androidApp/build.gradle.kts`**

Add a `dependencies { }` entry:
```kotlin
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
```

- [ ] **Step 2: Write the failing ViewModel test**

`androidApp/src/test/kotlin/com/example/app/viewmodel/LibraryViewModelTest.kt`:
```kotlin
package com.example.app.viewmodel

import com.example.scoring.interactors.ObserveGames
import com.example.scoring.interactors.ObserveTemplates
import com.example.scoring.interactors.FakeGameRepository
import com.example.scoring.interactors.FakeTemplateRepository
import com.example.scoring.sample.DeepSea
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryViewModelTest {
    @Test fun exposes_templates_from_interactor() = runTest {
        val templates = FakeTemplateRepository(listOf(DeepSea.template))
        val vm = LibraryViewModel(ObserveTemplates(templates), ObserveGames(FakeGameRepository()))
        val state = vm.uiState.first { it.templates.isNotEmpty() }
        assertEquals(DeepSea.template.name, state.templates.single().name)
    }
}
```

> The fakes live in `:deepsea-scoring` `commonTest` (Task 3). To reuse them from `:androidApp` tests, move `FakeRepositories.kt` to `deepsea-scoring/src/commonMain/.../testing/` (a small `testing` package shipped in main) OR duplicate the two fakes into `androidApp/src/test/...`. Prefer the shared `testing` package; update Task 3's import path accordingly if you do.

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :androidApp:testDebugUnitTest --tests "com.example.app.viewmodel.LibraryViewModelTest"`
Expected: FAIL — `LibraryViewModel` unresolved.

- [ ] **Step 4: Create the ViewModel**

`androidApp/src/main/kotlin/com/example/app/viewmodel/LibraryViewModel.kt`:
```kotlin
package com.example.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scoring.interactors.ObserveGames
import com.example.scoring.interactors.ObserveTemplates
import com.example.ui.model.LibraryUiState
import com.example.ui.model.libraryUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class LibraryViewModel(
    observeTemplates: ObserveTemplates,
    observeGames: ObserveGames,
) : ViewModel() {
    val uiState: StateFlow<LibraryUiState> =
        combine(observeTemplates(), observeGames()) { templates, games ->
            libraryUiState(templates, games)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :androidApp:testDebugUnitTest --tests "com.example.app.viewmodel.LibraryViewModelTest"`
Expected: PASS.

- [ ] **Step 6: Create the NavHost**

`androidApp/src/main/kotlin/com/example/app/nav/AppNavHost.kt`:
```kotlin
package com.example.app.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.app.di.AppContainer
import com.example.app.viewmodel.LibraryViewModel
import com.example.ui.DeepSeaScreenA
import com.example.ui.screens.LibraryScreen

object Routes {
    const val HOME = "home"
    const val SCORE_ENTRY = "score_entry"
}

@Composable
fun AppNavHost(container: AppContainer) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            val vm = viewModel {
                LibraryViewModel(container.observeTemplates, container.observeGames)
            }
            val state by vm.uiState.collectAsStateWithLifecycle()
            LibraryScreen(
                state = state,
                onNewTemplate = { /* RuleBuilder route — Plan B */ },
                onOpenTemplate = { /* RuleBuilder route — Plan B */ },
                onResumeGame = { navController.navigate(Routes.SCORE_ENTRY) },
            )
        }
        composable(Routes.SCORE_ENTRY) {
            // Plan C binds this to a persisted game; for now the existing demo screen.
            DeepSeaScreenA()
        }
    }
}
```

> `collectAsStateWithLifecycle` comes from `androidx.lifecycle.runtime.compose`; it is pulled in transitively by `lifecycle-viewmodel-compose`. If unresolved, add `androidx.lifecycle:lifecycle-runtime-compose` to the catalog and `:androidApp`.

- [ ] **Step 7: Make `MainActivity` edge-to-edge and host the NavHost inside a Scaffold/TopAppBar**

Replace `androidApp/src/main/kotlin/com/example/app/MainActivity.kt` with:
```kotlin
package com.example.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.app.nav.AppNavHost

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as App).container
        setContent {
            MaterialTheme {
                Scaffold(
                    topBar = { TopAppBar(title = { Text("Board Game Score") }) },
                ) { innerPadding ->
                    Surface(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        AppNavHost(container)
                    }
                }
            }
        }
    }
}
```

> The `Scaffold` applies system-bar/cutout insets via `innerPadding`; combined with `enableEdgeToEdge()` this fixes content drawing behind the camera cutout. The `TopAppBar` is the toolbar from the user's request.

- [ ] **Step 8: Build the app**

Run: `./gradlew :androidApp:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 9: Full regression run**

Run: `./gradlew jvmTest :data:testDebugUnitTest :androidApp:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`; the Deep Sea §5 tests still pass (total 83, ranked 8/8/0).

- [ ] **Step 10: Commit**

```bash
git add androidApp/ deepsea-scoring/
git commit -m "feat(app): edge-to-edge Scaffold host + Navigation-Compose + Library screen"
```

---

## Self-review notes (for the implementer)

- **Verify-against-source flags** are inline (marked `> Note`): the exact import paths for `validate()`/`resolve()`, whether `Template` carries `scenarios`, the Flow-in-`map` suspend-DAO concern, and the location of the shared fake repositories. Resolve each against the real files before moving on — don't guess.
- **Manual verification (emulator):** after Task 10, run the app and confirm the top app bar sits below the status bar/cutout and the Library screen scrolls clear of system bars. Use the `gradle-builder` agent for builds and the `run`/`verify` skills for on-device checks.

## Plans B and C (authored after this lands)

- **Plan B — Rule builder:** `RuleBuilderUiState` + stateless `RuleBuilderScreen` (fields editor, 5-type rule palette, `describe()` lines, inline `validate()`), `RuleBuilderViewModel` using `SaveTemplate`, and a `RuleBuilder` route. Reuses `TemplateRepository`/interactors from this plan.
- **Plan C — Game flow:** `NewGameScreen` + `NewGameViewModel` (`CreateGame`), refactor `ScoreEntryScreen` to stateless + `ScoreEntryViewModel` with autosave via `UpdatePlayerValue`, and wire the `ScoreEntry` route to a real `GameId`.

These depend on the repository interfaces, interactors, `:data` module, DI, and NavHost delivered here.
