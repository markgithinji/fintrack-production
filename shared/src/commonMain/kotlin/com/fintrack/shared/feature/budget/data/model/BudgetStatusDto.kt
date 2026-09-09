package com.fintrack.shared.feature.budget.data.model

import com.fintrack.shared.feature.core.data.serialization.BigDecimalSerializer
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.serialization.Serializable

@Serializable
data class BudgetStatusDto(
    @Serializable(with = BigDecimalSerializer::class)
    val spent: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val remaining: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val percentageUsed: BigDecimal,
    val isExceeded: Boolean
)
