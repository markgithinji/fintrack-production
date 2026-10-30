package com.fintrack.shared.feature.core.ui.chart

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrack.shared.feature.navigation.ui.toCurrencyString
import kotlin.math.roundToInt

data class ChartSeries(
    val name: String,
    val color: Color,
    val values: List<Double>,
    val showGradientFill: Boolean = true
)

data class InteractiveChartData(
    val labels: List<String>,
    val series: List<ChartSeries>
)

@Composable
fun InteractiveLineChart(
    chartData: InteractiveChartData,
    chartHeight: Dp = 200.dp,
    showYAxis: Boolean = true,
    isScrollable: Boolean = true,
    minPointSpacing: Dp = 48.dp,
    onPointSelected: ((index: Int, label: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        fontSize = 10.sp
    )

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(chartData) {
        animationProgress.animateTo(1f, animationSpec = tween(1200))
    }

    val pointCount = chartData.labels.size
    if (pointCount == 0 || chartData.series.isEmpty()) return

    val allValues = chartData.series.flatMap { it.values }
    val rawMinVal = allValues.minOrNull() ?: 0.0
    val rawMaxVal = allValues.maxOrNull() ?: 1.0

    val chartMinY = if (rawMinVal < 0) rawMinVal * 1.15 else 0.0
    val chartMaxY = if (rawMaxVal > 0) rawMaxVal * 1.15 else 1.0
    val chartRangeY = (chartMaxY - chartMinY).coerceAtLeast(1.0)

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var touchOffset by remember { mutableStateOf(Offset.Zero) }

    val startPadding = if (showYAxis) 36.dp else 0.dp
    val endPadding = if (showYAxis) 16.dp else 0.dp
    val bottomPadding = if (showYAxis) 24.dp else 16.dp

    val density = LocalDensity.current
    val minSpacingPx = with(density) { minPointSpacing.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { selectedIndex = null }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.height(chartHeight)) {
            // Y-Axis & Grid Background
            if (showYAxis) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight)
                        .padding(bottom = bottomPadding, start = startPadding, end = endPadding)
                ) {
                    val height = size.height
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val y = height - (height / gridLines.toFloat()) * i
                        val value = chartMinY + (chartRangeY / gridLines.toDouble()) * i

                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.15f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )

                        val label = when {
                            value >= 1000 -> {
                                val kValue = value / 1000.0
                                if (kValue >= 100) "${kValue.toInt()}k"
                                else if (kValue % 1.0 == 0.0) "${kValue.toInt()}k"
                                else "${(kValue * 10).toInt() / 10.0}k"
                            }

                            else -> value.toInt().toString()
                        }
                        val textLayoutResult = textMeasurer.measure(label, labelStyle)
                        drawText(
                            textLayoutResult = textLayoutResult,
                            topLeft = Offset(-textLayoutResult.size.width.toFloat() - 6.dp.toPx(), y - textLayoutResult.size.height / 2)
                        )
                    }
                }
            }

            // Scrollable / Full-Width Drawing Container
            val scrollState = rememberScrollState()

            LaunchedEffect(pointCount) {
                if (isScrollable) {
                    scrollState.scrollTo(Int.MAX_VALUE)
                }
            }

            var viewportWidth by remember { mutableStateOf(0f) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = startPadding)
                    .onGloballyPositioned { viewportWidth = it.size.width.toFloat() }
                    .then(if (isScrollable) Modifier.horizontalScroll(scrollState) else Modifier)
            ) {
                val totalChartWidthDp = if (isScrollable) {
                    (minPointSpacing * (pointCount - 1).coerceAtLeast(0)) + 32.dp
                } else {
                    Dp.Unspecified
                }

                val crosshairColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

                Box(
                    modifier = Modifier
                        .then(if (isScrollable) Modifier.width(totalChartWidthDp) else Modifier.fillMaxWidth())
                        .height(chartHeight)
                        .pointerInput(chartData, chartMaxY, chartMinY) {
                            detectTapGestures { offset ->
                                val stepX = if (pointCount > 1) size.width / (pointCount - 1) else size.width
                                val idx = (offset.x / stepX).roundToInt().coerceIn(0, pointCount - 1)
                                if (selectedIndex == idx) {
                                    selectedIndex = null
                                } else {
                                    selectedIndex = idx
                                    touchOffset = offset
                                    onPointSelected?.invoke(idx, chartData.labels[idx])
                                }
                            }
                        }
                        .pointerInput(chartData, chartMaxY, chartMinY) {
                            detectDragGestures(
                                onDragEnd = {},
                                onDragCancel = {},
                                onDrag = { change, _ ->
                                    change.consume()
                                    val stepX = if (pointCount > 1) size.width / (pointCount - 1) else size.width
                                    val idx = (change.position.x / stepX).roundToInt().coerceIn(0, pointCount - 1)
                                    selectedIndex = idx
                                    touchOffset = change.position
                                    onPointSelected?.invoke(idx, chartData.labels[idx])
                                }
                            )
                        }
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = bottomPadding, end = if (isScrollable) endPadding else 0.dp)
                    ) {
                        val width = size.width
                        val height = size.height
                        val stepX = if (pointCount > 1) width / (pointCount - 1) else width

                        // Optional Zero-Line when values cross between negative and positive
                        if (chartMinY < 0 && chartMaxY > 0) {
                            val zeroY = height - ((0.0 - chartMinY) / chartRangeY * height).toFloat()
                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.35f),
                                start = Offset(0f, zeroY),
                                end = Offset(width, zeroY),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            )
                        }

                        chartData.series.forEach { series ->
                            if (series.values.size >= pointCount) {
                                val linePath = Path()
                                val fillPath = Path()

                                for (i in 0 until pointCount) {
                                    val x = i * stepX
                                    val valDouble = series.values[i]
                                    val y = height - ((valDouble - chartMinY) / chartRangeY * height).toFloat()

                                    if (i == 0) {
                                        linePath.moveTo(x, y)
                                        fillPath.moveTo(x, height)
                                        fillPath.lineTo(x, y)
                                    } else {
                                        linePath.lineTo(x, y)
                                        fillPath.lineTo(x, y)
                                    }

                                    if (i == pointCount - 1) {
                                        fillPath.lineTo(x, height)
                                        fillPath.close()
                                    }
                                }

                                clipRect(right = width * animationProgress.value) {
                                    if (series.showGradientFill) {
                                        drawPath(
                                            path = fillPath,
                                            brush = Brush.verticalGradient(
                                                colors = listOf(series.color.copy(alpha = 0.15f), Color.Transparent)
                                            )
                                        )
                                    }
                                    drawPath(
                                        path = linePath,
                                        color = series.color,
                                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                            }
                        }

                        // X-Axis Date/Label Tick Text
                        val stepInterval = when {
                            !isScrollable && pointCount > 20 -> 12
                            !isScrollable && pointCount > 10 -> 2
                            !isScrollable && pointCount > 6 -> 2
                            else -> 1
                        }

                        for (i in 0 until pointCount) {
                            val isFirst = i == 0
                            val isLast = i == pointCount - 1
                            val isStep = i % stepInterval == 0

                            if (!isFirst && !isLast && !isStep) continue

                            val x = i * stepX
                            var label = chartData.labels[i]

                            if (!isScrollable && pointCount > 20 && label.contains(" '")) {
                                label = "20" + label.substringAfter(" '")
                            } else if (!isScrollable && pointCount > 8 && label.contains(" '")) {
                                label = label.substringBefore(" '")
                            }

                            val textLayoutResult = textMeasurer.measure(label, labelStyle)
                            val textWidth = textLayoutResult.size.width.toFloat()
                            val labelX = when (i) {
                                0 -> x
                                pointCount - 1 -> (x - textWidth).coerceAtLeast(0f)
                                else -> x - textWidth / 2
                            }

                            drawText(
                                textLayoutResult = textLayoutResult,
                                topLeft = Offset(labelX, height + 4.dp.toPx())
                            )
                        }

                        // Selection Crosshair & Data Points Highlight
                        selectedIndex?.let { idx ->
                            val x = idx * stepX

                            drawLine(
                                color = crosshairColor,
                                start = Offset(x, 0f),
                                end = Offset(x, height),
                                strokeWidth = 1.5.dp.toPx()
                            )

                            chartData.series.forEach { series ->
                                if (idx in series.values.indices) {
                                    val valDouble = series.values[idx]
                                    val y = height - ((valDouble - chartMinY) / chartRangeY * height).toFloat()

                                    drawCircle(Color.White, radius = 5.dp.toPx(), center = Offset(x, y))
                                    drawCircle(series.color, radius = 3.5.dp.toPx(), center = Offset(x, y))
                                }
                            }
                        }
                    }
                }
            }

            // Interactive Tooltip Callout Overlay
            var tooltipSize by remember { mutableStateOf(IntSize.Zero) }
            val scrollVal = if (isScrollable) scrollState.value else 0
            val isTooltipVisible = selectedIndex != null && (touchOffset.x >= scrollVal && touchOffset.x <= scrollVal + viewportWidth)

            AnimatedVisibility(
                visible = isTooltipVisible,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier.offset {
                    val xInViewport = touchOffset.x - scrollVal
                    val xOffset = (startPadding.toPx() + xInViewport - tooltipSize.width / 2)
                        .coerceIn(0f, (viewportWidth + startPadding.toPx() - tooltipSize.width).coerceAtLeast(0f))

                    val chartHeightPx = chartHeight.toPx()
                    val yOffset = if (touchOffset.y - tooltipSize.height - 12.dp.toPx() < 0) {
                        (touchOffset.y + 12.dp.toPx()).coerceAtMost(chartHeightPx - tooltipSize.height)
                    } else {
                        touchOffset.y - tooltipSize.height - 12.dp.toPx()
                    }

                    IntOffset(xOffset.toInt(), yOffset.toInt())
                }
            ) {
                selectedIndex?.let { idx ->
                    if (idx in chartData.labels.indices) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            shadowElevation = 6.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .widthIn(min = 110.dp)
                                .clickable { selectedIndex = null }
                                .onSizeChanged { tooltipSize = it }
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = chartData.labels[idx],
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                chartData.series.forEach { s ->
                                    if (idx in s.values.indices) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(Modifier.size(6.dp).background(s.color, CircleShape))
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = "${s.name}: ${s.values[idx].toCurrencyString()}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = s.color
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
