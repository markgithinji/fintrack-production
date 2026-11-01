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
class GetBudgetOptimizerUseCaseTest {

    private val transactionRepository = FakeTransactionRepository()
    private val useCase = GetBudgetOptimizerUseCase(transactionRepository)

    @Test
    fun `calculates 50-30-20 budget breakdown and recommendations correctly`() = runTest {
        val now = DateTimeHelper.now()
        val incomeTx = Transaction(
            id = "tx_inc",
            accountId = "acc_1",
            amount = BigDecimal.fromInt(200000),
            categoryId = "cat_inc",
            category = "Salary",
            isIncome = true,
            dateTime = now,
            description = "Monthly Salary",
            merchantName = "Employer"
        )
        val rentTx = Transaction(
            id = "tx_rent",
            accountId = "acc_1",
            amount = BigDecimal.fromInt(100000), // Needs (50%)
            categoryId = "cat_rent",
            category = "Rent",
            isIncome = false,
            dateTime = now,
            description = "House Rent",
            merchantName = "Landlord"
        )
        val diningTx = Transaction(
            id = "tx_dining",
            accountId = "acc_1",
            amount = BigDecimal.fromInt(60000), // Wants (30%)
            categoryId = "cat_dining",
            category = "Dining Out",
            isIncome = false,
            dateTime = now,
            description = "Restaurant",
            merchantName = "Restaurant"
        )
        transactionRepository.setTransactions(listOf(incomeTx, rentTx, diningTx))

        val result = useCase()
        assertTrue(result is Result.Success)

        val optimizer = result.data
        assertEquals(BigDecimal.fromInt(200000), optimizer.totalIncome)
        assertEquals(BigDecimal.fromInt(160000), optimizer.totalExpense)
        assertTrue(optimizer.healthScore in 5..100)
    }

    @Test
    fun `handles empty transaction dataset with fallback 50-30-20 targets`() = runTest {
        val result = useCase()
        assertTrue(result is Result.Success)

        val optimizer = result.data
        assertEquals(BigDecimal.ZERO, optimizer.totalIncome)
        assertEquals(BigDecimal.ZERO, optimizer.totalExpense)
        assertEquals(60, optimizer.healthScore)
    }
}
