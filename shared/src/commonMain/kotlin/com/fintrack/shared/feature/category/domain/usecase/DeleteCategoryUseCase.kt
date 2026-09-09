package com.fintrack.shared.feature.category.domain.usecase

import com.fintrack.shared.feature.category.data.LocalCategoryDataSource
import com.fintrack.shared.feature.category.domain.repository.CategoryRepository
import com.fintrack.shared.feature.core.util.Result

class DeleteCategoryUseCase(
    private val repository: CategoryRepository,
    private val localDataSource: LocalCategoryDataSource
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        return when (val result = repository.deleteCategory(id)) {
            is Result.Success -> {
                localDataSource.removeCategory(id)
                Result.Success(Unit)
            }
            is Result.Error -> Result.Error(result.exception)
            is Result.Loading -> Result.Loading
        }
    }
}