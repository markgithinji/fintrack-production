package com.fintrack.shared.feature.transaction.domain.usecase

import com.fintrack.shared.feature.core.util.FakeFileSaver
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.settings.domain.model.ExportFormat
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.fintrack.shared.feature.transaction.domain.repository.FakeTransactionRepository
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertTrue

class ExportTransactionsUseCaseTest {

    private val fakeRepo = FakeTransactionRepository()
    private val fakeFileSaver = FakeFileSaver()
    private val exportUseCase = ExportTransactionsUseCase(fakeRepo, fakeFileSaver)

    @Test
    fun `exports transactions successfully as CSV`() = runTest {
        val transactions = listOf(
            Transaction(
                id = "1",
                accountId = "acc_1",
                isIncome = false,
                amount = 500.0.toBigDecimal(),
                transactionCost = 10.0.toBigDecimal(),
                category = "Food",
                categoryId = "cat_1",
                dateTime = Instant.fromEpochMilliseconds(0),
                description = "Lunch"
            )
        )
        fakeRepo.setTransactions(transactions)

        val result = exportUseCase(format = ExportFormat.CSV)
        val errorMsg = (result as? Result.Error)?.exception?.message ?: "none"
        assertTrue(result is Result.Success, "Export failed with error: $errorMsg")
        
        val path = result.data
        assertTrue(path.contains("fintrack_export_"))
        
        assertTrue(fakeFileSaver.savedFiles.isNotEmpty(), "savedFiles should not be empty")
        
        val csvContent = fakeFileSaver.savedFiles.values.first()
        assertTrue(csvContent.contains("Date,Category,Amount,Transaction Fees,Total,Type,Account,Description"))
        assertTrue(csvContent.contains("Lunch"))
        assertTrue(csvContent.contains("Food"))
    }
}
