package com.fintrack.shared.feature.budget.domain.model

import com.ionspin.kotlin.bignum.decimal.BigDecimal

data class BudgetStatus(
    val spent: BigDecimal,
    val remaining: BigDecimal,
    val percentageUsed: BigDecimal,
    val isExceeded: Boolean
)
