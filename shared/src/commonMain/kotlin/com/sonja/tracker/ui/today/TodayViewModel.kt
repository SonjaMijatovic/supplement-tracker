package com.sonja.tracker.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonja.tracker.data.repository.ItemRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class TodayViewModel(private val repo: ItemRepository) : ViewModel() {
    val uiState: StateFlow<TodayUiState> = repo
        .observeTodayGroups()
        .map<_, TodayUiState> { groups -> TodayUiState.Success(groups, groups.all { g -> g.allLogged }) }
        .catch { emit(TodayUiState.Error(it.message ?: "Unknown error")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayUiState.Loading)
}
