package com.fintrack.shared.feature.summary.ui.custom

import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.transaction.domain.model.Transaction

enum class DatePreset(val label: String) {
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    LAST_90_DAYS("Last 90 Days"),
    THIS_YEAR("This Year"),
    ALL_TIME("All Time"),
    CUSTOM("Custom")
}

enum class TransactionTypeFilter(val label: String) {
    ALL("All Types"),
    EXPENSE_ONLY("Expenses Only"),
    INCOME_ONLY("Income Only")
}

data class CategoryShare(
    val categoryId: String,
    val categoryName: String,
    val totalAmount: Double,
    val percentage: Double,
    val isExpense: Boolean
)

data class CustomAnalysisState(
    // Filter parameters
    val datePreset: DatePreset = DatePreset.THIS_MONTH,
    val startDate: String? = null,
    val endDate: String? = null,
    val selectedAccountIds: Set<String> = emptySet(),
    val selectedCategoryIds: Set<String> = emptySet(),
    val typeFilter: TransactionTypeFilter = TransactionTypeFilter.ALL,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,

    // Available Metadata
    val allAccounts: List<Account> = emptyList(),
    val allCategories: List<Category> = emptyList(),

    // Calculated Insights
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netFlow: Double = 0.0,
    val averageTransaction: Double = 0.0,
    val highestTransaction: Transaction? = null,
    val transactionCount: Int = 0,
    val categoryBreakdown: List<CategoryShare> = emptyList(),
    val matchingTransactions: List<Transaction> = emptyList(),

    // UI state
    val isLoading: Boolean = true,
    val isExporting: Boolean = false,
    val exportSuccessMessage: String? = null,
    val error: String? = null
)
