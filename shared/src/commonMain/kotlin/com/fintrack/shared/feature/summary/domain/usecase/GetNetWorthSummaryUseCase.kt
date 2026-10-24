package com.fintrack.shared.feature.summary.domain.usecase

import com.fintrack.shared.feature.account.domain.model.AccountType
import com.fintrack.shared.feature.account.domain.model.PortfolioGroup
import com.fintrack.shared.feature.account.domain.model.portfolioGroup
import com.fintrack.shared.feature.account.domain.repository.AccountRepository
import com.fintrack.shared.feature.budget.domain.model.GoalType
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.core.util.toDouble
import com.fintrack.shared.feature.core.util.toInt
import com.fintrack.shared.feature.summary.domain.model.AssetLiabilityItem
import com.fintrack.shared.feature.summary.domain.model.FireScore
import com.fintrack.shared.feature.summary.domain.model.NetWorthMilestone
import com.fintrack.shared.feature.summary.domain.model.NetWorthSummary
import com.fintrack.shared.feature.summary.domain.model.PortfolioAllocationGroup
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
    private val savingsGoalRepository: com.fintrack.shared.feature.budget.domain.repository.SavingsGoalRepository? = null,
    private val debtRepository: com.fintrack.shared.feature.planning.domain.repository.DebtRepository? = null
) {
    private val decimalMode = DecimalMode(decimalPrecision = 20, roundingMode = RoundingMode.ROUND_HALF_CEILING)

    suspend operator fun invoke(
        accountId: String? = null,
        targetMilestoneAmount: BigDecimal? = null
    ): Result<NetWorthSummary> = withContext(Dispatchers.IO) {
        try {
            val targetAccountIds = accountId?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()

            // 1. Accounts Fetch
            val accountsResult = accountRepository.getAccounts()
            val accounts = (accountsResult as? Result.Success)?.data ?: emptyList()
            val targetAccounts = if (targetAccountIds.isNotEmpty()) {
                accounts.filter { targetAccountIds.contains(it.id) }
            } else accounts

            // Assets vs Liabilities Items and Portfolio Allocation Map
            val assetItems = mutableListOf<AssetLiabilityItem>()
            val liabilityItems = mutableListOf<AssetLiabilityItem>()
            val groupAmountMap = mutableMapOf<PortfolioGroup, BigDecimal>()

            targetAccounts.forEach { acc ->
                val bal = acc.balance ?: BigDecimal.ZERO
                val group = acc.type.portfolioGroup

                if (bal >= BigDecimal.ZERO) {
                    assetItems.add(AssetLiabilityItem(acc.id, acc.name, bal, isAsset = true))
                    val currentGroupSum = groupAmountMap[group] ?: BigDecimal.ZERO
                    groupAmountMap[group] = currentGroupSum + bal
                } else {
                    liabilityItems.add(AssetLiabilityItem(acc.id, acc.name, bal.abs(), isAsset = false))
                    val currentGroupSum = groupAmountMap[PortfolioGroup.LIABILITIES_DEBT] ?: BigDecimal.ZERO
                    groupAmountMap[PortfolioGroup.LIABILITIES_DEBT] = currentGroupSum + bal.abs()
                }
            }

            // 2. Planning Screen Goals, MMFs, Saccos & Assets
            val goalsResult = savingsGoalRepository?.getSavingsGoals()
            val savingsGoals = (goalsResult as? Result.Success)?.data ?: emptyList()

            savingsGoals.forEach { goal ->
                val goalBal = BigDecimal.fromDouble(goal.currentAmount)
                val alreadyIncluded = assetItems.any {
                    it.name.equals(goal.name, ignoreCase = true) ||
                    it.name.contains(goal.name, ignoreCase = true) ||
                    goal.name.contains(it.name, ignoreCase = true)
                } || liabilityItems.any {
                    it.name.equals(goal.name, ignoreCase = true) ||
                    it.name.contains(goal.name, ignoreCase = true)
                }

                if (goalBal > BigDecimal.ZERO && !alreadyIncluded) {
                    val group = when (goal.goalType) {
                        GoalType.INVESTMENT_MMF -> PortfolioGroup.INVESTMENTS_MMF
                        GoalType.SACCO_DEPOSIT -> PortfolioGroup.SAVINGS_SACCO
                        GoalType.PROPERTY_ASSET -> PortfolioGroup.REAL_ESTATE
                        GoalType.DEBT_LOAN -> PortfolioGroup.LIABILITIES_DEBT
                        GoalType.SAVINGS_ASSET -> PortfolioGroup.SAVINGS_SACCO
                    }

                    if (goal.goalType == GoalType.DEBT_LOAN) {
                        liabilityItems.add(AssetLiabilityItem(goal.id, "${goal.name} (Loan)", goalBal, isAsset = false))
                        val currentSum = groupAmountMap[PortfolioGroup.LIABILITIES_DEBT] ?: BigDecimal.ZERO
                        groupAmountMap[PortfolioGroup.LIABILITIES_DEBT] = currentSum + goalBal
                    } else {
                        val labelExtra = if (goal.annualRatePercentage != null && goal.annualRatePercentage > 0) " (${goal.annualRatePercentage}% p.a.)" else ""
                        assetItems.add(AssetLiabilityItem(goal.id, "${goal.name}$labelExtra", goalBal, isAsset = true))
                        val currentSum = groupAmountMap[group] ?: BigDecimal.ZERO
                        groupAmountMap[group] = currentSum + goalBal
                    }
                }
            }

            // 3. Planning Screen Debts & Loans
            val debtsResult = debtRepository?.getDebts()
            val debts = (debtsResult as? Result.Success)?.data ?: emptyList()

            debts.forEach { debt ->
                val debtBal = BigDecimal.fromDouble(debt.balance)
                val alreadyInLiabilities = liabilityItems.any {
                    it.name.equals(debt.name, ignoreCase = true) ||
                    it.name.contains(debt.name, ignoreCase = true) ||
                    debt.name.contains(it.name, ignoreCase = true)
                }
                if (debtBal > BigDecimal.ZERO && !alreadyInLiabilities) {
                    liabilityItems.add(AssetLiabilityItem(debt.id, debt.name, debtBal, isAsset = false))
                    val currentDebtSum = groupAmountMap[PortfolioGroup.LIABILITIES_DEBT] ?: BigDecimal.ZERO
                    groupAmountMap[PortfolioGroup.LIABILITIES_DEBT] = currentDebtSum + debtBal
                }
            }

            // 3. Recurring Liabilities & Overdrafts -> LIABILITIES_DEBT Group
            val billsResult = transactionRepository.getRecurringBills()
            val recurringBills = (billsResult as? Result.Success)?.data ?: emptyList()

            val accountFilteredBills = if (targetAccountIds.isNotEmpty()) {
                recurringBills.filter { bill ->
                    val isBankDebt = bill.name.contains("bank", ignoreCase = true) || bill.name.contains("kcb", ignoreCase = true) || bill.name.contains("equity", ignoreCase = true)
                    val isMpesaSelected = targetAccounts.any { it.name.contains("mpesa", ignoreCase = true) || it.type == AccountType.MPESA }
                    if (isMpesaSelected && isBankDebt) false else true
                }
            } else recurringBills

            accountFilteredBills.filter { it.isActive && (it.name.contains("loan", ignoreCase = true) || it.name.contains("fuliza", ignoreCase = true)) }
                .forEach { bill ->
                    val alreadyInLiabilities = liabilityItems.any { it.name.contains(bill.name, ignoreCase = true) || bill.name.contains(it.name, ignoreCase = true) }
                    if (!alreadyInLiabilities) {
                        liabilityItems.add(AssetLiabilityItem(bill.id, bill.name, bill.amount, isAsset = false))
                        val currentDebtSum = groupAmountMap[PortfolioGroup.LIABILITIES_DEBT] ?: BigDecimal.ZERO
                        groupAmountMap[PortfolioGroup.LIABILITIES_DEBT] = currentDebtSum + bill.amount
                    }
                }

            val totalAssets = assetItems.fold(BigDecimal.ZERO) { sum, item -> sum + item.amount }
            val totalLiabilities = liabilityItems.fold(BigDecimal.ZERO) { sum, item -> sum + item.amount }
            val netWorth = totalAssets - totalLiabilities

            // Build Portfolio Allocation Groups
            val portfolioAllocations = mutableListOf<PortfolioAllocationGroup>()
            if (totalAssets > BigDecimal.ZERO) {
                groupAmountMap.forEach { (group, amount) ->
                    if (group != PortfolioGroup.LIABILITIES_DEBT && amount > BigDecimal.ZERO) {
                        val pct = amount.divide(totalAssets, decimalMode).multiply(BigDecimal.fromInt(100))
                        val (name, color) = when (group) {
                            PortfolioGroup.LIQUID_CASH -> "Liquid Cash" to "#00E676"
                            PortfolioGroup.INVESTMENTS_MMF -> "MMFs & Investments" to "#2979FF"
                            PortfolioGroup.SAVINGS_SACCO -> "Sacco & Savings Assets" to "#AA00FF"
                            PortfolioGroup.REAL_ESTATE -> "Real Estate & Assets" to "#FF9100"
                            PortfolioGroup.LIABILITIES_DEBT -> "Liabilities" to "#FF1744"
                        }
                        portfolioAllocations.add(
                            PortfolioAllocationGroup(
                                group = group,
                                displayName = name,
                                totalAmount = amount,
                                percentageOfAssets = pct,
                                hexColor = color
                            )
                        )
                    }
                }
            }

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

            // YTD Net Worth Calculation
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

            // Net Worth Milestone Calculation
            val targetMilestone = targetMilestoneAmount ?: when {
                netWorth < BigDecimal.fromInt(1_000_000) -> BigDecimal.fromInt(1_000_000)
                netWorth < BigDecimal.fromInt(5_000_000) -> BigDecimal.fromInt(5_000_000)
                netWorth < BigDecimal.fromInt(10_000_000) -> BigDecimal.fromInt(10_000_000)
                else -> netWorth.multiply(BigDecimal.fromDouble(1.5))
            }

            val progressPct = if (targetMilestone > BigDecimal.ZERO) {
                netWorth.divide(targetMilestone, decimalMode).multiply(BigDecimal.fromInt(100))
            } else BigDecimal.ZERO

            val remainingMilestone = if (targetMilestone > netWorth) targetMilestone - netWorth else BigDecimal.ZERO
            val estMonths = if (past30dNetIncome > BigDecimal.ZERO && remainingMilestone > BigDecimal.ZERO) {
                (remainingMilestone.toDouble() / past30dNetIncome.toDouble()).toInt().coerceAtLeast(1)
            } else null

            val milestone = NetWorthMilestone(
                targetAmount = targetMilestone,
                currentProgressPercentage = progressPct,
                remainingAmount = remainingMilestone,
                estimatedMonthsToTarget = estMonths
            )

            // FIRE Score Calculation (25x Annual Living Expenses Rule)
            val realLivingExpenses = filteredTx.filter { tx ->
                if (tx.isIncome) return@filter false
                val amt = tx.amount.doubleValue(false)
                if (amt <= 0.0 || amt > 150000.0) return@filter false

                val catName = (tx.category ?: "").lowercase()
                val desc = (tx.description ?: "").lowercase()

                if (catName.contains("reversal") || desc.contains("reversal of")) return@filter false
                if (catName.contains("savings") || desc.contains("m-shwari") || desc.contains("lock savings") || desc.contains("vault")) return@filter false
                if (desc.contains("transfer to own account") || desc.contains("internal transfer") || desc.contains("transfer between")) return@filter false
                if (catName.contains("property") || catName.contains("investment") || catName.contains("asset") || desc.contains("property purchase") || desc.contains("land purchase")) return@filter false

                true
            }

            val total30dExpenses = realLivingExpenses.sumOf { it.amount.doubleValue(false) }
            val monthlyExpenses = if (total30dExpenses > 0) total30dExpenses else 20000.0
            val annualExpenses = monthlyExpenses * 12.0
            val fireTarget = annualExpenses * 25.0
            val netWorthVal = netWorth.doubleValue(false).coerceAtLeast(0.0)
            val firePct = if (fireTarget > 0) (netWorthVal / fireTarget * 100.0).coerceAtMost(1000.0) else 0.0
            val firePctRounded = kotlin.math.round(firePct * 10.0) / 10.0

            val phase = when {
                firePctRounded >= 100.0 -> "FIRE Unlocked! Financial Independence Achieved 🏆"
                firePctRounded >= 75.0 -> "Phase 4: Near Financial Independence"
                firePctRounded >= 50.0 -> "Phase 3: Halfway To FIRE Independence"
                firePctRounded >= 25.0 -> "Phase 2: Coast FI Growth Momentum"
                else -> "Phase 1: Wealth Foundation Building"
            }

            val netMonthlySavings = past30dNetIncome.doubleValue(false)
            val remainingFireCash = (fireTarget - netWorthVal).coerceAtLeast(0.0)
            val estYears = if (netMonthlySavings > 0 && remainingFireCash > 0) {
                val yrs = remainingFireCash / (netMonthlySavings * 12.0)
                kotlin.math.round(yrs * 10.0) / 10.0
            } else null

            val fireScore = FireScore(
                netWorth = netWorthVal,
                monthlyLivingExpenses = monthlyExpenses,
                annualLivingExpenses = annualExpenses,
                fireTargetAmount = fireTarget,
                fireProgressPercentage = firePctRounded,
                phaseName = phase,
                estimatedYearsToFire = estYears
            )

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
                    liabilityBreakdown = liabilityItems,
                    portfolioAllocations = portfolioAllocations.sortedByDescending { it.totalAmount.toDouble() },
                    milestone = milestone,
                    fireScore = fireScore
                )
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
