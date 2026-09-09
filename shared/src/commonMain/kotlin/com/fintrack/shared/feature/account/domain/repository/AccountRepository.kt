package com.fintrack.shared.feature.account.domain.repository

import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.core.util.Result

interface AccountRepository {
    suspend fun getAccounts(): Result<List<Account>>
    suspend fun getAccountById(id: String): Result<Account>
    suspend fun addOrUpdateAccount(account: Account): Result<Account>
    suspend fun deleteAccount(id: String): Result<Unit>
}