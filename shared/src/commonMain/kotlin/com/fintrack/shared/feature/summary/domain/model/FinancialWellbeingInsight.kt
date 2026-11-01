package com.fintrack.shared.feature.summary.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class WellbeingInsightCategory {
    INSURANCE_HEALTH,
    FEE_LEAK_PREVENTION,
    IDLE_CASH_YIELD,
    EMERGENCY_SHIELD,
    DEBT_PAYOFF_BOOST
}

@Serializable
data class FinancialWellbeingInsight(
    val id: String,
    val category: WellbeingInsightCategory,
    val title: String,
    val actionTip: String,
    val estimatedMonthlyImpactAmount: Double? = null,
    val iconName: String = "HealthAndSafety"
)
