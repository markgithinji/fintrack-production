package com.fintrack.shared.feature.summary.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class FireScore(
    val netWorth: Double = 0.0,
    val monthlyLivingExpenses: Double = 0.0,
    val annualLivingExpenses: Double = 0.0,
    val fireTargetAmount: Double = 0.0,
    val fireProgressPercentage: Double = 0.0,
    val phaseName: String = "",
    val estimatedYearsToFire: Double? = null
)
