package com.fintrack.shared.feature.user.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.shared.feature.settings.domain.datasource.SettingsDataSource
import com.fintrack.shared.feature.user.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val settingsDataSource: SettingsDataSource,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _nameError = MutableStateFlow<String?>(null)
    val nameError: StateFlow<String?> = _nameError.asStateFlow()

    private val _isMpesaEnabled = MutableStateFlow(true)
    val isMpesaEnabled: StateFlow<Boolean> = _isMpesaEnabled.asStateFlow()

    private val _isEquityEnabled = MutableStateFlow(true)
    val isEquityEnabled: StateFlow<Boolean> = _isEquityEnabled.asStateFlow()

    private val _isBillRemindersEnabled = MutableStateFlow(true)
    val isBillRemindersEnabled: StateFlow<Boolean> = _isBillRemindersEnabled.asStateFlow()

    fun onNameChange(newName: String) {
        _name.value = newName
        if (newName.isNotBlank()) {
            _nameError.value = null
        }
    }

    fun toggleMpesa(enabled: Boolean) {
        _isMpesaEnabled.value = enabled
    }

    fun toggleEquity(enabled: Boolean) {
        _isEquityEnabled.value = enabled
    }

    fun toggleBillReminders(enabled: Boolean) {
        _isBillRemindersEnabled.value = enabled
    }

    fun completeOnboarding(onComplete: () -> Unit) {
        val userName = _name.value.trim()
        if (userName.isBlank()) {
            _nameError.value = "Please enter your name"
            return
        }

        viewModelScope.launch {
            userRepository.updateProfile(userName, "user@fintrack.local")
            settingsDataSource.setMpesaListenerEnabled(_isMpesaEnabled.value)
            settingsDataSource.setEquityListenerEnabled(_isEquityEnabled.value)
            settingsDataSource.setBillReminderEnabled(_isBillRemindersEnabled.value)
            settingsDataSource.setOnboardingCompleted(true)
            onComplete()
        }
    }
}
