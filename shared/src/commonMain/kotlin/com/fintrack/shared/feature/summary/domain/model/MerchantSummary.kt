package com.fintrack.shared.feature.summary.domain.model

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.serialization.Serializable

@Serializable
data class MonthlyTrendPoint(
    val monthLabel: String,
    val amount: Double
)

data class MerchantSummary(
    val rank: Int,
    val merchantName: String,
    val totalSpent: BigDecimal,
    val visitCount: Int,
    val averageTransactionCost: BigDecimal,
    val firstSeenDate: String,
    val lastSeenDate: String,
    val sixMonthTrend: List<MonthlyTrendPoint> = emptyList()
)
