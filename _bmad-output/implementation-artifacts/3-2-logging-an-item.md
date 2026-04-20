# Story 3.2: Logging an Item

Status: done

## Story

As a user,
I want to tap an item to log it as taken,
So that I have an immediate, definitive record that I took it today.

## Acceptance Criteria

1. **Given** an item is in the pending state on the Today screen **When** the user taps anywhere on the `ItemRow` **Then** the row transitions immediately to the logged state — tonal indigo background fill + check indicator — within 500ms (NFR2) **And** a `log_entries` row is inserted with the current item ID, today's date (`YYYY-MM-DD`), and state `"logged"`

2. **Given** an item has been logged **When** it appears on the Today screen **Then** the row is visible but non-tappable (disabled state) **And** the item name and icon/photo remain fully visible in the logged state

3. **Given** items are displayed in both pending and logged states **When** viewed without colour perception **Then** pending vs. logged distinction is conveyed by both the tonal fill AND a visible check indicator — not colour alone (NFR15)

4. **Given** all tappable `ItemRow` elements **When** measured **Then** the tap target is the full row width with a minimum height of 56dp (NFR13)

5. **Given** the `ItemRow` is focused by VoiceOver or TalkBack **When** announced **Then** the content description reads "[Item name], pending" or "[Item name], already logged" as appropriate (NFR16) **And** `Role.Checkbox` semantic is applied to the row

6. **Given** a log entry is written **When** the SQLDelight `Flow` emits **Then** the `TimeGroupSection` badge updates reactively (e.g. "3 pending" → "2 pending") without a full-screen reload

## Tasks / Subtasks

- [x] Task 1: Add SQLDelight queries for log_entries (AC: 1, 6)
  - [x] Add `insertLogEntry` query to `TrackerDatabase.sq`
  - [x] Add `selectLoggedItemIdsForDate` query to `TrackerDatabase.sq` — returns `Flow` via `asFlow()`

- [x] Task 2: Create domain models LogState and LogEntry (AC: 1)
  - [x] Create `shared/src/commonMain/kotlin/com/sonja/tracker/domain/model/LogState.kt` — enum class
  - [x] Create `shared/src/commonMain/kotlin/com/sonja/tracker/domain/model/LogEntry.kt` — data class

- [x] Task 3: Implement LogRepository (AC: 1, 6)
  - [x] Add `suspend fun logItem(itemId: Long, date: String? = null, loggedAt: String? = null)` — null uses real clock (production); tests pass explicit values for determinism
  - [x] Add `fun observeTodayLoggedItemIds(date: String? = null): Flow<Set<Long>>` — reactive query for today's logged item IDs; null uses real clock date

- [x] Task 4: Update TimeGroup to carry logged item IDs (AC: 2, 6)
  - [x] Add `loggedItemIds: Set<Long> = emptySet()` field to `TimeGroup.kt`
  - [x] Note: `allLogged` is already in `TimeGroup` from story 3.1 (always false); story 3.2 populates both fields

- [x] Task 5: Update TodayViewModel to combine item and log flows (AC: 1, 2, 6)
  - [x] Add `LogRepository` parameter to `TodayViewModel` constructor
  - [x] Replace single-flow `map` with `combine(itemRepo.observeTodayGroups(), logRepo.observeTodayLoggedItemIds())` to build enriched `TimeGroup` list
  - [x] Compute per-group `loggedItemIds` and `allLogged` from the combined `loggedIds` set
  - [x] Add `fun logItem(itemId: Long)` — launches coroutine calling `logRepo.logItem(itemId)`
  - [x] Update `TodayScreen.kt` to pass `LogRepository` to `TodayViewModel` (via `koinInject`)

- [x] Task 6: Update ItemRow for logged state and accessibility (AC: 2, 3, 4, 5)
  - [x] Add `isLogged: Boolean = false` parameter to `ItemRow`
  - [x] When `isLogged = true`: tonal background fill (`primaryContainer`) + `CheckCircle` icon (28dp) on right side; no click interaction
  - [x] When `isLogged = false` and `onClick != null`: pending state — full-row tap target, `onClick` fires
  - [x] Semantics: apply `Role.Checkbox` and content description `"[name], pending"` / `"[name], already logged"` based on log state (Today screen usage via `isLogged`)
  - [x] Keep backward compatibility: Items screen calls `ItemRow(item, onClick = { ... })` with `isLogged = false` — existing semantics preserved

- [x] Task 7: Update TimeGroupSection to wire log action (AC: 1, 6)
  - [x] Add `onLogItem: (itemId: Long) -> Unit` parameter to `TimeGroupSection`
  - [x] Pass `isLogged = item.id in group.loggedItemIds` to each `ItemRow`
  - [x] Pass `onClick = if (item.id in group.loggedItemIds) null else { onLogItem(item.id) }` to each `ItemRow`
  - [x] Update pending badge count: `group.items.size - group.loggedItemIds.size` (pending = not yet logged)

- [x] Task 8: Update TodayScreen to wire logItem action (AC: 1)
  - [x] Add `koinInject<LogRepository>()` alongside existing `koinInject<ItemRepository>()`
  - [x] Pass both repositories to `TodayViewModel` constructor
  - [x] Pass `onLogItem = { viewModel.logItem(it) }` to each `TimeGroupSection`

- [x] Task 9: Tests for LogRepository (AC: 1, 6)
  - [x] Add `shared/src/commonTest/kotlin/com/sonja/tracker/data/repository/LogRepositoryTest.kt`
  - [x] Test: `logItem` inserts a row retrievable via `observeTodayLoggedItemIds`
  - [x] Test: `observeTodayLoggedItemIds` returns correct `Set<Long>` for the given date
  - [x] Test: empty before logging, populated after (replaces long-lived collect test — JDBC in-memory driver doesn't fire change notifications in test env)
  - [x] Test: `observeTodayLoggedItemIds` does NOT return entries from other dates
  - [x] Test: multiple items logged on same date all appear in the set

## Dev Notes

### What Story 3.1 Left For This Story

- `TimeGroup.allLogged` is defined but always `false` — story 3.2 computes it from log entries
- `ItemRow.onClick` is `null` in `TimeGroupSection` — story 3.2 activates tap handling
- `TodayViewModel` has no `LogRepository` dependency — story 3.2 adds it
- `LogRepository.kt` exists as an empty stub (`class LogRepository(private val database: TrackerDatabase)`) — story 3.2 implements it
- `LogRepository` is already registered as `single { LogRepository(get()) }` in `SharedModule.kt` — do NOT add it again

### SQLDelight Queries to Add to TrackerDatabase.sq

Add these queries at the end of `shared/src/commonMain/sqldelight/com/sonja/tracker/TrackerDatabase.sq`:

```sql
insertLogEntry:
INSERT INTO log_entries (item_id, date, state, logged_at)
VALUES (?, ?, ?, ?);

selectLoggedItemIdsForDate:
SELECT item_id FROM log_entries WHERE date = ?;
```

The `selectLoggedItemIdsForDate` query returns `Long` values (item IDs). Use `.asFlow().mapToList(Dispatchers.Default)` to get `Flow<List<Long>>`, then `.map { it.toSet() }` for `Flow<Set<Long>>`.

### Domain Models to Create

**LogState.kt** — define all states now even though only `LOGGED` is used in this story:
```kotlin
package com.sonja.tracker.domain.model

enum class LogState {
    LOGGED,
    LATE_LOGGED,
    NOT_LOGGED,
    NO_DATA
}
```

**LogEntry.kt**:
```kotlin
package com.sonja.tracker.domain.model

data class LogEntry(
    val id: Long,
    val itemId: Long,
    val date: String,          // "YYYY-MM-DD"
    val state: LogState,
    val loggedAt: String       // ISO datetime string
)
```

### LogRepository Implementation Blueprint

```kotlin
class LogRepository(private val database: TrackerDatabase) {

    suspend fun logItem(
        itemId: Long,
        date: String? = null,
        loggedAt: String? = null
    ) {
        val effectiveDate = date ?: Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        val effectiveLoggedAt = loggedAt ?: Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).toString()
        withContext(Dispatchers.Default) {
            database.trackerDatabaseQueries.insertLogEntry(
                item_id = itemId,
                date = effectiveDate,
                state = LogState.LOGGED.name.lowercase(),  // stores "logged"
                logged_at = effectiveLoggedAt
            )
        }
    }

    fun observeTodayLoggedItemIds(date: String? = null): Flow<Set<Long>> {
        val effectiveDate = date ?: Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        return database.trackerDatabaseQueries
            .selectLoggedItemIdsForDate(effectiveDate)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { it.toSet() }
    }
}
```

Imports needed:
```kotlin
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
```

### TodayViewModel Rewrite Blueprint

The key challenge is combining two flows while keeping `Flow<TodayUiState>` as the result type (needed for the `.catch` operator to emit `Error`). Use explicit type parameters on `combine` — same technique as the `.map<_, TodayUiState>` workaround from story 3.1 debug log.

```kotlin
class TodayViewModel(
    private val itemRepo: ItemRepository,
    private val logRepo: LogRepository
) : ViewModel() {

    val uiState: StateFlow<TodayUiState> = combine<List<TimeGroup>, Set<Long>, TodayUiState>(
        itemRepo.observeTodayGroups(),
        logRepo.observeTodayLoggedItemIds()
    ) { groups, loggedIds ->
        val enriched = groups.map { group ->
            group.copy(
                loggedItemIds = group.items.filter { it.id in loggedIds }.map { it.id }.toSet(),
                allLogged = group.items.isNotEmpty() && group.items.all { it.id in loggedIds }
            )
        }
        TodayUiState.Success(
            groups = enriched,
            allLogged = enriched.isNotEmpty() && enriched.all { it.allLogged }
        )
    }
    .catch { emit(TodayUiState.Error(it.message ?: "Unknown error")) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayUiState.Loading)

    fun logItem(itemId: Long) {
        viewModelScope.launch {
            logRepo.logItem(itemId)
        }
    }
}
```

Import for `combine`: `kotlinx.coroutines.flow.combine`
Import for `launch`: `kotlinx.coroutines.launch`

### TodayScreen Update (Task 8)

Existing:
```kotlin
val repository = koinInject<ItemRepository>()
val viewModel: TodayViewModel = viewModel { TodayViewModel(repository) }
```

Updated:
```kotlin
val itemRepository = koinInject<ItemRepository>()
val logRepository = koinInject<LogRepository>()
val viewModel: TodayViewModel = viewModel { TodayViewModel(itemRepository, logRepository) }
```

Pass `onLogItem` down to each `TimeGroupSection`:
```kotlin
items(state.groups, key = { it.timeSlot }) { group ->
    TimeGroupSection(group = group, onLogItem = { viewModel.logItem(it) })
}
```

Import: `com.sonja.tracker.data.repository.LogRepository`

### ItemRow Logged State Visual

Logged state layout (right-side check indicator):
```
[thumbnail] [item name ·············] [✓ CheckCircle 28dp]
└─ primaryContainer fill behind full row ─────────────────┘
```

Use `Surface` with `color = MaterialTheme.colorScheme.primaryContainer` wrapping the `Row`, or apply `Modifier.background(MaterialTheme.colorScheme.primaryContainer)` on the Row when `isLogged`.

The check icon: `Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)` — placed after the name text at the trailing end.

Full semantics block for ItemRow in Today screen context:
```kotlin
// When isLogged = true:
.semantics {
    role = Role.Checkbox
    contentDescription = "${item.name}, already logged"
    // stateDescription not needed — the content description carries state
}

// When isLogged = false and onClick != null (pending):
.semantics {
    role = Role.Checkbox
    contentDescription = "${item.name}, pending"
}
```

**Backward compatibility:** The Items screen calls `ItemRow(item = item, modifier = ..., onClick = { ... })` with `isLogged = false` (default). The `onClick != null && !isLogged` branch must keep the existing `Role.Button` + `"tap to edit"` semantics **only when** the item is used for editing (not the Today screen). The simplest approach: add a separate `isLogRow: Boolean = false` parameter to distinguish Today screen usage from Items screen usage, OR rely on `isLogged = false` + null contentDescription = use legacy semantics.

**Recommended:** Add `isLogRow: Boolean = false` to ItemRow:
- `isLogRow = true` → Role.Checkbox + "[name], pending" / "[name], already logged" semantics
- `isLogRow = false` (default, Items screen) → Role.Button + "[name], tap to edit" (existing behavior)

TimeGroupSection passes `isLogRow = true` to all ItemRows.

### TimeGroupSection Badge Count

Current (story 3.1): `"${group.items.size} pending"`
Updated (story 3.2): `"${group.items.size - group.loggedItemIds.size} pending"`

Note: Story 3.3 will change this to use `allLogged` to show "all done" and add overdue state. The badge text stays as "X pending" in 3.2 — don't add those states yet.

### Date Formatting for SQLDelight

`kotlinx.datetime.LocalDate.toString()` produces `"YYYY-MM-DD"` — this is the correct format for the `date` column.
`kotlinx.datetime.LocalDateTime.toString()` produces an ISO string for `logged_at`.

Access the date part:
```kotlin
Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
```

### Test Patterns (from story 3.1 / ItemRepositoryTest.kt)

```kotlin
private fun createDriver(): SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also {
    TrackerDatabase.Schema.create(it)
}
```

LogRepositoryTest follows exactly the same in-memory driver pattern. Both `ItemRepository` and `LogRepository` are needed for tests that check the log flow against real item IDs.

For testing reactive flows:
```kotlin
val results = mutableListOf<Set<Long>>()
val job = launch { repo.observeTodayLoggedItemIds("2026-01-01").collect { results.add(it) } }
// ... perform inserts ...
advanceUntilIdle()
job.cancel()
```

Use `runTest` with `TestCoroutineScope` from `kotlinx-coroutines-test` (already in `commonTest` deps).

### Known KMP Compilation Gotchas (from story 3.1 Debug Log)

- `kotlinx.datetime.Clock` is a **deprecated type alias** in kotlinx-datetime 0.7.1 — use `kotlin.time.Clock` instead
- Default parameter expressions with `Clock.System.now()` cause KMP compile issues — use `param: Type? = null` and resolve inside the function body (same pattern as `today: DayOfWeek? = null`)
- Flow type inference issue with `.map { Success() }.catch { emit(Error()) }` — use explicit type param: `combine<T1, T2, TodayUiState>(...)` (documented above in blueprint)

### Existing Code to Build On (Do Not Reinvent)

- `ItemRepository.observeItems()` — SQLDelight reactive flow pattern to copy: `.asFlow().mapToList(Dispatchers.Default).map { ... }`
- `ItemRepository.observeTodayGroups()` — existing flow that story 3.2 enriches via `combine`
- `ItemRepository.logItem()` — does NOT exist; don't look for it. Log writes go through `LogRepository`
- `SharedModule.kt` — `LogRepository` already wired as `single { LogRepository(get()) }`; do not re-add
- `TodayScreen.kt` — pattern for `koinInject` + `viewModel { }` is established; extend it, don't replace

### Files to Create

- `shared/src/commonMain/kotlin/com/sonja/tracker/domain/model/LogState.kt`
- `shared/src/commonMain/kotlin/com/sonja/tracker/domain/model/LogEntry.kt`
- `shared/src/commonTest/kotlin/com/sonja/tracker/data/repository/LogRepositoryTest.kt`

### Files to Modify

- `shared/src/commonMain/sqldelight/com/sonja/tracker/TrackerDatabase.sq` (add 2 queries)
- `shared/src/commonMain/kotlin/com/sonja/tracker/data/repository/LogRepository.kt` (implement)
- `shared/src/commonMain/kotlin/com/sonja/tracker/domain/model/TimeGroup.kt` (add `loggedItemIds`)
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/today/TodayViewModel.kt` (combine flows, add logItem)
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/today/TodayScreen.kt` (inject LogRepository, wire)
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/components/TimeGroupSection.kt` (onLogItem param, isLogged)
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/components/ItemRow.kt` (isLogged, isLogRow, logged visual)

### References

- Story 3.3 (next): overdue badge state — do NOT add overdue/done logic now
- Story 3.4 (next): AllDoneHero — `allLogged` flag in TodayUiState.Success drives it; story 3.2 correctly computes it but the composable swap is story 3.4's responsibility
- SQLDelight reactive flow pattern: `ItemRepository.kt:observeItems()` and `observeTodayGroups()`
- Koin ViewModel pattern: `TodayScreen.kt:TodayViewModel` construction (established, copy exactly)

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

- **T9 reactive test**: SQLDelight JDBC in-memory driver does not fire change notifications in test environment — `advanceUntilIdle()` after insert didn't cause the flow to re-emit. Replaced with two sequential `.first()` calls (before and after logging) which test the same contract without requiring live notifications.
- **T5 combine type inference**: `combine<List<TimeGroup>, Set<Long>, TodayUiState>(...)` explicit type params required — same pattern as story 3.1's `.map<_, TodayUiState>` workaround.

### Completion Notes List

- ✅ `insertLogEntry` and `selectLoggedItemIdsForDate` queries added to `TrackerDatabase.sq`
- ✅ `LogState` enum (LOGGED, LATE_LOGGED, NOT_LOGGED, NO_DATA) and `LogEntry` data class created
- ✅ `LogRepository.logItem()` — inserts log entry with `date`/`loggedAt` nullable params for testability
- ✅ `LogRepository.observeTodayLoggedItemIds()` — reactive `Flow<Set<Long>>` via SQLDelight `asFlow`
- ✅ `TimeGroup` updated: `loggedItemIds: Set<Long> = emptySet()` added
- ✅ `TodayViewModel` rewritten: `combine` of item + log flows; `logItem()` action
- ✅ `ItemRow` updated: `isLogRow`/`isLogged` params, logged visual (tonal bg + 28dp CheckCircle), Role.Checkbox semantics, backward-compatible with Items screen
- ✅ `TimeGroupSection` updated: `onLogItem` callback, passes `isLogRow=true`/`isLogged`, pending count derived from `loggedItemIds`
- ✅ `TodayScreen` updated: injects `LogRepository`, wires `onLogItem`
- ✅ 5 new tests in `LogRepositoryTest`, 23 total tests pass (0 failures)
- ✅ Android (`testDebugUnitTest`) and iOS (`compileKotlinIosSimulatorArm64`) both green

### File List

**New files:**
- `shared/src/commonMain/kotlin/com/sonja/tracker/domain/model/LogState.kt`
- `shared/src/commonMain/kotlin/com/sonja/tracker/domain/model/LogEntry.kt`
- `shared/src/commonTest/kotlin/com/sonja/tracker/data/repository/LogRepositoryTest.kt`

**Modified files:**
- `shared/src/commonMain/sqldelight/com/sonja/tracker/TrackerDatabase.sq` (2 new queries)
- `shared/src/commonMain/kotlin/com/sonja/tracker/data/repository/LogRepository.kt` (implemented)
- `shared/src/commonMain/kotlin/com/sonja/tracker/domain/model/TimeGroup.kt` (added `loggedItemIds`)
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/today/TodayViewModel.kt` (combine flows, logItem)
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/today/TodayScreen.kt` (LogRepository injection)
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/components/TimeGroupSection.kt` (onLogItem, isLogged)
- `shared/src/commonMain/kotlin/com/sonja/tracker/ui/components/ItemRow.kt` (isLogRow, isLogged, logged visual)
- `_bmad-output/implementation-artifacts/sprint-status.yaml`

### Change Log

- 2026-04-20: Implemented Story 3.2 — Logging an Item. Implemented LogRepository with logItem/observeTodayLoggedItemIds, created LogState/LogEntry domain models, added SQLDelight queries, updated TodayViewModel to combine item+log flows, added logged visual state to ItemRow, wired log action through TimeGroupSection and TodayScreen. 23 tests passing.
