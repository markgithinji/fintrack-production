package com.fintrack.shared.feature.summary.domain.model

import com.ionspin.kotlin.bignum.decimal.BigDecimal

data class StatisticsSummary(
    val period: String = "",
    val isCurrent: Boolean = true,
    val income: BigDecimal = BigDecimal.ZERO,
    val expense: BigDecimal = BigDecimal.ZERO,
    val balance: BigDecimal = BigDecimal.ZERO,
    val incomeHighlights: Highlights = Highlights(),
    val expenseHighlights: Highlights = Highlights(),
    val totalTransactionCost: BigDecimal = BigDecimal.ZERO,
    val projectedTotal: BigDecimal? = null,
    val amount: BigDecimal = BigDecimal.ZERO,
)

data class Highlights(
    val highestMonth: Highlight? = null,
    val highestCategory: Highlight? = null,
    val highestDay: Highlight? = null,
    val averagePerDay: BigDecimal = BigDecimal.ZERO,
    val ytdChangePercentage: BigDecimal? = null,
    val projectedTotal: BigDecimal? = null,
    val savingsRate: BigDecimal? = null,
    val essentialSpendRatio: BigDecimal? = null,
    val projectedExceedMonth: String? = null,
    val correlations: List<Correlation>? = null
)

data class Correlation(
    val source: String,
    val target: String,
    val insight: String
)

data class Highlight(
    val label: String = "",
    val value: String = "",
    val amount: BigDecimal = BigDecimal.ZERO,
    val volatilityPercentage: BigDecimal? = null
)
