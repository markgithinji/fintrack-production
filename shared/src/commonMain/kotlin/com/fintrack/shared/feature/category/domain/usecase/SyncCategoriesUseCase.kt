package com.fintrack.shared.feature.category.domain.usecase

import com.fintrack.shared.feature.category.data.LocalCategoryDataSource
import com.fintrack.shared.feature.category.domain.repository.CategoryRepository
import com.fintrack.shared.feature.core.util.Result

class SyncCategoriesUseCase(
    private val categoryRepository: CategoryRepository,
    private val localDataSource: LocalCategoryDataSource
) {
    suspend operator fun invoke(): Result<Unit> {
        val categoriesResult = categoryRepository.getCategories()
        if (categoriesResult is Result.Error) return Result.Error(categoriesResult.exception)
        
        val rulesResult = categoryRepository.getCategoryRules()
        if (rulesResult is Result.Error) return Result.Error(rulesResult.exception)

        if (categoriesResult is Result.Success && rulesResult is Result.Success) {
            localDataSource.updateCategories(categoriesResult.data)
            localDataSource.updateRules(rulesResult.data)
            return Result.Success(Unit)
        }
        
        return Result.Loading
    }
}