package com.fintrack.shared.feature.category.domain.usecase

import com.fintrack.shared.feature.category.data.LocalCategoryDataSource
import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.category.domain.repository.CategoryRepository
import com.fintrack.shared.feature.core.util.Result

class AddCategoryUseCase(
    private val categoryRepository: CategoryRepository,
    private val localDataSource: LocalCategoryDataSource
) {
    suspend operator fun invoke(name: String, isExpense: Boolean, iconName: String? = null): Result<Category> {
        return when (val result = categoryRepository.addCategory(name, isExpense, iconName)) {
            is Result.Success -> {
                localDataSource.addCategory(result.data)
                Result.Success(result.data)
            }
            is Result.Error -> Result.Error(result.exception)
            is Result.Loading -> Result.Loading
        }
    }
}