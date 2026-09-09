package com.fintrack.shared.feature.summary.data.model

import com.fintrack.shared.feature.core.data.serialization.BigDecimalSerializer
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.serialization.Serializable

@Serializable
data class ProfileMetricsDto(
    val name: String,
    val email: String,
    @Serializable(with = BigDecimalSerializer::class)
    val netWorth: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val savingsRate: BigDecimal?,
    @Serializable(with = BigDecimalSerializer::class)
    val essentialSpendRatio: BigDecimal?
)
