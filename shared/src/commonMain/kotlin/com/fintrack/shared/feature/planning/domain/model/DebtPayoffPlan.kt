package com.fintrack.shared.feature.planning.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DebtWithSchedule(
    val debt: Debt,
    val payOffRank: Int,
    val estimatedPayoffMonths: Int,
    val estimatedFreedomDate: String,
    val totalInterestPaid: Double,
    val isNegativeAmortization: Boolean = false,
    val minPaymentRequiredToAmortize: Double? = null
)

@Serializable
data class DebtPayoffPlan(
    val strategy: DebtStrategy,
    val totalBalance: Double,
    val totalInterestToPay: Double,
    val interestSavedVsOtherStrategy: Double,
    val estimatedPayoffMonths: Int,
    val estimatedFreedomDate: String,
    val orderedDebts: List<DebtWithSchedule>,
    val hasNegativeAmortization: Boolean = false
)
