package com.fintrack.shared.feature.summary.domain.model

sealed class TabType(val displayName: String) {
    data object Income : TabType("Income")
    data object Expense : TabType("Expenses")
}