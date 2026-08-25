package com.example.borderly

// BORDERLY_COUNTRY_DETAILS_CURRENT_CARD_DESIGN_2026_08_19

import android.graphics.Paint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

private const val BorderlyIssueReportEmail = "grachevdmtr@gmail.com"

@Composable
internal fun CountryDetailsSheet(
    country: CountryInfo,
    passport: Passport,
    dataSource: String,
    dataSourceUrl: String,
    dataUpdated: String,
    dataOrigin: VisaDatabaseOrigin,
    dataVersion: Int,
    dataLastCheckedAt: Long,
    onChooseAnother: () -> Unit,
    onClose: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var applicationDocumentsExpanded by remember(
        country.isoNumeric,
        passport.isoNumeric
    ) { mutableStateOf(false) }
    var statusExplanationExpanded by remember(
        country.isoNumeric,
        passport.isoNumeric
    ) { mutableStateOf(false) }
    val detailsListState = rememberLazyListState()
    val effectiveSource = country.source ?: dataSource
    val effectiveSourceUrl = country.sourceUrl ?: dataSourceUrl
    val effectiveUpdated = country.sourceUpdated ?: dataUpdated
    val effectiveSourceType = country.sourceType.takeUnless {
        it == VisaSourceType.UNKNOWN
    } ?: VisaSourceType.DATASET
    val readableDataDate = remember(effectiveUpdated) {
        formatDataDateForUi(effectiveUpdated)
    }
    val lastCheckText = remember(dataLastCheckedAt) {
        formatLastSuccessfulCheckForUi(dataLastCheckedAt)
    }
    val databaseLabel = remember(dataOrigin, dataVersion) {
        buildString {
            append("Borderly · ")
            append(dataOrigin.label)
            if (dataVersion > 0) {
                append(" · версия ")
                append(dataVersion)
            }
        }
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        val compact = maxWidth < 360.dp
        val horizontalPadding = if (compact) 14.dp else 22.dp

        LazyColumn(
            state = detailsListState,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .keepSheetStillUntilNextGesture(detailsListState),
            contentPadding = PaddingValues(
                start = horizontalPadding,
                end = horizontalPadding,
                bottom = 16.dp
            ),
            overscrollEffect = null
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = country.flag, fontSize = if (compact) 37.sp else 43.sp)
                    Column(
                        modifier = Modifier
                            .padding(start = if (compact) 10.dp else 14.dp)
                            .weight(1f)
                    ) {
                    Text(
                        text = country.name,
                        color = borderlyDetailsPrimaryContentColor(),
                        fontSize = if (compact) 25.sp else 29.sp,
                        lineHeight = if (compact) 29.sp else 33.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Паспорт: ${passport.name}",
                        color = borderlyDetailsSecondaryContentColor(),
                        fontSize = 13.sp
                    )
                    }
                }
            }

            item {
            BorderlyDetailsCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                backgroundColor = country.visaType.color.copy(alpha = .14f),
                rimColor = country.visaType.color.copy(alpha = .44f)
            ) {
                Column(modifier = Modifier.padding(if (compact) 15.dp else 18.dp)) {
                    Text(
                        text = country.visaType.title,
                        color = country.visaType.color,
                        fontSize = if (compact) 20.sp else 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = country.stay,
                        modifier = Modifier.padding(top = 4.dp),
                        color = borderlyDetailsPrimaryContentColor(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

            if (country.entryRequirements.isNotEmpty()) {
                item { SheetSectionTitle("Дополнительные требования") }

                items(country.entryRequirements) { requirement ->
                    EntryRequirementCard(
                        requirement = requirement,
                        onOpenSource = { url ->
                            if (url.startsWith("https://")) {
                                runCatching { uriHandler.openUri(url) }
                            }
                        }
                    )
                }
            }

            if (
                country.visaType != VisaType.HOME_COUNTRY &&
                country.entryConditions.isNotEmpty()
            ) {
                item { SheetSectionTitle("Условия въезда") }

                item {
                    EntryConditionsCard(
                        conditions = country.entryConditions,
                        accentColor = country.visaType.color
                    )
                }
            }

            if (country.beforeTrip.isNotEmpty()) {
                item { SheetSectionTitle("Перед поездкой") }

                items(country.beforeTrip) { step ->
                    RequirementRow(text = step)
                }
            }

            if (country.entryGuide != null) {
                item { SheetSectionTitle("Оформление") }

                item {
                    EntryGuideSummaryCard(
                        guide = country.entryGuide
                    )
                }

                if (country.entryGuide.steps.isNotEmpty()) {
                    item { SheetSectionTitle("Что нужно сделать") }

                    item {
                        ApplicationStepsCard(
                            steps = country.entryGuide.steps
                        )
                    }
                }
            }

            if (
                country.applicationDocumentsTitle != null &&
                country.applicationDocuments.isNotEmpty()
            ) {
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    ExpandableApplicationDocumentsCard(
                        title = country.applicationDocumentsTitle,
                        documents = country.applicationDocuments,
                        note = country.applicationDocumentsNote,
                        expanded = applicationDocumentsExpanded,
                        onToggle = {
                            applicationDocumentsExpanded =
                                !applicationDocumentsExpanded
                        }
                    )
                }
            }

            if (
                country.entryGuide != null &&
                country.entryGuide.links.isNotEmpty()
            ) {
                item { SheetSectionTitle("Официальные сервисы") }

                item {
                    OfficialEntryLinksCard(
                        guide = country.entryGuide,
                        onOpen = { url ->
                            if (url.startsWith("https://")) {
                                runCatching { uriHandler.openUri(url) }
                            }
                        }
                    )
                }
            }

            if (country.showPassportNote) {
                item { SheetSectionTitle("Для вашего паспорта") }

                item {
                    InformationCard(text = country.passportNote)
                }
            }

            if (country.showStatusExplanation) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    ExpandableExplanationCard(
                        title = "Почему такой статус?",
                        text = country.statusExplanation,
                        expanded = statusExplanationExpanded,
                        onToggle = {
                            statusExplanationExpanded =
                                !statusExplanationExpanded
                        }
                    )
                }
            }

            if (country.warning != null) {
                item { SheetSectionTitle("Важно") }

                item {
                    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
                    BorderlyDetailsCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = if (darkTheme) {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                        } else {
                            Color(0xFFFFF4E6).copy(alpha = 0.92f)
                        }
                    ) {
                        Text(
                            text = country.warning,
                            modifier = Modifier.padding(15.dp),
                            color = if (darkTheme) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                Color(0xFF6D4A22)
                            },
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            item { SheetSectionTitle("Подтверждение статуса") }

            item {
                SourceInformationCard(
                    source = effectiveSource,
                    sourceUrl = effectiveSourceUrl,
                    dataChangedDate = readableDataDate,
                    lastCheckText = lastCheckText,
                    databaseLabel = databaseLabel,
                    sourceType = effectiveSourceType,
                    sourceDescription = country.sourceDescription,
                    sourceLicense = country.sourceLicense,
                    specificRuleSource = country.sourceIsRuleSpecific,
                    onReportIssue = {
                        val uri = buildCountryIssueReportMailtoUri(
                            country.name,
                            passport.name,
                            country.visaType.title,
                            country.stay,
                            effectiveSource,
                            effectiveSourceUrl,
                            readableDataDate,
                            lastCheckText,
                            databaseLabel,
                            "Не указан",
                            "",
                            null
                        )
                        runCatching { uriHandler.openUri(uri) }
                    },
                    onOpenSource = {
                        if (effectiveSourceUrl.startsWith("https://")) {
                            runCatching { uriHandler.openUri(effectiveSourceUrl) }
                        }
                    }
                )
            }

            item {
            Text(
                text = "Справочные данные • перепроверьте условия перед поездкой",
                modifier = Modifier.padding(top = 14.dp),
                color = borderlyDetailsSecondaryContentColor(),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

            item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BorderlyDetailsActionButton(
                    text = "Другая страна",
                    modifier = Modifier.weight(1f),
                    compact = compact,
                    filled = false,
                    onClick = onChooseAnother
                )
                BorderlyDetailsActionButton(
                    text = "Закрыть",
                    modifier = Modifier.weight(1f),
                    compact = compact,
                    filled = true,
                    onClick = onClose
                )
            }
        }
    }
}

}

private fun buildCountryIssueReportMailtoUri(
    countryName: String,
    passportName: String,
    status: String,
    stay: String,
    source: String,
    sourceUrl: String,
    readableDataDate: String,
    lastCheckText: String,
    databaseLabel: String,
    issueType: String,
    userComment: String,
    userContact: String?
): String {
    val subject = "Borderly: ошибка в данных ${passportName} → ${countryName}"
    val body = buildString {
        appendLine("Сообщение об ошибке в данных Borderly")
        appendLine()
        appendLine("Тип ошибки: ${issueType}")
        appendLine()
        appendLine("Комментарий пользователя:")
        appendLine(userComment)
        appendLine()
        appendLine("Данные из приложения:")
        appendLine("Паспорт: ${passportName}")
        appendLine("Направление: ${countryName}")
        appendLine("Текущий статус: ${status}")
        appendLine("Срок пребывания: ${stay}")
        appendLine("Источник: ${source}")
        appendLine("Ссылка: ${sourceUrl}")
        appendLine("Версия/дата источника: ${readableDataDate}")
        appendLine("Последняя проверка базы: ${lastCheckText}")
        appendLine("База: ${databaseLabel}")
        if (!userContact.isNullOrBlank()) {
            appendLine()
            appendLine("Контакт пользователя: ${userContact}")
        }
    }
    return "mailto:${android.net.Uri.encode(BorderlyIssueReportEmail)}" +
        "?subject=${android.net.Uri.encode(subject)}" +
        "&body=${android.net.Uri.encode(body)}"
}

internal class SheetListGestureGuard : NestedScrollConnection {
    var gestureStartedAtTop = true

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        val mustKeepSheetStill =
            source == NestedScrollSource.UserInput &&
                available.y > 0f &&
                !gestureStartedAtTop
        return if (mustKeepSheetStill) {
            Offset(x = 0f, y = available.y)
        } else {
            Offset.Zero
        }
    }

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity
    ): Velocity {
        return if (available.y > 0f && !gestureStartedAtTop) {
            Velocity(x = 0f, y = available.y)
        } else {
            Velocity.Zero
        }
    }
}

internal fun Modifier.keepSheetStillUntilNextGesture(
    listState: LazyListState
): Modifier = composed {
    val guard = remember(listState) { SheetListGestureGuard() }

    nestedScroll(guard).pointerInput(listState) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            guard.gestureStartedAtTop =
                listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0

            do {
                val event = awaitPointerEvent()
            } while (event.changes.any { it.pressed })
        }
    }
}

@Composable
private fun borderlyDetailsDarkTheme(): Boolean =
    MaterialTheme.colorScheme.background.luminance() < 0.5f

@Composable
private fun borderlyDetailsPrimaryContentColor(): Color = if (borderlyDetailsDarkTheme()) {
    Color(0xFFF7F9FB)
} else {
    TextPrimary
}

@Composable
private fun borderlyDetailsSecondaryContentColor(): Color = if (borderlyDetailsDarkTheme()) {
    Color(0xFFE6EBF1)
} else {
    TextSecondary
}

@Composable
private fun borderlyDetailsControlSurfaceColor(filled: Boolean): Color {
    val darkTheme = borderlyDetailsDarkTheme()
    return when {
        filled && darkTheme -> Color(0xFF2A323A).copy(alpha = 0.96f)
        filled -> Color(0xFFDCE0E5).copy(alpha = 0.98f)
        darkTheme -> MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
        else -> Color(0xFFF9F9F9).copy(alpha = 0.92f)
    }
}

@Composable
private fun borderlyDetailsControlRimColor(filled: Boolean = false): Color {
    val darkTheme = borderlyDetailsDarkTheme()
    return when {
        filled && darkTheme -> Color(0xFF67727D).copy(alpha = 0.98f)
        filled -> Color(0xFFB7C0CA).copy(alpha = 0.98f)
        darkTheme -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.98f)
        else -> Color.White.copy(alpha = 0.98f)
    }
}

@Composable
private fun BorderlyDetailsActionButton(
    text: String,
    modifier: Modifier = Modifier,
    compact: Boolean,
    filled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .countryDetailsRoundedRectRim(
                rimColor = borderlyDetailsControlRimColor(filled),
                cornerRadius = 26.dp
            )
            .background(borderlyDetailsControlSurfaceColor(filled), RoundedCornerShape(50))
            .noRippleClick(onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (filled && borderlyDetailsDarkTheme()) Color.White else borderlyDetailsPrimaryContentColor(),
            fontSize = if (compact) 12.sp else 14.sp,
            fontWeight = if (filled) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BorderlyDetailsCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    rimColor: Color? = null,
    content: @Composable () -> Unit
) {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val resolvedBackground = backgroundColor ?: if (darkTheme) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
    } else {
        Color(0xFFF9F9F9).copy(alpha = 0.90f)
    }
    val resolvedRim = rimColor ?: if (darkTheme) {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.92f)
    } else {
        Color.White
    }

    Surface(
        modifier = modifier.countryDetailsRoundedRectRim(
            rimColor = resolvedRim,
            cornerRadius = 26.dp
        ),
        color = resolvedBackground,
        shape = RoundedCornerShape(26.dp),
        border = null,
        shadowElevation = 0.dp,
        content = content
    )
}

/**
 * Current Borderly rounded-card rim: two asymmetric routes with long smooth fades.
 * Matches the card language used by Settings, pickers, regions and statistics.
 */
private fun Modifier.countryDetailsRoundedRectRim(
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
internal fun SheetSectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 22.dp, bottom = 10.dp),
        color = borderlyDetailsPrimaryContentColor(),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
internal fun RequirementRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(
                    color = TextSecondary.copy(alpha = .09f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(
                        color = borderlyDetailsSecondaryContentColor(),
                        shape = CircleShape
                    )
            )
        }
        Text(
            text = text,
            modifier = Modifier.padding(start = 11.dp),
            color = borderlyDetailsPrimaryContentColor(),
            fontSize = 14.sp,
            lineHeight = 19.sp
        )
    }
}

@Composable
internal fun ExpandableExplanationCard(
    title: String,
    text: String,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    BorderlyDetailsCard(
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClick(onToggle)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 15.dp,
                vertical = 14.dp
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    color = borderlyDetailsPrimaryContentColor(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (expanded) "⌃" else "⌄",
                    color = borderlyDetailsSecondaryContentColor(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(
                    color = Line.copy(alpha = .75f),
                    thickness = 1.dp
                )
                Text(
                    text = text,
                    modifier = Modifier.padding(top = 11.dp),
                    color = borderlyDetailsSecondaryContentColor(),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
internal fun EntryGuideSummaryCard(
    guide: PassportEntryGuide
) {
    BorderlyDetailsCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Text(
                text = guide.permitName,
                color = borderlyDetailsPrimaryContentColor(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            EntryGuideFactRow(
                label = "Подача",
                value = guide.applicationMethod
            )

            guide.fee?.let {
                EntryGuideFactRow(
                    label = "Сбор",
                    value = it
                )
            }

            guide.timing?.let {
                EntryGuideFactRow(
                    label = "Срок / подача",
                    value = it
                )
            }

            guide.extraNote?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(top = 12.dp),
                    color = borderlyDetailsSecondaryContentColor(),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 13.dp),
                color = Line.copy(alpha = .75f),
                thickness = 1.dp
            )

            Text(
                text = "${guide.officialAuthority} · проверено ${guide.verified}",
                modifier = Modifier.padding(top = 10.dp),
                color = borderlyDetailsSecondaryContentColor(),
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun EntryGuideFactRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(.32f),
            color = borderlyDetailsSecondaryContentColor(),
            fontSize = 11.sp
        )
        Text(
            text = value,
            modifier = Modifier
                .weight(.68f)
                .padding(start = 12.dp),
            color = borderlyDetailsPrimaryContentColor(),
            fontSize = 12.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
internal fun ApplicationStepsCard(
    steps: List<String>
) {
    BorderlyDetailsCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 15.dp,
                vertical = 14.dp
            )
        ) {
            steps.forEachIndexed { index, step ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(23.dp)
                            .background(
                                color = TextPrimary.copy(alpha = .07f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (index + 1).toString(),
                            color = borderlyDetailsPrimaryContentColor(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = step,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 11.dp, top = 2.dp),
                        color = borderlyDetailsPrimaryContentColor(),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
internal fun OfficialEntryLinksCard(
    guide: PassportEntryGuide,
    onOpen: (String) -> Unit
) {
    BorderlyDetailsCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Text(
                text = guide.officialAuthority,
                color = borderlyDetailsPrimaryContentColor(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Ссылки проверены ${guide.verified}",
                modifier = Modifier.padding(top = 3.dp),
                color = borderlyDetailsSecondaryContentColor(),
                fontSize = 10.sp
            )

            guide.links.forEach { link ->
                val buttonColor = if (link.primary) TextPrimary else Color.White
                val textColor = if (link.primary) Color.White else TextPrimary

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .heightIn(min = 44.dp)
                        .background(
                            color = buttonColor,
                            shape = RoundedCornerShape(13.dp)
                        )
                        .then(
                            if (link.primary) {
                                Modifier
                            } else {
                                Modifier.border(
                                    1.dp,
                                    Line,
                                    RoundedCornerShape(13.dp)
                                )
                            }
                        )
                        .noRippleClick { onOpen(link.url) }
                        .padding(horizontal = 13.dp, vertical = 11.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = if (link.primary) {
                            "${link.title}  ↗"
                        } else {
                            link.title
                        },
                        color = textColor,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = "Borderly открывает только указанный государственный ресурс. " +
                    "Перед оплатой проверьте домен в браузере.",
                modifier = Modifier.padding(top = 11.dp),
                color = borderlyDetailsSecondaryContentColor(),
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
internal fun ExpandableApplicationDocumentsCard(
    title: String,
    documents: List<String>,
    note: String?,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    BorderlyDetailsCard(
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClick(onToggle)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 15.dp,
                vertical = 14.dp
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    color = borderlyDetailsPrimaryContentColor(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (expanded) "⌃" else "⌄",
                    color = borderlyDetailsSecondaryContentColor(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(
                    color = Line.copy(alpha = .75f),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Подготовьте",
                    color = borderlyDetailsSecondaryContentColor(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                documents.forEach { document ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp)
                                .size(5.dp)
                                .background(
                                    color = borderlyDetailsSecondaryContentColor(),
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text = document,
                            modifier = Modifier.padding(start = 10.dp),
                            color = borderlyDetailsPrimaryContentColor(),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                if (!note.isNullOrBlank()) {
                    Text(
                        text = note,
                        modifier = Modifier.padding(top = 14.dp),
                        color = borderlyDetailsSecondaryContentColor(),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
internal fun EntryRequirementCard(
    requirement: EntryRequirement,
    onOpenSource: (String) -> Unit
) {
    BorderlyDetailsCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = requirement.type.label,
                    modifier = Modifier.weight(1f),
                    color = borderlyDetailsSecondaryContentColor(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (requirement.mandatory) {
                    Text(
                        text = "Обязательно",
                        color = VisaRequired,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = requirement.title,
                modifier = Modifier.padding(top = 5.dp),
                color = borderlyDetailsPrimaryContentColor(),
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = requirement.summary,
                modifier = Modifier.padding(top = 7.dp),
                color = borderlyDetailsSecondaryContentColor(),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Surface(
                modifier = Modifier.padding(top = 11.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .46f),
                shape = RoundedCornerShape(14.dp),
                border = null,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Когда",
                        modifier = Modifier.weight(.28f),
                        color = borderlyDetailsSecondaryContentColor(),
                        fontSize = 11.sp
                    )
                    Text(
                        text = requirement.timing,
                        modifier = Modifier.weight(.72f),
                        color = borderlyDetailsPrimaryContentColor(),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (requirement.steps.isNotEmpty()) {
                Text(
                    text = "Что сделать",
                    modifier = Modifier.padding(top = 13.dp),
                    color = borderlyDetailsPrimaryContentColor(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                requirement.steps.forEach { step ->
                    RequirementRow(text = step)
                }
            }

            Text(
                text = requirement.officialAuthority,
                modifier = Modifier.padding(top = 8.dp),
                color = borderlyDetailsSecondaryContentColor(),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
            Text(
                text = "Проверено ${formatDataDateForUi(requirement.verified)}",
                modifier = Modifier.padding(top = 2.dp),
                color = borderlyDetailsSecondaryContentColor(),
                fontSize = 10.sp
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(48.dp)
                    .border(1.dp, Line, RoundedCornerShape(12.dp))
                    .noRippleClick { onOpenSource(requirement.sourceUrl) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Официальный источник",
                    color = borderlyDetailsPrimaryContentColor(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
internal fun EntryConditionsCard(
    conditions: List<EntryCondition>,
    accentColor: Color
) {
    BorderlyDetailsCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 15.dp)) {
            conditions.forEachIndexed { index, condition ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = condition.label,
                        modifier = Modifier.weight(.42f),
                        color = borderlyDetailsSecondaryContentColor(),
                        fontSize = 12.sp
                    )
                    Text(
                        text = condition.value,
                        modifier = Modifier
                            .weight(.58f)
                            .padding(start = 12.dp),
                        color = borderlyDetailsPrimaryContentColor(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (index != conditions.lastIndex) {
                    HorizontalDivider(
                        color = Line.copy(alpha = .75f),
                        thickness = 1.dp
                    )
                }
            }
        }
    }
}

@Composable
internal fun SourceInformationCard(
    source: String,
    sourceUrl: String,
    dataChangedDate: String,
    lastCheckText: String,
    databaseLabel: String,
    sourceType: VisaSourceType,
    sourceDescription: String?,
    sourceLicense: String?,
    specificRuleSource: Boolean,
    onReportIssue: (() -> Unit)? = null,
    onOpenSource: () -> Unit
) {
    val badgeTitle = when (sourceType) {
        VisaSourceType.OFFICIAL -> "Официальный источник"
        VisaSourceType.CORROBORATED -> "Подтверждено несколькими источниками"
        VisaSourceType.DATASET -> "Общий источник данных"
        VisaSourceType.DERIVED -> "Производное правило"
        VisaSourceType.UNKNOWN -> "Источник базы"
    }
    val badgeColor = when (sourceType) {
        VisaSourceType.OFFICIAL,
        VisaSourceType.CORROBORATED -> Freedom

        VisaSourceType.DATASET -> VisaOnArrival
        VisaSourceType.DERIVED,
        VisaSourceType.UNKNOWN -> TextSecondary
    }
    val badgeSymbol = when (sourceType) {
        VisaSourceType.OFFICIAL,
        VisaSourceType.CORROBORATED -> "✓"

        else -> "i"
    }
    val explanation = sourceDescription ?: when (sourceType) {
        VisaSourceType.OFFICIAL -> if (specificRuleSource) {
            "Источник относится к выбранному паспорту и направлению."
        } else {
            "Официальный источник относится к этому направлению."
        }

        VisaSourceType.CORROBORATED ->
            "Статус подтверждён официальным сигналом и общей базой данных."

        VisaSourceType.DATASET ->
            "Это источник происхождения данных, а не отдельное подтверждение государственного органа."

        VisaSourceType.DERIVED ->
            "Статус рассчитан по реестру территориальных правил Borderly."

        VisaSourceType.UNKNOWN ->
            "Для этой записи пока не указан отдельный тип подтверждения."
    }
    val changedDateLabel = when (sourceType) {
        VisaSourceType.OFFICIAL,
        VisaSourceType.CORROBORATED -> "Правило проверено"

        else -> "Версия источников базы"
    }
    val openButtonLabel = when (sourceType) {
        VisaSourceType.OFFICIAL,
        VisaSourceType.CORROBORATED -> "Открыть подтверждение"

        VisaSourceType.DERIVED -> "Открыть основание"
        else -> "Открыть источник"
    }
    BorderlyDetailsCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(badgeColor.copy(alpha = .14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeSymbol,
                        color = badgeColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = badgeTitle,
                    modifier = Modifier.padding(start = 9.dp),
                    color = borderlyDetailsPrimaryContentColor(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(11.dp))

            Text(
                text = source,
                color = borderlyDetailsPrimaryContentColor(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )


            if (!sourceLicense.isNullOrBlank()) {
                Text(
                    text = "Лицензия источника: $sourceLicense",
                    modifier = Modifier.padding(top = 4.dp),
                    color = borderlyDetailsSecondaryContentColor(),
                    fontSize = 10.sp
                )
            }

            Text(
                text = "Последняя проверка базы: $lastCheckText",
                modifier = Modifier.padding(top = 9.dp),
                color = borderlyDetailsSecondaryContentColor(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$changedDateLabel: $dataChangedDate",
                modifier = Modifier.padding(top = 2.dp),
                color = borderlyDetailsSecondaryContentColor(),
                fontSize = 10.sp
            )
            Text(
                text = databaseLabel,
                modifier = Modifier.padding(top = 5.dp),
                color = borderlyDetailsSecondaryContentColor(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )

            if (sourceUrl.startsWith("https://")) {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .height(48.dp)
                        .countryDetailsRoundedRectRim(
                            rimColor = borderlyDetailsControlRimColor(filled = true),
                            cornerRadius = 24.dp
                        )
                        .background(borderlyDetailsControlSurfaceColor(filled = true), RoundedCornerShape(50))
                        .noRippleClick(onOpenSource)
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = openButtonLabel,
                        color = if (borderlyDetailsDarkTheme()) Color.White else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (onReportIssue != null) {
                Box(
                    modifier = Modifier
                        .padding(top = 9.dp)
                        .height(48.dp)
                        .countryDetailsRoundedRectRim(
                            rimColor = borderlyDetailsControlRimColor(filled = true),
                            cornerRadius = 24.dp
                        )
                        .background(borderlyDetailsControlSurfaceColor(filled = true), RoundedCornerShape(50))
                        .noRippleClick(onReportIssue)
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Сообщить об ошибке",
                        color = if (borderlyDetailsDarkTheme()) Color.White else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}



@Composable
internal fun InformationCard(text: String) {
    BorderlyDetailsCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(15.dp),
            color = borderlyDetailsSecondaryContentColor(),
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}


