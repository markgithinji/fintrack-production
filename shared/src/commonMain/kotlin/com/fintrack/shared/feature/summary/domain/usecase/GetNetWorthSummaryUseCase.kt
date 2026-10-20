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

class GetNetWorthSummaryUseCase(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
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

            // Also check for Fuliza / Loan recurring liabilities
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

            Result.Success(
                NetWorthSummary(
                    totalAssets = totalAssets,
                    totalLiabilities = totalLiabilities,
                    netWorth = netWorth,
                    previousNetWorth = null,
                    growthRatePercentage = BigDecimal.fromInt(12),
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
