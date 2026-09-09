package com.fintrack.shared.feature.summary.data.model

import com.fintrack.shared.feature.core.data.serialization.BigDecimalSerializer
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.serialization.Serializable

@Serializable
data class HighlightsSummaryDto(
    val period: String = "",
    val isCurrent: Boolean = true,
    @Serializable(with = BigDecimalSerializer::class)
    val income: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val expense: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val balance: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val totalTransactionCost: BigDecimal = BigDecimal.ZERO,
    val incomeHighlights: HighlightsDto = HighlightsDto(),
    val expenseHighlights: HighlightsDto = HighlightsDto()
)

@Serializable
data class HighlightsDto(
    val highestMonth: HighlightDto? = null,
    val highestCategory: HighlightDto? = null,
    val highestDay: HighlightDto? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val averagePerDay: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val ytdChangePercentage: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val projectedTotal: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val savingsRate: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val essentialSpendRatio: BigDecimal? = null,
    val projectedExceedMonth: String? = null,
    val correlations: List<CorrelationDto>? = null
)

@Serializable
data class CorrelationDto(
    val source: String,
    val target: String,
    val insight: String
)

@Serializable
data class HighlightDto(
    val label: String = "",
    val value: String = "",
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val volatilityPercentage: BigDecimal? = null
)
