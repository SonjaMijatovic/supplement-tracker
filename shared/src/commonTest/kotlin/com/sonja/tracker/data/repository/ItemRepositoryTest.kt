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
    fun observeItems_emitsEmptyAfterDelete() = runTest {
        val db = TrackerDatabase(createDriver())
        val repo = ItemRepository(db)

        db.trackerDatabaseQueries.insertItem("Magnesium", "21:00", null, null, null)
        val inserted = db.trackerDatabaseQueries.selectAll().executeAsList()
        db.trackerDatabaseQueries.deleteItemById(inserted[0].id)

        val items = repo.observeItems().first()
        assertEquals(0, items.size)
    }
}
