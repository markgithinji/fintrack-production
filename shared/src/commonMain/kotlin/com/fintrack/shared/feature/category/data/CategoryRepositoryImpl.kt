package com.fintrack.shared.feature.category.data

import com.fintrack.shared.feature.category.data.model.toDomain
import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.category.domain.model.CategoryRule
import com.fintrack.shared.feature.category.domain.repository.CategoryRepository
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.core.util.safeApiCall

class CategoryRepositoryImpl(
    private val categoryApi: CategoryApi,
    private val localDataSource: LocalCategoryDataSource
) : CategoryRepository {

    override suspend fun getCategories(): Result<List<Category>> = safeApiCall {
        val dtos = categoryApi.getCategories()
        dtos.map { it.toDomain() }
    }

    override suspend fun getCategoryRules(): Result<List<CategoryRule>> {
        localDataSource.rules?.let { return Result.Success(it) }
        return safeApiCall {
            categoryApi.getCategoryRules().also { 
                localDataSource.updateRules(it)
            }
        }
    }

    override suspend fun addCategory(name: String, isExpense: Boolean, iconName: String?): Result<Category> = safeApiCall {
        val dto = categoryApi.addCategory(name, isExpense, iconName)
        dto.toDomain()
    }

    override suspend fun deleteCategory(id: String): Result<Unit> = safeApiCall {
        categoryApi.deleteCategory(id)
    }
}
