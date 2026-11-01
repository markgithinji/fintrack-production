package com.fintrack.shared.feature.budget.domain.repository

import com.fintrack.shared.feature.budget.domain.model.SavingsGoal
import com.fintrack.shared.feature.core.util.Result

class FakeSavingsGoalRepository : SavingsGoalRepository {
    private val goals = mutableListOf<SavingsGoal>()

    override suspend fun getSavingsGoals(userId: String): Result<List<SavingsGoal>> {
        return Result.Success(goals.toList())
    }

    override suspend fun getSavingsGoalById(id: String): Result<SavingsGoal?> {
        return Result.Success(goals.find { it.id == id })
    }

    override suspend fun saveSavingsGoal(goal: SavingsGoal): Result<Unit> {
        goals.removeAll { it.id == goal.id }
        goals.add(goal)
        return Result.Success(Unit)
    }

    override suspend fun updateGoalAmount(id: String, newAmount: Double): Result<Unit> {
        val index = goals.indexOfFirst { it.id == id }
        if (index != -1) {
            goals[index] = goals[index].copy(currentAmount = newAmount)
        }
        return Result.Success(Unit)
    }

    override suspend fun deleteSavingsGoal(id: String): Result<Unit> {
        goals.removeAll { it.id == id }
        return Result.Success(Unit)
    }
}
