package com.example.borderly.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFF2F4F5),
    onPrimary = Color(0xFF111315),
    primaryContainer = Color(0xFF2A2E32),
    onPrimaryContainer = Color(0xFFF5F6F7),
    secondary = Color(0xFFBCC2C8),
    onSecondary = Color(0xFF151719),
    background = Color(0xFF0F1113),
    onBackground = Color(0xFFF4F5F6),
    surface = Color(0xFF181B1E),
    onSurface = Color(0xFFF4F5F6),
    surfaceVariant = Color(0xFF202428),
    onSurfaceVariant = Color(0xFFA9AFB5),
    outline = Color(0xFF444A50),
    outlineVariant = Color(0xFF2B3035),
    scrim = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF121313),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF0F1F2),
    onPrimaryContainer = Color(0xFF171717),
    secondary = Color(0xFF60656A),
    onSecondary = Color.White,
    background = Color(0xFFEEEEF3),
    onBackground = Color(0xFF171717),
    surface = Color.White,
    onSurface = Color(0xFF171717),
    surfaceVariant = Color(0xFFF3F4F5),
    onSurfaceVariant = Color(0xFF686D72),
    outline = Color(0xFFD5D8DB),
    outlineVariant = Color(0xFFE3E5E7),
    scrim = Color.Black
)

/** True when Borderly is currently rendering its dark palette. */
val LocalBorderlyDarkTheme = staticCompositionLocalOf { false }

@Composable
fun BorderlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalBorderlyDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            content = content
        )
    }
}
