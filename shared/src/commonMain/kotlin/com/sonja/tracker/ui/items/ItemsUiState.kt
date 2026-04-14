package com.sonja.tracker.ui.items

import com.sonja.tracker.domain.model.Item

sealed class ItemsUiState {
    object Loading : ItemsUiState()
    data class Success(val items: List<Item>) : ItemsUiState()
    data class Error(val message: String) : ItemsUiState()
}
