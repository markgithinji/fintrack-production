package com.fintrack.shared.feature.category.domain.usecase

import com.fintrack.shared.feature.category.data.LocalCategoryDataSource
import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.category.domain.repository.FakeCategoryRepository
import com.fintrack.shared.feature.core.util.Result
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class DeleteCategoryUseCaseTest {

    private val fakeRepo = FakeCategoryRepository()
    private val dataSource = LocalCategoryDataSource()
    private val useCase = DeleteCategoryUseCase(fakeRepo, dataSource)

    @Test
    fun `deletes category successfully and removes from local data source`() = runTest {
        val cat = Category(id = "1", name = "Test", isExpense = true)
        dataSource.addCategory(cat)

        val result = useCase("1")
        assertTrue(result is Result.Success)
        assertTrue(dataSource.categories.value.none { it.id == "1" })
    }
}
