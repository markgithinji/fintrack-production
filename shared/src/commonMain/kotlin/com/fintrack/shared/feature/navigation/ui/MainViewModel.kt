package com.fintrack.shared.feature.navigation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.shared.feature.account.domain.usecase.GetAccountsUseCase
import com.fintrack.shared.feature.auth.domain.datasource.TokenDataSource
import com.fintrack.shared.feature.budget.domain.usecase.CheckBudgetThresholdsUseCase
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.settings.domain.datasource.SettingsDataSource
import com.fintrack.shared.feature.transaction.domain.usecase.SyncRecurringBillsUseCase
import com.fintrack.shared.feature.user.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.time.Clock

class MainViewModel(
    private val settingsDataSource: SettingsDataSource,
    private val tokenDataSource: TokenDataSource,
    userRepository: UserRepository,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val checkBudgetThresholdsUseCase: CheckBudgetThresholdsUseCase,
    private val syncRecurringBillsUseCase: SyncRecurringBillsUseCase,
) : ViewModel() {

    private val _selectedAccountId = MutableStateFlow<String?>(null)
    val selectedAccountId: StateFlow<String?> = _selectedAccountId.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0)
    val refreshTrigger: StateFlow<Int> = _refreshTrigger.asStateFlow()

    private val _smsSyncTrigger = MutableStateFlow<SmsSyncSignal?>(null)
    val smsSyncTrigger: StateFlow<SmsSyncSignal?> = _smsSyncTrigger.asStateFlow()

    // Global states that multiple screens care about
    val isBalanceHidden = settingsDataSource.isBalanceHidden
    val currency = settingsDataSource.currency
    val theme = settingsDataSource.theme
    val showDecimals = settingsDataSource.showDecimals
    val timeFormat = settingsDataSource.timeFormat
    val isSmsRationaleHidden = settingsDataSource.isSmsRationaleHidden
    val userProfile = userRepository.getUserProfile()

    private val _toastMessage = MutableStateFlow<Pair<String, Boolean>?>(null)
    val toastMessage: StateFlow<Pair<String, Boolean>?> = _toastMessage.asStateFlow()

    fun showToast(message: String, isError: Boolean = false) {
        _toastMessage.value = message to isError
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    init {
        // Initialize selected account from default settings or first available account
        viewModelScope.launch {
            combine(
                tokenDataSource.accessToken,
                settingsDataSource.defaultAccountId
            ) { token, defaultId -> token to defaultId }
                .collectLatest { (token, defaultId) ->
                    if (token != null) {
                        if (defaultId != null) {
                            _selectedAccountId.value = defaultId
                        } else if (_selectedAccountId.value == null) {
                            fetchAndSelectFirstAccount()
                        }
                        
                        // Maintenance tasks that need a token
                        checkBudgets()
                        syncBills()
                    } else {
                        _selectedAccountId.value = null
                        _refreshTrigger.value = 0
                    }
                }
        }

        // Maintenance tasks that react to settings changes
        viewModelScope.launch {
            // React to settings changes for bills
            combine(
                tokenDataSource.accessToken,
                settingsDataSource.isBillReminderEnabled,
                settingsDataSource.billReminderDaysBefore,
            ) { token, enabled, days -> Triple(token, enabled, days) }
                .distinctUntilChanged()
                .collectLatest { (token, enabled, _) ->
                    if (token != null && enabled) {
                        syncBills()
                    }
                }
        }
    }

    private suspend fun fetchAndSelectFirstAccount() {
        when (val result = getAccountsUseCase()) {
            is Result.Success -> {
                val firstAccount = result.data.firstOrNull()
                if (firstAccount != null) {
                    _selectedAccountId.value = firstAccount.id
                    // Optionally set this as default if none exists
                    settingsDataSource.setDefaultAccountId(firstAccount.id)
                }
            }
            else -> Unit
        }
    }

    fun onAccountSelected(id: String) {
        _selectedAccountId.value = id
    }

    fun triggerSmsSync(accountId: String? = null) {
        _smsSyncTrigger.value = SmsSyncSignal(
            accountId = accountId ?: _selectedAccountId.value,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )
    }

    fun consumeSmsSyncSignal() {
        _smsSyncTrigger.value = null
    }

    fun setSmsRationaleHidden(hidden: Boolean) {
        viewModelScope.launch {
            settingsDataSource.setSmsRationaleHidden(hidden)
        }
    }

    fun triggerGlobalRefresh() {
        viewModelScope.launch {
            val newValue = _refreshTrigger.value + 1
            _refreshTrigger.value = newValue
            // Maintenance tasks that react to data changes
            checkBudgets()
            syncBills()
        }
    }

    private suspend fun checkBudgets() {
        try {
            checkBudgetThresholdsUseCase()
        } catch (_: Exception) {
        }
    }

    private suspend fun syncBills() {
        try {
            syncRecurringBillsUseCase()
        } catch (_: Exception) {
        }
    }
}

data class SmsSyncSignal(val accountId: String?, val timestamp: Long)
