package com.fintrack.shared.feature.summary.data.model

import com.fintrack.shared.feature.core.data.serialization.BigDecimalSerializer
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.serialization.Serializable

@Serializable
data class OverviewSummaryDto(
    val period: String = "",
    val isCurrent: Boolean = true,
    val weeklyOverview: List<DaySummaryDto>,
    val monthlyOverview: List<DaySummaryDto>
)

@Serializable
data class DaySummaryDto(
    val date: String,
    @Serializable(with = BigDecimalSerializer::class)
    val income: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val expense: BigDecimal
)
