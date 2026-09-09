package com.fintrack.shared.feature.budget.domain.model

import com.fintrack.shared.feature.category.domain.model.Category
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.LocalDate

data class Budget(
    val id: String? = null,
    val accountIds: List<String>,
    val name: String,
    val categories: List<Category>,
    val limit: BigDecimal,
    val isExpense: Boolean,
    val startDate: LocalDate,
    val endDate: LocalDate
)
