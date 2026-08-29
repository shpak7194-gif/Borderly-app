package com.example.borderly

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf

internal const val BorderlyPreferences = "borderly_preferences"
internal const val SelectedPassportPreference = "selected_passport_iso"
internal const val RecentPassportsPreference = "recent_passport_ids"

internal fun loadSelectedPassportIso(context: Context): Int =
    context.getSharedPreferences(BorderlyPreferences, Context.MODE_PRIVATE)
        .getInt(SelectedPassportPreference, RussiaIsoNumeric)

internal fun loadRecentPassportIds(context: Context): List<Int> =
    context.getSharedPreferences(BorderlyPreferences, Context.MODE_PRIVATE)
        .getString(RecentPassportsPreference, null)
        ?.split(',')
        ?.mapNotNull(String::toIntOrNull)
        ?.distinct()
        ?.take(5)
        .orEmpty()

internal fun savePassportPreferences(
    context: Context,
    selectedPassportIso: Int,
    recentPassportIds: List<Int>
) {
    context.getSharedPreferences(BorderlyPreferences, Context.MODE_PRIVATE)
        .edit()
        .putInt(SelectedPassportPreference, selectedPassportIso)
        .putString(RecentPassportsPreference, recentPassportIds.joinToString(","))
        .apply()
}

internal enum class PerformanceMode(
    val storageValue: String,
    val title: String
) {
    AUTO("auto", "Авто"),
    ON("on", "Вкл"),
    OFF("off", "Выкл");

    companion object {
        fun fromStorage(value: String?): PerformanceMode =
            entries.firstOrNull { it.storageValue == value } ?: AUTO
    }
}

internal const val PerformanceModePreference = "performance_mode"

internal fun loadPerformanceMode(context: Context): PerformanceMode =
    context.getSharedPreferences(BorderlyPreferences, Context.MODE_PRIVATE)
        .getString(PerformanceModePreference, PerformanceMode.AUTO.storageValue)
        .let(PerformanceMode::fromStorage)

internal fun savePerformanceMode(context: Context, mode: PerformanceMode) {
    context.getSharedPreferences(BorderlyPreferences, Context.MODE_PRIVATE)
        .edit()
        .putString(PerformanceModePreference, mode.storageValue)
        .apply()
}

internal fun resolveLowEndMode(context: Context, mode: PerformanceMode): Boolean =
    when (mode) {
        PerformanceMode.AUTO -> isBorderlyLowEndDevice(context)
        PerformanceMode.ON -> true
        PerformanceMode.OFF -> false
    }

// Provided by MainActivity from the saved setting. Composable defaults to
// false (blur enabled) in previews where no provider exists.
val LocalBorderlyLowEndMode = staticCompositionLocalOf { false }

