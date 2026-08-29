package com.example.borderly

import android.content.Context

internal fun mapCountryInfo(
    context: Context,
    countryIso: Int,
    name: String,
    flag: String,
    passport: Passport,
    requirements: Map<Int, VisaRequirement>,
    entryGuideDatabase: EntryGuideDatabase,
    entryRequirementDatabase: EntryRequirementDatabase
): CountryInfo {
    val requirement = requirements[countryIso]
    val visaType = visaTypeFor(passport, countryIso, requirements)
    val entryGuide = entryGuideDatabase.guideFor(
        passportIso = passport.isoNumeric,
        destinationIso = countryIso,
        currentVisaType = visaType
    )
    val entryRequirements = entryRequirementDatabase.requirementsFor(
        passportIso = passport.isoNumeric,
        destinationIso = countryIso,
        currentVisaType = visaType
    )
    val days = requirement?.stayDays?.takeUnless {
        visaType in setOf(
            VisaType.HOME_COUNTRY,
            VisaType.FREEDOM,
            VisaType.ENTRY_RESTRICTED,
            VisaType.SPECIAL_PERMIT,
            VisaType.MIXED_REQUIREMENTS
        )
    }
    val locale = currentAppLocale(context)
    val passportName = localizedCountryName(passport.isoNumeric, passport.name, locale)
    val visaTitle = context.getString(visaType.visaTitleResource())
    val stay = if (days != null) {
        context.getString(R.string.stay_up_to_days, visaTitle, days)
    } else {
        visaTitle
    }
    val stayCondition = when {
        visaType == VisaType.HOME_COUNTRY ->
            context.getString(R.string.no_visa_restrictions)
        days != null -> context.getString(R.string.up_to_days, days)
        else -> context.getString(R.string.permit_conditions)
    }
    val needsPreparation = visaType in setOf(
        VisaType.ETA,
        VisaType.E_VISA,
        VisaType.VISA_REQUIRED,
        VisaType.SPECIAL_PERMIT,
        VisaType.MIXED_REQUIREMENTS
    )
    val entryConditions = buildList {
        add(EntryCondition(context.getString(R.string.stay_length), stayCondition))
        if (
            needsPreparation ||
            visaType == VisaType.VISA_ON_ARRIVAL ||
            visaType == VisaType.ENTRY_RESTRICTED ||
            visaType == VisaType.NO_DATA
        ) {
            add(
                EntryCondition(
                    context.getString(R.string.before_travel),
                    visaTitle,
                    accent = visaType != VisaType.NO_DATA
                )
            )
        }
    }
    val beforeTrip = buildList {
        if (visaType != VisaType.HOME_COUNTRY) {
            if (needsPreparation) {
                add(context.getString(R.string.arrange_before_trip, visaTitle))
            }
            if (
                visaType in setOf(
                    VisaType.FREEDOM,
                    VisaType.VISA_FREE,
                    VisaType.ETA,
                    VisaType.VISA_ON_ARRIVAL,
                    VisaType.E_VISA,
                    VisaType.VISA_REQUIRED
                )
            ) {
                add(context.getString(R.string.check_passport_validity))
            }
            add(context.getString(R.string.check_official_requirements))
        }
    }
    val passportNote = buildString {
        append(context.getString(R.string.passport_note_generic, passportName, stay))
        requirement?.note?.takeIf { it.isNotBlank() }?.let { note ->
            append("\n\n")
            append(context.getString(R.string.source_note, note))
        }
        requirement?.validUntil?.takeIf { it.isNotBlank() }?.let { validUntil ->
            append("\n\n")
            append(context.getString(R.string.valid_until, validUntil))
        }
    }
    val showPassportNote = visaType == VisaType.FREEDOM ||
        visaType == VisaType.ENTRY_RESTRICTED ||
        visaType == VisaType.SPECIAL_PERMIT ||
        visaType == VisaType.MIXED_REQUIREMENTS ||
        visaType == VisaType.NO_DATA ||
        !requirement?.note.isNullOrBlank()
    val showStatusExplanation = visaType in setOf(
        VisaType.FREEDOM,
        VisaType.ETA,
        VisaType.VISA_ON_ARRIVAL,
        VisaType.E_VISA,
        VisaType.VISA_REQUIRED,
        VisaType.ENTRY_RESTRICTED,
        VisaType.SPECIAL_PERMIT,
        VisaType.MIXED_REQUIREMENTS,
        VisaType.NO_DATA
    )
    val warning = when (visaType) {
        VisaType.ENTRY_RESTRICTED -> context.getString(R.string.warning_restricted)
        VisaType.SPECIAL_PERMIT,
        VisaType.MIXED_REQUIREMENTS,
        VisaType.NO_DATA -> context.getString(R.string.warning_official)
        else -> null
    }

    return CountryInfo(
        isoNumeric = countryIso,
        flag = flag.ifBlank { "🌍" },
        name = name,
        region = passportRegionFor(countryIso).name,
        visaType = visaType,
        stay = stay,
        stayDays = days,
        entryConditions = entryConditions,
        beforeTrip = beforeTrip,
        applicationDocumentsTitle = entryGuide?.let {
            context.getString(R.string.documents_for_passport, passportName)
        },
        applicationDocuments = entryGuide?.documents.orEmpty(),
        applicationDocumentsNote = entryGuide?.documentsNote,
        passportNote = passportNote,
        showPassportNote = showPassportNote,
        statusExplanation = context.getString(
            R.string.status_explanation_generic,
            visaTitle
        ),
        showStatusExplanation = showStatusExplanation,
        warning = warning,
        entryRequirements = entryRequirements,
        entryGuide = entryGuide,
        source = requirement?.source,
        sourceUrl = requirement?.sourceUrl,
        sourceUpdated = requirement?.updated,
        sourceType = requirement?.sourceType ?: VisaSourceType.UNKNOWN,
        sourceDescription = requirement?.sourceDescription,
        sourceLicense = requirement?.sourceLicense,
        sourceIsRuleSpecific = requirement?.sourceIsRuleSpecific == true
    )
}
