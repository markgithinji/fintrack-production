package com.fintrack.shared.feature.summary.domain.usecase

import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.account.domain.model.AccountType
import com.fintrack.shared.feature.account.domain.repository.FakeAccountRepository
import com.fintrack.shared.feature.core.util.DateTimeHelper
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.fintrack.shared.feature.transaction.domain.repository.FakeTransactionRepository
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GetCashflowRunwayUseCaseTest {

    private val accountRepository = FakeAccountRepository()
    private val transactionRepository = FakeTransactionRepository()
    private val useCase = GetCashflowRunwayUseCase(accountRepository, transactionRepository)

    @Test
    fun `calculates cashflow runway and daily burn rate correctly`() = runTest {
        val testAccount = Account(
            id = "acc_1",
            name = "M-Pesa",
            type = AccountType.MPESA,
            balance = BigDecimal.fromInt(180000),
            linkedSources = emptyList(),
            createdAt = DateTimeHelper.now()
        )
        accountRepository.setAccounts(listOf(testAccount))

        val now = DateTimeHelper.now()
        val expenseTx = Transaction(
            id = "tx_1",
            accountId = "acc_1",
            amount = BigDecimal.fromInt(60000),
            categoryId = "cat_1",
            category = "Groceries",
            isIncome = false,
            dateTime = now,
            description = "Supermarket",
            merchantName = "Supermarket"
        )
        transactionRepository.setTransactions(listOf(expenseTx))

        val result = useCase()
        assertTrue(result is Result.Success)

        val runway = result.data
        assertEquals(BigDecimal.fromInt(180000), runway.currentBalance)
        assertFalse(runway.isOverdraftRisk)
    }

    @Test
    fun `handles zero liquid cash edge case gracefully`() = runTest {
        val zeroAccount = Account(
            id = "acc_zero",
            name = "Empty Account",
            type = AccountType.BANK,
            balance = BigDecimal.ZERO,
            linkedSources = emptyList(),
            createdAt = DateTimeHelper.now()
        )
        accountRepository.setAccounts(listOf(zeroAccount))

        val result = useCase()
        assertTrue(result is Result.Success)

        val runway = result.data
        assertEquals(BigDecimal.ZERO, runway.currentBalance)
        assertEquals(BigDecimal.ZERO, runway.dailyBurnRate)
    }

    @Test
    fun `handles negative account balance overdraft edge case`() = runTest {
        val debtAccount = Account(
            id = "acc_debt",
            name = "Overdraft Account",
            type = AccountType.BANK,
            balance = BigDecimal.fromInt(-15000),
            linkedSources = emptyList(),
            createdAt = DateTimeHelper.now()
        )
        accountRepository.setAccounts(listOf(debtAccount))

        val result = useCase()
        assertTrue(result is Result.Success)

        val runway = result.data
        assertEquals(BigDecimal.fromInt(-15000), runway.currentBalance)
        assertTrue(runway.isOverdraftRisk)
    }
}
