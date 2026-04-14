package com.sonja.tracker.ui.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonja.tracker.data.repository.ItemRepository
import com.sonja.tracker.domain.model.Item
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ItemsViewModel(private val repository: ItemRepository) : ViewModel() {
    fun addItem(name: String, weekdayTime: String) {
        viewModelScope.launch {
            repository.addItem(name, weekdayTime)
        }
    }

    val uiState: StateFlow<ItemsUiState> = repository
        .observeItems()
        .map<List<Item>, ItemsUiState> { items -> ItemsUiState.Success(items) }
        .retryWhen { cause, _ ->
            emit(ItemsUiState.Error(cause.message ?: "Unknown error"))
            true // resubscribe after emitting the error so the flow stays alive
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ItemsUiState.Loading
        )
}
