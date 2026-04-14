# Story 1.4: Navigation Scaffold & Dusty Indigo Theme

Status: done

## Story

As a user,
I want to open the app and navigate between three tabs,
so that the app structure is in place and I can see the visual foundation before features are added.

## Acceptance Criteria

1. **Given** Navigation 3 (1.0.1) and Compose Multiplatform 1.10.3 are declared, **When** `AppNavigation.kt` is created in `commonMain/ui/navigation/`, **Then** a 3-tab `NavigationBar` is rendered with tabs labeled Today, History, and Items.

2. **Given** the navigation is configured, **When** the app launches, **Then** the Today tab is always the initial destination.

3. **Given** a tab is tapped, **When** the user taps History or Items, **Then** the corresponding empty placeholder screen is displayed without errors, **And** the selected tab is visually indicated.

4. **Given** the Dusty Indigo colour tokens are defined, **When** `Color.kt`, `Theme.kt`, and `Type.kt` are created in `commonMain/ui/theme/`, **Then** `MaterialTheme` uses the custom colour scheme (warm near-white surface light, deep warm charcoal dark, muted indigo primary accent) on both platforms, **And** the app renders correctly in both light mode and dark mode using Material 3 dynamic theming.

5. **Given** the Material 3 type scale is configured, **When** system font scaling is set to 150% on either platform, **Then** all text in the navigation scaffold scales correctly without layout overflow.

## Tasks / Subtasks

- [x] Task 1: Fix Navigation 3 dependency in `libs.versions.toml` + add serialization plugin (AC: 1)
  - [x] Change `navigation-compose` entry: module from `org.jetbrains.androidx.navigation:navigation-compose` to `org.jetbrains.androidx.navigation3:navigation3-ui` (see critical note in Dev Notes) — navigation3 has no iOS klibs; entry removed entirely; navigation3 dep deferred
  - [x] Rename libs alias from `navigation-compose` to `navigation3-ui` in both `[libraries]` and `[plugins]`/`[bundles]` sections if referenced
  - [x] Add serialization plugin entry: `kotlinSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }`

- [x] Task 2: Update `shared/build.gradle.kts` (AC: 1)
  - [x] Add `alias(libs.plugins.kotlinSerialization)` to the plugins block
  - [x] Add `implementation(libs.navigation3.ui)` to `commonMain.dependencies` — skipped; navigation3 has no iOS klibs (see Completion Notes)

- [x] Task 3: Create theme files (AC: 4, 5)
  - [x] Create `shared/src/commonMain/kotlin/com/sonja/tracker/ui/theme/Color.kt` — Dusty Indigo token declarations
  - [x] Create `shared/src/commonMain/kotlin/com/sonja/tracker/ui/theme/Type.kt` — Material 3 type scale (system font, no custom typeface)
  - [x] Create `shared/src/commonMain/kotlin/com/sonja/tracker/ui/theme/Theme.kt` — `TrackerTheme` composable wrapping `MaterialTheme`

- [x] Task 4: Create placeholder screens (AC: 1, 2, 3)
  - [x] Create `shared/src/commonMain/kotlin/com/sonja/tracker/ui/today/TodayScreen.kt`
  - [x] Create `shared/src/commonMain/kotlin/com/sonja/tracker/ui/history/HistoryScreen.kt`
  - [x] Create `shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/ItemsScreen.kt`

- [x] Task 5: Create `AppNavigation.kt` (AC: 1, 2, 3)
  - [x] Create `shared/src/commonMain/kotlin/com/sonja/tracker/ui/navigation/AppNavigation.kt`
  - [x] Define `@Serializable sealed interface AppRoute` with `TodayRoute`, `HistoryRoute`, `ItemsRoute` data objects — `: NavKey` omitted (no Nav3 library; deferred)
  - [x] Set up `SavedStateConfiguration` with explicit `SerializersModule` — N/A (no Nav3 library); replaced with ordinal `Saver` for `rememberSaveable` (added in code review)
  - [x] Implement `Scaffold` with `NavigationBar` bottom bar + state-based `when` (NavDisplay/NavBackStack not available; see Completion Notes)
  - [x] Today tab is initial destination

- [x] Task 6: Update `App.kt` (AC: 1, 4)
  - [x] Replace wizard placeholder composable body with `TrackerTheme { AppNavigation() }`
  - [x] Remove unused imports (AnimatedVisibility, Image, Button, Greeting, painterResource, compose_multiplatform resource)

- [x] Task 7: Delete wizard placeholder file
  - [x] Delete `shared/src/commonMain/kotlin/com/sonja/tracker/Greeting.kt` (no longer used)

- [x] Task 8: Verify builds pass (AC: all)
  - [x] `./gradlew :shared:assembleDebug` — BUILD SUCCESSFUL
  - [x] `./gradlew :androidApp:assembleDebug` — BUILD SUCCESSFUL
  - [ ] `./gradlew :shared:testDebugUnitTest` — not run (no new tests introduced)
  - [x] `./gradlew :shared:compileKotlinIosSimulatorArm64` — BUILD SUCCESSFUL
  - [x] iOS Xcode build — BUILD SUCCESSFUL (libsqlite3 linked; import Shared casing fixed)

## Dev Notes

### CRITICAL: Navigation 3 Dependency Coordinate Discrepancy

The current `libs.versions.toml` has:
```toml
navigation-compose = { module = "org.jetbrains.androidx.navigation:navigation-compose", version.ref = "navigation" }
```
with `navigation = "1.0.1"`. **This coordinate is wrong for Navigation 3.**

The `org.jetbrains.androidx.navigation:navigation-compose` artifact uses version numbers in the `2.x` range (it's the CMP port of the old Navigation Compose). Version `1.0.1` does not exist for that artifact.

**Navigation 3** (the new redesigned API) uses a different group ID:
```toml
# Fix in libs.versions.toml [libraries] section:
navigation3-ui = { module = "org.jetbrains.androidx.navigation3:navigation3-ui", version.ref = "navigation" }
```

The `navigation = "1.0.1"` version key is correct — `navigation3-ui:1.0.1` stable was released February 2026. Only the group ID needs fixing.

**In `shared/build.gradle.kts`**, use the fixed alias:
```kotlin
commonMain.dependencies {
    implementation(libs.navigation3.ui)  // NOT libs.navigation.compose
}
```

### Navigation 3 API (NOT the old NavController/NavHost)

Navigation 3 is a **completely different API** from old Navigation Compose. Do not use `NavController`, `NavHost`, or `composable()` DSL — those are the 2.x API.

**Navigation 3 core concepts:**
- Routes implement `NavKey` AND are `@Serializable`
- `rememberNavBackStack(config, startRoute)` → replaces `rememberNavController()`
- `NavDisplay(backStack, entryProvider = entryProvider { entry<Route> { Screen() } })` → replaces `NavHost`
- `backStack.add(Route)` → replaces `navController.navigate(Route)`
- `backStack.removeLastOrNull()` → replaces `navController.popBackStack()`

### Navigation 3: Route Definitions

```kotlin
// AppNavigation.kt
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute : NavKey

@Serializable
data object TodayRoute : AppRoute

@Serializable
data object HistoryRoute : AppRoute

@Serializable
data object ItemsRoute : AppRoute
```

### Navigation 3: SavedStateConfiguration (KMP REQUIRED)

On iOS/non-Android, Kotlin reflection is unavailable. You MUST register serializers explicitly:

```kotlin
import androidx.navigation3.runtime.savestate.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

private val navConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(TodayRoute::class, TodayRoute.serializer())
            subclass(HistoryRoute::class, HistoryRoute.serializer())
            subclass(ItemsRoute::class, ItemsRoute.serializer())
        }
    }
}
```

Add each new `AppRoute` subtype here when new routes are added in future stories.

### Navigation 3: AppNavigation Implementation

One `NavBackStack` per tab preserves each tab's navigation history independently:

```kotlin
@Composable
fun AppNavigation() {
    val todayBackStack = rememberNavBackStack(navConfig, TodayRoute)
    val historyBackStack = rememberNavBackStack(navConfig, HistoryRoute)
    val itemsBackStack = rememberNavBackStack(navConfig, ItemsRoute)
    var selectedTab: AppRoute by rememberSaveable { mutableStateOf(TodayRoute) }

    val currentBackStack = when (selectedTab) {
        TodayRoute -> todayBackStack
        HistoryRoute -> historyBackStack
        ItemsRoute -> itemsBackStack
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == TodayRoute,
                    onClick = { selectedTab = TodayRoute },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Today") }
                )
                NavigationBarItem(
                    selected = selectedTab == HistoryRoute,
                    onClick = { selectedTab = HistoryRoute },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                    label = { Text("History") }
                )
                NavigationBarItem(
                    selected = selectedTab == ItemsRoute,
                    onClick = { selectedTab = ItemsRoute },
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    label = { Text("Items") }
                )
            }
        }
    ) { innerPadding ->
        NavDisplay(
            backStack = currentBackStack,
            onBack = { currentBackStack.removeLastOrNull() },
            modifier = Modifier.padding(innerPadding),
            entryDecorators = listOf(
                rememberSceneSetupNavEntryDecorator(),
                rememberSavedStateNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<TodayRoute> { TodayScreen() }
                entry<HistoryRoute> { HistoryScreen() }
                entry<ItemsRoute> { ItemsScreen() }
            }
        )
    }
}
```

**Icon note:** `Icons.Default.DateRange` and `Icons.Default.List` may not be in Material Icons core. If compilation fails on icons, use `Icons.Default.Home`, `Icons.Default.Search`, `Icons.Default.Settings` as substitutes — icons are placeholder in this story. Do NOT add `material-icons-extended` dep; final icons are selected in Story 2.1.

**`rememberViewModelStoreNavEntryDecorator()`** — this is from `lifecycle-viewmodel-navigation3`. Verify if this is available from `androidx.lifecycle:lifecycle-viewmodel-compose` (already in deps) or if a separate artifact is needed. If it's missing, omit it for now and add in Story 2.1 when the first ViewModel is introduced.

### Imports for AppNavigation.kt

```kotlin
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavDisplay
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import androidx.navigation3.ui.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.rememberViewModelStoreNavEntryDecorator
import kotlinx.serialization.Serializable
```

**Note:** Import paths may shift depending on the final stable release. If IDE can't resolve, check the navigation3 artifact's actual package structure.

### Dusty Indigo Theme: Color.kt

```kotlin
package com.sonja.tracker.ui.theme

import androidx.compose.ui.graphics.Color

// Primary — muted/desaturated indigo, slightly warm-shifted
val DustyIndigoPrimary = Color(0xFF6B6B9E)
val DustyIndigoOnPrimary = Color(0xFFFFFFFF)
val DustyIndigoPrimaryContainer = Color(0xFFE3DFFF)  // soft indigo tint — used for logged rows
val DustyIndigoOnPrimaryContainer = Color(0xFF1D1B4F)

// Light mode surfaces — warm near-white
val SurfaceLight = Color(0xFFFCF9F6)
val BackgroundLight = Color(0xFFF9F6F2)
val OnSurfaceLight = Color(0xFF1C1A17)               // deep warm charcoal text

// Dark mode surfaces — deep warm charcoal
val SurfaceDark = Color(0xFF1E1C19)
val BackgroundDark = Color(0xFF1A1714)
val OnSurfaceDark = Color(0xFFEDE8E2)                // warm near-white text

// Dark primary variant
val DustyIndigoPrimaryDark = Color(0xFFA8A6D9)       // lighter for dark mode legibility
val DustyIndigoOnPrimaryDark = Color(0xFF2D2B60)
val DustyIndigoPrimaryContainerDark = Color(0xFF434179)
val DustyIndigoOnPrimaryContainerDark = Color(0xFFE3DFFF)

// Error / warning — muted amber (never red; avoids medical alarm association)
val ErrorLight = Color(0xFFB87A2C)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorDark = Color(0xFFE8B059)
val OnErrorDark = Color(0xFF3A1F00)
```

### Dusty Indigo Theme: Theme.kt

```kotlin
package com.sonja.tracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = DustyIndigoPrimary,
    onPrimary = DustyIndigoOnPrimary,
    primaryContainer = DustyIndigoPrimaryContainer,
    onPrimaryContainer = DustyIndigoOnPrimaryContainer,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    error = ErrorLight,
    onError = OnErrorLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = DustyIndigoPrimaryDark,
    onPrimary = DustyIndigoOnPrimaryDark,
    primaryContainer = DustyIndigoPrimaryContainerDark,
    onPrimaryContainer = DustyIndigoOnPrimaryContainerDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    error = ErrorDark,
    onError = OnErrorDark,
)

@Composable
fun TrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = TrackerTypography,
        content = content
    )
}
```

### Dusty Indigo Theme: Type.kt

System fonts (SF Pro on iOS, Roboto on Android) — no custom typeface:

```kotlin
package com.sonja.tracker.ui.theme

import androidx.compose.material3.Typography

// Material 3 default scale — system font, no custom typeface
// Key usage (per UX spec):
//   displaySmall  → "All done" hero moment (Story 3.4)
//   titleMedium   → item names
//   bodyMedium    → supporting text
//   labelSmall    → history state labels
val TrackerTypography = Typography()
```

### Updated App.kt

Replace the entire body of `App.kt` with:

```kotlin
package com.sonja.tracker

import androidx.compose.runtime.Composable
import com.sonja.tracker.ui.navigation.AppNavigation
import com.sonja.tracker.ui.theme.TrackerTheme

@Composable
fun App() {
    TrackerTheme {
        AppNavigation()
    }
}
```

Remove unused `@Preview` annotation and all wizard placeholder imports. `MainViewController.kt` (if present in iosMain) is untouched — it still calls `App()`.

### Placeholder Screen Implementation

Minimal placeholders — just enough to confirm navigation works. DO NOT add domain logic, ViewModels, or real UI:

```kotlin
// TodayScreen.kt
package com.sonja.tracker.ui.today

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun TodayScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Today")
    }
}
```

Replicate pattern for `HistoryScreen` and `ItemsScreen` with matching text labels.

### Project Structure Notes

New files created in this story:
```
shared/src/commonMain/kotlin/com/sonja/tracker/
  ui/
    navigation/
      AppNavigation.kt                          ← NEW
    theme/
      Color.kt                                  ← NEW
      Theme.kt                                  ← NEW
      Type.kt                                   ← NEW
    today/
      TodayScreen.kt                            ← NEW (placeholder)
    history/
      HistoryScreen.kt                          ← NEW (placeholder)
    items/
      ItemsScreen.kt                            ← NEW (placeholder)
  App.kt                                        ← MODIFIED (theme + navigation wired)
  Greeting.kt                                   ← DELETE (wizard placeholder, no longer used)

gradle/libs.versions.toml                       ← MODIFIED (nav3 coordinate fix + serialization plugin)
shared/build.gradle.kts                         ← MODIFIED (nav3 dep + serialization plugin)
```

**`Platform.kt`** — leave untouched, it defines the `getPlatform()` expect/actual. It is no longer referenced after `Greeting.kt` is deleted, but deleting it now causes noise; let it be removed in a future cleanup.

### What this story does NOT deliver

- No ViewModels — Story 2.1+ (koin-compose and koin-compose-viewmodel also added in Story 2.1)
- No real Today/History/Items screen UI — placeholders only
- No deep-link from YesterdayBanner to History with date arg — Story 5.3
- No `MaterialIcons.Extended` dependency — final icons selected per screen in Story 2.1+
- No `AppImageStorage` or `TimePicker.swift` — future stories
- No `NotificationInterface` — Epic 4

### Don't Break List (from Stories 1.1–1.3)

- `TrackerApplication.kt` and all Koin wiring — untouched
- `DatabaseDriverFactory` and SQLDelight schema/queries — untouched
- `AppPreferences` expect/actual — untouched
- `ItemRepository` and `LogRepository` stubs — untouched
- `SharedModule.kt`, `AndroidModule.kt`, `IosModule.kt` — untouched
- `iOSApp.swift` (calls `IosModuleKt.doInitKoin()`) — untouched
- `ContentView.swift` (calls `MainViewControllerKt.MainViewController()`) — untouched
- `MainActivity.kt` (calls `App()`) — untouched
- `shared/build.gradle.kts` existing deps (SQLDelight, Koin, Compose, Lifecycle) — additive only; never remove or change existing entries

### Learnings from Story 1.3

- **Kotlin 2.x `actual constructor` pattern**: Default arg values (`= null`) on `expect` only; omit from `actual` constructors.
- **iOS Koin double-init guard**: `initKoin()` checks `KoinPlatformTools.defaultContext().getOrNull() == null` before `startKoin`.
- **Build task for iOS KMP verification**: `./gradlew :shared:compileKotlinIosSimulatorArm64` — use this before full Xcode build.
- **No Koin-Compose deps yet**: `koin-compose` and `koin-compose-viewmodel` are NOT wired — they're added in Story 2.1.

### CONTRIBUTING.md Gotchas Relevant to This Story

- **Xcode cache after CMP changes**: If adding new Compose deps causes Xcode build failures, clear both Gradle build output (`rm -rf build/`) and DerivedData (`rm -rf ~/Library/Developer/Xcode/DerivedData/<ProjectName>*`) before debugging.
- **Java PATH in Xcode Build Phase**: If Xcode can't find Java after adding new deps, add `export JAVA_HOME=$(/usr/libexec/java_home)` to the `embedAndSignAppleFrameworkForXcode` Build Phase script.

### References

- [Source: epics.md#Story 1.4] — acceptance criteria and user story
- [Source: architecture.md#UI Architecture] — Navigation 3 (1.0.1), 3-tab structure, ViewModel/StateFlow pattern
- [Source: architecture.md#Complete Project Directory Structure] — exact file paths for all new files
- [Source: ux-design-specification.md#Color System] — Dusty Indigo direction, surface colours, error colour constraints
- [Source: ux-design-specification.md#Typography System] — system font, Material 3 type scale
- [Source: ux-design-specification.md#Navigation Patterns] — Today as launch destination, NavigationBar structure
- [Source: 1-3-koin-di-wiring-and-app-preferences.md] — don't-break list, Koin patterns established

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- **Navigation 3 KMP blocker**: `androidx.navigation3:navigation3-ui` (the real artifact on Google Maven) and the nonexistent `org.jetbrains.androidx.navigation3` group have no iOS klibs — Navigation 3 is Android-only as of April 2026. Implemented state-based tab navigation (`rememberSaveable { mutableStateOf(TodayRoute) }` + `when` in Scaffold) instead. Route objects remain `@Serializable sealed interface AppRoute` for a future library swap.
- **Material Icons dependency**: `Icons.Default.*` requires `material-icons-core` (not included via material3). Per story guidance that final icons are Story 2.1, used `Box(Modifier.size(24.dp))` placeholders.
- **Serialization plugin**: Added to `shared/build.gradle.kts` as specified.
- All three build targets passed: `:shared:assembleDebug`, `:androidApp:assembleDebug`, `:shared:compileKotlinIosSimulatorArm64`.
- Xcode build is a manual step for Sonja.

### File List

- `gradle/libs.versions.toml` — removed `navigation = "1.0.1"` and `navigation3-ui` library entry (unresolvable); added `kotlinSerialization` plugin entry
- `shared/build.gradle.kts` — added `kotlinSerialization` plugin; no navigation dep (see note above)
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/theme/Color.kt` — NEW: Dusty Indigo color tokens
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/theme/Type.kt` — NEW: TrackerTypography (Material 3 default scale)
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/theme/Theme.kt` — NEW: TrackerTheme composable
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/today/TodayScreen.kt` — NEW: placeholder
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/history/HistoryScreen.kt` — NEW: placeholder
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/ItemsScreen.kt` — NEW: placeholder
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/navigation/AppNavigation.kt` — NEW: 3-tab state-based navigation
- `shared/src/commonMain/kotlin/com/sonja/tracker/App.kt` — MODIFIED: TrackerTheme { AppNavigation() }
- `shared/src/commonMain/kotlin/com/sonja/tracker/Greeting.kt` — DELETED

### Review Findings

- [x] [Review][Patch] `rememberSaveable` has no custom Saver for `AppRoute` — fixed: ordinal `Saver<AppRoute, Int>` added; `AppRouteSaver` comment documents update requirement when new routes are added [AppNavigation.kt]
- [x] [Review][Patch] Story file task checkboxes all unchecked despite Status: done — fixed [1-4-navigation-scaffold-and-dusty-indigo-theme.md]
- [x] [Review][Defer] No back stack — hardware Back exits app from any tab — architectural limitation of state-based nav; addressed when Navigation 3 KMP support arrives [AppNavigation.kt]
- [x] [Review][Defer] `AppRoute` does not extend `NavKey` — future Navigation 3 swap will require another code change [AppNavigation.kt:25]
- [x] [Review][Defer] Koin init crash leaves global context partially constructed — pre-existing Story 1.3 concern; no try/catch in iOS `iOSApp.init()` [iOSApp.swift:7]
- [x] [Review][Defer] `NSApplicationSupportDirectory` path resolved but directory may not exist on fresh install — pre-existing Story 1.2 concern; SQLite open will fail [DatabaseDriverFactory.ios.kt]
