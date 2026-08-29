package com.example.borderly

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// BORDERLY_SHARED_COLOR_TOKENS_2026_08_21_READABLE_UNIFIED

@Composable
internal fun borderlyIsDarkTheme(): Boolean =
    MaterialTheme.colorScheme.background.luminance() < 0.5f

@Composable
internal fun borderlyControlSurfaceColor(): Color {
    return if (borderlyIsDarkTheme()) {
        Color(0xFF151B20).copy(alpha = 0.96f)
    } else {
        Color(0xFFF7F8FA).copy(alpha = 0.96f)
    }
}

@Composable
internal fun borderlyOpaqueControlSurfaceColor(): Color {
    return if (borderlyIsDarkTheme()) {
        Color(0xFF151B20)
    } else {
        Color(0xFFF7F8FA)
    }
}

@Composable
internal fun borderlyNavigationSurfaceColor(): Color {
    return if (borderlyIsDarkTheme()) {
        Color(0xFF101519).copy(alpha = 0.72f)
    } else {
        Color(0xFFF2F4F7).copy(alpha = 0.72f)
    }
}

@Composable
internal fun borderlyControlRimColor(): Color {
    return if (borderlyIsDarkTheme()) {
        Color(0xFF2A343D)
    } else {
        Color.White
    }
}

@Composable
internal fun borderlySelectedControlColor(): Color {
    return if (borderlyIsDarkTheme()) {
        Color(0xFF26313D)
    } else {
        Color(0xFF1E2A36)
    }
}

@Composable
internal fun borderlySelectedContentColor(): Color = Color.White

@Composable
internal fun borderlyMutedControlColor(): Color {
    return if (borderlyIsDarkTheme()) {
        Color(0xFF26313D)
    } else {
        Color(0xFFE8ECF1)
    }
}

@Composable
internal fun borderlyPrimaryContentColor(): Color {
    return if (borderlyIsDarkTheme()) {
        Color(0xFFF8FAFC)
    } else {
        Color(0xFF1E2A36)
    }
}

@Composable
internal fun borderlySecondaryContentColor(): Color {
    return if (borderlyIsDarkTheme()) {
        Color(0xFFD0D6DD)
    } else {
        Color(0xFF68727D)
    }
}
