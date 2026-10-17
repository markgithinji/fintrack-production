package com.fintrack.shared.feature.budget.domain.usecase

import com.fintrack.shared.feature.budget.domain.model.SavingsGoal
import com.fintrack.shared.feature.budget.domain.repository.FakeSavingsGoalRepository
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.transaction.domain.repository.FakeTransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GetSavingsGoalsUseCaseTest {

    private val savingsGoalRepo = FakeSavingsGoalRepository()
    private val transactionRepo = FakeTransactionRepository()
    private val useCase = GetSavingsGoalsUseCase(savingsGoalRepo, transactionRepo)

    @Test
    fun `calculates required monthly savings and feasibility correctly`() = runTest {
        val goal = SavingsGoal(
            id = "goal_1",
            userId = "local_user",
            name = "Emergency Fund",
            targetAmount = 120000.0,
            currentAmount = 20000.0,
            targetDate = "2025-12-31",
            createdAt = "2024-01-01T00:00:00Z"
        )
        savingsGoalRepo.saveSavingsGoal(goal)

        val result = useCase("local_user")
        assertTrue(result is Result.Success)
        val goalCalcs = (result as Result.Success).data
        assertEquals(1, goalCalcs.size)

        val calc = goalCalcs.first()
        assertEquals("Emergency Fund", calc.goal.name)
        assertEquals(100000.0, calc.goal.remainingAmount)
        assertTrue(calc.requiredMonthlyContribution > 0)
    }
}
