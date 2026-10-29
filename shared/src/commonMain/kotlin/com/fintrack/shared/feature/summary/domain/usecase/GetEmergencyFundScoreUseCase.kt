package com.fintrack.shared.feature.summary.domain.usecase

import com.fintrack.shared.feature.account.domain.repository.AccountRepository
import com.fintrack.shared.feature.category.domain.model.*
import com.fintrack.shared.feature.core.util.DateTimeHelper
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.summary.domain.model.EmergencyFundScore
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

class GetEmergencyFundScoreUseCase(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val savingsGoalRepository: com.fintrack.shared.feature.budget.domain.repository.SavingsGoalRepository? = null
) {
    private val decimalMode = DecimalMode(decimalPrecision = 20, roundingMode = RoundingMode.ROUND_HALF_CEILING)

    suspend operator fun invoke(accountId: String? = null): Result<EmergencyFundScore> = withContext(Dispatchers.IO) {
        try {
            val targetAccountIds = accountId?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()

            // 1. Calculate Total Liquid Assets (Accounts Balances + Savings Goal Vaults)
            val accountsResult = accountRepository.getAccounts()
            val accounts = (accountsResult as? Result.Success)?.data ?: emptyList()
            val targetAccounts = if (targetAccountIds.isNotEmpty()) {
                accounts.filter { targetAccountIds.contains(it.id) }
            } else accounts

            val accountsBalance = targetAccounts.fold(BigDecimal.ZERO) { sum, acc ->
                val bal = acc.balance ?: BigDecimal.ZERO
                if (bal > BigDecimal.ZERO) sum + bal else sum
            }

            val goalsResult = savingsGoalRepository?.getSavingsGoals()
            val savingsGoals = (goalsResult as? Result.Success)?.data ?: emptyList()
            val vaultsBalance = savingsGoals.fold(BigDecimal.ZERO) { sum, goal ->
                val amount = BigDecimal.fromDouble(goal.currentAmount)
                if (amount > BigDecimal.ZERO) sum + amount else sum
            }

            val totalLiquidAssets = accountsBalance + vaultsBalance

            // 2. Calculate Monthly Essential Expenses (Strictly Past 90 Days)
            val nowInstant = DateTimeHelper.now()
            val today = nowInstant.toLocalDateTime(TimeZone.currentSystemDefault()).date
            val ninetyDaysAgoDate = today.minus(DatePeriod(days = 90))
            val ninetyDaysAgoStr = ninetyDaysAgoDate.toString()
            val todayStr = today.toString()

            val singleAccountId = if (targetAccountIds.size == 1) targetAccountIds.first() else null
            val txResult = transactionRepository.getAllTransactions(startDate = ninetyDaysAgoStr, endDate = todayStr, accountId = singleAccountId)
            val allTx = (txResult as? Result.Success)?.data ?: emptyList()

            // Strictly filter in memory to ONLY past 90 days & target account
            val last90DaysTx = allTx.filter { tx ->
                val matchesAccount = if (targetAccountIds.isNotEmpty()) targetAccountIds.contains(tx.accountId) else true
                val txDate = try {
                    tx.dateTime.toLocalDateTime(TimeZone.currentSystemDefault()).date
                } catch (_: Exception) { null }
                val matchesDate = if (txDate != null) (txDate >= ninetyDaysAgoDate && txDate <= today) else true
                matchesAccount && matchesDate
            }

            val realExpenses = last90DaysTx.filter { tx ->
                if (tx.isIncome) return@filter false

                // Exclude massive single outlier transactions (> KES 200,000) from living expense baseline
                if (tx.amount.doubleValue(false) > 200000.0) return@filter false

                val catName = tx.category?.lowercase() ?: ""
                val desc = tx.description?.lowercase() ?: ""

                // Exclude reversals, internal self-transfers, savings deposits (M-Shwari), property/asset purchases, and investments
                if (catName.contains("reversal") || desc.contains("reversal of")) return@filter false
                if (catName.contains("savings") || desc.contains("m-shwari") || desc.contains("transferred to m-shwari") || desc.contains("lock savings")) return@filter false
                if (desc.contains("transfer to own account") || desc.contains("internal transfer") || desc.contains("transfer between")) return@filter false
                if (catName.contains("property") || catName.contains("investment") || catName.contains("asset") || desc.contains("property purchase") || desc.contains("land purchase")) return@filter false

                true
            }

            val essentialCategoryIds = setOf(
                Category.Rent.id,
                Category.Groceries.id,
                Category.Utilities.id,
                Category.Health.id,
                Category.Education.id,
                Category.Insurance.id,
                Category.Loans.id,
                Category.Bills.id
            )
            val essentialKeywords = listOf("rent", "food", "groceries", "utilities", "bills", "healthcare", "housing", "home")
            val essentialExpenses = realExpenses.filter { tx ->
                val matchesId = essentialCategoryIds.contains(tx.categoryId)
                val catName = tx.category?.lowercase() ?: ""
                val matchesKeyword = essentialKeywords.any { catName.contains(it) }
                matchesId || matchesKeyword
            }

            val essentialExpensesTotal = essentialExpenses.fold(BigDecimal.ZERO) { sum, tx -> sum + tx.amount }
            val realExpensesTotal = realExpenses.fold(BigDecimal.ZERO) { sum, tx -> sum + tx.amount }

            val monthlyEssentialExpenses = if (essentialExpensesTotal > BigDecimal.ZERO) {
                essentialExpensesTotal.divide(BigDecimal.fromInt(3), decimalMode)
            } else if (realExpensesTotal > BigDecimal.ZERO) {
                realExpensesTotal.divide(BigDecimal.fromInt(3), decimalMode)
            } else BigDecimal.fromInt(15000)

            // 3. Compute Months Covered
            val monthsCovered = if (monthlyEssentialExpenses > BigDecimal.ZERO) {
                totalLiquidAssets.divide(monthlyEssentialExpenses, decimalMode)
            } else BigDecimal.ZERO

            val monthsDouble = monthsCovered.doubleValue(false)
            val readinessStatus = when {
                monthsDouble >= 6.0 -> "Healthy Buffer"
                monthsDouble >= 3.0 -> "Building Shield"
                else -> "Low Shield"
            }

            Result.Success(
                EmergencyFundScore(
                    liquidAssetsTotal = totalLiquidAssets,
                    liquidCashTotal = accountsBalance,
                    savingsVaultsTotal = vaultsBalance,
                    investmentsTotal = BigDecimal.ZERO,
                    monthlyEssentialExpenses = monthlyEssentialExpenses,
                    monthsCovered = monthsCovered,
                    targetMonths = BigDecimal.fromInt(6),
                    readinessStatus = readinessStatus
                )
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
