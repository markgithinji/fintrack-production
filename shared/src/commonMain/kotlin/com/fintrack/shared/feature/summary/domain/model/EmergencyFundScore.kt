package com.fintrack.shared.feature.summary.domain.model

import com.ionspin.kotlin.bignum.decimal.BigDecimal

data class EmergencyFundScore(
    val liquidAssetsTotal: BigDecimal,
    val liquidCashTotal: BigDecimal = BigDecimal.ZERO,
    val savingsVaultsTotal: BigDecimal = BigDecimal.ZERO,
    val investmentsTotal: BigDecimal = BigDecimal.ZERO,
    val monthlyEssentialExpenses: BigDecimal,
    val monthsCovered: BigDecimal,
    val targetMonths: BigDecimal = BigDecimal.fromInt(6),
    val readinessStatus: String
)
