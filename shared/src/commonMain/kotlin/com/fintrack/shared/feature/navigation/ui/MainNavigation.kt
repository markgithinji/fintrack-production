package com.fintrack.shared.feature.navigation.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.fintrack.shared.feature.account.ui.AccountsScreen
import com.fintrack.shared.feature.budget.ui.BudgetDetailScreen
import com.fintrack.shared.feature.budget.ui.BudgetScreen
import com.fintrack.shared.feature.category.ui.CategoryManagementScreen
import com.fintrack.shared.feature.core.ui.util.navigateThrottled
import com.fintrack.shared.feature.navigation.model.Screen
import com.fintrack.shared.feature.settings.ui.SettingsScreen
import com.fintrack.shared.feature.summary.ui.StatisticsScreen
import com.fintrack.shared.feature.transaction.ui.addtransaction.AddTransactionScreen
import com.fintrack.shared.feature.transaction.ui.home.HomeScreen
import com.fintrack.shared.feature.transaction.ui.transactionlist.TransactionListScreen
import com.fintrack.shared.feature.user.ui.EditProfileScreen
import com.fintrack.shared.feature.user.ui.ProfileScreen
import org.koin.compose.koinInject

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainNavigation(
    paddingValues: PaddingValues,
    smsSyncSignal: SmsSyncSignal?,
    mainViewModel: MainViewModel = koinInject(),
    onLogout: () -> Unit,
    onSmsPermissionRequired: (forceRationale: Boolean) -> Unit
) {
    val navController = LocalNavController.current
    val selectedAccountId by mainViewModel.selectedAccountId.collectAsStateWithLifecycle()
    val refreshTrigger by mainViewModel.refreshTrigger.collectAsStateWithLifecycle()
    val onGlobalRefresh = remember { { mainViewModel.triggerGlobalRefresh() } }

    NavHost(
        navController = navController,
        startDestination = Screen.Home(),
        modifier = Modifier.fillMaxSize(),
        enterTransition = {
            if (targetState.destination.isMorphScreen()) {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(350, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300))
            } else if (targetState.destination.isProfileDetailScreen()) {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
            } else {
                fadeIn(animationSpec = tween(250))
            }
        },
        exitTransition = {
            if (initialState.destination.isMorphScreen()) {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(350, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(300))
            } else if (initialState.destination.isProfileDetailScreen()) {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
            } else if (targetState.destination.isProfileDetailScreen()) {
                slideOutHorizontally(
                    targetOffsetX = { -it / 4 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
            } else {
                fadeOut(animationSpec = tween(250))
            }
        },
        popEnterTransition = {
            if (targetState.destination.isMorphScreen() || targetState.destination.isMainScreen()) {
                fadeIn(animationSpec = tween(250))
            } else if (targetState.destination.hasRoute<Screen.Profile>()) {
                slideInHorizontally(
                    initialOffsetX = { -it / 4 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
            } else {
                fadeIn(animationSpec = tween(250))
            }
        },
        popExitTransition = {
            if (initialState.destination.isMorphScreen()) {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(350, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(300))
            } else if (initialState.destination.isProfileDetailScreen()) {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
            } else {
                fadeOut(animationSpec = tween(250))
            }
        }
    ) {
        composable<Screen.Home> { backStackEntry ->
            val route: Screen.Home = backStackEntry.toRoute()
            val accountId = route.accountId ?: selectedAccountId

            HomeScreen(
                selectedAccountId = accountId,
                refreshTrigger = refreshTrigger,
                smsSyncSignal = smsSyncSignal,
                onGlobalRefresh = {
                    mainViewModel.triggerGlobalRefresh()
                    mainViewModel.consumeSmsSyncSignal()
                },
                onAccountSelected = { mainViewModel.onAccountSelected(it) },
                onSmsPermissionRequired = onSmsPermissionRequired,
                onShowToast = { message, isError -> mainViewModel.showToast(message, isError) },
                paddingValues = paddingValues,
                animatedVisibilityScope = this,
                onEditTransaction = { transactionId ->
                    navController.navigateThrottled(Screen.AddTransaction(transactionId))
                },
                onCardClick = { accountIdParam, isIncome ->
                    navController.navigateThrottled(
                        Screen.TransactionList(
                            accountId = accountIdParam,
                            isIncome = isIncome
                        )
                    )
                }
            )
        }

        composable<Screen.AddTransaction> { backStackEntry ->
            val route: Screen.AddTransaction = backStackEntry.toRoute()
            val transactionId = route.transactionId

            AddTransactionScreen(
                transactionId = transactionId,
                onShowToast = { message, isError -> mainViewModel.showToast(message, isError) },
                onGlobalRefresh = onGlobalRefresh,
                paddingValues = paddingValues,
                animatedVisibilityScope = this,
                onBack = { navController.popBackStack() }
            )
        }

        composable<Screen.Statistics> { backStackEntry ->
            val route: Screen.Statistics = backStackEntry.toRoute()
            val accountId = route.accountId ?: selectedAccountId

            StatisticsScreen(
                selectedAccountId = accountId,
                refreshTrigger = refreshTrigger,
                paddingValues = paddingValues,
                animatedVisibilityScope = this,
                onCategoryClick = { categoryName: String, categoryId: String, isIncome: Boolean, startDate: String?, endDate: String?, accountIdParam: String ->
                    val isTransactionCost = categoryId == "transaction_cost" || categoryName == "Transaction Fees"
                    navController.navigateThrottled(
                        Screen.TransactionList(
                            accountId = accountIdParam.ifEmpty { accountId ?: "" },
                            isIncome = if (isTransactionCost) null else isIncome,
                            categoryId = if (isTransactionCost) null else categoryId,
                            categoryName = if (isTransactionCost) null else categoryName,
                            startDate = startDate,
                            endDate = endDate,
                            hasTransactionCost = if (isTransactionCost) true else null
                        )
                    )
                }
            )
        }

        composable<Screen.Budget> { 
            BudgetScreen(
                refreshTrigger = refreshTrigger,
                paddingValues = paddingValues,
                animatedVisibilityScope = this,
                onAddBudget = {
                    navController.navigateThrottled(Screen.BudgetDetail(null))
                },
                onBudgetClick = { budgetWithStatus ->
                    navController.navigateThrottled(
                        Screen.BudgetDetail(
                            budgetWithStatus.budget.id
                        )
                    )
                }
            )
        }

        composable<Screen.Profile> { 
            ProfileScreen(
                paddingValues = paddingValues,
                onNavigateToAccounts = { navController.navigateThrottled(Screen.Accounts) },
                onNavigateToCategories = { navController.navigateThrottled(Screen.Categories) },
                onNavigateToSettings = { navController.navigateThrottled(Screen.Settings) },
                onNavigateToEditProfile = { navController.navigateThrottled(Screen.EditProfile) },
                onLogout = onLogout
            )
        }

        composable<Screen.EditProfile> { 
            EditProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onShowToast = { message, isError -> mainViewModel.showToast(message, isError) }
            )
        }

        composable<Screen.Accounts> { 
            AccountsScreen(
                refreshTrigger = refreshTrigger,
                onGlobalRefresh = onGlobalRefresh,
                onShowToast = { message, isError -> mainViewModel.showToast(message, isError) },
                paddingValues = paddingValues
            )
        }

        composable<Screen.Categories> { 
            CategoryManagementScreen(
                refreshTrigger = refreshTrigger,
                onShowToast = { message, isError -> mainViewModel.showToast(message, isError) },
                paddingValues = paddingValues,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Screen.Settings> { 
            SettingsScreen(
                refreshTrigger = refreshTrigger,
                onGlobalRefresh = onGlobalRefresh,
                onShowToast = { message, isError -> mainViewModel.showToast(message, isError) },
                paddingValues = paddingValues
            )
        }

        composable<Screen.BudgetDetail> { backStackEntry ->
            val route: Screen.BudgetDetail = backStackEntry.toRoute()
            val budgetId = route.budgetId

            BudgetDetailScreen(
                budgetId = budgetId,
                onGlobalRefresh = onGlobalRefresh,
                onShowToast = { message, isError -> mainViewModel.showToast(message, isError) },
                paddingValues = paddingValues,
                animatedVisibilityScope = this,
                onSave = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable<Screen.TransactionList> { backStackEntry ->
            val route: Screen.TransactionList = backStackEntry.toRoute()
            val accountId = route.accountId
            val isIncome = route.isIncome
            val categoryId = route.categoryId
            val categoryName = route.categoryName
            val startDate = route.startDate
            val endDate = route.endDate
            val hasTransactionCost = route.hasTransactionCost

            TransactionListScreen(
                accountId = accountId, 
                refreshTrigger = refreshTrigger,
                isIncome = isIncome,
                categoryId = categoryId,
                categoryName = categoryName,
                startDate = startDate,
                endDate = endDate,
                hasTransactionCost = hasTransactionCost,
                paddingValues = paddingValues,
                animatedVisibilityScope = this,
                onEditTransaction = { transactionId ->
                    navController.navigateThrottled(Screen.AddTransaction(transactionId))
                }
            )
        }
    }
}
