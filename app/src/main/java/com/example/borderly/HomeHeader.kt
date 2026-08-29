package com.example.borderly

import android.graphics.Paint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

internal fun Modifier.borderlyAdaptivePillRim(
    rimColor: Color,
    solidFraction: Float = 0.13f,
    fadeFraction: Float = 0.19f,
    enabled: Boolean = true
): Modifier = drawWithContent {
    drawContent()

    if (!enabled || size.width <= 0f || size.height <= 0f) {
        return@drawWithContent
    }

    val inset = 0.75.dp.toPx()
    val left = inset
    val top = inset
    val right = size.width - inset
    val bottom = size.height - inset
    val heightPx = (bottom - top).coerceAtLeast(0f)
    val radius = heightPx / 2f
    val centerY = (top + bottom) / 2f

    if (radius <= 0f || right <= left) return@drawWithContent

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

    /*
     * EXACT TRAJECTORY FOR LONG PILL CONTROLS
     *
     * We do NOT calculate the route from the full perimeter anymore.
     *
     * Upper segment:
     * left middle -> upper-left rounding -> entire top straight ->
     * only part of upper-right rounding.
     *
     * Lower segment:
     * right middle -> lower-right rounding -> entire bottom straight ->
     * only part of lower-left rounding.
     *
     * This is the same visible geometry as the approved filter-chip rim:
     * gap at upper-right + gap at lower-left.
     */
    val cornerEndProgress = 0.58f

    val upperPath = AndroidPath().apply {
        moveTo(left, centerY)

        // left-middle -> top-left tangent
        arcTo(
            leftArc,
            180f,
            90f,
            false
        )

        // whole top straight
        lineTo(right - radius, top)

        // enter the top-right rounding, but do not travel to right-middle
        arcTo(
            rightArc,
            -90f,
            90f * cornerEndProgress,
            false
        )
    }

    val lowerPath = AndroidPath().apply {
        moveTo(right, centerY)

        // right-middle -> bottom-right tangent
        arcTo(
            rightArc,
            0f,
            90f,
            false
        )

        // whole bottom straight
        lineTo(left + radius, bottom)

        // enter the bottom-left rounding, but do not travel to left-middle
        arcTo(
            leftArc,
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

        fun drawTrajectory(path: AndroidPath) {
            val measure = android.graphics.PathMeasure(path, false)
            val length = measure.length
            if (length <= 0f) return

            /*
             * Fade stays smooth and happens at BOTH ends.
             * Its physical length is tied to the pill rounding, not to the
             * entire width, so a long passport/search pill does not distort
             * the trajectory the way perimeter fractions did.
             */
            val quarterArcLength = (Math.PI.toFloat() * radius) / 2f
            val fadeLength = (quarterArcLength * 0.95f)
                .coerceAtMost(length * 0.30f)
            val solidStart = fadeLength
            val solidEnd = (length - fadeLength).coerceAtLeast(solidStart)
            val fadeSteps = 28

            fun drawSegment(
                from: Float,
                to: Float,
                alpha: Float
            ) {
                if (to <= from || alpha <= 0f) return

                val segment = AndroidPath()
                if (
                    measure.getSegment(
                        from.coerceIn(0f, length),
                        to.coerceIn(0f, length),
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

            // Fade-in: 0 -> 100%.
            for (index in 0 until fadeSteps) {
                val t0 = index.toFloat() / fadeSteps
                val t1 = (index + 1f) / fadeSteps

                drawSegment(
                    from = fadeLength * t0,
                    to = fadeLength * t1,
                    alpha = smoothStep((t0 + t1) / 2f)
                )
            }

            // Long solid section along the straight edge.
            drawSegment(
                from = solidStart,
                to = solidEnd,
                alpha = 1f
            )

            // Fade-out: 100% -> 0%.
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

        drawTrajectory(upperPath)
        drawTrajectory(lowerPath)
    }
}

internal fun Modifier.borderlyRoundControlRim(
    rimColor: Color
): Modifier = drawWithContent {
    drawContent()

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
        val base = rimColor
        val transparent = base.copy(alpha = 0f)
        val stroke = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Butt)

        fun stop(angle: Float): Float =
            (angle / 360f).coerceIn(0f, 1f)

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

@Composable
internal fun Header(
    selectedPassport: Passport,
    onPassportClick: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        val headerWidth = maxWidth
        val compact = headerWidth < 360.dp
        val horizontalPadding = 20.dp
        val controlHeight = 48.dp
        val floatingControlColor = borderlyControlSurfaceColor()
        val floatingControlBorder = borderlyControlRimColor()
        val floatingControlRadius = 50.dp

        PassportSelector(
            selectedPassport = selectedPassport,
            onClick = onPassportClick,
            maxSelectorWidth = headerWidth,
            controlHeight = controlHeight,
            controlColor = floatingControlColor,
            controlBorderColor = floatingControlBorder,
            controlRadius = floatingControlRadius,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = horizontalPadding,
                    top = if (compact) 18.dp else 22.dp,
                    end = horizontalPadding,
                    bottom = if (compact) 13.dp else 17.dp
                )
        )
    }
}

@Composable
internal fun PassportSelector(
    selectedPassport: Passport,
    onClick: () -> Unit,
    label: String? = null,
    maxSelectorWidth: Dp = 202.dp,
    controlHeight: Dp = 46.dp,
    controlColor: Color = MaterialTheme.colorScheme.surface,
    controlBorderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    controlRadius: Dp = 14.dp,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(min = 118.dp, max = maxSelectorWidth)
            .height(controlHeight)
            .borderlyAdaptivePillRim(
                    rimColor = controlBorderColor,
                    solidFraction = 0.13f,
                    fadeFraction = 0.19f
                )
            .noRippleClick(onClick),
        color = controlColor,
        shape = RoundedCornerShape(50),
        border = null
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label ?: stringResource(R.string.passport),
                color = borderlySecondaryContentColor(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = selectedPassport.flag,
                fontSize = 17.sp,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.width(7.dp))

            Text(
                text = selectedPassport.localizedName(),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.width(10.dp))

            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = stringResource(R.string.choose_passport),
                modifier = Modifier.size(19.dp),
                tint = borderlySecondaryContentColor()
            )
        }
    }
}
