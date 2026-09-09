package com.fintrack.shared.feature.summary.domain.model

import com.ionspin.kotlin.bignum.decimal.BigDecimal

data class CategoryComparisonSummary(
    val period: String,
    val isCurrent: Boolean = true,
    val data: List<CategoryComparison>
)

data class CategoryComparison(
    val category: String,
    val currentTotal: BigDecimal,
    val previousTotal: BigDecimal,
    val changePercentage: BigDecimal,
    val isIncome: Boolean = false,
    val period: String? = null,
    val weeklyChangePercentage: BigDecimal? = null,
    val weeklyCurrentTotal: BigDecimal? = null
)
