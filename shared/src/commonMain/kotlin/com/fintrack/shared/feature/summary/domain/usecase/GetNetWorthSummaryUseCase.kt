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

            // 2. Savings Goal Vault Balances (Assets) - Deduplicated against account names
            val goalsResult = savingsGoalRepository?.getSavingsGoals()
            val savingsGoals = (goalsResult as? Result.Success)?.data ?: emptyList()

            savingsGoals.forEach { goal ->
                val goalBal = BigDecimal.fromDouble(goal.currentAmount)
                val alreadyIncluded = assetItems.any {
                    it.name.equals(goal.name, ignoreCase = true) ||
                    it.name.contains(goal.name, ignoreCase = true) ||
                    goal.name.contains(it.name, ignoreCase = true)
                }
                if (goalBal > BigDecimal.ZERO && !alreadyIncluded) {
                    assetItems.add(AssetLiabilityItem(goal.id, "${goal.name} (Vault)", goalBal, isAsset = true))
                }
            }

            // 3. Recurring Liabilities & Overdrafts - Deduplicated against account liabilities
            val billsResult = transactionRepository.getRecurringBills()
            val recurringBills = (billsResult as? Result.Success)?.data ?: emptyList()

            val accountFilteredBills = if (targetAccountIds.isNotEmpty()) {
                recurringBills.filter { bill ->
                    val isBankDebt = bill.name.contains("bank", ignoreCase = true) || bill.name.contains("kcb", ignoreCase = true) || bill.name.contains("equity", ignoreCase = true)
                    val isMpesaSelected = targetAccounts.any { it.name.contains("mpesa", ignoreCase = true) || it.type == com.fintrack.shared.feature.account.domain.model.AccountType.MPESA }
                    if (isMpesaSelected && isBankDebt) false else true
                }
            } else recurringBills

            accountFilteredBills.filter { it.isActive && (it.name.contains("loan", ignoreCase = true) || it.name.contains("fuliza", ignoreCase = true)) }
                .forEach { bill ->
                    val alreadyInLiabilities = liabilityItems.any { it.name.contains(bill.name, ignoreCase = true) || bill.name.contains(it.name, ignoreCase = true) }
                    if (!alreadyInLiabilities) {
                        liabilityItems.add(AssetLiabilityItem(bill.id, bill.name, bill.amount, isAsset = false))
                    }
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

            // 4. YTD (Jan 1st to Today) Net Worth Calculation
            val janFirstStr = "${today.year}-01-01"
            val ytdTxResult = transactionRepository.getAllTransactions(startDate = janFirstStr, endDate = todayStr, accountId = singleAccountId)
            val ytdRawTx = (ytdTxResult as? Result.Success)?.data ?: emptyList()
            val ytdFilteredTx = if (targetAccountIds.size > 1) {
                ytdRawTx.filter { targetAccountIds.contains(it.accountId) }
            } else ytdRawTx

            var ytdNetIncome = BigDecimal.ZERO
            for (tx in ytdFilteredTx) {
                if (tx.isIncome) {
                    ytdNetIncome = ytdNetIncome + tx.amount
                } else {
                    ytdNetIncome = ytdNetIncome - tx.amount
                }
            }

            val janFirstNetWorth = netWorth - ytdNetIncome
            val ytdGrowthRatePercentage = if (janFirstNetWorth > BigDecimal.ZERO && netWorth != janFirstNetWorth) {
                (netWorth - janFirstNetWorth).divide(janFirstNetWorth, decimalMode).multiply(BigDecimal.fromInt(100))
            } else if (netWorth > BigDecimal.ZERO && janFirstNetWorth <= BigDecimal.ZERO) {
                BigDecimal.fromInt(100)
            } else BigDecimal.ZERO

            Result.Success(
                NetWorthSummary(
                    totalAssets = totalAssets,
                    totalLiabilities = totalLiabilities,
                    netWorth = netWorth,
                    previousNetWorth = previousNetWorth,
                    growthRatePercentage = growthRatePercentage,
                    ytdGrowthRatePercentage = ytdGrowthRatePercentage,
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
