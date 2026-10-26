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

                    // Goal is On Track ONLY if both timeline pace AND cashflow capacity are sufficient
                    val isOnTrack = goal.isCompleted || (isTimelineOnPace && isCashflowSufficient)

                    val (recMonths, recDateStr) = if (!isCashflowSufficient && goalSurplus > 0.0 && goal.remainingAmount > 0) {
                        val monthsNeeded = kotlin.math.ceil(goal.remainingAmount / goalSurplus).toInt().coerceAtLeast(1)
                        val futureDate = now.plus(DatePeriod(months = monthsNeeded))
                        monthsNeeded to futureDate.toString()
                    } else {
                        null to null
                    }

                    val statusReasonText = when {
                        goal.isCompleted -> "Target reached 100%!"
                        !isCashflowSufficient -> if (goalSurplus <= 0) "Requires KES ${requiredMonthly.toLong()}/mo, exceeding monthly cashflow" else "Requires KES ${requiredMonthly.toLong()}/mo vs KES ${goalSurplus.toLong()}/mo surplus"
                        !isTimelineOnPace -> "Saved ${(actualProgressRatio * 100).toInt()}% vs ${(timeElapsedRatio * 100).toInt()}% time passed"
                        else -> "Saved ${(actualProgressRatio * 100).toInt()}% vs ${(timeElapsedRatio * 100).toInt()}% time passed"
                    }

                    println("[SAVINGS_GOAL_CALC_LOG] Goal Name: '${goal.name}' | Linked Account: ${linkedAccount?.name ?: "All Accounts"} | Target: KES ${goal.targetAmount} | Saved: KES ${goal.currentAmount} | Remaining: KES ${goal.remainingAmount}")
                    println("[SAVINGS_GOAL_CALC_LOG]   Target Date: ${goal.targetDate} | Months Left: $monthsLeft | Required Monthly: KES $requiredMonthly/mo")
                    println("[SAVINGS_GOAL_CALC_LOG]   Time Elapsed: $monthsPassed / $totalMonths months (${(timeElapsedRatio * 100).toInt()}%) | Actual Progress: ${(actualProgressRatio * 100).toInt()}% | isOnTrack: $isOnTrack | Reason: $statusReasonText")
                    println("[SAVINGS_GOAL_CALC_LOG]   Account Cashflow Surplus: KES $goalSurplus/mo | isCashflowSufficient: $isCashflowSufficient | Rec Months: $recMonths | Rec Date: $recDateStr")

                    SavingsGoalWithCalculation(
                        goal = goal,
                        requiredMonthlyContribution = requiredMonthly,
                        remainingMonths = monthsLeft,
                        isFeasible = isOnTrack,
                        monthlySurplus = goalSurplus,
                        uncommittedMonthlyCashflow = goalSurplus,
                        recommendedMonthsNeeded = recMonths,
                        recommendedTargetDate = recDateStr,
                        isCashflowSufficient = isCashflowSufficient,
                        statusReason = statusReasonText
                    )
                }
                println("[SAVINGS_GOAL_CALC_LOG] ==================== SAVINGS GOAL CALCULATION END ====================")

                Result.Success(calculatedGoals)
            }
        }
    }

    private suspend fun calculateMonthlySurplus(accountId: String? = null): Double {
        when (val res = transactionRepository.getAllTransactions()) {
            is Result.Success -> {
                val transactions = res.data
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                val sixMonthsAgo = Clock.System.now().minus(180, kotlinx.datetime.DateTimeUnit.DAY, TimeZone.currentSystemDefault())

                val targetAccountIds = accountId?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()

                val filteredTxs = transactions.filter { tx ->
                    val isWithinTime = tx.dateTime >= sixMonthsAgo
                    val matchesAccount = targetAccountIds.isEmpty() || targetAccountIds.contains(tx.accountId)
                    val isNotTransfer = tx.category == null || !tx.category.contains("transfer", ignoreCase = true)
                    isWithinTime && matchesAccount && isNotTransfer
                }

                if (filteredTxs.isNotEmpty()) {
                    val oldestTxDate = filteredTxs.minOf { it.dateTime }.toLocalDateTime(TimeZone.currentSystemDefault()).date
                    val daysSpan = (now.toEpochDays() - oldestTxDate.toEpochDays()).coerceAtLeast(30)
                    val monthsSpan = (daysSpan / 30.0).coerceAtLeast(1.0)

                    val totalIncome = filteredTxs.filter { it.isIncome }.sumOf { it.amount.doubleValue(false) }
                    val totalExpense = filteredTxs.filter { !it.isIncome }.sumOf { it.amount.doubleValue(false) }

                    val avgMonthlyIncome = totalIncome / monthsSpan
                    val avgMonthlyExpense = totalExpense / monthsSpan
                    val netMonthlySurplus = avgMonthlyIncome - avgMonthlyExpense

                    println("[SAVINGS_GOAL_CALC_LOG] 6-Month Analysis for Account [${accountId ?: "All Accounts"}] (${monthsSpan.toInt()} mo span, ${filteredTxs.size} txs):")
                    println("[SAVINGS_GOAL_CALC_LOG]   Income: Total KES $totalIncome (Avg: KES $avgMonthlyIncome/mo)")
                    println("[SAVINGS_GOAL_CALC_LOG]   Expense: Total KES $totalExpense (Avg: KES $avgMonthlyExpense/mo)")
                    println("[SAVINGS_GOAL_CALC_LOG]   Net Surplus: KES $netMonthlySurplus/mo")

                    return netMonthlySurplus
                }
            }
            else -> {}
        }

        if (accountRepository != null) {
            when (val accRes = accountRepository.getAccounts()) {
                is Result.Success -> {
                    val accounts = accRes.data
                    val totalAccountCash = accounts.sumOf { (it.balance?.doubleValue(false) ?: 0.0) }
                    println("[SAVINGS_GOAL_CALC_LOG] Fallback Checking Account Balances Total: KES $totalAccountCash")
                    if (totalAccountCash > 0) {
                        return totalAccountCash
                    }
                }
                else -> {}
            }
        }

        return 0.0
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
