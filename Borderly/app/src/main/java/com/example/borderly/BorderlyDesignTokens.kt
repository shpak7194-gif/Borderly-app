package com.example.borderly

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

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

/**
 * Keeps the map palette intact while making the same accent readable when it
 * is used as small text. Some travel-status yellows are excellent map fills
 * but do not have enough contrast against a light card without darkening.
 */
internal fun borderlyReadableAccentColor(
    accent: Color,
    background: Color,
    accentBackgroundAlpha: Float = 0f,
    minimumContrast: Float = 4.5f
): Color {
    val textBackground = if (accentBackgroundAlpha > 0f) {
        accent.copy(alpha = accentBackgroundAlpha).compositeOver(background)
    } else {
        background
    }

    fun contrast(first: Color, second: Color): Float {
        val firstLuminance = first.luminance()
        val secondLuminance = second.luminance()
        return (maxOf(firstLuminance, secondLuminance) + 0.05f) /
            (minOf(firstLuminance, secondLuminance) + 0.05f)
    }

    val opaqueAccent = accent.copy(alpha = 1f)
    if (contrast(opaqueAccent, textBackground) >= minimumContrast) {
        return opaqueAccent
    }

    val target = if (textBackground.luminance() > 0.5f) Color.Black else Color.White
    var insufficient = 0f
    var sufficient = 1f
    repeat(12) {
        val fraction = (insufficient + sufficient) / 2f
        if (contrast(lerp(opaqueAccent, target, fraction), textBackground) >= minimumContrast) {
            sufficient = fraction
        } else {
            insufficient = fraction
        }
    }
    return lerp(opaqueAccent, target, sufficient)
}
