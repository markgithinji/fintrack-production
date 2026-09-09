package com.fintrack.shared.feature.core.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fintrack.shared.feature.settings.domain.datasource.SettingsDataSource
import com.fintrack.shared.feature.transaction.domain.usecase.GetSpendingSummaryUseCase
import com.fintrack.shared.feature.transaction.domain.usecase.SummaryPeriod
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.core.util.formatToAmount
import com.fintrack.shared.feature.core.domain.service.NotificationService
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver(), KoinComponent {
    private val notificationService: NotificationService by inject()
    private val settingsDataSource: SettingsDataSource by inject()
    private val getSpendingSummaryUseCase: GetSpendingSummaryUseCase by inject()

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (action) {
                    NotificationConstants.ACTION_SHOW_REMINDER -> {
                        val isEnabled = settingsDataSource.isReminderEnabled.first()
                        if (isEnabled) {
                            notificationService.showReminderNotification()
                            val time = settingsDataSource.reminderTime.first()
                            notificationService.scheduleDailyReminder(time)
                        }
                    }
                    NotificationConstants.ACTION_SHOW_BILL_REMINDER -> {
                        val billName = intent.getStringExtra(NotificationConstants.EXTRA_BILL_NAME) ?: "Bill"
                        val amountStr = intent.getStringExtra(NotificationConstants.EXTRA_AMOUNT) ?: "0"
                        val amount = try { BigDecimal.parseString(amountStr) } catch(_: Exception) { BigDecimal.ZERO }
                        notificationService.showBillReminderNotification(billName, amount)
                    }
                    NotificationConstants.ACTION_SHOW_SUMMARY -> {
                        handleSummaryNotification()
                    }
                    Intent.ACTION_BOOT_COMPLETED,
                    NotificationConstants.ACTION_QUICKBOOT_POWERON,
                    NotificationConstants.ACTION_HTC_QUICKBOOT_POWERON -> {
                        // Restore alarms after reboot
                        val dailyEnabled = settingsDataSource.isReminderEnabled.first()
                        if (dailyEnabled) {
                            notificationService.scheduleDailyReminder(settingsDataSource.reminderTime.first())
                        }
                        
                        val summaryEnabled = settingsDataSource.isDailySummaryEnabled.first() || 
                                          settingsDataSource.isWeeklySummaryEnabled.first()
                        if (summaryEnabled) {
                            notificationService.scheduleSummaryNotification(settingsDataSource.summaryNotificationTime.first())
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleSummaryNotification() {
        val dailyEnabled = settingsDataSource.isDailySummaryEnabled.first()
        val weeklyEnabled = settingsDataSource.isWeeklySummaryEnabled.first()
        val time = settingsDataSource.summaryNotificationTime.first()
        val showDecimals = settingsDataSource.showDecimals.first()

        if (dailyEnabled) {
            val yesterdayResult = getSpendingSummaryUseCase(SummaryPeriod.YESTERDAY)
            if (yesterdayResult is Result.Success) {
                val yesterdaySpending = yesterdayResult.data
                notificationService.showSummaryNotification(
                    title = "Daily Spending Summary",
                    content = "You spent Ksh ${yesterdaySpending.formatToAmount(showDecimals = showDecimals)} yesterday."
                )
            }
        }

        // Only show weekly on Sundays
        if (weeklyEnabled && Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            val weeklyResult = getSpendingSummaryUseCase(SummaryPeriod.LAST_WEEK)
            if (weeklyResult is Result.Success) {
                val weeklySpending = weeklyResult.data
                notificationService.showSummaryNotification(
                    title = "Weekly Spending Summary",
                    content = "You spent Ksh ${weeklySpending.formatToAmount(showDecimals = showDecimals)} this past week."
                )
            }
        }

        // Re-schedule
        if (dailyEnabled || weeklyEnabled) {
            notificationService.scheduleSummaryNotification(time)
        }
    }
}
