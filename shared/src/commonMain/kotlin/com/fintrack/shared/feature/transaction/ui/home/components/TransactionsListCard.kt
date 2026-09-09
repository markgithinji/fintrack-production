package com.fintrack.shared.feature.transaction.ui.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.category.domain.model.fromId
import com.fintrack.shared.feature.category.ui.util.toColor
import com.fintrack.shared.feature.category.ui.util.toIcon
import com.fintrack.shared.feature.core.ui.AnimatedShimmerBox
import com.fintrack.shared.feature.core.ui.CommonErrorState
import com.fintrack.shared.feature.core.ui.LocalSharedTransitionScope
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.navigation.ui.LocalTimeFormat
import com.fintrack.shared.feature.navigation.ui.toCurrencyString
import com.fintrack.shared.feature.settings.domain.util.format
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.fintrack.shared.ui.theme.GreenIncome
import com.fintrack.shared.ui.theme.PinkExpense
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TransactionsListCard(
    transactionsResult: Result<List<Transaction>>,
    onViewAllClick: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    accountId: String? = null,
    onTransactionClick: (Transaction) -> Unit = {},
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current

    var lastTransactions by remember(accountId) { mutableStateOf<List<Transaction>?>(null) }
    if (transactionsResult is Result.Success) {
        lastTransactions = transactionsResult.data
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (sharedTransitionScope != null) {
                    with(sharedTransitionScope) {
                        Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "all_transactions_card"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(24.dp))
                        )
                    }
                } else Modifier
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            RecentTransactionsHeader(
                transactionsResult = transactionsResult,
                onViewAllClick = onViewAllClick
            )

            AnimatedContent(
                targetState = transactionsResult,
                transitionSpec = {
                    fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) togetherWith
                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow))
                },
                label = "TransactionsListContent"
            ) { result ->
                when (result) {
                    is Result.Loading -> {
                        val currentData = lastTransactions
                        if (currentData != null && currentData.isNotEmpty()) {
                            TransactionsListContent(
                                transactions = currentData,
                                animatedVisibilityScope = animatedVisibilityScope,
                                onTransactionClick = onTransactionClick
                            )
                        } else {
                            TransactionsLoadingState()
                        }
                    }

                    is Result.Error -> {
                        TransactionsErrorState(
                            error = result.exception,
                            onRetry = onRetry
                        )
                    }

                    is Result.Success -> {
                        val transactions = result.data
                        if (transactions.isEmpty()) {
                            TransactionsEmptyState()
                        } else {
                            TransactionsListContent(
                                transactions = transactions,
                                animatedVisibilityScope = animatedVisibilityScope,
                                onTransactionClick = onTransactionClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentTransactionsHeader(
    transactionsResult: Result<List<Transaction>>,
    onViewAllClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)
            .height(48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Recent Transactions",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )

        when (transactionsResult) {
            is Result.Loading -> {
                // Header remains clean while content shimmers
            }

            is Result.Success -> {
                if (transactionsResult.data.isNotEmpty()) {
                    Surface(
                        onClick = onViewAllClick,
                        shape = RoundedCornerShape(16.dp),
                        color = GreenIncome.copy(alpha = 0.1f),
                        contentColor = GreenIncome
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "View All",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            else -> {
                // Don't show anything for Error state
            }
        }
    }
}

@Composable
private fun TransactionsLoadingState() {
    Column {
        repeat(6) { index ->
            RecentTransactionLoadingItem()
            if (index < 5) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 80.dp, end = 20.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
private fun RecentTransactionLoadingItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedShimmerBox(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            AnimatedShimmerBox(
                modifier = Modifier
                    .width(120.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(2.dp))

            AnimatedShimmerBox(
                modifier = Modifier
                    .width(80.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            horizontalAlignment = Alignment.End
        ) {
            AnimatedShimmerBox(
                modifier = Modifier
                    .width(70.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(2.dp))

            AnimatedShimmerBox(
                modifier = Modifier
                    .width(40.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun TransactionsErrorState(
    error: Throwable,
    onRetry: () -> Unit
) {
    CommonErrorState(
        modifier = Modifier.padding(bottom = 8.dp),
        title = "Failed to load transactions",
        error = error,
        onRetry = onRetry
    )
}

@Composable
private fun TransactionsEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
            contentDescription = "No Transactions",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(32.dp)
        )

        Text(
            text = "No recent transactions",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun TransactionsListContent(
    transactions: List<Transaction>,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onTransactionClick: (Transaction) -> Unit
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current

    Column {
        transactions.forEachIndexed { index, transaction ->
            if (sharedTransitionScope != null) {
                with(sharedTransitionScope) {
                    TransactionRow(
                        transaction = transaction,
                        onClick = { onTransactionClick(transaction) },
                        modifier = Modifier.sharedBounds(
                            rememberSharedContentState(key = "transaction_header_${transaction.id}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ ->
                                spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            },
                            clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(12.dp))
                        )
                    )
                }
            } else {
                TransactionRow(
                    transaction = transaction,
                    onClick = { onTransactionClick(transaction) }
                )
            }

            if (index < transactions.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 80.dp, end = 20.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun TransactionRow(
    transaction: Transaction,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val category = Category.fromId(
        transaction.categoryId,
        name = transaction.category,
        isExpense = !transaction.isIncome
    )
    val isExpense = !transaction.isIncome
    val amountColor = if (isExpense) PinkExpense else GreenIncome
    val timeFormat = LocalTimeFormat.current
    val localDateTime = transaction.dateTime.toLocalDateTime(TimeZone.currentSystemDefault())
    val formattedTime = localDateTime.time.format(timeFormat)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = category.toColor().copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = category.toIcon(),
                contentDescription = category.name,
                tint = category.toColor(),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            transaction.description?.let { description ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "${if (isExpense) "-" else "+"}${transaction.amount.toCurrencyString()}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = formattedTime,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}