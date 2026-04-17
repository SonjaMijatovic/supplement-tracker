package com.sonja.tracker.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sonja.tracker.TrackerDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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

    @Test
    fun addItem_withImagePath_storesImagePath() = runTest {
        val db = TrackerDatabase(createDriver())
        val repo = ItemRepository(db)

        repo.addItem("Vitamin D3", "08:00", null, imagePath = "/some/path/img.jpg")

        val items = repo.observeItems().first()
        assertEquals(1, items.size)
        assertEquals("/some/path/img.jpg", items[0].imagePath)
    }

    @Test
    fun addItem_withNullImagePath_storesNull() = runTest {
        val db = TrackerDatabase(createDriver())
        val repo = ItemRepository(db)

        repo.addItem("Vitamin C", "09:00", null)  // default imagePath = null

        val items = repo.observeItems().first()
        assertNull(items[0].imagePath)
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

    // --- observeTodayGroups tests ---

    @Test
    fun observeTodayGroups_weekday_groupsItemsByWeekdayTime() = runTest {
        val db = TrackerDatabase(createDriver())
        val repo = ItemRepository(db)

        repo.addItem("Vitamin C", "08:00", null)
        repo.addItem("Fish Oil", "08:00", null)
        repo.addItem("Magnesium", "21:00", null)

        val groups = repo.observeTodayGroups(today = DayOfWeek.MONDAY).first()

        assertEquals(2, groups.size)
        assertEquals("08:00", groups[0].timeSlot)
        assertEquals(2, groups[0].items.size)
        assertEquals("21:00", groups[1].timeSlot)
        assertEquals(1, groups[1].items.size)
    }

    @Test
    fun observeTodayGroups_weekday_groupsSortedChronologically() = runTest {
        val db = TrackerDatabase(createDriver())
        val repo = ItemRepository(db)

        repo.addItem("Magnesium", "21:00", null)
        repo.addItem("Vitamin C", "08:00", null)
        repo.addItem("Fish Oil", "13:00", null)

        val groups = repo.observeTodayGroups(today = DayOfWeek.WEDNESDAY).first()

        assertEquals(3, groups.size)
        assertEquals("08:00", groups[0].timeSlot)
        assertEquals("13:00", groups[1].timeSlot)
        assertEquals("21:00", groups[2].timeSlot)
    }

    @Test
    fun observeTodayGroups_weekend_usesWeekendTimeWhenSet() = runTest {
        val db = TrackerDatabase(createDriver())
        val repo = ItemRepository(db)

        // Item with both weekday and weekend times
        repo.addItem("Vitamin C", "08:00", "10:00")

        val groups = repo.observeTodayGroups(today = DayOfWeek.SATURDAY).first()

        assertEquals(1, groups.size)
        assertEquals("10:00", groups[0].timeSlot)
    }

    @Test
    fun observeTodayGroups_weekend_fallsBackToWeekdayTimeWhenNoWeekendTime() = runTest {
        val db = TrackerDatabase(createDriver())
        val repo = ItemRepository(db)

        // Item with no weekend time
        repo.addItem("Vitamin C", "08:00", null)

        val groups = repo.observeTodayGroups(today = DayOfWeek.SUNDAY).first()

        assertEquals(1, groups.size)
        assertEquals("08:00", groups[0].timeSlot)
    }

    @Test
    fun observeTodayGroups_emptyWhenNoItems() = runTest {
        val db = TrackerDatabase(createDriver())
        val repo = ItemRepository(db)

        val groups = repo.observeTodayGroups(today = DayOfWeek.TUESDAY).first()

        assertEquals(0, groups.size)
    }

    @Test
    fun observeTodayGroups_weekend_mixedItemsGroupedCorrectly() = runTest {
        val db = TrackerDatabase(createDriver())
        val repo = ItemRepository(db)

        // Item A: weekday 08:00, weekend 10:00
        // Item B: weekday 08:00, no weekend time (falls back to 08:00)
        repo.addItem("Item A", "08:00", "10:00")
        repo.addItem("Item B", "08:00", null)

        val groups = repo.observeTodayGroups(today = DayOfWeek.SATURDAY).first()

        // Item A -> 10:00, Item B -> 08:00 (fallback) => 2 separate groups
        assertEquals(2, groups.size)
        assertEquals("08:00", groups[0].timeSlot)
        assertEquals("Item B", groups[0].items[0].name)
        assertEquals("10:00", groups[1].timeSlot)
        assertEquals("Item A", groups[1].items[0].name)
    }
}
