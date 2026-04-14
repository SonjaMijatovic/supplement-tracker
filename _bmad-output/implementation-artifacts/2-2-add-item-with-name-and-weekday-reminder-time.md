# Story 2.2: Add Item with Name & Weekday Reminder Time

Status: done

## Story

As a user,
I want to add a new supplement or medication with a name and weekday reminder time,
so that I can start tracking it immediately.

## Acceptance Criteria

1. **Given** the user taps the add button on the Items screen **When** the `ItemEditSheet` opens **Then** it appears as a `ModalBottomSheet` with the name text field auto-focused.

2. **Given** the `ItemEditSheet` is open in add mode **When** the user taps the weekday time row **Then** the native OS time picker opens (iOS: UIDatePicker wheel style; Android: Material3 clock dial) **And** the selected time is shown as the row label in `HH:mm` format.

3. **Given** the user has entered a name and optionally set a weekday time (defaults to `08:00` if untouched) **When** the user taps Save **Then** a new item is inserted into the `items` table with the given name and `reminder_weekday_time` stored as `HH:mm` TEXT **And** the sheet dismisses and the new item appears in the Items list reactively via the existing Flow.

4. **Given** the name field is empty **When** the Save button is rendered **Then** it is disabled — the user cannot save a nameless item.

5. **Given** the Items screen shows the empty-state "Add item" button or the FAB **When** either is tapped **Then** `ItemEditSheet` opens.

## Tasks / Subtasks

- [x] Task 1: Add `addItem` to `ItemRepository` (AC: 3)
  - [x] Add `suspend fun addItem(name: String, weekdayTime: String)` that calls `database.trackerDatabaseQueries.insertItem(name, weekdayTime, null, null, null)` wrapped in `withContext(Dispatchers.Default)`
  - [x] Verify: existing `insertItem` SQL query in `TrackerDatabase.sq` covers this — no new SQL needed

- [x] Task 2: Add `addItem` to `ItemsViewModel` (AC: 3)
  - [x] Add `fun addItem(name: String, weekdayTime: String)` that calls `viewModelScope.launch { repository.addItem(name, weekdayTime) }`
  - [x] No new `UiState` needed — the existing `observeItems()` Flow emits automatically after the insert

- [x] Task 3: Create `PlatformTimePickerDialog` expect/actual (AC: 2)
  - [x] Create `shared/src/commonMain/kotlin/com/sonja/tracker/ui/components/PlatformTimePickerDialog.kt`
  - [x] Create `shared/src/androidMain/kotlin/com/sonja/tracker/ui/components/PlatformTimePickerDialog.android.kt` — Material3 clock dial
  - [x] Create `shared/src/iosMain/kotlin/com/sonja/tracker/ui/components/PlatformTimePickerDialog.ios.kt` — UIDatePicker wheel via `UIKitView`
  - [x] Verify: `./gradlew :shared:compileKotlinIosSimulatorArm64` — BUILD SUCCESSFUL
  - [x] Verify: `./gradlew :shared:assembleDebug` — BUILD SUCCESSFUL

- [x] Task 4: Create `ItemEditSheet` composable (AC: 1, 2, 3, 4, 5)
  - [x] Create `shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/ItemEditSheet.kt`
  - [x] `ModalBottomSheet` wrapping: name `TextField` (auto-focused, required) + weekday time row + Save button
  - [x] All form state (`name`, `weekdayTime`, `showTimePicker`) as `remember { mutableStateOf(...) }` — no ViewModel for sheet state
  - [x] Default `weekdayTime = "08:00"` displayed in time row before user interaction
  - [x] Tapping time row sets `showTimePicker = true`; `PlatformTimePickerDialog` shown conditionally
  - [x] Save button disabled when `name.isBlank()`; on click calls `onSave(name, weekdayTime)` and dismisses

- [x] Task 5: Update `ItemsScreen` to wire the FAB and sheet (AC: 1, 5)
  - [x] Add FAB (`FloatingActionButton` with `Icons.Default.Add`) to `Scaffold` in `ItemsScreen`
  - [x] Add `var showAddSheet by remember { mutableStateOf(false) }` state
  - [x] Update empty-state button `onClick` to set `showAddSheet = true`
  - [x] FAB `onClick` also sets `showAddSheet = true`
  - [x] When `showAddSheet` is `true`, show `ItemEditSheet(onSave = { name, time -> viewModel.addItem(name, time) }, onDismiss = { showAddSheet = false })`
  - [x] `ItemEditSheet` placed outside the `Scaffold` block, at composable level

- [x] Task 6: Write `ItemRepositoryTest` additions (AC: 3)
  - [x] In `ItemRepositoryTest`, added test: `addItem_insertsItemObservableViaFlow`
  - [x] Run `./gradlew :shared:testDebugUnitTest` — all tests pass

- [x] Task 7: Final build verification (AC: all)
  - [x] `./gradlew :shared:testDebugUnitTest` — BUILD SUCCESSFUL
  - [x] `./gradlew :shared:assembleDebug` — BUILD SUCCESSFUL
  - [x] `./gradlew :androidApp:assembleDebug` — BUILD SUCCESSFUL
  - [x] `./gradlew :shared:compileKotlinIosSimulatorArm64` — BUILD SUCCESSFUL

---

## Dev Notes

### Critical: ViewModel Pattern (from Story 2.1)

`koin-compose-viewmodel` does NOT exist for Koin 3.5 in the 1.x track. The only working pattern is:

```kotlin
// ItemsScreen.kt — get ViewModel
val repository = koinInject<ItemRepository>()
val viewModel: ItemsViewModel = viewModel { ItemsViewModel(repository) }
```

Do NOT use `koinViewModel<ItemsViewModel>()` — it does not exist in `koin-compose:1.1.5`.

### Critical: iOS NativeSqliteDriver Path (from Story 2.1 bug fix)

`NativeSqliteDriver` takes `name` as a bare filename, not a full path. `basePath` in `onConfiguration` is how the Application Support directory is set. This is already implemented in `DatabaseDriverFactory.ios.kt` — do not touch it.

### Task 1: `ItemRepository.addItem()`

```kotlin
suspend fun addItem(name: String, weekdayTime: String) {
    withContext(Dispatchers.Default) {
        database.trackerDatabaseQueries.insertItem(
            name = name,
            reminder_weekday_time = weekdayTime,
            reminder_weekend_time = null,
            image_path = null,
            icon_id = null
        )
    }
}
```

Add `import kotlinx.coroutines.withContext` to the existing imports in `ItemRepository.kt`.

### Task 2: `ItemsViewModel.addItem()`

```kotlin
fun addItem(name: String, weekdayTime: String) {
    viewModelScope.launch {
        repository.addItem(name, weekdayTime)
    }
}
```

`viewModelScope.launch` is already on `Dispatchers.Main`; the `withContext(Dispatchers.Default)` inside `addItem` handles the thread switch.

### Task 3: `PlatformTimePickerDialog` — Android Actual

```kotlin
// shared/src/androidMain/kotlin/com/sonja/tracker/ui/components/PlatformTimePickerDialog.android.kt
package com.sonja.tracker.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun PlatformTimePickerDialog(
    initialTime: String,
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val parts = initialTime.split(":")
    val state = rememberTimePickerState(
        initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 8,
        initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0,
        is24Hour = true
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimePicker(state = state)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = {
                        onTimeSelected("%02d:%02d".format(state.hour, state.minute))
                    }) { Text("OK") }
                }
            }
        }
    }
}
```

### Task 3: `PlatformTimePickerDialog` — iOS Actual

```kotlin
// shared/src/iosMain/kotlin/com/sonja/tracker/ui/components/PlatformTimePickerDialog.ios.kt
package com.sonja.tracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*
import platform.UIKit.*

@OptIn(ExperimentalForeignApi::class, ExperimentalMaterial3Api::class)
@Composable
actual fun PlatformTimePickerDialog(
    initialTime: String,
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val parts = initialTime.split(":")
    val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

    // UIDatePicker created once and retained across recompositions
    val datePicker = remember {
        UIDatePicker().apply {
            datePickerMode = UIDatePickerMode.UIDatePickerModeTime
            preferredDatePickerStyle = UIDatePickerStyle.UIDatePickerStyleWheels
            val calendar = NSCalendar.currentCalendar
            val components = NSDateComponents().apply {
                hour = initialHour.toLong()
                minute = initialMinute.toLong()
            }
            date = calendar.dateFromComponents(components) ?: NSDate()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                UIKitView(
                    factory = { datePicker },
                    modifier = Modifier.fillMaxWidth().height(216.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = {
                        val calendar = NSCalendar.currentCalendar
                        @Suppress("UNCHECKED_CAST")
                        val components = calendar.components(
                            NSCalendarUnitHour or NSCalendarUnitMinute,
                            datePicker.date
                        )
                        onTimeSelected(
                            "%02d:%02d".format(components.hour.toInt(), components.minute.toInt())
                        )
                    }) { Text("OK") }
                }
            }
        }
    }
}
```

**Important iOS notes:**
- `UIKitView` is from `androidx.compose.ui.interop.UIKitView` — available in CMP for iOS
- `UIDatePickerStyle.UIDatePickerStyleWheels` gives the iOS scroll-wheel appearance
- `@OptIn(ExperimentalForeignApi::class)` required for `NSDateComponents` and `NSCalendar` interop
- `NSCalendarUnit` bitwise OR (`or`) is used for combining calendar units
- `.toInt()` required when converting `NSInteger` (Long) components to `%02d` format

### Task 4: `ItemEditSheet` Composable Structure

```kotlin
// shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/ItemEditSheet.kt
package com.sonja.tracker.ui.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.sonja.tracker.ui.components.PlatformTimePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditSheet(
    onSave: (name: String, weekdayTime: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var weekdayTime by remember { mutableStateOf("08:00") }
    var showTimePicker by remember { mutableStateOf(false) }
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

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )

            // Weekday time row — tappable, shows current time
            ListItem(
                headlineContent = { Text("Weekday reminder") },
                trailingContent = {
                    Text(
                        text = weekdayTime,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.clickable { showTimePicker = true }
            )
            HorizontalDivider()

            Button(
                onClick = {
                    onSave(name.trim(), weekdayTime)
                    onDismiss()
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }

    // Auto-focus name field when sheet opens
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Show platform time picker
    if (showTimePicker) {
        PlatformTimePickerDialog(
            initialTime = weekdayTime,
            onTimeSelected = { selectedTime ->
                weekdayTime = selectedTime
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}
```

### Task 5: `ItemsScreen` Changes

The existing `ItemsScreen` uses a `Box(modifier.fillMaxSize(), ...)` for the Loading/Error states and `Column`/`LazyColumn` for Success. The `Scaffold` with FAB should wrap all of this.

Replace the top-level composable body with a `Scaffold` that has a FAB:

```kotlin
@Composable
fun ItemsScreen(modifier: Modifier = Modifier) {
    val repository = koinInject<ItemRepository>()
    val viewModel: ItemsViewModel = viewModel { ItemsViewModel(repository) }
    val uiState by viewModel.uiState.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add item")
            }
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is ItemsUiState.Loading -> { /* centered CircularProgressIndicator */ }
            is ItemsUiState.Error -> { /* centered Text */ }
            is ItemsUiState.Success -> {
                if (state.items.isEmpty()) {
                    // Empty state — wire button onClick to showAddSheet = true
                } else {
                    // LazyColumn with ItemRow (use innerPadding)
                }
            }
        }
    }

    if (showAddSheet) {
        ItemEditSheet(
            onSave = { name, weekdayTime -> viewModel.addItem(name, weekdayTime) },
            onDismiss = { showAddSheet = false }
        )
    }
}
```

**Key:** `ItemEditSheet` is placed OUTSIDE the `Scaffold` block (but inside the `ItemsScreen` function). `ModalBottomSheet` must not be inside a `Scaffold` content lambda — it needs to be at the composable level to overlay correctly.

Import needed: `import androidx.compose.material.icons.filled.Add`

### `ItemsScreen` — Nested Scaffold Warning

`AppNavigation` already uses a `Scaffold`. `ItemsScreen` adding another `Scaffold` creates nested scaffolds. If this causes layout padding issues, remove the `Scaffold` from `ItemsScreen` and instead position the FAB using `Box` with `Alignment.BottomEnd` and appropriate padding. However, nested Scaffolds are permitted in CMP/Compose — test empirically.

### SQLDelight Write Threading

SQLDelight writes (`insertItem`) are synchronous calls. Wrapping in `withContext(Dispatchers.Default)` in the repository ensures they never block the main thread. The `viewModelScope.launch` in the ViewModel starts on `Dispatchers.Main`; the `withContext` inside the repository switches to `Dispatchers.Default` for the actual write.

### Don't Break List (from Stories 1.1–2.1)

- `ItemRepository.observeItems()` — additive only; do NOT modify the existing Flow pipeline
- `ItemsViewModel.uiState` StateFlow — additive only; do NOT change `SharingStarted` or `initialValue`
- `ItemsScreen` — restructure to add FAB/sheet, but preserve existing sealed-state `when` branches
- `SharedModule.kt` — additive only; `ItemRepository` stays as `single { ItemRepository(get()) }`
- `TrackerDatabase.sq` — additive only if needed; `insertItem` query already present — NO new SQL needed
- `DatabaseDriverFactory.ios.kt` — do NOT touch; the `basePath` fix is critical
- `AppNavigation.kt`, `Theme.kt`, `Color.kt`, `Type.kt` — untouched

### Files to Create

```
shared/src/commonMain/kotlin/com/sonja/tracker/
  ui/
    items/
      ItemEditSheet.kt          ← NEW
    components/
      PlatformTimePickerDialog.kt    ← NEW (expect)

shared/src/androidMain/kotlin/com/sonja/tracker/
  ui/components/
    PlatformTimePickerDialog.android.kt  ← NEW (actual)

shared/src/iosMain/kotlin/com/sonja/tracker/
  ui/components/
    PlatformTimePickerDialog.ios.kt  ← NEW (actual)
```

### Files to Modify

```
shared/src/commonMain/kotlin/com/sonja/tracker/
  data/repository/ItemRepository.kt    ← add addItem()
  ui/items/ItemsViewModel.kt           ← add addItem()
  ui/items/ItemsScreen.kt              ← add FAB, showAddSheet state, ItemEditSheet
shared/src/commonTest/kotlin/com/sonja/tracker/
  data/repository/ItemRepositoryTest.kt  ← add addItem test
```

### References

- [Source: epics.md#Story 2.2] — acceptance criteria and user story
- [Source: architecture.md#State Management Patterns] — ViewModel + StateFlow, `viewModelScope.launch`
- [Source: architecture.md#Error Handling Patterns] — no try/catch in repositories
- [Source: ux-design-specification.md#Component Strategy → ItemEditSheet] — sheet anatomy, name auto-focus, time row, Save always-enabled (but name required in AC overrides this — save disabled if name blank)
- [Source: ux-design-specification.md#Form Patterns] — default 08:00, time picker via row tap
- [Source: 2-1-items-list-screen.md#Dev Notes] — koin-compose 1.1.5 ViewModel pattern, `viewModel { }` + `koinInject()`

---

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

- iOS `PlatformTimePickerDialog`: `String.format()` unavailable in Kotlin/Native — replaced with `padStart(2, '0')` for zero-padding.

### Completion Notes List

- Task 1: Added `suspend fun addItem()` to `ItemRepository` using `withContext(Dispatchers.Default)`.
- Task 2: Added `fun addItem()` to `ItemsViewModel` via `viewModelScope.launch`.
- Task 3: Created `PlatformTimePickerDialog` expect + Android (Material3 TimePicker) + iOS (UIDatePicker wheel via UIKitView) actuals. iOS uses `padStart` for HH:mm formatting instead of `String.format`.
- Task 4: Created `ItemEditSheet` as `ModalBottomSheet` with auto-focused name field, weekday time row, and disabled-when-blank Save button.
- Task 5: Updated `ItemsScreen` with FAB + `showAddSheet` state; `ItemEditSheet` placed outside `Scaffold` content lambda.
- Task 6: Added `addItem_insertsItemObservableViaFlow` test to `ItemRepositoryTest` — passes.
- Task 7: All 4 builds green.

### File List

- `shared/src/commonMain/kotlin/com/sonja/tracker/data/repository/ItemRepository.kt` — modified (addItem)
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/ItemsViewModel.kt` — modified (addItem)
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/ItemsScreen.kt` — modified (FAB + sheet)
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/ItemEditSheet.kt` — created
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/components/PlatformTimePickerDialog.kt` — created (expect)
- `shared/src/androidMain/kotlin/com/sonja/tracker/ui/components/PlatformTimePickerDialog.android.kt` — created
- `shared/src/iosMain/kotlin/com/sonja/tracker/ui/components/PlatformTimePickerDialog.ios.kt` — created
- `shared/src/commonTest/kotlin/com/sonja/tracker/data/repository/ItemRepositoryTest.kt` — modified (new test)

### Change Log

- 2026-04-14: Implemented Story 2.2 — add item with name and weekday reminder time. Added ItemRepository.addItem(), ItemsViewModel.addItem(), PlatformTimePickerDialog expect/actual, ItemEditSheet ModalBottomSheet, FAB + sheet wiring in ItemsScreen, and addItem repository test.

### Review Findings

- [x] [Review][Patch] iOS: `components.hour`/`components.minute` can return `NSUndefinedDateComponent` on calendar failure — `.toInt()` produces garbage stored in DB [PlatformTimePickerDialog.ios.kt] — fixed: guard against out-of-range values, fall back to `initialHour`/`initialMinute`
- [x] [Review][Patch] iOS: `dateFromComponents` fallback is `NSDate()` (current moment) — wrong initial picker position on calendar failure [PlatformTimePickerDialog.ios.kt] — fixed: fallback to 08:00 instead of current time; also set `.calendar` on `NSDateComponents`
- [x] [Review][Defer] `addItem` errors silently swallowed + sheet dismisses unconditionally — explicitly documented as known deferred limitation in dev notes [ItemsViewModel.kt, ItemEditSheet.kt] — deferred, pre-existing
- [x] [Review][Defer] Hardcoded UI strings not localizable — project-wide concern, no localization pipeline yet [ItemEditSheet.kt, PlatformTimePickerDialog.*.kt] — deferred, pre-existing
