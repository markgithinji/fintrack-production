package com.fintrack.shared.feature.summary.domain.model

import com.ionspin.kotlin.bignum.decimal.BigDecimal

data class OverviewSummary(
    val period: String,
    val isCurrent: Boolean,
    val weeklyOverview: List<DaySummary>,
    val monthlyOverview: List<DaySummary>
)

data class DaySummary(
    val date: String,
    val income: BigDecimal,
    val expense: BigDecimal
)
