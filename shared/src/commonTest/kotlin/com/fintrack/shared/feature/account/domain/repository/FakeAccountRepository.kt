package com.fintrack.shared.feature.account.domain.repository

import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.core.util.Result

class FakeAccountRepository : AccountRepository {
    private val accounts = mutableListOf<Account>()

    fun setAccounts(list: List<Account>) {
        accounts.clear()
        accounts.addAll(list)
    }

    override suspend fun getAccounts(): Result<List<Account>> = Result.Success(accounts.toList())

    override suspend fun getAccountById(id: String): Result<Account> {
        val acc = accounts.find { it.id == id }
        return if (acc != null) Result.Success(acc) else Result.Error(Exception("Account not found"))
    }

    override suspend fun addOrUpdateAccount(account: Account): Result<Account> {
        val index = accounts.indexOfFirst { it.id == account.id }
        if (index != -1) {
            accounts[index] = account
        } else {
            accounts.add(account)
        }
        return Result.Success(account)
    }

    override suspend fun deleteAccount(id: String): Result<Unit> {
        accounts.removeAll { it.id == id }
        return Result.Success(Unit)
    }
}
