package com.sonja.tracker

import androidx.compose.runtime.Composable
import com.sonja.tracker.ui.navigation.AppNavigation
import com.sonja.tracker.ui.theme.TrackerTheme

@Composable
fun App() {
    TrackerTheme {
        AppNavigation()
    }
}
