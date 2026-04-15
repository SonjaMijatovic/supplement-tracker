# Story 2.4: Edit & Delete Item

Status: ready-for-dev

## Story

As a user,
I want to edit an existing item's name and reminder times, or delete it,
so that I can keep my list accurate as my routine changes.

## Acceptance Criteria

1. **Given** items exist in the Items list **When** the user taps an item row **Then** the `ItemEditSheet` opens in edit mode with the name, weekday time, and weekend time pre-filled from the stored values (weekend toggle pre-expanded if `reminderWeekendTime` is non-null).

2. **Given** the `ItemEditSheet` is in edit mode **When** the user changes the name and/or reminder times and taps Save **Then** the item is updated in the `items` table and the list reflects the change immediately via the existing `observeItems()` Flow.

3. **Given** the `ItemEditSheet` is in edit mode **When** the user taps the Delete button (amber `TextButton`) **Then** an `AlertDialog` appears with the item name in the title (e.g. "Delete Vitamin D3?") and "Delete" (amber) + "Cancel" actions.

4. **Given** the delete confirmation dialog is shown **When** the user taps Delete **Then** the item is removed from the `items` table and disappears from the Items list.

5. **Given** the delete confirmation dialog is shown **When** the user taps Cancel **Then** the dialog dismisses and the `ItemEditSheet` remains open with no changes made.

6. **Given** an item is deleted **When** deletion completes **Then** notification rescheduling/cancellation is triggered for that item (stub comment acceptable; full implementation in Epic 4).

## Tasks / Subtasks

- [ ] Task 1: Add `updateItem` SQL query to `TrackerDatabase.sq` (AC: 2)
  - [ ] Add the `updateItem` named query below `insertItem`:
    ```sql
    updateItem:
    UPDATE items
    SET name = ?, reminder_weekday_time = ?, reminder_weekend_time = ?, image_path = ?, icon_id = ?
    WHERE id = ?;
    ```
  - [ ] No other SQL changes — `deleteItemById` already exists from Story 1.2

- [ ] Task 2: Add `updateItem()` and `deleteItem()` to `ItemRepository` (AC: 2, 4)
  - [ ] Add `suspend fun updateItem(id: Long, name: String, weekdayTime: String, weekendTime: String?, imagePath: String?, iconId: String?)` — see Dev Notes for exact implementation
  - [ ] Add `suspend fun deleteItem(id: Long)` — wraps `trackerDatabaseQueries.deleteItemById(id)` in `withContext(Dispatchers.Default)`

- [ ] Task 3: Add `editItem()` and `deleteItem()` to `ItemsViewModel` (AC: 2, 4, 6)
  - [ ] Add `fun editItem(id: Long, name: String, weekdayTime: String, weekendTime: String?)` — reads current `imagePath`/`iconId` from `uiState` to preserve them; see Dev Notes
  - [ ] Add `fun deleteItem(id: Long)` — calls `repository.deleteItem(id)` in `viewModelScope.launch` with Epic 4 stub comment

- [ ] Task 4: Update `ItemEditSheet` to support edit mode (AC: 1, 2, 3, 4, 5)
  - [ ] Add `initialName: String = ""` parameter
  - [ ] Add `initialWeekdayTime: String = "08:00"` parameter
  - [ ] Add `onDelete: (() -> Unit)? = null` parameter — non-null signals edit mode
  - [ ] Add `var showDeleteConfirm by remember { mutableStateOf(false) }` at top of composable
  - [ ] Update `name` state init: `var name by remember { mutableStateOf(initialName) }`
  - [ ] Update `weekdayTime` state init: `var weekdayTime by remember { mutableStateOf(initialWeekdayTime) }`
  - [ ] Change title: `if (onDelete != null) "Edit item" else "Add item"`
  - [ ] Add Delete button after Save button in the `else` branch (visible only when `onDelete != null`) — see Dev Notes for exact code
  - [ ] Add `AlertDialog` for delete confirmation after the `ModalBottomSheet` block — see Dev Notes for exact code
  - [ ] Verify: `./gradlew :shared:compileKotlinIosSimulatorArm64` — BUILD SUCCESSFUL
  - [ ] Verify: `./gradlew :shared:assembleDebug` — BUILD SUCCESSFUL

- [ ] Task 5: Update `ItemRow` to accept an optional `onClick` (AC: 1)
  - [ ] Add `onClick: (() -> Unit)? = null` parameter to `ItemRow`
  - [ ] Apply `Modifier.clickable { onClick() }.semantics { role = Role.Button; contentDescription = "${item.name}, tap to edit" }` when `onClick != null` — see Dev Notes for exact pattern
  - [ ] Add new imports to `ItemRow.kt`: `androidx.compose.foundation.clickable`, `androidx.compose.ui.semantics.Role`, `androidx.compose.ui.semantics.role`, `androidx.compose.ui.semantics.semantics`

- [ ] Task 6: Update `ItemsScreen` to wire edit/delete (AC: 1, 2, 3, 4, 5, 6)
  - [ ] Add `var selectedItem by remember { mutableStateOf<Item?>(null) }` state variable
  - [ ] Add `import com.sonja.tracker.domain.model.Item`
  - [ ] Pass `onClick = { selectedItem = item }` to each `ItemRow` in `LazyColumn`
  - [ ] Add edit sheet invocation after the existing add sheet block — see Dev Notes for exact code

- [ ] Task 7: Add `ItemRepositoryTest` additions (AC: 2, 4)
  - [ ] Add `updateItem_updatesNameAndTimes` test — see Dev Notes for exact pattern
  - [ ] Add `deleteItem_removesItemFromDb` test — see Dev Notes for exact pattern
  - [ ] Run `./gradlew :shared:testDebugUnitTest` — all tests pass

- [ ] Task 8: Clean up `deferred-work.md`
  - [ ] Remove the entry: *"initialWeekendTime scaffolded without full edit-mode param set — Story 2.4 adds initialName, initialWeekdayTime"*
  - [ ] Remove the entry: *"Collapsing weekend toggle in edit mode gives no UX warning — Story 2.4's concern"*
  - [ ] Add a new entry for any items this story defers (e.g. composable UI test for weekend toggle pre-expansion — Compose UI test infrastructure not yet set up)

- [ ] Task 9: Final build verification (AC: all)
  - [ ] `./gradlew :shared:testDebugUnitTest` — BUILD SUCCESSFUL
  - [ ] `./gradlew :shared:assembleDebug` — BUILD SUCCESSFUL
  - [ ] `./gradlew :androidApp:assembleDebug` — BUILD SUCCESSFUL
  - [ ] `./gradlew :shared:compileKotlinIosSimulatorArm64` — BUILD SUCCESSFUL

**Files to modify for Task 8:**
`_bmad-output/implementation-artifacts/deferred-work.md`

---

## Dev Notes

### SQL: `updateItem` Query

The parameter order in SQLDelight named queries must match the `SET` clause order, with `id` last (`WHERE id = ?`):

```sql
updateItem:
UPDATE items
SET name = ?, reminder_weekday_time = ?, reminder_weekend_time = ?, image_path = ?, icon_id = ?
WHERE id = ?;
```

`deleteItemById` already exists — do not add it again.

### SQLDelight Named Parameters — Required

Always call `updateItem` using **named parameters** in Kotlin (as shown below). The positional `?` order in the SQL is `name, reminder_weekday_time, reminder_weekend_time, image_path, icon_id, id` — using named params prevents silent ordering bugs if the query is ever modified.

### Task 2: `ItemRepository` New Methods

```kotlin
suspend fun updateItem(
    id: Long,
    name: String,
    weekdayTime: String,
    weekendTime: String?,
    imagePath: String?,
    iconId: String?
) {
    withContext(Dispatchers.Default) {
        database.trackerDatabaseQueries.updateItem(
            name = name,
            reminder_weekday_time = weekdayTime,
            reminder_weekend_time = weekendTime,
            image_path = imagePath,
            icon_id = iconId,
            id = id
        )
    }
}

suspend fun deleteItem(id: Long) {
    withContext(Dispatchers.Default) {
        database.trackerDatabaseQueries.deleteItemById(id)
    }
}
```

### Task 3: `ItemsViewModel` New Methods

Read `imagePath`/`iconId` from current `uiState` to avoid requiring the caller to track them (they're always `null` in Stories 2.1–2.4; Stories 2.5/2.6 will not need to change this ViewModel method):

```kotlin
fun editItem(id: Long, name: String, weekdayTime: String, weekendTime: String?) {
    viewModelScope.launch {
        val current = (uiState.value as? ItemsUiState.Success)?.items?.find { it.id == id }
        repository.updateItem(
            id = id,
            name = name,
            weekdayTime = weekdayTime,
            weekendTime = weekendTime,
            imagePath = current?.imagePath,
            iconId = current?.iconId
        )
        // TODO Epic 4: NotificationScheduler.rescheduleForSlot(weekdayTime, weekendTime)
    }
}

fun deleteItem(id: Long) {
    viewModelScope.launch {
        repository.deleteItem(id)
        // TODO Epic 4: NotificationScheduler.cancelForItem(id)
    }
}
```

### Task 4: `ItemEditSheet` — Full Updated Signature

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditSheet(
    onSave: (name: String, weekdayTime: String, weekendTime: String?) -> Unit,
    onDismiss: () -> Unit,
    initialName: String = "",
    initialWeekdayTime: String = "08:00",
    initialWeekendTime: String? = null,
    onDelete: (() -> Unit)? = null
)
```

**State initialisation changes (top of composable):**
```kotlin
var name by remember { mutableStateOf(initialName) }          // was: mutableStateOf("")
var weekdayTime by remember { mutableStateOf(initialWeekdayTime) }  // was: mutableStateOf("08:00")
var showDeleteConfirm by remember { mutableStateOf(false) }   // NEW
```

`weekendTime` and `weekendToggleExpanded` are unchanged — they already use `initialWeekendTime`.

**Title change (inside Column, first item):**
```kotlin
Text(
    text = if (onDelete != null) "Edit item" else "Add item",
    style = MaterialTheme.typography.titleLarge
)
```

**Delete button — add after Save button, inside `else` branch of the `when` block, inside Column:**
```kotlin
if (onDelete != null) {
    TextButton(
        onClick = { showDeleteConfirm = true },
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.error
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Delete item")
    }
}
```

**AlertDialog — add AFTER the `ModalBottomSheet { ... }` block, before `LaunchedEffect(Unit)` for focus:**
```kotlin
if (showDeleteConfirm) {
    AlertDialog(
        onDismissRequest = { showDeleteConfirm = false },
        title = { Text("Delete $initialName?") },
        confirmButton = {
            TextButton(
                onClick = {
                    onDelete?.invoke()
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = { showDeleteConfirm = false }) {
                Text("Cancel")
            }
        }
    )
}
```

**New imports needed in `ItemEditSheet.kt`:**
```kotlin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
```

**Note:** Do NOT place `AlertDialog` inside the Column or inside the `when` block. It must be at the top level of the composable (after `ModalBottomSheet`) so it renders as an overlay. Time pickers are inline by necessity (iOS UIWindow conflict); `AlertDialog` does not have that constraint.

### Task 5: `ItemRow` — `onClick` Pattern

```kotlin
@Composable
fun ItemRow(
    item: Item,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val clickModifier = if (onClick != null) {
        Modifier
            .clickable { onClick() }
            .semantics {
                role = Role.Button
                contentDescription = "${item.name}, tap to edit"
            }
    } else Modifier

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(vertical = 4.dp)
            .then(clickModifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ... unchanged content
    }
}
```

New imports needed in `ItemRow.kt` (none of these are currently present — add all):
```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
```

### Task 6: `ItemsScreen` — Edit Sheet Wiring

Add `selectedItem` state and edit sheet block. The add sheet block is unchanged:

```kotlin
var selectedItem by remember { mutableStateOf<Item?>(null) }

// In LazyColumn items block — pass onClick:
items(state.items, key = { it.id }) { item ->
    ItemRow(item = item, onClick = { selectedItem = item })
}

// After the existing add sheet block (after `if (showAddSheet) { ... }`):
selectedItem?.let { item ->
    ItemEditSheet(
        initialName = item.name,
        initialWeekdayTime = item.reminderWeekdayTime ?: "08:00",
        initialWeekendTime = item.reminderWeekendTime,
        onSave = { name, weekdayTime, weekendTime ->
            viewModel.editItem(item.id, name, weekdayTime, weekendTime)
        },
        onDismiss = { selectedItem = null },
        onDelete = { viewModel.deleteItem(item.id) }
    )
}
```

Add import: `import com.sonja.tracker.domain.model.Item`

### Task 7: Test Patterns

```kotlin
@Test
fun updateItem_updatesNameAndTimes() = runTest {
    val db = TrackerDatabase(createDriver())
    val repo = ItemRepository(db)

    repo.addItem("Vitamin C", "09:00", null)
    val id = repo.observeItems().first()[0].id

    repo.updateItem(id, "Magnesium Glycinate", "21:00", "22:00", null, null)

    val items = repo.observeItems().first()
    assertEquals(1, items.size)
    assertEquals("Magnesium Glycinate", items[0].name)
    assertEquals("21:00", items[0].reminderWeekdayTime)
    assertEquals("22:00", items[0].reminderWeekendTime)
}

@Test
fun deleteItem_removesItemFromDb() = runTest {
    val db = TrackerDatabase(createDriver())
    val repo = ItemRepository(db)

    repo.addItem("Vitamin D3", "08:00", null)
    val id = repo.observeItems().first()[0].id

    repo.deleteItem(id)

    assertEquals(0, repo.observeItems().first().size)
}
```

Note: Existing test `observeItems_emitsEmptyAfterDelete` calls `deleteItemById` directly on SQLDelight — keep it as-is. The new `deleteItem_removesItemFromDb` test exercises the repository method.

### Collapsing Weekend Toggle in Edit Mode

Collapsing the weekend toggle when editing and saving will store `NULL` for `reminder_weekend_time`, removing the previously set weekend time. No UX warning is shown — this is intentional for v1 simplicity. The `deferred-work.md` item from Story 2.3 review can be resolved as "accepted for v1, no warning needed".

### Notification Stub Placement

Do not add any `NotificationScheduler` logic — the class does not exist yet. Add only the two `// TODO Epic 4` comments as specified in Task 3. This satisfies AC6 ("stub call acceptable").

### Don't Break List (from Stories 2.1–2.3)

- `ItemRepository.addItem()` — do NOT modify
- `ItemRepository.observeItems()` — do NOT modify
- `ItemsViewModel.addItem()` and `uiState` StateFlow — do NOT modify
- `ItemsViewModel` constructor and Koin wiring — do NOT modify; pattern is `viewModel { ItemsViewModel(repository) }` (NOT `koinViewModel()`)
- `PlatformTimePickerDialog` expect/actual — do NOT modify
- `DatabaseDriverFactory.ios.kt` — do NOT touch
- `TrackerDatabase.sq` — add only `updateItem`; do NOT modify existing queries
- `SharedModule.kt`, `AndroidModule.kt`, `IosModule.kt` — do NOT modify
- `AppNavigation.kt` and `LocalHideNavBar` — do NOT modify
- `ItemEditSheet` `showTimePicker`/`showWeekendTimePicker` inline pattern — preserve exactly

### Files to Create

None.

### Files to Modify

```
shared/src/commonMain/sqldelight/com/sonja/tracker/TrackerDatabase.sq
shared/src/commonMain/kotlin/com/sonja/tracker/data/repository/ItemRepository.kt
shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/ItemsViewModel.kt
shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/ItemEditSheet.kt
shared/src/commonMain/kotlin/com/sonja/tracker/ui/components/ItemRow.kt
shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/ItemsScreen.kt
shared/src/commonTest/kotlin/com/sonja/tracker/data/repository/ItemRepositoryTest.kt
_bmad-output/implementation-artifacts/deferred-work.md
```

---

## Dev Agent Record

### Agent Model Used

_to be filled in by dev agent_

### Debug Log References

_to be filled in by dev agent_

### Completion Notes List

_to be filled in by dev agent_

### File List

_to be filled in by dev agent_

### Change Log

_to be filled in by dev agent_
