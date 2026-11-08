package com.tiempo.app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TiempoColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Background,
    primaryContainer = SurfaceSecondary,
    onPrimaryContainer = Accent,
    inversePrimary = Accent,
    secondary = TextSecondary,
    onSecondary = Background,
    secondaryContainer = SurfaceSecondary,
    onSecondaryContainer = TextPrimary,
    tertiary = Accent,
    onTertiary = Background,
    tertiaryContainer = SurfaceSecondary,
    onTertiaryContainer = TextPrimary,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceSecondary,
    onSurfaceVariant = TextSecondary,
    surfaceTint = Accent,
    inverseSurface = TextPrimary,
    inverseOnSurface = Background,
    error = Error,
    onError = Background,
    errorContainer = ErrorContainer,
    onErrorContainer = TextPrimary,
    outline = TextSecondary,
    outlineVariant = SurfaceSecondary,
    surfaceBright = SurfaceSecondary,
    surfaceDim = Background,
    surfaceContainerLowest = Background,
    surfaceContainerLow = Surface,
    surfaceContainer = Surface,
    surfaceContainerHigh = SurfaceSecondary,
    surfaceContainerHighest = SurfaceSecondary
)

/**
 * Single dark theme for the app. Dynamic color is intentionally disabled so the
 * product palette is always the same on every device.
 */
@Composable
fun TiempoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TiempoColorScheme,
        typography = TiempoTypography,
        shapes = TiempoShapes,
        content = content
    )
}
