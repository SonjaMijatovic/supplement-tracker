# Deferred Work

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
