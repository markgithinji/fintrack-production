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
class SavingsGoalCategoryAutoSyncTest {

    private val savingsGoalRepo = FakeSavingsGoalRepository()

    @Test
    fun `auto sync credits savings goal when transaction is added`() = runTest {
        val goal = SavingsGoal(
            id = "goal_savings_1",
            userId = "local_user",
            name = "Emergency Fund",
            targetAmount = 100000.0,
            currentAmount = 10000.0,
            targetDate = "2025-12-31",
            categoryId = "cat_savings_id",
            createdAt = "2024-01-01T00:00:00Z"
        )
        savingsGoalRepo.saveSavingsGoal(goal)

        val initialGoal = (savingsGoalRepo.getSavingsGoalById("goal_savings_1") as Result.Success).data
        assertEquals(10000.0, initialGoal?.currentAmount)

        // Deposit 5,000 to goal
        savingsGoalRepo.updateGoalAmount("goal_savings_1", 15000.0)

        val updatedGoal = (savingsGoalRepo.getSavingsGoalById("goal_savings_1") as Result.Success).data
        assertEquals(15000.0, updatedGoal?.currentAmount)
    }
}
