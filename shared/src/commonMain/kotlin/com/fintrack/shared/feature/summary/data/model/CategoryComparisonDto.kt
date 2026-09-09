package com.fintrack.shared.feature.summary.data.model

import com.fintrack.shared.feature.core.data.serialization.BigDecimalSerializer
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.serialization.Serializable

@Serializable
data class CategoryComparisonDto(
    val category: String,
    @Serializable(with = BigDecimalSerializer::class)
    val currentTotal: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val previousTotal: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val changePercentage: BigDecimal,
    val isIncome: Boolean = false,
    val period: String? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val weeklyChangePercentage: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val weeklyCurrentTotal: BigDecimal? = null
)
