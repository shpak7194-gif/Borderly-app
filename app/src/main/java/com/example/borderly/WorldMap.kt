package com.example.borderly

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.Region
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
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
import org.json.JSONObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal data class NativeFloatBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
    val center: Offset get() = Offset((left + right) / 2f, (top + bottom) / 2f)

    fun contains(x: Float, y: Float): Boolean =
        x >= left && x <= right && y >= top && y <= bottom
}

internal class NativeCountryShape(
    val id: Int,
    val name: String,
    val flag: String,
    val path: AndroidPath,
    val region: Region,
    val floatBounds: NativeFloatBounds,
    val markerBounds: NativeFloatBounds,
    val markerAnchor: Offset,
    val rings: List<List<Offset>>
)

internal class NativeMapData(
    val width: Float,
    val height: Float,
    val countries: List<NativeCountryShape>
)

internal fun createWorldGraticule(map: NativeMapData): List<AndroidPath> {
    val paths = mutableListOf<AndroidPath>()
    val centerX = map.width / 2f
    val centerY = map.height / 2f
    val radiusX = map.width * .465f
    val radiusY = map.height * .455f

    // Parallels gently bend toward the equator at the edges, matching the
    // rounded world projection from the visual reference.
    for (latitudeIndex in -4..4) {
        val latitude = latitudeIndex / 5f
        val y = centerY + latitude * radiusY
        val halfWidth = radiusX * kotlin.math.sqrt(
            (1f - latitude * latitude).coerceAtLeast(0f)
        )
        val edgeY = y - latitude * radiusY * .055f
        paths += AndroidPath().apply {
            moveTo(centerX - halfWidth, edgeY)
            cubicTo(
                centerX - halfWidth * .42f,
                y,
                centerX + halfWidth * .42f,
                y,
                centerX + halfWidth,
                edgeY
            )
        }
    }

    // Meridians narrow near the poles and bow out around the equator.
    for (longitudeIndex in -8..8) {
        val longitude = longitudeIndex / 9f
        val equatorX = centerX + longitude * radiusX
        val poleX = centerX + longitude * radiusX * .28f
        paths += AndroidPath().apply {
            moveTo(poleX, centerY - radiusY)
            cubicTo(
                centerX + longitude * radiusX * .56f,
                centerY - radiusY * .72f,
                equatorX,
                centerY - radiusY * .28f,
                equatorX,
                centerY
            )
            cubicTo(
                equatorX,
                centerY + radiusY * .28f,
                centerX + longitude * radiusX * .56f,
                centerY + radiusY * .72f,
                poleX,
                centerY + radiusY
            )
        }
    }
    return paths
}

internal fun loadNativeWorldMap(context: Context): NativeMapData {
    val json = context.assets
        .open("borderly_world_map_native.json")
        .bufferedReader(Charsets.UTF_8)
        .use { it.readText() }
    val root = JSONObject(json)
    val width = root.getDouble("width").toFloat()
    val height = root.getDouble("height").toFloat()
    val clip = Region(0, 0, width.roundToInt() + 1, height.roundToInt() + 1)
    val countryArray = root.getJSONArray("countries")
    val countries = ArrayList<NativeCountryShape>(countryArray.length())

    for (countryIndex in 0 until countryArray.length()) {
        val country = countryArray.getJSONObject(countryIndex)
        val countryId = country.getInt("id")
        val path = AndroidPath().apply {
            fillType = AndroidPath.FillType.EVEN_ODD
        }
        var firstRussianRing = true
        val allRings = mutableListOf<List<Offset>>()
        var countryLeft = Float.POSITIVE_INFINITY
        var countryTop = Float.POSITIVE_INFINITY
        var countryRight = Float.NEGATIVE_INFINITY
        var countryBottom = Float.NEGATIVE_INFINITY
        var bestRingBounds: NativeFloatBounds? = null
        var bestRingArea = -1f

        val rings = country.getJSONArray("rings")
        for (ringIndex in 0 until rings.length()) {
            val ring = rings.getJSONArray(ringIndex)
            if (ring.length() < 3) continue

            val firstRawPoint = ring.getJSONArray(0)
            val firstRawX = firstRawPoint.getDouble(0).toFloat()
            // The source projection cuts Russia at the international date line.
            val russianWrapOffset = if (countryId == 643 && firstRawX < 200f) {
                width * .9f
            } else {
                0f
            }

            val points = ArrayList<Offset>(ring.length())
            var ringLeft = Float.POSITIVE_INFINITY
            var ringTop = Float.POSITIVE_INFINITY
            var ringRight = Float.NEGATIVE_INFINITY
            var ringBottom = Float.NEGATIVE_INFINITY
            for (pointIndex in 0 until ring.length()) {
                val point = ring.getJSONArray(pointIndex)
                val x = point.getDouble(0).toFloat() + russianWrapOffset
                val y = point.getDouble(1).toFloat()
                points += Offset(x, y)
                ringLeft = min(ringLeft, x)
                ringTop = min(ringTop, y)
                ringRight = max(ringRight, x)
                ringBottom = max(ringBottom, y)
                countryLeft = min(countryLeft, x)
                countryTop = min(countryTop, y)
                countryRight = max(countryRight, x)
                countryBottom = max(countryBottom, y)
            }
            if (points.size < 3) continue
            allRings += points

            val ringPath = AndroidPath().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { point -> lineTo(point.x, point.y) }
                close()
            }

            if (countryId == 643) {
                if (firstRussianRing) {
                    path.addPath(ringPath)
                    firstRussianRing = false
                } else if (!path.op(ringPath, AndroidPath.Op.UNION)) {
                    path.addPath(ringPath)
                }
            } else {
                path.addPath(ringPath)
            }

            var twiceArea = 0f
            points.forEachIndexed { index, point ->
                val next = points[(index + 1) % points.size]
                twiceArea += point.x * next.y - next.x * point.y
            }
            val ringArea = kotlin.math.abs(twiceArea) / 2f
            if (ringArea > bestRingArea) {
                bestRingArea = ringArea
                bestRingBounds = NativeFloatBounds(
                    left = ringLeft,
                    top = ringTop,
                    right = ringRight,
                    bottom = ringBottom
                )
            }
        }

        val floatBounds = if (countryLeft.isFinite()) {
            NativeFloatBounds(countryLeft, countryTop, countryRight, countryBottom)
        } else {
            NativeFloatBounds(0f, 0f, 0f, 0f)
        }
        val markerBounds = bestRingBounds ?: floatBounds
        val region = Region().apply { setPath(path, clip) }
        countries += NativeCountryShape(
            id = countryId,
            name = country.getString("name"),
            flag = country.getString("flag"),
            path = path,
            region = region,
            floatBounds = floatBounds,
            markerBounds = markerBounds,
            markerAnchor = markerBounds.center,
            rings = allRings
        )
    }
    return NativeMapData(width, height, countries)
}

internal fun pointInRing(x: Float, y: Float, ring: List<Offset>): Boolean {
    if (ring.size < 3) return false
    var inside = false
    var previous = ring.last()
    for (current in ring) {
        val crosses = (current.y > y) != (previous.y > y)
        if (crosses) {
            val denominator = previous.y - current.y
            if (denominator != 0f) {
                val crossingX = (previous.x - current.x) *
                        (y - current.y) / denominator + current.x
                if (x < crossingX) inside = !inside
            }
        }
        previous = current
    }
    return inside
}

internal fun countryContainsPoint(country: NativeCountryShape, x: Float, y: Float): Boolean {
    if (!country.floatBounds.contains(x, y)) return false
    var inside = false
    country.rings.forEach { ring ->
        if (pointInRing(x, y, ring)) inside = !inside
    }
    return inside
}

internal fun clampNativeMapPan(
    pan: Offset,
    zoom: Float,
    canvasSize: IntSize,
    map: NativeMapData
): Offset {
    if (canvasSize.width == 0 || canvasSize.height == 0) return Offset.Zero
    val baseScale = min(
        canvasSize.width / map.width,
        canvasSize.height / map.height
    )
    val scaledWidth = map.width * baseScale * zoom
    val scaledHeight = map.height * baseScale * zoom
    // Keep the map draggable even at 1x. Previously both limits became zero
    // while the whole world fitted inside the viewport, so pan only started
    // working after zooming in.
    val freePanX = canvasSize.width * 0.18f
    val freePanY = canvasSize.height * 0.18f
    val maxX = max(freePanX, (scaledWidth - canvasSize.width) / 2f)
    val maxY = max(freePanY, (scaledHeight - canvasSize.height) / 2f)
    return Offset(
        x = pan.x.coerceIn(-maxX, maxX),
        y = pan.y.coerceIn(-maxY, maxY)
    )
}

// Small countries and island states that are difficult to hit with a finger at
// the initial world view. Their dots are only a visual/touch aid; geography is
// still taken from the real polygon data.
internal val SmallCountryMarkerIds = setOf(
    20, 28, 44, 48, 52, 90, 96, 132, 136, 174, 196, 212, 234, 242, 248, 258,
    262, 275, 296, 316, 344, 414, 422, 438, 442, 470, 480, 531, 548, 584,
    585, 630, 634, 646, 659, 666, 678, 702, 748, 780, 833, 882, 983,
    308, 336, 446, 462, 492, 520, 583, 662, 670, 674, 690, 776, 798,
    16, 60, 86, 92, 162, 166, 175, 184, 254, 260, 292, 312, 334, 474, 500, 533,
    534, 535, 570, 574, 580, 581, 612, 638, 652, 654, 660, 663, 744,
    772, 796, 831, 832, 850, 876
)

// A few crowded areas need a tiny screen-space offset so dots do not sit on
// top of one another. A leader line keeps the geographic relationship clear.
internal fun smallCountryMarkerOffsetDp(countryIso: Int): Offset = when (countryIso) {
    20 -> Offset(-7f, 8f)
    28 -> Offset(11f, -10f)
    308 -> Offset(12f, 11f)
    336 -> Offset(13f, 10f)
    446 -> Offset(-10f, 10f)
    492 -> Offset(-12f, 8f)
    662 -> Offset(12f, 0f)
    670 -> Offset(-12f, 8f)
    674 -> Offset(13f, -8f)
    438 -> Offset(9f, -8f)
    442 -> Offset(-9f, -8f)
    470 -> Offset(8f, 8f)
    422 -> Offset(-10f, -9f)
    275 -> Offset(10f, 3f)
    414 -> Offset(-12f, 2f)
    48 -> Offset(-11f, 12f)
    634 -> Offset(11f, 12f)
    659 -> Offset(-10f, -10f)
    212 -> Offset(-11f, 3f)
    52 -> Offset(11f, 5f)
    780 -> Offset(3f, 13f)
    702 -> Offset(0f, 9f)
    344 -> Offset(0f, -9f)
    96 -> Offset(0f, 9f)
    else -> Offset.Zero
}

internal fun resolveSmallMarkerZoomOffsets(
    desiredCenters: List<Pair<Int, Offset>>,
    minDistancePx: Float,
    maxShiftPx: Float
): Map<Int, Offset> {
    if (desiredCenters.size < 2 || minDistancePx <= 0f || maxShiftPx <= 0f) {
        return emptyMap()
    }

    val result = desiredCenters.associate { it.first to Offset.Zero }.toMutableMap()
    val desired = desiredCenters.toMap()
    val ids = desiredCenters.map { it.first }.sorted()
    val minDistanceSquared = minDistancePx * minDistancePx

    // Only a very small correction: one marker may move a few dp when two
    // visible circles still overlap after zooming. This deliberately does not
    // try to "spread out" whole clusters like the previous v61 algorithm.
    repeat(2) {
        for (firstIndex in 0 until ids.lastIndex) {
            for (secondIndex in firstIndex + 1 until ids.size) {
                val firstId = ids[firstIndex]
                val secondId = ids[secondIndex]
                val first = desired.getValue(firstId) + result.getValue(firstId)
                val second = desired.getValue(secondId) + result.getValue(secondId)

                var dx = second.x - first.x
                var dy = second.y - first.y
                var distanceSquared = dx * dx + dy * dy
                if (distanceSquared >= minDistanceSquared) continue

                if (distanceSquared < 0.01f) {
                    dx = if ((firstId + secondId) % 2 == 0) 1f else 0f
                    dy = if (dx == 0f) 1f else 0f
                    distanceSquared = 1f
                }

                val distance = sqrt(distanceSquared)
                val required = (minDistancePx - distance).coerceAtLeast(0f)
                val unitX = dx / distance
                val unitY = dy / distance

                // Split the smallest necessary movement between both markers.
                fun limited(offset: Offset): Offset {
                    val lengthSquared = offset.x * offset.x + offset.y * offset.y
                    if (lengthSquared <= maxShiftPx * maxShiftPx) return offset
                    val length = sqrt(lengthSquared)
                    return Offset(
                        offset.x / length * maxShiftPx,
                        offset.y / length * maxShiftPx
                    )
                }

                result[firstId] = limited(
                    result.getValue(firstId) -
                            Offset(unitX * required / 2f, unitY * required / 2f)
                )
                result[secondId] = limited(
                    result.getValue(secondId) +
                            Offset(unitX * required / 2f, unitY * required / 2f)
                )
            }
        }
    }

    return result.filterValues { it != Offset.Zero }
}

internal fun shouldShowSmallCountryMarker(
    country: NativeCountryShape,
    totalScale: Float,
    density: Float
): Boolean {
    if (country.id !in SmallCountryMarkerIds || density <= 0f) return false
    val widthDp = country.markerBounds.width * totalScale / density
    val heightDp = country.markerBounds.height * totalScale / density
    val longestSideDp = max(widthDp, heightDp)
    // Keep the marker until the actual polygon is comfortably visible. At high
    // zoom the marker disappears and precise float-polygon hit testing takes over.
    return longestSideDp < 14f
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun InteractiveWorldMap(
    map: NativeMapData?,
    passport: Passport,
    visaRequirements: Map<Int, VisaRequirement>,
    quickFilter: MapVisaQuickFilter = MapVisaQuickFilter.ALL,
    selectedRegion: PassportRegion? = null,
    selectedCountryIso: Int?,
    onCountrySelected: (Int, String, String) -> Unit,
    onEmptySpaceSelected: () -> Unit,
    onInteractionChanged: (Boolean) -> Unit = {},
    zoomInRequest: Int = 0,
    zoomOutRequest: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentCountryCallback = rememberUpdatedState(onCountrySelected)
    val currentEmptySpaceCallback = rememberUpdatedState(onEmptySpaceSelected)
    val currentInteractionCallback = rememberUpdatedState(onInteractionChanged)
    val displayDensity = context.resources.displayMetrics.density
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val mapCanvasBackground = if (darkTheme) Color(0xFF111B23) else Color(0xFFEEF5FA)
    val mapMarkerHalo = if (darkTheme) Color(0xFF11161B) else Color.White
    val mapMarkerOutline = if (darkTheme) Color(0xFFF1F3F4) else Black
    val mapMarkerLeader = if (darkTheme) Color(0xFFC1C8CE) else Black

    if (map == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = R.drawable.borderly_world_map),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
            Text(
                text = stringResource(R.string.interactive_map_missing),
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = .94f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                color = VisaRequired,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
        return
    }

    val panSaver = remember {
        listSaver<Offset, Float>(
            save = { offset -> listOf(offset.x, offset.y) },
            restore = { values -> Offset(values[0], values[1]) }
        )
    }
    var zoom by rememberSaveable { mutableStateOf(1f) }
    var pan by rememberSaveable(stateSaver = panSaver) { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // While the finger is down the map renders in fast mode: no graticule
    // and no country border strokes, so weak GPUs keep a smooth frame rate.
    var isInteracting by remember { mutableStateOf(false) }
    val fillPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    }
    val borderPaint = remember(darkTheme) {
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
    val graticulePaint = remember(darkTheme) {
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
    val selectedPaint = remember(darkTheme) {
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
    val hitCountries = remember(map) {
        map.countries.sortedBy { country ->
            country.floatBounds.width * country.floatBounds.height
        }
    }
    val smallMarkerCountries = remember(map) {
        map.countries.filter { it.id in SmallCountryMarkerIds }
    }
    val graticulePaths = remember(map) { createWorldGraticule(map) }
    val markerCollisionOffsets = remember(map, canvasSize, zoom, displayDensity) {
        // At the normal world view markers stay exactly where v60 placed them.
        // Only after zooming in do we allow a tiny correction if two visible
        // circles still physically overlap.
        if (canvasSize.width == 0 || canvasSize.height == 0 || zoom < 1.35f) {
            emptyMap()
        } else {
            val baseScale = min(
                canvasSize.width / map.width,
                canvasSize.height / map.height
            )
            val totalScale = baseScale * zoom
            val desiredCenters = smallMarkerCountries
                .asSequence()
                .filter { shouldShowSmallCountryMarker(it, totalScale, displayDensity) }
                .map { country ->
                    val screenCenter = Offset(
                        x = canvasSize.width / 2f +
                                (country.markerAnchor.x - map.width / 2f) * totalScale,
                        y = canvasSize.height / 2f +
                                (country.markerAnchor.y - map.height / 2f) * totalScale
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
                // v60 visual halo is 12 dp across. 13 dp only prevents real
                // circle-on-circle overlap; it does not spread nearby markers.
                minDistancePx = 13f * displayDensity,
                maxShiftPx = 5f * displayDensity
            )
        }
    }

    fun mapPointToScreen(point: Offset, totalScale: Float): Offset = Offset(
        x = canvasSize.width / 2f + pan.x +
                (point.x - map.width / 2f) * totalScale,
        y = canvasSize.height / 2f + pan.y +
                (point.y - map.height / 2f) * totalScale
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
        pan = clampNativeMapPan(nextPan, newZoom, canvasSize, map)
    }

    suspend fun animateButtonZoom(factor: Float) {
        if (canvasSize.width == 0 || canvasSize.height == 0) return

        val startZoom = zoom
        val targetZoom = (startZoom * factor).coerceIn(1f, 20f)
        if (targetZoom == startZoom) return

        // Button zoom is always centred on the visible map.  Pan scales with the
        // zoom so the same geographical point stays under the centre while the
        // transition is animated instead of jumping in a single frame.
        val panRatio = targetZoom / startZoom
        val targetPan = clampNativeMapPan(
            Offset(pan.x * panRatio, pan.y * panRatio),
            targetZoom,
            canvasSize,
            map
        )
        val startPan = pan

        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 210,
                easing = FastOutSlowInEasing
            )
        ) { progress, _ ->
            val currentZoom = startZoom + (targetZoom - startZoom) * progress
            val currentPan = Offset(
                x = startPan.x + (targetPan.x - startPan.x) * progress,
                y = startPan.y + (targetPan.y - startPan.y) * progress
            )
            zoom = currentZoom
            pan = clampNativeMapPan(currentPan, currentZoom, canvasSize, map)
        }
    }

    LaunchedEffect(zoomInRequest) {
        if (zoomInRequest > 0) {
            animateButtonZoom(factor = 1.60f)
        }
    }

    LaunchedEffect(zoomOutRequest) {
        if (zoomOutRequest > 0) {
            animateButtonZoom(factor = 1f / 1.60f)
        }
    }

    val scaleDetector = remember(map) {
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

    val gestureDetector = remember(map) {
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
                        map
                    )
                    return true
                }

                override fun onSingleTapUp(event: MotionEvent): Boolean {
                    if (canvasSize.width == 0 || canvasSize.height == 0) return false
                    val baseScale = min(
                        canvasSize.width / map.width,
                        canvasSize.height / map.height
                    )
                    val totalScale = baseScale * zoom
                    val markerHitRadiusPx = 20f * displayDensity
                    val markerCountry = smallMarkerCountries
                        .asSequence()
                        .filter { shouldShowSmallCountryMarker(it, totalScale, displayDensity) }
                        .map { country -> country to markerScreenCenter(country, totalScale) }
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
                            map.width / 2f
                    val mapY = (event.y - canvasSize.height / 2f - pan.y) / totalScale +
                            map.height / 2f
                    val country = hitCountries.firstOrNull {
                        countryContainsPoint(it, mapX, mapY)
                    }
                    if (country == null) {
                        currentEmptySpaceCallback.value()
                        return true
                    }
                    currentCountryCallback.value(country.id, country.name, country.flag)
                    return true
                }

                override fun onDoubleTap(event: MotionEvent): Boolean {
                    zoomAt(2.0f, Offset(event.x, event.y))
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
                pan = clampNativeMapPan(pan, zoom, it, map)
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
        val baseScale = min(size.width / map.width, size.height / map.height)
        val totalScale = baseScale * zoom
        val translateX = size.width / 2f + pan.x - map.width * totalScale / 2f
        val translateY = size.height / 2f + pan.y - map.height * totalScale / 2f

        val filteredOutColor = if (darkTheme) Color(0xFF28323A) else MapFilteredOut

        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            nativeCanvas.save()
            nativeCanvas.translate(translateX, translateY)
            nativeCanvas.scale(totalScale, totalScale)
            borderPaint.strokeWidth = 1.25f / totalScale
            graticulePaint.strokeWidth = .56f / totalScale
            selectedPaint.strokeWidth = 3.2f / totalScale

            // Draw only the subtle curved coordinate grid behind the country
            // fills. Skipped during gestures to keep weak GPUs smooth.
            if (!isInteracting) {
                graticulePaths.forEach { path ->
                    nativeCanvas.drawPath(path, graticulePaint)
                }
            }

            map.countries.forEach { country ->
                val visaType = visaTypeFor(
                    passport = passport,
                    countryIso = country.id,
                    requirements = visaRequirements
                )
                val inSelectedRegion = selectedRegion == null ||
                        nativeCountryBelongsToPassportRegion(country.id, selectedRegion)
                val countryColor = when {
                    !inSelectedRegion -> filteredOutColor
                    visaType == VisaType.HOME_COUNTRY -> HomeCountry
                    quickFilter == MapVisaQuickFilter.ALL -> visaType.color
                    quickFilter.matches(visaType) -> visaType.color
                    else -> filteredOutColor
                }
                fillPaint.color = countryColor.toArgb()
                nativeCanvas.drawPath(country.path, fillPaint)
                nativeCanvas.drawPath(country.path, borderPaint)
            }

            map.countries.firstOrNull { it.id == selectedCountryIso }?.let { selected ->
                nativeCanvas.drawPath(selected.path, selectedPaint)
            }
            nativeCanvas.restore()
        }

        // Cleaner world overview:
        // 1x = 50% of the original visual marker size,
        // 10x = 100% of the original size,
        // 10x..20x = keep the same 100% size.
        // The 20 dp touch target below is intentionally NOT changed.
        val markerSizeProgress = ((zoom - 1f) / 9f).coerceIn(0f, 1f)
        val markerRadiusDp = 1.45f + (4.2f - 1.45f) * markerSizeProgress
        val markerHaloRadiusDp = 2.25f + (6.0f - 2.25f) * markerSizeProgress
        val markerRadius = markerRadiusDp.dp.toPx()
        val markerHaloRadius = markerHaloRadiusDp.dp.toPx()
        val markerLeaderWidth = 1.dp.toPx()
        val selectedMarkerWidth = 1.6.dp.toPx()
        smallMarkerCountries.forEach { country ->
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
            val markerVisaType = visaTypeFor(
                passport = passport,
                countryIso = country.id,
                requirements = visaRequirements
            )
            val markerInSelectedRegion = selectedRegion == null ||
                    nativeCountryBelongsToPassportRegion(country.id, selectedRegion)
            val markerColor = when {
                !markerInSelectedRegion -> filteredOutColor
                markerVisaType == VisaType.HOME_COUNTRY -> HomeCountry
                quickFilter == MapVisaQuickFilter.ALL -> markerVisaType.color
                quickFilter.matches(markerVisaType) -> markerVisaType.color
                else -> filteredOutColor
            }
            val markerVisualColor = if (country.id == selectedCountryIso) {
                markerColor
            } else {
                markerColor.copy(alpha = .68f + .32f * markerSizeProgress)
            }
            drawCircle(
                color = markerVisualColor,
                radius = markerRadius,
                center = markerCenter
            )
            if (country.id == selectedCountryIso) {
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

// One shared spacing value for all floating map controls.
// + / - keep their existing 8dp internal gap unchanged.
private val MapOverlaySpacing = 20.dp

@Composable
internal fun MapCard(
    nativeMap: NativeMapData?,
    passport: Passport,
    visaRequirements: Map<Int, VisaRequirement>,
    visaCounts: Map<VisaType, Int>,
    quickFilter: MapVisaQuickFilter,
    selectedRegion: PassportRegion? = null,
    countries: List<CountryInfo>,
    selectedCountryIso: Int?,
    onCountrySelected: (Int, String, String) -> Unit,
    onEmptySpaceSelected: () -> Unit,
    onInteractionChanged: (Boolean) -> Unit,
    onQuickFilterSelected: (MapVisaQuickFilter) -> Unit,
    onDestinationClick: () -> Unit,
    onExpand: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {
        val compact = maxWidth < 360.dp
        val mapAspectRatio = 0.98f
        // Extra vertical room keeps the floating filter carousel clear of the legend.
        // Floating-control spacing is calculated independently below.
        val filtersLowerOffset = if (compact) 46.dp else 54.dp
        val mapHeight = (maxWidth / mapAspectRatio) + filtersLowerOffset
        val legendHorizontalPadding = if (compact) 12.dp else 20.dp
        val darkTheme = borderlyIsDarkTheme()
        val openFullscreenMapDescription = stringResource(R.string.open_fullscreen_map)
        val zoomInDescription = stringResource(R.string.zoom_in)
        val zoomOutDescription = stringResource(R.string.zoom_out)
        val selectedRegionSuffix = selectedRegion
            ?.localizedTitle()
            ?.let { " · $it" }
            .orEmpty()
        val destinationSearchLabel = stringResource(
            R.string.search_country_with_region,
            selectedRegionSuffix
        )
        val passportStatisticsLabel = stringResource(
            R.string.passport_statistics_with_region,
            selectedRegionSuffix
        )
        val mapBackground = if (darkTheme) Color(0xFF111B23) else Color(0xFFEEF5FA)
        var zoomInRequest by remember { mutableStateOf(0) }
        var zoomOutRequest by remember { mutableStateOf(0) }

        // Shared by the map search and the statistics pill.
        var measuredFilterChipHeightPx by remember { mutableStateOf(0) }
        val overlayDensity = LocalDensity.current
        val lowEndDevice = LocalBorderlyLowEndMode.current

        val mapControlColor = borderlyControlSurfaceColor()
        val mapControlContentColor = borderlyPrimaryContentColor()
        val mapControlSecondaryContentColor = borderlySecondaryContentColor()
        val mapControlBorderColor = borderlyControlRimColor()

        val roundControlRimColor = mapControlBorderColor
        val roundControlColor = if (lowEndDevice) {
            mapControlColor.copy(alpha = 1f)
        } else {
            mapControlColor.copy(alpha = 0.68f)
        }

        // Same soft glass blur as the bottom navigation.
        // Existing surface color/alpha remain the visual tint.
        val mapOverlayHazeState = rememberHazeState()
        val mapOverlayHazeStyle = HazeStyle(
            backgroundColor = MaterialTheme.colorScheme.surface,
            tint = HazeTint(roundControlColor),
            blurRadius = 3.dp,
            noiseFactor = 0f,
            fallbackTint = HazeTint(roundControlColor)
        )

        val searchHeight = if (measuredFilterChipHeightPx > 0) {
            with(overlayDensity) { measuredFilterChipHeightPx.toDp() }
        } else {
            40.dp
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .background(MaterialTheme.colorScheme.background)
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = mapBackground,
                shape = RoundedCornerShape(
                    topStart = 30.dp,
                    topEnd = 30.dp,
                    bottomStart = 26.dp,
                    bottomEnd = 26.dp
                ),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = .72f)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(mapHeight)
                ) {
                    InteractiveWorldMap(
                        map = nativeMap,
                        passport = passport,
                        visaRequirements = visaRequirements,
                        quickFilter = quickFilter,
                        selectedRegion = selectedRegion,
                        selectedCountryIso = selectedCountryIso,
                        onCountrySelected = onCountrySelected,
                        onEmptySpaceSelected = onEmptySpaceSelected,
                        onInteractionChanged = onInteractionChanged,
                        zoomInRequest = zoomInRequest,
                        zoomOutRequest = zoomOutRequest,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(
                                RoundedCornerShape(
                                    topStart = 29.dp,
                                    topEnd = 29.dp
                                )
                            )
                            .hazeSource(state = mapOverlayHazeState)
                    )

                    // The existing filter carousel is now overlaid on the map.
                    // Chip sizes, colors, spacing, scrolling and filter behavior are unchanged.
                    MapQuickFiltersSection(
                        selected = quickFilter,
                        countries = countries,
                        hazeState = mapOverlayHazeState,
                        hazeStyle = mapOverlayHazeStyle,
                        onSelected = onQuickFilterSelected,
                        showHeader = false,
                        onChipHeightMeasured = { heightPx ->
                            if (heightPx > 0 && measuredFilterChipHeightPx != heightPx) {
                                measuredFilterChipHeightPx = heightPx
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(bottom = MapOverlaySpacing)
                    )

                    val searchTopPadding = MapOverlaySpacing
                    val expandTopPadding = searchTopPadding + searchHeight + MapOverlaySpacing
                    val expandInteractionSource = remember { MutableInteractionSource() }
                    val zoomInInteractionSource = remember { MutableInteractionSource() }
                    val zoomOutInteractionSource = remember { MutableInteractionSource() }
                    // Press-down feedback: floating controls must feel alive
                    // the instant the finger lands, not after the tap ends.
                    val expandPressScale = rememberBorderlyPressScale(expandInteractionSource)
                    val zoomInPressScale = rememberBorderlyPressScale(zoomInInteractionSource)
                    val zoomOutPressScale = rememberBorderlyPressScale(zoomOutInteractionSource)

                    // Search now floats directly over the map, using the same translucent
                    // surface treatment as the map controls while keeping its pill shape.
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(
                                start = MapOverlaySpacing,
                                top = searchTopPadding,
                                end = MapOverlaySpacing
                            )
                            .fillMaxWidth()
                            .height(searchHeight)
                            .borderlyAdaptivePillRim(
                                rimColor = roundControlRimColor,
                                solidFraction = 0.13f,
                                fadeFraction = 0.19f
                            )
                            .clip(RoundedCornerShape(50))
                            .hazeEffect(
                                state = mapOverlayHazeState,
                                style = mapOverlayHazeStyle
                            ) {
                                inputScale = HazeInputScale.Auto
                                blurEnabled = !lowEndDevice
                            }
                            .borderlyPressable(onDestinationClick),
                        color = Color.Transparent,
                        shape = RoundedCornerShape(50),
                        border = null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = destinationSearchLabel,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 2.dp),
                                color = mapControlSecondaryContentColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.open_country_list),
                                modifier = Modifier.size(20.dp),
                                tint = mapControlSecondaryContentColor
                            )
                        }
                    }

                    // Draw the control surface directly instead of using Surface/noRippleClick.
                    // This avoids the rectangular off-screen layer that can become visible on
                    // some Samsung renderers while keeping the exact same size and position.
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(
                                top = expandTopPadding,
                                end = MapOverlaySpacing
                            )
                            .size(48.dp)
                            .clip(CircleShape)
                            .hazeEffect(
                                state = mapOverlayHazeState,
                                style = mapOverlayHazeStyle
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
                                // Keep the current solid arc exactly the same, but extend only its fading tails.
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

                                    // Long continuous fade-in. The solid section begins at exactly the same angle as before.
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

                                    // Solid section: same 48° length and same position as in the previous version.
                                    drawArc(
                                        color = base,
                                        startAngle = solidStart,
                                        sweepAngle = solidSweep,
                                        useCenter = false,
                                        topLeft = Offset(borderInset, borderInset),
                                        size = borderSize,
                                        style = stroke
                                    )

                                    // Long continuous fade-out, extending farther into the previously empty sector.
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
                            .graphicsLayer {
                                scaleX = expandPressScale
                                scaleY = expandPressScale
                            }
                            .semantics {
                                role = Role.Button
                                contentDescription = openFullscreenMapDescription
                            }
                            .clickable(
                                interactionSource = expandInteractionSource,
                                indication = null,
                                onClick = onExpand
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        ExpandGlyph(
                            color = mapControlContentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(
                                end = MapOverlaySpacing,
                                // Bottom edge -> filters = MapOverlaySpacing.
                                // Filter top -> minus button bottom = MapOverlaySpacing.
                                // searchHeight is the measured filter-chip height, so this remains
                                // exact at different font/display scales without resizing + / -.
                                bottom = MapOverlaySpacing + searchHeight + MapOverlaySpacing
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .hazeEffect(
                                    state = mapOverlayHazeState,
                                    style = mapOverlayHazeStyle
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
                                    // Keep the current solid arc exactly the same, but extend only its fading tails.
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

                                        // Long continuous fade-in. The solid section begins at exactly the same angle as before.
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

                                        // Solid section: same 48° length and same position as in the previous version.
                                        drawArc(
                                            color = base,
                                            startAngle = solidStart,
                                            sweepAngle = solidSweep,
                                            useCenter = false,
                                            topLeft = Offset(borderInset, borderInset),
                                            size = borderSize,
                                            style = stroke
                                        )

                                        // Long continuous fade-out, extending farther into the previously empty sector.
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
                                .graphicsLayer {
                                    scaleX = zoomInPressScale
                                    scaleY = zoomInPressScale
                                }
                                .semantics {
                                    role = Role.Button
                                    contentDescription = zoomInDescription
                                }
                                .clickable(
                                    interactionSource = zoomInInteractionSource,
                                    indication = null
                                ) { zoomInRequest += 1 },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                color = mapControlContentColor,
                                fontSize = 28.sp,
                                lineHeight = 28.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        // Invisible touch shield: keeps the original 8dp visual gap, but taps
                        // between + and - can no longer fall through to the map/countries.
                        Spacer(
                            modifier = Modifier
                                .width(48.dp)
                                .height(8.dp)
                                .pointerInteropFilter { true }
                        )

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .hazeEffect(
                                    state = mapOverlayHazeState,
                                    style = mapOverlayHazeStyle
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
                                    // Keep the current solid arc exactly the same, but extend only its fading tails.
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

                                        // Long continuous fade-in. The solid section begins at exactly the same angle as before.
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

                                        // Solid section: same 48° length and same position as in the previous version.
                                        drawArc(
                                            color = base,
                                            startAngle = solidStart,
                                            sweepAngle = solidSweep,
                                            useCenter = false,
                                            topLeft = Offset(borderInset, borderInset),
                                            size = borderSize,
                                            style = stroke
                                        )

                                        // Long continuous fade-out, extending farther into the previously empty sector.
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
                                .graphicsLayer {
                                    scaleX = zoomOutPressScale
                                    scaleY = zoomOutPressScale
                                }
                                .semantics {
                                    role = Role.Button
                                    contentDescription = zoomOutDescription
                                }
                                .clickable(
                                    interactionSource = zoomOutInteractionSource,
                                    indication = null
                                ) { zoomOutRequest += 1 },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "−",
                                color = mapControlContentColor,
                                fontSize = 29.sp,
                                lineHeight = 29.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }

            }

            var legendExpanded by rememberSaveable { mutableStateOf(true) }

            Spacer(modifier = Modifier.height(if (compact) 13.dp else 17.dp))

            // Keep the existing open and closed shapes, but use the same
            // simple disclosure algorithm as the passport-documents card.
            val legendRadius = if (legendExpanded) 26.dp else 50.dp
            val legendArrowRotation by animateFloatAsState(
                targetValue = if (legendExpanded) 180f else 0f,
                animationSpec = tween(durationMillis = 180),
                label = "legendArrow"
            )

            Surface(
                modifier = Modifier
                    .padding(horizontal = MapOverlaySpacing)
                    .fillMaxWidth()
                    // The existing rim follows the current open/closed radius.
                    .borderlyRoundedRectRim(
                        rimColor = roundControlRimColor,
                        cornerRadius = legendRadius
                    )
                    .borderlyPressable { legendExpanded = !legendExpanded },
                color = roundControlColor,
                shape = RoundedCornerShape(legendRadius),
                border = null
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(searchHeight)
                            .padding(horizontal = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = passportStatisticsLabel,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 2.dp),
                            color = borderlyPrimaryContentColor(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = if (legendExpanded) {
                                stringResource(R.string.collapse_statistics)
                            } else {
                                stringResource(R.string.expand_statistics)
                            },
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(legendArrowRotation),
                            tint = mapControlSecondaryContentColor
                        )
                    }

                    if (legendExpanded) {
                        VisaLegendGrid(
                            passportName = passport.localizedName(),
                            visaCounts = visaCounts,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = legendHorizontalPadding,
                                    top = if (compact) 13.dp else 15.dp,
                                    end = legendHorizontalPadding,
                                    bottom = if (compact) 26.dp else 28.dp
                                ),
                            compact = compact
                        )
                    }
                }
            }
        }
    }
}

internal fun Modifier.borderlyRoundedRectRim(
    rimColor: Color,
    cornerRadius: Dp,
    drawTop: Boolean = true,
    drawBottom: Boolean = true
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

    /*
     * Expanded statistics uses its OWN trajectory.
     *
     * Upper rim:
     * left side -> upper-left rounding -> full top edge ->
     * enters upper-right rounding.
     *
     * Lower rim:
     * right side -> lower-right rounding -> full bottom edge ->
     * enters lower-left rounding.
     *
     * Nothing is drawn through the middle of the card.
     */
    val cornerEndProgress = 0.58f

    val upperPath = AndroidPath().apply {
        moveTo(left, top + radius)
        arcTo(
            leftTopRect,
            180f,
            90f,
            false
        )
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
        arcTo(
            rightBottomRect,
            0f,
            90f,
            false
        )
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

            // Long smooth fade on both ends, but never long enough to eat
            // the straight visible section of the expanded card.
            val fadeLength = min(
                routeLength * 0.18f,
                70.dp.toPx()
            )
            val solidStart = fadeLength
            val solidEnd = (routeLength - fadeLength)
                .coerceAtLeast(solidStart)
            // Меньше сегментов = дешевле перерисовка анимирующихся рамок
            // (градиент всё равно плавный благодаря smoothStep).
            val fadeSteps = 10

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

        if (drawTop) drawRoute(upperPath)
        if (drawBottom) drawRoute(lowerPath)
    }
}

private data class VisaLegendItem(
    val color: Color,
    val text: String,
    val count: Int = 0,
    val showCount: Boolean = true
)

@Composable
private fun VisaType.mapLegendTitle(): String = when (this) {
    VisaType.FREEDOM -> stringResource(R.string.map_legend_freedom_short)
    VisaType.SPECIAL_PERMIT -> stringResource(R.string.map_legend_special_permit_short)
    VisaType.MIXED_REQUIREMENTS -> stringResource(R.string.map_legend_mixed_requirements_short)
    else -> localizedTitle()
}

@Composable
internal fun VisaLegendGrid(
    passportName: String,
    visaCounts: Map<VisaType, Int>,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val freedomCount = visaCounts[VisaType.FREEDOM] ?: 0

    val legendItems = buildList {
        add(
            VisaLegendItem(
                color = HomeCountry,
                text = passportName,
                showCount = false
            )
        )
        if (freedomCount > 0) {
            add(
                VisaLegendItem(
                    color = Freedom,
                    text = VisaType.FREEDOM.mapLegendTitle(),
                    count = freedomCount
                )
            )
        }
        add(
            VisaLegendItem(
                color = VisaFree,
                text = VisaType.VISA_FREE.mapLegendTitle(),
                count = visaCounts[VisaType.VISA_FREE] ?: 0
            )
        )
        add(
            VisaLegendItem(
                color = Eta,
                text = VisaType.ETA.mapLegendTitle(),
                count = visaCounts[VisaType.ETA] ?: 0
            )
        )
        add(
            VisaLegendItem(
                color = EVisa,
                text = VisaType.E_VISA.mapLegendTitle(),
                count = visaCounts[VisaType.E_VISA] ?: 0
            )
        )
        add(
            VisaLegendItem(
                color = VisaOnArrival,
                text = VisaType.VISA_ON_ARRIVAL.mapLegendTitle(),
                count = visaCounts[VisaType.VISA_ON_ARRIVAL] ?: 0
            )
        )
        add(
            VisaLegendItem(
                color = VisaRequired,
                text = VisaType.VISA_REQUIRED.mapLegendTitle(),
                count = visaCounts[VisaType.VISA_REQUIRED] ?: 0
            )
        )
        if ((visaCounts[VisaType.ENTRY_RESTRICTED] ?: 0) > 0) {
            add(
                VisaLegendItem(
                    color = EntryRestricted,
                    text = VisaType.ENTRY_RESTRICTED.mapLegendTitle(),
                    count = visaCounts[VisaType.ENTRY_RESTRICTED] ?: 0
                )
            )
        }
        if ((visaCounts[VisaType.SPECIAL_PERMIT] ?: 0) > 0) {
            add(
                VisaLegendItem(
                    color = SpecialPermit,
                    text = VisaType.SPECIAL_PERMIT.mapLegendTitle(),
                    count = visaCounts[VisaType.SPECIAL_PERMIT] ?: 0
                )
            )
        }
        if ((visaCounts[VisaType.MIXED_REQUIREMENTS] ?: 0) > 0) {
            add(
                VisaLegendItem(
                    color = MixedRequirements,
                    text = VisaType.MIXED_REQUIREMENTS.mapLegendTitle(),
                    count = visaCounts[VisaType.MIXED_REQUIREMENTS] ?: 0
                )
            )
        }
        if ((visaCounts[VisaType.NO_DATA] ?: 0) > 0) {
            add(
                VisaLegendItem(
                    color = NoVisaData,
                    text = VisaType.NO_DATA.mapLegendTitle(),
                    count = visaCounts[VisaType.NO_DATA] ?: 0
                )
            )
        }
    }

    BoxWithConstraints(modifier = modifier) {
        // Two columns keep the category count visible on phones without
        // reducing the accessible 12sp text. Wide layouts retain three.
        val itemsPerRow = if (maxWidth < 480.dp) 2 else 3
        Column(modifier = Modifier.fillMaxWidth()) {
            legendItems.chunked(itemsPerRow).forEachIndexed { rowIndex, rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (rowIndex > 0) Modifier.padding(top = 10.dp)
                            else Modifier
                        ),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowItems.forEach { item ->
                        Legend(
                            color = item.color,
                            text = item.text,
                            count = item.count,
                            modifier = Modifier.weight(1f),
                            compact = compact,
                            showCount = item.showCount
                        )
                    }
                    repeat(itemsPerRow - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
internal fun Legend(
    color: Color,
    text: String,
    count: Int,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    showCount: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 10.dp else 12.dp)
                .background(color, RoundedCornerShape(3.dp))
        )

        Text(
            text = if (showCount) {
                buildAnnotatedString {
                    append(text)
                    append(" ")
                    pushStyle(SpanStyle(fontWeight = FontWeight.SemiBold))
                    append(count.toString())
                    pop()
                }
            } else {
                buildAnnotatedString { append(text) }
            },
            modifier = Modifier.padding(start = if (compact) 4.dp else 6.dp),
            color = borderlyPrimaryContentColor(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FullScreenWorldMap(
    nativeMap: NativeMapData?,
    passport: Passport,
    visaRequirements: Map<Int, VisaRequirement>,
    entryGuideDatabase: EntryGuideDatabase,
    entryRequirementDatabase: EntryRequirementDatabase,
    visaCounts: Map<VisaType, Int>,
    dataSource: String,
    dataSourceUrl: String,
    dataUpdated: String,
    dataOrigin: VisaDatabaseOrigin,
    dataVersion: Int,
    dataLastCheckedAt: Long,
    quickFilter: MapVisaQuickFilter,
    selectedRegion: PassportRegion? = null,
    selectedCountryIso: Int?,
    onCountrySelected: (Int, String, String) -> Unit,
    onEmptySpaceSelected: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val displayLocale = LocalConfiguration.current.locales[0]
    var fullScreenDetailsCountry by remember { mutableStateOf<CountryInfo?>(null) }
    var localSelectedCountryIso by remember(selectedCountryIso) {
        mutableStateOf(selectedCountryIso)
    }
    var fullScreenZoomInRequest by remember { mutableStateOf(0) }
    var fullScreenZoomOutRequest by remember { mutableStateOf(0) }
    val fullScreenDetailsSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )
    val fullScreenControlColor = borderlyControlSurfaceColor()
    val fullScreenControlRimColor = borderlyControlRimColor()
    val fullScreenPrimaryContentColor = borderlyPrimaryContentColor()
    val zoomInDescription = stringResource(R.string.zoom_in)
    val zoomOutDescription = stringResource(R.string.zoom_out)
    val closeDescription = stringResource(R.string.close)

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                        .padding(horizontal = 21.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.opportunity_map),
                            color = fullScreenPrimaryContentColor,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.passport_label, passport.localizedName()),
                            modifier = Modifier.padding(top = 2.dp),
                            color = borderlySecondaryContentColor(),
                            fontSize = 12.sp
                        )
                    }
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .borderlyPressable(onClose)
                            .semantics {
                                role = Role.Button
                                contentDescription = closeDescription
                            },
                        color = fullScreenControlColor,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, fullScreenControlRimColor)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "×", color = fullScreenPrimaryContentColor, fontSize = 26.sp)
                        }
                    }
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    val compact = maxWidth < 360.dp

                    InteractiveWorldMap(
                        map = nativeMap,
                        passport = passport,
                        visaRequirements = visaRequirements,
                        quickFilter = quickFilter,
                        selectedRegion = selectedRegion,
                        selectedCountryIso = localSelectedCountryIso,
                        onCountrySelected = { countryIso, name, flag ->
                            localSelectedCountryIso = countryIso
                            val country = mapCountryInfo(
                                context = context,
                                countryIso = countryIso,
                                name = name,
                                flag = flag,
                                passport = passport,
                                requirements = visaRequirements,
                                entryGuideDatabase = entryGuideDatabase,
                                entryRequirementDatabase = entryRequirementDatabase
                            )
                            fullScreenDetailsCountry = country
                            onCountrySelected(countryIso, name, flag)
                        },
                        onEmptySpaceSelected = {
                            localSelectedCountryIso = null
                            onEmptySpaceSelected()
                        },
                        zoomInRequest = fullScreenZoomInRequest,
                        zoomOutRequest = fullScreenZoomOutRequest,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Same zoom controls as on the main map: separate translucent buttons,
                    // 1.60x button zoom with 210ms FastOutSlowIn easing handled by InteractiveWorldMap,
                    // plus an invisible touch shield in the 8dp gap so a missed tap cannot open a country.
                    val mapControlColor = fullScreenControlColor
                    val mapControlBorderColor = fullScreenControlRimColor
                    val zoomInInteractionSource = remember { MutableInteractionSource() }
                    val zoomOutInteractionSource = remember { MutableInteractionSource() }
                    val fullScreenZoomInScale = rememberBorderlyPressScale(zoomInInteractionSource)
                    val fullScreenZoomOutScale = rememberBorderlyPressScale(zoomOutInteractionSource)

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(
                                end = if (compact) 13.dp else 21.dp,
                                bottom = if (compact) 14.dp else 18.dp
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .drawBehind {
                                    val radius = size.minDimension / 2f
                                    drawCircle(
                                        color = mapControlColor,
                                        radius = radius
                                    )
                                    drawCircle(
                                        color = mapControlBorderColor,
                                        radius = radius - 0.5.dp.toPx(),
                                        style = Stroke(width = 1.dp.toPx())
                                    )
                                }
                                .graphicsLayer {
                                    scaleX = fullScreenZoomInScale
                                    scaleY = fullScreenZoomInScale
                                }
                                .semantics {
                                    role = Role.Button
                                    contentDescription = zoomInDescription
                                }
                                .clickable(
                                    interactionSource = zoomInInteractionSource,
                                    indication = null
                                ) { fullScreenZoomInRequest += 1 },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                color = fullScreenPrimaryContentColor,
                                fontSize = 28.sp,
                                lineHeight = 28.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        Spacer(
                            modifier = Modifier
                                .width(48.dp)
                                .height(8.dp)
                                .pointerInteropFilter { true }
                        )

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .drawBehind {
                                    val radius = size.minDimension / 2f
                                    drawCircle(
                                        color = mapControlColor,
                                        radius = radius
                                    )
                                    drawCircle(
                                        color = mapControlBorderColor,
                                        radius = radius - 0.5.dp.toPx(),
                                        style = Stroke(width = 1.dp.toPx())
                                    )
                                }
                                .graphicsLayer {
                                    scaleX = fullScreenZoomOutScale
                                    scaleY = fullScreenZoomOutScale
                                }
                                .semantics {
                                    role = Role.Button
                                    contentDescription = zoomOutDescription
                                }
                                .clickable(
                                    interactionSource = zoomOutInteractionSource,
                                    indication = null
                                ) { fullScreenZoomOutRequest += 1 },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "−",
                                color = fullScreenPrimaryContentColor,
                                fontSize = 29.sp,
                                lineHeight = 29.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    val compact = maxWidth < 360.dp
                    VisaLegendGrid(
                        passportName = passport.localizedName(),
                        visaCounts = visaCounts,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = if (compact) 12.dp else 22.dp,
                                vertical = 14.dp
                            ),
                        compact = compact
                    )
                }
                Text(
                    text = if (dataLastCheckedAt > 0L) {
                        stringResource(
                            R.string.database_checked,
                            formatLastSuccessfulCheckForUi(
                                dataLastCheckedAt,
                                displayLocale
                            )
                        )
                    } else {
                        stringResource(
                            R.string.data_changed,
                            formatDataDateForUi(dataUpdated, displayLocale)
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .navigationBarsPadding()
                        .padding(bottom = 12.dp),
                    color = borderlySecondaryContentColor(),
                    fontSize = 10.sp
                )
            }
        }
    }

    fullScreenDetailsCountry?.let { country ->
        ModalBottomSheet(
            modifier = Modifier.fillMaxHeight(),
            onDismissRequest = { fullScreenDetailsCountry = null },
            sheetState = fullScreenDetailsSheetState,
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
                CountryDetailsSheet(
                    country = country,
                    passport = passport,
                    dataSource = dataSource,
                    dataSourceUrl = dataSourceUrl,
                    dataUpdated = dataUpdated,
                    dataOrigin = dataOrigin,
                    dataVersion = dataVersion,
                    dataLastCheckedAt = dataLastCheckedAt,
                    onChooseAnother = {
                        fullScreenDetailsCountry = null
                    },
                    onClose = {
                        fullScreenDetailsCountry = null
                    }
                )
            }
        }

        LaunchedEffect(country.isoNumeric) {
            if (fullScreenDetailsSheetState.isVisible) {
                fullScreenDetailsSheetState.partialExpand()
            }
        }
    }

}

@Composable
internal fun MapQuickFiltersSection(
    selected: MapVisaQuickFilter,
    countries: List<CountryInfo>,
    hazeState: HazeState,
    hazeStyle: HazeStyle,
    onSelected: (MapVisaQuickFilter) -> Unit,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    onChipHeightMeasured: (Int) -> Unit = {}
) {
    val selectedCount = remember(selected, countries) {
        if (selected == MapVisaQuickFilter.ALL) {
            countries.size
        } else {
            countries.count { selected.matches(it.visaType) }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (showHeader) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.show_on_map),
                    color = borderlyPrimaryContentColor(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                if (selected != MapVisaQuickFilter.ALL) {
                    Text(
                        text = stringResource(R.string.selected_countries_count, selectedCount),
                        color = borderlySecondaryContentColor(),
                        fontSize = 11.sp
                    )
                }
            }
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (showHeader) 11.dp else 0.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val visibleFilters = MapVisaQuickFilter.entries.filter { filter ->
                filter == MapVisaQuickFilter.ALL ||
                        countries.any { country -> filter.matches(country.visaType) }
            }

            items(visibleFilters, key = { it.name }) { filter ->
                MapQuickFilterChip(
                    filter = filter,
                    selected = filter == selected,
                    hazeState = hazeState,
                    hazeStyle = hazeStyle,
                    onClick = {
                        onSelected(
                            if (filter == selected && filter != MapVisaQuickFilter.ALL) {
                                MapVisaQuickFilter.ALL
                            } else {
                                filter
                            }
                        )
                    },
                    onMeasuredHeight = onChipHeightMeasured
                )
            }
        }
    }
}

@Composable
internal fun MapQuickFilterChip(
    filter: MapVisaQuickFilter,
    selected: Boolean,
    hazeState: HazeState,
    hazeStyle: HazeStyle,
    onClick: () -> Unit,
    onMeasuredHeight: (Int) -> Unit = {}
) {
    val lowEndDevice = LocalBorderlyLowEndMode.current
    val rimBaseColor = borderlyControlRimColor()

    // Selection is a smooth state change, not a swap: surface, border, text
    // and dot all ease to their new colors together in ~180ms.
    val chipSurfaceColor by animateColorAsState(
        targetValue = if (selected) borderlySelectedControlColor() else Color.Transparent,
        animationSpec = tween(180, easing = BorderlyStrongEaseOut),
        label = "filterChipSurface"
    )
    val chipBorderColor by animateColorAsState(
        targetValue = if (selected) borderlySelectedControlColor() else Color.Transparent,
        animationSpec = tween(180, easing = BorderlyStrongEaseOut),
        label = "filterChipBorder"
    )
    val chipTextColor by animateColorAsState(
        targetValue = if (selected) {
            borderlySelectedContentColor()
        } else {
            borderlyPrimaryContentColor()
        },
        animationSpec = tween(180, easing = BorderlyStrongEaseOut),
        label = "filterChipText"
    )

    Surface(
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                onMeasuredHeight(coordinates.size.height)
            }
            // IMPORTANT:
            // Filters, passport selector and "Поиск страны" now use
            // literally the SAME rim renderer, not three copies of similar math.
            .borderlyAdaptivePillRim(
                rimColor = rimBaseColor,
                solidFraction = 0.13f,
                fadeFraction = 0.19f,
                enabled = !selected
            )
            .then(
                if (!selected) {
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .hazeEffect(
                            state = hazeState,
                            style = hazeStyle
                        ) {
                            inputScale = HazeInputScale.Auto
                            blurEnabled = !lowEndDevice
                        }
                } else {
                    Modifier
                }
            )
            .borderlyPressable(onClick),
        color = chipSurfaceColor,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, chipBorderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            filter.accentColor?.let { accent ->
                val chipDotColor by animateColorAsState(
                    targetValue = if (selected) {
                        borderlySelectedContentColor().copy(alpha = .92f)
                    } else {
                        accent
                    },
                    animationSpec = tween(180, easing = BorderlyStrongEaseOut),
                    label = "filterChipDot"
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(chipDotColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(7.dp))
            }
            Text(
                text = filter.localizedTitle(),
                color = chipTextColor,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}

@Composable
internal fun RegionAccessSection(
    nativeMap: NativeMapData? = null,
    regionCounts: Map<PassportRegion, Int>,
    activeFilter: MapVisaQuickFilter,
    selectedRegion: PassportRegion? = null,
    onRegionClick: (PassportRegion) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        AnimatedContent(
            targetState = activeFilter,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith
                    fadeOut(animationSpec = tween(120))
            },
            label = "regionSectionHeader"
        ) { filter ->
            Column {
                Text(
                    text = stringResource(
                        R.string.by_region,
                        if (filter == MapVisaQuickFilter.ALL) {
                            stringResource(R.string.all_countries)
                        } else {
                            filter.localizedTitle()
                        }
                    ),
                    color = borderlyPrimaryContentColor(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (filter == MapVisaQuickFilter.ALL) {
                        stringResource(R.string.region_country_count_description)
                    } else {
                        filter.localizedTitle()
                    },
                    modifier = Modifier.padding(top = 3.dp),
                    color = borderlySecondaryContentColor(),
                    fontSize = 11.sp
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RegionAccessCard(
                nativeMap = nativeMap,
                region = PassportRegion.EUROPE,
                count = regionCounts[PassportRegion.EUROPE] ?: 0,
                selected = selectedRegion == PassportRegion.EUROPE,
                modifier = Modifier.weight(1f),
                onClick = onRegionClick
            )
            RegionAccessCard(
                nativeMap = nativeMap,
                region = PassportRegion.ASIA,
                count = regionCounts[PassportRegion.ASIA] ?: 0,
                selected = selectedRegion == PassportRegion.ASIA,
                modifier = Modifier.weight(1f),
                onClick = onRegionClick
            )
            RegionAccessCard(
                nativeMap = nativeMap,
                region = PassportRegion.AMERICAS,
                count = regionCounts[PassportRegion.AMERICAS] ?: 0,
                selected = selectedRegion == PassportRegion.AMERICAS,
                modifier = Modifier.weight(1f),
                onClick = onRegionClick
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RegionAccessCard(
                nativeMap = nativeMap,
                region = PassportRegion.AFRICA,
                count = regionCounts[PassportRegion.AFRICA] ?: 0,
                selected = selectedRegion == PassportRegion.AFRICA,
                modifier = Modifier.weight(1f),
                onClick = onRegionClick
            )
            RegionAccessCard(
                nativeMap = nativeMap,
                region = PassportRegion.OCEANIA,
                count = regionCounts[PassportRegion.OCEANIA] ?: 0,
                selected = selectedRegion == PassportRegion.OCEANIA,
                modifier = Modifier.weight(1f),
                onClick = onRegionClick
            )
        }
    }
}

@Composable
internal fun RegionAccessCard(
    nativeMap: NativeMapData?,
    region: PassportRegion,
    count: Int,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (PassportRegion) -> Unit
) {
    var tapFeedback by remember { mutableStateOf(false) }
    val tapFeedbackScope = rememberCoroutineScope()
    // Respond on pointer-down: the card reacts the instant the finger lands,
    // while the existing short flash still confirms the completed tap.
    val regionInteractionSource = remember { MutableInteractionSource() }
    val regionPressed by regionInteractionSource.collectIsPressedAsState()
    val regionPressScale = rememberBorderlyPressScale(regionInteractionSource)

    val regionCardCornerRadius = 26.dp
    val regionCardRimColor = borderlyControlRimColor()
    val regionCardColor = borderlyControlSurfaceColor()
    val selectedRegionFill = borderlyMutedControlColor()
    val selectedRegionStroke = borderlySecondaryContentColor().copy(alpha = 0.42f)
    val tapRegionFill = borderlyPrimaryContentColor().copy(alpha = 0.10f)

    // Выбор — плавное перетекание, а не мгновенная замена заливки.
    val regionHighlightFill by animateColorAsState(
        targetValue = when {
            regionPressed || tapFeedback -> tapRegionFill
            selected -> selectedRegionFill
            else -> Color.Transparent
        },
        animationSpec = tween(160, easing = BorderlyStrongEaseOut),
        label = "regionCardFill"
    )
    val regionHighlightStroke by animateColorAsState(
        targetValue = if (selected && !regionPressed && !tapFeedback) {
            selectedRegionStroke
        } else {
            Color.Transparent
        },
        animationSpec = tween(160, easing = BorderlyStrongEaseOut),
        label = "regionCardStroke"
    )

    Surface(
        modifier = modifier
            .borderlyRoundedRectRim(
                rimColor = regionCardRimColor,
                cornerRadius = regionCardCornerRadius
            )
            .graphicsLayer {
                scaleX = regionPressScale
                scaleY = regionPressScale
            }
            .clickable(
                interactionSource = regionInteractionSource,
                indication = null
            ) {
                // Short confirmation flash after the tap completes; the
                // press-down response itself is handled above via
                // regionPressed + regionPressScale.
                tapFeedback = true
                tapFeedbackScope.launch {
                    delay(110)
                    tapFeedback = false
                }
                onClick(region)
            },
        color = regionCardColor,
        shape = RoundedCornerShape(regionCardCornerRadius),
        border = null
    ) {
        Box(
            modifier = Modifier.drawBehind {
                val inset = 4.dp.toPx()
                val innerWidth = (size.width - inset * 2f).coerceAtLeast(0f)
                val innerHeight = (size.height - inset * 2f).coerceAtLeast(0f)
                drawRoundRect(
                    color = regionHighlightFill,
                    topLeft = Offset(inset, inset),
                    size = Size(innerWidth, innerHeight),
                    cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx())
                )
                drawRoundRect(
                    color = regionHighlightStroke,
                    topLeft = Offset(inset, inset),
                    size = Size(innerWidth, innerHeight),
                    cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
                    style = Stroke(width = 1.35.dp.toPx())
                )
            }
        ) {
            // Minimal globe: the card itself keeps exactly the same size/style.
            // Only the decorative region visual changes.
            val globeSize = when (region) {
                PassportRegion.AFRICA -> 52.dp
                PassportRegion.OCEANIA -> 52.dp
                PassportRegion.ASIA -> 50.dp
                else -> 48.dp
            }

            RegionGlobeWatermark(
                nativeMap = nativeMap,
                region = region,
                sizeDp = globeSize,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .size(globeSize)
            )

            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Text(
                    text = count.toString(),
                    color = borderlyPrimaryContentColor(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = region.localizedTitle(),
                    modifier = Modifier.padding(top = 2.dp),
                    color = borderlySecondaryContentColor(),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private data class RegionGlobeCamera(
    val centerX: Float,
    val centerY: Float,
    val zoom: Float
)

@Composable
private fun RegionGlobeWatermark(
    nativeMap: NativeMapData?,
    region: PassportRegion,
    sizeDp: Dp,
    modifier: Modifier = Modifier
) {
    if (nativeMap == null) return

    val density = LocalDensity.current
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val pixelSize = with(density) { sizeDp.toPx() }.roundToInt().coerceAtLeast(1)

    // The globe is rendered once into a small bitmap and only blitted after
    // that. Previously every filter tap replayed thousands of path draws on
    // the main thread, which froze weak devices for a moment.
    val cachedImage = remember(nativeMap, region, darkTheme, pixelSize) {
        renderRegionGlobeImage(
            nativeMap = nativeMap,
            region = region,
            darkTheme = darkTheme,
            sizePx = pixelSize
        )
    }

    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        drawImage(
            image = cachedImage,
            dstSize = IntSize(
                size.width.roundToInt(),
                size.height.roundToInt()
            ),
            dstOffset = IntOffset.Zero
        )
    }
}

private fun renderRegionGlobeImage(
    nativeMap: NativeMapData,
    region: PassportRegion,
    darkTheme: Boolean,
    sizePx: Int
): ImageBitmap {
    val diameter = sizePx.toFloat()
    val radius = diameter / 2f
    val centerX = diameter / 2f
    val centerY = diameter / 2f

    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    // Very subtle neutral sphere gradient. The selected region remains the only dark accent.
    val spherePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = android.graphics.RadialGradient(
            centerX - radius * .22f,
            centerY - radius * .28f,
            radius * 1.18f,
            if (darkTheme) {
                intArrayOf(
                    AndroidColor.argb(250, 52, 58, 64),
                    AndroidColor.argb(245, 42, 48, 53),
                    AndroidColor.argb(230, 32, 37, 42)
                )
            } else {
                intArrayOf(
                    AndroidColor.argb(250, 255, 255, 255),
                    AndroidColor.argb(245, 245, 246, 247),
                    AndroidColor.argb(230, 233, 235, 238)
                )
            },
            floatArrayOf(0f, 0.5f, 1f),
            android.graphics.Shader.TileMode.CLAMP
        )
    }
    canvas.drawCircle(centerX, centerY, radius, spherePaint)

    // Camera positions make each globe face the corresponding region,
    // while still leaving the rest of the world visible in light grey.
    val camera = when (region) {
        PassportRegion.EUROPE -> RegionGlobeCamera(.502f, .388f, 1.46f)
        PassportRegion.ASIA -> RegionGlobeCamera(.725f, .405f, 1.12f)
        PassportRegion.AMERICAS -> RegionGlobeCamera(.205f, .500f, 1.02f)
        PassportRegion.AFRICA -> RegionGlobeCamera(.515f, .575f, 1.17f)
        PassportRegion.OCEANIA -> RegionGlobeCamera(.825f, .705f, 1.24f)
    }

    val worldPath = AndroidPath().apply {
        nativeMap.countries.forEach { country -> addPath(country.path) }
    }
    val selectedPath = AndroidPath().apply {
        nativeMap.countries.forEach { country ->
            val selected = nativeCountryBelongsToRegionGlobe(country.id, region)
            if (selected) addPath(country.path)
        }
    }
    val russiaPath = nativeMap.countries.firstOrNull { it.id == 643 }?.path

    val worldScale = (diameter / nativeMap.height) * camera.zoom
    val mapCenterX = nativeMap.width * camera.centerX
    val mapCenterY = nativeMap.height * camera.centerY
    val tx = centerX - mapCenterX * worldScale
    val ty = centerY - mapCenterY * worldScale

    val otherFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = if (darkTheme) {
            AndroidColor.argb(210, 71, 78, 84)
        } else {
            AndroidColor.argb(182, 211, 214, 218)
        }
    }
    val selectedFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = if (darkTheme) {
            AndroidColor.argb(235, 176, 183, 190)
        } else {
            AndroidColor.argb(224, 113, 118, 125)
        }
    }
    val countryStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (0.45f / worldScale).coerceAtLeast(0.12f)
        color = if (darkTheme) {
            AndroidColor.argb(78, 25, 29, 33)
        } else {
            AndroidColor.argb(92, 247, 248, 249)
        }
    }

    canvas.save()
    canvas.clipPath(
        AndroidPath().apply {
            addCircle(centerX, centerY, radius * .955f, AndroidPath.Direction.CW)
        }
    )
    canvas.translate(tx, ty)
    canvas.scale(worldScale, worldScale)

    // Draw the world three times horizontally so camera positions near the dateline
    // (Asia/Oceania/Americas) wrap naturally instead of showing an empty edge.
    val repeats = floatArrayOf(-nativeMap.width, 0f, nativeMap.width)
    val europeRussiaClipRight = nativeMap.width * 0.675f
    repeats.forEach { xShift ->
        canvas.save()
        canvas.translate(xShift, 0f)
        canvas.drawPath(worldPath, otherFill)
        canvas.drawPath(selectedPath, selectedFill)
        if (region == PassportRegion.EUROPE && russiaPath != null) {
            canvas.save()
            canvas.clipRect(0f, 0f, europeRussiaClipRight, nativeMap.height)
            canvas.drawPath(russiaPath, selectedFill)
            canvas.restore()
        }
        canvas.drawPath(worldPath, countryStroke)
        canvas.restore()
    }
    canvas.restore()

    return bitmap.asImageBitmap()
}

private fun nativeCountryBelongsToPassportRegion(
    isoNumeric: Int,
    region: PassportRegion
): Boolean = when (region) {
    PassportRegion.EUROPE -> isoNumeric in EuropeanPassportIds
    PassportRegion.ASIA -> isoNumeric in AsianPassportIds
    PassportRegion.AMERICAS -> isoNumeric in AmericanPassportIds
    PassportRegion.AFRICA -> isoNumeric in AfricanPassportIds
    PassportRegion.OCEANIA -> isoNumeric in OceanianPassportIds
}

private val EuropeanRegionGlobeIds = EuropeanPassportIds - setOf(643)

private fun nativeCountryBelongsToRegionGlobe(
    isoNumeric: Int,
    region: PassportRegion
): Boolean = when (region) {
    PassportRegion.EUROPE -> isoNumeric in EuropeanRegionGlobeIds
    PassportRegion.ASIA -> isoNumeric in AsianPassportIds || isoNumeric == 643
    PassportRegion.AMERICAS -> isoNumeric in AmericanPassportIds
    PassportRegion.AFRICA -> isoNumeric in AfricanPassportIds
    PassportRegion.OCEANIA -> isoNumeric in OceanianPassportIds
}
