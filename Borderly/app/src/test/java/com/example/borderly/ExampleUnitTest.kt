package com.example.borderly

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductQualityUnitTest {
    @Test
    fun dataDatesFollowTheSelectedLocale() {
        assertEquals("Aug 14, 2026", formatDataDateForUi("2026-08-14", Locale.US))
        assertEquals("14.08.2026", formatDataDateForUi("2026-08-14", Locale.GERMANY))
        assertEquals("not-a-date", formatDataDateForUi("not-a-date", Locale.US))
    }

    @Test
    fun everyTravelStatusMeetsNormalTextContrastInBothThemes() {
        val surfaces = listOf(Color(0xFFF9F9F9), Color(0xFF151B20))

        VisaType.entries.forEach { type ->
            surfaces.forEach { surface ->
                val badgeBackground = type.color.copy(alpha = .15f).compositeOver(surface)
                val readable = borderlyReadableAccentColor(
                    accent = type.color,
                    background = surface,
                    accentBackgroundAlpha = .15f
                )

                assertTrue(
                    "${type.name} has insufficient contrast",
                    contrastRatio(readable, badgeBackground) >= 4.5f
                )
            }
        }
    }

    private fun contrastRatio(first: Color, second: Color): Float {
        val firstLuminance = first.luminance()
        val secondLuminance = second.luminance()
        return (maxOf(firstLuminance, secondLuminance) + .05f) /
            (minOf(firstLuminance, secondLuminance) + .05f)
    }
}
