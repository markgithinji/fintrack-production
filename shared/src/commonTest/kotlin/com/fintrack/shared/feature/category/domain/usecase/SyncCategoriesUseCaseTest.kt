package com.fintrack.shared.feature.category.domain.usecase

import com.fintrack.shared.feature.category.data.LocalCategoryDataSource
import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.category.domain.model.CategoryRule
import com.fintrack.shared.feature.category.domain.repository.FakeCategoryRepository
import com.fintrack.shared.feature.core.util.Result
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncCategoriesUseCaseTest {

    private class SyncFakeCategoryRepository : FakeCategoryRepository() {
        var categoriesToReturn = listOf(Category(id = "1", name = "Custom", isExpense = true))
        var rulesToReturn = listOf(CategoryRule(id = "r1", keyword = "test", categoryId = "1", isExpense = true))

        override suspend fun getCategories(): Result<List<Category>> = Result.Success(categoriesToReturn)
        override suspend fun getCategoryRules(): Result<List<CategoryRule>> = Result.Success(rulesToReturn)
    }

    @Test
    fun `syncs categories and rules successfully into local data source`() = runTest {
        val fakeRepo = SyncFakeCategoryRepository()
        val dataSource = LocalCategoryDataSource()
        val useCase = SyncCategoriesUseCase(fakeRepo, dataSource)

        val result = useCase()
        assertTrue(result is Result.Success)
        assertTrue(dataSource.categories.value.any { it.name == "Custom" })
        assertEquals(1, dataSource.rules?.size)
    }
}
