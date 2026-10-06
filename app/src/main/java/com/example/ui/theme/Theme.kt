package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SandPrimary,
    onPrimary = Color.Black,
    primaryContainer = SandPrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = SandSecondary,
    onSecondary = Color.Black,
    tertiary = DesertAccent,
    background = DesertNight,
    onBackground = DesertTextPrimary,
    surface = DesertSurface,
    onSurface = DesertTextPrimary,
    surfaceVariant = DesertCard,
    onSurfaceVariant = DesertTextSecondary,
    error = DesertRed
)

@Composable
fun ArmadilloEscapeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
