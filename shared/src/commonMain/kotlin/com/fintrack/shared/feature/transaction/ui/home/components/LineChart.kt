package com.fintrack.shared.feature.transaction.ui.home.components

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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrack.shared.feature.core.util.shortDayName
import com.fintrack.shared.feature.navigation.ui.toCurrencyString
import com.fintrack.shared.feature.summary.domain.model.DaySummary
import com.fintrack.shared.ui.theme.GreenIncome
import com.fintrack.shared.ui.theme.PinkExpense
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.LocalDate
import kotlin.math.roundToInt

@Composable
fun LineChart(
    data: List<DaySummary>,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No data for this period",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        return
    }

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        color = Color.Gray,
        fontSize = 10.sp
    )

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.animateTo(1f, animationSpec = tween(1500))
    }

    val sortedData = data.sortedBy { it.date }
    val maxIncome = sortedData.maxOfOrNull { it.income } ?: BigDecimal.ZERO
    val maxExpense = sortedData.maxOfOrNull { it.expense } ?: BigDecimal.ZERO
    val maxVal = if (maxIncome > maxExpense) maxIncome else maxExpense
    val maxValue = maxVal.doubleValue(false).coerceAtLeast(1.0).toFloat() * 1.2f

    var selectedDay by remember { mutableStateOf<DaySummary?>(null) }
    var touchOffset by remember { mutableStateOf(Offset.Zero) }

    val minSpacingDp = 48.dp
    val totalChartWidthDp = (minSpacingDp * (sortedData.size - 1).coerceAtLeast(0)) + 32.dp
    val selectionLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { selectedDay = null }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.height(200.dp)) {
            // Static Y-axis background
            Canvas(modifier = Modifier.fillMaxWidth().height(200.dp).padding(bottom = 24.dp, start = 36.dp, end = 12.dp)) {
                val height = size.height
                val gridLines = 4
                for (i in 0..gridLines) {
                    val y = height - (height / gridLines.toFloat()) * i
                    val value = (maxValue / gridLines.toFloat()) * i
                    
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.2f),
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
                        topLeft = Offset(-textLayoutResult.size.width.toFloat() - 8.dp.toPx(), y - textLayoutResult.size.height / 2)
                    )
                }
            }

            // Scrollable Area
            val scrollState = rememberScrollState()
            
            // Auto-scroll to the end to show latest data
            LaunchedEffect(sortedData) {
                scrollState.scrollTo(Int.MAX_VALUE)
            }

            var viewportWidth by remember { mutableStateOf(0f) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 36.dp)
                    .onGloballyPositioned { viewportWidth = it.size.width.toFloat() }
                    .horizontalScroll(scrollState)
            ) {
                val paddingStart = 0.dp
                val paddingEnd = 32.dp

                Box(
                    modifier = Modifier
                        .width(totalChartWidthDp)
                        .height(200.dp)
                        .pointerInput(sortedData, maxValue) {
                            detectTapGestures { offset ->
                                val chartWidth = size.width - (paddingStart + paddingEnd).toPx()
                                val spacingX = if (sortedData.size > 1) chartWidth / (sortedData.size - 1) else chartWidth
                                val index = ((offset.x - paddingStart.toPx()) / spacingX).roundToInt().coerceIn(0, sortedData.size - 1)
                                
                                selectedDay = sortedData[index]
                                touchOffset = offset
                            }
                        }
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = paddingStart, bottom = 24.dp, end = paddingEnd)
                    ) {
                        val width = size.width
                        val height = size.height
                        val spacingX = if (sortedData.size > 1) width / (sortedData.size - 1) else width

                        // Prepare Paths
                        val incomePath = Path()
                        val expensePath = Path()
                        val incomeFillPath = Path()
                        val expenseFillPath = Path()

                        sortedData.forEachIndexed { index, day ->
                            val x = index * spacingX
                            val yIncome = height - (day.income.floatValue(false) / maxValue) * height
                            val yExpense = height - (day.expense.floatValue(false) / maxValue) * height
                            val date = try { LocalDate.parse(day.date) } catch(_: Exception) { null }

                            // Month Transition Marker
                            if (date != null && date.day == 1 && index > 0) {
                                drawLine(
                                    color = Color.LightGray.copy(alpha = 0.4f),
                                    start = Offset(x, 0f),
                                    end = Offset(x, height),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )
                            }

                            if (index == 0) {
                                incomePath.moveTo(x, yIncome)
                                expensePath.moveTo(x, yExpense)
                                incomeFillPath.moveTo(x, height)
                                incomeFillPath.lineTo(x, yIncome)
                                expenseFillPath.moveTo(x, height)
                                expenseFillPath.lineTo(x, yExpense)
                            } else {
                                incomePath.lineTo(x, yIncome)
                                expensePath.lineTo(x, yExpense)
                                incomeFillPath.lineTo(x, yIncome)
                                expenseFillPath.lineTo(x, yExpense)
                            }

                            if (index == sortedData.size - 1) {
                                incomeFillPath.lineTo(x, height)
                                incomeFillPath.close()
                                expenseFillPath.lineTo(x, height)
                                expenseFillPath.close()
                            }

                            // X-axis Labels
                            val isFirstDayOfMonth = date?.day == 1
                            val isFirstPoint = index == 0
                            
                            val dateLabel = if (date != null && (isFirstDayOfMonth || isFirstPoint)) {
                                val monthName = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
                                "${date.day} $monthName"
                            } else {
                                day.date.split("-").last()
                            }

                            val textLayoutResult = textMeasurer.measure(dateLabel, labelStyle)
                            val textWidth = textLayoutResult.size.width.toFloat()
                            val labelX = when (index) {
                                0 -> x // Start align first label
                                sortedData.size - 1 -> x - textWidth // End align last label
                                else -> x - textWidth / 2 // Center align others
                            }
                            
                            drawText(
                                textLayoutResult = textLayoutResult,
                                topLeft = Offset(labelX, height + 8.dp.toPx())
                            )
                        }

                        // Draw Paths with Animation
                        clipRect(right = width * animationProgress.value) {
                            if (sortedData.size > 1) {
                                drawPath(
                                    path = expenseFillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(PinkExpense.copy(alpha = 0.15f), Color.Transparent)
                                    )
                                )
                                drawPath(
                                    path = expensePath,
                                    color = PinkExpense,
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawPath(
                                    path = incomeFillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(GreenIncome.copy(alpha = 0.15f), Color.Transparent)
                                    )
                                )
                                drawPath(
                                    path = incomePath,
                                    color = GreenIncome,
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                        }

                        // Selection Highlights
                        selectedDay?.let { day ->
                            val index = sortedData.indexOf(day)
                            val x = index * spacingX
                            val yIncome = height - (day.income.floatValue(false) / maxValue) * height
                            val yExpense = height - (day.expense.floatValue(false) / maxValue) * height

                            drawLine(
                                color = selectionLineColor,
                                start = Offset(x, 0f),
                                end = Offset(x, height),
                                strokeWidth = 2.dp.toPx()
                            )

                            drawCircle(Color.White, radius = 6.dp.toPx(), center = Offset(x, yIncome))
                            drawCircle(GreenIncome, radius = 4.dp.toPx(), center = Offset(x, yIncome))

                            drawCircle(Color.White, radius = 6.dp.toPx(), center = Offset(x, yExpense))
                            drawCircle(PinkExpense, radius = 4.dp.toPx(), center = Offset(x, yExpense))
                        }
                    }
                }
            }

            // Tooltip
            var tooltipSize by remember { mutableStateOf(IntSize.Zero) }
            val isTooltipVisible = selectedDay != null && (touchOffset.x >= scrollState.value && touchOffset.x <= scrollState.value + viewportWidth)

            AnimatedVisibility(
                visible = isTooltipVisible,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier.offset {
                    val startPaddingPx = 36.dp.toPx()
                    val xInViewport = touchOffset.x - scrollState.value
                    val xOffset = (startPaddingPx + xInViewport - tooltipSize.width / 2)
                        .coerceIn(0f, (viewportWidth + startPaddingPx - tooltipSize.width).coerceAtLeast(0f))
                    
                    val chartHeightPx = 200.dp.toPx()
                    val yOffset = if (touchOffset.y - tooltipSize.height - 12.dp.toPx() < 0) {
                        (touchOffset.y + 12.dp.toPx()).coerceAtMost(chartHeightPx - tooltipSize.height)
                    } else {
                        touchOffset.y - tooltipSize.height - 12.dp.toPx()
                    }
                    
                    IntOffset(xOffset.toInt(), yOffset.toInt())
                }
            ) {
                selectedDay?.let { day ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 8.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .widthIn(min = 120.dp)
                            .onSizeChanged { tooltipSize = it }
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = try { LocalDate.parse(day.date).shortDayName() + ", " + day.date.split("-").last() } catch(_: Exception) { day.date },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(6.dp).background(GreenIncome, CircleShape))
                                Text(
                                    text = " " + day.income.toCurrencyString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenIncome
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(6.dp).background(PinkExpense, CircleShape))
                                Text(
                                    text = " " + day.expense.toCurrencyString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PinkExpense
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
