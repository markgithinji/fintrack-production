package com.fintrack.shared.feature.summary.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrack.shared.feature.core.ui.AnimatedShimmerBox
import com.fintrack.shared.feature.core.ui.CommonErrorState
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.core.util.toInt
import com.fintrack.shared.feature.navigation.ui.toCurrencyString
import com.fintrack.shared.feature.summary.domain.model.BudgetBucketSummary
import com.fintrack.shared.feature.summary.domain.model.BudgetBucketType
import com.fintrack.shared.feature.summary.domain.model.BudgetOptimizerSummary
import com.fintrack.shared.feature.summary.domain.model.CategorySpendItem
import com.fintrack.shared.feature.summary.domain.model.TrimRecommendation
import com.fintrack.shared.ui.theme.GreenIncome
import com.fintrack.shared.ui.theme.PinkExpense
import com.fintrack.shared.ui.theme.SegmentColor1
import com.fintrack.shared.ui.theme.SegmentColor2
import com.fintrack.shared.ui.theme.SegmentColor3

@Composable
fun BudgetOptimizerCard(
    optimizerResult: Result<BudgetOptimizerSummary>,
    selectedDays: Int = 30,
    onDaysSelected: (Int) -> Unit = {},
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
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        if (optimizerResult is Result.Success) showBottomSheet = true
                    }
                ) {
                    Text(
                        text = "50/30/20 Budget Optimizer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Optimizer Info",
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
            }

            // Duration Filter Chips Row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(30 to "30d", 60 to "60d", 90 to "90d", 180 to "6mo", 365 to "1yr").forEach { (days, label) ->
                    val isSelected = selectedDays == days
                    val animatedBgColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        animationSpec = tween(150),
                        label = "chipBg"
                    )
                    val animatedContentColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(150),
                        label = "chipContent"
                    )

                    Surface(
                        onClick = { onDaysSelected(days) },
                        shape = RoundedCornerShape(8.dp),
                        color = animatedBgColor,
                        border = BorderStroke(
                            width = if (isSelected) 1.2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = animatedContentColor
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

            when (optimizerResult) {
                is Result.Loading -> {
                    OptimizerShimmerLoading()
                }

                is Result.Error -> {
                    CommonErrorState(
                        title = "Unable to load budget optimization",
                        error = optimizerResult.exception,
                        onRetry = onRetry
                    )
                }

                is Result.Success -> {
                    val summary = optimizerResult.data

                    // Health Score Readout Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Rule Alignment Score",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${summary.healthScore} / 100",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (summary.healthScore >= 75) GreenIncome else if (summary.healthScore >= 50) SegmentColor2 else PinkExpense
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = if (summary.healthScore >= 75) GreenIncome.copy(alpha = 0.1f) else PinkExpense.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = when {
                                    summary.healthScore >= 80 -> "Excellent Alignment"
                                    summary.healthScore >= 60 -> "Moderate Allocation"
                                    else -> "Needs Trimming"
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (summary.healthScore >= 75) GreenIncome else PinkExpense
                            )
                        }
                    }

                    // 3-Bucket Visual Allocation Row
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Current vs Target Breakdown",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BucketChip(
                                modifier = Modifier.weight(1f),
                                label = "Needs (50%)",
                                percentage = "${summary.needs.actualPercentage.toInt()}%",
                                color = SegmentColor1
                            )
                            BucketChip(
                                modifier = Modifier.weight(1f),
                                label = "Wants (30%)",
                                percentage = "${summary.wants.actualPercentage.toInt()}%",
                                color = SegmentColor3
                            )
                            BucketChip(
                                modifier = Modifier.weight(1f),
                                label = "Savings (20%)",
                                percentage = "${summary.savings.actualPercentage.toInt()}%",
                                color = GreenIncome
                            )
                        }
                    }

                    // Primary Actionable Insight Callout Banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = summary.primaryActionableInsight,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Top Spending Trim Recommendations
                    if (summary.recommendations.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Top Smart Trim Opportunities",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            summary.recommendations.take(3).forEach { rec ->
                                TrimRecommendationItem(recommendation = rec)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBottomSheet && optimizerResult is Result.Success) {
        BudgetOptimizerBottomSheet(
            summary = optimizerResult.data,
            onDismiss = { showBottomSheet = false }
        )
    }
}

@Composable
private fun BucketChip(
    label: String,
    percentage: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = percentage,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun TrimRecommendationItem(recommendation: TrimRecommendation) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(PinkExpense.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCut,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = PinkExpense
                    )
                }

                Column {
                    Text(
                        text = recommendation.categoryName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Spent ${recommendation.currentSpend.toCurrencyString()}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = GreenIncome.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "Trim ~${recommendation.suggestedTrimAmount.toCurrencyString()}/mo",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = GreenIncome
                    )
                }
                Text(
                    text = "Saves ${recommendation.projectedAnnualSavings.toCurrencyString()}/yr",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OptimizerShimmerLoading() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AnimatedShimmerBox(modifier = Modifier.width(100.dp).height(20.dp).clip(RoundedCornerShape(4.dp)))
            AnimatedShimmerBox(modifier = Modifier.width(60.dp).height(20.dp).clip(CircleShape))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AnimatedShimmerBox(modifier = Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(10.dp)))
            AnimatedShimmerBox(modifier = Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(10.dp)))
            AnimatedShimmerBox(modifier = Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(10.dp)))
        }
        AnimatedShimmerBox(modifier = Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(12.dp)))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetOptimizerBottomSheet(
    summary: BudgetOptimizerSummary,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colorScheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

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
                .padding(bottom = 32.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
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
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "50/30/20 Budget Breakdown",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Itemized Category & Planning Vault Ledgers",
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

            // Itemized Bucket Breakdown Cards (Needs, Wants, Savings)
            BucketCategoryBreakdownCard(
                title = "Needs (Target: 50%)",
                bucketSummary = summary.needs,
                accentColor = SegmentColor1
            )

            BucketCategoryBreakdownCard(
                title = "Wants (Target: 30%)",
                bucketSummary = summary.wants,
                accentColor = SegmentColor3
            )

            BucketCategoryBreakdownCard(
                title = "Savings & Investments (Target: 20%)",
                bucketSummary = summary.savings,
                accentColor = GreenIncome
            )

            // All Trim Recommendations Detailed List
            if (summary.recommendations.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Detailed Category Trim Action Plan:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )

                    summary.recommendations.forEach { rec ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = rec.categoryName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Current: ${rec.currentSpend.toCurrencyString()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = rec.impactMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = GreenIncome
                                )
                            }
                        }
                    }
                }
            }

            // Done Action Button
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
private fun BucketCategoryBreakdownCard(
    title: String,
    bucketSummary: BudgetBucketSummary,
    accentColor: Color
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Text(
                    text = "${bucketSummary.actualAmount.toCurrencyString()} (${bucketSummary.actualPercentage.toInt()}%)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

            if (bucketSummary.categories.isNotEmpty()) {
                bucketSummary.categories.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${item.amount.toCurrencyString()} (${item.percentageOfBucket.toInt()}%)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                Text(
                    text = "No tracked entries in this bucket.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
