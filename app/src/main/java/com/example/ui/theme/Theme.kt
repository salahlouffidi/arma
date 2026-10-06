package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = AmberPrimary,
    secondary = AmberSecondary,
    tertiary = BrassAccent,
    background = SurfaceDark,
    surface = CardSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color(0xFFF5E6D3),
    onSurface = Color(0xFFF5E6D3)
  )

@Composable
fun ArmadilloEscapeTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(colorScheme = DarkColorScheme, typography = Typography, content = content)
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  ArmadilloEscapeTheme(content = content)
}
