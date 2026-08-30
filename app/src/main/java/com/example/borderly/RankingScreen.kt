package com.example.borderly

import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlin.math.min
import kotlinx.coroutines.launch


internal fun passportStrengthColor(score: Int?, minScore: Int, maxScore: Int): Color {
    if (score == null) return StrengthNoData
    if (maxScore <= minScore) return StrengthVeryHigh
    val strength = (score - minScore).toFloat() / (maxScore - minScore).toFloat()
    return when {
        strength >= .80f -> StrengthVeryHigh
        strength >= .60f -> StrengthHigh
        strength >= .40f -> StrengthMedium
        strength >= .20f -> StrengthLow
        else -> StrengthVeryLow
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun InteractivePassportStrengthMap(
    nativeMap: NativeMapData,
    scoresByCountry: Map<Int, Int>,
    minScore: Int,
    maxScore: Int,
    selectedPassportIso: Int,
    onCountrySelected: (Int, String, String) -> Unit,
    onInteractionChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentCountryCallback = rememberUpdatedState(onCountrySelected)
    val currentInteractionCallback = rememberUpdatedState(onInteractionChanged)
    val displayDensity = context.resources.displayMetrics.density
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val mapCanvasBackground = if (darkTheme) Color(0xFF111B23) else Color(0xFFEEF5FA)
    val mapMarkerHalo = if (darkTheme) Color(0xFF11161B) else Color.White
    val mapMarkerOutline = if (darkTheme) Color(0xFFF1F3F4) else Black
    val mapMarkerLeader = if (darkTheme) Color(0xFFC1C8CE) else Black

    val panSaver = remember {
        listSaver<Offset, Float>(
            save = { offset -> listOf(offset.x, offset.y) },
            restore = { values -> Offset(values[0], values[1]) }
        )
    }
    var zoom by rememberSaveable { mutableStateOf(1f) }
    var pan by rememberSaveable(stateSaver = panSaver) { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var isInteracting by remember { mutableStateOf(false) }

    val fillPaint = remember(nativeMap) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    }
    val borderPaint = remember(nativeMap, darkTheme) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = if (darkTheme) {
                AndroidColor.argb(150, 230, 234, 237)
            } else {
                AndroidColor.WHITE
            }
            strokeJoin = Paint.Join.ROUND
        }
    }
    val graticulePaint = remember(nativeMap, darkTheme) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = if (darkTheme) {
                AndroidColor.argb(34, 142, 158, 169)
            } else {
                AndroidColor.argb(26, 82, 107, 116)
            }
            strokeCap = Paint.Cap.ROUND
        }
    }
    val selectedPaint = remember(nativeMap, darkTheme) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = if (darkTheme) {
                AndroidColor.rgb(244, 246, 247)
            } else {
                AndroidColor.rgb(18, 19, 19)
            }
            strokeJoin = Paint.Join.ROUND
        }
    }
    val graticulePaths = remember(nativeMap) { createWorldGraticule(nativeMap) }
    val markerCountries = remember(nativeMap) {
        nativeMap.countries.filter { it.id in SmallCountryMarkerIds }
    }

    val markerCollisionOffsets = remember(nativeMap, canvasSize, zoom, displayDensity) {
        if (canvasSize.width == 0 || canvasSize.height == 0 || zoom < 1.35f) {
            emptyMap()
        } else {
            val baseScale = min(
                canvasSize.width / nativeMap.width,
                canvasSize.height / nativeMap.height
            )
            val totalScale = baseScale * zoom
            val desiredCenters = markerCountries
                .asSequence()
                .filter { shouldShowSmallCountryMarker(it, totalScale, displayDensity) }
                .map { country ->
                    val screenCenter = Offset(
                        x = canvasSize.width / 2f +
                                (country.markerAnchor.x - nativeMap.width / 2f) * totalScale,
                        y = canvasSize.height / 2f +
                                (country.markerAnchor.y - nativeMap.height / 2f) * totalScale
                    )
                    val manualOffset = smallCountryMarkerOffsetDp(country.id)
                    country.id to (
                            screenCenter + Offset(
                                manualOffset.x * displayDensity,
                                manualOffset.y * displayDensity
                            )
                            )
                }
                .toList()

            resolveSmallMarkerZoomOffsets(
                desiredCenters = desiredCenters,
                minDistancePx = 13f * displayDensity,
                maxShiftPx = 5f * displayDensity
            )
        }
    }
    val hitCountries = remember(nativeMap) {
        nativeMap.countries.sortedBy { country ->
            country.floatBounds.width * country.floatBounds.height
        }
    }

    fun mapPointToScreen(point: Offset, totalScale: Float): Offset = Offset(
        x = canvasSize.width / 2f + pan.x +
                (point.x - nativeMap.width / 2f) * totalScale,
        y = canvasSize.height / 2f + pan.y +
                (point.y - nativeMap.height / 2f) * totalScale
    )

    fun markerScreenCenter(country: NativeCountryShape, totalScale: Float): Offset {
        val base = mapPointToScreen(country.markerAnchor, totalScale)
        val offsetDp = smallCountryMarkerOffsetDp(country.id)
        val collisionOffset = markerCollisionOffsets[country.id] ?: Offset.Zero
        return base +
                Offset(offsetDp.x * displayDensity, offsetDp.y * displayDensity) +
                collisionOffset
    }

    fun zoomAt(factor: Float, focus: Offset) {
        if (canvasSize.width == 0 || canvasSize.height == 0) return
        val oldZoom = zoom
        val newZoom = (oldZoom * factor).coerceIn(1f, 20f)
        if (newZoom == oldZoom) return
        val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
        val ratio = newZoom / oldZoom
        val nextPan = focus - center - (focus - center - pan) * ratio
        zoom = newZoom
        pan = clampNativeMapPan(nextPan, newZoom, canvasSize, nativeMap)
    }

    val scaleDetector = remember(nativeMap) {
        ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    zoomAt(
                        factor = detector.scaleFactor,
                        focus = Offset(detector.focusX, detector.focusY)
                    )
                    return true
                }
            }
        )
    }

    val gestureDetector = remember(nativeMap) {
        GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(event: MotionEvent): Boolean = true

                override fun onScroll(
                    firstEvent: MotionEvent?,
                    currentEvent: MotionEvent,
                    distanceX: Float,
                    distanceY: Float
                ): Boolean {
                    if (scaleDetector.isInProgress) return true
                    pan = clampNativeMapPan(
                        pan - Offset(distanceX, distanceY),
                        zoom,
                        canvasSize,
                        nativeMap
                    )
                    return true
                }

                override fun onSingleTapUp(event: MotionEvent): Boolean {
                    if (canvasSize.width == 0 || canvasSize.height == 0) return false

                    val baseScale = min(
                        canvasSize.width / nativeMap.width,
                        canvasSize.height / nativeMap.height
                    )
                    val totalScale = baseScale * zoom

                    // Small-country markers get a generous invisible tap target.
                    val markerHitRadiusPx = 20f * displayDensity
                    val markerCountry = markerCountries
                        .asSequence()
                        .filter {
                            shouldShowSmallCountryMarker(
                                it,
                                totalScale,
                                displayDensity
                            )
                        }
                        .map { country ->
                            country to markerScreenCenter(country, totalScale)
                        }
                        .filter { (_, center) ->
                            val dx = event.x - center.x
                            val dy = event.y - center.y
                            dx * dx + dy * dy <= markerHitRadiusPx * markerHitRadiusPx
                        }
                        .minByOrNull { (_, center) ->
                            val dx = event.x - center.x
                            val dy = event.y - center.y
                            dx * dx + dy * dy
                        }
                        ?.first

                    if (markerCountry != null) {
                        currentCountryCallback.value(
                            markerCountry.id,
                            markerCountry.name,
                            markerCountry.flag
                        )
                        return true
                    }

                    val mapX = (event.x - canvasSize.width / 2f - pan.x) / totalScale +
                            nativeMap.width / 2f
                    val mapY = (event.y - canvasSize.height / 2f - pan.y) / totalScale +
                            nativeMap.height / 2f
                    val country = hitCountries.firstOrNull {
                        countryContainsPoint(it, mapX, mapY)
                    } ?: return true

                    currentCountryCallback.value(country.id, country.name, country.flag)
                    return true
                }

                override fun onDoubleTap(event: MotionEvent): Boolean {
                    zoomAt(2f, Offset(event.x, event.y))
                    return true
                }
            }
        )
    }

    Canvas(
        modifier = modifier
            .background(mapCanvasBackground)
            .clipToBounds()
            .onSizeChanged {
                canvasSize = it
                pan = clampNativeMapPan(pan, zoom, it, nativeMap)
            }
            .pointerInteropFilter { event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    isInteracting = true
                    currentInteractionCallback.value(true)
                }
                if (event.actionMasked == MotionEvent.ACTION_SCROLL) {
                    val scroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
                    zoomAt(
                        factor = if (scroll >= 0f) 1.25f else 0.80f,
                        focus = Offset(event.x, event.y)
                    )
                } else {
                    scaleDetector.onTouchEvent(event)
                    gestureDetector.onTouchEvent(event)
                }
                if (
                    event.actionMasked == MotionEvent.ACTION_UP ||
                    event.actionMasked == MotionEvent.ACTION_CANCEL
                ) {
                    isInteracting = false
                    currentInteractionCallback.value(false)
                }
                true
            }
    ) {
        val baseScale = min(size.width / nativeMap.width, size.height / nativeMap.height)
        val totalScale = baseScale * zoom
        val translateX = size.width / 2f + pan.x - nativeMap.width * totalScale / 2f
        val translateY = size.height / 2f + pan.y - nativeMap.height * totalScale / 2f

        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            nativeCanvas.save()
            nativeCanvas.translate(translateX, translateY)
            nativeCanvas.scale(totalScale, totalScale)
            borderPaint.strokeWidth = 1.25f / totalScale
            graticulePaint.strokeWidth = .68f / totalScale
            selectedPaint.strokeWidth = 3.2f / totalScale

            if (!isInteracting) {
                graticulePaths.forEach { path ->
                    nativeCanvas.drawPath(path, graticulePaint)
                }
            }
            nativeMap.countries.forEach { country ->
                fillPaint.color = passportStrengthColor(
                    score = scoresByCountry[country.id],
                    minScore = minScore,
                    maxScore = maxScore
                ).toArgb()
                nativeCanvas.drawPath(country.path, fillPaint)
                nativeCanvas.drawPath(country.path, borderPaint)
            }
            nativeMap.countries.firstOrNull { it.id == selectedPassportIso }?.let { selected ->
                nativeCanvas.drawPath(selected.path, selectedPaint)
            }
            nativeCanvas.restore()
        }

        val markerSizeProgress = ((zoom - 1f) / 9f).coerceIn(0f, 1f)
        val markerRadiusDp = 1.45f + (4.2f - 1.45f) * markerSizeProgress
        val markerHaloRadiusDp = 2.25f + (6.0f - 2.25f) * markerSizeProgress
        val markerRadius = markerRadiusDp.dp.toPx()
        val markerHaloRadius = markerHaloRadiusDp.dp.toPx()
        val markerLeaderWidth = 1.dp.toPx()
        val selectedMarkerWidth = 1.6.dp.toPx()

        markerCountries.forEach { country ->
            if (!shouldShowSmallCountryMarker(country, totalScale, displayDensity)) {
                return@forEach
            }

            val actualCenter = mapPointToScreen(country.markerAnchor, totalScale)
            val markerCenter = markerScreenCenter(country, totalScale)
            val dx = markerCenter.x - actualCenter.x
            val dy = markerCenter.y - actualCenter.y
            val leaderThreshold = 7.dp.toPx()

            if (dx * dx + dy * dy > leaderThreshold * leaderThreshold) {
                drawLine(
                    color = mapMarkerLeader.copy(alpha = if (darkTheme) .34f else .28f),
                    start = actualCenter,
                    end = markerCenter,
                    strokeWidth = markerLeaderWidth,
                    cap = StrokeCap.Round
                )
            }

            drawCircle(
                color = mapMarkerHalo.copy(alpha = .78f + .18f * markerSizeProgress),
                radius = markerHaloRadius,
                center = markerCenter
            )

            val markerColor = passportStrengthColor(
                score = scoresByCountry[country.id],
                minScore = minScore,
                maxScore = maxScore
            )

            val markerVisualColor = if (country.id == selectedPassportIso) {
                markerColor
            } else {
                markerColor.copy(alpha = .68f + .32f * markerSizeProgress)
            }

            drawCircle(
                color = markerVisualColor,
                radius = markerRadius,
                center = markerCenter
            )

            if (country.id == selectedPassportIso) {
                drawCircle(
                    color = mapMarkerOutline,
                    radius = markerHaloRadius + 1.dp.toPx(),
                    center = markerCenter,
                    style = Stroke(width = selectedMarkerWidth)
                )
            }
        }
    }
}

@Composable
internal fun PassportStrengthMapCard(
    nativeMap: NativeMapData?,
    ranking: List<PassportMobility>,
    selectedPassportIso: Int,
    onCountrySelected: (Int, String, String) -> Unit,
    onInteractionChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scoresByCountry = remember(ranking) {
        ranking.associate { it.passport.isoNumeric to it.score }
    }
    val minScore = remember(ranking) { ranking.minOfOrNull { it.score } ?: 0 }
    val maxScore = remember(ranking) { ranking.maxOfOrNull { it.score } ?: 0 }

    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val mapBackground = if (darkTheme) Color(0xFF111B23) else Color(0xFFEEF5FA)
    val cardShape = RoundedCornerShape(
        topStart = 30.dp,
        topEnd = 30.dp,
        bottomStart = 26.dp,
        bottomEnd = 26.dp
    )
    val legendColor = borderlyControlSurfaceColor()
    val legendRimColor = borderlyControlRimColor()

    Box(modifier = modifier.fillMaxWidth()) {
        // Карта занимает всю карточку, включая нижние закругления.
        val mapAspectRatio = 1.03f

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .background(MaterialTheme.colorScheme.background)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = mapBackground,
            shape = cardShape,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = .72f)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(mapAspectRatio)
                    .clip(cardShape)
            ) {
                if (nativeMap == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(mapBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.map_unavailable),
                            color = borderlySecondaryContentColor(),
                            fontSize = 12.sp
                        )
                    }
                } else {
                    InteractivePassportStrengthMap(
                        nativeMap = nativeMap,
                        scoresByCountry = scoresByCountry,
                        minScore = minScore,
                        maxScore = maxScore,
                        selectedPassportIso = selectedPassportIso,
                        onCountrySelected = onCountrySelected,
                        onInteractionChanged = onInteractionChanged,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Пилюля содержит только шкалу «Сильнее — Слабее».
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                        .borderlyAdaptivePillRim(
                            rimColor = legendRimColor,
                            solidFraction = 0.13f,
                            fadeFraction = 0.19f
                        ),
                    color = legendColor,
                    shape = RoundedCornerShape(50),
                    border = null
                ) {
                    PassportStrengthLegend(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp)
                    )
                }

            }
        }
    }
}

@Composable
internal fun PassportStrengthLegend(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.stronger),
            color = borderlySecondaryContentColor(),
            fontSize = 10.sp
        )

        Row(
            modifier = Modifier
                .padding(horizontal = 9.dp)
                .weight(1f)
                .height(9.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            listOf(
                StrengthVeryHigh,
                StrengthHigh,
                StrengthMedium,
                StrengthLow,
                StrengthVeryLow
            ).forEach { color ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(color)
                )
            }
        }

        Text(
            text = stringResource(R.string.weaker),
            color = borderlySecondaryContentColor(),
            fontSize = 10.sp
        )
    }
}

@Composable
internal fun RankingSortToggle(
    selected: RankingSortOrder,
    onSelected: (RankingSortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    val toggleColor = borderlyControlSurfaceColor()
    val toggleRimColor = borderlyControlRimColor()
    val selectedColor = borderlySelectedControlColor()
    val selectedContentColor = borderlySelectedContentColor()

    BoxWithConstraints(
        modifier = modifier
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
        val options = RankingSortOrder.entries
        // maxWidth already represents the area inside padding(3.dp), so using
        // it directly keeps the active pill equally inset on every edge.
        val segmentWidth = maxWidth / options.size
        // One pill that glides between options: the active state physically
        // moves there. Critically damped spring = fluid without bouncing.
        val indicatorX by animateDpAsState(
            targetValue = segmentWidth * options.indexOf(selected),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "rankingSortIndicator"
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorX)
                .width(segmentWidth)
                .fillMaxHeight()
                .background(selectedColor, RoundedCornerShape(50))
        )

        Row(modifier = Modifier.fillMaxSize()) {
            options.forEach { option ->
                val isSelected = option == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .borderlyPressable { onSelected(option) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option.localizedTitle(),
                        color = if (isSelected) {
                            selectedContentColor
                        } else {
                            borderlySecondaryContentColor()
                        },
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
internal fun RankingDetailRow(
    label: String,
    value: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = borderlySecondaryContentColor(),
            fontSize = 13.sp
        )
        Text(
            text = value.toString(),
            color = borderlyPrimaryContentColor(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
internal fun PassportRankingCountryDialog(
    countryName: String,
    countryFlag: String,
    mobility: PassportMobility?,
    rank: Int?,
    minScore: Int,
    maxScore: Int,
    onDismiss: () -> Unit,
    onShowAvailableCountries: (() -> Unit)? = null
) {
    val dialogColor = borderlyOpaqueControlSurfaceColor()
    val dialogInnerColor = borderlyOpaqueControlSurfaceColor()
    val closeButtonColor = borderlyMutedControlColor()
    val closeButtonTextColor = borderlyPrimaryContentColor()
    val dialogRimColor = borderlyControlRimColor()
    val selectedButtonColor = borderlySelectedControlColor()
    val selectedButtonTextColor = borderlySelectedContentColor()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .borderlyRoundedRectRim(
                    rimColor = dialogRimColor,
                    cornerRadius = 30.dp
                ),
            color = dialogColor,
            shape = RoundedCornerShape(30.dp),
            border = null
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = countryFlag, fontSize = 34.sp)
                    Column(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .weight(1f)
                    ) {
                        Text(
                            text = countryName,
                            color = borderlyPrimaryContentColor(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2
                        )
                        if (rank != null) {
                            Text(
                                text = stringResource(R.string.world_rank, rank),
                                modifier = Modifier.padding(top = 2.dp),
                                color = borderlySecondaryContentColor(),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(
                                passportStrengthColor(
                                    score = mobility?.score,
                                    minScore = minScore,
                                    maxScore = maxScore
                                ),
                                CircleShape
                            )
                    )
                }

                if (mobility == null) {
                    Text(
                        text = stringResource(R.string.ranking_no_data),
                        modifier = Modifier.padding(top = 20.dp),
                        color = borderlySecondaryContentColor(),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                } else {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp)
                            .borderlyRoundedRectRim(
                                rimColor = dialogRimColor,
                                cornerRadius = 32.dp
                            ),
                        color = dialogInnerColor,
                        shape = RoundedCornerShape(32.dp),
                        border = null
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = mobility.score.toString(),
                                color = borderlyPrimaryContentColor(),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = " " + stringResource(R.string.visa_free_metric).lowercase(),
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                                color = borderlySecondaryContentColor(),
                                fontSize = 13.sp
                            )
                        }
                    }

                    val visaFree = mobility.counts[VisaType.VISA_FREE] ?: 0
                    val freedom = mobility.counts[VisaType.FREEDOM] ?: 0

                    Text(
                        text = stringResource(R.string.visa_free_and_freedom, visaFree, freedom),
                        modifier = Modifier.padding(top = 8.dp),
                        color = borderlySecondaryContentColor(),
                        fontSize = 11.sp
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(top = 15.dp, bottom = 3.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    RankingDetailRow(VisaType.ETA.localizedTitle(), mobility.counts[VisaType.ETA] ?: 0)
                    RankingDetailRow(
                        VisaType.VISA_ON_ARRIVAL.localizedTitle(),
                        mobility.counts[VisaType.VISA_ON_ARRIVAL] ?: 0
                    )
                    RankingDetailRow(VisaType.E_VISA.localizedTitle(), mobility.counts[VisaType.E_VISA] ?: 0)
                    RankingDetailRow(
                        VisaType.VISA_REQUIRED.localizedTitle(),
                        mobility.counts[VisaType.VISA_REQUIRED] ?: 0
                    )
                    RankingDetailRow(
                        VisaType.ENTRY_RESTRICTED.localizedTitle(),
                        mobility.counts[VisaType.ENTRY_RESTRICTED] ?: 0
                    )
                    RankingDetailRow(
                        VisaType.SPECIAL_PERMIT.localizedTitle(),
                        mobility.counts[VisaType.SPECIAL_PERMIT] ?: 0
                    )
                    RankingDetailRow(
                        VisaType.MIXED_REQUIREMENTS.localizedTitle(),
                        mobility.counts[VisaType.MIXED_REQUIREMENTS] ?: 0
                    )
                }

                onShowAvailableCountries?.let { onShow ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp)
                            .height(48.dp)
                            .background(selectedButtonColor, RoundedCornerShape(50))
                            .borderlyAdaptivePillRim(
                                rimColor = dialogRimColor,
                                solidFraction = 0.13f,
                                fadeFraction = 0.19f
                            )
                            .clip(RoundedCornerShape(50))
                            .borderlyPressable(onShow),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.show_accessible_countries),
                            color = selectedButtonTextColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (onShowAvailableCountries == null) 18.dp else 9.dp)
                        .height(48.dp)
                        .background(closeButtonColor, RoundedCornerShape(50))
                        .borderlyAdaptivePillRim(
                            rimColor = dialogRimColor,
                            solidFraction = 0.13f,
                            fadeFraction = 0.19f
                        )
                        .clip(RoundedCornerShape(50))
                        .borderlyPressable(onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.close),
                        color = closeButtonTextColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
internal fun PassportRankingScreen(
    nativeMap: NativeMapData?,
    visaDatabase: VisaDatabase,
    selectedPassport: Passport,
    ranking: List<PassportMobility>,
    rankByPassport: Map<Int, Int>,
    hazeState: HazeState,
    onHeaderPassportClick: () -> Unit,
    onPassportClick: (Passport) -> Unit
) {
    val displayLocale = LocalConfiguration.current.locales[0]
    var query by rememberSaveable { mutableStateOf("") }
    var regionFilter by rememberSaveable { mutableStateOf(PassportRegionFilter.ALL) }
    var sortOrder by rememberSaveable { mutableStateOf(RankingSortOrder.STRONGEST_FIRST) }
    var isStrengthMapInteracting by remember { mutableStateOf(false) }
    var selectedMapCountryIso by rememberSaveable { mutableStateOf<Int?>(null) }
    var isSearchFocused by remember { mutableStateOf(false) }
    var searchBottomInWindow by remember { mutableFloatStateOf(0f) }

    val rankingListState = rememberLazyListState()
    val scrollToTopHazeState = rememberHazeState()
    val scrollToTopScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val rootView = LocalView.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val searchKeyboardGapPx = with(density) { 12.dp.toPx() }

    // The keyboard overlays this screen instead of reliably resizing the LazyColumn.
    // Track the real keyboard top and the real search-field bottom in window
    // coordinates. While the IME moves, scroll only the overlapping amount.
    LaunchedEffect(
        isSearchFocused,
        imeBottomPx,
        searchBottomInWindow,
        rootView.height
    ) {
        if (
            isSearchFocused &&
            imeBottomPx > 0 &&
            searchBottomInWindow > 0f &&
            rootView.height > 0
        ) {
            val keyboardTopInWindow =
                rootView.height.toFloat() - imeBottomPx.toFloat()
            val safeSearchBottom =
                keyboardTopInWindow - searchKeyboardGapPx
            val overlapPx =
                searchBottomInWindow - safeSearchBottom

            if (overlapPx > 1f) {
                rankingListState.scrollBy(overlapPx)
            }
        }
    }

    // Ranking and dense ranks arrive precomputed from the home container,
    // so opening this tab no longer rebuilds ~200 passport mobilities on
    // the main thread.
    val visibleRanking = remember(
        ranking,
        query,
        regionFilter,
        sortOrder,
        displayLocale
    ) {
        val filtered = ranking.filter { mobility ->
            matchesCountrySearch(
                displayName = localizedCountryName(
                    mobility.passport.isoNumeric,
                    mobility.passport.name,
                    displayLocale
                ),
                isoNumeric = mobility.passport.isoNumeric,
                query = query
            ) &&
                (regionFilter.region == null || mobility.passport.region == regionFilter.region)
        }
        val localizedName: (PassportMobility) -> String = { mobility ->
            localizedCountryName(
                mobility.passport.isoNumeric,
                mobility.passport.name,
                displayLocale
            )
        }
        when (sortOrder) {
            RankingSortOrder.STRONGEST_FIRST -> filtered.sortedWith(
                compareByDescending<PassportMobility> { it.score }
                    .thenBy(localizedName)
            )
            RankingSortOrder.WEAKEST_FIRST -> filtered.sortedWith(
                compareBy<PassportMobility> { it.score }
                    .thenBy(localizedName)
            )
        }
    }
    val minScore = remember(ranking) { ranking.minOfOrNull { it.score } ?: 0 }
    val maxScore = remember(ranking) { ranking.maxOfOrNull { it.score } ?: 0 }
    val selectedMapCountry = remember(nativeMap, selectedMapCountryIso) {
        selectedMapCountryIso?.let { iso ->
            nativeMap?.countries
                ?.firstOrNull { it.id == iso }
                ?.let { country -> Triple(country.id, country.name, country.flag) }
        }
    }
    val highlightedPassportIso = selectedMapCountryIso ?: selectedPassport.isoNumeric

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

    // Exactly the same glass recipe as the 48.dp + / - controls on the map.
    val mapControlHazeStyle = HazeStyle(
        backgroundColor = MaterialTheme.colorScheme.surface,
        tint = HazeTint(roundControlColor),
        blurRadius = 3.dp,
        noiseFactor = 0f,
        fallbackTint = HazeTint(roundControlColor)
    )
    val glassRimColor = borderlyControlRimColor()
    val mapControlContentColor = borderlyPrimaryContentColor()

    val showScrollToTop =
        rankingListState.firstVisibleItemIndex > 1 ||
                rankingListState.firstVisibleItemScrollOffset > 300

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = rankingListState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = scrollToTopHazeState),
            contentPadding = PaddingValues(bottom = 132.dp),
            userScrollEnabled = !isStrengthMapInteracting,
            overscrollEffect = null
        ) {
        item {
            Header(
                selectedPassport = selectedPassport,
                onPassportClick = onHeaderPassportClick
            )
        }

        item {
            PassportStrengthMapCard(
                nativeMap = nativeMap,
                ranking = ranking,
                selectedPassportIso = highlightedPassportIso,
                onCountrySelected = { iso, name, flag ->
                    selectedMapCountryIso = iso
                },
                onInteractionChanged = { isStrengthMapInteracting = it },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 14.dp, end = 20.dp)
                    .height(48.dp)
                    .borderlyAdaptivePillRim(
                        rimColor = glassRimColor,
                        solidFraction = 0.13f,
                        fadeFraction = 0.19f
                    )
                    .background(glassControlColor, RoundedCornerShape(50))
                    .onGloballyPositioned { coordinates ->
                        searchBottomInWindow =
                            coordinates.boundsInWindow().bottom
                    }
                    .onFocusChanged { focusState ->
                        isSearchFocused = focusState.isFocused
                    }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = borderlySecondaryContentColor(),
                    modifier = Modifier.align(Alignment.CenterStart)
                )

                BasicTextField(
                    value = query,
                    onValueChange = { newValue: String -> query = newValue },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 52.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = borderlyPrimaryContentColor(),
                        lineHeight = 20.sp
                    ),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (query.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.passport_name),
                                    color = borderlySecondaryContentColor(),
                                    maxLines = 1,
                                    lineHeight = 20.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }

        item {
            RankingSortToggle(
                selected = sortOrder,
                onSelected = { sortOrder = it },
                modifier = Modifier.padding(start = 20.dp, top = 11.dp, end = 20.dp)
            )
        }

        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 11.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                items(PassportRegionFilter.entries, key = { it.name }) { option ->
                    PassportRegionChip(
                        filter = option,
                        selected = option == regionFilter,
                        compact = false,
                        onClick = { regionFilter = option }
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.world_ranking),
                    color = borderlyPrimaryContentColor(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.found_count, visibleRanking.size),
                    color = borderlySecondaryContentColor(),
                    fontSize = 11.sp
                )
            }
        }

        items(visibleRanking, key = { it.passport.isoNumeric }) { mobility ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                PassportRankingRow(
                    rank = rankByPassport[mobility.passport.isoNumeric] ?: 0,
                    mobility = mobility,
                    selected = mobility.passport.isoNumeric == selectedPassport.isoNumeric,
                    onClick = {
                        selectedMapCountryIso = mobility.passport.isoNumeric
                    }
                )
            }
        }

        item {
            Text(
                text = stringResource(
                    R.string.ranking_methodology,
                    visaDatabase.destinationCount
                ),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                color = borderlySecondaryContentColor(),
                fontSize = 11.sp,
                lineHeight = 16.sp
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
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .hazeEffect(
                        state = scrollToTopHazeState,
                        style = mapControlHazeStyle
                    ) {
                        inputScale = HazeInputScale.Auto
                        blurEnabled = !lowEndDevice
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
                            val base = roundControlRimColor
                            val transparent = base.copy(alpha = 0f)
                            val stroke = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Butt)

                            fun stop(angle: Float): Float = (angle / 360f).coerceIn(0f, 1f)

                            rotate(
                                degrees = solidStart - longFadeSweep,
                                pivot = center
                            ) {
                                val fadeInBrush = Brush.sweepGradient(
                                    0f to transparent,
                                    stop(longFadeSweep * 0.20f) to base.copy(alpha = base.alpha * 0.05f),
                                    stop(longFadeSweep * 0.40f) to base.copy(alpha = base.alpha * 0.18f),
                                    stop(longFadeSweep * 0.60f) to base.copy(alpha = base.alpha * 0.42f),
                                    stop(longFadeSweep * 0.80f) to base.copy(alpha = base.alpha * 0.72f),
                                    stop(longFadeSweep) to base,
                                    1f to base,
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
                                color = base,
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
                                    0f to base,
                                    stop(longFadeSweep * 0.20f) to base.copy(alpha = base.alpha * 0.72f),
                                    stop(longFadeSweep * 0.40f) to base.copy(alpha = base.alpha * 0.42f),
                                    stop(longFadeSweep * 0.60f) to base.copy(alpha = base.alpha * 0.18f),
                                    stop(longFadeSweep * 0.80f) to base.copy(alpha = base.alpha * 0.05f),
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
                    .borderlyPressable {
                        scrollToTopScope.launch {
                            rankingListState.scrollToItem(0)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.back_to_top),
                    tint = mapControlContentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }

    selectedMapCountry?.let { selected ->
        val mobility = ranking.firstOrNull { it.passport.isoNumeric == selected.first }
        PassportRankingCountryDialog(
            countryName = localizedCountryName(selected.first, selected.second),
            countryFlag = selected.third,
            mobility = mobility,
            rank = mobility?.let { rankByPassport[it.passport.isoNumeric] },
            minScore = minScore,
            maxScore = maxScore,
            onDismiss = { selectedMapCountryIso = null },
            onShowAvailableCountries = mobility?.let { selectedMobility ->
                {
                    selectedMapCountryIso = null
                    onPassportClick(selectedMobility.passport)
                }
            }
        )
    }
}

@Composable
internal fun PassportRankingRow(
    rank: Int,
    mobility: PassportMobility,
    selected: Boolean,
    onClick: () -> Unit
) {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val rowColor = if (darkTheme) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
    } else {
        Color(0xFFF9F9F9).copy(alpha = 0.90f)
    }
    val rowRimColor = if (darkTheme) {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.92f)
    } else {
        Color.White
    }

    // Selection is a smooth state change, not a swap: the row and its
    // content ease to the selected palette together in ~180ms.
    val rowSurfaceColor by animateColorAsState(
        targetValue = if (selected) borderlySelectedControlColor() else rowColor,
        animationSpec = tween(180, easing = BorderlyStrongEaseOut),
        label = "rankingRowSurface"
    )
    val rowBorderColor by animateColorAsState(
        targetValue = if (selected) borderlySelectedControlColor() else Color.Transparent,
        animationSpec = tween(180, easing = BorderlyStrongEaseOut),
        label = "rankingRowBorder"
    )
    val nameTextColor by animateColorAsState(
        targetValue = if (selected) Color.White else borderlyPrimaryContentColor(),
        animationSpec = tween(180, easing = BorderlyStrongEaseOut),
        label = "rankingRowName"
    )
    val subtitleTextColor by animateColorAsState(
        targetValue = if (selected) {
            Color.White.copy(alpha = .72f)
        } else {
            borderlySecondaryContentColor()
        },
        animationSpec = tween(180, easing = BorderlyStrongEaseOut),
        label = "rankingRowSubtitle"
    )
    val scoreTextColor by animateColorAsState(
        targetValue = if (selected) Color.White else borderlyPrimaryContentColor(),
        animationSpec = tween(180, easing = BorderlyStrongEaseOut),
        label = "rankingRowScore"
    )
    val badgeBackground by animateColorAsState(
        targetValue = when {
            selected -> Color.White
            rank <= 3 -> borderlySelectedControlColor()
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(180, easing = BorderlyStrongEaseOut),
        label = "rankingRowBadge"
    )
    val badgeTextColor by animateColorAsState(
        targetValue = when {
            selected -> borderlySelectedControlColor()
            rank <= 3 -> Color.White
            else -> borderlySecondaryContentColor()
        },
        animationSpec = tween(180, easing = BorderlyStrongEaseOut),
        label = "rankingRowBadgeText"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (selected) {
                    Modifier
                } else {
                    Modifier.borderlyRoundedRectRim(
                        rimColor = rowRimColor,
                        cornerRadius = 26.dp
                    )
                }
            )
            .borderlyPressable(onClick),
        color = rowSurfaceColor,
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, rowBorderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        color = badgeBackground,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rank.toString(),
                    color = badgeTextColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = mobility.passport.flag,
                modifier = Modifier.padding(start = 11.dp),
                fontSize = 27.sp
            )
            Column(
                modifier = Modifier
                    .padding(start = 11.dp)
                    .weight(1f)
            ) {
                Text(
                    text = mobility.passport.localizedName(),
                    color = nameTextColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val visaFreeCount = mobility.counts[VisaType.VISA_FREE] ?: 0
                val freedomCount = mobility.counts[VisaType.FREEDOM] ?: 0
                Text(
                    text = stringResource(
                        R.string.ranking_row_subtitle,
                        mobility.passport.region.localizedTitle(),
                        visaFreeCount,
                        freedomCount
                    ),
                    modifier = Modifier.padding(top = 2.dp),
                    color = subtitleTextColor,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = mobility.score.toString(),
                    color = scoreTextColor,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.visa_free_metric).lowercase(),
                    color = subtitleTextColor,
                    fontSize = 10.sp
                )
            }
        }
    }
}
