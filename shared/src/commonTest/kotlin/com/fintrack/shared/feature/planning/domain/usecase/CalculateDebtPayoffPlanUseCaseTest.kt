package com.fintrack.shared.feature.planning.domain.usecase

import com.fintrack.shared.feature.planning.domain.model.Debt
import com.fintrack.shared.feature.planning.domain.model.DebtStrategy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CalculateDebtPayoffPlanUseCaseTest {

    private val useCase = CalculateDebtPayoffPlanUseCase()

    private val sampleDebts = listOf(
        Debt(
            id = "d1",
            name = "Fuliza Overdraft",
            balance = 5000.0,
            initialBalance = 5000.0,
            interestRate = 28.0,
            minimumPayment = 1000.0,
            createdAt = "2026-01-01"
        ),
        Debt(
            id = "d2",
            name = "Equity Bank Loan",
            balance = 150000.0,
            initialBalance = 150000.0,
            interestRate = 14.0,
            minimumPayment = 4500.0,
            createdAt = "2026-01-01"
        )
    )

    @Test
    fun `Snowball strategy orders debts by balance ascending`() {
        val plan = useCase(sampleDebts, DebtStrategy.SNOWBALL)
        assertEquals(2, plan.orderedDebts.size)
        assertEquals("Fuliza Overdraft", plan.orderedDebts[0].debt.name)
        assertEquals("Equity Bank Loan", plan.orderedDebts[1].debt.name)
    }

    @Test
    fun `Avalanche strategy orders debts by interest rate descending`() {
        val plan = useCase(sampleDebts, DebtStrategy.AVALANCHE)
        assertEquals(2, plan.orderedDebts.size)
        assertEquals("Fuliza Overdraft", plan.orderedDebts[0].debt.name)
        assertEquals("Equity Bank Loan", plan.orderedDebts[1].debt.name)
    }

    @Test
    fun `Extra monthly payment accelerates payoff and reduces interest`() {
        val planWithoutExtra = useCase(sampleDebts, DebtStrategy.AVALANCHE, extraMonthlyPayment = 0.0)
        val planWithExtra = useCase(sampleDebts, DebtStrategy.AVALANCHE, extraMonthlyPayment = 5000.0)

        assertTrue(planWithExtra.estimatedPayoffMonths < planWithoutExtra.estimatedPayoffMonths)
        assertTrue(planWithExtra.totalInterestToPay < planWithoutExtra.totalInterestToPay)
    }
}
