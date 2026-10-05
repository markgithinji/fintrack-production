package com.fintrack.shared.feature.budget.domain.usecase

import com.fintrack.shared.feature.budget.domain.model.Budget
import com.fintrack.shared.feature.budget.domain.model.BudgetStatus
import com.fintrack.shared.feature.budget.domain.model.BudgetWithStatus
import com.fintrack.shared.feature.budget.domain.repository.BudgetRepository
import com.fintrack.shared.feature.core.domain.service.NotificationService
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.settings.domain.datasource.SettingsDataSource
import com.fintrack.shared.feature.settings.domain.model.*
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CheckBudgetThresholdsUseCaseTest {

    private class FakeNotificationService : NotificationService {
        var alertShown = false
        var shownBudgetName: String? = null
        var shownThreshold: Int? = null

        override fun showBudgetAlertNotification(budgetName: String, threshold: Int) {
            alertShown = true
            shownBudgetName = budgetName
            shownThreshold = threshold
        }

        override fun showReminderNotification() {}
        override fun scheduleDailyReminder(time: LocalTime?) {}
        fun scheduleDailyReminder() {}
        override fun cancelDailyReminder() {}
        override fun requestPermission(callback: (Boolean) -> Unit) {}
        override fun showTransactionNotification(transaction: Transaction) {}
        override fun showBillReminderNotification(billName: String, amount: BigDecimal) {}
        override fun scheduleBillReminder(billName: String, amount: BigDecimal, dueDate: LocalDate, daysBefore: Int) {}
        override fun showSummaryNotification(title: String, content: String) {}
        override fun scheduleSummaryNotification(time: LocalTime) {}
        override fun cancelSummaryNotification() {}
        override fun areNotificationsEnabled(): Boolean = true
    }

    private class FakeSettingsDataSource : SettingsDataSource {
        private val _budgetAlertsEnabled = MutableStateFlow(true)
        override val budgetAlertsEnabled: StateFlow<Boolean> = _budgetAlertsEnabled.asStateFlow()
        override suspend fun setBudgetAlertsEnabled(enabled: Boolean) { _budgetAlertsEnabled.value = enabled }

        private val _budgetAlertThresholds = MutableStateFlow(setOf(80, 100))
        override val budgetAlertThresholds: StateFlow<Set<Int>> = _budgetAlertThresholds.asStateFlow()
        override suspend fun setBudgetAlertThresholds(thresholds: Set<Int>) { _budgetAlertThresholds.value = thresholds }

        private val _alertBudgetId = MutableStateFlow<String?>("b_1")
        override val alertBudgetId: StateFlow<String?> = _alertBudgetId.asStateFlow()
        override suspend fun setAlertBudgetId(budgetId: String?) { _alertBudgetId.value = budgetId }

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
        override val isOnboardingCompleted: StateFlow<Boolean?> = MutableStateFlow(true)
        override suspend fun setOnboardingCompleted(completed: Boolean) {}
        override suspend fun clear() {}
    }

    @Test
    fun `triggers notification when threshold is reached`() = runTest {
        val budgetsWithStatus = listOf(
            BudgetWithStatus(
                budget = Budget(
                    id = "b_1",
                    accountIds = listOf("acc_1"),
                    name = "Food",
                    categories = emptyList(),
                    limit = 1000.0.toBigDecimal(),
                    isExpense = true,
                    startDate = LocalDate(2026, 1, 1),
                    endDate = LocalDate(2026, 1, 31)
                ),
                status = BudgetStatus(
                    spent = 850.0.toBigDecimal(),
                    remaining = 150.0.toBigDecimal(),
                    percentageUsed = 85.0.toBigDecimal(),
                    isExceeded = false
                )
            )
        )

        val fakeRepo = object : BudgetRepository {
            override suspend fun getBudgets(accountId: String?): Result<List<BudgetWithStatus>> {
                return Result.Success(budgetsWithStatus)
            }
            override suspend fun getBudgetById(id: String): Result<BudgetWithStatus> = Result.Error(Exception("Not found"))
            override suspend fun addOrUpdateBudget(budget: Budget): Result<Budget> = Result.Success(budget)
            override suspend fun deleteBudget(id: String): Result<Unit> = Result.Success(Unit)
            override suspend fun deleteAllBudgets(accountIds: List<String>?): Result<Unit> = Result.Success(Unit)
        }

        val notificationService = FakeNotificationService()
        val settingsDataSource = FakeSettingsDataSource()
        val useCase = CheckBudgetThresholdsUseCase(fakeRepo, settingsDataSource, notificationService)

        useCase()

        assertTrue(notificationService.alertShown)
        assertEquals("Food", notificationService.shownBudgetName)
        assertEquals(80, notificationService.shownThreshold)
    }
}
