# Story 1.1: KMP Project Initialization

Status: done

## Story

As a developer,
I want to initialize the KMP project from the JetBrains wizard with correct module naming and all library dependencies declared,
so that the project compiles and runs on both iOS and Android with no structural debt to fix later.

## Acceptance Criteria

1. **Given** the JetBrains KMP Wizard is used with Android + iOS + Compose Multiplatform selected, **When** the generated project is opened, **Then** the `composeApp` module is renamed to `shared` in `settings.gradle.kts`, all import paths, and the Xcode `embedAndSignAppleFrameworkForXcode` build phase script.

2. **Given** the project is configured, **When** `gradle/libs.versions.toml` is created, **Then** it declares all locked versions: Kotlin 2.3.20, Compose Multiplatform 1.10.0, SQLDelight 2.2.1, Koin 3.5, Navigation 3 (1.0.1), kotlinx-datetime 0.7.1, Coroutines 1.10.2.

3. **Given** the module structure is in place, **When** the project is built, **Then** the Android app launches to a blank screen on a device/emulator without errors.

4. **Given** the module structure is in place, **When** the iOS app is built in Xcode, **Then** it compiles and launches to a blank screen without errors.

5. **Given** the package name is configured, **When** any source file is opened, **Then** the root package is `com.sonja.tracker` throughout all source sets.

## Tasks / Subtasks

- [x] Task 1: Generate project from JetBrains KMP Wizard (AC: 1, 2, 5)
  - [x] Go to https://kmp.jetbrains.com/ — select Android + iOS targets; Compose Multiplatform UI; no server/desktop/web
  - [x] Download the generated zip and extract it

- [x] Task 2: Rename `composeApp` module to `shared` (AC: 1)
  - [x] Update `settings.gradle.kts`: change `include(":composeApp")` → `include(":shared")`
  - [x] Rename the `composeApp/` directory to `shared/`
  - [x] Updated package declarations in all Kotlin source files (no import path issue — wizard uses single module)
  - [x] Update the Xcode `embedAndSignAppleFrameworkForXcode` Build Phase script: `:composeApp:` → `:shared:`
  - [x] Java PATH fix added to Build Phase script (`export JAVA_HOME=$(/usr/libexec/java_home)`)
  - [x] Verified `shared/build.gradle.kts` has `kotlinMultiplatform` and `composeMultiplatform` plugins applied

- [x] Task 3: Create `gradle/libs.versions.toml` with all locked versions (AC: 2)
  - [x] Updated `[versions]` with all locked versions (see deviation notes below)
  - [x] Added SQLDelight, Koin, Navigation 3, kotlinx-datetime, Coroutines library entries
  - [x] Added `sqldelight` plugin entry
  - [x] All version references use catalog aliases — no hardcoded versions in build files

- [x] Task 4: Set root package to `com.sonja.tracker` (AC: 5)
  - [x] `namespace = "com.sonja.tracker"` in `shared/build.gradle.kts`
  - [x] `applicationId = "com.sonja.tracker"` in `shared/build.gradle.kts`
  - [x] All Kotlin source files under `commonMain`, `androidMain`, `iosMain`, `commonTest` updated to `package com.sonja.tracker`
  - [x] `compose.resources { packageOfResClass = "com.sonja.tracker.generated.resources" }` set; resource import in `App.kt` updated

- [x] Task 5: Verify Android build (AC: 3)
  - [x] `./gradlew :shared:assembleDebug` — BUILD SUCCESSFUL (42 tasks)
  - [x] `./gradlew :shared:testDebugUnitTest` — BUILD SUCCESSFUL
  - [ ] Launch on Android device/emulator — blank screen visible, no crash ← **manual step for Sonja**

- [ ] Task 6: Verify iOS build (AC: 4) ← **manual step for Sonja**
  - [x] Java PATH fix already applied to Xcode Build Phase script
  - [ ] Open `iosApp/iosApp.xcodeproj` in Xcode
  - [ ] Build and run — blank screen visible, no crash

## Dev Notes

### Project initialization — exact steps

1. **Wizard URL:** https://kmp.jetbrains.com/ — select "Android" + "iOS" targets; enable "Compose Multiplatform"; disable server/desktop/web
2. **Download** the generated zip; extract to your working directory
3. **Rename immediately** before writing any code — module names embed deeply into import paths, Gradle task names, and the Xcode framework name

### Module rename checklist (critical — do not miss any location)

The wizard generates `composeApp`. Every occurrence must be changed to `shared`:

| File | What to change |
|------|----------------|
| `settings.gradle.kts` | `include(":composeApp")` → `include(":shared")` |
| Directory name | `composeApp/` → `shared/` |
| Xcode Build Phase script | `:composeApp:embedAndSignAppleFrameworkForXcode` → `:shared:embedAndSignAppleFrameworkForXcode` |
| Any `build.gradle.kts` referencing `project(":composeApp")` | → `project(":shared")` |

### `gradle/libs.versions.toml` — all locked versions

```toml
[versions]
kotlin = "2.3.20"
compose-multiplatform = "1.10.0"
sqldelight = "2.2.1"
koin = "3.5.0"
navigation = "1.0.1"
kotlinx-datetime = "0.7.1"
coroutines = "1.10.2"
android-minSdk = "26"
android-compileSdk = "35"
android-targetSdk = "35"

[libraries]
# Compose
compose-ui = { module = "org.jetbrains.compose.ui:ui", version.ref = "compose-multiplatform" }
compose-material3 = { module = "org.jetbrains.compose.material3:material3", version.ref = "compose-multiplatform" }

# SQLDelight
sqldelight-runtime = { module = "app.cash.sqldelight:runtime", version.ref = "sqldelight" }
sqldelight-coroutines = { module = "app.cash.sqldelight:coroutines-extensions", version.ref = "sqldelight" }
sqldelight-android-driver = { module = "app.cash.sqldelight:android-driver", version.ref = "sqldelight" }
sqldelight-native-driver = { module = "app.cash.sqldelight:native-driver", version.ref = "sqldelight" }

# Koin
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin" }
koin-compose-viewmodel = { module = "io.insert-koin:koin-compose-viewmodel", version.ref = "koin" }

# Navigation 3
navigation-compose = { module = "org.jetbrains.androidx.navigation:navigation-compose", version.ref = "navigation" }

# kotlinx-datetime
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinx-datetime" }

# Coroutines
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
compose-multiplatform = { id = "org.jetbrains.compose", version.ref = "compose-multiplatform" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
sqldelight = { id = "app.cash.sqldelight", version.ref = "sqldelight" }
android-application = { id = "com.android.application", version = "9.0.0" }
android-library = { id = "com.android.library", version = "9.0.0" }
```

> **Note:** Do not add SQLDelight, Koin, or Navigation dependencies to any `build.gradle.kts` in this story — declare them in the version catalog only. They will be wired in stories 1.2 and 1.3.

### Module structure after rename

The final directory layout established by this story:

```
tracker/
├── build.gradle.kts          # root — plugin declarations only
├── settings.gradle.kts       # include(":shared"), include(":androidApp"), include(":iosApp")
├── gradle/
│   └── libs.versions.toml    # all locked versions (created in this story)
├── gradle.properties
├── shared/                   # renamed from composeApp
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/com/sonja/tracker/
│       ├── androidMain/kotlin/com/sonja/tracker/
│       └── iosMain/kotlin/com/sonja/tracker/
├── androidApp/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── kotlin/com/sonja/tracker/android/
└── iosApp/
    ├── iosApp.xcodeproj/
    └── iosApp/
        ├── iOSApp.swift
        └── ContentView.swift
```

Internal package structure within `shared/src/commonMain/kotlin/com/sonja/tracker/` will be created by stories 1.2–1.4:
- `data/db/`, `data/repository/`, `domain/model/`, `domain/scheduler/`, `ui/`, `notifications/`, `di/`

**Do not create these subdirectories in this story** — stub placeholders cause confusion for later stories.

### iOS build gotcha — Java PATH

Xcode Build Phase scripts run in a shell that may not have Java on `$PATH`. On first iOS build, the `embedAndSignAppleFrameworkForXcode` phase may fail silently or with `command not found`.

**Fix:** Add this to the top of the `embedAndSignAppleFrameworkForXcode` Build Phase script in Xcode:

```bash
export JAVA_HOME=$(/usr/libexec/java_home)
export PATH="$JAVA_HOME/bin:$PATH"
```

This is a one-time setup step. See `CONTRIBUTING.md` for the full debugging procedure.

### Package name

Root package `com.sonja.tracker` must be consistent across:
- `shared/build.gradle.kts` (kotlin multiplatform source sets)
- `androidApp/build.gradle.kts` (`android { namespace = "com.sonja.tracker.android" }`)
- `AndroidManifest.xml`
- All Kotlin source files

### What this story does NOT deliver

Stories 1.2–1.4 build on this foundation. Do **not** create any of the following in Story 1.1:
- `DatabaseDriverFactory.kt` or any SQLDelight files (Story 1.2)
- Any Koin module files or `AppPreferences` (Story 1.3)
- `AppNavigation.kt`, theme files, or any screens (Story 1.4)

The app at the end of this story shows whatever blank screen/preview the wizard generates by default.

### Project Structure Notes

- Architecture mandates one `:shared` module with internal packages — do NOT split into `:shared-domain`, `:shared-data`, etc.
- `androidApp` is the Android entry point; `iosApp` is the Xcode project
- All library versions are locked and must be set in `libs.versions.toml` — never hardcode versions in individual `build.gradle.kts` files
- iOS integration uses direct XCFramework embedding (not SPM) — the wizard sets this up automatically; preserve it

### References

- [Source: architecture.md#Starter Template Evaluation] — JetBrains KMP Wizard selection rationale
- [Source: architecture.md#Complete Project Directory Structure] — exact directory layout
- [Source: architecture.md#Naming Patterns] — package naming conventions
- [Source: CONTRIBUTING.md#Module naming] — module rename requirement and rationale
- [Source: CONTRIBUTING.md#iOS Build Notes] — Java PATH fix for Xcode Build Phase scripts
- [Source: CONTRIBUTING.md#Story 1 checklist] — minimum story 1 deliverables (note: DatabaseDriverFactory and schema stub are in Story 1.2)
- [Source: epics.md#Story 1.1] — acceptance criteria

### Review Findings

- [x] [Review][Decision] androidApplication plugin on :shared vs. separate androidApp module — resolved: refactored to separate `:androidApp` module (androidApplication) + `:shared` (androidLibrary), matching architecture spec.

- [x] [Review][Patch] enableEdgeToEdge() called before super.onCreate() [`androidApp/src/main/kotlin/com/sonja/tracker/MainActivity.kt`]
- [x] [Review][Patch] compose-uiToolingPreview declared in both commonMain and androidMain — removed from androidMain, kept in commonMain [`shared/build.gradle.kts`]
- [x] [Review][Patch] AndroidManifest uses legacy Theme.Material.Light.NoActionBar — replaced with Theme.AppCompat.DayNight.NoActionBar [`androidApp/src/main/AndroidManifest.xml`]
- [x] [Review][Patch] iOS framework baseName = "ComposeApp" — renamed to "Shared"; ContentView.swift import updated [`shared/build.gradle.kts`, `iosApp/iosApp/ContentView.swift`]
- [x] [Review][Patch] ComposeAppCommonTest.kt filename retains wizard prefix — renamed to SupplementTrackerCommonTest.kt

- [x] [Review][Defer] No iosX64 target — Intel Mac simulator builds will fail [`shared/build.gradle.kts`] — deferred, pre-existing wizard output; team is Apple Silicon
- [x] [Review][Defer] JAVA_HOME no error guard in Xcode build phase script — silent failure on machines with no JDK [`iosApp/iosApp.xcodeproj/project.pbxproj`] — deferred, pre-existing; documented in CONTRIBUTING.md
- [x] [Review][Defer] No androidTest source set despite espresso/testExt in catalog [`shared/build.gradle.kts`] — deferred, out of scope for Story 1.1

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- Module renamed `composeApp` → `shared` in directory, `settings.gradle.kts`, `shared/build.gradle.kts`, and Xcode Build Phase script.
- Package renamed from `org.sonja.supplementtracker` → `com.sonja.tracker` in all 8 Kotlin source files.
- `gradle/libs.versions.toml` updated: all locked versions added, SQLDelight/Koin/Navigation/datetime/Coroutines library+plugin entries added.
- `local.properties` `sdk.dir` set to `/Users/sonjabrzak/Library/Android/sdk`.
- `compose.resources { packageOfResClass }` set explicitly; `App.kt` resource import updated to `com.sonja.tracker.generated.resources`.
- Java PATH fix applied to `iosApp.xcodeproj` Build Phase script.
- **Version deviations from spec (incompatible artifacts — wizard versions used):**
  - `composeMultiplatform`: spec `1.10.0` → actual `1.10.3` (`material3:1.10.0` artifact does not exist in any Maven repo)
  - `material3`: separate version `1.10.0-alpha05` retained (tracks its own release cadence from CMP umbrella)
  - `android-compileSdk` / `android-targetSdk`: spec `35` → actual `36` (`activity-compose:1.13.0` enforces compileSdk ≥ 36)
- Android build: `assembleDebug` and `testDebugUnitTest` — BUILD SUCCESSFUL.
- iOS Kotlin compilation: `compileKotlinIosSimulatorArm64` — UP-TO-DATE/success. Framework link requires Xcode (manual Task 6).

### File List

- `settings.gradle.kts`
- `shared/build.gradle.kts`
- `gradle/libs.versions.toml`
- `local.properties`
- `iosApp/iosApp.xcodeproj/project.pbxproj`
- `shared/src/commonMain/kotlin/org/sonja/supplementtracker/App.kt`
- `shared/src/commonMain/kotlin/org/sonja/supplementtracker/Platform.kt`
- `shared/src/commonMain/kotlin/org/sonja/supplementtracker/Greeting.kt`
- `shared/src/androidMain/kotlin/org/sonja/supplementtracker/MainActivity.kt`
- `shared/src/androidMain/kotlin/org/sonja/supplementtracker/Platform.android.kt`
- `shared/src/iosMain/kotlin/org/sonja/supplementtracker/MainViewController.kt`
- `shared/src/iosMain/kotlin/org/sonja/supplementtracker/Platform.ios.kt`
- `shared/src/commonTest/kotlin/org/sonja/supplementtracker/ComposeAppCommonTest.kt`
