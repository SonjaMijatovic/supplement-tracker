package com.sonja.tracker.di

import com.sonja.tracker.data.db.DatabaseDriverFactory
import com.sonja.tracker.data.prefs.AppPreferences
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single { DatabaseDriverFactory(androidContext()) }
    single { AppPreferences(androidContext()) }
}
