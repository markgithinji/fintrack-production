package com.fintrack.shared.feature.summary.domain.usecase

import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.core.util.toDouble
import com.fintrack.shared.feature.summary.domain.model.FinancialWellbeingInsight
import com.fintrack.shared.feature.summary.domain.model.WellbeingInsightCategory
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import kotlin.math.ceil
import kotlin.math.round

object FinancialWellbeingInsightEngine {

    fun analyzeWellbeingInsights(
        transactions: List<Transaction>,
        accounts: List<Account> = emptyList(),
        netWorth: Double = 0.0,
        totalLiabilities: Double = 0.0,
        debtToAssetRatio: Double = 0.0,
        monthlySurplus: Double = 0.0,
        monthlyLivingExpenses: Double = 0.0,
        emergencyBufferMonths: Double = 0.0
    ): List<FinancialWellbeingInsight> {
        val insights = mutableListOf<FinancialWellbeingInsight>()

        // 1. Real Top Category Spend Pattern
        val topCategoryEntry = transactions
            .filter { !it.isIncome }
            .groupBy { it.category ?: "Uncategorized" }
            .mapValues { entry -> entry.value.sumOf { it.amount.doubleValue(false) } }
            .maxByOrNull { entry -> entry.value }

        if (topCategoryEntry != null && topCategoryEntry.value >= 1500.0) {
            val catName = topCategoryEntry.key
            val totalCatSpend = topCategoryEntry.value.toLong()
            val monthlyCatSpend = (totalCatSpend / 3.0).coerceAtLeast(100.0).toLong()
            val suggestTrim = (monthlyCatSpend * 0.20).toLong()
            val annualTrim = suggestTrim * 12

            insights.add(
                FinancialWellbeingInsight(
                    id = "top_category_optimization_$catName",
                    category = WellbeingInsightCategory.EMERGENCY_SHIELD,
                    title = "$catName Spend Optimization (KES $monthlyCatSpend/mo)",
                    actionTip = "Your highest spending category is $catName (KES $totalCatSpend spent over 90 days). Trimming 20% (KES $suggestTrim/mo) redirects KES $annualTrim/yr into high-yield MMF vaults.",
                    estimatedMonthlyImpactAmount = suggestTrim.toDouble(),
                    iconName = "TrendingUp"
                )
            )
        }

        // 2. Real Solvency & Debt Payoff Velocity Insight
        if (totalLiabilities > 0.0 && monthlySurplus > 0.0) {
            val monthsToPayoff = ceil(totalLiabilities / (monthlySurplus * 0.5)).toInt().coerceAtLeast(1)
            insights.add(
                FinancialWellbeingInsight(
                    id = "debt_payoff_velocity",
                    category = WellbeingInsightCategory.DEBT_PAYOFF_BOOST,
                    title = "Accelerate Debt Elimination (${debtToAssetRatio.toInt()}% Debt Ratio)",
                    actionTip = "Your Debt-to-Asset ratio is ${debtToAssetRatio.toInt()}% with KES ${totalLiabilities.toLong()} in active loans. Routing 50% of your KES ${monthlySurplus.toLong()}/mo cashflow surplus eliminates your debt in ~$monthsToPayoff months.",
                    estimatedMonthlyImpactAmount = monthlySurplus * 0.5,
                    iconName = "AccountBalance"
                )
            )
        }

        // 3. Real Emergency Cushion & Shortfall Analysis
        val liquidCheckingBalance = accounts.sumOf { acc -> (acc.balance?.doubleValue(false) ?: 0.0).coerceAtLeast(0.0) }
        val effectiveLivingCost = if (monthlyLivingExpenses > 0.0) monthlyLivingExpenses else 30000.0
        val target3MoBuffer = effectiveLivingCost * 3.0
        val bufferShortfall = (target3MoBuffer - liquidCheckingBalance).coerceAtLeast(0.0)

        if (emergencyBufferMonths < 3.0 && bufferShortfall > 0.0) {
            val roundedMonths = round(emergencyBufferMonths * 10.0) / 10.0
            val monthsToTarget = if (monthlySurplus > 0.0) ceil(bufferShortfall / monthlySurplus).toInt().coerceAtLeast(1) else null
            val timeFrameNotice = if (monthsToTarget != null) "in ~$monthsToTarget months at your current savings pace." else "by re-allocating discretionary spend into a liquid vault."

            insights.add(
                FinancialWellbeingInsight(
                    id = "emergency_cushion_shortfall",
                    category = WellbeingInsightCategory.EMERGENCY_SHIELD,
                    title = "3-Month Emergency Cushion Shortfall",
                    actionTip = "Your liquid cash reserve of KES ${liquidCheckingBalance.toLong()} covers $roundedMonths months of your KES ${effectiveLivingCost.toLong()}/mo living costs. Building your 3-month target (KES ${target3MoBuffer.toLong()}) can be achieved $timeFrameNotice",
                    estimatedMonthlyImpactAmount = bufferShortfall,
                    iconName = "HealthAndSafety"
                )
            )
        }

        // 4. Real Discretionary Spend Optimization
        val totalDiscretionarySpend = transactions.filter { tx ->
            if (tx.isIncome) return@filter false
            val cat = tx.category?.lowercase() ?: ""
            !cat.contains("rent") && !cat.contains("groceries") && !cat.contains("utilities") && !cat.contains("bills") && !cat.contains("health") && !cat.contains("education") && !cat.contains("loan")
        }.sumOf { it.amount.doubleValue(false) }

        if (totalDiscretionarySpend >= 3000.0) {
            val monthlyDiscretionary = (totalDiscretionarySpend / 3.0).toLong()
            val suggestTrim = (monthlyDiscretionary * 0.20).toLong()
            val annualSavings = suggestTrim * 12
            insights.add(
                FinancialWellbeingInsight(
                    id = "discretionary_lifestyle_trim",
                    category = WellbeingInsightCategory.FEE_LEAK_PREVENTION,
                    title = "Discretionary Lifestyle Trim Opportunity",
                    actionTip = "Discretionary lifestyle spend averaged KES $monthlyDiscretionary/mo over 90 days. Trimming 20% (KES $suggestTrim/mo) frees up KES $annualSavings/yr toward your 20% savings goal.",
                    estimatedMonthlyImpactAmount = suggestTrim.toDouble(),
                    iconName = "AccountBalance"
                )
            )
        }

        // 5. Real Cash Withdrawal Fee Leak Prevention
        val feeTxs = transactions.filter { tx ->
            if (tx.isIncome) return@filter false
            val cat = tx.category?.lowercase() ?: ""
            val desc = tx.description?.lowercase() ?: ""
            cat.contains("fee") || desc.contains("agent withdrawal") || desc.contains("atm withdrawal") || desc.contains("withdrawal fee")
        }
        val totalFeeSpend = feeTxs.sumOf { it.amount.doubleValue(false) }
        if (totalFeeSpend >= 150.0) {
            val annualFeeLoss = totalFeeSpend * 4.0
            insights.add(
                FinancialWellbeingInsight(
                    id = "fee_leak_prevention",
                    category = WellbeingInsightCategory.FEE_LEAK_PREVENTION,
                    title = "Eliminate Agent & ATM Withdrawal Fees",
                    actionTip = "You paid KES ${totalFeeSpend.toLong()} in agent/ATM withdrawal fees over 90 days (KES ${annualFeeLoss.toLong()}/yr). Paying merchants directly via M-Pesa Buy Goods Till or Paybill is 100% free of withdrawal fees.",
                    estimatedMonthlyImpactAmount = (totalFeeSpend / 3.0),
                    iconName = "AccountBalance"
                )
            )
        }

        // 6. Real Idle Checking Cash Yield Optimization
        if (liquidCheckingBalance >= 20000.0) {
            val idleCash = liquidCheckingBalance - 15000.0
            val potentialMonthlyMmfYield = idleCash * (0.115 / 12.0)
            if (potentialMonthlyMmfYield >= 150.0) {
                insights.add(
                    FinancialWellbeingInsight(
                        id = "idle_cash_yield_opportunity",
                        category = WellbeingInsightCategory.IDLE_CASH_YIELD,
                        title = "24h MMF Passive Yield on Idle Cash",
                        actionTip = "You have KES ${liquidCheckingBalance.toLong()} in checking accounts at 0% interest. Moving KES ${idleCash.toLong()} into a 24-hour liquid MMF earns +KES ${potentialMonthlyMmfYield.toLong()}/mo in net passive yield.",
                        estimatedMonthlyImpactAmount = potentialMonthlyMmfYield,
                        iconName = "TrendingUp"
                    )
                )
            }
        }

        return insights.distinctBy { it.id }.take(3)
    }
}
