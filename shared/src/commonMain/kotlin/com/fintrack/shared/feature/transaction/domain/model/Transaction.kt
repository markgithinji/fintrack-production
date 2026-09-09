package com.fintrack.shared.feature.transaction.domain.model

import com.fintrack.shared.feature.core.data.serialization.BigDecimalSerializer
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Transaction(
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
) {
    /**
     * The final amount that impacts the account balance.
     * For expenses, it's the amount plus the transaction cost.
     * For income, it's the amount minus the transaction cost (net income).
     */
    val totalAmount: BigDecimal
        get() = if (isIncome) amount - transactionCost else amount + transactionCost
}
