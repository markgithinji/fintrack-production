package com.fintrack.shared.feature.summary.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.category.ui.util.toIcon
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

import androidx.compose.runtime.saveable.rememberSaveable
import com.fintrack.shared.feature.category.domain.model.fromName
import com.fintrack.shared.feature.navigation.ui.toCurrencyString

@Composable
fun InteractiveDonutWithText(
    categorySums: List<Pair<String, Double>>,
    totalAmount: Double,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    chartSize: Dp = 250.dp,
    gapPercentage: Float = 0.03f,
    segmentColors: List<Color> = SegmentColors
) {
    if (categorySums.isEmpty() || totalAmount <= 0.0) return

    // Entrance animation control
    var hasAnimated by rememberSaveable { mutableStateOf(false) }
    val entranceProgress = remember { Animatable(if (hasAnimated) 1f else 0f) }
    
    LaunchedEffect(Unit) {
        if (!hasAnimated) {
            entranceProgress.animateTo(1f, animationSpec = tween(1000))
            hasAnimated = true
        }
    }

    // Animate stroke width on selection
    val animatedStrokes = List(categorySums.size) { index ->
        animateDpAsState(
            targetValue = if (selectedIndex == index) 52.dp else 36.dp,
            animationSpec = tween(300)
        )
    }

    val chartScale by animateFloatAsState(
        targetValue = if (entranceProgress.value < 1f) 0.8f + (0.2f * entranceProgress.value) else 1f,
        animationSpec = tween(500)
    )

    Box(
        modifier = modifier
            .size(chartSize)
            .scale(chartScale)
            .pointerInput(selectedIndex) {
                detectTapGestures { tapOffset ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val anglesList =
                        calculateAnglesList(categorySums, totalAmount, gapPercentage)

                    handleCanvasTap(
                        center,
                        tapOffset,
                        anglesList,
                        selectedIndex,
                        onItemSelected = { index ->
                            onSelectedIndexChange(index)
                        },
                        onItemDeselected = { _ -> },
                        onNoItemSelected = { onSelectedIndexChange(-1) }
                    )
                }
            }, 
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val baseStrokeWidthPx = 36.dp.toPx()
            val diameter = size.minDimension - 80.dp.toPx()
            var startAngle = -90f

            categorySums.forEachIndexed { index, (_, amount) ->
                val allocatedAngle = ((amount / totalAmount) * 360.0).toFloat()
                val sweep = (allocatedAngle * entranceProgress.value - 360f * gapPercentage).coerceAtLeast(0.1f)
                
                val isSelected = selectedIndex == index
                val strokeWidth = animatedStrokes[index].value.toPx()
                
                val color = segmentColors[index % segmentColors.size]
                
                // Draw a subtle shadow/glow if selected
                if (isSelected) {
                    drawArc(
                        color = color.copy(alpha = 0.2f),
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2),
                        size = Size(diameter, diameter),
                        style = Stroke(
                            width = strokeWidth + 8.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                }

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2),
                    size = Size(diameter, diameter),
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round
                    )
                )
                startAngle += allocatedAngle
            }
        }

        // Center label with conditional icon
        val isShowingOverallTotal = selectedIndex < 0
        val displayText = if (isShowingOverallTotal) "Total" else categorySums[selectedIndex].first
        val displayAmount = if (isShowingOverallTotal) totalAmount else categorySums[selectedIndex].second
        val currencyString = displayAmount.toCurrencyString()
        
        // Auto-scale text logic: shrink if currency string is long
        val amountFontSize = when {
            currencyString.length > 12 -> 14.sp
            currencyString.length > 10 -> 16.sp
            currencyString.length > 8 -> 18.sp
            else -> 20.sp
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            if (!isShowingOverallTotal) {
                val category = Category.fromName(displayText, isExpense = true)
                val icon = category.toIcon()

                Icon(
                    imageVector = icon,
                    contentDescription = displayText,
                    modifier = Modifier.size(28.dp),
                    tint = segmentColors[selectedIndex % segmentColors.size]
                )
            } else {
                Text(
                    text = "📊",
                    fontSize = 24.sp
                )
            }

            Text(
                text = displayText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = currencyString,
                fontSize = amountFontSize,
                lineHeight = amountFontSize,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Helper: calculate arcs
private data class DrawingAngles(val start: Float, val sweep: Float) {
    fun isInsideAngle(angle: Float): Boolean {
        val normalizedStart = (start % 360 + 360) % 360
        val end = (normalizedStart + sweep) % 360
        return if (normalizedStart < end) {
            angle in normalizedStart..end
        } else {
            angle >= normalizedStart || angle <= end
        }
    }
}

private fun calculateAnglesList(
    data: List<Pair<String, Double>>,
    totalAmount: Double,
    gapPercentage: Float
): List<DrawingAngles> {
    var startAngle = 270f // Start at 12 o'clock
    return data.map { (_, amount) ->
        val allocatedSweep = (amount / totalAmount * 360.0).toFloat()
        val angle = DrawingAngles(startAngle, allocatedSweep)
        startAngle += allocatedSweep
        angle
    }
}


// Tap detection helpers
private fun handleCanvasTap(
    center: Offset,
    tapOffset: Offset,
    anglesList: List<DrawingAngles>,
    currentSelectedIndex: Int,
    onItemSelected: (Int) -> Unit = {},
    onItemDeselected: (Int) -> Unit = {},
    onNoItemSelected: () -> Unit = {}
) {
    val dx = tapOffset.x - center.x
    val dy = tapOffset.y - center.y
    val distance = sqrt(dx * dx + dy * dy)
    val tapAngle = (atan2(dy, dx) * 180f / PI.toFloat() + 360f) % 360f

    val isDistanceValid = distance > center.x * 0.2f && distance < center.x * 1.5f

    var selectedIndex = -1
    var minDistanceToCenter = Float.MAX_VALUE

    if (isDistanceValid) {
        anglesList.forEachIndexed { index, angle ->
            val minTouchSweep = 25f
            val expandedSweep = angle.sweep.coerceAtLeast(minTouchSweep)
            val expansion = (expandedSweep - angle.sweep) / 2f
            val touchArea = DrawingAngles(angle.start - expansion, expandedSweep)

            if (touchArea.isInsideAngle(tapAngle)) {
                val centerAngle = (angle.start + angle.sweep / 2f) % 360f
                var diff = abs(tapAngle - centerAngle)
                if (diff > 180f) diff = 360f - diff
                
                if (diff < minDistanceToCenter) {
                    minDistanceToCenter = diff
                    selectedIndex = index
                }
            }
        }
    }

    if (selectedIndex >= 0) {
        if (currentSelectedIndex != selectedIndex) {
            // Selected a new item
            if (currentSelectedIndex >= 0) {
                onItemDeselected(currentSelectedIndex)
            }
            onItemSelected(selectedIndex)
        } else {
            // Tapped already selected item -> Toggle off
            onItemDeselected(currentSelectedIndex)
            onNoItemSelected()
        }
    } else if (currentSelectedIndex >= 0) {
        // Tapped empty space or outside valid radius -> Deselect
        onItemDeselected(currentSelectedIndex)
        onNoItemSelected()
    }
}
