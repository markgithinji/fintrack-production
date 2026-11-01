package com.fintrack.shared.feature.budget.domain.usecase

import com.fintrack.shared.feature.budget.domain.model.SavingsGoalWithCalculation
import com.fintrack.shared.feature.budget.domain.repository.SavingsGoalRepository
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.transaction.domain.repository.TransactionRepository
import kotlin.time.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

class GetSavingsGoalsUseCase(
    private val savingsGoalRepository: SavingsGoalRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: com.fintrack.shared.feature.account.domain.repository.AccountRepository? = null
) {
    suspend operator fun invoke(userId: String = "local_user"): Result<List<SavingsGoalWithCalculation>> {
        return when (val goalsResult = savingsGoalRepository.getSavingsGoals(userId)) {
            is Result.Error -> Result.Error(goalsResult.exception)
            is Result.Loading -> Result.Loading
            is Result.Success -> {
                val goals = goalsResult.data
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

                // Fetch accounts for account balances & diagnostic logs
                val accountsMap = if (accountRepository != null) {
                    when (val accRes = accountRepository.getAccounts()) {
                        is Result.Success -> accRes.data.associateBy { it.id }
                        else -> emptyMap()
                    }
                } else emptyMap()

                val mpesaAcc = accountsMap.values.find {
                    it.type == com.fintrack.shared.feature.account.domain.model.AccountType.MPESA ||
                            it.name.contains("mpesa", ignoreCase = true)
                }

                println("[SAVINGS_GOAL_CALC_LOG] ==================== SAVINGS GOAL CALCULATION START ====================")
                println("[SAVINGS_GOAL_CALC_LOG] Current System Date: $now")
                println("[SAVINGS_GOAL_CALC_LOG] M-Pesa Account Live Balance: KES ${mpesaAcc?.balance?.doubleValue(false) ?: 0.0}")
                println("[SAVINGS_GOAL_CALC_LOG] Total Accounts Live Cash: KES ${accountsMap.values.sumOf { it.balance?.doubleValue(false) ?: 0.0 }}")

                val calculatedGoals = goals.map { rawGoal ->
                    // Auto-sync current saved amount with linked account balance if applicable
                    val linkedAccount = rawGoal.accountId?.let { idOrName ->
                        accountsMap[idOrName] ?: accountsMap.values.find { acc ->
                            acc.id.equals(idOrName, ignoreCase = true) ||
                                    acc.name.equals(idOrName, ignoreCase = true) ||
                                    (idOrName.contains("mpesa", ignoreCase = true) && (acc.type == com.fintrack.shared.feature.account.domain.model.AccountType.MPESA || acc.name.contains("mpesa", ignoreCase = true)))
                        }
                    }

                    val effectiveSavedAmount = if (linkedAccount?.balance != null && rawGoal.currentAmount == 0.0) {
                        linkedAccount.balance.doubleValue(false)
                    } else {
                        rawGoal.currentAmount
                    }
                    val goal = rawGoal.copy(currentAmount = effectiveSavedAmount, accountId = linkedAccount?.id ?: rawGoal.accountId)

                    // Calculate surplus strictly for goal's linked account (or all accounts if null)
                    val goalSurplus = calculateMonthlySurplus(linkedAccount?.id ?: goal.accountId)

                    val monthsLeft = calculateRemainingMonths(now, goal.targetDate)
                    val requiredMonthly = if (monthsLeft > 0 && goal.remainingAmount > 0) {
                        goal.remainingAmount / monthsLeft
                    } else if (goal.remainingAmount > 0) {
                        goal.remainingAmount
                    } else {
                        0.0
                    }

                    val createdDate = try {
                        LocalDate.parse(goal.createdAt.substringBefore("T"))
                    } catch (_: Exception) {
                        now
                    }

                    val totalMonths = calculateRemainingMonths(createdDate, goal.targetDate).coerceAtLeast(1)
                    val monthsPassed = (totalMonths - monthsLeft).coerceAtLeast(0)

                    val timeElapsedRatio = if (totalMonths > 0) (monthsPassed.toDouble() / totalMonths.toDouble()).coerceIn(0.0, 1.0) else 0.0
                    val actualProgressRatio = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).coerceIn(0.0, 1.0) else 0.0

                    val isTimelineOnPace = goal.isCompleted || monthsPassed == 0 || actualProgressRatio >= timeElapsedRatio
                    val isCashflowSufficient = goal.isCompleted || requiredMonthly <= 0.0 || (goalSurplus > 0.0 && requiredMonthly <= goalSurplus)
                    val isOnTrack = goal.isCompleted || (isTimelineOnPace && isCashflowSufficient)

                    val recommendedTargetDate = if (!isCashflowSufficient && goalSurplus > 0.0 && goal.remainingAmount > 0) {
                        val neededMonths = kotlin.math.ceil(goal.remainingAmount / goalSurplus).toInt().coerceAtLeast(1)
                        now.plus(DatePeriod(months = neededMonths)).toString()
                    } else null

                    val statusReasonText = when {
                        goal.isCompleted -> "Target reached 100%!"
                        !isCashflowSufficient -> if (goalSurplus <= 0) "Requires KES ${requiredMonthly.toLong()}/mo, exceeding monthly cashflow" else "Requires KES ${requiredMonthly.toLong()}/mo vs KES ${goalSurplus.toLong()}/mo surplus"
                        !isTimelineOnPace -> "Saved ${(actualProgressRatio * 100).toInt()}% vs ${(timeElapsedRatio * 100).toInt()}% time passed"
                        else -> "Saved ${(actualProgressRatio * 100).toInt()}% vs ${(timeElapsedRatio * 100).toInt()}% time passed"
                    }

                    SavingsGoalWithCalculation(
                        goal = goal,
                        requiredMonthlyContribution = requiredMonthly,
                        remainingMonths = monthsLeft,
                        isFeasible = isOnTrack,
                        monthlySurplus = goalSurplus,
                        uncommittedMonthlyCashflow = goalSurplus,
                        recommendedMonthsNeeded = if (!isCashflowSufficient && goalSurplus > 0.0 && goal.remainingAmount > 0) kotlin.math.ceil(goal.remainingAmount / goalSurplus).toInt().coerceAtLeast(1) else null,
                        recommendedTargetDate = recommendedTargetDate,
                        isCashflowSufficient = isCashflowSufficient,
                        statusReason = statusReasonText
                    )
                }

                Result.Success(calculatedGoals)
            }
        }
    }

    private suspend fun calculateMonthlySurplus(targetAccountId: String?): Double {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val thirtyDaysAgo = today.minus(DatePeriod(days = 30)).toString()
        val todayStr = today.toString()

        val txResult = transactionRepository.getAllTransactions(
            startDate = thirtyDaysAgo,
            endDate = todayStr,
            accountId = targetAccountId
        )
        val txs = (txResult as? Result.Success)?.data ?: emptyList()

        var totalIncome = 0.0
        var totalExpense = 0.0

        txs.forEach { tx ->
            val amt = tx.amount.doubleValue(false)
            if (tx.isIncome) {
                totalIncome += amt
            } else {
                totalExpense += amt
            }
        }

        return (totalIncome - totalExpense).coerceAtLeast(0.0)
    }

    private fun calculateRemainingMonths(fromDate: LocalDate, targetDateStr: String): Int {
        return try {
            val targetDate = LocalDate.parse(targetDateStr)
            val yearsDiff = targetDate.year - fromDate.year
            val monthsDiff = targetDate.month.number - fromDate.month.number
            val totalMonths = (yearsDiff * 12) + monthsDiff
            if (targetDate.dayOfMonth < fromDate.dayOfMonth && totalMonths > 0) {
                (totalMonths - 1).coerceAtLeast(1)
            } else {
                totalMonths.coerceAtLeast(0)
            }
        } catch (_: Exception) {
            1
        }
    }
}
