package com.fintrack.shared.feature.budget.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.time.Clock
import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.budget.domain.model.Budget
import com.fintrack.shared.feature.budget.domain.model.BudgetFormState
import com.fintrack.shared.feature.budget.domain.model.BudgetWithStatus
import com.fintrack.shared.feature.budget.domain.repository.BudgetRepository
import com.fintrack.shared.feature.budget.domain.usecase.BudgetValidationUseCase
import com.fintrack.shared.feature.category.data.LocalCategoryDataSource
import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.category.domain.usecase.SyncCategoriesUseCase
import com.fintrack.shared.feature.core.domain.SaveState
import com.fintrack.shared.feature.core.domain.ValidationResult
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.core.util.formatToTwoPrecision
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

class BudgetViewModel(
    private val budgetRepository: BudgetRepository,
    private val validationUseCase: BudgetValidationUseCase,
    private val localCategoryDataSource: LocalCategoryDataSource,
    private val syncCategoriesUseCase: SyncCategoriesUseCase
) : ViewModel() {

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _budgets = MutableStateFlow<Result<List<BudgetWithStatus>>>(Result.Loading)
    val budgets: StateFlow<Result<List<BudgetWithStatus>>> = _budgets.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState<Budget>>(SaveState.Idle)
    val saveState: StateFlow<SaveState<Budget>> = _saveState.asStateFlow()

    private val _deleteResult = MutableStateFlow<Result<Unit>?>(null)
    val deleteResult: StateFlow<Result<Unit>?> = _deleteResult.asStateFlow()

    private val _selectedBudget = MutableStateFlow<Result<BudgetWithStatus>>(Result.Loading)
    val selectedBudget: StateFlow<Result<BudgetWithStatus>> = _selectedBudget.asStateFlow()

    private val _validationError = MutableStateFlow<String?>(null)
    val validationError: StateFlow<String?> = _validationError.asStateFlow()

    private val _formState = MutableStateFlow(BudgetFormState())
    val formState: StateFlow<BudgetFormState> = _formState.asStateFlow()

    private var hasInitializedForm = false

    val validationState: StateFlow<ValidationResult> = _formState.map { state ->
        validationUseCase(
            name = state.name,
            amount = state.amount,
            categories = state.selectedCategories,
            startDate = state.startDate,
            endDate = state.endDate,
            selectedAccounts = state.selectedAccounts
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ValidationResult.Error("Initializing...")
    )

    init {
        viewModelScope.launch {
            localCategoryDataSource.categories.collect {
                _categories.value = it
            }
        }
        viewModelScope.launch {
            syncCategoriesUseCase()
        }
        reloadBudgets()
    }

    fun initializeForm(budgetId: String?, availableAccounts: List<Account>) {
        if (hasInitializedForm && _formState.value.id == budgetId) return

        if (budgetId == null) {
            _formState.value = computeInitialFormState()
            hasInitializedForm = true
        } else {
            val currentSelected = _selectedBudget.value
            if (currentSelected is Result.Success) {
                _formState.value = computeEditFormState(currentSelected.data.budget, availableAccounts)
                hasInitializedForm = true
            }
        }
    }

    private fun computeInitialFormState(): BudgetFormState {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return BudgetFormState(
            name = "",
            amount = "",
            selectedCategories = emptySet(),
            isExpense = true,
            startDate = today,
            endDate = today + DatePeriod(months = 1),
            selectedAccounts = emptySet()
        )
    }

    private fun computeEditFormState(budget: Budget, availableAccounts: List<Account>): BudgetFormState {
        val budgetAccounts = availableAccounts.filter { it.id in budget.accountIds }.toSet()
        val allCategories = _categories.value

        return BudgetFormState(
            id = budget.id,
            name = budget.name,
            amount = budget.limit.formatToTwoPrecision(),
            selectedCategories = budget.categories.map { budgetCat ->
                allCategories.find { it.id == budgetCat.id } ?: budgetCat
            }.toSet(),
            isExpense = budget.isExpense,
            startDate = budget.startDate,
            endDate = budget.endDate,
            selectedAccounts = budgetAccounts
        )
    }

    fun toggleAccount(account: Account) {
        _formState.update { state ->
            val updatedAccounts = if (state.selectedAccounts.any { it.id == account.id }) {
                state.selectedAccounts.filterNot { it.id == account.id }.toSet()
            } else {
                state.selectedAccounts + account
            }
            state.copy(selectedAccounts = updatedAccounts)
        }
    }

    fun setName(name: String) {
        _formState.update { it.copy(name = name) }
    }

    fun setAmount(amount: String, selectionStart: Int? = null, selectionEnd: Int? = null) {
        _formState.update { 
            it.copy(
                amount = amount,
                amountSelectionStart = selectionStart ?: amount.length,
                amountSelectionEnd = selectionEnd ?: amount.length
            ) 
        }
    }

    fun onAmountSelectionChange(start: Int, end: Int) {
        _formState.update { it.copy(amountSelectionStart = start, amountSelectionEnd = end) }
    }

    fun handleAmountInput(input: String) {
        val current = _formState.value
        val amount = current.amount
        val start = current.amountSelectionStart
        val end = current.amountSelectionEnd

        if (input == "." && amount.contains(".")) return
        if (amount.length >= 12 && start == end && start == amount.length) return

        val newAmount = amount.take(start) + input + amount.drop(end)
        
        val newSelection = start + input.length
        setAmount(newAmount, newSelection, newSelection)
    }

    fun handleAmountBackspace() {
        val current = _formState.value
        val amount = current.amount
        val start = current.amountSelectionStart
        val end = current.amountSelectionEnd

        if (start == 0 && end == 0) return

        val newAmount: String
        val newSelection: Int
        if (start != end) {
            newAmount = amount.take(start) + amount.drop(end)
            newSelection = start
        } else {
            newAmount = amount.take(start - 1) + amount.drop(start)
            newSelection = start - 1
        }
        setAmount(newAmount, newSelection, newSelection)
    }

    fun setIsExpense(isExpense: Boolean) {
        _formState.update { current ->
            val updatedCategories = current.selectedCategories.filter { it.isExpense == isExpense }.toSet()
            current.copy(
                isExpense = isExpense,
                selectedCategories = updatedCategories
            )
        }
    }

    fun setCategories(categories: Set<Category>) {
        _formState.update { it.copy(selectedCategories = categories) }
    }

    fun setPeriod(startDate: LocalDate?, endDate: LocalDate?) {
        _formState.update { it.copy(startDate = startDate, endDate = endDate) }
    }

    fun reloadBudgets(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _budgets.value = Result.Loading
            }
            _budgets.value = budgetRepository.getBudgets(accountId = null)
        }
    }

    fun saveBudget() {
        val currentForm = _formState.value
        val validation = validationUseCase(
            name = currentForm.name,
            amount = currentForm.amount,
            categories = currentForm.selectedCategories,
            startDate = currentForm.startDate,
            endDate = currentForm.endDate,
            selectedAccounts = currentForm.selectedAccounts
        )

        if (validation is ValidationResult.Error) {
            _validationError.value = validation.message
            return
        }

        _validationError.value = null
        viewModelScope.launch {
            _saveState.value = SaveState.Loading
            val budget = Budget(
                id = currentForm.id,
                accountIds = currentForm.selectedAccounts.map { it.id },
                name = currentForm.name,
                categories = currentForm.selectedCategories.toList(),
                limit = try { BigDecimal.parseString(currentForm.amount) } catch (e: Exception) { BigDecimal.ZERO },
                isExpense = currentForm.isExpense,
                startDate = currentForm.startDate!!,
                endDate = currentForm.endDate!!
            )

            val result = budgetRepository.addOrUpdateBudget(budget)
            _saveState.value = when (result) {
                is Result.Success -> {
                    reloadBudgets(showLoading = false)
                    SaveState.Success(result.data)
                }
                is Result.Error -> SaveState.Error(result.exception)
                is Result.Loading -> SaveState.Loading
            }
        }
    }

    fun removeBudget(id: String) {
        viewModelScope.launch {
            _deleteResult.value = Result.Loading
            val result = budgetRepository.deleteBudget(id)
            _deleteResult.value = result
            if (result is Result.Success) {
                reloadBudgets(showLoading = false)
            }
        }
    }

    fun loadBudgetById(id: String) {
        viewModelScope.launch {
            _selectedBudget.value = Result.Loading
            _selectedBudget.value = budgetRepository.getBudgetById(id)
        }
    }

    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }

    fun resetDeleteResult() {
        _deleteResult.value = null
    }

    fun clearValidationError() {
        _validationError.value = null
    }
}
