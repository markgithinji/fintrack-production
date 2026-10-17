package com.fintrack.shared.feature.budget.domain.repository

import com.fintrack.shared.feature.budget.domain.model.SavingsGoal
import com.fintrack.shared.feature.core.util.Result

interface SavingsGoalRepository {
    suspend fun getSavingsGoals(userId: String = "local_user"): Result<List<SavingsGoal>>
    suspend fun getSavingsGoalById(id: String): Result<SavingsGoal?>
    suspend fun saveSavingsGoal(goal: SavingsGoal): Result<Unit>
    suspend fun updateGoalAmount(id: String, newAmount: Double): Result<Unit>
    suspend fun deleteSavingsGoal(id: String): Result<Unit>
}
