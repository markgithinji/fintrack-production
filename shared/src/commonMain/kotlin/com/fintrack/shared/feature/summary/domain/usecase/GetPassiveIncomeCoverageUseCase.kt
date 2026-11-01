package com.fintrack.shared.feature.summary.domain.usecase

import com.fintrack.shared.feature.core.util.DateTimeHelper
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.planning.domain.repository.AssetRepository
import com.fintrack.shared.feature.summary.domain.model.PassiveIncomeCoverage
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.fintrack.shared.feature.transaction.domain.repository.TransactionRepository
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

class GetPassiveIncomeCoverageUseCase(
    private val transactionRepository: TransactionRepository,
    private val assetRepository: AssetRepository? = null
) {
    suspend operator fun invoke(accountId: String? = null): Result<PassiveIncomeCoverage> {
        return try {
            val tz = TimeZone.currentSystemDefault()
            val today = DateTimeHelper.now().toLocalDateTime(tz).date
            val thirtyDaysAgo = today.minus(DatePeriod(days = 30)).toString()
            val todayStr = today.toString()

            // 1. Fetch monthly yield from logged Asset holdings
            val assetMonthlyYield = if (assetRepository != null) {
                when (val assetRes = assetRepository.getAssets()) {
                    is Result.Success -> assetRes.data.sumOf { it.monthlyPassiveYield }
                    else -> 0.0
                }
            } else 0.0

            // 2. Fetch last 30 days of transactions
            val txRes = transactionRepository.getAllTransactions(
                accountId = accountId,
                startDate = thirtyDaysAgo,
                endDate = todayStr
            )

            val transactions = (txRes as? Result.Success)?.data ?: emptyList()

            // 3. Calculate passive income transactions (Rental, Dividends, Interest, Investments)
            val passiveInflowTxns = transactions
                .filter { it.isIncome }
                .filter { tx ->
                    val catName = (tx.category ?: tx.description ?: "").lowercase()
                    catName.contains("dividend") ||
                        catName.contains("rental") ||
                        catName.contains("interest") ||
                        catName.contains("investment") ||
                        catName.contains("yield")
                }
                .sumOf { it.amount.doubleValue(false) }

            val totalPassiveIncome = assetMonthlyYield + passiveInflowTxns

            // 4. Calculate actual living expenses in last 30 days
            val realLivingExpenses = transactions.filter { isRealLivingExpense(it) }
            val totalLivingExpenses = realLivingExpenses.sumOf { it.amount.doubleValue(false) }

            val monthlyEssentialExpenses = if (totalLivingExpenses > 0) totalLivingExpenses else 1.0

            // 5. Compute Coverage Percentage
            val rawCoverage = (totalPassiveIncome / monthlyEssentialExpenses) * 100.0
            val coveragePct = kotlin.math.round(rawCoverage * 10.0) / 10.0

            // 6. Generate Milestone Description
            val milestone = when {
                coveragePct >= 100.0 -> "Financial Freedom Achieved! Passive returns cover 100%+ of your living expenses!"
                coveragePct >= 50.0 -> "Halfway To Independence! Passive yield covers $coveragePct% of monthly expenses."
                coveragePct >= 20.0 -> "Solid Cushion! Passive yield covers $coveragePct% of rent & utility bills."
                coveragePct > 0.0 -> "Building Momentum! Passive yield covers $coveragePct% of living costs."
                else -> "Log MMFs, Saccos, or dividend stocks on the Assets tab to track passive coverage!"
            }

            Result.Success(
                PassiveIncomeCoverage(
                    monthlyPassiveYieldAmount = assetMonthlyYield,
                    monthlyPassiveInflowTransactions = passiveInflowTxns,
                    totalMonthlyPassiveIncome = totalPassiveIncome,
                    monthlyEssentialExpenses = monthlyEssentialExpenses,
                    coveragePercentage = coveragePct,
                    milestoneDescription = milestone
                )
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private fun isRealLivingExpense(tx: Transaction): Boolean {
        if (tx.isIncome) return false

        val amount = tx.amount.doubleValue(false)
        if (amount <= 0.0) return false

        val categoryName = tx.category?.lowercase() ?: ""
        val description = tx.description?.lowercase() ?: ""

        // 1. Exclude reversals and internal self-transfers between user's accounts
        if (categoryName.contains("reversal") || description.contains("reversal of")) return false
        if (description.contains("transfer to own account") || description.contains("internal transfer") || description.contains("transfer between")) return false

        // 2. Exclude savings deposits (M-Shwari / Lock Savings / Vaults / MMF deposits)
        if (categoryName.contains("savings") || description.contains("m-shwari") || description.contains("lock savings") || description.contains("vault")) return false

        // 3. Exclude Bank Paybill transfers (Equity 247247, KCB 522522, Co-op 011011, Family Bank 222111, NCBA 888888, Stanbic 900200)
        if (description.contains("247247") || description.contains("522522") || description.contains("011011") || description.contains("222111") || description.contains("888888") || description.contains("900200")) return false

        // 4. Exclude Loan Repayments (Fuliza, M-Shwari loan, KCB M-Pesa loan, OKash, Tala, Branch)
        if (description.contains("fuliza") || description.contains("loan repayment") || description.contains("repay loan") || description.contains("kcb mpesa loan")) return false

        // 5. Exclude Property / Land / Capital Asset Purchases (> KSh 150,000)
        if (categoryName.contains("property") || categoryName.contains("investment") || categoryName.contains("asset") || amount > 150000.0) return false

        return true
    }
}
