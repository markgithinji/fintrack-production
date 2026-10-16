package com.fintrack.shared.feature.summary.domain.usecase

import com.fintrack.shared.feature.account.domain.repository.AccountRepository
import com.fintrack.shared.feature.core.util.DateTimeHelper
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.summary.domain.model.CashflowRunway
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.fintrack.shared.feature.transaction.domain.repository.TransactionRepository
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.DecimalMode
import com.ionspin.kotlin.bignum.decimal.RoundingMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

class GetCashflowRunwayUseCase(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) {
    private val decimalMode = DecimalMode(decimalPrecision = 20, roundingMode = RoundingMode.ROUND_HALF_CEILING)

    suspend operator fun invoke(accountId: String? = null): Result<CashflowRunway> = withContext(Dispatchers.IO) {
        val nowInstant = DateTimeHelper.now()
        val today = nowInstant.toLocalDateTime(TimeZone.currentSystemDefault()).date

        // 1. Current Total Balance
        val accountsResult = accountRepository.getAccounts()
        val accounts = (accountsResult as? Result.Success)?.data ?: emptyList()
        val targetAccounts = if (accountId != null) accounts.filter { it.id == accountId } else accounts
        val currentBalance = targetAccounts.fold(BigDecimal.ZERO) { acc, a -> acc + (a.balance ?: BigDecimal.ZERO) }

        // 2. Query All Transactions ONCE for Account & Filter in Memory (0ms Disk Read)
        val allTxResult = transactionRepository.getAllTransactions(startDate = null, endDate = null, accountId = accountId)
        val allTx = (allTxResult as? Result.Success)?.data ?: emptyList()

        val thirtyDaysAgo = today.minus(DatePeriod(days = 30)).toString()
        val todayStr = today.toString()

        val past30Expenses = allTx.filter { tx ->
            if (!isRealVariableExpense(tx)) return@filter false
            val txDateStr = try {
                tx.dateTime.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
            } catch (_: Exception) { "" }
            txDateStr >= thirtyDaysAgo && txDateStr <= todayStr
        }

        val past30DaysExpensesTotal = past30Expenses.fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }

        // Accuracy Check: If no recent variable expenses in past 30 days, fallback to historical burn rate
        val dailyBurnRate = if (past30DaysExpensesTotal > BigDecimal.ZERO) {
            past30DaysExpensesTotal.divide(BigDecimal.fromInt(30), decimalMode)
        } else {
            val allExpenses = allTx.filter { isRealVariableExpense(it) }
            val totalExpense = allExpenses.fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }

            if (totalExpense > BigDecimal.ZERO && allExpenses.isNotEmpty()) {
                val earliestDate = allExpenses.minOf { it.dateTime.toLocalDateTime(TimeZone.currentSystemDefault()).date }
                val daysSpan = (today.toEpochDays() - earliestDate.toEpochDays()).coerceAtLeast(1)
                totalExpense.divide(BigDecimal.fromLong(daysSpan), decimalMode)
            } else {
                BigDecimal.ZERO
            }
        }

        // 3. Days Remaining in Current Calendar Month
        val nextMonthFirstDay = if (today.monthNumber == 12) {
            kotlinx.datetime.LocalDate(today.year + 1, 1, 1)
        } else {
            kotlinx.datetime.LocalDate(today.year, today.monthNumber + 1, 1)
        }
        val lastDayOfThisMonth = nextMonthFirstDay.minus(DatePeriod(days = 1))
        val daysInMonth = lastDayOfThisMonth.day
        val remainingDaysInMonth = (daysInMonth - today.day + 1).coerceAtLeast(1)

        val projectedVariableExpense = dailyBurnRate.multiply(BigDecimal.fromInt(remainingDaysInMonth))

        // 4. Upcoming Bills Due Before Month End
        val billsResult = transactionRepository.getRecurringBills()
        val recurringBills = (billsResult as? Result.Success)?.data ?: emptyList()
        val upcomingBillsTotal = recurringBills
            .filter { it.isActive && it.nextDueDate <= lastDayOfThisMonth }
            .fold(BigDecimal.ZERO) { acc, b -> acc + b.amount }

        // 5. Projected End-of-Month Balance
        val totalProjectedDrain = projectedVariableExpense + upcomingBillsTotal
        val projectedEomBalance = currentBalance - totalProjectedDrain
        val isOverdraftRisk = projectedEomBalance < BigDecimal.ZERO

        // 6. Estimated Depletion Days
        val estimatedDepletionDays = if (dailyBurnRate > BigDecimal.ZERO && currentBalance > BigDecimal.ZERO) {
            val days = currentBalance.divide(dailyBurnRate, decimalMode).intValue(false)
            if (days <= remainingDaysInMonth) days else null
        } else if (currentBalance <= BigDecimal.ZERO) {
            0
        } else {
            null
        }

        Result.Success(
            CashflowRunway(
                currentBalance = currentBalance,
                dailyBurnRate = dailyBurnRate,
                remainingDaysInMonth = remainingDaysInMonth,
                projectedVariableExpense = projectedVariableExpense,
                upcomingBillsTotal = upcomingBillsTotal,
                projectedEomBalance = projectedEomBalance,
                isOverdraftRisk = isOverdraftRisk,
                estimatedDepletionDays = estimatedDepletionDays
            )
        )
    }

    private fun isRealVariableExpense(tx: Transaction): Boolean {
        if (tx.isIncome) return false

        val categoryName = tx.category?.lowercase() ?: ""
        val description = tx.description?.lowercase() ?: ""

        // Exclude reversals and internal self-transfers between user's own linked accounts
        if (categoryName.contains("reversal") || description.contains("reversal of")) return false
        if (description.contains("transfer to own account") || description.contains("internal transfer")) return false

        return true
    }
}
