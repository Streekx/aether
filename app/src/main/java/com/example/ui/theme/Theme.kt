package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = RoyalPurple,
    secondary = MutedPurple,
    tertiary = GlowPurple,
    background = ObsidianBlack,
    surface = DarkSurface,
    onPrimary = WhiteSmoke,
    onSecondary = WhiteSmoke,
    onTertiary = WhiteSmoke,
    onBackground = WhiteSmoke,
    onSurface = WhiteSmoke,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = LightGray
  )

private val LightColorScheme = DarkColorScheme // Keep dark themes only for futuristic aesthetic!

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme for a consistent futuristic dark branding
  dynamicColor: Boolean = false, // Disable dynamic colors to preserve our tailored royal purple & black brand identity!
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
