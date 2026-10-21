package com.fintrack.shared.feature.summary.domain.usecase

import com.fintrack.shared.feature.core.util.DateTimeHelper
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.core.util.toDouble
import com.fintrack.shared.feature.core.util.toInt
import com.fintrack.shared.feature.summary.domain.model.BudgetBucketSummary
import com.fintrack.shared.feature.summary.domain.model.BudgetBucketType
import com.fintrack.shared.feature.summary.domain.model.BudgetOptimizerSummary
import com.fintrack.shared.feature.summary.domain.model.TrimPriority
import com.fintrack.shared.feature.summary.domain.model.TrimRecommendation
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

class GetBudgetOptimizerUseCase(
    private val transactionRepository: TransactionRepository
) {
    private val decimalMode = DecimalMode(decimalPrecision = 20, roundingMode = RoundingMode.ROUND_HALF_CEILING)

    suspend operator fun invoke(accountId: String? = null): Result<BudgetOptimizerSummary> = withContext(Dispatchers.IO) {
        try {
            val targetAccountIds = accountId?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
            val singleAccountId = if (targetAccountIds.size == 1) targetAccountIds.first() else null

            val nowInstant = DateTimeHelper.now()
            val today = nowInstant.toLocalDateTime(TimeZone.currentSystemDefault()).date
            val thirtyDaysAgo = today.minus(DatePeriod(days = 30)).toString()
            val todayStr = today.toString()

            val txResult = transactionRepository.getAllTransactions(startDate = thirtyDaysAgo, endDate = todayStr, accountId = singleAccountId)
            val rawTx = (txResult as? Result.Success)?.data ?: emptyList()
            val transactions = if (targetAccountIds.size > 1) {
                rawTx.filter { targetAccountIds.contains(it.accountId) }
            } else rawTx

            var totalIncome = BigDecimal.ZERO
            var totalExpense = BigDecimal.ZERO
            val categorySpendMap = mutableMapOf<String, BigDecimal>()

            transactions.forEach { tx ->
                if (tx.isIncome) {
                    totalIncome = totalIncome + tx.amount
                } else {
                    totalExpense = totalExpense + tx.amount
                    val catName = tx.category ?: "Uncategorized"
                    val current = categorySpendMap[catName] ?: BigDecimal.ZERO
                    categorySpendMap[catName] = current + tx.amount
                }
            }

            // Baseline budget amount for 50/30/20 calculation
            val baseAmount = if (totalIncome > BigDecimal.ZERO) totalIncome else if (totalExpense > BigDecimal.ZERO) totalExpense else BigDecimal.fromInt(1)

            var needsSpend = BigDecimal.ZERO
            var wantsSpend = BigDecimal.ZERO
            var savingsSpend = BigDecimal.ZERO

            val needsCategories = setOf(
                "Rent", "Groceries", "Utilities", "Internet", "Bills", "Health", "Education",
                "Insurance", "Bank", "Loans", "Transaction Fees", "Maintenance", "Government", "Transport"
            )
            val savingsCategories = setOf(
                "Savings", "Investments", "Vault", "Emergency Fund"
            )

            categorySpendMap.forEach { (cat, amount) ->
                val isNeeds = needsCategories.any { cat.equals(it, ignoreCase = true) }
                val isSavings = savingsCategories.any { cat.equals(it, ignoreCase = true) }
                when {
                    isSavings -> savingsSpend = savingsSpend + amount
                    isNeeds -> needsSpend = needsSpend + amount
                    else -> wantsSpend = wantsSpend + amount
                }
            }

            // Calculate percentages
            val needsPct = needsSpend.divide(baseAmount, decimalMode).multiply(BigDecimal.fromInt(100))
            val wantsPct = wantsSpend.divide(baseAmount, decimalMode).multiply(BigDecimal.fromInt(100))
            val savingsPct = savingsSpend.divide(baseAmount, decimalMode).multiply(BigDecimal.fromInt(100))

            val needsTarget = baseAmount.multiply(BigDecimal.fromDouble(0.50))
            val wantsTarget = baseAmount.multiply(BigDecimal.fromDouble(0.30))
            val savingsTarget = baseAmount.multiply(BigDecimal.fromDouble(0.20))

            val needsSummary = BudgetBucketSummary(
                bucketType = BudgetBucketType.NEEDS,
                actualAmount = needsSpend,
                actualPercentage = needsPct,
                targetPercentage = BigDecimal.fromInt(50),
                targetAmount = needsTarget,
                varianceAmount = needsSpend - needsTarget
            )

            val wantsSummary = BudgetBucketSummary(
                bucketType = BudgetBucketType.WANTS,
                actualAmount = wantsSpend,
                actualPercentage = wantsPct,
                targetPercentage = BigDecimal.fromInt(30),
                targetAmount = wantsTarget,
                varianceAmount = wantsSpend - wantsTarget
            )

            val savingsSummary = BudgetBucketSummary(
                bucketType = BudgetBucketType.SAVINGS,
                actualAmount = savingsSpend,
                actualPercentage = savingsPct,
                targetPercentage = BigDecimal.fromInt(20),
                targetAmount = savingsTarget,
                varianceAmount = savingsSpend - savingsTarget
            )

            // Calculate Budget Health Score (0-100)
            var score = 100
            val wantsExcessPct = wantsPct.toDouble() - 30.0
            if (wantsExcessPct > 0) score -= (wantsExcessPct * 2.0).toInt()

            val needsExcessPct = needsPct.toDouble() - 50.0
            if (needsExcessPct > 0) score -= (needsExcessPct * 1.5).toInt()

            val savingsDeficitPct = 20.0 - savingsPct.toDouble()
            if (savingsDeficitPct > 0) score -= (savingsDeficitPct * 2.0).toInt()

            val healthScore = score.coerceIn(5, 100)

            // Generate Trim Recommendations
            val trimRecommendations = mutableListOf<TrimRecommendation>()

            categorySpendMap.forEach { (catName, spend) ->
                val isWantsCategory = !needsCategories.any { catName.equals(it, ignoreCase = true) } &&
                        !savingsCategories.any { catName.equals(it, ignoreCase = true) }

                if (spend > BigDecimal.ZERO) {
                    val trimRatio = when {
                        catName.equals("Dining Out", ignoreCase = true) || catName.equals("Fastfood", ignoreCase = true) -> 0.35
                        catName.equals("Subscriptions", ignoreCase = true) -> 0.30
                        catName.equals("Entertainment", ignoreCase = true) -> 0.25
                        catName.equals("Shopping", ignoreCase = true) -> 0.20
                        isWantsCategory && spend > baseAmount.multiply(BigDecimal.fromDouble(0.05)) -> 0.15
                        else -> 0.0
                    }

                    if (trimRatio > 0.0) {
                        val trimVal = spend.multiply(BigDecimal.fromDouble(trimRatio))
                        if (trimVal > BigDecimal.fromInt(100)) {
                            val annualSavings = trimVal.multiply(BigDecimal.fromInt(12))
                            val priority = when {
                                trimRatio >= 0.30 || spend > baseAmount.multiply(BigDecimal.fromDouble(0.10)) -> TrimPriority.HIGH
                                trimRatio >= 0.20 -> TrimPriority.MEDIUM
                                else -> TrimPriority.LOW
                            }

                            trimRecommendations.add(
                                TrimRecommendation(
                                    id = catName,
                                    categoryName = catName,
                                    bucketType = if (isWantsCategory) BudgetBucketType.WANTS else BudgetBucketType.NEEDS,
                                    currentSpend = spend,
                                    suggestedTrimAmount = trimVal,
                                    projectedAnnualSavings = annualSavings,
                                    impactMessage = "Trimming Ksh ${trimVal.toInt()}/mo from $catName frees up Ksh ${annualSavings.toInt()}/yr toward your 20% savings goal.",
                                    iconName = catName,
                                    priority = priority
                                )
                            )
                        }
                    }
                }
            }

            // Sort recommendations by suggested trim amount descending
            val sortedRecs = trimRecommendations.sortedByDescending { it.suggestedTrimAmount.toDouble() }.take(4)

            val primaryInsight = when {
                wantsPct.toDouble() > 35.0 -> "Your 'Wants' category is at ${wantsPct.toInt()}% (target 30%). Trimming discretionary items like ${sortedRecs.firstOrNull()?.categoryName ?: "Dining Out"} will boost your savings."
                savingsPct.toDouble() < 15.0 -> "Your savings allocation is at ${savingsPct.toInt()}% (target 20%). Redirecting small trims from wants directly into your vaults will hit your target."
                else -> "Great job! Your spending aligns well with the 50/30/20 financial health rule."
            }

            Result.Success(
                BudgetOptimizerSummary(
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    needs = needsSummary,
                    wants = wantsSummary,
                    savings = savingsSummary,
                    recommendations = sortedRecs,
                    healthScore = healthScore,
                    primaryActionableInsight = primaryInsight
                )
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
