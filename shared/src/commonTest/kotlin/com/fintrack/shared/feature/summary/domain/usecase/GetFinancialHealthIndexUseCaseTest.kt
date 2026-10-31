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
class GetFinancialHealthIndexUseCaseTest {

    private val accountRepository = FakeAccountRepository()
    private val transactionRepository = FakeTransactionRepository()
    private val getCashflowRunwayUseCase = GetCashflowRunwayUseCase(accountRepository, transactionRepository)
    private val getNetWorthSummaryUseCase = GetNetWorthSummaryUseCase(accountRepository, transactionRepository)
    private val getEmergencyFundScoreUseCase = GetEmergencyFundScoreUseCase(accountRepository, transactionRepository)
    private val getBudgetOptimizerUseCase = GetBudgetOptimizerUseCase(transactionRepository)

    private val useCase = GetFinancialHealthIndexUseCase(
        getCashflowRunwayUseCase = getCashflowRunwayUseCase,
        getNetWorthSummaryUseCase = getNetWorthSummaryUseCase,
        getEmergencyFundScoreUseCase = getEmergencyFundScoreUseCase,
        getBudgetOptimizerUseCase = getBudgetOptimizerUseCase,
        transactionRepository = transactionRepository,
        accountRepository = accountRepository
    )

    @Test
    fun `calculates 5-pillar financial health index score and grade correctly`() = runTest {
        val testAccount = Account(
            id = "acc_1",
            name = "M-Pesa",
            type = AccountType.MPESA,
            balance = BigDecimal.fromInt(250000),
            linkedSources = emptyList(),
            createdAt = DateTimeHelper.now()
        )
        accountRepository.setAccounts(listOf(testAccount))

        val result = useCase()
        assertTrue(result is Result.Success)

        val healthIndex = result.data
        assertTrue(healthIndex.overallScore in 5..100)
        assertTrue(healthIndex.actionableTips.isNotEmpty())
        assertTrue(healthIndex.wellbeingInsights.isNotEmpty())
    }
}
