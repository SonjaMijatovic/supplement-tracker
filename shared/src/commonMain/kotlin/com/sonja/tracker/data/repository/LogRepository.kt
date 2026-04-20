package com.sonja.tracker.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.sonja.tracker.TrackerDatabase
import com.sonja.tracker.domain.model.LogState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class LogRepository(private val database: TrackerDatabase) {

    /**
     * Inserts a log entry for [itemId] with state "logged".
     * Pass explicit [date] ("YYYY-MM-DD") and [loggedAt] (ISO datetime) for test determinism.
     * Null values use the current system clock (production path).
     */
    suspend fun logItem(
        itemId: Long,
        date: String? = null,
        loggedAt: String? = null
    ) {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val effectiveDate = date ?: now.date.toString()
        val effectiveLoggedAt = loggedAt ?: now.toString()
        withContext(Dispatchers.Default) {
            database.trackerDatabaseQueries.insertLogEntry(
                item_id = itemId,
                date = effectiveDate,
                state = LogState.LOGGED.name.lowercase(),
                logged_at = effectiveLoggedAt
            )
        }
    }

    /**
     * Returns a reactive [Flow] of item IDs logged on [date].
     * Pass explicit [date] ("YYYY-MM-DD") for test determinism; null uses today.
     */
    fun observeTodayLoggedItemIds(date: String? = null): Flow<Set<Long>> {
        return database.trackerDatabaseQueries
            .selectAllLogEntries()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                // Compute date on each emission so the flow stays correct across midnight
                val today = date ?: Clock.System.now()
                    .toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
                rows.filter { it.date == today }.map { it.item_id }.toSet()
            }
    }
}
