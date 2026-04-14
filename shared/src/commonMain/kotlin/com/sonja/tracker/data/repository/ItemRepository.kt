package com.sonja.tracker.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.sonja.tracker.TrackerDatabase
import com.sonja.tracker.domain.model.Item
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ItemRepository(private val database: TrackerDatabase) {
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
