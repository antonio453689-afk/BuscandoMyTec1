package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
  primary = BluePrimary,
  onPrimary = Color.White,
  primaryContainer = BlueContainer,
  onPrimaryContainer = OnBlueContainer,
  secondary = Slate700,
  onSecondary = Color.White,
  secondaryContainer = Slate100,
  onSecondaryContainer = Slate900,
  tertiary = VerifiedGreen,
  onTertiary = Color.White,
  tertiaryContainer = GreenContainer,
  onTertiaryContainer = GreenText,
  background = CanvasBackground,
  onBackground = Slate900,
  surface = SurfaceWhite,
  onSurface = Slate900,
  surfaceVariant = Slate100,
  onSurfaceVariant = Slate600,
  outline = Slate200,
  outlineVariant = Color(0xFFCBD5E1)
)

private val DarkColorScheme = darkColorScheme(
  primary = BluePrimary,
  onPrimary = Color.White,
  primaryContainer = Slate800,
  onPrimaryContainer = BlueBorder,
  secondary = Slate400,
  onSecondary = Slate900,
  secondaryContainer = Slate800,
  onSecondaryContainer = Slate200,
  tertiary = VerifiedGreen,
  onTertiary = Color.White,
  background = Color(0xFF0B0F19),
  onBackground = Color(0xFFF8FAFC),
  surface = DarkCardSurface,
  onSurface = Color(0xFFF8FAFC),
  surfaceVariant = Slate800,
  onSurfaceVariant = Slate400,
  outline = Slate700,
  outlineVariant = Slate600
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false,
  content: @Composable () -> Unit
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
