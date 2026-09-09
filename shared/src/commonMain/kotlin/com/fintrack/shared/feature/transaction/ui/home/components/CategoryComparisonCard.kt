package com.fintrack.shared.feature.transaction.ui.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.category.domain.model.fromName
import com.fintrack.shared.feature.category.ui.util.toColor
import com.fintrack.shared.feature.category.ui.util.toIcon
import com.fintrack.shared.feature.core.ui.AnimatedShimmerBox
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.core.util.formatToSinglePrecision
import com.fintrack.shared.feature.navigation.ui.toCurrencyString
import com.fintrack.shared.feature.summary.domain.model.CategoryComparison
import com.fintrack.shared.feature.summary.domain.model.CategoryComparisonSummary
import com.ionspin.kotlin.bignum.decimal.BigDecimal

@Composable
fun CategoryComparisonCard(
    categoryComparisonResult: Result<CategoryComparisonSummary>,
    accountId: String? = null,
    modifier: Modifier = Modifier
) {
    var lastSummary by remember(accountId) { mutableStateOf<CategoryComparisonSummary?>(null) }
    if (categoryComparisonResult is Result.Success) {
        lastSummary = categoryComparisonResult.data
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Category Trends",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                val displayPeriod = (categoryComparisonResult as? Result.Success)?.data?.let { it.period to it.isCurrent }
                    ?: lastSummary?.let { it.period to it.isCurrent }

                if (displayPeriod != null) {
                    val (period, isCurrent) = displayPeriod
                    
                    Text(
                        text = if (isCurrent) formatPeriod(period) else "Older (${formatPeriod(period)})",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCurrent) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedContent(
                targetState = categoryComparisonResult,
                transitionSpec = {
                    fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) togetherWith
                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow))
                },
                label = "CategoryComparisonContent"
            ) { result ->
                when (result) {
                    is Result.Loading -> {
                        val currentData = lastSummary?.data
                        if (currentData != null) {
                            Column {
                                val sortedData = currentData.sortedWith(
                                    compareByDescending<CategoryComparison> { it.isIncome }
                                        .thenBy { it.category == "Transaction Fees" }
                                )
                                sortedData.forEachIndexed { index, comparison ->
                                    CategoryComparisonItem(
                                        comparison = comparison,
                                        isCurrent = lastSummary?.isCurrent ?: true,
                                        isLast = index == sortedData.lastIndex
                                    )
                                }
                            }
                        } else {
                            Column {
                                repeat(2) { index ->
                                    LoadingCategoryComparisonItem(isLast = index == 1)
                                }
                            }
                        }
                    }

                    is Result.Error -> {
                        CategoryComparisonErrorState(
                            onRetry = { /* Add retry logic */ }
                        )
                    }

                    is Result.Success -> {
                        if (result.data.data.isEmpty()) {
                            CategoryComparisonEmptyState()
                        } else {
                            // Sort: Income first, then Expense, with Transaction Fees at the very bottom
                            val sortedData = result.data.data.sortedWith(
                                compareByDescending<CategoryComparison> { it.isIncome }
                                    .thenBy { it.category == "Transaction Fees" }
                                    .thenByDescending { it.currentTotal }
                            )

                            Column {
                                sortedData.forEachIndexed { index, comparison ->
                                    CategoryComparisonItem(
                                        comparison = comparison,
                                        isCurrent = result.data.isCurrent,
                                        isLast = index == sortedData.lastIndex
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

private fun formatPeriod(period: String): String {
    return try {
        if (period.contains("-W")) {
            // 2024-W25 -> Week 25, 2024
            val parts = period.split("-W")
            "Week ${parts[1]}, ${parts[0]}"
        } else if (period.count { it == '-' } == 1) {
            // 2024-06 -> June 2024
            val parts = period.split("-")
            val year = parts[0]
            val month = when (parts[1]) {
                "01" -> "January"
                "02" -> "February"
                "03" -> "March"
                "04" -> "April"
                "05" -> "May"
                "06" -> "June"
                "07" -> "July"
                "08" -> "August"
                "09" -> "September"
                "10" -> "October"
                "11" -> "November"
                "12" -> "December"
                else -> parts[1]
            }
            "$month $year"
        } else {
            period
        }
    } catch (_: Exception) {
        period
    }
}

@Composable
private fun CategoryComparisonItem(
    comparison: CategoryComparison,
    isCurrent: Boolean,
    isLast: Boolean
) {
    val category = Category.fromName(
        comparison.category,
        !comparison.isIncome
    )
    val icon = category.toIcon()
    val bgColor = category.toColor().copy(alpha = 0.15f)
    val iconTint = category.toColor()

    // Monthly Trend (Primary)
    val monthlyPositive = comparison.changePercentage >= BigDecimal.ZERO
    val monthlyChangeColor = if (monthlyPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    
    val monthlyChangeText = if (isCurrent) {
        if (monthlyPositive) {
            "${comparison.changePercentage.formatToSinglePrecision()}% more than last month"
        } else {
            "${(comparison.changePercentage.negate()).formatToSinglePrecision()}% less than last month"
        }
    } else {
        "${comparison.changePercentage.formatToSinglePrecision().removePrefix("-")}% vs previous month"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(bgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = comparison.category,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Content area
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = comparison.category,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = comparison.currentTotal.toCurrencyString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Change indicators
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Monthly Badge
                    TrendBadge(text = monthlyChangeText, color = monthlyChangeColor, isPositive = monthlyPositive)
                    
                    // Weekly Context (If available)
                    comparison.weeklyChangePercentage?.let { weeklyChange ->
                        val weeklyPositive = weeklyChange >= BigDecimal.ZERO
                        val weeklyColor = if (weeklyPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        val weeklyComparisonText = if (isCurrent) "last week" else "previous week"
                        
                        val weeklyPercentText = if (isCurrent) {
                            if (weeklyPositive) {
                                "${weeklyChange.formatToSinglePrecision()}% more than $weeklyComparisonText"
                            } else {
                                "${(weeklyChange.negate()).formatToSinglePrecision()}% less than $weeklyComparisonText"
                            }
                        } else {
                            "${weeklyChange.formatToSinglePrecision().removePrefix("-")}% vs $weeklyComparisonText"
                        }
                        
                        val weeklyAmount = comparison.weeklyCurrentTotal?.toCurrencyString() ?: ""
                        val weeklyFullText = if (weeklyAmount.isNotEmpty()) {
                            "Weekly: $weeklyAmount ($weeklyPercentText)"
                        } else {
                            "Weekly: $weeklyPercentText"
                        }
                        
                        Text(
                            text = weeklyFullText,
                            style = MaterialTheme.typography.labelSmall,
                            color = weeklyColor.copy(alpha = 0.8f),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }

        if (!isLast) {
            HorizontalDivider(
                modifier = Modifier.padding(top = 12.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
private fun TrendBadge(text: String, color: Color, isPositive: Boolean) {
    val arrowIcon = if (isPositive)
        Icons.AutoMirrored.Outlined.TrendingUp
    else
        Icons.AutoMirrored.Outlined.TrendingDown

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = arrowIcon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LoadingCategoryComparisonItem(isLast: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedShimmerBox(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AnimatedShimmerBox(
                        modifier = Modifier
                            .width(100.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    AnimatedShimmerBox(
                        modifier = Modifier
                            .width(80.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                AnimatedShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.height(2.dp))

                AnimatedShimmerBox(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .width(180.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }

        if (!isLast) {
            HorizontalDivider(
                modifier = Modifier.padding(top = 12.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
private fun CategoryComparisonErrorState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Unable to load trends",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Check your connection and try again",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = "Retry",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try Again")
        }
    }
}

@Composable
private fun CategoryComparisonEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Analytics,
                contentDescription = "No Data",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No trends available",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Transaction data will appear here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
