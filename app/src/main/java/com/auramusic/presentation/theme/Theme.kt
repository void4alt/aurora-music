package com.auramusic.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AuroraPrimary = Color(0xFFC2A5FF)
val AuroraSecondary = Color(0xFF69E2C4)
val AuroraBackground = Color(0xFF08080D)
val AuroraSurface = Color(0xFF14141E)

private val DarkColors = darkColorScheme(
    primary = AuroraPrimary,
    onPrimary = Color(0xFF1B102E),
    secondary = AuroraSecondary,
    onSecondary = Color(0xFF05231D),
    tertiary = Color(0xFFFFA7C4),
    background = AuroraBackground,
    onBackground = Color(0xFFF5F2FF),
    surface = AuroraSurface,
    onSurface = Color(0xFFF5F2FF),
    surfaceVariant = Color(0xFF1E1E2B),
    onSurfaceVariant = Color(0xFFB8B4C8),
    error = Color(0xFFFF7B8B)
)

@Composable
fun AuroraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography(),
        content = content
    )
}
