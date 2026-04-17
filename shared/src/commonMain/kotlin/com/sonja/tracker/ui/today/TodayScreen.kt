package com.sonja.tracker.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonja.tracker.data.repository.ItemRepository
import com.sonja.tracker.ui.components.TimeGroupSection
import org.koin.compose.koinInject

@Composable
fun TodayScreen(
    modifier: Modifier = Modifier,
    onNavigateToItems: () -> Unit = {}
) {
    val repository = koinInject<ItemRepository>()
    val viewModel: TodayViewModel = viewModel { TodayViewModel(repository) }
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is TodayUiState.Loading -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is TodayUiState.Error -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        is TodayUiState.Success -> {
            if (state.groups.isEmpty()) {
                TodayEmptyState(
                    modifier = modifier,
                    onNavigateToItems = onNavigateToItems
                )
            } else {
                LazyColumn(modifier = modifier.fillMaxSize()) {
                    items(state.groups, key = { it.timeSlot }) { group ->
                        TimeGroupSection(group = group)
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayEmptyState(
    modifier: Modifier = Modifier,
    onNavigateToItems: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Nothing here yet",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Add items to start tracking your daily routine",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onNavigateToItems) {
            Text("Add your first item")
        }
    }
}
