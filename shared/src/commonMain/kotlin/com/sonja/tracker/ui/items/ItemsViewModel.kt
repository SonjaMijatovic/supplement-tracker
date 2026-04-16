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
    fun addItem(name: String, weekdayTime: String, weekendTime: String?, iconId: String? = null) {
        viewModelScope.launch {
            repository.addItem(name, weekdayTime, weekendTime, iconId)
        }
    }

    fun editItem(id: Long, name: String, weekdayTime: String, weekendTime: String?, iconId: String?) {
        viewModelScope.launch {
            val current = (uiState.value as? ItemsUiState.Success)?.items?.find { it.id == id }
            repository.updateItem(
                id = id,
                name = name,
                weekdayTime = weekdayTime,
                weekendTime = weekendTime,
                imagePath = current?.imagePath,  // still read from uiState (always null until Story 2.6)
                iconId = iconId                  // now passed directly — fixes stale-state risk
            )
            // TODO Epic 4: NotificationScheduler.rescheduleForSlot(weekdayTime, weekendTime)
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            repository.deleteItem(id)
            // TODO Epic 4: NotificationScheduler.cancelForItem(id)
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
