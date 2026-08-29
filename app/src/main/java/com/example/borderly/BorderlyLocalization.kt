package com.example.borderly

import android.content.Context
import android.content.ContextWrapper
import android.app.Activity
import android.app.LocaleManager
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import java.util.Locale

internal enum class AppLanguage(
    val languageTag: String,
    val nativeName: String
) {
    SYSTEM("", "System"),
    RUSSIAN("ru", "Русский"),
    ENGLISH("en", "English"),
    SPANISH_LATAM("es-419", "Español (Latinoamérica)"),
    PORTUGUESE_BRAZIL("pt-BR", "Português (Brasil)"),
    GERMAN("de", "Deutsch"),
    FRENCH("fr", "Français");

    companion object {
        fun current(context: Context): AppLanguage {
            val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.getSystemService(LocaleManager::class.java)
                    .applicationLocales
                    .toLanguageTags()
                    .substringBefore(',')
            } else {
                context.getSharedPreferences(LanguagePreferencesName, Context.MODE_PRIVATE)
                    .getString(LanguagePreferenceKey, "")
                    .orEmpty()
            }
            if (tag.isBlank()) return SYSTEM
            return entries.firstOrNull {
                it.languageTag.equals(tag, ignoreCase = true)
            } ?: entries.firstOrNull {
                Locale.forLanguageTag(it.languageTag).language ==
                    Locale.forLanguageTag(tag).language
            } ?: SYSTEM
        }
    }
}

private const val LanguagePreferencesName = "borderly_language_preferences"
private const val LanguagePreferenceKey = "app_language"

internal fun setAppLanguage(context: Context, language: AppLanguage) {
    context.getSharedPreferences(LanguagePreferencesName, Context.MODE_PRIVATE)
        .edit()
        .putString(LanguagePreferenceKey, language.languageTag)
        .apply()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.getSystemService(LocaleManager::class.java).applicationLocales =
            LocaleList.forLanguageTags(language.languageTag)
    } else {
        context.findActivity()?.recreate()
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

internal fun localizedAppContext(context: Context): Context {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return context
    val tag = context.getSharedPreferences(LanguagePreferencesName, Context.MODE_PRIVATE)
        .getString(LanguagePreferenceKey, "")
        .orEmpty()
    if (tag.isBlank()) return context

    val configuration = Configuration(context.resources.configuration)
    configuration.setLocale(Locale.forLanguageTag(tag))
    return context.createConfigurationContext(configuration)
}

@Composable
internal fun AppLanguage.localizedName(): String =
    if (this == AppLanguage.SYSTEM) stringResource(R.string.language_system)
    else nativeName

@Composable
internal fun AppThemeMode.localizedTitle(): String = stringResource(
    when (this) {
        AppThemeMode.SYSTEM -> R.string.theme_system
        AppThemeMode.LIGHT -> R.string.theme_light
        AppThemeMode.DARK -> R.string.theme_dark
    }
)

@Composable
internal fun PerformanceMode.localizedTitle(): String = stringResource(
    when (this) {
        PerformanceMode.AUTO -> R.string.performance_auto
        PerformanceMode.ON -> R.string.performance_on
        PerformanceMode.OFF -> R.string.performance_off
    }
)

@Composable
internal fun PassportRegion.localizedTitle(): String = stringResource(
    when (this) {
        PassportRegion.EUROPE -> R.string.region_europe
        PassportRegion.ASIA -> R.string.region_asia
        PassportRegion.AMERICAS -> R.string.region_americas
        PassportRegion.AFRICA -> R.string.region_africa
        PassportRegion.OCEANIA -> R.string.region_oceania
    }
)

@Composable
internal fun PassportRegionFilter.localizedTitle(): String =
    if (this == PassportRegionFilter.ALL) stringResource(R.string.all)
    else requireNotNull(region).localizedTitle()

@Composable
internal fun RankingSortOrder.localizedTitle(): String = stringResource(
    when (this) {
        RankingSortOrder.STRONGEST_FIRST -> R.string.strongest_first
        RankingSortOrder.WEAKEST_FIRST -> R.string.weakest_first
    }
)

@Composable
internal fun VisaType.localizedTitle(): String = stringResource(visaTitleResource())

@StringRes
internal fun VisaType.visaTitleResource(): Int = when (this) {
    VisaType.HOME_COUNTRY -> R.string.visa_home_country
    VisaType.FREEDOM -> R.string.visa_freedom
    VisaType.VISA_FREE -> R.string.visa_free
    VisaType.ETA -> R.string.visa_eta
    VisaType.VISA_ON_ARRIVAL -> R.string.visa_on_arrival
    VisaType.E_VISA -> R.string.visa_evisa
    VisaType.VISA_REQUIRED -> R.string.visa_required
    VisaType.ENTRY_RESTRICTED -> R.string.visa_entry_restricted
    VisaType.SPECIAL_PERMIT -> R.string.visa_special_permit
    VisaType.MIXED_REQUIREMENTS -> R.string.visa_mixed_requirements
    VisaType.NO_DATA -> R.string.visa_no_data
}

@Composable
internal fun CountryFilter.localizedTitle(): String =
    if (this == CountryFilter.ALL) stringResource(R.string.all_countries)
    else requireNotNull(region).localizedTitle()

@Composable
internal fun VisaStatusFilter.localizedTitle(): String =
    if (this == VisaStatusFilter.ALL) stringResource(R.string.all)
    else requireNotNull(visaType).localizedTitle()

@Composable
internal fun MapVisaQuickFilter.localizedTitle(): String = when (this) {
    MapVisaQuickFilter.ALL -> stringResource(R.string.all)
    MapVisaQuickFilter.VISA_FREE -> VisaType.VISA_FREE.localizedTitle()
    MapVisaQuickFilter.FREEDOM -> VisaType.FREEDOM.localizedTitle()
    MapVisaQuickFilter.ETA -> VisaType.ETA.localizedTitle()
    MapVisaQuickFilter.VISA_ON_ARRIVAL -> VisaType.VISA_ON_ARRIVAL.localizedTitle()
    MapVisaQuickFilter.E_VISA -> VisaType.E_VISA.localizedTitle()
    MapVisaQuickFilter.VISA_REQUIRED -> VisaType.VISA_REQUIRED.localizedTitle()
    MapVisaQuickFilter.ENTRY_RESTRICTED -> VisaType.ENTRY_RESTRICTED.localizedTitle()
    MapVisaQuickFilter.SPECIAL_PERMIT -> VisaType.SPECIAL_PERMIT.localizedTitle()
    MapVisaQuickFilter.MIXED_REQUIREMENTS -> VisaType.MIXED_REQUIREMENTS.localizedTitle()
}

@Composable
internal fun EntryRequirementType.localizedTitle(): String = stringResource(
    when (this) {
        EntryRequirementType.ARRIVAL_CARD -> R.string.requirement_arrival_card
        EntryRequirementType.PRE_TRAVEL_REGISTRATION -> R.string.requirement_pretravel_registration
        EntryRequirementType.HEALTH_DECLARATION -> R.string.requirement_health_declaration
        EntryRequirementType.CUSTOMS_DECLARATION -> R.string.requirement_customs_declaration
        EntryRequirementType.TOURISM_REGISTRATION -> R.string.requirement_tourism_registration
        EntryRequirementType.OTHER_ENTRY_FORMALITY -> R.string.requirement_other
    }
)

@Composable
internal fun localizedCountryName(isoNumeric: Int, fallback: String): String {
    val configuration = LocalConfiguration.current
    val displayLocale = configuration.locales[0] ?: Locale.getDefault()
    return localizedCountryName(isoNumeric, fallback, displayLocale)
}

internal fun localizedCountryName(
    isoNumeric: Int,
    fallback: String,
    displayLocale: Locale
): String {
    val alpha2 = IsoCountryCodesByNumeric[isoNumeric]?.alpha2
    if (alpha2 == "XK") {
        return when (displayLocale.language) {
            "ru" -> "Косово"
            "de" -> "Kosovo"
            "fr" -> "Kosovo"
            "es" -> "Kosovo"
            "pt" -> "Kosovo"
            else -> "Kosovo"
        }
    }
    return alpha2
        ?.let { Locale.Builder().setRegion(it).build().getDisplayCountry(displayLocale) }
        ?.takeIf { it.isNotBlank() }
        ?: fallback
}

@Composable
internal fun Passport.localizedName(): String = localizedCountryName(isoNumeric, name)

internal fun currentAppLocale(context: Context): Locale =
    context.resources.configuration.locales[0] ?: Locale.getDefault()
