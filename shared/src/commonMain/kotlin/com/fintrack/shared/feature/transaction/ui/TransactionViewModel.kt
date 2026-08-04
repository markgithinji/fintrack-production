package com.fintrack.shared.feature.transaction.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.category.data.LocalCategoryDataSource
import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.category.domain.model.allCategories
import com.fintrack.shared.feature.category.domain.model.fromId
import com.fintrack.shared.feature.category.domain.usecase.SyncCategoriesUseCase
import com.fintrack.shared.feature.core.domain.SaveState
import com.fintrack.shared.feature.core.domain.ValidationResult
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.core.util.formatToTwoPrecision
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.fintrack.shared.feature.transaction.domain.model.TransactionFormState
import com.fintrack.shared.feature.transaction.domain.repository.TransactionRepository
import com.fintrack.shared.feature.transaction.domain.service.TransactionImporter
import com.fintrack.shared.feature.transaction.domain.usecase.CreateTransactionUseCase
import com.fintrack.shared.feature.transaction.domain.usecase.ValidateTransactionUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

@OptIn(FlowPreview::class)
class TransactionViewModel(
    private val repo: TransactionRepository,
    private val localCategoryDataSource: LocalCategoryDataSource,
    private val syncCategoriesUseCase: SyncCategoriesUseCase,
    private val validateTransactionUseCase: ValidateTransactionUseCase,
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val transactionImporter: TransactionImporter
) : ViewModel() {

    val categories: StateFlow<List<Category>> = localCategoryDataSource.categories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Category.allCategories
        )

    private val _formState = MutableStateFlow(TransactionFormState())
    val formState: StateFlow<TransactionFormState> = _formState.asStateFlow()

    private val _recentTransactions = MutableStateFlow<Result<List<Transaction>>>(Result.Loading)
    val recentTransactions: StateFlow<Result<List<Transaction>>> = _recentTransactions

    private val _saveState = MutableStateFlow<SaveState<Transaction>>(SaveState.Idle)
    val saveState: StateFlow<SaveState<Transaction>> = _saveState

    private val _deleteResult = MutableStateFlow<Result<Unit>?>(null)
    val deleteResult: StateFlow<Result<Unit>?> = _deleteResult

    private val _validationError = MutableStateFlow<String?>(null)
    val validationError: StateFlow<String?> = _validationError

    private val _importState = MutableStateFlow<Map<String?, Result<Unit>>>(emptyMap())
    val importState: StateFlow<Map<String?, Result<Unit>>> = _importState.asStateFlow()

    private val _importProgress = MutableStateFlow<Map<String?, Float>>(emptyMap())
    val importProgress: StateFlow<Map<String?, Float>> = _importProgress.asStateFlow()

    private val _importEvents = MutableSharedFlow<ImportEvent>()
    val importEvents = _importEvents.asSharedFlow()

    private var lastLoadedRecentAccountId: String? = null
    private var recentTransactionsJob: Job? = null
    private val importJobs = mutableMapOf<String?, Job>()
    private val lastAutoSyncTimes = mutableMapOf<String?, Instant>()
    
    private var lastPagingParams: TransactionPagingParams? = null
    private var cachedPagingFlow: Flow<PagingData<Transaction>>? = null

    fun importTransactions(accountId: String? = null, isPortfolioSeed: Boolean = false) {
        if (_importState.value[accountId] is Result.Loading) {
            return
        }
        
        // Stop any existing sync for THIS specific account if it's already running
        cancelImport(accountId)

        lastAutoSyncTimes[accountId] = Clock.System.now()
        val job = viewModelScope.launch {
            _importState.update { it + (accountId to Result.Loading) }
            _importProgress.update { it + (accountId to 0f) }
            try {
                transactionImporter.importHistory(accountId, isPortfolioSeed) { progress ->
                    _importProgress.update { it + (accountId to progress) }
                }
                _importState.update { it + (accountId to Result.Success(Unit)) }
                _importEvents.emit(ImportEvent.Success(accountId))
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _importState.update { it + (accountId to Result.Error(e)) }
                    _importEvents.emit(ImportEvent.Error(accountId, e))
                }
            } finally {
                importJobs.remove(accountId)
            }
        }
        importJobs[accountId] = job
    }

    fun autoSyncTransactions(accountId: String? = null) {
        val now = Clock.System.now()
        val lastSync = lastAutoSyncTimes[accountId]
        if (lastSync == null || (now - lastSync) >= 2.minutes) {
            importTransactions(accountId)
        }
    }

    fun resetImportState(accountId: String? = null) {
        _importState.update { it - accountId }
        _importProgress.update { it - accountId }
    }

    fun cancelImport(accountId: String? = null) {
        if (accountId == null && importJobs.isNotEmpty()) {
            // Cancel all if no ID provided (legacy behavior/global cancel)
            importJobs.values.forEach { it.cancel() }
            importJobs.clear()
        } else {
            importJobs[accountId]?.cancel()
            importJobs.remove(accountId)
        }

        _importState.update { currentMap ->
            if (accountId == null) {
                currentMap.filterValues { it !is Result.Loading }
            } else {
                currentMap.filter { (id, state) -> id != accountId || state !is Result.Loading }
            }
        }
        _importProgress.update { currentMap ->
            if (accountId == null) {
                currentMap.filter { entry -> 
                    _importState.value[entry.key] !is Result.Loading 
                }
            } else {
                currentMap.filter { entry -> entry.key != accountId || _importState.value[entry.key] !is Result.Loading }
            }
        }
    }

    fun refreshCategories() {
        viewModelScope.launch { syncCategoriesUseCase() }
    }

    fun loadRecentTransactions(accountId: String, limit: Int = 6, force: Boolean = false) {
        if (!force && accountId == lastLoadedRecentAccountId && _recentTransactions.value is Result.Success) return
        lastLoadedRecentAccountId = accountId
        recentTransactionsJob?.cancel()
        recentTransactionsJob = viewModelScope.launch {
            _recentTransactions.value = Result.Loading
            val result = repo.getTransactions(limit = limit, sortBy = "dateTime", order = "desc", accountId = accountId)
            _recentTransactions.value = when (result) {
                is Result.Success -> Result.Success(result.data.first)
                is Result.Error -> Result.Error(result.exception)
                is Result.Loading -> Result.Loading
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTransactionsPagingData(
        accountId: String?,
        isIncome: Boolean? = null,
        categoryId: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        hasTransactionCost: Boolean? = null
    ): Flow<PagingData<Transaction>> {
        val newParams = TransactionPagingParams(accountId, isIncome, categoryId, startDate, endDate, hasTransactionCost)
        if (newParams == lastPagingParams && cachedPagingFlow != null) return cachedPagingFlow!!
        lastPagingParams = newParams
        val flow = repo.getTransactionsPagingFlow(accountId, isIncome, categoryId, startDate, endDate, hasTransactionCost).cachedIn(viewModelScope)
        cachedPagingFlow = flow
        return flow
    }

    fun onAmountChange(newAmount: String, selectionStart: Int? = null, selectionEnd: Int? = null) {
        _formState.update { it.copy(amount = newAmount, amountSelectionStart = selectionStart ?: newAmount.length, amountSelectionEnd = selectionEnd ?: newAmount.length) }
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
        onAmountChange(newAmount, newSelection, newSelection)
    }

    fun handleAmountBackspace() {
        val current = _formState.value
        val amount = current.amount
        val start = current.amountSelectionStart
        val end = current.amountSelectionEnd
        if (start == 0 && end == 0) return
        val newAmount = if (start != end) amount.take(start) + amount.drop(end) else amount.take((start - 1).coerceAtLeast(0)) + amount.drop(start)
        val newSelection = if (start != end) start else (start - 1).coerceAtLeast(0)
        onAmountChange(newAmount, newSelection, newSelection)
    }

    fun onTransactionCostChange(newCost: String, selectionStart: Int? = null, selectionEnd: Int? = null) {
        _formState.update { it.copy(transactionCost = newCost, costSelectionStart = selectionStart ?: newCost.length, costSelectionEnd = selectionEnd ?: newCost.length) }
    }

    fun onCostSelectionChange(start: Int, end: Int) {
        _formState.update { it.copy(costSelectionStart = start, costSelectionEnd = end) }
    }

    fun handleCostInput(input: String) {
        val current = _formState.value
        val cost = current.transactionCost
        val start = current.costSelectionStart
        val end = current.costSelectionEnd
        if (input == "." && cost.contains(".")) return
        val newCost = cost.take(start) + input + cost.drop(end)
        val newSelection = start + input.length
        onTransactionCostChange(newCost, newSelection, newSelection)
    }

    fun handleCostBackspace() {
        val current = _formState.value
        val cost = current.transactionCost
        val start = current.costSelectionStart
        val end = current.costSelectionEnd
        if (start == 0 && end == 0) return
        val newCost = if (start != end) cost.take(start) + cost.drop(end) else cost.take((start - 1).coerceAtLeast(0)) + cost.drop(start)
        val newSelection = if (start != end) start else (start - 1).coerceAtLeast(0)
        onTransactionCostChange(newCost, newSelection, newSelection)
    }

    fun onTypeChange(isIncome: Boolean) { _formState.update { it.copy(isIncome = isIncome) } }
    fun onAccountChange(account: Account?) { _formState.update { it.copy(selectedAccount = account) } }
    fun onCategoryChange(category: Category?) { _formState.update { it.copy(selectedCategory = category) } }
    fun onDescriptionChange(description: String) { _formState.update { it.copy(description = description) } }
    fun onDateTimeChange(dateTime: Instant) { _formState.update { it.copy(dateTime = dateTime) } }

    fun addTransaction() {
        val state = _formState.value
        val validation = validateTransactionUseCase(state.amount, state.transactionCost, state.description, state.selectedCategory, state.selectedAccount)
        if (validation is ValidationResult.Error) { _validationError.value = validation.message; return }
        viewModelScope.launch {
            _saveState.value = SaveState.Loading
            val transaction = createTransactionUseCase(state.amount, state.transactionCost, state.isIncome, state.selectedCategory, state.description, state.selectedAccount, state.dateTime) ?: return@launch
            val result = repo.addTransaction(transaction)
            _saveState.value = when (result) {
                is Result.Success -> SaveState.Success(result.data)
                is Result.Error -> SaveState.Error(result.exception)
                is Result.Loading -> SaveState.Loading
            }
        }
    }

    fun updateTransaction(id: String) {
        val state = _formState.value
        val validation = validateTransactionUseCase(state.amount, state.transactionCost, state.description, state.selectedCategory, state.selectedAccount)
        if (validation is ValidationResult.Error) { _validationError.value = validation.message; return }
        viewModelScope.launch {
            _saveState.value = SaveState.Loading
            val transaction = createTransactionUseCase(state.amount, state.transactionCost, state.isIncome, state.selectedCategory, state.description, state.selectedAccount, state.dateTime)?.copy(id = id) ?: return@launch
            val result = repo.updateTransaction(id, transaction)
            _saveState.value = when (result) {
                is Result.Success -> SaveState.Success(result.data)
                is Result.Error -> SaveState.Error(result.exception)
                is Result.Loading -> SaveState.Loading
            }
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            _deleteResult.value = Result.Loading
            _deleteResult.value = repo.deleteTransaction(id)
        }
    }

    fun loadTransactionById(id: String, accounts: List<Account>) {
        viewModelScope.launch {
            val result = repo.getTransaction(id)
            if (result is Result.Success) {
                val t = result.data
                val currentCategories = categories.value
                _formState.value = TransactionFormState(
                    amount = t.amount.formatToTwoPrecision(),
                    description = t.description ?: "",
                    isIncome = t.isIncome,
                    selectedCategory = Category.fromId(
                        t.categoryId,
                        t.category,
                        isExpense = !t.isIncome,
                        knownCategories = currentCategories
                    ),
                    selectedAccount = accounts.find { it.id == t.accountId },
                    dateTime = t.dateTime,
                    transactionCost = t.transactionCost.formatToTwoPrecision()
                )
            }
        }
    }

    fun resetSelectedTransaction() { _formState.value = TransactionFormState(); _saveState.value = SaveState.Idle }
    fun resetDeleteResult() { _deleteResult.value = null }
    fun resetSaveState() { _saveState.value = SaveState.Idle }
    fun clearValidationError() { _validationError.value = null }
}

sealed class ImportEvent {
    data class Success(val accountId: String?) : ImportEvent()
    data class Error(val accountId: String?, val exception: Exception) : ImportEvent()
}

private data class TransactionPagingParams(
    val accountId: String?,
    val isIncome: Boolean?,
    val categoryId: String?,
    val startDate: String?,
    val endDate: String?,
    val hasTransactionCost: Boolean?
)
