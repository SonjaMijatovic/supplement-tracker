package com.sonja.tracker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.UIKit.UIApplication
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

// WindowInsets returns 0 inside a ModalBottomSheet popup on iOS because the popup's
// composition context doesn't receive system insets. Read from UIKit directly instead.
// Uses connectedScenes (iOS 13+) rather than the deprecated keyWindow, which returns
// nil in multi-scene environments (iPad Split View, Stage Manager).
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberBottomSafeAreaPadding(): Dp {
    val bottomPts = UIApplication.sharedApplication
        .connectedScenes
        .filterIsInstance<UIWindowScene>()
        .firstOrNull { it.activationState == UISceneActivationStateForegroundActive }
        ?.windows
        ?.filterIsInstance<UIWindow>()
        ?.firstOrNull { it.isKeyWindow() }
        ?.safeAreaInsets
        ?.useContents { bottom }
        ?: 0.0
    // UIKit points == Compose dp on iOS (both are logical pixels at 1x scale)
    return bottomPts.toFloat().dp
}
