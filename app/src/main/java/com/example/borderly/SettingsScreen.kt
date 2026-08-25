package com.example.borderly

// BORDERLY_SETTINGS_SUBTLE_ROW_DIVIDERS_2026_08_18

// BORDERLY_SETTINGS_REDESIGN_V1_COMPILE_FIX_2026_08_18

// BORDERLY_SETTINGS_REDESIGN_V1_2026_08_18

import android.graphics.Paint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

@Composable
internal fun SettingsScreen(
    dataVersion: Int,
    dataUpdated: String,
    dataSource: String,
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    performanceMode: PerformanceMode,
    onPerformanceModeChange: (PerformanceMode) -> Unit,
    onBack: () -> Unit
) {
    // onBack remains in the public signature for compatibility with MainActivity.
    // The visible back button is intentionally removed because Settings is now
    // a first-class bottom-navigation tab.
    @Suppress("UNUSED_VARIABLE")
    val keepBackCallbackCompatible = onBack

    val context = LocalContext.current
    val appVersion = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty().ifBlank { "—" }
    }

    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardColor = if (darkTheme) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
    } else {
        Color(0xFFF9F9F9).copy(alpha = 0.90f)
    }
    val cardRimColor = if (darkTheme) {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.92f)
    } else {
        Color.White
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "Настройки",
            modifier = Modifier
                .statusBarsPadding()
                .padding(
                    start = 20.dp,
                    top = 18.dp,
                    end = 20.dp,
                    bottom = 10.dp
                ),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 18.dp,
                top = 6.dp,
                end = 18.dp,
                bottom = 116.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                SettingsSection(
                    title = "Оформление",
                    cardColor = cardColor,
                    rimColor = cardRimColor
                ) {
                    Text(
                        text = "Тема",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ThemeSegmentedControl(
                        selected = themeMode,
                        onSelected = onThemeModeChange,
                        darkTheme = darkTheme
                    )
                }
            }

            item {
                SettingsSection(
                    title = "Производительность",
                    cardColor = cardColor,
                    rimColor = cardRimColor
                ) {
                    Text(
                        text = "Режим для слабых устройств",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "Отключает размытие фона и упрощает отрисовку карты. «Авто» определяет режим по устройству.",
                        modifier = Modifier.padding(top = 3.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PerformanceModeSegmentedControl(
                        selected = performanceMode,
                        onSelected = onPerformanceModeChange,
                        darkTheme = darkTheme
                    )
                }
            }

            item {
                SettingsSection(
                    title = "Язык",
                    cardColor = cardColor,
                    rimColor = cardRimColor
                ) {
                    SettingsInfoRow(
                        title = "Язык приложения",
                        value = "Русский",
                        badge = "Скоро"
                    )
                }
            }

            item {
                SettingsSection(
                    title = "Данные",
                    cardColor = cardColor,
                    rimColor = cardRimColor
                ) {
                    SettingsInfoRow(
                        title = "Версия базы",
                        value = dataVersion.toString()
                    )
                    SettingsDivider()
                    SettingsInfoRow(
                        title = "Последнее обновление",
                        value = dataUpdated.ifBlank { "—" }
                    )
                }
            }

            item {
                SettingsSection(
                    title = "О приложении",
                    cardColor = cardColor,
                    rimColor = cardRimColor
                ) {
                    SettingsInfoRow(
                        title = "Источник данных",
                        value = dataSource.ifBlank { "—" }
                    )
                    SettingsDivider()
                    SettingsInfoRow(
                        title = "Версия Borderly",
                        value = appVersion
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    cardColor: Color,
    rimColor: Color,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            modifier = Modifier.padding(start = 5.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .settingsRoundedRectRim(
                    rimColor = rimColor,
                    cornerRadius = 26.dp
                ),
            color = cardColor,
            shape = RoundedCornerShape(26.dp),
            border = null,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 15.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun ThemeSegmentedControl(
    selected: AppThemeMode,
    onSelected: (AppThemeMode) -> Unit,
    darkTheme: Boolean
) {
    SettingsSegmentedControl(
        options = AppThemeMode.entries.map { it.title },
        selectedIndex = AppThemeMode.entries.indexOf(selected),
        onSelected = { index -> onSelected(AppThemeMode.entries[index]) },
        darkTheme = darkTheme
    )
}

@Composable
private fun PerformanceModeSegmentedControl(
    selected: PerformanceMode,
    onSelected: (PerformanceMode) -> Unit,
    darkTheme: Boolean
) {
    SettingsSegmentedControl(
        options = PerformanceMode.entries.map { it.title },
        selectedIndex = PerformanceMode.entries.indexOf(selected),
        onSelected = { index -> onSelected(PerformanceMode.entries[index]) },
        darkTheme = darkTheme
    )
}

@Composable
private fun SettingsSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    darkTheme: Boolean
) {
    val trackColor = if (darkTheme) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    } else {
        Color(0xFFE9E9EE).copy(alpha = 0.68f)
    }
    val selectedColor = if (darkTheme) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        Color(0xFFE3E3E8).copy(alpha = 0.78f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(
                color = trackColor,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        options.forEachIndexed { index, title ->
            val active = index == selectedIndex

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .noRippleClick { onSelected(index) },
                color = if (active) selectedColor else Color.Transparent,
                shape = RoundedCornerShape(15.dp),
                border = null,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        color = if (active) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontWeight = if (active) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Normal
                        },
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsDivider() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )
    )
}

@Composable
private fun SettingsInfoRow(
    title: String,
    value: String,
    badge: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = value,
            modifier = Modifier.padding(start = 14.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (badge != null) {
            Spacer(modifier = Modifier.size(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                shape = RoundedCornerShape(9.dp),
                border = null
            ) {
                Text(
                    text = badge,
                    modifier = Modifier.padding(
                        horizontal = 8.dp,
                        vertical = 4.dp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Same asymmetric faded outer-rim trajectory used by Borderly's rounded cards.
 * It is duplicated locally so SettingsScreen does not depend on private
 * implementation details from WorldMap.kt.
 */
private fun Modifier.settingsRoundedRectRim(
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

