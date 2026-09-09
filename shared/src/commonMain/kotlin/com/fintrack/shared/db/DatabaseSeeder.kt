package com.fintrack.shared.db

import com.fintrack.shared.feature.core.util.randomUUID
import com.fintrack.shared.feature.core.util.DateTimeHelper
import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.category.domain.model.allCategories
import kotlinx.datetime.Instant

class DatabaseSeeder(private val database: FintrackDatabase) {
    private val queries = database.fintrackDatabaseQueries
    private val offlineUserId = "offline_user"

    fun seedIfEmpty() {
        seedAccounts()
        seedCategories()
    }

    private fun seedAccounts() {
        val accounts = queries.selectAllAccounts(offlineUserId).executeAsList()
        if (accounts.isEmpty()) {
            val defaultAccounts = listOf(
                AccountData("Mpesa", "MPESA", "2024-01-01T00:00:00Z"),
                AccountData("Bank", "BANK", "2024-01-01T00:00:01Z"),
                AccountData("Wallet", "WALLET", "2024-01-01T00:00:02Z"),
                AccountData("Savings", "SAVINGS", "2024-01-01T00:00:03Z"),
                AccountData("Cash", "CASH", "2024-01-01T00:00:04Z")
            )

            defaultAccounts.forEach { account ->
                queries.insertAccount(
                    id = randomUUID(),
                    userId = offlineUserId,
                    name = account.name,
                    isDefault = 1L,
                    type = account.type,
                    balance = "0",
                    createdAt = Instant.parse(account.createdAt),
                    lastSyncedAt = null
                )
            }
        }
    }

    private fun seedCategories() {
        val categories = queries.selectAllCategories(offlineUserId).executeAsList()
        if (categories.isEmpty()) {
            Category.allCategories.forEach { category ->
                queries.insertCategory(
                    id = category.id,
                    userId = offlineUserId,
                    name = category.name,
                    isExpense = if (category.isExpense) 1L else 0L,
                    iconName = category.iconName ?: "default",
                    isDefault = if (category.isDefault) 1L else 0L,
                    createdAt = DateTimeHelper.now()
                )
            }
        }
    }

    private data class AccountData(
        val name: String,
        val type: String,
        val createdAt: String
    )
}
