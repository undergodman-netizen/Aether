package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

object ThemeConfig {
    var isDarkTheme by mutableStateOf(true)
}

private val DarkColorScheme = darkColorScheme(
    primary = AetherPrimary,
    onPrimary = AetherOnPrimary,
    primaryContainer = AetherPrimaryContainer,
    onPrimaryContainer = AetherOnPrimaryContainer,
    secondary = AetherSecondary,
    onSecondary = AetherOnSecondary,
    secondaryContainer = AetherSecondaryContainer,
    onSecondaryContainer = AetherOnSecondaryContainer,
    background = AetherBackground,
    onBackground = AetherOnBackground,
    surface = AetherSurface,
    onSurface = AetherOnSurface,
    surfaceVariant = AetherSurfaceVariant,
    onSurfaceVariant = AetherOnSurfaceVariant,
    outline = AetherOutline,
    error = AetherError,
    onError = AetherOnError
)

private val LightColorScheme = lightColorScheme(
    primary = AetherPrimary,
    onPrimary = Color(0xFF161616),
    primaryContainer = AetherPrimaryContainer,
    onPrimaryContainer = Color(0xFF161616),
    secondary = Color(0xFFECEAEF),
    onSecondary = Color(0xFF1B1B1F),
    secondaryContainer = Color(0xFFF1F0F5),
    onSecondaryContainer = Color(0xFF1B1B1F),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0x1F1B1B1F),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF)
)

@Composable
fun AetherTheme(
    darkTheme: Boolean = ThemeConfig.isDarkTheme,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
