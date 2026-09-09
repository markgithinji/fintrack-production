package com.fintrack.shared.feature.transaction.ui.transactionlist

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.navigation.ui.toCurrencyString
import com.fintrack.shared.feature.summary.domain.model.TransactionCountSummary
import com.fintrack.shared.ui.theme.GreenIncome
import com.fintrack.shared.ui.theme.PinkExpense
import com.ionspin.kotlin.bignum.decimal.BigDecimal

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TransactionCountHeaderCard(
    transactionCounts: Result<TransactionCountSummary>,
    isIncome: Boolean?,
    includeFees: Boolean = true,
    onIncludeFeesChange: (Boolean) -> Unit = {},
    hasTransactionCost: Boolean? = null,
    categoryName: String? = null,
    modifier: Modifier = Modifier,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedTransitionScope: SharedTransitionScope? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        when (transactionCounts) {
            is Result.Loading -> TransactionCountLoadingState()
            is Result.Error -> TransactionCountErrorState()
            is Result.Success -> TransactionCountSuccessState(
                counts = transactionCounts.data,
                isIncome = isIncome,
                includeFees = includeFees,
                onIncludeFeesChange = onIncludeFeesChange,
                hasTransactionCost = hasTransactionCost,
                categoryName = categoryName,
                animatedVisibilityScope = animatedVisibilityScope,
                sharedTransitionScope = sharedTransitionScope
            )
        }
    }
}

@Composable
private fun TransactionCountLoadingState() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = GreenIncome
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Loading summary...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TransactionCountErrorState() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Failed to load summary",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun TransactionCountSuccessState(
    counts: TransactionCountSummary,
    isIncome: Boolean?,
    includeFees: Boolean,
    onIncludeFeesChange: (Boolean) -> Unit,
    hasTransactionCost: Boolean? = null,
    categoryName: String? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedTransitionScope: SharedTransitionScope? = null
) {
    val themeColor = when {
        hasTransactionCost == true -> MaterialTheme.colorScheme.tertiary
        isIncome == true -> GreenIncome
        isIncome == false -> PinkExpense
        else -> MaterialTheme.colorScheme.primary
    }

    val sharedElementKey = remember(categoryName) {
        if (categoryName?.contains(",") == true) "Others" else categoryName
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(themeColor.copy(alpha = 0.1f))
                .then(
                    if (sharedTransitionScope != null && animatedVisibilityScope != null && sharedElementKey != null) {
                        with(sharedTransitionScope) {
                            Modifier.sharedElement(
                                rememberSharedContentState(key = "category_icon_$sharedElementKey"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = null,
                tint = themeColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                val totalDisplayAmount = remember(counts, includeFees, isIncome, hasTransactionCost) {
                    if (includeFees) {
                        when {
                            isIncome == true -> counts.totalAmount - counts.totalTransactionCost
                            isIncome == false || hasTransactionCost == true -> counts.totalAmount + counts.totalTransactionCost
                            else -> counts.totalAmount
                        }
                    } else {
                        counts.totalAmount
                    }
                }

                Text(
                    text = totalDisplayAmount.toCurrencyString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = themeColor
                )

                if (counts.totalTransactionCost > BigDecimal.ZERO) {
                    Spacer(modifier = Modifier.weight(1f))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onIncludeFeesChange(!includeFees) }
                            .background(themeColor.copy(alpha = 0.08f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Checkbox(
                            checked = includeFees,
                            onCheckedChange = onIncludeFeesChange,
                            modifier = Modifier
                                .size(12.dp)
                                .scale(0.7f),
                            colors = CheckboxDefaults.colors(checkedColor = themeColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Incl. fees",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = themeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            val countText = remember(counts, isIncome, hasTransactionCost) {
                when {
                    hasTransactionCost == true -> "${counts.totalTransactions} transactions with fees"
                    isIncome == true -> "${counts.totalIncomeTransactions} total items"
                    isIncome == false -> "${counts.totalExpenseTransactions} total items"
                    else -> "${counts.totalTransactions} total items"
                }
            }

            Text(
                text = countText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (counts.totalTransactionCost > BigDecimal.ZERO) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Total Fees: ${counts.totalTransactionCost.toCurrencyString()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = themeColor.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}