package com.sonja.tracker.ui.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonja.tracker.data.repository.ItemRepository
import com.sonja.tracker.ui.components.ItemRow
import org.koin.compose.koinInject

@Composable
fun ItemsScreen(modifier: Modifier = Modifier) {
    val repository = koinInject<ItemRepository>()
    val viewModel: ItemsViewModel = viewModel { ItemsViewModel(repository) }
    val uiState by viewModel.uiState.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add item")
            }
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is ItemsUiState.Loading -> {
                Box(
                    Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is ItemsUiState.Error -> {
                Box(
                    Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(state.message)
                }
            }
            is ItemsUiState.Success -> {
                if (state.items.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Add your first item",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { showAddSheet = true }) {
                            Text("Add item")
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        items(state.items, key = { it.id }) { item ->
                            ItemRow(item = item)
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        ItemEditSheet(
            onSave = { name, weekdayTime -> viewModel.addItem(name, weekdayTime) },
            onDismiss = { showAddSheet = false }
        )
    }
}
