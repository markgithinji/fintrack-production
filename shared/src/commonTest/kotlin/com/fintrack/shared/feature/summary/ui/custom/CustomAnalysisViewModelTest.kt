package com.fintrack.shared.feature.summary.ui.custom

import com.fintrack.shared.feature.account.domain.repository.FakeAccountRepository
import com.fintrack.shared.feature.category.data.LocalCategoryDataSource
import com.fintrack.shared.feature.category.domain.repository.FakeCategoryRepository
import com.fintrack.shared.feature.core.util.DateTimeHelper
import com.fintrack.shared.feature.core.util.FakeFileSaver
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.fintrack.shared.feature.transaction.domain.repository.FakeTransactionRepository
import com.ionspin.kotlin.bignum.decimal.BigDecimal
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CustomAnalysisViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val transactionRepository = FakeTransactionRepository()
    private val accountRepository = FakeAccountRepository()
    private val categoryRepository = FakeCategoryRepository()
    private val fakeFileSaver = FakeFileSaver()

    private lateinit var viewModel: CustomAnalysisViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CustomAnalysisViewModel(
            transactionRepository = transactionRepository,
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            fileSaver = fakeFileSaver
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `applyDatePreset updates date range and preset state correctly`() = runTest(testDispatcher) {
        viewModel.applyDatePreset(DatePreset.THIS_YEAR)
        assertEquals(DatePreset.THIS_YEAR, viewModel.state.value.datePreset)

        viewModel.applyDatePreset(DatePreset.ALL_TIME)
        assertEquals(DatePreset.ALL_TIME, viewModel.state.value.datePreset)
        assertNull(viewModel.state.value.startDate)
        assertNull(viewModel.state.value.endDate)
    }

    @Test
    fun `toggleAccountSelection adds and removes account IDs correctly`() = runTest(testDispatcher) {
        viewModel.toggleAccountSelection("acc_1")
        assertTrue(viewModel.state.value.selectedAccountIds.contains("acc_1"))

        viewModel.toggleAccountSelection("acc_1")
        assertFalse(viewModel.state.value.selectedAccountIds.contains("acc_1"))
    }

    @Test
    fun `toggleCategorySelection adds and removes category IDs correctly`() = runTest(testDispatcher) {
        viewModel.toggleCategorySelection("cat_groceries")
        assertTrue(viewModel.state.value.selectedCategoryIds.contains("cat_groceries"))

        viewModel.toggleCategorySelection("cat_groceries")
        assertFalse(viewModel.state.value.selectedCategoryIds.contains("cat_groceries"))
    }

    @Test
    fun `setTransactionTypeFilter updates transaction type filter state`() = runTest(testDispatcher) {
        viewModel.setTransactionTypeFilter(TransactionTypeFilter.EXPENSE_ONLY)
        assertEquals(TransactionTypeFilter.EXPENSE_ONLY, viewModel.state.value.typeFilter)

        viewModel.setTransactionTypeFilter(TransactionTypeFilter.INCOME_ONLY)
        assertEquals(TransactionTypeFilter.INCOME_ONLY, viewModel.state.value.typeFilter)
    }

    @Test
    fun `clearAllFilters resets all custom filters back to defaults`() = runTest(testDispatcher) {
        viewModel.toggleAccountSelection("acc_1")
        viewModel.toggleCategorySelection("cat_1")
        viewModel.setTransactionTypeFilter(TransactionTypeFilter.EXPENSE_ONLY)
        viewModel.setAmountRange(100.0, 5000.0)

        viewModel.clearAllFilters()

        val state = viewModel.state.value
        assertEquals("", state.searchQuery)
        assertEquals(DatePreset.THIS_MONTH, state.datePreset)
        assertTrue(state.selectedAccountIds.isEmpty())
        assertTrue(state.selectedCategoryIds.isEmpty())
        assertEquals(TransactionTypeFilter.ALL, state.typeFilter)
        assertNull(state.minAmount)
        assertNull(state.maxAmount)
    }

    @Test
    fun `exportReportCsv exports matching transactions to CSV file`() = runTest(testDispatcher) {
        val now = DateTimeHelper.now()
        val tx = Transaction(
            id = "tx_exp",
            accountId = "acc_1",
            amount = BigDecimal.fromInt(1500),
            categoryId = "cat_1",
            category = "Dining Out",
            isIncome = false,
            dateTime = now,
            description = "Dinner at Java",
            merchantName = "Java House"
        )
        val customRepo = FakeTransactionRepository()
        customRepo.setTransactions(listOf(tx))

        val customViewModel = CustomAnalysisViewModel(
            transactionRepository = customRepo,
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            fileSaver = fakeFileSaver
        )

        customViewModel.applyDatePreset(DatePreset.ALL_TIME)
        testScheduler.advanceUntilIdle()

        customViewModel.exportReportCsv()
        testScheduler.advanceUntilIdle()

        assertTrue(customViewModel.state.value.exportSuccessMessage != null)
        assertTrue(customViewModel.state.value.exportSuccessMessage!!.contains("Exported report"))
    }
}
