package com.fintrack.shared.feature.transaction.data.model

import com.fintrack.shared.feature.core.data.serialization.BigDecimalSerializer
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class TransactionDto(
    val id: String? = null,
    val accountId: String,
    val isIncome: Boolean,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val transactionCost: BigDecimal = BigDecimal.ZERO,
    val category: String? = null,
    val categoryId: String,
    val dateTime: Instant,
    val description: String? = null,
    val externalId: String? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val balance: BigDecimal? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
)