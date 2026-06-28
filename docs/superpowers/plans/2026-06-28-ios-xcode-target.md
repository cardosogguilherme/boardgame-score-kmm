# Plan — Make the project compile & run on iOS (Xcode), full Compose-Multiplatform app

**Date:** 2026-06-28
**Goal (user-chosen scope):** the **entire app** (all screens + shared ViewModels) runs on iPhone via
Compose Multiplatform, hosted by an `iosApp` Xcode project embedding a `ComposeUIViewController`;
persistence via **Room KMP (2.7+)** as one shared data layer.

## ⚠️ Hard constraint: iOS builds require macOS
Kotlin/Native iOS compilation and Xcode are **macOS-only**. This repo's current host is Windows 11
(the module build files literally note *"iOS targets deferred — no macOS on this host"*). Therefore:
- On Windows we can **author** every change and keep the **Android app + JVM tests green** (the
  83/8-8-0 anchor stays Windows-verifiable).
- The iOS link/compile steps (`linkDebugFrameworkIosSimulatorArm64`, `embedAndSignAppleFrameworkForXcode`,
  the Xcode build, simulator run) **must be done on a Mac** — a developer Mac or a **macOS CI runner**
  (e.g. GitHub Actions `macos-14`). Every increment below marks its steps **[Win-ok]** or **[Mac-only]**.
- Recommendation: add a macOS CI job early (I1) so iOS compilation is verified continuously even though
  day-to-day authoring happens on Windows.

## Where we start (current state — already iOS-favourable)
- `:deepsea-scoring` — pure Kotlin (serialization + coroutines), KMP with `androidTarget()`/`jvm()`. No
  Compose/Android in source. **iOS-ready once targets are added.**
- `:ui` — Compose Multiplatform (CMP 1.7.3, which supports iOS), all screens **stateless** in
  `commonMain`. Needs iOS targets + an iOS entry point.
- `:data` — **Android-only** (`kotlin-android` + Room 2.6.1 + KSP). **The main blocker.**
- ViewModels (`LibraryViewModel`, `RuleBuilderViewModel`, `NewGameViewModel`, `ScoreEntryViewModel`),
  `AppNavHost`, the Koin `appModule`, and `startKoin` all live in **`:androidApp`** on
  `androidx.lifecycle` + `koin-androidx-compose`. **Must move to shared/commonMain for iOS.**
- DI is **Koin** (multiplatform-friendly — the reason this is feasible). Repository interfaces +
  interactors already live in `commonMain`.
- Stack: Kotlin 2.1.0, AGP 8.7.3, Compose MP 1.7.3, KSP 2.1.0-1.0.29, Gradle 8.14, Koin 4.0.0.

## Target architecture
```
:deepsea-scoring  (commonMain + android/jvm + IOS)        domain — unchanged logic
:data             (KMP: commonMain entities/DAOs/db,      Room 2.7 KMP
                   androidMain + iosMain drivers)         (was Android-only)
:shared (NEW, or fold into :ui commonMain)                ViewModels + AppNavHost + Koin module +
                   commonMain + android + IOS             App() composable + MainViewController (iOS)
:ui               (commonMain + android/jvm + IOS)        stateless screens (Compose MP)
:androidApp       thin Android host (MainActivity -> App())
iosApp (NEW, Xcode/SwiftUI)                               embeds ComposeUIViewController(App())
```
Where **IOS** = `iosX64()`, `iosArm64()`, `iosSimulatorArm64()`.

Key portability moves:
- **ViewModels → multiplatform.** `androidx.lifecycle:lifecycle-viewmodel` is multiplatform from 2.8.0;
  `ViewModel`/`viewModelScope` work in `commonMain`. The 4 ViewModels move to the shared module
  unchanged in logic.
- **Compose VM injection → multiplatform Koin.** Replace `koin-androidx-compose`'s `koinViewModel()`
  with **`koin-compose-viewmodel`** (`org.koin.compose.viewmodel.koinViewModel`) so `AppNavHost` lives in
  `commonMain`. Navigation-Compose 2.8 is multiplatform (`org.jetbrains.androidx.navigation:navigation-compose`)
  — swap the AndroidX nav artifact for the JetBrains MP one.
- **DB construction is platform-specific.** The Koin `appModule` keeps the common bindings (repos as
  interfaces, interactors, viewModels); the `AppDatabase` builder becomes an `expect/actual` (or a
  platform Koin module): Android uses `Room.databaseBuilder(context, …)`; iOS uses
  `Room.databaseBuilder<AppDatabase>(path)` + `BundledSQLiteDriver()`.
- **`startKoin`** moves to a shared `initKoin()` called from both `ScoreApp` (Android) and the iOS
  entry point.

## Global constraints (bind every increment)
- **Android stays green throughout.** After every increment: `./gradlew :androidApp:assembleDebug` +
  `./gradlew jvmTest :data:testDebugUnitTest :androidApp:testDebugUnitTest` pass; **83 / 8-8-0 holds.**
- Keep the anti-corruption layering and Koin DI. Don't regress the existing feature work (Plans A/B/C).
- Each increment is independently buildable; iOS-only steps are **[Mac-only]** and verified on a Mac/CI.
- Pin versions in `gradle/libs.versions.toml`; verify Room 2.7 ↔ Kotlin 2.1.0 ↔ KSP ↔ AGP compatibility
  before committing the bump.

## Increments

### I0 — Decision record + dependency groundwork  [Win-ok]
- Write an ADR/spec capturing: full-Compose-on-iOS, Room KMP, macOS requirement, CI plan.
- Add catalog entries (no wiring yet): Room **2.7.x** (room-runtime KMP + `androidx.sqlite:sqlite-bundled`),
  `koin-compose-viewmodel`, multiplatform `lifecycle-viewmodel` (2.8.x), the JetBrains MP
  `navigation-compose`, and the Compose-iOS bits. Verify versions resolve for the **Android** build first.
- **Verify:** Android build + full regression green. No iOS targets yet.

### I1 — iOS targets on `:deepsea-scoring` + macOS CI  [authoring Win-ok; link Mac-only]
- Add `iosX64()/iosArm64()/iosSimulatorArm64()` to `:deepsea-scoring`; remove the "deferred" comment.
  Pure-Kotlin domain should compile for iOS unchanged.
- Add a **GitHub Actions macOS job** that runs the iOS compile (e.g. `./gradlew compileKotlinIosSimulatorArm64`)
  so iOS is verified from here on, despite the Windows dev host.
- **Verify:** Android+JVM green [Win]; domain compiles for iOS [Mac/CI].

### I2 — Convert `:data` to KMP with Room 2.7 KMP  [authoring Win-ok; iOS link Mac-only]  ← hardest
- Make `:data` a Kotlin-Multiplatform module (`androidTarget()` + IOS); move `entity/`, `dao/`,
  `AppDatabase`, `mapper/`, `repository/` into `commonMain`.
- Upgrade Room 2.6.1 → 2.7 KMP: annotate `@Database`/DAOs as before; add the
  `@ConstructedBy(AppDatabaseConstructor::class)` + `expect object … : RoomDatabaseConstructor<AppDatabase>`
  pattern; KSP runs per target. Use `BundledSQLiteDriver` (`androidx.sqlite:sqlite-bundled`).
- `expect fun appDatabaseBuilder(...)` / `actual` per platform (Android: Context; iOS: NSDocumentDirectory
  path). Keep the Robolectric `:data` tests for Android (`androidUnitTest`); the round-trip/mapper tests
  move to `commonTest` where they don't need Android.
- **Verify:** Android `:data` tests (Room 2.7) + full regression green [Win]; `:data` compiles for iOS [Mac/CI].
- **Risk:** Room KMP migration is the riskiest step (driver setup, KSP per-target, schema export). Do it
  in isolation; if Room 2.7↔Kotlin 2.1 friction appears, fall back to a per-platform repo impl behind the
  existing domain interfaces (the Android Room impl stays; an iOS impl is added) — but the chosen path is
  one shared Room layer.

### I3 — Move ViewModels + AppNavHost + Koin into a shared module  [Win-ok]
- Create `:shared` (KMP: commonMain + androidTarget + IOS) — or fold into `:ui` commonMain — and move the
  4 ViewModels, `AppNavHost`, `Routes`, the Koin `appModule`, and an `initKoin()` there.
- Swap `koin-androidx-compose` → `koin-compose-viewmodel`'s `koinViewModel()`/`parametersOf`; swap
  AndroidX `navigation-compose` → JetBrains MP `navigation-compose`. Swap `lifecycle-viewmodel-compose`
  (Android) → multiplatform `lifecycle-viewmodel`.
- Split the DB binding into platform Koin modules (Android context vs iOS path) via expect/actual.
- `:androidApp` becomes a thin host: `ScoreApp` calls `initKoin{ androidContext }`; `MainActivity` calls
  the shared `App()` composable (the Scaffold/NavHost now in shared). Keep `enableEdgeToEdge()`.
- **Verify:** Android app builds & runs, full regression + 83/8-8-0 green [Win]. (No behavior change —
  pure relocation + DI/nav artifact swap.)

### I4 — iOS targets on `:ui`/`:shared` + Compose iOS entry point  [authoring Win-ok; link Mac-only]
- Add IOS targets to `:ui` (and `:shared`). Add the Compose-iOS runtime deps.
- Add `MainViewController.kt` in the shared iOS source: `fun MainViewController() = ComposeUIViewController { App() }`.
- Configure the framework export (umbrella framework from `:shared`/`:ui`, `isStatic`/baseName), and the
  `embedAndSignAppleFrameworkForXcode` Gradle task wiring.
- **Verify:** `linkDebugFrameworkIosSimulatorArm64` succeeds [Mac/CI]; Android still green [Win].

### I5 — `iosApp` Xcode project  [Mac-only]
- Create `iosApp/` — a SwiftUI app whose `ContentView` wraps the Kotlin `MainViewController()` via
  `UIViewControllerRepresentable`. Integrate the framework (direct Xcode "Run Script" calling
  `embedAndSignAppleFrameworkForXcode`, or SPM/CocoaPods). Set bundle id, deployment target, signing.
- **Verify [Mac-only]:** open in Xcode, build, run on the iOS Simulator — exercise the full flow
  (Library → "+" author template → New game → score entry autosave → resume → finish). Confirm Room
  persistence survives an app relaunch on iOS.

### I6 — Polish & parity  [mixed]
- iOS insets/safe-area parity (Compose `WindowInsets` on iOS), app icon/launch screen, and confirm the
  edge-to-edge behavior maps sensibly to iOS.
- Wire the iOS compile (and, if feasible, a simulator UI smoke) into CI.

## Risks & mitigations
- **No Mac on the dev host** → all iOS verification via macOS CI / a Mac. Author on Windows; never assume
  iOS compiles until CI says so. *Biggest schedule risk.*
- **Room KMP 2.7 migration** (I2) — driver/KSP/schema friction. Mitigation: isolate; fallback to a
  per-platform repo impl behind the existing interfaces if needed.
- **Version matrix** (Room 2.7 ↔ Kotlin 2.1.0 ↔ Compose MP 1.7.3 ↔ nav-MP ↔ Koin 4) — verify each bump
  keeps Android green before adding iOS.
- **ViewModel/nav artifact swaps** (I3) could subtly change Android behavior — guard with the existing VM
  tests + full regression.

## Out of scope (flag, don't silently add)
- Re-skinning to native SwiftUI (we're doing shared Compose UI, per the chosen scope).
- iOS-specific features (widgets, iCloud sync), App Store distribution/signing beyond a dev build.
- Replacing Room with SQLDelight (Room KMP was chosen).

## Suggested execution
Run as its own SDD increment after the current branch closes out. I1→I2→I3 are the substance (and
Win-authorable); I4→I5 need a Mac/CI to verify. Land a macOS CI runner in I1 so every later increment is
provable.
