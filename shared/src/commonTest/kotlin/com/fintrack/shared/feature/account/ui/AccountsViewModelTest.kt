package com.fintrack.shared.feature.account.ui

import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.account.domain.repository.FakeAccountRepository
import com.fintrack.shared.feature.account.domain.usecase.GetAccountsUseCase
import com.fintrack.shared.feature.budget.domain.repository.FakeBudgetRepository
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.settings.domain.usecase.ClearAllUserDataUseCase
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AccountsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeRepo = FakeAccountRepository()
    private val getAccountsUseCase = GetAccountsUseCase(fakeRepo)
    private val clearAllUserDataUseCase = ClearAllUserDataUseCase(
        transactionRepository = FakeTransactionRepository(),
        budgetRepository = FakeBudgetRepository()
    )

    private lateinit var viewModel: AccountsViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AccountsViewModel(
            repo = fakeRepo,
            getAccountsUseCase = getAccountsUseCase,
            clearAllUserDataUseCase = clearAllUserDataUseCase
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `prevent editing name of default accounts`() = runTest(testDispatcher) {
        val defaultAcc = Account(id = "1", name = "M-Pesa", isDefault = true)
        fakeRepo.setAccounts(listOf(defaultAcc))
        viewModel.reloadAccounts(showLoading = false)

        val modified = defaultAcc.copy(name = "New Name")
        viewModel.saveAccount(modified)

        val saveResult = viewModel.saveResult.value
        assertTrue(saveResult is Result.Error)
        assertEquals("Default accounts cannot have their names changed", saveResult.exception.message)
    }

    @Test
    fun `prevent deleting default accounts`() = runTest(testDispatcher) {
        val defaultAcc = Account(id = "1", name = "M-Pesa", isDefault = true)
        fakeRepo.setAccounts(listOf(defaultAcc))
        viewModel.reloadAccounts(showLoading = false)

        viewModel.removeAccount("1")

        val deleteResult = viewModel.deleteResult.value
        assertTrue(deleteResult is Result.Error)
        assertEquals("Default accounts cannot be removed", deleteResult.exception.message)
    }
}
