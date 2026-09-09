package com.fintrack.shared.feature.navigation.ui

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import com.fintrack.shared.feature.navigation.model.AppBarState
import com.fintrack.shared.feature.navigation.model.Screen

fun NavDestination.isAuthScreen(): Boolean =
    hasRoute<Screen.Login>() || hasRoute<Screen.Register>()

fun NavDestination.isMorphScreen(): Boolean =
    hasRoute<Screen.BudgetDetail>() ||
            hasRoute<Screen.TransactionList>() ||
            hasRoute<Screen.AddTransaction>()

fun NavDestination.isProfileDetailScreen(): Boolean =
    hasRoute<Screen.Accounts>() ||
            hasRoute<Screen.Categories>() ||
            hasRoute<Screen.Settings>() ||
            hasRoute<Screen.EditProfile>()

fun NavDestination.isMainScreen(): Boolean =
    hasRoute<Screen.Home>() ||
            hasRoute<Screen.Statistics>() ||
            hasRoute<Screen.Budget>() ||
            hasRoute<Screen.Profile>()

fun NavDestination.shouldShowBottomBar(): Boolean = isMainScreen()

fun NavDestination.shouldShowFAB(): Boolean = shouldShowBottomBar()

fun NavDestination.getAppBarState(entry: NavBackStackEntry?, navController: NavController): AppBarState {
    if (entry == null) return AppBarState(title = "Home")

    return when {
        hasRoute<Screen.Home>() -> AppBarState(title = "Home")
        hasRoute<Screen.Statistics>() -> AppBarState(title = "Statistics")
        hasRoute<Screen.Budget>() -> AppBarState(title = "Budget")
        hasRoute<Screen.Profile>() -> AppBarState(title = "Profile")
        hasRoute<Screen.EditProfile>() -> AppBarState(
            title = "Edit Profile",
            showBackButton = true,
            onBack = { navController.popBackStack() }
        )
        hasRoute<Screen.Accounts>() -> AppBarState(
            title = "Accounts",
            showBackButton = true,
            onBack = { navController.popBackStack() }
        )
        hasRoute<Screen.Categories>() -> AppBarState(
            title = "Categories",
            showBackButton = true,
            onBack = { navController.popBackStack() }
        )
        hasRoute<Screen.Settings>() -> AppBarState(
            title = "Settings",
            showBackButton = true,
            onBack = { navController.popBackStack() }
        )
        hasRoute<Screen.BudgetDetail>() -> {
            val route = entry.toRoute<Screen.BudgetDetail>()
            AppBarState(
                title = if (route.budgetId == null) "Add Budget" else "Edit Budget",
                showBackButton = true,
                onBack = { navController.popBackStack() }
            )
        }
        hasRoute<Screen.AddTransaction>() -> {
            val route = entry.toRoute<Screen.AddTransaction>()
            AppBarState(
                title = if (route.transactionId == null) "Add Transaction" else "Edit Transaction",
                showBackButton = true,
                onBack = { navController.popBackStack() }
            )
        }
        hasRoute<Screen.TransactionList>() -> {
            val route = entry.toRoute<Screen.TransactionList>()
            AppBarState(
                title = when {
                    route.hasTransactionCost == true -> "Transaction Fees"
                    route.categoryName?.contains(",") == true -> "Other Categories"
                    route.categoryName != null -> route.categoryName
                    route.isIncome == true -> "Income Transactions"
                    route.isIncome == false -> "Expense Transactions"
                    else -> "All Transactions"
                },
                showBackButton = true,
                onBack = { navController.popBackStack() }
            )
        }
        hasRoute<Screen.Login>() -> AppBarState(title = "Login")
        hasRoute<Screen.Register>() -> AppBarState(
            title = "Create Account",
            showBackButton = true,
            onBack = {
                navController.navigate(Screen.Login) {
                    popUpTo(Screen.Register) { inclusive = true }
                }
            }
        )
        else -> AppBarState(title = "Fintrack")
    }
}
