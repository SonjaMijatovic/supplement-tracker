---
stepsCompleted: [1, 2, 3, 4, 5, 6, 7, 8]
status: 'complete'
completedAt: '2026-04-07'
inputDocuments:
  - "_bmad-output/planning-artifacts/prd.md"
  - "_bmad-output/planning-artifacts/ux-design-specification.md"
  - "_bmad-output/planning-artifacts/product-brief-Projects.md"
  - "_bmad-output/planning-artifacts/implementation-readiness-report-2026-04-03.md"
workflowType: 'architecture'
project_name: 'Daily Supplement & Medication Tracker'
user_name: 'Sonja'
date: '2026-04-07'
---

# Architecture Decision Document

_This document builds collaboratively through step-by-step discovery. Sections are appended as we work through each architectural decision together._

## Starter Template Evaluation

### Primary Technology Domain

Kotlin Multiplatform Mobile — iOS 16+ and Android API 26+ — from a single shared codebase. No web, desktop, or server targets.

### Starter Options Considered

| Option | Assessment |
|---|---|
| JetBrains KMP Wizard (kmp.jetbrains.com) | Official, minimal, current versions, web-based |
| Kotlin/KMP-App-Template (GitHub) | Official Kotlin org template; equivalent baseline |
| KMPShip | Production-ready but includes Firebase Auth and other unneeded dependencies |
| openMF/kmp-project-template | Community multi-module generator; more surface area than needed |

### Selected Starter: JetBrains KMP Wizard

**Rationale:** The official wizard generates a clean, minimal KMP scaffold with no unnecessary dependencies. Compose Multiplatform 1.10.0 has stable iOS support.

**Initialization:** Web-based at https://kmp.jetbrains.com/
- Select: Android + iOS targets; Compose Multiplatform UI; no server/desktop/web
- Download generated project zip

**Module Naming Adaptation:**
The wizard generates `composeApp` as the shared module name. Rename to `shared` immediately at project creation — before writing any code — to match the PRD's module naming (`:shared`, `:androidApp`, `:iosApp`). Module names embed deeply into import paths, Gradle task names, and the Xcode framework name.

**Architectural Decisions Provided by Starter:**

**Language & Runtime:**
- Kotlin 2.3.20 (stable, March 2026)
- Kotlin Multiplatform with commonMain / androidMain / iosMain source sets

**UI Framework:**
- Compose Multiplatform 1.10.0 — iOS stable; shared UI composables in commonMain

**Build Tooling:**
- Gradle with Kotlin DSL (build.gradle.kts)
- Android Gradle Plugin 9+; Gradle 9.3.0+

**Project Structure:**
- `:shared` — commonMain (business logic, SQLDelight schema, notification interface, scheduler)
- `:androidApp` — Android entry point, platform notification impl (BroadcastReceiver), SQLDelight Android driver
- `:iosApp` — iOS entry point (Xcode project), platform notification impl (NSE), SQLDelight iOS driver

**iOS Framework Integration:**
Direct XCFramework embedding via `embedAndSignAppleFrameworkForXcode` Build Phase script — not SPM. The Build Phase compiles `:shared` into an XCFramework, signs it, and bundles it into the iOS app on every build.

**Module Structure Decision:**
One `:shared` module with internal packages (`data/`, `domain/`, `ui/`). Do not split into `:shared-domain` / `:shared-data` / `:shared-presentation` — premature for a 3-screen single-developer app.

**Note:** Project initialization using the wizard is the first implementation story. See also `CONTRIBUTING.md` for iOS build gotchas and the Kotlin upgrade procedure.

---

## Core Architectural Decisions

### Decision Priority Analysis

**Critical Decisions (Block Implementation):**
- SQLDelight 2.2.1 as local persistence layer
- ViewModel + StateFlow as state management pattern
- Koin 3.5 LTS as dependency injection framework
- Navigation 3 (1.0.1) for screen navigation
- Icon storage strategy (String ID → `ImageVector` mapping)

**Important Decisions (Shape Architecture):**
- App-private file storage for item images (expect/actual path resolution)
- In-memory SQLDelight driver for `commonTest`
- `tracker.db` as database file name

**Deferred Decisions (Post-MVP):**
- CI/CD pipeline — deferred; no pipeline for v1; GitHub Actions (lint + build) appropriate at publication time
- Multi-profile support — explicitly out of scope for v1

---

### Data Architecture

**SQLDelight version:** 2.2.1 (`app.cash.sqldelight` coordinates)

**State flow from DB to UI:**
ViewModel + StateFlow pattern. SQLDelight generates reactive `Flow`-based queries; ViewModels collect these and expose `StateFlow` to composables. DB queries run off the main thread naturally. `lifecycle-viewmodel` library supports CMP — one ViewModel pattern across both platforms.

**No separate UI state store.** All logged/pending state derives from SQLDelight queries only. Composables observe `StateFlow` from ViewModels; no manual state synchronisation.

**Database file name:** `tracker.db`

**Storage location:**
- iOS: `Application Support/tracker.db` (backup-eligible)
- Android: Auto Backup eligible via `context.filesDir`

**Migration strategy:** Explicit `.sqm` migration scripts per schema version. Migration failure = hard crash with clear error message, never silent data loss. Dummy migration performed during development before calling schema stable.

---

### Authentication & Security

N/A — fully offline, no account, no cloud, no auth. All data on-device. No third-party SDKs. No telemetry.

---

### API & Communication

N/A — fully offline. No network dependency at runtime.

---

### UI Architecture

**Navigation:** Navigation 3 (version 1.0.1, stable February 2026). Bundled with CMP 1.10.0. Supports the 3-tab `NavigationBar` structure (Today / History / Items) and type-safe deep-link navigation (YesterdayBanner → History screen at yesterday's date via navigation argument).

**State management:** ViewModel + StateFlow. One ViewModel per screen. `koin-compose-viewmodel` provides `koinViewModel()` in Compose composables — works across iOS and Android in CMP.

**Dependency Injection:** Koin 3.5 LTS.

- Shared module (`:shared/commonMain`): `DatabaseDriverFactory`, SQLDelight DB instance, `ItemRepository`, `NotificationScheduler`
- Platform modules (`:androidApp`, `:iosApp`): platform-specific `DatabaseDriverFactory` binding, ViewModel declarations
- `startKoin {}` called from Android `Application.onCreate()` and iOS `@main` entry point via a shared `initKoin()` function in `:shared`
- `single { }` for DB, repository, scheduler (app lifetime); `viewModel { }` for screen ViewModels (lifecycle-scoped)

---

### Image & Icon Storage

**Item images (camera / gallery):**
Stored in app-private file storage — not accessible to the system media gallery (NFR7). Expect/actual for platform path resolution:
- iOS: `Application Support/<app>/images/`
- Android: `context.filesDir/images/`

Image file path stored as nullable `TEXT` in SQLDelight items table.

**Icon picker:**
~20–24 icons sourced from **Material Icons Extended** (built into Compose, no additional assets, no licensing concerns). 3–4 supplement-specific icons not covered by Material Icons Extended sourced from **Phosphor Icons** or **Lucide** (MIT-licensed), converted to Compose vector drawables, stored in `commonMain/composeResources/drawable/`.

Selected icon stored as a nullable `TEXT` icon ID in the items table (e.g. `"medication"`, `"spa"`, `"water_drop"`). Rendered at display time by mapping ID → `ImageVector`. Null icon ID + null image path = default placeholder.

---

### Testing Strategy

| Layer | Approach |
|---|---|
| SQLDelight queries | In-memory SQLDelight driver in `commonTest` — no real DB file required |
| Business logic (scheduler, suppression logic) | `commonTest` unit tests — pure Kotlin, no platform dependency |
| Notification expect/actual implementations | Platform tests in `androidTest` / `iosTest` |
| UI & screen flows | Manual testing on Sonja's own devices (iPhone SE, iPhone 15, one Android) |
| Notification force-quit path | Manual test — NSE (iOS) and BroadcastReceiver (Android) with app fully force-quit |

No formal E2E test framework for v1.

---

### Infrastructure & Deployment

No CI/CD pipeline for v1. Solo developer, two-device daily use target. At App Store / Play Store publication (stretch goal), introduce GitHub Actions: lint + Gradle build check on PR. No deployment infra — local builds only.

---

### Decision Impact Analysis

**Implementation sequence (dependency order):**
1. Project init + module rename + `DatabaseDriverFactory` + SQLDelight schema stub — unblocks everything
2. Koin module setup — required before any ViewModel can be wired
3. Navigation 3 scaffold (3 tabs, empty screens) — required before screen-level stories
4. Item management (CRUD + icon picker + image storage)
5. Daily logging + main screen state
6. Notification scheduling (highest risk — build early)
7. History screen
8. Permission edge states

**Cross-component dependencies:**
- `DatabaseDriverFactory` → required by SQLDelight DB → required by all repositories → required by all ViewModels
- `NotificationScheduler` → depends on `ItemRepository` (for suppression logic) → depends on platform notification interface
- `YesterdayBanner` visibility → derived from SQLDelight query (unlogged items yesterday) → drives Navigation 3 deep-link argument

---

## Implementation Patterns & Consistency Rules

### Naming Patterns

**SQLDelight columns:** `snake_case` — SQLDelight auto-generates `camelCase` Kotlin properties from these at compile time. Never write `camelCase` column names in `.sq` files.

```sql
-- Correct
CREATE TABLE items (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    reminder_weekday_time TEXT,
    reminder_weekend_time TEXT
);
-- Kotlin: item.reminderWeekdayTime ✅

-- Wrong
CREATE TABLE items (
    reminderWeekdayTime TEXT   -- generates awkward Kotlin, breaks convention ❌
);
```

**Kotlin code:** Standard Kotlin conventions — `PascalCase` for classes/interfaces/objects, `camelCase` for functions/variables/properties, `SCREAMING_SNAKE_CASE` for constants.

**Package naming:** `com.sonja.tracker` root; sub-packages follow layer names (`data`, `domain`, `ui`, `notifications`).

**File naming:** One class per file; filename matches class name exactly.

---

### Structure Patterns

**Package organisation within `:shared` — by layer:**

```
shared/
  commonMain/
    kotlin/com/sonja/tracker/
      data/
        db/          # SQLDelight schema, generated DB class, DatabaseDriverFactory
        repository/  # ItemRepository, LogRepository
      domain/
        model/       # Item, LogEntry, TimeGroup data classes
        scheduler/   # NotificationScheduler (shared logic)
      ui/
        today/       # TodayViewModel, TodayUiState
        history/     # HistoryViewModel, HistoryUiState
        items/       # ItemsViewModel, ItemsUiState
      notifications/ # NotificationInterface (expect/actual declaration)
      di/            # Koin shared module definition
    sqldelight/com/sonja/tracker/
      TrackerDatabase.sq
  androidMain/
    kotlin/com/sonja/tracker/
      notifications/ # Android BroadcastReceiver, AlarmManager impl
      di/            # Android Koin module
  iosMain/
    kotlin/com/sonja/tracker/
      notifications/ # iOS UNUserNotificationCenter impl
      di/            # iOS Koin module
```

**Test files:** Co-located in the matching source set — `commonTest/`, `androidTest/`, `iosTest/`. Mirror the `main` package structure. Test class named `[ClassName]Test`.

**Koin modules:** One module file per source set — `sharedModule.kt` in `commonMain/di/`, `androidModule.kt` in `androidMain/di/`, `iosModule.kt` in `iosMain/di/`. Never scatter `module { }` declarations across feature files.

---

### State Management Patterns

**ViewModel UiState — sealed class pattern for all screens:**

```kotlin
// Every screen's state follows this pattern
sealed class TodayUiState {
    object Loading : TodayUiState()
    data class Success(val groups: List<TimeGroup>, val allLogged: Boolean) : TodayUiState()
    data class Error(val message: String) : TodayUiState()
}

class TodayViewModel(private val repo: ItemRepository) : ViewModel() {
    val uiState: StateFlow<TodayUiState> = repo
        .observeTodayGroups()
        .map { TodayUiState.Success(it, it.all { g -> g.allLogged }) }
        .catch { emit(TodayUiState.Error(it.message ?: "Unknown error")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayUiState.Loading)
}
```

**Rules:**
- Every ViewModel exposes exactly one `StateFlow<XxxUiState>`
- `Loading` is the initial state — never expose an uninitialised state
- Composables `when`-branch on the sealed class — no `if (isLoading)` flags
- State is immutable — never mutate a `Success` state's contents directly; emit a new instance

---

### Format Patterns

**Date storage:** ISO `YYYY-MM-DD` strings in `TEXT` columns.
- `"2026-04-07"` — human-readable, sorts correctly as text, maps directly to `kotlinx-datetime` `LocalDate`
- Never store epoch millis for date-only values

**Time storage:** `HH:mm` strings in `TEXT` columns.
- `"08:00"`, `"21:00"` — maps directly to time picker output; readable in DB inspection
- Never store minutes-since-midnight integers

**Log state values:** Stored as `TEXT` using exact string constants — `"logged"`, `"late_logged"`, `"not_logged"`, `"no_data"`. Define as a `sealed class` or `enum class LogState` in `domain/model/` and map to/from these strings at the DB boundary.

**Notification IDs:** `{itemId}_{date}_{timeSlot}` — e.g. `"3_2026-04-07_08:00"`. Consistent across platforms; used for both scheduling and cancellation.

---

### Error Handling Patterns

**Strategy:** Exceptions propagate from SQLDelight through repositories; caught at the ViewModel boundary and mapped to `UiState.Error`.

```kotlin
// Repository — let exceptions propagate naturally
fun observeItems(): Flow<List<Item>> = db.itemQueries.selectAll().asFlow().mapToList()

// ViewModel — catch at boundary, map to Error state
.catch { emit(TodayUiState.Error(it.message ?: "Something went wrong")) }
```

**Rules:**
- No `try/catch` blocks inside repositories — repositories are thin data-access layers
- All user-visible errors go through `UiState.Error` — never `Toast` or `Snackbar` for data errors
- `Snackbar` is reserved for system-level feedback only (lapse recovery confirmation per UX spec)
- Migration failure: hard crash with `IllegalStateException` — never swallow silently

---

### Process Patterns

**Notification rescheduling — always via `NotificationScheduler`:**

All four trigger points (app foreground, item add, item edit, item delete) call the same `NotificationScheduler.rescheduleAll()` or `rescheduleForSlot(timeSlot)` function. Never schedule or cancel notifications directly from a ViewModel or Repository — always route through the scheduler.

**`expect`/`actual` rule:** Platform `actual` implementations are thin — they execute OS-specific operations only. All logic (suppression checks, grouping, ID construction) lives in `:shared`. No business decisions in platform code.

**Composable state observation:**
```kotlin
// Correct — collect in composable body via ViewModel
val uiState by viewModel.uiState.collectAsState()

// Wrong — never create a new Flow inside a composable
// Creates a new subscription on every recomposition; wrong lifecycle; couples UI to data layer
val items by repo.observeItems().collectAsState(emptyList())  ❌
```

---

### All AI Agents MUST:

- Use `snake_case` for all SQLDelight column names
- Follow the sealed `UiState` pattern for every ViewModel
- Store dates as `YYYY-MM-DD TEXT` and times as `HH:mm TEXT`
- Route all notification operations through `NotificationScheduler`
- Keep platform `actual` implementations logic-free — all decisions in `commonMain`
- Define Koin dependencies in the module files only (`sharedModule.kt`, `androidModule.kt`, `iosModule.kt`)
- Never access SQLDelight directly from a Composable — always through ViewModel

---

## Project Structure & Boundaries

### Complete Project Directory Structure

```
tracker/
├── build.gradle.kts                          # root build — plugin declarations only
├── settings.gradle.kts                       # module includes: shared, androidApp, iosApp
├── gradle.properties
├── gradle/
│   └── libs.versions.toml                    # version catalog — all library versions here
├── CLAUDE.md
├── CONTRIBUTING.md
│
├── shared/
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/
│       │   ├── kotlin/com/sonja/tracker/
│       │   │   ├── data/
│       │   │   │   ├── db/
│       │   │   │   │   ├── DatabaseDriverFactory.kt      # expect class (nullable context)
│       │   │   │   │   └── AppImageStorage.kt            # expect — app-private image path
│       │   │   │   └── repository/
│       │   │   │       ├── ItemRepository.kt             # FR1–13 data access
│       │   │   │       └── LogRepository.kt              # FR14–18, FR35–39 data access
│       │   │   ├── domain/
│       │   │   │   ├── model/
│       │   │   │   │   ├── Item.kt                       # core domain model
│       │   │   │   │   ├── LogEntry.kt
│       │   │   │   │   ├── LogState.kt                   # enum: logged/late_logged/not_logged/no_data
│       │   │   │   │   └── TimeGroup.kt                  # items grouped by time slot (UI model)
│       │   │   │   └── scheduler/
│       │   │   │       └── NotificationScheduler.kt      # FR24–34 scheduling + suppression logic
│       │   │   ├── ui/
│       │   │   │   ├── today/
│       │   │   │   │   ├── TodayViewModel.kt             # FR19–23, FR14–16
│       │   │   │   │   ├── TodayUiState.kt               # sealed: Loading / Success / Error
│       │   │   │   │   └── TodayScreen.kt
│       │   │   │   ├── history/
│       │   │   │   │   ├── HistoryViewModel.kt           # FR35–39, FR17
│       │   │   │   │   ├── HistoryUiState.kt
│       │   │   │   │   └── HistoryScreen.kt
│       │   │   │   ├── items/
│       │   │   │   │   ├── ItemsViewModel.kt             # FR1–13
│       │   │   │   │   ├── ItemsUiState.kt
│       │   │   │   │   └── ItemsScreen.kt
│       │   │   │   ├── components/
│       │   │   │   │   ├── ItemRow.kt                    # pending / logged / disabled states
│       │   │   │   │   ├── TimeGroupSection.kt           # pending / overdue / done badge
│       │   │   │   │   ├── AllDoneHero.kt                # FR22 completion state
│       │   │   │   │   ├── YesterdayBanner.kt            # FR17 late-log discovery
│       │   │   │   │   ├── ItemEditSheet.kt              # FR1–4, FR7 add/edit bottom sheet
│       │   │   │   │   ├── IconPickerGrid.kt             # FR8–9 icon picker
│       │   │   │   │   ├── HistoryDayView.kt             # FR36 4-state per-day view
│       │   │   │   │   └── NotificationDeniedBanner.kt   # FR23 permission prompt
│       │   │   │   ├── navigation/
│       │   │   │   │   └── AppNavigation.kt              # Navigation 3 graph — 3 tabs + deep-link
│       │   │   │   └── theme/
│       │   │   │       ├── Theme.kt                      # MaterialTheme with Dusty Indigo tokens
│       │   │   │       ├── Color.kt                      # Dusty Indigo colour tokens
│       │   │   │       └── Type.kt                       # Material 3 type scale
│       │   │   ├── notifications/
│       │   │   │   └── NotificationInterface.kt          # expect interface — schedule/cancel
│       │   │   └── di/
│       │   │       └── SharedModule.kt                   # Koin: DB, repositories, scheduler
│       │   ├── composeResources/
│       │   │   └── drawable/                             # custom SVG icons (Phosphor/Lucide, MIT)
│       │   └── sqldelight/com/sonja/tracker/
│       │       └── TrackerDatabase.sq                    # schema + all queries (single file)
│       │
│       ├── commonTest/
│       │   └── kotlin/com/sonja/tracker/
│       │       ├── data/repository/
│       │       │   ├── ItemRepositoryTest.kt             # in-memory SQLDelight driver
│       │       │   └── LogRepositoryTest.kt
│       │       └── domain/scheduler/
│       │           └── NotificationSchedulerTest.kt      # suppression + grouping logic
│       │
│       ├── androidMain/
│       │   └── kotlin/com/sonja/tracker/
│       │       ├── data/db/
│       │       │   ├── DatabaseDriverFactory.android.kt  # actual — AndroidSqliteDriver
│       │       │   └── AppImageStorage.android.kt        # actual — context.filesDir/images/
│       │       ├── notifications/
│       │       │   ├── NotificationInterface.android.kt  # actual — AlarmManager scheduling
│       │       │   ├── NotificationReceiver.kt           # BroadcastReceiver — YES action (FR34)
│       │       │   └── NotificationHelper.kt             # channel setup, alarm construction
│       │       └── di/
│       │           └── AndroidModule.kt                  # Koin: androidContext, ViewModels
│       │
│       ├── androidTest/
│       │   └── kotlin/com/sonja/tracker/
│       │       └── notifications/
│       │           └── NotificationReceiverTest.kt
│       │
│       ├── iosMain/
│       │   └── kotlin/com/sonja/tracker/
│       │       ├── data/db/
│       │       │   ├── DatabaseDriverFactory.ios.kt      # actual — NativeSqliteDriver
│       │       │   └── AppImageStorage.ios.kt            # actual — Application Support/images/
│       │       ├── notifications/
│       │       │   └── NotificationInterface.ios.kt      # actual — UNUserNotificationCenter
│       │       └── di/
│       │           └── IosModule.kt                      # Koin: iOS bindings, initKoin()
│       │
│       └── iosTest/
│           └── kotlin/com/sonja/tracker/
│               └── notifications/
│                   └── NotificationInterfaceTest.kt
│
├── androidApp/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml                           # permissions: POST_NOTIFICATIONS, SCHEDULE_EXACT_ALARM
│       ├── kotlin/com/sonja/tracker/android/
│       │   ├── TrackerApplication.kt                     # startKoin { androidContext + modules }
│       │   └── MainActivity.kt                           # single Activity, Compose entry point
│       └── res/                                          # launcher icons, strings
│
└── iosApp/
    ├── iosApp.xcodeproj/                                 # Xcode project — direct XCFramework embed
    ├── iosApp/
    │   ├── iOSApp.swift                                  # @main — calls initKoin() on launch
    │   ├── ContentView.swift                             # root ComposeUIViewController
    │   ├── TimePicker.swift                              # UIDatePicker wheel (expect/actual bridge)
    │   ├── Info.plist
    │   └── PrivacyInfo.xcprivacy                         # iOS privacy manifest (store compliance)
    └── NotificationServiceExtension/                     # separate Xcode target — YES bulk-action
        ├── NotificationService.swift                     # calls shared framework to write log (FR34)
        └── Info.plist
```

### Architectural Boundaries

**Module boundaries:**

| Boundary | Rule |
|---|---|
| `:shared` → `:androidApp` | `:shared` exposes public API only — repositories, ViewModels, `initKoin()`. Android app never accesses SQLDelight directly. |
| `:shared` → `:iosApp` | Same — Swift calls Kotlin through the XCFramework public API only |
| `NotificationScheduler` | All notification scheduling/cancellation goes through this class. No direct OS notification calls from repositories or ViewModels. |
| `NotificationInterface` | `actual` implementations contain zero business logic. Suppression checks, grouping, ID construction all live in `NotificationScheduler` in `commonMain`. |
| `BroadcastReceiver` / `NSE` | Standalone process entry points. Init SQLDelight via `DatabaseDriverFactory` independently. Write log entry then exit. No Koin — too heavyweight for a background process. |

**Data flow:**

```
SQLDelight (.sq queries)
    → Repository (Flow<T>)
        → ViewModel (StateFlow<UiState>)
            → Composable (collectAsState)
                → UI
```

Reverse path (user action):
```
Composable tap
    → ViewModel function call
        → Repository suspend function
            → SQLDelight write
                → Flow emits new value
                    → StateFlow updates
                        → Composable recomposes
```

### Requirements to Structure Mapping

| FR Category | Primary Location |
|---|---|
| Item Management (FR1–13) | `ItemRepository`, `ItemsViewModel`, `ItemEditSheet`, `IconPickerGrid`, `AppImageStorage` |
| Daily Logging (FR14–18) | `LogRepository`, `TodayViewModel`, `ItemRow`, `YesterdayBanner`, `HistoryDayView` |
| Main Screen (FR19–23) | `TodayScreen`, `TimeGroupSection`, `AllDoneHero`, `NotificationDeniedBanner` |
| Notification Delivery (FR24–34) | `NotificationScheduler`, `NotificationInterface` (expect/actual), `NotificationReceiver`, `NotificationService.swift` |
| History (FR35–39) | `LogRepository`, `HistoryViewModel`, `HistoryScreen`, `HistoryDayView` |
| Data & Persistence (FR40–43) | `TrackerDatabase.sq`, `DatabaseDriverFactory`, `AppImageStorage` |

---

## Architecture Validation Results

### Coherence Validation ✅

**Decision Compatibility:** All library versions are KMP-compatible with no conflicts. Kotlin 2.3.20 + CMP 1.10.0 + SQLDelight 2.2.1 + Koin 3.5 LTS + Navigation 3 (1.0.1) + Coroutines 1.10.2 + kotlinx-datetime 0.7.1 form a coherent, well-supported stack.

**Pattern Consistency:** Sealed `UiState` + `StateFlow` + `koin-compose-viewmodel` + `collectAsState()` form a coherent end-to-end state pipeline. `expect`/`actual` applied consistently across four abstractions (DB driver, image storage, notifications, time picker). All notification operations route through `NotificationScheduler` — no direct OS calls from business logic.

**Structure Alignment:** Project structure directly reflects the by-layer package organisation. Each FR category maps to specific files. Module boundaries prevent business logic from leaking into platform code.

---

### Requirements Coverage Validation ✅

**Functional Requirements: 43/43 covered**

| FR Category | Coverage |
|---|---|
| Item Management (FR1–13) | `ItemRepository`, `ItemsViewModel`, `ItemEditSheet`, `IconPickerGrid`, `AppImageStorage` (expect/actual) |
| Daily Logging (FR14–18) | `LogRepository`, `TodayViewModel`, `ItemRow`, `YesterdayBanner`, `HistoryDayView` |
| Main Screen (FR19–23) | `TodayScreen`, `TimeGroupSection`, `AllDoneHero`, `NotificationDeniedBanner`, `AppPreferences` (banner dismissed state) |
| Notification Delivery (FR24–34) | `NotificationScheduler`, `NotificationInterface` (expect/actual), `NotificationReceiver`, `NotificationService.swift`, `AppPreferences` (lapse detection) |
| History (FR35–39) | `LogRepository`, `HistoryViewModel`, `HistoryScreen`, `HistoryDayView`, `AppPreferences` (install date) |
| Data & Persistence (FR40–43) | `TrackerDatabase.sq`, `DatabaseDriverFactory`, backup-eligible storage locations |

**Non-Functional Requirements: 16/16 covered**

| NFR Area | Architectural Support |
|---|---|
| Performance | Reactive SQLDelight `Flow`s; non-blocking notification scheduling; StateFlow only recomposes affected composables |
| Privacy | Fully local; no SDKs; app-private image storage; no telemetry |
| Reliability | Atomic SQLDelight writes; migration hard-fail (`IllegalStateException`); `DatabaseDriverFactory` independently initializable from BroadcastReceiver/NSE |
| Accessibility | Material 3 defaults (touch targets, Dynamic Type); semantic annotations mandated in patterns; non-colour state distinction enforced |

---

### Gaps Found & Resolved

**Gap 1 — App preferences storage ✅ Resolved**

`AppPreferences` expect/actual added to architecture. Stores three values: `installDate` (FR37 history boundary), `lastScheduledDate` (FR31 lapse detection), `notificationBannerDismissed` (FR23 one-time banner).

```kotlin
// commonMain/data/prefs/AppPreferences.kt
expect class AppPreferences {
    fun getInstallDate(): String?           // YYYY-MM-DD, null until first set
    fun setInstallDate(date: String)
    fun getLastScheduledDate(): String?     // YYYY-MM-DD
    fun setLastScheduledDate(date: String)
    fun isNotificationBannerDismissed(): Boolean
    fun setNotificationBannerDismissed()
}
```

- Android `actual`: `SharedPreferences` via `context.getSharedPreferences("tracker_prefs", MODE_PRIVATE)`
- iOS `actual`: `NSUserDefaults.standardUserDefaults`
- Added to Koin `SharedModule` as `single { AppPreferences(...) }`

**Project structure additions:**
```
data/prefs/
    AppPreferences.kt                     # expect (commonMain)
    AppPreferences.android.kt             # actual — SharedPreferences (androidMain)
    AppPreferences.ios.kt                 # actual — NSUserDefaults (iosMain)
```

---

**Gap 2 — kotlinx-datetime ✅ Resolved**

Added as explicit dependency: **kotlinx-datetime 0.7.1**. Used throughout `commonMain` for `LocalDate` (date storage/comparison), `LocalTime` (time slot handling), and `DayOfWeek` (weekday/weekend detection). All date/time operations in `commonMain` use `kotlinx-datetime` types — no platform date APIs in shared code.

---

**Gap 3 — `TimePicker` expect/actual Kotlin location ✅ Documented**

```kotlin
// commonMain/ui/components/TimePicker.kt
expect fun TimePicker(
    selectedTime: LocalTime,
    onTimeSelected: (LocalTime) -> void
)
```

- `androidMain/ui/components/TimePicker.android.kt` — Material 3 clock dial
- `iosMain/ui/components/TimePicker.ios.kt` — bridges to `TimePicker.swift` (UIDatePicker wheel)

Used inside `ItemEditSheet` for weekday and weekend time selection.

---

### Architecture Completeness Checklist

- [x] Requirements analysis — 43 FRs and 16 NFRs mapped to architectural components
- [x] Tech stack — all versions verified and documented
- [x] Starter template — JetBrains KMP Wizard, initialization approach defined
- [x] Module structure — `:shared` / `:androidApp` / `:iosApp` with internal package layout
- [x] Data layer — SQLDelight 2.2.1, schema approach, migration strategy, date/time formats
- [x] State management — ViewModel + sealed UiState + StateFlow
- [x] Navigation — Navigation 3 (1.0.1), 3-tab structure, deep-link pattern
- [x] Dependency injection — Koin 3.5 LTS, module structure defined
- [x] Notifications — full architecture including NSE, BroadcastReceiver, suppression, lapse detection
- [x] Image/icon storage — app-private expect/actual, icon ID mapping
- [x] Preferences — AppPreferences expect/actual (SharedPreferences / NSUserDefaults)
- [x] Testing strategy — in-memory SQLDelight, commonTest, manual device testing
- [x] Implementation patterns — naming, structure, state, error handling, process rules
- [x] Project structure — complete file tree with FR mapping
- [x] iOS build notes — documented in `CONTRIBUTING.md`

### Architecture Readiness Assessment

**Overall Status: READY FOR IMPLEMENTATION**

**Confidence Level: High**

**Key Strengths:**
- Tight scope with no architectural ambiguity — every FR maps to a specific file
- `expect`/`actual` boundaries are clean and logic-free in platform code
- Single source of truth: SQLDelight for all domain data, `AppPreferences` for app state
- Implementation sequence defined — story 1 unblocks all downstream work

**Areas for future enhancement (post-MVP):**
- CI/CD pipeline (GitHub Actions) at publication time
- Multi-profile support would require `user_id` column on items/logs tables — additive migration, no structural change
- FCM-backed notification delivery in v2 requires only a new `actual` for `NotificationInterface` — shared logic unchanged by design

### Implementation Handoff

**AI Agent Guidelines:**
- Follow all architectural decisions exactly as documented
- Use sealed `UiState` pattern for every ViewModel — no exceptions
- Route all notification operations through `NotificationScheduler`
- Keep platform `actual` implementations logic-free
- Check `CONTRIBUTING.md` for iOS build gotchas before first build

**First Implementation Priority:**
1. Initialize project from JetBrains KMP Wizard (kmp.jetbrains.com)
2. Rename `composeApp` → `shared`
3. Define `DatabaseDriverFactory` (expect/actual)
4. Add SQLDelight schema stub (`items` table minimum)
5. Set up Koin modules
6. Verify iOS first build (Java PATH check)

---

## Project Context Analysis

### Requirements Overview

**Functional Requirements:**

43 FRs across 6 capability areas:

- **Item Management (FR1–13):** Add/edit/delete items with name, weekday/weekend reminder times, and image/icon. Permission-conditional image placeholder visible throughout the entire app (hidden entirely if both camera and gallery permanently denied).
- **Daily Logging (FR14–18):** Single-tap log from main screen, YES bulk-action from notification (including force-quit state), late logging up to 1 day back. All log entries are "taken" regardless of time logged.
- **Main Screen & Today View (FR19–23):** Items grouped by time slot; empty state, all-done state, notification-denied banner. All today's items visible (pending + done).
- **Notification Delivery (FR24–34):** Grouped notifications per shared time slot; YES bulk-action writes to DB without opening app (requires NSE on iOS, BroadcastReceiver on Android, each capable of independent SQLDelight initialization); suppression of already-logged groups; 7-day lookahead; lapse recovery; stale-payload rescheduling on every item change.
- **History & Log Review (FR35–39):** Rolling 7-day history; 4 log states (logged, late-logged, not-logged, no-data); read-only; no-data for days before item creation or app install.
- **Data & Persistence (FR40–43):** Fully local via SQLDelight; backup-eligible storage location; explicit migration scripts with hard-fail on failure; no network dependency ever.

**Non-Functional Requirements:**

- **Performance:** App launch < 2s; tap-to-confirmation < 500ms; notification rescheduling non-blocking (< 1s background); history load < 1s.
- **Privacy:** All data on-device; no analytics, telemetry, or third-party SDKs; images in app-private storage.
- **Reliability:** Atomic DB writes; no crashes surfaced as unhandled exceptions; data persists across force-quit, OS update, device restart; notification delivery ≥ 95%.
- **Accessibility:** Min 48dp touch targets; Dynamic Type support; non-colour state distinction; VoiceOver/TalkBack navigable.

**Scale & Complexity:**

- Primary domain: Mobile — iOS 16+ and Android API 26+, single KMP codebase
- Complexity level: Low — no backend, no auth, no external integrations, no cloud
- Estimated architectural components: 3 KMP modules (:shared, :androidApp, :iosApp), ~8 custom UI composables, 1 SQLDelight schema, 2 platform notification implementations (NSE + BroadcastReceiver), 2 expect/actual interfaces (notifications, time picker)

### Technical Constraints & Dependencies

- **KMP / Compose Multiplatform:** Single codebase for all shared logic and UI; Kotlin 1.9+ / stable KMP release
- **SQLDelight:** Schema in `:shared`; single source of truth for all data including log states; must be initializable from background processes (NSE, BroadcastReceiver) independently of the main app lifecycle
- **Notification interface design constraint:** No local-scheduling concerns (alarm IDs, OS timing APIs) may leak into `:shared` — interface expresses only *what* to notify and *when*. Designed for FCM replaceability in v2 without rewriting shared logic.
- **Notification identifier scheme:** `{itemId}_{date}_{timeSlot}` — consistent across platforms for targeted cancellation and suppression
- **Device local time only:** No timezone conversion in v1; weekday/weekend detection based on device calendar
- **iOS minimum API:** 16+ (UserNotifications framework, modern Compose Multiplatform compatibility)
- **Android minimum API:** 26 (notification channels required); exact alarms require API 31+ `SCHEDULE_EXACT_ALARM` permission with fallback to inexact
- **Store compliance:** Publication-ready structure required (bundle IDs, iOS privacy manifest, permissions declarations) even if not published in v1
- **Image storage:** App-private storage (not system media gallery); iOS `Application Support/`, Android Auto Backup eligible

### Cross-Cutting Concerns Identified

1. **Notification scheduling lifecycle** — Suppression logic and scheduling decisions live in `:shared`; OS alarm/trigger APIs live in platform modules; background-process DB writes (YES action from force-quit) require independent initialization in NSE (iOS) and BroadcastReceiver (Android). Touches all three modules.

2. **SQLDelight initialization** — Must support two initialization contexts: (a) normal app lifecycle, and (b) background process (NSE / BroadcastReceiver) without the app running. Database driver creation must be encapsulatable outside the main Application/App class.

3. **Permission state propagation** — Notification permission affects scheduling behaviour and main screen banner (FR23); camera/gallery permissions affect image placeholder visibility throughout the entire app (FR10). Permission state must be observable and accessible across multiple screens and the notification scheduling path.

4. **Date/time handling** — Weekday/weekend detection at scheduling time and at render time (overdue group indicator in UX); device local time only; consistent "today" and "yesterday" definitions across UI, scheduler, and log writer.

5. **UI state derivation from DB** — All logged/pending state derives from SQLDelight queries, not a separate UI state store. Reactivity (Flow/StateFlow) from SQLDelight queries drives composable recomposition; no manual state synchronization layer needed.

6. **Notification rescheduling triggers** — Four distinct trigger points: app foreground, item add, item edit, item delete. Rescheduling logic must be reachable from both UI-triggered paths (item management) and lifecycle-triggered paths (app foreground), without duplication.
