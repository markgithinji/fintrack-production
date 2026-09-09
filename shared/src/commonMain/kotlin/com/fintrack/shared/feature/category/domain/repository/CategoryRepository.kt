package com.fintrack.shared.feature.category.domain.repository

import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.category.domain.model.CategoryRule
import com.fintrack.shared.feature.core.util.Result

interface CategoryRepository {
    suspend fun getCategories(): Result<List<Category>>
    suspend fun getCategoryRules(): Result<List<CategoryRule>>
    suspend fun addCategory(name: String, isExpense: Boolean, iconName: String? = null): Result<Category>
    suspend fun deleteCategory(id: String): Result<Unit>
}
