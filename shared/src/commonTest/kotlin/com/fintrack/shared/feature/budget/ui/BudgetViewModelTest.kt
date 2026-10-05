package com.fintrack.shared.feature.budget.ui

import com.fintrack.shared.feature.budget.domain.repository.FakeBudgetRepository
import com.fintrack.shared.feature.budget.domain.usecase.BudgetValidationUseCase
import com.fintrack.shared.feature.category.data.LocalCategoryDataSource
import com.fintrack.shared.feature.category.domain.repository.FakeCategoryRepository
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
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeRepo = FakeBudgetRepository()
    private val validationUseCase = BudgetValidationUseCase()
    private val localCategoryDataSource = LocalCategoryDataSource()
    private val syncCategoriesUseCase = SyncCategoriesUseCase(FakeCategoryRepository(), localCategoryDataSource)

    private lateinit var viewModel: BudgetViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = BudgetViewModel(
            budgetRepository = fakeRepo,
            validationUseCase = validationUseCase,
            localCategoryDataSource = localCategoryDataSource,
            syncCategoriesUseCase = syncCategoriesUseCase,
            receiptScanner = null
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial form state is empty`() = runTest(testDispatcher) {
        val formState = viewModel.formState.value
        assertEquals("", formState.name)
        assertEquals("", formState.amount)
        assertTrue(formState.selectedCategories.isEmpty())
        assertTrue(formState.selectedAccounts.isEmpty())
    }
}
