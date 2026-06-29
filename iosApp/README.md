# iosApp — Xcode host for the shared Compose app

This is a thin SwiftUI host. The entire UI + logic comes from the Kotlin `:shared` module
(`MainViewController()` → `ComposeUIViewController { App() }`), exactly the same code that the
Android app runs. SwiftUI only wraps that view controller.

## ⚠️ Requires macOS + Xcode
Kotlin/Native iOS compilation and Xcode are **macOS-only**. This project was authored on Windows,
where the Android app + JVM tests are fully built and verified, but **none of the iOS targets can be
compiled or linked off a Mac**. Everything in this folder is therefore *unverified on the authoring
host* — build it on a Mac (or a macOS CI runner) to confirm.

## What's here
- `iosApp/iOSApp.swift` — `@main` SwiftUI app.
- `iosApp/ContentView.swift` — wraps `MainViewControllerKt.MainViewController()` in a
  `UIViewControllerRepresentable`.
- `iosApp/Info.plist` — bundle metadata (driven by build settings).
- `Configuration/Config.xcconfig` — `APP_NAME`, `BUNDLE_ID`, `TEAM_ID` (fill in on the Mac).
- `iosApp.xcodeproj` — the Xcode project. Its **"Compile Kotlin Framework"** build phase runs
  `./gradlew :shared:embedAndSignAppleFrameworkForXcode`, which builds and embeds the `Shared`
  framework (configured in `shared/build.gradle.kts`).

## Build & run on a Mac
1. Install **JDK 21** (the Gradle 8.14 daemon needs it; see the root README) and Xcode 15+.
2. From the repo root, sanity-check the shared framework compiles for the simulator:
   ```bash
   ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
   ```
3. Set your team/bundle id in `Configuration/Config.xcconfig` (or in Xcode → Signing & Capabilities).
4. Open `iosApp/iosApp.xcodeproj` in Xcode, pick an iPhone simulator, and **Run**. The
   "Compile Kotlin Framework" phase will invoke Gradle to build + embed `Shared.framework`.
5. Exercise the full flow to confirm parity with Android: Library → **+** author a template →
   **New game** → score entry (autosave) → resume → finish. Relaunch the app to confirm Room
   persistence survives on iOS.

## Notes / likely first-run touch-ups (on the Mac)
- `embedAndSignAppleFrameworkForXcode` selects the right target/arch from Xcode's environment
  variables; no per-arch wiring is needed in the project.
- If Xcode's user-script sandboxing blocks Gradle, `ENABLE_USER_SCRIPT_SANDBOXING = NO` is already
  set. If signing fails for a device build, set a valid `TEAM_ID`.
- No app icon / accent color asset catalog is included (a dev/simulator build doesn't need one); add
  `Assets.xcassets` in Xcode if you want an icon.
