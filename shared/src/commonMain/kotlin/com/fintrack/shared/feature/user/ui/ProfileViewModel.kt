package com.fintrack.shared.feature.user.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.shared.feature.core.domain.SaveState
import com.fintrack.shared.feature.core.domain.ValidationResult
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.summary.domain.model.ProfileMetrics
import com.fintrack.shared.feature.summary.domain.repository.SummaryRepository
import com.fintrack.shared.feature.user.domain.model.User
import com.fintrack.shared.feature.user.domain.repository.UserRepository
import com.fintrack.shared.feature.user.domain.usecase.ProfileValidationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val validationUseCase: ProfileValidationUseCase,
    private val summaryRepository: SummaryRepository
) : ViewModel() {

    private val _profileState = MutableStateFlow<Result<User>>(
        userRepository.getUserProfile().value?.let { Result.Success(it) } ?: Result.Loading
    )

    private val _metricsState = MutableStateFlow<Result<ProfileMetrics>>(Result.Loading)
    val metricsState: StateFlow<Result<ProfileMetrics>> = _metricsState.asStateFlow()

    private val _editState = MutableStateFlow<SaveState<Unit>>(SaveState.Idle)
    val editState: StateFlow<SaveState<Unit>> = _editState.asStateFlow()

    private val _formState = MutableStateFlow(
        userRepository.getUserProfile().value?.let { 
            ProfileFormState(name = it.name, email = it.email) 
        } ?: ProfileFormState()
    )
    val formState: StateFlow<ProfileFormState> = _formState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.getUserProfile().collect { user ->
                if (user != null) {
                    _profileState.value = Result.Success(user)
                    _formState.update { state ->
                        state.copy(name = user.name, email = user.email)
                    }
                }
            }
        }
    }

    fun onNameChange(name: String) {
        _formState.update { it.copy(name = name, nameError = null) }
    }

    fun onEmailChange(email: String) {
        _formState.update { it.copy(email = email, emailError = null) }
    }

    fun updateProfile() {
        val name = _formState.value.name
        val email = _formState.value.email

        val result = validationUseCase(name, email)

        if (!result.isValid) {
            _formState.update {
                it.copy(
                    nameError = (result.nameResult as? ValidationResult.Error)?.message,
                    emailError = (result.emailResult as? ValidationResult.Error)?.message
                )
            }
            return
        }

        viewModelScope.launch {
            _editState.value = SaveState.Loading
            val result = userRepository.updateProfile(name, email)
            
            _editState.value = when (result) {
                is Result.Success -> {
                    _profileState.value = Result.Success(result.data)
                    SaveState.Success(Unit)
                }
                is Result.Error -> SaveState.Error(result.exception)
                is Result.Loading -> SaveState.Loading
            }
        }
    }

    fun resetEditState() {
        _editState.value = SaveState.Idle
    }

    fun refreshProfile() {
        viewModelScope.launch {
            // Refresh profile summary (Metrics + User Info)
            if (_metricsState.value !is Result.Success) {
                _metricsState.value = Result.Loading
            }
            
            val result = summaryRepository.getProfileMetrics()
            _metricsState.value = result

            // If we got fresh user info, we can also update the form state
            if (result is Result.Success) {
                _formState.update { it.copy(name = result.data.name, email = result.data.email) }
            }
        }
    }
}

data class ProfileFormState(
    val name: String = "",
    val nameError: String? = null,
    val email: String = "",
    val emailError: String? = null
)
