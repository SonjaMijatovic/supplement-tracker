package com.sonja.tracker.ui.components

import androidx.compose.runtime.Composable

@Composable
expect fun PlatformTimePickerDialog(
    initialTime: String,
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit
)
