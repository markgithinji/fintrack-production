package com.fintrack.shared.feature.category.domain.usecase

import com.fintrack.shared.feature.category.data.LocalCategoryDataSource
import com.fintrack.shared.feature.category.domain.repository.FakeCategoryRepository
import com.fintrack.shared.feature.core.util.Result
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AddCategoryUseCaseTest {

    private val fakeRepo = FakeCategoryRepository()
    private val dataSource = LocalCategoryDataSource()
    private val useCase = AddCategoryUseCase(fakeRepo, dataSource)

    @Test
    fun `adds category successfully and updates local data source`() = runTest {
        val result = useCase(name = "Utilities", isExpense = true, iconName = "bolt")
        assertTrue(result is Result.Success)
        assertEquals("Utilities", result.data.name)
        assertTrue(dataSource.categories.value.any { it.name == "Utilities" })
    }
}
