package com.sonja.tracker.di

import com.sonja.tracker.data.db.DatabaseDriverFactory
import com.sonja.tracker.data.prefs.AppPreferences
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools

val iosModule = module {
    single { DatabaseDriverFactory() }
    single { AppPreferences() }
}

fun initKoin() {
    if (KoinPlatformTools.defaultContext().getOrNull() == null) {
        startKoin {
            modules(sharedModule, iosModule)
        }
    }
}
