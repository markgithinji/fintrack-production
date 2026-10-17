package com.fintrack.shared.feature.budget.domain.usecase

import com.fintrack.shared.feature.budget.domain.model.SavingsGoalWithCalculation
import com.fintrack.shared.feature.budget.domain.repository.SavingsGoalRepository
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.transaction.domain.repository.TransactionRepository
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

class GetSavingsGoalsUseCase(
    private val savingsGoalRepository: SavingsGoalRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(userId: String = "local_user"): Result<List<SavingsGoalWithCalculation>> {
        return when (val goalsResult = savingsGoalRepository.getSavingsGoals(userId)) {
            is Result.Error -> Result.Error(goalsResult.exception)
            is Result.Loading -> Result.Loading
            is Result.Success -> {
                val goals = goalsResult.data
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

                // Fetch monthly net surplus (Income - Expense)
                val surplus = calculateMonthlySurplus()

                val calculatedGoals = goals.map { goal ->
                    val monthsLeft = calculateRemainingMonths(now, goal.targetDate)
                    val requiredMonthly = if (monthsLeft > 0 && goal.remainingAmount > 0) {
                        goal.remainingAmount / monthsLeft
                    } else if (goal.remainingAmount > 0) {
                        goal.remainingAmount
                    } else {
                        0.0
                    }

                    val isFeasible = goal.isCompleted || requiredMonthly <= 0.0 || (surplus > 0 && requiredMonthly <= surplus)

                    SavingsGoalWithCalculation(
                        goal = goal,
                        requiredMonthlyContribution = requiredMonthly,
                        remainingMonths = monthsLeft,
                        isFeasible = isFeasible,
                        monthlySurplus = surplus
                    )
                }

                Result.Success(calculatedGoals)
            }
        }
    }

    private suspend fun calculateMonthlySurplus(): Double {
        return when (val res = transactionRepository.getAllTransactions()) {
            is Result.Success -> {
                val transactions = res.data
                val totalIncome = transactions.filter { it.isIncome }.sumOf { it.amount.doubleValue(false) }
                val totalExpense = transactions.filter { !it.isIncome }.sumOf { it.amount.doubleValue(false) }
                (totalIncome - totalExpense).coerceAtLeast(0.0)
            }
            else -> 0.0
        }
    }

    private fun calculateRemainingMonths(currentDate: LocalDate, targetDateString: String): Int {
        return try {
            val targetDate = LocalDate.parse(targetDateString)
            val yearsDiff = targetDate.year - currentDate.year
            val monthsDiff = targetDate.month.number - currentDate.month.number
            val totalMonths = yearsDiff * 12 + monthsDiff
            totalMonths.coerceAtLeast(1)
        } catch (e: Exception) {
            1
        }
    }
}
