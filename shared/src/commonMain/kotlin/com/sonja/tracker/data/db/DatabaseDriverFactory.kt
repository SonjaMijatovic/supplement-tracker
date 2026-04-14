package com.sonja.tracker.data.db

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory(context: Any? = null) {
    fun createDriver(): SqlDriver
}
