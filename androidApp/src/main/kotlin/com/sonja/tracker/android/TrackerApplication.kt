package com.sonja.tracker.android

import android.app.Application
import com.sonja.tracker.di.androidModule
import com.sonja.tracker.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@TrackerApplication)
            modules(sharedModule, androidModule)
        }
    }
}
