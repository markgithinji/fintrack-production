package com.fintrack.shared.feature.planning.domain.usecase

import com.fintrack.shared.feature.planning.domain.model.Debt
import com.fintrack.shared.feature.planning.domain.model.DebtPayoffPlan
import com.fintrack.shared.feature.planning.domain.model.DebtStrategy
import com.fintrack.shared.feature.planning.domain.model.DebtWithSchedule
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

class CalculateDebtPayoffPlanUseCase {

    operator fun invoke(
        debts: List<Debt>,
        strategy: DebtStrategy,
        extraMonthlyPayment: Double = 0.0
    ): DebtPayoffPlan {
        val activeDebts = debts.filter { it.balance > 0 }
        if (activeDebts.isEmpty()) {
            return DebtPayoffPlan(
                strategy = strategy,
                totalBalance = 0.0,
                totalInterestToPay = 0.0,
                interestSavedVsOtherStrategy = 0.0,
                estimatedPayoffMonths = 0,
                estimatedFreedomDate = "Debt Free!",
                orderedDebts = emptyList()
            )
        }

        val snowballPlan = simulateAmortization(activeDebts, DebtStrategy.SNOWBALL, extraMonthlyPayment)
        val avalanchePlan = simulateAmortization(activeDebts, DebtStrategy.AVALANCHE, extraMonthlyPayment)

        val targetPlan = if (strategy == DebtStrategy.SNOWBALL) snowballPlan else avalanchePlan
        val otherPlan = if (strategy == DebtStrategy.SNOWBALL) avalanchePlan else snowballPlan

        val interestSavedVsOther = (otherPlan.totalInterestToPay - targetPlan.totalInterestToPay).coerceAtLeast(0.0)

        return targetPlan.copy(interestSavedVsOtherStrategy = interestSavedVsOther)
    }

    private fun simulateAmortization(
        debts: List<Debt>,
        strategy: DebtStrategy,
        extraMonthlyPayment: Double
    ): DebtPayoffPlan {
        val sortedDebts = if (strategy == DebtStrategy.SNOWBALL) {
            debts.sortedBy { it.balance }
        } else {
            debts.sortedByDescending { it.interestRate }
        }

        val totalBalance = sortedDebts.sumOf { it.balance }

        class SimDebt(val debt: Debt, var balance: Double)

        val simList = sortedDebts.map { SimDebt(it, it.balance) }
        val interestPaidPerDebt = mutableMapOf<String, Double>()
        val payoffMonthsPerDebt = mutableMapOf<String, Int>()

        sortedDebts.forEach {
            interestPaidPerDebt[it.id] = 0.0
            payoffMonthsPerDebt[it.id] = 0
        }

        var currentMonth = 0
        val maxSimMonths = 360

        while (simList.any { it.balance > 0 } && currentMonth < maxSimMonths) {
            currentMonth++
            var availableExtra = extraMonthlyPayment

            simList.filter { it.balance > 0 }.forEach { sim ->
                val monthlyRate = (sim.debt.interestRate / 100.0) / 12.0
                val monthlyInterest = sim.balance * monthlyRate
                sim.balance += monthlyInterest
                interestPaidPerDebt[sim.debt.id] = (interestPaidPerDebt[sim.debt.id] ?: 0.0) + monthlyInterest

                val minPay = sim.debt.minimumPayment.coerceAtMost(sim.balance)
                sim.balance -= minPay

                if (sim.balance <= 0.001) {
                    sim.balance = 0.0
                    if (payoffMonthsPerDebt[sim.debt.id] == 0) {
                        payoffMonthsPerDebt[sim.debt.id] = currentMonth
                    }
                }
            }

            for (sim in simList) {
                if (sim.balance <= 0) continue

                if (availableExtra > 0) {
                    val extraPay = availableExtra.coerceAtMost(sim.balance)
                    sim.balance -= extraPay
                    availableExtra -= extraPay

                    if (sim.balance <= 0.001) {
                        sim.balance = 0.0
                        if (payoffMonthsPerDebt[sim.debt.id] == 0) {
                            payoffMonthsPerDebt[sim.debt.id] = currentMonth
                        }
                    }
                }

                if (availableExtra <= 0) break
            }
        }

        val totalInterestPaid = interestPaidPerDebt.values.sum()
        val totalMonths = payoffMonthsPerDebt.values.maxOrNull() ?: currentMonth

        val today = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val freedomDate = today + DatePeriod(months = totalMonths)
        val freedomDateStr = "${freedomDate.month.name.take(3)} ${freedomDate.year}"

        val orderedDebtsWithSchedule = sortedDebts.mapIndexed { index, debt ->
            val months = payoffMonthsPerDebt[debt.id] ?: totalMonths
            val debtFreedom = today + DatePeriod(months = months)
            val debtFreedomStr = "${debtFreedom.month.name.take(3)} ${debtFreedom.year}"

            DebtWithSchedule(
                debt = debt,
                payOffRank = index + 1,
                estimatedPayoffMonths = months,
                estimatedFreedomDate = debtFreedomStr,
                totalInterestPaid = interestPaidPerDebt[debt.id] ?: 0.0
            )
        }

        return DebtPayoffPlan(
            strategy = strategy,
            totalBalance = totalBalance,
            totalInterestToPay = totalInterestPaid,
            interestSavedVsOtherStrategy = 0.0,
            estimatedPayoffMonths = totalMonths,
            estimatedFreedomDate = freedomDateStr,
            orderedDebts = orderedDebtsWithSchedule
        )
    }
}
