package com.fintrack.shared.feature.summary.domain.usecase

import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.account.domain.model.AccountType
import com.fintrack.shared.feature.account.domain.repository.FakeAccountRepository
import com.fintrack.shared.feature.core.util.DateTimeHelper
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.summary.domain.model.AffordabilityVerdict
import com.fintrack.shared.feature.transaction.domain.repository.FakeTransactionRepository
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SimulateAffordabilityUseCaseTest {

    private val accountRepository = FakeAccountRepository()
    private val transactionRepository = FakeTransactionRepository()
    private val getCashflowRunwayUseCase = GetCashflowRunwayUseCase(accountRepository, transactionRepository)
    private val useCase = SimulateAffordabilityUseCase(getCashflowRunwayUseCase)

    @Test
    fun `calculates affordability simulation result correctly`() = runTest {
        val testAccount = Account(
            id = "acc_1",
            name = "M-Pesa",
            type = AccountType.MPESA,
            balance = BigDecimal.fromInt(100000),
            linkedSources = emptyList(),
            createdAt = DateTimeHelper.now()
        )
        accountRepository.setAccounts(listOf(testAccount))

        val result = useCase(20000.0)
        assertTrue(result is Result.Success)

        val sim = (result as Result.Success).data
        assertEquals(20000.0, sim.itemPrice)
        assertEquals(100000.0, sim.currentAccountCash)
        assertTrue(sim.verdict == AffordabilityVerdict.SAFE || sim.verdict == AffordabilityVerdict.CAUTION)
    }

    @Test
    fun `returns error for invalid price`() = runTest {
        val result = useCase(-500.0)
        assertTrue(result is Result.Error)
    }

    @Test
    fun `returns IMMEDIATE_DEFICIT when item price exceeds current cash`() = runTest {
        val testAccount = Account(
            id = "acc_1",
            name = "M-Pesa",
            type = AccountType.MPESA,
            balance = BigDecimal.fromInt(15000),
            linkedSources = emptyList(),
            createdAt = DateTimeHelper.now()
        )
        accountRepository.setAccounts(listOf(testAccount))

        val result = useCase(35000.0)
        assertTrue(result is Result.Success)

        val sim = (result as Result.Success).data
        assertEquals(AffordabilityVerdict.IMMEDIATE_DEFICIT, sim.verdict)
    }
}
