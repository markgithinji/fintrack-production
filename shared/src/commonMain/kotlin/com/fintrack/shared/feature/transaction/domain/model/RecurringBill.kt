package com.fintrack.shared.feature.transaction.domain.model

import com.fintrack.shared.feature.core.data.serialization.BigDecimalSerializer
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class RecurringBill(
    val id: String,
    val name: String,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    val categoryId: String,
    val category: String? = null,
    val frequency: String, // "Monthly", "Weekly", etc.
    val nextDueDate: LocalDate,
    val isActive: Boolean = true
)
