package com.fintrack.shared.feature.account.data.repository

import com.fintrack.shared.feature.account.data.model.toCreateRequest
import com.fintrack.shared.feature.account.data.model.toDomain
import com.fintrack.shared.feature.account.data.model.toUpdateRequest
import com.fintrack.shared.feature.account.data.remote.AccountsApi
import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.account.domain.repository.AccountRepository
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.core.util.safeApiCall

class AccountRepositoryImpl(
    private val accountsApi: AccountsApi
) : AccountRepository {

    override suspend fun getAccounts(): Result<List<Account>> = safeApiCall {
        val accountsDto = accountsApi.getAccounts()
        accountsDto.map { it.toDomain() }
    }

    override suspend fun getAccountById(id: String): Result<Account> = safeApiCall {
        val accountDto = accountsApi.getAccountById(id)
        accountDto.toDomain()
    }

    override suspend fun addOrUpdateAccount(account: Account): Result<Account> = safeApiCall {
        if (account.id.isEmpty()) {
            // Create account
            val createRequest = account.toCreateRequest()
            val dto = accountsApi.addAccount(createRequest)
            dto.toDomain()
        } else {
            // Update account
            val updateRequest = account.toUpdateRequest()
            val dto = accountsApi.updateAccount(account.id, updateRequest)
            dto.toDomain()
        }
    }

    override suspend fun deleteAccount(id: String): Result<Unit> = safeApiCall {
        accountsApi.deleteAccount(id)
    }
}