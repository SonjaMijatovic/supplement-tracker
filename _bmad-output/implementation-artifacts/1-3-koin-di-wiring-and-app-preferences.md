# Story 1.3: Koin DI Wiring & App Preferences

Status: done

## Story

As a developer,
I want Koin fully wired across all three module files and AppPreferences implemented,
so that any ViewModel or repository can be injected by subsequent stories without additional DI setup.

## Acceptance Criteria

1. **Given** Koin 3.5 is declared in `libs.versions.toml`, **When** `SharedModule.kt` is created in `commonMain/di/`, **Then** it declares `single { }` bindings for the SQLDelight DB instance, `ItemRepository`, and `LogRepository` (stubs acceptable at this stage).

2. **Given** platform modules are configured, **When** `AndroidModule.kt` is created in `androidMain/di/`, **Then** it binds `androidContext()` and the Android `DatabaseDriverFactory` actual, **And** `startKoin { androidContext(this) + modules(...) }` is called from `TrackerApplication.onCreate()`.

3. **Given** the iOS module is configured, **When** `IosModule.kt` is created in `iosMain/di/`, **Then** a shared `initKoin()` function in `iosMain` is callable from the iOS `@main` entry point.

4. **Given** `AppPreferences` expect class is created in `commonMain/data/prefs/`, **When** the iOS and Android `actual` implementations are provided, **Then** `getInstallDate()` / `setInstallDate(date)` reads and writes a `YYYY-MM-DD` string, `getLastScheduledDate()` / `setLastScheduledDate(date)` similarly, **And** `isNotificationBannerDismissed()` / `setNotificationBannerDismissed()` persists correctly across app restarts on both platforms.

5. **Given** Koin is initialized, **When** any `koinViewModel()` or `get()` call is made, **Then** dependencies resolve without runtime injection errors on both platforms.

## Tasks / Subtasks

- [x] Task 1: Add Koin dependencies to build files (AC: 1, 2, 3, 5)
  - [x] Add `implementation(libs.koin.core)` to `commonMain.dependencies` in `shared/build.gradle.kts`
  - [x] Add `implementation(libs.koin.android)` to `androidMain.dependencies` in `shared/build.gradle.kts` (needed for `androidContext()` in `AndroidModule`)
  - [x] Add `implementation(libs.koin.android)` to `androidApp/build.gradle.kts` (needed for `startKoin` in `TrackerApplication`)

- [x] Task 2: Create repository stubs (AC: 1)
  - [x] Create `shared/src/commonMain/kotlin/com/sonja/tracker/data/repository/ItemRepository.kt` — stub class that takes `TrackerDatabase` constructor arg
  - [x] Create `shared/src/commonMain/kotlin/com/sonja/tracker/data/repository/LogRepository.kt` — stub class that takes `TrackerDatabase` constructor arg

- [x] Task 3: Create Koin module files (AC: 1, 2, 3)
  - [x] Create `shared/src/commonMain/kotlin/com/sonja/tracker/di/SharedModule.kt`
  - [x] Create `shared/src/androidMain/kotlin/com/sonja/tracker/di/AndroidModule.kt`
  - [x] Create `shared/src/iosMain/kotlin/com/sonja/tracker/di/IosModule.kt` (includes `initKoin()`)

- [x] Task 4: Create `AppPreferences` expect/actual (AC: 4)
  - [x] Create `shared/src/commonMain/kotlin/com/sonja/tracker/data/prefs/AppPreferences.kt` (expect)
  - [x] Create `shared/src/androidMain/kotlin/com/sonja/tracker/data/prefs/AppPreferences.android.kt` (actual — SharedPreferences)
  - [x] Create `shared/src/iosMain/kotlin/com/sonja/tracker/data/prefs/AppPreferences.ios.kt` (actual — NSUserDefaults)

- [x] Task 5: Wire Android entry point (AC: 2, 5)
  - [x] Create `androidApp/src/main/kotlin/com/sonja/tracker/android/TrackerApplication.kt`
  - [x] Register `TrackerApplication` in `androidApp/src/main/AndroidManifest.xml`

- [x] Task 6: Wire iOS entry point (AC: 3, 5)
  - [x] Update `iosApp/iosApp/iOSApp.swift` to call `initKoin()` on launch

- [x] Task 7: Verify builds pass (AC: 5)
  - [x] `./gradlew :shared:assembleDebug` — BUILD SUCCESSFUL
  - [x] `./gradlew :androidApp:assembleDebug` — BUILD SUCCESSFUL
  - [x] `./gradlew :shared:testDebugUnitTest` — no failures introduced
  - [ ] iOS build in Xcode — still compiles and runs ← manual step for Sonja

### Review Findings

- [x] [Review][Decision] AppPreferences placement — kept in platform modules (correct design for expect/actual with platform context). Architecture doc note is aspirational but incorrect for this pattern. Dismissed.
- [x] [Review][Patch] Koin double-init crash on iOS — fixed: `initKoin()` now checks `KoinPlatformTools.defaultContext().getOrNull() == null` before calling `startKoin` [shared/src/iosMain/kotlin/com/sonja/tracker/di/IosModule.kt]
- [x] [Review][Patch] `setNotificationBannerDismissed()` is write-only — fixed: added `clearNotificationBannerDismissed()` to expect class and both actuals [shared/src/commonMain/kotlin/com/sonja/tracker/data/prefs/AppPreferences.kt]
- [x] [Review][Patch] Android `AppPreferences` lazy null crash is deferred — fixed: replaced `by lazy` with `run { }` initializer so context validation fails eagerly at construction time [shared/src/androidMain/kotlin/com/sonja/tracker/data/prefs/AppPreferences.android.kt]
- [x] [Review][Defer] BroadcastReceiver/NSE process restart without Koin — by architecture design; receivers init SQLDelight directly, no Koin (Epic 4)
- [x] [Review][Defer] `NSSearchPathForDirectoriesInDomains` empty-list crash in `DatabaseDriverFactory.ios.kt` — pre-existing Story 1-2 code, not introduced here
- [x] [Review][Defer] iOS iosArm64Main/iosSimulatorArm64Main declared separately — pre-existing Story 1-2 pattern, not introduced here
- [x] [Review][Defer] `TrackerDatabase` driver creation failure not caught — pre-existing; no graceful fallback if SQLite init fails
- [x] [Review][Defer] iOS `AppPreferences` context ignored for NSE App Group shared storage — future concern for Epic 4 notification implementation

## Dev Notes

### Critical: expect/actual constructor pattern (from Story 1.2 learnings)

Kotlin 2.x requires `actual constructor` keyword explicitly. Default argument values (`= null`) must only appear on the `expect` declaration; omit them from the `actual` constructor. The `-Xexpect-actual-classes` flag is already added to `shared/build.gradle.kts` in Story 1.2.

```kotlin
// commonMain — expect with default
expect class AppPreferences(context: Any? = null) {
    fun getInstallDate(): String?
    fun setInstallDate(date: String)
    fun getLastScheduledDate(): String?
    fun setLastScheduledDate(date: String)
    fun isNotificationBannerDismissed(): Boolean
    fun setNotificationBannerDismissed()
}

// Android actual — no default arg, explicit actual constructor
actual class AppPreferences actual constructor(private val context: Any?) {
    ...
}

// iOS actual — same pattern
actual class AppPreferences actual constructor(private val context: Any?) {
    ...
}
```

### Task 1: Koin dependency additions

`shared/build.gradle.kts` — add to the existing `sourceSets` block alongside existing SQLDelight entries:

```kotlin
commonMain.dependencies {
    // existing deps...
    implementation(libs.koin.core)
}
androidMain.dependencies {
    implementation(libs.sqldelight.android.driver)  // already exists
    implementation(libs.koin.android)               // ADD
}
```

`androidApp/build.gradle.kts` — add to `dependencies` block:

```kotlin
dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.koin.android)               // ADD
    debugImplementation(libs.compose.uiTooling)
}
```

**Note:** `iosMain` does NOT need a Koin dep — `koin-core` from `commonMain` already includes the native Kotlin multiplatform support for iOS.

**Note:** `koin-compose` and `koin-compose-viewmodel` are NOT wired in this story — they're added in Story 2.1 when the first ViewModel is created.

### Task 2: Repository stubs

Keep these minimal — they exist only to satisfy Koin bindings. Do NOT add methods, domain models, or business logic.

```kotlin
// shared/src/commonMain/kotlin/com/sonja/tracker/data/repository/ItemRepository.kt
package com.sonja.tracker.data.repository

import com.sonja.tracker.TrackerDatabase

class ItemRepository(private val database: TrackerDatabase)
```

```kotlin
// shared/src/commonMain/kotlin/com/sonja/tracker/data/repository/LogRepository.kt
package com.sonja.tracker.data.repository

import com.sonja.tracker.TrackerDatabase

class LogRepository(private val database: TrackerDatabase)
```

### Task 3: Koin module files

**SharedModule.kt** — provides DB + repositories. Consumes `DatabaseDriverFactory` and `AppPreferences` that are bound by platform modules.

```kotlin
// shared/src/commonMain/kotlin/com/sonja/tracker/di/SharedModule.kt
package com.sonja.tracker.di

import com.sonja.tracker.TrackerDatabase
import com.sonja.tracker.data.db.DatabaseDriverFactory
import com.sonja.tracker.data.repository.ItemRepository
import com.sonja.tracker.data.repository.LogRepository
import org.koin.dsl.module

val sharedModule = module {
    single { get<DatabaseDriverFactory>().createDriver() }
    single { TrackerDatabase(get()) }
    single { ItemRepository(get()) }
    single { LogRepository(get()) }
}
```

**AndroidModule.kt** — provides Android-specific actuals. Uses `androidContext()` from koin-android.

```kotlin
// shared/src/androidMain/kotlin/com/sonja/tracker/di/AndroidModule.kt
package com.sonja.tracker.di

import com.sonja.tracker.data.db.DatabaseDriverFactory
import com.sonja.tracker.data.prefs.AppPreferences
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single { DatabaseDriverFactory(androidContext()) }
    single { AppPreferences(androidContext()) }
}
```

**IosModule.kt** — provides iOS-specific actuals + `initKoin()` entry point callable from Swift.

```kotlin
// shared/src/iosMain/kotlin/com/sonja/tracker/di/IosModule.kt
package com.sonja.tracker.di

import com.sonja.tracker.data.db.DatabaseDriverFactory
import com.sonja.tracker.data.prefs.AppPreferences
import org.koin.core.context.startKoin
import org.koin.dsl.module

val iosModule = module {
    single { DatabaseDriverFactory() }
    single { AppPreferences() }
}

fun initKoin() {
    startKoin {
        modules(sharedModule, iosModule)
    }
}
```

### Task 4: AppPreferences implementations

**commonMain expect:**

```kotlin
// shared/src/commonMain/kotlin/com/sonja/tracker/data/prefs/AppPreferences.kt
package com.sonja.tracker.data.prefs

expect class AppPreferences(context: Any? = null) {
    fun getInstallDate(): String?
    fun setInstallDate(date: String)
    fun getLastScheduledDate(): String?
    fun setLastScheduledDate(date: String)
    fun isNotificationBannerDismissed(): Boolean
    fun setNotificationBannerDismissed()
}
```

**Android actual — SharedPreferences:**

```kotlin
// shared/src/androidMain/kotlin/com/sonja/tracker/data/prefs/AppPreferences.android.kt
package com.sonja.tracker.data.prefs

import android.content.Context
import android.content.SharedPreferences

private const val PREFS_NAME = "tracker_prefs"
private const val KEY_INSTALL_DATE = "install_date"
private const val KEY_LAST_SCHEDULED_DATE = "last_scheduled_date"
private const val KEY_NOTIFICATION_BANNER_DISMISSED = "notification_banner_dismissed"

actual class AppPreferences actual constructor(private val context: Any?) {
    private val prefs: SharedPreferences by lazy {
        val ctx = requireNotNull(context as? Context) {
            "Android AppPreferences requires a non-null Context"
        }
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun getInstallDate(): String? = prefs.getString(KEY_INSTALL_DATE, null)
    actual fun setInstallDate(date: String) = prefs.edit().putString(KEY_INSTALL_DATE, date).apply()
    actual fun getLastScheduledDate(): String? = prefs.getString(KEY_LAST_SCHEDULED_DATE, null)
    actual fun setLastScheduledDate(date: String) = prefs.edit().putString(KEY_LAST_SCHEDULED_DATE, date).apply()
    actual fun isNotificationBannerDismissed(): Boolean = prefs.getBoolean(KEY_NOTIFICATION_BANNER_DISMISSED, false)
    actual fun setNotificationBannerDismissed() = prefs.edit().putBoolean(KEY_NOTIFICATION_BANNER_DISMISSED, true).apply()
}
```

**iOS actual — NSUserDefaults:**

```kotlin
// shared/src/iosMain/kotlin/com/sonja/tracker/data/prefs/AppPreferences.ios.kt
package com.sonja.tracker.data.prefs

import platform.Foundation.NSUserDefaults

private const val KEY_INSTALL_DATE = "install_date"
private const val KEY_LAST_SCHEDULED_DATE = "last_scheduled_date"
private const val KEY_NOTIFICATION_BANNER_DISMISSED = "notification_banner_dismissed"

actual class AppPreferences actual constructor(private val context: Any?) {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun getInstallDate(): String? = defaults.stringForKey(KEY_INSTALL_DATE)
    actual fun setInstallDate(date: String) = defaults.setObject(date, KEY_INSTALL_DATE)
    actual fun getLastScheduledDate(): String? = defaults.stringForKey(KEY_LAST_SCHEDULED_DATE)
    actual fun setLastScheduledDate(date: String) = defaults.setObject(date, KEY_LAST_SCHEDULED_DATE)
    actual fun isNotificationBannerDismissed(): Boolean = defaults.boolForKey(KEY_NOTIFICATION_BANNER_DISMISSED)
    actual fun setNotificationBannerDismissed() = defaults.setBool(true, KEY_NOTIFICATION_BANNER_DISMISSED)
}
```

### Task 5: Android entry point

**TrackerApplication.kt** — lives in `androidApp`, not `shared`:

```kotlin
// androidApp/src/main/kotlin/com/sonja/tracker/android/TrackerApplication.kt
package com.sonja.tracker.android

import android.app.Application
import com.sonja.tracker.di.androidModule
import com.sonja.tracker.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@TrackerApplication)
            modules(sharedModule, androidModule)
        }
    }
}
```

**AndroidManifest.xml** — add `android:name` to `<application>`:

```xml
<application
    android:name=".android.TrackerApplication"
    android:allowBackup="true"
    ...>
```

The existing `MainActivity` package is `com.sonja.tracker` (no `.android` sub-package), but `TrackerApplication` is in `com.sonja.tracker.android`. The manifest namespace is `com.sonja.tracker`, so `.android.TrackerApplication` resolves to `com.sonja.tracker.android.TrackerApplication`. Verify this compiles — if needed, use the full class name instead.

### Task 6: iOS entry point

Swift calls Kotlin's top-level `initKoin()` function. In Swift, Kotlin top-level functions are accessed on the file's companion object using the file name. `IosModule.kt` → `IosModuleKt`:

```swift
// iosApp/iosApp/iOSApp.swift
import SwiftUI
import shared  // the KMP framework

@main
struct iOSApp: App {
    init() {
        IosModuleKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
```

**Note:** Kotlin functions named `init` are prefixed with `do` in Swift to avoid conflict with Swift's `init` keyword. `initKoin` → `doInitKoin`. Verify the exact generated Swift name in Xcode if the build fails.

### Project Structure Notes

New files created in this story:

```
shared/
  src/
    commonMain/
      kotlin/com/sonja/tracker/
        data/
          prefs/
            AppPreferences.kt                       ← NEW (expect)
          repository/
            ItemRepository.kt                       ← NEW (stub)
            LogRepository.kt                        ← NEW (stub)
        di/
          SharedModule.kt                           ← NEW
    androidMain/
      kotlin/com/sonja/tracker/
        data/
          prefs/
            AppPreferences.android.kt               ← NEW (actual)
        di/
          AndroidModule.kt                          ← NEW
    iosMain/
      kotlin/com/sonja/tracker/
        data/
          prefs/
            AppPreferences.ios.kt                   ← NEW (actual)
        di/
          IosModule.kt                              ← NEW

androidApp/
  src/main/
    kotlin/com/sonja/tracker/android/
      TrackerApplication.kt                         ← NEW
    AndroidManifest.xml                             ← MODIFIED (add android:name)

iosApp/iosApp/
  iOSApp.swift                                      ← MODIFIED (add init + initKoin call)

shared/build.gradle.kts                             ← MODIFIED (koin-core + koin-android deps)
androidApp/build.gradle.kts                         ← MODIFIED (koin-android dep)
```

### What this story does NOT deliver

- No `koin-compose` or `koin-compose-viewmodel` — added in Story 2.1 when first ViewModel is created
- No domain model classes (`Item`, `LogEntry`, etc.) — Story 2.1+
- No queries in `TrackerDatabase.sq` — added when repositories are implemented (Story 2.1+)
- No navigation or UI changes — Story 1.4
- No `NotificationInterface` — deferred to Epic 4
- Do NOT create `AppImageStorage`, `TimePicker`, or theme files — Story 1.4

### Don't break Story 1.2

- `shared/build.gradle.kts` changes are additive only. Do NOT remove or change the SQLDelight plugin, deps, or config block.
- `DatabaseDriverFactory` and `TrackerDatabase.sq` are untouched.
- `App.kt`, `MainActivity.kt`, `Greeting.kt`, `Platform.kt`, `MainViewController.kt` remain untouched.
- The Compose UI greeting screen must still compile and run after this story.

### References

- [Source: architecture.md#Dependency Injection] — Koin 3.5 LTS, module structure, startKoin patterns
- [Source: architecture.md#Gaps Found & Resolved — Gap 1] — AppPreferences expect/actual exact signatures
- [Source: architecture.md#Complete Project Directory Structure] — exact file paths
- [Source: architecture.md#Architectural Boundaries] — SharedModule vs platform module responsibility split
- [Source: epics.md#Story 1.3] — acceptance criteria
- [Source: 1-2-sqldelight-database-schema-and-driver.md#Dev Agent Record] — expect/actual constructor patterns for Kotlin 2.x

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

- No issues encountered. All builds passed on first attempt.

### Completion Notes List

- Added `koin-core` to `commonMain.dependencies` and `koin-android` to `androidMain.dependencies` in `shared/build.gradle.kts`.
- Added `koin-android` to `androidApp/build.gradle.kts`.
- Created `ItemRepository` and `LogRepository` stub classes in `shared/src/commonMain/.../data/repository/`.
- Created `SharedModule.kt` binding DB driver, `TrackerDatabase`, `ItemRepository`, `LogRepository`.
- Created `AndroidModule.kt` binding `DatabaseDriverFactory(androidContext())` and `AppPreferences(androidContext())`.
- Created `IosModule.kt` binding `DatabaseDriverFactory()` and `AppPreferences()`, plus top-level `initKoin()` function.
- Created `AppPreferences` expect class with nullable `Any?` context and `= null` default.
- Android actual uses lazy `SharedPreferences` via `context.getSharedPreferences("tracker_prefs", MODE_PRIVATE)`.
- iOS actual uses `NSUserDefaults.standardUserDefaults` — no context needed.
- Created `TrackerApplication` in `androidApp/src/main/kotlin/com/sonja/tracker/android/` calling `startKoin { androidContext + modules(sharedModule, androidModule) }`.
- Registered `android:name=".android.TrackerApplication"` in `AndroidManifest.xml`.
- Updated `iOSApp.swift` to add `init() { IosModuleKt.doInitKoin() }`.
- `shared:assembleDebug`, `androidApp:assembleDebug`, `shared:testDebugUnitTest`, and `shared:compileKotlinIosSimulatorArm64` all BUILD SUCCESSFUL.
- iOS Xcode build is a manual verification step for Sonja.

### File List

- `shared/build.gradle.kts` — added `koin-core` (commonMain), `koin-android` (androidMain)
- `androidApp/build.gradle.kts` — added `koin-android`
- `shared/src/commonMain/kotlin/com/sonja/tracker/data/repository/ItemRepository.kt` — NEW stub
- `shared/src/commonMain/kotlin/com/sonja/tracker/data/repository/LogRepository.kt` — NEW stub
- `shared/src/commonMain/kotlin/com/sonja/tracker/di/SharedModule.kt` — NEW
- `shared/src/androidMain/kotlin/com/sonja/tracker/di/AndroidModule.kt` — NEW
- `shared/src/iosMain/kotlin/com/sonja/tracker/di/IosModule.kt` — NEW (includes `initKoin()`)
- `shared/src/commonMain/kotlin/com/sonja/tracker/data/prefs/AppPreferences.kt` — NEW expect
- `shared/src/androidMain/kotlin/com/sonja/tracker/data/prefs/AppPreferences.android.kt` — NEW actual (SharedPreferences)
- `shared/src/iosMain/kotlin/com/sonja/tracker/data/prefs/AppPreferences.ios.kt` — NEW actual (NSUserDefaults)
- `androidApp/src/main/kotlin/com/sonja/tracker/android/TrackerApplication.kt` — NEW
- `androidApp/src/main/AndroidManifest.xml` — added `android:name=".android.TrackerApplication"`
- `iosApp/iosApp/iOSApp.swift` — added `init()` block calling `IosModuleKt.doInitKoin()`
