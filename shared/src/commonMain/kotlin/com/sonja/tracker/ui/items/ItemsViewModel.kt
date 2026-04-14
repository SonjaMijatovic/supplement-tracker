package com.sonja.tracker.ui.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonja.tracker.data.repository.ItemRepository
import com.sonja.tracker.domain.model.Item
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ItemsViewModel(private val repository: ItemRepository) : ViewModel() {
    val uiState: StateFlow<ItemsUiState> = repository
        .observeItems()
        .map<List<Item>, ItemsUiState> { items -> ItemsUiState.Success(items) }
        .catch { e -> emit(ItemsUiState.Error(e.message ?: "Unknown error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ItemsUiState.Loading
        )
}
