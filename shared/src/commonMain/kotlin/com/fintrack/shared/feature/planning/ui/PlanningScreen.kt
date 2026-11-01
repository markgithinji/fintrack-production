package com.fintrack.shared.feature.planning.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import com.fintrack.shared.feature.core.ui.util.rememberLongPressHapticFeedback
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import com.fintrack.shared.feature.account.ui.AccountsViewModel
import com.fintrack.shared.feature.budget.domain.model.SavingsGoalWithCalculation
import com.fintrack.shared.feature.planning.ui.components.AddEditAssetDialog
import com.fintrack.shared.feature.planning.ui.components.AddEditDebtDialog
import com.fintrack.shared.feature.planning.ui.components.LogDebtRepaymentDialog
import com.fintrack.shared.feature.planning.ui.components.AffordabilityToolCard
import com.fintrack.shared.feature.planning.ui.components.AssetCard
import com.fintrack.shared.feature.planning.ui.components.AssetsSummaryCard
import com.fintrack.shared.feature.planning.ui.components.DebtCard
import com.fintrack.shared.feature.planning.ui.components.DebtPayoffSummaryCard
import com.fintrack.shared.feature.summary.ui.components.AffordabilitySimulatorDialog
import com.fintrack.shared.feature.budget.ui.components.AddSavingsGoalDialog
import com.fintrack.shared.feature.budget.ui.components.DepositToGoalDialog
import com.fintrack.shared.feature.budget.ui.components.SavingsGoalCard
import com.fintrack.shared.feature.budget.ui.components.SavingsGoalShimmerLoading
import com.fintrack.shared.feature.planning.ui.components.AssetShimmerLoading
import com.fintrack.shared.feature.planning.ui.components.DebtShimmerLoading
import com.fintrack.shared.feature.budget.ui.components.SavingsGoalsInfoCard
import com.fintrack.shared.feature.planning.ui.components.calculators.FinancialCalculatorsHubSheet
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fintrack.shared.feature.budget.domain.model.BudgetWithStatus
import com.fintrack.shared.feature.core.ui.AnimatedShimmerBox
import com.fintrack.shared.feature.core.ui.CommonErrorState
import com.fintrack.shared.feature.core.ui.util.rememberThrottleClick
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.core.util.formatAsShortDateWithYear
import com.fintrack.shared.feature.core.util.formatToSinglePrecision
import com.fintrack.shared.feature.core.util.toDouble
import com.fintrack.shared.feature.core.ui.LocalSharedTransitionScope
import com.fintrack.shared.feature.navigation.ui.toCurrencyString
import com.fintrack.shared.ui.theme.GreenIncome
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlanningScreen(
    refreshTrigger: Int,
    viewModel: PlanningViewModel = koinViewModel(),
    accountsViewModel: AccountsViewModel = koinViewModel(),
    paddingValues: PaddingValues = PaddingValues(0.dp),
    animatedVisibilityScope: AnimatedVisibilityScope,
    onAddBudget: () -> Unit = {},
    onBudgetClick: (BudgetWithStatus) -> Unit
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
        ?: throw IllegalStateException("No SharedTransitionScope found")

    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val savingsGoals by viewModel.savingsGoals.collectAsStateWithLifecycle()
    val assets by viewModel.assets.collectAsStateWithLifecycle()
    val debts by viewModel.debts.collectAsStateWithLifecycle()
    val payoffPlan by viewModel.payoffPlan.collectAsStateWithLifecycle()
    val calculatorsState by viewModel.calculatorsState.collectAsStateWithLifecycle()
    val accountsResult by accountsViewModel.accounts.collectAsStateWithLifecycle()
    val accounts = (accountsResult as? Result.Success)?.data ?: emptyList()

    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showAddAssetDialog by remember { mutableStateOf(false) }
    var depositTargetGoal by remember { mutableStateOf<SavingsGoalWithCalculation?>(null) }
    var goalToEdit by remember { mutableStateOf<SavingsGoalWithCalculation?>(null) }
    var debtToPay by remember { mutableStateOf<com.fintrack.shared.feature.planning.domain.model.Debt?>(null) }
    var debtToEdit by remember { mutableStateOf<com.fintrack.shared.feature.planning.domain.model.Debt?>(null) }
    var assetToEdit by remember { mutableStateOf<com.fintrack.shared.feature.planning.domain.model.Asset?>(null) }
    var showAffordabilitySimulator by remember { mutableStateOf(false) }
    var showAddDebtDialog by remember { mutableStateOf(false) }
    var pendingDeleteTarget by remember { mutableStateOf<PendingDeleteTarget?>(null) }
    var activeNumpadField by remember { mutableStateOf<com.fintrack.shared.feature.core.ui.NumpadTargetField?>(null) }
    var showCalculatorsHub by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    
    val throttledOnBudgetClick = rememberThrottleClick(onClick = onBudgetClick)
    val throttledOnAddBudget = rememberThrottleClick(onClick = onAddBudget)

    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0) {
            viewModel.reloadBudgets(showLoading = false)
            if (selectedTab == PlanningTab.SAVINGS_GOALS) {
                viewModel.loadSavingsGoals()
            } else if (selectedTab == PlanningTab.ASSETS_PORTFOLIO) {
                viewModel.loadAssets()
            } else if (selectedTab == PlanningTab.DEBT_PAYOFF) {
                viewModel.loadDebts()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = paddingValues.calculateTopPadding())
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Fixed Top Segmented Tab Switcher (OUTSIDE SCROLL CONTAINER)
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    onClick = { viewModel.selectTab(PlanningTab.SPENDING_BUDGETS) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedTab == PlanningTab.SPENDING_BUDGETS) Color.White else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (selectedTab == PlanningTab.SPENDING_BUDGETS) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Budgets",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedTab == PlanningTab.SPENDING_BUDGETS) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == PlanningTab.SPENDING_BUDGETS) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    onClick = { viewModel.selectTab(PlanningTab.SAVINGS_GOALS) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedTab == PlanningTab.SAVINGS_GOALS) Color.White else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (selectedTab == PlanningTab.SAVINGS_GOALS) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Goals",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedTab == PlanningTab.SAVINGS_GOALS) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == PlanningTab.SAVINGS_GOALS) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    onClick = { viewModel.selectTab(PlanningTab.ASSETS_PORTFOLIO) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedTab == PlanningTab.ASSETS_PORTFOLIO) Color.White else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (selectedTab == PlanningTab.ASSETS_PORTFOLIO) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Assets",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedTab == PlanningTab.ASSETS_PORTFOLIO) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == PlanningTab.ASSETS_PORTFOLIO) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    onClick = { viewModel.selectTab(PlanningTab.DEBT_PAYOFF) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedTab == PlanningTab.DEBT_PAYOFF) Color.White else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (selectedTab == PlanningTab.DEBT_PAYOFF) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Debts",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedTab == PlanningTab.DEBT_PAYOFF) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == PlanningTab.DEBT_PAYOFF) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = paddingValues.calculateBottomPadding() + 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            if (selectedTab == PlanningTab.SPENDING_BUDGETS) {
                // SPENDING BUDGETS TAB
                item(key = "affordability_tool_card") {
                    AffordabilityToolCard(
                        onSimulateClick = { showAffordabilitySimulator = true },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                item(key = "calculators_hub_card") {
                    Card(
                        onClick = { showCalculatorsHub = true },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Calculate,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Financial Calculators Hub",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "MMF Yield, Tip Splitter, Loan & FIRE Tools",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                when (val currentBudgets = budgets) {
                    is Result.Loading -> {
                        spendingBudgetsShimmerItems()
                    }

                    is Result.Error -> {
                        item(key = "spending_error") {
                            BudgetErrorRetryState(
                                error = currentBudgets.exception,
                                onRetry = { viewModel.reloadBudgets() }
                            )
                        }
                    }

                    is Result.Success -> {
                        val data = currentBudgets.data
                        if (data.isNotEmpty()) {
                            item(key = "spending_summary_header") {
                                BudgetSummaryHeader(data)
                            }

                            item(key = "spending_add_button") {
                                with(sharedTransitionScope) {
                                    SexyAddBudgetButton(
                                        onClick = throttledOnAddBudget,
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                            .sharedBounds(
                                                rememberSharedContentState(key = "budget_header_new"),
                                                animatedVisibilityScope = animatedVisibilityScope,
                                                boundsTransform = { _, _ ->
                                                    spring(
                                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    )
                                                },
                                                clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(20.dp))
                                            )
                                    )
                                }
                            }

                            items(data, key = { "spending_${it.budget.id ?: it.budget.name}" }) { budgetWithStatus ->
                                with(sharedTransitionScope) {
                                    BudgetItem(
                                        budgetWithStatus = budgetWithStatus,
                                        onClick = { throttledOnBudgetClick(budgetWithStatus) },
                                        onDeleteClick = {
                                            budgetWithStatus.budget.id?.let { id ->
                                                pendingDeleteTarget = PendingDeleteTarget.BudgetTarget(id, budgetWithStatus.budget.name)
                                            }
                                        },
                                        modifier = Modifier
                                            .animateItem()
                                            .sharedBounds(
                                                rememberSharedContentState(key = "budget_header_${budgetWithStatus.budget.id}"),
                                                animatedVisibilityScope = animatedVisibilityScope,
                                                boundsTransform = { _, _ ->
                                                    spring(
                                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    )
                                                },
                                                clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(16.dp))
                                            )
                                    )
                                }
                            }
                        } else {
                            item(key = "spending_add_button_empty") {
                                with(sharedTransitionScope) {
                                    SexyAddBudgetButton(
                                        onClick = throttledOnAddBudget,
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                            .sharedBounds(
                                                rememberSharedContentState(key = "budget_header_new"),
                                                animatedVisibilityScope = animatedVisibilityScope,
                                                boundsTransform = { _, _ ->
                                                    spring(
                                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    )
                                                },
                                                clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(20.dp))
                                            )
                                    )
                                }
                            }

                            item(key = "spending_empty_state") {
                                Surface(
                                    shape = RoundedCornerShape(22.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "No Spending Budgets Created",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Set category limits for Dining, Utilities, or Fuel. FinTrack auto-detects matching transactions across all accounts and tracks remaining limits in real-time.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (selectedTab == PlanningTab.SAVINGS_GOALS) {
                // SAVINGS GOALS TAB
                item(key = "savings_info_card") {
                    SavingsGoalsInfoCard(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                item(key = "savings_add_goal_button") {
                    SexyAddSavingsGoalButton(
                        onClick = { showAddGoalDialog = true },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                when (val goalsResult = savingsGoals) {
                    is Result.Loading -> {
                        item(key = "savings_loading_shimmer") {
                            SavingsGoalShimmerLoading()
                        }
                    }
                    is Result.Error -> {
                        item(key = "savings_error") {
                            CommonErrorState(
                                title = "Unable to load savings goals",
                                error = goalsResult.exception,
                                onRetry = { viewModel.loadSavingsGoals() }
                            )
                        }
                    }
                    is Result.Success -> {
                        val goalsList = goalsResult.data
                        if (goalsList.isNotEmpty()) {
                            items(goalsList, key = { "savings_${it.goal.id}" }) { goalCalc ->
                                SavingsGoalCard(
                                    goalCalc = goalCalc,
                                    onCardClick = { goalToEdit = goalCalc },
                                    onDepositClick = { depositTargetGoal = goalCalc },
                                    onDeleteClick = {
                                        pendingDeleteTarget = PendingDeleteTarget.GoalTarget(goalCalc.goal.id, goalCalc.goal.name)
                                    },
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            item(key = "savings_empty_state") {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "No Assets or Savings Goals",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Set up a goal for emergency savings, MMF yields, Sacco deposits, or real estate assets. FinTrack will auto-calculate required monthly savings and cashflow feasibility!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (selectedTab == PlanningTab.ASSETS_PORTFOLIO) {
                // ASSETS & INVESTMENTS PORTFOLIO TAB
                when (val currentAssets = assets) {
                    is Result.Loading -> {
                        item(key = "assets_loading_shimmer") {
                            AssetShimmerLoading()
                        }
                    }
                    is Result.Error -> {
                        item(key = "assets_error") {
                            CommonErrorState(
                                title = "Unable to load assets",
                                error = currentAssets.exception,
                                onRetry = { viewModel.loadAssets() }
                            )
                        }
                    }
                    is Result.Success -> {
                        val assetList = currentAssets.data

                        if (assetList.isNotEmpty()) {
                            item(key = "assets_summary_card") {
                                AssetsSummaryCard(
                                    assets = assetList,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }

                            item(key = "add_asset_button") {
                                Button(
                                    onClick = { showAddAssetDialog = true },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                        .height(48.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Add Asset or Investment", fontWeight = FontWeight.Bold)
                                }
                            }

                            items(assetList, key = { "asset_${it.id}" }) { asset ->
                                AssetCard(
                                    asset = asset,
                                    onCardClick = { assetToEdit = asset },
                                    onDeleteClick = {
                                        pendingDeleteTarget = PendingDeleteTarget.AssetTarget(asset.id, asset.name)
                                    },
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            item(key = "add_asset_button_empty") {
                                Button(
                                    onClick = { showAddAssetDialog = true },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                        .height(48.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Add Asset or Investment", fontWeight = FontWeight.Bold)
                                }
                            }

                            item(key = "assets_empty_state") {
                                Surface(
                                    shape = RoundedCornerShape(22.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "No Investment Assets Logged",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Log Money Market Funds (MMFs), Saccos, Real Estate, and Stock holdings to track passive returns & net worth.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (selectedTab == PlanningTab.DEBT_PAYOFF) {
                when (val currentDebts = debts) {
                    is Result.Loading -> {
                        item(key = "debt_loading_shimmer") {
                            DebtShimmerLoading()
                        }
                    }
                    is Result.Error -> {
                        item(key = "debt_error") {
                            CommonErrorState(
                                title = "Unable to load debts",
                                error = currentDebts.exception,
                                onRetry = { viewModel.loadDebts() }
                            )
                        }
                    }
                    is Result.Success -> {
                        if (payoffPlan != null && payoffPlan!!.orderedDebts.isNotEmpty()) {
                            item(key = "debt_summary_card") {
                                DebtPayoffSummaryCard(
                                    plan = payoffPlan!!,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }

                            item(key = "debt_add_button") {
                                Button(
                                    onClick = { showAddDebtDialog = true },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                        .height(48.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Add Loan or Debt", fontWeight = FontWeight.Bold)
                                }
                            }

                            items(payoffPlan!!.orderedDebts, key = { "debt_${it.debt.id}" }) { debtSchedule ->
                                DebtCard(
                                    debtSchedule = debtSchedule,
                                    onCardClick = { debtToEdit = debtSchedule.debt },
                                    onPayClick = { debtToPay = debtSchedule.debt },
                                    onDeleteClick = {
                                        pendingDeleteTarget = PendingDeleteTarget.DebtTarget(debtSchedule.debt.id, debtSchedule.debt.name)
                                    },
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            item(key = "debt_add_button_empty") {
                                Button(
                                    onClick = { showAddDebtDialog = true },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                        .height(48.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Add Loan or Debt", fontWeight = FontWeight.Bold)
                                }
                            }

                            item(key = "debt_empty_state") {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "No Loans or Debts",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Add bank loans, credit cards, or overdraft balances. Compare Debt Snowball vs Avalanche to eliminate debt fast!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
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

    if (showAddGoalDialog || goalToEdit != null) {
        AddSavingsGoalDialog(
            categories = categories,
            accounts = accounts,
            goal = goalToEdit?.goal,
            onFocusNumpadField = { activeNumpadField = it },
            onGoalSaved = { id, name, targetAmount, targetDate, categoryId, accountId ->
                viewModel.saveOrUpdateSavingsGoal(id, name, targetAmount, targetDate, categoryId, accountId)
            },
            onDismiss = {
                showAddGoalDialog = false
                goalToEdit = null
                activeNumpadField = null
            }
        )
    }

    if (depositTargetGoal != null) {
        DepositToGoalDialog(
            goalName = depositTargetGoal!!.goal.name,
            onDeposit = { amount ->
                viewModel.depositToGoal(depositTargetGoal!!.goal.id, amount)
            },
            onDismiss = { depositTargetGoal = null }
        )
    }

    if (showAffordabilitySimulator) {
        AffordabilitySimulatorDialog(
            onDismiss = { showAffordabilitySimulator = false }
        )
    }

    if (showCalculatorsHub) {
        FinancialCalculatorsHubSheet(
            state = calculatorsState,
            onUpdateState = { transform -> viewModel.updateCalculatorsState(transform) },
            onDismiss = { showCalculatorsHub = false }
        )
    }

    if (showAddDebtDialog || debtToEdit != null) {
        AddEditDebtDialog(
            debt = debtToEdit,
            onDebtSaved = { id, name, balance, rate, minPay ->
                viewModel.saveOrUpdateDebt(id, name, balance, rate, minPay)
            },
            onDismiss = {
                showAddDebtDialog = false
                debtToEdit = null
            }
        )
    }

    if (debtToPay != null) {
        LogDebtRepaymentDialog(
            debtName = debtToPay!!.name,
            currentBalance = debtToPay!!.balance,
            onRepaymentLogged = { amount ->
                viewModel.logDebtRepayment(debtToPay!!.id, amount)
            },
            onDismiss = { debtToPay = null }
        )
    }

    if (showAddAssetDialog || assetToEdit != null) {
        AddEditAssetDialog(
            asset = assetToEdit,
            onAssetSaved = { id, name, type, balance, yieldPct ->
                viewModel.saveOrUpdateAsset(id, name, type, balance, yieldPct)
            },
            onDismiss = {
                showAddAssetDialog = false
                assetToEdit = null
            }
        )
    }

    if (pendingDeleteTarget != null) {
        val target = pendingDeleteTarget!!
        ConfirmDeleteDialog(
            title = "Delete ${target.name}?",
            message = "Are you sure you want to delete ${target.name}? This action cannot be undone.",
            onConfirm = {
                when (target) {
                    is PendingDeleteTarget.BudgetTarget -> viewModel.removeBudget(target.id)
                    is PendingDeleteTarget.GoalTarget -> viewModel.deleteSavingsGoal(target.id)
                    is PendingDeleteTarget.AssetTarget -> viewModel.deleteAsset(target.id)
                    is PendingDeleteTarget.DebtTarget -> viewModel.deleteDebt(target.id)
                }
            },
            onDismiss = { pendingDeleteTarget = null }
        )
    }
}

@Composable
fun BudgetSummaryHeader(budgets: List<BudgetWithStatus>) {
    val totalLimit = budgets.fold(BigDecimal.ZERO) { acc, b -> acc + b.budget.limit }
    val totalSpent = budgets.fold(BigDecimal.ZERO) { acc, b -> acc + b.status.spent }
    val progress = if (totalLimit > BigDecimal.ZERO) {
        try {
            (totalSpent.toDouble() / totalLimit.toDouble()).toFloat().coerceIn(0f, 1f)
        } catch (_: Exception) {
            0f
        }
    } else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Monthly Budget",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = totalLimit.toCurrencyString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.AccountBalanceWallet,
                        null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Spent ${totalSpent.toCurrencyString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = "${(progress * 100).toInt()}% used",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (progress > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = if (progress > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
        }
    }
}

@Composable
fun SexyAddBudgetButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .shadow(
                elevation = if (isPressed) 1.dp else 4.dp,
                shape = RoundedCornerShape(20.dp),
                clip = false
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        try {
                            awaitRelease()
                        } finally {
                            isPressed = false
                        }
                    },
                    onTap = { onClick() }
                )
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val rotation by animateFloatAsState(
                        targetValue = if (isPressed) 90f else 0f,
                        animationSpec = tween(durationMillis = 200),
                        label = "add_icon_rotation"
                    )

                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.8f else 1f,
                        animationSpec = tween(durationMillis = 200),
                        label = "add_icon_scale"
                    )

                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Budget",
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                rotationZ = rotation
                                scaleX = scale
                                scaleY = scale
                            },
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Create New Budget",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                    Text(
                        text = "Auto-tracks matching transactions across accounts",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.85f)
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun SexyAddSavingsGoalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .shadow(
                elevation = if (isPressed) 1.dp else 4.dp,
                shape = RoundedCornerShape(20.dp),
                clip = false
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        try {
                            awaitRelease()
                        } finally {
                            isPressed = false
                        }
                    },
                    onTap = { onClick() }
                )
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val rotation by animateFloatAsState(
                        targetValue = if (isPressed) 90f else 0f,
                        animationSpec = tween(durationMillis = 200),
                        label = "add_goal_icon_rotation"
                    )

                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.8f else 1f,
                        animationSpec = tween(durationMillis = 200),
                        label = "add_goal_icon_scale"
                    )

                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Savings Goal",
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                rotationZ = rotation
                                scaleX = scale
                                scaleY = scale
                            },
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Add Savings Goal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                    Text(
                        text = "Set a goal target & auto-sync savings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.6f)
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.4f)
            )
        }
    }
}

sealed interface PendingDeleteTarget {
    val name: String
    data class BudgetTarget(val id: String, override val name: String) : PendingDeleteTarget
    data class GoalTarget(val id: String, override val name: String) : PendingDeleteTarget
    data class AssetTarget(val id: String, override val name: String) : PendingDeleteTarget
    data class DebtTarget(val id: String, override val name: String) : PendingDeleteTarget
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(24.dp)
            .widthIn(max = 400.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BudgetItem(
    budgetWithStatus: BudgetWithStatus,
    onClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val budget = budgetWithStatus.budget
    val status = budgetWithStatus.status
    val hapticLongPress = rememberLongPressHapticFeedback()

    val progress = if (budget.limit > BigDecimal.ZERO) {
        try {
            (status.spent.toDouble() / budget.limit.toDouble()).toFloat().coerceAtMost(1f)
        } catch (_: Exception) {
            0f
        }
    } else 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    if (onDeleteClick != null) {
                        hapticLongPress()
                        onDeleteClick()
                    }
                }
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = budget.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${budget.startDate.formatAsShortDateWithYear()} - ${budget.endDate.formatAsShortDateWithYear()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusChip(status.percentageUsed, status.isExceeded)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = status.spent.toCurrencyString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (status.isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "of ${budget.limit.toCurrencyString()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50)),
                    color = if (status.isExceeded) MaterialTheme.colorScheme.error
                    else if (progress > 0.8f) Color(0xFFFF9800)
                    else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {}
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${status.percentageUsed.formatToSinglePrecision()}% used",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (status.isExceeded) "Over budget" else "${status.remaining.toCurrencyString()} left",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (status.isExceeded) MaterialTheme.colorScheme.error else GreenIncome
                    )
                }
            }

            if (budget.categories.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    budget.categories.take(3).forEach { category ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.wrapContentSize()
                        ) {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (budget.categories.size > 3) {
                        Text(
                            text = "+${budget.categories.size - 3} more",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(percentage: BigDecimal, isExceeded: Boolean) {
    val (label, color) = when {
        isExceeded -> "Exceeded" to MaterialTheme.colorScheme.error
        percentage > BigDecimal.fromInt(90) -> "Critical" to Color(0xFFF44336)
        percentage > BigDecimal.fromInt(75) -> "Warning" to Color(0xFFFF9800)
        else -> "On Track" to GreenIncome
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun BudgetEmptyState(
    onAddBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.AccountBalanceWallet,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Master Your Money",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Set category limits for Dining, Utilities, or Fuel. FinTrack auto-detects matching transactions across all accounts and tracks remaining limits in real-time.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(40.dp))

        Button(
            onClick = onAddBudget,
            modifier = Modifier
                .height(56.dp)
                .fillMaxWidth(0.8f),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Add First Budget",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BudgetErrorRetryState(
    error: Throwable,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    CommonErrorState(
        modifier = modifier.fillMaxSize(),
        title = "Something went wrong",
        error = error,
        onRetry = onRetry
    )
}

fun LazyListScope.spendingBudgetsShimmerItems() {
    // 1. BudgetSummaryHeader Shimmer
    item(key = "spending_shimmer_header") {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            val shimmerColors = listOf(
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f),
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
            )
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AnimatedShimmerBox(
                            modifier = Modifier.width(120.dp).height(14.dp).clip(RoundedCornerShape(4.dp)),
                            shimmerColors = shimmerColors
                        )
                        AnimatedShimmerBox(
                            modifier = Modifier.width(160.dp).height(28.dp).clip(RoundedCornerShape(6.dp)),
                            shimmerColors = shimmerColors
                        )
                    }

                    AnimatedShimmerBox(
                        modifier = Modifier.size(48.dp).clip(CircleShape),
                        shimmerColors = shimmerColors
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    AnimatedShimmerBox(
                        modifier = Modifier.width(100.dp).height(12.dp).clip(RoundedCornerShape(4.dp)),
                        shimmerColors = shimmerColors
                    )
                    AnimatedShimmerBox(
                        modifier = Modifier.width(60.dp).height(12.dp).clip(RoundedCornerShape(4.dp)),
                        shimmerColors = shimmerColors
                    )
                }

                Spacer(modifier = Modifier.height(9.dp))

                AnimatedShimmerBox(
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    shimmerColors = shimmerColors
                )
            }
        }
    }

    // 2. SexyAddBudgetButton Shimmer
    item(key = "spending_shimmer_add_button") {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(72.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            AnimatedShimmerBox(
                modifier = Modifier.fillMaxSize(),
                shimmerColors = listOf(
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.03f),
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                )
            )
        }
    }

    // 3. BudgetItem Shimmers
    items(3, key = { "spending_shimmer_item_$it" }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(205.7.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Row 1: Title & Status Chip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        AnimatedShimmerBox(modifier = Modifier.width(140.dp).height(22.dp).clip(RoundedCornerShape(4.dp)))
                        AnimatedShimmerBox(modifier = Modifier.width(110.dp).height(12.dp).clip(RoundedCornerShape(4.dp)))
                    }
                    AnimatedShimmerBox(modifier = Modifier.width(64.dp).height(24.dp).clip(RoundedCornerShape(12.dp)))
                }

                // Row 2: Amounts, Progress Bar, Percentage Used
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedShimmerBox(modifier = Modifier.width(90.dp).height(18.dp).clip(RoundedCornerShape(4.dp)))
                        AnimatedShimmerBox(modifier = Modifier.width(100.dp).height(12.dp).clip(RoundedCornerShape(4.dp)))
                    }

                    AnimatedShimmerBox(modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AnimatedShimmerBox(modifier = Modifier.width(70.dp).height(12.dp).clip(RoundedCornerShape(4.dp)))
                        AnimatedShimmerBox(modifier = Modifier.width(80.dp).height(12.dp).clip(RoundedCornerShape(4.dp)))
                    }
                }
            }
        }
    }
}
