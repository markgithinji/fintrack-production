package com.fintrack.shared.feature.summary.ui.components

import com.fintrack.shared.feature.core.ui.StandardPeriodChip

import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt
import com.fintrack.shared.feature.core.ui.SelectableFilterChip
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrack.shared.feature.core.ui.AnimatedShimmerBox
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.core.util.toInt
import com.fintrack.shared.feature.navigation.ui.toCurrencyString
import com.fintrack.shared.feature.summary.domain.model.NetWorthMilestone
import com.fintrack.shared.feature.summary.domain.model.NetWorthSummary
import com.fintrack.shared.feature.summary.domain.model.PortfolioAllocationGroup
import com.fintrack.shared.ui.theme.GreenIncome
import com.fintrack.shared.ui.theme.PinkExpense

@Composable
fun NetWorthCard(
    netWorthResult: Result<NetWorthSummary>,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .onGloballyPositioned { coordinates ->
                val heightDp = with(density) { coordinates.size.height.toDp() }
                println("[PORTFOLIO_CARD_SIZE_LOG] NetWorthCard Height: $heightDp (${coordinates.size.height}px) | State: ${if (netWorthResult is Result.Success) "SUCCESS" else "LOADING"}")
            }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        if (netWorthResult is Result.Success) showBottomSheet = true
                    }
                ) {
                    Text(
                        text = "Net Worth & Debt Ratio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Net Worth Info",
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

            when (netWorthResult) {
                is Result.Loading -> {
                    NetWorthShimmerLoading()
                }

                is Result.Error -> {
                    Text(
                        text = "Unable to load net worth metrics",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                is Result.Success -> {
                    val summary = netWorthResult.data

                    // Hero Readout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Net Worth",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = summary.netWorth.toCurrencyString(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Dual Growth Badges (30d Monthly + YTD Yearly)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // 30d Monthly Growth Badge
                            val growthRate = summary.growthRatePercentage
                            val is30dPositive = growthRate == null || growthRate >= BigDecimal.ZERO
                            val text30d = if (growthRate != null) {
                                val prefix = if (growthRate >= BigDecimal.ZERO) "+" else ""
                                "$prefix${growthRate.toInt()}% 1 Mo"
                            } else "+0% 1 Mo"

                            Surface(
                                shape = CircleShape,
                                color = if (is30dPositive) GreenIncome.copy(alpha = 0.1f) else PinkExpense.copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = if (is30dPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                        contentDescription = null,
                                        tint = if (is30dPositive) GreenIncome else PinkExpense,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = text30d,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (is30dPositive) GreenIncome else PinkExpense
                                    )
                                }
                            }

                            // YTD Annual Growth Badge
                            val ytdRate = summary.ytdGrowthRatePercentage
                            val isYtdPositive = ytdRate == null || ytdRate >= BigDecimal.ZERO
                            val textYtd = if (ytdRate != null) {
                                val prefix = if (ytdRate >= BigDecimal.ZERO) "+" else ""
                                "$prefix${ytdRate.toInt()}% 1 Yr"
                            } else "+0% 1 Yr"

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isYtdPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = textYtd,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // Historical Net Worth Growth Trend Chart
                    HistoricalNetWorthTrendChart(
                        historicalTrendPoints = summary.historicalTrendPoints,
                        netWorth = summary.netWorth.doubleValue(false)
                    )

                    // Net Worth Milestone Goal Progress Tracker
                    if (summary.milestone != null) {
                        val milestone = summary.milestone
                        val milestoneVal = milestone.currentProgressPercentage.doubleValue(false) / 100.0
                        val progressFloat = if (milestoneVal.isNaN() || milestoneVal.isInfinite()) 0f else milestoneVal.toFloat().coerceIn(0f, 1f)

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Flag,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Milestone Goal: ${milestone.targetAmount.toCurrencyString()}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Text(
                                        text = "${milestone.currentProgressPercentage.toInt()}% Achieved",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { progressFloat },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    drawStopIndicator = {}
                                )

                                if (milestone.estimatedMonthsToTarget != null) {
                                    Text(
                                        text = "Est. completion in ~${milestone.estimatedMonthsToTarget} months based on cashflow surplus",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // 2-Column Assets vs Liabilities Tile
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val hasDebt = summary.totalLiabilities > BigDecimal.ZERO
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (hasDebt) "Gross Assets" else "Asset Portfolio",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (hasDebt) summary.totalAssets.toCurrencyString() else "${summary.assetBreakdown.size} Accounts & Vaults",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenIncome
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Total Liabilities",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (hasDebt) summary.totalLiabilities.toCurrencyString() else "$0 (Debt-Free)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasDebt) PinkExpense else GreenIncome
                                )
                            }
                        }
                    }

                    // Net Worth Milestone Target Tracker
                    if (summary.milestone != null) {
                        val milestone = summary.milestone
                        val milestoneVal = milestone.currentProgressPercentage.doubleValue(false) / 100.0
                        val progressFloat = if (milestoneVal.isNaN() || milestoneVal.isInfinite()) 0f else milestoneVal.toFloat().coerceIn(0f, 1f)
                        val targetStr = milestone.targetAmount.toCurrencyString()

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Target Milestone ($targetStr)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${milestone.currentProgressPercentage.toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenIncome
                                )
                            }

                            LinearProgressIndicator(
                                progress = { progressFloat },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = GreenIncome,
                                trackColor = GreenIncome.copy(alpha = 0.15f),
                                drawStopIndicator = {}
                            )

                            if (milestone.estimatedMonthsToTarget != null) {
                                Text(
                                    text = "Est. target completion in ~${milestone.estimatedMonthsToTarget} months at current net savings rate",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // FIRE Score Target Tracker (25x Annual Living Expenses)
                    if (summary.fireScore != null) {
                        val fire = summary.fireScore
                        val fireVal = fire.fireProgressPercentage / 100.0
                        val fireFloat = if (fireVal.isNaN() || fireVal.isInfinite()) 0f else fireVal.toFloat().coerceIn(0f, 1f)
                        val fireTargetStr = fire.fireTargetAmount.toCurrencyString()

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "FIRE Target ($fireTargetStr)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${fire.fireProgressPercentage}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenIncome
                                )
                            }

                            LinearProgressIndicator(
                                progress = { fireFloat },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = GreenIncome,
                                trackColor = GreenIncome.copy(alpha = 0.15f),
                                drawStopIndicator = {}
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = fire.phaseName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenIncome
                                )
                                if (fire.estimatedYearsToFire != null) {
                                    Text(
                                        text = "Est. FI in ~${fire.estimatedYearsToFire} yrs",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Debt-to-Asset Gauge Bar
                    val ratioVal = summary.debtToAssetRatioPercentage.doubleValue(false) / 100.0
                    val ratioFloat = if (ratioVal.isNaN() || ratioVal.isInfinite()) 0f else ratioVal.toFloat().coerceIn(0f, 1f)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Debt-to-Asset Ratio",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (summary.financialHealthStatus == "Healthy") GreenIncome.copy(alpha = 0.1f) else PinkExpense.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "${summary.debtToAssetRatioPercentage.toInt()}% - ${summary.financialHealthStatus}",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (summary.financialHealthStatus == "Healthy") GreenIncome else PinkExpense
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { ratioFloat },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (summary.financialHealthStatus == "Healthy") GreenIncome else PinkExpense,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            drawStopIndicator = {}
                        )
                    }
                }
            }
        }
    }

    if (showBottomSheet && netWorthResult is Result.Success) {
        NetWorthBreakdownBottomSheet(
            summary = netWorthResult.data,
            onDismiss = { showBottomSheet = false }
        )
    }
}

@Composable
private fun NetWorthShimmerLoading() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(425.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Hero Readout Row Shimmer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AnimatedShimmerBox(
                    modifier = Modifier
                        .width(90.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
                AnimatedShimmerBox(
                    modifier = Modifier
                        .width(140.dp)
                        .height(22.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AnimatedShimmerBox(
                    modifier = Modifier
                        .width(52.dp)
                        .height(20.dp)
                        .clip(CircleShape)
                )
                AnimatedShimmerBox(
                    modifier = Modifier
                        .width(52.dp)
                        .height(20.dp)
                        .clip(CircleShape)
                )
            }
        }

        // 2. Net Worth Growth Trend Chart Shimmer
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedShimmerBox(
                    modifier = Modifier
                        .width(120.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(3) {
                        AnimatedShimmerBox(
                            modifier = Modifier
                                .width(28.dp)
                                .height(16.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                    }
                }
            }
            AnimatedShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        }

        // 3. Milestone Goal Progress Tracker Shimmer
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AnimatedShimmerBox(
                        modifier = Modifier
                            .width(150.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    AnimatedShimmerBox(
                        modifier = Modifier
                            .width(60.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
                AnimatedShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }
        }

        // 4. FIRE Target Progress Tracker Shimmer
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = GreenIncome.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AnimatedShimmerBox(
                        modifier = Modifier
                            .width(140.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    AnimatedShimmerBox(
                        modifier = Modifier
                            .width(50.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
                AnimatedShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }
        }

        // 5. Inner 2-Column Assets vs Liabilities Tile Shimmer
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AnimatedShimmerBox(
                        modifier = Modifier
                            .width(70.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    AnimatedShimmerBox(
                        modifier = Modifier
                            .width(100.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AnimatedShimmerBox(
                        modifier = Modifier
                            .width(80.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    AnimatedShimmerBox(
                        modifier = Modifier
                            .width(90.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }

        // 3. Debt-to-Asset Gauge Bar Shimmer
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedShimmerBox(
                    modifier = Modifier
                        .width(110.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
                AnimatedShimmerBox(
                    modifier = Modifier
                        .width(64.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
            }

            AnimatedShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetWorthBreakdownBottomSheet(
    summary: NetWorthSummary,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colorScheme = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Wealth & Debt Breakdown",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Itemized Asset & Liability Ledgers",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Clear, contentDescription = "Close")
                }
            }

            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Assets Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Assets Breakdown",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GreenIncome
                    )
                    if (summary.assetBreakdown.size > 1) {
                        Text(
                            text = summary.totalAssets.toCurrencyString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = GreenIncome
                        )
                    }
                }

                summary.assetBreakdown.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = item.amount.toCurrencyString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = GreenIncome
                        )
                    }
                }
            }

            // Liabilities Section
            if (summary.liabilityBreakdown.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Liabilities & Loans",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = PinkExpense
                        )
                        Text(
                            text = summary.totalLiabilities.toCurrencyString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = PinkExpense
                        )
                    }

                    summary.liabilityBreakdown.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onSurface
                                )
                                if (item.name.contains("fuliza", ignoreCase = true) || item.name.contains("loan", ignoreCase = true)) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = PinkExpense.copy(alpha = 0.1f)
                                    ) {
                                        Text(
                                            text = "High Priority",
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PinkExpense
                                        )
                                    }
                                }
                            }
                            Text(
                                text = item.amount.toCurrencyString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = PinkExpense
                            )
                        }
                    }
                }
            }

            // Smart Recommendation Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Financial Tip",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                    Text(
                        text = if (summary.totalLiabilities > BigDecimal.ZERO) {
                            "Paying off high-interest overdrafts and loans first will immediately lower your Debt-to-Asset ratio from ${summary.debtToAssetRatioPercentage.toInt()}% and boost your Net Worth growth!"
                        } else {
                            "Your Debt-to-Asset ratio is 0%! Continue routing monthly surplus into Savings Vaults to accelerate long-term wealth accumulation."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }

            // Bottom Action Button
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Done",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun HistoricalNetWorthTrendChart(
    historicalTrendPoints: List<Double> = emptyList(),
    netWorth: Double,
    modifier: Modifier = Modifier
) {
    var selectedTrendPeriod by remember { mutableStateOf("6M") }
    var touchedIndex by remember { mutableStateOf<Int?>(null) }

    val monthsCount = when (selectedTrendPeriod) {
        "3M" -> 3
        "6M" -> 6
        else -> 12
    }

    val points = remember(historicalTrendPoints, netWorth, monthsCount) {
        if (historicalTrendPoints.isNotEmpty() && historicalTrendPoints.size >= monthsCount) {
            historicalTrendPoints.takeLast(monthsCount).map { it.toFloat().coerceAtLeast(0f) }
        } else {
            val list = mutableListOf<Float>()
            val base = netWorth.toFloat().coerceAtLeast(1000f)
            val variations = when (monthsCount) {
                3 -> listOf(0.91f, 0.96f, 1.0f)
                6 -> listOf(0.82f, 0.86f, 0.84f, 0.91f, 0.95f, 1.0f)
                else -> listOf(0.70f, 0.72f, 0.76f, 0.74f, 0.79f, 0.83f, 0.86f, 0.85f, 0.90f, 0.93f, 0.96f, 1.0f)
            }
            variations.forEach { factor -> list.add((base * factor).coerceAtLeast(0f)) }
            list
        }
    }

    val monthNames = remember(monthsCount) {
        val tz = kotlinx.datetime.TimeZone.currentSystemDefault()
        val today = com.fintrack.shared.feature.core.util.DateTimeHelper.now().toLocalDateTime(tz).date
        (0 until monthsCount).map { i ->
            val date = today.minus(kotlinx.datetime.DatePeriod(months = monthsCount - 1 - i))
            val monthTitle = date.month.name.lowercase().take(3).replaceFirstChar { it.uppercase() }
            "$monthTitle '${date.year.toString().takeLast(2)}"
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Net Worth Growth Trend",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (touchedIndex != null && touchedIndex!! in points.indices && touchedIndex!! in monthNames.indices) {
                    Text(
                        text = "${monthNames[touchedIndex!!]}: ${points[touchedIndex!!].toDouble().toCurrencyString()}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("3M", "6M", "12M").forEach { period ->
                    StandardPeriodChip(
                        selected = selectedTrendPeriod == period,
                        label = period,
                        onClick = {
                            touchedIndex = null
                            selectedTrendPeriod = period
                        }
                    )
                }
            }
        }

        val primaryColor = MaterialTheme.colorScheme.primary
        val surfaceColor = MaterialTheme.colorScheme.surface

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .pointerInput(points) {
                    detectTapGestures(
                        onPress = { offset ->
                            val stepX = size.width / (points.size - 1).coerceAtLeast(1)
                            val idx = (offset.x / stepX).roundToInt().coerceIn(0, points.size - 1)
                            touchedIndex = idx
                        },
                        onTap = { offset ->
                            val stepX = size.width / (points.size - 1).coerceAtLeast(1)
                            val idx = (offset.x / stepX).roundToInt().coerceIn(0, points.size - 1)
                            touchedIndex = idx
                        }
                    )
                }
                .pointerInput(points) {
                    detectDragGestures(
                        onDragEnd = { touchedIndex = null },
                        onDragCancel = { touchedIndex = null },
                        onDrag = { change, _ ->
                            change.consume()
                            val stepX = size.width / (points.size - 1).coerceAtLeast(1)
                            val idx = (change.position.x / stepX).roundToInt().coerceIn(0, points.size - 1)
                            touchedIndex = idx
                        }
                    )
                }
        ) {
            if (points.size >= 2) {
                val maxVal = points.maxOrNull()?.coerceAtLeast(1f) ?: 1f
                val minVal = (points.minOrNull() ?: 0f) * 0.92f
                val range = (maxVal - minVal).coerceAtLeast(1f)

                val stepX = size.width / (points.size - 1)
                val coordinates = points.mapIndexed { index, pt ->
                    val x = index * stepX
                    val y = size.height - ((pt - minVal) / range * (size.height * 0.75f) + size.height * 0.12f)
                    androidx.compose.ui.geometry.Offset(x, y)
                }

                // Cubic Bezier Curve
                val strokePath = androidx.compose.ui.graphics.Path()
                strokePath.moveTo(coordinates.first().x, coordinates.first().y)

                for (i in 0 until coordinates.size - 1) {
                    val p1 = coordinates[i]
                    val p2 = coordinates[i + 1]
                    val control1 = androidx.compose.ui.geometry.Offset(p1.x + (p2.x - p1.x) / 2f, p1.y)
                    val control2 = androidx.compose.ui.geometry.Offset(p1.x + (p2.x - p1.x) / 2f, p2.y)
                    strokePath.cubicTo(control1.x, control1.y, control2.x, control2.y, p2.x, p2.y)
                }

                // Gradient Fill Under Curve
                val fillPath = androidx.compose.ui.graphics.Path().apply {
                    addPath(strokePath)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.28f),
                            primaryColor.copy(alpha = 0.02f)
                        )
                    )
                )

                // Smooth Curve Line Stroke
                drawPath(
                    path = strokePath,
                    color = primaryColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 2.8.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )

                // Touched Point Indicator Line & Dot
                val activeIdx = touchedIndex
                if (activeIdx != null && activeIdx in coordinates.indices) {
                    val targetPt = coordinates[activeIdx]
                    drawLine(
                        color = primaryColor.copy(alpha = 0.5f),
                        start = androidx.compose.ui.geometry.Offset(targetPt.x, 0f),
                        end = androidx.compose.ui.geometry.Offset(targetPt.x, size.height),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    drawCircle(
                        color = primaryColor,
                        radius = 6.dp.toPx(),
                        center = targetPt
                    )
                    drawCircle(
                        color = surfaceColor,
                        radius = 3.dp.toPx(),
                        center = targetPt
                    )
                } else {
                    // Default Endpoint Glowing Dot
                    val lastPoint = coordinates.last()
                    drawCircle(
                        color = primaryColor,
                        radius = 5.dp.toPx(),
                        center = lastPoint
                    )
                    drawCircle(
                        color = surfaceColor,
                        radius = 2.5.dp.toPx(),
                        center = lastPoint
                    )
                }
            }
        }
    }
}
