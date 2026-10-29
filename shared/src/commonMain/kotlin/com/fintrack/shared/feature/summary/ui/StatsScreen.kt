package com.fintrack.shared.feature.summary.ui

import com.fintrack.shared.feature.core.ui.SelectableFilterChip

import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import Period
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tune
import com.fintrack.shared.feature.summary.domain.model.MerchantSummary
import com.fintrack.shared.feature.summary.domain.model.OverviewPeriod
import com.fintrack.shared.feature.summary.ui.components.EmergencyFundScoreCard
import com.fintrack.shared.feature.summary.ui.components.MerchantDetailBottomSheet
import com.fintrack.shared.feature.summary.ui.components.NetWorthCard
import com.fintrack.shared.feature.summary.ui.components.SpendSpikeAlertCard
import com.fintrack.shared.feature.summary.ui.components.FinancialHealthIndexCard
import com.fintrack.shared.feature.summary.ui.components.PassiveIncomeCoverageCard
import com.fintrack.shared.feature.summary.ui.components.TopMerchantsContainerCard
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fintrack.shared.feature.core.ui.AnimatedShimmerBox
import com.fintrack.shared.feature.core.ui.CommonErrorState
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.summary.domain.model.TabType
import com.fintrack.shared.feature.summary.ui.components.BudgetOptimizerCard
import com.fintrack.shared.feature.summary.ui.components.CashflowRunwayCard
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
    val selectedOverviewPeriod by viewModel.selectedOverviewPeriod.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val availableWeeks by viewModel.availableWeeks.collectAsStateWithLifecycle()
    val availableMonths by viewModel.availableMonths.collectAsStateWithLifecycle()
    val availableYears by viewModel.availableYears.collectAsStateWithLifecycle()
    val highlights by viewModel.highlights.collectAsStateWithLifecycle()
    val distributionResult by viewModel.distribution.collectAsStateWithLifecycle()
    val runway by viewModel.runway.collectAsStateWithLifecycle()
    val netWorthSummary by viewModel.netWorthSummary.collectAsStateWithLifecycle()
    val spendSpikes by viewModel.spendSpikes.collectAsStateWithLifecycle()
    val emergencyFundScore by viewModel.emergencyFundScore.collectAsStateWithLifecycle()
    val budgetOptimizer by viewModel.budgetOptimizer.collectAsStateWithLifecycle()
    val selectedBudgetOptimizerDays by viewModel.selectedBudgetOptimizerDays.collectAsStateWithLifecycle()
    val metadataState by viewModel.metadataState.collectAsStateWithLifecycle()
    val hasNextPeriod by viewModel.hasNextPeriod.collectAsStateWithLifecycle()
    val hasPreviousPeriod by viewModel.hasPreviousPeriod.collectAsStateWithLifecycle()
    val hasNextOverviewPeriod by viewModel.hasNextOverviewPeriod.collectAsStateWithLifecycle()
    val hasPreviousOverviewPeriod by viewModel.hasPreviousOverviewPeriod.collectAsStateWithLifecycle()
    val availableAccounts by viewModel.availableAccounts.collectAsStateWithLifecycle()
    val selectedAccountIds by viewModel.selectedAccountIds.collectAsStateWithLifecycle()
    val topMerchants by viewModel.topMerchants.collectAsStateWithLifecycle()
    val passiveIncomeCoverage by viewModel.passiveIncomeCoverage.collectAsStateWithLifecycle()
    val financialHealthIndex by viewModel.financialHealthIndex.collectAsStateWithLifecycle()
    val selectedMerchantDays by viewModel.selectedMerchantDays.collectAsStateWithLifecycle()
    var selectedMerchant by remember { mutableStateOf<MerchantSummary?>(null) }
    val listState = rememberLazyListState()

    val importStateMap by transactionsViewModel.importState.collectAsStateWithLifecycle()

    val activeAccountId = remember(selectedAccountIds) {
        viewModel.getEffectiveAccountId()
    }

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
    var activeSubTab by rememberSaveable { mutableIntStateOf(0) }

    val safePeriod = selectedPeriod ?: remember(availableWeeks, availableMonths, availableYears) {
        getDefaultPeriod(availableWeeks, availableMonths, availableYears)
    }

    LaunchedEffect(activeAccountId, refreshTrigger, activeSubTab) {
        selectedMerchant = null
        val force = refreshTrigger > lastProcessedRefreshTrigger && lastProcessedRefreshTrigger != -1
        if (force) {
            viewModel.resetStats()
        }
        viewModel.loadAvailablePeriods(activeAccountId, force = force)

        if (activeSubTab == 0) {
            viewModel.loadOverview(activeAccountId, force = force)
            viewModel.loadCashflowRunway(activeAccountId, force = force)
            viewModel.loadSpendSpikes(activeAccountId, force = force)
            viewModel.loadTopMerchants(activeAccountId)
        } else if (activeSubTab == 1) {
            viewModel.loadNetWorthSummary(activeAccountId, force = force)
            viewModel.loadFinancialHealthIndex(activeAccountId, force = force)
            viewModel.loadPassiveIncomeCoverage(activeAccountId, force = force)
            viewModel.loadBudgetOptimizer(activeAccountId, force = force)
            viewModel.loadEmergencyFundScore(activeAccountId, force = force)
        }

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

    val density = LocalDensity.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = paddingValues.calculateTopPadding())
    ) {
        // Unified Top Control Panel Container (Row 1: Sub-Tabs, Row 2: Account Chips)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    val heightDp = with(density) { coordinates.size.height.toDp() }
                    println("[TOP_CONTROL_PANEL_SIZE_LOG] Top Control Panel Height: $heightDp (${coordinates.size.height}px) | AccountBar: ${if (availableAccounts.isNotEmpty()) "LOADED" else "SHIMMER"}")
                },
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: Top Segmented Sub-Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Analytics Sub-Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (activeSubTab == 0) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { activeSubTab = 0 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Analytics",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (activeSubTab == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (activeSubTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Portfolio Sub-Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (activeSubTab == 1) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { activeSubTab = 1 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Portfolio",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (activeSubTab == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (activeSubTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Row 2: Account Selection Filter Bar (Fixed height container to prevent wrap shifts)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (availableAccounts.isNotEmpty()) {
                        AccountFilterChipBar(
                            accounts = availableAccounts,
                            selectedAccountIds = selectedAccountIds,
                            onToggleAccount = { accId ->
                                viewModel.toggleAccountFilter(accId)
                            }
                        )
                    } else {
                        AccountFilterShimmerBar()
                    }
                }
            }
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
            item { Spacer(Modifier.height(4.dp)) }

            if (activeSubTab == 0) {
                // Sub-Tab 1: ANALYTICS (Expenses vs Income, Highlights, Category Donut Chart, Merchants)
                stickyHeader(key = "tabSwitcherHeader") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.background,
                        tonalElevation = 2.dp
                    ) {
                        TabSwitcher(
                            selectedTab = selectedTab,
                            onTabSelected = { 
                                viewModel.onTabChanged(it, activeAccountId)
                            }
                        )
                    }
                }

                item(key = "spendingHighlights") {
                    SpendingHighlightsSection(
                        tabType = selectedTab,
                        selectedYear = selectedOverviewPeriod?.code?.split("-")?.firstOrNull() ?: selectedOverviewPeriod?.code ?: com.fintrack.shared.feature.core.util.DateTimeHelper.currentMonthCode().substringBefore("-"),
                        highlightsResult = highlights,
                        availableYears = availableYears,
                        onYearSelected = { year: String ->
                            viewModel.onOverviewPeriodChanged(Period.Year(year), activeAccountId)
                        },
                        onPreviousPeriod = {
                            viewModel.navigateToPreviousOverviewPeriod(activeAccountId)
                        },
                        onNextPeriod = {
                            viewModel.navigateToNextOverviewPeriod(activeAccountId)
                        },
                        hasNext = hasNextOverviewPeriod,
                        hasPrevious = hasPreviousOverviewPeriod,
                        loadHighlights = {
                            val yearCode =
                                selectedOverviewPeriod?.code?.split("-")?.firstOrNull() ?: selectedOverviewPeriod?.code
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

                item(key = "topMerchantsContainerCard") {
                    TopMerchantsContainerCard(
                        merchantsResult = topMerchants,
                        selectedDays = selectedMerchantDays,
                        isIncome = selectedTab is TabType.Income,
                        onDaysSelected = { viewModel.setMerchantDays(it, activeAccountId) },
                        onMerchantClick = { selectedMerchant = it },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            } else {
                // Sub-Tab 2: NET WORTH & PORTFOLIO (Net Worth, 50/30/20 Optimizer, Cashflow Runway, Emergency Shield)
                item(key = "netWorthCard") {
                    NetWorthCard(
                        netWorthResult = netWorthSummary,
                        onRetry = {
                            viewModel.loadNetWorthSummary(activeAccountId, force = true)
                        }
                    )
                }

                item(key = "financialHealthIndex") {
                    FinancialHealthIndexCard(
                        healthIndexResult = financialHealthIndex,
                        onRetry = {
                            viewModel.loadFinancialHealthIndex(activeAccountId, force = true)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                item(key = "passiveIncomeCoverage") {
                    PassiveIncomeCoverageCard(
                        coverageResult = passiveIncomeCoverage,
                        onRetry = {
                            viewModel.loadPassiveIncomeCoverage(activeAccountId, force = true)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                item(key = "budgetOptimizerCard") {
                    BudgetOptimizerCard(
                        optimizerResult = budgetOptimizer,
                        selectedDays = selectedBudgetOptimizerDays,
                        onDaysSelected = { viewModel.setBudgetOptimizerDays(it, activeAccountId) },
                        onRetry = {
                            viewModel.loadBudgetOptimizer(activeAccountId, force = true)
                        }
                    )
                }

                item(key = "cashflowRunway") {
                    CashflowRunwayCard(
                        runwayResult = runway,
                        onRetry = {
                            viewModel.loadCashflowRunway(activeAccountId, force = true)
                        }
                    )
                }

                item(key = "emergencyFundScore") {
                    EmergencyFundScoreCard(
                        scoreResult = emergencyFundScore,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (selectedMerchant != null) {
            MerchantDetailBottomSheet(
                merchant = selectedMerchant!!,
                onDismiss = { selectedMerchant = null }
            )
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
            .height(38.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            style = MaterialTheme.typography.labelMedium
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
        else -> Period.Month(com.fintrack.shared.feature.core.util.DateTimeHelper.currentMonthCode())
    }
}

@Composable
private fun AccountFilterChipBar(
    accounts: List<com.fintrack.shared.feature.account.domain.model.Account>,
    selectedAccountIds: Set<String>,
    onToggleAccount: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item(key = "all_accounts") {
            val isSelected = selectedAccountIds.isEmpty()
            SelectableFilterChip(
                selected = isSelected,
                label = "All Accounts",
                onClick = { onToggleAccount(null) }
            )
        }

        items(accounts, key = { it.id }) { acc ->
            val isSelected = selectedAccountIds.contains(acc.id)
            SelectableFilterChip(
                selected = isSelected,
                label = acc.name,
                onClick = { onToggleAccount(acc.id) }
            )
        }
    }
}

@Composable
private fun AccountFilterShimmerBar(
    modifier: Modifier = Modifier
) {
    val shimmerWidths = listOf(104.dp, 80.dp, 72.dp, 76.dp, 84.dp)
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(count = shimmerWidths.size, key = { "account_filter_shimmer_$it" }) { index ->
            Box(
                modifier = Modifier.height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedShimmerBox(
                    modifier = Modifier
                        .width(shimmerWidths[index])
                        .height(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
        }
    }
}
