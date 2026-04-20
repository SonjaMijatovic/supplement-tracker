package com.sonja.tracker.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonja.tracker.data.repository.ItemRepository
import com.sonja.tracker.data.repository.LogRepository
import com.sonja.tracker.domain.model.TimeGroup
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodayViewModel(
    private val itemRepo: ItemRepository,
    private val logRepo: LogRepository
) : ViewModel() {

    val uiState: StateFlow<TodayUiState> = combine<List<TimeGroup>, Set<Long>, TodayUiState>(
        itemRepo.observeTodayGroups(),
        logRepo.observeTodayLoggedItemIds()
    ) { groups, loggedIds ->
        val enriched = groups.map { group ->
            group.copy(
                loggedItemIds = group.items.filter { it.id in loggedIds }.map { it.id }.toSet(),
                allLogged = group.items.isNotEmpty() && group.items.all { it.id in loggedIds }
            )
        }
        TodayUiState.Success(
            groups = enriched,
            allLogged = enriched.isNotEmpty() && enriched.all { it.allLogged }
        )
    }
    .catch { emit(TodayUiState.Error(it.message ?: "Unknown error")) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayUiState.Loading)

    fun logItem(itemId: Long) {
        viewModelScope.launch {
            logRepo.logItem(itemId)
        }
    }
}
