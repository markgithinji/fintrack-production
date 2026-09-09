package com.fintrack.shared.feature.account.data.repository

import com.fintrack.shared.db.FintrackDatabase
import com.fintrack.shared.feature.account.data.model.toDomain
import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.account.domain.repository.AccountRepository
import com.fintrack.shared.feature.core.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.fintrack.shared.feature.core.util.DateTimeHelper

class AccountRepositoryOfflineImpl(
    private val database: FintrackDatabase
) : AccountRepository {

    private val queries = database.fintrackDatabaseQueries
    private val offlineUserId = "offline_user"

    override suspend fun getAccounts(): Result<List<Account>> = withContext(Dispatchers.IO) {
        try {
            val accounts = queries.selectAllAccounts(offlineUserId).executeAsList().map { it.toDomain() }
            Result.Success(accounts)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getAccountById(id: String): Result<Account> = withContext(Dispatchers.IO) {
        try {
            val account = queries.selectAccountById(id).executeAsOneOrNull()?.toDomain()
            if (account != null) Result.Success(account) else Result.Error(Exception("Account not found"))
        } catch (e: Exception) {
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
                createdAt = account.createdAt ?: DateTimeHelper.now(),
                lastSyncedAt = account.lastSyncedAt
            )
            Result.Success(account)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteAccount(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.deleteAccount(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
