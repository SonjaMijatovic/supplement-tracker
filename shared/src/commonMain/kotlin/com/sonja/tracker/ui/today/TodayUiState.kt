package com.sonja.tracker.ui.today

import com.sonja.tracker.domain.model.TimeGroup

sealed class TodayUiState {
    object Loading : TodayUiState()
    data class Success(val groups: List<TimeGroup>, val allLogged: Boolean) : TodayUiState()
    data class Error(val message: String) : TodayUiState()
}
