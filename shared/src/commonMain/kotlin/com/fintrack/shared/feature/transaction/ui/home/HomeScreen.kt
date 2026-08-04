package com.fintrack.shared.feature.transaction.ui.home

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fintrack.shared.feature.account.ui.AccountsViewModel
import com.fintrack.shared.feature.core.ui.CommonErrorState
import com.fintrack.shared.feature.core.ui.util.rememberThrottleClick
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.navigation.ui.SmsSyncSignal
import com.fintrack.shared.feature.settings.ui.SettingsViewModel
import com.fintrack.shared.feature.summary.ui.StatisticsViewModel
import com.fintrack.shared.feature.transaction.ui.ImportEvent
import com.fintrack.shared.feature.transaction.ui.TransactionViewModel
import com.fintrack.shared.feature.transaction.ui.home.components.CategoryComparisonCard
import com.fintrack.shared.feature.transaction.ui.home.components.CurrentBalanceCardWrapper
import com.fintrack.shared.feature.transaction.ui.home.components.IncomeExpenseCards
import com.fintrack.shared.feature.transaction.ui.home.components.IncomeExpensesOverview
import com.fintrack.shared.feature.transaction.ui.home.components.TransactionsListCard
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    selectedAccountId: String?,
    onAccountSelected: (String) -> Unit,
    refreshTrigger: Int,
    smsSyncSignal: SmsSyncSignal? = null,
    onGlobalRefresh: () -> Unit,
    onSmsPermissionRequired: (forceRationale: Boolean) -> Unit,
    onShowToast: (String, Boolean) -> Unit,
    accountsViewModel: AccountsViewModel = koinViewModel(),
    transactionsViewModel: TransactionViewModel = koinViewModel(),
    statsViewModel: StatisticsViewModel = koinViewModel(),
    settingsViewModel: SettingsViewModel = koinViewModel(),
    paddingValues: PaddingValues = PaddingValues(0.dp),
    animatedVisibilityScope: AnimatedVisibilityScope,
    onEditTransaction: (String) -> Unit,
    onCardClick: (accountId: String, isIncome: Boolean?) -> Unit
) {
    val accountsResult by accountsViewModel.accounts.collectAsStateWithLifecycle()
    val selectedAccountResult by accountsViewModel.selectedAccount.collectAsStateWithLifecycle()
    val transactionsResult by transactionsViewModel.recentTransactions.collectAsStateWithLifecycle()
    val overviewResult by statsViewModel.overview.collectAsStateWithLifecycle()
    val categoryComparisonResult by statsViewModel.categoryComparisons.collectAsStateWithLifecycle()
    val isBalanceHidden by settingsViewModel.isBalanceHidden.collectAsStateWithLifecycle()
    val defaultAccountId by settingsViewModel.defaultAccountId.collectAsStateWithLifecycle()
    val isMpesaListenerEnabled by settingsViewModel.isMpesaListenerEnabled.collectAsStateWithLifecycle()
    val isEquityListenerEnabled by settingsViewModel.isEquityListenerEnabled.collectAsStateWithLifecycle()
    val mpesaLinkedAccountIds by settingsViewModel.mpesaLinkedAccountIds.collectAsStateWithLifecycle()
    val equityLinkedAccountIds by settingsViewModel.equityLinkedAccountIds.collectAsStateWithLifecycle()
    val importStateMap by transactionsViewModel.importState.collectAsStateWithLifecycle()
    val importProgressMap by transactionsViewModel.importProgress.collectAsStateWithLifecycle()

    val enrichedSelectedAccount = remember(
        selectedAccountResult,
        mpesaLinkedAccountIds,
        equityLinkedAccountIds,
        defaultAccountId
    ) {
        if (selectedAccountResult is Result.Success) {
            val account = (selectedAccountResult as Result.Success).data
            val sources = mutableListOf<String>()
            if (mpesaLinkedAccountIds.contains(account.id)) sources.add("mpesa")
            if (equityLinkedAccountIds.contains(account.id)) sources.add("equity")
            Result.Success(
                account.copy(
                    isDefault = account.id == defaultAccountId,
                    linkedSources = sources
                )
            )
        } else {
            selectedAccountResult
        }
    }

    val enrichedAccountsResult =
        remember(accountsResult, mpesaLinkedAccountIds, equityLinkedAccountIds, defaultAccountId) {
            if (accountsResult is Result.Success) {
                val accounts = (accountsResult as Result.Success).data
                Result.Success(accounts.map { account ->
                    val sources = mutableListOf<String>()
                    if (mpesaLinkedAccountIds.contains(account.id)) sources.add("mpesa")
                    if (equityLinkedAccountIds.contains(account.id)) sources.add("equity")
                    account.copy(
                        isDefault = account.id == defaultAccountId,
                        linkedSources = sources
                    )
                })
            } else {
                accountsResult
            }
        }

    val accountId = (enrichedSelectedAccount as? Result.Success)?.data?.id
    val importState = importStateMap[accountId]
    val importProgress = importProgressMap[accountId] ?: 0f

    var isManualSyncInProgress by remember { mutableStateOf(false) }

    LaunchedEffect(accountId) {
        transactionsViewModel.importEvents.collect { event ->
            val wasManualSync = isManualSyncInProgress
            isManualSyncInProgress = false
            
            when (event) {
                is ImportEvent.Success -> {
                    if (event.accountId == accountId) {
                        onGlobalRefresh()
                        delay(1500)
                        transactionsViewModel.resetImportState(event.accountId)
                    }
                }

                is ImportEvent.Error -> {
                    if (event.accountId == accountId) {
                        val account = (enrichedSelectedAccount as? Result.Success)?.data
                        val isLinked =
                            account?.linkedSources?.let { it.contains("mpesa") || it.contains("equity") }
                                ?: false
                        val message = event.exception.message ?: ""

                        if (isLinked && (message.contains("permission", ignoreCase = true) || message.contains(
                                "access",
                                ignoreCase = true
                            ))
                        ) {
                            onSmsPermissionRequired(wasManualSync)
                        }
                    }
                }
            }
        }
    }

    // Handle completed sync states even if the event was missed (e.g., user was on another screen)
    LaunchedEffect(importState, accountId) {
        if (importState is Result.Success && accountId != null) {
            onGlobalRefresh()
            delay(1500)
            transactionsViewModel.resetImportState(accountId)
        }
    }
    val throttledOnEditTransaction = rememberThrottleClick(onClick = onEditTransaction)
    val throttledOnCardClick = rememberThrottleClick<Pair<String, Boolean?>> { (accId, isInc) ->
        onCardClick(accId, isInc)
    }

    LaunchedEffect(selectedAccountId) {
        selectedAccountId?.let { accountsViewModel.selectAccount(it) }
    }

    if (selectedAccountId == null && (accountsResult is Result.Success && (accountsResult as Result.Success).data.isEmpty() || accountsResult is Result.Error)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (accountsResult is Result.Error) {
                CommonErrorState(
                    title = "Failed to load accounts",
                    error = (accountsResult as Result.Error).exception,
                    onRetry = { accountsViewModel.reloadAccounts() }
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No account found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Create your first account to start tracking your finances.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { onAccountSelected("") },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add First Account")
                    }
                }
            }
        }
        return
    }


    LaunchedEffect(smsSyncSignal) {
        if (smsSyncSignal != null) {
            transactionsViewModel.importTransactions(smsSyncSignal.accountId)
            onGlobalRefresh() // Clear the signal in MainViewModel
        }
    }

    // Keep track of the last processed refresh trigger to avoid redundant refreshes on re-entry
    // We use rememberSaveable to ensure it persists across navigation
    var lastProcessedRefreshTrigger by rememberSaveable { mutableIntStateOf(refreshTrigger) }

    LaunchedEffect(refreshTrigger, enrichedSelectedAccount) {
        val accountId = (enrichedSelectedAccount as? Result.Success)?.data?.id
        if (accountId != null && refreshTrigger > lastProcessedRefreshTrigger) {
            accountsViewModel.reloadAccounts(showLoading = false)
            transactionsViewModel.loadRecentTransactions(accountId, force = true)
            statsViewModel.loadOverview(accountId, force = true)
            statsViewModel.loadCategoryComparisons(accountId, force = true)
            statsViewModel.loadHighlights(accountId, force = true)
            transactionsViewModel.refreshCategories()
            lastProcessedRefreshTrigger = refreshTrigger
        }
    }

    LaunchedEffect(Unit) {
        transactionsViewModel.refreshCategories()
        if (accountsResult !is Result.Success || (accountsResult as Result.Success).data.isEmpty()) {
            accountsViewModel.reloadAccounts()
        }
    }

    LaunchedEffect(enrichedSelectedAccount) {
        val account = (enrichedSelectedAccount as? Result.Success)?.data
        account?.let { acc ->
            transactionsViewModel.loadRecentTransactions(acc.id)
            statsViewModel.loadOverview(acc.id)
            statsViewModel.loadCategoryComparisons(acc.id)

            // Trigger auto-sync only if the account has linked sources
            if (acc.linkedSources.contains("mpesa") || acc.linkedSources.contains("equity")) {
                transactionsViewModel.autoSyncTransactions(acc.id)
            }
        }
    }

    val bottomPadding = paddingValues.calculateBottomPadding()
    var lastBottomPadding by remember { mutableStateOf(80.dp) }
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

    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = stableBottomPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = paddingValues.calculateTopPadding() + 16.dp,
                end = 16.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CurrentBalanceCardWrapper(
                    accountsResult = enrichedAccountsResult,
                    selectedAccountResult = enrichedSelectedAccount,
                    defaultAccountId = defaultAccountId,
                    isBalanceHidden = isBalanceHidden,
                    isMpesaAutoSyncEnabled = isMpesaListenerEnabled,
                    isEquityAutoSyncEnabled = isEquityListenerEnabled,
                    importState = importState,
                    syncProgress = importProgress,
                    onAccountSelected = { accountId ->
                        onAccountSelected(accountId)
                        // No longer canceling sync here to allow background sync to continue
                    },
                    onToggleBalanceVisibility = { settingsViewModel.setBalanceHidden(it) },
                    onManualSync = {
                        isManualSyncInProgress = true
                        val accountId = (enrichedSelectedAccount as? Result.Success)?.data?.id
                        transactionsViewModel.importTransactions(accountId)
                    },
                    onSyncErrorClick = { message ->
                        onShowToast(message, true)
                    },
                    onRetry = {
                        accountsViewModel.reloadAccounts()
                        selectedAccountId?.let { accountsViewModel.selectAccount(it) }
                    }
                )
            }

            item {
                IncomeExpenseCards(
                    accountResult = enrichedSelectedAccount,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onCardClick = { isIncome ->
                        val accountId = (enrichedSelectedAccount as? Result.Success)?.data?.id
                        accountId?.let { throttledOnCardClick(it to isIncome) }
                    }
                )
            }

            item { IncomeExpensesOverview(overviewResult) }
            item {
                CategoryComparisonCard(
                    categoryComparisonResult = categoryComparisonResult,
                    accountId = (enrichedSelectedAccount as? Result.Success)?.data?.id,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                TransactionsListCard(
                    transactionsResult = transactionsResult,
                    animatedVisibilityScope = animatedVisibilityScope,
                    accountId = (enrichedSelectedAccount as? Result.Success)?.data?.id,
                    onViewAllClick = {
                        val accountId = (enrichedSelectedAccount as? Result.Success)?.data?.id
                        accountId?.let { throttledOnCardClick(it to null) }
                    },
                    onTransactionClick = { transaction ->
                        transaction.id?.let { id -> throttledOnEditTransaction(id) }
                    },
                    onRetry = {
                        val accountId = (enrichedSelectedAccount as? Result.Success)?.data?.id
                        accountId?.let {
                            transactionsViewModel.loadRecentTransactions(
                                it,
                                force = true
                            )
                        }
                    }
                )
            }
        }
    }
}
