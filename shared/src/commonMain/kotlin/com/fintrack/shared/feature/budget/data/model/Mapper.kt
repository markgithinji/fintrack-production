package com.fintrack.shared.feature.budget.data.model

import com.fintrack.shared.feature.budget.domain.model.Budget
import com.fintrack.shared.feature.budget.domain.model.BudgetStatus
import com.fintrack.shared.feature.budget.domain.model.BudgetWithStatus
import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.category.domain.model.fromId

fun BudgetDto.toDomain(): Budget =
    Budget(
        id = id,
        accountIds = accountIds,
        name = name,
        categories = categoryIds.map { id -> Category.fromId(id = id, isExpense = isExpense) },
        limit = limit,
        isExpense = isExpense,
        startDate = startDate,
        endDate = endDate
    )

fun Budget.toCreateRequest(): CreateBudgetRequest = CreateBudgetRequest(
    accountIds = this.accountIds,
    name = this.name,
    categoryIds = this.categories.map { it.id },
    limit = this.limit,
    isExpense = this.isExpense,
    startDate = this.startDate,
    endDate = this.endDate
)

fun Budget.toUpdateRequest(): UpdateBudgetRequest = UpdateBudgetRequest(
    accountIds = this.accountIds,
    name = this.name,
    categoryIds = this.categories.map { it.id },
    limit = this.limit,
    isExpense = this.isExpense,
    startDate = this.startDate,
    endDate = this.endDate
)

fun BudgetWithStatusDto.toDomain(): BudgetWithStatus {
    return BudgetWithStatus(
        budget = budget.toDomain(),
        status = BudgetStatus(
            spent = status.spent,
            remaining = status.remaining,
            percentageUsed = status.percentageUsed,
            isExceeded = status.isExceeded
        )
    )
}
