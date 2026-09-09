package com.fintrack.shared.feature.account.domain.usecase

import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.account.domain.repository.AccountRepository
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.core.util.map

class GetAccountsUseCase(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(): Result<List<Account>> {
        return accountRepository.getAccounts().map { accounts ->
            // Simple sorting:
            // 1. Primary Default account first
            // 2. Then by creation time (oldest first)
            accounts.sortedWith { a, b ->
                when {
                    a.isDefault != b.isDefault -> if (a.isDefault) -1 else 1
                    else -> {
                        val timeA = a.createdAt
                        val timeB = b.createdAt
                        if (timeA != null && timeB != null) {
                            timeA.compareTo(timeB)
                        } else {
                            a.name.compareTo(b.name, ignoreCase = true)
                        }
                    }
                }
            }
        }
    }
}
