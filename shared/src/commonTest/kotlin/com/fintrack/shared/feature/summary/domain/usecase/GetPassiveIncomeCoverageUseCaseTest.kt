package com.fintrack.shared.feature.summary.domain.usecase

import com.fintrack.shared.feature.core.util.DateTimeHelper
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.fintrack.shared.feature.transaction.domain.repository.FakeTransactionRepository
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GetPassiveIncomeCoverageUseCaseTest {

    private val transactionRepository = FakeTransactionRepository()
    private val useCase = GetPassiveIncomeCoverageUseCase(transactionRepository)

    @Test
    fun `calculates passive income coverage percentage and milestone correctly`() = runTest {
        val now = DateTimeHelper.now()
        val dividendTx = Transaction(
            id = "tx_div",
            accountId = "acc_1",
            amount = BigDecimal.fromInt(20000),
            categoryId = "cat_div",
            category = "Dividends",
            isIncome = true,
            dateTime = now,
            description = "Safaricom Dividend",
            merchantName = "Safaricom"
        )
        val rentExpense = Transaction(
            id = "tx_rent",
            accountId = "acc_1",
            amount = BigDecimal.fromInt(40000),
            categoryId = "cat_rent",
            category = "Rent",
            isIncome = false,
            dateTime = now,
            description = "House Rent",
            merchantName = "Landlord"
        )
        transactionRepository.setTransactions(listOf(dividendTx, rentExpense))

        val result = useCase()
        assertTrue(result is Result.Success)

        val coverage = result.data
        assertEquals(20000.0, coverage.totalMonthlyPassiveIncome)
        assertEquals(40000.0, coverage.monthlyEssentialExpenses)
        assertEquals(50.0, coverage.coveragePercentage)
        assertTrue(coverage.milestoneDescription.contains("Halfway"))
    }

    @Test
    fun `handles zero passive income edge case gracefully`() = runTest {
        val result = useCase()
        assertTrue(result is Result.Success)

        val coverage = result.data
        assertEquals(0.0, coverage.totalMonthlyPassiveIncome)
        assertEquals(0.0, coverage.coveragePercentage)
        assertTrue(coverage.milestoneDescription.contains("Log MMFs"))
    }
}
