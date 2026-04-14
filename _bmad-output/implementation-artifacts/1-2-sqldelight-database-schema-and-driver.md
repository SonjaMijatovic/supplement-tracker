# Story 1.2: SQLDelight Database Schema & Driver

Status: review

## Story

As a developer,
I want the SQLDelight schema defined and the database driver wired for both platforms with backup-eligible storage,
So that any future story can read and write data without additional database setup.

## Acceptance Criteria

1. **Given** SQLDelight 2.2.1 is declared in `libs.versions.toml`, **When** `TrackerDatabase.sq` is created in `shared/src/commonMain/sqldelight/com/sonja/tracker/`, **Then** it defines the `items` table and `log_entries` table with `snake_case` column names as specified below.

2. **Given** the schema is defined, **When** `DatabaseDriverFactory.kt` expect class is created in `shared/src/commonMain/kotlin/com/sonja/tracker/data/db/`, **Then** the Android `actual` uses `AndroidSqliteDriver` with `context.filesDir` (Auto Backup eligible) and the iOS `actual` uses `NativeSqliteDriver` with `Application Support/tracker.db` (backup-eligible).

3. **Given** the database driver is configured, **When** the app starts, **Then** `tracker.db` is created in the correct backup-eligible location on both platforms.

4. **Given** a schema migration is needed, **When** the schema version is incremented and a `.sqm` file is added, **Then** migration runs on next app open without data loss and migration failure throws `IllegalStateException` with a clear message (never silent).

## Tasks / Subtasks

- [x] Task 1: Wire SQLDelight plugin and dependencies in `shared/build.gradle.kts` (AC: 1, 2, 3)
  - [x] Add `alias(libs.plugins.sqldelight)` to plugins block
  - [x] Add `sqldelight-runtime` and `sqldelight-coroutines` to `commonMain.dependencies`
  - [x] Add `sqldelight-android-driver` to `androidMain.dependencies` (new sourceSet block needed)
  - [x] Add `sqldelight-native-driver` to `iosMain.dependencies` (new sourceSet block needed)
  - [x] Add `sqldelight-sqlite-driver` to `commonTest.dependencies` (new catalog entry needed in libs.versions.toml)
  - [x] Add `sqldelight { databases { create("TrackerDatabase") { packageName.set("com.sonja.tracker") } } }` configuration block

- [x] Task 2: Create `TrackerDatabase.sq` schema file (AC: 1)
  - [x] Create directory `shared/src/commonMain/sqldelight/com/sonja/tracker/`
  - [x] Create `TrackerDatabase.sq` with `items` and `log_entries` tables

- [x] Task 3: Create `DatabaseDriverFactory` expect class and actuals (AC: 2, 3)
  - [x] Create `shared/src/commonMain/kotlin/com/sonja/tracker/data/db/DatabaseDriverFactory.kt` (expect)
  - [x] Create `shared/src/androidMain/kotlin/com/sonja/tracker/data/db/DatabaseDriverFactory.android.kt` (actual)
  - [x] Create `shared/src/iosMain/kotlin/com/sonja/tracker/data/db/DatabaseDriverFactory.ios.kt` (actual)

- [x] Task 4: Verify builds pass (AC: 1, 2, 3)
  - [x] `./gradlew :shared:generateCommonMainTrackerDatabaseInterface` — SQLDelight code generation succeeds
  - [x] `./gradlew :shared:assembleDebug` — Android BUILD SUCCESSFUL
  - [x] `./gradlew :shared:testDebugUnitTest` — unit tests GREEN (no failures introduced)
  - [ ] iOS build in Xcode — still compiles and runs (no regression from Story 1.1) ← manual step for Sonja

## Dev Notes

### Critical: DatabaseDriverFactory signature (CONTRIBUTING.md requirement)

The `DatabaseDriverFactory` MUST use a nullable `Any?` context to support three initialization contexts:
- Normal Android app (pass `applicationContext`)
- Android `BroadcastReceiver` YES action (pass the `Context` from `onReceive()`)
- iOS / NSE (no argument needed)

```kotlin
// commonMain — DatabaseDriverFactory.kt
expect class DatabaseDriverFactory(context: Any? = null) {
    fun createDriver(): SqlDriver
}
```

Do NOT hardcode `applicationContext`. The BroadcastReceiver and NSE run as separate processes.

### Task 1: `shared/build.gradle.kts` changes

Add the SQLDelight plugin alongside existing plugins:
```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.sqldelight)  // ADD THIS
}
```

Add dependency source sets (after existing `commonMain` and `commonTest` blocks):
```kotlin
sourceSets {
    commonMain.dependencies {
        // existing deps...
        implementation(libs.sqldelight.runtime)
        implementation(libs.sqldelight.coroutines)
    }
    commonTest.dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.sqldelight.sqlite.driver)  // in-memory testing
    }
    androidMain.dependencies {
        implementation(libs.sqldelight.android.driver)
    }
    val iosMain by getting {
        dependencies {
            implementation(libs.sqldelight.native.driver)
        }
    }
}
```

Note: `iosMain` is a merged source set from `iosArm64Main` + `iosSimulatorArm64Main`. Use `val iosMain by getting { }` syntax rather than `iosMain.dependencies { }` shorthand.

Add the `sqldelight` configuration block (after existing `compose.resources { }` block):
```kotlin
sqldelight {
    databases {
        create("TrackerDatabase") {
            packageName.set("com.sonja.tracker")
        }
    }
}
```

### Task 1: `gradle/libs.versions.toml` — add missing entry

Add `sqldelight-sqlite-driver` to the `[libraries]` section (alongside existing SQLDelight entries):
```toml
sqldelight-sqlite-driver = { module = "app.cash.sqldelight:sqlite-driver", version.ref = "sqldelight" }
```

The catalog alias used in Gradle will be `libs.sqldelight.sqlite.driver`.

### Task 2: Schema file — exact content

**File:** `shared/src/commonMain/sqldelight/com/sonja/tracker/TrackerDatabase.sq`

```sql
CREATE TABLE items (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    reminder_weekday_time TEXT,
    reminder_weekend_time TEXT,
    image_path TEXT,
    icon_id TEXT
);

CREATE TABLE log_entries (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    item_id INTEGER NOT NULL,
    date TEXT NOT NULL,
    state TEXT NOT NULL,
    logged_at TEXT NOT NULL,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);
```

**Column naming rules (CRITICAL):** All columns MUST be `snake_case`. SQLDelight auto-generates `camelCase` Kotlin properties at compile time (`reminderWeekdayTime`, `loggedAt`, etc.). Never write `camelCase` in `.sq` files.

**Data formats (for downstream stories):**
- `date TEXT`: YYYY-MM-DD string (e.g. `"2026-04-08"`) — stored as text, compared as string
- `state TEXT`: one of `"logged"`, `"late_logged"`, `"not_logged"`, `"no_data"` (matches `LogState` enum, Story 2+)
- `logged_at TEXT`: ISO-8601 datetime string

**No queries in this story.** Queries (`insertItem`, `getAllItems`, etc.) are added in Stories 2.1 and 3.1 when repositories are implemented. Story 1.2 only needs the schema to unblock compilation.

### Task 3: Android actual

**File:** `shared/src/androidMain/kotlin/com/sonja/tracker/data/db/DatabaseDriverFactory.android.kt`

```kotlin
package com.sonja.tracker.data.db

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.cash.sqldelight.db.SqlDriver
import com.sonja.tracker.TrackerDatabase

actual class DatabaseDriverFactory(private val context: Any? = null) {
    actual fun createDriver(): SqlDriver {
        val ctx = requireNotNull(context as? Context) {
            "Android DatabaseDriverFactory requires a non-null Context"
        }
        return AndroidSqliteDriver(
            schema = TrackerDatabase.Schema,
            context = ctx,
            name = "tracker.db"
        )
    }
}
```

`AndroidSqliteDriver` stores `tracker.db` in the default `databases/` folder. This location is covered by Android Auto Backup (API 23+) without any additional configuration — satisfies the "Auto Backup eligible" architecture requirement.

### Task 3: iOS actual

**File:** `shared/src/iosMain/kotlin/com/sonja/tracker/data/db/DatabaseDriverFactory.ios.kt`

```kotlin
package com.sonja.tracker.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.sonja.tracker.TrackerDatabase
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSUserDomainMask

actual class DatabaseDriverFactory(private val context: Any? = null) {
    actual fun createDriver(): SqlDriver {
        val supportDir = NSSearchPathForDirectoriesInDomains(
            NSApplicationSupportDirectory,
            NSUserDomainMask,
            true
        ).first() as String
        return NativeSqliteDriver(
            schema = TrackerDatabase.Schema,
            name = "$supportDir/tracker.db"
        )
    }
}
```

`Application Support` is iCloud-backup-eligible by default on iOS (unlike `tmp/` or `Caches/`).

### In-memory testing (commonTest setup)

The `app.cash.sqldelight:sqlite-driver` provides `JdbcSqliteDriver` for JVM-based (Android unit) tests. Tests in `commonTest` compile and run on the JVM via `testDebugUnitTest` — this is sufficient for repository unit testing.

Usage in future test files (Stories 2.1+):
```kotlin
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
TrackerDatabase.Schema.create(driver)
val database = TrackerDatabase(driver)
```

Note: `sqlite-driver` is JVM-only. iOS native tests cannot use `JdbcSqliteDriver`. Repository tests that need a DB should run in `commonTest` (JVM) or use `NativeSqliteDriver` with `":memory:"` in `iosTest` if iOS-specific tests are needed (deferred to when repositories are implemented).

### Migration strategy (AC: 4)

This is schema version 1 — no migration files needed now. When any future story requires a schema change:

1. Increment schema version (handled automatically by adding a `.sqm` file)
2. Create `shared/src/commonMain/sqldelight/com/sonja/tracker/migrations/1.sqm` containing the `ALTER TABLE` or `CREATE TABLE` DDL
3. SQLDelight runs it automatically on next app open

Migration failure must throw `IllegalStateException` — this is the SQLDelight default behavior. Do NOT catch `SQLiteException` from migration callbacks silently. Architecture rule: hard-fail on migration failure, never silent data loss.

Do NOT perform any dummy migration in this story. Schema is not yet stable and changing before Story 2+ would require extra `.sqm` files unnecessarily.

### File locations summary

```
shared/
  build.gradle.kts                                         ← modified (plugin + sqldelight block + deps)
  src/
    commonMain/
      kotlin/com/sonja/tracker/
        data/
          db/
            DatabaseDriverFactory.kt                       ← NEW (expect)
      sqldelight/com/sonja/tracker/
        TrackerDatabase.sq                                 ← NEW
    androidMain/
      kotlin/com/sonja/tracker/
        data/
          db/
            DatabaseDriverFactory.android.kt               ← NEW (actual)
    iosMain/
      kotlin/com/sonja/tracker/
        data/
          db/
            DatabaseDriverFactory.ios.kt                   ← NEW (actual)
gradle/
  libs.versions.toml                                       ← modified (sqldelight-sqlite-driver entry)
```

### What this story does NOT deliver

- No Koin DI wiring — `DatabaseDriverFactory` is created and used directly (Story 1.3)
- No repository implementations (`ItemRepository`, `LogRepository`) — Story 2.1+
- No SQL queries beyond the schema DDL — queries added when repositories are implemented
- No `AppPreferences` — Story 1.3
- No navigation or UI changes

Do NOT create `SharedModule.kt`, `AndroidModule.kt`, or any Koin files. Do NOT create repository stubs. Do NOT create `domain/model/` data classes. These introduce premature abstraction that causes confusion.

### Don't break Story 1.1

The only files touched outside `data/db/` are `shared/build.gradle.kts` and `gradle/libs.versions.toml`. All existing source files (`App.kt`, `Platform.kt`, `Greeting.kt`, `MainActivity.kt`, etc.) remain untouched. The Compose UI must still compile and display the wizard greeting screen.

### References

- [Source: CONTRIBUTING.md#DatabaseDriverFactory] — nullable `Any?` context pattern, rationale
- [Source: CONTRIBUTING.md#SQLDelight Schema Notes] — schema stub required in Story 1.2
- [Source: architecture.md#Database] — `tracker.db` name, storage locations, migration strategy
- [Source: architecture.md#Complete Project Directory Structure] — exact file placement
- [Source: architecture.md#Naming Patterns] — `snake_case` SQLDelight columns
- [Source: epics.md#Story 1.2] — acceptance criteria

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

- `iosMain` source set not accessible via `val iosMain by getting` in Kotlin 2.3.20 at configuration time — fixed by targeting `iosArm64Main` and `iosSimulatorArm64Main` directly.
- Kotlin 2.x requires `actual constructor` keyword explicitly on primary constructors of `actual class` when corresponding `expect class` has a primary constructor.
- Default argument values (`= null`) must only appear on the `expect` declaration; the `actual` constructor must omit them.
- Added `-Xexpect-actual-classes` compiler flag to suppress Beta-feature warning for expect/actual classes.

### Completion Notes List

- Added `alias(libs.plugins.sqldelight)` to `shared/build.gradle.kts` plugins block.
- Added `sqldelight-runtime` + `sqldelight-coroutines` to `commonMain`; `sqldelight-android-driver` to `androidMain`; `sqldelight-native-driver` to both `iosArm64Main` and `iosSimulatorArm64Main`; `sqldelight-sqlite-driver` to `commonTest`.
- Added `sqldelight { databases { create("TrackerDatabase") { packageName.set("com.sonja.tracker") } } }` block — generates `TrackerDatabase` class in `com.sonja.tracker` package.
- Added `sqldelight-sqlite-driver` entry to `gradle/libs.versions.toml`.
- Created `TrackerDatabase.sq` with `items` and `log_entries` tables; all columns `snake_case`; FK with `ON DELETE CASCADE` on `log_entries.item_id`.
- Created `DatabaseDriverFactory` expect class with nullable `Any?` context and `= null` default (supports app, BroadcastReceiver, and NSE contexts).
- Android actual uses `AndroidSqliteDriver` storing `tracker.db` in default `databases/` folder (Auto Backup eligible).
- iOS actual reads `NSApplicationSupportDirectory` path and passes full path to `NativeSqliteDriver` (backup-eligible).
- `assembleDebug`, `generateCommonMainTrackerDatabaseInterface`, `testDebugUnitTest`, and `compileKotlinIosSimulatorArm64` all BUILD SUCCESSFUL.
- iOS Xcode build is a manual verification step for Sonja.

### File List

- `gradle/libs.versions.toml` — added `sqldelight-sqlite-driver` entry
- `shared/build.gradle.kts` — added sqldelight plugin, dependencies for all source sets, sqldelight config block, `-Xexpect-actual-classes` flag
- `shared/src/commonMain/sqldelight/com/sonja/tracker/TrackerDatabase.sq` — NEW: schema with items + log_entries tables
- `shared/src/commonMain/kotlin/com/sonja/tracker/data/db/DatabaseDriverFactory.kt` — NEW: expect class
- `shared/src/androidMain/kotlin/com/sonja/tracker/data/db/DatabaseDriverFactory.android.kt` — NEW: Android actual
- `shared/src/iosMain/kotlin/com/sonja/tracker/data/db/DatabaseDriverFactory.ios.kt` — NEW: iOS actual
