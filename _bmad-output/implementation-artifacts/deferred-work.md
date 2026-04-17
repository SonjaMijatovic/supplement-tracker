# Deferred Work

## Deferred from: code review of 3-1-today-screen-grouped-item-list (2026-04-17)

- `formatTimeSlot` no hour-range validation — a malformed stored value like "25:00" would render as "13:00 PM"; app controls time storage via its own picker so unreachable in practice. Add validation when a time-input sanitisation pass is done.

## Deferred from: code review of 2-6-item-photos-camera-and-gallery (2026-04-17)

- No image file cleanup on item delete or photo replace — `AppImageStorage` files are never deleted; each "replace photo" orphans the old file. Fix in a future story when a storage management pass is done.
- `AppImageStorage` uses `Any?` context type — by-design pattern chosen to mirror `DatabaseDriverFactory`; runtime cast is safe given Koin DI wiring, but compile-time safety is lost. Refactor when KMP adds platform-typed expect parameters.
- Coil image size bounding and memory/disk cache policy not configured — `AsyncImage(model = item.imagePath)` loads full-res images in list rows; no sampling or cache limit. Configure via `ImageRequest` when profiling shows memory pressure.
- `UIImagePickerController` deprecated (iOS 14+) — known suppressed with `@Suppress("DEPRECATION")`; upgrade to `PHPickerViewController` in a future story targeting iOS 14+ only.
- `ItemRow` empty thumbnail when both `imagePath` and `iconId` are null — pre-existing behavior from Story 2.1; the `when` block has no `else` branch. Add a generic placeholder icon when a UX polish pass is done.
- iOS `UIApplication.sharedApplication.keyWindow` deprecated (iOS 15+) — single-scene app; not currently broken, but will need migration to `UIApplication.sharedApplication.connectedScenes` for multi-window support.
- Clearing existing photo via `null` `imagePath` in `updateItem` — no current evidence of breakage; verify explicitly when a "remove photo" affordance is added to `ItemEditSheet`.

## Deferred from: code review of 2-5-icon-picker (2026-04-16)

- `ItemRow` contentDescription is hardcoded English — Story 2.5 added `"No icon"` / `"[name] icon"`; project-wide localisation deferred.
- Silent no-op for unknown `iconId` in `ItemIconContent` — if a previously-stored `iconId` is removed from `ItemIcons` registry, the item thumbnail renders empty with no warning; intentional design, low risk while iconIds are app-controlled. Add debug logging when a logging infrastructure is in place.
- `editItem` `imagePath` race condition — if `uiState` is not `Success` when `editItem` coroutine runs, `current == null` and `imagePath` is passed as `null`, silently clearing any existing `imagePath` value in the DB; benign until Story 2.6 when `imagePath` becomes non-null. Fix before or during Story 2.6 implementation.
- Skip button doesn't reset scroll position after collapsing picker — minor UX: user scrolled down to the picker may be left viewing blank space after tapping Skip; fix when UX polish pass is done.

## Deferred from: code review of 2-4-edit-and-delete-item (2026-04-15)

- No error handling in `editItem`/`deleteItem` ViewModel or Repository — `viewModelScope.launch` swallows exceptions silently; matches pre-existing `addItem` pattern. Fix project-wide when error handling is added to write paths.
- `editItem` reads `uiState.value` for `imagePath`; if state is not `Success`, it is passed as `null` and overwrites existing DB value — harmless until Story 2.6 when imagePath is populated; revisit then.
- Sheet dismisses via `onDismiss()` synchronously before DB coroutine completes — fire-and-forget pattern; matches `addItem` behaviour. User sees item briefly before list refreshes. Fix project-wide when error handling is added.
- `updateItem`/`deleteItem` SQL are silent no-ops if `WHERE id = ?` matches no rows — ID always sourced from the DB-backed `observeItems()` flow, so a stale ID is theoretical; add affected-rows check when error handling is added.
- `selectedItem` in `ItemsScreen` holds an item snapshot; if an external source modifies the record while the sheet is open, pre-filled values are stale — no external sync in v1; revisit when background sync or multi-device support is added.
- `editItem`/`deleteItem` coroutines have no mutex — concurrent execution theoretically possible, not triggerable in single-user flow since the sheet dismisses after each action.
- `initialWeekdayTime` falls back to `"08:00"` when `reminderWeekdayTime` is `null` in the DB — opening and saving without changing the time silently writes `"08:00"` to a previously-null field; acceptable for v1, consistent with `addItem` default.
- `contentDescription` in `ItemRow` contains hardcoded English string `"tap to edit"` — not localisable; matches project-wide pattern of hardcoded strings.
- `weekdayTime` is forwarded from the time picker to the DB without format validation — time picker enforces valid format in practice; will need validation when Epic 4 parses this field for notification scheduling.
- `clickable` modifier applied after `.padding(vertical = 4dp)` in `ItemRow` — touch target excludes 4dp on top/bottom; spec-prescribed modifier order; `heightIn(min = 56dp)` ensures adequate touch area.

## Deferred from: code review of 1-1-kmp-project-initialization (2026-04-08)

- No `iosX64` simulator target — Intel Mac developers will get a linker error from `embedAndSignAppleFrameworkForXcode`. Add `iosX64()` to `shared/build.gradle.kts` if Intel Mac support is needed.
- JAVA_HOME no error guard in Xcode build phase script — on machines with no JDK, `$(/usr/libexec/java_home)` exits silently. Add a guard: `if ! JAVA_HOME=$(/usr/libexec/java_home 2>/dev/null); then echo "JDK not found"; exit 1; fi`. Documented in CONTRIBUTING.md.
- No `androidTest` source set despite espresso/testExt declared in the version catalog. Wire up instrumentation tests when Story 1.3 (DI) or later stories introduce logic worth integration-testing.

## Deferred from: code review of 1-3-koin-di-wiring-and-app-preferences (2026-04-08)

- BroadcastReceiver/NSE process restart without Koin — by architecture design; receivers init SQLDelight directly, no Koin. Relevant when Epic 4 (Notifications) is implemented.
- `NSSearchPathForDirectoriesInDomains` empty-list crash in `DatabaseDriverFactory.ios.kt` — pre-existing Story 1-2 code; `.first() as String` will NPE if the directory list is empty. Harden in a future refactor.
- iOS `iosArm64Main`/`iosSimulatorArm64Main` declared separately — pre-existing Story 1-2 pattern; adding a future iOS target will require manually adding its deps. Consider consolidating when Kotlin tooling supports `iosMain` at config time.
- `TrackerDatabase` driver creation failure uncaught — no graceful fallback if SQLite init fails; the app will crash on first DB access with an opaque `InstanceCreationException`. Add error handling when repositories are implemented (Story 2.1+).
- iOS `AppPreferences` ignores `context` parameter — when Epic 4 implements the Notification Service Extension, NSE may need a shared App Group `NSUserDefaults` suite rather than `standardUserDefaults`. The current API has no way to pass a suite name. Revisit in Epic 4.

## Deferred from: code review of 2-3-weekend-reminder-time (2026-04-15)

- `hideNavBar` cleanup race on swipe-dismiss — `awaitCancellation` finally block is correct in normal flow; theoretical risk if `ItemEditSheet` is ever hosted outside `AppNavigation`'s `CompositionLocalProvider`. Revisit if sheet is reused in other routes.
- `onSave` + `onDismiss` called sequentially with no error guard — pre-existing pattern; ViewModel launch boundary prevents synchronous throw. Fix project-wide when error handling is added to write paths.
- Collapsing weekend toggle in edit mode stores NULL for `reminder_weekend_time` with no UX warning — accepted for v1 simplicity; no warning needed.
- No UI/composable test for weekend toggle pre-expansion (`initialWeekendTime`) — Compose UI test infrastructure not yet set up; cover when UI test infra is established.
- `weekendTime ?: weekdayTime` fallback in weekend time row is dead code (invariant: weekendTime is always non-null when toggle is expanded) — defensive but harmless; remove or document when invariant is formally enforced.

## Deferred from: code review of 2-2-add-item-with-name-and-weekday-reminder-time (2026-04-14)

- `addItem` errors silently swallowed — `viewModelScope.launch { repository.addItem() }` has no try/catch and the UI never surfaces insert failures to the user; sheet always dismisses regardless of success. Explicitly noted in story dev notes as a known limitation. Fix when adding error handling to the write path.
- Hardcoded UI strings not localizable — `"Add item"`, `"Save"`, `"Cancel"`, `"OK"`, `"Name"`, `"Weekday reminder"` etc. are raw literals. No localization pipeline exists yet. Address project-wide when localisation is added.

## Deferred from: code review of 2-1-items-list-screen (2026-04-14)

- Story 2.2 `addItem` errors silently swallowed — `viewModelScope.launch { repository.addItem() }` has no try/catch and no UI feedback on failure. Fix when addressing Story 2.2 review.
- `log_entries` date/time columns stored as `TEXT` with no format constraint or index — any bad string passes silently; range queries will do full table scans. Revisit when Epic 3 (logging) implements date-based queries.
- `LogRepository` stub registered in Koin `sharedModule` but has no queries or test coverage — intentional deferred stub; wire up when Epic 3 implements log entries.
- Nested `Scaffold` in `ItemsScreen` (Story 2.2 introduced a `Scaffold`+FAB inside `ItemsScreen`, which is already inside `AppNavigation`'s `Scaffold`) — double `innerPadding` application may clip list content behind the nav bar on Android. Fix in Story 2.2 review.

## Deferred from: code review of 1-4-navigation-scaffold-and-dusty-indigo-theme (2026-04-14)

- No back stack — hardware Back exits app from any tab. Architectural limitation of state-based navigation; will be resolved when Navigation 3 ships KMP/iOS klibs and is swapped in.
- `AppRoute` does not extend `NavKey` — the documented future Navigation 3 swap will require modifying the routes again to add `: NavKey`. Cannot be added now without the library.
- Koin init crash leaves global context partially constructed — `iOSApp.init()` calls `IosModuleKt.doInitKoin()` with no try/catch; a failure (e.g., DatabaseDriverFactory) leaves Koin half-started, causing opaque black screen. Address when repositories are wired (Story 2.1+).
- `NSApplicationSupportDirectory` path may not exist on fresh install — `DatabaseDriverFactory.ios.kt` constructs the DB path without ensuring the directory exists; SQLite open fails on first launch. Call `NSFileManager.createDirectoryAtPath` before constructing the driver (Story 2.1+ when DB is first exercised).
