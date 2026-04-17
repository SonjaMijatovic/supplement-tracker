package com.sonja.tracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.sonja.tracker.ui.history.HistoryScreen
import com.sonja.tracker.ui.items.ItemsScreen
import com.sonja.tracker.ui.today.TodayScreen
import kotlinx.serialization.Serializable

/** Set to true while a full-screen overlay (e.g. time picker inside bottom sheet) is visible,
 *  so the tab bar can be hidden to avoid peeking through on iOS. */
val LocalHideNavBar = compositionLocalOf { mutableStateOf(false) }

// Route definitions — @Serializable for future Navigation 3 KMP library swap.
// (androidx.navigation3 does not yet publish iOS klibs; navigation is state-based for now.)
// When adding a new route: add a data object here, a branch in AppRouteSaver, and a branch
// in the when block inside AppNavigation.
@Serializable
sealed interface AppRoute

@Serializable
data object TodayRoute : AppRoute

@Serializable
data object HistoryRoute : AppRoute

@Serializable
data object ItemsRoute : AppRoute

// Ordinal-based Saver so rememberSaveable can survive Android config changes and process death.
// Update when new AppRoute subtypes are added.
private val AppRouteSaver: Saver<AppRoute, Int> = Saver(
    save = {
        when (it) {
            TodayRoute -> 0
            HistoryRoute -> 1
            ItemsRoute -> 2
        }
    },
    restore = {
        when (it) {
            0 -> TodayRoute
            1 -> HistoryRoute
            2 -> ItemsRoute
            else -> TodayRoute
        }
    }
)

@Composable
fun AppNavigation() {
    var selectedTab: AppRoute by rememberSaveable(stateSaver = AppRouteSaver) {
        mutableStateOf(TodayRoute)
    }
    val hideNavBar = remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalHideNavBar provides hideNavBar) {
        Scaffold(
            bottomBar = {
                if (!hideNavBar.value) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = selectedTab == TodayRoute,
                            onClick = { selectedTab = TodayRoute },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Today") },
                            label = { Text("Today") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == HistoryRoute,
                            onClick = { selectedTab = HistoryRoute },
                            icon = { Icon(Icons.Default.DateRange, contentDescription = "History") },
                            label = { Text("History") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == ItemsRoute,
                            onClick = { selectedTab = ItemsRoute },
                            icon = { Icon(Icons.Default.List, contentDescription = "Items") },
                            label = { Text("Items") }
                        )
                    }
                }
            }
        ) { innerPadding ->
            when (selectedTab) {
                TodayRoute -> TodayScreen(
                    modifier = Modifier.padding(innerPadding),
                    onNavigateToItems = { selectedTab = ItemsRoute }
                )
                HistoryRoute -> HistoryScreen(modifier = Modifier.padding(innerPadding))
                ItemsRoute -> ItemsScreen(modifier = Modifier.padding(innerPadding))
            }
        }
    }
}
