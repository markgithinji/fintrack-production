package com.fintrack.shared.feature.category.domain.repository

import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.category.domain.model.CategoryRule
import com.fintrack.shared.feature.core.util.Result

open class FakeCategoryRepository : CategoryRepository {
    override suspend fun getCategories(): Result<List<Category>> = Result.Success(emptyList())
    override suspend fun getCategoryRules(): Result<List<CategoryRule>> = Result.Success(emptyList())
    override suspend fun addCategory(name: String, isExpense: Boolean, iconName: String?): Result<Category> =
        Result.Success(Category(id = "1", name = name, isExpense = isExpense))
    override suspend fun deleteCategory(id: String): Result<Unit> = Result.Success(Unit)
    override suspend fun addCategoryRule(keyword: String, categoryId: String, isExpense: Boolean): Result<CategoryRule> =
        Result.Error(NotImplementedError())
    override suspend fun deleteCategoryRule(id: String): Result<Unit> = Result.Success(Unit)
}
