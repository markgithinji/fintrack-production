package com.fintrack.shared.feature.core.service

import com.fintrack.shared.feature.core.domain.service.NotificationService
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter

class IOSNotificationService(
    private val notificationCenter: UNUserNotificationCenter = UNUserNotificationCenter.currentNotificationCenter()
) : NotificationService {

    override fun showReminderNotification() {}

    override fun scheduleDailyReminder(time: LocalTime?) {}

    override fun cancelDailyReminder() {}

    override fun requestPermission(callback: (Boolean) -> Unit) {
        val options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        notificationCenter.requestAuthorizationWithOptions(options) { granted, _ ->
            callback(granted)
        }
    }

    override fun showTransactionNotification(transaction: Transaction) {}

    override fun showBudgetAlertNotification(budgetName: String, threshold: Int) {}

    override fun showBillReminderNotification(billName: String, amount: BigDecimal) {}

    override fun scheduleBillReminder(
        billName: String,
        amount: BigDecimal,
        dueDate: LocalDate,
        daysBefore: Int
    ) {}

    override fun showSummaryNotification(title: String, content: String) {}

    override fun scheduleSummaryNotification(time: LocalTime) {}

    override fun cancelSummaryNotification() {}

    override fun areNotificationsEnabled(): Boolean {
        return true
    }
}
