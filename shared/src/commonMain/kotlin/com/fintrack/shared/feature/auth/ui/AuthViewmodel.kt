package com.fintrack.shared.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.shared.feature.auth.domain.model.AuthResponse
import com.fintrack.shared.feature.auth.domain.model.AuthState
import com.fintrack.shared.feature.auth.domain.model.LoginFormState
import com.fintrack.shared.feature.auth.domain.model.RegisterFormState
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

    private val _loginState = MutableStateFlow<AuthState<AuthResponse>>(AuthState.Idle)
    val loginState: StateFlow<AuthState<AuthResponse>> = _loginState

    private val _registerState = MutableStateFlow<AuthState<AuthResponse>>(AuthState.Idle)
    val registerState: StateFlow<AuthState<AuthResponse>> = _registerState

    private val _authStatus = MutableStateFlow<AuthState<Boolean>>(AuthState.Success(true))
    val authStatus: StateFlow<AuthState<Boolean>> = _authStatus

    private val _toastMessage = MutableStateFlow<Pair<String, Boolean>?>(null)
    val toastMessage: StateFlow<Pair<String, Boolean>?> = _toastMessage.asStateFlow()

    private val _registerFormState = MutableStateFlow(RegisterFormState())
    val registerFormState: StateFlow<RegisterFormState> = _registerFormState

    private val _loginFormState = MutableStateFlow(LoginFormState())
    val loginFormState: StateFlow<LoginFormState> = _loginFormState

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

    fun login() {}
    fun register() {}
    fun checkAuthenticationStatus() {
        _authStatus.value = AuthState.Success(true)
    }
    fun clearAuthStates() {}
    fun logout() {}
    
    // Stub methods to keep UI code compiling if it references them
    fun updateLoginEmail(email: String) {}
    fun validateLoginEmail(trigger: Any? = null) {}
    fun updateLoginPassword(password: String) {}
    fun validateLoginPassword(trigger: Any? = null) {}
    fun updateName(name: String) {}
    fun validateName(trigger: Any? = null) {}
    fun updateEmail(email: String) {}
    fun validateEmail(trigger: Any? = null) {}
    fun updatePassword(password: String) {}
    fun validatePassword(trigger: Any? = null) {}
    fun updateConfirmPassword(confirmPassword: String) {}
    fun validateConfirmPassword(trigger: Any? = null) {}
}
