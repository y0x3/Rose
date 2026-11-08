package com.tiempo.app.presentation.theme

import androidx.compose.ui.graphics.Color

// Design tokens. Only the theme reads these values; screens use MaterialTheme.colorScheme.
internal val Background = Color(0xFF111318)
internal val Surface = Color(0xFF1B1F27)
internal val SurfaceSecondary = Color(0xFF30343E)
internal val TextPrimary = Color(0xFFF2F2F2)
internal val TextSecondary = Color(0xFFA5A9B5)
internal val Accent = Color(0xFFE6C58D)

// Functional color, kept out of the main palette but required for accessible errors.
internal val Error = Color(0xFFE8A0A0)
internal val ErrorContainer = Color(0xFF4A2B2B)
