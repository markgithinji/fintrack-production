package com.fintrack.shared.feature.budget.data.repository

import com.fintrack.shared.feature.budget.data.model.toCreateRequest
import com.fintrack.shared.feature.budget.data.model.toDomain
import com.fintrack.shared.feature.budget.data.model.toUpdateRequest
import com.fintrack.shared.feature.budget.data.remote.BudgetApi
import com.fintrack.shared.feature.budget.domain.model.Budget
import com.fintrack.shared.feature.budget.domain.model.BudgetWithStatus
import com.fintrack.shared.feature.budget.domain.repository.BudgetRepository
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.core.util.safeApiCall

class BudgetRepositoryImpl(
    private val budgetApi: BudgetApi
) : BudgetRepository {

    override suspend fun getBudgets(
        accountId: String?
    ): Result<List<BudgetWithStatus>> = safeApiCall {
        val budgetsWithStatusDto = budgetApi.getBudgets(limit = 100, offset = 0, accountId = accountId)
        budgetsWithStatusDto.map { it.toDomain() }
    }

    override suspend fun getBudgetById(id: String): Result<BudgetWithStatus> =
        safeApiCall {
            val budgetWithStatusDto = budgetApi.getBudgetById(id)
            budgetWithStatusDto.toDomain()
        }

    override suspend fun addOrUpdateBudget(budget: Budget): Result<Budget> = safeApiCall {
        if (budget.id == null) {
            val createRequest = budget.toCreateRequest()
            val dto = budgetApi.addBudget(createRequest)
            dto.budget.toDomain()
        } else {
            val updateRequest = budget.toUpdateRequest()
            val dto = budgetApi.updateBudget(budget.id, updateRequest)
            dto.budget.toDomain()
        }
    }

    override suspend fun deleteBudget(id: String): Result<Unit> = safeApiCall {
        budgetApi.deleteBudget(id)
    }

    override suspend fun deleteAllBudgets(accountIds: List<String>?): Result<Unit> = safeApiCall {
        budgetApi.deleteAllBudgets(accountIds)
    }
}