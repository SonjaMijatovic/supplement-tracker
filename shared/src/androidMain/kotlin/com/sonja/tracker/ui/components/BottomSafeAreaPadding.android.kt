package com.sonja.tracker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// On Android, WindowInsets-based modifiers (navigationBarsPadding) work correctly
// inside ModalBottomSheet popups, so no extra padding is needed here.
@Composable
actual fun rememberBottomSafeAreaPadding(): Dp = 0.dp
