package com.fintrack.shared.feature.transaction.data.model

import com.fintrack.shared.feature.transaction.domain.model.Transaction

fun TransactionDto.toDomain(): Transaction =
    Transaction(
        id = id,
        accountId = accountId,
        isIncome = isIncome,
        amount = amount,
        transactionCost = transactionCost,
        category = category,
        categoryId = categoryId,
        dateTime = dateTime,
        description = description,
        externalId = externalId,
        balance = balance
    )

fun Transaction.toCreateRequest(): CreateTransactionRequest {
    return CreateTransactionRequest(
        accountId = this.accountId,
        isIncome = this.isIncome,
        amount = this.amount,
        transactionCost = this.transactionCost,
        category = this.category ?: "",
        categoryId = this.categoryId,
        dateTime = this.dateTime,
        description = this.description ?: "",
        externalId = this.externalId,
        balance = this.balance
    )
}
