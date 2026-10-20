package com.fintrack.shared.feature.summary.domain.usecase

import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.account.domain.model.AccountType
import com.fintrack.shared.feature.account.domain.repository.FakeAccountRepository
import com.fintrack.shared.feature.core.util.DateTimeHelper
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.transaction.domain.repository.FakeTransactionRepository
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GetEmergencyFundScoreUseCaseTest {

    private val accountRepository = FakeAccountRepository()
    private val transactionRepository = FakeTransactionRepository()
    private val useCase = GetEmergencyFundScoreUseCase(accountRepository, transactionRepository)

    @Test
    fun `calculates emergency fund readiness score correctly`() = runTest {
        val testAccount = Account(
            id = "acc_1",
            name = "M-Pesa",
            type = AccountType.MPESA,
            balance = BigDecimal.fromInt(90000),
            linkedSources = emptyList(),
            createdAt = DateTimeHelper.now()
        )
        accountRepository.setAccounts(listOf(testAccount))

        val result = useCase()
        assertTrue(result is Result.Success)
        val score = result.data
        assertTrue(score.monthsCovered.doubleValue(false) > 0.0)
    }
}
