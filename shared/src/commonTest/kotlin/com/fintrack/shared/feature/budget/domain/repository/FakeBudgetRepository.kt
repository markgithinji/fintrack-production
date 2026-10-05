package com.fintrack.shared.feature.budget.domain.repository

import com.fintrack.shared.feature.budget.domain.model.Budget
import com.fintrack.shared.feature.budget.domain.model.BudgetWithStatus
import com.fintrack.shared.feature.core.util.Result

class FakeBudgetRepository : BudgetRepository {
    override suspend fun getBudgets(accountId: String?): Result<List<BudgetWithStatus>> = Result.Success(emptyList())
    override suspend fun getBudgetById(id: String): Result<BudgetWithStatus> = Result.Error(Exception("Not found"))
    override suspend fun addOrUpdateBudget(budget: Budget): Result<Budget> = Result.Success(budget)
    override suspend fun deleteBudget(id: String): Result<Unit> = Result.Success(Unit)
    override suspend fun deleteAllBudgets(accountIds: List<String>?): Result<Unit> = Result.Success(Unit)
}
