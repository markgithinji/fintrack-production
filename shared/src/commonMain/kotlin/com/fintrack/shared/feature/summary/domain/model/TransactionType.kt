package com.fintrack.shared.feature.summary.domain.model

sealed class TransactionType(val apiName: String) {
    object Income : TransactionType("income")
    object Expense : TransactionType("expense")
}