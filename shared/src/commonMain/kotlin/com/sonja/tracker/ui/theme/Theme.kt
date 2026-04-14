package com.sonja.tracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = DustyIndigoPrimary,
    onPrimary = DustyIndigoOnPrimary,
    primaryContainer = DustyIndigoPrimaryContainer,
    onPrimaryContainer = DustyIndigoOnPrimaryContainer,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    error = ErrorLight,
    onError = OnErrorLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = DustyIndigoPrimaryDark,
    onPrimary = DustyIndigoOnPrimaryDark,
    primaryContainer = DustyIndigoPrimaryContainerDark,
    onPrimaryContainer = DustyIndigoOnPrimaryContainerDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    error = ErrorDark,
    onError = OnErrorDark,
)

@Composable
fun TrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = TrackerTypography,
        content = content
    )
}
