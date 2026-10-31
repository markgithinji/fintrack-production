package com.fintrack.shared.feature.planning.ui

import com.fintrack.shared.feature.budget.domain.repository.FakeBudgetRepository
import com.fintrack.shared.feature.budget.domain.repository.FakeSavingsGoalRepository
import com.fintrack.shared.feature.budget.domain.usecase.BudgetValidationUseCase
import com.fintrack.shared.feature.budget.domain.usecase.GetSavingsGoalsUseCase
import com.fintrack.shared.feature.category.data.LocalCategoryDataSource
import com.fintrack.shared.feature.category.domain.repository.FakeCategoryRepository
import com.fintrack.shared.feature.category.domain.usecase.SyncCategoriesUseCase
import com.fintrack.shared.feature.transaction.domain.repository.FakeTransactionRepository
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PlanningViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeRepo = FakeBudgetRepository()
    private val validationUseCase = BudgetValidationUseCase()
    private val localCategoryDataSource = LocalCategoryDataSource()
    private val syncCategoriesUseCase = SyncCategoriesUseCase(FakeCategoryRepository(), localCategoryDataSource)

    private val fakeSavingsGoalRepo = FakeSavingsGoalRepository()
    private val fakeTransactionRepo = FakeTransactionRepository()
    private val getSavingsGoalsUseCase = GetSavingsGoalsUseCase(fakeSavingsGoalRepo, fakeTransactionRepo)

    private lateinit var viewModel: PlanningViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = PlanningViewModel(
            budgetRepository = fakeRepo,
            savingsGoalRepository = fakeSavingsGoalRepo,
            getSavingsGoalsUseCase = getSavingsGoalsUseCase,
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

    @Test
    fun `selectTab updates selectedTab state flow across all planning tabs`() = runTest(testDispatcher) {
        viewModel.selectTab(PlanningTab.SAVINGS_GOALS)
        assertEquals(PlanningTab.SAVINGS_GOALS, viewModel.selectedTab.value)

        viewModel.selectTab(PlanningTab.ASSETS_PORTFOLIO)
        assertEquals(PlanningTab.ASSETS_PORTFOLIO, viewModel.selectedTab.value)

        viewModel.selectTab(PlanningTab.DEBT_PAYOFF)
        assertEquals(PlanningTab.DEBT_PAYOFF, viewModel.selectedTab.value)

        viewModel.selectTab(PlanningTab.SPENDING_BUDGETS)
        assertEquals(PlanningTab.SPENDING_BUDGETS, viewModel.selectedTab.value)
    }

    @Test
    fun `form setters update form state fields correctly`() = runTest(testDispatcher) {
        viewModel.setName("Monthly Groceries")
        viewModel.setIsExpense(true)

        val formState = viewModel.formState.value
        assertEquals("Monthly Groceries", formState.name)
        assertTrue(formState.isExpense)
    }
}
