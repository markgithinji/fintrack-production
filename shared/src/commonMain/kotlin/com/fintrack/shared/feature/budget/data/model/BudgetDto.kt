package com.fintrack.shared.feature.budget.data.model

import com.fintrack.shared.feature.core.data.serialization.BigDecimalSerializer
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class BudgetDto(
    val id: String? = null,
    val accountIds: List<String>,
    val name: String,
    val categoryIds: List<String>,
    @Serializable(with = BigDecimalSerializer::class)
    val limit: BigDecimal,
    val isExpense: Boolean,
    val startDate: LocalDate,
    val endDate: LocalDate
)
