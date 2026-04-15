# Story 2.3: Weekend Reminder Time

Status: done

## Story

As a user,
I want to set a different reminder time for weekends,
so that my weekend routine (which often starts later) gets the right reminder.

## Acceptance Criteria

1. **Given** the `ItemEditSheet` is open **When** the sheet is displayed **Then** a "Different time on weekends?" toggle row is visible below the weekday time row, collapsed by default.

2. **Given** the toggle row is tapped **When** it expands **Then** a weekend time row appears with its own native OS time picker **And** it defaults to the current weekday time value.

3. **Given** the weekend toggle is expanded and a time is selected **When** the user taps Save **Then** `reminder_weekend_time` is stored as `HH:mm` TEXT in the `items` table.

4. **Given** the weekend toggle is not expanded (collapsed) **When** the user taps Save **Then** `reminder_weekend_time` is stored as `NULL` in the `items` table.

5. **Given** an item with a stored weekend time is opened for editing (Story 2.4) **When** `ItemEditSheet` opens with `initialWeekendTime` set **Then** the weekend toggle is pre-expanded showing the stored weekend time. *(This AC verifies the parameter scaffolding exists in Story 2.3; full edit-mode wiring — passing a stored value to `initialWeekendTime` — is Story 2.4's responsibility.)*

## Tasks / Subtasks

- [x] Task 1: Update `ItemRepository.addItem()` to accept `weekendTime: String?` (AC: 3, 4)
  - [x] Change signature to `suspend fun addItem(name: String, weekdayTime: String, weekendTime: String?)`
  - [x] Pass `reminder_weekend_time = weekendTime` to `insertItem` (was hardcoded `null`)
  - [x] No SQL changes — `reminder_weekend_time TEXT` column and `insertItem` query already exist

- [x] Task 2: Update `ItemsViewModel.addItem()` to accept `weekendTime: String?` (AC: 3, 4)
  - [x] Change signature to `fun addItem(name: String, weekdayTime: String, weekendTime: String?)`
  - [x] Pass `weekendTime` through to `repository.addItem(name, weekdayTime, weekendTime)`

- [x] Task 3: Update `ItemEditSheet` to add the weekend toggle (AC: 1, 2, 3, 4, 5)
  - [x] Add `initialWeekendTime: String? = null` parameter to `ItemEditSheet` (enables pre-fill for Story 2.4 edit mode)
  - [x] Add state: `var weekendTime by remember { mutableStateOf(initialWeekendTime) }`
  - [x] Add state: `var weekendToggleExpanded by remember { mutableStateOf(initialWeekendTime != null) }`
  - [x] Add state: `var showWeekendTimePicker by remember { mutableStateOf(false) }`
  - [x] Add toggle `ListItem` below the weekday `HorizontalDivider`:
    - `headlineContent = { Text("Different time on weekends?") }`
    - `trailingContent = { Checkbox(checked = weekendToggleExpanded, onCheckedChange = null) }`
    - `modifier = Modifier.clickable { ... }.semantics { role = Role.Checkbox }` — see Dev Notes for exact toggle logic (Role.Checkbox required per UX-DR14; add `import androidx.compose.ui.semantics.semantics` and `import androidx.compose.ui.semantics.Role`)
  - [x] Conditionally show weekend time `ListItem` (same pattern as weekday row) when `weekendToggleExpanded`
  - [x] Show `PlatformTimePickerDialog` for weekend time when `showWeekendTimePicker = true`
  - [x] Change `onSave` signature to `(name: String, weekdayTime: String, weekendTime: String?) -> Unit`
  - [x] On Save click: pass `if (weekendToggleExpanded) weekendTime else null` as third argument
  - [x] Verify: `./gradlew :shared:compileKotlinIosSimulatorArm64` — BUILD SUCCESSFUL
  - [x] Verify: `./gradlew :shared:assembleDebug` — BUILD SUCCESSFUL

- [x] Task 4: Update `ItemsScreen` to pass `weekendTime` to `onSave` (AC: 3, 4)
  - [x] Update `ItemEditSheet` call in `ItemsScreen`:
    `onSave = { name, weekdayTime, weekendTime -> viewModel.addItem(name, weekdayTime, weekendTime) }`

- [x] Task 5: Write `ItemRepositoryTest` additions (AC: 3, 4)
  - [x] Add test `addItem_withWeekendTime_storesWeekendTime`:
    - Call `repo.addItem("Vitamin C", "09:00", "10:00")`
    - Assert `repo.observeItems().first()[0].reminderWeekendTime == "10:00"`
  - [x] Add test `addItem_withoutWeekendTime_storesNull`:
    - Call `repo.addItem("Vitamin C", "09:00", null)`
    - Assert `repo.observeItems().first()[0].reminderWeekendTime == null`
  - [x] Run `./gradlew :shared:testDebugUnitTest` — all tests pass

- [x] Task 6: Final build verification (AC: all)
  - [x] `./gradlew :shared:testDebugUnitTest` — BUILD SUCCESSFUL
  - [x] `./gradlew :shared:assembleDebug` — BUILD SUCCESSFUL
  - [x] `./gradlew :androidApp:assembleDebug` — BUILD SUCCESSFUL
  - [x] `./gradlew :shared:compileKotlinIosSimulatorArm64` — BUILD SUCCESSFUL

### Senior Developer Review (AI)

**Date:** 2026-04-15
**Outcome:** Changes Requested
**Layers:** Blind Hunter · Edge Case Hunter · Acceptance Auditor

#### Action Items

- [x] [Review][Patch] `UIApplication.sharedApplication.keyWindow` is deprecated (iOS 13+) and returns nil in multi-scene apps — replaced with `connectedScenes` iteration [`shared/src/iosMain/.../BottomSafeAreaPadding.ios.kt`] **[Med]** ✅ Fixed
- [x] [Review][Patch] `sheetState.expand()` inside `LaunchedEffect` is a no-op — `skipPartiallyExpanded = true` already guarantees full expansion; removed the redundant call [`ItemEditSheet.kt`] **[Low]** ✅ Fixed
- [x] [Review][Defer] `hideNavBar` cleanup race on swipe-dismiss — `awaitCancellation` finally block is correct; theoretical risk only if sheet hosted outside `AppNavigation` [`ItemEditSheet.kt`] — deferred, pre-existing architecture risk
- [x] [Review][Defer] `initialWeekendTime` scaffolded without full edit-mode param set (`initialName`, `initialWeekdayTime`) — intentional; Story 2.4 wires full edit mode [`ItemEditSheet.kt`] — deferred, Story 2.4
- [x] [Review][Defer] `onSave` + `onDismiss` called sequentially with no error guard — pre-existing pattern, ViewModel launch boundary prevents synchronous throw [`ItemEditSheet.kt`] — deferred, pre-existing
- [x] [Review][Defer] Collapsing weekend toggle in edit mode gives no UX warning that saved value will become NULL — Story 2.4's concern [`ItemEditSheet.kt`] — deferred, Story 2.4
- [x] [Review][Defer] No UI/composable test for AC5 pre-expansion from `initialWeekendTime` — test infrastructure for Compose UI not yet set up [`ItemRepositoryTest.kt`] — deferred, Story 2.4
- [x] [Review][Defer] `weekendTime ?: weekdayTime` fallback in weekend time row is dead code (invariant: `weekendTime` is always non-null when toggle is expanded) — defensive but harmless [`ItemEditSheet.kt`] — deferred, low risk

---

## Dev Notes

### Save Button: Disabled When Name Blank

The UX spec (Form Patterns) says "Save button: Always enabled" — this is overridden by AC4 of Story 2.2, which disables Save when `name.isBlank()`. Keep `enabled = name.isNotBlank()` as implemented in 2.2.

### Notification Reschedule Stub — Deferred

The epics file specifies that every item save should trigger a notification reschedule stub. This was not implemented in Story 2.2 and is intentionally deferred to Epic 4, where the full `NotificationScheduler` is built. Do not add any scheduling logic in this story.

### No SQL Changes Needed

`reminder_weekend_time TEXT` already exists in `TrackerDatabase.sq` and the `insertItem` query already accepts it as the 3rd positional parameter. Simply pass `weekendTime` instead of hardcoded `null`.

Current `insertItem` SQL:
```sql
insertItem:
INSERT INTO items (name, reminder_weekday_time, reminder_weekend_time, image_path, icon_id)
VALUES (?, ?, ?, ?, ?);
```

### Task 1: `ItemRepository.addItem()` Updated Signature

```kotlin
suspend fun addItem(name: String, weekdayTime: String, weekendTime: String?) {
    withContext(Dispatchers.Default) {
        database.trackerDatabaseQueries.insertItem(
            name = name,
            reminder_weekday_time = weekdayTime,
            reminder_weekend_time = weekendTime,   // was null
            image_path = null,
            icon_id = null
        )
    }
}
```

### Task 2: `ItemsViewModel.addItem()` Updated Signature

```kotlin
fun addItem(name: String, weekdayTime: String, weekendTime: String?) {
    viewModelScope.launch {
        repository.addItem(name, weekdayTime, weekendTime)
    }
}
```

### Task 3: `ItemEditSheet` — Weekend Toggle Logic

When the toggle row is tapped:
```kotlin
modifier = Modifier.clickable {
    weekendToggleExpanded = !weekendToggleExpanded
    if (weekendToggleExpanded && weekendTime == null) {
        weekendTime = weekdayTime  // default to current weekday time
    }
}
```

When toggle collapses, do NOT clear `weekendTime` from state — only pass `null` on Save when collapsed. This preserves the user's selection if they accidentally collapse and re-expand.

Full `ItemEditSheet` signature and structure (add `import androidx.compose.ui.semantics.semantics` and `import androidx.compose.ui.semantics.Role` to the existing imports — all other imports already present from Story 2.2):
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditSheet(
    onSave: (name: String, weekdayTime: String, weekendTime: String?) -> Unit,
    onDismiss: () -> Unit,
    initialWeekendTime: String? = null   // for edit mode pre-fill (Story 2.4)
) {
    var name by remember { mutableStateOf("") }
    var weekdayTime by remember { mutableStateOf("08:00") }
    var weekendTime by remember { mutableStateOf(initialWeekendTime) }
    var weekendToggleExpanded by remember { mutableStateOf(initialWeekendTime != null) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showWeekendTimePicker by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Add item", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(...)  // unchanged

            // Weekday row (unchanged)
            ListItem(
                headlineContent = { Text("Weekday reminder") },
                trailingContent = { Text(weekdayTime, ...) },
                modifier = Modifier.clickable { showTimePicker = true }
            )
            HorizontalDivider()

            // Weekend toggle row
            ListItem(
                headlineContent = { Text("Different time on weekends?") },
                trailingContent = {
                    Checkbox(checked = weekendToggleExpanded, onCheckedChange = null)
                },
                modifier = Modifier
                    .clickable {
                        weekendToggleExpanded = !weekendToggleExpanded
                        if (weekendToggleExpanded && weekendTime == null) {
                            weekendTime = weekdayTime
                        }
                    }
                    .semantics { role = Role.Checkbox }  // UX-DR14: screen reader announces as checkbox
            )

            // Weekend time row (conditional)
            if (weekendToggleExpanded) {
                ListItem(
                    headlineContent = { Text("Weekend reminder") },
                    trailingContent = {
                        Text(
                            text = weekendTime ?: weekdayTime,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.clickable { showWeekendTimePicker = true }
                )
                HorizontalDivider()
            }

            Button(
                onClick = {
                    onSave(name.trim(), weekdayTime, if (weekendToggleExpanded) weekendTime else null)
                    onDismiss()
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    if (showTimePicker) {
        PlatformTimePickerDialog(
            initialTime = weekdayTime,
            onTimeSelected = { weekdayTime = it; showTimePicker = false },
            onDismiss = { showTimePicker = false }
        )
    }

    if (showWeekendTimePicker) {
        PlatformTimePickerDialog(
            initialTime = weekendTime ?: weekdayTime,
            onTimeSelected = { weekendTime = it; showWeekendTimePicker = false },
            onDismiss = { showWeekendTimePicker = false }
        )
    }
}
```

### Task 4: `ItemsScreen` — Updated `onSave` Lambda

```kotlin
if (showAddSheet) {
    ItemEditSheet(
        onSave = { name, weekdayTime, weekendTime ->
            viewModel.addItem(name, weekdayTime, weekendTime)
        },
        onDismiss = { showAddSheet = false }
    )
}
```

### Task 5: Test Pattern

Existing `ItemRepositoryTest` creates a fresh in-memory driver per test. New tests follow the same pattern:

```kotlin
@Test
fun addItem_withWeekendTime_storesWeekendTime() = runTest {
    val db = TrackerDatabase(createDriver())
    val repo = ItemRepository(db)

    repo.addItem("Vitamin C", "09:00", "10:00")

    val items = repo.observeItems().first()
    assertEquals(1, items.size)
    assertEquals("10:00", items[0].reminderWeekendTime)
}

@Test
fun addItem_withoutWeekendTime_storesNull() = runTest {
    val db = TrackerDatabase(createDriver())
    val repo = ItemRepository(db)

    repo.addItem("Vitamin C", "09:00", null)

    val items = repo.observeItems().first()
    assertEquals(1, items.size)
    assertEquals(null, items[0].reminderWeekendTime)
}
```

### Existing Pattern: ViewModel in `ItemsScreen`

The working pattern from Story 2.1/2.2 — do NOT change this:
```kotlin
val repository = koinInject<ItemRepository>()
val viewModel: ItemsViewModel = viewModel { ItemsViewModel(repository) }
```

Do NOT use `koinViewModel<ItemsViewModel>()` — it does not exist in `koin-compose:1.1.5`.

### Existing Pattern: `PlatformTimePickerDialog`

Already implemented in Story 2.2 — reuse as-is:
- `shared/src/commonMain/.../ui/components/PlatformTimePickerDialog.kt` (expect)
- `shared/src/androidMain/.../ui/components/PlatformTimePickerDialog.android.kt` (Material3 TimePicker)
- `shared/src/iosMain/.../ui/components/PlatformTimePickerDialog.ios.kt` (UIDatePicker wheels)

No changes to these files needed.

### AC5: Edit Mode Pre-fill

AC5 refers to edit mode which is Story 2.4's job. Story 2.3 sets up `ItemEditSheet` to *accept* `initialWeekendTime: String?` so that Story 2.4 can pass a stored value when opening an existing item. The state initialization `weekendToggleExpanded = initialWeekendTime != null` handles AC5 automatically once Story 2.4 passes the value.

### Don't Break List (from Stories 2.1–2.2)

- `ItemRepository.observeItems()` — do NOT modify
- `ItemsViewModel.uiState` StateFlow — do NOT modify
- `PlatformTimePickerDialog` expect/actual — do NOT modify
- `DatabaseDriverFactory.ios.kt` — do NOT touch (`basePath` fix is critical)
- `TrackerDatabase.sq` — do NOT modify (schema is complete for this story)
- `SharedModule.kt`, `AndroidModule.kt`, `IosModule.kt` — do NOT modify
- `AppNavigation.kt` — do NOT modify

### Files to Create

None.

### Files to Modify

```
shared/src/commonMain/kotlin/com/sonja/tracker/data/repository/ItemRepository.kt
shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/ItemsViewModel.kt
shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/ItemEditSheet.kt
shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/ItemsScreen.kt
shared/src/commonTest/kotlin/com/sonja/tracker/data/repository/ItemRepositoryTest.kt
```

---

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

- Fixed missing `import androidx.compose.ui.semantics.role` (lowercase property import, separate from `Role` enum) — required for `semantics { role = Role.Checkbox }` to compile.
- Followed existing inline time picker pattern (inside Column, not after ModalBottomSheet) to preserve iOS UIWindow/ModalBottomSheet gesture compatibility.

### Completion Notes List

- Updated `ItemRepository.addItem()` to accept `weekendTime: String?` and pass it to `insertItem` (was hardcoded `null`).
- Updated `ItemsViewModel.addItem()` to accept and thread `weekendTime: String?` through to repository.
- Rewrote `ItemEditSheet` to add `initialWeekendTime: String?` parameter, weekend toggle `ListItem` with `Checkbox` and `Role.Checkbox` semantics, conditional weekend time row with inline `PlatformTimePickerDialog`, and updated `onSave` to three-arg lambda. When collapsed, `null` is passed for `weekendTime`; when expanded, the selected time is passed.
- Updated `ItemsScreen` `onSave` lambda to destructure three parameters and call `viewModel.addItem(name, weekdayTime, weekendTime)`.
- Updated existing test `addItem_insertsItemObservableViaFlow` to pass `null` as third arg after signature change.
- Added two new tests: `addItem_withWeekendTime_storesWeekendTime` and `addItem_withoutWeekendTime_storesNull`.
- All 5 unit tests pass. All 4 build targets pass.

### File List

- `shared/src/commonMain/kotlin/com/sonja/tracker/data/repository/ItemRepository.kt`
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/ItemsViewModel.kt`
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/ItemEditSheet.kt`
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/ItemsScreen.kt`
- `shared/src/commonTest/kotlin/com/sonja/tracker/data/repository/ItemRepositoryTest.kt`

### Change Log

- 2026-04-15: Story 2.3 implemented — added weekend reminder time toggle to `ItemEditSheet`, threaded `weekendTime: String?` through ViewModel and Repository, added 2 new repository tests. All builds and tests pass.
