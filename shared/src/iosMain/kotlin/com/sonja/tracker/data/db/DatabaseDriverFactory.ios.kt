package com.sonja.tracker.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.sonja.tracker.TrackerDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual class DatabaseDriverFactory actual constructor(private val context: Any?) {
    @OptIn(ExperimentalForeignApi::class)
    actual fun createDriver(): SqlDriver {
        val paths = NSSearchPathForDirectoriesInDomains(
            NSApplicationSupportDirectory,
            NSUserDomainMask,
            true
        )
        val supportDir = paths.firstOrNull() as? String
            ?: error("NSApplicationSupportDirectory not found")

        // Ensure directory exists before SQLite open (may not exist on fresh install)
        NSFileManager.defaultManager.createDirectoryAtPath(
            supportDir,
            true,
            null,
            null
        )

        return NativeSqliteDriver(
            schema = TrackerDatabase.Schema,
            name = "tracker.db",
            onConfiguration = { config ->
                config.copy(
                    extendedConfig = config.extendedConfig.copy(basePath = supportDir)
                )
            }
        )
    }
}
