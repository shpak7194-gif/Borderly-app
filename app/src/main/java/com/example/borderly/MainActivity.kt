package com.example.borderly

// BORDERLY8_FLOATING_BOTTOM_NAV_OVER_CONTENT_2026_08_18

import android.content.Context
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.res.stringResource
import com.example.borderly.ui.theme.BorderlyTheme
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal enum class AppThemeMode(
    val storageValue: String,
    val title: String
) {
    SYSTEM("system", "Как в системе"),
    LIGHT("light", "Светлая"),
    DARK("dark", "Тёмная");

    companion object {
        fun fromStorage(value: String?): AppThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: SYSTEM
    }
}

private const val ThemePreferencesName = "borderly_theme_preferences"
private const val ThemeModeKey = "theme_mode"

internal fun loadAppThemeMode(context: Context): AppThemeMode {
    val value = context
        .getSharedPreferences(ThemePreferencesName, Context.MODE_PRIVATE)
        .getString(ThemeModeKey, AppThemeMode.SYSTEM.storageValue)

    return AppThemeMode.fromStorage(value)
}

internal fun saveAppThemeMode(
    context: Context,
    mode: AppThemeMode
) {
    context
        .getSharedPreferences(ThemePreferencesName, Context.MODE_PRIVATE)
        .edit()
        .putString(ThemeModeKey, mode.storageValue)
        .apply()
}

// Borderly v51: UI/behavior preserved; implementation split across files.
class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(localizedAppContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val borderlyViewModel =
            ViewModelProvider(this)[BorderlyViewModel::class.java]

        setContent {
            var themeMode by remember {
                mutableStateOf(loadAppThemeMode(this@MainActivity))
            }
            var performanceMode by remember {
                mutableStateOf(loadPerformanceMode(this@MainActivity))
            }
            val systemDarkTheme = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> systemDarkTheme
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            LaunchedEffect(darkTheme) {
                this@MainActivity.enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            AndroidColor.TRANSPARENT,
                            AndroidColor.TRANSPARENT
                        )
                    },
                    navigationBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(AndroidColor.rgb(15, 17, 19))
                    } else {
                        SystemBarStyle.light(
                            AndroidColor.WHITE,
                            AndroidColor.WHITE
                        )
                    }
                )
            }

            val lowEndMode = remember(this@MainActivity, performanceMode) {
                resolveLowEndMode(this@MainActivity, performanceMode)
            }

            CompositionLocalProvider(LocalBorderlyLowEndMode provides lowEndMode) {
                BorderlyTheme(darkTheme = darkTheme) {
                    BorderlyHomeScreen(
                        viewModel = borderlyViewModel,
                        themeMode = themeMode,
                        onThemeModeChange = { mode ->
                            themeMode = mode
                            saveAppThemeMode(this@MainActivity, mode)
                        },
                        performanceMode = performanceMode,
                        onPerformanceModeChange = { mode ->
                            performanceMode = mode
                            savePerformanceMode(this@MainActivity, mode)
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun BorderlyHomeScreen(
    viewModel: BorderlyViewModel,
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    performanceMode: PerformanceMode,
    onPerformanceModeChange: (PerformanceMode) -> Unit
) {
    when (val state = viewModel.dataState) {
        BorderlyDataState.Loading -> BorderlyLoadingScreen()

        is BorderlyDataState.Error -> BorderlyErrorScreen(
            message = state.message,
            onRetry = viewModel::retry
        )

        is BorderlyDataState.Ready -> BorderlyHomeContent(
            nativeMap = state.nativeMap,
            visaDatabase = state.visaDatabase,
            entryGuideDatabase = state.entryGuideDatabase,
            entryRequirementDatabase = state.entryRequirementDatabase,
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            performanceMode = performanceMode,
            onPerformanceModeChange = onPerformanceModeChange
        )
    }
}

@Composable
private fun BorderlyLoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.loading_data),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun BorderlyErrorScreen(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.data_load_error),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = message,
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
            Surface(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .height(48.dp)
                    .noRippleClick(onRetry),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(13.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.retry),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BorderlyHomeContent(
    nativeMap: NativeMapData?,
    visaDatabase: VisaDatabase,
    entryGuideDatabase: EntryGuideDatabase,
    entryRequirementDatabase: EntryRequirementDatabase,
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    onThemeModeChange: (AppThemeMode) -> Unit = {},
    performanceMode: PerformanceMode = PerformanceMode.AUTO,
    onPerformanceModeChange: (PerformanceMode) -> Unit = {}
) {
    val context = LocalContext.current
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    val passports = remember(nativeMap, visaDatabase) {
        val supportedIds = visaDatabase.supportedPassportIds()
        nativeMap?.countries
            ?.asSequence()
            ?.filter { it.id in supportedIds }
            ?.map { country ->
                Passport(
                    name = passportDisplayName(country.id, country.name),
                    flag = country.flag,
                    isoNumeric = country.id,
                    region = passportRegionFor(country.id)
                )
            }
            ?.sortedBy { it.name.lowercase() }
            ?.toList()
            .orEmpty()
            .ifEmpty {
                listOf(Passport("Россия", "🇷🇺", RussiaIsoNumeric, PassportRegion.EUROPE))
            }
    }
    // Тяжёлый рейтинг считается один раз на уровне контейнера и
    // переиспользуется вкладками Сравнение и Рейтинг: при переключении
    // вкладок он больше не пересчитывается заново на главном потоке.
    // Сам расчёт выполняется в фоновом диспетчере и не блокирует UI.
    val sharedRanking by produceState(
        initialValue = emptyList<PassportMobility>(),
        passports,
        nativeMap,
        visaDatabase
    ) {
        value = withContext(Dispatchers.Default) {
            passports
                .map { passportMobility(it, nativeMap, visaDatabase) }
                .sortedWith(
                    compareByDescending<PassportMobility> { it.score }
                        .thenBy { it.passport.name }
                )
        }
    }
    val sharedRankByPassport = remember(sharedRanking) {
        buildMap {
            var previousScore: Int? = null
            var currentRank = 0
            sharedRanking.forEach { mobility ->
                if (mobility.score != previousScore) {
                    currentRank += 1
                    previousScore = mobility.score
                }
                put(mobility.passport.isoNumeric, currentRank)
            }
        }
    }
    val savedPassportIso = remember { loadSelectedPassportIso(context) }
    val passportSaver = remember(passports) {
        Saver<Passport, Int>(
            save = { passport -> passport.isoNumeric },
            restore = { iso -> passports.firstOrNull { it.isoNumeric == iso } }
        )
    }
    var selectedPassport by rememberSaveable(
        passports,
        savedPassportIso,
        stateSaver = passportSaver
    ) {
        mutableStateOf(
            passports.firstOrNull { it.isoNumeric == savedPassportIso }
                ?: passports.firstOrNull { it.isoNumeric == RussiaIsoNumeric }
                ?: passports.first()
        )
    }
    var comparisonFirstPassport by rememberSaveable(passports, stateSaver = passportSaver) {
        mutableStateOf(selectedPassport)
    }
    var comparisonSecondPassport by rememberSaveable(passports, stateSaver = passportSaver) {
        mutableStateOf(
            passports.firstOrNull { it.isoNumeric == 276 && it != selectedPassport }
                ?: passports.firstOrNull { it != selectedPassport }
                ?: selectedPassport
        )
    }
    var recentPassportIds by remember {
        mutableStateOf(
            (listOf(selectedPassport.isoNumeric) + loadRecentPassportIds(context))
                .distinct()
                .take(5)
        )
    }
    val visaRequirements = remember(selectedPassport, visaDatabase) {
        visaDatabase.requirementsFor(selectedPassport.isoNumeric)
    }
    val supportedDestinationIds = remember(visaDatabase) {
        visaDatabase.supportedDestinationIds()
    }
    val countries = remember(
        selectedPassport,
        nativeMap,
        visaRequirements,
        entryGuideDatabase,
        entryRequirementDatabase
    ) {
        nativeMap?.countries
            ?.asSequence()
            ?.filter { it.id in supportedDestinationIds }
            ?.map { country ->
            mapCountryInfo(
                context = context,
                countryIso = country.id,
                name = country.name,
                flag = country.flag,
                passport = selectedPassport,
                requirements = visaRequirements,
                entryGuideDatabase = entryGuideDatabase,
                entryRequirementDatabase = entryRequirementDatabase
            )
        }
            ?.toList()
            .orEmpty()
    }
    val visaCounts = remember(selectedPassport, nativeMap, visaRequirements) {
        nativeMap?.countries
            ?.asSequence()
            ?.filter { it.id in supportedDestinationIds }
            ?.groupingBy { country ->
                visaTypeFor(
                    passport = selectedPassport,
                    countryIso = country.id,
                    requirements = visaRequirements
                )
            }
            ?.eachCount()
            .orEmpty()
    }
    var selectedCountryIso by rememberSaveable { mutableStateOf<Int?>(null) }
    val selectedCountry = remember(countries, selectedCountryIso) {
        countries.firstOrNull { it.isoNumeric == selectedCountryIso }
    }
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.MAP) }
    val homeSheetSaver = remember(countries) {
        Saver<HomeSheet?, String>(
            save = { sheet ->
                when (sheet) {
                    null -> "none"
                    is HomeSheet.Details -> "details:${sheet.country.isoNumeric}"
                    is HomeSheet.PassportPicker -> "passport:${sheet.target.name}"
                    is HomeSheet.Picker -> listOf(
                        "picker",
                        sheet.filter.name,
                        sheet.initialVisaFilter?.name.orEmpty(),
                        sheet.exactVisaType?.name.orEmpty()
                    ).joinToString(":")
                }
            },
            restore = { saved ->
                val parts = saved.split(":")
                when (parts.firstOrNull()) {
                    "none" -> null
                    "details" -> parts.getOrNull(1)
                        ?.toIntOrNull()
                        ?.let { iso -> countries.firstOrNull { it.isoNumeric == iso } }
                        ?.let { country -> HomeSheet.Details(country) }
                    "passport" -> parts.getOrNull(1)
                        ?.let { runCatching { PassportPickerTarget.valueOf(it) }.getOrNull() }
                        ?.let { target -> HomeSheet.PassportPicker(target) }
                    "picker" -> parts.getOrNull(1)
                        ?.let { runCatching { CountryFilter.valueOf(it) }.getOrNull() }
                        ?.let { filter ->
                            HomeSheet.Picker(
                                filter = filter,
                                initialVisaFilter = parts.getOrNull(2)
                                    ?.takeIf(String::isNotEmpty)
                                    ?.let {
                                        runCatching { VisaStatusFilter.valueOf(it) }.getOrNull()
                                    },
                                exactVisaType = parts.getOrNull(3)
                                    ?.takeIf(String::isNotEmpty)
                                    ?.let { runCatching { VisaType.valueOf(it) }.getOrNull() }
                            )
                        }
                    else -> null
                }
            }
        )
    }
    var activeSheet by rememberSaveable(countries, stateSaver = homeSheetSaver) {
        mutableStateOf<HomeSheet?>(null)
    }
    var showFullMap by rememberSaveable { mutableStateOf(false) }
    var isMapInteracting by remember { mutableStateOf(false) }
    var mapVisaQuickFilter by rememberSaveable { mutableStateOf(MapVisaQuickFilter.ALL) }
    var selectedMapRegion by rememberSaveable { mutableStateOf<PassportRegion?>(null) }
    val homeListState = rememberLazyListState()
    val homeScrollScope = rememberCoroutineScope()

    val hasEntryRestrictedCountries = remember(countries) {
        countries.any { it.visaType == VisaType.ENTRY_RESTRICTED }
    }

    LaunchedEffect(selectedPassport.isoNumeric, hasEntryRestrictedCountries) {
        if (
            mapVisaQuickFilter == MapVisaQuickFilter.ENTRY_RESTRICTED &&
            !hasEntryRestrictedCountries
        ) {
            mapVisaQuickFilter = MapVisaQuickFilter.ALL
        }
    }

    val filteredRegionCounts = remember(countries, mapVisaQuickFilter) {
        PassportRegion.entries.associateWith { region ->
            countries.count { country ->
                passportRegionFor(country.isoNumeric) == region &&
                    (
                        mapVisaQuickFilter == MapVisaQuickFilter.ALL ||
                            mapVisaQuickFilter.matches(country.visaType)
                    )
            }
        }
    }

    val visibleVisaCounts = remember(countries, visaCounts, selectedMapRegion) {
        selectedMapRegion?.let { region ->
            countries
                .asSequence()
                .filter { country -> passportRegionFor(country.isoNumeric) == region }
                .groupingBy { country -> country.visaType }
                .eachCount()
        } ?: visaCounts
    }

    val selectPassport: (Passport, PassportPickerTarget) -> Unit = { passport, target ->
        when (target) {
            PassportPickerTarget.MAIN -> {
                selectedPassport = passport
                comparisonFirstPassport = passport
                selectedCountryIso = null
                mapVisaQuickFilter = MapVisaQuickFilter.ALL
            }
            PassportPickerTarget.COMPARE_FIRST -> comparisonFirstPassport = passport
            PassportPickerTarget.COMPARE_SECOND -> comparisonSecondPassport = passport
        }
        recentPassportIds = (listOf(passport.isoNumeric) + recentPassportIds)
            .distinct()
            .take(5)
        savePassportPreferences(
            context = context,
            selectedPassportIso = if (target == PassportPickerTarget.MAIN) {
                passport.isoNumeric
            } else {
                selectedPassport.isoNumeric
            },
            recentPassportIds = recentPassportIds
        )
        activeSheet = null
    }

    val openCountryFromMap: (Int, String, String) -> Unit = { countryIso, name, flag ->
        val country = countries.firstOrNull { it.isoNumeric == countryIso }
            ?: mapCountryInfo(
                context = context,
                countryIso = countryIso,
                name = name,
                flag = flag,
                passport = selectedPassport,
                requirements = visaRequirements,
                entryGuideDatabase = entryGuideDatabase,
                entryRequirementDatabase = entryRequirementDatabase
            )
        selectedCountryIso = country.isoNumeric
        showFullMap = false
        activeSheet = HomeSheet.Details(country)
    }

    BackHandler(enabled = selectedTab == AppTab.SETTINGS) {
        selectedTab = AppTab.MAP
    }

    val bottomNavigationHazeState = rememberHazeState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = bottomNavigationHazeState),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
            AppTab.MAP -> {
                LazyColumn(
                    state = homeListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 132.dp),
                    userScrollEnabled = !isMapInteracting
                ) {
                    item {
                        Header(
                            selectedPassport = selectedPassport,
                            onPassportClick = {
                                activeSheet = HomeSheet.PassportPicker(
                                    PassportPickerTarget.MAIN
                                )
                            }
                        )
                    }

                    item {
                        MapCard(
                            nativeMap = nativeMap,
                            passport = selectedPassport,
                            visaRequirements = visaRequirements,
                            visaCounts = visibleVisaCounts,
                            quickFilter = mapVisaQuickFilter,
                            selectedRegion = selectedMapRegion,
                            countries = countries,
                            selectedCountryIso = selectedCountry?.isoNumeric,
                            onCountrySelected = openCountryFromMap,
                            onEmptySpaceSelected = { selectedCountryIso = null },
                            onInteractionChanged = { isMapInteracting = it },
                            onQuickFilterSelected = { mapVisaQuickFilter = it },
                            onDestinationClick = {
                                activeSheet = HomeSheet.Picker(CountryFilter.ALL)
                            },
                            onExpand = { showFullMap = true }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(18.dp)) }

                    item {
                        RegionAccessSection(
                            nativeMap = nativeMap,
                            regionCounts = filteredRegionCounts,
                            activeFilter = mapVisaQuickFilter,
                            selectedRegion = selectedMapRegion,
                            onRegionClick = { region ->
                                selectedMapRegion = if (selectedMapRegion == region) {
                                    null
                                } else {
                                    region
                                }
                                homeScrollScope.launch {
                                    homeListState.animateScrollToItem(index = 1)
                                }
                            }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }

            AppTab.COMPARE -> PassportComparisonScreen(
                firstPassport = comparisonFirstPassport,
                secondPassport = comparisonSecondPassport,
                nativeMap = nativeMap,
                visaDatabase = visaDatabase,
                rankByPassport = sharedRankByPassport,
                onChooseFirst = {
                    activeSheet = HomeSheet.PassportPicker(
                        PassportPickerTarget.COMPARE_FIRST
                    )
                },
                onChooseSecond = {
                    activeSheet = HomeSheet.PassportPicker(
                        PassportPickerTarget.COMPARE_SECOND
                    )
                },
                onOpenOnMap = { passport ->
                    selectPassport(passport, PassportPickerTarget.MAIN)
                    selectedTab = AppTab.MAP
                }
            )

            AppTab.RANKING -> PassportRankingScreen(
                nativeMap = nativeMap,
                visaDatabase = visaDatabase,
                selectedPassport = selectedPassport,
                ranking = sharedRanking,
                rankByPassport = sharedRankByPassport,
                hazeState = bottomNavigationHazeState,
                onHeaderPassportClick = {
                    activeSheet = HomeSheet.PassportPicker(
                        PassportPickerTarget.MAIN
                    )
                },
                onPassportClick = { passport ->
                    selectPassport(passport, PassportPickerTarget.MAIN)
                    selectedTab = AppTab.MAP
                }
            )

            AppTab.SETTINGS -> Box(
                modifier = Modifier.fillMaxSize()
            ) {
                SettingsScreen(
                    dataVersion = visaDatabase.version,
                    dataUpdated = visaDatabase.updated,
                    dataSource = visaDatabase.source,
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange,
                    performanceMode = performanceMode,
                    onPerformanceModeChange = onPerformanceModeChange,
                    onBack = { selectedTab = AppTab.MAP }
                )
            }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            BottomNavigation(
                selectedTab = selectedTab,
                hazeState = bottomNavigationHazeState,
                onTabClick = { tab ->
                    activeSheet = null
                    showFullMap = false
                    selectedTab = tab
                }
            )
        }
    }

    activeSheet?.let { sheet ->
        ModalBottomSheet(
            modifier = Modifier.fillMaxHeight(),
            onDismissRequest = { activeSheet = null },
            sheetState = bottomSheetState,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 42.dp, height = 4.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp))
                    )
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                when (sheet) {
                    is HomeSheet.Picker -> CountryPickerSheet(
                        countries = countries,
                        filter = sheet.filter,
                        selectedRegion = selectedMapRegion,
                        initialVisaFilter = sheet.initialVisaFilter,
                        initialExactVisaType = sheet.exactVisaType,
                        onClearSelectedRegion = {
                            selectedMapRegion = null
                        },
                        onCountrySelected = { country ->
                            selectedCountryIso = country.isoNumeric
                            activeSheet = HomeSheet.Details(country)
                        }
                    )

                    is HomeSheet.Details -> CountryDetailsSheet(
                        country = sheet.country,
                        passport = selectedPassport,
                        dataSource = visaDatabase.source,
                        dataSourceUrl = visaDatabase.sourceUrl,
                        dataUpdated = visaDatabase.updated,
                        dataOrigin = visaDatabase.origin,
                        dataVersion = visaDatabase.version,
                        dataLastCheckedAt = visaDatabase.lastSuccessfulCheckAt,
                        onChooseAnother = {
                            activeSheet = HomeSheet.Picker(CountryFilter.ALL)
                        },
                        onClose = { activeSheet = null }
                    )

                    is HomeSheet.PassportPicker -> PassportPickerSheet(
                        passports = passports,
                        selectedPassport = when (sheet.target) {
                            PassportPickerTarget.MAIN -> selectedPassport
                            PassportPickerTarget.COMPARE_FIRST -> comparisonFirstPassport
                            PassportPickerTarget.COMPARE_SECOND -> comparisonSecondPassport
                        },
                        recentPassportIds = recentPassportIds,
                        onPassportSelected = { passport ->
                            selectPassport(passport, sheet.target)
                        }
                    )
                }
            }
        }

        LaunchedEffect(sheet) {
            // A country card and the picker always start at half height.
            // Expanding to the full screen remains an explicit user gesture.
            if (bottomSheetState.isVisible) {
                bottomSheetState.partialExpand()
            }
        }
    }

    if (showFullMap) {
        FullScreenWorldMap(
            nativeMap = nativeMap,
            passport = selectedPassport,
            visaRequirements = visaRequirements,
            entryGuideDatabase = entryGuideDatabase,
            entryRequirementDatabase = entryRequirementDatabase,
            visaCounts = visibleVisaCounts,
            selectedRegion = selectedMapRegion,
            dataSource = visaDatabase.source,
            dataSourceUrl = visaDatabase.sourceUrl,
            dataUpdated = visaDatabase.updated,
            dataOrigin = visaDatabase.origin,
            dataVersion = visaDatabase.version,
            dataLastCheckedAt = visaDatabase.lastSuccessfulCheckAt,
            quickFilter = mapVisaQuickFilter,
            selectedCountryIso = selectedCountry?.isoNumeric,
            onCountrySelected = { countryIso, _, _ ->
                selectedCountryIso = countryIso
            },
            onEmptySpaceSelected = { selectedCountryIso = null },
            onClose = { showFullMap = false }
        )
    }
}
