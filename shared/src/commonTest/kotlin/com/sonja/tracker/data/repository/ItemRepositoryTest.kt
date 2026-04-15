package com.sonja.tracker.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sonja.tracker.TrackerDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ItemRepositoryTest {

    private fun createDriver(): JdbcSqliteDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TrackerDatabase.Schema.create(driver)
        return driver
    }

    @Test
    fun observeItems_emitsInsertedItem() = runTest {
        val db = TrackerDatabase(createDriver())
        val repo = ItemRepository(db)

        db.trackerDatabaseQueries.insertItem("Vitamin D3", "08:00", null, null, null)

        val items = repo.observeItems().first()
        assertEquals(1, items.size)
        assertEquals("Vitamin D3", items[0].name)
    }

    @Test
    fun addItem_insertsItemObservableViaFlow() = runTest {
        val db = TrackerDatabase(createDriver())
        val repo = ItemRepository(db)

        repo.addItem("Vitamin C", "09:00", null)

        val items = repo.observeItems().first()
        assertEquals(1, items.size)
        assertEquals("Vitamin C", items[0].name)
        assertEquals("09:00", items[0].reminderWeekdayTime)
    }

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

    @Test
    fun observeItems_emitsEmptyAfterDelete() = runTest {
        val db = TrackerDatabase(createDriver())
        val repo = ItemRepository(db)

        db.trackerDatabaseQueries.insertItem("Magnesium", "21:00", null, null, null)
        db.trackerDatabaseQueries.deleteItemById(1L) // fresh in-memory DB; first AUTOINCREMENT id is always 1

        val items = repo.observeItems().first()
        assertEquals(0, items.size)
    }
}
