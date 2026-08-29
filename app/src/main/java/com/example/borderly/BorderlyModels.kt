package com.example.borderly

import androidx.compose.ui.graphics.Color

internal val Black = Color(0xFF121313)
internal val Page = Color(0xFFFAFAFA)
internal val TextPrimary = Color(0xFF171717)
internal val TextSecondary = Color(0xFF565656)
internal val Line = Color(0xFFDADADA)
// Unified map palette. Both the visa map and the passport-strength map now
// use the same green -> yellow -> orange -> red visual language.
internal val HomeCountry = Color(0xFF7B61A8)
internal val Freedom = Color(0xFF2F8F5B)
internal val VisaFree = Color(0xFF79AE3D)
internal val Eta = Color(0xFFB6B943)
internal val VisaOnArrival = Color(0xFFF0C63A)
internal val EVisa = Color(0xFFE27B2B)
internal val VisaRequired = Color(0xFFC44736)
internal val EntryRestricted = Color(0xFF8F3029)
internal val SpecialPermit = Color(0xFF6F7690)
internal val MixedRequirements = Color(0xFF8B6F47)
internal val NoVisaData = Color(0xFFD4D9DA)

// Passport-strength palette used by the ranking map: strongest -> weakest.
internal val StrengthVeryHigh = Color(0xFF2F8F5B)
internal val StrengthHigh = Color(0xFF79AE3D)
internal val StrengthMedium = Color(0xFFF0C63A)
internal val StrengthLow = Color(0xFFE27B2B)
internal val StrengthVeryLow = Color(0xFFC44736)
internal val StrengthNoData = Color(0xFFD4D9DA)

internal data class Passport(
    val name: String,
    val flag: String,
    val isoNumeric: Int,
    val region: PassportRegion
)

internal enum class PassportRegion(val title: String) {
    EUROPE("Европа"),
    ASIA("Азия"),
    AMERICAS("Америка"),
    AFRICA("Африка"),
    OCEANIA("Океания")
}

internal enum class PassportRegionFilter(
    val title: String,
    val region: PassportRegion?
) {
    ALL("Все", null),
    EUROPE("Европа", PassportRegion.EUROPE),
    ASIA("Азия", PassportRegion.ASIA),
    AMERICAS("Америка", PassportRegion.AMERICAS),
    AFRICA("Африка", PassportRegion.AFRICA),
    OCEANIA("Океания", PassportRegion.OCEANIA)
}

internal enum class RankingSortOrder(val title: String) {
    STRONGEST_FIRST("Сильные сначала"),
    WEAKEST_FIRST("Слабые сначала")
}

internal enum class VisaType(val title: String, val color: Color) {
    HOME_COUNTRY("Страна паспорта", HomeCountry),
    FREEDOM("Свобода передвижения", Freedom),
    VISA_FREE("Без визы", VisaFree),
    ETA("eTA/ESTA", Eta),
    VISA_ON_ARRIVAL("Виза по прибытии", VisaOnArrival),
    E_VISA("Электронная виза", EVisa),
    VISA_REQUIRED("Нужна виза", VisaRequired),
    ENTRY_RESTRICTED("Въезд ограничен", EntryRestricted),
    SPECIAL_PERMIT("Специальное разрешение", SpecialPermit),
    MIXED_REQUIREMENTS("Условия различаются", MixedRequirements),
    NO_DATA("Нет подтверждённых данных", NoVisaData)
}

internal fun visaTypeFromStorageStatus(status: String): VisaType = when (status) {
    "freedom" -> VisaType.FREEDOM
    "visa free" -> VisaType.VISA_FREE
    "eta" -> VisaType.ETA
    "visa on arrival" -> VisaType.VISA_ON_ARRIVAL
    "e-visa" -> VisaType.E_VISA
    "visa required" -> VisaType.VISA_REQUIRED
    "entry restricted" -> VisaType.ENTRY_RESTRICTED
    "special permit" -> VisaType.SPECIAL_PERMIT
    "mixed requirements" -> VisaType.MIXED_REQUIREMENTS
    "no data" -> VisaType.NO_DATA
    else -> error("visa_requirements.json: unknown status $status")
}

internal enum class VisaSourceType {
    OFFICIAL,
    CORROBORATED,
    DATASET,
    DERIVED,
    UNKNOWN;

    companion object {
        fun fromStorage(value: String?): VisaSourceType = when (value?.lowercase()) {
            "official" -> OFFICIAL
            "corroborated" -> CORROBORATED
            "dataset" -> DATASET
            "derived" -> DERIVED
            else -> UNKNOWN
        }
    }
}

internal data class VisaSource(
    val id: String,
    val name: String,
    val url: String,
    val type: VisaSourceType,
    val description: String?,
    val license: String?
)

internal data class VisaRequirement(
    val status: String,
    val visaType: VisaType,
    val stayDays: Int?,
    val source: String?,
    val sourceUrl: String?,
    val updated: String?,
    val validUntil: String?,
    val note: String?,
    val sourceType: VisaSourceType = VisaSourceType.UNKNOWN,
    val sourceDescription: String? = null,
    val sourceLicense: String? = null,
    val sourceIsRuleSpecific: Boolean = false
)

internal enum class VisaDatabaseOrigin(val label: String) {
    BUNDLED("встроенная база"),
    REMOTE("синхронизирована")
}

internal enum class VisaUpdateCheckStatus {
    UPDATED,
    CURRENT,
    SKIPPED,
    FAILED
}

internal data class VisaUpdateCheckResult(
    val status: VisaUpdateCheckStatus,
    val database: VisaDatabase? = null,
    val remoteVersion: Int? = null,
    val checkedAtMillis: Long? = null,
    val failureMessage: String? = null
)

internal class VisaDatabase(
    val source: String,
    val sourceUrl: String,
    val updated: String,
    val origin: VisaDatabaseOrigin,
    val version: Int,
    val destinationCount: Int,
    val lastSuccessfulCheckAt: Long,
    private val requirementsByPassport: Map<Int, Map<Int, VisaRequirement>>
) {
    fun withLastSuccessfulCheckAt(timestamp: Long): VisaDatabase =
        VisaDatabase(
            source = source,
            sourceUrl = sourceUrl,
            updated = updated,
            origin = origin,
            version = version,
            destinationCount = destinationCount,
            lastSuccessfulCheckAt = timestamp,
            requirementsByPassport = requirementsByPassport
        )

    fun supportedPassportIds(): Set<Int> = requirementsByPassport.keys

    fun supportedDestinationIds(): Set<Int> {
        val firstPassportIso = supportedPassportIds().firstOrNull()
            ?: return emptySet()
        return requirementsByPassport[firstPassportIso]
            .orEmpty()
            .keys
            .plus(firstPassportIso)
            .toSet()
    }

    fun requirementsFor(passportIso: Int): Map<Int, VisaRequirement> {
        return requirementsByPassport[passportIso].orEmpty()
    }
}

internal enum class CountryFilter(
    val title: String,
    val region: PassportRegion? = null
) {
    ALL("Все страны"),
    EUROPE("Европа", PassportRegion.EUROPE),
    ASIA("Азия", PassportRegion.ASIA),
    AMERICAS("Америка", PassportRegion.AMERICAS),
    AFRICA("Африка", PassportRegion.AFRICA),
    OCEANIA("Океания", PassportRegion.OCEANIA)
}

internal enum class VisaStatusFilter(
    val title: String,
    val visaType: VisaType?
) {
    ALL("Все", null),
    VISA_FREE("Без визы", VisaType.VISA_FREE),
    FREEDOM("Свобода передвижения", VisaType.FREEDOM),
    ETA("eTA/ESTA", VisaType.ETA),
    VISA_ON_ARRIVAL("Виза по прибытии", VisaType.VISA_ON_ARRIVAL),
    E_VISA("Электронная виза", VisaType.E_VISA),
    VISA_REQUIRED("Нужна виза", VisaType.VISA_REQUIRED),
    ENTRY_RESTRICTED("Въезд ограничен", VisaType.ENTRY_RESTRICTED),
    SPECIAL_PERMIT("Специальное разрешение", VisaType.SPECIAL_PERMIT),
    MIXED_REQUIREMENTS("Условия различаются", VisaType.MIXED_REQUIREMENTS),
    NO_DATA("Нет подтверждённых данных", VisaType.NO_DATA)
}

internal enum class MapVisaQuickFilter(val title: String) {
    ALL("Все"),
    VISA_FREE("Без визы"),
    FREEDOM("Свобода передвижения"),
    ETA("eTA/ESTA"),
    VISA_ON_ARRIVAL("Виза по прибытии"),
    E_VISA("eVisa"),
    VISA_REQUIRED("Нужна виза"),
    ENTRY_RESTRICTED("Въезд ограничен"),
    SPECIAL_PERMIT("Специальное разрешение"),
    MIXED_REQUIREMENTS("Условия различаются");

    fun matches(visaType: VisaType): Boolean = when (this) {
        ALL -> true
        VISA_FREE -> visaType == VisaType.VISA_FREE
        FREEDOM -> visaType == VisaType.FREEDOM
        ETA -> visaType == VisaType.ETA
        VISA_ON_ARRIVAL -> visaType == VisaType.VISA_ON_ARRIVAL
        E_VISA -> visaType == VisaType.E_VISA
        VISA_REQUIRED -> visaType == VisaType.VISA_REQUIRED
        ENTRY_RESTRICTED -> visaType == VisaType.ENTRY_RESTRICTED
        SPECIAL_PERMIT -> visaType == VisaType.SPECIAL_PERMIT
        MIXED_REQUIREMENTS -> visaType == VisaType.MIXED_REQUIREMENTS
    }

    val accentColor: Color?
        get() = when (this) {
            ALL -> null
            VISA_FREE -> VisaFree
            FREEDOM -> Freedom
            ETA -> Eta
            VISA_ON_ARRIVAL -> VisaOnArrival
            E_VISA -> EVisa
            VISA_REQUIRED -> VisaRequired
            ENTRY_RESTRICTED -> EntryRestricted
            SPECIAL_PERMIT -> SpecialPermit
            MIXED_REQUIREMENTS -> MixedRequirements
        }
}

internal val MapFilteredOut = Color(0xFFE3E7E8)

internal fun MapVisaQuickFilter.toVisaStatusFilter(): VisaStatusFilter = when (this) {
    MapVisaQuickFilter.ALL -> VisaStatusFilter.ALL
    MapVisaQuickFilter.VISA_FREE -> VisaStatusFilter.VISA_FREE
    MapVisaQuickFilter.FREEDOM -> VisaStatusFilter.FREEDOM
    MapVisaQuickFilter.ETA -> VisaStatusFilter.ETA
    MapVisaQuickFilter.VISA_ON_ARRIVAL -> VisaStatusFilter.VISA_ON_ARRIVAL
    MapVisaQuickFilter.E_VISA -> VisaStatusFilter.E_VISA
    MapVisaQuickFilter.VISA_REQUIRED -> VisaStatusFilter.VISA_REQUIRED
    MapVisaQuickFilter.ENTRY_RESTRICTED -> VisaStatusFilter.ENTRY_RESTRICTED
    MapVisaQuickFilter.SPECIAL_PERMIT -> VisaStatusFilter.SPECIAL_PERMIT
    MapVisaQuickFilter.MIXED_REQUIREMENTS -> VisaStatusFilter.MIXED_REQUIREMENTS
}

internal enum class AppTab {
    MAP,
    COMPARE,
    RANKING,
    SETTINGS
}

internal enum class PassportPickerTarget {
    MAIN,
    COMPARE_FIRST,
    COMPARE_SECOND
}

internal data class EntryCondition(
    val label: String,
    val value: String,
    val accent: Boolean = false
)

internal data class OfficialEntryLink(
    val title: String,
    val url: String,
    val primary: Boolean = false
)

internal enum class EntryRequirementType(val label: String) {
    ARRIVAL_CARD("Въездная форма"),
    PRE_TRAVEL_REGISTRATION("Предварительная регистрация"),
    HEALTH_DECLARATION("Медицинская декларация"),
    CUSTOMS_DECLARATION("Таможенная декларация"),
    TOURISM_REGISTRATION("Регистрация поездки"),
    OTHER_ENTRY_FORMALITY("Дополнительное требование")
}

internal data class EntryRequirement(
    val id: String,
    val passportIso: Int,
    val destinationIso: Int,
    val applicableVisaTypes: Set<VisaType>,
    val type: EntryRequirementType,
    val title: String,
    val summary: String,
    val timing: String,
    val mandatory: Boolean,
    val steps: List<String>,
    val officialAuthority: String,
    val sourceUrl: String,
    val verified: String
)

internal data class EntryRequirementUpdateResult(
    val status: VisaUpdateCheckStatus,
    val database: EntryRequirementDatabase? = null,
    val remoteVersion: Int? = null,
    val checkedAtMillis: Long? = null,
    val failureMessage: String? = null
)

internal class EntryRequirementDatabase(
    val version: Int,
    val updated: String,
    val origin: VisaDatabaseOrigin,
    val lastSuccessfulCheckAt: Long,
    private val requirements: Map<Pair<Int, Int>, List<EntryRequirement>>
) {
    fun requirementsFor(
        passportIso: Int,
        destinationIso: Int,
        currentVisaType: VisaType
    ): List<EntryRequirement> =
        requirements[passportIso to destinationIso]
            .orEmpty()
            .filter { currentVisaType in it.applicableVisaTypes }

    fun withLastSuccessfulCheckAt(timestamp: Long): EntryRequirementDatabase =
        EntryRequirementDatabase(
            version = version,
            updated = updated,
            origin = origin,
            lastSuccessfulCheckAt = timestamp,
            requirements = requirements
        )

    fun requirementCount(): Int = requirements.values.sumOf { it.size }
}

internal data class PassportEntryGuide(
    val passportIso: Int,
    val destinationIso: Int,
    val applicableVisaTypes: Set<VisaType>,
    val permitName: String,
    val applicationMethod: String,
    val steps: List<String>,
    val documents: List<String>,
    val documentsNote: String,
    val fee: String?,
    val timing: String?,
    val extraNote: String?,
    val officialAuthority: String,
    val verified: String,
    val links: List<OfficialEntryLink>
)

internal data class EntryGuideUpdateResult(
    val status: VisaUpdateCheckStatus,
    val database: EntryGuideDatabase? = null,
    val remoteVersion: Int? = null,
    val checkedAtMillis: Long? = null,
    val failureMessage: String? = null
)

internal class EntryGuideDatabase(
    val version: Int,
    val updated: String,
    val origin: VisaDatabaseOrigin,
    val lastSuccessfulCheckAt: Long,
    private val guides: Map<Pair<Int, Int>, PassportEntryGuide>
) {
    fun guideFor(
        passportIso: Int,
        destinationIso: Int,
        currentVisaType: VisaType
    ): PassportEntryGuide? {
        val guide = guides[passportIso to destinationIso] ?: return null
        return guide.takeIf { currentVisaType in it.applicableVisaTypes }
    }

    fun withLastSuccessfulCheckAt(timestamp: Long): EntryGuideDatabase =
        EntryGuideDatabase(
            version = version,
            updated = updated,
            origin = origin,
            lastSuccessfulCheckAt = timestamp,
            guides = guides
        )

    fun guideCount(): Int = guides.size
}

internal data class CountryInfo(
    val isoNumeric: Int,
    val flag: String,
    val name: String,
    val region: String,
    val visaType: VisaType,
    val stay: String,
    val stayDays: Int?,
    val entryConditions: List<EntryCondition>,
    val beforeTrip: List<String>,
    val applicationDocumentsTitle: String?,
    val applicationDocuments: List<String>,
    val applicationDocumentsNote: String?,
    val passportNote: String,
    val showPassportNote: Boolean,
    val statusExplanation: String,
    val showStatusExplanation: Boolean,
    val warning: String?,
    val entryRequirements: List<EntryRequirement>,
    val entryGuide: PassportEntryGuide?,
    val source: String?,
    val sourceUrl: String?,
    val sourceUpdated: String?,
    val sourceType: VisaSourceType = VisaSourceType.UNKNOWN,
    val sourceDescription: String? = null,
    val sourceLicense: String? = null,
    val sourceIsRuleSpecific: Boolean = false
)

internal sealed interface HomeSheet {
    data class Picker(
        val filter: CountryFilter,
        val initialVisaFilter: VisaStatusFilter? = null,
        val exactVisaType: VisaType? = null
    ) : HomeSheet

    data class Details(val country: CountryInfo) : HomeSheet

    data class PassportPicker(val target: PassportPickerTarget) : HomeSheet
}

internal const val RussiaIsoNumeric = 643

// Geographic filters follow the UN M49 classification. Kosovo is kept in
// Europe because it belongs to Borderly's destination universe but does not
// have a separate regional row in M49.
internal val EuropeanPassportIds = setOf(
    8, 20, 40, 56, 70, 100, 112, 191, 203, 208, 233, 246, 250,
    276, 300, 348, 352, 372, 380, 428, 438, 440, 442, 470, 498, 499,
    528, 578, 616, 620, 642, 643, 688, 703, 705, 724, 752, 756, 804,
    807, 826, 983, 336, 492, 674,
    234, 248, 292, 744, 831, 832, 833
)

internal val AsianPassportIds = setOf(
    4, 31, 48, 50, 51, 64, 96, 104, 116, 144, 156, 158, 268, 275,
    344, 356, 360, 364, 368, 376, 392, 398, 400, 408, 410, 414, 417,
    418, 422, 458, 496, 512, 524, 586, 608, 626, 634, 682, 702, 704,
    760, 762, 764, 784, 792, 795, 860, 887, 446, 462, 196
)

internal val AmericanPassportIds = setOf(
    32, 44, 52, 68, 76, 84, 124, 152, 170, 188, 192, 212, 214, 218,
    222, 320, 328, 332, 340, 388, 484, 558, 591, 600, 604, 659, 740,
    780, 840, 858, 862, 28, 308, 662, 670,
    60, 92, 136, 238, 239, 254, 312, 474, 500, 531, 533, 534, 535,
    630, 652, 660, 663, 796, 850,
    304, // Greenland
    666  // Saint Pierre and Miquelon
)

internal val AfricanPassportIds = setOf(
    12, 24, 72, 108, 120, 132, 140, 148, 174, 178, 180, 204, 226, 231,
    232, 262, 266, 270, 288, 324, 384, 404, 426, 430, 434, 450, 454,
    466, 478, 480, 504, 508, 516, 562, 566, 624, 646, 678, 686, 694,
    706, 710, 716, 728, 729, 748, 768, 788, 800, 818, 834, 854, 894,
    690, 175, 638, 654, 732, 86, 260
)

internal val OceanianPassportIds = setOf(
    36, 90, 242, 296, 548, 554, 584, 585, 598, 882, 520, 583, 776, 798,
    16, 162, 166, 184, 258, 316, 334, 540, 570, 574, 580, 581,
    612, 772, 876
)

internal fun passportRegionFor(isoNumeric: Int): PassportRegion = when (isoNumeric) {
    in EuropeanPassportIds -> PassportRegion.EUROPE
    in AsianPassportIds -> PassportRegion.ASIA
    in AmericanPassportIds -> PassportRegion.AMERICAS
    in AfricanPassportIds -> PassportRegion.AFRICA
    in OceanianPassportIds -> PassportRegion.OCEANIA
    else -> error("Unknown Borderly region for ISO numeric code: $isoNumeric")
}

internal fun passportDisplayName(isoNumeric: Int, mapName: String): String =
    when (isoNumeric) {
        156 -> "Китай"
        408 -> "КНДР"
        410 -> "Южная Корея"
        417 -> "Кыргызстан"
        498 -> "Молдова"
        643 -> "Россия"
        795 -> "Туркменистан"
        else -> mapName
    }

// Visa categories are authoritative in visa_requirements.json.
// The Android client must never promote/downgrade a destination based on
// hard-coded passport/country lists. HOME_COUNTRY is the only local UI state.
internal fun visaTypeFor(
    passport: Passport,
    countryIso: Int,
    requirements: Map<Int, VisaRequirement>
): VisaType {
    if (passport.isoNumeric == countryIso) return VisaType.HOME_COUNTRY
    return requirements[countryIso]?.visaType ?: VisaType.NO_DATA
}

// Passport strength is intentionally based only on travel that does not
// require arranging a visa: visa-free entry + freedom of movement.
internal fun VisaType.countsTowardPassportRanking(): Boolean = when (this) {
    VisaType.FREEDOM,
    VisaType.VISA_FREE -> true
    else -> false
}

// Keep this lazy: VisaType reads the palette above while its enum constants are
// being created. Eagerly reading VisaType here would create a JVM initialization
// cycle and could leave the ranking set incomplete during local unit tests.
internal val ScoredVisaTypes: Set<VisaType> by lazy(LazyThreadSafetyMode.PUBLICATION) {
    setOf(VisaType.FREEDOM, VisaType.VISA_FREE)
}

internal data class PassportMobility(
    val passport: Passport,
    val score: Int,
    val accessibleCountries: Set<Int>,
    val counts: Map<VisaType, Int>
)

internal fun passportMobility(
    passport: Passport,
    map: NativeMapData?,
    visaDatabase: VisaDatabase
): PassportMobility {
    val requirements = visaDatabase.requirementsFor(passport.isoNumeric)
    val counts = map?.countries
        ?.groupingBy { country ->
            visaTypeFor(
                passport = passport,
                countryIso = country.id,
                requirements = requirements
            )
        }
        ?.eachCount()
        .orEmpty()
    val accessibleCountries = map?.countries
        ?.asSequence()
        ?.filter { country ->
            visaTypeFor(passport, country.id, requirements) in ScoredVisaTypes
        }
        ?.map { it.id }
        ?.toSet()
        .orEmpty()
    return PassportMobility(
        passport = passport,
        score = accessibleCountries.size,
        accessibleCountries = accessibleCountries,
        counts = counts
    )
}

// Remote visa-data updates.
//
// Public version manifest for Borderly visa-data updates.
// The full database URL is resolved relative to this file.
