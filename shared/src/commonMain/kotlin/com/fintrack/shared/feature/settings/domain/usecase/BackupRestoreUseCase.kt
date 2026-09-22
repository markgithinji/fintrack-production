package com.fintrack.shared.feature.settings.domain.usecase

import com.fintrack.shared.db.FintrackDatabase
import com.fintrack.shared.feature.core.logger.KMPLogger
import com.fintrack.shared.feature.core.util.DateTimeHelper
import com.fintrack.shared.feature.core.util.FileSaver
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.settings.domain.model.AccountBackupDto
import com.fintrack.shared.feature.settings.domain.model.BudgetBackupDto
import com.fintrack.shared.feature.settings.domain.model.CategoryBackupDto
import com.fintrack.shared.feature.settings.domain.model.FullBackupData
import com.fintrack.shared.feature.settings.domain.model.TransactionBackupDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock

class BackupRestoreUseCase(
    database: FintrackDatabase,
    private val fileSaver: FileSaver,
    private val logger: KMPLogger
) {
    private val queries = database.fintrackDatabaseQueries
    private val offlineUserId = "offline_user"
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    private val tag = "BackupRestoreUseCase"

    suspend fun createBackupJson(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val accounts = queries.selectAllAccounts(offlineUserId).executeAsList().map {
                AccountBackupDto(
                    id = it.id,
                    name = it.name,
                    isDefault = it.isDefault != 0L,
                    type = it.type,
                    balance = it.balance,
                    linkedSources = it.linkedSources.joinToString(","),
                    createdAt = it.createdAt.toString(),
                    lastSyncedAt = it.lastSyncedAt?.toString()
                )
            }

            val categories = queries.selectAllCategories(offlineUserId).executeAsList().map {
                CategoryBackupDto(
                    id = it.id,
                    name = it.name,
                    isExpense = it.isExpense != 0L,
                    iconName = it.iconName,
                    isDefault = it.isDefault != 0L
                )
            }

            val budgets = queries.selectAllBudgets(offlineUserId).executeAsList().map {
                BudgetBackupDto(
                    id = it.id,
                    name = it.name,
                    limitAmount = it.limitAmount,
                    isExpense = it.isExpense != 0L,
                    startDate = it.startDate,
                    endDate = it.endDate,
                    categoryIds = it.categoryIds,
                    accountIds = it.accountIds
                )
            }

            val transactions = queries.selectAllTransactions(offlineUserId).executeAsList().map {
                TransactionBackupDto(
                    id = it.id,
                    accountId = it.accountId,
                    categoryId = it.categoryId,
                    isIncome = it.isIncome != 0L,
                    amount = it.amount,
                    transactionCost = it.transactionCost,
                    dateTime = it.dateTime.toString(),
                    description = it.description,
                    externalId = it.externalId,
                    balance = it.balance
                )
            }

            val backupData = FullBackupData(
                version = 1,
                exportedAt = Clock.System.now().toString(),
                accounts = accounts,
                categories = categories,
                budgets = budgets,
                transactions = transactions
            )

            val jsonString = json.encodeToString(backupData)
            Result.Success(jsonString)
        } catch (e: Exception) {
            logger.error(tag, "Failed to create backup JSON", e)
            Result.Error(e)
        }
    }

    suspend fun exportLocalBackup(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jsonResult = createBackupJson()
            if (jsonResult !is Result.Success) return@withContext Result.Error((jsonResult as Result.Error).exception)

            val now = Clock.System.now()
            val timestamp = now.toLocalDateTime(TimeZone.currentSystemDefault())
                .toString()
                .replace(":", "-")
                .split(".")[0]
            val fileName = "fintrack_backup_$timestamp.json"

            val savedPath = fileSaver.saveFile(fileName, jsonResult.data)
            if (savedPath != null) {
                Result.Success(savedPath)
            } else {
                Result.Error(Exception("Failed to save local backup file"))
            }
        } catch (e: Exception) {
            logger.error(tag, "Failed to export local backup", e)
            Result.Error(e)
        }
    }

    suspend fun restoreFromBackupJson(jsonString: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val backupData = json.decodeFromString<FullBackupData>(jsonString)

            queries.transaction {
                // Restore Accounts
                backupData.accounts.forEach { acc ->
                    val linkedSourcesList = acc.linkedSources?.split(",")?.filter { s -> s.isNotEmpty() } ?: emptyList()
                    val createdInstant = try { Instant.parse(acc.createdAt) } catch (_: Exception) { DateTimeHelper.now() }
                    val lastSyncedInstant = acc.lastSyncedAt?.let { s -> try { Instant.parse(s) } catch (_: Exception) { null } }

                    queries.insertAccount(
                        id = acc.id,
                        userId = offlineUserId,
                        name = acc.name,
                        isDefault = if (acc.isDefault) 1L else 0L,
                        type = acc.type,
                        balance = acc.balance,
                        linkedSources = linkedSourcesList,
                        createdAt = createdInstant,
                        lastSyncedAt = lastSyncedInstant
                    )
                }

                // Restore Categories
                backupData.categories.forEach { cat ->
                    queries.insertCategory(
                        id = cat.id,
                        userId = offlineUserId,
                        name = cat.name,
                        isExpense = if (cat.isExpense) 1L else 0L,
                        iconName = cat.iconName,
                        isDefault = if (cat.isDefault) 1L else 0L,
                        createdAt = DateTimeHelper.now()
                    )
                }

                // Restore Budgets
                backupData.budgets.forEach { b ->
                    queries.insertBudget(
                        id = b.id,
                        userId = offlineUserId,
                        name = b.name,
                        limitAmount = b.limitAmount,
                        isExpense = if (b.isExpense) 1L else 0L,
                        startDate = b.startDate,
                        endDate = b.endDate,
                        categoryIds = b.categoryIds,
                        accountIds = b.accountIds,
                        createdAt = DateTimeHelper.now()
                    )
                }

                // Restore Transactions
                backupData.transactions.forEach { tx ->
                    val dtInstant = try { Instant.parse(tx.dateTime) } catch (_: Exception) { DateTimeHelper.now() }
                    queries.insertTransaction(
                        id = tx.id,
                        userId = offlineUserId,
                        accountId = tx.accountId,
                        categoryId = tx.categoryId,
                        isIncome = if (tx.isIncome) 1L else 0L,
                        amount = tx.amount,
                        transactionCost = tx.transactionCost,
                        dateTime = dtInstant,
                        description = tx.description,
                        externalId = tx.externalId,
                        balance = tx.balance,
                        createdAt = DateTimeHelper.now(),
                        updatedAt = DateTimeHelper.now()
                    )
                }
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            logger.error(tag, "Failed to restore database from backup JSON", e)
            Result.Error(e)
        }
    }
}
