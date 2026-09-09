package com.fintrack.shared.feature.navigation.model

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data class Home(val accountId: String? = null) : Screen

    @Serializable
    data class AddTransaction(val transactionId: String? = null) : Screen

    @Serializable
    data class Statistics(val accountId: String? = null) : Screen

    @Serializable
    data object Budget : Screen

    @Serializable
    data object Profile : Screen

    @Serializable
    data object EditProfile : Screen

    @Serializable
    data object Accounts : Screen

    @Serializable
    data object Categories : Screen

    @Serializable
    data object Settings : Screen

    @Serializable
    data class BudgetDetail(val budgetId: String? = null) : Screen

    @Serializable
    data object Login : Screen

    @Serializable
    data object Register : Screen

    @Serializable
    data class TransactionList(
        val accountId: String,
        val isIncome: Boolean? = null,
        val categoryId: String? = null,
        val categoryName: String? = null,
        val startDate: String? = null,
        val endDate: String? = null,
        val hasTransactionCost: Boolean? = null
    ) : Screen
}
