package com.fintrack.shared.feature.category.ui

import com.fintrack.shared.feature.category.data.LocalCategoryDataSource
import com.fintrack.shared.feature.category.domain.repository.FakeCategoryRepository
import com.fintrack.shared.feature.category.domain.usecase.AddCategoryUseCase
import com.fintrack.shared.feature.category.domain.usecase.DeleteCategoryUseCase
import com.fintrack.shared.feature.category.domain.usecase.SyncCategoriesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryManagementViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeRepo = FakeCategoryRepository()
    private val dataSource = LocalCategoryDataSource()
    private val syncUseCase = SyncCategoriesUseCase(fakeRepo, dataSource)
    private val addUseCase = AddCategoryUseCase(fakeRepo, dataSource)
    private val deleteUseCase = DeleteCategoryUseCase(fakeRepo, dataSource)

    private lateinit var viewModel: CategoryManagementViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CategoryManagementViewModel(
            localCategoryDataSource = dataSource,
            syncCategoriesUseCase = syncUseCase,
            addCategoryUseCase = addUseCase,
            deleteCategoryUseCase = deleteUseCase,
            categoryRepository = fakeRepo
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state contains preset categories`() = runTest(testDispatcher) {
        val state = viewModel.state.value
        assertTrue(state.categories.isNotEmpty())
    }
}
