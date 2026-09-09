package com.fintrack.shared.feature.summary.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fintrack.shared.feature.core.ui.AnimatedShimmerBox
import com.fintrack.shared.feature.core.ui.CommonErrorState
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.core.util.toDouble
import com.fintrack.shared.feature.core.util.toInt
import com.fintrack.shared.feature.navigation.ui.toCurrencyString
import com.fintrack.shared.feature.summary.domain.model.Correlation
import com.fintrack.shared.feature.summary.domain.model.StatisticsSummary
import com.fintrack.shared.feature.summary.domain.model.TabType
import com.fintrack.shared.feature.summary.ui.util.toFormattedDate
import com.fintrack.shared.feature.summary.ui.util.toMonthName
import com.fintrack.shared.ui.theme.GreenIncome
import com.fintrack.shared.ui.theme.PinkExpense
import com.fintrack.shared.ui.theme.SegmentColor1
import com.fintrack.shared.ui.theme.SegmentColor2
import com.fintrack.shared.ui.theme.SegmentColor3
import com.fintrack.shared.ui.theme.SegmentColor4
import com.fintrack.shared.ui.theme.SegmentColor5
import com.ionspin.kotlin.bignum.decimal.BigDecimal

@Composable
fun SpendingHighlightsSection(
    tabType: TabType,
    highlightsResult: Result<StatisticsSummary>,
    loadHighlights: () -> Unit
) {
    val sectionTitle = when (tabType) {
        TabType.Income -> "Income Overview"
        TabType.Expense -> "Spending Overview"
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = sectionTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (highlightsResult is Result.Success && !highlightsResult.data.isCurrent) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "From ${extractYear(highlightsResult.data.period)}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            if (highlightsResult is Result.Success) {
                Text(
                    text = extractYear(highlightsResult.data.period),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (highlightsResult.data.isCurrent)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.End,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.heightIn(min = 264.dp)) {
            Crossfade(
                targetState = highlightsResult,
                animationSpec = tween(durationMillis = 300)
            ) { result ->
                when (result) {
                    is Result.Loading -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            LoadingHealthSummaryCard()

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                LoadingHighlightCard(modifier = Modifier.weight(1f))
                                LoadingHighlightCard(modifier = Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                LoadingHighlightCard(modifier = Modifier.weight(1f))
                                LoadingHighlightCard(modifier = Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                LoadingHighlightCard(modifier = Modifier.weight(1f))
                                LoadingHighlightCard(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    is Result.Error -> {
                        ErrorHighlightCard(
                            message = result.exception.message ?: "Failed to load highlights",
                            onRetry = loadHighlights
                        )
                    }

                    is Result.Success -> {
                        SuccessContent(tabType, result.data)
                    }
                }
            }
        }
    }
}

@Composable
private fun SuccessContent(
    tabType: TabType,
    data: StatisticsSummary
) {
    val highlights = when (tabType) {
        TabType.Income -> data.incomeHighlights
        TabType.Expense -> data.expenseHighlights
    }

    val isIncome = tabType == TabType.Income
    val accentColor = if (isIncome) GreenIncome else PinkExpense

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val savingsRate = highlights.savingsRate
        val essentialRatio = highlights.essentialSpendRatio

        if (savingsRate != null || essentialRatio != null) {
            HealthSummaryCard(
                savingsRate = savingsRate,
                essentialRatio = essentialRatio
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HighlightCard(
                modifier = Modifier.weight(1f),
                title = if (isIncome) "Best Month" else "Busiest Month",
                value = highlights.highestMonth?.value?.toMonthName() ?: "N/A",
                subValue = highlights.highestMonth?.amount?.toCurrencyString() ?: "$0",
                icon = Icons.Default.CalendarMonth,
                color = SegmentColor3
            )

            val volatility = highlights.highestCategory?.volatilityPercentage
            HighlightCard(
                modifier = Modifier.weight(1f),
                title = "Top Category",
                value = highlights.highestCategory?.value ?: "N/A",
                subValue = highlights.highestCategory?.amount?.toCurrencyString() ?: "$0",
                icon = Icons.Default.Category,
                color = SegmentColor4,
                badge = volatility?.let {
                    val isIncrease = it > BigDecimal.ZERO
                    val prefix = if (isIncrease) "+" else ""
                    val badgeColor = if (isIncome) {
                        if (isIncrease) GreenIncome else PinkExpense
                    } else {
                        if (isIncrease) PinkExpense else GreenIncome
                    }
                    "$prefix${it.toInt()}% vs LY" to badgeColor
                }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val ytdChange = highlights.ytdChangePercentage
            val currentYearTotal = if (isIncome) data.income else data.expense
            val yearLabel = extractYear(data.period).ifBlank { "Annual" }

            HighlightCard(
                modifier = Modifier.weight(1f),
                title = "$yearLabel Total",
                value = currentYearTotal.toCurrencyString(),
                subValue = if (ytdChange != null) {
                    val prefix = if (ytdChange > BigDecimal.ZERO) "+" else ""
                    "$prefix${ytdChange.toInt()}% vs Last Year"
                } else "So far this year",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                color = if (ytdChange != null) {
                    val isBetter = if (isIncome) ytdChange > BigDecimal.ZERO else ytdChange < BigDecimal.ZERO
                    if (isBetter) GreenIncome else PinkExpense
                } else SegmentColor1,
                badge = ytdChange?.let {
                    val prefix = if (it > BigDecimal.ZERO) "+" else ""
                    "$prefix${it.toInt()}%" to if (isIncome) (if (it > BigDecimal.ZERO) GreenIncome else PinkExpense) else (if (it > BigDecimal.ZERO) PinkExpense else GreenIncome)
                }
            )

            val exceedMonth = highlights.projectedExceedMonth
            val projected = highlights.projectedTotal ?: BigDecimal.ZERO
            val progressPercent = if (projected > BigDecimal.ZERO) {
                try {
                    ((currentYearTotal.toDouble() / projected.toDouble()) * 100).toInt()
                } catch (_: Exception) {
                    0
                }
            } else 0

            HighlightCard(
                modifier = Modifier.weight(1f),
                title = "Annual Forecast",
                value = projected.toCurrencyString(),
                subValue = if (exceedMonth != null) "Budget ends in $exceedMonth" else "Paced at $progressPercent% of est.",
                icon = Icons.AutoMirrored.Filled.ShowChart,
                color = if (exceedMonth != null) PinkExpense else SegmentColor2,
                badge = if (exceedMonth != null) "Budget Risk" to PinkExpense else if (progressPercent > 0) "$progressPercent%" to SegmentColor2 else null
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HighlightCard(
                modifier = Modifier.weight(1f),
                title = "Peak Day",
                value = highlights.highestDay?.value?.toFormattedDate() ?: "N/A",
                subValue = highlights.highestDay?.amount?.toCurrencyString() ?: "$0",
                icon = Icons.Default.Today,
                color = SegmentColor5
            )
            HighlightCard(
                modifier = Modifier.weight(1f),
                title = "Daily Avg",
                value = highlights.averagePerDay.toCurrencyString(),
                subValue = if (isIncome) "Avg. Income" else "Avg. Expense",
                icon = Icons.Default.CalendarMonth,
                color = SegmentColor1
            )
        }

        val correlations = highlights.correlations
        if (!correlations.isNullOrEmpty()) {
            correlations.forEach { correlation ->
                CorrelationCard(
                    correlation = correlation,
                    color = accentColor
                )
            }
        }
    }
}

@Composable
fun CorrelationCard(
    correlation: Correlation,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TipsAndUpdates,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Smart Insight",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = correlation.insight,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun HighlightCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subValue: String,
    icon: ImageVector,
    color: Color,
    badge: Pair<String, Color>? = null
) {
    Card(
        modifier = modifier.height(134.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(color.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (badge != null) {
                    Surface(
                        color = badge.second.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = badge.first,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = badge.second,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subValue,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = color
                )
            }
        }
    }
}

@Composable
fun HealthSummaryCard(
    savingsRate: BigDecimal?,
    essentialRatio: BigDecimal?
) {
    Card(
        modifier = Modifier.fillMaxWidth().height(134.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (savingsRate != null) {
                val isHealthy = savingsRate >= BigDecimal.fromInt(20)
                HealthMetricItem(
                    modifier = Modifier.weight(1f),
                    title = "Income Saved",
                    value = "${savingsRate.toInt()}%",
                    subValue = "Portion kept",
                    icon = Icons.AutoMirrored.Filled.ShowChart,
                    color = if (isHealthy) GreenIncome else SegmentColor2,
                    badge = if (isHealthy) "Healthy" else null
                )
            }

            if (savingsRate != null && essentialRatio != null) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        .padding(vertical = 8.dp)
                )
            }

            if (essentialRatio != null) {
                val isGood = essentialRatio <= BigDecimal.fromInt(50)
                HealthMetricItem(
                    modifier = Modifier.weight(1f),
                    title = "Needs & Bills",
                    value = "${essentialRatio.toInt()}%",
                    subValue = "Rent & Food",
                    icon = Icons.Default.Category,
                    color = if (isGood) GreenIncome else PinkExpense,
                    badge = if (isGood) "Good" else null
                )
            }
        }
    }
}

@Composable
private fun HealthMetricItem(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subValue: String,
    icon: ImageVector,
    color: Color,
    badge: String? = null
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (badge != null) {
                Surface(
                    color = color.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = badge,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subValue,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LoadingHealthSummaryCard() {
    Card(
        modifier = Modifier.fillMaxWidth().height(134.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LoadingHealthMetricItem(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            )
            LoadingHealthMetricItem(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun LoadingHealthMetricItem(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnimatedShimmerBox(modifier = Modifier.size(36.dp).clip(CircleShape))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AnimatedShimmerBox(
                modifier = Modifier.width(60.dp).height(10.dp).clip(RoundedCornerShape(4.dp))
            )
            AnimatedShimmerBox(
                modifier = Modifier.width(40.dp).height(14.dp).clip(RoundedCornerShape(4.dp))
            )
            AnimatedShimmerBox(
                modifier = Modifier.width(50.dp).height(10.dp).clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
fun LoadingHighlightCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(134.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                AnimatedShimmerBox(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                AnimatedShimmerBox(
                    modifier = Modifier
                        .width(60.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                AnimatedShimmerBox(
                    modifier = Modifier
                        .width(100.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                AnimatedShimmerBox(
                    modifier = Modifier
                        .width(80.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

@Composable
fun ErrorHighlightCard(
    message: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        CommonErrorState(
            modifier = Modifier.padding(16.dp),
            title = "Oops! Something went wrong",
            errorMessage = message,
            onRetry = onRetry
        )
    }
}

private fun extractYear(period: String): String {
    return period.split("-").firstOrNull() ?: period
}
