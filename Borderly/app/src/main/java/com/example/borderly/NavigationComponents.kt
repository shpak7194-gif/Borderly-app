package com.example.borderly

import android.graphics.Paint
import android.graphics.Path as AndroidPath
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
internal fun BottomNavigation(
    selectedTab: AppTab,
    hazeState: HazeState,
    onTabClick: (AppTab) -> Unit
) {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val lowEndDevice = LocalBorderlyLowEndMode.current

    // Same surface + rim design code as the region cards.
    val navigationSurface = if (lowEndDevice) {
        if (darkTheme) Color(0xFF141A20) else Color(0xFFF1F3F6)
    } else {
        borderlyNavigationSurfaceColor()
    }
    // Более светлый нейтральный овал активного элемента только в тёмной теме.
    val selectedDarkNavigationColor = Color(0xFF36414B)
    val selectedNavigationSurface = if (lowEndDevice) {
        if (darkTheme) selectedDarkNavigationColor else Color(0xFFDCE0E5)
    } else {
        if (darkTheme) selectedDarkNavigationColor.copy(alpha = 0.52f) else Color(0xFFDCE0E5).copy(alpha = 0.70f)
    }
    val navigationContentColor = if (darkTheme) Color(0xFFE7ECF1) else Color(0xFF1E2A36)
    val navigationSecondaryContentColor = if (darkTheme) Color(0xFF9EA8B2) else Color(0xFF68727D)
    val navigationRimColor = borderlyControlRimColor()
    val navigationShadowColor = if (darkTheme) {
        Color.White.copy(alpha = 0.03f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }
    // Настоящий backdrop blur: контент под панелью берётся из hazeSource в MainActivity.
    // Полупрозрачная тонировка сохраняет текущий визуальный язык Borderly.
    val navigationHazeStyle = HazeStyle(
        backgroundColor = MaterialTheme.colorScheme.surface,
        tint = HazeTint(
            color = if (darkTheme) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.34f)
            } else {
                Color.White.copy(alpha = 0.34f)
            }
        ),
        blurRadius = 4.dp,
        noiseFactor = 0f,
        fallbackTint = HazeTint(navigationSurface)
    )

    // Separate glass layer for the selected tab. It is drawn above the main
    // capsule and uses #BDBDBD only in the light theme.
    val selectedTabHazeStyle = HazeStyle(
        backgroundColor = if (darkTheme) {
            selectedDarkNavigationColor
        } else {
            Color(0xFFDCE0E5)
        },
        tint = HazeTint(
            color = if (darkTheme) {
                selectedDarkNavigationColor.copy(alpha = 0.52f)
            } else {
                Color(0xFFDCE0E5).copy(alpha = 0.70f)
            }
        ),
        blurRadius = 3.dp,
        noiseFactor = 0f,
        fallbackTint = HazeTint(
            if (darkTheme) {
                selectedDarkNavigationColor.copy(alpha = 0.52f)
            } else {
                Color(0xFFDCE0E5).copy(alpha = 0.70f)
            }
        )
    )

    // Important: no wrapping Surface here.
    // A Surface clips child rendering to its own bounds, which made the
    // outer navigation shadows effectively invisible. Box does not clip,
    // so the soft shadow can render outside the capsule/circle.
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                start = 10.dp,
                top = 8.dp,
                end = 10.dp,
                bottom = 8.dp
            )
            .height(68.dp)
    ) {
        val settingsSize = 68.dp
        val controlGap = 10.dp
        val selectedInset = 4.dp
        val selectedHeight = 60.dp
        val capsuleWidth = (maxWidth - settingsSize - controlGap).coerceAtLeast(0.dp)
        val capsuleInnerWidth = (capsuleWidth - selectedInset * 2).coerceAtLeast(0.dp)
        val segmentWidth = capsuleInnerWidth / 3f
        val density = LocalDensity.current
        val capsuleTabs = remember {
            listOf(AppTab.MAP, AppTab.COMPARE, AppTab.RANKING)
        }
        val selectedCapsuleIndex = when (selectedTab) {
            AppTab.MAP -> 0
            AppTab.COMPARE -> 1
            AppTab.RANKING -> 2
            AppTab.SETTINGS -> null
        }
        val dragStartIndex = selectedCapsuleIndex ?: 0
        var isIndicatorDragging by remember { mutableStateOf(false) }
        var draggedIndicatorX by remember { mutableStateOf(selectedInset) }
        var pendingDraggedTab by remember { mutableStateOf<AppTab?>(null) }

        // Settings uses a separate selected layer. While it is active, the
        // capsule indicator rests invisibly at Ranking, so returning from
        // Settings always starts at the capsule's right edge.
        val indicatorTargetX = when (selectedTab) {
            AppTab.MAP -> selectedInset
            AppTab.COMPARE -> selectedInset + segmentWidth
            AppTab.RANKING -> selectedInset + segmentWidth * 2
            AppTab.SETTINGS -> selectedInset + segmentWidth * 2
        }
        val pendingIndicatorTargetX = when (pendingDraggedTab) {
            AppTab.MAP -> selectedInset
            AppTab.COMPARE -> selectedInset + segmentWidth
            AppTab.RANKING -> selectedInset + segmentWidth * 2
            else -> null
        }
        val displayedIndicatorTargetX = when {
            isIndicatorDragging -> draggedIndicatorX
            pendingIndicatorTargetX != null -> pendingIndicatorTargetX
            else -> indicatorTargetX
        }
        val indicatorX by animateDpAsState(
            targetValue = displayedIndicatorTargetX,
            animationSpec = if (isIndicatorDragging) {
                snap()
            } else {
                spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            },
            label = "navigationIndicatorX"
        )
        val draggedCapsuleIndex = if (isIndicatorDragging && segmentWidth > 0.dp) {
            ((draggedIndicatorX - selectedInset).value / segmentWidth.value)
                .roundToInt()
                .coerceIn(0, capsuleTabs.lastIndex)
        } else {
            null
        }
        val visuallySelectedTab = draggedCapsuleIndex
            ?.let(capsuleTabs::get)
            ?: pendingDraggedTab
            ?: selectedTab
        LaunchedEffect(selectedTab, pendingDraggedTab) {
            if (pendingDraggedTab == selectedTab) {
                pendingDraggedTab = null
            }
        }
        val settingsIconTint by animateColorAsState(
            targetValue = if (visuallySelectedTab == AppTab.SETTINGS) {
                navigationContentColor
            } else {
                navigationSecondaryContentColor
            },
            animationSpec = tween(200, easing = BorderlyStrongEaseOut),
            label = "settingsIconTint"
        )
        val capsuleIndicatorAlpha by animateFloatAsState(
            targetValue = if (selectedTab == AppTab.SETTINGS) 0f else 1f,
            animationSpec = if (selectedTab == AppTab.SETTINGS) {
                tween(
                    durationMillis = 140,
                    delayMillis = 110,
                    easing = BorderlyStrongEaseOut
                )
            } else {
                tween(durationMillis = 160, easing = BorderlyStrongEaseOut)
            },
            label = "capsuleIndicatorAlpha"
        )
        val settingsIndicatorAlpha by animateFloatAsState(
            targetValue = if (selectedTab == AppTab.SETTINGS) 1f else 0f,
            animationSpec = if (selectedTab == AppTab.SETTINGS) {
                tween(
                    durationMillis = 170,
                    delayMillis = 110,
                    easing = BorderlyStrongEaseOut
                )
            } else {
                tween(durationMillis = 120, easing = BorderlyStrongEaseOut)
            },
            label = "settingsIndicatorAlpha"
        )
        val settingsIndicatorScale by animateFloatAsState(
            targetValue = if (selectedTab == AppTab.SETTINGS) 1f else 0.92f,
            animationSpec = tween(
                durationMillis = 180,
                delayMillis = if (selectedTab == AppTab.SETTINGS) 90 else 0,
                easing = BorderlyStrongEaseOut
            ),
            label = "settingsIndicatorScale"
        )
        val capsuleDragModifier = if (selectedCapsuleIndex != null && segmentWidth > 0.dp) {
            Modifier.pointerInput(
                selectedCapsuleIndex,
                selectedInset,
                segmentWidth,
                density
            ) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        pendingDraggedTab = null
                        draggedIndicatorX = selectedInset + segmentWidth * dragStartIndex
                        isIndicatorDragging = true
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        val dragAmountDp = with(density) { dragAmount.toDp() }
                        val rankingStopX = selectedInset + segmentWidth * capsuleTabs.lastIndex
                        draggedIndicatorX = (draggedIndicatorX + dragAmountDp)
                            .coerceIn(selectedInset, rankingStopX)
                    },
                    onDragEnd = {
                        val targetIndex = if (segmentWidth > 0.dp) {
                            ((draggedIndicatorX - selectedInset).value / segmentWidth.value)
                                .roundToInt()
                                .coerceIn(0, capsuleTabs.lastIndex)
                        } else {
                            dragStartIndex
                        }
                        val targetTab = capsuleTabs[targetIndex]
                        pendingDraggedTab = targetTab
                        isIndicatorDragging = false
                        onTabClick(targetTab)
                    },
                    onDragCancel = {
                        pendingDraggedTab = null
                        isIndicatorDragging = false
                    }
                )
            }
        } else {
            Modifier
        }

        // Base glass surfaces and their shadows.
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(controlGap)
        ) {
            Box(
                modifier = Modifier
                    .width(capsuleWidth)
                    .fillMaxHeight()
                    .borderlyNavigationRim(
                        rimColor = navigationRimColor,
                        cornerRadius = 34.dp
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1.012f
                            scaleY = 1.055f
                        }
                        .blur(
                            radius = 6.dp,
                            edgeTreatment = BlurredEdgeTreatment.Unbounded
                        )
                        .background(
                            color = navigationShadowColor,
                            shape = RoundedCornerShape(34.dp)
                        )
                )
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(34.dp))
                        .hazeEffect(
                            state = hazeState,
                            style = navigationHazeStyle
                        ) {
                            inputScale = HazeInputScale.Auto
                            blurEnabled = !lowEndDevice
                        },
                    color = if (lowEndDevice) navigationSurface else Color.Transparent,
                    shape = RoundedCornerShape(34.dp),
                    border = null,
                    shadowElevation = 0.dp
                ) {}
            }

            Box(
                modifier = Modifier
                    .size(settingsSize)
                    .borderlyNavigationRim(
                        rimColor = navigationRimColor,
                        cornerRadius = 34.dp
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1.055f
                            scaleY = 1.055f
                        }
                        .blur(
                            radius = 6.dp,
                            edgeTreatment = BlurredEdgeTreatment.Unbounded
                        )
                        .background(
                            color = navigationShadowColor,
                            shape = CircleShape
                        )
                )
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .hazeEffect(
                            state = hazeState,
                            style = navigationHazeStyle
                        ) {
                            inputScale = HazeInputScale.Auto
                            blurEnabled = !lowEndDevice
                        },
                    color = if (lowEndDevice) navigationSurface else Color.Transparent,
                    shape = CircleShape,
                    border = null,
                    shadowElevation = 0.dp
                ) {}
            }
        }

        // The main selected layer never leaves the capsule. When Settings is
        // opened, it moves only as far as Ranking and fades at the right edge.
        Box(
            modifier = Modifier
                .offset(x = indicatorX, y = selectedInset)
                .width(segmentWidth)
                .height(selectedHeight)
                .zIndex(1f)
                .graphicsLayer { alpha = capsuleIndicatorAlpha }
                .clip(RoundedCornerShape(30.dp))
                .background(
                    if (lowEndDevice) selectedNavigationSurface else Color.Transparent
                )
                .hazeEffect(
                    state = hazeState,
                    style = selectedTabHazeStyle
                ) {
                    inputScale = HazeInputScale.Auto
                    blurEnabled = !lowEndDevice
                }
        )

        // Settings owns a separate circular selected layer. The empty gap
        // between the capsule and this button therefore always stays empty.
        Box(
            modifier = Modifier
                .offset(
                    x = capsuleWidth + controlGap + selectedInset,
                    y = selectedInset
                )
                .size(selectedHeight)
                .zIndex(1f)
                .graphicsLayer {
                    alpha = settingsIndicatorAlpha
                    scaleX = settingsIndicatorScale
                    scaleY = settingsIndicatorScale
                }
                .clip(CircleShape)
                .background(
                    if (lowEndDevice) selectedNavigationSurface else Color.Transparent
                )
                .hazeEffect(
                    state = hazeState,
                    style = selectedTabHazeStyle
                ) {
                    inputScale = HazeInputScale.Auto
                    blurEnabled = !lowEndDevice
                }
        )

        // Interactive content is a separate top layer so the moving glass
        // indicator never covers the icons or labels.
        Row(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(2f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .width(capsuleWidth)
                    .fillMaxHeight()
                    .then(capsuleDragModifier)
                    .padding(selectedInset)
            ) {
                NavigationItem(
                    label = stringResource(R.string.nav_map),
                    icon = Icons.Outlined.Map,
                    selected = visuallySelectedTab == AppTab.MAP,
                    modifier = Modifier.weight(1f),
                    onClick = { onTabClick(AppTab.MAP) }
                )
                NavigationItem(
                    label = stringResource(R.string.nav_compare),
                    icon = Icons.Outlined.Balance,
                    selected = visuallySelectedTab == AppTab.COMPARE,
                    modifier = Modifier.weight(1f),
                    onClick = { onTabClick(AppTab.COMPARE) }
                )
                NavigationItem(
                    label = stringResource(R.string.nav_ranking),
                    icon = Icons.Outlined.EmojiEvents,
                    selected = visuallySelectedTab == AppTab.RANKING,
                    modifier = Modifier.weight(1f),
                    onClick = { onTabClick(AppTab.RANKING) }
                )
            }

            val settingsLabel = stringResource(R.string.nav_settings)
            Box(
                modifier = Modifier
                    .padding(start = controlGap)
                    .size(settingsSize)
                    .borderlyPressable { onTabClick(AppTab.SETTINGS) }
                    .semantics {
                        role = Role.Tab
                        selected = visuallySelectedTab == AppTab.SETTINGS
                        contentDescription = settingsLabel
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = settingsIconTint
                )
            }
        }
    }
}

@Composable
internal fun NavigationItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val contentColor = if (darkTheme) Color(0xFFF8FAFC) else Color(0xFF1E2A36)
    val secondaryContentColor = if (darkTheme) Color(0xFFAAB4BE) else Color(0xFF5E6975)

    // Иконка и подпись плавно перетекают в выбранные цвета вслед за
    // скользящей стеклянной таблеткой.
    val itemContentColor by animateColorAsState(
        targetValue = if (selected) contentColor else secondaryContentColor,
        animationSpec = tween(200, easing = BorderlyStrongEaseOut),
        label = "navigationItemColor"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .borderlyPressable(onClick)
            .semantics {
                role = Role.Tab
                this.selected = selected
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(25.dp),
                tint = itemContentColor
            )

            Text(
                text = label,
                modifier = Modifier.padding(top = 3.dp),
                color = itemContentColor,
                fontSize = 11.sp,
                fontWeight = if (selected) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Medium
                },
                maxLines = 1
            )
        }
    }
}

private fun Modifier.borderlyNavigationRim(
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
        left,
        top,
        left + radius * 2f,
        top + radius * 2f
    )
    val rightTopRect = android.graphics.RectF(
        right - radius * 2f,
        top,
        right,
        top + radius * 2f
    )
    val rightBottomRect = android.graphics.RectF(
        right - radius * 2f,
        bottom - radius * 2f,
        right,
        bottom
    )
    val leftBottomRect = android.graphics.RectF(
        left,
        bottom - radius * 2f,
        left + radius * 2f,
        bottom
    )

    val cornerEndProgress = 0.58f

    val upperPath = AndroidPath().apply {
        moveTo(left, top + radius)
        arcTo(leftTopRect, 180f, 90f, false)
        lineTo(right - radius, top)
        arcTo(
            rightTopRect,
            -90f,
            90f * cornerEndProgress,
            false
        )
    }

    val lowerPath = AndroidPath().apply {
        moveTo(right, bottom - radius)
        arcTo(rightBottomRect, 0f, 90f, false)
        lineTo(left + radius, bottom)
        arcTo(
            leftBottomRect,
            90f,
            90f * cornerEndProgress,
            false
        )
    }

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
        val nativeCanvas = canvas.nativeCanvas

        fun drawRoute(path: AndroidPath) {
            val measure = android.graphics.PathMeasure(path, false)
            val routeLength = measure.length
            if (routeLength <= 0f) return

            val fadeLength = min(
                routeLength * 0.18f,
                70.dp.toPx()
            )
            val solidStart = fadeLength
            val solidEnd = (routeLength - fadeLength)
                .coerceAtLeast(solidStart)
            val fadeSteps = 32

            fun drawSegment(
                from: Float,
                to: Float,
                alpha: Float
            ) {
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
                    nativeCanvas.drawPath(segment, paint)
                }
            }

            for (index in 0 until fadeSteps) {
                val t0 = index.toFloat() / fadeSteps
                val t1 = (index + 1f) / fadeSteps

                drawSegment(
                    from = fadeLength * t0,
                    to = fadeLength * t1,
                    alpha = smoothStep((t0 + t1) / 2f)
                )
            }

            drawSegment(
                from = solidStart,
                to = solidEnd,
                alpha = 1f
            )

            for (index in 0 until fadeSteps) {
                val t0 = index.toFloat() / fadeSteps
                val t1 = (index + 1f) / fadeSteps

                drawSegment(
                    from = solidEnd + fadeLength * t0,
                    to = solidEnd + fadeLength * t1,
                    alpha = smoothStep(1f - (t0 + t1) / 2f)
                )
            }
        }

        drawRoute(upperPath)
        drawRoute(lowerPath)
    }
}

@Composable
internal fun PassportGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = size.minDimension * .075f
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * .08f, size.height * .05f),
            size = Size(size.width * .84f, size.height * .90f),
            cornerRadius = CornerRadius(size.minDimension * .10f),
            style = Stroke(stroke)
        )
        drawCircle(
            color = color,
            radius = size.minDimension * .19f,
            center = Offset(size.width * .50f, size.height * .43f),
            style = Stroke(stroke)
        )
        drawLine(
            color = color,
            start = Offset(size.width * .31f, size.height * .43f),
            end = Offset(size.width * .69f, size.height * .43f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(size.width * .50f, size.height * .24f),
            end = Offset(size.width * .50f, size.height * .62f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(size.width * .34f, size.height * .76f),
            end = Offset(size.width * .66f, size.height * .76f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
internal fun ExpandGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = size.minDimension * .10f
        drawLine(
            color,
            Offset(size.width * .08f, size.height * .38f),
            Offset(size.width * .08f, size.height * .08f),
            stroke,
            StrokeCap.Round
        )
        drawLine(
            color,
            Offset(size.width * .08f, size.height * .08f),
            Offset(size.width * .38f, size.height * .08f),
            stroke,
            StrokeCap.Round
        )
        drawLine(
            color,
            Offset(size.width * .08f, size.height * .08f),
            Offset(size.width * .39f, size.height * .39f),
            stroke,
            StrokeCap.Round
        )
        drawLine(
            color,
            Offset(size.width * .92f, size.height * .62f),
            Offset(size.width * .92f, size.height * .92f),
            stroke,
            StrokeCap.Round
        )
        drawLine(
            color,
            Offset(size.width * .92f, size.height * .92f),
            Offset(size.width * .62f, size.height * .92f),
            stroke,
            StrokeCap.Round
        )
        drawLine(
            color,
            Offset(size.width * .61f, size.height * .61f),
            Offset(size.width * .92f, size.height * .92f),
            stroke,
            StrokeCap.Round
        )
    }
}

internal fun Modifier.noRippleClick(onClick: () -> Unit): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
    )
}

// Strong ease-out for interface feedback: starts fast, lands soft.
// Mirrors cubic-bezier(0.23, 1, 0.32, 1) from Emil Kowalski's design skill.
internal val BorderlyStrongEaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

/**
 * Press feedback driven by an already-created interaction source: the
 * control scales down to ~0.97 while the finger is down and eases back out.
 * Transform-only, so it stays enabled even on low-end devices.
 */
@Composable
internal fun rememberBorderlyPressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.97f
): Float {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = tween(120, easing = BorderlyStrongEaseOut),
        label = "borderlyPressScale"
    )
    return scale
}

/**
 * Clickable without ripple plus the "button must feel responsive" press
 * feedback: a subtle 0.97 scale that springs down in ~120ms and eases back
 * out. Transform-only, so it stays enabled even on low-end devices.
 */
@Composable
internal fun Modifier.borderlyPressable(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberBorderlyPressScale(interactionSource)
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}
