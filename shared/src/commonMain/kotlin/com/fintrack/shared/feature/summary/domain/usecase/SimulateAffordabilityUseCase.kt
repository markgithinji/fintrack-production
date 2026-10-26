package com.fintrack.shared.feature.summary.domain.usecase

import com.fintrack.shared.feature.budget.domain.usecase.GetSavingsGoalsUseCase
import com.fintrack.shared.feature.core.util.DateTimeHelper
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.planning.domain.repository.AssetRepository
import com.fintrack.shared.feature.planning.domain.repository.DebtRepository
import com.fintrack.shared.feature.summary.domain.model.AffordabilitySimulationResult
import com.fintrack.shared.feature.summary.domain.model.AffordabilityVerdict
import com.fintrack.shared.feature.summary.domain.model.CashflowRunway
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

class SimulateAffordabilityUseCase(
    private val getCashflowRunwayUseCase: GetCashflowRunwayUseCase,
    private val getSavingsGoalsUseCase: GetSavingsGoalsUseCase? = null,
    private val assetRepository: AssetRepository? = null,
    private val debtRepository: DebtRepository? = null
) {
    suspend operator fun invoke(
        itemPrice: Double,
        accountId: String? = null,
        currencySymbol: String = "KES"
    ): Result<AffordabilitySimulationResult> {
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

                // Fetch liquid MMF & asset reserves
                val liquidAssetsTotal = fetchLiquidAssetsTotal(accountId)

                // Fetch active debts total
                val activeDebtsTotal = fetchActiveDebtsTotal(accountId)

                val result = runSimulation(
                    itemPrice = itemPrice,
                    runway = runway,
                    requiredMonthlySavings = totalRequiredMonthlySavings,
                    liquidAssetsTotal = liquidAssetsTotal,
                    activeDebtsTotal = activeDebtsTotal,
                    currencySymbol = currencySymbol
                )
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

    private suspend fun fetchLiquidAssetsTotal(accountId: String? = null): Double {
        if (assetRepository == null) return 0.0
        val targetAccountIds = accountId?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        return when (val assetsRes = assetRepository.getAssets()) {
            is Result.Success -> assetsRes.data.filter { asset ->
                asset.isLiquid && (targetAccountIds.isEmpty() || asset.accountId == null || targetAccountIds.contains(asset.accountId))
            }.sumOf { it.currentBalance }
            else -> 0.0
        }
    }

    private suspend fun fetchActiveDebtsTotal(accountId: String? = null): Double {
        if (debtRepository == null) return 0.0
        val targetAccountIds = accountId?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        return when (val debtsRes = debtRepository.getDebts()) {
            is Result.Success -> debtsRes.data.filter { debt ->
                targetAccountIds.isEmpty() || debt.accountId == null || targetAccountIds.contains(debt.accountId)
            }.sumOf { it.balance }
            else -> 0.0
        }
    }

    private fun runSimulation(
        itemPrice: Double,
        runway: CashflowRunway,
        requiredMonthlySavings: Double,
        liquidAssetsTotal: Double,
        activeDebtsTotal: Double,
        currencySymbol: String
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
            "Reduces surplus below your $currencySymbol ${requiredMonthlySavings.toFormattedString()}/mo required savings for active goals."
        } else null

        // "When Can I Buy This?" Readiness Timeline Calculation
        val tz = TimeZone.currentSystemDefault()
        val todayDate = DateTimeHelper.now().toLocalDateTime(tz).date
        val remainingCashNeeded = (itemPrice - currentCash).coerceAtLeast(0.0)

        // Net uncommitted monthly surplus (projected EOM cash balance minus required monthly goal contributions)
        val monthlySurplus = (eomBefore - requiredMonthlySavings).coerceAtLeast(0.0)
        val estimatedMonths: Int?
        val readinessDateStr: String?
        val reqMonthlySavings: Double?
        val readinessMsg: String?

        if (remainingCashNeeded == 0.0) {
            estimatedMonths = 0
            readinessDateStr = "Today"
            reqMonthlySavings = 0.0
            readinessMsg = "Ready Today! You currently have 100% of the cash available in your account."
        } else if (monthlySurplus > 0.0) {
            val monthsDouble = remainingCashNeeded / monthlySurplus
            val monthsInt = kotlin.math.ceil(monthsDouble).toInt().coerceAtLeast(1)
            estimatedMonths = monthsInt

            val futureDate = todayDate.plus(DatePeriod(months = monthsInt))
            val monthTitle = futureDate.month.name.lowercase().replaceFirstChar { it.uppercase() }
            readinessDateStr = "$monthTitle ${futureDate.year}"

            reqMonthlySavings = kotlin.math.round((remainingCashNeeded / monthsInt) * 10.0) / 10.0
            readinessMsg = "Ready in ~$monthsInt months ($readinessDateStr)! Saving $currencySymbol ${reqMonthlySavings.toFormattedString()}/mo reaches 100% cash readiness without taking on debt."
        } else {
            estimatedMonths = null
            readinessDateStr = null
            reqMonthlySavings = null
            readinessMsg = "Your projected month-end surplus is currently zero. Trimming discretionary spending will create surplus cashflow to reach this goal."
        }

        val verdict: AffordabilityVerdict
        val message: String
        val recommendation: String

        if (itemPrice > currentCash) {
            if (liquidAssetsTotal >= itemPrice) {
                verdict = AffordabilityVerdict.CAUTION
                message = "Checking Cash Deficit: Today's checking cash ($currencySymbol ${currentCash.toFormattedString()}) is less than item price, but you have $currencySymbol ${liquidAssetsTotal.toFormattedString()} in MMFs/Assets."
                recommendation = "Doable via MMF withdrawal! Transfer $currencySymbol ${itemPrice.toFormattedString()} from your MMF Asset to cover this purchase without taking on high-interest loan debt."
            } else {
                verdict = AffordabilityVerdict.IMMEDIATE_DEFICIT
                message = "Immediate Cash Deficit: Today's available cash ($currencySymbol ${currentCash.toFormattedString()}) is less than item price."
                recommendation = if (activeDebtsTotal > 0) {
                    "You have $currencySymbol ${activeDebtsTotal.toFormattedString()} in active debts. Avoid taking on new credit or overdrafts for non-essential purchases."
                } else {
                    "You cannot afford this purchase today without using credit or overdraft."
                }
            }
        } else if (eomAfter < 0.0) {
            verdict = AffordabilityVerdict.RISKY
            message = "High Risk: Will cause an estimated overdraft of $currencySymbol ${kotlin.math.abs(eomAfter).toFormattedString()} before month-end."
            recommendation = "Postpone this purchase or extend your savings timeline to protect your cash flow."
        } else if (eomAfter < billsTotal * 0.2 || (requiredMonthlySavings > 0 && eomAfter < requiredMonthlySavings)) {
            verdict = AffordabilityVerdict.CAUTION
            message = "Borderline: Leaves a tight month-end cushion of $currencySymbol ${eomAfter.toFormattedString()}."
            recommendation = if (savingsGoalImpact != null) {
                "Purchase is doable but limits funding your active savings goals this month."
            } else {
                "Purchase is doable but leaves narrow margin for unexpected expenses before next paycheck."
            }
        } else {
            verdict = AffordabilityVerdict.SAFE
            message = "Safe! Leaves a healthy cushion of $currencySymbol ${eomAfter.toFormattedString()} after scheduled bills."
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
            requiredMonthlySavingsTotal = requiredMonthlySavings,
            liquidAssetsTotal = liquidAssetsTotal,
            activeDebtsTotal = activeDebtsTotal,
            remainingDaysInMonth = runway.remainingDaysInMonth,
            remainingBurnDaysAfterPurchase = remainingBurnDays,
            savingsGoalImpactMessage = savingsGoalImpact,
            estimatedMonthsToReadiness = estimatedMonths,
            targetReadinessDate = readinessDateStr,
            requiredMonthlySavingsForTarget = reqMonthlySavings,
            readinessMessage = readinessMsg,
            message = message,
            recommendation = recommendation
        )
    }

    private fun Double.toFormattedString(): String {
        val intVal = this.toLong()
        return intVal.toString().reversed().chunked(3).joinToString(",").reversed()
    }
}
