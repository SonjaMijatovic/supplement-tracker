package com.sonja.tracker.data.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.sonja.tracker.TrackerDatabase

actual class DatabaseDriverFactory actual constructor(private val context: Any?) {
    actual fun createDriver(): SqlDriver {
        val ctx = requireNotNull(context as? Context) {
            "Android DatabaseDriverFactory requires a non-null Context"
        }
        return AndroidSqliteDriver(
            schema = TrackerDatabase.Schema,
            context = ctx,
            name = "tracker.db"
        )
    }
}
