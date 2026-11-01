package com.fintrack.shared.feature.budget.data.repository

import com.fintrack.shared.db.FintrackDatabase
import com.fintrack.shared.db.SavingsGoalEntity
import com.fintrack.shared.feature.budget.domain.model.SavingsGoal
import com.fintrack.shared.feature.budget.domain.repository.SavingsGoalRepository
import com.fintrack.shared.feature.core.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class SavingsGoalRepositoryImpl(
    private val database: FintrackDatabase
) : SavingsGoalRepository {

    init {
        try {
            database.fintrackDatabaseQueries.createSavingsGoalTableIfNotExists()
        } catch (e: Exception) {
            println("[SAVINGS_GOAL_DB_LOG] Error ensuring SavingsGoalEntity table exists: ${e.message}")
        }
    }

    override suspend fun getSavingsGoals(userId: String): Result<List<SavingsGoal>> = withContext(Dispatchers.IO) {
        try {
            val entities = database.fintrackDatabaseQueries.selectAllSavingsGoals(userId).executeAsList()
            Result.Success(entities.map { it.toDomain() })
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getSavingsGoalById(id: String): Result<SavingsGoal?> = withContext(Dispatchers.IO) {
        try {
            val entity = database.fintrackDatabaseQueries.selectSavingsGoalById(id).executeAsOneOrNull()
            Result.Success(entity?.toDomain())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun saveSavingsGoal(goal: SavingsGoal): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.fintrackDatabaseQueries.insertSavingsGoal(
                id = goal.id,
                userId = goal.userId,
                name = goal.name,
                targetAmount = goal.targetAmount,
                currentAmount = goal.currentAmount,
                targetDate = goal.targetDate,
                categoryId = goal.categoryId,
                accountId = goal.accountId,
                iconName = goal.iconName,
                colorHex = goal.colorHex,
                createdAt = goal.createdAt
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateGoalAmount(id: String, newAmount: Double): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.fintrackDatabaseQueries.updateSavingsGoalAmount(newAmount, id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteSavingsGoal(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.fintrackDatabaseQueries.deleteSavingsGoal(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private fun SavingsGoalEntity.toDomain() = SavingsGoal(
        id = id,
        userId = userId,
        name = name,
        targetAmount = targetAmount,
        currentAmount = currentAmount,
        targetDate = targetDate,
        categoryId = categoryId,
        accountId = accountId,
        iconName = iconName,
        colorHex = colorHex,
        createdAt = createdAt
    )
}
