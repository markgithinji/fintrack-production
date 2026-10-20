package com.fintrack.shared.feature.summary.domain.usecase

import com.fintrack.shared.feature.account.domain.repository.AccountRepository
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.summary.domain.model.AssetLiabilityItem
import com.fintrack.shared.feature.summary.domain.model.NetWorthSummary
import com.fintrack.shared.feature.transaction.domain.repository.TransactionRepository
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.DecimalMode
import com.ionspin.kotlin.bignum.decimal.RoundingMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

class GetNetWorthSummaryUseCase(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val savingsGoalRepository: com.fintrack.shared.feature.budget.domain.repository.SavingsGoalRepository? = null
) {
    private val decimalMode = DecimalMode(decimalPrecision = 20, roundingMode = RoundingMode.ROUND_HALF_CEILING)

    suspend operator fun invoke(accountId: String? = null): Result<NetWorthSummary> = withContext(Dispatchers.IO) {
        try {
            val targetAccountIds = accountId?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()

            // 1. Accounts Fetch
            val accountsResult = accountRepository.getAccounts()
            val accounts = (accountsResult as? Result.Success)?.data ?: emptyList()
            val targetAccounts = if (targetAccountIds.isNotEmpty()) {
                accounts.filter { targetAccountIds.contains(it.id) }
            } else accounts

            // Assets: Positive Account Balances
            val assetItems = mutableListOf<AssetLiabilityItem>()
            val liabilityItems = mutableListOf<AssetLiabilityItem>()

            targetAccounts.forEach { acc ->
                val bal = acc.balance ?: BigDecimal.ZERO
                if (bal >= BigDecimal.ZERO) {
                    assetItems.add(AssetLiabilityItem(acc.id, acc.name, bal, isAsset = true))
                } else {
                    liabilityItems.add(AssetLiabilityItem(acc.id, acc.name, bal.abs(), isAsset = false))
                }
            }

            // 2. Savings Goal Vault Balances (Assets)
            val goalsResult = savingsGoalRepository?.getSavingsGoals()
            val savingsGoals = (goalsResult as? Result.Success)?.data ?: emptyList()

            savingsGoals.forEach { goal ->
                val goalBal = BigDecimal.fromDouble(goal.currentAmount)
                if (goalBal > BigDecimal.ZERO) {
                    assetItems.add(AssetLiabilityItem(goal.id, "${goal.name} (Vault)", goalBal, isAsset = true))
                }
            }

            // 3. Recurring Liabilities & Overdrafts
            val billsResult = transactionRepository.getRecurringBills()
            val recurringBills = (billsResult as? Result.Success)?.data ?: emptyList()

            recurringBills.filter { it.isActive && (it.name.contains("loan", ignoreCase = true) || it.name.contains("fuliza", ignoreCase = true)) }
                .forEach { bill ->
                    liabilityItems.add(AssetLiabilityItem(bill.id, bill.name, bill.amount, isAsset = false))
                }

            val totalAssets = assetItems.fold(BigDecimal.ZERO) { sum, item -> sum + item.amount }
            val totalLiabilities = liabilityItems.fold(BigDecimal.ZERO) { sum, item -> sum + item.amount }
            val netWorth = totalAssets - totalLiabilities

            // Debt-to-Asset Ratio
            val ratio = if (totalAssets > BigDecimal.ZERO) {
                totalLiabilities.divide(totalAssets, decimalMode).multiply(BigDecimal.fromInt(100))
            } else BigDecimal.ZERO

            val healthStatus = when {
                ratio < BigDecimal.fromInt(30) -> "Healthy"
                ratio <= BigDecimal.fromInt(50) -> "Moderate"
                else -> "High Debt"
            }

            val singleAccountId = if (targetAccountIds.size == 1) targetAccountIds.first() else null
            val nowInstant = com.fintrack.shared.feature.core.util.DateTimeHelper.now()
            val today = nowInstant.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
            val thirtyDaysAgo = today.minus(kotlinx.datetime.DatePeriod(days = 30)).toString()
            val todayStr = today.toString()

            val allTxResult = transactionRepository.getAllTransactions(startDate = thirtyDaysAgo, endDate = todayStr, accountId = singleAccountId)
            val rawTx = (allTxResult as? Result.Success)?.data ?: emptyList()
            val filteredTx = if (targetAccountIds.size > 1) {
                rawTx.filter { targetAccountIds.contains(it.accountId) }
            } else rawTx

            var past30dNetIncome = BigDecimal.ZERO
            for (tx in filteredTx) {
                if (tx.isIncome) {
                    past30dNetIncome = past30dNetIncome + tx.amount
                } else {
                    past30dNetIncome = past30dNetIncome - tx.amount
                }
            }

            val previousNetWorth = netWorth - past30dNetIncome
            val growthRatePercentage = if (previousNetWorth > BigDecimal.ZERO && netWorth != previousNetWorth) {
                (netWorth - previousNetWorth).divide(previousNetWorth, decimalMode).multiply(BigDecimal.fromInt(100))
            } else if (netWorth > BigDecimal.ZERO && previousNetWorth <= BigDecimal.ZERO) {
                BigDecimal.fromInt(100)
            } else BigDecimal.ZERO

            Result.Success(
                NetWorthSummary(
                    totalAssets = totalAssets,
                    totalLiabilities = totalLiabilities,
                    netWorth = netWorth,
                    previousNetWorth = previousNetWorth,
                    growthRatePercentage = growthRatePercentage,
                    debtToAssetRatioPercentage = ratio,
                    financialHealthStatus = healthStatus,
                    assetBreakdown = assetItems,
                    liabilityBreakdown = liabilityItems
                )
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
