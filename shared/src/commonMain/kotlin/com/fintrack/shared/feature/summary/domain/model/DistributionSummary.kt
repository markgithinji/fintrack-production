package com.fintrack.shared.feature.summary.domain.model

import com.ionspin.kotlin.bignum.decimal.BigDecimal

data class DistributionSummary(
    val period: String,
    val totalTransactionCost: BigDecimal,
    val incomeCategories: List<CategorySummary>,
    val expenseCategories: List<CategorySummary>,
    val othersInsightSummary: String? = null
)

data class CategorySummary(
    val category: String,
    val categoryId: String,
    val total: BigDecimal,
    val percentage: BigDecimal,
    val transactionCount: Int,
    val averageTransactionCount: BigDecimal? = null,
    val momentumTrend: String? = null,
    val topDescriptionInsights: List<String>? = null
)
