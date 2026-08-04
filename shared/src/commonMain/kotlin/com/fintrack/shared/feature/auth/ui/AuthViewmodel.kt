package com.fintrack.shared.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.shared.feature.auth.domain.datasource.TokenDataSource
import com.fintrack.shared.feature.auth.domain.model.AuthResponse
import com.fintrack.shared.feature.auth.domain.model.AuthState
import com.fintrack.shared.feature.auth.domain.model.LoginFormState
import com.fintrack.shared.feature.auth.domain.model.RegisterFormState
import com.fintrack.shared.feature.auth.domain.repository.AuthRepository
import com.fintrack.shared.feature.auth.domain.usecase.LoginValidationUseCase
import com.fintrack.shared.feature.auth.domain.usecase.RegisterValidationUseCase
import com.fintrack.shared.feature.core.domain.ValidationResult
import com.fintrack.shared.feature.core.domain.ValidationTrigger
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.core.data.model.ApiException
import com.fintrack.shared.feature.settings.domain.datasource.SettingsDataSource
import com.fintrack.shared.feature.user.domain.repository.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository,
    private val userRepository: UserRepository,
    private val tokenDataSource: TokenDataSource,
    private val registerValidationUseCase: RegisterValidationUseCase,
    private val loginValidationUseCase: LoginValidationUseCase,
    private val settingsDataSource: SettingsDataSource
) : ViewModel() {

    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _loginState = MutableStateFlow<AuthState<AuthResponse>>(AuthState.Idle)
    val loginState: StateFlow<AuthState<AuthResponse>> = _loginState

    private val _registerState = MutableStateFlow<AuthState<AuthResponse>>(AuthState.Idle)
    val registerState: StateFlow<AuthState<AuthResponse>> = _registerState

    private val _authStatus = MutableStateFlow<AuthState<Boolean>>(AuthState.Idle)
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
        checkAuthenticationStatus()
        observeTokenChanges()
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

    private fun observeTokenChanges() {
        viewModelScope.launch {
            tokenDataSource.accessToken.collect { token ->
                val currentStatus = _authStatus.value
                if (token == null) {
                    // When token is cleared, ensure we transition to non-auth state and reset screen states
                    if (currentStatus != AuthState.Success(false)) {
                        _authStatus.value = AuthState.Success(false)
                    }
                    clearAuthStates()
                } else {
                    val isGloballyAuthenticated =
                        (currentStatus as? AuthState.Success)?.data ?: false
                    if (!isGloballyAuthenticated) {
                        // If we are currently logging in or just succeeded, delay to let UI show success on the button
                        if (_loginState.value !is AuthState.Idle || _registerState.value !is AuthState.Idle) {
                            delay(1500)
                        }
                        _authStatus.value = AuthState.Success(true)
                    }
                }
            }
        }
    }

    fun updateLoginEmail(email: String) {
        val currentState = _loginFormState.value
        val validationResult = loginValidationUseCase(email, currentState.password)
        val newState = currentState.copy(
            email = email,
            emailError = null, // Hide error while typing
            activeError = if (currentState.activeError == currentState.emailError) null else currentState.activeError,
            isFormValid = validationResult.isValid
        )
        _loginFormState.value = newState
    }

    fun validateLoginEmail(trigger: ValidationTrigger = ValidationTrigger.Manual) {
        val currentState = _loginFormState.value

        val result = loginValidationUseCase(currentState.email, currentState.password)
        val emailError = (result.emailResult as? ValidationResult.Error)?.message

        // Mark as dirty on FocusLoss (leaving the field)
        val isDirty = currentState.isEmailDirty || trigger == ValidationTrigger.FocusLoss
        val showError = isDirty && emailError != null

        val newState = currentState.copy(
            emailError = if (showError) emailError else null,
            activeError = (if (showError) emailError else null) ?: currentState.passwordError,
            isFormValid = result.isValid,
            isEmailDirty = isDirty
        )
        _loginFormState.value = newState
    }

    fun updateLoginPassword(password: String) {
        val currentState = _loginFormState.value
        val validationResult = loginValidationUseCase(currentState.email, password)
        val newState = currentState.copy(
            password = password,
            passwordError = null, // Hide error while typing
            activeError = if (currentState.activeError == currentState.passwordError) null else currentState.activeError,
            isFormValid = validationResult.isValid
        )
        _loginFormState.value = newState
    }

    fun validateLoginPassword(trigger: ValidationTrigger = ValidationTrigger.Manual) {
        val currentState = _loginFormState.value

        val result = loginValidationUseCase(currentState.email, currentState.password)
        val passwordError = (result.passwordResult as? ValidationResult.Error)?.message

        val isDirty = currentState.isPasswordDirty || trigger == ValidationTrigger.FocusLoss
        val showError = isDirty && passwordError != null

        val newState = currentState.copy(
            passwordError = if (showError) passwordError else null,
            activeError = (if (showError) passwordError else null) ?: currentState.emailError,
            isFormValid = result.isValid,
            isPasswordDirty = isDirty
        )
        _loginFormState.value = newState
    }

    fun login() {
        val formState = _loginFormState.value

        // Final validation check
        val result = loginValidationUseCase(formState.email, formState.password)
        if (!result.isValid) {
            val emailError = (result.emailResult as? ValidationResult.Error)?.message
            val passwordError = (result.passwordResult as? ValidationResult.Error)?.message
            _loginFormState.value = formState.copy(
                emailError = emailError,
                passwordError = passwordError,
                activeError = emailError ?: passwordError,
                isFormValid = result.isValid,
                isEmailDirty = true,
                isPasswordDirty = true
            )
            return
        }

        _loginState.value = AuthState.Loading("Logging in...")
        viewModelScope.launch {
            when (val result = repository.login(formState.email, formState.password)) {
                is Result.Success -> {
                    // Refresh user profile before proceeding
                    userRepository.refreshProfile()

                    // Set login state to success to show success on button
                    // The global authStatus will be updated by the token observer with a delay
                    _loginState.value = AuthState.Success(result.data)
                }

                is Result.Error -> {
                    _loginState.value = AuthState.Error(result.exception)
                }

                is Result.Loading -> {
                    _loginState.value = AuthState.Loading("Connecting...")
                }
            }
        }
    }

    fun updateName(name: String) {
        val currentState = _registerFormState.value
        val result = registerValidationUseCase(
            name = name,
            email = currentState.email,
            password = currentState.password,
            confirmPassword = currentState.confirmPassword
        )
        val newState = currentState.copy(
            name = name,
            nameError = null,
            activeError = if (currentState.activeError == currentState.nameError) null else currentState.activeError,
            isFormValid = result.isValid
        )
        _registerFormState.value = newState
    }

    fun validateName(trigger: ValidationTrigger = ValidationTrigger.Manual) {
        val currentState = _registerFormState.value

        val result = registerValidationUseCase(
            name = currentState.name,
            email = currentState.email,
            password = currentState.password,
            confirmPassword = currentState.confirmPassword
        )
        val nameError = (result.nameResult as? ValidationResult.Error)?.message

        val isDirty = currentState.isNameDirty || trigger == ValidationTrigger.FocusLoss
        val showError = isDirty && nameError != null

        val newState = currentState.copy(
            nameError = if (showError) nameError else null,
            activeError = (if (showError) nameError else null) ?: currentState.emailError
            ?: currentState.passwordError ?: currentState.confirmPasswordError,
            isFormValid = result.isValid,
            isNameDirty = isDirty
        )
        _registerFormState.value = newState
    }

    fun updateEmail(email: String) {
        val currentState = _registerFormState.value
        val result = registerValidationUseCase(
            name = currentState.name,
            email = email,
            password = currentState.password,
            confirmPassword = currentState.confirmPassword
        )
        val newState = currentState.copy(
            email = email,
            emailError = null,
            activeError = if (currentState.activeError == currentState.emailError) null else currentState.activeError,
            isFormValid = result.isValid
        )
        _registerFormState.value = newState
    }

    fun validateEmail(trigger: ValidationTrigger = ValidationTrigger.Manual) {
        val currentState = _registerFormState.value

        val result = registerValidationUseCase(
            name = currentState.name,
            email = currentState.email,
            password = currentState.password,
            confirmPassword = currentState.confirmPassword
        )
        val emailError = (result.emailResult as? ValidationResult.Error)?.message

        val isDirty = currentState.isEmailDirty || trigger == ValidationTrigger.FocusLoss
        val showError = isDirty && emailError != null

        val newState = currentState.copy(
            emailError = if (showError) emailError else null,
            activeError = (if (showError) emailError else null) ?: currentState.nameError
            ?: currentState.passwordError ?: currentState.confirmPasswordError,
            isFormValid = result.isValid,
            isEmailDirty = isDirty
        )
        _registerFormState.value = newState
    }

    fun updatePassword(password: String) {
        val currentState = _registerFormState.value
        val result = registerValidationUseCase(
            name = currentState.name,
            email = currentState.email,
            password = password,
            confirmPassword = currentState.confirmPassword
        )

        val newState = currentState.copy(
            password = password,
            passwordError = null,
            confirmPasswordError = if (result.confirmPasswordResult is ValidationResult.Success) null else currentState.confirmPasswordError,
            activeError = if (currentState.activeError == currentState.passwordError || (result.confirmPasswordResult is ValidationResult.Success && currentState.activeError == currentState.confirmPasswordError)) null else currentState.activeError,
            passwordStrength = result.passwordStrength,
            isFormValid = result.isValid
        )
        _registerFormState.value = newState
    }

    fun validatePassword(trigger: ValidationTrigger = ValidationTrigger.Manual) {
        val currentState = _registerFormState.value

        val result = registerValidationUseCase(
            name = currentState.name,
            email = currentState.email,
            password = currentState.password,
            confirmPassword = currentState.confirmPassword
        )
        val passwordError = (result.passwordResult as? ValidationResult.Error)?.message

        val isDirty = currentState.isPasswordDirty || trigger == ValidationTrigger.FocusLoss
        val showError = isDirty && passwordError != null
        
        // Also update confirm password error if it was a mismatch and is now resolved
        val confirmPasswordError = if (result.confirmPasswordResult is ValidationResult.Success) null else currentState.confirmPasswordError

        val newState = currentState.copy(
            passwordError = if (showError) passwordError else null,
            confirmPasswordError = confirmPasswordError,
            activeError = (if (showError) passwordError else null) ?: currentState.nameError
            ?: currentState.emailError ?: confirmPasswordError,
            isFormValid = result.isValid,
            isPasswordDirty = isDirty
        )
        _registerFormState.value = newState
    }

    fun updateConfirmPassword(confirmPassword: String) {
        val currentState = _registerFormState.value
        val result = registerValidationUseCase(
            name = currentState.name,
            email = currentState.email,
            password = currentState.password,
            confirmPassword = confirmPassword
        )
        val newState = currentState.copy(
            confirmPassword = confirmPassword,
            confirmPasswordError = null,
            passwordError = if (result.passwordResult is ValidationResult.Success) null else currentState.passwordError,
            activeError = if (currentState.activeError == currentState.confirmPasswordError || (result.passwordResult is ValidationResult.Success && currentState.activeError == currentState.passwordError)) null else currentState.activeError,
            isFormValid = result.isValid
        )
        _registerFormState.value = newState
    }

    fun validateConfirmPassword(trigger: ValidationTrigger = ValidationTrigger.Manual) {
        val currentState = _registerFormState.value

        val result = registerValidationUseCase(
            name = currentState.name,
            email = currentState.email,
            password = currentState.password,
            confirmPassword = currentState.confirmPassword
        )
        val confirmPasswordError =
            (result.confirmPasswordResult as? ValidationResult.Error)?.message

        val isDirty = currentState.isConfirmPasswordDirty || trigger == ValidationTrigger.FocusLoss
        val showError = isDirty && confirmPasswordError != null

        // Also update password error if it was resolved
        val passwordError = if (result.passwordResult is ValidationResult.Success) null else currentState.passwordError

        val newState = currentState.copy(
            confirmPasswordError = if (showError) confirmPasswordError else null,
            passwordError = passwordError,
            activeError = (if (showError) confirmPasswordError else null) ?: currentState.nameError
            ?: currentState.emailError ?: passwordError,
            isFormValid = result.isValid,
            isConfirmPasswordDirty = isDirty
        )
        _registerFormState.value = newState
    }

    fun register() {
        val formState = _registerFormState.value

        // Final validation check
        val result = registerValidationUseCase(
            name = formState.name,
            email = formState.email,
            password = formState.password,
            confirmPassword = formState.confirmPassword
        )

        if (!result.isValid) {
            val nameError = (result.nameResult as? ValidationResult.Error)?.message
            val emailError = (result.emailResult as? ValidationResult.Error)?.message
            val passwordError = (result.passwordResult as? ValidationResult.Error)?.message
            val confirmPasswordError =
                (result.confirmPasswordResult as? ValidationResult.Error)?.message

            _registerFormState.value = formState.copy(
                nameError = nameError,
                emailError = emailError,
                passwordError = passwordError,
                confirmPasswordError = confirmPasswordError,
                activeError = nameError ?: emailError ?: passwordError ?: confirmPasswordError,
                isFormValid = result.isValid,
                isNameDirty = true,
                isEmailDirty = true,
                isPasswordDirty = true,
                isConfirmPasswordDirty = true
            )
            return
        }

        // Clear errors if validation passes
        _registerFormState.value = formState.copy(
            nameError = null,
            emailError = null,
            passwordError = null,
            confirmPasswordError = null,
            activeError = null,
            isFormValid = true
        )

        _registerState.value = AuthState.Loading("Creating account...")
        viewModelScope.launch {
            when (val result =
                repository.register(formState.name, formState.email, formState.password)) {
                is Result.Success -> {
                    // Refresh user profile before proceeding
                    userRepository.refreshProfile()

                    // Set register state to success to show success on button
                    // The global authStatus will be updated by the token observer with a delay
                    _registerState.value = AuthState.Success(result.data)
                }

                is Result.Error -> {
                    _registerState.value = AuthState.Error(result.exception)
                }

                is Result.Loading -> {
                    _registerState.value = AuthState.Loading("Processing...")
                }
            }
        }
    }

    fun checkAuthenticationStatus() {
        viewModelScope.launch {
            val currentStatus = _authStatus.value
            // Only set loading if we are currently Idle or Error
            if (currentStatus is AuthState.Idle || currentStatus is AuthState.Error) {
                _authStatus.value = AuthState.Loading("Checking authentication...")
            }
            val currentToken = tokenDataSource.accessToken.first()

            if (currentToken == null) {
                _authStatus.value = AuthState.Success(false)
                return@launch
            }

            when (val result = repository.validateToken(currentToken)) {
                is Result.Success -> {
                    if (result.data) {
                        // Refresh profile on successful token validation (app start)
                        userRepository.refreshProfile()

                        _authStatus.value = AuthState.Success(true)
                    } else {
                        // Token is invalid but request succeeded (e.g. backend says false)
                        tokenDataSource.clearTokens()
                        _authStatus.value = AuthState.Success(false)
                    }
                }

                is Result.Error -> {
                    val exception = result.exception
                    // If the user no longer exists (404) or we are unauthorized (401),
                    // we must clear tokens and force a re-login instead of showing an error.
                    if (exception is ApiException.NotFound ||
                        exception is ApiException.Unauthorized
                    ) {
                        tokenDataSource.clearTokens()
                        userRepository.clearProfile()
                        _authStatus.value = AuthState.Success(false)
                    } else {
                        // On network error or other server errors, show error state to allow retry
                        _authStatus.value = AuthState.Error(exception)
                    }
                }

                is Result.Loading -> {
                    // Stay in loading if the repository returns loading
                }
            }
        }
    }

    fun clearAuthStates() {
        _loginState.value = AuthState.Idle
        _registerState.value = AuthState.Idle
        _loginFormState.value = LoginFormState()
        _registerFormState.value = RegisterFormState()
    }

    fun logout() {
        viewModelScope.launch {
            try {
                repository.logout()
            } catch (_: Exception) {
                // Silently logout
            } finally {
                tokenDataSource.clearTokens()
                userRepository.clearProfile()
                settingsDataSource.clear()
                _authStatus.value = AuthState.Success(false)

                // Reset states
                clearAuthStates()
            }
        }
    }
}