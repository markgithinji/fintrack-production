package com.fintrack.shared.feature.settings.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.fintrack.shared.feature.settings.domain.datasource.SettingsDataSource
import com.fintrack.shared.feature.settings.domain.model.AppTheme
import com.fintrack.shared.feature.settings.domain.model.Currency
import com.fintrack.shared.feature.settings.domain.model.ExportFormat
import com.fintrack.shared.feature.settings.domain.model.TimeFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.datetime.LocalTime

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "fintrack_settings",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, "fintrack_settings"))
    }
)

class AndroidSettingsDataSource(
    private val context: Context,
) : SettingsDataSource {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override val theme: StateFlow<AppTheme> = context.settingsDataStore.data
        .map { prefs -> AppTheme.fromName(prefs[Keys.APP_THEME] ?: AppTheme.SYSTEM.name) }
        .stateIn(scope, SharingStarted.Eagerly, AppTheme.SYSTEM)

    override suspend fun setTheme(theme: AppTheme) {
        context.settingsDataStore.edit { it[Keys.APP_THEME] = theme.name }
    }

    override val timeFormat: StateFlow<TimeFormat> = context.settingsDataStore.data
        .map { prefs -> TimeFormat.fromName(prefs[Keys.TIME_FORMAT] ?: TimeFormat.TWENTY_FOUR_HOUR.name) }
        .stateIn(scope, SharingStarted.Eagerly, TimeFormat.TWENTY_FOUR_HOUR)

    override suspend fun setTimeFormat(format: TimeFormat) {
        context.settingsDataStore.edit { it[Keys.TIME_FORMAT] = format.name }
    }

    override val currency: StateFlow<Currency> = context.settingsDataStore.data
        .map { prefs -> Currency.fromCode(prefs[Keys.CURRENCY_CODE] ?: Currency.KES.code) }
        .stateIn(scope, SharingStarted.Eagerly, Currency.KES)

    override suspend fun setCurrency(currency: Currency) {
        context.settingsDataStore.edit { it[Keys.CURRENCY_CODE] = currency.code }
    }

    override val isBiometricEnabled: StateFlow<Boolean> = context.settingsDataStore.data
        .map { it[Keys.BIOMETRIC_ENABLED] ?: false }
        .stateIn(scope, SharingStarted.Eagerly, false)

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    override val isBalanceHidden: StateFlow<Boolean> = context.settingsDataStore.data
        .map { it[Keys.BALANCE_HIDDEN] ?: false }
        .stateIn(scope, SharingStarted.Eagerly, false)

    override suspend fun setBalanceHidden(hidden: Boolean) {
        context.settingsDataStore.edit { it[Keys.BALANCE_HIDDEN] = hidden }
    }

    override val isReminderEnabled: StateFlow<Boolean> = context.settingsDataStore.data
        .map { it[Keys.REMINDER_ENABLED] ?: false }
        .stateIn(scope, SharingStarted.Eagerly, false)

    override suspend fun setReminderEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.REMINDER_ENABLED] = enabled }
    }

    override val reminderTime: StateFlow<LocalTime> = context.settingsDataStore.data
        .map { prefs -> LocalTime.parse(prefs[Keys.REMINDER_TIME] ?: "20:00") }
        .stateIn(scope, SharingStarted.Eagerly, LocalTime(20, 0))

    override suspend fun setReminderTime(time: LocalTime) {
        context.settingsDataStore.edit { it[Keys.REMINDER_TIME] = time.toString() }
    }

    override val mpesaSimSlot: StateFlow<Int?> = context.settingsDataStore.data
        .map { it[Keys.MPESA_SIM_SLOT] }
        .stateIn(scope, SharingStarted.Eagerly, null)

    override suspend fun setMpesaSimSlot(slot: Int?) {
        context.settingsDataStore.edit {
            if (slot == null) it.remove(Keys.MPESA_SIM_SLOT)
            else it[Keys.MPESA_SIM_SLOT] = slot
        }
    }

    override val mpesaLinkedAccountIds: StateFlow<Set<String>> = context.settingsDataStore.data
        .map { it[Keys.MPESA_LINKED_ACCOUNT_IDS] ?: emptySet() }
        .stateIn(scope, SharingStarted.Eagerly, emptySet())

    override suspend fun setMpesaLinkedAccountIds(ids: Set<String>) {
        context.settingsDataStore.edit { it[Keys.MPESA_LINKED_ACCOUNT_IDS] = ids }
    }

    override val equityLinkedAccountIds: StateFlow<Set<String>> = context.settingsDataStore.data
        .map { it[Keys.EQUITY_LINKED_ACCOUNT_IDS] ?: emptySet() }
        .stateIn(scope, SharingStarted.Eagerly, emptySet())

    override suspend fun setEquityLinkedAccountIds(ids: Set<String>) {
        context.settingsDataStore.edit { it[Keys.EQUITY_LINKED_ACCOUNT_IDS] = ids }
    }

    override val isMpesaListenerEnabled: StateFlow<Boolean> = context.settingsDataStore.data
        .map { it[Keys.MPESA_LISTENER_ENABLED] ?: false }
        .stateIn(scope, SharingStarted.Eagerly, false)

    override suspend fun setMpesaListenerEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.MPESA_LISTENER_ENABLED] = enabled }
    }

    override val isEquityListenerEnabled: StateFlow<Boolean> = context.settingsDataStore.data
        .map { it[Keys.EQUITY_LISTENER_ENABLED] ?: false }
        .stateIn(scope, SharingStarted.Eagerly, false)

    override suspend fun setEquityListenerEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.EQUITY_LISTENER_ENABLED] = enabled }
    }

    override val budgetAlertsEnabled: StateFlow<Boolean> = context.settingsDataStore.data
        .map { it[Keys.BUDGET_ALERTS_ENABLED] ?: false }
        .stateIn(scope, SharingStarted.Eagerly, false)

    override suspend fun setBudgetAlertsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.BUDGET_ALERTS_ENABLED] = enabled }
    }

    override val budgetAlertThresholds: StateFlow<Set<Int>> = context.settingsDataStore.data
        .map { prefs -> prefs[Keys.BUDGET_ALERT_THRESHOLDS]?.mapNotNull { it.toIntOrNull() }?.toSet() ?: setOf(50, 80, 100) }
        .stateIn(scope, SharingStarted.Eagerly, setOf(50, 80, 100))

    override suspend fun setBudgetAlertThresholds(thresholds: Set<Int>) {
        context.settingsDataStore.edit { it[Keys.BUDGET_ALERT_THRESHOLDS] = thresholds.map { it.toString() }.toSet() }
    }

    override val alertBudgetId: StateFlow<String?> = context.settingsDataStore.data
        .map { it[Keys.ALERT_BUDGET_ID] }
        .stateIn(scope, SharingStarted.Eagerly, null)

    override suspend fun setAlertBudgetId(budgetId: String?) {
        context.settingsDataStore.edit {
            if (budgetId == null) it.remove(Keys.ALERT_BUDGET_ID)
            else it[Keys.ALERT_BUDGET_ID] = budgetId
        }
    }

    override val isBillReminderEnabled: StateFlow<Boolean> = context.settingsDataStore.data
        .map { it[Keys.BILL_REMINDER_ENABLED] ?: false }
        .stateIn(scope, SharingStarted.Eagerly, false)

    override suspend fun setBillReminderEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.BILL_REMINDER_ENABLED] = enabled }
    }

    override val billReminderDaysBefore: StateFlow<Int> = context.settingsDataStore.data
        .map { it[Keys.BILL_REMINDER_DAYS_BEFORE] ?: 2 }
        .stateIn(scope, SharingStarted.Eagerly, 2)

    override suspend fun setBillReminderDaysBefore(days: Int) {
        context.settingsDataStore.edit { it[Keys.BILL_REMINDER_DAYS_BEFORE] = days }
    }

    override val isDailySummaryEnabled: StateFlow<Boolean> = context.settingsDataStore.data
        .map { it[Keys.DAILY_SUMMARY_ENABLED] ?: false }
        .stateIn(scope, SharingStarted.Eagerly, false)

    override suspend fun setDailySummaryEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.DAILY_SUMMARY_ENABLED] = enabled }
    }

    override val isWeeklySummaryEnabled: StateFlow<Boolean> = context.settingsDataStore.data
        .map { it[Keys.WEEKLY_SUMMARY_ENABLED] ?: false }
        .stateIn(scope, SharingStarted.Eagerly, false)

    override suspend fun setWeeklySummaryEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.WEEKLY_SUMMARY_ENABLED] = enabled }
    }

    override val summaryNotificationTime: StateFlow<LocalTime> = context.settingsDataStore.data
        .map { prefs -> LocalTime.parse(prefs[Keys.SUMMARY_NOTIFICATION_TIME] ?: "08:00") }
        .stateIn(scope, SharingStarted.Eagerly, LocalTime(8, 0))

    override suspend fun setSummaryNotificationTime(time: LocalTime) {
        context.settingsDataStore.edit { it[Keys.SUMMARY_NOTIFICATION_TIME] = time.toString() }
    }

    override val showDecimals: StateFlow<Boolean> = context.settingsDataStore.data
        .map { it[Keys.SHOW_DECIMALS] ?: true }
        .stateIn(scope, SharingStarted.Eagerly, initialValue = true)

    override suspend fun setShowDecimals(show: Boolean) {
        context.settingsDataStore.edit { it[Keys.SHOW_DECIMALS] = show }
    }

    override val defaultAccountId: StateFlow<String?> = context.settingsDataStore.data
        .map { it[Keys.DEFAULT_ACCOUNT_ID] }
        .stateIn(scope, SharingStarted.Eagerly, null)

    override suspend fun setDefaultAccountId(id: String?) {
        context.settingsDataStore.edit {
            if (id == null) it.remove(Keys.DEFAULT_ACCOUNT_ID)
            else it[Keys.DEFAULT_ACCOUNT_ID] = id
        }
    }

    override val exportFormat: StateFlow<ExportFormat> = context.settingsDataStore.data
        .map { prefs ->
            val name = prefs[Keys.EXPORT_FORMAT] ?: ExportFormat.CSV.name
            try { ExportFormat.valueOf(name) } catch (_: Exception) { ExportFormat.CSV }
        }
        .stateIn(scope, SharingStarted.Eagerly, ExportFormat.CSV)

    override suspend fun setExportFormat(format: ExportFormat) {
        context.settingsDataStore.edit { it[Keys.EXPORT_FORMAT] = format.name }
    }

    override val isSmsRationaleHidden: StateFlow<Boolean> = context.settingsDataStore.data
        .map { it[Keys.SMS_RATIONALE_HIDDEN] ?: false }
        .stateIn(scope, SharingStarted.Eagerly, false)

    override suspend fun setSmsRationaleHidden(hidden: Boolean) {
        context.settingsDataStore.edit { it[Keys.SMS_RATIONALE_HIDDEN] = hidden }
    }

    override suspend fun clear() {
        context.settingsDataStore.edit { it.clear() }
    }

    private object Keys {
        val APP_THEME = stringPreferencesKey("app_theme")
        val TIME_FORMAT = stringPreferencesKey("time_format")
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val BALANCE_HIDDEN = booleanPreferencesKey("balance_hidden")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_TIME = stringPreferencesKey("reminder_time")
        val MPESA_SIM_SLOT = intPreferencesKey("mpesa_sim_slot")
        val MPESA_LINKED_ACCOUNT_IDS = stringSetPreferencesKey("mpesa_linked_account_ids")
        val EQUITY_LINKED_ACCOUNT_IDS = stringSetPreferencesKey("equity_linked_account_ids")
        val MPESA_LISTENER_ENABLED = booleanPreferencesKey("mpesa_listener_enabled")
        val EQUITY_LISTENER_ENABLED = booleanPreferencesKey("equity_listener_enabled")
        val BUDGET_ALERTS_ENABLED = booleanPreferencesKey("budget_alerts_enabled")
        val BUDGET_ALERT_THRESHOLDS = stringSetPreferencesKey("budget_alert_thresholds")
        val ALERT_BUDGET_ID = stringPreferencesKey("alert_budget_id")
        val BILL_REMINDER_ENABLED = booleanPreferencesKey("bill_reminder_enabled")
        val BILL_REMINDER_DAYS_BEFORE = intPreferencesKey("bill_reminder_days_before")
        val DAILY_SUMMARY_ENABLED = booleanPreferencesKey("daily_summary_enabled")
        val WEEKLY_SUMMARY_ENABLED = booleanPreferencesKey("weekly_summary_enabled")
        val SUMMARY_NOTIFICATION_TIME = stringPreferencesKey("summary_notification_time")
        val SHOW_DECIMALS = booleanPreferencesKey("show_decimals")
        val DEFAULT_ACCOUNT_ID = stringPreferencesKey("default_account_id")
        val EXPORT_FORMAT = stringPreferencesKey("export_format")
        val SMS_RATIONALE_HIDDEN = booleanPreferencesKey("sms_rationale_hidden")
    }
}
