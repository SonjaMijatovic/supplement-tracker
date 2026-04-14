# Story 2.1: Items List Screen

Status: review

## Story

As a user,
I want to see all my configured supplements and medications in a list,
so that I have a clear overview of everything I'm tracking.

## Acceptance Criteria

1. **Given** the Items tab is tapped **When** no items have been added yet **Then** an empty state is displayed with a warm prompt ("Add your first item") and a filled button to add an item (button is non-functional in this story — wired in Story 2.2).

2. **Given** one or more items exist in the database **When** the Items screen is displayed **Then** all configured items are listed, each showing their name and a 40dp rounded-square icon/image placeholder (default placeholder icon when no icon/image is set).

3. **Given** the Items screen is loaded **When** items are fetched **Then** `ItemsViewModel` exposes a `StateFlow<ItemsUiState>` with `Loading`, `Success(items: List<Item>)`, and `Error(message: String)` sealed states; **And** the composable `when`-branches on the sealed state — no `if (isLoading)` flags.

4. **Given** a new item is added or deleted in a subsequent story **When** the database changes **Then** the list updates reactively via SQLDelight `Flow` without a manual refresh.

5. **Given** the Items screen shows items **When** rendered **Then** each `ItemRow` has a minimum touch target of 56dp height and 16dp horizontal screen margins.

## Tasks / Subtasks

- [x] Task 1: Add Koin Compose dependencies to `shared/build.gradle.kts` (AC: 3)
  - [x] Add `implementation(libs.koin.compose)` to `commonMain.dependencies`
  - [x] Add `implementation(libs.koin.compose.viewmodel)` to `commonMain.dependencies`
  - [x] Verify: `./gradlew :shared:assembleDebug` — BUILD SUCCESSFUL

- [x] Task 2: Fix iOS `Application Support` directory creation (deferred from Story 1.4)
  - [x] In `DatabaseDriverFactory.ios.kt`, call `NSFileManager.defaultManager.createDirectoryAtPath(supportDir, true, null, null)` (positional args — see Dev Notes) immediately before creating the `NativeSqliteDriver`
  - [x] Guard against empty list: replace `.first() as String` with `.firstOrNull() as? String ?: error("NSApplicationSupportDirectory not found")`
  - [x] Verify: `./gradlew :shared:compileKotlinIosSimulatorArm64` — BUILD SUCCESSFUL

- [x] Task 3: Add queries to `TrackerDatabase.sq` (AC: 2, 4)
  - [x] Append to `shared/src/commonMain/sqldelight/com/sonja/tracker/TrackerDatabase.sq`:
    ```sql
    selectAll:
    SELECT * FROM items;

    insertItem:
    INSERT INTO items (name, reminder_weekday_time, reminder_weekend_time, image_path, icon_id)
    VALUES (?, ?, ?, ?, ?);

    deleteItemById:
    DELETE FROM items WHERE id = ?;
    ```
  - [x] `insertItem` and `deleteItemById` are needed by `ItemRepositoryTest` (Task 12) and will be used by Story 2.2 — adding them now avoids backtracking
  - [x] Verify SQLDelight code generation: `./gradlew :shared:generateCommonMainTrackerDatabaseInterface`

- [x] Task 4: Create `Item` domain model (AC: 2, 3)
  - [x] Create `shared/src/commonMain/kotlin/com/sonja/tracker/domain/model/Item.kt`:
    ```kotlin
    package com.sonja.tracker.domain.model

    data class Item(
        val id: Long,
        val name: String,
        val reminderWeekdayTime: String?,
        val reminderWeekendTime: String?,
        val imagePath: String?,
        val iconId: String?
    )
    ```
  - [x] Column mapping mirrors SQLDelight `items` table exactly; `snake_case` in DB → `camelCase` in Kotlin

- [x] Task 5: Implement `ItemRepository.observeItems()` (AC: 2, 4)
  - [x] Update `shared/src/commonMain/kotlin/com/sonja/tracker/data/repository/ItemRepository.kt`:
    ```kotlin
    package com.sonja.tracker.data.repository

    import app.cash.sqldelight.coroutines.asFlow
    import app.cash.sqldelight.coroutines.mapToList
    import com.sonja.tracker.TrackerDatabase
    import com.sonja.tracker.domain.model.Item
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.flow.Flow
    import kotlinx.coroutines.flow.map

    class ItemRepository(private val database: TrackerDatabase) {
        fun observeItems(): Flow<List<Item>> =
            database.trackerDatabaseQueries.selectAll()
                .asFlow()
                .mapToList(Dispatchers.Default)
                .map { rows -> rows.map { row ->
                    Item(
                        id = row.id,
                        name = row.name,
                        reminderWeekdayTime = row.reminder_weekday_time,
                        reminderWeekendTime = row.reminder_weekend_time,
                        imagePath = row.image_path,
                        iconId = row.icon_id
                    )
                }}
    }
    ```
  - [x] Use `Dispatchers.Default` for `mapToList` (off main thread)
  - [x] The SQLDelight generated queries object is `database.trackerDatabaseQueries`

- [x] Task 6: Create `ItemsUiState.kt` sealed class (AC: 3)
  - [x] Create `shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/ItemsUiState.kt`:
    ```kotlin
    package com.sonja.tracker.ui.items

    import com.sonja.tracker.domain.model.Item

    sealed class ItemsUiState {
        object Loading : ItemsUiState()
        data class Success(val items: List<Item>) : ItemsUiState()
        data class Error(val message: String) : ItemsUiState()
    }
    ```

- [x] Task 7: Create `ItemsViewModel.kt` (AC: 3, 4)
  - [x] Create `shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/ItemsViewModel.kt`:
    ```kotlin
    package com.sonja.tracker.ui.items

    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.viewModelScope
    import com.sonja.tracker.data.repository.ItemRepository
    import kotlinx.coroutines.flow.SharingStarted
    import kotlinx.coroutines.flow.StateFlow
    import kotlinx.coroutines.flow.catch
    import kotlinx.coroutines.flow.map
    import kotlinx.coroutines.flow.stateIn

    class ItemsViewModel(private val repository: ItemRepository) : ViewModel() {
        val uiState: StateFlow<ItemsUiState> = repository
            .observeItems()
            .map { items -> ItemsUiState.Success(items) }
            .catch { e -> emit(ItemsUiState.Error(e.message ?: "Unknown error")) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = ItemsUiState.Loading
            )
    }
    ```

- [x] Task 8: Register `ItemsViewModel` in Koin (AC: 3)
  - [x] Update `shared/src/commonMain/kotlin/com/sonja/tracker/di/SharedModule.kt`:
    - Add `import org.koin.compose.viewmodel.dsl.viewModel` (koin-compose-viewmodel API)
    - Add `viewModel { ItemsViewModel(get()) }` to the `module { }` block
  - [x] Do NOT add `viewModel {}` declarations to `androidModule.kt` or `iosModule.kt` — the ViewModel depends only on commonMain code

- [x] Task 9: Implement `ItemsScreen.kt` (AC: 1, 2, 3, 5)
  - [x] Replace placeholder in `shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/ItemsScreen.kt` with full implementation (see Dev Notes for composable structure)
  - [x] Use `koinViewModel<ItemsViewModel>()` to obtain the ViewModel
  - [x] Collect `uiState` with `collectAsState()`; `when`-branch on sealed type
  - [x] Empty state: warm text prompt + `FilledButton` (non-functional in this story; `onClick = {}`)
  - [x] Loading state: centered `CircularProgressIndicator()`
  - [x] Error state: centered error `Text`
  - [x] Success with items: `LazyColumn` with `ItemRow` composables

- [x] Task 10: Create `ItemRow` composable in components package (AC: 2, 5)
  - [x] Create `shared/src/commonMain/kotlin/com/sonja/tracker/ui/components/ItemRow.kt`
  - [x] This story only needs the **list-view** variant (name + icon placeholder; no tap logging)
  - [x] Full `ItemRow` with pending/logged/disabled states is built in Story 3.2; keep this implementation minimal for Story 2.1 needs
  - [x] Min height: 56dp; 16dp horizontal padding; 40dp rounded-square thumbnail; see Dev Notes

- [x] Task 11: Update nav icons in `AppNavigation.kt` (Story 1.4 deferral)
  - [x] Replace `Box(Modifier.size(24.dp))` placeholder icons with real `Icons.Default.*`:
    - Today tab: `Icons.Default.Home`
    - History tab: `Icons.Default.DateRange`  
    - Items tab: `Icons.Default.List`
  - [x] Add `import androidx.compose.material.icons.Icons` and `import androidx.compose.material.icons.filled.*`
  - [x] If any icon fails compilation (not in core set), use `Icons.Default.Menu` as fallback — do NOT add `material-icons-extended` dep yet (reserved for Story 2.5 IconPickerGrid)
  - [x] Verify: `./gradlew :androidApp:assembleDebug` — BUILD SUCCESSFUL

- [x] Task 12: Write `ItemRepositoryTest` (AC: 4 — reactive Flow)
  - [x] Create `shared/src/commonTest/kotlin/com/sonja/tracker/data/repository/ItemRepositoryTest.kt`
  - [x] Use in-memory `JdbcSqliteDriver` (from `libs.sqldelight.sqlite.driver`, already in `commonTest.dependencies`)
  - [x] Use `database.trackerDatabaseQueries.insertItem(...)` and `deleteItemById(...)` (added in Task 3) — do NOT use raw SQL
  - [x] Test 1: call `insertItem` → `observeItems()` emits a list containing the new item
  - [x] Test 2: call `insertItem` then `deleteItemById` → `observeItems()` emits an empty list
  - [x] See Dev Notes for in-memory driver setup and Flow collection pattern

- [x] Task 13: Verify builds pass (AC: all)
  - [x] `./gradlew :shared:assembleDebug` — BUILD SUCCESSFUL
  - [x] `./gradlew :androidApp:assembleDebug` — BUILD SUCCESSFUL
  - [x] `./gradlew :shared:testDebugUnitTest` — BUILD SUCCESSFUL (ItemRepositoryTest passes)
  - [x] `./gradlew :shared:compileKotlinIosSimulatorArm64` — BUILD SUCCESSFUL

---

## Dev Notes

### Critical: koin-compose-viewmodel Import Path (Koin 3.5 KMP)

In Koin 3.5 for KMP, the ViewModel registration DSL is in `koin-compose-viewmodel`, **not** `koin-core`. The correct import for the module DSL is:
```kotlin
import org.koin.compose.viewmodel.dsl.viewModel
```

In composables, obtain the ViewModel with:
```kotlin
import org.koin.compose.viewmodel.koinViewModel
// ...
val viewModel = koinViewModel<ItemsViewModel>()
```

Do NOT use `org.koin.androidx.viewmodel.dsl.viewModel` (Android-only, won't compile in commonMain).

### SQLDelight Generated Queries Object Name

The SQLDelight plugin generates a queries class named after the database: `TrackerDatabase` → `TrackerDatabaseQueries`. Access via:
```kotlin
database.trackerDatabaseQueries.selectAll()
```

If the generated accessor is named differently (e.g., `database.itemsQueries`), check the build output. The accessor name is determined by the `.sq` file name + `Queries` suffix or the database name.

### `ItemRepository` — `asFlow()` + `mapToList()` Imports

Both extension functions are in `app.cash.sqldelight:coroutines-extensions` (already in `commonMain.dependencies`):
```kotlin
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
```

### `ItemsScreen.kt` Composable Structure

```kotlin
package com.sonja.tracker.ui.items

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sonja.tracker.ui.components.ItemRow
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ItemsScreen(modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<ItemsViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is ItemsUiState.Loading -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is ItemsUiState.Error -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.message)
            }
        }
        is ItemsUiState.Success -> {
            if (state.items.isEmpty()) {
                // Empty state
                Column(
                    modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Add your first item",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { /* wired in Story 2.2 */ }) {
                        Text("Add item")
                    }
                }
            } else {
                LazyColumn(
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(state.items, key = { it.id }) { item ->
                        ItemRow(item = item)
                    }
                }
            }
        }
    }
}
```

### `ItemRow.kt` — List-View Variant (Story 2.1 Scope)

Story 2.1 only needs a display-only `ItemRow` (no tap handler, no pending/logged states). The full interactive `ItemRow` with 3 states (pending/logged/disabled) is built in Story 3.2. Keep this minimal:

```kotlin
package com.sonja.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sonja.tracker.domain.model.Item

@Composable
fun ItemRow(
    item: Item,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 40dp rounded-square thumbnail placeholder
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .semantics { contentDescription = "Item icon" },
            contentAlignment = Alignment.Center
        ) {
            // Icon/image rendering: Story 2.5 (icon picker) + Story 2.6 (photos)
            // Placeholder box for now
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = item.name,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
    }
}
```

**Note:** Do NOT add `Role.Checkbox` semantic or tap affordances here — those belong to the Today screen `ItemRow` states in Story 3.2. The Items list variant is read-only navigation into edit.

### In-Memory SQLDelight Driver for Tests

```kotlin
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sonja.tracker.TrackerDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ItemRepositoryTest {

    private fun createDriver(): JdbcSqliteDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TrackerDatabase.Schema.create(driver)
        return driver
    }

    @Test
    fun observeItems_emitsInsertedItem() = runTest {
        val db = TrackerDatabase(createDriver())
        val repo = ItemRepository(db)

        db.trackerDatabaseQueries.insertItem("Vitamin D3", "08:00", null, null, null)

        val items = repo.observeItems().first()
        assertEquals(1, items.size)
        assertEquals("Vitamin D3", items[0].name)
    }

    @Test
    fun observeItems_emitsEmptyAfterDelete() = runTest {
        val db = TrackerDatabase(createDriver())
        val repo = ItemRepository(db)

        db.trackerDatabaseQueries.insertItem("Magnesium", "21:00", null, null, null)
        val inserted = db.trackerDatabaseQueries.selectAll().executeAsList()
        db.trackerDatabaseQueries.deleteItemById(inserted[0].id)

        val items = repo.observeItems().first()
        assertEquals(0, items.size)
    }
}
```

**Note:** These tests use `.first()` (snapshot, not reactive subscription) because `JdbcSqliteDriver` in-memory does not emit reactive `Flow` updates across transactions in the test environment. Reactive subscription testing requires a real file-based driver. The tests here verify correct query execution and mapping — sufficient for this story.

`app.cash.sqldelight:sqlite-driver` is already in `commonTest.dependencies` as `libs.sqldelight.sqlite.driver`. Add `kotlinx-coroutines-test` if not already present — check `commonTest.dependencies`; if missing, add `implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${libs.versions.coroutines.get()}")` to `commonTest.dependencies`.

### iOS `DatabaseDriverFactory` Fix (Deferred from Story 1.4)

Replace the body of `createDriver()` in `DatabaseDriverFactory.ios.kt`:

```kotlin
actual fun createDriver(): SqlDriver {
    val paths = NSSearchPathForDirectoriesInDomains(
        NSApplicationSupportDirectory,
        NSUserDomainMask,
        true
    )
    val supportDir = paths.firstOrNull() as? String
        ?: error("NSApplicationSupportDirectory not found")

    // Ensure directory exists before SQLite open (may not exist on fresh install)
    // IMPORTANT: Use positional arguments — Kotlin/Native ObjC interop does not expose
    // ObjC selector labels as Kotlin named parameters for this method.
    NSFileManager.defaultManager.createDirectoryAtPath(
        supportDir,   // path
        true,         // withIntermediateDirectories
        null,         // attributes
        null          // error — ignored; SQLite open will surface any failure
    )

    return NativeSqliteDriver(
        schema = TrackerDatabase.Schema,
        name = "$supportDir/tracker.db"
    )
}
```

Additional import needed: `import platform.Foundation.NSFileManager`

### ViewModel Lifecycle in KMP (koin-compose-viewmodel 3.5)

`koinViewModel<T>()` in Compose Multiplatform uses the same lifecycle-viewmodel scope as the Android `ViewModel`. On iOS, CMP's `lifecycle-viewmodel-compose` (already in deps as `libs.androidx.lifecycle.viewmodelCompose`) manages the lifecycle. No additional platform setup needed.

### New Files Created in This Story

```
shared/src/commonMain/kotlin/com/sonja/tracker/
  domain/
    model/
      Item.kt                                     ← NEW
  ui/
    items/
      ItemsUiState.kt                             ← NEW
      ItemsViewModel.kt                           ← NEW
      ItemsScreen.kt                              ← REPLACE placeholder
    components/
      ItemRow.kt                                  ← NEW (list-view variant)

shared/src/commonTest/kotlin/com/sonja/tracker/
  data/repository/
    ItemRepositoryTest.kt                         ← NEW
```

### Modified Files

```
shared/build.gradle.kts                           ← add koin-compose + koin-compose-viewmodel
shared/src/commonMain/sqldelight/.../TrackerDatabase.sq  ← add selectAll query
shared/src/commonMain/kotlin/.../data/repository/ItemRepository.kt  ← implement observeItems()
shared/src/commonMain/kotlin/.../di/SharedModule.kt  ← add viewModel { ItemsViewModel(get()) }
shared/src/iosMain/kotlin/.../data/db/DatabaseDriverFactory.ios.kt  ← directory creation fix
shared/src/commonMain/kotlin/.../ui/navigation/AppNavigation.kt  ← replace icon placeholders
```

### What This Story Does NOT Deliver

- No add/edit/delete item functionality — Story 2.2+
- No icon picker — Story 2.5
- No camera/gallery photo — Story 2.6
- No `ItemRow` tap handler or pending/logged states — Story 3.2
- No navigation from Items list into `ItemEditSheet` — Story 2.2
- No `material-icons-extended` dependency — reserved for Story 2.5 (IconPickerGrid)
- No `AppImageStorage` — Story 2.6

### Don't Break List (from Stories 1.1–1.4)

- `TrackerApplication.kt` and all Koin wiring — **additive only** to `sharedModule`; never remove existing `single {}` bindings
- `DatabaseDriverFactory` expect/actual — only the iOS actual is modified (directory fix); Android actual untouched
- `AppPreferences` expect/actual — untouched
- `LogRepository` — untouched (empty stub is fine)
- `iOSApp.swift` (calls `IosModuleKt.doInitKoin()`) — untouched
- `ContentView.swift` and `MainActivity.kt` — untouched
- `TrackerTheme`, `Color.kt`, `Type.kt` — untouched
- `AppNavigation.kt` — additive icon update only; `AppRouteSaver`, route definitions, and navigation logic untouched
- `TodayScreen.kt`, `HistoryScreen.kt` — untouched placeholders

### Learnings from Story 1.4 Relevant to This Story

- **No Navigation 3 library**: Navigation is state-based (`rememberSaveable { mutableStateOf(TodayRoute) }` + `when`). Do NOT attempt to use `NavController`, `NavHost`, or `NavDisplay` — the library is not wired.
- **Material icons**: `Icons.Default.Home` etc. are available from `compose-material3` without `material-icons-extended`. If an icon is unavailable, the build will fail with an unresolved reference — use `Icons.Default.Menu` as fallback.
- **`koin-compose` and `koin-compose-viewmodel` NOT yet wired** — this story adds them; see Task 1.
- **CMP version is actually 1.10.3** (not 1.10.0 as in the original plan) — the wizard generated 1.10.3; `material3` tracks separately at `1.10.0-alpha05`. Do not change these.
- **`-Xexpect-actual-classes` compiler flag** already in `shared/build.gradle.kts` — required for expect/actual class declarations.

### References

- [Source: epics.md#Story 2.1] — acceptance criteria and user story
- [Source: architecture.md#Data Architecture] — SQLDelight Flow pattern, ViewModel/StateFlow
- [Source: architecture.md#State Management Patterns] — sealed UiState pattern with code examples
- [Source: architecture.md#Structure Patterns] — exact file locations for all new files
- [Source: ux-design-specification.md#Custom Components → ItemRow] — 56dp height, 40dp thumbnail
- [Source: ux-design-specification.md#Spacing & Layout] — 16dp horizontal margins
- [Source: 1-4-navigation-scaffold-and-dusty-indigo-theme.md#Completion Notes] — Nav3 KMP blocker, icon placeholder details, koin-compose deferral
- [Source: deferred-work.md] — iOS Application Support directory fix, TrackerDatabase driver error

---

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List

### Review Findings
