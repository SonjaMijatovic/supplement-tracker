---
stepsCompleted: ["step-01-validate-prerequisites", "step-02-design-epics", "step-03-create-stories", "step-04-final-validation"]
workflowComplete: true
completedDate: 2026-04-07
inputDocuments:
  - "_bmad-output/planning-artifacts/prd.md"
  - "_bmad-output/planning-artifacts/architecture.md"
  - "_bmad-output/planning-artifacts/ux-design-specification.md"
---

# Daily Supplement & Medication Tracker - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for the Daily Supplement & Medication Tracker, decomposing the requirements from the PRD, UX Design Specification, and Architecture into implementable stories.

## Requirements Inventory

### Functional Requirements

FR1: User can add a supplement or medication with a name and weekday reminder time via the native OS time picker
FR2: User can set a separate weekend reminder time per item via the native OS time picker
FR3: User can edit an existing item's name in place
FR4: User can edit an existing item's weekday and weekend reminder times via the native OS time picker
FR5: User can delete an existing item
FR6: User can view all configured items in a list
FR7: User can attach an image to an item by selecting from the device gallery or taking a photo
FR8: An image placeholder is displayed next to the item name throughout the app (main screen, history, item list) wherever the item name appears, except in notifications
FR9: If the user has attached an image, it is shown in the placeholder; otherwise a default icon is displayed
FR10: If the user has permanently declined both camera and photo library permissions, no image placeholder is shown anywhere in the app
FR11: System requests camera permission when user first attempts to take a photo for an item image
FR12: System requests photo library / media access permission when user first attempts to select an image from the gallery
FR13: If one permission is denied, the corresponding option is unavailable; the other remains available if its permission is granted
FR14: User can log an item as taken with a single tap from the main screen
FR15: User can log all items in a notification group as taken via the YES bulk-action without opening the app
FR16: User can log items by opening the app directly, with the same interaction as a notification-triggered open
FR17: User can log items from the previous day (up to 1 day back)
FR18: System records any log entry as "taken" for the day regardless of the time it was logged
FR19: User sees an empty state prompting item creation when no items are configured
FR20: User can see all configured items for today (both pending and already logged) on the main screen
FR21: User can visually distinguish pending items from already-logged items on the main screen
FR22: User sees an "all done" confirmation state with message and icon when all today's items are logged
FR23: User sees an in-app prompt to enable notifications if notification permission has been denied
FR24: System sends a single grouped notification for all items sharing the same reminder time slot
FR25: System sends a separate notification for items with a unique reminder time
FR26: Notification includes a YES bulk-action that logs all items in the group as taken
FR27: System suppresses a scheduled notification if all items in the group are already logged for the day
FR28: System reschedules affected time-slot notifications immediately when any item is added, edited, or deleted
FR29: System reschedules all pending notifications when the app is foregrounded
FR30: System maintains a 7-day notification lookahead window
FR31: System detects scheduling lapse and displays a subtle "Reminders rescheduled" confirmation on next app open
FR32: System requests notification permission on first launch
FR33: App remains fully functional for manual logging when notification permission is denied
FR34: System writes a log entry when the YES notification action is triggered, even when the app is fully force-quit
FR35: User can view a rolling 7-day history for each item
FR36: Each day in the history displays one of four states per item: logged, late-logged, not-logged, no-data
FR37: History displays only days from app installation onwards; days before install are not shown; history shows fewer than 7 days if app was installed less than 7 days ago
FR38: History displays no-data state for days between app installation and item creation date
FR39: History is read-only; no editing or deletion of past log entries
FR40: App stores all data locally with no network requirement at any time
FR41: App data persists across app restarts, OS updates, and normal device lifecycle
FR42: App data stored in backup-eligible location on both iOS (Application Support/) and Android (Auto Backup), supporting OS-level device-to-device migration
FR43: App applies database schema migrations on update without data loss; migration failure surfaces a clear error

### NonFunctional Requirements

NFR1: App launch to interactive state: < 2 seconds on mid-range devices (Android API 26 equivalent, iPhone XR equivalent)
NFR2: Single-tap log action to visual confirmation: < 500ms
NFR3: Notification rescheduling on app foreground: non-blocking, completes in background within 1 second
NFR4: History view load (7-day data): < 1 second
NFR5: All user data stored exclusively on-device; no data transmitted to any external server
NFR6: No analytics, crash reporting, or telemetry that transmits personal or usage data
NFR7: Item images stored in app-private storage, not accessible to other apps or the system media gallery
NFR8: No advertising SDKs or third-party tracking libraries
NFR9: App must not crash during normal use; errors logged locally, never surfaced as unhandled exceptions
NFR10: All database writes atomic — no partial writes on unexpected termination
NFR11: Data persists correctly across: app force-quit, app update, OS update, device restart
NFR12: Notification delivery rate ≥ 95% under normal device conditions (charged device, network not required)
NFR13: All interactive elements meet minimum touch target size: 44×44pt (iOS) / 48×48dp (Android)
NFR14: App supports Dynamic Type / system font scaling on both platforms without layout breakage
NFR15: No functionality or state conveyed by colour alone; pending vs. logged states use shape or label in addition to colour
NFR16: Core flows navigable with VoiceOver (iOS) and TalkBack (Android)

### Additional Requirements

- **Starter template:** JetBrains KMP Wizard (kmp.jetbrains.com) — select Android + iOS + Compose Multiplatform; rename generated `composeApp` module to `shared` immediately before writing any code
- **Module structure:** `:shared` (commonMain business logic, SQLDelight schema, notification interface, scheduler), `:androidApp`, `:iosApp`
- **Library versions (locked):** Kotlin 2.3.20, Compose Multiplatform 1.10.0, SQLDelight 2.2.1, Koin 3.5 LTS, Navigation 3 (1.0.1), kotlinx-datetime 0.7.1, Coroutines 1.10.2 — all in `libs.versions.toml`
- **State management:** Sealed `UiState` (Loading / Success / Error) + `StateFlow` pattern for every ViewModel; one ViewModel per screen
- **DI:** Koin — declarations only in `sharedModule.kt`, `androidModule.kt`, `iosModule.kt`; never scatter across feature files
- **Database file:** `tracker.db`; iOS: `Application Support/tracker.db`; Android: `context.filesDir` (Auto Backup eligible)
- **Migration strategy:** Explicit `.sqm` migration scripts per schema version; failure = hard crash with `IllegalStateException`, never silent
- **AppPreferences (expect/actual):** Stores `installDate` (FR37), `lastScheduledDate` (FR31 lapse detection), `notificationBannerDismissed` (FR23); added to project structure
- **Notification IDs:** `{itemId}_{date}_{timeSlot}` format for consistent scheduling and cancellation across platforms
- **BroadcastReceiver (Android) / NSE (iOS):** Initialize SQLDelight independently without Koin; write log entry then exit
- **Testing:** In-memory SQLDelight driver in `commonTest` for all repository and scheduler unit tests; manual UI/notification testing on Sonja's devices
- **No CI/CD for v1**; GitHub Actions (lint + build) deferred to App Store publication milestone
- **Image storage:** Expect/actual `AppImageStorage` — iOS: `Application Support/<app>/images/`; Android: `context.filesDir/images/`; never in system media gallery
- **Notification scheduling:** All operations (schedule, cancel, reschedule) route through `NotificationScheduler` only — never from ViewModels or Repositories directly
- **Platform actual rule:** `actual` implementations contain zero business logic — all suppression, grouping, and ID construction live in `commonMain`

### UX Design Requirements

UX-DR1: Implement the Dusty Indigo custom Material 3 colour token set — warm near-white/deep charcoal surfaces, muted indigo primary accent (~15–20% opacity tint for logged row state), amber for overdue/warning, cool-warm grey for not-logged history state — applied via `Theme.kt` and `Color.kt` in commonMain; light and dark mode both supported
UX-DR2: Implement `ItemRow` composable with 3 states (pending, logged with tonal fill + check, disabled/non-tappable); full-row tap target minimum 56dp height; 40dp rounded-square thumbnail/icon; 28dp check circle; content description "[Item name], [pending/logged/already logged]"; Role.Checkbox semantic
UX-DR3: Implement `TimeGroupSection` composable — groups `ItemRow` items under their reminder time slot label; 3 badge states: pending ("X pending"), overdue ("X overdue" with subtle amber-tinted header, derived from current time > slot time at render), done ("all done"); no extra storage needed for overdue state
UX-DR4: Implement `AllDoneHero` composable — animated composable swap from grouped list when `allLogged == true`; 52dp large icon; "All done today" primary text + "See you tomorrow" secondary; compact checked item list below; light fade/scale-in transition
UX-DR5: Implement `YesterdayBanner` composable — visible only when yesterday has unlogged items; amber-tinted icon + message + "Log →" action text; tap deep-links to yesterday's date in history (not history root) via Navigation 3 argument; auto-dismisses on return to main screen once all yesterday's items resolved; Role.Button + destination context announcement
UX-DR6: Implement `ItemEditSheet` as `ModalBottomSheet` — name field (single-line, auto-focused, required); icon picker auto-opens after name entry with prominent Skip; weekday time row (native OS picker via expect/actual); "Different time on weekends?" toggle (collapsed by default, expands to weekend time row); photo option row (camera / gallery); Save button (always enabled); Delete button (amber TextButton, in edit mode only)
UX-DR7: Implement `IconPickerGrid` — ~24 icons from Material Icons Extended + 3–4 custom Phosphor/Lucide MIT SVGs stored in `commonMain/composeResources/drawable/`; scrollable grid of rounded-square tiles; selected tile indigo tint border; prominent Skip button; selected icon ID stored as nullable TEXT in items table alongside image path
UX-DR8: Implement `HistoryDayView` composable — date header + 4-state per-item indicators (logged: indigo tint, late-logged: lighter indigo tint + "late" label, not-logged: warm grey, no-data: empty/no indicator); late-log tap affordance only for yesterday's items; icon shape + label distinguish states (not colour alone)
UX-DR9: Implement `NotificationDeniedBanner` — bell icon + short message + "Enable" TextButton; shown max once; dismissed state persisted in `AppPreferences.setNotificationBannerDismissed()`; never re-shown after dismissal; tap navigates to system notification settings
UX-DR10: Implement 3-tab `NavigationBar` using Navigation 3 — Today / History / Items tabs; no badge on any tab; app always launches to Today tab; `YesterdayBanner` deep-link navigates to yesterday's date as navigation argument (not tab root)
UX-DR11: Implement grouped-by-time Today screen — `LazyColumn` with time slot group headers (`TimeGroupSection`); composable swap to `AllDoneHero` when `allLogged == true`; `YesterdayBanner` displayed above grouped list when relevant
UX-DR12: Implement item image/icon strategy with 3 paths — icon picker (always available, no permissions), camera (permission-gated: requests on first use), gallery (permission-gated: requests on first use); if both permanently denied, hide image placeholder throughout app; default placeholder shown when neither image nor icon is set
UX-DR13: Implement all empty states — main screen (no items): warm illustration + "Add your first item" + FilledButton; history screen (no data): "No history yet — come back tomorrow" (neutral); items screen (no items): same prompt as main screen
UX-DR14: Apply accessibility requirements across all composables — `Modifier.minimumInteractiveComponentSize()` on all tappable elements; `sp` for all text sizes; `contentDescription` on all image/icon elements; semantic `Role` annotations; all states distinguished by shape/fill + label, never colour alone; dark mode via Material 3 dynamic theming

### FR Coverage Map

FR1: Epic 2 — Add item with name and weekday reminder time
FR2: Epic 2 — Set separate weekend reminder time per item
FR3: Epic 2 — Edit item name in place
FR4: Epic 2 — Edit weekday and weekend reminder times
FR5: Epic 2 — Delete an existing item
FR6: Epic 2 — View all configured items in a list
FR7: Epic 2 — Attach image via gallery or camera
FR8: Epic 2 — Image placeholder displayed throughout app (except notifications)
FR9: Epic 2 — Show attached image or default icon in placeholder
FR10: Epic 2 — Hide image placeholder when both camera and gallery permanently denied
FR11: Epic 2 — Request camera permission on first photo attempt
FR12: Epic 2 — Request photo library permission on first gallery attempt
FR13: Epic 2 — Unavailable permission hides that option; other remains available
FR14: Epic 3 — Single-tap logging from main screen
FR15: Epic 4 — YES notification bulk-action logs entire group
FR16: Epic 3 — Manual in-app logging with same UX as notification-triggered open
FR17: Epic 5 — Log items from previous day (up to 1 day back)
FR18: Epic 3 — Log entry recorded as "taken" regardless of time logged
FR19: Epic 3 — Empty state when no items configured
FR20: Epic 3 — All today's items (pending + logged) on main screen
FR21: Epic 3 — Visual distinction between pending and logged items
FR22: Epic 3 — All-done confirmation state with message and icon
FR23: Epic 4 — In-app prompt to enable notifications when permission denied
FR24: Epic 4 — Grouped notification per shared reminder time slot
FR25: Epic 4 — Separate notification for unique reminder times
FR26: Epic 4 — YES action logs all items in group without opening app
FR27: Epic 4 — Suppress notification when all items in group already logged
FR28: Epic 4 — Reschedule affected slot notifications on item add/edit/delete
FR29: Epic 4 — Reschedule all notifications on app foreground
FR30: Epic 4 — Maintain 7-day notification lookahead window
FR31: Epic 4 — Detect scheduling lapse; show "Reminders rescheduled" on next open
FR32: Epic 4 — Request notification permission on first launch
FR33: Epic 4 — App fully functional for manual logging without notification permission
FR34: Epic 4 — Write log entry on YES action even when app is fully force-quit
FR35: Epic 5 — Rolling 7-day history per item
FR36: Epic 5 — Four states per day: logged, late-logged, not-logged, no-data
FR37: Epic 5 — History starts from app installation date only
FR38: Epic 5 — No-data state for days before item creation date
FR39: Epic 5 — History is read-only; no editing of past entries
FR40: Epic 1 — Local-only storage, no network dependency
FR41: Epic 1 — Data persists across restarts, updates, and device lifecycle
FR42: Epic 1 — Backup-eligible storage on iOS (Application Support/) and Android (Auto Backup)
FR43: Epic 1 — Schema migrations with explicit scripts; failure surfaces clear error

## Epic List

### Epic 1: Project Foundation & App Shell
A runnable KMP app on both iOS and Android with the full tech stack wired: SQLDelight schema defined, Koin DI configured, Navigation 3 with 3-tab scaffold, Dusty Indigo theme applied, and AppPreferences implemented. No user-facing features yet — but every subsequent epic builds directly on top of this without structural rework.

**FRs covered:** FR40, FR41, FR42, FR43
**Architecture requirements:** JetBrains KMP Wizard starter (rename `composeApp` → `shared`), `libs.versions.toml` with locked library versions, `TrackerDatabase.sq` schema, `DatabaseDriverFactory` expect/actual, `AppPreferences` expect/actual, Koin module files (`sharedModule.kt`, `androidModule.kt`, `iosModule.kt`), Navigation 3 scaffold (3 empty screens), `Theme.kt` / `Color.kt` / `Type.kt` (Dusty Indigo)

### Epic 2: Item Management
Users can configure their full supplement/medication list — add items with a name, weekday and weekend reminder times via the native OS time picker, choose an icon from the picker grid, optionally attach a photo, edit items in-place, and delete with confirmation. The Items screen is completely functional.

**FRs covered:** FR1, FR2, FR3, FR4, FR5, FR6, FR7, FR8, FR9, FR10, FR11, FR12, FR13

### Epic 3: Daily Logging & Today Screen
Users can see all today's items grouped by reminder time slot, tap once to log each item, clearly distinguish pending from logged items, and experience the all-done completion moment. The app delivers full daily-use value as a manual habit tracker — no notifications required.

**FRs covered:** FR14, FR16, FR18, FR19, FR20, FR21, FR22

### Epic 4: Notifications
Users receive timely grouped reminders at the right scheduled times and can bulk-log via the YES notification action without opening the app — even when the app is fully force-quit. The system reschedules automatically on every item change and app foreground. Graceful degradation when permission is denied, including the in-app banner prompt.

**FRs covered:** FR15, FR23, FR24, FR25, FR26, FR27, FR28, FR29, FR30, FR31, FR32, FR33, FR34

### Epic 5: History & Late Logging
Users can review their rolling 7-day history showing exactly what they took or missed each day, with 4 distinct visual states per item. A contextual Yesterday banner surfaces late-logging directly on the Today screen — users can recover missed items from the previous day without hunting for the feature.

**FRs covered:** FR17, FR35, FR36, FR37, FR38, FR39

---

## Epic 1: Project Foundation & App Shell

A runnable KMP app on both iOS and Android with the full tech stack wired: SQLDelight schema defined, Koin DI configured, Navigation 3 with 3-tab scaffold, Dusty Indigo theme applied, and AppPreferences implemented. No user-facing features yet — but every subsequent epic builds directly on top of this without structural rework.

### Story 1.1: KMP Project Initialization

As a developer,
I want to initialize the KMP project from the JetBrains wizard with correct module naming and all library dependencies declared,
So that the project compiles and runs on both iOS and Android with no structural debt to fix later.

**Acceptance Criteria:**

**Given** the JetBrains KMP Wizard (kmp.jetbrains.com) is used to generate the project with Android + iOS + Compose Multiplatform selected
**When** the generated project is opened
**Then** the `composeApp` module is renamed to `shared` in `settings.gradle.kts`, all import paths, and the Xcode `embedAndSignAppleFrameworkForXcode` build phase

**Given** the project is configured
**When** `libs.versions.toml` is created
**Then** it declares all locked versions: Kotlin 2.3.20, Compose Multiplatform 1.10.0, SQLDelight 2.2.1, Koin 3.5, Navigation 3 (1.0.1), kotlinx-datetime 0.7.1, Coroutines 1.10.2

**Given** the module structure is in place
**When** the project is built
**Then** the Android app launches to a blank screen on an Android device/emulator without errors
**And** the iOS app compiles and launches to a blank screen in Xcode without errors

**Given** the package name is configured
**When** any source file is opened
**Then** the root package is `com.sonja.tracker` throughout all source sets

### Story 1.2: SQLDelight Database Schema & Driver

As a developer,
I want the SQLDelight schema defined and the database driver wired for both platforms with backup-eligible storage,
So that any future story can read and write data without additional database setup.

**Acceptance Criteria:**

**Given** SQLDelight 2.2.1 is declared in `libs.versions.toml`
**When** `TrackerDatabase.sq` is created in `commonMain/sqldelight/com/sonja/tracker/`
**Then** it defines the `items` table (id, name, reminder_weekday_time, reminder_weekend_time, image_path, icon_id — all `snake_case` column names) and the `log_entries` table (id, item_id, date TEXT YYYY-MM-DD, state TEXT, logged_at)

**Given** the schema is defined
**When** `DatabaseDriverFactory.kt` expect class is created in `commonMain/data/db/`
**Then** the Android `actual` uses `AndroidSqliteDriver` with `context.filesDir` (Auto Backup eligible)
**And** the iOS `actual` uses `NativeSqliteDriver` with `Application Support/tracker.db` (backup-eligible)

**Given** a schema migration is needed
**When** the schema version is incremented and a `.sqm` file is added
**Then** migration runs on next app open without data loss
**And** migration failure throws `IllegalStateException` with a clear message (never silent)

**Given** the database driver is configured
**When** the app starts
**Then** `tracker.db` is created in the correct backup-eligible location on both platforms

### Story 1.3: Koin DI Wiring & App Preferences

As a developer,
I want Koin fully wired across all three module files and AppPreferences implemented,
So that any ViewModel or repository can be injected by subsequent stories without additional DI setup.

**Acceptance Criteria:**

**Given** Koin 3.5 is declared in `libs.versions.toml`
**When** `SharedModule.kt` is created in `commonMain/di/`
**Then** it declares `single { }` bindings for the SQLDelight DB instance, `ItemRepository`, and `LogRepository` (stubs are acceptable at this stage)

**Given** platform modules are configured
**When** `AndroidModule.kt` is created in `androidMain/di/`
**Then** it binds `androidContext()` and the Android `DatabaseDriverFactory` actual
**And** `startKoin { androidContext(this) + modules(...) }` is called from `TrackerApplication.onCreate()`

**Given** the iOS module is configured
**When** `IosModule.kt` is created in `iosMain/di/`
**Then** a shared `initKoin()` function in `commonMain` is called from the iOS `@main` entry point

**Given** `AppPreferences` expect class is created in `commonMain/data/prefs/`
**When** the iOS and Android `actual` implementations are provided
**Then** `getInstallDate()` / `setInstallDate(date)` reads and writes a `YYYY-MM-DD` string to platform-native key-value storage
**And** `getLastScheduledDate()` / `setLastScheduledDate(date)` similarly reads and writes correctly
**And** `isNotificationBannerDismissed()` / `setNotificationBannerDismissed()` persists correctly across app restarts on both platforms

**Given** Koin is initialized
**When** any `koinViewModel()` or `get()` call is made in a composable or ViewModel
**Then** dependencies resolve without runtime injection errors on both platforms

### Story 1.4: Navigation Scaffold & Dusty Indigo Theme

As a user,
I want to open the app and navigate between three tabs,
So that the app structure is in place and I can see the visual foundation before features are added.

**Acceptance Criteria:**

**Given** Navigation 3 (1.0.1) and Compose Multiplatform 1.10.0 are declared
**When** `AppNavigation.kt` is created in `commonMain/ui/navigation/`
**Then** a 3-tab `NavigationBar` is rendered with tabs labeled Today, History, and Items

**Given** the navigation is configured
**When** the app launches
**Then** the Today tab is always the initial destination

**Given** a tab is tapped
**When** the user taps History or Items
**Then** the corresponding empty placeholder screen is displayed without errors
**And** the selected tab is visually indicated

**Given** the Dusty Indigo colour tokens are defined
**When** `Color.kt`, `Theme.kt`, and `Type.kt` are created in `commonMain/ui/theme/`
**Then** `MaterialTheme` uses the custom colour scheme (warm near-white surface light, deep warm charcoal dark, muted indigo primary accent) on both platforms
**And** the app renders correctly in both light mode and dark mode using Material 3 dynamic theming

**Given** the Material 3 type scale is configured
**When** system font scaling is set to 150% on either platform
**Then** all text in the navigation scaffold scales correctly without layout overflow

---

## Epic 2: Item Management

Users can configure their full supplement/medication list — add items with a name, weekday and weekend reminder times via the native OS time picker, choose an icon from the picker grid, optionally attach a photo, edit items in-place, and delete with confirmation. The Items screen is completely functional.

### Story 2.1: Items List Screen

As a user,
I want to see all my configured supplements and medications in a list,
So that I have a clear overview of everything I'm tracking.

**Acceptance Criteria:**

**Given** the Items tab is tapped
**When** no items have been added yet
**Then** an empty state is displayed with a warm prompt ("Add your first item") and a filled button to add an item

**Given** one or more items exist in the database
**When** the Items screen is displayed
**Then** all configured items are listed, each showing their name and icon/image placeholder

**Given** items exist
**When** the Items screen is loaded
**Then** `ItemsViewModel` exposes a `StateFlow<ItemsUiState>` with `Loading`, `Success(items)`, and `Error` sealed states
**And** the composable `when`-branches on the sealed state — no `if (isLoading)` flags

**Given** the app is on the Items screen
**When** a new item is added or deleted in a subsequent story
**Then** the list updates reactively via SQLDelight `Flow` without a manual refresh

### Story 2.2: Add Item with Name & Weekday Reminder Time

As a user,
I want to add a new supplement or medication with a name and weekday reminder time,
So that I can start tracking it immediately.

**Acceptance Criteria:**

**Given** the user taps the add button on the Items screen
**When** the `ItemEditSheet` opens
**Then** it appears as a `ModalBottomSheet` with the name text field auto-focused

**Given** the `ItemEditSheet` is open in add mode
**When** the user enters a name and taps the weekday time row
**Then** the native OS time picker opens (iOS: UIDatePicker wheel style; Android: Material3 clock dial)
**And** the selected time is shown as the row label in `HH:mm` format

**Given** the user has entered a name (required) and optionally set a weekday time (defaults to 08:00 if untouched)
**When** the user taps Save
**Then** a new item is inserted into the `items` table with the given name and `reminder_weekday_time` stored as `HH:mm` TEXT
**And** the sheet dismisses and the new item appears in the Items list

**Given** the name field is empty
**When** the user taps Save
**Then** the OS keyboard prevents submission (name field is the only required field; no custom inline error message needed)

**Given** a new item is saved
**When** the save completes
**Then** notification rescheduling is triggered for the affected time slot (stub call acceptable in this epic; full scheduling implemented in Epic 4)

### Story 2.3: Weekend Reminder Time

As a user,
I want to set a different reminder time for weekends,
So that my weekend routine (which often starts later) gets the right reminder.

**Acceptance Criteria:**

**Given** the `ItemEditSheet` is open
**When** the name field has been filled
**Then** a "Different time on weekends?" toggle row is visible below the weekday time, collapsed by default

**Given** the toggle is tapped
**When** it expands
**Then** a weekend time row appears with its own native OS time picker, defaulting to the weekday time value

**Given** the weekend toggle is expanded and a time is selected
**When** the user saves the item
**Then** `reminder_weekend_time` is stored as `HH:mm` TEXT in the `items` table

**Given** the weekend toggle is not expanded (collapsed)
**When** the user saves the item
**Then** `reminder_weekend_time` is stored as NULL in the `items` table

**Given** an item with a weekend time is opened for editing
**When** the `ItemEditSheet` opens
**Then** the weekend toggle is pre-expanded showing the stored weekend time

### Story 2.4: Edit & Delete Item

As a user,
I want to edit an existing item's name and reminder times, or delete it,
So that I can keep my list accurate as my routine changes.

**Acceptance Criteria:**

**Given** items exist in the Items list
**When** the user taps an item
**Then** the `ItemEditSheet` opens in edit mode with all fields pre-filled from the stored values

**Given** the `ItemEditSheet` is in edit mode
**When** the user changes the name and/or reminder times and taps Save
**Then** the item is updated in the `items` table and the list reflects the change immediately

**Given** the `ItemEditSheet` is in edit mode
**When** the user taps the Delete button (amber `TextButton` — not red)
**Then** an `AlertDialog` appears with the item name in the title (e.g. "Delete Vitamin D3?") and "Delete" (amber) + "Cancel" actions

**Given** the delete confirmation dialog is shown
**When** the user taps Delete
**Then** the item is removed from the `items` table and disappears from the Items list

**Given** the delete confirmation dialog is shown
**When** the user taps Cancel
**Then** the dialog dismisses and the `ItemEditSheet` remains open with no changes made

**Given** an item is deleted
**When** deletion completes
**Then** notification rescheduling/cancellation is triggered for that item (stub call acceptable; full implementation in Epic 4)

### Story 2.5: Icon Picker

As a user,
I want to choose an icon for each item from a curated grid,
So that I can instantly recognise each supplement by its icon without reading the name.

**Acceptance Criteria:**

**Given** the user has entered an item name in the `ItemEditSheet`
**When** the keyboard is dismissed after name entry
**Then** the `IconPickerGrid` opens automatically as a bottom section within the sheet, with a prominent "Skip" button visible

**Given** the `IconPickerGrid` is displayed
**When** rendered
**Then** it shows approximately 24 icons — sourced from Material Icons Extended and up to 4 custom Phosphor/Lucide MIT SVGs stored in `commonMain/composeResources/drawable/` — in a scrollable grid of rounded-square tiles

**Given** the user taps an icon tile
**When** selected
**Then** the tile shows an indigo tint border indicating selection
**And** the icon is previewed in the item row area above

**Given** the user taps Skip or proceeds without selecting an icon
**When** the item is saved with no icon selected and no image attached
**Then** `icon_id` is stored as NULL and a default placeholder icon is displayed throughout the app

**Given** an icon is selected and the item is saved
**When** the item appears in any screen (Items, Today, History)
**Then** the selected icon is displayed in a 40dp rounded-square thumbnail next to the item name

**Given** an item with an existing icon is opened for editing
**When** the `ItemEditSheet` opens
**Then** the previously selected icon is shown as the current selection in the `IconPickerGrid`

### Story 2.6: Item Photos (Camera & Gallery)

As a user,
I want to attach a photo of my supplement bottle or pill,
So that I can recognise items instantly by their actual appearance.

**Acceptance Criteria:**

**Given** the `ItemEditSheet` is open
**When** a photo option row is displayed below the icon picker
**Then** it shows two options: Camera and Gallery

**Given** the user taps Camera for the first time
**When** the system camera permission prompt appears
**Then** if granted, the camera opens and the captured photo replaces the icon as the item's image
**And** the image is stored in app-private storage via `AppImageStorage` expect/actual (iOS: `Application Support/<app>/images/`; Android: `context.filesDir/images/`) — not in the system media gallery

**Given** the user taps Gallery for the first time
**When** the system photo library permission prompt appears
**Then** if granted, the photo picker opens and the selected image is copied to app-private storage as the item image

**Given** camera permission is permanently denied
**When** the `ItemEditSheet` is displayed
**Then** the Camera option is hidden; the Gallery option remains if gallery permission is granted

**Given** photo library permission is permanently denied
**When** the `ItemEditSheet` is displayed
**Then** the Gallery option is hidden; the Camera option remains if camera permission is granted

**Given** both camera and gallery permissions are permanently denied
**When** any screen displays items
**Then** no image placeholder is shown anywhere in the app (Items list, Today screen, History)

---

## Epic 3: Daily Logging & Today Screen

Users can see all today's items grouped by reminder time slot, tap once to log each item, clearly distinguish pending from logged items, and experience the all-done completion moment. The app delivers full daily-use value as a manual habit tracker — no notifications required.

### Story 3.1: Today Screen — Grouped Item List

As a user,
I want to open the app and immediately see all of today's items grouped by reminder time slot,
So that I know exactly what's pending and what I've already taken at a glance.

**Acceptance Criteria:**

**Given** the app launches
**When** the Today tab is displayed
**Then** `TodayViewModel` exposes a `StateFlow<TodayUiState>` sealed class with `Loading`, `Success(groups, allLogged)`, and `Error` states

**Given** no items have been configured
**When** the Today screen renders in `Success` state with an empty item list
**Then** an empty state is displayed with a warm illustration, "Add your first item" prompt, and a filled button navigating to the Items tab

**Given** items exist
**When** the Today screen renders
**Then** items are displayed in a `LazyColumn` with `TimeGroupSection` headers grouping items by their reminder time slot (weekday or weekend time, selected based on current day of week)
**And** the screen is fully interactive within 2 seconds of app launch on mid-range devices (NFR1)

**Given** multiple items share the same reminder time
**When** displayed on the Today screen
**Then** they appear under a single `TimeGroupSection` header for that time slot

**Given** items have different reminder times
**When** displayed on the Today screen
**Then** each unique time slot has its own `TimeGroupSection` header, ordered chronologically

### Story 3.2: Logging an Item

As a user,
I want to tap an item to log it as taken,
So that I have an immediate, definitive record that I took it today.

**Acceptance Criteria:**

**Given** an item is in the pending state on the Today screen
**When** the user taps anywhere on the `ItemRow`
**Then** the row transitions immediately to the logged state — tonal indigo background fill + check indicator — within 500ms (NFR2)
**And** a `log_entries` row is inserted with the current item ID, today's date (`YYYY-MM-DD`), and state `"logged"`

**Given** an item has been logged
**When** it appears on the Today screen
**Then** the row is visible but non-tappable (disabled state) — consistent with the read-only log principle
**And** the item name and icon/photo remain fully visible in the logged state

**Given** items are displayed in both pending and logged states
**When** viewed without colour perception
**Then** pending vs. logged distinction is conveyed by both the tonal fill AND a visible check indicator — not colour alone (NFR15)

**Given** all tappable `ItemRow` elements
**When** measured
**Then** the tap target is the full row width with a minimum height of 56dp / 44pt (NFR13)

**Given** the `ItemRow` is focused by VoiceOver or TalkBack
**When** announced
**Then** the content description reads "[Item name], pending" or "[Item name], logged" or "[Item name], already logged" as appropriate (NFR16)
**And** `Role.Checkbox` semantic is applied to the row

**Given** a log entry is written
**When** the SQLDelight `Flow` emits
**Then** the `TimeGroupSection` badge updates reactively (e.g. "3 pending" → "2 pending") without a full-screen reload

### Story 3.3: TimeGroupSection Badge & Overdue State

As a user,
I want to see at a glance how many items remain in each time group, and a subtle indicator when a group's reminder time has passed,
So that I know exactly where I stand throughout the day without counting items manually.

**Acceptance Criteria:**

**Given** a `TimeGroupSection` has items remaining
**When** displayed before the group's reminder time has passed
**Then** the group badge shows "X pending" in neutral styling

**Given** a `TimeGroupSection` has items remaining
**When** the current time is past the group's reminder time slot
**Then** the group header shows a subtle amber-tinted "X overdue" badge — derived at render time from `currentTime > slotTime`, no extra DB field required

**Given** all items in a `TimeGroupSection` are logged
**When** the last item in the group is tapped
**Then** the group badge updates to "all done" and the overdue indicator (if showing) clears

**Given** different groups on the Today screen
**When** some are overdue and some are upcoming
**Then** only groups whose reminder time has passed with pending items show the overdue indicator; upcoming groups remain neutral

### Story 3.4: All-Done Completion State

As a user,
I want to see a warm completion message when I've logged everything for the day,
So that the habit loop closes with a satisfying, positive moment.

**Acceptance Criteria:**

**Given** at least one item exists and all items across all groups are logged
**When** the last item is tapped
**Then** the grouped list composable swaps to the `AllDoneHero` composable — driven by `allLogged == true` in `TodayUiState.Success`

**Given** the `AllDoneHero` is displayed
**When** rendered
**Then** it shows a large icon (52dp), the primary message "All done today", and the secondary line "See you tomorrow"
**And** a compact checked item list is visible below the message (all items remain visible in logged state)

**Given** the composable swap occurs
**When** `allLogged` transitions from false to true
**Then** a light fade/scale-in animation plays on the `AllDoneHero` — no abrupt replacement

**Given** VoiceOver or TalkBack is active
**When** `AllDoneHero` appears
**Then** it announces "All items logged for today"

---

## Epic 4: Notifications

Users receive timely grouped reminders at the right scheduled times and can bulk-log via the YES notification action without opening the app — even when the app is fully force-quit. The system reschedules automatically on every item change and app foreground. Graceful degradation when permission is denied, including the in-app banner prompt.

### Story 4.1: Notification Permission & Denied Banner

As a user,
I want the app to ask for notification permission on first launch and show a non-intrusive banner if I decline,
So that I can make an informed choice without the app nagging me repeatedly.

**Acceptance Criteria:**

**Given** the app is launched for the very first time
**When** the Today screen appears
**Then** the system notification permission prompt is shown (iOS: `UNUserNotificationCenter.requestAuthorization`; Android 13+: `POST_NOTIFICATIONS` runtime permission)

**Given** the user grants notification permission
**When** the prompt is dismissed
**Then** no banner is shown and the app proceeds normally

**Given** the user denies notification permission
**When** the prompt is dismissed
**Then** the `NotificationDeniedBanner` appears on the Today screen — bell icon + short message + "Enable" `TextButton`

**Given** the `NotificationDeniedBanner` is visible
**When** the user taps "Enable"
**Then** the app navigates to the system notification settings for the app

**Given** the user dismisses the `NotificationDeniedBanner`
**When** dismissed
**Then** `AppPreferences.setNotificationBannerDismissed()` is called and the banner never appears again — even after app restart

**Given** notification permission is denied and the banner has been dismissed
**When** the user uses the app
**Then** all manual logging flows work identically to the permission-granted experience (FR33)

### Story 4.2: Notification Scheduling Engine

As a user,
I want to receive a reminder notification at the right time each day for each of my time slots,
So that I never have to remember to open the app.

**Acceptance Criteria:**

**Given** `NotificationInterface.kt` expect class is created in `commonMain/notifications/`
**When** the Android and iOS `actual` implementations are provided
**Then** the Android actual uses `AlarmManager.setExactAndAllowWhileIdle` / `RTC_WAKEUP` with a `BroadcastReceiver` to fire notifications
**And** the iOS actual uses `UNUserNotificationCenter` to schedule local notifications
**And** both `actual` implementations contain zero business logic — all grouping, suppression checks, and ID construction live in `NotificationScheduler` in `commonMain`

**Given** `NotificationScheduler.kt` is created in `commonMain/domain/scheduler/`
**When** `rescheduleAll()` is called
**Then** it queries all items, groups them by time slot (weekday or weekend, based on day of week), and schedules one notification per unique time slot for the next 7 days
**And** notification IDs follow the format `{itemId}_{date}_{timeSlot}` (e.g. `"3_2026-04-07_08:00"`)

**Given** the app is brought to the foreground
**When** `rescheduleAll()` is triggered
**Then** it completes in the background within 1 second without blocking the UI (NFR3)
**And** `AppPreferences.setLastScheduledDate(today)` is written to support lapse detection

**Given** multiple items share the same reminder time slot
**When** a notification is scheduled for that slot
**Then** a single grouped notification is created covering all items in the slot (FR24)

**Given** an item has a unique reminder time
**When** scheduled
**Then** it receives its own individual notification (FR25)

**Given** `NotificationSchedulerTest.kt` exists in `commonTest/domain/scheduler/`
**When** the grouping and ID construction logic is tested with an in-memory SQLDelight driver
**Then** all unit tests pass

### Story 4.3: YES Bulk-Action (Foreground & Background)

As a user,
I want to tap YES on a notification to log all items in that group without opening the app,
So that my morning routine can be logged in one tap from the lock screen.

**Acceptance Criteria:**

**Given** a grouped notification is delivered
**When** it is displayed on iOS or Android
**Then** it includes a "YES" action button alongside the notification message

**Given** the user taps YES on a notification while the app is in the foreground or background (not force-quit)
**When** the action is handled
**Then** a `log_entries` row is inserted for every item in the notification's time slot group with today's date and state `"logged"`
**And** the notification is dismissed

**Given** items in a group were already logged before the YES action fires
**When** the action is handled
**Then** only items without an existing log entry for today are inserted — no duplicate log entries

**Given** the user opens the app after tapping YES
**When** the Today screen loads
**Then** the logged items already show the logged state — seamless continuity between notification and in-app views

**Given** the notification payload
**When** constructed by `NotificationScheduler`
**Then** it carries the item IDs or time-slot key needed to identify which items to log — no hardcoded IDs in platform code

### Story 4.4: YES Bulk-Action (Force-Quit)

As a user,
I want tapping YES on a notification to log my items even if the app has been completely force-quit,
So that I never lose a log entry because of how the OS manages app state.

**Acceptance Criteria:**

**Given** the iOS app is fully force-quit
**When** the user taps YES on a notification
**Then** the iOS Notification Service Extension (`NotificationServiceExtension/NotificationService.swift`) is invoked
**And** it initialises `DatabaseDriverFactory` directly (no Koin) and writes a `log_entries` row for each item in the notification group with today's date and state `"logged"`
**And** the extension exits cleanly without starting the full app

**Given** the Android app is fully force-quit
**When** the user taps YES on a notification
**Then** the `NotificationReceiver` `BroadcastReceiver` is invoked
**And** it initialises `DatabaseDriverFactory` directly (no Koin) and writes a `log_entries` row for each item with today's date and state `"logged"`

**Given** the force-quit YES action completes on either platform
**When** the user subsequently opens the app
**Then** the Today screen shows those items in the logged state — the SQLDelight `Flow` emits the written entries correctly

**Given** the BroadcastReceiver or NSE writes to the database
**When** the main app is later opened and also queries the same database
**Then** no data conflicts or corruption occur — writes are atomic (NFR10)

### Story 4.5: Notification Suppression, Rescheduling & Lapse Recovery

As a user,
I want notifications to be automatically suppressed when I've already logged those items, rescheduled when my list changes, and recovered gracefully if the app hasn't been opened in a while,
So that I never get a redundant reminder and reminders always reflect my current routine.

**Acceptance Criteria:**

**Given** an item or group of items has already been logged for the day
**When** `NotificationScheduler` evaluates pending notifications
**Then** any notification for that time slot on that day is cancelled via `NotificationInterface.cancelNotification(id)` (FR27)

**Given** an item is added, edited, or deleted
**When** the save or delete operation completes
**Then** `NotificationScheduler.rescheduleAll()` or `rescheduleForSlot(timeSlot)` is called immediately — never from a ViewModel or Repository directly (FR28)
**And** stale notifications for affected slots are cancelled and replaced with updated ones

**Given** the app has not been opened for more than 7 days
**When** the app is next foregrounded
**Then** `AppPreferences.getLastScheduledDate()` reveals the lapse
**And** `rescheduleAll()` runs to restore the 7-day lookahead window (FR29, FR30)
**And** a `Snackbar` with the message "Reminders rescheduled" is shown — auto-dismiss, no action button (FR31)

**Given** the `SCHEDULE_EXACT_ALARM` permission is not granted on Android 12+
**When** scheduling
**Then** the scheduler falls back to inexact alarms gracefully — no crash, reminders still fire (accepted known limitation per PRD)

**Given** notification suppression logic is tested
**When** `NotificationSchedulerTest.kt` runs the suppression scenario with an in-memory driver
**Then** `cancelNotification` is called for already-logged slots and not called for pending ones

---

## Epic 5: History & Late Logging

Users can review their rolling 7-day history showing exactly what they took or missed each day, with 4 distinct visual states per item. A contextual Yesterday banner surfaces late-logging directly on the Today screen — users can recover missed items from the previous day without hunting for the feature.

### Story 5.1: History Screen — 7-Day Log View

As a user,
I want to see a rolling 7-day history for all my items,
So that I can reflect on my routine and know exactly what I took on any given day.

**Acceptance Criteria:**

**Given** the History tab is tapped
**When** the app was installed less than 1 day ago (no history yet)
**Then** an empty state is displayed with a neutral message "No history yet — come back tomorrow"

**Given** the app has been in use for at least 1 day
**When** the History screen is displayed
**Then** `HistoryViewModel` exposes a `StateFlow<HistoryUiState>` with `Loading`, `Success(days)`, and `Error` sealed states
**And** days are displayed from today backwards, showing only days from app installation onwards (FR37)
**And** `AppPreferences.getInstallDate()` is used as the history boundary — days before install are not shown

**Given** a day is displayed in the history
**When** rendered via `HistoryDayView`
**Then** each item shows exactly one of four states per day:
- `logged` — indigo tint fill
- `late-logged` — lighter indigo tint + "late" label
- `not-logged` — warm grey; never red (avoids medical alarm association)
- `no-data` — empty, no indicator shown (days before item creation date per FR38)

**Given** items are in different history states
**When** displayed
**Then** each state is distinguishable by icon shape AND label — not colour alone (NFR15)

**Given** the history screen loads 7 days of data
**When** measured on a mid-range device
**Then** it renders within 1 second (NFR4)

**Given** the history is displayed
**When** the user attempts to tap a logged or not-logged item from any day other than yesterday
**Then** no interaction is available — history is read-only (FR39)

### Story 5.2: Late Logging Yesterday's Items

As a user,
I want to log items I missed yesterday directly from the history screen,
So that I can recover from a missed day without losing the record entirely.

**Acceptance Criteria:**

**Given** the History screen is showing yesterday's date
**When** an item has no log entry for yesterday
**Then** the item row shows a tap affordance indicating it can be logged

**Given** the user taps an unlogged item in yesterday's `HistoryDayView`
**When** the tap is handled
**Then** a `log_entries` row is inserted with yesterday's date (`YYYY-MM-DD`) and state `"late_logged"`
**And** the item row immediately updates to the `late-logged` visual state (lighter indigo tint + "late" label)

**Given** an item has already been logged or late-logged for yesterday
**When** displayed in yesterday's `HistoryDayView`
**Then** the row is non-tappable — consistent with the read-only principle for all days except yesterday

**Given** a late-log entry is written
**When** the Today screen is subsequently viewed
**Then** the `YesterdayBanner` (Story 5.3) reflects the updated count of unresolved items correctly

### Story 5.3: Yesterday Banner — Late-Log Discovery

As a user,
I want a contextual banner on the Today screen to tell me when yesterday had unlogged items,
So that I discover the opportunity to late-log without having to check the history tab every morning.

**Acceptance Criteria:**

**Given** the user opens the app on any day
**When** `TodayViewModel` derives whether yesterday has unlogged items (SQLDelight query over `log_entries` for yesterday's date)
**Then** the `YesterdayBanner` is visible on the Today screen only when yesterday has at least one item with no log entry

**Given** yesterday has no unlogged items (all logged, late-logged, or no-data)
**When** the Today screen renders
**Then** the `YesterdayBanner` is not shown

**Given** the `YesterdayBanner` is visible
**When** rendered
**Then** it shows an amber-tinted icon, a message like "2 items unlogged yesterday", and a "Log →" `TextButton`

**Given** the user taps the `YesterdayBanner`
**When** the tap is handled
**Then** Navigation 3 navigates directly to yesterday's date in the History screen — not the history root — via a navigation argument

**Given** the user taps the banner, late-logs all remaining items in Story 5.2, and returns to the Today screen
**When** `TodayViewModel` re-evaluates
**Then** the `YesterdayBanner` auto-dismisses because yesterday no longer has unlogged items

**Given** VoiceOver or TalkBack is active
**When** the `YesterdayBanner` is focused
**Then** it is announced as an actionable element with destination context (e.g. "Yesterday, 2 items unlogged. Navigate to log them.")
**And** `Role.Button` semantic is applied
