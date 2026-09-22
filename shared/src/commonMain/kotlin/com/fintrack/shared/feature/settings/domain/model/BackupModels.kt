package com.fintrack.shared.feature.settings.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AccountBackupDto(
    val id: String,
    val name: String,
    val isDefault: Boolean,
    val type: String,
    val balance: String,
    val linkedSources: String? = null,
    val createdAt: String,
    val lastSyncedAt: String? = null
)

@Serializable
data class CategoryBackupDto(
    val id: String,
    val name: String,
    val isExpense: Boolean,
    val iconName: String? = null,
    val isDefault: Boolean
)

@Serializable
data class BudgetBackupDto(
    val id: String,
    val name: String,
    val limitAmount: String,
    val isExpense: Boolean,
    val startDate: String,
    val endDate: String,
    val categoryIds: String,
    val accountIds: String
)

@Serializable
data class TransactionBackupDto(
    val id: String,
    val accountId: String,
    val categoryId: String,
    val isIncome: Boolean,
    val amount: String,
    val transactionCost: String,
    val dateTime: String,
    val description: String? = null,
    val externalId: String? = null,
    val balance: String? = null
)

@Serializable
data class FullBackupData(
    val version: Int = 1,
    val exportedAt: String,
    val accounts: List<AccountBackupDto>,
    val categories: List<CategoryBackupDto>,
    val budgets: List<BudgetBackupDto>,
    val transactions: List<TransactionBackupDto>
)
