package com.fintrack.shared.feature.transaction.ui

import com.fintrack.shared.feature.category.data.LocalCategoryDataSource
import com.fintrack.shared.feature.category.domain.repository.FakeCategoryRepository
import com.fintrack.shared.feature.category.domain.usecase.SyncCategoriesUseCase
import com.fintrack.shared.feature.transaction.domain.repository.FakeTransactionRepository
import com.fintrack.shared.feature.transaction.domain.service.FakeTransactionImporter
import com.fintrack.shared.feature.transaction.domain.usecase.CreateTransactionUseCase
import com.fintrack.shared.feature.transaction.domain.usecase.ValidateTransactionUseCase
import kotlin.test.Test
import kotlin.test.assertEquals

class TransactionViewModelTest {

    private val fakeRepo = FakeTransactionRepository()
    private val localCategoryDataSource = LocalCategoryDataSource()
    private val syncCategoriesUseCase = SyncCategoriesUseCase(FakeCategoryRepository(), localCategoryDataSource)
    private val validateTransactionUseCase = ValidateTransactionUseCase()
    private val createTransactionUseCase = CreateTransactionUseCase()
    private val transactionImporter = FakeTransactionImporter()

    private val viewModel = TransactionViewModel(
        repo = fakeRepo,
        localCategoryDataSource = localCategoryDataSource,
        syncCategoriesUseCase = syncCategoriesUseCase,
        validateTransactionUseCase = validateTransactionUseCase,
        createTransactionUseCase = createTransactionUseCase,
        transactionImporter = transactionImporter,
        receiptScanner = null
    )

    @Test
    fun `initial form state is empty`() {
        val formState = viewModel.formState.value
        assertEquals("", formState.amount)
        assertEquals("", formState.description)
        assertEquals(false, formState.isIncome)
    }

    @Test
    fun `updating amount changes form state`() {
        viewModel.onAmountChange("1500")
        assertEquals("1500", viewModel.formState.value.amount)
    }

    @Test
    fun `updating description changes form state`() {
        viewModel.onDescriptionChange("Groceries")
        assertEquals("Groceries", viewModel.formState.value.description)
    }

    @Test
    fun `updating transaction type changes form state`() {
        viewModel.onTypeChange(true)
        assertEquals(true, viewModel.formState.value.isIncome)
    }
}
