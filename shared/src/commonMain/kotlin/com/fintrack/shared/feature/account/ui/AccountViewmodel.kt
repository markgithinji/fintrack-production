package com.fintrack.shared.feature.account.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.account.domain.repository.AccountRepository
import com.fintrack.shared.feature.account.domain.usecase.GetAccountsUseCase
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.settings.domain.usecase.ClearAllUserDataUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AccountsViewModel(
    private val repo: AccountRepository,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val clearAllUserDataUseCase: ClearAllUserDataUseCase
) : ViewModel() {

    private val _accounts = MutableStateFlow<Result<List<Account>>>(Result.Loading)
    val accounts: StateFlow<Result<List<Account>>> = _accounts

    private val _selectedAccount = MutableStateFlow<Result<Account>>(Result.Loading)
    val selectedAccount: StateFlow<Result<Account>> = _selectedAccount

    private val _saveResult = MutableStateFlow<Result<Account>?>(null)
    val saveResult: StateFlow<Result<Account>?> = _saveResult

    private val _deleteResult = MutableStateFlow<Result<Unit>?>(null)
    val deleteResult: StateFlow<Result<Unit>?> = _deleteResult

    private val _clearDataResult = MutableStateFlow<Result<Unit>?>(null)
    val clearDataResult: StateFlow<Result<Unit>?> = _clearDataResult.asStateFlow()

    // Reload all accounts from the server
    fun reloadAccounts(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _accounts.value = Result.Loading
            }

            val result = getAccountsUseCase()
            _accounts.value = result

            // If we have a selected account, update it from the new list if possible
            val currentSelectedId = (_selectedAccount.value as? Result.Success)?.data?.id
            if (result is Result.Success && currentSelectedId != null) {
                val updatedAccount = result.data.find { it.id == currentSelectedId }
                if (updatedAccount != null) {
                    _selectedAccount.value = Result.Success(updatedAccount)
                }
            }
        }
    }

    fun selectAccount(id: String) {
        // Only load if not already selected
        val currentAccount = (_selectedAccount.value as? Result.Success)?.data
        if (currentAccount?.id == id) return

        viewModelScope.launch {
            _selectedAccount.value = Result.Loading
            val accounts = (_accounts.value as? Result.Success)?.data
            val account = accounts?.firstOrNull { it.id == id }
            if (account != null) {
                _selectedAccount.value = Result.Success(account)
            } else {
                val result = repo.getAccountById(id)
                _selectedAccount.value = result
            }
        }
    }

    fun saveAccount(account: Account) {
        // Prevent editing name of default accounts
        val currentAccount = (accounts.value as? Result.Success)?.data?.find { it.id == account.id }
        if (currentAccount?.isDefault == true && currentAccount.name != account.name) {
            _saveResult.value = Result.Error(Exception("Default accounts cannot have their names changed"))
            return
        }

        viewModelScope.launch {
            _saveResult.value = Result.Loading
            val result = repo.addOrUpdateAccount(account)
            _saveResult.value = result
            if (result is Result.Success) {
                // Update local state immediately for a smooth transition
                val currentResult = _accounts.value
                if (currentResult is Result.Success) {
                    val updatedList = if (currentResult.data.any { it.id == result.data.id }) {
                        currentResult.data.map { if (it.id == result.data.id) result.data else it }
                    } else {
                        currentResult.data + result.data
                    }
                    _accounts.value = Result.Success(updatedList)
                }

                // If the updated account is the currently selected one, update it immediately
                val currentSelectedId = (selectedAccount.value as? Result.Success)?.data?.id
                if (result.data.id == currentSelectedId) {
                    _selectedAccount.value = Result.Success(result.data)
                }

                reloadAccounts(showLoading = false)
            }
        }
    }

    fun removeAccount(id: String) {
        val account = (accounts.value as? Result.Success)?.data?.find { it.id == id }
        if (account?.isDefault == true) {
            _deleteResult.value = Result.Error(Exception("Default accounts cannot be removed"))
            return
        }

        viewModelScope.launch {
            _deleteResult.value = Result.Loading
            val result = repo.deleteAccount(id)
            _deleteResult.value = result
            if (result is Result.Success) {
                // Update local state immediately for a smooth transition
                val currentResult = _accounts.value
                if (currentResult is Result.Success) {
                    _accounts.value = Result.Success(currentResult.data.filter { it.id != id })
                }
                reloadAccounts(showLoading = false)
            }
        }
    }

    fun clearAccountData(id: String) {
        viewModelScope.launch {
            _clearDataResult.value = Result.Loading
            _clearDataResult.value = clearAllUserDataUseCase(listOf(id))
            // The UI should trigger a global refresh after this succeeds.
        }
    }

    fun clearResults() {
        _saveResult.value = null
        _deleteResult.value = null
        _clearDataResult.value = null
    }
}
