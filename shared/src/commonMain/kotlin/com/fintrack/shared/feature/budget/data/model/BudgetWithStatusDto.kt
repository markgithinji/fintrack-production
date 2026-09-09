package com.fintrack.shared.feature.budget.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BudgetWithStatusDto(
    val budget: BudgetDto,
    val status: BudgetStatusDto
)