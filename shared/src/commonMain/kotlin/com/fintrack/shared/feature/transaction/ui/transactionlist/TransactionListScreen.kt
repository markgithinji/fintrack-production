package com.fintrack.shared.feature.transaction.ui.transactionlist

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.*
import com.fintrack.shared.feature.core.ui.LocalSharedTransitionScope
import com.fintrack.shared.feature.core.ui.util.rememberThrottleClick
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.core.util.formatAsHeaderDate
import com.fintrack.shared.feature.summary.domain.model.TransactionCountSummary
import com.fintrack.shared.feature.summary.ui.StatisticsViewModel
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.fintrack.shared.feature.transaction.ui.TransactionViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TransactionListScreen(
    accountId: String,
    refreshTrigger: Int = 0,
    isIncome: Boolean? = null,
    categoryId: String? = null,
    categoryName: String? = null,
    startDate: String? = null,
    endDate: String? = null,
    hasTransactionCost: Boolean? = null,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    animatedVisibilityScope: AnimatedVisibilityScope,
    onEditTransaction: (String) -> Unit,
    transactionsViewModel: TransactionViewModel = koinViewModel(),
    statisticsViewModel: StatisticsViewModel = koinViewModel()
) {
    val transactionCounts by statisticsViewModel.transactionCounts.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val sharedTransitionScope = LocalSharedTransitionScope.current

    val sharedBoundsKey =
        remember(accountId, isIncome, categoryId, categoryName, hasTransactionCost) {
            when {
                hasTransactionCost == true -> "header_card_Transaction Fees"
                categoryName?.contains(",") == true -> "header_card_Others"
                categoryName != null -> "header_card_$categoryName"
                categoryId != null -> "header_card_$categoryId"
                isIncome == true -> "income_card"
                isIncome == false -> "expense_card"
                else -> "all_transactions_card"
            }
        }

    val transactions =
        remember(accountId, isIncome, categoryId, startDate, endDate, hasTransactionCost) {
            transactionsViewModel.getTransactionsPagingData(
                accountId = accountId,
                isIncome = isIncome,
                categoryId = categoryId,
                startDate = startDate,
                endDate = endDate,
                hasTransactionCost = hasTransactionCost
            )
        }.collectAsLazyPagingItems()

    LaunchedEffect(Unit) {
        transactionsViewModel.refreshCategories()
    }

    LaunchedEffect(
        accountId,
        isIncome,
        categoryId,
        startDate,
        endDate,
        hasTransactionCost,
        refreshTrigger
    ) {
        statisticsViewModel.loadTransactionCounts(
            accountId = accountId,
            isIncome = isIncome,
            categoryId = categoryId,
            start = startDate,
            end = endDate,
            hasCost = hasTransactionCost
        )

        // Also refresh paging data if this is a manual refresh trigger
        if (refreshTrigger > 0) {
            transactions.refresh()
        }
    }

    val bottomPadding = paddingValues.calculateBottomPadding()
    var lastBottomPadding by remember { mutableStateOf(0.dp) }
    SideEffect {
        if (bottomPadding > 0.dp) {
            lastBottomPadding = bottomPadding
        }
    }

    val transition = animatedVisibilityScope.transition
    val isTransitionRunning = transition.isRunning
    val isExiting = transition.targetState == androidx.compose.animation.EnterExitState.PostExit ||
            transition.targetState == androidx.compose.animation.EnterExitState.PreEnter

    val stableBottomPadding =
        if (isTransitionRunning || isExiting || (bottomPadding == 0.dp && lastBottomPadding > 0.dp)) {
            lastBottomPadding
        } else {
            bottomPadding
        }

    var includeFees by remember { mutableStateOf(true) }
    val throttledOnEditTransaction = rememberThrottleClick(onClick = onEditTransaction)

    val onRetry = {
        statisticsViewModel.loadTransactionCounts(
            accountId = accountId,
            isIncome = isIncome,
            categoryId = categoryId,
            start = startDate,
            end = endDate,
            hasCost = hasTransactionCost,
            force = true
        )
        transactions.retry()
    }

    TransactionListContent(
        transactionCounts = transactionCounts,
        transactions = transactions,
        isIncome = isIncome,
        categoryId = categoryId,
        categoryName = categoryName,
        hasTransactionCost = hasTransactionCost,
        includeFees = includeFees,
        onIncludeFeesChange = { includeFees = it },
        listState = listState,
        paddingValues = paddingValues,
        animatedVisibilityScope = animatedVisibilityScope,
        sharedTransitionScope = sharedTransitionScope,
        onTransactionClick = throttledOnEditTransaction,
        onRetry = onRetry,
        modifier = (if (sharedTransitionScope != null) {
            with(sharedTransitionScope) {
                Modifier.sharedBounds(
                    rememberSharedContentState(key = sharedBoundsKey),
                    animatedVisibilityScope = animatedVisibilityScope,
                    clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(0.dp))
                )
            }
        } else Modifier)
            .padding(bottom = stableBottomPadding)
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun TransactionListContent(
    transactionCounts: Result<TransactionCountSummary>,
    transactions: LazyPagingItems<Transaction>,
    isIncome: Boolean?,
    categoryId: String?,
    categoryName: String?,
    hasTransactionCost: Boolean? = null,
    includeFees: Boolean = true,
    onIncludeFeesChange: (Boolean) -> Unit = {},
    listState: androidx.compose.foundation.lazy.LazyListState,
    paddingValues: PaddingValues,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope?,
    onTransactionClick: (String) -> Unit = {},
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp + paddingValues.calculateTopPadding(),
            end = 16.dp,
            bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        state = listState
    ) {
        item(key = "header_card") {
            TransactionCountHeaderCard(
                transactionCounts = transactionCounts,
                isIncome = isIncome,
                includeFees = includeFees,
                onIncludeFeesChange = onIncludeFeesChange,
                hasTransactionCost = hasTransactionCost,
                categoryName = categoryName ?: categoryId,
                modifier = Modifier,
                animatedVisibilityScope = animatedVisibilityScope,
                sharedTransitionScope = sharedTransitionScope
            )
        }

        item { Spacer(modifier = Modifier.height(4.dp)) }

        val refreshState = transactions.loadState.refresh

        if (refreshState is LoadState.Loading && transactions.itemCount == 0) {
            items(5) { index ->
                TransactionLoadingItem(
                    padding = PaddingValues(16.dp)
                )
            }
        } else if (refreshState is LoadState.Error && transactions.itemCount == 0) {
            item {
                TransactionListErrorState(
                    error = refreshState.error,
                    onRetry = onRetry
                )
            }
        } else if (transactions.itemCount == 0 && refreshState is LoadState.NotLoading) {
            item {
                TransactionListEmptyState(isIncome = isIncome)
            }
        } else {
            items(
                count = transactions.itemCount,
                key = { index ->
                    val t = transactions[index]
                    (t as? Transaction)?.id ?: "loading_$index"
                }
            ) { index ->
                val transaction = transactions[index] as? Transaction

                if (transaction != null) {
                    val timeZone = TimeZone.currentSystemDefault()
                    val transactionDate = transaction.dateTime.toLocalDateTime(timeZone).date

                    // Show date header if it's the first item or the date has changed
                    val prevTransaction =
                        if (index > 0) transactions[index - 1] as? Transaction else null
                    val showHeader =
                        index == 0 || (prevTransaction != null && prevTransaction.dateTime.toLocalDateTime(
                            timeZone
                        ).date != transactionDate)

                    if (showHeader) {
                        DateHeader(
                            dateString = transactionDate.formatAsHeaderDate(),
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }

                    TransactionItem(
                        transaction = transaction,
                        animatedVisibilityScope = animatedVisibilityScope,
                        includeFees = includeFees,
                        modifier = Modifier.animateItem(),
                        onClick = { transaction.id?.let { id -> onTransactionClick(id) } }
                    )
                }
            }
        }

        when (val appendState = transactions.loadState.append) {
            is LoadState.Loading -> {
                item {
                    TransactionListLoadingMoreState()
                }
            }

            is LoadState.Error -> {
                item {
                    TransactionListErrorState(
                        error = appendState.error,
                        onRetry = onRetry
                    )
                }
            }

            else -> Unit
        }
    }
}

@Composable
fun DateHeader(
    dateString: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = dateString,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 8.dp)
    )
}