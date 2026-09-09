package com.fintrack.shared.feature.transaction.data.model

import com.fintrack.shared.db.TransactionEntity
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.ionspin.kotlin.bignum.decimal.BigDecimal

fun TransactionEntity.toDomain(categoryName: String? = null): Transaction {
    return Transaction(
        id = id,
        accountId = accountId,
        isIncome = isIncome != 0L,
        amount = BigDecimal.parseString(amount),
        transactionCost = BigDecimal.parseString(transactionCost),
        category = categoryName ?: "Uncategorized",
        categoryId = categoryId,
        dateTime = dateTime,
        description = description,
        externalId = externalId,
        balance = balance?.let { BigDecimal.parseString(it) }
    )
}
