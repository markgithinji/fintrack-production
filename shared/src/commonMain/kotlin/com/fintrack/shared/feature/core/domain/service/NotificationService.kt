package com.fintrack.shared.feature.core.domain.service

import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.LocalTime
import kotlinx.datetime.LocalDate

interface NotificationService {
    fun showReminderNotification()
    fun scheduleDailyReminder(time: LocalTime? = null)
    fun cancelDailyReminder()
    fun requestPermission(callback: (Boolean) -> Unit)
    fun showTransactionNotification(transaction: Transaction)
    fun showBudgetAlertNotification(budgetName: String, threshold: Int)
    fun showBillReminderNotification(billName: String, amount: BigDecimal)
    fun scheduleBillReminder(billName: String, amount: BigDecimal, dueDate: LocalDate, daysBefore: Int)
    fun showSummaryNotification(title: String, content: String)
    fun scheduleSummaryNotification(time: LocalTime)
    fun cancelSummaryNotification()
}
