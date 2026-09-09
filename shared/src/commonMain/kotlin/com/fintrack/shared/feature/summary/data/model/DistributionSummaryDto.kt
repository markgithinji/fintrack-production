package com.fintrack.shared.feature.summary.data.model

import com.fintrack.shared.feature.core.data.serialization.BigDecimalSerializer
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.serialization.Serializable

@Serializable
data class DistributionSummaryDto(
    val period: String = "", // e.g. "2025-W37" or "2025-09"
    @Serializable(with = BigDecimalSerializer::class)
    val totalTransactionCost: BigDecimal = BigDecimal.ZERO,
    val incomeCategories: List<CategorySummaryDto> = emptyList(),
    val expenseCategories: List<CategorySummaryDto> = emptyList(),
    val othersInsightSummary: String? = null
)

@Serializable
data class CategorySummaryDto(
    val category: String = "",
    val categoryId: String = "",
    @Serializable(with = BigDecimalSerializer::class)
    val total: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val percentage: BigDecimal = BigDecimal.ZERO,
    val transactionCount: Int = 0,
    @Serializable(with = BigDecimalSerializer::class)
    val averageTransactionCount: BigDecimal? = null,
    val momentumTrend: String? = null,
    val topDescriptionInsights: List<String>? = null
)
