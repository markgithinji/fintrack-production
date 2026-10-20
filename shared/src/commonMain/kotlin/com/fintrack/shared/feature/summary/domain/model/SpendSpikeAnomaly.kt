package com.fintrack.shared.feature.summary.domain.model

import com.ionspin.kotlin.bignum.decimal.BigDecimal

data class SpendSpikeAnomaly(
    val categoryName: String,
    val categoryId: String,
    val currentMonthAmount: BigDecimal,
    val historical3MonthAvgAmount: BigDecimal,
    val spikePercentage: BigDecimal,
    val alertMessage: String
)
