package com.fintrack.shared.feature.summary.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PassiveIncomeCoverage(
    val monthlyPassiveYieldAmount: Double = 0.0,
    val monthlyPassiveInflowTransactions: Double = 0.0,
    val totalMonthlyPassiveIncome: Double = 0.0,
    val monthlyEssentialExpenses: Double = 0.0,
    val coveragePercentage: Double = 0.0,
    val milestoneDescription: String = ""
)
