package com.sonja.tracker.di

import com.sonja.tracker.TrackerDatabase
import com.sonja.tracker.data.db.DatabaseDriverFactory
import com.sonja.tracker.data.repository.ItemRepository
import com.sonja.tracker.data.repository.LogRepository
import org.koin.dsl.module

val sharedModule = module {
    single { get<DatabaseDriverFactory>().createDriver() }
    single { TrackerDatabase(get()) }
    single { ItemRepository(get()) }
    single { LogRepository(get()) }
}
