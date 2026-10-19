package com.fintrack.shared.feature.summary.domain.usecase

import com.fintrack.shared.feature.budget.domain.usecase.GetSavingsGoalsUseCase
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.summary.domain.model.AffordabilitySimulationResult
import com.fintrack.shared.feature.summary.domain.model.AffordabilityVerdict
import com.fintrack.shared.feature.summary.domain.model.CashflowRunway

class SimulateAffordabilityUseCase(
    private val getCashflowRunwayUseCase: GetCashflowRunwayUseCase,
    private val getSavingsGoalsUseCase: GetSavingsGoalsUseCase? = null
) {
    suspend operator fun invoke(itemPrice: Double, accountId: String? = null): Result<AffordabilitySimulationResult> {
        if (itemPrice <= 0.0) {
            return Result.Error(IllegalArgumentException("Please enter a valid item price greater than zero."))
        }

        return when (val runwayResult = getCashflowRunwayUseCase(accountId)) {
            is Result.Error -> Result.Error(runwayResult.exception)
            is Result.Loading -> Result.Loading
            is Result.Success -> {
                val runway = runwayResult.data

                // Fetch total active savings goals monthly requirement
                val totalRequiredMonthlySavings = fetchTotalRequiredSavings()

                val result = runSimulation(itemPrice, runway, totalRequiredMonthlySavings)
                Result.Success(result)
            }
        }
    }

    private suspend fun fetchTotalRequiredSavings(): Double {
        if (getSavingsGoalsUseCase == null) return 0.0
        return when (val goalsRes = getSavingsGoalsUseCase()) {
            is Result.Success -> {
                goalsRes.data.filter { !it.goal.isCompleted }.sumOf { it.requiredMonthlyContribution }
            }
            else -> 0.0
        }
    }

    private fun runSimulation(
        itemPrice: Double,
        runway: CashflowRunway,
        requiredMonthlySavings: Double
    ): AffordabilitySimulationResult {
        val currentCash = runway.currentBalance.doubleValue(false)
        val eomBefore = runway.projectedEomBalance.doubleValue(false)
        val billsTotal = runway.upcomingBillsTotal.doubleValue(false)
        val dailyBurn = runway.dailyBurnRate.doubleValue(false)

        val eomAfter = eomBefore - itemPrice
        val cushion = (eomAfter - billsTotal).coerceAtLeast(0.0)

        // Calculate remaining burn days capacity after purchase and bills
        val cashLeftAfterItemAndBills = (currentCash - itemPrice - billsTotal).coerceAtLeast(0.0)
        val remainingBurnDays = if (dailyBurn > 0) {
            (cashLeftAfterItemAndBills / dailyBurn).toInt().coerceAtMost(365)
        } else null

        // Savings Goal Impact
        val savingsGoalImpact = if (requiredMonthlySavings > 0 && eomAfter < requiredMonthlySavings) {
            "Reduces surplus below your KES ${requiredMonthlySavings.toFormattedString()}/mo required savings for active goals."
        } else null

        val verdict: AffordabilityVerdict
        val message: String
        val recommendation: String

        if (itemPrice > currentCash) {
            verdict = AffordabilityVerdict.IMMEDIATE_DEFICIT
            message = "Immediate Cash Deficit: Today's account balance (KES ${currentCash.toFormattedString()}) is less than item price."
            recommendation = "You cannot afford this purchase today without using credit or overdraft."
        } else if (eomAfter < 0.0) {
            verdict = AffordabilityVerdict.RISKY
            message = "High Risk: Will cause an estimated overdraft of KES ${kotlin.math.abs(eomAfter).toFormattedString()} before month-end."
            recommendation = "Postpone this purchase or extend your savings timeline to protect your cash flow."
        } else if (eomAfter < billsTotal * 0.2 || (requiredMonthlySavings > 0 && eomAfter < requiredMonthlySavings)) {
            verdict = AffordabilityVerdict.CAUTION
            message = "Borderline: Leaves a tight month-end cushion of KES ${eomAfter.toFormattedString()}."
            recommendation = if (savingsGoalImpact != null) {
                "Purchase is doable but limits funding your active savings goals this month."
            } else {
                "Purchase is doable but leaves narrow margin for unexpected expenses before next paycheck."
            }
        } else {
            verdict = AffordabilityVerdict.SAFE
            message = "Safe! Leaves a healthy cushion of KES ${eomAfter.toFormattedString()} after scheduled bills."
            recommendation = "Fits comfortably within your cashflow runway without overdraft risk!"
        }

        return AffordabilitySimulationResult(
            itemPrice = itemPrice,
            verdict = verdict,
            currentAccountCash = currentCash,
            projectedEomBalanceBefore = eomBefore,
            projectedEomBalanceAfter = eomAfter,
            cushionRemaining = cushion,
            upcomingBillsTotal = billsTotal,
            remainingDaysInMonth = runway.remainingDaysInMonth,
            remainingBurnDaysAfterPurchase = remainingBurnDays,
            savingsGoalImpactMessage = savingsGoalImpact,
            message = message,
            recommendation = recommendation
        )
    }

    private fun Double.toFormattedString(): String {
        val intVal = this.toLong()
        return intVal.toString().reversed().chunked(3).joinToString(",").reversed()
    }
}
