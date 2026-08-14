package com.fintrack.shared.feature.summary.ui

import Period
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
    val listState = rememberLazyListState()

    val importStateMap by transactionsViewModel.importState.collectAsStateWithLifecycle()

    // Syncing state from TransactionViewModel
    LaunchedEffect(selectedAccountId, importStateMap) {
        val isSyncing = selectedAccountId != null && importStateMap[selectedAccountId] is Result.Loading
        viewModel.setSyncing(isSyncing)
    }

    // Keep track of the last processed refresh trigger to avoid redundant refreshes
    var lastProcessedRefreshTrigger by rememberSaveable { mutableIntStateOf(refreshTrigger) }

    val safePeriod = selectedPeriod ?: remember(availableWeeks, availableMonths, availableYears) {
        getDefaultPeriod(availableWeeks, availableMonths, availableYears)
    }

    LaunchedEffect(selectedAccountId, refreshTrigger) {
        if (selectedAccountId != null) {
            val force = refreshTrigger > lastProcessedRefreshTrigger
            viewModel.loadAvailablePeriods(selectedAccountId, force = force)
            viewModel.loadOverview(selectedAccountId, force = force)
            lastProcessedRefreshTrigger = refreshTrigger
        }
    }

    if (selectedAccountId == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (metadataState is Result.Error) {
        CommonErrorState(
            title = "Unable to load statistics",
            error = (metadataState as Result.Error).exception,
            onRetry = {
                viewModel.loadAvailablePeriods(selectedAccountId, force = true)
                viewModel.loadOverview(selectedAccountId, force = true)
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
        // Top Tab Switcher
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp)
        ) {
            TabSwitcher(
                selectedTab = selectedTab,
                onTabSelected = { 
                    viewModel.onTabChanged(it, selectedAccountId)
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

            item(key = "spendingHighlights") {
                SpendingHighlightsSection(
                    tabType = selectedTab,
                    highlightsResult = highlights,
                    loadHighlights = {
                        val yearCode =
                            selectedPeriod?.code?.split("-")?.firstOrNull() ?: selectedPeriod?.code
                        viewModel.loadHighlights(selectedAccountId, yearCode, force = true)
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
                        viewModel.onPeriodChanged(Period.Week(week), selectedAccountId)
                    },
                    onMonthSelected = { month ->
                        viewModel.onPeriodChanged(Period.Month(month), selectedAccountId)
                    },
                    onYearSelected = { year ->
                        viewModel.onPeriodChanged(Period.Year(year), selectedAccountId)
                    },
                    onPeriodSelected = { period ->
                        viewModel.onPeriodChanged(period, selectedAccountId)
                    },
                    onPreviousPeriod = {
                        viewModel.navigateToPreviousPeriod(selectedAccountId)
                    },
                    onNextPeriod = {
                        viewModel.navigateToNextPeriod(selectedAccountId)
                    },
                    hasNext = hasNextPeriod,
                    hasPrevious = hasPreviousPeriod,
                    onRetry = {
                        viewModel.reloadDistributionForCurrentSelection(
                            selectedAccountId,
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
                            selectedAccountId
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
