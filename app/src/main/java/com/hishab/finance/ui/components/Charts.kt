package com.hishab.finance.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hishab.finance.ui.theme.ChartPalette
import kotlin.math.max

data class ChartSlice(val label: String, val value: Double, val color: Color)
data class BarGroup(val label: String, val income: Double, val expense: Double)

/**
 * Donut for category and source breakdowns. Slices animate in once on first draw; nothing
 * animates on recomposition, so scrolling stays still.
 */
@Composable
fun DonutChart(
    slices: List<ChartSlice>,
    modifier: Modifier = Modifier,
    centerLabel: String? = null,
    centerValue: String? = null,
    thickness: Float = 34f
) {
    val total = slices.sumOf { it.value }.toFloat()
    val progress = remember(slices.size, total) { Animatable(0f) }
    LaunchedEffect(slices.size, total) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
    }
    val track = MaterialTheme.colorScheme.surfaceVariant

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxWidth().height(190.dp)) {
            val diameter = minOf(size.width, size.height) - thickness
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            drawArc(
                color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(width = thickness)
            )
            if (total <= 0f) return@Canvas

            var start = -90f
            slices.forEach { slice ->
                val sweep = (slice.value / total).toFloat() * 360f * progress.value
                drawArc(
                    color = slice.color, startAngle = start, sweepAngle = max(sweep - 1.5f, 0f),
                    useCenter = false, topLeft = topLeft, size = arcSize,
                    style = Stroke(width = thickness)
                )
                start += sweep
            }
        }
        if (centerValue != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    centerValue,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (centerLabel != null) {
                    Text(
                        centerLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Income beside expense, one pair per period. */
@Composable
fun GroupedBarChart(
    groups: List<BarGroup>,
    incomeColor: Color,
    expenseColor: Color,
    modifier: Modifier = Modifier,
    height: Int = 170
) {
    if (groups.isEmpty()) return
    val peak = groups.maxOf { maxOf(it.income, it.expense) }.toFloat().coerceAtLeast(1f)
    val progress = remember(groups) { Animatable(0f) }
    LaunchedEffect(groups) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
    }
    val grid = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxWidth().height(height.dp)) {
            val slotWidth = size.width / groups.size
            val barWidth = (slotWidth * 0.26f).coerceAtMost(26f)
            val gap = barWidth * 0.35f

            repeat(4) { i ->
                val y = size.height * i / 3f
                drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }

            groups.forEachIndexed { index, group ->
                val centerX = slotWidth * index + slotWidth / 2f
                val incomeHeight = (group.income / peak).toFloat() * size.height * progress.value
                val expenseHeight = (group.expense / peak).toFloat() * size.height * progress.value

                drawRoundRect(
                    color = incomeColor,
                    topLeft = Offset(centerX - barWidth - gap / 2f, size.height - incomeHeight),
                    size = Size(barWidth, incomeHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2.5f)
                )
                drawRoundRect(
                    color = expenseColor,
                    topLeft = Offset(centerX + gap / 2f, size.height - expenseHeight),
                    size = Size(barWidth, expenseHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2.5f)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            groups.forEach { group ->
                Text(
                    group.label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Daily spending trend: filled line, one point per day. */
@Composable
fun TrendLineChart(
    values: List<Double>,
    labels: List<String>,
    lineColor: Color,
    modifier: Modifier = Modifier,
    height: Int = 160
) {
    if (values.isEmpty()) return
    val peak = (values.maxOrNull() ?: 0.0).toFloat().coerceAtLeast(1f)
    val progress = remember(values) { Animatable(0f) }
    LaunchedEffect(values) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
    }
    val grid = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxWidth().height(height.dp)) {
            repeat(4) { i ->
                val y = size.height * i / 3f
                drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }
            if (values.size == 1) {
                drawCircle(lineColor, radius = 6f, center = Offset(size.width / 2f, size.height / 2f))
                return@Canvas
            }

            val stepX = size.width / (values.size - 1)
            val points = values.mapIndexed { index, value ->
                Offset(stepX * index, size.height - (value / peak).toFloat() * size.height * progress.value)
            }

            val fill = Path().apply {
                moveTo(points.first().x, size.height)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, size.height)
                close()
            }
            drawPath(
                fill,
                Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0f)))
            )

            val stroke = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(stroke, lineColor, style = Stroke(width = 4f))

            val peakValue = values.maxOrNull() ?: 0.0
            val peakIndex = values.indexOf(peakValue)
            if (peakIndex >= 0) drawCircle(lineColor, radius = 7f, center = points[peakIndex])
        }
        if (labels.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                labels.forEach {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** Forecast against what actually happened, as paired bars. */
@Composable
fun ForecastAccuracyChart(
    labels: List<String>,
    forecast: List<Double>,
    actual: List<Double>,
    forecastColor: Color,
    actualColor: Color,
    modifier: Modifier = Modifier
) {
    val groups = labels.indices.map {
        BarGroup(labels[it], forecast.getOrElse(it) { 0.0 }, actual.getOrElse(it) { 0.0 })
    }
    GroupedBarChart(
        groups = groups,
        incomeColor = forecastColor,
        expenseColor = actualColor,
        modifier = modifier
    )
}

/** A horizontal share bar used in every breakdown list. */
@Composable
fun ShareBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val animated = remember(fraction) { Animatable(0f) }
    LaunchedEffect(fraction) { animated.animateTo(fraction.coerceIn(0f, 1f), tween(500)) }
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(trackColor)
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated.value)
                .height(8.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
fun LegendDot(color: Color, label: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun paletteColor(index: Int): Color = ChartPalette[index % ChartPalette.size]

fun Int.asComposeColor(): Color = Color(this)
