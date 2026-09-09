package com.fintrack.shared.feature.summary.domain.model

import com.ionspin.kotlin.bignum.decimal.BigDecimal

data class ProfileMetrics(
    val name: String,
    val email: String,
    val netWorth: BigDecimal,
    val savingsRate: BigDecimal?,
    val essentialSpendRatio: BigDecimal?
)
