package com.fintrack.shared.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.account.domain.repository.AccountRepository
import com.fintrack.shared.feature.auth.domain.repository.AuthRepository
import com.fintrack.shared.feature.auth.domain.usecase.ChangePasswordValidationUseCase
import com.fintrack.shared.feature.budget.domain.model.BudgetWithStatus
import com.fintrack.shared.feature.budget.domain.repository.BudgetRepository
import com.fintrack.shared.feature.category.data.LocalCategoryDataSource
import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.category.domain.model.TransactionCost
import com.fintrack.shared.feature.category.domain.usecase.SyncCategoriesUseCase
import com.fintrack.shared.feature.core.domain.SaveState
import com.fintrack.shared.feature.core.domain.ValidationResult
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.settings.domain.datasource.SettingsDataSource
import com.fintrack.shared.feature.settings.domain.model.AppTheme
import com.fintrack.shared.feature.settings.domain.model.Currency
import com.fintrack.shared.feature.settings.domain.model.ExportFormat
import com.fintrack.shared.feature.settings.domain.model.TimeFormat
import com.fintrack.shared.feature.core.domain.service.NotificationService
import com.fintrack.shared.feature.transaction.domain.service.TransactionImporter
import com.fintrack.shared.feature.transaction.domain.usecase.ExportTransactionsUseCase
import com.fintrack.shared.feature.user.domain.repository.UserRepository
import com.fintrack.shared.feature.user.domain.usecase.DeleteAccountUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime

class SettingsViewModel(
    private val settingsDataSource: SettingsDataSource,
    private val exportTransactionsUseCase: ExportTransactionsUseCase,
    private val notificationService: NotificationService,
    private val authRepository: AuthRepository,
    private val validationUseCase: ChangePasswordValidationUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val userRepository: UserRepository,
    private val localCategoryDataSource: LocalCategoryDataSource,
    private val syncCategoriesUseCase: SyncCategoriesUseCase,
    private val accountRepository: AccountRepository,
    private val budgetRepository: BudgetRepository,
    private val transactionImporter: TransactionImporter,
) : ViewModel() {

    // State Flows
    private val _budgets = MutableStateFlow<Result<List<BudgetWithStatus>>>(Result.Loading)
    val budgets: StateFlow<Result<List<BudgetWithStatus>>> = _budgets.asStateFlow()

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _exportResult = MutableStateFlow<String?>(null)
    val exportResult: StateFlow<String?> = _exportResult.asStateFlow()

    private val _exportStartDate = MutableStateFlow<String?>(null)
    val exportStartDate: StateFlow<String?> = _exportStartDate.asStateFlow()

    private val _exportEndDate = MutableStateFlow<String?>(null)
    val exportEndDate: StateFlow<String?> = _exportEndDate.asStateFlow()

    private val _isLoading = MutableStateFlow(value = false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _showPermissionRequest = MutableStateFlow(false)
    val showPermissionRequest: StateFlow<Boolean> = _showPermissionRequest.asStateFlow()

    private val _changePasswordState = MutableStateFlow<SaveState<Unit>>(SaveState.Idle)
    val changePasswordState: StateFlow<SaveState<Unit>> = _changePasswordState.asStateFlow()

    private val _changePasswordFormState = MutableStateFlow(ChangePasswordFormState())
    val changePasswordFormState: StateFlow<ChangePasswordFormState> = _changePasswordFormState.asStateFlow()

    private val _deleteAccountState = MutableStateFlow<SaveState<Unit>>(SaveState.Idle)
    val deleteAccountState: StateFlow<SaveState<Unit>> = _deleteAccountState.asStateFlow()

    private val _seedProgress = MutableStateFlow(0f)
    val seedProgress: StateFlow<Float> = _seedProgress.asStateFlow()

    private val _seedState = MutableStateFlow<SaveState<String>>(SaveState.Idle)
    val seedState: StateFlow<SaveState<String>> = _seedState.asStateFlow()

    // Settings Flows
    val theme: StateFlow<AppTheme> = settingsDataSource.theme
    val timeFormat: StateFlow<TimeFormat> = settingsDataSource.timeFormat
    val currency: StateFlow<Currency> = settingsDataSource.currency
    val isBalanceHidden: StateFlow<Boolean> = settingsDataSource.isBalanceHidden
    val isReminderEnabled: StateFlow<Boolean> = settingsDataSource.isReminderEnabled
    val isBiometricEnabled: StateFlow<Boolean> = settingsDataSource.isBiometricEnabled
    val reminderTime: StateFlow<LocalTime> = settingsDataSource.reminderTime
    val isMpesaListenerEnabled: StateFlow<Boolean> = settingsDataSource.isMpesaListenerEnabled
    val isEquityListenerEnabled: StateFlow<Boolean> = settingsDataSource.isEquityListenerEnabled
    val budgetAlertsEnabled: StateFlow<Boolean> = settingsDataSource.budgetAlertsEnabled
    val budgetAlertThresholds: StateFlow<Set<Int>> = settingsDataSource.budgetAlertThresholds
    val alertBudgetId: StateFlow<String?> = settingsDataSource.alertBudgetId
    val mpesaLinkedAccountIds: StateFlow<Set<String>> = settingsDataSource.mpesaLinkedAccountIds
    val equityLinkedAccountIds: StateFlow<Set<String>> = settingsDataSource.equityLinkedAccountIds
    val isBillReminderEnabled: StateFlow<Boolean> = settingsDataSource.isBillReminderEnabled
    val billReminderDaysBefore: StateFlow<Int> = settingsDataSource.billReminderDaysBefore
    val isDailySummaryEnabled: StateFlow<Boolean> = settingsDataSource.isDailySummaryEnabled
    val isWeeklySummaryEnabled: StateFlow<Boolean> = settingsDataSource.isWeeklySummaryEnabled
    val summaryNotificationTime: StateFlow<LocalTime> = settingsDataSource.summaryNotificationTime
    val showDecimals: StateFlow<Boolean> = settingsDataSource.showDecimals
    val defaultAccountId: StateFlow<String?> = settingsDataSource.defaultAccountId
    val exportFormat: StateFlow<ExportFormat> = settingsDataSource.exportFormat
    val isSmsRationaleHidden: StateFlow<Boolean> = settingsDataSource.isSmsRationaleHidden

    val allCategories: StateFlow<List<Category>> = localCategoryDataSource.categories
        .map { categories ->
            val filtered = categories.filter { it.id != Category.TransactionCost.id }
                .sortedWith(
                    compareByDescending<Category> { it.isDefault }
                        .thenBy { !it.isExpense }
                        .thenBy { it.name }
                )
            (listOf(Category.TransactionCost) + filtered).distinctBy { it.id }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = localCategoryDataSource.categories.value.let { categories ->
                val filtered = categories.filter { it.id != Category.TransactionCost.id }
                    .sortedWith(
                        compareByDescending<Category> { it.isDefault }
                            .thenBy { !it.isExpense }
                            .thenBy { it.name }
                    )
                (listOf(Category.TransactionCost) + filtered).distinctBy { it.id }
            }
        )

    val trackedCategoryIds: StateFlow<List<String>> = userRepository.getUserProfile()
        .map { it?.trackedCategoryIds ?: emptyList() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = userRepository.getUserProfile().value?.trackedCategoryIds ?: emptyList()
        )

    val trackedCategoryNames: StateFlow<List<String>> = combine(trackedCategoryIds, allCategories) { ids, all ->
        ids.mapNotNull { id -> all.find { it.id == id }?.name }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun reloadBudgets(force: Boolean = true, showLoading: Boolean = true) {
        val currentBudgets = _budgets.value
        if (!force && currentBudgets is Result.Success && currentBudgets.data.isNotEmpty()) return

        viewModelScope.launch {
            if (showLoading) {
                _budgets.value = Result.Loading
            }
            _budgets.value = budgetRepository.getBudgets()
        }
    }

    fun syncCategories() {
        viewModelScope.launch {
            syncCategoriesUseCase()
        }
    }

    fun loadAccounts() {
        viewModelScope.launch {
            val result = accountRepository.getAccounts()
            if (result is Result.Success) {
                _accounts.value = result.data
            }
        }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            settingsDataSource.setTheme(theme)
        }
    }

    fun setTimeFormat(format: TimeFormat) {
        viewModelScope.launch {
            settingsDataSource.setTimeFormat(format)
        }
    }

    fun setCurrency(currency: Currency) {
        viewModelScope.launch {
            settingsDataSource.setCurrency(currency)
        }
    }

    fun setBalanceHidden(hidden: Boolean) {
        viewModelScope.launch {
            settingsDataSource.setBalanceHidden(hidden)
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataSource.setBiometricEnabled(enabled)
        }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                _showPermissionRequest.value = true
            } else {
                settingsDataSource.setReminderEnabled(false)
                notificationService.cancelDailyReminder()
            }
        }
    }

    fun setReminderTime(time: LocalTime) {
        viewModelScope.launch {
            settingsDataSource.setReminderTime(time)
            if (isReminderEnabled.value) {
                notificationService.scheduleDailyReminder(time)
            }
        }
    }

    fun setMpesaListenerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataSource.setMpesaListenerEnabled(enabled)
        }
    }

    fun setEquityListenerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataSource.setEquityListenerEnabled(enabled)
        }
    }

    fun setBudgetAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataSource.setBudgetAlertsEnabled(enabled)
        }
    }

    fun setBudgetAlertThresholds(thresholds: Set<Int>) {
        viewModelScope.launch {
            settingsDataSource.setBudgetAlertThresholds(thresholds)
        }
    }

    fun setAlertBudgetId(budgetId: String?) {
        viewModelScope.launch {
            settingsDataSource.setAlertBudgetId(budgetId)
        }
    }

    fun setMpesaLinkedAccountIds(ids: Set<String>) {
        viewModelScope.launch {
            settingsDataSource.setMpesaLinkedAccountIds(ids)
        }
    }

    fun setEquityLinkedAccountIds(ids: Set<String>) {
        viewModelScope.launch {
            settingsDataSource.setEquityLinkedAccountIds(ids)
        }
    }

    fun setBillReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataSource.setBillReminderEnabled(enabled)
        }
    }

    fun setBillReminderDaysBefore(days: Int) {
        viewModelScope.launch {
            settingsDataSource.setBillReminderDaysBefore(days)
        }
    }

    fun setDailySummaryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataSource.setDailySummaryEnabled(enabled)
            updateSummaryScheduling()
        }
    }

    fun setWeeklySummaryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataSource.setWeeklySummaryEnabled(enabled)
            updateSummaryScheduling()
        }
    }

    fun setSummaryNotificationTime(time: LocalTime) {
        viewModelScope.launch {
            settingsDataSource.setSummaryNotificationTime(time)
            updateSummaryScheduling()
        }
    }

    fun setShowDecimals(show: Boolean) {
        viewModelScope.launch {
            settingsDataSource.setShowDecimals(show)
        }
    }

    fun setDefaultAccountId(id: String?) {
        viewModelScope.launch {
            settingsDataSource.setDefaultAccountId(id)
        }
    }

    fun setExportFormat(format: ExportFormat) {
        viewModelScope.launch {
            settingsDataSource.setExportFormat(format)
        }
    }

    fun setSmsRationaleHidden(hidden: Boolean) {
        viewModelScope.launch {
            settingsDataSource.setSmsRationaleHidden(hidden)
        }
    }

    private suspend fun updateSummaryScheduling() {
        val daily = settingsDataSource.isDailySummaryEnabled.first()
        val weekly = settingsDataSource.isWeeklySummaryEnabled.first()
        val time = settingsDataSource.summaryNotificationTime.first()

        if (daily || weekly) {
            notificationService.scheduleSummaryNotification(time)
        } else {
            notificationService.cancelSummaryNotification()
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _showPermissionRequest.value = false
        if (granted) {
            viewModelScope.launch {
                settingsDataSource.setReminderEnabled(true)
                notificationService.scheduleDailyReminder(reminderTime.value)
            }
        } else {
            _error.value = "Notification permission denied"
        }
    }

    fun dismissPermissionRequest() {
        _showPermissionRequest.value = false
    }

    fun exportTransactions() {
        viewModelScope.launch {
            _exportResult.value = null
            _isLoading.value = true
            val format = settingsDataSource.exportFormat.value
            val result = exportTransactionsUseCase(
                format = format,
                startDate = _exportStartDate.value,
                endDate = _exportEndDate.value
            )
            when (result) {
                is Result.Success -> {
                    _exportResult.value = result.data
                }
                is Result.Error -> {
                    _error.value = "Failed to export transactions"
                }
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _deleteAccountState.value = SaveState.Loading
            val result = deleteAccountUseCase()
            if (result is Result.Error) {
                _deleteAccountState.value = SaveState.Error(result.exception)
                _error.value = "Failed to delete account"
            } else {
                // Wipe any remaining ViewModel state
                _budgets.value = Result.Loading
                _accounts.value = emptyList()
                _deleteAccountState.value = SaveState.Success(Unit)
            }
        }
    }

    fun resetDeleteAccountState() {
        _deleteAccountState.value = SaveState.Idle
    }

    fun setError(message: String) {
        _error.value = message
    }

    fun clearExportResult() {
        _exportResult.value = null
    }

    fun setExportDateRange(startDate: String?, endDate: String?) {
        _exportStartDate.value = startDate
        _exportEndDate.value = endDate
    }

    // Password change methods
    fun updateCurrentPassword(password: String) {
        _changePasswordFormState.update { it.copy(currentPassword = password, currentPasswordError = null) }
    }

    fun updateNewPassword(password: String) {
        _changePasswordFormState.update { it.copy(newPassword = password, newPasswordError = null) }
    }

    fun updateConfirmPassword(password: String) {
        _changePasswordFormState.update { it.copy(confirmPassword = password, confirmPasswordError = null) }
    }

    fun changePassword() {
        val form = _changePasswordFormState.value
        val validationResult = validationUseCase(
            currentPassword = form.currentPassword,
            newPassword = form.newPassword,
            confirmPassword = form.confirmPassword
        )

        if (!validationResult.isValid) {
            _changePasswordFormState.update {
                it.copy(
                    currentPasswordError = (validationResult.currentPasswordResult as? ValidationResult.Error)?.message,
                    newPasswordError = (validationResult.newPasswordResult as? ValidationResult.Error)?.message,
                    confirmPasswordError = (validationResult.confirmPasswordResult as? ValidationResult.Error)?.message
                )
            }
            return
        }

        viewModelScope.launch {
            _changePasswordState.value = SaveState.Loading
            when (val result = authRepository.changePassword(form.currentPassword, form.newPassword)) {
                is Result.Success -> {
                    _changePasswordState.value = SaveState.Success(Unit)
                    _changePasswordFormState.value = ChangePasswordFormState()
                }
                is Result.Error -> {
                    _changePasswordState.value = SaveState.Error(result.exception)
                }
                else -> {}
            }
        }
    }

    fun resetChangePasswordState() {
        _changePasswordState.value = SaveState.Idle
        _changePasswordFormState.value = ChangePasswordFormState()
    }

    fun updateTrackedCategories(categories: List<String>, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = userRepository.updateTrackedCategories(categories)
            if (result is Result.Success) {
                onSuccess()
            } else if (result is Result.Error) {
                _error.value = "Failed to update tracked categories: ${result.exception.message}"
            }
            _isLoading.value = false
        }
    }

    fun seedPortfolioData() {
        viewModelScope.launch {
            _seedState.value = SaveState.Loading
            _seedProgress.value = 0f
            try {
                val accounts = _accounts.value
                val defaultId = defaultAccountId.value
                
                // Priority: 1. Default Account, 2. Account with "mpesa" in name, 3. First account
                val targetAccount = accounts.find { it.id == defaultId }
                    ?: accounts.find { it.name.lowercase().contains("mpesa") }
                    ?: accounts.firstOrNull()

                val targetAccountId = targetAccount?.id
                val targetAccountName = targetAccount?.name ?: "Unknown Account"

                transactionImporter.importHistory(
                    targetAccountId = targetAccountId,
                    isPortfolioSeed = true
                ) { progress ->
                    _seedProgress.value = progress
                }
                _seedState.value = SaveState.Success(targetAccountName)
            } catch (e: Exception) {
                _seedState.value = SaveState.Error(e)
                _error.value = "Failed to seed portfolio data: ${e.message}"
            }
        }
    }

    fun resetSeedState() {
        _seedState.value = SaveState.Idle
        _seedProgress.value = 0f
    }
}

data class ChangePasswordFormState(
    val currentPassword: String = "",
    val currentPasswordError: String? = null,
    val newPassword: String = "",
    val newPasswordError: String? = null,
    val confirmPassword: String = "",
    val confirmPasswordError: String? = null
)
