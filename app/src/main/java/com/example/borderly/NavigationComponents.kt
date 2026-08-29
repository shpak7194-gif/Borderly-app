package com.example.borderly

// BORDERLY_BOTTOM_NAV_LABEL_WEIGHT_MEDIUM_SEMIBOLD_2026_08_19

// BORDERLY_BOTTOM_NAV_LIGHT_SHADOW_ALPHA_008_2026_08_19

// BORDERLY_BOTTOM_NAV_LIGHT_SHADOW_ALPHA_014_2026_08_19

// BORDERLY_BOTTOM_NAV_REAL_BLURRED_HALO_V5_2026_08_18

// BORDERLY_BOTTOM_NAV_SHADOW_UNCLIPPED_V4_2026_08_18

// BORDERLY_BOTTOM_NAV_VISIBLE_SOFT_SHADOW_V3_2026_08_18

// BORDERLY_BOTTOM_NAV_SUBTLE_SOFT_SHADOW_V2_2026_08_18

// BORDERLY_SETTINGS_SELECTED_INSET_MATCH_TABS_4DP_2026_08_18

// BORDERLY_SETTINGS_SELECTED_GLASS_SAME_AS_TABS_2026_08_18

// BORDERLY_SELECTED_TAB_GLASS_LAYER_BDBDBD_2026_08_18

// BORDERLY_BOTTOM_NAV_LIGHT_SURFACE_F4_BLUR_3DP_2026_08_18

// BORDERLY_BOTTOM_NAV_HAZE_BLUR_14DP_2026_08_18

// BORDERLY_BOTTOM_NAV_HAZE_GLASS_18DP_2026_08_18

// BORDERLY_BOTTOM_NAV_RESTORE_CAPSULE_SHAPE_2026_08_18

// BORDERLY_BOTTOM_NAV_MATCH_REGION_CARD_DESIGN_2026_08_18

// BORDERLY_BOTTOM_NAV_TRANSPARENT_BACKGROUND_2026_08_18

// BORDERLY_CAPSULE_NAV_WITH_SETTINGS_CIRCLE_2026_08_18

// BORDERLY_BOTTOM_NAV_SETTINGS_FOURTH_2026_08_18

import android.graphics.Paint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.remember
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
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = 10.dp,
                    top = 8.dp,
                    end = 10.dp,
                    bottom = 8.dp
                )
                .height(68.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Карта + Сравнение + Рейтинг находятся в одной общей капсуле.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .borderlyNavigationRim(
                        rimColor = navigationRimColor,
                        cornerRadius = 34.dp
                    )
            ) {
                // Real soft halo behind the capsule. Unlike elevation shadow,
                // this is a blurred shape and remains visible on the pale UI background.
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
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NavigationItem(
                            label = "Карта",
                            icon = Icons.Outlined.Map,
                            selected = selectedTab == AppTab.MAP,
                            hazeState = hazeState,
                            selectedHazeStyle = selectedTabHazeStyle,
                            modifier = Modifier.weight(1f),
                            onClick = { onTabClick(AppTab.MAP) }
                        )
                        NavigationItem(
                            label = "Сравнение",
                            icon = Icons.Outlined.Balance,
                            selected = selectedTab == AppTab.COMPARE,
                            hazeState = hazeState,
                            selectedHazeStyle = selectedTabHazeStyle,
                            modifier = Modifier.weight(1f),
                            onClick = { onTabClick(AppTab.COMPARE) }
                        )
                        NavigationItem(
                            label = "Рейтинг",
                            icon = Icons.Outlined.EmojiEvents,
                            selected = selectedTab == AppTab.RANKING,
                            hazeState = hazeState,
                            selectedHazeStyle = selectedTabHazeStyle,
                            modifier = Modifier.weight(1f),
                            onClick = { onTabClick(AppTab.RANKING) }
                        )
                    }
                }
            }

            // Настройки — отдельная круглая кнопка без подписи.
            // Базовое стекло остаётся как раньше, а при выборе сверху
            // накладывается тот же selected glass layer, что у Карта/Сравнение/Рейтинг.
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .borderlyNavigationRim(
                        rimColor = navigationRimColor,
                        cornerRadius = 34.dp
                    )
                    .noRippleClick { onTabClick(AppTab.SETTINGS) },
                contentAlignment = Alignment.Center
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

                if (selectedTab == AppTab.SETTINGS) {
                    Box(
                        modifier = Modifier
                            // Same 4dp inset as the selected oval inside
                            // the main navigation capsule.
                            .size(60.dp)
                            .zIndex(1f)
                            .clip(CircleShape)
                            .background(if (lowEndDevice) selectedNavigationSurface else Color.Transparent)
                            .hazeEffect(
                                state = hazeState,
                                style = selectedTabHazeStyle
                            ) {
                                inputScale = HazeInputScale.Auto
                                blurEnabled = !lowEndDevice
                            }
                    )
                }

                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Настройки",
                    modifier = Modifier
                        .size(28.dp)
                        .zIndex(2f),
                    tint = if (selectedTab == AppTab.SETTINGS) {
                        navigationContentColor
                    } else {
                        navigationSecondaryContentColor
                    }
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
    hazeState: HazeState,
    selectedHazeStyle: HazeStyle,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val selectedShape = RoundedCornerShape(30.dp)
    val lowEndDevice = LocalBorderlyLowEndMode.current
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val selectedFallbackColor = if (darkTheme) Color(0xFF36414B) else Color(0xFFDCE0E5)
    val contentColor = if (darkTheme) Color(0xFFF8FAFC) else Color(0xFF1E2A36)
    val secondaryContentColor = if (darkTheme) Color(0xFFAAB4BE) else Color(0xFF5E6975)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .zIndex(if (selected) 1f else 0f)
            .then(
                if (selected) {
                    Modifier
                        .clip(selectedShape)
                        .background(if (lowEndDevice) selectedFallbackColor else Color.Transparent)
                        .hazeEffect(
                            state = hazeState,
                            style = selectedHazeStyle
                        ) {
                            inputScale = HazeInputScale.Auto
                            blurEnabled = !lowEndDevice
                        }
                } else {
                    Modifier
                }
            )
            .noRippleClick(onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(25.dp),
                tint = if (selected) {
                    contentColor
                } else {
                    secondaryContentColor
                }
            )

            Text(
                text = label,
                modifier = Modifier.padding(top = 3.dp),
                color = if (selected) {
                    contentColor
                } else {
                    secondaryContentColor
                },
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