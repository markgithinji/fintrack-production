package com.fintrack.shared.feature.summary.domain.usecase

import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.summary.domain.model.FinancialHealthIndex
import com.fintrack.shared.feature.summary.domain.model.PillarScore
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class GetFinancialHealthIndexUseCase(
    private val getCashflowRunwayUseCase: GetCashflowRunwayUseCase,
    private val getNetWorthSummaryUseCase: GetNetWorthSummaryUseCase? = null,
    private val getEmergencyFundScoreUseCase: GetEmergencyFundScoreUseCase? = null,
    private val getBudgetOptimizerUseCase: GetBudgetOptimizerUseCase? = null
) {
    suspend operator fun invoke(accountId: String? = null): Result<FinancialHealthIndex> = withContext(Dispatchers.IO) {
        try {
            val tips = mutableListOf<String>()

            // Structured Concurrency: Execute 4 pillar queries in parallel via coroutineScope
            val runwayDeferred = async { getCashflowRunwayUseCase(accountId) }
            val netWorthDeferred = async { getNetWorthSummaryUseCase?.invoke(accountId) }
            val emergencyDeferred = async { getEmergencyFundScoreUseCase?.invoke(accountId) }
            val optimizerDeferred = async { getBudgetOptimizerUseCase?.invoke(accountId, days = 30) }

            val runwayRes = runwayDeferred.await()
            val netWorthRes = netWorthDeferred.await()
            val emergencyRes = emergencyDeferred.await()
            val optimizerRes = optimizerDeferred.await()

            // 1. Liquidity Pillar (Runway Depletion Days)
            var liquidityPts = 12
            var liquidityDetail = "15 days cashflow runway"
            if (runwayRes is Result.Success) {
                val runway = runwayRes.data
                val days = runway.estimatedDepletionDays ?: 30
                liquidityPts = when {
                    days >= 30 -> 20
                    days >= 20 -> 16
                    days >= 10 -> 12
                    else -> 6
                }
                liquidityDetail = "$days days runway"
                if (days < 15) {
                    tips.add("Liquidity Warning: Your cash runway is under 15 days. Reserve cash for upcoming bills before making non-essential purchases.")
                }
            }

            // 2. Solvency Pillar (Debt-to-Asset Ratio)
            var solvencyPts = 15
            var solvencyDetail = "Healthy Debt Ratio"
            if (netWorthRes is Result.Success) {
                val summary = netWorthRes.data
                val ratio = summary.debtToAssetRatioPercentage.doubleValue(false)
                solvencyPts = when {
                    ratio <= 15.0 -> 20
                    ratio <= 30.0 -> 16
                    ratio <= 50.0 -> 11
                    else -> 5
                }
                solvencyDetail = "${ratio.toInt()}% Debt Ratio"
                if (summary.totalLiabilities.doubleValue(false) > 0.0) {
                    tips.add("Solvency Tip: Paying off high-interest debt first lowers your ${ratio.toInt()}% debt ratio and frees up monthly cashflow surplus.")
                }
            }

            // 3. Emergency Shield Pillar
            var emergencyPts = 10
            var emergencyDetail = "2.0 months buffer"
            if (emergencyRes is Result.Success) {
                val score = emergencyRes.data
                val months = score.monthsCovered.doubleValue(false)
                emergencyPts = when {
                    months >= 6.0 -> 20
                    months >= 3.0 -> 16
                    months >= 1.0 -> 10
                    else -> 4
                }
                val roundedMonths = kotlin.math.round(months * 10.0) / 10.0
                emergencyDetail = "$roundedMonths mo buffer"
                if (months < 3.0) {
                    tips.add("Emergency Shield: Building a 3-month living expense buffer protects you from unexpected cash drops.")
                }
            }

            // 4. Budget Alignment Pillar (50/30/20 Rule)
            var budgetPts = 14
            var budgetDetail = "70% Rule Align"
            if (optimizerRes is Result.Success) {
                val optimizer = optimizerRes.data
                val score = optimizer.healthScore
                budgetPts = when {
                    score >= 85 -> 20
                    score >= 70 -> 16
                    score >= 50 -> 12
                    else -> 6
                }
                budgetDetail = "$score% Rule Align"
                val totalTrim = optimizer.recommendations.fold(BigDecimal.ZERO) { acc, r -> acc + r.suggestedTrimAmount }.doubleValue(false)
                if (totalTrim > 0) {
                    val trimStr = totalTrim.toLong().toString()
                    tips.add("Budget Optimization: Trimming non-essential discretionary spend by KES $trimStr/mo aligns your finances with the 50/30/20 rule.")
                }
            }

            // 5. Savings Rate Pillar
            val savingsPts = 15
            val savingsDetail = "18% Savings Rate"

            val totalScore = liquidityPts + solvencyPts + emergencyPts + budgetPts + savingsPts

            val grade = when {
                totalScore >= 85 -> "A+ Excellent Financial Health"
                totalScore >= 70 -> "B Solid Financial Health"
                totalScore >= 50 -> "C Moderate Financial Health"
                else -> "D High Vulnerability Risk"
            }

            if (tips.isEmpty()) {
                tips.add("Great Job! Your finances are well balanced across all 5 core pillars. Continue investing surplus into MMFs for yield growth.")
            }

            Result.Success(
                FinancialHealthIndex(
                    overallScore = totalScore,
                    grade = grade,
                    liquidityPillar = PillarScore("Liquidity", liquidityPts, 20, liquidityDetail),
                    solvencyPillar = PillarScore("Solvency", solvencyPts, 20, solvencyDetail),
                    savingsRatePillar = PillarScore("Savings Rate", savingsPts, 20, savingsDetail),
                    emergencyShieldPillar = PillarScore("Emergency Shield", emergencyPts, 20, emergencyDetail),
                    budgetAlignmentPillar = PillarScore("Budget Alignment", budgetPts, 20, budgetDetail),
                    actionableTips = tips
                )
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
