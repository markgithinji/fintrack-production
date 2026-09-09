package com.fintrack.shared.feature.budget.domain.model

data class BudgetWithStatus(
    val budget: Budget,
    val status: BudgetStatus
)