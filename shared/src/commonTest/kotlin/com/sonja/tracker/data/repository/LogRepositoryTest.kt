package com.sonja.tracker.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sonja.tracker.TrackerDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LogRepositoryTest {

    private fun createDriver(): JdbcSqliteDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TrackerDatabase.Schema.create(driver)
        return driver
    }

    // --- logItem and observeTodayLoggedItemIds ---

    @Test
    fun logItem_insertsEntryRetrievableViaObserve() = runTest {
        val db = TrackerDatabase(createDriver())
        val itemRepo = ItemRepository(db)
        val logRepo = LogRepository(db)

        itemRepo.addItem("Vitamin C", "08:00", null)
        val itemId = itemRepo.observeItems().first().first().id

        logRepo.logItem(itemId, date = "2026-01-01", loggedAt = "2026-01-01T08:05:00")

        val loggedIds = logRepo.observeTodayLoggedItemIds("2026-01-01").first()
        assertTrue(itemId in loggedIds, "Expected itemId $itemId in logged set $loggedIds")
    }

    @Test
    fun observeTodayLoggedItemIds_returnsCorrectSetForDate() = runTest {
        val db = TrackerDatabase(createDriver())
        val itemRepo = ItemRepository(db)
        val logRepo = LogRepository(db)

        itemRepo.addItem("Vitamin C", "08:00", null)
        itemRepo.addItem("Fish Oil", "08:00", null)
        val items = itemRepo.observeItems().first()
        val idA = items[0].id
        val idB = items[1].id

        logRepo.logItem(idA, date = "2026-01-01", loggedAt = "2026-01-01T08:05:00")

        val loggedIds = logRepo.observeTodayLoggedItemIds("2026-01-01").first()
        assertEquals(setOf(idA), loggedIds)
        assertFalse(idB in loggedIds)
    }

    @Test
    fun observeTodayLoggedItemIds_doesNotReturnOtherDateEntries() = runTest {
        val db = TrackerDatabase(createDriver())
        val itemRepo = ItemRepository(db)
        val logRepo = LogRepository(db)

        itemRepo.addItem("Vitamin C", "08:00", null)
        val itemId = itemRepo.observeItems().first().first().id

        // Log on a different date
        logRepo.logItem(itemId, date = "2026-01-02", loggedAt = "2026-01-02T08:05:00")

        val loggedIds = logRepo.observeTodayLoggedItemIds("2026-01-01").first()
        assertTrue(loggedIds.isEmpty(), "Expected empty set for 2026-01-01, got $loggedIds")
    }

    @Test
    fun observeTodayLoggedItemIds_multipleItemsOnSameDateAllAppear() = runTest {
        val db = TrackerDatabase(createDriver())
        val itemRepo = ItemRepository(db)
        val logRepo = LogRepository(db)

        itemRepo.addItem("Vitamin C", "08:00", null)
        itemRepo.addItem("Fish Oil", "08:00", null)
        itemRepo.addItem("Magnesium", "21:00", null)
        val items = itemRepo.observeItems().first()
        val ids = items.map { it.id }

        ids.forEach { id ->
            logRepo.logItem(id, date = "2026-01-01", loggedAt = "2026-01-01T08:05:00")
        }

        val loggedIds = logRepo.observeTodayLoggedItemIds("2026-01-01").first()
        assertEquals(ids.toSet(), loggedIds)
    }

    @Test
    fun observeTodayLoggedItemIds_emptyBeforeLogAndPopulatedAfter() = runTest {
        // Tests that the flow reflects state before and after a log insert
        val db = TrackerDatabase(createDriver())
        val itemRepo = ItemRepository(db)
        val logRepo = LogRepository(db)

        itemRepo.addItem("Vitamin C", "08:00", null)
        val itemId = itemRepo.observeItems().first().first().id

        // Before logging: set is empty
        val before = logRepo.observeTodayLoggedItemIds("2026-01-01").first()
        assertTrue(before.isEmpty(), "Expected empty set before logging, got $before")

        // After logging: set contains the item
        logRepo.logItem(itemId, date = "2026-01-01", loggedAt = "2026-01-01T08:05:00")
        val after = logRepo.observeTodayLoggedItemIds("2026-01-01").first()
        assertTrue(itemId in after, "Expected $itemId in set after logging, got $after")
    }
}
