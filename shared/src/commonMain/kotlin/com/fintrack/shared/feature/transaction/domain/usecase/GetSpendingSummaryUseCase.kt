package com.fintrack.shared.feature.transaction.domain.usecase

import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.transaction.domain.repository.TransactionRepository
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class GetSpendingSummaryUseCase(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(period: SummaryPeriod): Result<BigDecimal> {
        val timeZone = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(timeZone).date
        
        val (startDate, endDate, limit) = when (period) {
            SummaryPeriod.YESTERDAY -> {
                val yesterday = today.minus(1, DateTimeUnit.DAY).toString()
                Triple(yesterday, yesterday, 100)
            }
            SummaryPeriod.LAST_WEEK -> {
                val lastWeekStart = today.minus(7, DateTimeUnit.DAY).toString()
                val yesterday = today.minus(1, DateTimeUnit.DAY).toString()
                Triple(lastWeekStart, yesterday, 500)
            }
        }
        
        val result = transactionRepository.getTransactions(
            limit = limit,
            sortBy = "date",
            order = "DESC",
            startDate = startDate,
            endDate = endDate,
            isIncome = false
        )
        
        return when (result) {
            is Result.Success -> Result.Success(
                result.data.first.fold(BigDecimal.ZERO) { acc, transaction -> acc + transaction.amount }
            )
            is Result.Error -> Result.Error(result.exception)
            is Result.Loading -> Result.Loading
        }
    }
}

enum class SummaryPeriod {
    YESTERDAY,
    LAST_WEEK
}
