package com.example.borderly

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Which side wins a head-to-head metric: true = first passport better,
 * false = second passport better, null = tie or no data.
 */
private fun <T : Comparable<T>> betterSide(
    first: T?,
    second: T?,
    lowerWins: Boolean
): Boolean? {
    if (first == null || second == null || first == second) return null
    val firstWins = if (lowerWins) first < second else first > second
    return if (firstWins) true else false
}

private data class PassportEntryDifference(
    val countryIso: Int,
    val countryName: String,
    val countryFlag: String,
    val firstType: VisaType,
    val secondType: VisaType,
    val firstStayDays: Int?,
    val secondStayDays: Int?,
    val advantage: Int,
    val gap: Int
)

private data class PassportComparisonSummary(
    val firstAdvantages: List<PassportEntryDifference>,
    val secondAdvantages: List<PassportEntryDifference>,
    val allDirections: List<PassportEntryDifference>,
    val firstVisaFreeOnly: List<PassportEntryDifference>,
    val secondVisaFreeOnly: List<PassportEntryDifference>,
    val sameConditions: Int,
    val unknownConditions: Int
)

/**
 * Lower value = easier entry. HOME_COUNTRY and NO_DATA are not compared as
 * advantages: a home country is not a foreign destination and missing data
 * must never be presented as a travel privilege.
 */
private fun visaConvenienceRank(type: VisaType): Int? = when (type) {
    VisaType.FREEDOM -> 0
    VisaType.VISA_FREE -> 1
    VisaType.ETA -> 2
    VisaType.VISA_ON_ARRIVAL -> 3
    VisaType.E_VISA -> 4
    VisaType.SPECIAL_PERMIT -> 5
    VisaType.MIXED_REQUIREMENTS -> 6
    VisaType.VISA_REQUIRED -> 7
    VisaType.ENTRY_RESTRICTED -> 8
    VisaType.HOME_COUNTRY, VisaType.NO_DATA -> null
}

@Composable
internal fun PassportComparisonScreen(
    firstPassport: Passport,
    secondPassport: Passport,
    nativeMap: NativeMapData?,
    visaDatabase: VisaDatabase,
    rankByPassport: Map<Int, Int>,
    onChooseFirst: () -> Unit,
    onChooseSecond: () -> Unit,
    onOpenOnMap: (Passport) -> Unit
) {
    val displayLocale = LocalConfiguration.current.locales[0]
    val firstMobility = remember(firstPassport, nativeMap, visaDatabase) {
        passportMobility(firstPassport, nativeMap, visaDatabase)
    }
    val secondMobility = remember(secondPassport, nativeMap, visaDatabase) {
        passportMobility(secondPassport, nativeMap, visaDatabase)
    }

    // Ranks come precomputed from the home container (shared with the
    // Rating tab), so opening this tab no longer rebuilds the whole world
    // ranking on the main thread.

    val comparison = remember(
        firstPassport,
        secondPassport,
        nativeMap,
        visaDatabase,
        displayLocale
    ) {
        val firstRequirements = visaDatabase.requirementsFor(firstPassport.isoNumeric)
        val secondRequirements = visaDatabase.requirementsFor(secondPassport.isoNumeric)
        val differences = mutableListOf<PassportEntryDifference>()
        val allDirections = mutableListOf<PassportEntryDifference>()
        var same = 0
        var unknown = 0

        nativeMap?.countries.orEmpty().forEach { country ->
            // The map also contains a few technical/disputed geometry entries
            // without ISO numeric codes. They are not travel destinations.
            if (country.id !in 1..999) {
                return@forEach
            }

            val firstType = visaTypeFor(firstPassport, country.id, firstRequirements)
            val secondType = visaTypeFor(secondPassport, country.id, secondRequirements)
            val firstStayDays = firstRequirements[country.id]?.stayDays
            val secondStayDays = secondRequirements[country.id]?.stayDays
            val firstRank = visaConvenienceRank(firstType)
            val secondRank = visaConvenienceRank(secondType)
            val direction = PassportEntryDifference(
                countryIso = country.id,
                countryName = country.name,
                countryFlag = country.flag,
                firstType = firstType,
                secondType = secondType,
                firstStayDays = firstStayDays,
                secondStayDays = secondStayDays,
                advantage = 0,
                gap = 0
            )
            allDirections += direction

            // Home countries stay visible in the "All" tab, but cannot count
            // as a passport advantage.
            if (country.id == firstPassport.isoNumeric || country.id == secondPassport.isoNumeric) {
                return@forEach
            }

            if (firstRank == null || secondRank == null) {
                unknown += 1
                return@forEach
            }

            val typeGap = kotlin.math.abs(firstRank - secondRank)
            val sameType = firstRank == secondRank
            val comparableStay = sameType &&
                firstStayDays != null && secondStayDays != null &&
                firstStayDays != secondStayDays

            if (sameType && !comparableStay) {
                same += 1
            } else {
                val advantage = when {
                    firstRank < secondRank -> 1
                    secondRank < firstRank -> 2
                    firstStayDays != null && secondStayDays != null && firstStayDays > secondStayDays -> 1
                    else -> 2
                }
                differences += direction.copy(
                    advantage = advantage,
                    gap = if (typeGap > 0) typeGap * 1000
                    else kotlin.math.abs((firstStayDays ?: 0) - (secondStayDays ?: 0))
                )
            }
        }

        val ordering = compareByDescending<PassportEntryDifference> { it.gap }
            .thenBy {
                localizedCountryName(it.countryIso, it.countryName, displayLocale)
            }
        val alphabeticalDirections = allDirections.sortedBy {
            localizedCountryName(it.countryIso, it.countryName, displayLocale)
        }

        PassportComparisonSummary(
            firstAdvantages = differences.filter { it.advantage == 1 }.sortedWith(ordering),
            secondAdvantages = differences.filter { it.advantage == 2 }.sortedWith(ordering),
            allDirections = alphabeticalDirections,
            firstVisaFreeOnly = alphabeticalDirections.filter { direction ->
                direction.countryIso != firstPassport.isoNumeric &&
                    direction.countryIso != secondPassport.isoNumeric &&
                    direction.firstType in ScoredVisaTypes &&
                    direction.secondType !in ScoredVisaTypes
            },
            secondVisaFreeOnly = alphabeticalDirections.filter { direction ->
                direction.countryIso != firstPassport.isoNumeric &&
                    direction.countryIso != secondPassport.isoNumeric &&
                    direction.secondType in ScoredVisaTypes &&
                    direction.firstType !in ScoredVisaTypes
            },
            sameConditions = same,
            unknownConditions = unknown
        )
    }

    val comparisonListState = rememberLazyListState()
    val scrollToTopHazeState = rememberHazeState()
    val scrollToTopScope = rememberCoroutineScope()
    val glassControlColor = borderlyControlSurfaceColor()
    val lowEndDevice = LocalBorderlyLowEndMode.current
    val roundControlRimColor = borderlyControlRimColor()
    val roundControlColor = if (lowEndDevice) {
        // Непрозрачная поверхность вместо стекла: blur выключен, а кнопка
        // не должна просвечивать.
        borderlyOpaqueControlSurfaceColor()
    } else {
        glassControlColor.copy(alpha = 0.68f)
    }
    val scrollToTopHazeStyle = HazeStyle(
        backgroundColor = MaterialTheme.colorScheme.surface,
        tint = HazeTint(roundControlColor),
        blurRadius = 3.dp,
        noiseFactor = 0f,
        fallbackTint = HazeTint(roundControlColor)
    )
    val scrollToTopContentColor = borderlyPrimaryContentColor()
    val showScrollToTop =
        comparisonListState.firstVisibleItemIndex > 1 ||
            comparisonListState.firstVisibleItemScrollOffset > 300

    // Sub-tab selection of the advantages card lives here so the card can be
    // flattened into lazy items: rows are no longer all composed at once.
    var advantagesTab by remember(firstPassport.isoNumeric, secondPassport.isoNumeric) {
        mutableStateOf(1)
    }
    val advantagesRows = when (advantagesTab) {
        0 -> comparison.firstVisaFreeOnly
        2 -> comparison.secondVisaFreeOnly
        else -> comparison.allDirections
    }
    // Cascade plays once per sub-tab switch and only on capable devices.
    // The window closes shortly after the cascade, so lazy recycling never
    // replays the entrance mid-scroll (no disappearing rows).
    var lastCascadeTab by remember { mutableStateOf(-1) }
    val cascadeActive = !lowEndDevice && advantagesTab != lastCascadeTab
    LaunchedEffect(advantagesTab) {
        if (advantagesTab != lastCascadeTab) {
            delay(360L)
            lastCascadeTab = advantagesTab
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = comparisonListState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = scrollToTopHazeState),
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
                ComparisonOverviewCard(
                    firstPassport = firstPassport,
                    secondPassport = secondPassport,
                    firstRank = rankByPassport[firstPassport.isoNumeric],
                    secondRank = rankByPassport[secondPassport.isoNumeric],
                    firstMobility = firstMobility,
                    secondMobility = secondMobility
                )
            }

            item {
                ComparisonAdvantagesTopCard(
                    firstPassport = firstPassport,
                    secondPassport = secondPassport,
                    directionCount = advantagesRows.size,
                    selectedTab = advantagesTab,
                    onTabSelected = { advantagesTab = it },
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            if (advantagesRows.isEmpty()) {
                item {
                    ComparisonAdvantagesEmptyBottom()
                }
            } else {
                // Rows are separate lazy items: only the visible ones get
                // composed instead of ~200 rows in one pass. Keys are stable
                // per country so sub-tab switches update rows in place.
                itemsIndexed(
                    items = advantagesRows,
                    key = { _, direction -> direction.countryIso }
                ) { index, direction ->
                    ComparisonAdvantagesRowItem(
                        direction = direction,
                        index = index,
                        isLast = direction.countryIso == advantagesRows.last().countryIso,
                        cascadeActive = cascadeActive
                    )
                }
            }

            item {
                SameConditionsCard(
                    sameCount = comparison.sameConditions,
                    unknownCount = comparison.unknownConditions,
                    modifier = Modifier
                        .padding(start = 12.dp, top = 12.dp, end = 12.dp)
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Materialize instead of popping in: scale from 0.9 (never from 0)
        // with a quick fade, per the "nothing appears from nothing" rule.
        AnimatedVisibility(
            visible = showScrollToTop,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 132.dp),
            enter = fadeIn(animationSpec = tween(160)) + scaleIn(
                initialScale = 0.9f,
                animationSpec = tween(180, easing = BorderlyStrongEaseOut)
            ),
            exit = fadeOut(animationSpec = tween(120))
        ) {
            ComparisonScrollToTopButton(
                hazeState = scrollToTopHazeState,
                hazeStyle = scrollToTopHazeStyle,
                blurEnabled = !lowEndDevice,
                rimColor = roundControlRimColor,
                contentColor = scrollToTopContentColor,
                onClick = {
                    scrollToTopScope.launch {
                        comparisonListState.scrollToItem(0)
                    }
                }
            )
        }
    }
}

@Composable
private fun ComparisonScrollToTopButton(
    hazeState: HazeState,
    hazeStyle: HazeStyle,
    blurEnabled: Boolean,
    rimColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .hazeEffect(
                state = hazeState,
                style = hazeStyle
            ) {
                inputScale = HazeInputScale.Auto
                this.blurEnabled = blurEnabled
            }
            .drawBehind {
                val borderInset = 0.75.dp.toPx()
                val borderSize = Size(
                    width = size.width - borderInset * 2f,
                    height = size.height - borderInset * 2f
                )

                fun drawFadedRimArc(startAngle: Float) {
                    val oldFadeSweep = 16f
                    val solidSweep = 48f
                    val longFadeSweep = 70f
                    val solidStart = startAngle + oldFadeSweep
                    val solidEnd = solidStart + solidSweep
                    val transparent = rimColor.copy(alpha = 0f)
                    val stroke = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Butt)

                    fun stop(angle: Float): Float = (angle / 360f).coerceIn(0f, 1f)

                    rotate(
                        degrees = solidStart - longFadeSweep,
                        pivot = center
                    ) {
                        val fadeInBrush = Brush.sweepGradient(
                            0f to transparent,
                            stop(longFadeSweep * 0.20f) to rimColor.copy(alpha = rimColor.alpha * 0.05f),
                            stop(longFadeSweep * 0.40f) to rimColor.copy(alpha = rimColor.alpha * 0.18f),
                            stop(longFadeSweep * 0.60f) to rimColor.copy(alpha = rimColor.alpha * 0.42f),
                            stop(longFadeSweep * 0.80f) to rimColor.copy(alpha = rimColor.alpha * 0.72f),
                            stop(longFadeSweep) to rimColor,
                            1f to rimColor,
                            center = center
                        )
                        drawArc(
                            brush = fadeInBrush,
                            startAngle = 0f,
                            sweepAngle = longFadeSweep,
                            useCenter = false,
                            topLeft = Offset(borderInset, borderInset),
                            size = borderSize,
                            style = stroke
                        )
                    }

                    drawArc(
                        color = rimColor,
                        startAngle = solidStart,
                        sweepAngle = solidSweep,
                        useCenter = false,
                        topLeft = Offset(borderInset, borderInset),
                        size = borderSize,
                        style = stroke
                    )

                    rotate(
                        degrees = solidEnd,
                        pivot = center
                    ) {
                        val fadeOutBrush = Brush.sweepGradient(
                            0f to rimColor,
                            stop(longFadeSweep * 0.20f) to rimColor.copy(alpha = rimColor.alpha * 0.72f),
                            stop(longFadeSweep * 0.40f) to rimColor.copy(alpha = rimColor.alpha * 0.42f),
                            stop(longFadeSweep * 0.60f) to rimColor.copy(alpha = rimColor.alpha * 0.18f),
                            stop(longFadeSweep * 0.80f) to rimColor.copy(alpha = rimColor.alpha * 0.05f),
                            stop(longFadeSweep) to transparent,
                            1f to transparent,
                            center = center
                        )
                        drawArc(
                            brush = fadeOutBrush,
                            startAngle = 0f,
                            sweepAngle = longFadeSweep,
                            useCenter = false,
                            topLeft = Offset(borderInset, borderInset),
                            size = borderSize,
                            style = stroke
                        )
                    }
                }

                drawFadedRimArc(185f)
                drawFadedRimArc(5f)
            }
            .borderlyPressable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowUp,
            contentDescription = stringResource(R.string.back_to_top),
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
    }
}


@Composable
private fun ComparisonOverviewCard(
    firstPassport: Passport,
    secondPassport: Passport,
    firstRank: Int?,
    secondRank: Int?,
    firstMobility: PassportMobility,
    secondMobility: PassportMobility
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        ComparisonCard(headerLabel = stringResource(R.string.comparison)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PassportMiniHeader(
                    passport = firstPassport,
                    modifier = Modifier.weight(1f)
                )
                VsBadge(modifier = Modifier.width(132.dp))
                PassportMiniHeader(
                    passport = secondPassport,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            ComparisonCenterMetric(
                left = firstRank,
                right = secondRank,
                title = stringResource(R.string.ranking_position),
                firstBetter = betterSide(firstRank, secondRank, lowerWins = true)
            )
            ComparisonCenterMetric(
                left = firstMobility.score,
                right = secondMobility.score,
                title = stringResource(R.string.visa_free_metric),
                firstBetter = betterSide(firstMobility.score, secondMobility.score, lowerWins = false)
            )
        }
    }
}

@Composable
private fun PassportMiniHeader(
    passport: Passport,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Flag sits in a soft circular chip so both sides of the duel
        // read as equal, mirrored "players".
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                .border(1.dp, borderlyControlRimColor(), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = passport.flag,
                fontSize = 20.sp,
                lineHeight = 24.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = passport.localizedName(),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VsBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                .border(1.dp, borderlyControlRimColor(), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "VS",
                color = borderlySecondaryContentColor(),
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ComparisonCenterMetric(
    left: Any?,
    right: Any?,
    title: String,
    firstBetter: Boolean?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ComparisonMetricValue(
            value = left,
            isWinner = firstBetter == true,
            isTie = firstBetter == null,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = title.uppercase(),
            modifier = Modifier.width(132.dp),
            color = borderlySecondaryContentColor(),
            fontSize = 10.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.4.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        ComparisonMetricValue(
            value = right,
            isWinner = firstBetter == false,
            isTie = firstBetter == null,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ComparisonMetricValue(
    value: Any?,
    isWinner: Boolean,
    isTie: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 54.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isWinner) borderlyMutedControlColor()
                    else Color.Transparent
                )
                .padding(horizontal = 10.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value?.toString() ?: "—",
                color = if (isWinner || isTie) {
                    borderlyPrimaryContentColor()
                } else {
                    borderlySecondaryContentColor()
                },
                fontSize = if (isWinner || isTie) 17.sp else 15.sp,
                lineHeight = 22.sp,
                fontWeight = if (isWinner || isTie) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Top part of the flattened advantages card: rounded top corners, top rim
 * route, header, the sliding sub-tab toggle and the column captions.
 */
@Composable
private fun comparisonAdvantagesCardColor(): Color {
    return borderlyControlSurfaceColor()
}

@Composable
private fun ComparisonAdvantagesTopCard(
    firstPassport: Passport,
    secondPassport: Passport,
    directionCount: Int,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val cardRimColor = borderlyControlRimColor()
    val cardColor = comparisonAdvantagesCardColor()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .borderlyRoundedRectRim(
                rimColor = cardRimColor,
                cornerRadius = 30.dp,
                drawBottom = false
            ),
        color = cardColor,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        border = null
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.passport_advantages),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.directions_count, directionCount),
                    color = borderlySecondaryContentColor(),
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, top = 4.dp, end = 18.dp)
            ) {
                ComparisonAdvantagesToggle(
                    firstPassport = firstPassport,
                    secondPassport = secondPassport,
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.destination),
                        modifier = Modifier.fillMaxWidth(),
                        color = borderlySecondaryContentColor(),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparisonAdvantagesToggle(
    firstPassport: Passport,
    secondPassport: Passport,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val toggleColor = if (!borderlyIsDarkTheme()) {
        // Darken the complete selector track in the light theme. The selected
        // segment keeps its own color and continues to glide across this track.
        borderlyMutedControlColor()
    } else {
        borderlyControlSurfaceColor()
    }
    val toggleRimColor = borderlyControlRimColor()
    val selectedColor = borderlySelectedControlColor()
    val selectedContentColor = borderlySelectedContentColor()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(toggleColor, RoundedCornerShape(50))
            .borderlyAdaptivePillRim(
                rimColor = toggleRimColor,
                solidFraction = 0.13f,
                fadeFraction = 0.19f
            )
            .clip(RoundedCornerShape(50))
            .padding(3.dp)
    ) {
        // The outer track and its three tap targets stay fixed. Only the dark
        // slider borrows a little visual space when a country label is long.
        val segmentWidth = maxWidth / 3f
        val firstTitle = "${firstPassport.flag} ${firstPassport.localizedName()}"
        val secondTitle = "${secondPassport.flag} ${secondPassport.localizedName()}"
        val textMeasurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val widestLabelStyle = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        val firstTextWidth = with(density) {
            textMeasurer.measure(
                text = firstTitle,
                style = widestLabelStyle,
                maxLines = 1
            ).size.width.toDp()
        }
        val secondTextWidth = with(density) {
            textMeasurer.measure(
                text = secondTitle,
                style = widestLabelStyle,
                maxLines = 1
            ).size.width.toDp()
        }
        val maxAdaptiveExtra = ((segmentWidth - 56.dp) / 2f).coerceAtLeast(0.dp)
        val firstExtra = (firstTextWidth + 16.dp - segmentWidth)
            .coerceIn(0.dp, maxAdaptiveExtra)
        val secondExtra = (secondTextWidth + 16.dp - segmentWidth)
            .coerceIn(0.dp, maxAdaptiveExtra)
        val centerInset = maxOf(firstExtra, secondExtra)

        val targetIndicatorX = when (selectedTab) {
            0 -> 0.dp
            1 -> segmentWidth + centerInset
            else -> segmentWidth * 2f - secondExtra
        }
        val targetIndicatorWidth = when (selectedTab) {
            0 -> segmentWidth + firstExtra
            1 -> segmentWidth - centerInset * 2f
            else -> segmentWidth + secondExtra
        }

        // Position and width animate together, so the same slider glides and
        // reshapes instead of swapping between three separate backgrounds.
        val indicatorX by animateDpAsState(
            targetValue = targetIndicatorX,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "comparisonTabIndicator"
        )
        val indicatorWidth by animateDpAsState(
            targetValue = targetIndicatorWidth,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "comparisonTabIndicatorWidth"
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorX)
                .width(indicatorWidth)
                .fillMaxHeight()
                .background(selectedColor, RoundedCornerShape(50))
        )

        Row(modifier = Modifier.fillMaxSize()) {
            listOf(
                firstTitle,
                stringResource(R.string.all),
                secondTitle
            ).forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        selectedContentColor
                    } else {
                        borderlySecondaryContentColor()
                    },
                    animationSpec = tween(160, easing = BorderlyStrongEaseOut),
                    label = "comparisonTabContentColor"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .borderlyPressable { onTabSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    val adaptiveTextModifier = when (index) {
                        0 -> Modifier
                            .requiredWidth(segmentWidth + firstExtra)
                            .offset(x = firstExtra / 2f)
                        2 -> Modifier
                            .requiredWidth(segmentWidth + secondExtra)
                            .offset(x = -(secondExtra / 2f))
                        else -> Modifier
                    }
                    Text(
                        text = title,
                        modifier = adaptiveTextModifier.padding(horizontal = 8.dp),
                        color = contentColor,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * One lazy row of the flattened advantages card. The last row also carries
 * the bottom padding and the rounded bottom corners with the rim route, so
 * the card still reads as one continuous surface. The entrance cascade only
 * plays right after a sub-tab switch on capable devices.
 */
@Composable
private fun ComparisonAdvantagesRowItem(
    direction: PassportEntryDifference,
    index: Int,
    isLast: Boolean,
    cascadeActive: Boolean
) {
    val cardRimColor = borderlyControlRimColor()
    val cardColor = comparisonAdvantagesCardColor()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .then(
                if (isLast) {
                    Modifier
                        .clip(
                            RoundedCornerShape(
                                bottomStart = 30.dp,
                                bottomEnd = 30.dp
                            )
                        )
                        .borderlyRoundedRectRim(
                            rimColor = cardRimColor,
                            cornerRadius = 30.dp,
                            drawTop = false
                        )
                } else {
                    Modifier
                }
            )
            .background(cardColor)
            .padding(horizontal = 18.dp)
    ) {
        if (cascadeActive && index < 6) {
            var started by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(index * 24L)
                started = true
            }
            val rowAlpha by animateFloatAsState(
                targetValue = if (started) 1f else 0f,
                animationSpec = tween(160, easing = BorderlyStrongEaseOut),
                label = "directionRowAlpha"
            )
            val rowOffsetY by animateDpAsState(
                targetValue = if (started) 0.dp else 6.dp,
                animationSpec = tween(180, easing = BorderlyStrongEaseOut),
                label = "directionRowOffset"
            )
            Box(
                modifier = Modifier.graphicsLayer {
                    alpha = rowAlpha
                    translationY = rowOffsetY.toPx()
                }
            ) {
                Column {
                    ComparisonDirectionRow(item = direction)
                    if (isLast) {
                        Spacer(modifier = Modifier.height(20.dp))
                    } else {
                        ComparisonDirectionDivider()
                    }
                }
            }
        } else {
            Column {
                ComparisonDirectionRow(item = direction)
                if (isLast) {
                    Spacer(modifier = Modifier.height(20.dp))
                } else {
                    ComparisonDirectionDivider()
                }
            }
        }
    }
}

/**
 * Bottom of the advantages card for the empty state: message plus the
 * rounded bottom corners with the bottom rim route.
 */
@Composable
private fun ComparisonAdvantagesEmptyBottom() {
    val cardRimColor = borderlyControlRimColor()
    val cardColor = comparisonAdvantagesCardColor()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(
                RoundedCornerShape(
                    bottomStart = 30.dp,
                    bottomEnd = 30.dp
                )
            )
            .borderlyRoundedRectRim(
                rimColor = cardRimColor,
                cornerRadius = 30.dp,
                drawTop = false
            )
            .background(cardColor)
            .padding(horizontal = 18.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.no_unique_visa_free),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 4.dp),
                color = borderlySecondaryContentColor(),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ComparisonDirectionRow(item: PassportEntryDifference) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            ComparisonEntryBadge(type = item.firstType)
        }

        Column(
            modifier = Modifier
                .weight(1.25f)
                .padding(horizontal = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.countryFlag,
                fontSize = 20.sp,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = localizedCountryName(item.countryIso, item.countryName),
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            ComparisonEntryBadge(type = item.secondType)
        }
    }
}

@Composable
private fun ComparisonDirectionDivider() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
    )
}

@Composable
private fun comparisonVisaTypeTitle(type: VisaType): String = type.localizedTitle()

@Composable
private fun ComparisonEntryBadge(type: VisaType) {
    Row(
        modifier = Modifier
            .background(
                color = type.color.copy(alpha = 0.12f),
                shape = RoundedCornerShape(50)
            )
            .widthIn(max = 124.dp)
            .padding(horizontal = 11.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(type.color, CircleShape)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = comparisonVisaTypeTitle(type),
            color = type.color,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PassportComparisonColumn(
    passport: Passport,
    otherPassport: Passport,
    rank: Int?,
    mobility: PassportMobility,
    advantages: List<PassportEntryDifference>,
    passportIsFirst: Boolean,
    modifier: Modifier = Modifier,
    onOpenOnMap: () -> Unit
) {
    Column(modifier = modifier) {
        PassportColumnSummaryCard(
            passport = passport,
            rank = rank,
            score = mobility.score,
            advantagesCount = advantages.size
        )

        PassportTypeColumnCard(
            mobility = mobility,
            modifier = Modifier.padding(top = 8.dp)
        )

        PassportAdvantagesCard(
            title = stringResource(R.string.advantages),
            passport = passport,
            otherPassport = otherPassport,
            advantages = advantages,
            passportIsFirst = passportIsFirst,
            modifier = Modifier.padding(top = 8.dp)
        )

        ComparisonMapButton(
            passport = passport,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            onClick = onOpenOnMap
        )
    }
}

@Composable
private fun PassportColumnSummaryCard(
    passport: Passport,
    rank: Int?,
    score: Int,
    advantagesCount: Int,
    modifier: Modifier = Modifier
) {
    ComparisonCard(
        headerLabel = "${passport.flag} ${passport.localizedName()}",
        modifier = modifier
    ) {
        ColumnMetric(
            label = stringResource(R.string.place),
            value = rank?.let { "$it" } ?: "—"
        )
        ColumnMetric(
            label = stringResource(R.string.destinations),
            value = score.toString(),
            modifier = Modifier.padding(top = 9.dp)
        )
        ColumnMetric(
            label = stringResource(R.string.advantages_count_label),
            value = advantagesCount.toString(),
            modifier = Modifier.padding(top = 9.dp)
        )
    }
}

@Composable
private fun ColumnMetric(
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
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PassportTypeColumnCard(
    mobility: PassportMobility,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        VisaType.FREEDOM.localizedTitle() to VisaType.FREEDOM,
        VisaType.VISA_FREE.localizedTitle() to VisaType.VISA_FREE,
        VisaType.ETA.localizedTitle() to VisaType.ETA,
        VisaType.VISA_ON_ARRIVAL.localizedTitle() to VisaType.VISA_ON_ARRIVAL,
        VisaType.E_VISA.localizedTitle() to VisaType.E_VISA,
        VisaType.SPECIAL_PERMIT.localizedTitle() to VisaType.SPECIAL_PERMIT,
        VisaType.MIXED_REQUIREMENTS.localizedTitle() to VisaType.MIXED_REQUIREMENTS,
        VisaType.VISA_REQUIRED.localizedTitle() to VisaType.VISA_REQUIRED,
        VisaType.ENTRY_RESTRICTED.localizedTitle() to VisaType.ENTRY_RESTRICTED,
        VisaType.NO_DATA.localizedTitle() to VisaType.NO_DATA
    )

    ComparisonCard(
        headerLabel = stringResource(R.string.entry_type),
        modifier = modifier
    ) {
        rows.forEachIndexed { index, (label, visaType) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (index > 0) Modifier.padding(top = 8.dp)
                        else Modifier
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(visaType.color, RoundedCornerShape(2.dp))
                )
                Text(
                    text = label,
                    modifier = Modifier
                        .padding(start = 5.dp)
                        .weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = (mobility.counts[visaType] ?: 0).toString(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
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
        val floatingControlColor = borderlyControlSurfaceColor()
        val floatingControlBorder = borderlyControlRimColor()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 12.dp,
                    top = if (compact) 18.dp else 22.dp,
                    end = 12.dp,
                    bottom = 12.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val selectorWidth = (headerWidth - 32.dp) / 2f
            PassportSelector(
                selectedPassport = firstPassport,
                onClick = onChooseFirst,
                label = "1",
                maxSelectorWidth = selectorWidth,
                controlHeight = 48.dp,
                controlColor = floatingControlColor,
                controlBorderColor = floatingControlBorder,
                controlRadius = 50.dp,
                modifier = Modifier.weight(1f)
            )
            PassportSelector(
                selectedPassport = secondPassport,
                onClick = onChooseSecond,
                label = "2",
                maxSelectorWidth = selectorWidth,
                controlHeight = 48.dp,
                controlColor = floatingControlColor,
                controlBorderColor = floatingControlBorder,
                controlRadius = 50.dp,
                modifier = Modifier.weight(1f)
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
    val cardRimColor = borderlyControlRimColor()
    val cardColor = borderlyControlSurfaceColor()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .borderlyRoundedRectRim(
                rimColor = cardRimColor,
                cornerRadius = 30.dp
            ),
        color = cardColor,
        shape = RoundedCornerShape(30.dp),
        border = null
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = headerLabel,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                headerTrailing()
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 18.dp,
                        top = 4.dp,
                        end = 18.dp,
                        bottom = 20.dp
                    ),
                content = content
            )
        }
    }
}

@Composable
private fun ComparisonRankingCard(
    firstPassport: Passport,
    secondPassport: Passport,
    firstRank: Int?,
    secondRank: Int?,
    firstScore: Int,
    secondScore: Int,
    modifier: Modifier = Modifier
) {
    ComparisonCard(
        headerLabel = stringResource(R.string.world_ranking),
        modifier = modifier,
        headerTrailing = {
            Text(
                text = stringResource(R.string.visa_free_plus_freedom),
                color = borderlySecondaryContentColor(),
                fontSize = 10.sp
            )
        }
    ) {
        PassportRankRow(
            passport = firstPassport,
            rank = firstRank,
            score = firstScore
        )
        PassportRankRow(
            passport = secondPassport,
            rank = secondRank,
            score = secondScore,
            modifier = Modifier.padding(top = 12.dp)
        )

        if (firstRank != null && secondRank != null && firstRank != secondRank) {
            val better = if (firstRank < secondRank) firstPassport else secondPassport
            val difference = kotlin.math.abs(firstRank - secondRank)
            Text(
                text = stringResource(
                    R.string.higher_by_positions,
                    better.flag,
                    better.localizedName(),
                    difference
                ),
                modifier = Modifier.padding(top = 14.dp),
                color = borderlySecondaryContentColor(),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun PassportRankRow(
    passport: Passport,
    rank: Int?,
    score: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = passport.flag, fontSize = 21.sp)
        Text(
            text = passport.localizedName(),
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = rank?.let { "$it место" } ?: "—",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.directions_count, score),
                color = borderlySecondaryContentColor(),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun ComparisonSummaryCard(
    firstPassport: Passport,
    secondPassport: Passport,
    summary: PassportComparisonSummary,
    modifier: Modifier = Modifier
) {
    ComparisonCard(
        headerLabel = stringResource(R.string.main_differences),
        modifier = modifier,
        headerTrailing = {
            Text(
                text = stringResource(R.string.entry_conditions),
                color = borderlySecondaryContentColor(),
                fontSize = 10.sp
            )
        }
    ) {
        ComparisonMetricRow(
            label = stringResource(
                R.string.better_for_passport,
                firstPassport.flag,
                firstPassport.localizedName()
            ),
            value = summary.firstAdvantages.size.toString()
        )
        ComparisonMetricRow(
            label = stringResource(
                R.string.better_for_passport,
                secondPassport.flag,
                secondPassport.localizedName()
            ),
            value = summary.secondAdvantages.size.toString(),
            modifier = Modifier.padding(top = 12.dp)
        )
        ComparisonMetricRow(
            label = stringResource(R.string.same_conditions),
            value = summary.sameConditions.toString(),
            modifier = Modifier.padding(top = 12.dp)
        )
        if (summary.unknownConditions > 0) {
            ComparisonMetricRow(
                label = stringResource(R.string.not_enough_comparison_data),
                value = summary.unknownConditions.toString(),
                modifier = Modifier.padding(top = 12.dp)
            )
        }
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
        VisaType.FREEDOM.localizedTitle() to VisaType.FREEDOM,
        VisaType.VISA_FREE.localizedTitle() to VisaType.VISA_FREE,
        VisaType.ETA.localizedTitle() to VisaType.ETA,
        VisaType.VISA_ON_ARRIVAL.localizedTitle() to VisaType.VISA_ON_ARRIVAL,
        VisaType.E_VISA.localizedTitle() to VisaType.E_VISA,
        VisaType.SPECIAL_PERMIT.localizedTitle() to VisaType.SPECIAL_PERMIT,
        VisaType.MIXED_REQUIREMENTS.localizedTitle() to VisaType.MIXED_REQUIREMENTS,
        VisaType.VISA_REQUIRED.localizedTitle() to VisaType.VISA_REQUIRED,
        VisaType.ENTRY_RESTRICTED.localizedTitle() to VisaType.ENTRY_RESTRICTED,
        VisaType.NO_DATA.localizedTitle() to VisaType.NO_DATA
    )
    ComparisonCard(
        headerLabel = stringResource(R.string.entry_type),
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
private fun PassportAdvantagesCard(
    title: String,
    passport: Passport,
    otherPassport: Passport,
    advantages: List<PassportEntryDifference>,
    passportIsFirst: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember(passport.isoNumeric, otherPassport.isoNumeric) {
        mutableStateOf(false)
    }
    ComparisonCard(
        headerLabel = title,
        modifier = modifier,
        headerTrailing = {
            Text(
                text = stringResource(R.string.countries_count, advantages.size),
                color = borderlySecondaryContentColor(),
                fontSize = 11.sp
            )
        }
    ) {
        if (advantages.isEmpty()) {
            Text(
                text = stringResource(R.string.no_easier_destinations),
                color = borderlySecondaryContentColor(),
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        } else {
            // Strongest differences first; the full list can be expanded in place.
            val visibleAdvantages = if (expanded) advantages else advantages.take(8)
            visibleAdvantages.forEachIndexed { index, item ->
                AdvantageDestinationRow(
                    difference = item,
                    passport = passport,
                    otherPassport = otherPassport,
                    passportIsFirst = passportIsFirst,
                    modifier = if (index == 0) Modifier else Modifier.padding(top = 13.dp)
                )
            }

            if (advantages.size > 8) {
                Text(
                    text = if (expanded) {
                        stringResource(R.string.hide)
                    } else {
                        stringResource(R.string.show_all_count, advantages.size)
                    },
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .noRippleClick { expanded = !expanded },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun AdvantageDestinationRow(
    difference: PassportEntryDifference,
    passport: Passport,
    otherPassport: Passport,
    passportIsFirst: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(
                    if (passportIsFirst) difference.firstType.color else difference.secondType.color,
                    RoundedCornerShape(4.dp)
                )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = difference.countryFlag,
                fontSize = 20.sp
            )
            Text(
                text = localizedCountryName(difference.countryIso, difference.countryName),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .size(14.dp)
                .background(
                    if (passportIsFirst) difference.secondType.color else difference.firstType.color,
                    RoundedCornerShape(4.dp)
                )
        )
    }
}

@Composable
private fun SameConditionsCard(
    sameCount: Int,
    unknownCount: Int,
    modifier: Modifier = Modifier
) {
    ComparisonCard(
        headerLabel = stringResource(R.string.same_conditions),
        modifier = modifier,
        headerTrailing = {
            Text(
                text = stringResource(R.string.countries_count, sameCount),
                color = borderlySecondaryContentColor(),
                fontSize = 11.sp
            )
        }
    ) {
        Text(
            text = stringResource(R.string.same_category_summary, sameCount),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
        if (unknownCount > 0) {
            Text(
                text = stringResource(R.string.unknown_comparison_summary, unknownCount),
                modifier = Modifier.padding(top = 7.dp),
                color = borderlySecondaryContentColor(),
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
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
            .height(48.dp)
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
                text = stringResource(R.string.open_on_map, passport.flag),
                color = borderlySelectedContentColor(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}
