package com.fintrack.shared.feature.transaction.domain.usecase

import com.fintrack.shared.feature.core.util.FileSaver
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.settings.domain.model.ExportFormat
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.fintrack.shared.feature.transaction.domain.repository.TransactionRepository
import com.fintrack.shared.feature.transaction.domain.util.generatePdfBytes
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlin.time.Clock

class ExportTransactionsUseCase(
    private val transactionRepository: TransactionRepository,
    private val fileSaver: FileSaver,
) {
    private val json = Json { prettyPrint = true }

    suspend operator fun invoke(
        format: ExportFormat = ExportFormat.CSV,
        startDate: String? = null,
        endDate: String? = null
    ): Result<String> {
        val result = transactionRepository.getAllTransactions(
            startDate = startDate,
            endDate = endDate
        )
        if (result is Result.Error) return Result.Error(result.exception)
        
        val transactions = (result as Result.Success).data
        
        val now = Clock.System.now()
        val timestamp = now.toLocalDateTime(TimeZone.currentSystemDefault())
            .toString()
            .replace(":", "-")
            .split(".")[0]
        val fileName = "fintrack_export_$timestamp.${format.extension}"

        val savedPath = when (format) {
            ExportFormat.CSV -> fileSaver.saveFile(fileName, generateCsv(transactions))
            ExportFormat.JSON -> fileSaver.saveFile(fileName, json.encodeToString(transactions))
            ExportFormat.PDF -> fileSaver.saveFileBytes(fileName, generatePdfBytes(transactions))
        }
        
        return if (savedPath != null) {
            Result.Success(savedPath)
        } else {
            Result.Error(Exception("Failed to save export file"))
        }
    }

    private fun generateCsv(transactions: List<Transaction>): String {
        val csv = StringBuilder()
        csv.append("Date,Category,Amount,Transaction Fees,Total,Type,Account,Description\n")
        
        transactions.forEach { transaction ->
            csv.append("${transaction.dateTime},")
            csv.append("${escapeCsv(transaction.category ?: "Uncategorized")},")
            csv.append("${transaction.amount},")
            csv.append("${transaction.transactionCost},")
            csv.append("${transaction.totalAmount},")
            csv.append("${if (transaction.isIncome) "Income" else "Expense"},")
            csv.append("${escapeCsv(transaction.accountId)},")
            csv.append("${escapeCsv(transaction.description ?: "")}\n")
        }
        return csv.toString()
    }

    private fun escapeCsv(value: String): String {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\""
        }
        return value
    }
}
