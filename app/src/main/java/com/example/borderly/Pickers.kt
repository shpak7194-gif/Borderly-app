package com.example.borderly

// BORDERLY_COUNTRY_HEADER_EQUAL_VERTICAL_SPACING_2026_08_19

// BORDERLY_SELECTED_PASSPORT_INNER_INSET_4DP_2026_08_19

// BORDERLY_PICKER_SECTION_HEADERS_SIDE_INSETS_FIX_2026_08_19

// BORDERLY_PICKER_FILTERS_EDGE_TO_EDGE_2026_08_19

// BORDERLY_PICKERS_RANKING_API_COMPAT_FIX_2026_08_19

// BORDERLY_PICKERS_SHARED_DESIGN_CODE_V1_COMPILE_FIX_2026_08_19

// BORDERLY_PICKERS_SHARED_DESIGN_CODE_V1_2026_08_19

import android.graphics.Paint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

@Composable
internal fun PassportPickerSheet(
    passports: List<Passport>,
    selectedPassport: Passport,
    recentPassportIds: List<Int>,
    onPassportSelected: (Passport) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var regionFilter by rememberSaveable { mutableStateOf(PassportRegionFilter.ALL) }
    val passportListState = rememberLazyListState()
    val normalizedQuery = query.trim()
    val visiblePassports = passports.filter { passport ->
        matchesCountrySearch(
            displayName = passport.name,
            isoNumeric = passport.isoNumeric,
            query = normalizedQuery
        ) &&
            (regionFilter.region == null || passport.region == regionFilter.region)
    }
    val showRecent = normalizedQuery.isBlank() &&
        regionFilter == PassportRegionFilter.ALL
    val recentPassports = if (showRecent) {
        recentPassportIds.mapNotNull { iso ->
            passports.firstOrNull { it.isoNumeric == iso }
        }
    } else {
        emptyList()
    }
    val recentIds = recentPassports.mapTo(mutableSetOf()) { it.isoNumeric }
    val mainList = if (showRecent) {
        visiblePassports.filterNot { it.isoNumeric in recentIds }
    } else {
        visiblePassports
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        val compact = maxWidth < 360.dp
        val horizontalPadding = if (compact) 14.dp else 22.dp
        val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val pickerSurfaceColor = if (darkTheme) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
        } else {
            Color(0xFFF9F9F9).copy(alpha = 0.90f)
        }
        val pickerRimColor = if (darkTheme) {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.92f)
        } else {
            Color.White
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            Text(
                text = "Выберите паспорт",
                modifier = Modifier.padding(horizontal = horizontalPadding),
                color = borderlyPrimaryContentColor(),
                fontSize = if (compact) 24.sp else 27.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${passports.size} паспортов с визовыми данными",
                modifier = Modifier.padding(
                    start = horizontalPadding,
                    top = 5.dp,
                    end = horizontalPadding
                ),
                color = borderlySecondaryContentColor(),
                fontSize = if (compact) 13.sp else 14.sp
            )

            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = horizontalPadding,
                        top = if (compact) 14.dp else 18.dp,
                        end = horizontalPadding
                    )
                    .heightIn(min = 54.dp)
                    .pickerRoundedRectRim(
                        rimColor = pickerRimColor,
                        cornerRadius = 26.dp
                    ),
                placeholder = {
                    Text(
                        text = "Название паспорта",
                        color = borderlySecondaryContentColor(),
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = borderlySecondaryContentColor()
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(26.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = pickerSurfaceColor,
                    unfocusedContainerColor = pickerSurfaceColor,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.onSurface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
                contentPadding = PaddingValues(
                    start = horizontalPadding,
                    end = horizontalPadding
                )
            ) {
                items(PassportRegionFilter.entries, key = { it.name }) { option ->
                    PassportPickerRegionChip(
                        filter = option,
                        selected = option == regionFilter,
                        compact = compact,
                        surfaceColor = pickerSurfaceColor,
                        rimColor = pickerRimColor,
                        onClick = { regionFilter = option }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = horizontalPadding,
                        top = 13.dp,
                        end = horizontalPadding,
                        bottom = 7.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showRecent && recentPassports.isNotEmpty()) {
                        "Недавние"
                    } else {
                        regionFilter.title
                    },
                    color = borderlyPrimaryContentColor(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Найдено: ${visiblePassports.size}",
                    color = borderlySecondaryContentColor(),
                    fontSize = 12.sp
                )
            }

            if (visiblePassports.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding)
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Паспорт не найден",
                        color = borderlySecondaryContentColor(),
                        fontSize = 15.sp
                    )
                }
            } else {
                LazyColumn(
                    state = passportListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding)
                        .weight(1f)
                        .keepSheetStillUntilNextGesture(passportListState),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    overscrollEffect = null
                ) {
                    if (recentPassports.isNotEmpty()) {
                        items(
                            items = recentPassports,
                            key = { "recent_${it.isoNumeric}" }
                        ) { passport ->
                            PassportPickerRow(
                                passport = passport,
                                selected = passport.isoNumeric == selectedPassport.isoNumeric,
                                compact = compact,
                                surfaceColor = pickerSurfaceColor,
                                rimColor = pickerRimColor,
                                darkTheme = darkTheme,
                                onClick = { onPassportSelected(passport) }
                            )
                        }
                        item(key = "all_passports_title") {
                            Text(
                                text = "Все паспорта",
                                modifier = Modifier.padding(top = 7.dp, bottom = 1.dp),
                                color = borderlyPrimaryContentColor(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    items(
                        items = mainList,
                        key = { "passport_${it.isoNumeric}" }
                    ) { passport ->
                        PassportPickerRow(
                            passport = passport,
                            selected = passport.isoNumeric == selectedPassport.isoNumeric,
                            compact = compact,
                            surfaceColor = pickerSurfaceColor,
                            rimColor = pickerRimColor,
                            darkTheme = darkTheme,
                            onClick = { onPassportSelected(passport) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PassportPickerRegionChip(
    filter: PassportRegionFilter,
    selected: Boolean,
    compact: Boolean,
    surfaceColor: Color,
    rimColor: Color,
    onClick: () -> Unit
) {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val selectedChipColor = if (darkTheme) Color(0xFF303B46) else borderlySelectedControlColor()
    val selectedChipRimColor = if (darkTheme) Color(0xFF657381) else borderlySelectedControlColor()

    Surface(
        modifier = Modifier
            .then(
                if (selected) {
                    Modifier
                } else {
                    Modifier.pickerPillRim(
                        rimColor = rimColor,
                        enabled = true
                    )
                }
            )
            .noRippleClick(onClick),
        color = if (selected) borderlySelectedControlColor() else surfaceColor,
        shape = RoundedCornerShape(50),
        border = if (selected) BorderStroke(1.dp, borderlySelectedControlColor()) else null,
        shadowElevation = 0.dp
    ) {
        Text(
            text = filter.title,
            modifier = Modifier.padding(
                horizontal = if (compact) 10.dp else 13.dp,
                vertical = if (compact) 8.dp else 9.dp
            ),
            color = if (selected) borderlySelectedContentColor() else borderlyPrimaryContentColor(),
            fontSize = if (compact) 11.sp else 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
internal fun PassportRegionChip(
    filter: PassportRegionFilter,
    selected: Boolean,
    compact: Boolean,
    onClick: () -> Unit
) {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val selectedChipColor = if (darkTheme) Color(0xFF303B46) else borderlySelectedControlColor()
    val selectedChipRimColor = if (darkTheme) Color(0xFF657381) else borderlySelectedControlColor()
    val unselectedColor = if (darkTheme) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
    } else {
        Color(0xFFF9F9F9).copy(alpha = 0.90f)
    }
    val unselectedRimColor = if (darkTheme) {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.92f)
    } else {
        Color.White
    }

    Surface(
        modifier = Modifier
            .then(
                if (selected) {
                    Modifier
                } else {
                    Modifier.borderlyAdaptivePillRim(
                        rimColor = unselectedRimColor,
                        solidFraction = 0.13f,
                        fadeFraction = 0.19f
                    )
                }
            )
            .noRippleClick(onClick),
        color = if (selected) borderlySelectedControlColor() else unselectedColor,
        shape = RoundedCornerShape(50),
        border = if (selected) BorderStroke(1.dp, borderlySelectedControlColor()) else null
    ) {
        Text(
            text = filter.title,
            modifier = Modifier.padding(
                horizontal = if (compact) 10.dp else 13.dp,
                vertical = if (compact) 8.dp else 9.dp
            ),
            color = if (selected) borderlySelectedContentColor() else borderlyPrimaryContentColor(),
            fontSize = if (compact) 11.sp else 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
internal fun PassportPickerRow(
    passport: Passport,
    selected: Boolean,
    compact: Boolean,
    surfaceColor: Color,
    rimColor: Color,
    darkTheme: Boolean,
    onClick: () -> Unit
) {
    val rowColor = if (selected) borderlySelectedControlColor() else surfaceColor
    val titleColor = if (selected) borderlySelectedContentColor() else borderlyPrimaryContentColor()
    val subtitleColor = if (selected) borderlySelectedContentColor().copy(alpha = 0.78f) else borderlySecondaryContentColor()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (selected) {
                    Modifier
                } else {
                    Modifier.pickerRoundedRectRim(
                        rimColor = rimColor,
                        cornerRadius = 26.dp
                    )
                }
            )
            .noRippleClick(onClick),
        color = rowColor,
        shape = RoundedCornerShape(26.dp),
        border = if (selected) BorderStroke(1.dp, borderlySelectedControlColor()) else null,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (compact) 12.dp else 16.dp,
                    vertical = if (compact) 11.dp else 13.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = passport.flag, fontSize = if (compact) 27.sp else 30.sp)
            Column(
                modifier = Modifier
                    .padding(start = if (compact) 10.dp else 13.dp)
                    .weight(1f)
            ) {
                Text(
                    text = passport.name,
                    color = titleColor,
                    fontSize = if (compact) 15.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = passport.region.title,
                    modifier = Modifier.padding(top = 2.dp),
                    color = subtitleColor,
                    fontSize = 12.sp
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(borderlySelectedContentColor(), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        color = borderlySelectedControlColor(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
@Composable
internal fun CountryPickerSheet(
    countries: List<CountryInfo>,
    filter: CountryFilter,
    selectedRegion: PassportRegion? = null,
    initialVisaFilter: VisaStatusFilter? = null,
    initialExactVisaType: VisaType? = null,
    onClearSelectedRegion: () -> Unit = {},
    onCountrySelected: (CountryInfo) -> Unit
) {
    var query by rememberSaveable(filter, initialExactVisaType) { mutableStateOf("") }
    val countryListState = rememberLazyListState()
    val effectiveRegion = selectedRegion ?: filter.region
    var statusFilter by rememberSaveable(
        filter,
        selectedRegion,
        initialVisaFilter,
        initialExactVisaType
    ) {
        mutableStateOf(
            initialVisaFilter ?: if (filter.region != null && selectedRegion == null) {
                VisaStatusFilter.VISA_FREE
            } else {
                VisaStatusFilter.ALL
            }
        )
    }
    var exactVisaType by rememberSaveable(filter, initialExactVisaType) {
        mutableStateOf(initialExactVisaType)
    }

    val sortedCountries = remember(countries) {
        countries.sortedBy { it.name }
    }

    val visibleCountries = remember(
        sortedCountries,
        query,
        effectiveRegion,
        statusFilter,
        exactVisaType
    ) {
        sortedCountries.filter { country ->
            matchesCountrySearch(
                displayName = country.name,
                isoNumeric = country.isoNumeric,
                query = query
            ) &&
                (
                    effectiveRegion == null ||
                        passportRegionFor(country.isoNumeric) == effectiveRegion
                ) &&
                (
                    exactVisaType?.let { country.visaType == it }
                        ?: when (statusFilter) {
                            VisaStatusFilter.ALL -> true
                            else -> country.visaType == statusFilter.visaType
                        }
                )
        }
    }

    val visibleStatusFilters = remember(countries) {
        val hasFreedom = countries.any { it.visaType == VisaType.FREEDOM }
        VisaStatusFilter.entries.filter { option ->
            option != VisaStatusFilter.FREEDOM || hasFreedom
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        val compact = maxWidth < 360.dp
        val horizontalPadding = if (compact) 14.dp else 22.dp
        val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val pickerSurfaceColor = if (darkTheme) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
        } else {
            Color(0xFFF9F9F9).copy(alpha = 0.90f)
        }
        val pickerRimColor = if (darkTheme) {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.92f)
        } else {
            Color.White
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            Text(
                text = "Выберите страну",
                modifier = Modifier.padding(horizontal = horizontalPadding),
                color = borderlyPrimaryContentColor(),
                fontSize = if (compact) 24.sp else 27.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Паспорт и направление определяют условия въезда",
                modifier = Modifier.padding(
                    start = horizontalPadding,
                    top = 5.dp,
                    end = horizontalPadding
                ),
                color = borderlySecondaryContentColor(),
                fontSize = if (compact) 13.sp else 14.sp
            )

            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = horizontalPadding,
                        top = if (compact) 14.dp else 18.dp,
                        end = horizontalPadding
                    )
                    .heightIn(min = 54.dp)
                    .pickerRoundedRectRim(
                        rimColor = pickerRimColor,
                        cornerRadius = 26.dp
                    ),
                placeholder = {
                    Text(
                        "Название страны",
                        color = borderlySecondaryContentColor(),
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = borderlySecondaryContentColor()
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(26.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = pickerSurfaceColor,
                    unfocusedContainerColor = pickerSurfaceColor,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.onSurface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
                contentPadding = PaddingValues(
                    start = horizontalPadding,
                    end = horizontalPadding
                )
            ) {
                items(visibleStatusFilters, key = { it.name }) { option ->
                    VisaStatusChip(
                        filter = option,
                        selected = when (val exactType = exactVisaType) {
                            null -> option == statusFilter
                            VisaType.VISA_FREE -> option == VisaStatusFilter.VISA_FREE
                            else -> option.visaType == exactType
                        },
                        compact = compact,
                        surfaceColor = pickerSurfaceColor,
                        rimColor = pickerRimColor,
                        onClick = {
                            if (
                                exactVisaType == VisaType.VISA_FREE &&
                                option == VisaStatusFilter.VISA_FREE
                            ) {
                                statusFilter = VisaStatusFilter.VISA_FREE
                            } else {
                                exactVisaType = null
                                statusFilter = option
                            }
                        }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = horizontalPadding,
                        top = 10.dp,
                        end = horizontalPadding,
                        bottom = 10.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            selectedRegion != null && exactVisaType != null ->
                                "${selectedRegion.title} · ${exactVisaType!!.title}"
                            selectedRegion != null && statusFilter != VisaStatusFilter.ALL ->
                                "${selectedRegion.title} · ${statusFilter.title}"
                            selectedRegion != null -> selectedRegion.title
                            exactVisaType != null -> exactVisaType!!.title
                            filter.region != null && statusFilter != VisaStatusFilter.ALL ->
                                "${filter.title} · ${statusFilter.title}"
                            statusFilter == VisaStatusFilter.ALL -> filter.title
                            else -> statusFilter.title
                        },
                        color = borderlyPrimaryContentColor(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (selectedRegion != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .noRippleClick(onClearSelectedRegion),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Сбросить регион",
                                modifier = Modifier.size(15.dp),
                                tint = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Найдено: ${visibleCountries.size}",
                    color = borderlySecondaryContentColor(),
                    fontSize = 12.sp
                )
            }

            if (visibleCountries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding)
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Страна не найдена",
                        color = borderlySecondaryContentColor(),
                        fontSize = 15.sp
                    )
                }
            } else {
                LazyColumn(
                    state = countryListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding)
                        .weight(1f)
                        .keepSheetStillUntilNextGesture(countryListState),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    overscrollEffect = null
                ) {
                    items(visibleCountries, key = { it.name }) { country ->
                        CountryPickerRow(
                            country = country,
                            compact = compact,
                            surfaceColor = pickerSurfaceColor,
                            rimColor = pickerRimColor,
                            onClick = { onCountrySelected(country) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun VisaStatusChip(
    filter: VisaStatusFilter,
    selected: Boolean,
    compact: Boolean = false,
    surfaceColor: Color,
    rimColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .then(
                if (selected) {
                    Modifier
                } else {
                    Modifier.pickerPillRim(
                        rimColor = rimColor,
                        enabled = true
                    )
                }
            )
            .noRippleClick(onClick),
        color = if (selected) borderlySelectedControlColor() else surfaceColor,
        shape = RoundedCornerShape(50),
        border = if (selected) BorderStroke(1.dp, borderlySelectedControlColor()) else null,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 10.dp else 13.dp,
                vertical = if (compact) 8.dp else 9.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val chipColor = filter.visaType?.color
            chipColor?.let { color ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (selected) Color.White.copy(alpha = .9f) else color,
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(7.dp))
            }
            Text(
                text = filter.title,
                color = if (selected) borderlySelectedContentColor() else borderlyPrimaryContentColor(),
                fontSize = if (compact) 11.sp else 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun CountryPickerRow(
    country: CountryInfo,
    compact: Boolean = false,
    surfaceColor: Color,
    rimColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .pickerRoundedRectRim(
                rimColor = rimColor,
                cornerRadius = 26.dp
            )
            .noRippleClick(onClick),
        color = surfaceColor,
        shape = RoundedCornerShape(26.dp),
        border = null,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 12.dp else 16.dp,
                vertical = if (compact) 11.dp else 13.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = country.flag, fontSize = if (compact) 25.sp else 29.sp)
            Column(
                modifier = Modifier
                    .padding(start = if (compact) 9.dp else 13.dp)
                    .weight(1f)
            ) {
                Text(
                    text = country.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = if (compact) 15.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = country.stay,
                    modifier = Modifier.padding(top = 2.dp),
                    color = borderlySecondaryContentColor(),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .background(
                        country.visaType.color.copy(alpha = .15f),
                        RoundedCornerShape(12.dp)
                    )
                    .widthIn(max = if (compact) 92.dp else 116.dp)
                    .padding(
                        horizontal = if (compact) 7.dp else 9.dp,
                        vertical = 6.dp
                    )
            ) {
                Text(
                    text = country.visaType.title,
                    color = country.visaType.color,
                    fontSize = if (compact) 10.sp else 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Borderly long-pill rim used by picker filters.
 * Explicit upper/lower paths preserve the same design code as map filter pills.
 */
private fun Modifier.pickerPillRim(
    rimColor: Color,
    enabled: Boolean = true
): Modifier = drawWithContent {
    drawContent()
    if (!enabled || size.width <= 0f || size.height <= 0f) return@drawWithContent

    val inset = 0.75.dp.toPx()
    val left = inset
    val top = inset
    val right = size.width - inset
    val bottom = size.height - inset
    val radius = ((bottom - top) / 2f).coerceAtLeast(1f)
    val centerY = (top + bottom) / 2f

    val leftArc = android.graphics.RectF(
        left,
        top,
        left + radius * 2f,
        bottom
    )
    val rightArc = android.graphics.RectF(
        right - radius * 2f,
        top,
        right,
        bottom
    )

    val cornerEndProgress = 0.58f

    val upperPath = AndroidPath().apply {
        moveTo(left, centerY)
        arcTo(leftArc, 180f, 90f, false)
        lineTo(right - radius, top)
        arcTo(rightArc, -90f, 90f * cornerEndProgress, false)
    }

    val lowerPath = AndroidPath().apply {
        moveTo(right, centerY)
        arcTo(rightArc, 0f, 90f, false)
        lineTo(left + radius, bottom)
        arcTo(leftArc, 90f, 90f * cornerEndProgress, false)
    }

    drawPickerFadedRoutes(
        upperPath = upperPath,
        lowerPath = lowerPath,
        rimColor = rimColor
    )
}

/**
 * Borderly outer rounded-card rim used by passport/country cards and search.
 */
private fun Modifier.pickerRoundedRectRim(
    rimColor: Color,
    cornerRadius: Dp
): Modifier = drawWithContent {
    drawContent()
    if (size.width <= 0f || size.height <= 0f) return@drawWithContent

    val inset = 0.75.dp.toPx()
    val left = inset
    val top = inset
    val right = size.width - inset
    val bottom = size.height - inset

    val radius = cornerRadius.toPx()
        .coerceAtMost((right - left) / 2f)
        .coerceAtMost((bottom - top) / 2f)

    if (radius <= 0f) return@drawWithContent

    val leftTopRect = android.graphics.RectF(
        left, top, left + radius * 2f, top + radius * 2f
    )
    val rightTopRect = android.graphics.RectF(
        right - radius * 2f, top, right, top + radius * 2f
    )
    val rightBottomRect = android.graphics.RectF(
        right - radius * 2f, bottom - radius * 2f, right, bottom
    )
    val leftBottomRect = android.graphics.RectF(
        left, bottom - radius * 2f, left + radius * 2f, bottom
    )

    val cornerEndProgress = 0.58f

    val upperPath = AndroidPath().apply {
        moveTo(left, top + radius)
        arcTo(leftTopRect, 180f, 90f, false)
        lineTo(right - radius, top)
        arcTo(rightTopRect, -90f, 90f * cornerEndProgress, false)
    }

    val lowerPath = AndroidPath().apply {
        moveTo(right, bottom - radius)
        arcTo(rightBottomRect, 0f, 90f, false)
        lineTo(left + radius, bottom)
        arcTo(leftBottomRect, 90f, 90f * cornerEndProgress, false)
    }

    drawPickerFadedRoutes(
        upperPath = upperPath,
        lowerPath = lowerPath,
        rimColor = rimColor
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPickerFadedRoutes(
    upperPath: AndroidPath,
    lowerPath: AndroidPath,
    rimColor: Color
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.dp.toPx()
        strokeCap = Paint.Cap.BUTT
        strokeJoin = Paint.Join.ROUND
    }

    fun smoothStep(value: Float): Float {
        val t = value.coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas

        fun drawRoute(path: AndroidPath) {
            val measure = android.graphics.PathMeasure(path, false)
            val routeLength = measure.length
            if (routeLength <= 0f) return

            val fadeLength = min(routeLength * 0.18f, 70.dp.toPx())
            val solidStart = fadeLength
            val solidEnd = (routeLength - fadeLength).coerceAtLeast(solidStart)
            val fadeSteps = 32

            fun drawSegment(from: Float, to: Float, alpha: Float) {
                if (to <= from || alpha <= 0f) return

                val segment = AndroidPath()
                if (
                    measure.getSegment(
                        from.coerceIn(0f, routeLength),
                        to.coerceIn(0f, routeLength),
                        segment,
                        true
                    )
                ) {
                    paint.color = rimColor.copy(
                        alpha = rimColor.alpha * alpha.coerceIn(0f, 1f)
                    ).toArgb()
                    native.drawPath(segment, paint)
                }
            }

            for (index in 0 until fadeSteps) {
                val t0 = index.toFloat() / fadeSteps
                val t1 = (index + 1f) / fadeSteps
                drawSegment(
                    fadeLength * t0,
                    fadeLength * t1,
                    smoothStep((t0 + t1) / 2f)
                )
            }

            drawSegment(solidStart, solidEnd, 1f)

            for (index in 0 until fadeSteps) {
                val t0 = index.toFloat() / fadeSteps
                val t1 = (index + 1f) / fadeSteps
                drawSegment(
                    solidEnd + fadeLength * t0,
                    solidEnd + fadeLength * t1,
                    smoothStep(1f - (t0 + t1) / 2f)
                )
            }
        }

        drawRoute(upperPath)
        drawRoute(lowerPath)
    }
}



