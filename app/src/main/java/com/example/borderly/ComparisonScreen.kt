package com.example.borderly

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun PassportComparisonScreen(
    firstPassport: Passport,
    secondPassport: Passport,
    nativeMap: NativeMapData?,
    visaDatabase: VisaDatabase,
    onChooseFirst: () -> Unit,
    onChooseSecond: () -> Unit,
    onOpenOnMap: (Passport) -> Unit
) {
    val firstMobility = remember(firstPassport, nativeMap, visaDatabase) {
        passportMobility(firstPassport, nativeMap, visaDatabase)
    }
    val secondMobility = remember(secondPassport, nativeMap, visaDatabase) {
        passportMobility(secondPassport, nativeMap, visaDatabase)
    }
    val commonCountries = remember(firstMobility, secondMobility) {
        firstMobility.accessibleCountries intersect secondMobility.accessibleCountries
    }
    val firstOnly = remember(firstMobility, secondMobility) {
        firstMobility.accessibleCountries - secondMobility.accessibleCountries
    }
    val secondOnly = remember(firstMobility, secondMobility) {
        secondMobility.accessibleCountries - firstMobility.accessibleCountries
    }
    val countryNames = remember(nativeMap) {
        nativeMap?.countries?.associate { it.id to it.name }.orEmpty()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 132.dp)
    ) {
        item {
            ComparisonHeader(
                firstPassport = firstPassport,
                secondPassport = secondPassport,
                onChooseFirst = onChooseFirst,
                onChooseSecond = onChooseSecond
            )
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                ComparisonResultCard(
                    firstPassport = firstPassport,
                    secondPassport = secondPassport,
                    commonCount = commonCountries.size,
                    firstOnlyCount = firstOnly.size,
                    secondOnlyCount = secondOnly.size
                )

                ComparisonBreakdownCard(
                    firstPassport = firstPassport,
                    secondPassport = secondPassport,
                    firstMobility = firstMobility,
                    secondMobility = secondMobility,
                    modifier = Modifier.padding(top = 10.dp)
                )

                UniqueDestinationsCard(
                    firstPassport = firstPassport,
                    secondPassport = secondPassport,
                    firstOnly = firstOnly,
                    secondOnly = secondOnly,
                    countryNames = countryNames,
                    modifier = Modifier.padding(top = 10.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ComparisonMapButton(
                        passport = firstPassport,
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenOnMap(firstPassport) }
                    )
                    ComparisonMapButton(
                        passport = secondPassport,
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenOnMap(secondPassport) }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// Same floating pill header as the Map tab, with two passport selectors.
@Composable
private fun ComparisonHeader(
    firstPassport: Passport,
    secondPassport: Passport,
    onChooseFirst: () -> Unit,
    onChooseSecond: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        val headerWidth = maxWidth
        val compact = headerWidth < 360.dp
        val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val floatingControlColor = if (darkTheme) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
        } else {
            Color(0xFFF9F9F9).copy(alpha = 0.90f)
        }
        val floatingControlBorder = if (darkTheme) {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.92f)
        } else {
            Color.White
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 20.dp,
                    top = if (compact) 18.dp else 22.dp,
                    end = 20.dp,
                    bottom = if (compact) 13.dp else 17.dp
                )
        ) {
            PassportSelector(
                selectedPassport = firstPassport,
                onClick = onChooseFirst,
                label = "Первый паспорт",
                maxSelectorWidth = headerWidth,
                controlHeight = 48.dp,
                controlColor = floatingControlColor,
                controlBorderColor = floatingControlBorder,
                controlRadius = 50.dp,
                modifier = Modifier.fillMaxWidth()
            )
            PassportSelector(
                selectedPassport = secondPassport,
                onClick = onChooseSecond,
                label = "Второй паспорт",
                maxSelectorWidth = headerWidth,
                controlHeight = 48.dp,
                controlColor = floatingControlColor,
                controlBorderColor = floatingControlBorder,
                controlRadius = 50.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}

// Shared card container: same glass surface and faded custom rim as the
// statistics and region cards on the Map tab.
@Composable
private fun ComparisonCard(
    headerLabel: String,
    modifier: Modifier = Modifier,
    headerTrailing: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardRimColor = if (darkTheme) {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.92f)
    } else {
        Color.White
    }
    val cardColor = if (darkTheme) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
    } else {
        Color(0xFFF9F9F9).copy(alpha = 0.90f)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .borderlyRoundedRectRim(
                rimColor = cardRimColor,
                cornerRadius = 26.dp
            ),
        color = cardColor,
        shape = RoundedCornerShape(26.dp),
        border = null
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = headerLabel,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                headerTrailing()
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 15.dp,
                        top = 2.dp,
                        end = 15.dp,
                        bottom = 18.dp
                    ),
                content = content
            )
        }
    }
}

@Composable
private fun ComparisonResultCard(
    firstPassport: Passport,
    secondPassport: Passport,
    commonCount: Int,
    firstOnlyCount: Int,
    secondOnlyCount: Int,
    modifier: Modifier = Modifier
) {
    ComparisonCard(
        headerLabel = "Результат",
        modifier = modifier,
        headerTrailing = {
            Text(
                text = "${commonCount + firstOnlyCount + secondOnlyCount} стран",
                color = borderlySecondaryContentColor(),
                fontSize = 11.sp
            )
        }
    ) {
        ComparisonMetricRow(
            label = "Доступно обоим",
            value = commonCount.toString()
        )
        ComparisonMetricRow(
            label = "Только ${firstPassport.name}",
            value = firstOnlyCount.toString(),
            modifier = Modifier.padding(top = 12.dp)
        )
        ComparisonMetricRow(
            label = "Только ${secondPassport.name}",
            value = secondOnlyCount.toString(),
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
internal fun ComparisonMetricRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = borderlySecondaryContentColor(),
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
internal fun ComparisonBreakdownCard(
    firstPassport: Passport,
    secondPassport: Passport,
    firstMobility: PassportMobility,
    secondMobility: PassportMobility,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        "Свобода передвижения" to VisaType.FREEDOM,
        "Без визы" to VisaType.VISA_FREE,
        VisaType.ETA.title to VisaType.ETA,
        "Виза по прибытии" to VisaType.VISA_ON_ARRIVAL,
        VisaType.E_VISA.title to VisaType.E_VISA,
        "Нужна виза" to VisaType.VISA_REQUIRED,
        VisaType.ENTRY_RESTRICTED.title to VisaType.ENTRY_RESTRICTED,
        VisaType.SPECIAL_PERMIT.title to VisaType.SPECIAL_PERMIT,
        VisaType.MIXED_REQUIREMENTS.title to VisaType.MIXED_REQUIREMENTS,
        VisaType.NO_DATA.title to VisaType.NO_DATA
    )
    ComparisonCard(
        headerLabel = "Тип въезда",
        modifier = modifier,
        headerTrailing = {
            Text(
                text = firstPassport.flag,
                modifier = Modifier.width(42.dp),
                fontSize = 17.sp
            )
            Text(
                text = secondPassport.flag,
                modifier = Modifier.width(42.dp),
                fontSize = 17.sp
            )
        }
    ) {
        rows.forEachIndexed { index, (label, visaType) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (index > 0) Modifier.padding(top = 10.dp)
                        else Modifier
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(visaType.color, RoundedCornerShape(3.dp))
                )
                Text(
                    text = label,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = (firstMobility.counts[visaType] ?: 0).toString(),
                    modifier = Modifier.width(42.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = (secondMobility.counts[visaType] ?: 0).toString(),
                    modifier = Modifier.width(42.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
internal fun UniqueDestinationsCard(
    firstPassport: Passport,
    secondPassport: Passport,
    firstOnly: Set<Int>,
    secondOnly: Set<Int>,
    countryNames: Map<Int, String>,
    modifier: Modifier = Modifier
) {
    val firstNames = firstOnly.mapNotNull { countryNames[it] }.sorted().take(6)
    val secondNames = secondOnly.mapNotNull { countryNames[it] }.sorted().take(6)
    ComparisonCard(
        headerLabel = "Уникальные направления",
        modifier = modifier,
        headerTrailing = {
            Text(
                text = "${firstOnly.size + secondOnly.size} стран",
                color = borderlySecondaryContentColor(),
                fontSize = 11.sp
            )
        }
    ) {
        Text(
            text = "Только ${firstPassport.name}",
            color = borderlySecondaryContentColor(),
            fontSize = 11.sp
        )
        Text(
            text = firstNames.ifEmpty { listOf("Нет") }.joinToString(" • "),
            modifier = Modifier.padding(top = 3.dp),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
        Text(
            text = "Только ${secondPassport.name}",
            modifier = Modifier.padding(top = 13.dp),
            color = borderlySecondaryContentColor(),
            fontSize = 11.sp
        )
        Text(
            text = secondNames.ifEmpty { listOf("Нет") }.joinToString(" • "),
            modifier = Modifier.padding(top = 3.dp),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

// Same black pill treatment as the selected filter chips on the map.
@Composable
internal fun ComparisonMapButton(
    passport: Passport,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(46.dp)
            .noRippleClick(onClick),
        color = borderlySelectedControlColor(),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, borderlySelectedControlColor())
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${passport.flag} На карту",
                color = borderlySelectedContentColor(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}