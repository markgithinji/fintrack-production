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
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GetNetWorthSummaryUseCaseTest {

    private val accountRepository = FakeAccountRepository()
    private val transactionRepository = FakeTransactionRepository()
    private val useCase = GetNetWorthSummaryUseCase(accountRepository, transactionRepository)

    @Test
    fun `calculates total assets liabilities and net worth correctly`() = runTest {
        val testAccount1 = Account(
            id = "acc_1",
            name = "M-Pesa",
            type = AccountType.MPESA,
            balance = BigDecimal.fromInt(500000),
            linkedSources = emptyList(),
            createdAt = DateTimeHelper.now()
        )
        val testAccount2 = Account(
            id = "acc_2",
            name = "Fuliza Overdraft",
            type = AccountType.MPESA,
            balance = BigDecimal.fromInt(-20000),
            linkedSources = emptyList(),
            createdAt = DateTimeHelper.now()
        )
        accountRepository.setAccounts(listOf(testAccount1, testAccount2))

        val result = useCase()
        assertTrue(result is Result.Success)

        val summary = result.data
        assertEquals(BigDecimal.fromInt(500000), summary.totalAssets)
        assertEquals(BigDecimal.fromInt(20000), summary.totalLiabilities)
        assertEquals(BigDecimal.fromInt(480000), summary.netWorth)
        assertEquals("Healthy", summary.financialHealthStatus)
    }

    @Test
    fun `generates 36 historical trend points for 3 year net worth trend`() = runTest {
        val testAccount = Account(
            id = "acc_1",
            name = "Main Account",
            type = AccountType.BANK,
            balance = BigDecimal.fromInt(100000),
            linkedSources = emptyList(),
            createdAt = DateTimeHelper.now()
        )
        accountRepository.setAccounts(listOf(testAccount))

        val result = useCase()
        assertTrue(result is Result.Success)

        val summary = result.data
        assertEquals(36, summary.historicalTrendPoints.size)
    }

    @Test
    fun `preserves negative net worth in summary and trend points`() = runTest {
        val testAccount = Account(
            id = "acc_debt",
            name = "Bank Loan",
            type = AccountType.BANK,
            balance = BigDecimal.fromInt(-63114),
            linkedSources = emptyList(),
            createdAt = DateTimeHelper.now()
        )
        accountRepository.setAccounts(listOf(testAccount))

        val result = useCase()
        assertTrue(result is Result.Success)

        val summary = result.data
        assertEquals(BigDecimal.fromInt(-63114), summary.netWorth)
        assertEquals(36, summary.historicalTrendPoints.size)
        assertEquals(-63114.0, summary.historicalTrendPoints.last())
    }
}
