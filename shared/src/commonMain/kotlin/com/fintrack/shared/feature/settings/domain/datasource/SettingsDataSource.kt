package com.fintrack.shared.feature.settings.domain.datasource

import com.fintrack.shared.feature.settings.domain.model.AppTheme
import com.fintrack.shared.feature.settings.domain.model.Currency
import com.fintrack.shared.feature.settings.domain.model.ExportFormat
import com.fintrack.shared.feature.settings.domain.model.TimeFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalTime

interface SettingsDataSource {
    val theme: StateFlow<AppTheme>
    suspend fun setTheme(theme: AppTheme)

    val timeFormat: StateFlow<TimeFormat>
    suspend fun setTimeFormat(format: TimeFormat)

    val currency: StateFlow<Currency>
    suspend fun setCurrency(currency: Currency)

    val isBiometricEnabled: StateFlow<Boolean>
    suspend fun setBiometricEnabled(enabled: Boolean)

    val isBalanceHidden: StateFlow<Boolean>
    suspend fun setBalanceHidden(hidden: Boolean)

    val isReminderEnabled: StateFlow<Boolean>
    suspend fun setReminderEnabled(enabled: Boolean)

    val reminderTime: StateFlow<LocalTime>
    suspend fun setReminderTime(time: LocalTime)

    val mpesaSimSlot: StateFlow<Int?>
    suspend fun setMpesaSimSlot(slot: Int?)

    val mpesaLinkedAccountIds: StateFlow<Set<String>>
    suspend fun setMpesaLinkedAccountIds(ids: Set<String>)

    val equityLinkedAccountIds: StateFlow<Set<String>>
    suspend fun setEquityLinkedAccountIds(ids: Set<String>)

    val isMpesaListenerEnabled: StateFlow<Boolean>
    suspend fun setMpesaListenerEnabled(enabled: Boolean)

    val isEquityListenerEnabled: StateFlow<Boolean>
    suspend fun setEquityListenerEnabled(enabled: Boolean)

    val budgetAlertsEnabled: StateFlow<Boolean>
    suspend fun setBudgetAlertsEnabled(enabled: Boolean)

    val budgetAlertThresholds: StateFlow<Set<Int>>
    suspend fun setBudgetAlertThresholds(thresholds: Set<Int>)

    val alertBudgetId: StateFlow<String?>
    suspend fun setAlertBudgetId(budgetId: String?)

    val isBillReminderEnabled: StateFlow<Boolean>
    suspend fun setBillReminderEnabled(enabled: Boolean)

    val billReminderDaysBefore: StateFlow<Int>
    suspend fun setBillReminderDaysBefore(days: Int)

    val isDailySummaryEnabled: StateFlow<Boolean>
    suspend fun setDailySummaryEnabled(enabled: Boolean)

    val isWeeklySummaryEnabled: StateFlow<Boolean>
    suspend fun setWeeklySummaryEnabled(enabled: Boolean)

    val summaryNotificationTime: StateFlow<LocalTime>
    suspend fun setSummaryNotificationTime(time: LocalTime)

    val showDecimals: StateFlow<Boolean>
    suspend fun setShowDecimals(show: Boolean)

    val defaultAccountId: StateFlow<String?>
    suspend fun setDefaultAccountId(id: String?)

    val exportFormat: StateFlow<ExportFormat>
    suspend fun setExportFormat(format: ExportFormat)

    val isSmsRationaleHidden: StateFlow<Boolean>
    suspend fun setSmsRationaleHidden(hidden: Boolean)

    suspend fun clear()
}
