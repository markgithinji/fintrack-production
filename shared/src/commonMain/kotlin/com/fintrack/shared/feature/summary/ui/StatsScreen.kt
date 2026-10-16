package com.fintrack.shared.feature.summary.ui

import Period
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fintrack.shared.feature.core.ui.CommonErrorState
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.summary.domain.model.TabType
import com.fintrack.shared.feature.summary.ui.components.CategoryTotalsCardWithTabs
import com.fintrack.shared.feature.summary.ui.components.SpendingHighlightsSection
import com.fintrack.shared.ui.theme.GreenIncome
import com.fintrack.shared.ui.theme.PinkExpense
import com.fintrack.shared.feature.transaction.ui.TransactionViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun StatisticsScreen(
    selectedAccountId: String?,
    refreshTrigger: Int,
    viewModel: StatisticsViewModel = koinViewModel(),
    transactionsViewModel: TransactionViewModel = koinViewModel(),
    paddingValues: PaddingValues = PaddingValues(0.dp),
    animatedVisibilityScope: AnimatedVisibilityScope,
    onNavigateToCustomAnalysis: () -> Unit = {},
    onCategoryClick: (categoryName: String, categoryId: String, isIncome: Boolean, startDate: String?, endDate: String?, accountId: String) -> Unit = { _, _, _, _, _, _ -> }
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val availableWeeks by viewModel.availableWeeks.collectAsStateWithLifecycle()
    val availableMonths by viewModel.availableMonths.collectAsStateWithLifecycle()
    val availableYears by viewModel.availableYears.collectAsStateWithLifecycle()
    val highlights by viewModel.highlights.collectAsStateWithLifecycle()
    val distributionResult by viewModel.distribution.collectAsStateWithLifecycle()
    val metadataState by viewModel.metadataState.collectAsStateWithLifecycle()
    val hasNextPeriod by viewModel.hasNextPeriod.collectAsStateWithLifecycle()
    val hasPreviousPeriod by viewModel.hasPreviousPeriod.collectAsStateWithLifecycle()
    val availableAccounts by viewModel.availableAccounts.collectAsStateWithLifecycle()
    val selectedAccountIds by viewModel.selectedAccountIds.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val importStateMap by transactionsViewModel.importState.collectAsStateWithLifecycle()

    val activeAccountId = viewModel.getEffectiveAccountId()

    LaunchedEffect(Unit) {
        viewModel.loadAvailableAccounts()
    }

    // Syncing state from TransactionViewModel
    LaunchedEffect(activeAccountId, importStateMap) {
        val isSyncing = activeAccountId != null && importStateMap[activeAccountId] is Result.Loading
        viewModel.setSyncing(isSyncing)
    }

    // Keep track of the last processed refresh trigger to avoid redundant refreshes
    var lastProcessedRefreshTrigger by rememberSaveable { mutableIntStateOf(-1) }

    val safePeriod = selectedPeriod ?: remember(availableWeeks, availableMonths, availableYears) {
        getDefaultPeriod(availableWeeks, availableMonths, availableYears)
    }

    LaunchedEffect(activeAccountId, refreshTrigger) {
        val force = refreshTrigger > lastProcessedRefreshTrigger && lastProcessedRefreshTrigger != -1
        if (force) {
            viewModel.resetStats()
        }
        viewModel.loadAvailablePeriods(activeAccountId, force = force)
        viewModel.loadOverview(activeAccountId, force = force)
        viewModel.loadCashflowRunway(activeAccountId, force = force)
        lastProcessedRefreshTrigger = refreshTrigger
    }

    if (metadataState is Result.Error) {
        CommonErrorState(
            title = "Unable to load statistics",
            error = (metadataState as Result.Error).exception,
            onRetry = {
                viewModel.loadAvailablePeriods(activeAccountId, force = true)
                viewModel.loadOverview(activeAccountId, force = true)
                viewModel.loadCashflowRunway(activeAccountId, force = true)
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = paddingValues.calculateTopPadding())
    ) {
        // Account Selection Filter Bar
        if (availableAccounts.isNotEmpty()) {
            AccountFilterChipBar(
                accounts = availableAccounts,
                selectedAccountIds = selectedAccountIds,
                onToggleAccount = { accId ->
                    viewModel.toggleAccountFilter(accId)
                }
            )
        }

        // Top Tab Switcher
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 8.dp)
        ) {
            TabSwitcher(
                selectedTab = selectedTab,
                onTabSelected = { 
                    viewModel.onTabChanged(it, activeAccountId)
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(
                bottom = paddingValues.calculateBottomPadding() + 32.dp
            ),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            item(key = "customAnalysisBanner") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigateToCustomAnalysis() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Custom Financial Analysis",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Filter by date, category, account & amount",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            item(key = "spendingHighlights") {
                SpendingHighlightsSection(
                    tabType = selectedTab,
                    highlightsResult = highlights,
                    loadHighlights = {
                        val yearCode =
                            selectedPeriod?.code?.split("-")?.firstOrNull() ?: selectedPeriod?.code
                        viewModel.loadHighlights(activeAccountId, yearCode, force = true)
                    }
                )
            }

            item(key = "categoryTotals") {
                CategoryTotalsCardWithTabs(
                    tabType = selectedTab,
                    period = safePeriod,
                    distributionResult = distributionResult,
                    availableWeeks = availableWeeks,
                    availableMonths = availableMonths,
                    availableYears = availableYears,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onWeekSelected = { week ->
                        viewModel.onPeriodChanged(Period.Week(week), activeAccountId)
                    },
                    onMonthSelected = { month ->
                        viewModel.onPeriodChanged(Period.Month(month), activeAccountId)
                    },
                    onYearSelected = { year ->
                        viewModel.onPeriodChanged(Period.Year(year), activeAccountId)
                    },
                    onPeriodSelected = { period ->
                        viewModel.onPeriodChanged(period, activeAccountId)
                    },
                    onPreviousPeriod = {
                        viewModel.navigateToPreviousPeriod(activeAccountId)
                    },
                    onNextPeriod = {
                        viewModel.navigateToNextPeriod(activeAccountId)
                    },
                    hasNext = hasNextPeriod,
                    hasPrevious = hasPreviousPeriod,
                    onRetry = {
                        viewModel.reloadDistributionForCurrentSelection(
                            activeAccountId,
                            force = true
                        )
                    },
                    onCategoryClick = { categoryName, categoryId ->
                        val dateRange = safePeriod.getDateRange()
                        onCategoryClick(
                            categoryName,
                            categoryId,
                            selectedTab is TabType.Income,
                            dateRange?.first,
                            dateRange?.second,
                            activeAccountId ?: ""
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun TabSwitcher(
    selectedTab: TabType,
    onTabSelected: (TabType) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TabButton(
            text = "Expenses",
            isSelected = selectedTab == TabType.Expense,
            selectedColor = PinkExpense,
            modifier = Modifier.weight(1f),
            onClick = { onTabSelected(TabType.Expense) }
        )
        TabButton(
            text = "Income",
            isSelected = selectedTab == TabType.Income,
            selectedColor = GreenIncome,
            modifier = Modifier.weight(1f),
            onClick = { onTabSelected(TabType.Income) }
        )
    }
}

@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    selectedColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.98f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) selectedColor else Color.Transparent,
        animationSpec = tween(300)
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(300)
    )

    Box(
        modifier = modifier
            .height(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 15.sp
        )
    }
}

private fun getDefaultPeriod(
    availableWeeks: List<String>,
    availableMonths: List<String>,
    availableYears: List<String>
): Period {
    return when {
        availableWeeks.isNotEmpty() -> Period.Week(availableWeeks.first())
        availableMonths.isNotEmpty() -> Period.Month(availableMonths.first())
        availableYears.isNotEmpty() -> Period.Year(availableYears.first())
        else -> Period.Week("2024-W01")
    }
}

@Composable
private fun AccountFilterChipBar(
    accounts: List<com.fintrack.shared.feature.account.domain.model.Account>,
    selectedAccountIds: Set<String>,
    onToggleAccount: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = com.fintrack.shared.feature.core.ui.util.rememberHapticFeedback()

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item(key = "all_accounts") {
            val isSelected = selectedAccountIds.isEmpty()
            FilterChip(
                selected = isSelected,
                onClick = {
                    hapticFeedback()
                    onToggleAccount(null)
                },
                label = { Text("All Accounts", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                shape = RoundedCornerShape(12.dp)
            )
        }

        items(accounts, key = { it.id }) { acc ->
            val isSelected = selectedAccountIds.contains(acc.id)
            FilterChip(
                selected = isSelected,
                onClick = {
                    hapticFeedback()
                    onToggleAccount(acc.id)
                },
                label = { Text(acc.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}
