package com.sonja.tracker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

/**
 * Returns the bottom safe area inset (home indicator height on iOS, 0 on Android).
 * Reads directly from the platform rather than relying on WindowInsets, which returns
 * 0 inside a ModalBottomSheet popup context on iOS.
 */
@Composable
expect fun rememberBottomSafeAreaPadding(): Dp
