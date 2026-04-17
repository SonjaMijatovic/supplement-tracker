package com.sonja.tracker.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.sonja.tracker.TrackerDatabase
import com.sonja.tracker.domain.model.Item
import com.sonja.tracker.domain.model.TimeGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class ItemRepository(private val database: TrackerDatabase) {
    suspend fun addItem(
        name: String,
        weekdayTime: String,
        weekendTime: String?,
        iconId: String? = null,
        imagePath: String? = null
    ) {
        withContext(Dispatchers.Default) {
            database.trackerDatabaseQueries.insertItem(
                name = name,
                reminder_weekday_time = weekdayTime,
                reminder_weekend_time = weekendTime,
                image_path = imagePath,
                icon_id = iconId
            )
        }
    }

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

    /**
     * Returns items grouped by their effective reminder time for [today].
     * Weekdays use reminderWeekdayTime; weekends use reminderWeekendTime ?? reminderWeekdayTime.
     * Items with no effective time are excluded. Groups are sorted chronologically.
     * Pass null for [today] to use the current system day (production path).
     * Tests pass an explicit [DayOfWeek] for deterministic behaviour.
     */
    fun observeTodayGroups(today: DayOfWeek? = null): Flow<List<TimeGroup>> {
        return observeItems().map { items ->
            val effectiveDay = today ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).dayOfWeek
            val isWeekend = effectiveDay == DayOfWeek.SATURDAY || effectiveDay == DayOfWeek.SUNDAY
            items
                .mapNotNull { item ->
                    val slot = if (isWeekend) item.reminderWeekendTime ?: item.reminderWeekdayTime
                               else item.reminderWeekdayTime
                    slot?.let { item to it }
                }
                .groupBy { (_, slot) -> slot }
                .entries
                .sortedBy { (slot, _) -> slot }
                .map { (slot, pairs) ->
                    TimeGroup(timeSlot = slot, items = pairs.map { (item, _) -> item })
                }
        }
    }

    fun observeItems(): Flow<List<Item>> =
        database.trackerDatabaseQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                rows.map { row ->
                    Item(
                        id = row.id,
                        name = row.name,
                        reminderWeekdayTime = row.reminder_weekday_time,
                        reminderWeekendTime = row.reminder_weekend_time,
                        imagePath = row.image_path,
                        iconId = row.icon_id
                    )
                }
            }
}
