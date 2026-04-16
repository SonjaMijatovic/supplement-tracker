# Story 2.5: Icon Picker

Status: review

## Story

As a user,
I want to choose an icon for each item from a curated grid,
so that I can instantly recognise each supplement by its icon without reading the name.

## Acceptance Criteria

1. **Given** the user has entered an item name in the `ItemEditSheet` **When** the keyboard Done action is triggered **Then** the `IconPickerGrid` opens automatically as a section within the sheet, with a prominent "Skip" button visible.

2. **Given** the `IconPickerGrid` is displayed **When** rendered **Then** it shows approximately 23 icons — 20 from Material Icons Extended and 3 custom SVGs stored in `commonMain/composeResources/drawable/` — in a scrollable grid of rounded-square tiles.

3. **Given** the user taps an icon tile **When** selected **Then** the tile shows an indigo tint border (2dp primary colour border) indicating selection.

4. **Given** the user taps Skip or proceeds without selecting an icon **When** the item is saved with no icon selected and no image attached **Then** `icon_id` is stored as NULL and a default placeholder (coloured rounded-square box) is displayed in `ItemRow`.

5. **Given** an icon is selected and the item is saved **When** the item appears in any screen **Then** the selected icon is displayed inside the 40dp rounded-square thumbnail in `ItemRow`.

6. **Given** an item with an existing icon is opened for editing **When** the `ItemEditSheet` opens **Then** the previously selected icon is shown as the current selection in the `IconPickerGrid` (picker is visible on open in edit mode).

## Tasks / Subtasks

- [x] Task 1: Add `material-icons-extended` dependency (AC: 2)
  - [x] In `gradle/libs.versions.toml`, add under `[libraries]`:
    ```toml
    compose-material-icons-extended = { module = "org.jetbrains.compose.material:material-icons-extended", version = "1.7.3" }
    ```
  - [x] In `shared/build.gradle.kts`, add inside `commonMain.dependencies { }`:
    ```kotlin
    implementation(libs.compose.material.icons.extended)
    ```
  - [x] Run `./gradlew :shared:compileKotlinIosSimulatorArm64` to confirm resolution — BUILD SUCCESSFUL

- [x] Task 2: Add 3 custom SVG drawables to `commonMain/composeResources/drawable/` (AC: 2)
  - [x] `ic_capsule.xml` — a capsule/oval pill shape (e.g., from Phosphor Icons `Pill` or Lucide `Pill`, MIT-licensed); convert to Compose vector drawable format
  - [x] `ic_supplement_bottle.xml` — a cylindrical supplement bottle with lid (e.g., Phosphor `Bottle` variant); convert to Compose vector drawable format
  - [x] `ic_herb.xml` — a stylised herb/plant sprig (e.g., Phosphor `Plant`, Lucide `Leaf`); convert to Compose vector drawable format
  - [x] **Note:** Compose Resources generates accessors from filenames at compile time. Files named `ic_capsule.xml` become `Res.drawable.ic_capsule`. Verify with `./gradlew :shared:generateCommonMainResourceAccessors`

- [x] Task 3: Create `ItemIcons.kt` — icon registry and rendering composable (AC: 2, 3, 5)
  - [x] Create `shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/ItemIcons.kt`
  - [x] Define `object ItemIcons` with `materialIcons`, `customIcons`, and `allIconIds` — see Dev Notes for exact code
  - [x] Define `@Composable fun ItemIconContent(iconId: String, modifier: Modifier)` — shared rendering for both `ItemRow` and `IconPickerGrid` — see Dev Notes

- [x] Task 4: Create `IconPickerGrid.kt` composable (AC: 1, 2, 3)
  - [x] Create `shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/IconPickerGrid.kt`
  - [x] Implement `@Composable fun IconPickerGrid(selectedIconId: String?, onIconSelected: (String?) -> Unit, modifier: Modifier)` — see Dev Notes for exact code
  - [x] Layout: label row with prominent "Skip" `TextButton` at trailing end; `FlowRow` grid of tiles (4 per row), each 64dp tile with `RoundedCornerShape(12.dp)`
  - [x] Selected tile: `Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))` + `primaryContainer` background
  - [x] Unselected tile: `surfaceVariant` background, no border
  - [x] Each tile shows `ItemIconContent(iconId, Modifier.size(28.dp))`
  - [x] Verify: `./gradlew :shared:compileKotlinIosSimulatorArm64` — BUILD SUCCESSFUL

- [x] Task 5: Update `ItemRepository.addItem` to accept `iconId` (AC: 4, 5)
  - [x] Change signature: `suspend fun addItem(name: String, weekdayTime: String, weekendTime: String?, iconId: String? = null)`
  - [x] Pass `iconId` to `insertItem` instead of hardcoded `null` — see Dev Notes
  - [x] **Note:** Default value `= null` ensures existing tests compile without changes

- [x] Task 6: Update `ItemsViewModel` to propagate `iconId` (AC: 4, 5, 6)
  - [x] `addItem`: add `iconId: String? = null` parameter; pass to `repository.addItem()`
  - [x] `editItem`: add `iconId: String?` parameter; pass directly to `repository.updateItem()` instead of reading from stale `uiState` — see Dev Notes for updated implementation
  - [x] This resolves the deferred-work item: *"editItem reads stale uiState for iconId; null overwrite risk if state is not Success"*

- [x] Task 7: Update `ItemEditSheet` to integrate icon picker (AC: 1, 3, 4, 5, 6)
  - [x] Add `initialIconId: String? = null` parameter
  - [x] Change `onSave` lambda signature: `(name: String, weekdayTime: String, weekendTime: String?, iconId: String?) -> Unit`
  - [x] Add `var selectedIconId by remember { mutableStateOf(initialIconId) }` state
  - [x] Add `var iconPickerVisible by remember { mutableStateOf(onDelete != null || initialIconId != null) }` state (visible immediately in edit mode or when pre-selected)
  - [x] Add `Modifier.verticalScroll(rememberScrollState())` to the Column — required when icon picker + time pickers + buttons exceed screen height
  - [x] Add `KeyboardActions(onDone = { ... })` to the `OutlinedTextField` — triggers `iconPickerVisible = true` when name is non-blank; see Dev Notes
  - [x] Add `IconPickerGrid` section after the name field (before the `when` block): `if (iconPickerVisible) { IconPickerGrid(...) }` — see Dev Notes
  - [x] Update the `onSave` call at Save button click to pass `selectedIconId`: `onSave(name.trim(), weekdayTime, if (weekendToggleExpanded) weekendTime else null, selectedIconId)`
  - [x] Verify: `./gradlew :shared:compileKotlinIosSimulatorArm64` — BUILD SUCCESSFUL
  - [x] Verify: `./gradlew :shared:assembleDebug` — BUILD SUCCESSFUL

- [x] Task 8: Update `ItemsScreen` to wire `iconId` through `onSave` callbacks (AC: 4, 5)
  - [x] Add `initialIconId = item.iconId` to the edit sheet invocation
  - [x] Update both `onSave` lambdas to accept and forward `iconId` — see Dev Notes for exact code

- [x] Task 9: Update `ItemRow` to render the actual icon (AC: 4, 5)
  - [x] Replace the placeholder `Box` body: when `item.iconId != null`, render `ItemIconContent(item.iconId, Modifier.size(24.dp))`; otherwise leave placeholder empty (no change to the box itself)
  - [x] `contentDescription` on the box: `if (item.iconId != null) "${item.name} icon" else "No icon"`
  - [x] Add import for `ItemIconContent` (same package `com.sonja.tracker.ui.items`) — see Dev Notes

- [x] Task 10: Add repository tests (AC: 5)
  - [x] Add `addItem_withIconId_storesIconId` test — see Dev Notes
  - [x] Run `./gradlew :shared:testDebugUnitTest` — all tests pass

- [x] Task 11: Update `deferred-work.md` (clean-up)
  - [x] Remove: *"editItem reads stale uiState for imagePath/iconId; null overwrite risk..."* — resolved by Story 2.5 (iconId now passed directly)
  - [x] Add deferred entry: *"ItemRow contentDescription is hardcoded English — Story 2.5 added 'No icon' / '[name] icon'; project-wide localisation deferred"*

- [x] Task 12: Final build verification (AC: all)
  - [x] `./gradlew :shared:testDebugUnitTest` — BUILD SUCCESSFUL
  - [x] `./gradlew :shared:assembleDebug` — BUILD SUCCESSFUL
  - [x] `./gradlew :androidApp:assembleDebug` — BUILD SUCCESSFUL
  - [x] `./gradlew :shared:compileKotlinIosSimulatorArm64` — BUILD SUCCESSFUL

---

## Dev Notes

### Task 1: Dependency — Why `material-icons-extended`

Currently `libs.versions.toml` has only `compose-material-icons-core` (version `"1.7.3"`) which provides basic icons (`Add`, `Check`, etc.). Material Icons Extended adds ~2,000 more icons including all supplement/health-related ones needed for the icon grid. The `material-icons-extended` module is from `org.jetbrains.compose.material` at the same version `1.7.3`. **Warning:** Extended adds ~10–15 MB to the binary — acceptable per the architecture decision.

### Task 2: Custom SVG Conversion

Source icons from [Phosphor Icons](https://phosphoricons.com/) or [Lucide](https://lucide.dev/) (both MIT-licensed). Download as SVG. Convert to Android vector drawable XML format using Android Studio's SVG importer ("File → New → Vector Asset") or the `svg2vector` CLI tool, then move the XML to `shared/src/commonMain/composeResources/drawable/`.

Compose Resources XML format (must match this structure):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
  <path android:fillColor="#FF000000" android:pathData="..." />
</vector>
```

The `android:fillColor` will be overridden by the `tint` in `Icon()` at runtime — set it to any opaque value.

### Task 3: `ItemIcons.kt` — Full Code

File: `shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/ItemIcons.kt`

```kotlin
package com.sonja.tracker.ui.items

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.EmojiFoodBeverage
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.sonja.tracker.generated.resources.Res
import com.sonja.tracker.generated.resources.ic_capsule
import com.sonja.tracker.generated.resources.ic_supplement_bottle
import com.sonja.tracker.generated.resources.ic_herb
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

object ItemIcons {
    // Material Icons Extended — ordered by supplement relevance
    val materialIcons: Map<String, ImageVector> = mapOf(
        "medication"          to Icons.Outlined.Medication,
        "local_pharmacy"      to Icons.Outlined.LocalPharmacy,
        "medical_services"    to Icons.Outlined.MedicalServices,
        "healing"             to Icons.Outlined.Healing,
        "monitor_heart"       to Icons.Outlined.MonitorHeart,
        "favorite"            to Icons.Outlined.Favorite,
        "spa"                 to Icons.Outlined.Spa,
        "eco"                 to Icons.Outlined.Eco,
        "water_drop"          to Icons.Outlined.WaterDrop,
        "opacity"             to Icons.Outlined.Opacity,
        "grain"               to Icons.Outlined.Grain,
        "fitness_center"      to Icons.Outlined.FitnessCenter,
        "self_improvement"    to Icons.Outlined.SelfImprovement,
        "bolt"                to Icons.Outlined.Bolt,
        "restaurant"          to Icons.Outlined.Restaurant,
        "emoji_food_beverage" to Icons.Outlined.EmojiFoodBeverage,
        "science"             to Icons.Outlined.Science,
        "wb_sunny"            to Icons.Outlined.WbSunny,
        "nights_stay"         to Icons.Outlined.NightsStay,
        "star"                to Icons.Outlined.Star,
    )

    // Custom Phosphor/Lucide icons stored as Compose Resources drawables
    val customIcons: Map<String, DrawableResource> = mapOf(
        "ic_capsule"           to Res.drawable.ic_capsule,
        "ic_supplement_bottle" to Res.drawable.ic_supplement_bottle,
        "ic_herb"              to Res.drawable.ic_herb,
    )

    // Ordered list for display in the grid — material icons first, then custom
    val allIconIds: List<String> = materialIcons.keys.toList() + customIcons.keys.toList()
}

/**
 * Renders the icon for a given iconId using Material ImageVector (preferred) or
 * Compose Resource painterResource (for custom SVG icons).
 * Renders nothing for unknown iconIds — callers should guard against this.
 */
@Composable
fun ItemIconContent(iconId: String, modifier: Modifier = Modifier) {
    val vec = ItemIcons.materialIcons[iconId]
    val res = ItemIcons.customIcons[iconId]
    when {
        vec != null -> Icon(
            imageVector = vec,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = modifier
        )
        res != null -> Icon(
            painter = painterResource(res),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = modifier
        )
        // Unknown iconId — render nothing (should not occur with valid data)
    }
}
```

**Important:** If `Icons.Outlined.Medication` or any other icon is unavailable at compile time in material-icons-extended 1.7.3, replace it with the nearest available icon (e.g., `Icons.Outlined.MedicalServices` for `Medication`). The compiler error will clearly identify which icon is missing.

### Task 4: `IconPickerGrid.kt` — Full Code

File: `shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/IconPickerGrid.kt`

```kotlin
package com.sonja.tracker.ui.items

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconPickerGrid(
    selectedIconId: String?,
    onIconSelected: (String?) -> Unit,  // null = Skip (clears selection)
    modifier: Modifier = Modifier
) {
    val tileShape = RoundedCornerShape(12.dp)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Choose an icon",
                style = MaterialTheme.typography.labelLarge
            )
            TextButton(onClick = { onIconSelected(null) }) {
                Text("Skip")
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 4
        ) {
            ItemIcons.allIconIds.forEach { iconId ->
                val isSelected = iconId == selectedIconId
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(tileShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .then(
                            if (isSelected)
                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, tileShape)
                            else
                                Modifier
                        )
                        .clickable { onIconSelected(iconId) }
                        .semantics {
                            role = Role.Button
                            contentDescription = "$iconId icon${if (isSelected) ", selected" else ""}"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    ItemIconContent(iconId = iconId, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}
```

**`FlowRow` vs `LazyVerticalGrid`:** Use `FlowRow` (not `LazyVerticalGrid`) because the outer `Column` in `ItemEditSheet` will use `Modifier.verticalScroll(rememberScrollState())` — mixing a lazy layout inside a vertically-scrollable Column causes a runtime crash on some Compose versions. `FlowRow` is non-lazy and works correctly inside `verticalScroll`. With 23 icons at 4 per row, that is 6 rows at ~72dp each = ~432dp — fixed, predictable height.

### Task 5: `ItemRepository.addItem` — Signature Change

Before:
```kotlin
suspend fun addItem(name: String, weekdayTime: String, weekendTime: String?) {
    withContext(Dispatchers.Default) {
        database.trackerDatabaseQueries.insertItem(
            name = name,
            reminder_weekday_time = weekdayTime,
            reminder_weekend_time = weekendTime,
            image_path = null,
            icon_id = null  // was hardcoded
        )
    }
}
```

After:
```kotlin
suspend fun addItem(name: String, weekdayTime: String, weekendTime: String?, iconId: String? = null) {
    withContext(Dispatchers.Default) {
        database.trackerDatabaseQueries.insertItem(
            name = name,
            reminder_weekday_time = weekdayTime,
            reminder_weekend_time = weekendTime,
            image_path = null,
            icon_id = iconId
        )
    }
}
```

The `= null` default ensures all existing test calls `repo.addItem("name", "08:00", null)` still compile.

### Task 6: `ItemsViewModel` — Updated Methods

```kotlin
fun addItem(name: String, weekdayTime: String, weekendTime: String?, iconId: String? = null) {
    viewModelScope.launch {
        repository.addItem(name, weekdayTime, weekendTime, iconId)
    }
}

fun editItem(id: Long, name: String, weekdayTime: String, weekendTime: String?, iconId: String?) {
    viewModelScope.launch {
        val current = (uiState.value as? ItemsUiState.Success)?.items?.find { it.id == id }
        repository.updateItem(
            id = id,
            name = name,
            weekdayTime = weekdayTime,
            weekendTime = weekendTime,
            imagePath = current?.imagePath,  // still read from uiState (always null until Story 2.6)
            iconId = iconId                  // now passed directly — fixes stale-state risk
        )
        // TODO Epic 4: NotificationScheduler.rescheduleForSlot(weekdayTime, weekendTime)
    }
}
```

**Do NOT change** `deleteItem` — it doesn't touch `iconId`.

### Task 7: `ItemEditSheet` — Full Updated Signature and Integration

Updated signature:
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditSheet(
    onSave: (name: String, weekdayTime: String, weekendTime: String?, iconId: String?) -> Unit,
    onDismiss: () -> Unit,
    initialName: String = "",
    initialWeekdayTime: String = "08:00",
    initialWeekendTime: String? = null,
    initialIconId: String? = null,
    onDelete: (() -> Unit)? = null
)
```

New state variables (add near the top of the composable, with existing state):
```kotlin
var selectedIconId by remember { mutableStateOf(initialIconId) }
// Show picker immediately in edit mode (onDelete != null) or when there's a pre-selected icon.
// In add mode, becomes visible after the user presses Done on the name field.
var iconPickerVisible by remember { mutableStateOf(onDelete != null || initialIconId != null) }
```

Updated `OutlinedTextField` — add `keyboardActions` parameter:
```kotlin
OutlinedTextField(
    value = name,
    onValueChange = { name = it },
    label = { Text("Name") },
    singleLine = true,
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    keyboardActions = KeyboardActions(onDone = {
        focusManager.clearFocus()
        if (name.isNotBlank()) iconPickerVisible = true
    }),
    modifier = Modifier
        .fillMaxWidth()
        .focusRequester(focusRequester)
)
```

Add `verticalScroll` to the Column modifier:
```kotlin
Column(
    modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())   // NEW
        .padding(horizontal = 16.dp)
        .padding(bottom = 16.dp + bottomSafeArea),
    verticalArrangement = Arrangement.spacedBy(16.dp)
) {
```

Add `IconPickerGrid` section between the `OutlinedTextField` and the `when` block:
```kotlin
// After OutlinedTextField, before the when block:
if (iconPickerVisible) {
    IconPickerGrid(
        selectedIconId = selectedIconId,
        onIconSelected = { iconId ->
            selectedIconId = iconId
            // Skip: hide picker, clear selection
            if (iconId == null) iconPickerVisible = false
        }
    )
}
```

Updated Save button `onClick`:
```kotlin
Button(
    onClick = {
        onSave(name.trim(), weekdayTime, if (weekendToggleExpanded) weekendTime else null, selectedIconId)
        onDismiss()
    },
    enabled = name.isNotBlank() && !showTimePicker && !showWeekendTimePicker,
    modifier = Modifier.fillMaxWidth()
) {
    Text("Save")
}
```

New imports needed in `ItemEditSheet.kt`:
```kotlin
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
```

**Note on `LaunchedEffect` order:** Do NOT change the existing `LaunchedEffect(Unit)` for `focusRequester.requestFocus()` or `awaitCancellation`. They must remain at their current positions (at the bottom of the composable, after `ModalBottomSheet` and `AlertDialog`).

### Task 8: `ItemsScreen` — Updated `onSave` Callbacks

Add sheet — updated (4-parameter lambda):
```kotlin
if (showAddSheet) {
    ItemEditSheet(
        onSave = { name, weekdayTime, weekendTime, iconId ->
            viewModel.addItem(name, weekdayTime, weekendTime, iconId)
        },
        onDismiss = { showAddSheet = false }
    )
}
```

Edit sheet — updated (add `initialIconId`, 4-parameter lambda):
```kotlin
selectedItem?.let { item ->
    ItemEditSheet(
        initialName = item.name,
        initialWeekdayTime = item.reminderWeekdayTime ?: "08:00",
        initialWeekendTime = item.reminderWeekendTime,
        initialIconId = item.iconId,
        onSave = { name, weekdayTime, weekendTime, iconId ->
            viewModel.editItem(item.id, name, weekdayTime, weekendTime, iconId)
        },
        onDismiss = { selectedItem = null },
        onDelete = { viewModel.deleteItem(item.id) }
    )
}
```

### Task 9: `ItemRow` — Icon Rendering

Replace the placeholder `Box` body with:
```kotlin
Box(
    modifier = Modifier
        .size(40.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(MaterialTheme.colorScheme.primaryContainer)
        .semantics {
            contentDescription = if (item.iconId != null) "${item.name} icon" else "No icon"
        },
    contentAlignment = Alignment.Center
) {
    if (item.iconId != null) {
        ItemIconContent(iconId = item.iconId, modifier = Modifier.size(24.dp))
    }
    // item.imagePath rendering added in Story 2.6
}
```

`ItemIconContent` is in the same package (`com.sonja.tracker.ui.items`) — no import needed beyond the package-level visibility.

**Important:** Remove the existing `import androidx.compose.ui.semantics.contentDescription` and the hardcoded `contentDescription = "Item icon"` from the old box — they are replaced by the updated code above.

### Task 10: Repository Test Pattern

```kotlin
@Test
fun addItem_withIconId_storesIconId() = runTest {
    val db = TrackerDatabase(createDriver())
    val repo = ItemRepository(db)

    repo.addItem("Vitamin D3", "08:00", null, iconId = "medication")

    val items = repo.observeItems().first()
    assertEquals(1, items.size)
    assertEquals("medication", items[0].iconId)
}

@Test
fun addItem_withNullIconId_storesNull() = runTest {
    val db = TrackerDatabase(createDriver())
    val repo = ItemRepository(db)

    repo.addItem("Vitamin C", "09:00", null)  // default iconId = null

    val items = repo.observeItems().first()
    assertNull(items[0].iconId)
}
```

### Don't Break List (from Stories 2.1–2.4)

- `ItemRepository.observeItems()`, `deleteItem()`, `updateItem()` — do NOT modify
- `ItemsViewModel.deleteItem()` — do NOT modify
- `TrackerDatabase.sq` — do NOT modify (schema has `icon_id` column since Story 1.2)
- `ItemEditSheet` `showTimePicker`/`showWeekendTimePicker` inline pattern — preserve exactly; keep the entire `when` block intact
- `ItemEditSheet` `hideNavBar` / `awaitCancellation` `LaunchedEffect` — preserve exactly
- `AppNavigation.kt` and `LocalHideNavBar` — do NOT modify
- `SharedModule.kt`, `AndroidModule.kt`, `IosModule.kt` — do NOT modify
- `DatabaseDriverFactory.ios.kt` — do NOT touch
- `PlatformTimePickerDialog` expect/actual — do NOT modify

### Project Structure Notes

New files:
```
gradle/libs.versions.toml                                                    (modified)
shared/build.gradle.kts                                                      (modified)
shared/src/commonMain/composeResources/drawable/ic_capsule.xml              (NEW)
shared/src/commonMain/composeResources/drawable/ic_supplement_bottle.xml    (NEW)
shared/src/commonMain/composeResources/drawable/ic_herb.xml                 (NEW)
shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/ItemIcons.kt        (NEW)
shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/IconPickerGrid.kt   (NEW)
```

Modified files:
```
shared/src/commonMain/kotlin/com/sonja/tracker/data/repository/ItemRepository.kt
shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/ItemsViewModel.kt
shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/ItemEditSheet.kt
shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/ItemsScreen.kt
shared/src/commonMain/kotlin/com/sonja/tracker/ui/components/ItemRow.kt
shared/src/commonTest/kotlin/com/sonja/tracker/data/repository/ItemRepositoryTest.kt
_bmad-output/implementation-artifacts/deferred-work.md
_bmad-output/implementation-artifacts/sprint-status.yaml
```

### References

- Architecture: Icon storage strategy — [architecture.md, "Image & Icon Storage" section]: `icon_id` stored as nullable TEXT; rendered at display time by mapping ID → ImageVector; null icon ID + null image path = default placeholder
- Architecture: Material Icons Extended — ~20–24 icons from Material Icons Extended + 3-4 Phosphor/Lucide custom SVGs in `commonMain/composeResources/drawable/`
- UX spec: `IconPickerGrid` — scrollable grid of rounded-square tiles; selected tile indigo tint border; prominent Skip button [ux-design-specification.md, "IconPickerGrid" component section]
- UX spec: `ItemEditSheet` anatomy — "Name field → icon picker grid (auto-opens after name entry, Skip visible) → weekday time row → ..." [ux-design-specification.md, "ItemEditSheet" component section]
- Epic 2.5 AC — icon picker auto-opens after name entry; ~24 icons in scrollable grid; selected tile indigo border; Skip stores NULL; previously-selected icon shown in edit mode [epics.md, "Story 2.5: Icon Picker"]
- Deferred work fixed — *editItem reads stale uiState for iconId* [deferred-work.md, "Deferred from code review of 2-4-edit-and-delete-item"]

---

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- Added `material-icons-extended` (v1.7.3) to `libs.versions.toml` and `shared/build.gradle.kts`; verified dependency resolves for iOS and Android targets.
- Created 3 custom SVG vector drawables (`ic_capsule.xml`, `ic_supplement_bottle.xml`, `ic_herb.xml`) in `commonMain/composeResources/drawable/`; Compose Resources generated accessors confirmed via build output.
- Created `ItemIcons.kt` with `object ItemIcons` (20 Material Outlined icons + 3 custom SVGs) and `@Composable fun ItemIconContent` shared renderer.
- Created `IconPickerGrid.kt` using `FlowRow` (not `LazyVerticalGrid`) to avoid crash inside `verticalScroll`; 23 icons at 4/row; selected tile shows 2dp primary border + `primaryContainer` background.
- Updated `ItemRepository.addItem` signature to accept `iconId: String? = null`; existing test calls unchanged.
- Updated `ItemsViewModel.addItem` and `editItem` — `iconId` now passed directly in `editItem` instead of reading from stale `uiState`, resolving the deferred-work risk.
- Updated `ItemEditSheet`: new `initialIconId` parameter, `iconPickerVisible` state (auto-visible in edit mode), `verticalScroll` on Column, `KeyboardActions(onDone)` on name field, `IconPickerGrid` section before time pickers, `selectedIconId` forwarded in `onSave`.
- Updated `ItemsScreen` both `onSave` lambdas to 4-parameter form; edit sheet passes `initialIconId = item.iconId`.
- Updated `ItemRow`: renders `ItemIconContent` at 24dp when `item.iconId != null`; `contentDescription` is `"[name] icon"` or `"No icon"`.
- Added 2 new repository tests: `addItem_withIconId_storesIconId` and `addItem_withNullIconId_storesNull`; all 8 tests pass.
- All 4 final build targets passed: `testDebugUnitTest`, `shared:assembleDebug`, `androidApp:assembleDebug`, `compileKotlinIosSimulatorArm64`.

### File List

- `gradle/libs.versions.toml` (modified)
- `shared/build.gradle.kts` (modified)
- `shared/src/commonMain/composeResources/drawable/ic_capsule.xml` (NEW)
- `shared/src/commonMain/composeResources/drawable/ic_supplement_bottle.xml` (NEW)
- `shared/src/commonMain/composeResources/drawable/ic_herb.xml` (NEW)
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/ItemIcons.kt` (NEW)
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/IconPickerGrid.kt` (NEW)
- `shared/src/commonMain/kotlin/com/sonja/tracker/data/repository/ItemRepository.kt` (modified)
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/ItemsViewModel.kt` (modified)
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/ItemEditSheet.kt` (modified)
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/ItemsScreen.kt` (modified)
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/components/ItemRow.kt` (modified)
- `shared/src/commonTest/kotlin/com/sonja/tracker/data/repository/ItemRepositoryTest.kt` (modified)
- `_bmad-output/implementation-artifacts/deferred-work.md` (modified)
- `_bmad-output/implementation-artifacts/sprint-status.yaml` (modified)
- `_bmad-output/implementation-artifacts/2-5-icon-picker.md` (modified)
