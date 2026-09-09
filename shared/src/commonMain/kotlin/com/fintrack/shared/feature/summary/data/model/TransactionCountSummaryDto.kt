package com.fintrack.shared.feature.summary.data.model

import com.fintrack.shared.feature.core.data.serialization.BigDecimalSerializer
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.serialization.Serializable

@Serializable
data class TransactionCountSummaryDto(
    val totalIncomeTransactions: Int,
    val totalExpenseTransactions: Int,
    val totalTransactions: Int,
    @Serializable(with = BigDecimalSerializer::class)
    val totalAmount: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val totalTransactionCost: BigDecimal = BigDecimal.ZERO
)
