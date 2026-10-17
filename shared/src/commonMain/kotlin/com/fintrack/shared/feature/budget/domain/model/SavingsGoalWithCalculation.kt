package com.fintrack.shared.feature.budget.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SavingsGoalWithCalculation(
    val goal: SavingsGoal,
    val requiredMonthlyContribution: Double,
    val remainingMonths: Int,
    val isFeasible: Boolean,
    val monthlySurplus: Double
)
