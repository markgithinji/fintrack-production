package com.fintrack.shared.feature.budget.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SavingsGoalWithCalculation(
    val goal: SavingsGoal,
    val requiredMonthlyContribution: Double,
    val remainingMonths: Int,
    val isFeasible: Boolean,
    val monthlySurplus: Double,
    val uncommittedMonthlyCashflow: Double = 0.0,
    val recommendedMonthsNeeded: Int? = null,
    val recommendedTargetDate: String? = null,
    val isCashflowSufficient: Boolean = true,
    val statusReason: String = ""
)
