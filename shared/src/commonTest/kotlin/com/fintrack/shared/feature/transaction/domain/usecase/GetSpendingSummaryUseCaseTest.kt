package com.fintrack.shared.feature.transaction.domain.usecase

import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.fintrack.shared.feature.transaction.domain.repository.FakeTransactionRepository
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetSpendingSummaryUseCaseTest {

    private val fakeRepository = FakeTransactionRepository()
    private val useCase = GetSpendingSummaryUseCase(fakeRepository)

    @Test
    fun `calculates spending summary correctly for expense transactions`() = runTest {
        val transactions = listOf(
            Transaction(
                id = "1",
                accountId = "acc_1",
                isIncome = false,
                amount = 100.0.toBigDecimal(),
                categoryId = "cat_1",
                dateTime = Instant.fromEpochMilliseconds(0),
                description = "Groceries"
            ),
            Transaction(
                id = "2",
                accountId = "acc_1",
                isIncome = false,
                amount = 250.0.toBigDecimal(),
                categoryId = "cat_1",
                dateTime = Instant.fromEpochMilliseconds(0),
                description = "Dinner"
            ),
            Transaction(
                id = "3",
                accountId = "acc_1",
                isIncome = true, // Should be filtered out by isIncome = false in use case
                amount = 1000.0.toBigDecimal(),
                categoryId = "cat_2",
                dateTime = Instant.fromEpochMilliseconds(0),
                description = "Salary"
            )
        )

        fakeRepository.setTransactions(transactions)

        val result = useCase(SummaryPeriod.YESTERDAY)
        assertTrue(result is Result.Success)
        // 100 + 250 = 350
        assertEquals(350.0.toBigDecimal(), result.data)
    }
}
