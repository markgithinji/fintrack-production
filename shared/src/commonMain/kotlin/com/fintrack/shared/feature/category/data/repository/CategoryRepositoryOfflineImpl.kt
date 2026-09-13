package com.fintrack.shared.feature.category.data.repository

import com.fintrack.shared.db.FintrackDatabase
import com.fintrack.shared.feature.category.data.model.toDomain
import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.category.domain.model.CategoryRule
import com.fintrack.shared.feature.category.domain.repository.CategoryRepository
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.core.util.randomUUID
import com.fintrack.shared.feature.core.util.DateTimeHelper
import com.fintrack.shared.feature.core.logger.KMPLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class CategoryRepositoryOfflineImpl(
    private val database: FintrackDatabase,
    private val logger: KMPLogger
) : CategoryRepository {

    private val queries = database.fintrackDatabaseQueries
    private val offlineUserId = "offline_user"
    private val TAG = "CategoryRepo"

    override suspend fun getCategories(): Result<List<Category>> = withContext(Dispatchers.IO) {
        try {
            val categories = queries.selectAllCategories(offlineUserId).executeAsList().map { it.toDomain() }
            Result.Success(categories)
        } catch (e: Exception) {
            logger.error(TAG, "Error fetching categories", e)
            Result.Error(e)
        }
    }

    override suspend fun getCategoryRules(): Result<List<CategoryRule>> = Result.Success(emptyList())

    override suspend fun addCategory(name: String, isExpense: Boolean, iconName: String?): Result<Category> = withContext(Dispatchers.IO) {
        try {
            val category = Category(
                id = randomUUID(),
                name = name,
                isExpense = isExpense,
                iconName = iconName,
                isDefault = false
            )
            queries.insertCategory(
                id = category.id,
                userId = offlineUserId,
                name = category.name,
                isExpense = if (category.isExpense) 1L else 0L,
                iconName = category.iconName,
                isDefault = if (category.isDefault) 1L else 0L,
                createdAt = DateTimeHelper.now()
            )
            Result.Success(category)
        } catch (e: Exception) {
            logger.error(TAG, "Error adding category $name", e)
            Result.Error(e)
        }
    }

    override suspend fun deleteCategory(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.transaction {
                val category = queries.selectCategoryById(id).executeAsOneOrNull() ?: return@transaction
                if (category.isDefault != 0L) return@transaction // Safety check
                
                // Backend Port: Reassign transactions instead of deleting them (Cascade Safety)
                val fallbackName = if (category.isExpense != 0L) "Misc" else "Other Income"
                val fallbackCategory = queries.selectAllCategories(offlineUserId).executeAsList()
                    .find { it.name.equals(fallbackName, ignoreCase = true) && it.isExpense == category.isExpense }
                
                if (fallbackCategory != null) {
                    queries.reassignTransactions(
                        newCategoryId = fallbackCategory.id,
                        oldCategoryId = id,
                        userId = offlineUserId
                    )
                }

                // Handle budgets
                val budgets = queries.selectAllBudgets(offlineUserId).executeAsList()
                budgets.forEach { row ->
                    val ids = row.categoryIds.split(",").filter { it.isNotEmpty() }.toMutableList()
                    if (ids.contains(id)) {
                        ids.remove(id)
                        if (ids.isEmpty()) {
                            queries.deleteBudget(row.id)
                        } else {
                            queries.insertBudget(
                                id = row.id,
                                userId = row.userId,
                                name = row.name,
                                limitAmount = row.limitAmount,
                                isExpense = row.isExpense,
                                startDate = row.startDate,
                                endDate = row.endDate,
                                categoryIds = ids.joinToString(","),
                                accountIds = row.accountIds,
                                createdAt = row.createdAt
                            )
                        }
                    }
                }

                queries.deleteCategory(id)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.error(TAG, "Error deleting category $id", e)
            Result.Error(e)
        }
    }
}
