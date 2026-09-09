package com.fintrack.shared.feature.summary.domain.model

import com.ionspin.kotlin.bignum.decimal.BigDecimal

data class TransactionCountSummary(
    val totalIncomeTransactions: Int,
    val totalExpenseTransactions: Int,
    val totalTransactions: Int,
    val totalAmount: BigDecimal = BigDecimal.ZERO,
    val totalTransactionCost: BigDecimal = BigDecimal.ZERO
)
