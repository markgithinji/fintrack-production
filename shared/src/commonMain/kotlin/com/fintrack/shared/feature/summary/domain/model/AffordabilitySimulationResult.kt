package com.fintrack.shared.feature.summary.domain.model

import kotlinx.serialization.Serializable

enum class AffordabilityVerdict {
    SAFE,
    CAUTION,
    RISKY,
    IMMEDIATE_DEFICIT
}

@Serializable
data class AffordabilitySimulationResult(
    val itemPrice: Double,
    val verdict: AffordabilityVerdict,
    val currentAccountCash: Double,
    val projectedEomBalanceBefore: Double,
    val projectedEomBalanceAfter: Double,
    val cushionRemaining: Double,
    val upcomingBillsTotal: Double,
    val requiredMonthlySavingsTotal: Double = 0.0,
    val liquidAssetsTotal: Double = 0.0,
    val activeDebtsTotal: Double = 0.0,
    val remainingDaysInMonth: Int,
    val remainingBurnDaysAfterPurchase: Int? = null,
    val savingsGoalImpactMessage: String? = null,
    val message: String,
    val recommendation: String
)
