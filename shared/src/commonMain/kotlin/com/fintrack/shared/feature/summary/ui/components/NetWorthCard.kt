package com.fintrack.shared.feature.summary.ui.components

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
import com.fintrack.shared.feature.summary.domain.model.NetWorthSummary
import com.fintrack.shared.ui.theme.GreenIncome
import com.fintrack.shared.ui.theme.PinkExpense

@Composable
fun NetWorthCard(
    netWorthResult: Result<NetWorthSummary>,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showBottomSheet by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
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

                    // 2-Column Assets vs Liabilities Tile
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
                            Column {
                                Text(
                                    text = "Total Assets",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = summary.totalAssets.toCurrencyString(),
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
                                    text = summary.totalLiabilities.toCurrencyString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PinkExpense
                                )
                            }
                        }
                    }

                    // Debt-to-Asset Gauge Bar
                    val ratioFloat = (summary.debtToAssetRatioPercentage.doubleValue(false) / 100.0).toFloat().coerceIn(0f, 1f)
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AnimatedShimmerBox(modifier = Modifier.height(24.dp).fillMaxWidth(0.5f).clip(RoundedCornerShape(4.dp)))
        AnimatedShimmerBox(modifier = Modifier.height(60.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)))
        AnimatedShimmerBox(modifier = Modifier.height(14.dp).fillMaxWidth().clip(RoundedCornerShape(4.dp)))
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
                        text = "Assets",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GreenIncome
                    )
                    Text(
                        text = summary.totalAssets.toCurrencyString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GreenIncome
                    )
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
                                            text = "⚡ High Priority",
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
                        text = "💡 Financial Tip",
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
