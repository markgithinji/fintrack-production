package com.fintrack.shared.feature.settings.ui

import com.fintrack.shared.feature.account.domain.repository.FakeAccountRepository
import com.fintrack.shared.feature.budget.domain.repository.FakeBudgetRepository
import com.fintrack.shared.feature.category.data.LocalCategoryDataSource
import com.fintrack.shared.feature.category.domain.repository.FakeCategoryRepository
import com.fintrack.shared.feature.category.domain.usecase.SyncCategoriesUseCase
import com.fintrack.shared.feature.core.domain.service.NotificationService
import com.fintrack.shared.feature.core.util.FakeFileSaver
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.settings.domain.datasource.SettingsDataSource
import com.fintrack.shared.feature.settings.domain.model.AppTheme
import com.fintrack.shared.feature.settings.domain.model.Currency
import com.fintrack.shared.feature.settings.domain.model.ExportFormat
import com.fintrack.shared.feature.settings.domain.model.TimeFormat
import com.fintrack.shared.feature.settings.domain.usecase.BackupRestoreUseCase
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.fintrack.shared.feature.transaction.domain.repository.FakeTransactionRepository
import com.fintrack.shared.feature.transaction.domain.service.FakeTransactionImporter
import com.fintrack.shared.feature.transaction.domain.usecase.ExportTransactionsUseCase
import com.fintrack.shared.feature.user.domain.model.User
import com.fintrack.shared.feature.user.domain.repository.UserRepository
import com.fintrack.shared.feature.user.domain.usecase.DeleteAccountUseCase
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeSettingsDataSource = FakeTestSettingsDataSource()
    private val fakeTransactionRepo = FakeTransactionRepository()
    private val fakeAccountRepo = FakeAccountRepository()
    private val fakeBudgetRepo = FakeBudgetRepository()
    private val fakeFileSaver = FakeFileSaver()
    private val exportTransactionsUseCase = ExportTransactionsUseCase(fakeTransactionRepo, fakeFileSaver)
    private val fakeNotificationService = FakeTestNotificationService()
    private val fakeUserRepository = FakeTestUserRepository()
    private val localCategoryDataSource = LocalCategoryDataSource()
    private val syncCategoriesUseCase = SyncCategoriesUseCase(FakeCategoryRepository(), localCategoryDataSource)
    private val deleteAccountUseCase = DeleteAccountUseCase(fakeUserRepository, fakeSettingsDataSource)

    private lateinit var viewModel: SettingsViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SettingsViewModel(
            settingsDataSource = fakeSettingsDataSource,
            exportTransactionsUseCase = exportTransactionsUseCase,
            notificationService = fakeNotificationService,
            deleteAccountUseCase = deleteAccountUseCase,
            userRepository = fakeUserRepository,
            localCategoryDataSource = localCategoryDataSource,
            syncCategoriesUseCase = syncCategoriesUseCase,
            accountRepository = fakeAccountRepo,
            budgetRepository = fakeBudgetRepo,
            transactionImporter = FakeTransactionImporter(),
            backupRestoreUseCase = null
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `setTheme updates app theme preference correctly`() = runTest(testDispatcher) {
        viewModel.setTheme(AppTheme.DARK)
        assertEquals(AppTheme.DARK, viewModel.theme.value)

        viewModel.setTheme(AppTheme.LIGHT)
        assertEquals(AppTheme.LIGHT, viewModel.theme.value)
    }

    @Test
    fun `setCurrency updates active currency preference`() = runTest(testDispatcher) {
        viewModel.setCurrency(Currency.USD)
        assertEquals(Currency.USD, viewModel.currency.value)
    }

    @Test
    fun `setBalanceHidden updates privacy toggle state`() = runTest(testDispatcher) {
        viewModel.setBalanceHidden(true)
        assertTrue(viewModel.isBalanceHidden.value)

        viewModel.setBalanceHidden(false)
        assertFalse(viewModel.isBalanceHidden.value)
    }

    @Test
    fun `setCategorySpikeAlertEnabled updates category spike alert preference`() = runTest(testDispatcher) {
        viewModel.setCategorySpikeAlertEnabled(false)
        assertFalse(viewModel.isCategorySpikeAlertEnabled.value)

        viewModel.setCategorySpikeAlertEnabled(true)
        assertTrue(viewModel.isCategorySpikeAlertEnabled.value)
    }

    @Test
    fun `setExportFormat updates selected export format`() = runTest(testDispatcher) {
        viewModel.setExportFormat(ExportFormat.JSON)
        assertEquals(ExportFormat.JSON, viewModel.exportFormat.value)
    }
}

private class FakeTestSettingsDataSource : SettingsDataSource {
    private val _theme = MutableStateFlow(AppTheme.SYSTEM)
    override val theme: StateFlow<AppTheme> = _theme.asStateFlow()
    override suspend fun setTheme(theme: AppTheme) { _theme.value = theme }

    private val _timeFormat = MutableStateFlow(TimeFormat.TWENTY_FOUR_HOUR)
    override val timeFormat: StateFlow<TimeFormat> = _timeFormat.asStateFlow()
    override suspend fun setTimeFormat(format: TimeFormat) { _timeFormat.value = format }

    private val _currency = MutableStateFlow(Currency.KES)
    override val currency: StateFlow<Currency> = _currency.asStateFlow()
    override suspend fun setCurrency(currency: Currency) { _currency.value = currency }

    private val _isBiometricEnabled = MutableStateFlow(false)
    override val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()
    override suspend fun setBiometricEnabled(enabled: Boolean) { _isBiometricEnabled.value = enabled }

    private val _biometricLockTimeoutSeconds = MutableStateFlow(30)
    override val biometricLockTimeoutSeconds: StateFlow<Int> = _biometricLockTimeoutSeconds.asStateFlow()
    override suspend fun setBiometricLockTimeoutSeconds(seconds: Int) { _biometricLockTimeoutSeconds.value = seconds }

    private val _isBalanceHidden = MutableStateFlow(false)
    override val isBalanceHidden: StateFlow<Boolean> = _isBalanceHidden.asStateFlow()
    override suspend fun setBalanceHidden(hidden: Boolean) { _isBalanceHidden.value = hidden }

    private val _isReminderEnabled = MutableStateFlow(false)
    override val isReminderEnabled: StateFlow<Boolean> = _isReminderEnabled.asStateFlow()
    override suspend fun setReminderEnabled(enabled: Boolean) { _isReminderEnabled.value = enabled }

    private val _reminderTime = MutableStateFlow(LocalTime(20, 0))
    override val reminderTime: StateFlow<LocalTime> = _reminderTime.asStateFlow()
    override suspend fun setReminderTime(time: LocalTime) { _reminderTime.value = time }

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

    private val _exportFormat = MutableStateFlow(ExportFormat.CSV)
    override val exportFormat: StateFlow<ExportFormat> = _exportFormat.asStateFlow()
    override suspend fun setExportFormat(format: ExportFormat) { _exportFormat.value = format }

    override val isSmsRationaleHidden: StateFlow<Boolean> = MutableStateFlow(false)
    override suspend fun setSmsRationaleHidden(hidden: Boolean) {}

    private val _isCategorySpikeAlertEnabled = MutableStateFlow(true)
    override val isCategorySpikeAlertEnabled: StateFlow<Boolean> = _isCategorySpikeAlertEnabled.asStateFlow()
    override suspend fun setCategorySpikeAlertEnabled(enabled: Boolean) { _isCategorySpikeAlertEnabled.value = enabled }

    override val userName: StateFlow<String> = MutableStateFlow("User")
    override suspend fun setUserName(name: String) {}

    override val isOnboardingCompleted: StateFlow<Boolean?> = MutableStateFlow(true)
    override suspend fun setOnboardingCompleted(completed: Boolean) {}

    override suspend fun clear() {}
}

private class FakeTestNotificationService : NotificationService {
    override fun showReminderNotification() {}
    override fun scheduleDailyReminder(time: LocalTime?) {}
    override fun cancelDailyReminder() {}
    override fun requestPermission(callback: (Boolean) -> Unit) { callback(true) }
    override fun showTransactionNotification(transaction: Transaction) {}
    override fun showBudgetAlertNotification(budgetName: String, threshold: Int) {}
    override fun showBillReminderNotification(billName: String, amount: BigDecimal) {}
    override fun scheduleBillReminder(billName: String, amount: BigDecimal, dueDate: LocalDate, daysBefore: Int) {}
    override fun showSummaryNotification(title: String, content: String) {}
    override fun scheduleSummaryNotification(time: LocalTime) {}
    override fun cancelSummaryNotification() {}
    override fun areNotificationsEnabled(): Boolean = true
}

private class FakeTestUserRepository : UserRepository {
    private val _userProfile = MutableStateFlow<User?>(User(name = "Test User", email = "test@example.com"))
    override fun getUserProfile(): StateFlow<User?> = _userProfile.asStateFlow()
    override suspend fun refreshProfile(): Result<User> = Result.Success(_userProfile.value!!)
    override suspend fun updateProfile(name: String, email: String): Result<User> {
        val user = User(name = name, email = email)
        _userProfile.value = user
        return Result.Success(user)
    }
    override suspend fun updateTrackedCategories(categories: List<String>): Result<User> = Result.Success(_userProfile.value!!)
    override suspend fun deleteAccount(): Result<Unit> = Result.Success(Unit)
    override fun clearProfile() { _userProfile.value = null }
}
