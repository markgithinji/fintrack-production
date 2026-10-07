package com.fintrack.shared.feature.user.ui.onboarding

import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.settings.domain.datasource.SettingsDataSource
import com.fintrack.shared.feature.settings.domain.model.*
import com.fintrack.shared.feature.user.domain.model.User
import com.fintrack.shared.feature.user.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeSettingsDataSource : SettingsDataSource {
        var onboardingCompleted = false
        private val _onboardingCompleted = MutableStateFlow<Boolean?>(false)
        override val isOnboardingCompleted: StateFlow<Boolean?> = _onboardingCompleted.asStateFlow()
        override suspend fun setOnboardingCompleted(completed: Boolean) {
            _onboardingCompleted.value = completed
            onboardingCompleted = completed
        }

        override val theme: StateFlow<AppTheme> = MutableStateFlow(AppTheme.SYSTEM)
        override suspend fun setTheme(theme: AppTheme) {}
        override val timeFormat: StateFlow<TimeFormat> = MutableStateFlow(TimeFormat.TWENTY_FOUR_HOUR)
        override suspend fun setTimeFormat(format: TimeFormat) {}
        override val currency: StateFlow<Currency> = MutableStateFlow(Currency.KES)
        override suspend fun setCurrency(currency: Currency) {}
        override val isBiometricEnabled: StateFlow<Boolean> = MutableStateFlow(false)
        override suspend fun setBiometricEnabled(enabled: Boolean) {}
        override val isBalanceHidden: StateFlow<Boolean> = MutableStateFlow(false)
        override suspend fun setBalanceHidden(hidden: Boolean) {}
        override val isReminderEnabled: StateFlow<Boolean> = MutableStateFlow(false)
        override suspend fun setReminderEnabled(enabled: Boolean) {}
        override val reminderTime: StateFlow<LocalTime> = MutableStateFlow(LocalTime(20, 0))
        override suspend fun setReminderTime(time: LocalTime) {}
        override val mpesaSimSlot: StateFlow<Int?> = MutableStateFlow(null)
        override suspend fun setMpesaSimSlot(slot: Int?) {}
        override val mpesaLinkedAccountIds: StateFlow<Set<String>> = MutableStateFlow(emptySet())
        override suspend fun setMpesaLinkedAccountIds(ids: Set<String>) {}
        override val equityLinkedAccountIds: StateFlow<Set<String>> = MutableStateFlow(emptySet())
        override suspend fun setEquityLinkedAccountIds(ids: Set<String>) {}
        override val isMpesaListenerEnabled: StateFlow<Boolean> = MutableStateFlow(false)
        override suspend fun setMpesaListenerEnabled(enabled: Boolean) {}
        override val isEquityListenerEnabled: StateFlow<Boolean> = MutableStateFlow(false)
        override suspend fun setEquityListenerEnabled(enabled: Boolean) {}
        override val budgetAlertsEnabled: StateFlow<Boolean> = MutableStateFlow(false)
        override suspend fun setBudgetAlertsEnabled(enabled: Boolean) {}
        override val budgetAlertThresholds: StateFlow<Set<Int>> = MutableStateFlow(emptySet())
        override suspend fun setBudgetAlertThresholds(thresholds: Set<Int>) {}
        override val alertBudgetId: StateFlow<String?> = MutableStateFlow(null)
        override suspend fun setAlertBudgetId(budgetId: String?) {}
        override val isBillReminderEnabled: StateFlow<Boolean> = MutableStateFlow(false)
        override suspend fun setBillReminderEnabled(enabled: Boolean) {}
        override val billReminderDaysBefore: StateFlow<Int> = MutableStateFlow(3)
        override suspend fun setBillReminderDaysBefore(days: Int) {}
        override val isDailySummaryEnabled: StateFlow<Boolean> = MutableStateFlow(false)
        override suspend fun setDailySummaryEnabled(enabled: Boolean) {}
        override val isWeeklySummaryEnabled: StateFlow<Boolean> = MutableStateFlow(false)
        override suspend fun setWeeklySummaryEnabled(enabled: Boolean) {}
        override val summaryNotificationTime: StateFlow<LocalTime> = MutableStateFlow(LocalTime(20, 0))
        override suspend fun setSummaryNotificationTime(time: LocalTime) {}
        override val showDecimals: StateFlow<Boolean> = MutableStateFlow(true)
        override suspend fun setShowDecimals(show: Boolean) {}
        override val defaultAccountId: StateFlow<String?> = MutableStateFlow(null)
        override suspend fun setDefaultAccountId(id: String?) {}
        override val exportFormat: StateFlow<ExportFormat> = MutableStateFlow(ExportFormat.CSV)
        override suspend fun setExportFormat(format: ExportFormat) {}
        override val isSmsRationaleHidden: StateFlow<Boolean> = MutableStateFlow(false)
        override suspend fun setSmsRationaleHidden(hidden: Boolean) {}
        override val userName: StateFlow<String> = MutableStateFlow("User")
        override suspend fun setUserName(name: String) {}
        override suspend fun clear() {}
    }

    private class FakeUserRepository : UserRepository {
        var updatedName: String? = null
        private val _userProfile = MutableStateFlow<User?>(null)
        override fun getUserProfile(): StateFlow<User?> = _userProfile.asStateFlow()
        override suspend fun refreshProfile(): Result<User> = Result.Error(Exception())
        override suspend fun updateProfile(name: String, email: String): Result<User> {
            updatedName = name
            val user = User(name = name, email = email)
            _userProfile.value = user
            return Result.Success(user)
        }
        override suspend fun updateTrackedCategories(categories: List<String>): Result<User> = Result.Error(Exception())
        override suspend fun deleteAccount(): Result<Unit> = Result.Success(Unit)
        override fun clearProfile() { _userProfile.value = null }
    }

    @Test
    fun `complete onboarding updates user profile and settings`() = runTest(testDispatcher) {
        val settings = FakeSettingsDataSource()
        val userRepo = FakeUserRepository()
        val viewModel = OnboardingViewModel(settings, userRepo)

        viewModel.onNameChange("Alice")
        var completed = false
        viewModel.completeOnboarding { completed = true }

        assertEquals(true, completed)
        assertEquals("Alice", userRepo.updatedName)
        assertEquals(true, settings.onboardingCompleted)
    }
}
