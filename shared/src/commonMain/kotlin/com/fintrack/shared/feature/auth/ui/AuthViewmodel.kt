package com.fintrack.shared.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.shared.feature.settings.domain.datasource.SettingsDataSource
import com.fintrack.shared.feature.core.util.DateTimeHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AuthViewModel(
    private val settingsDataSource: SettingsDataSource
) : ViewModel() {

    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private var backgroundTimestamp: Long? = null
    private val lockTimeoutMillis = 30000L // 30 seconds

    private val _toastMessage = MutableStateFlow<Pair<String, Boolean>?>(null)
    val toastMessage: StateFlow<Pair<String, Boolean>?> = _toastMessage.asStateFlow()

    fun showToast(message: String, isError: Boolean = false) {
        _toastMessage.value = message to isError
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    init {
        checkAppLockStatus()
    }

    private fun checkAppLockStatus() {
        viewModelScope.launch {
            val isBiometricEnabled = settingsDataSource.isBiometricEnabled.first()
            _isAppLocked.value = isBiometricEnabled
        }
    }

    fun unlockWithBiometrics() {
        _isAppLocked.value = false
    }

    fun onAppBackgrounded() {
        backgroundTimestamp = DateTimeHelper.now().toEpochMilliseconds()
    }

    fun onAppForegrounded() {
        val timestamp = backgroundTimestamp ?: return
        val now = DateTimeHelper.now().toEpochMilliseconds()
        val elapsed = now - timestamp

        if (elapsed >= lockTimeoutMillis) {
            viewModelScope.launch {
                val isBiometricEnabled = settingsDataSource.isBiometricEnabled.first()
                if (isBiometricEnabled) {
                    _isAppLocked.value = true
                }
            }
        }
        backgroundTimestamp = null
    }

    fun logout() {}
}
