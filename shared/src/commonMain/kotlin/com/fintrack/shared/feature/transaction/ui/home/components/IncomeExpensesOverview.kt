package com.fintrack.shared.feature.transaction.ui.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.navigation.ui.toCurrencyString
import com.fintrack.shared.feature.summary.domain.model.DaySummary
import com.fintrack.shared.feature.summary.domain.model.OverviewSummary
import com.fintrack.shared.ui.theme.GreenIncome
import com.fintrack.shared.ui.theme.PinkExpense
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month

@Composable
fun IncomeExpensesOverview(overviewResult: Result<OverviewSummary>) {
    var selectedPeriod by remember { mutableStateOf(OverviewPeriod.Weekly) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        AnimatedContent(
            targetState = overviewResult,
            transitionSpec = {
                fadeIn(animationSpec = tween(500)) togetherWith
                        fadeOut(animationSpec = tween(500))
            },
            label = "OverviewContent"
        ) { result ->
            when (result) {
                is Result.Loading -> {
                    OverviewLoadingState(
                        selectedPeriod = selectedPeriod,
                        onPeriodSelected = { period -> selectedPeriod = period }
                    )
                }

                is Result.Error -> {
                    OverviewErrorState(
                        selectedPeriod = selectedPeriod,
                        onPeriodSelected = { period -> selectedPeriod = period }
                    )
                }

                is Result.Success -> {
                    OverviewSuccessState(
                        overview = result.data,
                        selectedPeriod = selectedPeriod,
                        onPeriodSelected = { period -> selectedPeriod = period }
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewLoadingState(
    selectedPeriod: OverviewPeriod,
    onPeriodSelected: (OverviewPeriod) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
    ) {
        OverviewHeader(
            selectedPeriod = selectedPeriod,
            onPeriodSelected = onPeriodSelected
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = selectedPeriod,
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
                transitionSpec = {
                    fadeIn(animationSpec = tween(400)) togetherWith
                            fadeOut(animationSpec = tween(400))
                }
            ) { period ->
                when (period) {
                    OverviewPeriod.Weekly -> LoadingBarChart()
                    OverviewPeriod.Monthly -> LoadingLineChart()
                }
            }
        }
    }
}

@Composable
private fun OverviewErrorState(
    selectedPeriod: OverviewPeriod,
    onPeriodSelected: (OverviewPeriod) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
    ) {
        OverviewHeader(
            selectedPeriod = selectedPeriod,
            onPeriodSelected = onPeriodSelected
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Failed to load overview",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun OverviewSuccessState(
    overview: OverviewSummary,
    selectedPeriod: OverviewPeriod,
    onPeriodSelected: (OverviewPeriod) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
    ) {
        OverviewHeader(
            selectedPeriod = selectedPeriod,
            onPeriodSelected = onPeriodSelected,
            periodName = overview.period,
            isCurrent = overview.isCurrent,
            data = if (selectedPeriod == OverviewPeriod.Weekly) overview.weeklyOverview else overview.monthlyOverview
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        ) {
            AnimatedContent(
                targetState = selectedPeriod,
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.92f)) togetherWith
                            fadeOut(animationSpec = tween(500))
                }
            ) { period ->
                when (period) {
                    OverviewPeriod.Weekly -> {
                        BarChart(
                            data = overview.weeklyOverview,
                            modifier = Modifier
                                .fillMaxHeight()
                        )
                    }

                    OverviewPeriod.Monthly -> {
                        LineChart(
                            data = overview.monthlyOverview,
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(bottom = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewHeader(
    selectedPeriod: OverviewPeriod,
    onPeriodSelected: (OverviewPeriod) -> Unit,
    periodName: String? = null,
    isCurrent: Boolean = true,
    data: List<DaySummary> = emptyList()
) {
    val periodInfo = remember(selectedPeriod, periodName, data) {
        formatOverviewPeriod(selectedPeriod, periodName ?: "", data)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Overview",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                if (periodInfo.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCurrent) periodInfo else "Older ($periodInfo)",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCurrent) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(GreenIncome, CircleShape))
                Text(
                    " Income",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(12.dp))
                Box(Modifier.size(8.dp).background(PinkExpense, CircleShape))
                Text(
                    " Expenses",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OverviewPeriod.entries.forEach { period ->
                    val isSelected = period == selectedPeriod
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { onPeriodSelected(period) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = period.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BarChart(
    data: List<DaySummary>,
    modifier: Modifier = Modifier
) {
    val totalBarHeight = 180.dp
    val barWidth = 16.dp
    val density = LocalDensity.current
    
    // 7 bars even if data is partial
    val maxTotalBD = (data.maxOfOrNull { it.income + it.expense } ?: BigDecimal.fromInt(1))
    val maxTotal = maxTotalBD.doubleValue(false) * 1.2
    val totalBarHeightPx = with(density) { totalBarHeight.toPx() }

    var selectedAmount by remember { mutableStateOf<BigDecimal?>(null) }
    var selectedColor by remember { mutableStateOf(Color.Transparent) }
    var touchOffset by remember { mutableStateOf(Offset.Zero) }
    var barsWidth by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { selectedAmount = null }
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(end = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                // Y-axis labels
                Column(
                    modifier = Modifier.width(28.dp).height(totalBarHeight),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    val levels = 5
                    val step = maxTotal / levels
                    for (i in levels downTo 0) {
                        val value = step * i
                        val text = when {
                            value >= 1000 -> {
                                val kValue = value / 1000.0
                                if (kValue >= 100) "${kValue.toInt()}k"
                                else if (kValue % 1.0 == 0.0) "${kValue.toInt()}k"
                                else "${(kValue * 10).toInt() / 10.0}k"
                            }
                            else -> value.toInt().toString()
                        }
                        Text(
                            text = text,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Interactive Bars Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(totalBarHeight)
                        .onGloballyPositioned { barsWidth = it.size.width.toFloat() }
                        .pointerInput(data, maxTotal) {
                            detectTapGestures { offset ->
                                if (barsWidth <= 0 || data.isEmpty()) return@detectTapGestures
                                
                                val barAreaWidth = barsWidth / data.size.toFloat()
                                val index = (offset.x / barAreaWidth).toInt().coerceIn(0, data.size - 1)
                                val day = data[index]
                                
                                val incomeH = (day.income.doubleValue(false) / maxTotal).toFloat() * totalBarHeightPx
                                val expenseH = (day.expense.doubleValue(false) / maxTotal).toFloat() * totalBarHeightPx
                                
                                val incomeTop = totalBarHeightPx - incomeH
                                val expenseTop = incomeTop - expenseH

                                when (offset.y) {
                                    in (expenseTop - 10.dp.toPx())..incomeTop -> {
                                        selectedAmount = day.expense
                                        selectedColor = PinkExpense
                                        touchOffset = offset
                                    }
                                    in incomeTop..(totalBarHeightPx + 10.dp.toPx()) -> {
                                        selectedAmount = day.income
                                        selectedColor = GreenIncome
                                        touchOffset = offset
                                    }
                                    else -> {
                                        selectedAmount = null
                                    }
                                }
                            }
                        }
                ) {
                    // Visual Bars
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        data.forEach { day ->
                            val incomeHeightFraction = (day.income.doubleValue(false) / maxTotal).toFloat()
                            val expenseHeightFraction = (day.expense.doubleValue(false) / maxTotal).toFloat()

                            Box(
                                modifier = Modifier
                                    .width(barWidth)
                                    .height(totalBarHeight)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                // Income bar
                                if (incomeHeightFraction > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxHeight(incomeHeightFraction)
                                            .width(barWidth)
                                            .background(GreenIncome, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                    )
                                }

                                // Expense stacked on income
                                if (expenseHeightFraction > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxHeight(expenseHeightFraction)
                                            .width(barWidth)
                                            .offset(y = -totalBarHeight * incomeHeightFraction)
                                            .background(PinkExpense, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    )
                                }
                            }
                        }
                    }

                    // Popup Overlay
                    androidx.compose.animation.AnimatedVisibility(
                        visible = selectedAmount != null,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut(),
                        modifier = Modifier.offset {
                            IntOffset(
                                x = (touchOffset.x - 40.dp.toPx()).toInt(),
                                y = (touchOffset.y - 45.dp.toPx()).toInt()
                            )
                        }
                    ) {
                        Surface(
                            color = selectedColor,
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = 4.dp
                        ) {
                            Text(
                                text = selectedAmount?.toCurrencyString() ?: "",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Day labels
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 36.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                data.forEach { day ->
                    val dateLabel = day.date.split("-").last()
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

enum class OverviewPeriod {
    Weekly, Monthly
}

private fun formatOverviewPeriod(
    selectedPeriod: OverviewPeriod,
    periodCode: String,
    data: List<DaySummary>
): String {
    val firstDate = data.firstOrNull()?.date?.let {
        try { LocalDate.parse(it) } catch (_: Exception) { null }
    } ?: return ""

    return when (selectedPeriod) {
        OverviewPeriod.Weekly -> {
            val weekPart = if (periodCode.contains("-W")) {
                "W${periodCode.split("-W")[1]} · "
            } else ""
            
            "$weekPart${getMonthName(firstDate.month)} ${firstDate.year}"
        }
        OverviewPeriod.Monthly -> {
            "${getMonthName(firstDate.month)} ${firstDate.year}"
        }
    }
}

private fun getMonthName(month: Month): String = when (month) {
    Month.JANUARY -> "January"
    Month.FEBRUARY -> "February"
    Month.MARCH -> "March"
    Month.APRIL -> "April"
    Month.MAY -> "May"
    Month.JUNE -> "June"
    Month.JULY -> "July"
    Month.AUGUST -> "August"
    Month.SEPTEMBER -> "September"
    Month.OCTOBER -> "October"
    Month.NOVEMBER -> "November"
    Month.DECEMBER -> "December"
}
