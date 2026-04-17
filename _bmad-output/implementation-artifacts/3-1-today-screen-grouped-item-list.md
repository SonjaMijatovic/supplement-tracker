# Story 3.1: Today Screen — Grouped Item List

Status: done

## Story

As a user,
I want to open the app and immediately see all of today's items grouped by reminder time slot,
so that I know exactly what's pending and what I've already taken at a glance.

## Acceptance Criteria

1. **Given** the app launches **When** the Today tab is displayed **Then** `TodayViewModel` exposes a `StateFlow<TodayUiState>` sealed class with `Loading`, `Success(groups, allLogged)`, and `Error` states

2. **Given** no items have been configured **When** the Today screen renders in `Success` state with an empty item list **Then** an empty state is displayed with a warm illustration, "Add your first item" prompt, and a filled button navigating to the Items tab

3. **Given** items exist **When** the Today screen renders **Then** items are displayed in a `LazyColumn` with `TimeGroupSection` headers grouping items by their reminder time slot (weekday or weekend time, selected based on current day of week) **And** the screen is fully interactive within 2 seconds of app launch on mid-range devices (NFR1)

4. **Given** multiple items share the same reminder time **When** displayed on the Today screen **Then** they appear under a single `TimeGroupSection` header for that time slot

5. **Given** items have different reminder times **When** displayed on the Today screen **Then** each unique time slot has its own `TimeGroupSection` header, ordered chronologically

## Tasks / Subtasks

- [x] Task 1: Add `TimeGroup` domain model (AC: 3, 4, 5)
  - [x] Create `shared/src/commonMain/kotlin/com/sonja/tracker/domain/model/TimeGroup.kt`
  - [x] Fields: `timeSlot: String` ("HH:mm"), `items: List<Item>`, `allLogged: Boolean = false`
  - [x] Note: `allLogged` is always false in story 3.1; story 3.2 derives it from log entries

- [x] Task 2: Add `ItemRepository.observeTodayGroups()` (AC: 3, 4, 5)
  - [x] Add method to `ItemRepository.kt` returning `Flow<List<TimeGroup>>`
  - [x] Uses `kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).dayOfWeek`
  - [x] Weekday (Mon–Fri): use `item.reminderWeekdayTime`; Weekend (Sat–Sun): use `item.reminderWeekendTime ?: item.reminderWeekdayTime`
  - [x] Items with null effective time are excluded from grouping (shouldn't occur given add-item form requires weekday time)
  - [x] Groups sorted chronologically by `timeSlot` string (ISO HH:mm sorts correctly as text)
  - [x] Built on top of existing `observeItems()` flow via `.map { items -> ... }`

- [x] Task 3: Create `TodayUiState.kt` (AC: 1)
  - [x] Create `shared/src/commonMain/kotlin/com/sonja/tracker/ui/today/TodayUiState.kt`
  - [x] Follow exact architecture sealed class pattern (see Dev Notes)

- [x] Task 4: Create `TodayViewModel.kt` (AC: 1)
  - [x] Create `shared/src/commonMain/kotlin/com/sonja/tracker/ui/today/TodayViewModel.kt`
  - [x] Follow exact architecture ViewModel pattern (see Dev Notes)
  - [x] In story 3.1: `allLogged = groups.all { g -> g.allLogged }` evaluates to false (since `TimeGroup.allLogged = false`)
  - [x] Dependency: `ItemRepository` only — `LogRepository` added in story 3.2

- [x] Task 5: Create `TimeGroupSection.kt` composable (AC: 3, 4, 5)
  - [x] Create `shared/src/commonMain/kotlin/com/sonja/tracker/ui/components/TimeGroupSection.kt`
  - [x] Anatomy: time label + badge ("X pending") + `ItemRow` list below
  - [x] In story 3.1, badge always shows "X pending" (no overdue/done logic — that's story 3.3)
  - [x] Render existing `ItemRow(item = item, onClick = null)` for each item — no tap action yet
  - [x] Accept `group: TimeGroup` parameter

- [x] Task 6: Implement `TodayScreen.kt` (AC: 1, 2, 3)
  - [x] Replace stub (`Box` with `Text("Today")`) with full screen implementation
  - [x] Add `onNavigateToItems: () -> Unit` parameter
  - [x] Inject `ItemRepository` via `koinInject<ItemRepository>()` — same pattern as `ItemsScreen`
  - [x] Instantiate ViewModel: `val viewModel: TodayViewModel = viewModel { TodayViewModel(repository) }`
  - [x] `when` on sealed state: Loading → `CircularProgressIndicator`; Error → error text; Success → content
  - [x] Success + empty list → empty state (see UX details in Dev Notes)
  - [x] Success + groups → `LazyColumn` with `TimeGroupSection` per group
  - [x] Update `AppNavigation.kt` to pass `onNavigateToItems = { selectedTab = ItemsRoute }` to `TodayScreen`

- [x] Task 7: Tests for `observeTodayGroups()` (AC: 3, 4, 5)
  - [x] Add tests to existing `ItemRepositoryTest.kt` (in-memory SQLDelight driver already set up)
  - [x] Test: items with same weekday time are grouped together
  - [x] Test: items with different times produce separate groups, sorted chronologically
  - [x] Test: on a weekend day, items use `reminderWeekendTime` when set, else fall back to `reminderWeekdayTime`
  - [x] Note: `Clock.System` kept testable by passing `today: DayOfWeek? = null` — null uses real clock, explicit value used in tests

## Dev Notes

### Existing Code to Build On (Do Not Reinvent)

- **`ItemRow.kt`** already exists at `ui/components/ItemRow.kt` — renders item with icon/photo. For story 3.1 pass `onClick = null`. Story 3.2 will extend `ItemRow` to add `isLogged: Boolean` and updated semantics.
- **`ItemRepository.observeItems()`** — the pattern for reactive SQLDelight flows: `.asFlow().mapToList(Dispatchers.Default).map { ... }`. `observeTodayGroups()` is built on top of this exact flow via an additional `.map { items -> ... }`.
- **`ItemsScreen.kt`** — exact ViewModel instantiation pattern to copy: `val repository = koinInject<ItemRepository>(); val viewModel: ItemsViewModel = viewModel { ItemsViewModel(repository) }`
- **`LogRepository`** stub is already registered in `sharedModule`. No changes needed there for story 3.1.
- **`AppNavigation.kt`** — `TodayRoute -> TodayScreen(modifier = ...)` needs one new parameter added: `onNavigateToItems`.

### Architecture Blueprints (Copy Exactly)

**TodayUiState.kt:**
```kotlin
sealed class TodayUiState {
    object Loading : TodayUiState()
    data class Success(val groups: List<TimeGroup>, val allLogged: Boolean) : TodayUiState()
    data class Error(val message: String) : TodayUiState()
}
```

**TodayViewModel.kt:**
```kotlin
class TodayViewModel(private val repo: ItemRepository) : ViewModel() {
    val uiState: StateFlow<TodayUiState> = repo
        .observeTodayGroups(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).dayOfWeek)
        .map { groups -> TodayUiState.Success(groups, groups.all { g -> g.allLogged }) }
        .catch { emit(TodayUiState.Error(it.message ?: "Unknown error")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayUiState.Loading)
}
```
Note: architecture blueprint passes `today` from ViewModel to repo to keep business logic in commonMain and make it testable.

**observeTodayGroups signature (in ItemRepository):**
```kotlin
fun observeTodayGroups(today: DayOfWeek = Clock.System.now()
    .toLocalDateTime(TimeZone.currentSystemDefault()).dayOfWeek): Flow<List<TimeGroup>>
```
Default value so existing call sites don't break; tests pass explicit `DayOfWeek.MONDAY`, etc.

**TimeGroup.kt:**
```kotlin
data class TimeGroup(
    val timeSlot: String,       // "HH:mm" — matches items table reminder_*_time format
    val items: List<Item>,
    val allLogged: Boolean = false  // story 3.2 will compute from log entries
)
```

### UX: Empty State

Spec: warm illustration + "Add your first item" text + filled button navigating to Items tab.
- No custom illustration asset is required for story 3.1 — use a large `Icon(Icons.Default.List, ...)` or Material `Favorite` as warm placeholder at 80dp. Story 3.4 or polish pass can add a proper illustration.
- Filled `Button` (Material 3) with text "Add your first item", `onClick = onNavigateToItems`.
- Center the content vertically and horizontally in the available space.

### UX: TimeGroupSection

```
08:00 AM          3 pending
─────────────────────────────
[icon] Vitamin C
[icon] Fish Oil
[icon] Magnesium
```
- Time label: `MaterialTheme.typography.titleSmall`, formatted from "HH:mm" (e.g. "08:00" → display as "08:00 AM" or "8:00 AM" using `kotlinx-datetime` LocalTime formatting).
- Badge: `MaterialTheme.typography.labelMedium`, neutral color (story 3.3 adds overdue amber tinting).
- Divider or `HorizontalDivider` below header optional — match Items screen visual style.
- `ItemRow` rendered with no padding changes needed — existing `ItemRow` padding is correct.

### Day-of-Week Detection (kotlinx-datetime)

```kotlin
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

val today: DayOfWeek = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).dayOfWeek
val isWeekend = today == DayOfWeek.SATURDAY || today == DayOfWeek.SUNDAY
val effectiveTime = if (isWeekend) item.reminderWeekendTime ?: item.reminderWeekdayTime
                   else item.reminderWeekdayTime
```
`kotlinx-datetime 0.7.1` is already in `libs.versions.toml` and `commonMain` dependencies.

### Testability: Pass DayOfWeek to observeTodayGroups

Do NOT call `Clock.System.now()` inside `observeTodayGroups()` — this makes it impossible to test weekend/weekday branching without real-time mocking. Accept `today: DayOfWeek` as a parameter with a default value. The ViewModel resolves `Clock.System` and passes the result in. Tests pass an explicit value.

### State Pattern Rules (from architecture)

- `Loading` is the initial state — never expose uninitialised state.
- Composables `when`-branch on the sealed class — no `if (isLoading)` flags.
- State is immutable — emit a new `Success` instance, never mutate.
- Collect in composable body: `val uiState by viewModel.uiState.collectAsState()`

### Previous Story Learnings (from story 2.6)

- **Koin ViewModel pattern**: `koinInject<Repository>()` then `viewModel { ViewModel(repository) }` — NOT `koinViewModel()`. The `viewModel { }` factory lambda is the established pattern in this codebase.
- **No DI registration for ViewModels**: ViewModels are NOT registered in Koin modules (`sharedModule`/`androidModule`/`iosModule`). They are created inline via the `viewModel { }` factory in the screen composable.
- **`Dispatchers.Default` for DB work**: All SQLDelight `mapToList` calls use `Dispatchers.Default` — do the same in `observeTodayGroups`.
- **One ViewModel per screen**: `TodayViewModel` only — do not share with other screens.

### Project Structure Notes

New files for this story:
- `shared/src/commonMain/kotlin/com/sonja/tracker/domain/model/TimeGroup.kt`
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/today/TodayUiState.kt`
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/today/TodayViewModel.kt`
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/components/TimeGroupSection.kt`

Modified files:
- `shared/src/commonMain/kotlin/com/sonja/tracker/data/repository/ItemRepository.kt` (add method)
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/today/TodayScreen.kt` (replace stub)
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/navigation/AppNavigation.kt` (add callback)
- `shared/src/commonTest/kotlin/com/sonja/tracker/data/repository/ItemRepositoryTest.kt` (add tests)

### References

- Architecture sealed UiState + ViewModel pattern: `architecture.md#State Management Patterns`
- Architecture ViewModel blueprint code: `architecture.md` (TodayViewModel snippet)
- Architecture directory structure: `architecture.md#Complete Project Directory Structure`
- `kotlinx-datetime` usage: `architecture.md#Format Patterns` + existing pattern in repo
- UX empty state spec: `ux-design-specification.md#Empty and error states`
- UX TimeGroupSection spec: `ux-design-specification.md#TimeGroupSection`
- Story 3.2–3.4 for what this story intentionally defers (log state, overdue, all-done)
- Existing ViewModel instantiation pattern: `ui/items/ItemsScreen.kt:39-40`
- SQLDelight reactive flow pattern: `ui/items/ItemRepository.kt:57-72`

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

- **T2 Clock import**: `kotlinx.datetime.Clock` is a deprecated type alias in 0.7.1. Must use `kotlin.time.Clock` (Kotlin stdlib) for `Clock.System.now()`. `kotlinx.datetime` still provides `TimeZone`, `DayOfWeek`, and the `toLocalDateTime()` extension on `kotlin.time.Instant`.
- **T2 default param**: Default parameter expressions (`today: DayOfWeek = Clock.System.now()...`) cause KMP compilation issues. Used `today: DayOfWeek? = null` pattern instead — null resolves to current day inside the function body.
- **T4 type inference**: `.map { TodayUiState.Success(...) }.catch { emit(TodayUiState.Error(...)) }` fails type inference (infers `Flow<Success>` then can't emit `Error`). Fixed with explicit type param: `.map<_, TodayUiState> { ... }`.

### Completion Notes List

- ✅ `TimeGroup` domain model created — `timeSlot`, `items`, `allLogged = false` (story 3.2 will compute from log entries)
- ✅ `ItemRepository.observeTodayGroups(today: DayOfWeek? = null)` — groups items by effective reminder time, weekend-aware, sorted chronologically
- ✅ `TodayUiState` sealed class — Loading / Success(groups, allLogged) / Error
- ✅ `TodayViewModel` — StateFlow<TodayUiState> via observeTodayGroups(), WhileSubscribed(5000)
- ✅ `TimeGroupSection` composable — time label (formatted HH:mm → "8:00 AM"), "X pending" badge, ItemRow list with onClick=null
- ✅ `TodayScreen` fully implemented — Loading/Error/Success states, empty state with navigate-to-Items button, LazyColumn with TimeGroupSection per group
- ✅ `AppNavigation` updated — passes `onNavigateToItems = { selectedTab = ItemsRoute }` to TodayScreen
- ✅ `kotlinx-datetime` added to commonMain dependencies in `shared/build.gradle.kts`
- ✅ 6 new tests in `ItemRepositoryTest`: grouping, chronological sort, weekend time selection, fallback, empty, mixed-items
- ✅ All 17 tests pass (11 existing + 6 new), 0 failures
- ✅ Android (`testDebugUnitTest`) and iOS (`compileKotlinIosSimulatorArm64`) both green

### File List

**New files:**
- `shared/src/commonMain/kotlin/com/sonja/tracker/domain/model/TimeGroup.kt`
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/today/TodayUiState.kt`
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/today/TodayViewModel.kt`
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/components/TimeGroupSection.kt`

**Modified files:**
- `shared/build.gradle.kts` (added `kotlinx-datetime` to commonMain)
- `shared/src/commonMain/kotlin/com/sonja/tracker/data/repository/ItemRepository.kt` (added `observeTodayGroups`)
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/today/TodayScreen.kt` (full implementation replacing stub)
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/navigation/AppNavigation.kt` (added onNavigateToItems callback)
- `shared/src/commonTest/kotlin/com/sonja/tracker/data/repository/ItemRepositoryTest.kt` (6 new tests)
- `_bmad-output/implementation-artifacts/sprint-status.yaml`

### Review Findings

- [x] [Review][Patch] isWeekend captured at flow creation time — stale after midnight day boundary [ItemRepository.kt:observeTodayGroups]
- [x] [Review][Patch] Empty state uses CheckCircle icon; spec dev notes recommend Icons.Default.List or Favorite [TodayScreen.kt:TodayEmptyState]
- [x] [Review][Defer] formatTimeSlot: no hour-range validation — malformed stored value (e.g. "25:00") would render oddly [TimeGroupSection.kt:formatTimeSlot] — deferred, app controls time storage so unreachable in practice

### Change Log

- 2026-04-17: Implemented Story 3.1 — Today Screen Grouped Item List. Created TimeGroup model, TodayUiState sealed class, TodayViewModel, TimeGroupSection composable, full TodayScreen with empty/loading/error/success states, and AppNavigation callback. Added observeTodayGroups() to ItemRepository with weekend-aware grouping. All builds green, 17 tests passing.
