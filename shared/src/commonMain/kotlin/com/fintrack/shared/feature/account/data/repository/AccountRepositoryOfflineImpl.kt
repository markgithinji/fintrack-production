package com.fintrack.shared.feature.account.data.repository

import com.fintrack.shared.db.FintrackDatabase
import com.fintrack.shared.feature.account.data.model.toDomain
import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.account.domain.repository.AccountRepository
import com.fintrack.shared.feature.core.logger.KMPLogger
import com.fintrack.shared.feature.core.util.DateTimeHelper
import com.fintrack.shared.feature.core.util.DateTimeUtils
import com.fintrack.shared.feature.core.util.Result
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant

class AccountRepositoryOfflineImpl(
    database: FintrackDatabase,
    private val logger: KMPLogger
) : AccountRepository {

    private val queries = database.fintrackDatabaseQueries
    private val offlineUserId = "offline_user"
    private val tag = "AccountRepo"

    override suspend fun getAccounts(): Result<List<Account>> = withContext(Dispatchers.IO) {
        try {
            val accounts = queries.selectAllAccounts(offlineUserId).executeAsList().map { entity ->
                val highlights = try {
                    queries.getHighlights(
                        accountId = entity.id,
                        userId = offlineUserId
                    ).executeAsOne()
                } catch (_: Exception) {
                    null
                }

                val latestTxBalance = queries.getLatestTransactionBalance(entity.id).executeAsOneOrNull()?.balance
                val income = BigDecimal.fromDouble(highlights?.incomeTotal ?: 0.0)
                val expense = BigDecimal.fromDouble(highlights?.expenseTotal ?: 0.0)
                val manualBalance = BigDecimal.parseString(entity.balance)
                
                // Backend Balance Derivation Logic
                val derivedBalance = if (latestTxBalance != null) {
                    BigDecimal.parseString(latestTxBalance)
                } else if (manualBalance != BigDecimal.ZERO || entity.lastSyncedAt != null) {
                    manualBalance
                } else if (income != BigDecimal.ZERO || expense != BigDecimal.ZERO) {
                    income - expense
                } else {
                    manualBalance
                }
                
                entity.toDomain().copy(
                    income = income,
                    expense = expense,
                    balance = derivedBalance
                )
            }
            Result.Success(accounts)
        } catch (e: Exception) {
            logger.error(tag, "Error fetching accounts", e)
            Result.Error(e)
        }
    }

    override suspend fun getAccountById(id: String): Result<Account> = withContext(Dispatchers.IO) {
        try {
            val entity = queries.selectAccountById(id).executeAsOneOrNull()
            if (entity == null) return@withContext Result.Error(Exception("Account not found"))

            val highlights = try {
                queries.getHighlights(
                    accountId = id,
                    userId = offlineUserId
                ).executeAsOne()
            } catch (_: Exception) {
                null
            }

            val latestTxBalance = queries.getLatestTransactionBalance(id).executeAsOneOrNull()?.balance
            val income = BigDecimal.fromDouble(highlights?.incomeTotal ?: 0.0)
            val expense = BigDecimal.fromDouble(highlights?.expenseTotal ?: 0.0)
            val manualBalance = BigDecimal.parseString(entity.balance)
            
            // Backend Balance Derivation Logic
            val derivedBalance = if (latestTxBalance != null) {
                BigDecimal.parseString(latestTxBalance)
            } else if (manualBalance != BigDecimal.ZERO || entity.lastSyncedAt != null) {
                manualBalance
            } else if (income != BigDecimal.ZERO || expense != BigDecimal.ZERO) {
                income - expense
            } else {
                manualBalance
            }

            val account = entity.toDomain().copy(
                income = income,
                expense = expense,
                balance = derivedBalance
            )
            Result.Success(account)
        } catch (e: Exception) {
            logger.error(tag, "Error fetching account $id", e)
            Result.Error(e)
        }
    }

    override suspend fun addOrUpdateAccount(account: Account): Result<Account> = withContext(Dispatchers.IO) {
        try {
            queries.insertAccount(
                id = account.id,
                userId = offlineUserId,
                name = account.name,
                isDefault = if (account.isDefault) 1L else 0L,
                type = account.type.name,
                balance = (account.balance ?: BigDecimal.ZERO).toPlainString(),
                linkedSources = account.linkedSources,
                createdAt = account.createdAt ?: DateTimeHelper.now(),
                lastSyncedAt = account.lastSyncedAt
            )
            Result.Success(account)
        } catch (e: Exception) {
            logger.error(tag, "Error saving account ${account.name}", e)
            Result.Error(e)
        }
    }

    override suspend fun deleteAccount(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.deleteAccount(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.error(tag, "Error deleting account $id", e)
            Result.Error(e)
        }
    }
}
