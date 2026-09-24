package com.fintrack.shared.feature.settings.data.local

import com.fintrack.shared.feature.settings.domain.datasource.SettingsDataSource
import com.fintrack.shared.feature.settings.domain.model.AppTheme
import com.fintrack.shared.feature.settings.domain.model.Currency
import com.fintrack.shared.feature.settings.domain.model.ExportFormat
import com.fintrack.shared.feature.settings.domain.model.TimeFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalTime
import platform.Foundation.NSUserDefaults

class IOSSettingsDataSource : SettingsDataSource {
    private val defaults = NSUserDefaults.standardUserDefaults

    private val _theme = MutableStateFlow(AppTheme.fromName(defaults.stringForKey("app_theme") ?: AppTheme.SYSTEM.name))
    override val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    override suspend fun setTheme(theme: AppTheme) {
        defaults.setObject(theme.name, "app_theme")
        _theme.value = theme
    }

    private val _timeFormat = MutableStateFlow(TimeFormat.fromName(defaults.stringForKey("time_format") ?: TimeFormat.TWENTY_FOUR_HOUR.name))
    override val timeFormat: StateFlow<TimeFormat> = _timeFormat.asStateFlow()

    override suspend fun setTimeFormat(format: TimeFormat) {
        defaults.setObject(format.name, "time_format")
        _timeFormat.value = format
    }

    private val _currency = MutableStateFlow(Currency.fromCode(defaults.stringForKey("currency_code") ?: Currency.KES.code))
    override val currency: StateFlow<Currency> = _currency.asStateFlow()

    override suspend fun setCurrency(currency: Currency) {
        defaults.setObject(currency.code, "currency_code")
        _currency.value = currency
    }

    private val _isBiometricEnabled = MutableStateFlow(defaults.boolForKey("biometric_enabled"))
    override val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        defaults.setBool(enabled, "biometric_enabled")
        _isBiometricEnabled.value = enabled
    }

    private val _isBalanceHidden = MutableStateFlow(defaults.boolForKey("balance_hidden"))
    override val isBalanceHidden: StateFlow<Boolean> = _isBalanceHidden.asStateFlow()

    override suspend fun setBalanceHidden(hidden: Boolean) {
        defaults.setBool(hidden, "balance_hidden")
        _isBalanceHidden.value = hidden
    }

    private val _isReminderEnabled = MutableStateFlow(defaults.boolForKey("reminder_enabled"))
    override val isReminderEnabled: StateFlow<Boolean> = _isReminderEnabled.asStateFlow()

    override suspend fun setReminderEnabled(enabled: Boolean) {
        defaults.setBool(enabled, "reminder_enabled")
        _isReminderEnabled.value = enabled
    }

    private val _reminderTime = MutableStateFlow(
        try { LocalTime.parse(defaults.stringForKey("reminder_time") ?: "20:00") } catch (_: Exception) { LocalTime(20, 0) }
    )
    override val reminderTime: StateFlow<LocalTime> = _reminderTime.asStateFlow()

    override suspend fun setReminderTime(time: LocalTime) {
        defaults.setObject(time.toString(), "reminder_time")
        _reminderTime.value = time
    }

    private val _mpesaSimSlot = MutableStateFlow<Int?>(null)
    override val mpesaSimSlot: StateFlow<Int?> = _mpesaSimSlot.asStateFlow()

    override suspend fun setMpesaSimSlot(slot: Int?) {
        _mpesaSimSlot.value = slot
    }

    private val _mpesaLinkedAccountIds = MutableStateFlow<Set<String>>(emptySet())
    override val mpesaLinkedAccountIds: StateFlow<Set<String>> = _mpesaLinkedAccountIds.asStateFlow()

    override suspend fun setMpesaLinkedAccountIds(ids: Set<String>) {
        _mpesaLinkedAccountIds.value = ids
    }

    private val _equityLinkedAccountIds = MutableStateFlow<Set<String>>(emptySet())
    override val equityLinkedAccountIds: StateFlow<Set<String>> = _equityLinkedAccountIds.asStateFlow()

    override suspend fun setEquityLinkedAccountIds(ids: Set<String>) {
        _equityLinkedAccountIds.value = ids
    }

    private val _isMpesaListenerEnabled = MutableStateFlow(defaults.boolForKey("mpesa_listener_enabled"))
    override val isMpesaListenerEnabled: StateFlow<Boolean> = _isMpesaListenerEnabled.asStateFlow()

    override suspend fun setMpesaListenerEnabled(enabled: Boolean) {
        defaults.setBool(enabled, "mpesa_listener_enabled")
        _isMpesaListenerEnabled.value = enabled
    }

    private val _isEquityListenerEnabled = MutableStateFlow(defaults.boolForKey("equity_listener_enabled"))
    override val isEquityListenerEnabled: StateFlow<Boolean> = _isEquityListenerEnabled.asStateFlow()

    override suspend fun setEquityListenerEnabled(enabled: Boolean) {
        defaults.setBool(enabled, "equity_listener_enabled")
        _isEquityListenerEnabled.value = enabled
    }

    private val _budgetAlertsEnabled = MutableStateFlow(defaults.boolForKey("budget_alerts_enabled"))
    override val budgetAlertsEnabled: StateFlow<Boolean> = _budgetAlertsEnabled.asStateFlow()

    override suspend fun setBudgetAlertsEnabled(enabled: Boolean) {
        defaults.setBool(enabled, "budget_alerts_enabled")
        _budgetAlertsEnabled.value = enabled
    }

    private val _budgetAlertThresholds = MutableStateFlow<Set<Int>>(setOf(80, 100))
    override val budgetAlertThresholds: StateFlow<Set<Int>> = _budgetAlertThresholds.asStateFlow()

    override suspend fun setBudgetAlertThresholds(thresholds: Set<Int>) {
        _budgetAlertThresholds.value = thresholds
    }

    private val _alertBudgetId = MutableStateFlow<String?>(defaults.stringForKey("alert_budget_id"))
    override val alertBudgetId: StateFlow<String?> = _alertBudgetId.asStateFlow()

    override suspend fun setAlertBudgetId(budgetId: String?) {
        if (budgetId != null) defaults.setObject(budgetId, "alert_budget_id") else defaults.removeObjectForKey("alert_budget_id")
        _alertBudgetId.value = budgetId
    }

    private val _isBillReminderEnabled = MutableStateFlow(defaults.boolForKey("bill_reminder_enabled"))
    override val isBillReminderEnabled: StateFlow<Boolean> = _isBillReminderEnabled.asStateFlow()

    override suspend fun setBillReminderEnabled(enabled: Boolean) {
        defaults.setBool(enabled, "bill_reminder_enabled")
        _isBillReminderEnabled.value = enabled
    }

    private val _billReminderDaysBefore = MutableStateFlow(defaults.integerForKey("bill_reminder_days").toInt().let { if (it <= 0) 3 else it })
    override val billReminderDaysBefore: StateFlow<Int> = _billReminderDaysBefore.asStateFlow()

    override suspend fun setBillReminderDaysBefore(days: Int) {
        defaults.setInteger(days.toLong(), "bill_reminder_days")
        _billReminderDaysBefore.value = days
    }

    private val _isDailySummaryEnabled = MutableStateFlow(defaults.boolForKey("daily_summary_enabled"))
    override val isDailySummaryEnabled: StateFlow<Boolean> = _isDailySummaryEnabled.asStateFlow()

    override suspend fun setDailySummaryEnabled(enabled: Boolean) {
        defaults.setBool(enabled, "daily_summary_enabled")
        _isDailySummaryEnabled.value = enabled
    }

    private val _isWeeklySummaryEnabled = MutableStateFlow(defaults.boolForKey("weekly_summary_enabled"))
    override val isWeeklySummaryEnabled: StateFlow<Boolean> = _isWeeklySummaryEnabled.asStateFlow()

    override suspend fun setWeeklySummaryEnabled(enabled: Boolean) {
        defaults.setBool(enabled, "weekly_summary_enabled")
        _isWeeklySummaryEnabled.value = enabled
    }

    private val _summaryNotificationTime = MutableStateFlow(
        try { LocalTime.parse(defaults.stringForKey("summary_notification_time") ?: "20:00") } catch (_: Exception) { LocalTime(20, 0) }
    )
    override val summaryNotificationTime: StateFlow<LocalTime> = _summaryNotificationTime.asStateFlow()

    override suspend fun setSummaryNotificationTime(time: LocalTime) {
        defaults.setObject(time.toString(), "summary_notification_time")
        _summaryNotificationTime.value = time
    }

    private val _showDecimals = MutableStateFlow(defaults.boolForKey("show_decimals"))
    override val showDecimals: StateFlow<Boolean> = _showDecimals.asStateFlow()

    override suspend fun setShowDecimals(show: Boolean) {
        defaults.setBool(show, "show_decimals")
        _showDecimals.value = show
    }

    private val _defaultAccountId = MutableStateFlow<String?>(defaults.stringForKey("default_account_id"))
    override val defaultAccountId: StateFlow<String?> = _defaultAccountId.asStateFlow()

    override suspend fun setDefaultAccountId(id: String?) {
        if (id != null) defaults.setObject(id, "default_account_id") else defaults.removeObjectForKey("default_account_id")
        _defaultAccountId.value = id
    }

    private val _exportFormat = MutableStateFlow(
        try {
            ExportFormat.valueOf(defaults.stringForKey("export_format") ?: ExportFormat.CSV.name)
        } catch (_: Exception) {
            ExportFormat.CSV
        }
    )
    override val exportFormat: StateFlow<ExportFormat> = _exportFormat.asStateFlow()

    override suspend fun setExportFormat(format: ExportFormat) {
        defaults.setObject(format.name, "export_format")
        _exportFormat.value = format
    }

    private val _isSmsRationaleHidden = MutableStateFlow(defaults.boolForKey("sms_rationale_hidden"))
    override val isSmsRationaleHidden: StateFlow<Boolean> = _isSmsRationaleHidden.asStateFlow()

    override suspend fun setSmsRationaleHidden(hidden: Boolean) {
        defaults.setBool(hidden, "sms_rationale_hidden")
        _isSmsRationaleHidden.value = hidden
    }

    private val _userName = MutableStateFlow(defaults.stringForKey("user_name") ?: "User")
    override val userName: StateFlow<String> = _userName.asStateFlow()

    override suspend fun setUserName(name: String) {
        defaults.setObject(name, "user_name")
        _userName.value = name
    }

    private val _isOnboardingCompleted = MutableStateFlow<Boolean?>(
        if (defaults.objectForKey("onboarding_completed") != null) defaults.boolForKey("onboarding_completed") else false
    )
    override val isOnboardingCompleted: StateFlow<Boolean?> = _isOnboardingCompleted.asStateFlow()

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        defaults.setBool(completed, "onboarding_completed")
        _isOnboardingCompleted.value = completed
    }

    override suspend fun clear() {
        defaults.removeObjectForKey("app_theme")
        defaults.removeObjectForKey("currency_code")
        defaults.removeObjectForKey("user_name")
        defaults.removeObjectForKey("onboarding_completed")
    }
}
